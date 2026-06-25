package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.user.NotificationSettingsRequest
import com.ssafy.e106.data.dto.user.NotificationSettingsResponse
import com.ssafy.e106.data.dto.user.OptionalConsentRequest
import com.ssafy.e106.data.dto.user.OptionalConsentResponse
import com.ssafy.e106.data.dto.user.UserProfile
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {
    @GET("/api/v1/users/me")
    suspend fun getMe(): ApiResponse<UserProfile>

    @PATCH("/api/v1/users/me/notification-settings")
    suspend fun updateNotificationSettings(
        @Body request: NotificationSettingsRequest,
    ): ApiResponse<NotificationSettingsResponse>

    @PATCH("/api/v1/users/me/optional-consent")
    suspend fun updateOptionalConsent(
        @Body request: OptionalConsentRequest,
    ): ApiResponse<OptionalConsentResponse>

    @DELETE("/api/v1/users/me")
    suspend fun deleteMe(): ApiResponse<Unit>
}
