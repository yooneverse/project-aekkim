package com.ssafy.e106.feature.checkin

/**
 * SCR-007-2 체크인 후속 권유 화면 Contract.
 *
 * 연속 BAD 응답 사용자에게 해지 또는 유지 선택지를 제시한다 (FR-CHECK-003).
 */

// ── UiState ───────────────────────────────────────────────────

data class CheckInFollowUpUiState(
    val isLoading: Boolean = true,
    val serviceName: String = "",
    val monthlyPrice: Int = 0,
    val error: String? = null,
    /** CTA 처리 중 중복 탭 방지 */
    val isNavigating: Boolean = false,
)

// ── UiEffect ──────────────────────────────────────────────────

sealed interface CheckInFollowUpUiEffect {
    data class NavigateToCancelGuide(val subscriptionId: String) : CheckInFollowUpUiEffect
    data object NavigateBack : CheckInFollowUpUiEffect
}

// ── Intent ────────────────────────────────────────────────────

sealed interface CheckInFollowUpIntent {
    data class Load(val subscriptionId: Long) : CheckInFollowUpIntent
    data object RetryLoad : CheckInFollowUpIntent
    /** Primary CTA: 아끼러 가기 → SCR-008 해지 지원 */
    data object GoToCancelGuide : CheckInFollowUpIntent
    /** Secondary CTA: 조금 더 유지할게요 → 대시보드 복귀 */
    data object KeepSubscription : CheckInFollowUpIntent
}
