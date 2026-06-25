package com.ssafy.e106.data.repository

internal data class PendingReviewMerchantGroup(
    val merchantName: String,
    val items: List<PendingReviewItem>,
)

internal fun List<PendingReviewItem>.toPendingReviewMerchantGroups(): List<PendingReviewMerchantGroup> {
    return groupBy(PendingReviewItem::merchantName)
        .map { (merchantName, items) ->
            PendingReviewMerchantGroup(
                merchantName = merchantName,
                items = items,
            )
        }
        .sortedByDescending { group -> group.items.size }
}

internal fun List<PendingReviewItem>.pendingReviewMerchantGroupCount(): Int {
    return toPendingReviewMerchantGroups().size
}
