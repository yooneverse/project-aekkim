package com.ssafy.e106.feature.analysis

import com.ssafy.e106.feature.analysis.model.AnalysisCandidate
import com.ssafy.e106.feature.analysis.model.CandidateResolution
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import javax.inject.Inject
import kotlin.math.abs

data class RuleBasedResolutionResult(
    val resolved: List<CandidateResolution> = emptyList(),
    val unresolved: List<AnalysisCandidate> = emptyList(),
)

class RuleBasedCandidateResolver @Inject constructor() {
    fun resolve(candidates: List<AnalysisCandidate>): RuleBasedResolutionResult {
        if (candidates.isEmpty()) return RuleBasedResolutionResult()

        val resolved = mutableListOf<CandidateResolution>()
        val unresolved = mutableListOf<AnalysisCandidate>()

        candidates.forEach { candidate ->
            val bestService = candidate.serviceCatalogHints.maxByOrNull { service ->
                scoreServiceHint(candidate, service)
            }
            val bestBundle = candidate.bundleCatalogHints.maxByOrNull { bundle ->
                scoreBundleHint(candidate, bundle)
            }

            val bestPlan = bestService?.plans?.minByOrNull { plan ->
                abs(plan.monthlyPrice - candidate.normalizedPaymentRecord.normalizedAmount)
            }
            val bestBundlePlan = bestBundle?.plans?.minByOrNull { plan ->
                abs(plan.monthlyPrice - candidate.normalizedPaymentRecord.normalizedAmount)
            }

            val servicePriceTolerance = bestService?.priceTolerance ?: DEFAULT_PRICE_TOLERANCE
            val bundlePriceTolerance = bestBundle?.priceTolerance ?: DEFAULT_PRICE_TOLERANCE
            val servicePriceMatched = bestPlan != null &&
                abs(bestPlan.monthlyPrice - candidate.normalizedPaymentRecord.normalizedAmount) <= servicePriceTolerance
            val bundlePriceMatched = bestBundlePlan != null &&
                abs(bestBundlePlan.monthlyPrice - candidate.normalizedPaymentRecord.normalizedAmount) <= bundlePriceTolerance
            val usageMatched = bestService?.packageNames?.any { packageName ->
                candidate.usageSignals.any { usage -> usage.packageName == packageName && usage.usage30dMs > 0L }
            } == true
            val aliasMatched = bestService?.let { service -> matchesAlias(candidate, service) } ?: false
            val bundleAliasMatched = bestBundle?.let { bundle -> matchesBundleAlias(candidate, bundle) } ?: false
            val bundleOverlapCount = bestBundle?.let { bundle ->
                maxOf(
                    bundle.serviceIds.count { serviceId -> serviceId in candidate.activeUsageServiceIds },
                    bundle.serviceCodes.count { serviceCode -> serviceCode in candidate.activeUsageServiceCodes },
                )
            } ?: 0
            val recurrenceMatched = candidate.recurrenceLabel in STRONG_RECURRENCE_LABELS
            val candidateScore = candidate.ruleScores["candidateScore"] ?: 0.0

            val serviceConfidence = buildConfidence(
                aliasMatched = aliasMatched,
                priceMatched = servicePriceMatched,
                usageMatched = usageMatched,
                recurrenceMatched = recurrenceMatched,
            )
            val bundleConfidence = buildBundleConfidence(
                aliasMatched = bundleAliasMatched,
                priceMatched = bundlePriceMatched,
                overlapCount = bundleOverlapCount,
                recurrenceMatched = recurrenceMatched,
            )

            when {
                bestBundle != null &&
                    bundleAliasMatched &&
                    bundlePriceMatched &&
                    bundleOverlapCount >= 2 &&
                    bundleConfidence >= serviceConfidence -> {
                    resolved += CandidateResolution(
                        reviewId = candidate.reviewId,
                        decision = CandidateResolution.Decision.CONFIRMED_SUBSCRIPTION,
                        resolutionSource = CandidateResolution.ResolutionSource.RULE_BASED,
                        merchantRaw = candidate.paymentRecord.merchantRaw,
                        merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized,
                        monthlyAmount = candidate.normalizedPaymentRecord.normalizedAmount,
                        billedAtLabel = candidate.paymentRecord.toBilledAtLabel(),
                        subscriptionType = CandidateResolution.SubscriptionType.BUNDLE,
                        serviceName = bestBundle.name,
                        planName = bestBundlePlan?.planName,
                        bundleCode = bestBundle.code,
                        bundleName = bestBundle.name,
                        coveredServiceIds = bestBundle.serviceIds,
                        coveredServiceNames = candidate.serviceCatalogHints
                            .filter { service -> service.code in bestBundle.serviceCodes }
                            .map { service -> service.name }
                            .distinct(),
                        subscriptionConfidence = bundleConfidence,
                        serviceConfidence = bundleConfidence,
                        planConfidence = bundleConfidence,
                        highConfidence = bundleConfidence >= CandidateResolution.DEFAULT_HIGH_CONFIDENCE_THRESHOLD,
                        reasonCodes = buildList {
                            add("RULE_BUNDLE_ALIAS_MATCH")
                            add("RULE_BUNDLE_PRICE_MATCH")
                            add("RULE_BUNDLE_USAGE_OVERLAP_$bundleOverlapCount")
                            if (recurrenceMatched) add("RULE_RECURRENCE_MATCH")
                        },
                    )
                }

                bestService != null && aliasMatched && (servicePriceMatched || usageMatched || recurrenceMatched) -> {
                    resolved += CandidateResolution(
                        reviewId = candidate.reviewId,
                        decision = CandidateResolution.Decision.CONFIRMED_SUBSCRIPTION,
                        resolutionSource = CandidateResolution.ResolutionSource.RULE_BASED,
                        merchantRaw = candidate.paymentRecord.merchantRaw,
                        merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized,
                        monthlyAmount = candidate.normalizedPaymentRecord.normalizedAmount,
                        billedAtLabel = candidate.paymentRecord.toBilledAtLabel(),
                        subscriptionType = CandidateResolution.SubscriptionType.SINGLE,
                        serviceId = bestService.serviceId,
                        servicePlanId = if (servicePriceMatched) bestPlan?.servicePlanId else null,
                        serviceName = bestService.name,
                        planName = if (servicePriceMatched) bestPlan?.planName else null,
                        subscriptionConfidence = serviceConfidence,
                        serviceConfidence = serviceConfidence,
                        planConfidence = if (servicePriceMatched) serviceConfidence else null,
                        highConfidence = serviceConfidence >= CandidateResolution.DEFAULT_HIGH_CONFIDENCE_THRESHOLD,
                        reasonCodes = buildList {
                            add("RULE_ALIAS_MATCH")
                            if (servicePriceMatched) add("RULE_PRICE_MATCH")
                            if (usageMatched) add("RULE_USAGE_MATCH")
                            if (recurrenceMatched) add("RULE_RECURRENCE_MATCH")
                        },
                    )
                }

                bestBundle != null && bundleConfidence >= 0.7f -> {
                    unresolved += candidate
                }

                bestService == null && candidateScore < NON_SUBSCRIPTION_SCORE_THRESHOLD -> {
                    resolved += CandidateResolution(
                        reviewId = candidate.reviewId,
                        decision = CandidateResolution.Decision.CONFIRMED_NON_SUBSCRIPTION,
                        resolutionSource = CandidateResolution.ResolutionSource.RULE_BASED,
                        merchantRaw = candidate.paymentRecord.merchantRaw,
                        merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized,
                        monthlyAmount = candidate.normalizedPaymentRecord.normalizedAmount,
                        billedAtLabel = candidate.paymentRecord.toBilledAtLabel(),
                        subscriptionConfidence = 0.15f,
                        reasonCodes = listOf("RULE_LOW_SCORE_NON_SUBSCRIPTION"),
                    )
                }

                else -> unresolved += candidate
            }
        }

        return RuleBasedResolutionResult(
            resolved = resolved,
            unresolved = unresolved,
        )
    }

