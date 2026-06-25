package com.ssafy.e106.feature.dashboard.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun DashboardHeaderHamster(
    hasUnreadNotifications: Boolean,
    modifier: Modifier = Modifier,
) {
    val walkConfig = remember {
        SpriteSheetConfig(
            assetPath = "dashboard/hamster/walk_right_strip.png",
            frameCount = 10,
            fps = 10,
            frameRects = WALK_FRAME_RECTS,
        )
    }
    val inspectConfig = remember {
        SpriteSheetConfig(
            assetPath = "dashboard/hamster/inspect_right_strip.png",
            frameCount = 6,
            fps = 8,
            columns = 6,
            rows = 1,
        )
    }
    val glanceConfig = remember {
        SpriteSheetConfig(
            assetPath = "dashboard/hamster/glance_front_strip.png",
            frameCount = 4,
            fps = 4,
            columns = 4,
            rows = 1,
        )
    }
    val moneyThrowConfig = remember {
        SpriteSheetConfig(
            assetPath = "dashboard/hamster/money_throw_front_sheet.png",
            frameCount = 10,
            fps = MONEY_THROW_FPS,
            frameRects = MONEY_THROW_FRAME_RECTS,
        )
    }

    val walkFrames = rememberSpriteFrames(walkConfig)
    val inspectFrames = rememberSpriteFrames(inspectConfig)
    val glanceFrames = rememberSpriteFrames(glanceConfig)
    val moneyThrowFrames = rememberSpriteFrames(moneyThrowConfig)
    val fallbackPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/sherlock_hamzzi.png")
            .crossfade(true)
            .build(),
    )

    val xProgress = remember { Animatable(0f) }
    var motion by remember { mutableStateOf(HamsterMotion.PauseRight) }
    var moneyThrowPlaybackToken by remember { mutableIntStateOf(0) }
    val hamsterInteractionSource = remember { MutableInteractionSource() }
    val idleFloat = rememberInfiniteTransition(label = "dashboardHamsterIdleFloat")
    val floatPhase by idleFloat.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dashboardHamsterFloatPhase",
    )

    LaunchedEffect(
        hasUnreadNotifications,
        walkFrames,
        inspectFrames,
        glanceFrames,
        moneyThrowFrames,
        moneyThrowPlaybackToken,
    ) {
        if (
            walkFrames.isEmpty() ||
            inspectFrames.isEmpty() ||
            glanceFrames.isEmpty()
        ) {
            return@LaunchedEffect
        }

        if (moneyThrowPlaybackToken > 0 && moneyThrowFrames.isNotEmpty()) {
            motion = HamsterMotion.MoneyThrow
            delay(loopDurationMillis(moneyThrowConfig, loops = 1))
        }

        if (hasUnreadNotifications) {
            xProgress.animateTo(
                targetValue = INSPECT_TARGET_PROGRESS,
                animationSpec = tween(durationMillis = 450, easing = LinearEasing),
            )
            while (true) {
                motion = HamsterMotion.InspectRight
                delay(loopDurationMillis(inspectConfig, loops = 2))
                motion = HamsterMotion.GlanceFront
                delay(loopDurationMillis(glanceConfig, loops = 1))
            }
        } else {
            while (true) {
                motion = HamsterMotion.WalkRight
                xProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = WALK_DURATION_MILLIS, easing = LinearEasing),
                )
                motion = HamsterMotion.PauseRight
                delay(IDLE_PAUSE_MILLIS)

                motion = HamsterMotion.WalkLeft
                xProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = WALK_DURATION_MILLIS, easing = LinearEasing),
                )
                motion = HamsterMotion.PauseLeft
                delay(IDLE_PAUSE_MILLIS)
            }
        }
    }

    val (frames, fps, mirror) = when (motion) {
        HamsterMotion.WalkRight -> Triple(walkFrames, walkConfig.fps, false)
        HamsterMotion.WalkLeft -> Triple(walkFrames, walkConfig.fps, true)
        HamsterMotion.PauseRight -> Triple(walkFrames.stillFrame(), 1, false)
        HamsterMotion.PauseLeft -> Triple(walkFrames.stillFrame(), 1, true)
        HamsterMotion.InspectRight -> Triple(inspectFrames, inspectConfig.fps, false)
        HamsterMotion.GlanceFront -> Triple(glanceFrames, glanceConfig.fps, false)
        HamsterMotion.MoneyThrow -> Triple(moneyThrowFrames, moneyThrowConfig.fps, false)
    }
    val playbackKey = when (motion) {
        HamsterMotion.MoneyThrow -> motion to moneyThrowPlaybackToken
        else -> motion
    }
    val frameIndex = rememberLoopingFrameIndex(
        frameCount = frames.size,
        fps = fps,
        playbackKey = playbackKey,
    )
    val currentFrame = frames.getOrNull(frameIndex)
    val bobOffset = when (motion) {
        HamsterMotion.WalkRight,
        HamsterMotion.WalkLeft,
        HamsterMotion.MoneyThrow,
        -> 0.dp

        else -> (sin(floatPhase * PI * 2).toFloat() * 1.8f).dp
    }
    val rotation = when (motion) {
        HamsterMotion.InspectRight -> 1.5f
        HamsterMotion.GlanceFront -> -1f
        else -> 0f
    }
    val visualScale = when (motion) {
        HamsterMotion.MoneyThrow -> MONEY_THROW_VISUAL_SCALE
        else -> 1f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clipToBounds(),
    ) {
        val maxTravel = (maxWidth - SPRITE_SIZE - TRACK_END_RESERVED).coerceAtLeast(0.dp)
        val xOffset = maxTravel * xProgress.value
        val hamsterModifier = Modifier
            .align(Alignment.BottomStart)
            .size(SPRITE_SIZE)
            .offset(x = xOffset, y = bobOffset)
            .clickable(
                interactionSource = hamsterInteractionSource,
                indication = null,
                onClick = { moneyThrowPlaybackToken += 1 },
            )

        if (currentFrame != null) {
            Image(
                bitmap = currentFrame,
                contentDescription = "Dashboard hamster",
                modifier = hamsterModifier
                    .graphicsLayer {
                        scaleX = if (mirror) -visualScale else visualScale
                        scaleY = visualScale
                        rotationZ = rotation
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                contentScale = ContentScale.Fit,
            )
        } else {
            Image(
                painter = fallbackPainter,
                contentDescription = "Dashboard hamster",
                modifier = hamsterModifier,
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun rememberSpriteFrames(config: SpriteSheetConfig): List<ImageBitmap> {
    val context = LocalContext.current
    val frames by produceState(
        initialValue = emptyList<ImageBitmap>(),
        key1 = config,
    ) {
        value = withContext(Dispatchers.IO) {
            loadSpriteFrames(
                context = context,
                config = config,
            )
        }
    }
    return frames
}

@Composable
private fun rememberLoopingFrameIndex(
    frameCount: Int,
    fps: Int,
    playbackKey: Any,
): Int {
    var frameIndex by remember(playbackKey, frameCount) { mutableIntStateOf(0) }

    LaunchedEffect(playbackKey, frameCount, fps) {
        frameIndex = 0
        if (frameCount <= 1 || fps <= 0) return@LaunchedEffect

        val frameDelayMillis = (1000f / fps).roundToInt().coerceAtLeast(1).toLong()
        while (true) {
            delay(frameDelayMillis)
            frameIndex = (frameIndex + 1) % frameCount
        }
    }

    return frameIndex
}

private fun loadSpriteFrames(
    context: Context,
    config: SpriteSheetConfig,
): List<ImageBitmap> {
    val sheet = context.assets.open(config.assetPath).use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    } ?: return emptyList()

    val croppedFrames = config.frameRects?.map { rect ->
        Bitmap.createBitmap(sheet, rect.left, rect.top, rect.width(), rect.height())
    } ?: run {
        val columns = config.columns ?: return emptyList()
        val rows = config.rows ?: return emptyList()
        val cellWidth = sheet.width / columns
        val cellHeight = sheet.height / rows
        val rawFrames = buildList(config.frameCount) {
            for (index in 0 until config.frameCount) {
                val column = index % columns
                val row = index / columns
                if (row >= rows) break

                add(
                    Bitmap.createBitmap(
                        sheet,
                        column * cellWidth,
                        row * cellHeight,
                        cellWidth,
                        cellHeight,
                    ),
                )
            }
        }
        if (rawFrames.isEmpty()) return emptyList()

        val cropRect = resolveSharedContentBounds(rawFrames)
        rawFrames.map { frame ->
            Bitmap.createBitmap(
                frame,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height(),
            )
        }
    }

    return normalizeFrames(croppedFrames)
}

private fun normalizeFrames(frames: List<Bitmap>): List<ImageBitmap> {
    if (frames.isEmpty()) return emptyList()

    val canvasSide = NORMALIZED_CANVAS_PX
    val contentLimit = (canvasSide * NORMALIZED_CONTENT_RATIO).roundToInt()
    val paint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }

    return frames.map { frame ->
        val scale = minOf(
            contentLimit.toFloat() / frame.width,
            contentLimit.toFloat() / frame.height,
        )
        val scaledWidth = (frame.width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (frame.height * scale).roundToInt().coerceAtLeast(1)
        val scaledBitmap = Bitmap.createScaledBitmap(frame, scaledWidth, scaledHeight, false)
        val normalized = Bitmap.createBitmap(canvasSide, canvasSide, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(normalized)
        val left = ((canvasSide - scaledWidth) / 2f).roundToInt()
        val top = (canvasSide - scaledHeight - NORMALIZED_BOTTOM_PADDING_PX).coerceAtLeast(0)
        canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), paint)
        normalized.asImageBitmap()
    }
}

private fun resolveSharedContentBounds(frames: List<Bitmap>): Rect {
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = Int.MIN_VALUE
    var bottom = Int.MIN_VALUE

    frames.forEach { frame ->
        val bounds = findContentBounds(frame) ?: return@forEach
        left = minOf(left, bounds.left)
        top = minOf(top, bounds.top)
        right = maxOf(right, bounds.right)
        bottom = maxOf(bottom, bounds.bottom)
    }

    if (left == Int.MAX_VALUE) {
        return Rect(0, 0, frames.first().width, frames.first().height)
    }

    return Rect(
        (left - SHARED_CROP_PADDING_PX).coerceAtLeast(0),
        (top - SHARED_CROP_PADDING_PX).coerceAtLeast(0),
        (right + SHARED_CROP_PADDING_PX).coerceAtMost(frames.first().width),
        (bottom + SHARED_CROP_PADDING_PX).coerceAtMost(frames.first().height),
    )
}

private fun findContentBounds(frame: Bitmap): Rect? {
    var left = frame.width
    var top = frame.height
    var right = 0
    var bottom = 0
    var hasContent = false

    for (y in 0 until frame.height) {
        for (x in 0 until frame.width) {
            val pixel = frame.getPixel(x, y)
            if (!pixel.isSpriteContent()) continue

            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x + 1)
            bottom = maxOf(bottom, y + 1)
            hasContent = true
        }
    }

    return if (hasContent) Rect(left, top, right, bottom) else null
}

private fun Int.isSpriteContent(): Boolean {
    val alpha = AndroidColor.alpha(this)
    if (alpha <= 12) return false

    return !(
        AndroidColor.red(this) >= 248 &&
            AndroidColor.green(this) >= 248 &&
            AndroidColor.blue(this) >= 248
        )
}

private fun List<ImageBitmap>.stillFrame(): List<ImageBitmap> {
    if (isEmpty()) return emptyList()
    return listOf(this[(size / 2).coerceIn(0, lastIndex)])
}

private fun loopDurationMillis(
    config: SpriteSheetConfig,
    loops: Int,
): Long {
    val singleLoopMillis = ((config.frameCount * 1000f) / config.fps).roundToInt().toLong()
    return singleLoopMillis * loops
}

private data class SpriteSheetConfig(
    val assetPath: String,
    val frameCount: Int,
    val fps: Int,
    val columns: Int? = null,
    val rows: Int? = null,
    val frameRects: List<Rect>? = null,
)

private enum class HamsterMotion {
    WalkRight,
    PauseRight,
    WalkLeft,
    PauseLeft,
    InspectRight,
    GlanceFront,
    MoneyThrow,
}

private val WALK_FRAME_RECTS = listOf(
    Rect(586, 71, 764, 249),
    Rect(848, 67, 1030, 253),
    Rect(42, 320, 228, 523),
    Rect(308, 320, 498, 524),
    Rect(582, 316, 768, 528),
    Rect(852, 320, 1034, 527),
    Rect(42, 569, 228, 776),
    Rect(312, 569, 498, 776),
    Rect(578, 565, 764, 772),
    Rect(852, 569, 1034, 777),
)

private val MONEY_THROW_FRAME_RECTS = listOf(
    paddedSheetRect(365, 61, 518, 263),
    paddedSheetRect(670, 91, 827, 263),
    paddedSheetRect(61, 378, 221, 568),
    paddedSheetRect(355, 373, 536, 637),
    paddedSheetRect(660, 362, 828, 572),
    paddedSheetRect(34, 673, 277, 886),
    paddedSheetRect(348, 638, 563, 886),
    paddedSheetRect(653, 646, 859, 885),
    paddedSheetRect(62, 994, 254, 1204),
    paddedSheetRect(304, 994, 519, 1205),
)

private fun paddedSheetRect(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
): Rect = Rect(
    (left - SHEET_FRAME_PADDING_PX).coerceAtLeast(0),
    (top - SHEET_FRAME_PADDING_PX).coerceAtLeast(0),
    (right + SHEET_FRAME_PADDING_PX).coerceAtMost(MONEY_THROW_SHEET_WIDTH_PX),
    (bottom + SHEET_FRAME_PADDING_PX).coerceAtMost(MONEY_THROW_SHEET_HEIGHT_PX),
)

private const val WALK_DURATION_MILLIS = 1600
private const val IDLE_PAUSE_MILLIS = 760L
private const val INSPECT_TARGET_PROGRESS = 1f
private const val MONEY_THROW_FPS = 12
private const val MONEY_THROW_VISUAL_SCALE = 1.14f
private const val NORMALIZED_CANVAS_PX = 240
private const val NORMALIZED_BOTTOM_PADDING_PX = 8
private const val SHARED_CROP_PADDING_PX = 6
private const val SHEET_FRAME_PADDING_PX = 8
private const val MONEY_THROW_SHEET_WIDTH_PX = 900
private const val MONEY_THROW_SHEET_HEIGHT_PX = 1275
private const val NORMALIZED_CONTENT_RATIO = 0.88f
private val SPRITE_SIZE = 56.dp
private val TRACK_END_RESERVED = 36.dp
