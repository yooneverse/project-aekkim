package com.ssafy.e106.data.repository

import com.ssafy.e106.feature.analysis.model.AnalysisSessionResult
import com.ssafy.e106.feature.analysis.model.CandidateResolution
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PendingReviewSeedState {
    data object Unavailable : PendingReviewSeedState

    data class Available(
        val candidates: List<PendingReviewCandidate>,
    ) : PendingReviewSeedState
}

@Singleton
class AnalysisHandoffRepository @Inject constructor() {

    @Volatile
    private var analysisSessionResult: AnalysisSessionResult? = null

    @Volatile
    private var pendingReviewSeedState: PendingReviewSeedState = PendingReviewSeedState.Unavailable

    fun storeAnalysisSessionResult(result: AnalysisSessionResult?) {
        if (result == null) {
            clearAnalysisSessionResult()
            return
        }

        analysisSessionResult = result
        pendingReviewSeedState = result.toPendingReviewSeedState()
    }

    fun peekAnalysisSessionResult(): AnalysisSessionResult? {
        return analysisSessionResult
    }

    fun consumeAnalysisSessionResult(): AnalysisSessionResult? {
        val currentResult = analysisSessionResult
        clearAnalysisSessionResult()
        return currentResult
    }

    fun clearAnalysisSessionResult() {
        analysisSessionResult = null
        pendingReviewSeedState = PendingReviewSeedState.Unavailable
    }

    fun storePendingReviewSeedState(state: PendingReviewSeedState) {
        pendingReviewSeedState = state
        analysisSessionResult = state.toAnalysisSessionResult()
    }

    fun storePendingReviewCandidates(candidates: List<PendingReviewCandidate>) {
        storePendingReviewSeedState(PendingReviewSeedState.Available(candidates))
    }

    fun markPendingReviewCandidatesUnavailable() {
        clearAnalysisSessionResult()
    }

    fun consumePendingReviewSeedState(): PendingReviewSeedState {
        val currentState = pendingReviewSeedState
        pendingReviewSeedState = PendingReviewSeedState.Unavailable
        return currentState
    }

    private fun AnalysisSessionResult.toPendingReviewSeedState(): PendingReviewSeedState {
        return PendingReviewSeedState.Available(
            candidates = pendingReviewSummary.items.map { resolution ->
                resolution.toPendingReviewCandidate()
            },
        )
    }

    private fun PendingReviewSeedState.toAnalysisSessionResult(): AnalysisSessionResult? {
        return when (this) {
            PendingReviewSeedState.Unavailable -> null
            is PendingReviewSeedState.Available -> AnalysisSessionResult(
                analyzedAtEpochMs = System.currentTimeMillis(),
                source = AnalysisSessionResult.AnalysisSource.LEGACY_SEED,
                resolutions = candidates.map { candidate -> candidate.toPendingReviewResolution() },
            )
        }
    }

    private fun CandidateResolution.toPendingReviewCandidate(): PendingReviewCandidate {
        return PendingReviewCandidate(
            reviewId = reviewId,
            merchantRaw = merchantRaw,
            monthlyAmount = monthlyAmount,
            billedAtLabel = billedAtLabel,
        )
    }

    private fun PendingReviewCandidate.toPendingReviewResolution(): CandidateResolution {
        return CandidateResolution(
            reviewId = reviewId,
            decision = CandidateResolution.Decision.REVIEW_NEEDED,
            resolutionSource = CandidateResolution.ResolutionSource.LEGACY_SEED,
            merchantRaw = merchantRaw,
            monthlyAmount = monthlyAmount,
            billedAtLabel = billedAtLabel,
        )
    }
}
