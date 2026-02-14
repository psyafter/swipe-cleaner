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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
) {
    if (!state.hasPermission) {
        PermissionScreen(onRequestPermissions)
        return
    }

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
        Text("Queue: ${state.remainingCount} items")
        Text("Marked for delete: ${state.selectedForDeleteCount}")
        Text("You can free ${Formatters.bytesToHumanReadable(state.selectedDeleteSizeBytes)}")

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
            Button(onClick = { onAction(SwipeAction.ARCHIVE) }, enabled = state.currentItem != null) { Text("Archive") }
            Button(onClick = { onAction(SwipeAction.MOVE) }, enabled = state.currentItem != null) { Text("Move") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onUndo, enabled = state.lastAction != null) { Text("Undo") }
            Button(onClick = onConfirmDelete, enabled = state.selectedForDeleteCount > 0) { Text("Delete selected") }
        }

        state.infoMessage?.let { Text(it, color = Color.Gray) }
    }
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
                Text("Done for now. Scan complete.")
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
