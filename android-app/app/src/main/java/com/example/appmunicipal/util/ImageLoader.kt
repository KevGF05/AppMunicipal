package com.example.appmunicipal.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import com.example.appmunicipal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Carga las fotografias de los reportes.
 *
 * Mejoras respecto a la version anterior:
 *  - Reduce la imagen antes de crear el Bitmap. Una foto de camara puede ocupar mas
 *    de 30 MB en memoria sin escalar y provocar un OutOfMemory.
 *  - Guarda un cache en memoria, asi volver al detalle no descarga otra vez.
 *  - Marca el ImageView con la URL pedida, para que al reciclarse una fila de la
 *    lista no aparezca la foto de otro ticket.
 *
 * Para una app real conviene usar Coil o Glide; esto resuelve lo mismo sin agregar
 * otra dependencia.
 */
object ImageLoader {

    private const val ANCHO_MAXIMO = 1080
    private const val ALTO_MAXIMO = 1080

    private val cache = object : LruCache<String, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun cargar(urlRelativaOAbsoluta: String?, imageView: ImageView) {
        if (urlRelativaOAbsoluta.isNullOrBlank()) return
        val url = construirUrl(urlRelativaOAbsoluta)

        // Se recuerda que fila pidio esta imagen.
        imageView.setTag(imageView.id, url)

        cache.get(url)?.let {
            imageView.setImageBitmap(it)
            return
        }

        val bitmap = withContext(Dispatchers.IO) { descargar(url) } ?: return

        // Si la vista se reciclo para otro ticket mientras se descargaba, no se pisa.
        if (imageView.getTag(imageView.id) != url) return

        cache.put(url, bitmap)
        imageView.setImageBitmap(bitmap)
    }

    private fun construirUrl(valor: String): String =
        if (valor.startsWith("http")) {
            valor
        } else {
            BuildConfig.API_BASE_URL.trimEnd('/') + "/" + valor.trimStart('/')
        }

    private fun descargar(url: String): Bitmap? = runCatching {
        val bytes = abrir(url).use { it.readBytes() }

        // Primera pasada: solo se leen las dimensiones, sin cargar los pixeles.
        val opcionesMedida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcionesMedida)

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calcularEscala(opcionesMedida.outWidth, opcionesMedida.outHeight)
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
    }.getOrNull()

    private fun abrir(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
    }.inputStream

    private fun calcularEscala(ancho: Int, alto: Int): Int {
        var escala = 1
        if (ancho <= 0 || alto <= 0) return escala
        while (ancho / (escala * 2) >= ANCHO_MAXIMO && alto / (escala * 2) >= ALTO_MAXIMO) {
            escala *= 2
        }
        return escala
    }
}
