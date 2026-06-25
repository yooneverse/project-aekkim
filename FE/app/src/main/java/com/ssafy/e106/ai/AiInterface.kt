package com.ssafy.e106.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [AI 모델 교체] FastText + XGBoost 파이프라인으로 교체 (2026-03-25)
 *
 * 변경 내용:
 *   1. FastTextEngine, XgboostEngine 싱글톤 인스턴스 추가
 *   2. initializeNewPipeline(context) 함수 추가 — 멱등, 이미 초기화된 경우 스킵
 *   3. 기존 LiteRtEngine 초기화 코드 주석 처리 (REPLACED_BY_FASTTEXT_XGBOOST)
 *   4. generateText()는 레거시 호환용으로만 유지 — OnDeviceAiResolver.resolve()에 의해 우회됨
 */
object AiInterface {

    private const val TAG_AI = "AEKKIM_AI_IF"

    // ── [신규] FastText + XGBoost 파이프라인 엔진 ─────────────────────
    val fastTextEngine = FastTextEngine()
    val xgboostEngine = XgboostEngine()
    private val pipelineMutex = Mutex()
    private var pipelineInitialized = false

    // ── [기존 — REPLACED_BY_FASTTEXT_XGBOOST] LiteRT 엔진 (비활성화) ──
    // private val engine = LiteRtEngine()
    private val prepareMutex = Mutex()
    // private var readyModelPath: String? = null

    // ── [신규] FastText + XGBoost 파이프라인 초기화 ───────────────────

    /**
     * FastText + XGBoost 파이프라인을 초기화한다.
     * 멱등(idempotent) — 이미 초기화된 경우 스킵한다.
     */
    suspend fun initializeNewPipeline(context: Context) {
        pipelineMutex.withLock {
            if (pipelineInitialized) {
                Log.d(TAG_AI, "FastText+XGBoost 파이프라인 이미 초기화됨 — 스킵")
                return
            }

            Log.i(TAG_AI, "FastText+XGBoost 파이프라인 초기화 시작")
            withContext(Dispatchers.IO) {
                val paths = AiModelAssetManager.ensureAllModels(context)
                fastTextEngine.initialize(paths.ftModelPath)
                xgboostEngine.initialize(paths.xgbModelPath)
            }
            pipelineInitialized = true
            Log.i(TAG_AI, "FastText+XGBoost 파이프라인 초기화 완료")
        }
    }

    // ── [기존 — REPLACED_BY_FASTTEXT_XGBOOST] LiteRT 관련 함수 ──────

    /**
     * REPLACED_BY_FASTTEXT_XGBOOST: LiteRT 엔진 비활성화
     * 기존 initEngine은 LiteRtEngine을 초기화했으나, 신규 파이프라인으로 대체되었다.
     * 함수 자체는 레거시 호환을 위해 시그니처 유지.
     */
    suspend fun initEngine(context: Context, modelPath: String) {
        Log.w(TAG_AI, "initEngine() 호출됨 — 현재 비활성화 상태 (FastText+XGBoost 사용 중)")
        // REPLACED_BY_FASTTEXT_XGBOOST: 기존 LiteRT 초기화 코드
        // Log.d(TAG, "initEngine 시작 — modelPath=$modelPath")
        // engine.initialize(context, modelPath)
        // readyModelPath = modelPath
        // Log.d(TAG, "initEngine 완료")
    }

    /**
     * 이 함수는 OnDeviceAiResolver.resolve()에 의해 우회됨.
     * OnDeviceAiResolver는 FastTextEngine → XgboostEngine 직접 경로를 사용하며,
     * 이 함수를 호출하지 않는다.
     * 직접 호출될 경우를 대비해 빈 JSON 배열 "[]"을 반환한다.
     *
     * REPLACED_BY_FASTTEXT_XGBOOST: 기존 LiteRT generateText 로직 비활성화
     */
    suspend fun generateText(context: Context, prompt: String): String {
        Log.w(TAG_AI, "generateText() 직접 호출 감지 — 신규 파이프라인 우회 경로")
        // REPLACED_BY_FASTTEXT_XGBOOST: 기존 LiteRT 호출 코드
        // prepareMutex.withLock {
        //     val modelPath = resolveModelPath(context)
        //     if (readyModelPath != modelPath || getEngineState().value !is EngineState.Ready) {
        //         Log.d(TAG, "엔진 미준비 — initEngine 재시도")
        //         initEngine(context, modelPath)
        //     }
        // }
        // return engine.generateText(prompt)
        return "[]"
    }

    /**
     * REPLACED_BY_FASTTEXT_XGBOOST: LiteRT 엔진 상태 반환 (비활성화)
     * 레거시 호환을 위해 함수 자체는 유지하되 dummy StateFlow 반환.
     */
    fun getEngineState(): StateFlow<EngineState> {
        // REPLACED_BY_FASTTEXT_XGBOOST: engine.state
        // return engine.state
        return kotlinx.coroutines.flow.MutableStateFlow(EngineState.Idle)
    }

    /**
     * REPLACED_BY_FASTTEXT_XGBOOST: LiteRT 엔진 해제 (비활성화)
     */
    fun release() {
        Log.w(TAG_AI, "release() 호출됨 — LiteRT 비활성화 상태 (FastText+XGBoost 사용 중)")
        // REPLACED_BY_FASTTEXT_XGBOOST: 기존 LiteRT 해제 코드
        // readyModelPath = null
        // engine.close()
    }

    // ── [기존 — REPLACED_BY_FASTTEXT_XGBOOST] 모델 경로 해석 ─────────
    // private suspend fun resolveModelPath(context: Context): String {
    //     Log.d(TAG, "resolveModelPath 시작")
    //     var resolvedPath: String? = null
    //     var errorMessage: String? = null
    //
    //     downloadModelIfNeeded(context).collect { state ->
    //         when (state) {
    //             is DownloadState.Done -> {
    //                 resolvedPath = state.modelPath
    //                 Log.d(TAG, "resolveModelPath — 모델 경로 확보: $resolvedPath")
    //             }
    //             is DownloadState.Error -> {
    //                 errorMessage = state.message
    //                 Log.e(TAG, "resolveModelPath — 다운로드 에러: $errorMessage")
    //             }
    //             DownloadState.Idle,
    //             is DownloadState.Downloading,
    //             -> Unit
    //         }
    //     }
    //
    //     return resolvedPath ?: run {
    //         Log.e(TAG, "resolveModelPath — 모델 경로 확보 실패")
    //         throw IllegalStateException(errorMessage ?: "Failed to download on-device AI model")
    //     }
    // }
}
