package com.ssafy.e106.feature.auth.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.e106.R
import com.ssafy.e106.core.ui.theme.AekkimTheme

enum class SocialLoginProvider {
    Google,
    Kakao,
    Naver,
}

@Composable
fun SocialLoginButton(
    provider: SocialLoginProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val style = socialLoginStyle(provider)
    val shape = MaterialTheme.shapes.large

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (style.borderColor != null) {
                    Modifier.border(width = 1.dp, color = style.borderColor, shape = shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = style.containerColor,
            contentColor = style.contentColor,
            disabledContainerColor = style.containerColor.copy(alpha = 0.6f),
            disabledContentColor = style.contentColor.copy(alpha = 0.6f),
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = style.contentColor,
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = style.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(20.dp)
                        .align(Alignment.CenterStart),
                    contentScale = ContentScale.Fit,
                )

                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class SocialLoginStyle(
    val label: String,
    @DrawableRes val iconRes: Int,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color?,
)

@Composable
private fun socialLoginStyle(provider: SocialLoginProvider): SocialLoginStyle = when (provider) {
    SocialLoginProvider.Google -> SocialLoginStyle(
        label = "Google로 시작하기",
        iconRes = R.drawable.ic_logo_google,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = MaterialTheme.colorScheme.outline,
    )

    SocialLoginProvider.Kakao -> SocialLoginStyle(
        label = "카카오로 시작하기",
        iconRes = R.drawable.ic_logo_kakao,
        containerColor = Color(0xFFFEE500),
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = null,
    )

    SocialLoginProvider.Naver -> SocialLoginStyle(
        label = "네이버로 시작하기",
        iconRes = R.drawable.ic_logo_naver,
        containerColor = Color(0xFF03C75A),
        contentColor = Color.White,
        borderColor = null,
    )
}

@Preview(showBackground = true)
@Composable
private fun SocialLoginButtonPreview() {
    AekkimTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(17.dp),
            ) {
                SocialLoginButton(provider = SocialLoginProvider.Google, onClick = {})
                SocialLoginButton(provider = SocialLoginProvider.Kakao, onClick = {})
                SocialLoginButton(provider = SocialLoginProvider.Naver, onClick = {}, loading = true)
            }
        }
    }
}
