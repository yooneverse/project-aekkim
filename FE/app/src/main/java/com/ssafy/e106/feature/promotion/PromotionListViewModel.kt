package com.ssafy.e106.feature.promotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.PromotionRepository
import com.ssafy.e106.data.repository.UserRepository
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
class PromotionListViewModel @Inject constructor(
    private val promotionRepository: PromotionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionListUiState())
    val uiState: StateFlow<PromotionListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PromotionListUiEffect>(replay = 0)
    val uiEffect: SharedFlow<PromotionListUiEffect> = _uiEffect.asSharedFlow()

    private var hasLoaded = false

    fun onIntent(intent: PromotionListIntent) {
        when (intent) {
            PromotionListIntent.LoadPromotions -> loadPromotions(forceRefresh = false)
            PromotionListIntent.RetryLoad -> loadPromotions(forceRefresh = true)
            is PromotionListIntent.SelectCategory -> selectCategory(intent.categoryKey)
            is PromotionListIntent.SelectTab -> selectTab(intent.tab)
            PromotionListIntent.ToggleExpanded -> toggleExpanded()
            is PromotionListIntent.OpenPromotionDetail -> navigateToPromotionDetail(intent.promotionId)
        }
    }

    private fun loadPromotions(forceRefresh: Boolean) {
        if (hasLoaded && !forceRefresh) return

        _uiState.update { state ->
            state.copy(screenState = PromotionListScreenState.Loading)
        }

        viewModelScope.launch {
            val nickname = resolveNickname(_uiState.value.nickname)

            when (val result = promotionRepository.getPromotionFeed()) {
                is Result.Success -> {
                    hasLoaded = true
                    val screenState = result.data.toListScreenState()
                    val selectedCategoryKey = screenState.firstCategoryKeyOrNull()
                    val selectedTab = screenState.firstAvailableTabOrDefault(selectedCategoryKey)

                    _uiState.update { state ->
                        state.copy(
                            nickname = nickname,
                            selectedCategoryKey = selectedCategoryKey,
                            selectedTab = selectedTab,
                            isExpanded = false,
                            screenState = screenState,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            nickname = nickname,
                            screenState = PromotionListScreenState.Error(
                                result.message.ifBlank { "추천 정보를 불러오지 못했습니다." },
                            ),
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun selectCategory(categoryKey: String) {
        val successState = _uiState.value.screenState as? PromotionListScreenState.Success ?: return
        val category = successState.categories.firstOrNull { it.categoryKey == categoryKey } ?: return

        _uiState.update { state ->
            val nextTab = if (category.availableTabs.contains(state.selectedTab)) {
                state.selectedTab
            } else {
                category.availableTabs.firstOrNull() ?: PromotionRecommendationTabUiType.Bundle
            }

            state.copy(
                selectedCategoryKey = category.categoryKey,
                selectedTab = nextTab,
                isExpanded = false,
            )
        }
    }

    private fun selectTab(tab: PromotionRecommendationTabUiType) {
        val successState = _uiState.value.screenState as? PromotionListScreenState.Success ?: return
        val selectedCategory = successState.categories.firstOrNull { it.categoryKey == _uiState.value.selectedCategoryKey }
            ?: successState.categories.firstOrNull()
            ?: return
        if (!selectedCategory.availableTabs.contains(tab)) return

        _uiState.update { state ->
            state.copy(
                selectedTab = tab,
                isExpanded = false,
            )
        }
    }

    private fun toggleExpanded() {
        val successState = _uiState.value.screenState as? PromotionListScreenState.Success ?: return
        val selectedCategory = successState.categories.firstOrNull { it.categoryKey == _uiState.value.selectedCategoryKey }
            ?: successState.categories.firstOrNull()
            ?: return
        val selectedItems = selectedCategory.itemsFor(_uiState.value.selectedTab)
        if (selectedItems.size <= DEFAULT_VISIBLE_ITEM_COUNT) return

        _uiState.update { state ->
            state.copy(isExpanded = !state.isExpanded)
        }
    }

    private suspend fun resolveNickname(currentNickname: String): String {
        return when (val result = userRepository.getMe()) {
            is Result.Success -> result.data.displayName.ifBlank { currentNickname.ifBlank { FALLBACK_NICKNAME } }
            is Result.Error -> currentNickname.ifBlank { FALLBACK_NICKNAME }
            Result.Loading -> currentNickname.ifBlank { FALLBACK_NICKNAME }
        }
    }

    private fun navigateToPromotionDetail(promotionId: Long) {
        viewModelScope.launch {
            _uiEffect.emit(PromotionListUiEffect.NavigateToPromotionDetail(promotionId))
        }
    }

    private companion object {
        const val FALLBACK_NICKNAME = "사용자"
        const val DEFAULT_VISIBLE_ITEM_COUNT = 3
    }
}

internal fun PromotionListScreenState.firstCategoryKeyOrNull(): String? {
    val categories = (this as? PromotionListScreenState.Success)?.categories.orEmpty()
    return categories.firstOrNull(PromotionRecommendationCategoryUiModel::hasAnyItems)?.categoryKey
        ?: categories.firstOrNull()?.categoryKey
}

internal fun PromotionListScreenState.firstAvailableTabOrDefault(
    categoryKey: String?,
): PromotionRecommendationTabUiType {
    val successState = this as? PromotionListScreenState.Success ?: return PromotionRecommendationTabUiType.Bundle
    val category = successState.categories.firstOrNull { category ->
        category.categoryKey == categoryKey && category.availableTabs.isNotEmpty()
    } ?: successState.categories.firstOrNull { category ->
        category.availableTabs.isNotEmpty()
    } ?: successState.categories.firstOrNull()
        ?: return PromotionRecommendationTabUiType.Bundle

    return category.availableTabs.firstOrNull() ?: PromotionRecommendationTabUiType.Bundle
}
