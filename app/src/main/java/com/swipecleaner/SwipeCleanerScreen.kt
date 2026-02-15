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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
    onOpenSettingsSheet: () -> Unit,
    onCloseSettingsSheet: () -> Unit,
    onSetRequireDeleteConfirmation: (Boolean) -> Unit,
    onSetSmartModeEnabled: (Boolean) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDeleteNow: () -> Unit,
    onShowSmartModeInfo: (Boolean) -> Unit,
    onShowLanguageDialog: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
) {
    if (!state.hasSeenOnboarding) {
        OnboardingScreen(onStartCleaning = onStartOnboarding)
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

    val freeLeft = (FREE_DELETE_LIMIT - state.freeDeleteUsedCount).coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            BottomActionBar(
                selectedCount = state.selectedForDeleteCount,
                canUndo = state.lastAction != null,
                canDelete = state.selectedForDeleteCount > 0,
                onUndo = onUndo,
                onDelete = onConfirmDelete,
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
                TextButton(onClick = onOpenSettingsSheet) { Text(stringResource(R.string.settings_title)) }
            }

            FilterRow(active = state.activeFilter, onFilterSelected = onFilterSelected)

            if (state.smartModeEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.smart_mode_title), style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { onShowSmartModeInfo(true) }) {
                        Text(stringResource(R.string.what_is_this))
                    }
                }
            }

            Text(
                text = stringResource(R.string.you_can_free, Formatters.bytesToHumanReadable(state.selectedDeleteSizeBytes)),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.marked_files, state.selectedForDeleteCount))
            Text(
                if (state.isProUnlocked) stringResource(R.string.pro_unlocked_unlimited)
                else stringResource(R.string.free_deletions_left, freeLeft),
                color = Color.Gray,
            )
            Text(stringResource(R.string.queue_items, state.remainingCount), color = Color.Gray)

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
                OutlinedButton(onClick = { onAction(SwipeAction.KEEP) }, enabled = state.currentItem != null) { Text(stringResource(R.string.keep)) }
                OutlinedButton(onClick = { onAction(SwipeAction.DELETE) }, enabled = state.currentItem != null) { Text(stringResource(R.string.delete)) }
            }

            Text(
                stringResource(R.string.delete_notice),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
            )

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
private fun OnboardingScreen(onStartCleaning: () -> Unit) {
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
    }
}

@Composable
private fun BottomActionBar(
    selectedCount: Int,
    canUndo: Boolean,
    canDelete: Boolean,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
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
        Button(onClick = onDelete, enabled = canDelete, modifier = Modifier.weight(2f)) {
            Text(stringResource(R.string.free_space_count, selectedCount))
        }
    }
}

@Composable
private fun FilterRow(active: FilterPreset, onFilterSelected: (FilterPreset) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(FilterPreset.entries) { preset ->
            FilterChip(
                selected = active == preset,
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
                    val label = AppLanguage.options.firstOrNull { it.tag == selectedLanguageTag }?.nativeName
                        ?.ifBlank { stringResource(R.string.use_system_language) }
                        ?: stringResource(R.string.english_language)
                    Text(stringResource(R.string.app_language_row, label))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun SmartModeInfoDialog(onDismiss: () -> Unit, onTurnOff: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.smart_mode_title)) },
        text = { Text(stringResource(R.string.smart_mode_full_explainer)) },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.got_it)) } },
        dismissButton = { TextButton(onClick = onTurnOff) { Text(stringResource(R.string.turn_off)) } },
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
                        val marker = if (option.tag == selectedTag) "✓ " else ""
                        val label = option.nativeName.ifBlank { stringResource(R.string.use_system_language) }
                        Text("$marker$label")
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = { onFilterSelected(FilterPreset.LARGE_ONLY) }) { Text(stringResource(R.string.big_files)) }
                    TextButton(onClick = { onFilterSelected(FilterPreset.OLD_ONLY) }) { Text(stringResource(R.string.old_files)) }
                }
            }
        }
        return
    }

    var dragX by remember { mutableFloatStateOf(0f) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDrag = { _, dragAmount -> dragX += dragAmount.x },
                    onDragEnd = {
                        when {
                            dragX > 180f -> onAction(SwipeAction.KEEP)
                            dragX < -180f -> onAction(SwipeAction.DELETE)
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
                text = stringResource(R.string.media_meta, item.kind.name, Formatters.bytesToHumanReadable(item.sizeBytes)),
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
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
