package com.ssafy.e106.data.repository

internal object PromotionRecommendationGroupingPolicy {
    const val temporaryRuleNote: String =
        "[temporary] The recommendations API does not return a group field yet. " +
            "CARD_BENEFIT items are treated as Conditional, and BUNDLE/PROMO items " +
            "are treated as SubscriptionList until the backend provides explicit grouping."

    fun classify(promotionType: PromotionType): PromotionRecommendationGroupType {
        return when (promotionType) {
            PromotionType.CardBenefit -> PromotionRecommendationGroupType.Conditional
            PromotionType.Bundle,
            PromotionType.Promo,
            -> PromotionRecommendationGroupType.SubscriptionList
        }
    }

    fun buildConditionDescription(
        groupType: PromotionRecommendationGroupType,
        promotionType: PromotionType,
    ): String? {
        if (groupType != PromotionRecommendationGroupType.Conditional) return null

        return when (promotionType) {
            PromotionType.CardBenefit -> CARD_BENEFIT_CONDITION_DESCRIPTION
            PromotionType.Bundle,
            PromotionType.Promo,
            -> CONDITIONAL_PROMOTION_DESCRIPTION
        }
    }

    private const val CARD_BENEFIT_CONDITION_DESCRIPTION =
        "카드 실적 또는 제휴 조건 충족 시 적용 가능한 혜택입니다."

    private const val CONDITIONAL_PROMOTION_DESCRIPTION =
        "추가 조건 충족 시 적용 가능한 혜택입니다."
}
