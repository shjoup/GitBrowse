package dev.gitbrowse.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder


data class SearchResult(
    val id: Long,
    val title: String,
    val description: String,
    val repositoryFullName: String?= null,
    val profileLogin: String? = null,
)

data class SearchPage(
    val items: List<SearchResult>,
    val hasNextPage: Boolean
)

class GitHubApiException(message: String) : Exception(message)

data class RepositoryDetails(
    val fullName: String,
    val description: String,
    val stars: Int,
    val forks: Int,
    val language: String
)

data class ProfileDetails(
    val login: String,
    val name: String?,
    val bio: String?,
    val location: String?,
    val followers: Int,
    val following: Int,
    val publicRepos: Int
)



object GitHubApi {
    private const val SEARCH_PAGE_SIZE = 30
    private const val MAX_SEARCH_RESULTS = 1_000

    suspend fun search(
        query: String,
        searchPeople: Boolean,
        page: Int = 1
    ): SearchPage = withContext(Dispatchers.IO) {
        val lastPage = (MAX_SEARCH_RESULTS + SEARCH_PAGE_SIZE - 1) / SEARCH_PAGE_SIZE
        require(page in 1..lastPage) { "Search page is outside GitHub's result limit." }

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val endpoint = if (searchPeople) "users" else "repositories"

        val url = URI.create(
            "https://api.github.com/search/$endpoint" +
                    "?q=$encodedQuery&per_page=$SEARCH_PAGE_SIZE&page=$page"
        ).toURL()

        val connection = url.openConnection() as HttpURLConnection

        try {
            currentCoroutineContext().ensureActive()

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            )
            connection.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            )
            connection.setRequestProperty(
                "User-Agent",
                "GitBrowse-Android"
            )
            val status = connection.responseCode

            if (status !in 200..299){
                val message = when {
                    status == 403 ->
                        "Github declined the request"

                    status == 429 ->
                        "Github's rate limit has been reached"

                    status == 422 ->
                        "Github could not understand this search"

                    status >= 500 ->
                        "Github is having trouble. Try again shortly"

                    else ->
                        "Search failed (HTTP $status)"
                }

                throw GitHubApiException(message)
            }
            val body = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use {it.readText()}

            currentCoroutineContext().ensureActive()

            val items = JSONObject(body).getJSONArray("items")

            val results = List(items.length()) {index ->
                val item = items.getJSONObject(index)

                SearchResult(
                    id = item.getLong("id"),
                    title = if (searchPeople) {
                        item.getString("login")
                    } else {
                        item.getString("full_name")
                    },
                    description = if (searchPeople) {
                        item.optString("type", "GitHub Profile")
                    } else if (item.isNull("description")) {
                        "No description available"
                    } else {
                        item.getString("description")
                    },
                    repositoryFullName = if(searchPeople){
                        null
                    } else {
                        item.getString("full_name")
                    },
                    profileLogin = if(searchPeople){
                        item.getString("login")
                    } else {
                        null
                    }
                )
            }

            val hasNextLink = connection.getHeaderField("Link")
                ?.split(',')
                ?.any { it.contains("rel=\"next\"") } == true

