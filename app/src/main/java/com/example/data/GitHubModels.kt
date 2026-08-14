package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "html_url") val htmlUrl: String?,
    val name: String?,
    val company: String?,
    val blog: String?,
    val location: String?,
    val bio: String?,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    @Json(name = "public_gists") val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val private: Boolean = false,
    @Json(name = "html_url") val htmlUrl: String,
    val description: String?,
    val fork: Boolean = false,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "watchers_count") val watchersCount: Int = 0,
    val language: String?,
    @Json(name = "forks_count") val forksCount: Int = 0,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    val owner: GitHubUserOwner?
)

@JsonClass(generateAdapter = true)
data class GitHubUserOwner(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class GitHubIssue(
    val id: Long,
    val number: Int,
    val title: String,
    val user: GitHubUserOwner?,
    val state: String,
    val comments: Int = 0,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "html_url") val htmlUrl: String,
    val body: String?,
    @Json(name = "pull_request") val pullRequest: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRun>
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRun(
    val id: Long,
    val name: String?,
    @Json(name = "head_branch") val headBranch: String?,
    @Json(name = "run_number") val runNumber: Int,
    val status: String?,
    val conclusion: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String?,
    val private: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitHubNotification(
    val id: String,
    val unread: Boolean = true,
    val reason: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val subject: NotificationSubject?,
    val repository: GitHubRepo?
)

@JsonClass(generateAdapter = true)
data class NotificationSubject(
    val title: String?,
    val type: String?,
    val url: String?,
    @Json(name = "latest_comment_url") val latestCommentUrl: String?
)

@JsonClass(generateAdapter = true)
data class SearchReposResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "incomplete_results") val incompleteResults: Boolean = false,
    val items: List<GitHubRepo>
)