    private fun scoreServiceHint(candidate: AnalysisCandidate, service: ServiceCatalog.Service): Int {
        var score = 0
        if (matchesAlias(candidate, service)) score += 4
        if (service.packageNames.any { packageName ->
                candidate.usageSignals.any { usage -> usage.packageName == packageName && usage.usage30dMs > 0L }
            }
        ) {
            score += 2
        }
        if (service.plans.any { plan ->
                abs(plan.monthlyPrice - candidate.normalizedPaymentRecord.normalizedAmount) <=
                    (service.priceTolerance ?: DEFAULT_PRICE_TOLERANCE)
            }
        ) {
            score += 1
        }
        return score
    }

    private fun scoreBundleHint(candidate: AnalysisCandidate, bundle: ServiceCatalog.Bundle): Int {
        var score = 0
        if (matchesBundleAlias(candidate, bundle)) score += 4
        val overlapCount = maxOf(
            bundle.serviceIds.count { serviceId -> serviceId in candidate.activeUsageServiceIds },
            bundle.serviceCodes.count { serviceCode -> serviceCode in candidate.activeUsageServiceCodes },
        )
        when {
            overlapCount >= 2 -> score += 3
            overlapCount == 1 -> score += 1
        }
        if (bundle.plans.any { plan ->
                abs(plan.monthlyPrice - candidate.normalizedPaymentRecord.normalizedAmount) <=
                    (bundle.priceTolerance ?: DEFAULT_PRICE_TOLERANCE)
            }
        ) {
            score += 2
        }
        return score
    }

