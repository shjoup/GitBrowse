package dev.gitbrowse.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitbrowse.app.data.GitHubApi
import dev.gitbrowse.app.data.GitHubApiException
import dev.gitbrowse.app.data.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException


data class SearchUiState(
    val submittedQuery: String? = null,
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SearchViewModel : ViewModel() {

    private val mutableState = MutableStateFlow(SearchUiState())


    val uiState: StateFlow<SearchUiState> =
        mutableState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String, searchPeople: Boolean) {
        val cleanQuery = query.trim()

        if (cleanQuery.isEmpty() || mutableState.value.isLoading) {
            return
        }

        searchJob?.cancel()

        mutableState.value = SearchUiState(
            submittedQuery = cleanQuery,
            isLoading = true
        )

        searchJob = viewModelScope.launch {
            try {
                val results = GitHubApi.search(
                    query = cleanQuery,
                    searchPeople = searchPeople
                )

                mutableState.value = mutableState.value.copy(
                    results = results
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubApiException) {
                mutableState.value = mutableState.value.copy(
                    errorMessage = e.message ?: "Search failed."
                )
            } catch (e: IOException) {
                mutableState.value = mutableState.value.copy(
                    errorMessage =
                        "Could not connect to GitHub. Check your connection."
                )
            } catch (e: Exception) {
                mutableState.value = mutableState.value.copy(
                    errorMessage = "Could not read GitHub's response."
                )
            }

            mutableState.value = mutableState.value.copy(
                isLoading = false
            )
        }
    }

    fun reset() {
        searchJob?.cancel()
        searchJob = null
        mutableState.value = SearchUiState()
    }
}