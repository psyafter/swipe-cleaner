package com.swipecleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun SwipeCleanerScreen(
    state: UiState,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onAction: (SwipeAction) -> Unit,
    onUndo: () -> Unit,
    onConfirmDelete: () -> Unit,
    onFilterSelected: (FilterPreset) -> Unit,
    onBuyPro: () -> Unit,
    onRestorePurchases: () -> Unit,
    onClosePaywall: () -> Unit,
    onDismissDeletionSuccess: () -> Unit,
    onRateApp: () -> Unit,
    onRescan: () -> Unit,
    onStartOnboarding: () -> Unit,
    onSkipOnboarding: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onCloseSettingsSheet: () -> Unit,
    onSetRequireDeleteConfirmation: (Boolean) -> Unit,
    onSetSmartModeEnabled: (Boolean) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDeleteNow: () -> Unit,
    onShowSmartModeInfo: (Boolean) -> Unit,
    onShowLanguageDialog: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onOpenReviewSelected: () -> Unit,
    onCloseReviewSelected: () -> Unit,
    onUnmarkItem: (Long) -> Unit,
) {
    if (!state.hasSeenOnboarding) {
        OnboardingScreen(onStartCleaning = onStartOnboarding, onSkip = onSkipOnboarding)
        return
    }

    if (!state.hasPermission) {
        PermissionScreen(
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings,
            isPermissionDenied = state.isPermissionDenied,
        )
        return
    }

    if (state.showReviewScreen) {
        ReviewSelectedScreen(
            items = state.reviewItems,
            onBack = onCloseReviewSelected,
            onKeep = onUnmarkItem,
        )
        return
    }

    val freeLeft = (FREE_DELETE_LIMIT - state.freeDeleteUsedCount).coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            BottomActionBar(
                selectedCount = state.selectedForDeleteCount,
                canUndo = state.lastAction != null,
                canDelete = state.selectedForDeleteCount > 0,
                onUndo = onUndo,
                onDelete = onConfirmDelete,
                onReview = onOpenReviewSelected,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onOpenSettingsSheet,
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.settings_button_a11y) },
                ) {
                    Text(stringResource(R.string.settings_title))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.smart_mode_title), style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = state.smartModeEnabled,
                        onCheckedChange = onSetSmartModeEnabled,
                        modifier = Modifier.semantics { contentDescription = stringResource(R.string.smart_mode_toggle_a11y) },
                    )
                }
                TextButton(onClick = { onShowSmartModeInfo(true) }) { Text(stringResource(R.string.what_is_this)) }
            }

            SessionSummaryCard(
                keptCount = state.keptCount,
                markedCount = state.selectedForDeleteCount,
                freeableBytes = state.selectedDeleteSizeBytes,
            )

            FilterRow(
                active = state.activeFilter,
                smartModeEnabled = state.smartModeEnabled,
                onFilterSelected = onFilterSelected,
            )

            if (state.smartModeEnabled) {
                Text(
                    text = stringResource(R.string.filters_disabled_smart_mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }

            Text(
                if (state.isProUnlocked) stringResource(R.string.pro_unlocked_unlimited)
                else stringResource(R.string.free_deletions_left, freeLeft),
                color = Color.Gray,
            )
            Text(stringResource(R.string.queue_items, state.remainingCount), color = Color.Gray)

            if (state.scanErrorMessage != null) {
                ScanErrorCard(message = state.scanErrorMessage, onTryAgain = onRescan)
            } else if (state.isLoading) {
                Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.scanning_library), color = Color.Gray)
                    }
                }
            } else {
                MediaCard(
                    item = state.currentItem,
                    onAction = onAction,
                    onRescan = onRescan,
                    onFilterSelected = onFilterSelected,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAction(SwipeAction.KEEP) },
                    enabled = state.currentItem != null,
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.keep_button_a11y) },
                ) { Text(stringResource(R.string.keep)) }
                OutlinedButton(
                    onClick = { onAction(SwipeAction.DELETE) },
                    enabled = state.currentItem != null,
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.delete_button_a11y) },
                ) { Text(stringResource(R.string.delete)) }
            }

            SafetyInfoRow()

            state.infoMessage?.let { Text(it, color = Color.Gray) }
        }
    }

    if (state.showDeletionSuccessDialog) {
        DeletionSuccessDialog(
            freedSizeBytes = state.lastFreedSizeBytes,
            deletedCount = state.lastDeletedCount,
            onContinueCleaning = onDismissDeletionSuccess,
            onRateApp = onRateApp,
        )
    }

    if (state.showPaywall) {
        PaywallDialog(
            onBuyPro = onBuyPro,
            onRestorePurchases = onRestorePurchases,
            onDismiss = onClosePaywall,
            paywallMessage = state.paywallMessage,
        )
    }

    if (state.showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            selectedCount = state.selectedForDeleteCount,
            selectedSizeBytes = state.selectedDeleteSizeBytes,
            onDismiss = onDismissDeleteConfirmation,
            onConfirm = onConfirmDeleteNow,
        )
    }

    if (state.showSettingsDialog) {
        SettingsDialog(
            requireDeleteConfirmation = state.requireDeleteConfirmation,
            smartModeEnabled = state.smartModeEnabled,
            selectedLanguageTag = state.appLanguageTag,
            onSetRequireDeleteConfirmation = onSetRequireDeleteConfirmation,
            onSetSmartModeEnabled = onSetSmartModeEnabled,
            onShowLanguageDialog = onShowLanguageDialog,
            onDismiss = onCloseSettingsSheet,
        )
    }

    if (state.showSmartModeInfoDialog) {
        SmartModeInfoDialog(onDismiss = { onShowSmartModeInfo(false) }, onTurnOff = {
            onSetSmartModeEnabled(false)
            onShowSmartModeInfo(false)
        })
    }

    if (state.showLanguageDialog) {
        AppLanguageDialog(
            selectedTag = state.appLanguageTag,
            onDismiss = { onShowLanguageDialog(false) },
            onSelect = onSetLanguage,
        )
    }
}

