package com.swipecleaner

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.SQLException
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SwipeCleanerViewModel(
    private val appContext: Context,
    private val contentResolver: ContentResolver,
    private val repository: MediaRepository,
    private val monetizationStore: MonetizationStore,
    private val billingManager: BillingManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            freeDeleteUsedCount = monetizationStore.getFreeDeleteUsedCount(),
            isProUnlocked = monetizationStore.getIsProUnlocked(),
            hasSeenOnboarding = monetizationStore.getHasSeenOnboarding(),
            requireDeleteConfirmation = monetizationStore.getRequireDeleteConfirmation(),
            smartModeEnabled = monetizationStore.getSmartModeEnabled(),
            appLanguageTag = monetizationStore.getAppLanguageTag(),
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val queue = ArrayDeque<MediaItem>()
    private val deleteSelection = linkedSetOf<MediaItem>()
    private var awaitingRestoreResult = false
    private var scanJob: Job? = null
    private var lastScanRequestAt = 0L
    private var cachedSourceItems: List<MediaItem>? = null

    init {
        AppLanguage.apply(appContext, monetizationStore.getAppLanguageTag())
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
                    infoMessage = appContext.getString(R.string.permission_denied_message),
                )
            }
            return
        }
        _uiState.update { it.copy(hasPermission = true, isPermissionDenied = false, infoMessage = null) }
        scan(forceRescan = true)
    }

    fun setFilterPreset(preset: FilterPreset) {
        if (_uiState.value.activeFilter == preset) return
        _uiState.update { it.copy(activeFilter = preset) }
        scan()
    }

    fun rescan() {
        scan(forceRescan = true, applyDebounce = false)
    }

    fun completeOnboarding() {
        monetizationStore.setHasSeenOnboarding(true)
        _uiState.update { it.copy(hasSeenOnboarding = true) }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setRequireDeleteConfirmation(value: Boolean) {
        monetizationStore.setRequireDeleteConfirmation(value)
        _uiState.update { it.copy(requireDeleteConfirmation = value) }
    }

    fun setSmartModeEnabled(value: Boolean) {
        monetizationStore.setSmartModeEnabled(value)
        _uiState.update {
            it.copy(
                smartModeEnabled = value,
                showSmartModeInfoDialog = false,
                activeFilter = if (value) FilterPreset.ALL else it.activeFilter,
                infoMessage = null,
            )
        }
        scan()
    }

    fun toggleSmartModeInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showSmartModeInfoDialog = show) }
    }

    fun toggleLanguageDialog(show: Boolean) {
        _uiState.update { it.copy(showLanguageDialog = show) }
    }

    fun setAppLanguage(tag: String) {
        monetizationStore.setAppLanguageTag(tag)
        AppLanguage.apply(appContext, tag)
        _uiState.update {
            it.copy(
                appLanguageTag = tag,
                showLanguageDialog = false,
                infoMessage = appContext.getString(R.string.language_applied),
            )
        }
    }

    private fun scan(forceRescan: Boolean = false, applyDebounce: Boolean = true) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            if (applyDebounce) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - lastScanRequestAt
                if (elapsed < SCAN_DEBOUNCE_MS) {
                    delay(SCAN_DEBOUNCE_MS - elapsed)
                }
                lastScanRequestAt = SystemClock.elapsedRealtime()
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    infoMessage = appContext.getString(R.string.scanning_library),
                    scanErrorMessage = null,
                )
            }

            try {
                val state = _uiState.value
                val sourceItems = if (forceRescan || cachedSourceItems == null) {
                    repository.scanMedia().also { cachedSourceItems = it }
                } else {
                    cachedSourceItems.orEmpty()
                }
                val filtered = MediaFilters.apply(sourceItems, state.activeFilter)
                val ordered = if (state.smartModeEnabled) {
                    MediaFilters.applySmartOrder(filtered)
                } else {
                    filtered
                }
                queue.clear()
                deleteSelection.clear()
                queue.addAll(ordered)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentItem = queue.firstOrNull(),
                        remainingCount = queue.size,
                        selectedForDeleteCount = 0,
                        selectedDeleteSizeBytes = 0,
                        keptCount = 0,
                        reviewItems = emptyList(),
                        showReviewScreen = false,
                        lastAction = null,
                        infoMessage = appContext.getString(R.string.scanned_items, ordered.size),
                        showDeletionSuccessDialog = false,
                        showDeleteConfirmationDialog = false,
                        scanErrorMessage = null,
                    )
                }
            } catch (securityException: SecurityException) {
                onScanFailed(appContext.getString(R.string.scan_failed_permission))
            } catch (_: IllegalArgumentException) {
                onScanFailed(appContext.getString(R.string.scan_failed_generic))
            } catch (_: SQLException) {
                onScanFailed(appContext.getString(R.string.scan_failed_generic))
            }
        }
    }

    private fun onScanFailed(message: String) {
        queue.clear()
        deleteSelection.clear()
        _uiState.update {
            it.copy(
                isLoading = false,
                currentItem = null,
                remainingCount = 0,
                selectedForDeleteCount = 0,
                selectedDeleteSizeBytes = 0,
                reviewItems = emptyList(),
                showReviewScreen = false,
                keptCount = 0,
                lastAction = null,
                infoMessage = null,
                scanErrorMessage = message,
            )
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
                keptCount = it.keptCount + if (action == SwipeAction.KEEP) 1 else 0,
                reviewItems = deleteSelection.toList(),
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

        val actionLabel = when (last.action) {
            SwipeAction.KEEP -> appContext.getString(R.string.keep)
            SwipeAction.DELETE -> appContext.getString(R.string.delete)
            SwipeAction.ARCHIVE -> appContext.getString(R.string.archive)
            SwipeAction.MOVE -> appContext.getString(R.string.move)
        }

        _uiState.update {
            it.copy(
                currentItem = queue.firstOrNull(),
                remainingCount = queue.size,
                selectedForDeleteCount = deleteSelection.size,
                selectedDeleteSizeBytes = MediaFilters.totalBytes(deleteSelection),
                keptCount = (it.keptCount - if (last.action == SwipeAction.KEEP) 1 else 0).coerceAtLeast(0),
                reviewItems = deleteSelection.toList(),
                lastAction = null,
                infoMessage = appContext.getString(R.string.undo_message, actionLabel),
            )
        }
    }

    fun openReviewSelected() {
        _uiState.update { it.copy(showReviewScreen = true, reviewItems = deleteSelection.toList()) }
    }

    fun closeReviewSelected() {
        _uiState.update { it.copy(showReviewScreen = false) }
    }

    fun unmarkFromSelection(itemId: Long) {
        deleteSelection.removeAll { it.id == itemId }
        _uiState.update {
            it.copy(
                reviewItems = deleteSelection.toList(),
                selectedForDeleteCount = deleteSelection.size,
                selectedDeleteSizeBytes = MediaFilters.totalBytes(deleteSelection),
            )
        }
    }

    fun requestDelete() {
        if (deleteSelection.isEmpty()) return
        if (_uiState.value.requireDeleteConfirmation) {
            _uiState.update { it.copy(showDeleteConfirmationDialog = true) }
            return
        }
        confirmDeletion()
    }

    fun dismissDeleteConfirmationDialog() {
        _uiState.update { it.copy(showDeleteConfirmationDialog = false) }
    }

    fun confirmDeletion() {
        if (deleteSelection.isEmpty()) return
        _uiState.update { it.copy(showDeleteConfirmationDialog = false) }

        val currentState = _uiState.value
        if (!currentState.isProUnlocked) {
            val nextCount = currentState.freeDeleteUsedCount + deleteSelection.size
            if (nextCount > FREE_DELETE_LIMIT) {
                _uiState.update {
                    it.copy(
                        showPaywall = true,
                        paywallMessage = billingManager.productAvailabilityMessage(),
                    )
                }
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
            _uiState.update { it.copy(infoMessage = appContext.getString(R.string.delete_failed_message)) }
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
                reviewItems = emptyList(),
                freeDeleteUsedCount = updatedCount,
                infoMessage = appContext.getString(
                    R.string.deleted_summary,
                    deletedCount,
                    Formatters.bytesToHumanReadable(deletedSize),
                ),
                showDeletionSuccessDialog = true,
                showReviewScreen = false,
                lastDeletedCount = deletedCount,
                lastFreedSizeBytes = deletedSize,
            )
        }
    }

    fun dismissDeletionSuccessDialog() {
        _uiState.update { it.copy(showDeletionSuccessDialog = false) }
    }

    fun closePaywall() {
        _uiState.update { it.copy(showPaywall = false, paywallMessage = null) }
    }

    fun buyPro(activity: Activity) {
        if (!billingManager.launchPurchaseFlow(activity)) {
            val paywallMessage = billingManager.productAvailabilityMessage()
                ?: appContext.getString(R.string.purchase_not_ready)
            _uiState.update {
                it.copy(
                    infoMessage = paywallMessage,
                    showPaywall = true,
                    paywallMessage = paywallMessage,
                )
            }
        }
    }

    fun restorePurchases() {
        awaitingRestoreResult = true
        _uiState.update { it.copy(infoMessage = appContext.getString(R.string.restoring_purchases)) }
        billingManager.queryPurchases()
    }

    private fun onProStatusChanged(isProUnlocked: Boolean) {
        monetizationStore.setIsProUnlocked(isProUnlocked)
        val restoreMessage = if (awaitingRestoreResult) {
            if (isProUnlocked) appContext.getString(R.string.purchases_restored) else appContext.getString(R.string.no_purchases_found)
        } else {
            if (isProUnlocked) appContext.getString(R.string.pro_unlocked) else null
        }
        awaitingRestoreResult = false
        _uiState.update {
            it.copy(
                isProUnlocked = isProUnlocked,
                showPaywall = if (isProUnlocked) false else it.showPaywall,
                paywallMessage = if (isProUnlocked) null else it.paywallMessage,
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
                appContext = context.applicationContext,
                contentResolver = contentResolver,
                repository = MediaRepository(context),
                monetizationStore = MonetizationStore(context),
                billingManager = billingManager,
            ) as T
        }
    }

    companion object {
        private const val SCAN_DEBOUNCE_MS = 250L
    }
}
