package com.chat.lightweight.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络模块 - Retrofit配置
 * 单一职责：负责网络层配置和依赖提供
 */
object NetworkModule {

    private const val BASE_URL = "https://chat.soft1688.vip/api/"
    private const val TIMEOUT_SECONDS = 30L

    /**
     * 提供配置好的OkHttpClient
     */
    private fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .method(original.method, original.body)

                // 添加通用请求头
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(createLoggingInterceptor())
            .build()
    }

    /**
     * 创建日志拦截器
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (com.chat.lightweight.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * 提供Retrofit实例
     */
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 为用户ID添加请求头拦截器
     */
    fun createHttpClientWithUserId(userId: String): OkHttpClient {
        return provideOkHttpClient().newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("x-user-id", userId)  // 重要：使用小写
                    .method(original.method, original.body)
                chain.proceed(requestBuilder.build())
            }
            .build()
    }
}
