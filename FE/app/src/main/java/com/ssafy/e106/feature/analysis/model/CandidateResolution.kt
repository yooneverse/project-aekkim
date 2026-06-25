package com.ssafy.e106.feature.analysis.model

data class CandidateResolution(
    val reviewId: Long,
    val decision: Decision,
    val resolutionSource: ResolutionSource,
    val merchantRaw: String,
    val merchantNormalized: String? = null,
    val monthlyAmount: Int,
    val billedAtLabel: String,
    val subscriptionType: SubscriptionType = SubscriptionType.SINGLE,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val serviceName: String? = null,
    val planName: String? = null,
    val bundleCode: String? = null,
    val bundleName: String? = null,
    val coveredServiceIds: Set<Long> = emptySet(),
    val coveredServiceNames: List<String> = emptyList(),
    val subscriptionConfidence: Float? = null,
    val serviceConfidence: Float? = null,
    val planConfidence: Float? = null,
    val highConfidence: Boolean = false,
    val reasonCodes: List<String> = emptyList(),
) {
    fun isConfirmedHighConfidence(threshold: Float = DEFAULT_HIGH_CONFIDENCE_THRESHOLD): Boolean {
        return decision == Decision.CONFIRMED_SUBSCRIPTION &&
            (highConfidence || (subscriptionConfidence ?: 0f) >= threshold)
    }

    fun requiresUserReview(): Boolean {
        return decision == Decision.REVIEW_NEEDED
    }

    fun isExcludedLocally(): Boolean {
        return decision == Decision.EXCLUDED_LOCAL
    }

    enum class Decision {
        CONFIRMED_SUBSCRIPTION,
        CONFIRMED_NON_SUBSCRIPTION,
        REVIEW_NEEDED,
        EXCLUDED_LOCAL,
    }

    enum class ResolutionSource {
        RULE_BASED,
        BATCH_LOOKUP,
        ON_DEVICE_AI,
        USER_CONFIRMED,
        LOCAL_OVERRIDE,
        LEGACY_SEED,
        UNKNOWN,
    }

    enum class SubscriptionType {
        SINGLE,
        BUNDLE,
        UNKNOWN,
    }

    companion object {
        const val DEFAULT_HIGH_CONFIDENCE_THRESHOLD = 0.85f
    }
}
