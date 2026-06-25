package com.ssafy.e106.feature.analysis

import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyItemRequest
import com.ssafy.e106.data.repository.AnalysisHandoffRepository
import com.ssafy.e106.data.repository.ServiceRepository
import com.ssafy.e106.data.repository.SubscriptionRepository
import com.ssafy.e106.data.repository.SubscriptionUsageRepository
import com.ssafy.e106.feature.analysis.model.AnalysisSessionResult
import com.ssafy.e106.feature.analysis.model.AnalysisCandidate
import com.ssafy.e106.feature.analysis.model.AppUsageSnapshot
import com.ssafy.e106.feature.analysis.model.CandidateResolution
import com.ssafy.e106.feature.analysis.model.PaymentRecord
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import com.ssafy.e106.feature.analysis.model.TrackedSubscriptionPackages
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class AnalysisPipelinePhase {
    PaymentBootstrap,
    CatalogPreload,
    UsageStatsCollection,
    Normalization,
    CandidateExtraction,
    FirstPassFilter,
    BatchLookup,
    AiResolver,
    HandoffPersisted,
}

data class AnalysisPipelineSnapshot(
    val paymentRecords: List<PaymentRecord>,
    val serviceCatalog: ServiceCatalog,
    val usageItems: List<AppUsageSnapshot>,
    val analysisSessionResult: AnalysisSessionResult,
    val estimatedSavingAmount: Int,
)

