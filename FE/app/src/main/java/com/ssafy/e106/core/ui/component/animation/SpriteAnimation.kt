package com.ssafy.e106.core.ui.component.animation

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * assets/<[assetFolder]> 안의 프레임 이미지를 순서대로 재생하는 스프라이트 애니메이션 Composable.
 *
 * 프레임 로딩은 [Dispatchers.IO]에서 비동기로 수행되므로 메인 스레드를 블로킹하지 않습니다.
 * 로딩이 완료되기 전까지는 아무것도 렌더링하지 않습니다.
 *
 * ## 사용법
 * 1. `assets/animations/hamster_run/` 폴더에 프레임 이미지를 추가합니다.
 *    - 파일 이름을 알파벳/숫자 순으로 정렬 가능하게 지정합니다.
 *    - 예: `frame_00.webp`, `frame_01.webp`, ..., `frame_11.webp`
 * 2. Composable에서 호출합니다:
 *    ```kotlin
 *    SpriteAnimation(
 *        assetFolder = "animations/hamster_run",
 *        size = 120.dp,
 *        frameIntervalMs = 80L,
 *    )
 *    ```
 *
 * @param assetFolder assets 내 프레임 이미지가 있는 폴더 경로
 * @param modifier Composable modifier
 * @param size 애니메이션 표시 크기 (정사각형)
 * @param frameIntervalMs 프레임 간 간격 (ms). 작을수록 빠른 애니메이션
 * @param contentDescription 접근성 설명
 */
@Composable
fun SpriteAnimation(
    assetFolder: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    frameIntervalMs: Long = 100L,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val frames by produceState<List<ImageBitmap>>(
        initialValue = emptyList(),
        key1 = assetFolder,
    ) {
        value = withContext(Dispatchers.IO) {
            loadFramesFromAssets(context, assetFolder)
        }
    }

    if (frames.isEmpty()) return

    var currentFrame by remember { mutableIntStateOf(0) }

    LaunchedEffect(frames.size, frameIntervalMs) {
        while (true) {
            delay(frameIntervalMs)
            currentFrame = (currentFrame + 1) % frames.size
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = frames[currentFrame],
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * assets 폴더에서 이미지 프레임을 정렬 순서대로 로드합니다.
 * 지원 형식: PNG, WebP, JPG
 *
 * **주의**: 이 함수는 파일 I/O와 비트맵 디코딩을 수행하므로
 * 반드시 [Dispatchers.IO] 등 백그라운드 스레드에서 호출해야 합니다.
 */
private fun loadFramesFromAssets(
    context: Context,
    folderPath: String,
): List<ImageBitmap> = runCatching {
    context.assets.list(folderPath)
        .orEmpty()
        .filter { name ->
            name.endsWith(".png", ignoreCase = true) ||
                name.endsWith(".webp", ignoreCase = true) ||
                name.endsWith(".jpg", ignoreCase = true)
        }
        .sorted()
        .mapNotNull { fileName ->
            context.assets.open("$folderPath/$fileName").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
}.getOrElse { emptyList() }
