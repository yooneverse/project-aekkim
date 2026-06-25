package com.ssafy.e106.feature.analysis

import com.ssafy.e106.data.repository.BatchLookupReviewHint
import com.ssafy.e106.feature.analysis.model.AnalysisCandidate
import com.ssafy.e106.feature.analysis.model.AppUsageSnapshot
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val onDeviceAiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = false
}

@Serializable
internal data class OnDeviceAiPromptPayload(
    val items: List<OnDeviceAiPromptItem>,
)

@Serializable
internal data class OnDeviceAiPromptItem(
    val reviewId: Long,
    val merchantRaw: String,
    val merchantNormalized: String? = null,
    val merchantTokens: List<String> = emptyList(),
    val monthlyAmount: Int,
    val billedAtLabel: String,
    val recurrenceLabel: String? = null,
    val ruleTags: List<String> = emptyList(),
    val ruleScores: Map<String, Double> = emptyMap(),
    val usageSignals: List<OnDeviceAiUsageSignal> = emptyList(),
    val batchLookupSuggestion: OnDeviceAiBatchLookupSuggestion? = null,
    val allowedServices: List<OnDeviceAiAllowedService> = emptyList(),
)

@Serializable
internal data class OnDeviceAiUsageSignal(
    val packageName: String,
    val usage7dMs: Long,
    val usage30dMs: Long,
    val lastUsedEpochMs: Long? = null,
    val permissionGranted: Boolean,
    val reason: String? = null,
)

@Serializable
internal data class OnDeviceAiBatchLookupSuggestion(
    val matched: Boolean,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val serviceName: String? = null,
    val hitCount: Int? = null,
)

@Serializable
internal data class OnDeviceAiAllowedService(
    val serviceId: Long,
    val name: String,
    val aliases: List<String> = emptyList(),
    val packageNames: List<String> = emptyList(),
    val knownMerchantPatterns: List<String> = emptyList(),
    val priceTolerance: Int? = null,
    val plans: List<OnDeviceAiAllowedPlan> = emptyList(),
)

@Serializable
internal data class OnDeviceAiAllowedPlan(
    val servicePlanId: Long,
    val planName: String,
    val billingCycle: String,
    val monthlyPrice: Int,
)

@Serializable
internal data class OnDeviceAiModelResponse(
    val items: List<OnDeviceAiModelItem> = emptyList(),
)

@Serializable
internal data class OnDeviceAiModelItem(
    val reviewId: Long,
    val decision: String,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val serviceName: String? = null,
    val planName: String? = null,
    val subscriptionConfidence: Float? = null,
    val serviceConfidence: Float? = null,
    val planConfidence: Float? = null,
    val reasonCodes: List<String> = emptyList(),
)

internal fun buildOnDeviceAiPrompt(reviewHints: List<BatchLookupReviewHint>): String {
    val payload = OnDeviceAiPromptPayload(items = reviewHints.map { hint -> hint.toPromptItem() })
    return buildString {
        appendLine("You classify unresolved OTT subscription candidates on-device.")
        appendLine("Return valid JSON only. Do not use markdown fences. Do not add prose.")
        appendLine("Output schema:")
        appendLine(
            """{"items":[{"reviewId":1,"decision":"REVIEW_NEEDED","serviceId":null,"servicePlanId":null,"serviceName":null,"planName":null,"subscriptionConfidence":0.0,"serviceConfidence":0.0,"planConfidence":0.0,"reasonCodes":["AI_LOW_CONFIDENCE_REVIEW"]}]}""",
        )
        appendLine("Rules:")
        appendLine("- Keep reviewId unchanged.")
        appendLine("- Output exactly one item for each input item.")
        appendLine("- Allowed decisions: CONFIRMED_SUBSCRIPTION, CONFIRMED_NON_SUBSCRIPTION, REVIEW_NEEDED.")
        appendLine("- Use only serviceId and servicePlanId values listed in allowedServices.")
        appendLine("- If confidence is below 0.85, signals conflict, or the plan is unclear, use REVIEW_NEEDED.")
        appendLine("- Prefer REVIEW_NEEDED over guessing.")
        appendLine("- For generic merchants like APPLE, GOOGLE, and BILL, combine usage, price, and recurrence.")
        appendLine("INPUT_JSON:")
        append(onDeviceAiJson.encodeToString(payload))
    }
}

