package com.ssafy.e106.data.repository

internal object PromotionPricingPolicy {
    private const val MONTHS_PER_YEAR = 12

    fun toPriceData(
        originalPrice: Int?,
        discountPrice: Int?,
    ): PromotionPriceData {
        return PromotionPriceData(
            originalPrice = originalPrice,
            discountPrice = discountPrice,
            monthlySavedAmount = calculateMonthlySavedAmount(
                originalPrice = originalPrice,
                discountPrice = discountPrice,
            ),
        )
    }

    fun calculateMonthlySavedAmount(
        originalPrice: Int?,
        discountPrice: Int?,
    ): Int {
        if (originalPrice == null || discountPrice == null) return 0
        return (originalPrice - discountPrice).coerceAtLeast(0)
    }

    fun calculateYearlySavedAmount(monthlySavedAmount: Int): Int {
        return monthlySavedAmount.coerceAtLeast(0) * MONTHS_PER_YEAR
    }
}
