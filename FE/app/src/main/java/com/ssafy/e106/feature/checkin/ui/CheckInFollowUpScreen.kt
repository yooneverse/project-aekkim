package com.ssafy.e106.feature.checkin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.checkin.CheckInFollowUpUiState

/**
 * SCR-007-2 체크인 후속 권유 화면.
 *
 * 레이아웃 (화면명세서 기준):
 * - 상단바: 뒤로가기 아이콘
 * - 중앙: 일러스트 대체 이모지 + 메인 헤드라인 + 안내 문구 (금액 강조)
 * - 하단: Primary CTA (아끼러 가기) + Secondary CTA (조금 더 유지할게요)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInFollowUpScreen(
    uiState: CheckInFollowUpUiState,
    onGoToCancelGuide: () -> Unit,
    onKeepSubscription: () -> Unit,
    onNavigateBack: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding),
        ) {
            when {
                uiState.isLoading -> {
                    AppLoadingState(
                        title = "정보를 불러오는 중이에요",
                        description = "잠시만 기다려 주세요",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.error != null -> {
                    AppErrorState(
                        title = "정보를 불러오지 못했어요",
                        description = uiState.error,
                        onRetryClick = onRetryLoad,
                        secondaryActionText = "대시보드 홈으로 이동",
                        onSecondaryActionClick = onNavigateBack,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    FollowUpContent(
                        serviceName = uiState.serviceName,
                        monthlyPrice = uiState.monthlyPrice,
                        isNavigating = uiState.isNavigating,
                        onGoToCancelGuide = onGoToCancelGuide,
                        onKeepSubscription = onKeepSubscription,
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowUpContent(
    serviceName: String,
    monthlyPrice: Int,
    isNavigating: Boolean,
    onGoToCancelGuide: () -> Unit,
    onKeepSubscription: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val hasAmount = monthlyPrice > 0
    val formattedAmount = if (hasAmount) String.format("%,d", monthlyPrice) else null

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── 중앙 콘텐츠 (가용 공간 채움, 세로 중앙 정렬) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 일러스트 대체 이모지
            Text(
                text = "\uD83D\uDCB8",
                fontSize = 64.sp,
            )

            Spacer(modifier = Modifier.height(spacing.space6))

            // 메인 헤드라인
            Text(
                text = "$serviceName\n잠시 쉬어갈까요?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(spacing.space4))

            // 안내 문구 (금액 강조)
            if (hasAmount && formattedAmount != null) {
                val bodyText = buildAnnotatedString {
                    append("이번 달에는 거의 사용하지 않았어요.\n지금 해지하면 다음달에 ")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append("${formattedAmount}원")
                    }
                    append("을 아낄 수 있어요!")
                }
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = "이번 달에는 거의 사용하지 않았어요.\n절약 금액을 불러오지 못했어요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ── CTA 버튼 영역 (하단 고정) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.space8),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            // Primary CTA
            AppButton(
                text = if (formattedAmount != null) "${formattedAmount}원 아끼러 가기" else "아끼러 가기",
                onClick = onGoToCancelGuide,
                enabled = !isNavigating,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
            )

            // Secondary CTA
            AppButton(
                text = "조금 더 유지할게요",
                onClick = onKeepSubscription,
                enabled = !isNavigating,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
            )
        }
    }
}
