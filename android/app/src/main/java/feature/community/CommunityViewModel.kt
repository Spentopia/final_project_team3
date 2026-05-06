package com.ict.spentopia.feature.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

data class CommunityUiState(
    val posts: List<CommunityPost> = emptyList(),
    val selectedPost: CommunityPost? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class CommunityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = RetrofitClient.communityApi.listPosts(pageSize = 50)
                _uiState.update {
                    it.copy(
                        posts = response.items.map { item -> item.toUiModel() },
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val created = RetrofitClient.communityApi.createPost(
                    CreateCommunityPostRequest(
                        post_type = category.toBackendType(),
                        title = title,
                        content = content
                    )
                ).toUiModel()

                _uiState.update { state ->
                    state.copy(
                        posts = listOf(created) + state.posts,
                        isSaving = false
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "createPost failed", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "게시글 등록에 실패했습니다."
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
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
        comments = comments,
        isLiked = is_reacted
    )
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
