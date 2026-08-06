package com.example.appmunicipal.data.remote

import android.content.Context
import com.example.appmunicipal.BuildConfig
import com.example.appmunicipal.data.local.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    @Volatile
    private var apiService: ApiService? = null

    fun api(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: crear(context.applicationContext).also { apiService = it }
        }
    }

    private fun crear(context: Context): ApiService {
        val tokenStore = TokenStore(context)
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                tokenStore.token()?.takeIf { it.isNotBlank() }?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
