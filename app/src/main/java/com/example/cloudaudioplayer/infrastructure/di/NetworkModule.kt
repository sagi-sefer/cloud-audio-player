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
            val host = request.url.host
            val headerAuth = request.header("Authorization")

            if (headerAuth == null) {
                val token = if (host.contains("googleapis.com") || host.contains("googleusercontent.com")) {
                    AuthTokenHolder.googleToken.ifBlank { AuthTokenHolder.currentToken }
                } else if (host.contains("dropboxapi.com") || host.contains("dropboxusercontent.com") || host.contains("dl.dropboxusercontent.com")) {
                    AuthTokenHolder.dropboxToken.ifBlank { AuthTokenHolder.currentToken }
                } else ""

                if (token.isNotBlank()) {
                    request = request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
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
