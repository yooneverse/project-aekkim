package com.ssafy.e106.data.repository

import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.SubscriptionUsageApi
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyItemRequest
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyUpsertRequest
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyUpsertResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageReportResponse
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class SubscriptionUsageRepository @Inject constructor(
    private val subscriptionUsageApi: SubscriptionUsageApi,
) {

    suspend fun upsertDailyUsage(
        items: List<SubscriptionUsageDailyItemRequest>,
    ): Result<SubscriptionUsageDailyUpsertResponse> {
        if (items.isEmpty()) {
            return Result.Success(SubscriptionUsageDailyUpsertResponse(savedCount = 0))
        }

        return try {
            val response = subscriptionUsageApi.upsertDailyUsage(
                SubscriptionUsageDailyUpsertRequest(items = items),
            )
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: DEFAULT_UPSERT_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(e.message ?: DEFAULT_UPSERT_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_UPSERT_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_UPSERT_ERROR_MESSAGE)
        }
    }

    suspend fun getUsageReport(
        days: Int = DEFAULT_WINDOW_DAYS,
    ): Result<SubscriptionUsageReportResponse> {
        return try {
            val response = subscriptionUsageApi.getSubscriptionUsageReport(days = days)
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: DEFAULT_REPORT_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(e.message ?: DEFAULT_REPORT_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_REPORT_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_REPORT_ERROR_MESSAGE)
        }
    }

    suspend fun getUsageDaily(
        days: Int = DEFAULT_WINDOW_DAYS,
        subscriptionId: Long? = null,
    ): Result<SubscriptionUsageDailyResponse> {
        return try {
            val response = subscriptionUsageApi.getSubscriptionUsageDaily(
                days = days,
                subscriptionId = subscriptionId,
            )
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: DEFAULT_DAILY_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(e.message ?: DEFAULT_DAILY_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_DAILY_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_DAILY_ERROR_MESSAGE)
        }
    }

    companion object {
        private const val DEFAULT_WINDOW_DAYS = 30
        private const val DEFAULT_UPSERT_ERROR_MESSAGE = "일별 사용량 업로드에 실패했어요."
        private const val DEFAULT_REPORT_ERROR_MESSAGE = "구독 사용량 리포트를 불러오지 못했어요."
        private const val DEFAULT_DAILY_ERROR_MESSAGE = "일별 사용 흐름을 불러오지 못했어요."
        private const val DEFAULT_NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인해 주세요."
        private const val DEFAULT_TIMEOUT_ERROR_MESSAGE = "서비스 응답이 지연되고 있어요."
    }
}
