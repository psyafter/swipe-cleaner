package com.swipecleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
    onAction: (SwipeAction) -> Unit,
    onUndo: () -> Unit,
    onConfirmDelete: () -> Unit,
    onFilterSelected: (FilterPreset) -> Unit,
    onBuyPro: () -> Unit,
    onRestorePurchases: () -> Unit,
    onClosePaywall: () -> Unit,
) {
    if (!state.hasPermission) {
        PermissionScreen(onRequestPermissions)
        return
    }

    val freeLeft = (FREE_DELETE_LIMIT - state.freeDeleteUsedCount).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Swipe Cleaner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        FilterRow(active = state.activeFilter, onFilterSelected = onFilterSelected)

        Text("Queue: ${state.remainingCount} items")
        Text("Marked for delete: ${state.selectedForDeleteCount}")
        Text("You can free ${Formatters.bytesToHumanReadable(state.selectedDeleteSizeBytes)}")
        Text(
            if (state.isProUnlocked) "Pro unlocked: unlimited deletions"
            else "Free deletions left: $freeLeft",
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            MediaCard(state.currentItem, onAction)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAction(SwipeAction.KEEP) }, enabled = state.currentItem != null) { Text("Keep") }
            Button(onClick = { onAction(SwipeAction.DELETE) }, enabled = state.currentItem != null) { Text("Delete") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onUndo, enabled = state.lastAction != null) { Text("Undo") }
            Button(onClick = onConfirmDelete, enabled = state.selectedForDeleteCount > 0) { Text("Delete selected") }
        }

        state.infoMessage?.let { Text(it, color = Color.Gray) }
    }

    if (state.showPaywall) {
        PaywallDialog(
            onBuyPro = onBuyPro,
            onRestorePurchases = onRestorePurchases,
            onDismiss = onClosePaywall,
        )
    }
}

@Composable
private fun FilterRow(active: FilterPreset, onFilterSelected: (FilterPreset) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterPreset.entries.forEach { preset ->
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
        FilterPreset.LARGE_ONLY -> "Large"
        FilterPreset.OLD_ONLY -> "Old"
        FilterPreset.SCREENSHOTS -> "Screenshots"
        FilterPreset.WHATSAPP_MEDIA -> "WhatsApp"
    }
}

@Composable
private fun PaywallDialog(
    onBuyPro: () -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pro unlock") },
        text = { Text("Pro unlock $2.99, unlimited deletions") },
        confirmButton = {
            Button(onClick = onBuyPro) {
                Text("Buy Pro")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRestorePurchases) { Text("Restore purchases") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
private fun PermissionScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Need media permissions to start cleaning")
        Button(onClick = onRequestPermissions, modifier = Modifier.padding(top = 12.dp)) {
            Text("Grant access")
        }
    }
}

@Composable
private fun MediaCard(item: MediaItem?, onAction: (SwipeAction) -> Unit) {
    if (item == null) {
        Card(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Done. Queue is empty.")
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