class AnalysisPipelineOrchestrator @Inject constructor(
    private val paymentBootstrapSeam: PaymentBootstrapSeam,
    private val serviceRepository: ServiceRepository,
    private val usageStatsCollector: UsageStatsCollector,
    private val paymentRecordNormalizer: PaymentRecordNormalizer,
    private val candidateExtractor: SubscriptionCandidateExtractor,
    private val ruleBasedCandidateResolver: RuleBasedCandidateResolver,
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionUsageRepository: SubscriptionUsageRepository,
    private val onDeviceAiResolver: OnDeviceAiResolver,
    private val analysisHandoffRepository: AnalysisHandoffRepository,
) {
    suspend fun run(
        onPhaseChanged: suspend (AnalysisPipelinePhase) -> Unit = {},
    ): AnalysisPipelineSnapshot {
        onPhaseChanged(AnalysisPipelinePhase.PaymentBootstrap)
        val paymentPayload = paymentBootstrapSeam.loadPaymentRecords()

        onPhaseChanged(AnalysisPipelinePhase.CatalogPreload)
        val serviceCatalog = preloadServiceCatalog()

        onPhaseChanged(AnalysisPipelinePhase.UsageStatsCollection)
        val usageItems = usageStatsCollector.collectAll(TrackedSubscriptionPackages.values)
        uploadDailyUsage(
            usageItems = usageItems,
            serviceCatalog = serviceCatalog,
        )

        onPhaseChanged(AnalysisPipelinePhase.Normalization)
        val normalizedPayments = paymentRecordNormalizer.normalize(paymentPayload.paymentRecords)

        onPhaseChanged(AnalysisPipelinePhase.CandidateExtraction)
        val candidates = candidateExtractor.extractCandidates(
            normalizedPayments = normalizedPayments,
            usageItems = usageItems,
            serviceCatalog = serviceCatalog,
        )

        onPhaseChanged(AnalysisPipelinePhase.FirstPassFilter)
        val firstPassResult = ruleBasedCandidateResolver.resolve(candidates)

        onPhaseChanged(AnalysisPipelinePhase.BatchLookup)
        val batchLookupOutcome = when (
            val lookupResult = subscriptionRepository.resolveCandidatesWithBatchLookup(
                firstPassResult.unresolved,
            )
        ) {
            is Result.Success -> lookupResult.data
            is Result.Error -> {
                subscriptionRepository.createBatchLookupFallback(firstPassResult.unresolved)
            }

            Result.Loading -> subscriptionRepository.createBatchLookupFallback(firstPassResult.unresolved)
        }

        onPhaseChanged(AnalysisPipelinePhase.AiResolver)
        val aiResolutions = onDeviceAiResolver.resolve(batchLookupOutcome.aiHints)
        val finalResolutions = buildFinalResolutions(
            ruleResolved = firstPassResult.resolved,
            batchLookupResolved = batchLookupOutcome.resolved,
            aiResolved = aiResolutions,
        )

        val analysisSessionResult = AnalysisSessionResult(
            sessionId = "analysis-${System.currentTimeMillis()}",
            analyzedAtEpochMs = System.currentTimeMillis(),
            source = AnalysisSessionResult.AnalysisSource.HYBRID,
            serviceCatalog = serviceCatalog,
            usageItems = usageItems,
            candidates = candidates,
            resolutions = finalResolutions,
        )

        analysisHandoffRepository.storeAnalysisSessionResult(analysisSessionResult)
        onPhaseChanged(AnalysisPipelinePhase.HandoffPersisted)

        return AnalysisPipelineSnapshot(
            paymentRecords = paymentPayload.paymentRecords,
            serviceCatalog = serviceCatalog,
            usageItems = usageItems,
            analysisSessionResult = analysisSessionResult,
            estimatedSavingAmount = estimateSavingAmount(
                usageItems = usageItems,
                resolutions = finalResolutions,
                candidates = candidates,
                paymentBootstrapIsPlaceholder = paymentPayload.isPlaceholder,
            ),
        )
    }

    private suspend fun uploadDailyUsage(
        usageItems: List<AppUsageSnapshot>,
        serviceCatalog: ServiceCatalog,
    ) {
        val usageItemsByServiceId = mutableMapOf<Pair<Long, LocalDate>, Int>()

        usageItems.forEach { snapshot ->
            val service = serviceCatalog.findServiceByPackageName(snapshot.packageName) ?: return@forEach
            val serviceId = service.serviceId ?: return@forEach

            snapshot.dailyUsageMinutesByDate.forEach { (usageDate, usedMinutes) ->
                if (usedMinutes < 1) return@forEach
                val key = serviceId to usageDate
                usageItemsByServiceId[key] = (usageItemsByServiceId[key] ?: 0) + usedMinutes
            }
        }

        val requestItems = usageItemsByServiceId.entries
            .sortedWith(
                compareBy<Map.Entry<Pair<Long, LocalDate>, Int>>(
                    { it.key.first },
                    { it.key.second },
                ),
            )
            .map { (key, usedMinutes) ->
                SubscriptionUsageDailyItemRequest(
                    serviceId = key.first,
                    usageDate = key.second.format(USAGE_DATE_FORMATTER),
                    usedMinutes = usedMinutes,
                )
            }

        when (subscriptionUsageRepository.upsertDailyUsage(requestItems)) {
            is Result.Success -> Unit
            is Result.Error -> Unit
            Result.Loading -> Unit
        }
    }

    private suspend fun preloadServiceCatalog(): ServiceCatalog {
        when (val serviceResult = serviceRepository.getAvailableServices()) {
            is Result.Success -> {
                serviceResult.data.forEach { service ->
                    when (serviceRepository.getServicePlans(service.serviceId)) {
                        is Result.Success -> Unit
                        is Result.Error -> Unit
                        Result.Loading -> Unit
                    }
                }
            }

            is Result.Error -> Unit
            Result.Loading -> Unit
        }

        return serviceRepository.getCachedServiceCatalog()
    }

    private fun buildFinalResolutions(
        ruleResolved: List<CandidateResolution>,
        batchLookupResolved: List<CandidateResolution>,
        aiResolved: List<CandidateResolution>,
    ): List<CandidateResolution> {
        return buildList {
            addAll(ruleResolved)
            addAll(batchLookupResolved)
            addAll(aiResolved)
        }.distinctBy { resolution -> resolution.reviewId }
    }

    private fun estimateSavingAmount(
        usageItems: List<AppUsageSnapshot>,
        resolutions: List<CandidateResolution>,
        candidates: List<AnalysisCandidate>,
        paymentBootstrapIsPlaceholder: Boolean,
    ): Int {
        val activeUsageCount = usageItems.count { usage ->
            usage.lastUsedEpochMs != null || usage.usage30dMs > 0L || usage.usage7dMs > 0L
        }
        val highConfidenceSavings = collapseHighConfidenceSubscriptions(
            resolutions = resolutions,
            candidates = candidates,
        )
            .sumOf { resolution -> resolution.monthlyAmount }
        val pendingReviewSignal = resolutions.count { resolution -> resolution.requiresUserReview() } * 800
        val placeholderPenalty = if (paymentBootstrapIsPlaceholder) 0 else 1_500
        return (highConfidenceSavings + (activeUsageCount * 600) + pendingReviewSignal + placeholderPenalty)
            .coerceIn(0, 18_700)
    }

    private fun collapseHighConfidenceSubscriptions(
        resolutions: List<CandidateResolution>,
        candidates: List<AnalysisCandidate>,
    ): List<CandidateResolution> {
        val paymentDatesByReviewId = candidates.associate { candidate ->
            candidate.reviewId to candidate.paymentRecord.paymentDate
        }

        return resolutions
            .asSequence()
            .filter { resolution -> resolution.isConfirmedHighConfidence() }
            .sortedWith(
                compareByDescending<CandidateResolution> { resolution ->
                    paymentDatesByReviewId[resolution.reviewId] ?: LocalDate.MIN
                }.thenByDescending { resolution ->
                    resolution.reviewId
                },
            )
            .distinctBy { resolution -> resolution.serviceId?.toString() ?: "review:${resolution.reviewId}" }
            .toList()
    }

    private companion object {
        val USAGE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
