package com.ssafy.e106.feature.analysis

import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.PaymentHistoryRepository
import com.ssafy.e106.feature.analysis.model.PaymentRecord
import javax.inject.Inject

data class PaymentBootstrapPayload(
    val paymentRecords: List<PaymentRecord>,
    val isPlaceholder: Boolean,
    val placeholderReason: String? = null,
)

class PaymentBootstrapSeam @Inject constructor(
    private val paymentHistoryRepository: PaymentHistoryRepository,
) {
    suspend fun loadPaymentRecords(): PaymentBootstrapPayload {
        return when (val result = paymentHistoryRepository.getPaymentRecords()) {
            is Result.Success -> PaymentBootstrapPayload(
                paymentRecords = result.data,
                isPlaceholder = false,
                placeholderReason = null,
            )

            is Result.Error -> PaymentBootstrapPayload(
                paymentRecords = emptyList(),
                isPlaceholder = true,
                placeholderReason = paymentHistoryRepository.toBootstrapFailureReason(result),
            )

            Result.Loading -> PaymentBootstrapPayload(
                paymentRecords = emptyList(),
                isPlaceholder = true,
                placeholderReason = PLACEHOLDER_REASON_LOADING_STATE,
            )
        }
    }

    companion object {
        const val PLACEHOLDER_REASON_LOADING_STATE = "payment_history_loading_state"
    }
}
