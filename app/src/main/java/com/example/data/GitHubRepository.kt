package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val statusCode: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

class GitHubRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Android-GitHub-App")
                .build()
            chain.proceed(request)
        }
        .build()

    private val apiService: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApiService::class.java)

    private fun formatAuthHeader(token: String): String {
        val cleanToken = token.trim()
        return if (cleanToken.startsWith("token ") || cleanToken.startsWith("Bearer ")) {
            cleanToken
        } else {
            "Bearer $cleanToken"
        }
    }

    suspend fun getAuthenticatedUser(token: String): ApiResult<GitHubUser> {
        return try {
            val response = apiService.getAuthenticatedUser(formatAuthHeader(token))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(
                    message = when (response.code()) {
                        401 -> "Invalid Personal Access Token. Please check token permissions."
                        403 -> "API Rate limit exceeded or access forbidden."
                        else -> "Failed to authenticate: ${response.message()} (Code ${response.code()})"
                    },
                    statusCode = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun getUserRepos(token: String): ApiResult<List<GitHubRepo>> {
        return try {
            val response = apiService.getUserRepos(formatAuthHeader(token))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to fetch repositories: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to GitHub API")
        }
    }

    suspend fun getUserIssues(token: String): ApiResult<List<GitHubIssue>> {
        return try {
            val response = apiService.getUserIssues(formatAuthHeader(token))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to fetch issues: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to GitHub API")
        }
    }

    suspend fun getRepoIssues(token: String, owner: String, repo: String): ApiResult<List<GitHubIssue>> {
        return try {
            val response = apiService.getRepoIssues(formatAuthHeader(token), owner, repo)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to fetch issues for $owner/$repo: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to GitHub API")
        }
    }

    suspend fun getRepoWorkflowRuns(token: String, owner: String, repo: String): ApiResult<List<GitHubWorkflowRun>> {
        return try {
            val response = apiService.getRepoWorkflowRuns(formatAuthHeader(token), owner, repo)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.workflowRuns)
            } else {
                ApiResult.Error("Failed to fetch workflow runs: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to GitHub API")
        }
    }

    suspend fun createRepository(token: String, name: String, description: String?, isPrivate: Boolean): ApiResult<GitHubRepo> {
        return try {
            val request = CreateRepoRequest(name = name, description = description, private = isPrivate)
            val response = apiService.createRepository(formatAuthHeader(token), request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to create repo: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to GitHub API")
        }
    }
}
