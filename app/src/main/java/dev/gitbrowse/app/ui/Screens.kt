package dev.gitbrowse.app.ui


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
import dev.gitbrowse.app.data.GitHubApi
import dev.gitbrowse.app.data.GitHubApiException
import dev.gitbrowse.app.data.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException







@Composable
fun GitBrowseApp(){
    PracticeTheme {
        HomeScreen()
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
private fun HomeScreen(){
    var query by rememberSaveable {mutableStateOf("")}
    var selectedTab by rememberSaveable {
        mutableStateOf("Repositories")
    }
    var submittedQuery by remember{
        mutableStateOf<String?>(null)
    }

    var results by remember {
        mutableStateOf<List<SearchResult>>(emptyList())
    }

    var isLoading by remember {mutableStateOf(false)}
    var errorMessage by remember {mutableStateOf<String?>(null)}

    val scope = rememberCoroutineScope()
    var searchJob by remember {mutableStateOf<Job?>(null)}
    val focusManager = LocalFocusManager.current

    fun submitSearch(){
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty() || isLoading) return

        val searchPeople = selectedTab == "People"

        focusManager.clearFocus()
        searchJob?.cancel()

        submittedQuery = cleanQuery
        results = emptyList()
        errorMessage = null
        isLoading = true

        searchJob = scope.launch {
            try {
                results = GitHubApi.search(
                    query = cleanQuery,
                    searchPeople = searchPeople
                )
            } catch (e: CancellationException){
                throw e
            } catch (e: GitHubApiException) {
                errorMessage = e.message
            } catch (e:IOException) {
                errorMessage = "Could not connect to Github"
            } catch (e:Exception) {
                errorMessage = "Could not read Github's response"
            }
            isLoading = false
        }

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
                            searchJob?.cancel()
                            selectedTab = tab
                            submittedQuery = null
                            results = emptyList()
                            errorMessage= null
                            isLoading = false
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

                            ) { result -> ResultCard(result) }

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: SearchResult){
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        border = BorderStroke(1.dp, Color.DarkGray),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
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