            SearchPage(
                items = results.take(MAX_SEARCH_RESULTS - (page - 1) * SEARCH_PAGE_SIZE),
                hasNextPage = hasNextLink && results.isNotEmpty() &&
                    page * SEARCH_PAGE_SIZE < MAX_SEARCH_RESULTS
            )
        } finally {
            connection.disconnect()
        }
    }
    suspend fun repository(
        fullName: String
    ): RepositoryDetails = withContext(Dispatchers.IO){
        val parts = fullName.split('/')

        require(parts.size == 2 && parts.all {it.isNotBlank()}){
            "Expected owner/repo"
        }

        val path = parts.joinToString("/"){
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }
        val connection = URI.create(
            "https://api.github.com/repos/$path"
        ).toURL().openConnection() as HttpURLConnection

        try {
            currentCoroutineContext().ensureActive()

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            )
            connection.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            )
            connection.setRequestProperty(
                "User-Agent",
                "GitBrowse-Android"
            )
            val status = connection.responseCode
            currentCoroutineContext().ensureActive()

            if (status !in 200..299) {
                val message = when(status){
                    404 -> "This repository is no longer available"
                    403 -> "Github declined this request"
                    429 -> "Github's rate limit was reached"
                    else -> "Could not load repository (HTTP $status$)"
                }

                throw GitHubApiException(message)
            }
            val body = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use {it.readText()}
            currentCoroutineContext().ensureActive()

            val json = JSONObject(body)

            RepositoryDetails(
                fullName = json.getString("full_name"),
                description = if (json.isNull("description")){
                    "No description available"
                } else {
                    json.getString("description")
                },
                stars = json.getInt("stargazers_count"),
                forks = json.getInt("forks_count"),
                language = if(json.isNull("language")){
                    "Not specified"
                } else {
                    json.getString("language")
                }
            )
        } finally{
            connection.disconnect()
        }
    }
    suspend fun profile(
        login: String
    ): ProfileDetails = withContext(Dispatchers.IO){
        val encodedLogin = URLEncoder.encode(
            login,
            "UTF-8"
        ).replace("+", "%20")


        val connection = URI.create(
            "https://api.github.com/users/$encodedLogin"
        ).toURL().openConnection() as HttpURLConnection

        try {
            currentCoroutineContext().ensureActive()

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            )
            connection.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            )
            connection.setRequestProperty(
                "User-Agent",
                "GitBrowse-Android"
            )
            val status = connection.responseCode
            currentCoroutineContext().ensureActive()

            if (status !in 200..299) {
                val message = when(status){
                    404 -> "This repository is no longer available"
                    403 -> "Github declined this request"
                    429 -> "Github's rate limit was reached"
                    else -> "Could not load repository (HTTP $status$)"
                }

                throw GitHubApiException(message)
            }
            val body = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use {it.readText()}
            currentCoroutineContext().ensureActive()

            val json = JSONObject(body)

            ProfileDetails(
                login = json.getString("login"),
                name = if (json.isNull("name")){
                    null
                } else {
                    json.getString("name")
                },
                bio = if (json.isNull("bio")){
                    null
                } else {
                    json.getString("bio")
                },
                location = if (json.isNull("location")){
                    null
                } else {
                    json.getString("location")
                },
                followers = json.getInt("followers"),
                following = json.getInt("following"),
                publicRepos = json.getInt("public_repos"),
            )
        } finally{
            connection.disconnect()
        }
    }
    suspend fun profileRepositories(
        login: String
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val encodedLogin = URLEncoder.encode(
            login,
            "UTF-8"
        ).replace("+", "%20")

        val connection = URI.create(
            "https://api.github.com/users/$encodedLogin/repos" +
                    "?sort=updated&direction=desc&per_page=30&page=1"
        ).toURL().openConnection() as HttpURLConnection

        try {
            currentCoroutineContext().ensureActive()

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000

            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            )
            connection.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            )
            connection.setRequestProperty(
                "User-Agent",
                "GitBrowse-Android"
            )

            val status = connection.responseCode
            currentCoroutineContext().ensureActive()

            if (status !in 200..299) {
                val message = when (status) {
                    404 -> "This profile is no longer available."
                    403 -> "GitHub declined this request."
                    429 -> "GitHub's rate limit was reached."
                    else -> "Could not load repositories (HTTP $status)."
                }

                throw GitHubApiException(message)
            }

            val body = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            currentCoroutineContext().ensureActive()

            val items = JSONArray(body)

            List(items.length()) { index ->
                val item = items.getJSONObject(index)

                SearchResult(
                    id = item.getLong("id"),
                    title = item.getString("full_name"),
                    description = if (item.isNull("description")) {
                        "No description available."
                    } else {
                        item.getString("description")
                    },
                    repositoryFullName = item.getString("full_name")
                )
            }
        } finally {
            connection.disconnect()
        }
    }
    suspend fun readme(
        fullName: String
    ): String? = withContext(Dispatchers.IO) {
        val parts = fullName.split('/')

        require(parts.size == 2 && parts.all { it.isNotBlank() }) {
            "Expected owner/repository"
        }

        val path = parts.joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }

        val connection = URI.create(
            "https://api.github.com/repos/$path/readme"
        ).toURL().openConnection() as HttpURLConnection

        try {
            currentCoroutineContext().ensureActive()

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000

            connection.setRequestProperty(
                "Accept",
                "application/vnd.github.raw+json"
            )
            connection.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            )
            connection.setRequestProperty(
                "User-Agent",
                "GitBrowse-Android"
            )

            val status = connection.responseCode
            currentCoroutineContext().ensureActive()

            if (status == 404) {
                return@withContext null
            }

            if (status !in 200..299) {
                val message = when (status) {
                    403 -> "GitHub declined the README request."
                    429 -> "GitHub's rate limit was reached."
                    else -> "Could not load README (HTTP $status)."
                }

                throw GitHubApiException(message)
            }

            val text = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            currentCoroutineContext().ensureActive()

            text
        } finally {
            connection.disconnect()
        }
    }
}
