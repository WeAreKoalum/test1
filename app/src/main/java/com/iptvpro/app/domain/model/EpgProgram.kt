package com.iptvpro.app.domain.model

data class EpgProgram(
    val title: String,
    val description: String?,
    val startTime: Long,  // epoch millis UTC
    val endTime: Long     // epoch millis UTC
) {
    /** Porcentaje de avance del programa (0.0 - 1.0) */
    fun progress(nowMillis: Long): Float {
        val duration = (endTime - startTime).toFloat()
        if (duration <= 0) return 0f
        return ((nowMillis - startTime) / duration).coerceIn(0f, 1f)
    }
}
