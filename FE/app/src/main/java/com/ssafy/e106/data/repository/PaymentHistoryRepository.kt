package com.ssafy.e106.data.repository

import com.ssafy.e106.core.model.ErrorResponse
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.PaymentHistoryApi
import com.ssafy.e106.data.dto.payment.PaymentHistoryResponse
import com.ssafy.e106.feature.analysis.model.PaymentRecord
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class PaymentHistoryRepository @Inject constructor(
    private val paymentHistoryApi: PaymentHistoryApi,
) {
    suspend fun getPaymentRecords(): Result<List<PaymentRecord>> {
        return try {
            val response = paymentHistoryApi.getPaymentHistory()
            if (response.success && response.data != null) {
                Result.Success(response.data.payments.map { item -> item.toPaymentRecord() })
            } else {
                Result.Error(response.message ?: DEFAULT_PAYMENT_HISTORY_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_PAYMENT_HISTORY_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_PAYMENT_HISTORY_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_PAYMENT_HISTORY_ERROR_MESSAGE)
        }
    }

    fun toBootstrapFailureReason(error: Result.Error): String {
        return when (error.code) {
            in 500..599 -> BOOTSTRAP_REASON_SERVER_ERROR
            in 400..499 -> BOOTSTRAP_REASON_HTTP_ERROR
            else -> when (error.message) {
                DEFAULT_NETWORK_ERROR_MESSAGE -> BOOTSTRAP_REASON_NETWORK_ERROR
                DEFAULT_TIMEOUT_ERROR_MESSAGE -> BOOTSTRAP_REASON_TIMEOUT
                else -> BOOTSTRAP_REASON_LOAD_ERROR
            }
        }
    }

    private fun parseHttpError(exception: HttpException): String? {
        val raw = exception.response()?.errorBody()?.string()?.trim().orEmpty()
        if (raw.isBlank()) return null

        val parsed = runCatching {
            json.decodeFromString<ErrorResponse>(raw)
        }.getOrNull()

        return parsed?.message ?: raw
    }

    private fun PaymentHistoryResponse.toPaymentRecord(): PaymentRecord {
        return PaymentRecord(
            paymentId = paymentId,
            merchantRaw = merchantRaw,
            amount = amount,
            paymentDate = LocalDate.parse(paymentDate),
            currency = DEFAULT_CURRENCY,
            installmentMonths = null,
            // TODO: Wire installment/cancel/refund flags when BE exposes them.
            canceled = false,
            refunded = false,
        )
    }

    companion object {
        private const val DEFAULT_PAYMENT_HISTORY_ERROR_MESSAGE = "Failed to load payment history."
        private const val DEFAULT_NETWORK_ERROR_MESSAGE = "Check your network connection."
        private const val DEFAULT_TIMEOUT_ERROR_MESSAGE = "The server response is taking too long."
        private const val DEFAULT_CURRENCY = "KRW"

        private val json = Json { ignoreUnknownKeys = true }

        const val BOOTSTRAP_REASON_NETWORK_ERROR = "payment_history_network_error"
        const val BOOTSTRAP_REASON_TIMEOUT = "payment_history_timeout"
        const val BOOTSTRAP_REASON_HTTP_ERROR = "payment_history_http_error"
        const val BOOTSTRAP_REASON_SERVER_ERROR = "payment_history_server_error"
        const val BOOTSTRAP_REASON_LOAD_ERROR = "payment_history_load_error"

        val paymentHistoryEndpoint: String = "GET /api/v1/payment-history"
    }
}
