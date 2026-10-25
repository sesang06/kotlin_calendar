package com.kotlincalendar.client.presentation.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EventDetailRoute(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EventDetailEffect.NavigateBack -> onBack()
                EventDetailEffect.Deleted -> onDeleted()
            }
        }
    }

    LaunchedEffect(state.deleteErrorMessage) {
        state.deleteErrorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onAction(EventDetailAction.DismissError)
        }
    }

    EventDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAction: (EventDetailAction) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
        ) {
            TextButton(
                onClick = { onAction(EventDetailAction.Back) },
                enabled = !state.isDeleting,
            ) {
                Text("← 뒤로")
            }

            Spacer(modifier = Modifier.height(16.dp))

            val loadError = state.loadErrorMessage
            when {
                state.isLoading -> {
                    CenteredBox { CircularProgressIndicator() }
                }
                loadError != null -> {
                    CenteredBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = loadError,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { onAction(EventDetailAction.Retry) }) {
                                Text("다시 시도")
                            }
                        }
                    }
                }
                state.isLoaded -> {
                    EventDetailContent(state = state, onAction = onAction)
                }
            }
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onAction(EventDetailAction.CancelDelete) },
            title = { Text("일정 삭제") },
            text = { Text("이 일정을 삭제할까요? 삭제한 일정은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { onAction(EventDetailAction.ConfirmDelete) }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(EventDetailAction.CancelDelete) }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun EventDetailContent(
    state: EventDetailUiState,
    onAction: (EventDetailAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = state.timeRangeText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.description.isNotEmpty()) {
            Text(
                text = state.description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onAction(EventDetailAction.RequestDelete) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.isDeleting,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
        ) {
            if (state.isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onError,
                )
            } else {
                Text("삭제")
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
