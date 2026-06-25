package com.ssafy.e106.feature.auth.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.core.ui.theme.PillShape

@Composable
fun PermissionPromptDialog(
    badge: String,
    title: String,
    description: String,
    highlights: List<String>,
    primaryActionLabel: String,
    onPrimaryActionClick: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space6),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                ),
                            ),
                        )
                        .padding(horizontal = spacing.space4, vertical = spacing.space5),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (highlights.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.space2),
                    ) {
                        highlights.forEach { item ->
                            val displayItem = item.replace(
                                oldValue = "다음 단계를 이어가요",
                                newValue = "다음\n단계를 이어가요",
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = spacing.space3, vertical = spacing.space3),
                                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 7.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                    Text(
                                        text = displayItem,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppButton(
                        text = primaryActionLabel,
                        onClick = onPrimaryActionClick,
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Large,
                    )

                    if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                        AppButton(
                            text = secondaryActionLabel,
                            onClick = onSecondaryActionClick,
                            modifier = Modifier.fillMaxWidth(),
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Large,
                        )
                    }
                }
            }
        }
    }
}
