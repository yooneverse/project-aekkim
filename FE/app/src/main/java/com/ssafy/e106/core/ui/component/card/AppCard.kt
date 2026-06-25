package com.ssafy.e106.core.ui.component.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.model.CardVariant

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Default,
    containerColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val elevation = when (variant) {
        CardVariant.Default -> CardDefaults.cardElevation(defaultElevation = 0.dp)
        CardVariant.Elevated -> CardDefaults.cardElevation(defaultElevation = 2.dp)
    }

    val cardModifier = modifier.fillMaxWidth()
    val resolvedContainerColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else {
        MaterialTheme.colorScheme.surface
    }
    val colors = CardDefaults.cardColors(
        containerColor = resolvedContainerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val shape = MaterialTheme.shapes.extraLarge

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