internal fun parseOnDeviceAiResponse(rawResponse: String): OnDeviceAiModelResponse? {
    val candidates = buildList {
        val trimmed = rawResponse.trim()
        if (trimmed.isNotBlank()) add(trimmed)
        extractFencedJson(trimmed)?.let(::add)
        extractJsonObject(trimmed)?.let(::add)
    }.distinct()

    candidates.forEach { candidate ->
        runCatching {
            return if (candidate.trim().startsWith("[")) {
                OnDeviceAiModelResponse(
                    items = onDeviceAiJson.decodeFromString<List<OnDeviceAiModelItem>>(candidate),
                )
            } else {
                onDeviceAiJson.decodeFromString<OnDeviceAiModelResponse>(candidate)
            }
        }
    }

    return null
}

private fun BatchLookupReviewHint.toPromptItem(): OnDeviceAiPromptItem {
    val candidate = candidate
    return OnDeviceAiPromptItem(
        reviewId = candidate.reviewId,
        merchantRaw = candidate.paymentRecord.merchantRaw,
        merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized,
        merchantTokens = candidate.normalizedPaymentRecord.merchantTokens,
        monthlyAmount = candidate.normalizedPaymentRecord.normalizedAmount,
        billedAtLabel = candidate.paymentRecord.toBilledAtLabel(),
        recurrenceLabel = candidate.recurrenceLabel,
        ruleTags = candidate.normalizedPaymentRecord.ruleTags.toList().sorted(),
        ruleScores = candidate.ruleScores.toSortedMap(),
        usageSignals = candidate.usageSignals.map(AppUsageSnapshot::toPromptSignal),
        batchLookupSuggestion = OnDeviceAiBatchLookupSuggestion(
            matched = matched,
            serviceId = serviceId,
            servicePlanId = servicePlanId,
            serviceName = serviceName,
            hitCount = hitCount,
        ),
        allowedServices = candidate.allowedServices(),
    )
}

private fun AnalysisCandidate.allowedServices(): List<OnDeviceAiAllowedService> {
    return serviceCatalogHints
        .mapNotNull { service -> service.toAllowedService() }
        .distinctBy(OnDeviceAiAllowedService::serviceId)
}

private fun ServiceCatalog.Service.toAllowedService(): OnDeviceAiAllowedService? {
    val resolvedServiceId = serviceId ?: return null
    return OnDeviceAiAllowedService(
        serviceId = resolvedServiceId,
        name = name,
        aliases = aliases.toList().sorted(),
        packageNames = packageNames.toList().sorted(),
        knownMerchantPatterns = knownMerchantPatterns.toList().sorted(),
        priceTolerance = priceTolerance,
        plans = plans.mapNotNull { plan -> plan.toAllowedPlan() }.sortedBy(OnDeviceAiAllowedPlan::servicePlanId),
    )
}

private fun ServiceCatalog.Plan.toAllowedPlan(): OnDeviceAiAllowedPlan? {
    val resolvedPlanId = servicePlanId ?: return null
    return OnDeviceAiAllowedPlan(
        servicePlanId = resolvedPlanId,
        planName = planName,
        billingCycle = billingCycle,
        monthlyPrice = monthlyPrice,
    )
}

private fun AppUsageSnapshot.toPromptSignal(): OnDeviceAiUsageSignal {
    return OnDeviceAiUsageSignal(
        packageName = packageName,
        usage7dMs = usage7dMs,
        usage30dMs = usage30dMs,
        lastUsedEpochMs = lastUsedEpochMs,
        permissionGranted = permissionGranted,
        reason = reason,
    )
}

private fun extractFencedJson(value: String): String? {
    val fenced = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""").find(value)
    return fenced?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("{") && it.endsWith("}") }
}

private fun extractJsonObject(value: String): String? {
    val start = value.indexOf('{')
    val end = value.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    return value.substring(start, end + 1)
}
