package com.ssafy.e106.core.ui.component.input

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val supportingText = errorText ?: helperText

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .heightIn(min = 52.dp),
            enabled = enabled,
            singleLine = singleLine,
            isError = errorText != null,
            shape = MaterialTheme.shapes.large,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(text = placeholder, style = MaterialTheme.typography.bodyLarge)
                }
            },
            leadingIcon = leadingIcon?.let { icon -> ({ androidx.compose.material3.Icon(icon, contentDescription = null) }) },
            trailingIcon = trailingIcon?.let { icon -> ({ androidx.compose.material3.Icon(icon, contentDescription = null) }) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                // TODO: Replace with a dedicated error token once the destructive palette is finalized.
                errorBorderColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.onSurface,
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )

        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 16.dp),
            )
        }
    }
}
