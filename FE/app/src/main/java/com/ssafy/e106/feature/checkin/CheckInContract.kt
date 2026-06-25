package com.ssafy.e106.feature.checkin

/**
 * SCR-007 체크인 응답 화면 Contract.
 *
 * 응답값 정본: GOOD, BAD, UNKNOWN (SRS v1.9 FR-CHECK-002)
 */

// ── Response enum ─────────────────────────────────────────────

enum class CheckInResponse(val label: String, val emoji: String) {
    GOOD("잘 쓰고 있어요", "\uD83D\uDE0A"),
    BAD("거의 안 썼어요", "\uD83D\uDE05"),
    UNKNOWN("잘 모르겠어요", "\uD83E\uDD14"),
}

data class CheckInSubmitError(
    val message: String,
    val retryResponse: CheckInResponse? = null,
    val canRetry: Boolean = true,
)

// ── UiState ───────────────────────────────────────────────────

data class CheckInUiState(
    val isLoading: Boolean = true,
    val serviceName: String = "",
    val logoUrl: String? = null,
    /** 현재 저장 요청 중인 응답. null이면 미선택 상태. */
    val submittingResponse: CheckInResponse? = null,
    val error: String? = null,
    val submitError: CheckInSubmitError? = null,
)

// ── UiEffect ──────────────────────────────────────────────────

sealed interface CheckInUiEffect {
    data class ShowToast(val message: String) : CheckInUiEffect
    data object NavigateBack : CheckInUiEffect
    data class NavigateToFollowUp(val subscriptionId: Long) : CheckInUiEffect
}

// ── Intent ────────────────────────────────────────────────────

sealed interface CheckInIntent {
    data class Load(val subscriptionId: Long) : CheckInIntent
    data object RetryLoad : CheckInIntent
    data object RetrySubmit : CheckInIntent
    data class Submit(val response: CheckInResponse) : CheckInIntent
}
