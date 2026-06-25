package com.ssafy.e106.feature.analysis

import com.ssafy.e106.feature.analysis.model.AnalysisCandidate
import com.ssafy.e106.feature.analysis.model.AppUsageSnapshot
import com.ssafy.e106.feature.analysis.model.NormalizedPaymentRecord
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import javax.inject.Inject
import kotlin.math.abs

class SubscriptionCandidateExtractor @Inject constructor() {
    fun extractCandidates(
        normalizedPayments: List<NormalizedPaymentRecord>,
        usageItems: List<AppUsageSnapshot>,
        serviceCatalog: ServiceCatalog,
    ): List<AnalysisCandidate> {
        if (normalizedPayments.isEmpty()) return emptyList()

        val paymentsByMerchant = normalizedPayments.groupBy { payment -> payment.merchantNormalized }
        val activeUsagePairs = usageItems
            .asSequence()
            .filter(::isActiveUsage)
            .mapNotNull { snapshot ->
                serviceCatalog.findServiceByPackageName(snapshot.packageName)?.let { service ->
                    snapshot to service
                }
            }
            .toList()
        val activeUsageServiceIds = activeUsagePairs.mapNotNull { (_, service) -> service.serviceId }.toSet()
        val activeUsageServiceCodes = activeUsagePairs.map { (_, service) -> service.code }.toSet()

        return normalizedPayments.mapNotNull { normalized ->
            if (normalized.paymentRecord.canceled || normalized.paymentRecord.refunded) return@mapNotNull null
            if ((normalized.paymentRecord.installmentMonths ?: 0) > 1) return@mapNotNull null

            val merchantPayments = paymentsByMerchant[normalized.merchantNormalized].orEmpty()
            val serviceHints = findServiceHints(
                normalizedPayment = normalized,
                activeUsagePairs = activeUsagePairs,
                serviceCatalog = serviceCatalog,
            )
            val bundleHints = findBundleHints(
                normalizedPayment = normalized,
                serviceCatalog = serviceCatalog,
                activeUsageServiceIds = activeUsageServiceIds,
                activeUsageServiceCodes = activeUsageServiceCodes,
            )
            val hintedServiceCodes = buildSet {
                addAll(serviceHints.map { service -> service.code })
                addAll(bundleHints.flatMap { bundle -> bundle.serviceCodes })
            }
            val usageSignals = activeUsagePairs
                .filter { (_, service) -> service.code in hintedServiceCodes }
                .map { (snapshot, _) -> snapshot }
            val recurrenceLabel = calculateRecurrenceLabel(merchantPayments)

            val scoreBreakdown = linkedMapOf<String, Double>()
            if (serviceHints.isNotEmpty()) scoreBreakdown["serviceHint"] = 0.95
            if (bundleHints.isNotEmpty()) scoreBreakdown["bundleHint"] = 1.05
            if (merchantPayments.size >= 2) {
                scoreBreakdown["repeatMerchant"] = if (recurrenceLabel != null) 0.9 else 0.6
            }
            if (matchesSinglePlanPrice(serviceHints, normalized.normalizedAmount)) {
                scoreBreakdown["planPriceHint"] = 0.55
            }
            if (matchesBundlePlanPrice(bundleHints, normalized.normalizedAmount)) {
                scoreBreakdown["bundlePriceHint"] = 0.75
            }
            val bundleOverlap = bundleHints.maxOfOrNull { bundle ->
                maxOf(
                    bundle.serviceIds.count { serviceId -> serviceId in activeUsageServiceIds },
                    bundle.serviceCodes.count { serviceCode -> serviceCode in activeUsageServiceCodes },
                )
            } ?: 0
            if (bundleOverlap >= 2) {
                scoreBreakdown["bundleUsageOverlap"] = 0.85
            } else if (usageSignals.isNotEmpty()) {
                scoreBreakdown["usageSignals"] = 0.45
            }
            if (GENERIC_SUBSCRIPTION_HINTS.any { token -> normalized.merchantNormalized.contains(token) }) {
                scoreBreakdown["genericSubscriptionToken"] = 0.25
            }
            if (OFFLINE_PURCHASE_HINTS.any { token -> normalized.merchantNormalized.contains(token) }) {
                scoreBreakdown["offlinePenalty"] = -1.35
            }

            val candidateScore = scoreBreakdown.values.sum()
            if (candidateScore < MINIMUM_CANDIDATE_SCORE) return@mapNotNull null

            AnalysisCandidate(
                reviewId = normalized.paymentRecord.toStableReviewId(),
                paymentRecord = normalized.paymentRecord,
                normalizedPaymentRecord = normalized,
                usageSignals = usageSignals,
                serviceCatalogHints = serviceHints,
                bundleCatalogHints = bundleHints,
                activeUsageServiceIds = activeUsageServiceIds,
                activeUsageServiceCodes = activeUsageServiceCodes,
                recurrenceLabel = recurrenceLabel,
                ruleScores = scoreBreakdown + ("candidateScore" to candidateScore),
            )
        }
    }

