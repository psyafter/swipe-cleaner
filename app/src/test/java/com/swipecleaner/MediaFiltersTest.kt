package com.swipecleaner

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaFiltersTest {

    @Test
    fun `large only filter keeps files bigger than 50MB`() {
        val items = listOf(
            media(1, size = 10L * 1024 * 1024, date = 100),
            media(2, size = 60L * 1024 * 1024, date = 200),
        )

        val filtered = MediaFilters.apply(items, FilterPreset.LARGE_ONLY)

        assertThat(filtered.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `old only filter keeps files older than six months`() {
        val now = 1_000_000_000_000L
        val oldEnough = now - (184L * 24 * 60 * 60 * 1000)
        val fresh = now - (30L * 24 * 60 * 60 * 1000)
        val items = listOf(
            media(1, size = 1000, date = fresh),
            media(2, size = 2000, date = oldEnough),
        )

        val filtered = MediaFilters.apply(items, FilterPreset.OLD_ONLY, nowMillis = now)

        assertThat(filtered.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `screenshots filter uses path and bucket`() {
        val items = listOf(
            media(1, size = 1, date = 100, path = "DCIM/Screenshots/"),
            media(2, size = 1, date = 90, bucket = "Screenshots"),
            media(3, size = 1, date = 80, path = "DCIM/Camera/"),
        )

        val filtered = MediaFilters.apply(items, FilterPreset.SCREENSHOTS)

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
            uri = Uri.parse("content://media/$id"),
            displayName = "item$id",
            sizeBytes = size,
            dateTakenMillis = date,
            mimeType = null,
            relativePath = path,
            bucketName = bucket,
            kind = MediaKind.IMAGE,
        )
    }
}
