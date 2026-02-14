package com.swipecleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: SwipeCleanerViewModel by viewModels {
        SwipeCleanerViewModel.Factory(
            context = applicationContext,
            contentResolver = contentResolver,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { viewModel.onPermissionResult(it.values.all { granted -> granted }) }

            val deleteLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                viewModel.onDeleteConfirmationResult(result.resultCode == RESULT_OK)
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.RequestPermission -> permissionLauncher.launch(event.permissions)
                        is UiEvent.LaunchDeleteConfirmation -> {
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(event.intentSender).build()
                            )
                        }
                    }
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SwipeCleanerScreen(
                        state = state,
                        onRequestPermissions = { viewModel.requestPermissions() },
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null),
                            )
                            startActivity(intent)
                        },
                        onAction = viewModel::onCardAction,
                        onUndo = viewModel::undo,
                        onConfirmDelete = viewModel::confirmDeletion,
                        onFilterSelected = viewModel::setFilterPreset,
                        onBuyPro = { viewModel.buyPro(this@MainActivity) },
                        onRestorePurchases = viewModel::restorePurchases,
                        onClosePaywall = viewModel::closePaywall,
                        onDismissDeletionSuccess = viewModel::dismissDeletionSuccessDialog,
                        onRateApp = viewModel::dismissDeletionSuccessDialog,
                    )
                }
            }
        }
    }
}
