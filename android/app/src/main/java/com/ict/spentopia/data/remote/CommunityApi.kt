package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버로 보낼 값을 표시하는 도구를 가져옴
import retrofit2.http.DELETE // DELETE API 표시를 가져옴
import retrofit2.http.GET // GET API 표시를 가져옴
import retrofit2.http.Multipart // Multipart 기능을 가져옴
import retrofit2.http.Part // Part 기능을 가져옴
import retrofit2.http.PATCH // PATCH 기능을 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴
import retrofit2.http.Path // 주소 중간에 들어갈 값 표시를 가져옴
import retrofit2.http.Query // 주소 뒤에 붙는 요청값 표시를 가져옴
import okhttp3.MultipartBody // MultipartBody 기능을 가져옴
import okhttp3.RequestBody // RequestBody 기능을 가져옴

data class CommunityPostListResponse( // CommunityPostListResponse 데이터를 묶어둘 클래스 시작
    val items: List<CommunityPostResponse> = emptyList(), // items 값을 저장함
    val total_count: Long = 0 // total_count 값을 저장함
)

data class CommunityPostResponse( // CommunityPostResponse 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val user_id: String, // user_id 값을 저장함
    val author_nickname: String? = null, // author_nickname 값을 저장함
    val author_profile_image: String? = null, // author_profile_image 값을 저장함
    val author_profile_image_url: String? = null, // author_profile_image_url 값을 저장함
    val contest_id: String? = null, // contest_id 값을 저장함
    val post_type: String, // post_type 값을 저장함
    val title: String, // 제목을 저장함
    val image_url: String? = null, // image_url 값을 저장함
    val content: String? = null, // 내용을 저장함
    val reaction_count: Int? = 0, // reaction_count 값을 저장함
    val is_reacted: Boolean = false, // 반응을 눌렀는지 저장함
    val view_count: Int = 0, // view_count 값을 저장함
    val created_at: String? = null // created_at 값을 저장함
)

data class CommunityContestResponse( // CommunityContestResponse 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val title: String, // 제목을 저장함
    val description: String? = null, // description 값을 저장함
    val start_date: String, // start_date 값을 저장함
    val end_date: String, // end_date 값을 저장함
    val status: String? = null, // status 값을 저장함
    val reward_description: String? = null // reward_description 값을 저장함
)

data class UploadCommunityImageResponse( // UploadCommunityImageResponse 데이터를 묶어둘 클래스 시작
    val path: String // path 값을 저장함
)

data class CommunityMeResponse( // CommunityMeResponse 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val role_type: String = "user" // role_type 값을 저장함
)

data class CreateContentReportRequest( // CreateContentReportRequest 데이터를 묶어둘 클래스 시작
    val target_type: String, // target_type 값을 저장함
    val target_id: String, // target_id 값을 저장함
    val reason: String, // reason 값을 저장함
    val detail: String? = null // detail 값을 저장함
)

data class ContentReportResponse( // ContentReportResponse 데이터를 묶어둘 클래스 시작
    val id: String // 아이디를 저장함
)

data class CreateCommunityPostRequest( // CreateCommunityPostRequest 데이터를 묶어둘 클래스 시작
    val post_type: String, // post_type 값을 저장함
    val title: String, // 제목을 저장함
    val contest_id: String? = null, // contest_id 값을 저장함
    val image_url: String? = null, // image_url 값을 저장함
    val content: String? = null // 내용을 저장함
)

data class UpdateCommunityPostRequest( // UpdateCommunityPostRequest 데이터를 묶어둘 클래스 시작
    val title: String? = null, // 제목을 저장함
    val image_url: String? = null, // image_url 값을 저장함
    val content: String? = null // 내용을 저장함
)

data class CommunityCommentResponse( // CommunityCommentResponse 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val post_id: String, // post_id 값을 저장함
    val parent_id: String? = null, // parent_id 값을 저장함
    val user_id: String, // user_id 값을 저장함
    val author_nickname: String? = null, // author_nickname 값을 저장함
    val author_profile_image: String? = null, // author_profile_image 값을 저장함
    val author_profile_image_url: String? = null, // author_profile_image_url 값을 저장함
    val content: String, // 내용을 저장함
    val created_at: String? = null, // created_at 값을 저장함
    val updated_at: String? = null // updated_at 값을 저장함
)

data class CreateCommunityCommentRequest( // CreateCommunityCommentRequest 데이터를 묶어둘 클래스 시작
    val content: String, // 내용을 저장함
    val parent_id: String? = null // parent_id 값을 저장함
)

data class UpdateCommunityCommentRequest( // UpdateCommunityCommentRequest 데이터를 묶어둘 클래스 시작
    val content: String // 내용을 저장함
)

