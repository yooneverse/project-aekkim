package com.ssafy.e106.feature.promotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.PromotionRepository
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
class PromotionDetailViewModel @Inject constructor(
    private val promotionRepository: PromotionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionDetailUiState())
    val uiState: StateFlow<PromotionDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PromotionDetailUiEffect>(replay = 0)
    val uiEffect: SharedFlow<PromotionDetailUiEffect> = _uiEffect.asSharedFlow()

    private var loadedPromotionId: Long? = null

    fun onIntent(intent: PromotionDetailIntent) {
        when (intent) {
            is PromotionDetailIntent.LoadPromotionDetail ->
                loadPromotionDetail(promotionId = intent.promotionId, forceRefresh = false)

            PromotionDetailIntent.RetryLoad -> {
                val promotionId = _uiState.value.promotionId ?: return
                loadPromotionDetail(promotionId = promotionId, forceRefresh = true)
            }

            PromotionDetailIntent.OpenSignupLink -> openSourceUrl()
        }
    }

    private fun loadPromotionDetail(
        promotionId: Long,
        forceRefresh: Boolean,
    ) {
        if (loadedPromotionId == promotionId && !forceRefresh) return

        _uiState.update { state ->
            state.copy(
                promotionId = promotionId,
                screenState = PromotionDetailScreenState.Loading,
            )
        }

        viewModelScope.launch {
            when (val result = promotionRepository.getPromotionDetail(promotionId)) {
                is Result.Success -> {
                    loadedPromotionId = promotionId
                    _uiState.update { state ->
                        state.copy(screenState = result.data.toDetailScreenState())
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            screenState = PromotionDetailScreenState.Error(
                                result.message.ifBlank { "프로모션 상세 정보를 불러오지 못했어요." },
                            ),
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun openSourceUrl() {
        viewModelScope.launch {
            when (val screenState = _uiState.value.screenState) {
                is PromotionDetailScreenState.Success -> {
                    val url = screenState.promotion.sourceUrl
                    if (url.isNullOrBlank()) {
                        _uiEffect.emit(PromotionDetailUiEffect.ShowToast("가입 링크가 아직 준비되지 않았어요."))
                    } else {
                        _uiEffect.emit(PromotionDetailUiEffect.OpenExternalLink(url))
                    }
                }

                is PromotionDetailScreenState.Expired -> {
                    _uiEffect.emit(PromotionDetailUiEffect.ShowToast("이미 종료된 프로모션이에요."))
                }

                is PromotionDetailScreenState.NoLink -> {
                    _uiEffect.emit(PromotionDetailUiEffect.ShowToast("가입 링크가 아직 준비되지 않았어요."))
                }

                is PromotionDetailScreenState.Error,
                PromotionDetailScreenState.Loading,
                -> Unit
            }
        }
    }
}
