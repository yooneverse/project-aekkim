package com.ssafy.e106.feature.cancelguide

data class CancelGuideUiState(
    val serviceName: String = "",
    val logoUrl: String? = null,
    val cancelGuideUrl: String? = null,
    val cancelGuideUrlDisplayText: String? = null,
    val customerServicePhone: String? = null,
    val contactEmail: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val hasCancelGuideUrl: Boolean
        get() = !cancelGuideUrl.isNullOrBlank()

    val hasCustomerServicePhone: Boolean
        get() = !customerServicePhone.isNullOrBlank()

    val hasContactEmail: Boolean
        get() = !contactEmail.isNullOrBlank()
}

sealed class CancelGuideUiEffect {
    data class OpenUri(val uri: String) : CancelGuideUiEffect()
    data class ShowToast(val message: String) : CancelGuideUiEffect()
}

sealed class CancelGuideIntent {
    data class Load(val subscriptionId: Long) : CancelGuideIntent()
    data object RetryLoad : CancelGuideIntent()
    data object OpenCancelGuideLink : CancelGuideIntent()
    data object CallCustomerService : CancelGuideIntent()
    data object SendContactEmail : CancelGuideIntent()
}
