package com.ssafy.e106.ai

import android.util.Log
import com.ssafy.e106.data.repository.BatchLookupReviewHint
import kotlin.math.abs
import kotlin.math.exp
import org.json.JSONObject
import java.io.File

/**
 * [AI 모델 교체] FastText + XGBoost 온디바이스 파이프라인 지원을 위해 신규 생성 (2026-03-25)
 *
 * 역할: xgb_model_android.json을 순수 Kotlin으로 직접 파싱해 트리 앙상블 추론을 수행한다.
 *       xgboost4j-android 아티팩트는 Maven Central 미존재, xgboost4j JVM은 Android ARM 미지원.
 *       XGBoost 네이티브 JSON 포맷(learner.gradient_booster.model.trees)을 직접 읽는다.
 *
 * 레이블 매핑 (label_map.json 기준):
 *   0 → CONFIRMED_NON_SUBSCRIPTION
 *   1 → CONFIRMED_SUBSCRIPTION
 *   2 → REVIEW_NEEDED
 *
 * 피처 순서 (feature_map.json f0~f20, 학습 시 FEATURE_COLS와 동일):
 *   f0:  fasttext_sub_prob
 *   f1:  has_subscription_token
 *   f2:  has_english_chars
 *   f3:  is_generic_gateway
 *   f4:  is_pay_gateway
 *   f5:  catalog_alias_score
 *   f6:  catalog_hint_count
 *   f7:  generic_subscription_token_score
 *   f8:  amount
 *   f9:  min_diff_to_catalog_price
 *   f10: plan_price_hint_score
 *   f11: is_installment
 *   f12: offline_penalty_score
 *   f13: repeat_merchant_score
 *   f14: recurrence_is_monthly
 *   f15: candidate_score_total
 *   f16: usage_7d_ms
 *   f17: usage_30d_ms
 *   f18: usage_permission_granted
 *   f19: batch_lookup_matched
 *   f20: batch_lookup_hit_count
 */
class XgboostEngine {

    private companion object {
        private const val TAG_XGB = "AEKKIM_AI_XGB"
        private const val FEATURE_COUNT = 21

        // 레이블 인덱스 → 문자열 (label_map.json 기준)
        private val LABEL_MAP = mapOf(
            0 to "CONFIRMED_NON_SUBSCRIPTION",
            1 to "CONFIRMED_SUBSCRIPTION",
            2 to "REVIEW_NEEDED",
        )

        // hasSubscriptionToken 판단 기준 토큰
        private val SUBSCRIPTION_TOKENS = setOf("정기결제", "정기", "SUBSCRIPTION", "BILL", "구독")

        // isGenericGateway 판단 기준 토큰
        private val GENERIC_GATEWAY_TOKENS = setOf("APPLE", "GOOGLE", "BILL")

        // isPayGateway 판단 기준 토큰 (실제 가맹점을 숨기는 간접 결제망)
        private val PAY_GATEWAY_TOKENS = setOf("네이버페이", "토스페이", "KB국민카드", "카카오페", "비자해외승인대금출금")
    }

    // ── 파일 내부 트리 자료구조 ──────────────────────────────────────

    private data class XgbNode(
        val nodeId: Int,
        val splitFeatureIdx: Int?,   // null이면 leaf
        val splitCondition: Float?,
        val leftChild: Int?,
        val rightChild: Int?,
        val leafValue: Float?,
    )

    private data class XgbTree(val nodes: Map<Int, XgbNode>)

    data class XgbPrediction(val decision: String, val confidence: Float)

    // ── 런타임 상태 ──────────────────────────────────────────────────

    private var initialized = false
    private var trees: List<XgbTree> = emptyList()
    private var numClass: Int = 3
    private var baseScore: Float = 0.5f
    // resolve()가 단일 코루틴 컨텍스트(Dispatchers.Default)에서 순차 실행되므로
    // featureBuffer 재사용이 스레드 안전하다.
    private val featureBuffer = FloatArray(FEATURE_COUNT)

    // ── 공개 API ─────────────────────────────────────────────────────

