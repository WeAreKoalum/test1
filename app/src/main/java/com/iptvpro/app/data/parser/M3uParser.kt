package com.iptvpro.app.data.parser

import java.io.InputStream

/**
 * Parser M3U robusto.
 * Soporta: nombre, URL, group-title, tvg-logo, tvg-id, tvg-name.
 * Extrae la URL EPG de la cabecera EXTM3U (url-tvg / x-tvg-url).
 * Las líneas malformadas se ignoran sin romper el parsing.
 */
object M3uParser {

    data class ParsedPlaylist(
        val channels: List<ParsedChannel>,
        /** URL EPG detectada en la cabecera EXTM3U, si existe */
        val epgUrl: String?
    )

    data class ParsedChannel(
        val name: String,
        val url: String,
        val groupTitle: String,
        val logoUrl: String?,
        val tvgId: String?,
        val tvgName: String?
    )

    /** Regex para atributos clave=valor (comillas dobles) */
    private val ATTR_REGEX = Regex("""([\w-]+)="([^"]*)"""")

    /** Regex para url-tvg o x-tvg-url en la cabecera EXTM3U */
    private val EPG_URL_REGEX = Regex("""(?:url-tvg|x-tvg-url)="([^"]+)"""")

    fun parse(inputStream: InputStream): ParsedPlaylist {
        var epgUrl: String? = null
        val channels = mutableListOf<ParsedChannel>()

        inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            var currentExtinf: String? = null

            reader.forEachLine { rawLine ->
                val line = rawLine.trim()
                when {
                    line.startsWith("#EXTM3U") -> {
                        epgUrl = EPG_URL_REGEX.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    }
                    line.startsWith("#EXTINF:") -> {
                        currentExtinf = line
                    }
                    line.startsWith("#") -> {
                        // Otras directivas ignoradas
                    }
                    line.isNotBlank() -> {
                        // Línea de URL del stream
                        val extinf = currentExtinf
                        currentExtinf = null
                        if (extinf != null) {
                            parseEntry(extinf, line)?.let { channels.add(it) }
                        }
                        // Si no hay EXTINF previo, ignorar la línea (M3U simple sin metadata)
                    }
                }
            }
        }

        return ParsedPlaylist(channels = channels, epgUrl = epgUrl)
    }

    private fun parseEntry(extinf: String, url: String): ParsedChannel? {
        return try {
            val attrs = parseAttributes(extinf)
            // El nombre del canal viene después de la última coma en la línea EXTINF
            val name = extinf.substringAfterLast(",").trim()
            if (name.isEmpty() || url.isEmpty()) return null

            ParsedChannel(
                name = name,
                url = url,
                groupTitle = attrs["group-title"]?.takeIf { it.isNotBlank() } ?: "Sin categoría",
                logoUrl = attrs["tvg-logo"]?.takeIf { it.isNotBlank() },
                tvgId = attrs["tvg-id"]?.takeIf { it.isNotBlank() },
                tvgName = attrs["tvg-name"]?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            // Entrada malformada; se ignora
            null
        }
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ATTR_REGEX.findAll(line).forEach { match ->
            result[match.groupValues[1].lowercase()] = match.groupValues[2]
        }
        return result
    }
}
