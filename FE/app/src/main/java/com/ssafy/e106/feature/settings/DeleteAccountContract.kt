package com.ssafy.e106.feature.settings

enum class DeleteAccountStep {
    Warning,
    Confirm,
}

data class DeleteAccountUiState(
    val step: DeleteAccountStep = DeleteAccountStep.Warning,
    val finalCheckAgreed: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface DeleteAccountIntent {
    data object NextStep : DeleteAccountIntent
    data object PreviousStep : DeleteAccountIntent
    data class ToggleFinalCheck(val agreed: Boolean) : DeleteAccountIntent
    data object ConfirmDelete : DeleteAccountIntent
}

sealed interface DeleteAccountUiEffect {
    data class ShowToast(val message: String) : DeleteAccountUiEffect
    data class NavigateToOnboarding(val message: String) : DeleteAccountUiEffect
    data object NavigateBack : DeleteAccountUiEffect
}
