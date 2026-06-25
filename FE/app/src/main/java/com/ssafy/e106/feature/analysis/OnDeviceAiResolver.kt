package com.ssafy.e106.feature.analysis

// LiteRT(Gemma3) 기반 LLM 롤백이 필요한 경우 OnDeviceAiContracts.kt를 참조할 것.
// REVIEW_NEEDED만 보려면: adb logcat -s AEKKIM_AI_RESOLVER:W
// 전체 파이프라인 흐름을 보려면: adb logcat -s AEKKIM_AI_RESOLVER:D

import android.content.Context
import android.util.Log
import com.ssafy.e106.ai.AiInterface
import com.ssafy.e106.data.repository.BatchLookupReviewHint
import com.ssafy.e106.feature.analysis.model.CandidateResolution
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnDeviceAiResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun resolve(reviewHints: List<BatchLookupReviewHint>): List<CandidateResolution> {
        if (reviewHints.isEmpty()) return emptyList()

        // 추론 루프는 CPU 집약적 작업이므로 IO 대신 Default 사용.
        // IO는 블로킹 I/O 대기용 스레드풀로 CPU 연산에는 불필요한 스레드를 낭비한다.
        return withContext(Dispatchers.Default) {
            // ── [신규 흐름] FastText + XGBoost 직접 추론 파이프라인 ──────────────────────────
            Log.i(TAG_RESOLVER, "파이프라인 실행 스레드: ${Thread.currentThread().name}")
            Log.i(TAG_RESOLVER, "FastText+XGBoost 파이프라인 시작, 후보 수: ${reviewHints.size}")

            // 파이프라인 초기화 (멱등 — 이미 초기화된 경우 스킵)
            val initResult = runCatching {
                AiInterface.initializeNewPipeline(context)
            }
            if (initResult.isFailure) {
                Log.e(TAG_RESOLVER, "FastText+XGBoost 파이프라인 초기화 실패 — 폴백 사용",
                    initResult.exceptionOrNull())
                return@withContext reviewHints.map { hint ->
                    fallbackReviewResolution(hint, reasonCodes = listOf("AI_PIPELINE_INIT_FAILED"))
                }
            }

            val fastTextEngine = AiInterface.fastTextEngine
            val xgboostEngine = AiInterface.xgboostEngine

            val results = reviewHints.map { hint ->
                runCatching {
                    // 1. FastText — 가맹점명 구독 확률 추론
                    val ftProb = fastTextEngine.predictSubscriptionProbability(
                        fastTextEngine.normalizeText(hint.candidate.normalizedPaymentRecord.merchantNormalized),
                    )
                    Log.d(TAG_RESOLVER, "FastText 결과: merchant='${hint.candidate.normalizedPaymentRecord.merchantNormalized}', sub_prob=$ftProb")

                    // 2. XGBoost — 21개 피처 기반 최종 판정
                    val features = xgboostEngine.buildFeatureArray(hint, ftProb)
                    val xgbPrediction = xgboostEngine.predict(features)
                    Log.d(TAG_RESOLVER, "XGBoost 결과: merchant='${hint.candidate.normalizedPaymentRecord.merchantNormalized}', decision=${xgbPrediction.decision}, conf=${xgbPrediction.confidence}")

                    // 3. 후처리 필터 적용
                    val decision = applyPostFilter(hint, xgbPrediction)

                    // 4. CandidateResolution 변환
                    val resolution = hint.toCandidateResolution(decision, xgbPrediction.confidence)
                    if (decision == REVIEW_NEEDED) {
                        val merchant = hint.candidate.normalizedPaymentRecord.merchantNormalized
                        Log.w(TAG_RESOLVER,
                            "REVIEW_NEEDED: merchant='$merchant'" +
                            ", conf=${xgbPrediction.confidence}" +
                            ", ftProb=$ftProb" +
                            ", amount=${hint.candidate.normalizedPaymentRecord.normalizedAmount}" +
                            ", isPayGateway=${PAY_GATEWAY_TOKENS.any { tok -> merchant.contains(tok) }}" +
                            ", offlinePenalty=${hint.offlinePenaltyScore}" +
                            ", candidateScore=${hint.candidate.ruleScores["candidateScore"] ?: 0.0}"
                        )
                    }
                    resolution
                }.getOrElse { e ->
                    Log.e(TAG_RESOLVER, "후보 추론 실패: reviewId=${hint.candidate.reviewId}", e)
                    fallbackReviewResolution(hint, reasonCodes = listOf("AI_INFERENCE_FAILED"))
                }
            }

            Log.i(TAG_RESOLVER, "파이프라인 완료, 결과 수: ${results.size}")
            Log.i(TAG_RESOLVER, "결과 요약: ${results.groupBy { it.decision }.mapValues { it.value.size }}")
            results
        }
    }

    // ── [신규] BatchLookupReviewHint → CandidateResolution 변환 확장 함수 ──────────────

    /**
     * XGBoost decision 값에 따라 CandidateResolution을 생성한다.
     * - "CONFIRMED_SUBSCRIPTION"     → 구독 확정 (confidence ≥ threshold 시 highConfidence=true)
     * - "CONFIRMED_NON_SUBSCRIPTION" → 비구독 확정
     * - "REVIEW_NEEDED"              → 검토 필요 (confidence < 0.85 포함)
     */
    private fun BatchLookupReviewHint.toCandidateResolution(
        decision: String,
        confidence: Float,
    ): CandidateResolution {
        Log.d(TAG_RESOLVER, "CandidateResolution 생성: svcKey=${this.candidate.reviewId}, decision=$decision")
        return when (decision.trim().uppercase()) {
            CONFIRMED_SUBSCRIPTION -> {
                if (confidence >= CandidateResolution.DEFAULT_HIGH_CONFIDENCE_THRESHOLD) {
                    CandidateResolution(
                        reviewId = candidate.reviewId,
                        decision = CandidateResolution.Decision.CONFIRMED_SUBSCRIPTION,
                        resolutionSource = CandidateResolution.ResolutionSource.ON_DEVICE_AI,
                        merchantRaw = candidate.paymentRecord.merchantRaw,
                        merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized,
                        monthlyAmount = candidate.normalizedPaymentRecord.normalizedAmount,
                        billedAtLabel = candidate.paymentRecord.toBilledAtLabel(),
                        serviceId = serviceId ?: candidate.primaryHintServiceId(),
                        servicePlanId = servicePlanId ?: candidate.primaryHintServicePlanId(),
                        serviceName = serviceName ?: candidate.primaryHintServiceName(),
                        subscriptionConfidence = confidence,
                        serviceConfidence = confidence,
                        highConfidence = true,
                        reasonCodes = buildReasonCodes(
                            modelReasonCodes = listOf("AI_CONFIRMED_SUBSCRIPTION"),
                            fallbackReason = "AI_CONFIRMED_SUBSCRIPTION",
                        ),
                    )
                } else {
                    // confidence < 0.85 → REVIEW_NEEDED로 강등
                    buildReviewResolution(
                        hint = this,
                        selection = fallbackSelection(),
                        confidence = confidence,
                        extraReasonCodes = listOf("AI_LOW_CONFIDENCE_REVIEW"),
                    )
                }
            }

            CONFIRMED_NON_SUBSCRIPTION -> {
                val effectiveConf = confidence.coerceAtLeast(0.91f)
                CandidateResolution(
                    reviewId = candidate.reviewId,
                    decision = CandidateResolution.Decision.CONFIRMED_NON_SUBSCRIPTION,
                    resolutionSource = CandidateResolution.ResolutionSource.ON_DEVICE_AI,
                    merchantRaw = candidate.paymentRecord.merchantRaw,
                    merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized,
                    monthlyAmount = candidate.normalizedPaymentRecord.normalizedAmount,
                    billedAtLabel = candidate.paymentRecord.toBilledAtLabel(),
                    subscriptionConfidence = effectiveConf,
                    highConfidence = false,
                    reasonCodes = buildReasonCodes(
                        modelReasonCodes = listOf("AI_CONFIRMED_NON_SUBSCRIPTION"),
                        fallbackReason = "AI_CONFIRMED_NON_SUBSCRIPTION",
                    ),
                )
            }

            else -> {
                // REVIEW_NEEDED 또는 기타 알 수 없는 decision
                buildReviewResolution(
                    hint = this,
                    selection = fallbackSelection(),
                    confidence = confidence.takeIf { it > 0.0f } ?: defaultFallbackConfidence(),
                    extraReasonCodes = listOf("AI_LOW_CONFIDENCE_REVIEW"),
                )
            }
        }
    }

    // ── [기존] 기존 OnDeviceAiModelItem 기반 변환 함수들 (삭제 금지) ─────────────────────

    private fun OnDeviceAiModelItem.toResolution(hint: BatchLookupReviewHint): CandidateResolution {
        val normalizedDecision = decision.trim().uppercase()
        return when (normalizedDecision) {
            CONFIRMED_SUBSCRIPTION -> toConfirmedSubscriptionResolution(hint)
            CONFIRMED_NON_SUBSCRIPTION -> toConfirmedNonSubscriptionResolution(hint)
            REVIEW_NEEDED -> toReviewResolution(hint)
            else -> fallbackReviewResolution(hint, reasonCodes = listOf("AI_UNKNOWN_DECISION"))
        }
    }

    private fun OnDeviceAiModelItem.toConfirmedSubscriptionResolution(
        hint: BatchLookupReviewHint,
    ): CandidateResolution {
        val selected = hint.resolveSelection(
            requestedServiceId = serviceId,
            requestedServicePlanId = servicePlanId,
            requestedServiceName = serviceName,
            requestedPlanName = planName,
        ) ?: return fallbackReviewResolution(
            hint,
            reasonCodes = listOf("AI_INVALID_CATALOG_REFERENCE"),
        )
        val confidence = subscriptionConfidence.normalizedConfidence()

        if (confidence < CandidateResolution.DEFAULT_HIGH_CONFIDENCE_THRESHOLD) {
            return buildReviewResolution(
                hint = hint,
                selection = selected,
                confidence = confidence,
                extraReasonCodes = reasonCodes + "AI_LOW_CONFIDENCE_REVIEW",
            )
        }

        return CandidateResolution(
            reviewId = hint.candidate.reviewId,
            decision = CandidateResolution.Decision.CONFIRMED_SUBSCRIPTION,
            resolutionSource = CandidateResolution.ResolutionSource.ON_DEVICE_AI,
            merchantRaw = hint.candidate.paymentRecord.merchantRaw,
            merchantNormalized = hint.candidate.normalizedPaymentRecord.merchantNormalized,
            monthlyAmount = hint.candidate.normalizedPaymentRecord.normalizedAmount,
            billedAtLabel = hint.candidate.paymentRecord.toBilledAtLabel(),
            serviceId = selected.serviceId,
            servicePlanId = selected.servicePlanId,
            serviceName = selected.serviceName,
            planName = selected.planName,
            subscriptionConfidence = confidence,
            serviceConfidence = serviceConfidence.normalizedConfidence(defaultValue = confidence),
            planConfidence = planConfidence.normalizedConfidence(defaultValue = confidence),
            highConfidence = true,
            reasonCodes = hint.buildReasonCodes(
                modelReasonCodes = reasonCodes,
                fallbackReason = "AI_CONFIRMED_SUBSCRIPTION",
            ),
        )
    }

    private fun OnDeviceAiModelItem.toConfirmedNonSubscriptionResolution(
        hint: BatchLookupReviewHint,
    ): CandidateResolution {
        val confidence = subscriptionConfidence.normalizedConfidence(defaultValue = 0.91f)
        return CandidateResolution(
            reviewId = hint.candidate.reviewId,
            decision = CandidateResolution.Decision.CONFIRMED_NON_SUBSCRIPTION,
            resolutionSource = CandidateResolution.ResolutionSource.ON_DEVICE_AI,
            merchantRaw = hint.candidate.paymentRecord.merchantRaw,
            merchantNormalized = hint.candidate.normalizedPaymentRecord.merchantNormalized,
            monthlyAmount = hint.candidate.normalizedPaymentRecord.normalizedAmount,
            billedAtLabel = hint.candidate.paymentRecord.toBilledAtLabel(),
            subscriptionConfidence = confidence,
            highConfidence = false,
            reasonCodes = hint.buildReasonCodes(
                modelReasonCodes = reasonCodes,
                fallbackReason = "AI_CONFIRMED_NON_SUBSCRIPTION",
            ),
        )
    }

    private fun OnDeviceAiModelItem.toReviewResolution(
        hint: BatchLookupReviewHint,
    ): CandidateResolution {
        val selection = hint.resolveSelection(
            requestedServiceId = serviceId,
            requestedServicePlanId = servicePlanId,
            requestedServiceName = serviceName,
            requestedPlanName = planName,
        )
        return buildReviewResolution(
            hint = hint,
            selection = selection ?: hint.fallbackSelection(),
            confidence = subscriptionConfidence.normalizedConfidence(defaultValue = hint.defaultFallbackConfidence()),
            extraReasonCodes = reasonCodes,
        )
    }

    private fun buildReviewResolution(
        hint: BatchLookupReviewHint,
        selection: ServiceSelection?,
        confidence: Float,
        extraReasonCodes: List<String>,
    ): CandidateResolution {
        return CandidateResolution(
            reviewId = hint.candidate.reviewId,
            decision = CandidateResolution.Decision.REVIEW_NEEDED,
            resolutionSource = CandidateResolution.ResolutionSource.ON_DEVICE_AI,
            merchantRaw = hint.candidate.paymentRecord.merchantRaw,
            merchantNormalized = hint.candidate.normalizedPaymentRecord.merchantNormalized,
            monthlyAmount = hint.candidate.normalizedPaymentRecord.normalizedAmount,
            billedAtLabel = hint.candidate.paymentRecord.toBilledAtLabel(),
            serviceId = selection?.serviceId,
            servicePlanId = selection?.servicePlanId,
            serviceName = selection?.serviceName,
            planName = selection?.planName,
            subscriptionConfidence = confidence,
            serviceConfidence = if (selection?.serviceId != null) confidence else null,
            planConfidence = if (selection?.servicePlanId != null) confidence else null,
            highConfidence = false,
            reasonCodes = hint.buildReasonCodes(
                modelReasonCodes = extraReasonCodes,
                fallbackReason = "AI_LOW_CONFIDENCE_REVIEW",
            ),
        )
    }

    private fun fallbackReviewResolution(
        hint: BatchLookupReviewHint,
        reasonCodes: List<String>,
    ): CandidateResolution {
        return buildReviewResolution(
            hint = hint,
            selection = hint.fallbackSelection(),
            confidence = hint.defaultFallbackConfidence(),
            extraReasonCodes = reasonCodes,
        )
    }

    private fun BatchLookupReviewHint.resolveSelection(
        requestedServiceId: Long?,
        requestedServicePlanId: Long?,
        requestedServiceName: String?,
        requestedPlanName: String?,
    ): ServiceSelection? {
        val service = candidate.serviceCatalogHints.firstOrNull { it.serviceId == requestedServiceId } ?: return null
        val resolvedServiceId = service.serviceId ?: return null
        val plan = service.plans.firstOrNull { it.servicePlanId == requestedServicePlanId } ?: return null
        val resolvedPlanId = plan.servicePlanId ?: return null
        return ServiceSelection(
            serviceId = resolvedServiceId,
            servicePlanId = resolvedPlanId,
            serviceName = requestedServiceName?.takeIf(String::isNotBlank) ?: service.name,
            planName = requestedPlanName?.takeIf(String::isNotBlank) ?: plan.planName,
        )
    }

    private fun BatchLookupReviewHint.fallbackSelection(): ServiceSelection? {
        val fallbackServiceId = serviceId ?: candidate.primaryHintServiceId()
        val service = candidate.serviceCatalogHints.firstOrNull { it.serviceId == fallbackServiceId }
            ?: candidate.serviceCatalogHints.firstOrNull()
            ?: return null
        val resolvedServiceId = service.serviceId ?: return null
        val plan = service.plans.firstOrNull { it.servicePlanId == servicePlanId }
            ?: service.plans.firstOrNull { it.servicePlanId == candidate.primaryHintServicePlanId() }
            ?: service.plans.minByOrNull { plan ->
                abs(plan.monthlyPrice - candidate.normalizedPaymentRecord.normalizedAmount)
            }
        return ServiceSelection(
            serviceId = resolvedServiceId,
            servicePlanId = plan?.servicePlanId,
            serviceName = serviceName?.takeIf(String::isNotBlank) ?: candidate.primaryHintServiceName() ?: service.name,
            planName = plan?.planName,
        )
    }

    private fun BatchLookupReviewHint.defaultFallbackConfidence(): Float {
        return when {
            matched && (hitCount ?: 0) >= 2 -> 0.72f
            matched -> 0.63f
            (serviceId ?: candidate.primaryHintServiceId()) != null -> 0.48f
            else -> 0.34f
        }
    }

    private fun BatchLookupReviewHint.buildReasonCodes(
        modelReasonCodes: List<String>,
        fallbackReason: String,
    ): List<String> {
        return buildList {
            addAll(modelReasonCodes.map(String::trim).filter(String::isNotBlank))
            if (matched) {
                add("BATCH_LOOKUP_SUGGESTION")
                hitCount?.let { count -> add("BATCH_LOOKUP_HIT_COUNT_$count") }
            } else {
                add("UNRESOLVED_AFTER_BATCH_LOOKUP")
            }
            if (candidate.primaryHintServiceId() != null) {
                add("CATALOG_HINT_AVAILABLE")
            }
            add(fallbackReason)
        }.distinct()
    }

    private fun Float?.normalizedConfidence(defaultValue: Float = 0.0f): Float {
        return (this ?: defaultValue).coerceIn(0.0f, 1.0f)
    }

    private data class ServiceSelection(
        val serviceId: Long,
        val servicePlanId: Long? = null,
        val serviceName: String,
        val planName: String? = null,
    )

    /**
     * XGBoost 추론 결과에 후처리 규칙을 적용해 최종 decision 문자열을 반환한다.
     *
     * Rule 1 — offlinePenaltyScore < 0f → CONFIRMED_NON_SUBSCRIPTION
     *   근거: -1.35 패널티는 카페·편의점·택시·병원 등 명확한 오프라인 결제 신호.
     *         모델이 piches 누락으로 이 신호를 받지 못하는 버그를 후처리로 보완.
     *
     * Rule 3 — catalog_alias_score=0.95 이지만 실제 구독 근거가 전혀 없는 케이스
     *   근거: bundleHint(1.05)가 ruleScores에 존재하면 XGBoost f5(catalog_alias_score)=0.95로
     *         클램핑되지만, f6(catalog_hint_count)=serviceCatalogHints.size 는 0으로 남는
     *         피처 불일치가 발생한다. 이 조합에서 price/usage/batch 근거도 없으면
     *         어떤 구독 서비스인지 특정 불가 → 비구독 확정.
     *         isPayGateway / isGenericGateway 케이스는 정상 REVIEW_NEEDED이므로 제외.
     *
     * Rule 2 — confidence < 0.65f → REVIEW_NEEDED
     *   근거: 모델 확신도가 낮으면 사용자 수동 확인에 위임하는 것이 오탐보다 안전.
     */
    private fun applyPostFilter(
        hint: BatchLookupReviewHint,
        aiDecision: com.ssafy.e106.ai.XgboostEngine.XgbPrediction,
    ): String {
        val merchant = hint.candidate.normalizedPaymentRecord.merchantNormalized
        val score = hint.offlinePenaltyScore
        if (score < 0f) {
            Log.d(TAG_RESOLVER, "후처리 Rule1: offlinePenalty=$score → CONFIRMED_NON_SUBSCRIPTION [$merchant]")
            return CONFIRMED_NON_SUBSCRIPTION
        }

        // Rule 3: alias=0.95 but hint=0, 구독 근거 없음 → CONFIRMED_NON_SUBSCRIPTION
        val ruleScores = hint.candidate.ruleScores
        val catalogAliasScoreHigh = (ruleScores["serviceHint"] ?: ruleScores["bundleHint"] ?: 0.0)
            .toFloat().coerceAtMost(0.95f) >= 0.95f
        val catalogHintEmpty = hint.candidate.serviceCatalogHints.isEmpty()
        val noPlanPriceHint = (ruleScores["planPriceHint"] ?: 0.0) == 0.0
        val noUsage30d = hint.candidate.usageSignals.none { it.usage30dMs > 0L }
        if (catalogAliasScoreHigh &&
            catalogHintEmpty &&
            noPlanPriceHint &&
            noUsage30d &&
            !hint.matched &&
            PAY_GATEWAY_TOKENS.none { tok -> merchant.contains(tok) } &&
            GENERIC_GATEWAY_TOKENS.none { tok -> merchant.contains(tok) }) {
            Log.d(TAG_RESOLVER,
                "후처리 Rule3: alias=0.95 but hint=0, 구독 근거 없음 → CONFIRMED_NON_SUBSCRIPTION [$merchant]")
            return CONFIRMED_NON_SUBSCRIPTION
        }

        val conf = aiDecision.confidence
        if (conf < 0.65f) {
            Log.d(TAG_RESOLVER, "후처리 Rule2: confidence=$conf < 0.65 → REVIEW_NEEDED [$merchant]")
            return REVIEW_NEEDED
        }
        return aiDecision.decision
    }

    private companion object {
        const val TAG_RESOLVER = "AEKKIM_AI_RESOLVER"
        // 기존 TAG 유지 (레거시 함수 내부 로그용)
        const val TAG = "OnDeviceAiResolver"
        const val CONFIRMED_SUBSCRIPTION = "CONFIRMED_SUBSCRIPTION"
        const val CONFIRMED_NON_SUBSCRIPTION = "CONFIRMED_NON_SUBSCRIPTION"
        const val REVIEW_NEEDED = "REVIEW_NEEDED"

        // Rule 3 후처리 제외 토큰 — XgboostEngine 학습 피처와 동일 기준
        // isPayGateway 해당 가맹점은 REVIEW_NEEDED 유지 (택시·간편결제 등 정상 케이스 포함)
        val PAY_GATEWAY_TOKENS = setOf("네이버페이", "토스페이", "KB국민카드", "카카오페", "비자해외승인대금출금")
        // isGenericGateway 해당 가맹점은 REVIEW_NEEDED 유지 (애플/구글 정기결제 정상 케이스 포함)
        val GENERIC_GATEWAY_TOKENS = setOf("APPLE", "GOOGLE", "BILL")
    }
}