    private fun matchesAlias(candidate: AnalysisCandidate, service: ServiceCatalog.Service): Boolean {
        val merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized
        return sequenceOf(service.code, service.name)
            .plus(service.aliases.asSequence())
            .plus(service.knownMerchantPatterns.asSequence())
            .map(::sanitizeMerchant)
            .any { alias -> alias.isNotBlank() && merchantNormalized.contains(alias) }
    }

    private fun matchesBundleAlias(candidate: AnalysisCandidate, bundle: ServiceCatalog.Bundle): Boolean {
        val merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized
        return sequenceOf(bundle.code, bundle.name)
            .plus(bundle.aliases.asSequence())
            .plus(bundle.knownMerchantPatterns.asSequence())
            .map(::sanitizeMerchant)
            .any { alias -> alias.isNotBlank() && merchantNormalized.contains(alias) }
    }

    private fun buildConfidence(
        aliasMatched: Boolean,
        priceMatched: Boolean,
        usageMatched: Boolean,
        recurrenceMatched: Boolean,
    ): Float {
        var confidence = 0.35f
        if (aliasMatched) confidence += 0.25f
        if (priceMatched) confidence += 0.2f
        if (usageMatched) confidence += 0.1f
        if (recurrenceMatched) confidence += 0.1f
        return confidence.coerceAtMost(0.95f)
    }

    private fun buildBundleConfidence(
        aliasMatched: Boolean,
        priceMatched: Boolean,
        overlapCount: Int,
        recurrenceMatched: Boolean,
    ): Float {
        var confidence = 0.4f
        if (aliasMatched) confidence += 0.22f
        if (priceMatched) confidence += 0.18f
        if (overlapCount >= 2) confidence += 0.15f
        if (recurrenceMatched) confidence += 0.08f
        return confidence.coerceAtMost(0.97f)
    }

    private companion object {
        const val DEFAULT_PRICE_TOLERANCE = 2_000
        const val NON_SUBSCRIPTION_SCORE_THRESHOLD = 1.15
        val STRONG_RECURRENCE_LABELS = setOf("MONTHLY", "YEARLY")
    }
}
