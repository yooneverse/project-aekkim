package com.ssafy.e106.feature.analysis

import com.ssafy.e106.feature.analysis.model.NormalizedPaymentRecord
import com.ssafy.e106.feature.analysis.model.PaymentRecord
import javax.inject.Inject

class PaymentRecordNormalizer @Inject constructor() {
    fun normalize(paymentRecords: List<PaymentRecord>): List<NormalizedPaymentRecord> {
        return paymentRecords.map { paymentRecord ->
            val merchantNormalized = sanitizeMerchant(paymentRecord.merchantRaw)
            val tags = buildSet {
                if (paymentRecord.canceled) add("canceled")
                if (paymentRecord.refunded) add("refunded")
                if ((paymentRecord.installmentMonths ?: 0) > 1) add("installment")
                if (GENERIC_PAYMENT_GATEWAY_PATTERNS.any { pattern -> merchantNormalized.contains(pattern) }) {
                    add("generic_gateway")
                }
            }

            NormalizedPaymentRecord(
                paymentRecord = paymentRecord,
                merchantNormalized = merchantNormalized,
                merchantTokens = tokenizeMerchant(merchantNormalized),
                normalizedAmount = paymentRecord.amount,
                paymentDate = paymentRecord.paymentDate,
                currency = paymentRecord.currency,
                ruleTags = tags,
            )
        }
    }

    private companion object {
        val GENERIC_PAYMENT_GATEWAY_PATTERNS = setOf(
            "APPLE",
            "GOOGLE",
            "INICIS",
            "KG",
            "TOSS",
            "PAYCO",
            "KCP",
            "PAYPAL",
        )
    }
}
