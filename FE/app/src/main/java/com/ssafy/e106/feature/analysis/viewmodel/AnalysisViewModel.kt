package com.ssafy.e106.feature.analysis.viewmodel

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.feature.analysis.AnalysisPipelineOrchestrator
import com.ssafy.e106.feature.analysis.AnalysisPipelinePhase
import com.ssafy.e106.feature.analysis.model.AnalysisStep
import com.ssafy.e106.feature.analysis.UsagePermissionChecker
import com.ssafy.e106.feature.analysis.model.AnalysisIntent
import com.ssafy.e106.feature.analysis.model.AnalysisUiEffect
import com.ssafy.e106.feature.analysis.model.AnalysisUiState
import com.ssafy.e106.feature.analysis.model.UsagePermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usagePermissionChecker: UsagePermissionChecker,
    private val analysisPipelineOrchestrator: AnalysisPipelineOrchestrator,
) : ViewModel() {
    private var analysisJob: Job? = null

    private companion object {
        const val TAG = "AnalysisViewModel"
        const val MIN_STEP_DURATION_MS = 1_500L
        const val POST_COMPLETE_DELAY_MS = 700L
        const val MATCHING_STEP_PLACEHOLDER_SAVING = 2_400
        const val REPORT_STEP_PLACEHOLDER_SAVING = 4_800
    }

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisUiEffect>(replay = 0)
    val uiEffect: SharedFlow<AnalysisUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: AnalysisIntent) {
        when (intent) {
            AnalysisIntent.Entered,
            AnalysisIntent.RecheckPermissionOnResume
            -> refreshPermissionAndCollect()

            AnalysisIntent.OpenSettingsClicked -> openUsageSettings()
            AnalysisIntent.ContinueClicked -> navigateToSubscriptionConfirm()
        }
    }

    private fun refreshPermissionAndCollect() {
        val granted = usagePermissionChecker.isGranted()
        if (!granted) {
            analysisJob?.cancel()
            analysisJob = null
            _uiState.update { state ->
                state.copy(
                    permissionState = UsagePermissionState.DENIED,
                    usageItems = emptyList(),
                    isCollecting = false,
                    currentStep = AnalysisStep.FetchingPayments,
                    completedSteps = emptySet(),
                    savingAmount = 0,
                )
            }
            return
        }

        if (analysisJob?.isActive == true) return

        analysisJob = viewModelScope.launch(Dispatchers.Default) {
            val visibleStepTracker = VisibleStepTracker(
                currentStep = AnalysisStep.FetchingPayments,
                startedAtElapsedMs = SystemClock.elapsedRealtime(),
            )
            try {
                _uiState.update { state ->
                    state.copy(
                        permissionState = UsagePermissionState.GRANTED,
                        usageItems = emptyList(),
                        isCollecting = true,
                        currentStep = AnalysisStep.FetchingPayments,
                        completedSteps = emptySet(),
                        savingAmount = 0,
                    )
                }
                val snapshot = analysisPipelineOrchestrator.run { phase ->
                    applyVisibleStepPhase(phase, visibleStepTracker)
                }
                awaitMinimumVisibleStepDuration(visibleStepTracker)
                _uiState.update { state ->
                    state.copy(
                        permissionState = UsagePermissionState.GRANTED,
                        usageItems = snapshot.usageItems,
                        isCollecting = false,
                        currentStep = AnalysisStep.BuildingReport,
                        completedSteps = AnalysisStep.entries.toSet(),
                        savingAmount = snapshot.estimatedSavingAmount,
                    )
                }

                delay(POST_COMPLETE_DELAY_MS)
                _uiEffect.emit(AnalysisUiEffect.NavigateToSubscriptionConfirm)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable

                awaitMinimumVisibleStepDuration(visibleStepTracker)
                _uiState.update { state ->
                    state.copy(
                        isCollecting = false,
                        completedSteps = AnalysisStep.entries.toSet(),
                        savingAmount = maxOf(state.savingAmount, REPORT_STEP_PLACEHOLDER_SAVING),
                    )
                }
                delay(POST_COMPLETE_DELAY_MS)
                _uiEffect.emit(AnalysisUiEffect.NavigateToSubscriptionConfirm)
            } finally {
                analysisJob = null
            }
        }
    }

    private fun openUsageSettings() {
        viewModelScope.launch {
            _uiEffect.emit(AnalysisUiEffect.OpenUsageAccessSettings)
        }
    }

    private fun navigateToSubscriptionConfirm() {
        viewModelScope.launch {
            _uiEffect.emit(AnalysisUiEffect.NavigateToSubscriptionConfirm)
        }
    }

    private suspend fun applyVisibleStepPhase(
        phase: AnalysisPipelinePhase,
        tracker: VisibleStepTracker,
    ) {
        val targetStep = phase.toVisibleAnalysisStep()
        if (targetStep != tracker.currentStep) {
            awaitMinimumVisibleStepDuration(tracker)
            tracker.currentStep = targetStep
            tracker.startedAtElapsedMs = SystemClock.elapsedRealtime()
        }

        _uiState.update { state ->
            state.copy(
                permissionState = UsagePermissionState.GRANTED,
                isCollecting = true,
                currentStep = targetStep,
                completedSteps = when (targetStep) {
                    AnalysisStep.FetchingPayments -> emptySet()
                    AnalysisStep.MatchingBenefits -> setOf(AnalysisStep.FetchingPayments)
                    AnalysisStep.BuildingReport -> setOf(
                        AnalysisStep.FetchingPayments,
                        AnalysisStep.MatchingBenefits,
                    )
                },
                savingAmount = when (targetStep) {
                    AnalysisStep.FetchingPayments -> state.savingAmount
                    AnalysisStep.MatchingBenefits -> maxOf(state.savingAmount, MATCHING_STEP_PLACEHOLDER_SAVING)
                    AnalysisStep.BuildingReport -> maxOf(state.savingAmount, REPORT_STEP_PLACEHOLDER_SAVING)
                },
            )
        }
    }

    private suspend fun awaitMinimumVisibleStepDuration(
        tracker: VisibleStepTracker,
    ) {
        val elapsed = SystemClock.elapsedRealtime() - tracker.startedAtElapsedMs
        val remaining = (MIN_STEP_DURATION_MS - elapsed).coerceAtLeast(0L)
        if (remaining > 0L) {
            delay(remaining)
        }
    }

    private fun AnalysisPipelinePhase.toVisibleAnalysisStep(): AnalysisStep {
        return when (this) {
            AnalysisPipelinePhase.PaymentBootstrap,
            AnalysisPipelinePhase.CatalogPreload,
            -> AnalysisStep.FetchingPayments

            AnalysisPipelinePhase.UsageStatsCollection,
            AnalysisPipelinePhase.Normalization,
            AnalysisPipelinePhase.CandidateExtraction,
            AnalysisPipelinePhase.FirstPassFilter,
            -> AnalysisStep.MatchingBenefits

            AnalysisPipelinePhase.BatchLookup,
            AnalysisPipelinePhase.AiResolver,
            AnalysisPipelinePhase.HandoffPersisted,
            -> AnalysisStep.BuildingReport
        }
    }

    private data class VisibleStepTracker(
        var currentStep: AnalysisStep,
        var startedAtElapsedMs: Long,
    )
}