    /**
     * XGBoost JSON 모델을 로드한다.
     * XGBoost 네이티브 JSON 포맷: learner.gradient_booster.model.trees 배열을 파싱.
     */
    fun initialize(modelPath: String) {
        Log.i(TAG_XGB, "XGBoost JSON 모델 로드 시작: $modelPath")
        try {
            val json = JSONObject(File(modelPath).readText())
            val learner = json.getJSONObject("learner")

            // ── learner_model_param ───────────────────────────────────
            val modelParam = learner.getJSONObject("learner_model_param")
            numClass = modelParam.getString("num_class").trim().toInt()
            val baseScoreRaw = modelParam.getString("base_score").trim()
            baseScore = if (baseScoreRaw.startsWith("[")) {
                // 배열 형태: "[v1,v2,v3]" → 첫 번째 값만 사용
                baseScoreRaw
                    .removePrefix("[").removeSuffix("]")
                    .split(",")
                    .firstOrNull()
                    ?.trim()
                    ?.toFloatOrNull() ?: 0.5f
            } else {
                baseScoreRaw.toFloatOrNull() ?: 0.5f
            }

            // ── 트리 파싱 ─────────────────────────────────────────────
            val treeArray = learner
                .getJSONObject("gradient_booster")
                .getJSONObject("model")
                .getJSONArray("trees")

            val loadedTrees = mutableListOf<XgbTree>()
            for (tIdx in 0 until treeArray.length()) {
                val treeObj = treeArray.getJSONObject(tIdx)
                val leftChildren = treeObj.getJSONArray("left_children")
                val rightChildren = treeObj.getJSONArray("right_children")
                val splitIndices = treeObj.getJSONArray("split_indices")
                val splitConditions = treeObj.getJSONArray("split_conditions")
                val baseWeights = treeObj.getJSONArray("base_weights")

                val nodes = HashMap<Int, XgbNode>(leftChildren.length() * 2)
                for (nIdx in 0 until leftChildren.length()) {
                    val lc = leftChildren.getInt(nIdx)
                    if (lc == -1) {
                        // leaf 노드: leafValue = base_weights[nIdx]
                        nodes[nIdx] = XgbNode(
                            nodeId = nIdx,
                            splitFeatureIdx = null,
                            splitCondition = null,
                            leftChild = null,
                            rightChild = null,
                            leafValue = baseWeights.getDouble(nIdx).toFloat(),
                        )
                    } else {
                        // 내부 노드
                        nodes[nIdx] = XgbNode(
                            nodeId = nIdx,
                            splitFeatureIdx = splitIndices.getInt(nIdx),
                            splitCondition = splitConditions.getDouble(nIdx).toFloat(),
                            leftChild = lc,
                            rightChild = rightChildren.getInt(nIdx),
                            leafValue = null,
                        )
                    }
                }
                loadedTrees.add(XgbTree(nodes = nodes))
            }

            trees = loadedTrees
            initialized = true
            Log.i(TAG_XGB, "XGBoost JSON 로드 완료: 트리 수=${trees.size}, num_class=$numClass")
        } catch (e: Exception) {
            Log.e(TAG_XGB, "XGBoost JSON 파싱 실패", e)
            initialized = false
            throw e
        }
    }

