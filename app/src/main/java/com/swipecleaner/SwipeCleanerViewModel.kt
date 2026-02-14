package com.swipecleaner

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
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
    private val monetizationStore: MonetizationStore,
    private val billingManager: BillingManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            freeDeleteUsedCount = monetizationStore.getFreeDeleteUsedCount(),
            isProUnlocked = monetizationStore.getIsProUnlocked(),
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val queue = ArrayDeque<MediaItem>()
    private val deleteSelection = linkedSetOf<MediaItem>()
    private var awaitingRestoreResult = false


    init {
        billingManager.setCallbacks(
            onProStatusChanged = ::onProStatusChanged,
            onMessage = ::onBillingMessage,
        )
        billingManager.connect()
    }


    fun requestPermissions() {
        _uiState.update { it.copy(isPermissionDenied = false, infoMessage = null) }
        viewModelScope.launch {
            eventChannel.send(UiEvent.RequestPermission(requiredPermissions()))
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.update {
                it.copy(
                    hasPermission = false,
                    isPermissionDenied = true,
                    infoMessage = "Permission denied",
                )
            }
            return
        }
        _uiState.update { it.copy(hasPermission = true, isPermissionDenied = false, infoMessage = null) }
        scan()
    }

    fun setFilterPreset(preset: FilterPreset) {
        if (_uiState.value.activeFilter == preset) return
        _uiState.update { it.copy(activeFilter = preset) }
        scan()
    }

    private fun scan() {
        viewModelScope.launch {
            val preset = _uiState.value.activeFilter
            _uiState.update { it.copy(isLoading = true, infoMessage = "Scanning media library…") }
            val items = repository.scanMedia()
            val filtered = MediaFilters.apply(items, preset)
            queue.clear()
            deleteSelection.clear()
            queue.addAll(filtered)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentItem = queue.firstOrNull(),
                    remainingCount = queue.size,
                    selectedForDeleteCount = 0,
                    selectedDeleteSizeBytes = 0,
                    lastAction = null,
                    infoMessage = "Scanned ${filtered.size} items",
                    showDeletionSuccessDialog = false,
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
            val bytes = MediaFilters.totalBytes(deleteSelection)
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
                selectedDeleteSizeBytes = MediaFilters.totalBytes(deleteSelection),
                lastAction = null,
                infoMessage = "Undid ${last.action.name.lowercase()} action",
            )
        }
    }

    fun confirmDeletion() {
        if (deleteSelection.isEmpty()) return

        val currentState = _uiState.value
        if (!currentState.isProUnlocked) {
            val nextCount = currentState.freeDeleteUsedCount + deleteSelection.size
            if (nextCount > FREE_DELETE_LIMIT) {
                _uiState.update { it.copy(showPaywall = true) }
                return
            }
        }

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
            _uiState.update { it.copy(infoMessage = "Canceled") }
            return
        }
        val deletedCount = deleteSelection.size
        val deletedSize = MediaFilters.totalBytes(deleteSelection)
        deleteSelection.clear()

        val state = _uiState.value
        val updatedCount = if (state.isProUnlocked) {
            state.freeDeleteUsedCount
        } else {
            (state.freeDeleteUsedCount + deletedCount).coerceAtMost(FREE_DELETE_LIMIT)
        }
        monetizationStore.setFreeDeleteUsedCount(updatedCount)

        _uiState.update {
            it.copy(
                selectedForDeleteCount = 0,
                selectedDeleteSizeBytes = 0,
                freeDeleteUsedCount = updatedCount,
                infoMessage = "Deleted $deletedCount files, freed ${Formatters.bytesToHumanReadable(deletedSize)}",
                showDeletionSuccessDialog = true,
                lastDeletedCount = deletedCount,
                lastFreedSizeBytes = deletedSize,
            )
        }
    }

    fun dismissDeletionSuccessDialog() {
        _uiState.update { it.copy(showDeletionSuccessDialog = false) }
    }

    fun closePaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun buyPro(activity: Activity) {
        if (!billingManager.launchPurchaseFlow(activity)) {
            _uiState.update {
                it.copy(
                    infoMessage = "Purchase is not ready yet. Please try again in a moment",
                    showPaywall = true,
                )
            }
        }
    }

    fun restorePurchases() {
        awaitingRestoreResult = true
        _uiState.update { it.copy(infoMessage = "Restoring purchases…") }
        billingManager.queryPurchases()
    }

    private fun onProStatusChanged(isProUnlocked: Boolean) {
        monetizationStore.setIsProUnlocked(isProUnlocked)
        val restoreMessage = if (awaitingRestoreResult) {
            if (isProUnlocked) "Purchases restored" else "No active purchases found"
        } else {
            if (isProUnlocked) "Pro unlocked" else null
        }
        awaitingRestoreResult = false
        _uiState.update {
            it.copy(
                isProUnlocked = isProUnlocked,
                showPaywall = if (isProUnlocked) false else it.showPaywall,
                infoMessage = restoreMessage,
            )
        }
    }

    private fun onBillingMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
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

    override fun onCleared() {
        billingManager.endConnection()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val contentResolver: ContentResolver,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val billingManager = BillingManager(context)
            return SwipeCleanerViewModel(
                contentResolver = contentResolver,
                repository = MediaRepository(contentResolver),
                monetizationStore = MonetizationStore(context),
                billingManager = billingManager,
            ) as T
        }
    }
}
