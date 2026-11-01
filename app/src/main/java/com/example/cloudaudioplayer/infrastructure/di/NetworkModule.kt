package com.example.cloudaudioplayer.infrastructure.di

import com.example.cloudaudioplayer.infrastructure.storage.AuthTokenHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            var request = chain.request()
            val token = AuthTokenHolder.currentToken
            if (token.isNotBlank() && request.url.host.contains("googleapis.com") && request.header("Authorization") == null && request.url.queryParameter("access_token") == null) {
                request = request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
