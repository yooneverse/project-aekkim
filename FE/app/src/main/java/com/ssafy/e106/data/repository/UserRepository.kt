package com.ssafy.e106.data.repository

import com.ssafy.e106.core.model.ErrorResponse
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.UserApi
import com.ssafy.e106.data.dto.user.NotificationSettingsRequest
import com.ssafy.e106.data.dto.user.NotificationSettingsResponse
import com.ssafy.e106.data.dto.user.OptionalConsentRequest
import com.ssafy.e106.data.dto.user.OptionalConsentResponse
import com.ssafy.e106.data.dto.user.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi,
) {
    suspend fun getMe(): Result<UserProfile> {
        return try {
            val response = userApi.getMe()
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "사용자 정보를 불러오지 못했어요.")
            }
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: "사용자 정보를 불러오지 못했어요.", e.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "사용자 정보를 불러오는 중 오류가 발생했어요.")
        }
    }

    suspend fun updateNotificationSettings(
        checkinAlertEnabled: Boolean,
        promoAlertEnabled: Boolean,
    ): Result<NotificationSettingsResponse> {
        return try {
            val response = userApi.updateNotificationSettings(
                NotificationSettingsRequest(
                    checkinAlertEnabled = checkinAlertEnabled,
                    promoAlertEnabled = promoAlertEnabled,
                ),
            )
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "알림 설정을 저장하지 못했어요.")
            }
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: "알림 설정을 저장하지 못했어요.", e.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "알림 설정을 저장하는 중 오류가 발생했어요.")
        }
    }

    suspend fun updateOptionalConsent(
        optionalConsentAgreed: Boolean,
    ): Result<OptionalConsentResponse> {
        return try {
            val response = userApi.updateOptionalConsent(
                OptionalConsentRequest(optionalConsentAgreed = optionalConsentAgreed),
            )
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "마케팅 수신 동의 상태를 저장하지 못했어요.")
            }
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: "마케팅 수신 동의 상태를 저장하지 못했어요.", e.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "마케팅 수신 동의 상태를 저장하는 중 오류가 발생했어요.")
        }
    }

    suspend fun deleteMe(): Result<Unit> {
        return try {
            val response = userApi.deleteMe()
            if (response.success) {
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: "계정을 삭제하지 못했어요.")
            }
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: "계정을 삭제하지 못했어요.", e.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "계정을 삭제하는 중 오류가 발생했어요.")
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

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
