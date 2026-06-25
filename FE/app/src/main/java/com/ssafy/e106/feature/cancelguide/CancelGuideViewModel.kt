package com.ssafy.e106.feature.cancelguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

@HiltViewModel
class CancelGuideViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CancelGuideUiState())
    val uiState: StateFlow<CancelGuideUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CancelGuideUiEffect>(replay = 0)
    val uiEffect: SharedFlow<CancelGuideUiEffect> = _uiEffect.asSharedFlow()

    private var loadedSubscriptionId: Long = -1L

    fun onIntent(intent: CancelGuideIntent) {
        when (intent) {
            is CancelGuideIntent.Load -> load(intent.subscriptionId)
            CancelGuideIntent.RetryLoad -> load(loadedSubscriptionId)
            CancelGuideIntent.OpenCancelGuideLink -> openCancelGuideLink()
            CancelGuideIntent.CallCustomerService -> callCustomerService()
            CancelGuideIntent.SendContactEmail -> sendContactEmail()
        }
    }

    private fun load(subscriptionId: Long) {
        loadedSubscriptionId = subscriptionId
        if (subscriptionId <= 0L) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = INVALID_SUBSCRIPTION_MESSAGE,
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            when (val result = subscriptionRepository.getSubscriptionDetail(subscriptionId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            serviceName = result.data.serviceName,
                            logoUrl = result.data.logoUrl,
                            cancelGuideUrl = result.data.cancelGuideUrl.normalizedOrNull(),
                            cancelGuideUrlDisplayText = result.data.cancelGuideUrl.toGuideDisplayText(),
                            customerServicePhone = result.data.customerServicePhone.normalizedOrNull(),
                            contactEmail = result.data.contactEmail.normalizedOrNull(),
                            isLoading = false,
                            error = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun openCancelGuideLink() {
        val url = _uiState.value.cancelGuideUrl
        if (url.isNullOrBlank()) {
            emitToast(MISSING_GUIDE_MESSAGE)
            return
        }
        emitUri(url)
    }

    private fun callCustomerService() {
        val phone = _uiState.value.customerServicePhone
        if (phone.isNullOrBlank()) {
            emitToast(MISSING_PHONE_MESSAGE)
            return
        }
        emitUri("tel:${phone.filterNot(Char::isWhitespace)}")
    }

    private fun sendContactEmail() {
        val email = _uiState.value.contactEmail
        if (email.isNullOrBlank()) {
            emitToast(MISSING_EMAIL_MESSAGE)
            return
        }
        emitUri("mailto:${email.trim()}")
    }

    private fun emitUri(uri: String) {
        viewModelScope.launch {
            _uiEffect.emit(CancelGuideUiEffect.OpenUri(uri))
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _uiEffect.emit(CancelGuideUiEffect.ShowToast(message))
        }
    }

    private fun String?.normalizedOrNull(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun String?.toGuideDisplayText(): String? {
        val normalized = normalizedOrNull() ?: return null
        return runCatching {
            val uri = URI(normalized)
            val host = uri.host?.removePrefix("www.")
            val path = uri.path?.takeIf { it.isNotBlank() && it != "/" }
            when {
                host != null && path != null -> "$host$path"
                host != null -> host
                else -> normalized
            }
        }.getOrElse { normalized }
    }

    private companion object {
        const val INVALID_SUBSCRIPTION_MESSAGE = "구독 정보를 찾을 수 없어요."
        const val MISSING_GUIDE_MESSAGE = "해지 가이드 링크가 아직 등록되지 않았어요."
        const val MISSING_PHONE_MESSAGE = "고객센터 전화번호가 아직 등록되지 않았어요."
        const val MISSING_EMAIL_MESSAGE = "문의 메일 주소가 아직 등록되지 않았어요."
    }
}