    /**
     * 21개 피처 배열로 추론을 수행하고 XgbPrediction을 반환한다.
     * multi:softprob 목적함수 기준: 트리를 numClass씩 묶어 margin 누적 후 softmax 적용.
     */
    fun predict(features: FloatArray): XgbPrediction {
        require(features.size == FEATURE_COUNT) {
            "피처 배열 크기 오류: expected $FEATURE_COUNT, got ${features.size}"
        }
        Log.d(TAG_XGB, "XGBoost 추론 시작, 피처 수: ${features.size}")
        Log.d(
            TAG_XGB,
            "주요 피처: fasttext_sub_prob=${features[0]}, amount=${features[8]}, recurrence_is_monthly=${features[14]}",
        )

        if (!initialized) {
            Log.w(TAG_XGB, "미초기화 상태")
            return XgbPrediction(decision = "REVIEW_NEEDED", confidence = 0.0f)
        }

        return try {
            // 1. margin 배열 초기화
            val margin = FloatArray(numClass) { 0f }

            // 2. 트리를 numClass씩 묶어 라운드 단위 순회
            //    트리 인덱스 i → 클래스 i % numClass에 leaf 값 누적
            for ((tIdx, tree) in trees.withIndex()) {
                val classIdx = tIdx % numClass
                margin[classIdx] += traverseTree(tree, features)
            }

            // 3. softmax 변환
            val maxVal = margin.max()!!
            val expVals = margin.map { exp((it - maxVal).toDouble()).toFloat() }
            val sum = expVals.sum()
            val probs = expVals.map { it / sum }.toFloatArray()

            // 4. argmax → LABEL_MAP으로 decision 결정
            val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
            val confidence = probs[maxIdx]
            val decision = LABEL_MAP[maxIdx] ?: "REVIEW_NEEDED"

            Log.d(TAG_XGB, "XGBoost 추론 결과: decision=$decision, confidence=$confidence")
            XgbPrediction(decision = decision, confidence = confidence)
        } catch (e: Exception) {
            Log.e(TAG_XGB, "XGBoost 추론 실패 — REVIEW_NEEDED 반환", e)
            XgbPrediction(decision = "REVIEW_NEEDED", confidence = 0.0f)
        }
    }

    // ── 트리 탐색 내부 로직 ──────────────────────────────────────────

    private fun traverseTree(tree: XgbTree, features: FloatArray): Float {
        var node = tree.nodes[0] ?: return 0.0f
        while (true) {
            val leafVal = node.leafValue
            if (leafVal != null) return leafVal

            val featureIdx = node.splitFeatureIdx ?: return 0.0f
            val condition = node.splitCondition ?: return 0.0f
            val featureVal = if (featureIdx < features.size) features[featureIdx] else 0.0f

            val nextNodeId = if (featureVal < condition) node.leftChild else node.rightChild
            node = tree.nodes[nextNodeId ?: return 0.0f] ?: return 0.0f
        }
    }

    // ── 피처 추출 헬퍼 함수 ──────────────────────────────────────────

    /**
     * 가맹점명에 구독 전용 토큰 포함 여부.
     * 기준 토큰: '정기결제', '정기', 'SUBSCRIPTION', 'BILL', '구독'
     */
    private fun hasSubscriptionToken(text: String): Boolean =
        SUBSCRIPTION_TOKENS.any { tok -> text.contains(tok) }

    /**
     * 가맹점명에 영문 알파벳(A-Z, a-z) 포함 여부.
     */
    private fun hasEnglishChars(text: String): Boolean =
        text.any { it.isLetter() && it.code < 128 }

    /**
     * 애플·구글 등 다양한 서비스를 경유하는 범용 결제망 패턴 여부.
     * 기준 토큰: 'APPLE', 'GOOGLE', 'BILL'
     */
    private fun isGenericGateway(text: String): Boolean =
        GENERIC_GATEWAY_TOKENS.any { tok -> text.contains(tok) }

    /**
     * 실제 가맹점을 숨기는 간접 결제망 패턴 여부.
     * 기준 토큰: '네이버페이', '토스페이', 'KB국민카드', '카카오페', '비자해외승인대금출금'
     *
     * XGBoost 피처(f4) 계산에만 사용하며, 후처리 Rule에는 적용하지 않는다.
     * 카카오페이는 택시 등 일반 결제에도 광범위하게 쓰이므로 후처리 강제 적용 시
     * 오분류가 발생할 수 있다.
     */
    private fun isPayGateway(text: String): Boolean =
        PAY_GATEWAY_TOKENS.any { tok -> text.contains(tok) }

