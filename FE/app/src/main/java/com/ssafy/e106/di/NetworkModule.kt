package com.ssafy.e106.di

import com.ssafy.e106.BuildConfig
import com.ssafy.e106.core.network.currentBaseUrl
import com.ssafy.e106.data.api.AuthApi
import com.ssafy.e106.data.api.CheckInApi
import com.ssafy.e106.data.api.MerchantMappingApi
import com.ssafy.e106.data.api.NotificationApi
import com.ssafy.e106.data.api.PaymentHistoryApi
import com.ssafy.e106.data.api.PromotionApi
import com.ssafy.e106.data.api.ServiceApi
import com.ssafy.e106.data.api.SubscriptionApi
import com.ssafy.e106.data.api.SubscriptionUsageApi
import com.ssafy.e106.data.api.UserApi
import com.ssafy.e106.data.auth.TokenAuthenticator
import com.ssafy.e106.data.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideSubscriptionApi(retrofit: Retrofit): SubscriptionApi =
        retrofit.create(SubscriptionApi::class.java)

    @Provides
    @Singleton
    fun provideSubscriptionUsageApi(retrofit: Retrofit): SubscriptionUsageApi =
        retrofit.create(SubscriptionUsageApi::class.java)

    @Provides
    @Singleton
    fun provideMerchantMappingApi(retrofit: Retrofit): MerchantMappingApi =
        retrofit.create(MerchantMappingApi::class.java)

    @Provides
    @Singleton
    fun provideCheckInApi(retrofit: Retrofit): CheckInApi =
        retrofit.create(CheckInApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun providePromotionApi(retrofit: Retrofit): PromotionApi =
        retrofit.create(PromotionApi::class.java)

    @Provides
    @Singleton
    fun providePaymentHistoryApi(retrofit: Retrofit): PaymentHistoryApi =
        retrofit.create(PaymentHistoryApi::class.java)

    @Provides
    @Singleton
    fun provideServiceApi(retrofit: Retrofit): ServiceApi = retrofit.create(ServiceApi::class.java)
}
