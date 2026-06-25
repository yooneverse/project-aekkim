/**
 * REPLACED_BY_FASTTEXT_XGBOOST (2026-03-25)
 * 이 엔진은 FastText + XGBoost 파이프라인으로 대체되었습니다.
 * AiInterface.initializeNewPipeline() 및 OnDeviceAiResolver 수정 내역 참조.
 * 코드는 롤백 대비용으로 보존합니다.
 */
package com.ssafy.e106.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend

import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "LiteRtEngine"

sealed class EngineState {
    data object Idle : EngineState()
    data object Loading : EngineState()
    data object Ready : EngineState()
    data class Error(val message: String) : EngineState()
}

class LiteRtEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private var engine: Engine? = null
    private var initializedModelPath: String? = null
    private val generationMutex = Mutex()

    suspend fun initialize(context: Context, modelPath: String) {
        Log.w("AEKKIM_AI_LITERT", "LiteRtEngine.initialize() 호출됨 — 현재 비활성화 상태 (FastText+XGBoost 사용 중)")
        Log.d(TAG, "initialize 시작 — modelPath=$modelPath")
        if (engine != null && initializedModelPath == modelPath && _state.value is EngineState.Ready) {
            Log.d(TAG, "이미 초기화됨 — 스킵")
            return
        }

        _state.value = EngineState.Loading
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir.path
                engine?.close()

                val newEngine = try {
                    Log.d(TAG, "GPU 백엔드로 엔진 생성 시도")
                    Engine(
                        EngineConfig(
                            modelPath = modelPath,
                            backend = Backend.GPU(),
                            cacheDir = cacheDir,
                        ),
                    )
                } catch (_: Exception) {
                    Log.d(TAG, "GPU 백엔드 실패 — CPU 폴백")
                    Engine(
                        EngineConfig(
                            modelPath = modelPath,
                            backend = Backend.CPU(),
                            cacheDir = cacheDir,
                        ),
                    )
                }

                newEngine.initialize()
                engine = newEngine
                initializedModelPath = modelPath
                Log.d(TAG, "엔진 초기화 완료 — EngineState.Ready")
                _state.value = EngineState.Ready
            } catch (e: Exception) {
                Log.e(TAG, "엔진 초기화 실패: ${e.message}")
                engine = null
                initializedModelPath = null
                _state.value = EngineState.Error(e.message ?: "Failed to initialize engine")
            }
        }
    }

    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "generateText 시작")
        val readyEngine = engine ?: throw IllegalStateException("Engine is not initialized")
        val result = generationMutex.withLock {
            readyEngine.createConversation(ConversationConfig()).use { conversation ->
                conversation.sendMessage(prompt).toString().trim()
            }
        }
        Log.d(TAG, "generateText 완료")
        result
    }

    fun close() {
        Log.d(TAG, "엔진 리소스 해제")
        engine?.close()
        engine = null
        initializedModelPath = null
        _state.value = EngineState.Idle
    }
}