    private fun findServiceHints(
        normalizedPayment: NormalizedPaymentRecord,
        activeUsagePairs: List<Pair<AppUsageSnapshot, ServiceCatalog.Service>>,
        serviceCatalog: ServiceCatalog,
    ): List<ServiceCatalog.Service> {
        val usageMatchedServiceIds = activeUsagePairs.mapNotNull { (_, service) -> service.serviceId }.toSet()

        return serviceCatalog.services
            .asSequence()
            .mapNotNull { service ->
                val aliasMatched = sequenceOf(service.code, service.name)
                    .plus(service.aliases.asSequence())
                    .plus(service.knownMerchantPatterns.asSequence())
                    .map(::sanitizeMerchant)
                    .any { alias ->
                        alias.isNotBlank() && normalizedPayment.merchantNormalized.contains(alias)
                    }
                val usageMatched = service.serviceId != null && service.serviceId in usageMatchedServiceIds
                if (aliasMatched || usageMatched) service else null
            }
            .sortedByDescending { service ->
                service.aliases.count { alias ->
                    normalizedPayment.merchantNormalized.contains(sanitizeMerchant(alias))
                }
            }
            .toList()
    }

    private fun findBundleHints(
        normalizedPayment: NormalizedPaymentRecord,
        serviceCatalog: ServiceCatalog,
        activeUsageServiceIds: Set<Long>,
        activeUsageServiceCodes: Set<String>,
    ): List<ServiceCatalog.Bundle> {
        return serviceCatalog.bundles
            .asSequence()
            .mapNotNull { bundle ->
                val score = scoreBundleHint(
                    bundle = bundle,
                    merchantNormalized = normalizedPayment.merchantNormalized,
                    activeUsageServiceIds = activeUsageServiceIds,
                    activeUsageServiceCodes = activeUsageServiceCodes,
                    amount = normalizedPayment.normalizedAmount,
                )
                if (score > 0) bundle to score else null
            }
            .sortedByDescending { (_, score) -> score }
            .map { (bundle, _) -> bundle }
            .toList()
    }

    private fun scoreBundleHint(
        bundle: ServiceCatalog.Bundle,
        merchantNormalized: String,
        activeUsageServiceIds: Set<Long>,
        activeUsageServiceCodes: Set<String>,
        amount: Int,
    ): Int {
        var score = 0

        val aliasMatched = sequenceOf(bundle.code, bundle.name)
            .plus(bundle.aliases.asSequence())
            .plus(bundle.knownMerchantPatterns.asSequence())
            .map(::sanitizeMerchant)
            .any { alias -> alias.isNotBlank() && merchantNormalized.contains(alias) }
        if (aliasMatched) score += 4

        val overlapCount = maxOf(
            bundle.serviceIds.count { serviceId -> serviceId in activeUsageServiceIds },
            bundle.serviceCodes.count { serviceCode -> serviceCode in activeUsageServiceCodes },
        )
        when {
            overlapCount >= 2 -> score += 3
            overlapCount == 1 -> score += 1
        }

        if (bundle.plans.any { plan ->
                abs(plan.monthlyPrice - amount) <= (bundle.priceTolerance ?: DEFAULT_PRICE_TOLERANCE)
            }
        ) {
            score += 2
        }

        return score
    }

    private fun calculateRecurrenceLabel(payments: List<NormalizedPaymentRecord>): String? {
        if (payments.size < 2) return null

        val sortedPayments = payments.sortedBy { payment -> payment.paymentDate }
        val dayIntervals = sortedPayments.zipWithNext { current, next ->
            java.time.temporal.ChronoUnit.DAYS.between(current.paymentDate, next.paymentDate).toInt()
        }
        if (dayIntervals.isEmpty()) return null

        val average = dayIntervals.average()
        return when {
            average in 6.0..8.0 -> "WEEKLY"
            average in 25.0..35.0 -> "MONTHLY"
            average in 80.0..100.0 -> "QUARTERLY"
            average in 330.0..390.0 -> "YEARLY"
            else -> "REPEATED"
        }
    }

    private fun matchesSinglePlanPrice(serviceHints: List<ServiceCatalog.Service>, amount: Int): Boolean {
        return serviceHints.any { service ->
            service.plans.any { plan ->
                abs(plan.monthlyPrice - amount) <= (service.priceTolerance ?: DEFAULT_PRICE_TOLERANCE)
            }
        }
    }

    private fun matchesBundlePlanPrice(bundleHints: List<ServiceCatalog.Bundle>, amount: Int): Boolean {
        return bundleHints.any { bundle ->
            bundle.plans.any { plan ->
                abs(plan.monthlyPrice - amount) <= (bundle.priceTolerance ?: DEFAULT_PRICE_TOLERANCE)
            }
        }
    }

    private fun isActiveUsage(snapshot: AppUsageSnapshot): Boolean {
        return snapshot.lastUsedEpochMs != null || snapshot.usage30dMs > 0L || snapshot.usage7dMs > 0L
    }

    private companion object {
        const val DEFAULT_PRICE_TOLERANCE = 2_000
        const val MINIMUM_CANDIDATE_SCORE = 1.0

        val OFFLINE_PURCHASE_HINTS = setOf(
            "편의점",
            "마트",
            "주유",
            "카페",
            "병원",
            "음식",
            "택시",
            "버스",
            "배달",
        )
        val GENERIC_SUBSCRIPTION_HINTS = setOf(
            "APPLE",
            "GOOGLE",
            "BILL",
            "SUBSCRIPTION",
            "정기결제",
        )
    }
}