@Composable
private fun SessionSummaryCard(keptCount: Int, markedCount: Int, freeableBytes: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.session_kept, keptCount))
            Text(stringResource(R.string.session_marked, markedCount))
            Text(stringResource(R.string.session_freeable, Formatters.bytesToHumanReadable(freeableBytes)))
        }
    }
}

@Composable
private fun ScanErrorCard(message: String, onTryAgain: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.scan_failed_title), style = MaterialTheme.typography.titleMedium)
            Text(message, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
            Button(onClick = onTryAgain, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.try_again))
            }
        }
    }
}

@Composable
private fun ReviewSelectedScreen(items: List<MediaItem>, onBack: () -> Unit, onKeep: (Long) -> Unit) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.review_selected_title), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
            if (items.isEmpty()) {
                Text(stringResource(R.string.review_selected_empty), color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.displayName,
                                    modifier = Modifier.height(64.dp).fillMaxWidth(0.25f)
                                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(Formatters.bytesToHumanReadable(item.sizeBytes), fontWeight = FontWeight.SemiBold)
                                    Text(Formatters.dateToShort(item.dateTakenMillis), color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (MediaFilters.isScreenshot(item)) Badge(stringResource(R.string.badge_screenshots))
                                        if (MediaFilters.isWhatsApp(item)) Badge(stringResource(R.string.badge_whatsapp))
                                    }
                                }
                                TextButton(onClick = { onKeep(item.id) }) { Text(stringResource(R.string.keep)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFE8F0FF), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SafetyInfoRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.info_icon), style = MaterialTheme.typography.bodySmall)
        Text(
            text = stringResource(R.string.safe_delete_notice),
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun OnboardingScreen(onStartCleaning: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_step_swipe), modifier = Modifier.padding(16.dp)) }
        Card(Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_step_private), modifier = Modifier.padding(16.dp)) }
        Card(Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_step_review), modifier = Modifier.padding(16.dp)) }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onStartCleaning, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.start_cleaning))
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.skip))
        }
    }
}

