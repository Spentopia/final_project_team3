package com.ict.spentopia.feature.community

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ict.spentopia.data.remote.CreateContentReportRequest
import com.ict.spentopia.data.remote.CreateCommunityCommentRequest
import com.ict.spentopia.data.remote.CreateCommunityPostRequest
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.UpdateCommunityCommentRequest
import com.ict.spentopia.data.remote.UpdateCommunityPostRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private const val COMMUNITY_SUPABASE_URL = "https://gapdntsijwgoucxhnojq.supabase.co"
private const val COMMUNITY_BUCKET = "posts"

data class CommunityUiState(
    val posts: List<CommunityPost> = emptyList(),
    val contests: List<CommunityContest> = emptyList(),
    val selectedPost: CommunityPost? = null,
    val currentUserId: String = "",
    val currentUserRole: String = "user",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class CommunityContest(
    val id: String,
    val title: String,
    val description: String?,
    val startDate: String,
    val endDate: String,
    val status: String?,
    val rewardDescription: String?
)

class CommunityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val me = runCatching {
                    RetrofitClient.communityApi.getMe()
                }.getOrNull()
                val contests = runCatching {
                    RetrofitClient.communityApi.listContests().map { it.toUiModel() }
                }.getOrDefault(_uiState.value.contests)
                val posts = loadAllCommunityPosts()
                _uiState.update {
                    it.copy(
                        posts = posts.map { item -> item.toUiModel() },
                        contests = contests,
                        currentUserId = me?.id ?: it.currentUserId,
                        currentUserRole = me?.role_type ?: it.currentUserRole,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "loadPosts failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "게시글을 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun selectPost(post: CommunityPost) {
        _uiState.update { it.copy(selectedPost = post) }
    }

    fun clearSelectedPost() {
        _uiState.update { it.copy(selectedPost = null) }
    }

    fun loadPostDetail(postId: String) {
        if (postId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val post = RetrofitClient.communityApi.getPost(postId)
                val comments = runCatching {
                    RetrofitClient.communityApi.listComments(postId)
                }.getOrDefault(emptyList())

                val detail = post.toUiModel(
                    comments = comments.map { it.toUiModel() }
                )

                _uiState.update { state ->
                    state.copy(
                        selectedPost = detail,
                        posts = state.posts.upsertPost(detail),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "loadPostDetail failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "게시글을 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun createPost(
        category: CommunityCategory,
        title: String,
        content: String,
        imageUri: Uri? = null,
        contentResolver: ContentResolver? = null,
        contestId: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                if (category == CommunityCategory.AVATAR_CONTEST && contestId.isNullOrBlank()) {
                    throw IllegalArgumentException("아바타 콘테스트 정보를 찾을 수 없습니다.")
                }

                val postType = category.toBackendType()
                val uploadedImagePath = if (
                    imageUri != null &&
                    contentResolver != null &&
                    category != CommunityCategory.NOTICE
                ) {
                    uploadCommunityImage(
                        contentResolver = contentResolver,
                        uri = imageUri,
                        postType = postType,
                        contestId = contestId
                    )
                } else {
                    null
                }

                var created = RetrofitClient.communityApi.createPost(
                    CreateCommunityPostRequest(
                        post_type = postType,
                        title = title,
                        contest_id = contestId,
                        image_url = uploadedImagePath,
                        content = content
                    )
                )

                if (
                    imageUri != null &&
                    contentResolver != null &&
                    category == CommunityCategory.NOTICE
                ) {
                    val noticeImagePath = uploadCommunityImage(
                        contentResolver = contentResolver,
                        uri = imageUri,
                        postType = postType,
                        postId = created.id
                    )
                    created = RetrofitClient.communityApi.updatePost(
                        id = created.id,
                        request = UpdateCommunityPostRequest(image_url = noticeImagePath)
                    )
                }

                val createdUi = created.toUiModel()

                _uiState.update { state ->
                    state.copy(
                        posts = listOf(createdUi) + state.posts,
                        isSaving = false
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "createPost failed", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message?.takeIf { message -> message.isNotBlank() }
                            ?: "게시글 등록에 실패했습니다."
                    )
                }
            }
        }
    }

    fun updatePost(updatedPost: CommunityPost) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val updated = RetrofitClient.communityApi.updatePost(
                    id = updatedPost.id,
                    request = UpdateCommunityPostRequest(
                        title = updatedPost.title,
                        content = updatedPost.fullContent
                    )
                ).toUiModel(comments = updatedPost.comments)

                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.upsertPost(updated),
                        selectedPost = updated,
                        isSaving = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "updatePost failed", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "게시글 수정에 실패했습니다."
                    )
                }
            }
        }
    }

    fun deletePost(postId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                RetrofitClient.communityApi.deletePost(postId)
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.filterNot { it.id == postId },
                        selectedPost = null,
                        isSaving = false
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "deletePost failed", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "게시글 삭제에 실패했습니다."
                    )
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        val current = _uiState.value.posts.find { it.id == postId }
            ?: _uiState.value.selectedPost?.takeIf { it.id == postId }
            ?: return

        viewModelScope.launch {
            try {
                if (current.isLiked) {
                    RetrofitClient.communityApi.unreactPost(postId)
                } else {
                    RetrofitClient.communityApi.reactPost(postId)
                }

                val updated = current.copy(
                    isLiked = !current.isLiked,
                    likeCount = if (current.isLiked) {
                        (current.likeCount - 1).coerceAtLeast(0)
                    } else {
                        current.likeCount + 1
                    }
                )

                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.upsertPost(updated),
                        selectedPost = state.selectedPost?.let {
                            if (it.id == postId) updated.copy(comments = it.comments) else it
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "toggleLike failed", e)
                _uiState.update { it.copy(errorMessage = "좋아요 처리에 실패했습니다.") }
            }
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.communityApi.createComment(
                    postId = postId,
                    request = CreateCommunityCommentRequest(content = content)
                )
                loadPostDetail(postId)
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "addComment failed", e)
                _uiState.update { it.copy(errorMessage = "댓글 등록에 실패했습니다.") }
            }
        }
    }

    fun updateComment(postId: String, commentId: String, content: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.communityApi.updateComment(
                    commentId = commentId,
                    request = UpdateCommunityCommentRequest(content = content)
                )
                loadPostDetail(postId)
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "updateComment failed", e)
                _uiState.update { it.copy(errorMessage = "댓글 수정에 실패했습니다.") }
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.communityApi.deleteComment(commentId)
                loadPostDetail(postId)
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "deleteComment failed", e)
                _uiState.update { it.copy(errorMessage = "댓글 삭제에 실패했습니다.") }
            }
        }
    }

    fun reportContent(
        targetType: String,
        targetId: String,
        reason: String,
        detail: String? = null
    ) {
        viewModelScope.launch {
            try {
                RetrofitClient.communityApi.createContentReport(
                    CreateContentReportRequest(
                        target_type = targetType,
                        target_id = targetId,
                        reason = reason,
                        detail = detail
                    )
                )
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "reportContent failed", e)
                _uiState.update { it.copy(errorMessage = "신고 접수에 실패했습니다.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

private suspend fun loadAllCommunityPosts(): List<com.ict.spentopia.data.remote.CommunityPostResponse> {
    val pageSize = 50
    val firstPage = RetrofitClient.communityApi.listPosts(page = 1, pageSize = pageSize)
    val items = firstPage.items.toMutableList()
    val totalCount = firstPage.total_count.toInt()
    var page = 2

    while (items.size < totalCount) {
        val nextPage = RetrofitClient.communityApi.listPosts(page = page, pageSize = pageSize)
        if (nextPage.items.isEmpty()) break
        items += nextPage.items
        page += 1
    }

    return items
}

private suspend fun uploadCommunityImage(
    contentResolver: ContentResolver,
    uri: Uri,
    postType: String,
    contestId: String? = null,
    postId: String? = null
): String {
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalArgumentException("첨부 이미지를 읽을 수 없습니다.")
    if (bytes.isEmpty()) {
        throw IllegalArgumentException("첨부 이미지가 비어 있습니다.")
    }

    val extension = when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    val filePart = MultipartBody.Part.createFormData(
        name = "file",
        filename = "community-image.$extension",
        body = body
    )

    return RetrofitClient.communityApi.uploadPostImage(
        file = filePart,
        postType = postType.toPlainRequestBody(),
        contestId = contestId?.toPlainRequestBody(),
        postId = postId?.toPlainRequestBody()
    ).path
}

private fun String.toPlainRequestBody(): RequestBody {
    return toRequestBody("text/plain".toMediaType())
}

private fun com.ict.spentopia.data.remote.CommunityContestResponse.toUiModel(): CommunityContest {
    return CommunityContest(
        id = id,
        title = title,
        description = description,
        startDate = start_date.toDateText(),
        endDate = end_date.toDateText(),
        status = status,
        rewardDescription = reward_description
    )
}

private fun com.ict.spentopia.data.remote.CommunityPostResponse.toUiModel(
    comments: List<CommunityComment> = emptyList()
): CommunityPost {
    val fullContent = content.orEmpty()
    return CommunityPost(
        id = id,
        title = title,
        content = fullContent.take(60),
        fullContent = fullContent,
        authorId = user_id,
        author = author_nickname ?: "사용자",
        timeText = created_at.toRelativeTimeText(),
        likeCount = reaction_count ?: 0,
        commentCount = comments.size,
        tagText = post_type.toCategory().label,
        category = post_type.toCategory(),
        viewCount = view_count,
        detailDateText = created_at.toDetailDateText(),
        comments = comments,
        isLiked = is_reacted,
        imageUrl = image_url.toCommunityImageUrl()
    )
}

private fun String.toDateText(): String {
    return try {
        OffsetDateTime.parse(this).toLocalDate().toString().replace("-", ".")
    } catch (_: DateTimeParseException) {
        take(10).replace("-", ".")
    }
}

private fun String?.toCommunityImageUrl(): String? {
    if (isNullOrBlank()) return null
    if (startsWith("http")) return this
    return "$COMMUNITY_SUPABASE_URL/storage/v1/object/public/$COMMUNITY_BUCKET/$this"
}

private fun String?.toDetailDateText(): String {
    if (isNullOrBlank()) return ""

    return try {
        val createdAt = OffsetDateTime.parse(this)
        val date = createdAt.toLocalDate()
        val time = createdAt.toLocalTime()
        "%04d.%02d.%02d %02d:%02d".format(
            date.year,
            date.monthValue,
            date.dayOfMonth,
            time.hour,
            time.minute
        )
    } catch (_: DateTimeParseException) {
        take(16).replace("-", ".").replace("T", " ")
    }
}

private fun com.ict.spentopia.data.remote.CommunityCommentResponse.toUiModel(): CommunityComment {
    return CommunityComment(
        id = id,
        authorId = user_id,
        author = author_nickname ?: "사용자",
        content = content,
        timeText = created_at.toRelativeTimeText()
    )
}

private fun List<CommunityPost>.upsertPost(post: CommunityPost): List<CommunityPost> {
    val index = indexOfFirst { it.id == post.id }
    if (index == -1) return listOf(post) + this
    return toMutableList().also { it[index] = post }
}

private fun CommunityCategory.toBackendType(): String {
    return when (this) {
        CommunityCategory.NOTICE -> "notice"
        CommunityCategory.AVATAR_CONTEST -> "contest"
        CommunityCategory.REQUEST -> "request"
        CommunityCategory.FREE_BOARD -> "free"
    }
}

private fun String.toCategory(): CommunityCategory {
    return when (lowercase()) {
        "notice" -> CommunityCategory.NOTICE
        "contest" -> CommunityCategory.AVATAR_CONTEST
        "request" -> CommunityCategory.REQUEST
        else -> CommunityCategory.FREE_BOARD
    }
}

private fun String?.toRelativeTimeText(): String {
    if (isNullOrBlank()) return ""

    return try {
        val createdAt = OffsetDateTime.parse(this)
        val duration = Duration.between(createdAt, OffsetDateTime.now())
        when {
            duration.toMinutes() < 1 -> "방금 전"
            duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
            duration.toDays() < 1 -> "${duration.toHours()}시간 전"
            duration.toDays() < 7 -> "${duration.toDays()}일 전"
            else -> createdAt.toLocalDate().toString()
        }
    } catch (_: DateTimeParseException) {
        ""
    }
}
