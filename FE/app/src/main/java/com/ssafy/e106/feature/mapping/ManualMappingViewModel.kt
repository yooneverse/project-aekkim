package com.ssafy.e106.feature.mapping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.PendingReviewItem
import com.ssafy.e106.data.repository.ServicePlanBundle
import com.ssafy.e106.data.repository.ServiceRepository
import com.ssafy.e106.data.repository.SubscriptionRepository
import com.ssafy.e106.data.repository.toPendingReviewMerchantGroups
import com.ssafy.e106.feature.dashboard.ManualAddStep
import com.ssafy.e106.feature.dashboard.toDashboardPlanItem
import com.ssafy.e106.feature.dashboard.toDashboardServiceItem
import com.ssafy.e106.feature.dashboard.toDashboardServiceItems
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

@HiltViewModel
class ManualMappingViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualMappingUiState())
    val uiState: StateFlow<ManualMappingUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ManualMappingUiEffect>(replay = 0)
    val uiEffect: SharedFlow<ManualMappingUiEffect> = _uiEffect.asSharedFlow()

    private var hasLoaded = false

    fun onIntent(intent: ManualMappingIntent) {
        when (intent) {
            ManualMappingIntent.LoadPendingReviews -> loadPendingReviews(force = false)
            ManualMappingIntent.RetryLoad -> loadPendingReviews(force = true)
            is ManualMappingIntent.OpenAddFlow -> openAddFlow(intent.reviewIds)
            ManualMappingIntent.CloseManualAddFlow -> closeAddFlow()
            is ManualMappingIntent.SelectService -> selectService(intent.serviceId)
            is ManualMappingIntent.SelectPlan -> {
                _uiState.update { state -> state.copy(selectedPlanId = intent.servicePlanId) }
            }

            is ManualMappingIntent.SelectBillingDay -> {
                _uiState.update { state -> state.copy(selectedBillingDay = intent.day) }
            }

            ManualMappingIntent.SubmitManualAdd -> submitManualAdd()
            is ManualMappingIntent.RemovePendingReviews -> removePendingReviews(intent.reviewIds)
            is ManualMappingIntent.ToggleMerchantExpand -> {
                _uiState.update { state ->
                    val current = state.expandedMerchants
                    val toggled = if (intent.merchantName in current)
                        current - intent.merchantName
                    else
                        current + intent.merchantName
                    state.copy(expandedMerchants = toggled)
                }
            }
        }
    }

    private fun loadPendingReviews(force: Boolean) {
        if (hasLoaded && !force) return

        _uiState.update { state -> state.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = subscriptionRepository.getPendingReviews(forceRefresh = force)) {
                is Result.Success -> {
                    hasLoaded = true
                    val items = result.data.map { review -> review.toUiItem() }
                    val groups = result.data
                        .toPendingReviewMerchantGroups()
                        .map { group ->
                            val transactions = group.items.map { review -> review.toUiItem() }
                            MerchantGroupCardItem(
                                merchantName = group.merchantName,
                                reviewIds = transactions.map { it.reviewId },
                                transactions = transactions,
                                suggestedServiceName = transactions.firstOrNull()?.suggestedServiceName,
                            )
                        }
                    _uiState.update { state ->
                        state.copy(
                            pendingReviews = items,
                            merchantGroups = groups,
                            isLoading = false,
                            isEmpty = items.isEmpty(),
                            error = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isEmpty = false,
                            error = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun openAddFlow(reviewIds: List<Long>) {
        viewModelScope.launch {
            when (val result = serviceRepository.getAvailableServices()) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            manualAddStep = ManualAddStep.SelectService,
                            selectedReviewId = reviewIds.firstOrNull(),
                            selectedReviewIds = reviewIds,
                            serviceOptions = result.data.toDashboardServiceItems(),
                            selectedService = null,
                            planOptions = emptyList(),
                            selectedPlanId = null,
                            selectedBillingDay = null,
                        )
                    }
                }

                is Result.Error -> _uiEffect.emit(ManualMappingUiEffect.ShowToast(result.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun selectService(serviceId: Long) {
        val state = _uiState.value
        viewModelScope.launch {
            when (val result = serviceRepository.getServicePlans(serviceId)) {
                is Result.Success -> {
                    val pendingReview = state.selectedReviewId?.let(::findPendingReview)
                    val exactMatchedPlans = pendingReview?.let { review ->
                        result.data.plans.filter { plan -> plan.monthlyPrice == review.monthlyAmount }
                    }.orEmpty()

                    if (pendingReview != null && pendingReview.billingDay != null && exactMatchedPlans.size == 1) {
                        val matchedPlan = exactMatchedPlans.first()
                        _uiState.update { current ->
                            current.copy(
                                manualAddStep = ManualAddStep.ConfigurePlanAndBillingDay,
                                selectedService = result.data.toDashboardServiceItem(),
                                planOptions = result.data.plans.map { plan -> plan.toDashboardPlanItem() },
                                selectedPlanId = matchedPlan.servicePlanId,
                                selectedBillingDay = pendingReview.billingDay,
                            )
                        }
                        submitManualAdd(
                            reviewIds = state.selectedReviewIds,
                            serviceId = result.data.serviceId,
                            planId = matchedPlan.servicePlanId,
                            billingDay = pendingReview.billingDay,
                        )
                    } else {
                        applyServicePlanBundle(result.data)
                    }
                }
                is Result.Error -> _uiEffect.emit(ManualMappingUiEffect.ShowToast(result.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun applyServicePlanBundle(bundle: ServicePlanBundle) {
        _uiState.update { state ->
            state.copy(
                manualAddStep = ManualAddStep.ConfigurePlanAndBillingDay,
                selectedService = bundle.toDashboardServiceItem(),
                planOptions = bundle.plans.map { plan -> plan.toDashboardPlanItem() },
                selectedPlanId = null,
                selectedBillingDay = null,
            )
        }
    }

    private fun submitManualAdd() {
        val state = _uiState.value
        val serviceId = state.selectedService?.serviceId ?: return
        val planId = state.selectedPlanId ?: return
        val billingDay = state.selectedBillingDay ?: return
        submitManualAdd(
            reviewIds = state.selectedReviewIds,
            serviceId = serviceId,
            planId = planId,
            billingDay = billingDay,
        )
    }

    private fun submitManualAdd(
        reviewIds: List<Long>,
        serviceId: Long,
        planId: Long,
        billingDay: Int,
    ) {
        viewModelScope.launch {
            if (reviewIds.isEmpty()) {
                val result = subscriptionRepository.createSubscription(
                    subscriptionType = "SINGLE",
                    serviceId = serviceId,
                    servicePlanId = planId,
                    billingDay = billingDay,
                )
                handleSubmitResult(result)
                return@launch
            }

            // 첫 번째 reviewId로만 서버에 confirm 요청 (구독 1개 생성)
            val confirmResult = subscriptionRepository.confirmPendingReview(
                reviewId = reviewIds.first(),
                serviceId = serviceId,
                servicePlanId = planId,
                billingDay = billingDay,
            )

            if (confirmResult is Result.Error) {
                _uiEffect.emit(ManualMappingUiEffect.ShowToast("구독 저장에 실패했습니다."))
                return@launch
            }

            // 나머지 reviewId는 로컬에서 dismiss (서버 호출 없음)
            reviewIds.drop(1).forEach { reviewId ->
                subscriptionRepository.dismissPendingReview(reviewId)
            }

            // confirm 내부에서 removePendingReviewLocally가 이미 호출됐으므로
            // loadPendingReviews로 uiState만 갱신
            closeAddFlow()
            loadPendingReviews(force = true)
            _uiEffect.emit(ManualMappingUiEffect.ShowToast("구독이 저장되었습니다."))
        }
    }

    private suspend fun handleSubmitResult(result: Result<*>) {
        when (result) {
            is Result.Success -> {
                closeAddFlow()
                _uiEffect.emit(ManualMappingUiEffect.ShowToast("구독이 저장되었습니다."))
            }
            is Result.Error -> _uiEffect.emit(ManualMappingUiEffect.ShowToast(result.message))
            Result.Loading -> Unit
        }
    }

    private fun removePendingReviews(reviewIds: List<Long>) {
        viewModelScope.launch {
            val results = reviewIds.map { reviewId ->
                subscriptionRepository.dismissPendingReview(reviewId)
            }
            val hasError = results.any { it is Result.Error }
            loadPendingReviews(force = true)
            if (hasError) {
                _uiEffect.emit(ManualMappingUiEffect.ShowToast("일부 결제 후보를 제거하지 못했습니다."))
            } else {
                _uiEffect.emit(ManualMappingUiEffect.ShowToast("결제 후보를 제거했습니다."))
            }
        }
    }

    private fun closeAddFlow() {
        _uiState.update { state ->
            state.copy(
                manualAddStep = null,
                selectedReviewId = null,
                selectedReviewIds = emptyList(),
                selectedService = null,
                planOptions = emptyList(),
                selectedPlanId = null,
                selectedBillingDay = null,
            )
        }
    }

    private fun PendingReviewItem.toUiItem(): PendingReviewCardItem {
        return PendingReviewCardItem(
            reviewId = reviewId,
            merchantName = merchantName,
            suggestedServiceName = suggestedServiceName,
            monthlyAmount = monthlyAmount,
            billedAtLabel = billedAtLabel,
            billingDay = billingDay,
        )
    }

    private fun findPendingReview(reviewId: Long): PendingReviewCardItem? {
        return _uiState.value.pendingReviews.firstOrNull { review -> review.reviewId == reviewId }
    }
}