@Composable
private fun BottomActionBar(
    selectedCount: Int,
    canUndo: Boolean,
    canDelete: Boolean,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onReview: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.undo))
        }
        OutlinedButton(onClick = onReview, enabled = selectedCount > 0, modifier = Modifier.weight(1.2f)) {
            Text(stringResource(R.string.review_count, selectedCount))
        }
        Button(onClick = onDelete, enabled = canDelete, modifier = Modifier.weight(1.8f)) {
            Text(stringResource(R.string.free_space_count, selectedCount))
        }
    }
}

@Composable
private fun FilterRow(active: FilterPreset, smartModeEnabled: Boolean, onFilterSelected: (FilterPreset) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(FilterPreset.entries) { preset ->
            FilterChip(
                selected = active == preset,
                enabled = !smartModeEnabled,
                onClick = { onFilterSelected(preset) },
                label = { Text(filterLabel(preset)) },
            )
        }
    }
}

@Composable
private fun filterLabel(preset: FilterPreset): String {
    return when (preset) {
        FilterPreset.ALL -> stringResource(R.string.filter_all)
        FilterPreset.LARGE_ONLY -> stringResource(R.string.filter_big)
        FilterPreset.OLD_ONLY -> stringResource(R.string.filter_old)
        FilterPreset.SCREENSHOTS -> stringResource(R.string.filter_shots)
        FilterPreset.WHATSAPP_MEDIA -> stringResource(R.string.filter_whatsapp)
    }
}

@Composable
private fun PaywallDialog(
    onBuyPro: () -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit,
    paywallMessage: String?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.paywall_title)) },
        text = {
            Text(
                buildString {
                    append(stringResource(R.string.paywall_intro))
                    append("\n\n")
                    append(stringResource(R.string.paywall_bullet_1))
                    append("\n")
                    append(stringResource(R.string.paywall_bullet_2))
                    append("\n")
                    append(stringResource(R.string.paywall_bullet_3))
                    if (!paywallMessage.isNullOrBlank()) {
                        append("\n\n")
                        append(paywallMessage)
                    }
                },
            )
        },
        confirmButton = {
            Button(onClick = onBuyPro) {
                Text(stringResource(R.string.upgrade_to_pro))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRestorePurchases) { Text(stringResource(R.string.restore_purchases)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) }
            }
        },
    )
}

@Composable
private fun DeleteConfirmationDialog(
    selectedCount: Int,
    selectedSizeBytes: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_cleaning_session)) },
        text = {
            Text(
                stringResource(
                    R.string.delete_confirmation_message,
                    selectedCount,
                    Formatters.bytesToHumanReadable(selectedSizeBytes),
                ),
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}


@Composable
private fun SmartModeInfoDialog(onDismiss: () -> Unit, onTurnOff: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.smart_mode_title)) },
        text = { Text(stringResource(R.string.smart_mode_full_explainer)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.got_it)) } },
        dismissButton = { TextButton(onClick = onTurnOff) { Text(stringResource(R.string.turn_off)) } },
    )
}

