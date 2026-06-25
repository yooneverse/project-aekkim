package com.ssafy.e106.core.ui.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.Disabled

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    accessibilityLabel: String? = null,
    accessibilityStateDescription: String? = null,
) {
    val metrics = buttonMetrics(size)
    val isInteractive = enabled && !loading
    val resolvedStateDescription = accessibilityStateDescription ?: when {
        loading -> "로딩 중"
        !isInteractive -> "비활성화"
        else -> null
    }
    val contentModifier = modifier
        .defaultMinSize(minHeight = metrics.height)
        .semantics(mergeDescendants = true) {
            contentDescription = accessibilityLabel ?: text
            resolvedStateDescription?.let { stateDescription = it }
        }

    when (variant) {
        ButtonVariant.Primary -> Button(
            onClick = onClick,
            enabled = isInteractive,
            modifier = contentModifier,
            shape = metrics.shape,
            contentPadding = PaddingValues(horizontal = metrics.horizontalPadding),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = Disabled,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            ButtonContent(
                text = text,
                loading = loading,
                loadingColor = MaterialTheme.colorScheme.onPrimary,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                iconSize = metrics.iconSize,
            )
        }

        ButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = isInteractive,
            modifier = contentModifier,
            shape = metrics.shape,
            contentPadding = PaddingValues(horizontal = metrics.horizontalPadding),
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            ButtonContent(
                text = text,
                loading = loading,
                loadingColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                iconSize = metrics.iconSize,
            )
        }

        ButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            enabled = isInteractive,
            modifier = contentModifier,
            shape = metrics.shape,
            contentPadding = PaddingValues(horizontal = metrics.horizontalPadding),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            ButtonContent(
                text = text,
                loading = loading,
                loadingColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                iconSize = metrics.iconSize,
            )
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    loadingColor: Color,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    iconSize: androidx.compose.ui.unit.Dp,
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(iconSize),
            strokeWidth = 2.dp,
            color = loadingColor,
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(iconSize))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null, modifier = Modifier.size(iconSize))
        }
    }
}

private data class ButtonMetrics(
    val height: androidx.compose.ui.unit.Dp,
    val horizontalPadding: androidx.compose.ui.unit.Dp,
    val shape: Shape,
    val iconSize: androidx.compose.ui.unit.Dp,
)

@Composable
private fun buttonMetrics(size: ButtonSize): ButtonMetrics = when (size) {
    ButtonSize.Small -> ButtonMetrics(
        height = 40.dp,
        horizontalPadding = 14.dp,
        shape = MaterialTheme.shapes.medium,
        iconSize = 16.dp,
    )

    ButtonSize.Medium -> ButtonMetrics(
        height = 48.dp,
        horizontalPadding = 16.dp,
        shape = MaterialTheme.shapes.large,
        iconSize = 18.dp,
    )

    ButtonSize.Large -> ButtonMetrics(
        height = 52.dp,
        horizontalPadding = 20.dp,
        shape = MaterialTheme.shapes.large,
        iconSize = 20.dp,
    )
}
