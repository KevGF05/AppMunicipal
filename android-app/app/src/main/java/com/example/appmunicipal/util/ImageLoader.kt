package com.example.appmunicipal.util

import android.graphics.BitmapFactory
import android.widget.ImageView
import com.example.appmunicipal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object ImageLoader {
    suspend fun cargar(relativeOrAbsoluteUrl: String?, imageView: ImageView) {
        if (relativeOrAbsoluteUrl.isNullOrBlank()) return
        val finalUrl = if (relativeOrAbsoluteUrl.startsWith("http")) {
            relativeOrAbsoluteUrl
        } else {
            BuildConfig.API_BASE_URL.trimEnd('/') + "/" + relativeOrAbsoluteUrl.trimStart('/')
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                URL(finalUrl).openStream().use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
        bitmap?.let(imageView::setImageBitmap)
    }
}
