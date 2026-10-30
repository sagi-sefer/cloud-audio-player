package com.example.cloudaudioplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class CloudAudioPlayerApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
