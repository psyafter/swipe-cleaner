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
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDeleteNow: () -> Unit,
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
                    text = "Swipe Cleaner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onOpenSettingsSheet) { Text("Settings") }
            }

            FilterRow(active = state.activeFilter, onFilterSelected = onFilterSelected)

            Text(
                text = "You can free ${Formatters.bytesToHumanReadable(state.selectedDeleteSizeBytes)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Marked: ${state.selectedForDeleteCount} files")
            Text(
                if (state.isProUnlocked) "Pro unlocked: unlimited deletions"
                else "Free deletions left: $freeLeft",
                color = Color.Gray,
            )
            Text("Queue: ${state.remainingCount} items", color = Color.Gray)

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
                OutlinedButton(onClick = { onAction(SwipeAction.KEEP) }, enabled = state.currentItem != null) { Text("Keep") }
                OutlinedButton(onClick = { onAction(SwipeAction.DELETE) }, enabled = state.currentItem != null) { Text("Delete") }
            }

            Text(
                "Deletion requires Android system confirmation. Nothing leaves your device.",
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
            onSetRequireDeleteConfirmation = onSetRequireDeleteConfirmation,
            onDismiss = onCloseSettingsSheet,
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
        Text("Clean your gallery in minutes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) { Text("Swipe right to keep. Swipe left to mark for delete.", modifier = Modifier.padding(16.dp)) }
        Card(Modifier.fillMaxWidth()) { Text("Private by design: media stays on your device.", modifier = Modifier.padding(16.dp)) }
        Card(Modifier.fillMaxWidth()) { Text("Review selected files and free space fast.", modifier = Modifier.padding(16.dp)) }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onStartCleaning, modifier = Modifier.fillMaxWidth()) {
            Text("Start cleaning")
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
            Text("Undo")
        }
        Button(onClick = onDelete, enabled = canDelete, modifier = Modifier.weight(2f)) {
            Text("Free space ($selectedCount)")
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

private fun filterLabel(preset: FilterPreset): String {
    return when (preset) {
        FilterPreset.ALL -> "All"
        FilterPreset.LARGE_ONLY -> "Big"
        FilterPreset.OLD_ONLY -> "Old"
        FilterPreset.SCREENSHOTS -> "Shots"
        FilterPreset.WHATSAPP_MEDIA -> "WhatsApp"
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
        title = { Text("Swipe Cleaner Pro\nOne-time $2.99") },
        text = {
            Text(
                buildString {
                    append("You've reached the free delete limit.\n\n")
                    append("• Unlimited deletes\n")
                    append("• One-time purchase, no subscription\n")
                    append("• Offline and private-first")
                    if (!paywallMessage.isNullOrBlank()) {
                        append("\n\n")
                        append(paywallMessage)
                    }
                },
            )
        },
        confirmButton = {
            Button(onClick = onBuyPro) {
                Text("Upgrade to Pro")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRestorePurchases) { Text("Restore purchases") }
                TextButton(onClick = onDismiss) { Text("Not now") }
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
        title = { Text("Confirm cleaning session") },
        text = {
            Text(
                "Delete $selectedCount files and free about ${Formatters.bytesToHumanReadable(selectedSizeBytes)}?\n\nAndroid will ask for final system confirmation.",
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsDialog(
    requireDeleteConfirmation: Boolean,
    onSetRequireDeleteConfirmation: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Require confirmation before deleting session")
                Switch(
                    checked = requireDeleteConfirmation,
                    onCheckedChange = onSetRequireDeleteConfirmation,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
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
        title = { Text("Storage freed🎉") },
        text = { Text("Freed ${Formatters.bytesToHumanReadable(freedSizeBytes)} from $deletedCount files") },
        confirmButton = {
            Button(onClick = onContinueCleaning) {
                Text("Continue cleaning")
            }
        },
        dismissButton = {
            TextButton(onClick = onRateApp) {
                Text("Rate app")
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
        Text("Need media permissions to start cleaning")
        Button(onClick = onRequestPermissions, modifier = Modifier.padding(top = 12.dp)) {
            Text("Grant access")
        }
        if (isPermissionDenied) {
            Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                Text("Open Settings")
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
                Text("All done", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("No more items in this queue.")
                Button(onClick = onRescan, modifier = Modifier.padding(top = 12.dp)) { Text("Rescan") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = { onFilterSelected(FilterPreset.LARGE_ONLY) }) { Text("Big files") }
                    TextButton(onClick = { onFilterSelected(FilterPreset.OLD_ONLY) }) { Text("Old files") }
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
                text = "${item.kind} • ${Formatters.bytesToHumanReadable(item.sizeBytes)}",
                modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.6f)).padding(8.dp),
                color = Color.White,
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Pill("R→Keep")
                Pill("L→Delete")
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
