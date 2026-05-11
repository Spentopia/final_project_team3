package com.ict.spentopia.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody

data class CommunityPostListResponse(
    val items: List<CommunityPostResponse> = emptyList(),
    val total_count: Long = 0
)

data class CommunityPostResponse(
    val id: String,
    val user_id: String,
    val author_nickname: String? = null,
    val author_profile_image: String? = null,
    val author_profile_image_url: String? = null,
    val contest_id: String? = null,
    val post_type: String,
    val title: String,
    val image_url: String? = null,
    val content: String? = null,
    val reaction_count: Int? = 0,
    val is_reacted: Boolean = false,
    val view_count: Int = 0,
    val created_at: String? = null
)

data class CommunityContestResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val start_date: String,
    val end_date: String,
    val status: String? = null,
    val reward_description: String? = null
)

data class UploadCommunityImageResponse(
    val path: String
)

data class CommunityMeResponse(
    val id: String,
    val role_type: String = "user"
)

data class CreateContentReportRequest(
    val target_type: String,
    val target_id: String,
    val reason: String,
    val detail: String? = null
)

data class ContentReportResponse(
    val id: String
)

data class CreateCommunityPostRequest(
    val post_type: String,
    val title: String,
    val contest_id: String? = null,
    val image_url: String? = null,
    val content: String? = null
)

data class UpdateCommunityPostRequest(
    val title: String? = null,
    val image_url: String? = null,
    val content: String? = null
)

data class CommunityCommentResponse(
    val id: String,
    val post_id: String,
    val parent_id: String? = null,
    val user_id: String,
    val author_nickname: String? = null,
    val author_profile_image: String? = null,
    val author_profile_image_url: String? = null,
    val content: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class CreateCommunityCommentRequest(
    val content: String,
    val parent_id: String? = null
)

data class UpdateCommunityCommentRequest(
    val content: String
)

interface CommunityApi {
    @GET("/me")
    suspend fun getMe(): CommunityMeResponse

    @GET("/api/contests")
    suspend fun listContests(): List<CommunityContestResponse>

    @GET("/api/posts")
    suspend fun listPosts(
        @Query("post_type") postType: String? = null,
        @Query("contest_id") contestId: String? = null,
        @Query("sort") sort: String = "date",
        @Query("title") title: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): CommunityPostListResponse

    @GET("/api/posts/{id}")
    suspend fun getPost(
        @Path("id") id: String
    ): CommunityPostResponse

    @POST("/api/posts")
    suspend fun createPost(
        @Body request: CreateCommunityPostRequest
    ): CommunityPostResponse

    @Multipart
    @POST("/api/posts/image/upload")
    suspend fun uploadPostImage(
        @Part file: MultipartBody.Part,
        @Part("post_type") postType: RequestBody,
        @Part("contest_id") contestId: RequestBody? = null,
        @Part("post_id") postId: RequestBody? = null
    ): UploadCommunityImageResponse

    @PATCH("/api/posts/{id}")
    suspend fun updatePost(
        @Path("id") id: String,
        @Body request: UpdateCommunityPostRequest
    ): CommunityPostResponse

    @DELETE("/api/posts/{id}")
    suspend fun deletePost(
        @Path("id") id: String
    )

    @POST("/api/posts/{id}/react")
    suspend fun reactPost(
        @Path("id") id: String
    )

    @DELETE("/api/posts/{id}/react")
    suspend fun unreactPost(
        @Path("id") id: String
    )

    @GET("/api/posts/{id}/comments")
    suspend fun listComments(
        @Path("id") postId: String
    ): List<CommunityCommentResponse>

    @POST("/api/posts/{id}/comments")
    suspend fun createComment(
        @Path("id") postId: String,
        @Body request: CreateCommunityCommentRequest
    ): CommunityCommentResponse

    @PATCH("/api/comments/{id}")
    suspend fun updateComment(
        @Path("id") commentId: String,
        @Body request: UpdateCommunityCommentRequest
    ): CommunityCommentResponse

    @DELETE("/api/comments/{id}")
    suspend fun deleteComment(
        @Path("id") commentId: String
    )

    @POST("/api/content-reports")
    suspend fun createContentReport(
        @Body request: CreateContentReportRequest
    ): ContentReportResponse
}
