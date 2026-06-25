package com.ssafy.e106.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.CheckInPolicyRepository
import com.ssafy.e106.data.repository.DashboardSubscriptionsData
import com.ssafy.e106.data.repository.NotificationRepository
import com.ssafy.e106.data.repository.PromotionRepository
import com.ssafy.e106.data.repository.ServicePlanBundle
import com.ssafy.e106.data.repository.ServiceRepository
import com.ssafy.e106.data.repository.SubscriptionReminderBannerRepository
import com.ssafy.e106.data.repository.SubscriptionUsageRepository
import com.ssafy.e106.data.repository.SubscriptionMutationType
import com.ssafy.e106.data.repository.SubscriptionSyncEvent
import com.ssafy.e106.data.repository.SubscriptionRepository
import com.ssafy.e106.data.repository.pendingReviewMerchantGroupCount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val promotionRepository: PromotionRepository,
    private val serviceRepository: ServiceRepository,
    private val subscriptionUsageRepository: SubscriptionUsageRepository,
    private val subscriptionReminderBannerRepository: SubscriptionReminderBannerRepository,
    private val checkInPolicyRepository: CheckInPolicyRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<DashboardUiEffect>(replay = 0)
    val uiEffect: SharedFlow<DashboardUiEffect> = _uiEffect.asSharedFlow()

    private var hasLoadedDashboard = false
    private var syncRefreshJob: Job? = null
    private var usageInsightJob: Job? = null

    init {
        observeSubscriptionSync()
        observeUnreadNotifications()
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.LoadDashboard -> loadDashboard(forceRefresh = false)
            DashboardIntent.RefreshDashboard -> loadDashboard(forceRefresh = true)
            DashboardIntent.RetryLoad -> loadDashboard(forceRefresh = true)
            DashboardIntent.RetrySubscriptionDetail -> retrySubscriptionDetail()
            DashboardIntent.DismissUsageReminderBanner -> dismissUsageReminderBanner()
            DashboardIntent.ToggleEditMode -> toggleEditMode()

            DashboardIntent.OpenAddFlow -> openAddFlow()
            DashboardIntent.CloseManualAddFlow -> closeManualAddFlow()
            is DashboardIntent.UpdateManualAddQuery -> updateManualAddQuery(intent.query)
            is DashboardIntent.SelectService -> selectService(intent.serviceId)
            is DashboardIntent.SelectBundle -> selectBundle(intent.bundleCode)
            is DashboardIntent.SelectPlan -> selectPlan(intent.servicePlanId)
            is DashboardIntent.SelectBillingDay -> {
                _uiState.update { state ->
                    state.copy(
                        selectedBillingDay = intent.day,
                        selectedBillingDate = null,
                    )
                }
            }
            is DashboardIntent.SelectBillingDate -> {
                _uiState.update { state ->
                    state.copy(
                        selectedBillingDate = intent.date,
                        selectedBillingDay = null,
                    )
                }
            }

            DashboardIntent.SubmitManualAdd -> submitManualAdd()
            is DashboardIntent.OpenSubscriptionDetail -> openSubscriptionDetail(intent.subscriptionId)
            DashboardIntent.CloseSubscriptionDetail -> closeSubscriptionDetail()
            is DashboardIntent.StartEditSubscription -> startEditSubscription(intent.subscriptionId)
            is DashboardIntent.DeleteSubscription -> togglePendingDeletion(intent.subscriptionId)
        }
    }

    private fun observeSubscriptionSync() {
        viewModelScope.launch {
            // Dashboard state depends on server-calculated totals/labels and shared pending-review state, so it is re-fetched.
            subscriptionRepository.syncEvents.collectLatest { event ->
                when (event) {
                    SubscriptionSyncEvent.PendingReviewsChanged -> scheduleDashboardRefresh()

                    is SubscriptionSyncEvent.SubscriptionChanged -> {
                        handleSubscriptionDetailSync(event)
                        scheduleDashboardRefresh()
                    }
                }
            }
        }
    }

    private fun observeUnreadNotifications() {
        viewModelScope.launch {
            notificationRepository.hasUnreadNotifications.collectLatest { hasUnread ->
                _uiState.update { state -> state.copy(hasUnreadNotifications = hasUnread) }
            }
        }
    }

    private fun scheduleDashboardRefresh() {
        syncRefreshJob?.cancel()
        syncRefreshJob = viewModelScope.launch {
            delay(SUBSCRIPTION_SYNC_DEBOUNCE_MILLIS)
            loadDashboard(forceRefresh = true)
        }
    }

    private fun handleSubscriptionDetailSync(event: SubscriptionSyncEvent.SubscriptionChanged) {
        if (_uiState.value.selectedSubscriptionId != event.subscriptionId) return

        when (event.mutationType) {
            SubscriptionMutationType.Created -> Unit
            SubscriptionMutationType.Updated -> {
                _uiState.update { state ->
                    state.copy(
                        subscriptionDetailError = null,
                        isDetailLoading = true,
                    )
                }
                loadSubscriptionDetail(event.subscriptionId)
            }

            SubscriptionMutationType.Deleted -> closeSubscriptionDetail()
        }
    }

    private fun loadDashboard(forceRefresh: Boolean) {
        if (hasLoadedDashboard && !forceRefresh) return

        val hasExistingData = _uiState.value.subscriptions.isNotEmpty()
        _uiState.update { state ->
            state.copy(
                isLoading = !hasExistingData,
                isRefreshing = hasExistingData,
                error = if (hasExistingData) state.error else null,
            )
        }

        viewModelScope.launch {
            notificationRepository.refreshUnreadIndicator()
            when (val result = subscriptionRepository.getDashboardData()) {
                is Result.Success -> {
                    hasLoadedDashboard = true
                    applyDashboardData(result.data)
                }

                is Result.Error -> {
                    if (_uiState.value.subscriptions.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isStale = true,
                            )
                        }
                        _uiEffect.emit(
                            DashboardUiEffect.ShowToast(
                                result.message.ifBlank { "최근 데이터로 계속 보여드릴게요." },
                            ),
                        )
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isEmpty = false,
                                error = result.message,
                                isStale = false,
                            )
                        }
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun applyDashboardData(data: DashboardSubscriptionsData) {
        val policyBanner = checkInPolicyRepository.getPendingCheckinForBanner()
        val bannerState = policyBanner?.let { checkin ->
            CheckinBannerState(
                subscriptionId = checkin.subscriptionId,
                title = "${checkin.serviceName} 이번 달도 보고 계세요?",
                description = if (checkin.isDueToday) {
                    "결제일이 가까워지고 있어요."
                } else {
                    "지난 체크인에 아직 응답하지 않았어요."
                },
            )
        }

        val subscriptionItems = data.subscriptions.map { subscription -> subscription.toSubscriptionCardItem() }
        val serverExpectedSavingAmount = subscriptionItems.totalExpectedSavingAmount()
        val subscriptionSnapshot = subscriptionItems.map(SubscriptionCardItem::subscriptionId)
        _uiState.update { state ->
            state.copy(
                subscriptions = subscriptionItems,
                monthlyTotalAmount = data.monthlyTotalAmount,
                monthlyExpectedSavingAmount = when {
                    serverExpectedSavingAmount > 0 -> serverExpectedSavingAmount
                    state.subscriptions.map(SubscriptionCardItem::subscriptionId) == subscriptionSnapshot ->
                        state.monthlyExpectedSavingAmount
                    else -> 0
                },
                pendingCheckinBanner = bannerState,
                pendingReviewCount = data.pendingReviews.pendingReviewMerchantGroupCount(),
                pendingReviews = data.pendingReviews.map { review -> review.toPendingReviewUiItem() },
                isLoading = false,
                isRefreshing = false,
                isEmpty = data.subscriptions.isEmpty(),
                pendingDeletedSubscriptionIds = emptySet(),
                isApplyingEditChanges = false,
                error = null,
                isStale = false,
            )
        }

        loadPromotionFallbackSavings(
            subscriptions = subscriptionItems,
            baselineExpectedSavingAmount = serverExpectedSavingAmount,
        )
        loadSubscriptionUsageInsights(subscriptionItems)
        resolveMissingBillingCycles(subscriptionItems)
        checkAutoFullScreenEntry()
    }

    private fun loadPromotionFallbackSavings(
        subscriptions: List<SubscriptionCardItem>,
        baselineExpectedSavingAmount: Int,
    ) {
        if (subscriptions.isEmpty() || baselineExpectedSavingAmount > 0) return

        val subscriptionSnapshot = subscriptions.map(SubscriptionCardItem::subscriptionId)
        viewModelScope.launch {
            when (val result = promotionRepository.getPromotionFeed()) {
                is Result.Success -> {
                    val fallbackExpectedSavingAmount =
                        subscriptions.calculatePromotionFeedExpectedSavingAmount(result.data)
                    _uiState.update { state ->
                        if (state.subscriptions.map(SubscriptionCardItem::subscriptionId) != subscriptionSnapshot) {
                            return@update state
                        }
                        if (state.monthlyExpectedSavingAmount > 0) {
                            return@update state
                        }
                        state.copy(monthlyExpectedSavingAmount = fallbackExpectedSavingAmount)
                    }
                }

                is Result.Error,
                Result.Loading -> Unit
            }
        }
    }

    private fun resolveMissingBillingCycles(subscriptions: List<SubscriptionCardItem>) {
        val unresolvedSubscriptions = subscriptions.filter { item ->
            item.subscriptionType == "SINGLE" &&
                item.serviceId > 0L &&
                item.servicePlanId > 0L &&
                item.billingCycle.isNullOrBlank()
        }
        if (unresolvedSubscriptions.isEmpty()) return

        val serviceIds = unresolvedSubscriptions.map { item -> item.serviceId }.distinct()
        viewModelScope.launch {
            val billingCycleByPlanId = mutableMapOf<Long, String>()
            serviceIds.forEach { serviceId ->
                when (val result = serviceRepository.getServicePlans(serviceId)) {
                    is Result.Success -> {
                        result.data.plans.forEach { plan ->
                            billingCycleByPlanId[plan.servicePlanId] = plan.billingCycle
                        }
                    }

                    is Result.Error -> Unit
                    Result.Loading -> Unit
                }
            }

            if (billingCycleByPlanId.isEmpty()) return@launch

            _uiState.update { state ->
                state.copy(
                    subscriptions = state.subscriptions.map { item ->
                        val resolvedBillingCycle = item.billingCycle
                            ?: billingCycleByPlanId[item.servicePlanId]
                        if (resolvedBillingCycle == item.billingCycle) {
                            item
                        } else {
                            item.copy(billingCycle = resolvedBillingCycle)
                        }
                    },
                )
            }
        }
    }

    private fun loadSubscriptionUsageInsights(
        subscriptions: List<SubscriptionCardItem>,
    ) {
        usageInsightJob?.cancel()
        if (subscriptions.isEmpty()) return

        usageInsightJob = viewModelScope.launch {
            val reportResult = subscriptionUsageRepository.getUsageReport()
            val reportItemsBySubscriptionId = when (reportResult) {
                is Result.Success -> reportResult.data.items.associateBy { item -> item.subscriptionId }
                else -> emptyMap()
            }

            _uiState.update { state ->
                state.copy(
                    subscriptions = state.subscriptions.map { item ->
                        item.withUsageInsight(
                            reportItem = reportItemsBySubscriptionId[item.subscriptionId],
                        )
                    },
                    usageReminderBanner = resolveUsageReminderBanner(
                        reportItems = reportItemsBySubscriptionId.values.toList(),
                    ),
                )
            }
        }
    }

    private fun resolveUsageReminderBanner(
        reportItems: List<com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageReportItemResponse>,
    ): UsageReminderBannerState? {
        val today = LocalDate.now()
        val candidates = reportItems.mapNotNull { item ->
            val lastUsedDate = item.lastUsedDate
                ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
                ?: return@mapNotNull null
            val daysSinceLastUsed = java.time.temporal.ChronoUnit.DAYS.between(lastUsedDate, today)
            if (daysSinceLastUsed < 7) return@mapNotNull null

            UsageReminderCandidate(
                subscriptionId = item.subscriptionId,
                serviceName = item.serviceName,
                lastUsedDate = lastUsedDate,
                daysSinceLastUsed = daysSinceLastUsed,
                monthlyPrice = item.monthlyPrice,
            )
        }.sortedWith(
            compareByDescending<UsageReminderCandidate> { candidate -> candidate.daysSinceLastUsed }
                .thenByDescending { candidate -> candidate.monthlyPrice }
                .thenBy { candidate -> candidate.lastUsedDate },
        )

        if (candidates.isEmpty()) return null

        val signature = buildUsageReminderSignature(candidates)
        if (subscriptionReminderBannerRepository.getDismissedSignature() == signature) {
            return null
        }

        val primaryCandidate = candidates.first()

        return if (candidates.size == 1) {
            UsageReminderBannerState(
                signature = signature,
                title = "${primaryCandidate.serviceName}을 최근 사용하지 않았어요",
                candidateCount = 1,
                targetSubscriptionId = primaryCandidate.subscriptionId,
            )
        } else {
            UsageReminderBannerState(
                signature = signature,
                title = "최근 사용하지 않은 구독이 ${candidates.size}개 있어요",
                candidateCount = candidates.size,
                targetSubscriptionId = primaryCandidate.subscriptionId,
            )
        }
    }

    private fun buildUsageReminderSignature(
        candidates: List<UsageReminderCandidate>,
    ): String {
        return candidates.joinToString(
            prefix = if (candidates.size == 1) "single:" else "group:",
            separator = "|",
        ) { candidate ->
            "${candidate.subscriptionId}:${candidate.lastUsedDate}"
        }
    }

    private fun dismissUsageReminderBanner() {
        val banner = _uiState.value.usageReminderBanner ?: return
        subscriptionReminderBannerRepository.dismiss(banner.signature)
        _uiState.update { state ->
            state.copy(usageReminderBanner = null)
        }
    }

    private fun toggleEditMode() {
        val state = _uiState.value
        if (state.isApplyingEditChanges) return

        if (!state.isEditMode) {
            _uiState.update { current ->
                current.copy(
                    isEditMode = true,
                    pendingDeletedSubscriptionIds = emptySet(),
                )
            }
            return
        }

        completePendingDeletions()
    }

    private fun togglePendingDeletion(subscriptionId: Long) {
        _uiState.update { state ->
            if (!state.isEditMode || state.isApplyingEditChanges) return@update state

            val nextPendingIds = state.pendingDeletedSubscriptionIds.toMutableSet().apply {
                if (!add(subscriptionId)) remove(subscriptionId)
            }
            state.copy(pendingDeletedSubscriptionIds = nextPendingIds)
        }
    }

    private fun completePendingDeletions() {
        val state = _uiState.value
        val pendingIds = state.pendingDeletedSubscriptionIds.toList()

        if (pendingIds.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isEditMode = false,
                    pendingDeletedSubscriptionIds = emptySet(),
                )
            }
            return
        }

        _uiState.update { current -> current.copy(isApplyingEditChanges = true) }

        viewModelScope.launch {
            val succeededIds = mutableListOf<Long>()
            val failedIds = mutableListOf<Long>()

            pendingIds.forEach { subscriptionId ->
                when (subscriptionRepository.deleteSubscription(subscriptionId = subscriptionId)) {
                    is Result.Success -> succeededIds += subscriptionId
                    is Result.Error -> failedIds += subscriptionId
                    Result.Loading -> Unit
                }
            }

            if (succeededIds.isNotEmpty() && _uiState.value.selectedSubscriptionId in succeededIds) {
                closeSubscriptionDetail()
            }

            _uiState.update { current ->
                val remainingSubscriptions = current.subscriptions.filterNot { item ->
                    item.subscriptionId in succeededIds
                }
                current.copy(
                    subscriptions = remainingSubscriptions,
                    monthlyTotalAmount = remainingSubscriptions.sumOf { item -> item.monthlyPrice },
                    isEmpty = remainingSubscriptions.isEmpty(),
                    isEditMode = failedIds.isNotEmpty(),
                    pendingDeletedSubscriptionIds = failedIds.toSet(),
                    isApplyingEditChanges = false,
                )
            }

            when {
                failedIds.isEmpty() -> _uiEffect.emit(DashboardUiEffect.ShowToast("구독 변경을 저장했어요."))
                succeededIds.isEmpty() -> _uiEffect.emit(DashboardUiEffect.ShowToast("구독 삭제에 실패했어요."))
                else -> _uiEffect.emit(
                    DashboardUiEffect.ShowToast(
                        "일부 구독만 삭제했어요. 남은 항목을 다시 확인해 주세요.",
                    ),
                )
            }
        }
    }

    private fun checkAutoFullScreenEntry() {
        val target = checkInPolicyRepository.getAutoFullScreenTarget() ?: return
        checkInPolicyRepository.markFullScreenShown()

        viewModelScope.launch {
            _uiEffect.emit(DashboardUiEffect.AutoNavigateToCheckIn(target.subscriptionId))
        }
    }

    private fun openAddFlow() {
        viewModelScope.launch {
            when (val result = serviceRepository.getSubscriptionAddCatalog()) {
                is Result.Success -> {
                    val serviceOptions = result.data.services.toDashboardServiceItems()
                    val bundleOptions = result.data.bundles.map { bundle -> bundle.toDashboardBundleItem() }
                    if (serviceOptions.isEmpty() && bundleOptions.isEmpty()) {
                        _uiEffect.emit(
                            DashboardUiEffect.ShowToast(
                                "추가 가능한 서비스가 아직 준비되지 않았어요. 잠시 후 다시 시도해 주세요.",
                            ),
                        )
                        return@launch
                    }

                    val searchResults = filterDashboardAddTargets(
                        query = "",
                        services = serviceOptions,
                        bundles = bundleOptions,
                    )
                    _uiState.update { state ->
                        state.copy(
                            manualAddStep = ManualAddStep.SearchCatalog,
                            manualAddQuery = "",
                            serviceOptions = serviceOptions,
                            serviceSearchResults = searchResults.services,
                            bundleOptions = bundleOptions,
                            bundleSearchResults = searchResults.bundles,
                            selectedService = null,
                            selectedBundle = null,
                            planOptions = emptyList(),
                            selectedPlanId = null,
                            selectedBillingDay = null,
                            selectedBillingDate = null,
                            editingSubscriptionId = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowToast(result.message))
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun updateManualAddQuery(query: String) {
        _uiState.update { state ->
            val searchResults = filterDashboardAddTargets(
                query = query,
                services = state.serviceOptions,
                bundles = state.bundleOptions,
            )
            state.copy(
                manualAddQuery = query,
                serviceSearchResults = searchResults.services,
                bundleSearchResults = searchResults.bundles,
            )
        }
    }

    private fun selectPlan(servicePlanId: Long) {
        _uiState.update { state ->
            val selectedPlan = state.planOptions.firstOrNull { plan -> plan.servicePlanId == servicePlanId }
            state.copy(
                selectedPlanId = servicePlanId,
                selectedBillingDay = if (selectedPlan?.isYearly() == true) null else state.selectedBillingDay,
                selectedBillingDate = if (selectedPlan?.isYearly() == true) state.selectedBillingDate else null,
            )
        }
    }

    private fun selectService(serviceId: Long) {
        viewModelScope.launch {
            when (val result = serviceRepository.getServicePlans(serviceId)) {
                is Result.Success -> {
                    applyServicePlanBundle(
                        bundle = result.data,
                        editingSubscriptionId = _uiState.value.editingSubscriptionId,
                        selectedPlanId = _uiState.value.selectedPlanId,
                        selectedBillingDay = _uiState.value.selectedBillingDay,
                        selectedBillingDate = _uiState.value.selectedBillingDate,
                    )
                }

                is Result.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowToast(result.message))
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun selectBundle(bundleCode: String) {
        val targetBundle = _uiState.value.bundleOptions.firstOrNull { bundle -> bundle.code == bundleCode }
            ?: return

        applyBundleConfiguration(
            bundle = targetBundle,
            editingSubscriptionId = null,
            selectedBillingDay = null,
            selectedBillingDate = null,
        )
    }

    private fun startEditSubscription(subscriptionId: Long) {
        val target = _uiState.value.subscriptions.firstOrNull { item -> item.subscriptionId == subscriptionId }
            ?: return

        if (target.subscriptionType == "BUNDLE" && target.bundleCode != null) {
            startEditBundleSubscription(target)
            return
        }

        viewModelScope.launch {
            when (val result = serviceRepository.getServicePlans(target.serviceId)) {
                is Result.Success -> {
                    val selectedPlan = result.data.plans.firstOrNull { plan ->
                        plan.servicePlanId == target.servicePlanId
                    }
                    val nextBillingDate = target.nextBillingDate?.toLocalDateOrNull()
                    applyServicePlanBundle(
                        bundle = result.data,
                        editingSubscriptionId = subscriptionId,
                        selectedPlanId = target.servicePlanId,
                        selectedBillingDay = if (selectedPlan?.isYearly() == true) {
                            null
                        } else {
                            nextBillingDate?.dayOfMonth
                        },
                        selectedBillingDate = if (selectedPlan?.isYearly() == true) {
                            nextBillingDate
                        } else {
                            null
                        },
                    )
                    _uiState.update { state ->
                        state.copy(
                            selectedSubscriptionId = null,
                            subscriptionDetail = null,
                            subscriptionDetailError = null,
                            isDetailLoading = false,
                        )
                    }
                }

                is Result.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowToast(result.message))
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun startEditBundleSubscription(target: SubscriptionCardItem) {
        viewModelScope.launch {
            when (val result = serviceRepository.getSubscriptionAddCatalog()) {
                is Result.Success -> {
                    val serviceOptions = result.data.services.toDashboardServiceItems()
                    val bundleOptions = result.data.bundles.map { bundle -> bundle.toDashboardBundleItem() }
                    val matchedBundle = bundleOptions.firstOrNull { bundle -> bundle.code == target.bundleCode }
                        ?: target.toFallbackBundleItem()
                    val searchResults = filterDashboardAddTargets(
                        query = "",
                        services = serviceOptions,
                        bundles = bundleOptions,
                    )

                    _uiState.update { state ->
                        val nextBillingDate = target.nextBillingDate?.toLocalDateOrNull()
                        state.copy(
                            manualAddQuery = "",
                            manualAddStep = ManualAddStep.ConfigureBundleAndBillingDay,
                            serviceOptions = serviceOptions,
                            serviceSearchResults = searchResults.services,
                            bundleOptions = bundleOptions,
                            bundleSearchResults = searchResults.bundles,
                            selectedService = null,
                            selectedBundle = matchedBundle,
                            planOptions = emptyList(),
                            selectedPlanId = null,
                            selectedBillingDay = if (matchedBundle.isYearly()) null else nextBillingDate?.dayOfMonth,
                            selectedBillingDate = if (matchedBundle.isYearly()) nextBillingDate else null,
                            editingSubscriptionId = target.subscriptionId,
                            selectedSubscriptionId = null,
                            subscriptionDetail = null,
                            subscriptionDetailError = null,
                            isDetailLoading = false,
                        )
                    }
                }

                is Result.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowToast(result.message))
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun applyServicePlanBundle(
        bundle: ServicePlanBundle,
        editingSubscriptionId: Long?,
        selectedPlanId: Long?,
        selectedBillingDay: Int?,
        selectedBillingDate: LocalDate?,
    ) {
        _uiState.update { state ->
            state.copy(
                manualAddStep = ManualAddStep.ConfigurePlanAndBillingDay,
                selectedService = bundle.toDashboardServiceItem(),
                selectedBundle = null,
                planOptions = bundle.plans.map { plan -> plan.toDashboardPlanItem() },
                selectedPlanId = selectedPlanId,
                selectedBillingDay = selectedBillingDay,
                selectedBillingDate = selectedBillingDate,
                editingSubscriptionId = editingSubscriptionId,
            )
        }
    }

    private fun applyBundleConfiguration(
        bundle: DashboardBundleItem,
        editingSubscriptionId: Long?,
        selectedBillingDay: Int?,
        selectedBillingDate: LocalDate?,
    ) {
        _uiState.update { state ->
            state.copy(
                manualAddStep = ManualAddStep.ConfigureBundleAndBillingDay,
                selectedService = null,
                selectedBundle = bundle,
                planOptions = emptyList(),
                selectedPlanId = null,
                selectedBillingDay = selectedBillingDay,
                selectedBillingDate = selectedBillingDate,
                editingSubscriptionId = editingSubscriptionId,
            )
        }
    }

    private fun submitManualAdd() {
        val state = _uiState.value
        val selectedPlan = state.planOptions.firstOrNull { plan -> plan.servicePlanId == state.selectedPlanId }
        val isYearlySelection = state.selectedBundle?.isYearly() == true || selectedPlan?.isYearly() == true
        val billingDay = if (isYearlySelection) null else state.selectedBillingDay
        val nextBillingDate = if (isYearlySelection) state.selectedBillingDate?.toString() else null

        if (!isYearlySelection && billingDay == null) return
        if (isYearlySelection && nextBillingDate == null) return

        viewModelScope.launch {
            val result = if (state.editingSubscriptionId != null) {
                when {
                    state.selectedBundle != null -> subscriptionRepository.updateSubscription(
                        subscriptionId = state.editingSubscriptionId,
                        bundleCode = state.selectedBundle.code,
                        billingDay = billingDay,
                        nextBillingDate = nextBillingDate,
                    )

                    else -> {
                        val planId = state.selectedPlanId ?: return@launch
                        subscriptionRepository.updateSubscription(
                            subscriptionId = state.editingSubscriptionId,
                            servicePlanId = planId,
                            billingDay = billingDay,
                            nextBillingDate = nextBillingDate,
                        )
                    }
                }
            } else {
                when {
                    state.selectedBundle != null -> subscriptionRepository.createSubscription(
                        subscriptionType = "BUNDLE",
                        bundleCode = state.selectedBundle.code,
                        billingDay = billingDay,
                        nextBillingDate = nextBillingDate,
                    )

                    else -> {
                        val serviceId = state.selectedService?.serviceId ?: return@launch
                        val planId = state.selectedPlanId ?: return@launch
                        subscriptionRepository.createSubscription(
                            subscriptionType = "SINGLE",
                            serviceId = serviceId,
                            servicePlanId = planId,
                            billingDay = billingDay,
                            nextBillingDate = nextBillingDate,
                        )
                    }
                }
            }

            when (result) {
                is Result.Success -> {
                    closeManualAddFlow()
                    _uiEffect.emit(
                        DashboardUiEffect.ShowToast(
                            if (state.editingSubscriptionId != null) {
                                "구독 정보를 수정했어요."
                            } else {
                                "구독을 추가했어요."
                            },
                        ),
                    )
                }

                is Result.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowToast(result.message))
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun openSubscriptionDetail(subscriptionId: Long) {
        if (_uiState.value.isEditMode) return

        _uiState.update { state ->
            state.copy(
                selectedSubscriptionId = subscriptionId,
                subscriptionDetail = null,
                subscriptionDetailError = null,
                isDetailLoading = true,
            )
        }

        loadSubscriptionDetail(subscriptionId)
    }

    private fun retrySubscriptionDetail() {
        val subscriptionId = _uiState.value.selectedSubscriptionId ?: return
        _uiState.update { state ->
            state.copy(
                subscriptionDetail = null,
                subscriptionDetailError = null,
                isDetailLoading = true,
            )
        }
        loadSubscriptionDetail(subscriptionId)
    }

    private fun loadSubscriptionDetail(subscriptionId: Long) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.getSubscriptionDetail(subscriptionId = subscriptionId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            subscriptionDetail = result.data.toSubscriptionDetailUiModel().copy(
                                isUsageDailyLoading = true,
                                usageDailyErrorMessage = null,
                            ),
                            subscriptionDetailError = null,
                            isDetailLoading = false,
                        )
                    }
                    loadSubscriptionUsageDaily(subscriptionId)
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            subscriptionDetail = null,
                            subscriptionDetailError = result.message,
                            isDetailLoading = false,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun closeSubscriptionDetail() {
        _uiState.update { state ->
            state.copy(
                selectedSubscriptionId = null,
                subscriptionDetail = null,
                subscriptionDetailError = null,
                isDetailLoading = false,
            )
        }
    }

    private fun loadSubscriptionUsageDaily(subscriptionId: Long) {
        viewModelScope.launch {
            when (
                val result = subscriptionUsageRepository.getUsageDaily(
                    subscriptionId = subscriptionId,
                )
            ) {
                is Result.Success -> {
                    _uiState.update { state ->
                        if (state.selectedSubscriptionId != subscriptionId) return@update state
                        val currentDetail = state.subscriptionDetail ?: return@update state
                        state.copy(
                            subscriptionDetail = currentDetail.copy(
                                usageDaily = result.data.items.toSubscriptionDetailUsagePoints(),
                                isUsageDailyLoading = false,
                                usageDailyErrorMessage = null,
                            ),
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        if (state.selectedSubscriptionId != subscriptionId) return@update state
                        val currentDetail = state.subscriptionDetail ?: return@update state
                        state.copy(
                            subscriptionDetail = currentDetail.copy(
                                isUsageDailyLoading = false,
                                usageDailyErrorMessage = result.message,
                            ),
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun closeManualAddFlow() {
        _uiState.update { state ->
            state.copy(
                manualAddStep = null,
                manualAddQuery = "",
                serviceOptions = emptyList(),
                serviceSearchResults = emptyList(),
                bundleOptions = emptyList(),
                bundleSearchResults = emptyList(),
                selectedService = null,
                selectedBundle = null,
                planOptions = emptyList(),
                selectedPlanId = null,
                selectedBillingDay = null,
                selectedBillingDate = null,
                editingSubscriptionId = null,
            )
        }
    }

    private fun SubscriptionCardItem.toFallbackBundleItem(): DashboardBundleItem {
        return DashboardBundleItem(
            code = bundleCode.orEmpty(),
            name = serviceName,
            planName = serviceName,
            billingCycle = "MONTHLY",
            monthlyPrice = monthlyPrice,
            logoUrl = logoUrl,
            includedServices = coveredServices,
        )
    }

    private companion object {
        const val SUBSCRIPTION_SYNC_DEBOUNCE_MILLIS = 75L
    }
}

private fun com.ssafy.e106.data.repository.ServicePlanOption.isYearly(): Boolean {
    return billingCycle.equals("YEARLY", ignoreCase = true)
}

private fun DashboardPlanItem.isYearly(): Boolean = billingCycle.equals("YEARLY", ignoreCase = true)

private fun DashboardBundleItem.isYearly(): Boolean = billingCycle.equals("YEARLY", ignoreCase = true)

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private data class UsageReminderCandidate(
    val subscriptionId: Long,
    val serviceName: String,
    val lastUsedDate: LocalDate,
    val daysSinceLastUsed: Long,
    val monthlyPrice: Int,
)
