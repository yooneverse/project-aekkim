package com.ssafy.e106.core.ui.component.sheet

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    scrimColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    scrollable: Boolean = false,
    footerContent: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = scrimColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.space2, bottom = spacing.space1),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (scrollable) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    },
                )
                .padding(
                    start = spacing.screenHorizontalPadding,
                    end = spacing.screenHorizontalPadding,
                    bottom = spacing.space6,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            content()

            if (footerContent != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3),
                    content = footerContent,
                )
            }
        }
    }
}