interface CommunityApi { // CommunityApi에서 꼭 만들어야 할 함수 규칙을 정함
    @GET("/me") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun getMe(): CommunityMeResponse // 데이터를 불러오는 함수 시작

    @GET("/api/contests") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun listContests(): List<CommunityContestResponse> // listContests 함수를 선언함

    @GET("/api/posts") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun listPosts( // listPosts 함수를 선언함
        @Query("post_type") postType: String? = null, // 이 값을 주소 뒤 요청값으로 보낸다는 표시
        @Query("contest_id") contestId: String? = null, // 이 값을 주소 뒤 요청값으로 보낸다는 표시
        @Query("sort") sort: String = "date", // 이 값을 주소 뒤 요청값으로 보낸다는 표시
        @Query("title") title: String? = null, // 이 값을 주소 뒤 요청값으로 보낸다는 표시
        @Query("page") page: Int = 1, // 이 값을 주소 뒤 요청값으로 보낸다는 표시
        @Query("page_size") pageSize: Int = 50 // 이 값을 주소 뒤 요청값으로 보낸다는 표시
    ): CommunityPostListResponse

    @GET("/api/posts/{id}") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun getPost( // 데이터를 불러오는 함수 시작
        @Path("id") id: String // 이 값을 API 주소 중간에 넣는다는 표시
    ): CommunityPostResponse

    @POST("/api/posts") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun createPost( // 데이터를 저장하는 함수 시작
        @Body request: CreateCommunityPostRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): CommunityPostResponse

    @Multipart // 이 코드에 특별한 역할을 붙이는 표시
    @POST("/api/posts/image/upload") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun uploadPostImage( // 데이터를 불러오는 함수 시작
        @Part file: MultipartBody.Part, // 이 코드에 특별한 역할을 붙이는 표시
        @Part("post_type") postType: RequestBody, // 이 코드에 특별한 역할을 붙이는 표시
        @Part("contest_id") contestId: RequestBody? = null, // 이 코드에 특별한 역할을 붙이는 표시
        @Part("post_id") postId: RequestBody? = null // 이 코드에 특별한 역할을 붙이는 표시
    ): UploadCommunityImageResponse

    @PATCH("/api/posts/{id}") // 서버 데이터 일부를 수정하는 API 주소를 적음
    suspend fun updatePost( // 데이터를 수정하는 함수 시작
        @Path("id") id: String, // 이 값을 API 주소 중간에 넣는다는 표시
        @Body request: UpdateCommunityPostRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): CommunityPostResponse

    @DELETE("/api/posts/{id}") // 서버 데이터를 삭제하는 API 주소를 적음
    suspend fun deletePost( // 데이터를 삭제하는 함수 시작
        @Path("id") id: String // 이 값을 API 주소 중간에 넣는다는 표시
    )

    @POST("/api/posts/{id}/react") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun reactPost( // reactPost 함수를 선언함
        @Path("id") id: String // 이 값을 API 주소 중간에 넣는다는 표시
    )

    @DELETE("/api/posts/{id}/react") // 서버 데이터를 삭제하는 API 주소를 적음
    suspend fun unreactPost( // unreactPost 함수를 선언함
        @Path("id") id: String // 이 값을 API 주소 중간에 넣는다는 표시
    )

    @GET("/api/posts/{id}/comments") // 서버에서 데이터를 가져오는 API 주소를 적음
    suspend fun listComments( // listComments 함수를 선언함
        @Path("id") postId: String // 이 값을 API 주소 중간에 넣는다는 표시
    ): List<CommunityCommentResponse>

    @POST("/api/posts/{id}/comments") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun createComment( // 데이터를 저장하는 함수 시작
        @Path("id") postId: String, // 이 값을 API 주소 중간에 넣는다는 표시
        @Body request: CreateCommunityCommentRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): CommunityCommentResponse

    @PATCH("/api/comments/{id}") // 서버 데이터 일부를 수정하는 API 주소를 적음
    suspend fun updateComment( // 데이터를 수정하는 함수 시작
        @Path("id") commentId: String, // 이 값을 API 주소 중간에 넣는다는 표시
        @Body request: UpdateCommunityCommentRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): CommunityCommentResponse

    @DELETE("/api/comments/{id}") // 서버 데이터를 삭제하는 API 주소를 적음
    suspend fun deleteComment( // 데이터를 삭제하는 함수 시작
        @Path("id") commentId: String // 이 값을 API 주소 중간에 넣는다는 표시
    )

    @POST("/api/content-reports") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun createContentReport( // 데이터를 저장하는 함수 시작
        @Body request: CreateContentReportRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): ContentReportResponse
}
