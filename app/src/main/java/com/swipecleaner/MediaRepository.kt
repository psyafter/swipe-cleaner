package com.swipecleaner

import android.content.ContentResolver
import android.content.Context
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(context: Context) {

    private val contentResolver = context.contentResolver
    private val unknownMediaName = context.getString(R.string.unknown_media_name)

    suspend fun scanMedia(limit: Int = 2_000): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        items += queryImages(limit)
        items += queryVideos(limit)
        items.sortedByDescending { it.dateTakenMillis }
    }

    private fun queryImages(limit: Int): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return query(uri, projection, limit, MediaKind.IMAGE)
    }

    private fun queryVideos(limit: Int): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        return query(uri, projection, limit, MediaKind.VIDEO)
    }

    private fun query(
        collectionUri: android.net.Uri,
        projection: Array<String>,
        limit: Int,
        kind: MediaKind,
    ): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT $limit"
        contentResolver.query(collectionUri, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTaken = cursor.getLong(takenColumn)
                val dateAddedMillis = cursor.getLong(addedColumn) * 1000
                val contentUri = ContentUris.withAppendedId(collectionUri, id)
                result += MediaItem(
                    id = id,
                    uri = contentUri,
                    displayName = cursor.getString(nameColumn) ?: unknownMediaName,
                    sizeBytes = cursor.getLong(sizeColumn),
                    dateTakenMillis = if (dateTaken > 0) dateTaken else dateAddedMillis,
                    mimeType = cursor.getString(mimeColumn),
                    relativePath = cursor.getString(relativePathColumn),
                    bucketName = cursor.getString(bucketColumn),
                    kind = kind,
                )
            }
        }
        return result
    }

    suspend fun deleteMediaItems(items: List<MediaItem>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        items.forEach { item ->
            val rows = contentResolver.delete(item.uri, null, null)
            if (rows > 0) deleted++
        }
        deleted
    }
}
