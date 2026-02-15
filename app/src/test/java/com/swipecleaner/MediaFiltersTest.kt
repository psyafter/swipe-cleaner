package com.swipecleaner

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaFiltersTest {

    @Test
    fun testLargeOnlyThreshold() {
        val threshold = 50L * 1024 * 1024
        val items = listOf(
            media(id = 1, size = threshold, date = BASE_NOW),
            media(id = 2, size = threshold + 1, date = BASE_NOW),
        )

        val filtered = MediaFilters.apply(items, FilterPreset.LARGE_ONLY, nowMillis = BASE_NOW)

        assertThat(filtered.map { it.id }).containsExactly(2L)
    }

    @Test
    fun testOldOnlyThreshold() {
        val millisInDay = 24L * 60 * 60 * 1000
        val items = listOf(
            media(id = 1, size = 1, date = BASE_NOW - (182L * millisInDay)),
            media(id = 2, size = 1, date = BASE_NOW - (183L * millisInDay)),
        )

        val filtered = MediaFilters.apply(items, FilterPreset.OLD_ONLY, nowMillis = BASE_NOW)

        assertThat(filtered.map { it.id }).containsExactly(2L)
    }

    @Test
    fun testScreenshotsDetection() {
        val items = listOf(
            media(id = 1, size = 1, date = BASE_NOW, path = "DCIM/Screenshots/"),
            media(id = 2, size = 1, date = BASE_NOW, bucket = "My Screenshots"),
            media(id = 3, size = 1, date = BASE_NOW, path = "DCIM/Camera/"),
        )

        val filtered = MediaFilters.apply(items, FilterPreset.SCREENSHOTS, nowMillis = BASE_NOW)

        assertThat(filtered.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun testWhatsAppDetection() {
        val items = listOf(
            media(id = 1, size = 1, date = BASE_NOW, path = "Pictures/WhatsApp Images/"),
            media(id = 2, size = 1, date = BASE_NOW, bucket = "WhatsApp Video"),
            media(id = 3, size = 1, date = BASE_NOW, path = "Pictures/Telegram/"),
        )

        val filtered = MediaFilters.apply(items, FilterPreset.WHATSAPP_MEDIA, nowMillis = BASE_NOW)

        assertThat(filtered.map { it.id }).containsExactly(1L, 2L)
    }

    private fun media(
        id: Long,
        size: Long,
        date: Long,
        path: String? = null,
        bucket: String? = null,
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = Uri.parse("content://test/1"),
            displayName = "item$id",
            sizeBytes = size,
            dateTakenMillis = date,
            mimeType = null,
            relativePath = path,
            bucketName = bucket,
            kind = MediaKind.IMAGE,
        )
    }


    @Test
    fun testSmartOrderPrioritizesImpactAndIsStable() {
        val items = listOf(
            media(id = 1, size = 5L * 1024 * 1024, date = BASE_NOW - (5L * DAY), path = "DCIM/Camera"),
            media(id = 2, size = 150L * 1024 * 1024, date = BASE_NOW - (2L * DAY), path = "DCIM/Camera"),
            media(id = 3, size = 20L * 1024 * 1024, date = BASE_NOW - (350L * DAY), path = "Pictures/WhatsApp Images"),
            media(id = 4, size = 20L * 1024 * 1024, date = BASE_NOW - (350L * DAY), path = "Pictures/Screenshots"),
        )

        val ordered = MediaFilters.applySmartOrder(items, nowMillis = BASE_NOW)

        assertThat(ordered.map { it.id }).containsExactly(4L, 3L, 2L, 1L).inOrder()
    }


    private companion object {
        const val BASE_NOW = 1_700_000_000_000L
        const val DAY = 24L * 60 * 60 * 1000
    }
}
