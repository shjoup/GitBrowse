package dev.gitbrowse.app.ui


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.gitbrowse.app.ProfileUiState
@Composable


fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val profile = state.profile
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
                    Text("Loading profile...")
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
                profile != null ->{
                    Text(
                        text=profile.name?.takeIf { it.isNotBlank() }?: profile.login,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text (
                        text = "@${profile.login}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    profile.bio
                        ?.takeIf { it.isNotBlank() }
                        ?.let { bio ->
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                    profile.location
                        ?.takeIf { it.isNotBlank() }
                        ?.let { location ->
                            Text(
                                "Location: $location"
                            )
                        }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Following: ${profile.following}")
                            Text("Followers: ${profile.followers}")
                            Text("Public repositories: ${profile.publicRepos}")
                        }
                    }
                }
            }
        }
    }
}