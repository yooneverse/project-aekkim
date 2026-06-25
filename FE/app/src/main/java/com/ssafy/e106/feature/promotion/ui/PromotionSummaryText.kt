package com.ssafy.e106.feature.promotion.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ssafy.e106.data.repository.PromotionType

@Composable
internal fun PromotionSummaryText(
    summary: String,
    promotionType: PromotionType,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = if (promotionType == PromotionType.CardBenefit) {
            buildCardBenefitSummary(summary)
        } else {
            AnnotatedString(summary)
        },
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private fun buildCardBenefitSummary(summary: String): AnnotatedString {
    val lines = summary
        .split('\n')
        .map(String::trim)
        .filter(String::isNotBlank)

    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            appendCardBenefitLine(line)
            if (index != lines.lastIndex) {
                append('\n')
            }
        }
    }
}

private fun AnnotatedString.Builder.appendCardBenefitLine(line: String) {
    val matchedLabel = CARD_BENEFIT_LABELS.firstOrNull { label ->
        line == label || line.startsWith("$label ")
    }

    if (matchedLabel == null) {
        append(line)
        return
    }

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
    append(matchedLabel)
    pop()
    append(line.removePrefix(matchedLabel))
}

private val CARD_BENEFIT_LABELS = listOf(
    "연회비",
    "국내전용",
    "해외겸용",
    "전월실적",
    "브랜드",
)
