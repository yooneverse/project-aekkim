package com.ssafy.e106.feature.analysis.model

data class AnalysisSessionResult(
    val sessionId: String? = null,
    val analyzedAtEpochMs: Long,
    val source: AnalysisSource = AnalysisSource.UNKNOWN,
    val serviceCatalog: ServiceCatalog? = null,
    val usageItems: List<AppUsageSnapshot> = emptyList(),
    val candidates: List<AnalysisCandidate> = emptyList(),
    val resolutions: List<CandidateResolution> = emptyList(),
) {
    val confirmedHighConfidence: List<CandidateResolution>
        get() = resolutions.filter { resolution -> resolution.isConfirmedHighConfidence() }

    val pendingReviewSummary: PendingReviewSummary
        get() = PendingReviewSummary(
            items = resolutions.filter { resolution -> resolution.requiresUserReview() },
        )

    val excludedLocal: List<CandidateResolution>
        get() = resolutions.filter { resolution -> resolution.isExcludedLocally() }

    fun findCandidate(reviewId: Long): AnalysisCandidate? {
        return candidates.firstOrNull { candidate -> candidate.reviewId == reviewId }
    }

    data class PendingReviewSummary(
        val items: List<CandidateResolution> = emptyList(),
    ) {
        val count: Int
            get() = items.size

        val suggestedServiceNames: Set<String>
            get() = items.mapNotNull { item -> item.serviceName }.toSet()

        val isEmpty: Boolean
            get() = items.isEmpty()
    }

    enum class AnalysisSource {
        UNKNOWN,
        RULE_BASED,
        HYBRID,
        ON_DEVICE_AI,
        USER_CONFIRMED,
        LEGACY_SEED,
    }
}
