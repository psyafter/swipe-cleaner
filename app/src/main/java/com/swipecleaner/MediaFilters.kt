package com.swipecleaner

enum class FilterPreset { ALL, SCREENSHOTS, VIDEOS, LARGEST, OLDEST, WHATSAPP_TELEGRAM }

object MediaFilters {
    fun apply(items: List<MediaItem>, preset: FilterPreset): List<MediaItem> {
        return when (preset) {
            FilterPreset.ALL -> items.sortedByDescending { it.dateTakenMillis }
            FilterPreset.SCREENSHOTS -> items.filter { it.relativePath?.contains("Screenshots", true) == true }
            FilterPreset.VIDEOS -> items.filter { it.kind == MediaKind.VIDEO }
            FilterPreset.LARGEST -> items.sortedByDescending { it.sizeBytes }
            FilterPreset.OLDEST -> items.sortedBy { it.dateTakenMillis }
            FilterPreset.WHATSAPP_TELEGRAM -> items.filter {
                val path = it.relativePath.orEmpty()
                path.contains("WhatsApp", true) || path.contains("Telegram", true)
            }
        }
    }

    fun totalBytes(items: Collection<MediaItem>): Long = items.sumOf { it.sizeBytes }
}
