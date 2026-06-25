package com.ssafy.e106.feature.auth.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ssafy.e106.core.ui.theme.LocalSpacing

@Composable
fun AuthFlowScaffold(
    modifier: Modifier = Modifier,
    scrollableBody: Boolean = false,
    centerBody: Boolean = false,
    bodyHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    body: @Composable ColumnScope.() -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalSpacing.current
    val baseBodyModifier = Modifier.fillMaxWidth()
    val bodyModifier = if (scrollableBody) {
        baseBodyModifier.verticalScroll(rememberScrollState())
    } else {
        baseBodyModifier
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = spacing.screenHorizontalPadding,
                vertical = spacing.space6,
            ),
    ) {
        Column(
            modifier = bodyModifier.weight(1f, fill = true),
            horizontalAlignment = bodyHorizontalAlignment,
            verticalArrangement = if (centerBody) {
                Arrangement.Center
            } else {
                Arrangement.spacedBy(spacing.sectionGap)
            },
            content = body,
        )

        Spacer(modifier = Modifier.height(spacing.space4))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
            content = bottomContent,
        )
    }
}
