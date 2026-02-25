package com.iptvpro.app.data.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parser XMLTV incremental (XmlPullParser / SAX-style).
 * No carga el XML completo en memoria; procesa elemento a elemento.
 * Soporta: <channel>, <programme> con start/stop, title, desc.
 * Zonas horarias: parsea el formato XMLTV estándar "20240101120000 +0100".
 */
object XmltvParser {

    data class ParsedEpgChannel(
        val id: String,
        val displayName: String,
        val iconUrl: String?
    )

    data class ParsedProgram(
        val channelId: String,
        val startTime: Long,  // epoch millis UTC
        val endTime: Long,    // epoch millis UTC
        val title: String,
        val description: String?
    )

    /** Los callbacks permiten procesar en streaming sin acumular todo en RAM */
    fun parse(
        inputStream: InputStream,
        onChannel: (ParsedEpgChannel) -> Unit,
        onProgram: (ParsedProgram) -> Unit
    ) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        try {
            parser.setInput(inputStream, null) // detecta charset automáticamente

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "channel" -> parseChannel(parser)?.let(onChannel)
                        "programme" -> parseProgram(parser)?.let(onProgram)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            // XML malformado: registrar y terminar de forma segura
            // La app seguirá sin EPG o con EPG parcial
        }
    }

    private fun parseChannel(parser: XmlPullParser): ParsedEpgChannel? {
        val id = parser.getAttributeValue(null, "id")?.takeIf { it.isNotBlank() } ?: return null
        var displayName = ""
        var iconUrl: String? = null

        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "display-name" -> {
                            if (displayName.isEmpty()) {
                                displayName = readText(parser)
                                depth-- // readText ya consume el END_TAG
                            }
                        }
                        "icon" -> {
                            iconUrl = parser.getAttributeValue(null, "src")?.takeIf { it.isNotBlank() }
                        }
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return null
            }
        }

        if (displayName.isEmpty()) return null
        return ParsedEpgChannel(id = id, displayName = displayName, iconUrl = iconUrl)
    }

    private fun parseProgram(parser: XmlPullParser): ParsedProgram? {
        val channelId = parser.getAttributeValue(null, "channel")?.takeIf { it.isNotBlank() } ?: return null
        val startStr = parser.getAttributeValue(null, "start") ?: return null
        val stopStr = parser.getAttributeValue(null, "stop") ?: return null

        val startTime = parseXmltvDate(startStr) ?: return null
        val endTime = parseXmltvDate(stopStr) ?: return null

        var title = ""
        var description: String? = null

        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "title" -> {
                            if (title.isEmpty()) {
                                title = readText(parser)
                                depth--
                            }
                        }
                        "desc" -> {
                            if (description == null) {
                                description = readText(parser)
                                depth--
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return null
            }
        }

        if (title.isEmpty()) return null
        return ParsedProgram(
            channelId = channelId,
            startTime = startTime,
            endTime = endTime,
            title = title,
            description = description
        )
    }

    /** Lee el texto de un elemento simple y posiciona el parser en END_TAG */
    private fun readText(parser: XmlPullParser): String {
        var text = ""
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text ?: ""
            parser.nextTag() // consume END_TAG
        }
        return text.trim()
    }

    /**
     * Parsea fecha XMLTV: "20240101123000 +0100" o "20240101123000 +0000".
     * Devuelve epoch millis UTC.
     */
    private fun parseXmltvDate(raw: String): Long? {
        return try {
            val trimmed = raw.trim()
            // Formato: YYYYMMDDHHmmss zona (zona puede ser +HHMM o -HHMM)
            val sdf: SimpleDateFormat
            val dateStr: String
            if (trimmed.length >= 19 && (trimmed[15] == '+' || trimmed[15] == '-')) {
                sdf = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                dateStr = trimmed.substring(0, 15) + " " + trimmed.substring(15).trim()
            } else if (trimmed.length >= 14) {
                // Sin zona horaria: asumir UTC
                sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                dateStr = trimmed.substring(0, 14)
            } else {
                return null
            }
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
