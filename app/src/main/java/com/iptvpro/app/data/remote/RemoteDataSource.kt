package com.iptvpro.app.data.remote

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga recursos remotos (M3U, XMLTV) como InputStream.
 * No usa OkHttp para minimizar dependencias; usa java.net.HttpURLConnection.
 */
object RemoteDataSource {

    private const val TIMEOUT_MS = 30_000

    /**
     * Abre un InputStream para la URL dada.
     * La función que recibe el stream es responsable de cerrarlo.
     * Lanza IOException si la conexión falla.
     */
    fun <T> fetch(url: String, block: (InputStream) -> T): T {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "IPTVPro/1.0")
        connection.instanceFollowRedirects = true
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            throw RuntimeException("Error HTTP $responseCode al descargar: $url")
        }

        return try {
            connection.inputStream.use(block)
        } finally {
            connection.disconnect()
        }
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            val u = URL(url)
            u.protocol in listOf("http", "https")
        } catch (e: Exception) {
            false
        }
    }
}
