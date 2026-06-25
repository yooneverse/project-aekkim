package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class BundleListResponse(
    val bundles: List<BundleListItemResponse> = emptyList(),
)
