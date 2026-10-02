package com.example.data.api

import com.example.data.model.CreateRefRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.GitBlobRequest
import com.example.data.model.GitBlobResponse
import com.example.data.model.GitCommitRequest
import com.example.data.model.GitCommitResponse
import com.example.data.model.GitRefResponse
import com.example.data.model.GitTreeRequest
import com.example.data.model.GitTreeResponse
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.UpdateRefRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GitHubApiService {

    @GET("user")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") auth: String
    ): GitHubUser

    @GET("user/repos")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun getUserRepos(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): List<GitHubRepo>

    @GET("repos/{owner}/{repo}")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun getRepo(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepo

    @POST("user/repos")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun createRepo(
        @Header("Authorization") auth: String,
        @Body request: CreateRepoRequest
    ): GitHubRepo

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun getBranchRef(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): Response<GitRefResponse>

    @POST("repos/{owner}/{repo}/git/blobs")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun createBlob(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GitBlobRequest
    ): GitBlobResponse

    @POST("repos/{owner}/{repo}/git/trees")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun createTree(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GitTreeRequest
    ): GitTreeResponse

    @POST("repos/{owner}/{repo}/git/commits")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun createCommit(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GitCommitRequest
    ): GitCommitResponse

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun updateBranchRef(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body request: UpdateRefRequest
    ): GitRefResponse

    @POST("repos/{owner}/{repo}/git/refs")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun createBranchRef(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateRefRequest
    ): GitRefResponse

    companion object {
        fun create(): GitHubApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestWithUserAgent = original.newBuilder()
                        .header("User-Agent", "GitPusher-Android-App")
                        .build()
                    chain.proceed(requestWithUserAgent)
                }
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(GitHubApiService::class.java)
        }
    }
}
