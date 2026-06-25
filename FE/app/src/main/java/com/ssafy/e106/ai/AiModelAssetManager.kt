package com.ssafy.e106.ai

import android.content.Context
import android.util.Log
import java.io.File

/**
 * [AI 모델 교체] FastText + XGBoost 온디바이스 파이프라인 지원을 위해 신규 생성 (2026-03-25)
 *
 * 역할: assets/ml/ 하위에 번들된 4개 모델 파일을 런타임에 filesDir/models/로 복사하고 경로를 반환한다.
 * 캐시된 파일이 이미 존재하면 복사를 스킵한다.
 */
object AiModelAssetManager {

    private const val TAG_ASSET = "AEKKIM_AI_ASSET"
    private const val ASSET_DIR = "ml"
    private const val MODEL_DIR = "models"

    data class ModelPaths(
        val ftModelPath: String,
        val xgbModelPath: String,
        val featureMapPath: String,
        val labelMapPath: String,
    )

    /**
     * assets/ml/{assetFileName} → filesDir/models/{assetFileName} 로 복사한다.
     * 이미 존재하면 스킵한다.
     * @return 복사된(또는 기존) 파일의 절대 경로
     */
    fun copyIfNeeded(context: Context, assetFileName: String): String {
        val modelsDir = File(context.filesDir, MODEL_DIR).also { it.mkdirs() }
        val destFile = File(modelsDir, assetFileName)

        // assets 원본 크기 확인 (openFd 우선, 압축 파일은 open().available() 폴백)
        val assetLength: Long = try {
            context.assets.openFd("$ASSET_DIR/$assetFileName").use { it.length }
        } catch (e: Exception) {
            // available()은 차단 없이 읽을 수 있는 바이트 수를 반환하며
            // 전체 스트림 길이와 다를 수 있다.
            // 모델 파일은 build.gradle의 noCompress 설정으로 비압축 번들되므로
            // openFd()가 정상 동작하는 한 이 폴백이 실행될 가능성은 낮다.
            context.assets.open("$ASSET_DIR/$assetFileName").use { it.available().toLong() }
        }

        if (destFile.exists() && destFile.length() == assetLength) {
            Log.d(TAG_ASSET, "$assetFileName 캐시 히트: dest=${destFile.length()}B ✓")
            return destFile.absolutePath
        }

        if (destFile.exists() && destFile.length() != assetLength) {
            Log.w(TAG_ASSET,
                "$assetFileName 캐시 크기 불일치 (dest=${destFile.length()}B, asset=${assetLength}B) — 재복사")
            destFile.delete()
        }

        Log.i(TAG_ASSET, "모델 복사 시작: $assetFileName")
        try {
            context.assets.open("$ASSET_DIR/$assetFileName").use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            val destLength = destFile.length()
            // Step 1 & 4: 복사 후 크기 검증
            if (destLength != assetLength) {
                Log.e(TAG_ASSET, "$assetFileName 복사 불일치: assets=${assetLength}B, dest=${destLength}B ← 잘림 의심")
                destFile.delete()
                throw IllegalStateException("복사 불완전: $assetFileName 삭제 후 재시도 필요")
            }
            Log.d(TAG_ASSET, "$assetFileName 복사 완료: assets=${assetLength}B, dest=${destLength}B ✓")
        } catch (e: Exception) {
            Log.e(TAG_ASSET, "모델 복사 실패: $assetFileName", e)
            throw e
        }

        return destFile.absolutePath
    }

    /**
     * 4개 모델 파일 전체를 copyIfNeeded로 복사하고 경로를 담은 ModelPaths를 반환한다.
     *
     * 파일 목록:
     *   ft_model.bin          — FastText 바이너리 모델 (ft_model.bin 직접 로드 방식)
     *   xgb_model_android.json — XGBoost JSON 모델
     *   feature_map.json      — XGBoost 피처 맵
     *   label_map.json        — XGBoost 레이블 맵
     *
     * assets 교체 안내:
     *   추가 필요: app/src/main/assets/ml/ft_model.bin  (Google Drive aekkim_models/ 에서 복사)
     *   제거 가능: app/src/main/assets/ml/input_matrix.npy
     *   제거 가능: app/src/main/assets/ml/output_matrix.npy
     *   제거 가능: app/src/main/assets/ml/ft_labels.json
     */
    fun ensureAllModels(context: Context): ModelPaths {
        val ftModelPath    = copyIfNeeded(context, "ft_model.bin")
        val xgbModelPath   = copyIfNeeded(context, "xgb_model_android.json")
        val featureMapPath = copyIfNeeded(context, "feature_map.json")
        val labelMapPath   = copyIfNeeded(context, "label_map.json")

        val paths = ModelPaths(
            ftModelPath    = ftModelPath,
            xgbModelPath   = xgbModelPath,
            featureMapPath = featureMapPath,
            labelMapPath   = labelMapPath,
        )
        Log.i(TAG_ASSET, "전체 모델 자산 준비 완료: $paths")
        return paths
    }
}
