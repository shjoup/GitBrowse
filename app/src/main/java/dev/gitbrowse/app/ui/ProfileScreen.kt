package dev.gitbrowse.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    onRetry: () -> Unit,
    onRetryRepositories: () -> Unit,
    onOpenRepository: (String) -> Unit
) {
    val profile = state.profile
    val error = state.errorMessage
    val repositoriesError = state.repositoriesError
    val listState = rememberLazyListState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "back") {
                OutlinedButton(
                    onClick = onBack,
                    shape = RectangleShape
                ) {
                    Text("Back")
                }
            }

            when {
                state.isLoading -> {
                    item(key = "profile-loading") {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    item(key = "profile-error") {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            Text(error)

                            OutlinedButton(
                                onClick = onRetry,
                                shape = RectangleShape
                            ) {
                                Text("Try again")
                            }
                        }
                    }
                }

                profile != null -> {
                    item(key = "profile-header") {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = profile.name
                                    ?.takeIf { it.isNotBlank() }
                                    ?: profile.login,
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Text("@${profile.login}")

                            profile.bio
                                ?.takeIf { it.isNotBlank() }
                                ?.let { Text(it) }

                            profile.location
                                ?.takeIf { it.isNotBlank() }
                                ?.let { Text("Location: $it") }

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RectangleShape
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Followers: ${profile.followers}")
                                    Text("Following: ${profile.following}")
                                    Text("Public repositories: ${profile.publicRepos}")
                                }
                            }
                        }
                    }

                    item(key = "repositories-title") {
                        Text(
                            text = "Public repositories",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (state.isLoadingRepositories) {
                        item(key = "repositories-loading") {
                            CircularProgressIndicator()
                        }
                    }

                    if (repositoriesError != null) {
                        item(key = "repositories-error") {
                            Column(
                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                Text(repositoriesError)

                                OutlinedButton(
                                    onClick = onRetryRepositories,
                                    shape = RectangleShape
                                ) {
                                    Text("Retry repositories")
                                }
                            }
                        }
                    }

                    if (
                        !state.isLoadingRepositories &&
                        repositoriesError == null &&
                        state.repositories.isEmpty()
                    ) {
                        item(key = "repositories-empty") {
                            Text("No public repositories.")
                        }
                    }

                    items(
                        items = state.repositories,
                        key = { repository -> "repo:${repository.id}" }
                    ) { repository ->
                        ResultCard(
                            result = repository,
                            onClick = {
                                repository.repositoryFullName?.let { fullName ->
                                    onOpenRepository(fullName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}