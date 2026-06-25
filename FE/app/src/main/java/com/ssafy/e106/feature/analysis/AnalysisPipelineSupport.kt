package com.ssafy.e106.feature.analysis

import com.ssafy.e106.feature.analysis.model.AnalysisCandidate
import com.ssafy.e106.feature.analysis.model.PaymentRecord
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

private val billedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

internal fun sanitizeMerchant(value: String): String {
    return value
        .uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9가-힣+]"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

internal fun tokenizeMerchant(value: String): List<String> {
    return sanitizeMerchant(value)
        .split(" ")
        .map { token -> token.trim() }
        .filter { token -> token.isNotBlank() }
}

internal fun PaymentRecord.toBilledAtLabel(): String {
    return paymentDate.format(billedAtFormatter)
}

internal fun PaymentRecord.toStableReviewId(): Long {
    val rawHash = paymentId.fold(17L) { acc, char ->
        (acc * 31L) + char.code.toLong()
    }
    return rawHash.absoluteValue
}

internal fun AnalysisCandidate.primaryHintServiceId(): Long? {
    return serviceCatalogHints.firstOrNull()?.serviceId
}

internal fun AnalysisCandidate.primaryHintServicePlanId(): Long? {
    val service = serviceCatalogHints.firstOrNull() ?: return null
    return service.plans.minByOrNull { plan ->
        kotlin.math.abs(plan.monthlyPrice - normalizedPaymentRecord.normalizedAmount)
    }?.servicePlanId
}

internal fun AnalysisCandidate.primaryHintServiceName(): String? {
    return serviceCatalogHints.firstOrNull()?.name
}
