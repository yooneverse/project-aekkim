package com.ssafy.e106.ai

// LiteRT(Gemma3) 모델 다운로더. 현재 비활성화됨.
// FastText+XGBoost 파이프라인 전환 후 모든 호출부가 제거됨.
// 롤백이 필요한 경우 AiInterface.initializeNewPipeline()에서 재호출할 것.

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState() // 0~100
    data class Done(val modelPath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object ModelDownloader {

    private const val TAG = "ModelDownloader"
    private const val MODEL_ASSETS_PATH = "ml/gemma3-270m-it-q8.litertlm"
    private const val MODEL_FILE_NAME = "gemma3-270m-it-q8.litertlm"
    private const val HUGGING_FACE_URL =
        "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.litertlm"
    private const val MAX_RETRY = 3
    private const val BUFFER_SIZE = 8 * 1024 // 8KB

    fun downloadIfNeeded(context: Context): Flow<DownloadState> = flow {
        val modelsDir = File(context.filesDir, "models").also { it.mkdirs() }
        val modelFile = File(modelsDir, MODEL_FILE_NAME)

        // 이미 다운로드된 경우 스킵
        if (modelFile.exists() && modelFile.length() > 0) {
            Log.d(TAG, "모델 파일 이미 존재 — 다운로드 스킵: ${modelFile.absolutePath}")
            emit(DownloadState.Done(modelFile.absolutePath))
            return@flow
        }

        val tmpFile = File(modelsDir, "$MODEL_FILE_NAME.tmp")

        // assets/ml/ 에 번들된 모델이 있으면 filesDir로 복사 후 Done 반환
        val bundledInAssets = try {
            (context.assets.list("ml") ?: emptyArray()).contains(MODEL_FILE_NAME)
        } catch (_: Exception) { false }
        if (bundledInAssets) {
            Log.d(TAG, "assets 번들 모델 발견 — filesDir로 복사 시작")
            emit(DownloadState.Downloading(0))
            try {
                context.assets.open(MODEL_ASSETS_PATH).use { input ->
                    tmpFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } >= 0) {
                            output.write(buffer, 0, bytes)
                        }
                    }
                }
                Log.d(TAG, "다운로드 완료 (100%)")
                tmpFile.renameTo(modelFile)
                Log.d(TAG, "모델 저장 완료: ${modelFile.absolutePath}")
                emit(DownloadState.Done(modelFile.absolutePath))
                return@flow
            } catch (e: Exception) {
                Log.e(TAG, "assets 복사 실패: ${e.message}")
                tmpFile.delete()
            }
        }

        emit(DownloadState.Idle)
        emit(DownloadState.Downloading(0))
        Log.d(TAG, "다운로드 시작")

        var lastError: Exception? = null
        var delayMs = 1000L

        for (attempt in 1..MAX_RETRY) {
            try {
                val connection = (URL(HUGGING_FACE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    connect()
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
                var downloadedBytes = 0L

                connection.inputStream.use { input ->
                    tmpFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } >= 0) {
                            output.write(buffer, 0, bytes)
                            downloadedBytes += bytes
                            if (totalBytes > 0) {
                                val progress = (downloadedBytes * 100 / totalBytes).toInt()
                                emit(DownloadState.Downloading(progress))
                            }
                        }
                    }
                }
                Log.d(TAG, "다운로드 완료 (100%)")

                // 완료 후 임시 파일을 최종 경로로 rename (불완전 파일 방지)
                tmpFile.renameTo(modelFile)
                Log.d(TAG, "모델 저장 완료: ${modelFile.absolutePath}")
                emit(DownloadState.Done(modelFile.absolutePath))
                return@flow

            } catch (e: Exception) {
                lastError = e
                Log.d(TAG, "다운로드 실패 (attempt=$attempt): ${e.message}")
                tmpFile.delete()
                if (attempt < MAX_RETRY) {
                    delay(delayMs)
                    delayMs *= 2 // 지수 백오프: 1s → 2s → 4s
                }
            }
        }

        Log.e(TAG, "최대 재시도 초과, 다운로드 실패: ${lastError?.message}")
        emit(DownloadState.Error(lastError?.message ?: "Unknown download error"))
    }.flowOn(Dispatchers.IO)
}
