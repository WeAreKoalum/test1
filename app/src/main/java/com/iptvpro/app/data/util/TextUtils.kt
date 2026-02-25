package com.iptvpro.app.data.util

import java.text.Normalizer

/**
 * Normaliza texto para búsqueda y comparación:
 * minúsculas + elimina acentos/diacríticos.
 */
fun String.normalize(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        .lowercase()
        .trim()
}
