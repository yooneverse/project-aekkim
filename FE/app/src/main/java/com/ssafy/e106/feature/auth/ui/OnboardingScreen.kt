package com.ssafy.e106.feature.auth.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.ssafy.e106.feature.auth.AuthIntent
import com.ssafy.e106.feature.auth.AuthUiEffect
import com.ssafy.e106.feature.auth.AuthUiState
import com.ssafy.e106.feature.auth.AuthViewModel
import com.ssafy.e106.feature.auth.google.GoogleLoginLauncher
import com.ssafy.e106.feature.auth.google.GoogleLoginResult
import com.ssafy.e106.feature.auth.google.rememberGoogleSignInLauncher
import com.ssafy.e106.feature.auth.kakao.KakaoLoginLauncher
import com.ssafy.e106.feature.auth.kakao.KakaoLoginResult
import com.ssafy.e106.feature.auth.kakao.rememberKakaoLoginLauncher
import com.ssafy.e106.feature.auth.ui.component.AuthFlowScaffold
import com.ssafy.e106.feature.auth.ui.component.AuthHeroSection
import com.ssafy.e106.feature.auth.ui.component.AuthHeroVariant
import com.ssafy.e106.feature.auth.ui.component.SocialLoginButton
import com.ssafy.e106.feature.auth.ui.component.SocialLoginProvider
import kotlinx.coroutines.flow.collectLatest

private const val ONBOARDING_HERO_ASSET = "onboarding/sherlock_hamzzi_onboarding.png"

@Composable
fun OnboardingRoute(
    onNavigateToTerms: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    googleLoginLauncher: GoogleLoginLauncher = rememberGoogleSignInLauncher(),
    kakaoLoginLauncher: KakaoLoginLauncher = rememberKakaoLoginLauncher(),
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is AuthUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                AuthUiEffect.LaunchGoogleLogin -> {
                    when (val result = googleLoginLauncher.launch()) {
                        is GoogleLoginResult.Success -> {
                            viewModel.onIntent(AuthIntent.GoogleLoginSucceeded(result.googleIdToken))
                        }

                        GoogleLoginResult.Cancelled -> {
                            viewModel.onIntent(AuthIntent.GoogleLoginCancelled)
                        }

                        is GoogleLoginResult.Failure -> {
                            viewModel.onIntent(AuthIntent.GoogleLoginFailed(result.message))
                        }
                    }
                }

                AuthUiEffect.LaunchKakaoLogin -> {
                    when (val result = kakaoLoginLauncher.launch()) {
                        is KakaoLoginResult.Success -> {
                            viewModel.onIntent(AuthIntent.KakaoLoginSucceeded(result.kakaoAccessToken))
                        }

                        KakaoLoginResult.Cancelled -> {
                            viewModel.onIntent(AuthIntent.KakaoLoginCancelled)
                        }

                        is KakaoLoginResult.Failure -> {
                            viewModel.onIntent(
                                AuthIntent.KakaoLoginFailed(
                                    errorType = result.errorType,
                                    message = result.message,
                                ),
                            )
                        }
                    }
                }

                AuthUiEffect.NavigateToTerms -> onNavigateToTerms()
                AuthUiEffect.NavigateToDashboard -> onNavigateToDashboard()
                AuthUiEffect.NavigateToAnalysis -> Unit
                AuthUiEffect.RequestNotificationPermission -> Unit
                AuthUiEffect.OpenUsageAccessSettings -> Unit
                is AuthUiEffect.OpenConsentDetail -> Unit
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onGoogleLoginClick = {
            viewModel.onIntent(AuthIntent.GoogleLoginClicked)
        },
        onKakaoLoginClick = {
            viewModel.onIntent(AuthIntent.KakaoLoginClicked)
        },
        onNaverLoginClick = {
            Toast.makeText(context, "해당 로그인은 곧 지원될 예정이에요", Toast.LENGTH_SHORT).show()
        },
    )
}

@Composable
fun OnboardingScreen(
    uiState: AuthUiState,
    onGoogleLoginClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    onNaverLoginClick: () -> Unit,
) {
    AuthFlowScaffold(
        centerBody = true,
        bodyHorizontalAlignment = Alignment.CenterHorizontally,
        body = {
            AuthHeroSection(
                title = "흩어진 구독료,\n한 번에 모아보기",
                subtitle = "매달 얼마 나가는지 파악하고\n스마트하게 아껴보세요.",
                variant = AuthHeroVariant.Display,
                horizontalAlignment = Alignment.CenterHorizontally,
                textAlign = TextAlign.Center,
                topContent = { OnboardingHeroImage() },
            )
        },
        bottomContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(17.dp),
            ) {
                SocialLoginButton(
                    provider = SocialLoginProvider.Google,
                    onClick = onGoogleLoginClick,
                    enabled = !uiState.isSocialLoginLoading,
                    loading = uiState.isGoogleLoading,
                )

                SocialLoginButton(
                    provider = SocialLoginProvider.Kakao,
                    onClick = onKakaoLoginClick,
                    enabled = !uiState.isSocialLoginLoading,
                    loading = uiState.isKakaoLoading,
                )

                SocialLoginButton(
                    provider = SocialLoginProvider.Naver,
                    onClick = onNaverLoginClick,
                    enabled = !uiState.isSocialLoginLoading,
                )
            }
        },
    )
}

@Composable
private fun OnboardingHeroImage(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/$ONBOARDING_HERO_ASSET")
            .crossfade(true)
            .build(),
    )

    Image(
        painter = painter,
        contentDescription = "온보딩 탐정 햄찌 일러스트",
        modifier = modifier
            .fillMaxWidth(0.58f)
            .heightIn(max = 196.dp),
        contentScale = ContentScale.Fit,
    )
}
