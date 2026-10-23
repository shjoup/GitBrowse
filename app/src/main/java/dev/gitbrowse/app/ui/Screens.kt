package dev.gitbrowse.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.gitbrowse.app.SearchViewModel
import dev.gitbrowse.app.data.RepositoryDetails
import dev.gitbrowse.app.data.SearchResult





@Composable
fun GitBrowseApp(model: SearchViewModel = viewModel()){
    val repositoryState by model.repositoryState.collectAsStateWithLifecycle()
    val profileState by model.profileState.collectAsStateWithLifecycle()
    val selectedRepository = repositoryState.fullName
    val selectedProfile = profileState.login
    val savedScreens = rememberSaveableStateHolder()

    BackHandler(enabled = selectedRepository != null || selectedProfile != null) {
        if (selectedRepository != null) {
            model.closeRepository()
        } else {
            selectedProfile?.let {
                login -> savedScreens.removeState("profile:$login")
            }

            model.closeProfile()
        }
    }

    PracticeTheme{
        when {
            selectedRepository != null -> {
                RepositoryScreen(
                    state = repositoryState,
                    onBack = model::closeRepository,
                    onRetry = {
                        model.openRepository(selectedRepository)
                    },
                    onRetryReadme = model::loadReadme
                )
            }
            selectedProfile != null -> {
                savedScreens.SaveableStateProvider(
                    "profile:$selectedProfile"
                ) {
                    ProfileScreen(
                        state = profileState,
                        onBack = {
                            savedScreens.removeState("profile:$selectedProfile")
                            model.closeProfile()
                        },
                        onRetry = {
                            model.openProfile(selectedProfile)
                        },
                        onRetryRepositories = model::loadProfileRepositories,
                        onOpenRepository = model::openRepository
                    )
                }
            }
            else -> {
                savedScreens.SaveableStateProvider("search") {
                    HomeScreen(model = model)
                }
            }
        }
    }
}


@Composable
private fun PracticeTheme(content: @Composable () -> Unit){
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray,
            outline = Color.Gray,
            surfaceTint = Color.White,
        ),
        content = content
    )
}


@Composable
private fun HomeScreen(model: SearchViewModel = viewModel()){
    var query by rememberSaveable {
        mutableStateOf("")
    }
    var selectedTab by rememberSaveable{
        mutableStateOf("Repositories")
    }

    val uiState by model.uiState.collectAsStateWithLifecycle()

    val submittedQuery = uiState.submittedQuery
    val results = uiState.results
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage

    val focusManager = LocalFocusManager.current

    fun submitSearch(){
        if (query.isBlank() || isLoading) return

        focusManager.clearFocus()

        model.search(
            query = query,
            searchPeople = selectedTab == "People"
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text("Search ${selectedTab.lowercase()}...")
                },
                singleLine = true,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { submitSearch() }
                )
            )


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {
                listOf("Repositories", "People").forEach { tab ->
                    val selected = selectedTab == tab


                    OutlinedButton(
                        onClick = {
                            if (selectedTab != tab){
                                selectedTab = tab
                                model.reset()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RectangleShape,
                        border = BorderStroke(
                            1.dp,
                            if (selected) Color.White else Color.Gray,
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor =
                                if (selected) Color.White else Color.Black,
                            contentColor =
                                if (selected) Color.Black else Color.White

                        )
                    ) {
                        Text(tab)
                    }

                }
            }


            Button(
                onClick = { submitSearch() },
                enabled = query.isNotBlank() && !isLoading,
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(if(isLoading) "Searching..." else "Search")
            }

            submittedQuery?.let { submitted ->
                Text(
                    text = "Results for \"$submitted\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = Color.White
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = Color.LightGray
                        )
                    }

                    results.isEmpty() -> {
                        Text(
                            text = "No matches found",
                            color = Color.LightGray
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = results,
                                key = { result -> result.id }

                            ) { result -> ResultCard(
                                result = result,
                                onClick = {
                                    val repository = result.repositoryFullName
                                    val profile = result.profileLogin

                                    when{
                                        repository != null ->{
                                            model.openRepository(repository)
                                        }
                                        profile != null ->{
                                            model.openProfile(profile)
                                        }
                                    }
                                }
                            ) }

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultCard(
    result: SearchResult,
    onClick: () -> Unit
){
    OutlinedCard(
        onClick = onClick,
        enabled = result.repositoryFullName != null || result.profileLogin != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        border = BorderStroke(1.dp, Color.DarkGray),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,



        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = result.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
        }
    }
}
