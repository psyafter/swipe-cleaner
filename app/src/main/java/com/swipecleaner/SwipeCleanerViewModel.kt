package com.swipecleaner

import android.content.ContentResolver
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SwipeCleanerViewModel(
    private val contentResolver: ContentResolver,
    private val repository: MediaRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val queue = ArrayDeque<MediaItem>()
    private val deleteSelection = linkedSetOf<MediaItem>()

    fun requestPermissions() {
        viewModelScope.launch {
            eventChannel.send(UiEvent.RequestPermission(requiredPermissions()))
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.update { it.copy(infoMessage = "Permission denied. Cannot scan gallery.") }
            return
        }
        _uiState.update { it.copy(hasPermission = true) }
        scan()
    }

    private fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = "Scanning media library…") }
            val items = repository.scanMedia()
            queue.clear()
            queue.addAll(items)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentItem = queue.firstOrNull(),
                    remainingCount = queue.size,
                    infoMessage = "Scanned ${items.size} items",
                )
            }
        }
    }

    fun onCardAction(action: SwipeAction) {
        val current = _uiState.value.currentItem ?: return
        queue.removeFirstOrNull()

        if (action == SwipeAction.DELETE) {
            deleteSelection.add(current)
        }

        _uiState.update {
            val bytes = deleteSelection.sumOf { item -> item.sizeBytes }
            it.copy(
                currentItem = queue.firstOrNull(),
                remainingCount = queue.size,
                selectedForDeleteCount = deleteSelection.size,
                selectedDeleteSizeBytes = bytes,
                lastAction = ActionRecord(current, action),
                infoMessage = null,
            )
        }
    }

    fun undo() {
        val last = _uiState.value.lastAction ?: return
        queue.addFirst(last.item)
        if (last.action == SwipeAction.DELETE) {
            deleteSelection.remove(last.item)
        }

        _uiState.update {
            it.copy(
                currentItem = queue.firstOrNull(),
                remainingCount = queue.size,
                selectedForDeleteCount = deleteSelection.size,
                selectedDeleteSizeBytes = deleteSelection.sumOf { item -> item.sizeBytes },
                lastAction = null,
                infoMessage = "Undid ${last.action.name.lowercase()} action",
            )
        }
    }

    fun confirmDeletion() {
        if (deleteSelection.isEmpty()) return
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val request = MediaStore.createDeleteRequest(contentResolver, deleteSelection.map { it.uri })
                eventChannel.send(UiEvent.LaunchDeleteConfirmation(request.intentSender))
            } else {
                val deleted = repository.deleteMediaItems(deleteSelection.toList())
                afterDeletion(deleted > 0)
            }
        }
    }

    fun onDeleteConfirmationResult(success: Boolean) {
        afterDeletion(success)
    }

    private fun afterDeletion(success: Boolean) {
        if (!success) {
            _uiState.update { it.copy(infoMessage = "Deletion canceled by user") }
            return
        }
        val deletedCount = deleteSelection.size
        val deletedSize = deleteSelection.sumOf { it.sizeBytes }
        deleteSelection.clear()
        _uiState.update {
            it.copy(
                selectedForDeleteCount = 0,
                selectedDeleteSizeBytes = 0,
                infoMessage = "Deleted $deletedCount files, freed ${Formatters.bytesToHumanReadable(deletedSize)}",
            )
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    class Factory(private val contentResolver: ContentResolver) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwipeCleanerViewModel(
                contentResolver = contentResolver,
                repository = MediaRepository(contentResolver),
            ) as T
        }
    }
}
