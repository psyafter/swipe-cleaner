package com.swipecleaner

import android.net.Uri

const val FREE_DELETE_LIMIT = 100
const val PRO_PRODUCT_ID = "pro_unlock"

enum class MediaKind { IMAGE, VIDEO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val mimeType: String?,
    val relativePath: String?,
    val bucketName: String?,
    val kind: MediaKind,
)

enum class SwipeAction { KEEP, DELETE, ARCHIVE, MOVE }

data class ActionRecord(
    val item: MediaItem,
    val action: SwipeAction,
)

data class UiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val isPermissionDenied: Boolean = false,
    val currentItem: MediaItem? = null,
    val remainingCount: Int = 0,
    val selectedForDeleteCount: Int = 0,
    val selectedDeleteSizeBytes: Long = 0,
    val lastAction: ActionRecord? = null,
    val infoMessage: String? = null,
    val activeFilter: FilterPreset = FilterPreset.ALL,
    val freeDeleteUsedCount: Int = 0,
    val isProUnlocked: Boolean = false,
    val showPaywall: Boolean = false,
)

sealed interface UiEvent {
    data class RequestPermission(val permissions: Array<String>) : UiEvent
    data class LaunchDeleteConfirmation(val intentSender: android.content.IntentSender) : UiEvent
}