    /**
     * BatchLookupReviewHint와 FastText 확률값으로 21개 피처 FloatArray를 구성한다.
     * feature_map.json의 f0~f20 순서와 동일하게 배치한다.
     *
     * 누락 필드 기본값:
     *   min_diff_to_catalog_price 누락 → -1.0f
     *   나머지 숫자 피처 누락       → 0.0f
     */
    fun buildFeatureArray(candidate: BatchLookupReviewHint, ftProb: Float): FloatArray {
        Log.d(TAG_XGB, "피처 배열 생성: merchant='${candidate.candidate.normalizedPaymentRecord.merchantNormalized}'")

        val merchantNorm = candidate.candidate.normalizedPaymentRecord.merchantNormalized
        val ruleScores = candidate.candidate.ruleScores
        val usageSignals = candidate.candidate.usageSignals
        val serviceHints = candidate.candidate.serviceCatalogHints
        val amount = candidate.candidate.normalizedPaymentRecord.normalizedAmount
        val paymentRecord = candidate.candidate.paymentRecord

        // f0: fasttext_sub_prob
        val f0 = ftProb

        // f1: has_subscription_token — 가맹점명에 구독 전용 토큰 포함 여부
        val f1 = if (hasSubscriptionToken(merchantNorm)) 1.0f else 0.0f

        // f2: has_english_chars — 영문자 포함 여부
        val f2 = if (hasEnglishChars(merchantNorm)) 1.0f else 0.0f

        // f3: is_generic_gateway — 애플/구글 등 범용 플랫폼 게이트웨이 여부
        val f3 = if (isGenericGateway(merchantNorm)) 1.0f else 0.0f

        // f4: is_pay_gateway — PAY 계열 게이트웨이 여부
        val f4 = if (isPayGateway(merchantNorm)) 1.0f else 0.0f

        // f5: catalog_alias_score — 카탈로그 서비스 힌트 존재 시 점수 (ruleScores["serviceHint"])
        // bundleHint(1.05)가 넘어올 수 있으나 학습 데이터 범위는 0.0 또는 0.95이므로 클램핑
        val f5 = (ruleScores["serviceHint"] ?: ruleScores["bundleHint"] ?: 0.0).toFloat()
            .coerceAtMost(0.95f)

        // f6: catalog_hint_count — 서비스 카탈로그 힌트 수
        val f6 = serviceHints.size.toFloat()

        // f7: generic_subscription_token_score — ruleScores["genericSubscriptionToken"]
        val f7 = if (ruleScores.containsKey("genericSubscriptionToken")) {
            (ruleScores["genericSubscriptionToken"] ?: 0.0).toFloat()
        } else {
            Log.w(TAG_XGB, "피처 누락: generic_subscription_token_score, 기본값 사용")
            0.0f
        }

        // f8: amount — 정규화된 결제 금액
        val f8 = amount.toFloat()

        // f9: min_diff_to_catalog_price — 카탈로그 플랜 금액과의 최소 차이
        val f9 = if (serviceHints.isNotEmpty()) {
            serviceHints
                .flatMap { svc -> svc.plans }
                .minOfOrNull { plan -> abs(plan.monthlyPrice - amount).toFloat() }
                ?: run {
                    Log.w(TAG_XGB, "피처 누락: min_diff_to_catalog_price (플랜 없음), 기본값 -1.0 사용")
                    -1.0f
                }
        } else {
            Log.w(TAG_XGB, "피처 누락: min_diff_to_catalog_price (서비스 힌트 없음), 기본값 -1.0 사용")
            -1.0f
        }

        // f10: plan_price_hint_score — ruleScores["planPriceHint"]
        val f10 = if (ruleScores.containsKey("planPriceHint")) {
            (ruleScores["planPriceHint"] ?: 0.0).toFloat()
        } else {
            Log.w(TAG_XGB, "피처 누락: plan_price_hint_score, 기본값 사용")
            0.0f
        }

        // f11: is_installment — 할부 결제 여부 (candidates로 도달한 경우 실질적으로 0)
        val f11 = if ((paymentRecord.installmentMonths ?: 1) > 1) 1.0f else 0.0f

        // f12: offline_penalty_score — BatchLookupReviewHint.offlinePenaltyScore로 직접 전달
        val f12 = candidate.offlinePenaltyScore

        // f14: repeat_merchant_score — ruleScores["repeatMerchant"]
        val f14 = if (ruleScores.containsKey("repeatMerchant")) {
            (ruleScores["repeatMerchant"] ?: 0.0).toFloat()
        } else {
            Log.w(TAG_XGB, "피처 누락: repeat_merchant_score, 기본값 사용")
            0.0f
        }

        // f19: recurrence_is_monthly — 반복 패턴이 월간인지 여부
        val f19 = if (candidate.candidate.recurrenceLabel == "MONTHLY") 1.0f else 0.0f

        // f20: candidate_score_total — ruleScores["candidateScore"]
        val f20 = if (ruleScores.containsKey("candidateScore")) {
            (ruleScores["candidateScore"] ?: 0.0).toFloat()
        } else {
            Log.w(TAG_XGB, "피처 누락: candidate_score_total, 기본값 사용")
            0.0f
        }

        // f21: usage_7d_ms — 7일 사용량 최대값 (ms)
        val f21 = usageSignals.maxOfOrNull { it.usage7dMs }?.toFloat() ?: 0.0f

        // f22: usage_30d_ms — 30일 사용량 최대값 (ms)
        val f22 = usageSignals.maxOfOrNull { it.usage30dMs }?.toFloat() ?: 0.0f

        // f23: usage_permission_granted — 사용 통계 권한 허용 여부
        val f23 = if (usageSignals.any { it.permissionGranted }) 1.0f else 0.0f

        // f24: batch_lookup_matched — 배치 룩업 매칭 여부
        val f24 = if (candidate.matched) 1.0f else 0.0f

        // f25: batch_lookup_hit_count — 배치 룩업 히트 수
        val f25 = candidate.hitCount?.toFloat() ?: 0.0f

        featureBuffer[0]  = f0   // fasttext_sub_prob
        featureBuffer[1]  = f1   // has_subscription_token
        featureBuffer[2]  = f2   // has_english_chars
        featureBuffer[3]  = f3   // is_generic_gateway
        featureBuffer[4]  = f4   // is_pay_gateway
        featureBuffer[5]  = f5   // catalog_alias_score
        featureBuffer[6]  = f6   // catalog_hint_count
        featureBuffer[7]  = f7   // generic_subscription_token_score
        featureBuffer[8]  = f8   // amount
        featureBuffer[9]  = f9   // min_diff_to_catalog_price
        featureBuffer[10] = f10  // plan_price_hint_score
        featureBuffer[11] = f11  // is_installment
        featureBuffer[12] = f12  // offline_penalty_score
        featureBuffer[13] = f14  // repeat_merchant_score       ← 기존 f14 (f13 제거로 한 칸 앞으로)
        featureBuffer[14] = f19  // recurrence_is_monthly       ← 기존 f19
        featureBuffer[15] = f20  // candidate_score_total       ← 기존 f20
        featureBuffer[16] = f21  // usage_7d_ms                 ← 기존 f21
        featureBuffer[17] = f22  // usage_30d_ms                ← 기존 f22
        featureBuffer[18] = f23  // usage_permission_granted    ← 기존 f23
        featureBuffer[19] = f24  // batch_lookup_matched        ← 기존 f24
        featureBuffer[20] = f25  // batch_lookup_hit_count      ← 기존 f25
        val features = featureBuffer
        Log.d(TAG_XGB, """
            피처 전체 [merchant='$merchantNorm']:
            f0  fasttext_sub_prob=$f0
            f1  has_subscription_token=$f1
            f2  has_english_chars=$f2
            f3  is_generic_gateway=$f3
            f4  is_pay_gateway=$f4
            f5  catalog_alias_score=$f5
            f6  catalog_hint_count=$f6
            f7  generic_subscription_token_score=$f7
            f8  amount=$f8
            f9  min_diff_to_catalog_price=$f9
            f10 plan_price_hint_score=$f10
            f11 is_installment=$f11
            f12 offline_penalty_score=$f12
            f13 repeat_merchant_score=$f14
            f14 recurrence_is_monthly=$f19
            f15 candidate_score_total=$f20
            f16 usage_7d_ms=$f21
            f17 usage_30d_ms=$f22
            f18 usage_permission_granted=$f23
            f19 batch_lookup_matched=$f24
            f20 batch_lookup_hit_count=$f25
        """.trimIndent())
        return features
    }
}
