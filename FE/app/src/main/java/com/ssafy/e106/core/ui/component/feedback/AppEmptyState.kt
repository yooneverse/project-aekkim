package com.ssafy.e106.core.ui.component.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant

@Composable
fun AppEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    illustration: (@Composable () -> Unit)? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 40.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (illustration != null) {
                illustration()
                Spacer(modifier = Modifier.height(4.dp))
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionText != null && onActionClick != null) {
                AppButton(
                    text = actionText,
                    onClick = onActionClick,
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Medium,
                )
            }
        }
    }
}
