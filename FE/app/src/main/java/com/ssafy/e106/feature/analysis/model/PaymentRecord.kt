package com.ssafy.e106.feature.analysis.model

import java.time.LocalDate

data class PaymentRecord(
    val paymentId: String,
    val merchantRaw: String,
    val amount: Int,
    val paymentDate: LocalDate,
    val currency: String = "KRW",
    val installmentMonths: Int? = null,
    val canceled: Boolean = false,
    val refunded: Boolean = false,
)
