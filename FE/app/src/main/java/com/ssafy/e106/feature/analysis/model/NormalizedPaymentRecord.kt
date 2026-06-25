package com.ssafy.e106.feature.analysis.model

import java.time.LocalDate

data class NormalizedPaymentRecord(
    val paymentRecord: PaymentRecord,
    val merchantNormalized: String,
    val merchantTokens: List<String> = emptyList(),
    val normalizedAmount: Int = paymentRecord.amount,
    val paymentDate: LocalDate = paymentRecord.paymentDate,
    val currency: String = paymentRecord.currency,
    val ruleTags: Set<String> = emptySet(),
)
