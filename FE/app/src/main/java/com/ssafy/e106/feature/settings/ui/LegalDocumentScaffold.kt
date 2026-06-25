package com.ssafy.e106.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LegalDocumentScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val spacing = LocalSpacing.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로 가기",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = spacing.screenHorizontalPadding,
                end = spacing.screenHorizontalPadding,
                top = spacing.space4,
                bottom = spacing.space8,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
            content = content,
        )
    }
}

@Composable
internal fun LegalSectionDivider() {
    val spacing = LocalSpacing.current
    Spacer(modifier = Modifier.height(spacing.space3))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    Spacer(modifier = Modifier.height(spacing.space3))
}

@Composable
internal fun LegalSectionHeading(text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = spacing.space2),
    )
}

@Composable
internal fun LegalSubHeading(text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = spacing.space2, bottom = spacing.space1),
    )
}

@Composable
internal fun LegalBodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun LegalBulletItem(text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = spacing.space2),
    )
}

@Composable
internal fun LegalNumberedItem(number: Int, text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = "$number. $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = spacing.space2),
    )
}
