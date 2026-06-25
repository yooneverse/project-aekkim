package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.auth.GoogleLoginRequest
import com.ssafy.e106.data.dto.auth.KakaoLoginRequest
import com.ssafy.e106.data.dto.auth.LoginResponse
import com.ssafy.e106.data.dto.auth.RefreshTokenRequest
import com.ssafy.e106.data.dto.auth.TokenPair
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/v1/auth/login/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleLoginRequest
    ): ApiResponse<LoginResponse>

    @POST("/api/v1/auth/login/kakao")
    suspend fun loginWithKakao(
        @Body request: KakaoLoginRequest
    ): ApiResponse<LoginResponse>

    @POST("/api/v1/auth/refresh")
    fun refreshTokenSync(
        @Body request: RefreshTokenRequest
    ): Call<ApiResponse<TokenPair>>

    @POST("/api/v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
