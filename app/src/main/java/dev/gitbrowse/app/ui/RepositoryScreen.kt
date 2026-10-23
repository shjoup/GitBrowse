package dev.gitbrowse.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.gitbrowse.app.RepositoryUiState

@Composable
fun RepositoryScreen(
    state: RepositoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryReadme: () -> Unit
) {
    val repository = state.repository
    val error = state.errorMessage

    Surface(
        modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RectangleShape
            ) {
                Text("Back")
            }
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                    Text("Loading repository...")
                }
                error != null -> {
                    Text(error)
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RectangleShape
                    ) {
                        Text("Try again")
                    }
                }
                repository != null ->{
                    Text (
                        text = repository.fullName,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text (
                        text = repository.description,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Stars: ${repository.stars}")
                            Text("Forks: ${repository.forks}")
                            Text("Language: ${repository.language}")
                        }
                    }

                    ReadmeSection(
                        state = state,
                        onRetry = onRetryReadme
                    )
                }
            }
        }
    }
}