@Composable
private fun SettingsDialog(
    requireDeleteConfirmation: Boolean,
    smartModeEnabled: Boolean,
    selectedLanguageTag: String,
    onSetRequireDeleteConfirmation: (Boolean) -> Unit,
    onSetSmartModeEnabled: (Boolean) -> Unit,
    onShowLanguageDialog: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.require_delete_confirmation))
                    Switch(
                        checked = requireDeleteConfirmation,
                        onCheckedChange = onSetRequireDeleteConfirmation,
                    )
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.smart_mode_title))
                        Switch(checked = smartModeEnabled, onCheckedChange = onSetSmartModeEnabled)
                    }
                    Text(stringResource(R.string.smart_mode_subtitle), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                TextButton(onClick = { onShowLanguageDialog(true) }, modifier = Modifier.fillMaxWidth()) {
                    val label = when {
                        selectedLanguageTag.isBlank() -> stringResource(R.string.use_system_language)
                        else -> AppLanguage.options.firstOrNull { it.tag == selectedLanguageTag }?.let { AppLanguage.nativeName(it.tag) }
                            ?: selectedLanguageTag
                    }
                    Text(stringResource(R.string.app_language_row, label))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun AppLanguageDialog(
    selectedTag: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_language_title)) },
        text = {
            LazyColumn {
                items(AppLanguage.options) { option ->
                    TextButton(onClick = { onSelect(option.tag) }, modifier = Modifier.fillMaxWidth()) {
                        val marker = if (option.tag == selectedTag) stringResource(R.string.selected_language_marker) else ""
                        val label = option.tag.takeIf { it.isNotBlank() }?.let { AppLanguage.nativeName(it) }
                            ?: stringResource(R.string.use_system_language)
                        Text(stringResource(R.string.app_language_option, marker, label))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun DeletionSuccessDialog(
    freedSizeBytes: Long,
    deletedCount: Int,
    onContinueCleaning: () -> Unit,
    onRateApp: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinueCleaning,
        title = { Text(stringResource(R.string.storage_freed)) },
        text = { Text(stringResource(R.string.freed_from_files, Formatters.bytesToHumanReadable(freedSizeBytes), deletedCount)) },
        confirmButton = {
            Button(onClick = onContinueCleaning) {
                Text(stringResource(R.string.continue_cleaning))
            }
        },
        dismissButton = {
            TextButton(onClick = onRateApp) {
                Text(stringResource(R.string.rate_app))
            }
        },
    )
}

@Composable
private fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    isPermissionDenied: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.permission_title))
        Button(onClick = onRequestPermissions, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.grant_access))
        }
        if (isPermissionDenied) {
            Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem?,
    onAction: (SwipeAction) -> Unit,
    onRescan: () -> Unit,
    onFilterSelected: (FilterPreset) -> Unit,
) {
    if (item == null) {
        Card(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.all_done), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.no_more_items))
                Button(onClick = onRescan, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.rescan)) }
                Text(
                    text = stringResource(R.string.try_another_filter),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = { onFilterSelected(FilterPreset.LARGE_ONLY) }) { Text(stringResource(R.string.big_files)) }
                    TextButton(onClick = { onFilterSelected(FilterPreset.OLD_ONLY) }) { Text(stringResource(R.string.old_files)) }
                }
            }
        }
        return
    }

    var dragX by remember { mutableFloatStateOf(0f) }
    val hapticFeedback = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDrag = { _, dragAmount -> dragX += dragAmount.x },
                    onDragEnd = {
                        when {
                            dragX > 180f -> {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAction(SwipeAction.KEEP)
                            }

                            dragX < -180f -> {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAction(SwipeAction.DELETE)
                            }
                        }
                        dragX = 0f
                    },
                )
            },
    ) {
        Box {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize().background(Color.LightGray),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = stringResource(
                    R.string.media_meta,
                    mediaKindLabel(item.kind),
                    Formatters.bytesToHumanReadable(item.sizeBytes),
                ),
                modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.6f)).padding(8.dp),
                color = Color.White,
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Pill(stringResource(R.string.swipe_keep))
                Pill(stringResource(R.string.swipe_delete))
            }
        }
    }
}

@Composable
private fun mediaKindLabel(kind: MediaKind): String {
    return when (kind) {
        MediaKind.IMAGE -> stringResource(R.string.media_kind_image)
        MediaKind.VIDEO -> stringResource(R.string.media_kind_video)
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
