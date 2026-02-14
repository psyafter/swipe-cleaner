package com.swipecleaner

import kotlin.math.ln
import kotlin.math.pow

object Formatters {
    fun bytesToHumanReadable(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        val digitGroup = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroup.toDouble())
        return "%.1f %s".format(value, units[digitGroup])
    }
}
