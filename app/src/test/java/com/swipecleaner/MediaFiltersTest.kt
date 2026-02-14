package com.swipecleaner

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaFiltersTest {

    private val sample = listOf(
        media(1, size = 1000, date = 300, path = "DCIM/Screenshots/", kind = MediaKind.IMAGE),
        media(2, size = 8000, date = 100, path = "WhatsApp/Media/", kind = MediaKind.VIDEO),
        media(3, size = 500, date = 200, path = "Telegram/Images/", kind = MediaKind.IMAGE),
    )

    @Test
    fun `largest filter sorts descending`() {
        val filtered = MediaFilters.apply(sample, FilterPreset.LARGEST)
        assertThat(filtered.map { it.id }).containsExactly(2L, 1L, 3L).inOrder()
    }

    @Test
    fun `oldest filter sorts ascending`() {
        val filtered = MediaFilters.apply(sample, FilterPreset.OLDEST)
        assertThat(filtered.map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun `whatsapp telegram filter returns expected`() {
        val filtered = MediaFilters.apply(sample, FilterPreset.WHATSAPP_TELEGRAM)
        assertThat(filtered.map { it.id }).containsExactly(2L, 3L)
    }

    @Test
    fun `total bytes sums selected items`() {
        assertThat(MediaFilters.totalBytes(sample)).isEqualTo(9500)
    }

    private fun media(id: Long, size: Long, date: Long, path: String, kind: MediaKind): MediaItem {
        return MediaItem(
            id = id,
            uri = Uri.parse("content://media/$id"),
            displayName = "item$id",
            sizeBytes = size,
            dateTakenMillis = date,
            mimeType = null,
            relativePath = path,
            bucketName = null,
            kind = kind,
        )
    }
}
