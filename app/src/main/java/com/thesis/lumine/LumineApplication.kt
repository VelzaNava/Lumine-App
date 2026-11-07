package com.thesis.lumine

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.thesis.lumine.data.api.ApiClient

class LumineApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }

    /**
     * One image loader for the whole app: photos fade in instead of popping,
     * and are cached in memory and on disk so each downloads only once.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .build()
}
