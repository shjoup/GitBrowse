package dev.gitbrowse.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitbrowse.app.data.GitHubApi
import dev.gitbrowse.app.data.GitHubApiException
import dev.gitbrowse.app.data.SearchResult
import dev.gitbrowse.app.data.RepositoryDetails
import dev.gitbrowse.app.data.ProfileDetails
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
data class RepositoryUiState(
    val fullName: String? = null,
    val repository: RepositoryDetails? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val readme: String? = null,
    val isLoadingReadme: Boolean = false,
    val readmeError: String? = null
)

data class ProfileUiState(
    val login: String? = null,
    val profile: ProfileDetails? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val repositories: List<SearchResult> = emptyList(),
    val isLoadingRepositories: Boolean = false,
    val repositoriesError: String? = null
)

class SearchViewModel : ViewModel() {


    private var profileReposJob: Job? = null
    private val mutableRepository = MutableStateFlow(
        RepositoryUiState()
    )

    val repositoryState:StateFlow<RepositoryUiState> =
        mutableRepository.asStateFlow()

    private var repositoryJob:Job? = null
    private var readmeJob: Job? = null

    fun openRepository(fullName:String){
        repositoryJob?.cancel()
        readmeJob?.cancel()
        readmeJob = null

        mutableRepository.value = RepositoryUiState(
            fullName = fullName,
            isLoading = true
        )

        repositoryJob = viewModelScope.launch {
            try {
                val repository = GitHubApi.repository(fullName)
                mutableRepository.value = RepositoryUiState(
                    fullName = fullName,
                    repository = repository
                )
                loadReadme()
            } catch (e: CancellationException){
                throw e
            } catch (e: Exception){
                val message = when (e){
                    is GitHubApiException ->
                        e.message ?:"Could not load repository"

                    is IOException ->
                        "Could not connect to github"
                    else ->
                        "Could not read repository details"
                }

                mutableRepository.value = RepositoryUiState(
                    fullName = fullName,
                    errorMessage = message
                )
            }
        }
    }

    fun closeRepository(){
        repositoryJob?.cancel()
        repositoryJob = null
        readmeJob?.cancel()
        readmeJob = null
        mutableRepository.value = RepositoryUiState()
    }

    fun loadReadme() {
        val current = mutableRepository.value
        val fullName = current.fullName ?: return

        if (current.repository == null || current.isLoadingReadme) {
            return
        }

        readmeJob?.cancel()
        mutableRepository.value = current.copy(
            isLoadingReadme = true,
            readmeError = null
        )

        readmeJob = viewModelScope.launch {
            try {
                val readme = GitHubApi.readme(fullName)
                mutableRepository.value = mutableRepository.value.copy(
                    readme = readme,
                    isLoadingReadme = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = when (e) {
                    is GitHubApiException ->
                        e.message ?: "Could not load README."

                    is IOException ->
                        "Could not connect to GitHub."

                    else ->
                        "Could not read the README."
                }

                mutableRepository.value = mutableRepository.value.copy(
                    isLoadingReadme = false,
                    readmeError = message
                )
            }
        }
    }

    private val mutableProfile = MutableStateFlow(
        ProfileUiState()
    )

    val profileState:StateFlow<ProfileUiState> =
        mutableProfile.asStateFlow()

    private var profileJob:Job? = null
    fun openProfile(login:String){
        profileJob?.cancel()
        profileReposJob?.cancel()

        mutableProfile.value = ProfileUiState(
            login = login,
            isLoading = true
        )

        profileJob = viewModelScope.launch {
            try {
                val profile = GitHubApi.profile(login)
                mutableProfile.value = ProfileUiState(
                    login = login,
                    profile = profile
                )

                loadProfileRepositories()
            } catch (e: CancellationException){
                throw e
            } catch (e: Exception){
                val message = when (e){
                    is GitHubApiException ->
                        e.message ?:"Could not load profile"

                    is IOException ->
                        "Could not connect to github"
                    else ->
                        "Could not read profile details"
                }

                mutableProfile.value = ProfileUiState(
                    login = login,
                    errorMessage = message
                )
            }
        }
    }

    fun closeProfile(){
        profileJob?.cancel()
        profileJob = null

        profileReposJob?.cancel()
        profileReposJob = null
        mutableProfile.value = ProfileUiState()
    }

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

    fun loadProfileRepositories() {
        val current = mutableProfile.value
        val login = current.login ?: return

        if (current.profile == null || current.isLoadingRepositories) {
            return
        }

        profileReposJob?.cancel()

        mutableProfile.value = current.copy(
            isLoadingRepositories = true,
            repositoriesError = null
        )

        profileReposJob = viewModelScope.launch {
            try {
                val repositories = GitHubApi.profileRepositories(login)

                mutableProfile.value = mutableProfile.value.copy(
                    repositories = repositories,
                    isLoadingRepositories = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = when (e) {
                    is GitHubApiException ->
                        e.message ?: "Could not load repositories."

                    is IOException ->
                        "Could not connect to GitHub."

                    else ->
                        "Could not read the repository list."
                }

                mutableProfile.value = mutableProfile.value.copy(
                    isLoadingRepositories = false,
                    repositoriesError = message
                )
            }
        }
    }
}
