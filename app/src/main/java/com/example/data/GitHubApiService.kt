package com.example.data

import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authHeader: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") authHeader: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50,
        @Query("type") type: String = "all"
    ): Response<List<GitHubRepo>>

    @GET("user/issues")
    suspend fun getUserIssues(
        @Header("Authorization") authHeader: String,
        @Query("filter") filter: String = "all",
        @Query("state") state: String = "all",
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50
    ): Response<List<GitHubIssue>>

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getRepoIssues(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "all",
        @Query("per_page") perPage: Int = 30
    ): Response<List<GitHubIssue>>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getRepoWorkflowRuns(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20
    ): Response<WorkflowRunsResponse>

    @POST("user/repos")
    suspend fun createRepository(
        @Header("Authorization") authHeader: String,
        @Body body: CreateRepoRequest
    ): Response<GitHubRepo>
}
