package com.ssafy.e106.feature.subscriptionconfirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.AnalysisHandoffRepository
import com.ssafy.e106.data.repository.PendingReviewItem
import com.ssafy.e106.data.repository.SubscriptionRepository
import com.ssafy.e106.data.repository.SubscriptionSyncEvent
import com.ssafy.e106.data.repository.UserRepository
import com.ssafy.e106.data.repository.pendingReviewMerchantGroupCount
import com.ssafy.e106.feature.analysis.model.AnalysisSessionResult
import com.ssafy.e106.feature.analysis.model.CandidateResolution
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class DetectedSubscriptionSeed(
    val paymentDate: LocalDate,
    val item: DetectedSubscriptionItem,
)

@HiltViewModel
class SubscriptionConfirmViewModel @Inject constructor(
    private val analysisHandoffRepository: AnalysisHandoffRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionConfirmUiState())
    val uiState: StateFlow<SubscriptionConfirmUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SubscriptionConfirmUiEffect>(replay = 0)
    val uiEffect: SharedFlow<SubscriptionConfirmUiEffect> = _uiEffect.asSharedFlow()

    private var hasLoaded = false

    init {
        observePendingReviewChanges()
    }

    fun onIntent(intent: SubscriptionConfirmIntent) {
        when (intent) {
            SubscriptionConfirmIntent.Load -> load(forceRefresh = false)
            SubscriptionConfirmIntent.RetryLoad -> load(forceRefresh = true)
            is SubscriptionConfirmIntent.ExcludeDetectedSubscription -> excludeDetectedSubscription(intent.reviewId)
            SubscriptionConfirmIntent.OpenPendingReview -> openPendingReview()
            SubscriptionConfirmIntent.ConfirmDetectedSubscriptions -> confirmDetectedSubscriptions()
        }
    }

    private fun load(forceRefresh: Boolean) {
        if (hasLoaded && !forceRefresh) return

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                error = null,
                isCompletingLocalSessionOnly = false,
            )
        }

        viewModelScope.launch {
            val sessionResult = analysisHandoffRepository.peekAnalysisSessionResult()
            if (sessionResult == null) {
                _uiState.update { state ->
                    state.copy(
                        nickname = state.nickname.ifBlank { FALLBACK_NICKNAME },
                        detectedSubscriptions = emptyList(),
                        pendingReviewSummary = null,
                        isLoading = false,
                        error = MISSING_SESSION_RESULT_MESSAGE,
                    )
                }
                return@launch
            }

            subscriptionRepository.seedPendingReviewsFromAnalysisSession(sessionResult)
            val nickname = resolveNickname(_uiState.value.nickname)
            val pendingReviewSummary = loadPendingReviewSummary()

            hasLoaded = true
            _uiState.update { state ->
                state.copy(
                    nickname = nickname,
                    detectedSubscriptions = sessionResult.toDetectedSubscriptionItems(),
                    pendingReviewSummary = pendingReviewSummary,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }

    private suspend fun resolveNickname(currentNickname: String): String {
        return when (val result = userRepository.getMe()) {
            is Result.Success -> result.data.displayName.ifBlank { FALLBACK_NICKNAME }
            is Result.Error -> currentNickname.ifBlank { FALLBACK_NICKNAME }
            Result.Loading -> currentNickname.ifBlank { FALLBACK_NICKNAME }
        }
    }

    private fun excludeDetectedSubscription(reviewId: Long) {
        _uiState.update { state ->
            state.copy(
                detectedSubscriptions = state.detectedSubscriptions.map { item ->
                    if (item.reviewId == reviewId) item.copy(excludedLocallyInSession = true) else item
                },
            )
        }
    }

    private fun openPendingReview() {
        viewModelScope.launch {
            _uiEffect.emit(SubscriptionConfirmUiEffect.NavigateToManualMapping)
        }
    }

    private fun observePendingReviewChanges() {
        viewModelScope.launch {
            subscriptionRepository.syncEvents.collectLatest { event ->
                if (event is SubscriptionSyncEvent.PendingReviewsChanged) {
                    refreshPendingReviewSummary()
                }
            }
        }
    }

    private suspend fun loadPendingReviewSummary(): PendingReviewSummaryItem? {
        return when (val result = subscriptionRepository.getPendingReviews()) {
            is Result.Success -> result.data.toPendingReviewSummaryItem()
            is Result.Error -> _uiState.value.pendingReviewSummary
            Result.Loading -> _uiState.value.pendingReviewSummary
        }
    }

    private suspend fun refreshPendingReviewSummary() {
        val pendingReviewSummary = loadPendingReviewSummary()
        _uiState.update { state -> state.copy(pendingReviewSummary = pendingReviewSummary) }
    }

    private fun confirmDetectedSubscriptions() {
        if (_uiState.value.isCompletingLocalSessionOnly) return

        _uiState.update { state -> state.copy(isCompletingLocalSessionOnly = true) }

        viewModelScope.launch {
            val targets = _uiState.value.visibleDetectedSubscriptions.distinctBy { item ->
                item.bundleCode ?: item.serviceId ?: item.reviewId
            }
            for (item in targets) {
                when (
                    val result = subscriptionRepository.createSubscription(
                        subscriptionType = item.subscriptionType,
                        serviceId = item.serviceId,
                        servicePlanId = item.servicePlanId,
                        bundleCode = item.bundleCode,
                        billingDay = item.billingDay,
                    )
                ) {
                    is Result.Success -> Unit
                    is Result.Error -> {
                        _uiState.update { state -> state.copy(isCompletingLocalSessionOnly = false) }
                        _uiEffect.emit(SubscriptionConfirmUiEffect.ShowToast(result.message))
                        return@launch
                    }

                    Result.Loading -> Unit
                }
            }

            analysisHandoffRepository.clearAnalysisSessionResult()
            _uiEffect.emit(SubscriptionConfirmUiEffect.NavigateToDashboard)
        }
    }

    private fun AnalysisSessionResult.toDetectedSubscriptionItems(): List<DetectedSubscriptionItem> {
        return confirmedHighConfidence
            .mapNotNull { resolution ->
                val matchedService = resolveCatalogService(resolution)
                val resolvedServiceId = resolution.serviceId ?: matchedService?.serviceId
                val resolvedServicePlanId = resolution.servicePlanId ?: resolveServicePlanId(resolution, matchedService)

                when (resolution.subscriptionType) {
                    CandidateResolution.SubscriptionType.BUNDLE -> DetectedSubscriptionSeed(
                        paymentDate = findCandidate(resolution.reviewId)?.paymentRecord?.paymentDate ?: LocalDate.MIN,
                        item = DetectedSubscriptionItem(
                            reviewId = resolution.reviewId,
                            subscriptionType = "BUNDLE",
                            bundleCode = resolution.bundleCode,
                            serviceName = resolution.bundleName ?: resolution.serviceName ?: resolution.merchantRaw,
                            monthlyAmount = resolution.monthlyAmount,
                            billedAtLabel = resolution.billedAtLabel,
                            billingDay = resolution.billedAtLabel.toBillingDay(),
                            logoUrl = matchedService?.logoUrl,
                        ),
                    )

                    else -> {
                        if (resolvedServiceId == null || resolvedServicePlanId == null) {
                            return@mapNotNull null
                        }
                        DetectedSubscriptionSeed(
                            paymentDate = findCandidate(resolution.reviewId)?.paymentRecord?.paymentDate ?: LocalDate.MIN,
                            item = DetectedSubscriptionItem(
                                reviewId = resolution.reviewId,
                                subscriptionType = "SINGLE",
                                serviceId = resolvedServiceId,
                                servicePlanId = resolvedServicePlanId,
                                serviceName = resolution.serviceName
                                    ?: matchedService?.name
                                    ?: findCandidate(resolution.reviewId)?.serviceCatalogHints?.firstOrNull()?.name
                                    ?: resolution.merchantRaw,
                                monthlyAmount = resolution.monthlyAmount,
                                billedAtLabel = resolution.billedAtLabel,
                                billingDay = resolution.billedAtLabel.toBillingDay(),
                                logoUrl = matchedService?.logoUrl
                                    ?: findCandidate(resolution.reviewId)?.serviceCatalogHints?.firstOrNull()?.logoUrl,
                            ),
                        )
                    }
                }
            }
            .sortedWith(
                compareByDescending<DetectedSubscriptionSeed> { it.paymentDate }
                    .thenByDescending { it.item.reviewId },
            )
            .distinctBy { seed -> seed.item.bundleCode ?: seed.item.serviceId ?: seed.item.reviewId }
            .map { seed -> seed.item }
    }

    private fun AnalysisSessionResult.resolveCatalogService(
        resolution: CandidateResolution,
    ): ServiceCatalog.Service? {
        return serviceCatalog?.services?.firstOrNull { service ->
            service.serviceId != null && service.serviceId == resolution.serviceId
        } ?: resolution.serviceName?.let { serviceName ->
            serviceCatalog?.findServiceByName(serviceName)
        }
    }

    private fun List<PendingReviewItem>.toPendingReviewSummaryItem(): PendingReviewSummaryItem? {
        if (isEmpty()) return null

        val previewLabel = mapNotNull { review -> review.suggestedServiceName }
            .toSet()
            .sorted()
            .take(MAX_PENDING_REVIEW_PREVIEW_COUNT)
            .joinToString(separator = ", ")
            .ifBlank { null }

        return PendingReviewSummaryItem(
            count = pendingReviewMerchantGroupCount(),
            suggestedServiceNamesLabel = previewLabel,
        )
    }

    private fun resolveServicePlanId(
        resolution: CandidateResolution,
        matchedService: ServiceCatalog.Service?,
    ): Long? {
        if (resolution.servicePlanId != null) {
            return resolution.servicePlanId
        }

        return matchedService?.plans?.minByOrNull { plan ->
            kotlin.math.abs(plan.monthlyPrice - resolution.monthlyAmount)
        }?.servicePlanId
    }

    private fun String.toBillingDay(): Int {
        return substringAfterLast('.').toIntOrNull()?.coerceIn(1, 31) ?: 1
    }

    private companion object {
        const val FALLBACK_NICKNAME = "회원"
        const val MAX_PENDING_REVIEW_PREVIEW_COUNT = 2
        const val MISSING_SESSION_RESULT_MESSAGE =
            "분석 세션 결과를 찾을 수 없어 다시 분석을 시작한 뒤 확인해 주세요."
    }
}
