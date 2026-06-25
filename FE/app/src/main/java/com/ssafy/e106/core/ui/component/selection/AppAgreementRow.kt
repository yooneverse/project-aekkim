package com.ssafy.e106.core.ui.component.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.e106.core.ui.theme.AekkimTheme
import com.ssafy.e106.core.ui.theme.LocalSpacing

@Composable
fun AppAgreementRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    pressFeedbackEnabled: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val rowBackground = Color.Transparent
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconBackground = if (checked) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.Transparent
    }
    val iconBorder = if (checked) {
        null
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetBackground = if (enabled && pressFeedbackEnabled && isPressed) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    } else {
        rowBackground
    }
    val animatedBackground by animateColorAsState(targetValue = targetBackground, label = "agreementRowBackground")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(animatedBackground)
            .semantics {
                stateDescription = if (checked) "선택됨" else "선택 안 됨"
            }
            .defaultMinSize(minHeight = 52.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .padding(horizontal = spacing.space4, vertical = spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = iconBackground,
            border = iconBorder,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (checked || emphasized) FontWeight.SemiBold else FontWeight.Normal,
                lineBreak = LineBreak.Paragraph,
            ),
            color = textColor,
            softWrap = true,
            overflow = TextOverflow.Clip,
        )

        trailingContent?.invoke()
    }
}

@Preview(showBackground = true)
@Composable
private fun AppAgreementRowPreview() {
    AekkimTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ColumnPreviewContent()
        }
    }
}

@Composable
private fun ColumnPreviewContent() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppAgreementRow(
            label = "서비스 이용약관 동의 (필수)",
            checked = false,
            onClick = {},
            trailingContent = {
                Text(
                    text = "보기 >",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        AppAgreementRow(
            label = "전체 동의",
            checked = true,
            onClick = {},
            emphasized = true,
        )
    }
}
