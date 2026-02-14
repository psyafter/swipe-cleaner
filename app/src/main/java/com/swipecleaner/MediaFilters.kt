package com.swipecleaner

import java.util.concurrent.TimeUnit

enum class FilterPreset {
    ALL,
    LARGE_ONLY,
    OLD_ONLY,
    SCREENSHOTS,
    WHATSAPP_MEDIA,
}

object MediaFilters {
    private const val LARGE_THRESHOLD_BYTES = 50L * 1024L * 1024L
    private val OLD_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(183)

    fun apply(
        items: List<MediaItem>,
        preset: FilterPreset,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<MediaItem> {
        val filtered = when (preset) {
            FilterPreset.ALL -> items
            FilterPreset.LARGE_ONLY -> items.filter { it.sizeBytes > LARGE_THRESHOLD_BYTES }
            FilterPreset.OLD_ONLY -> items.filter { nowMillis - it.dateTakenMillis >= OLD_THRESHOLD_MILLIS }
            FilterPreset.SCREENSHOTS -> items.filter(::isScreenshot)
            FilterPreset.WHATSAPP_MEDIA -> items.filter(::isWhatsApp)
        }

        return filtered.sortedByDescending { it.dateTakenMillis }
    }

    fun totalBytes(items: Collection<MediaItem>): Long = items.sumOf { it.sizeBytes }

    private fun isScreenshot(item: MediaItem): Boolean {
        val path = item.relativePath.orEmpty()
        val bucket = item.bucketName.orEmpty()
        val mime = item.mimeType.orEmpty()
        return path.contains("Screenshot", ignoreCase = true) ||
            bucket.contains("Screenshot", ignoreCase = true) ||
            mime.contains("screenshot", ignoreCase = true)
    }

    private fun isWhatsApp(item: MediaItem): Boolean {
        val path = item.relativePath.orEmpty()
        val bucket = item.bucketName.orEmpty()
        return path.contains("WhatsApp", ignoreCase = true) ||
            bucket.contains("WhatsApp", ignoreCase = true)
    }
}
