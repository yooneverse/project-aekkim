package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.notification.FcmTokenResponse
import com.ssafy.e106.data.dto.notification.NotificationListResponse
import com.ssafy.e106.data.dto.notification.NotificationReadResponse
import com.ssafy.e106.data.dto.notification.UpsertFcmTokenRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @POST("/api/v1/notifications/tokens")
    suspend fun upsertFcmToken(
        @Body request: UpsertFcmTokenRequest,
    ): ApiResponse<FcmTokenResponse>

    @GET("/api/v1/notifications")
    suspend fun getNotifications(
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("size") size: Int = 20,
    ): ApiResponse<NotificationListResponse>

    @PATCH("/api/v1/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("notificationId") notificationId: Long,
    ): ApiResponse<NotificationReadResponse>

    @DELETE("/api/v1/notifications/{notificationId}")
    suspend fun deleteNotification(
        @Path("notificationId") notificationId: Long,
    ): ApiResponse<Unit>

    @DELETE("/api/v1/notifications")
    suspend fun deleteAllNotifications(): ApiResponse<Unit>
}
