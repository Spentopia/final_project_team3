package com.ict.spentopia.feature.community // 이 파일이 속한 패키지 위치를 적음

import android.content.ContentResolver // ContentResolver 기능을 가져옴
import android.net.Uri // 이미지 주소 타입을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import androidx.lifecycle.ViewModel // ViewModel 기능을 가져옴
import androidx.lifecycle.viewModelScope // viewModelScope 기능을 가져옴
import com.ict.spentopia.data.remote.CreateContentReportRequest // CreateContentReportRequest 기능을 가져옴
import com.ict.spentopia.data.remote.CreateCommunityCommentRequest // CreateCommunityCommentRequest 기능을 가져옴
import com.ict.spentopia.data.remote.CreateCommunityPostRequest // CreateCommunityPostRequest 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.remote.UpdateCommunityCommentRequest // UpdateCommunityCommentRequest 기능을 가져옴
import com.ict.spentopia.data.remote.UpdateCommunityPostRequest // UpdateCommunityPostRequest 기능을 가져옴
import kotlinx.coroutines.flow.MutableStateFlow // 바뀌는 상태값 도구를 가져옴
import kotlinx.coroutines.flow.StateFlow // 읽기 전용 상태값 도구를 가져옴
import kotlinx.coroutines.flow.asStateFlow // asStateFlow 기능을 가져옴
import kotlinx.coroutines.flow.update // update 기능을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import okhttp3.MediaType.Companion.toMediaType // toMediaType 기능을 가져옴
import okhttp3.MediaType.Companion.toMediaTypeOrNull // toMediaTypeOrNull 기능을 가져옴
import okhttp3.MultipartBody // MultipartBody 기능을 가져옴
import okhttp3.RequestBody // RequestBody 기능을 가져옴
import okhttp3.RequestBody.Companion.toRequestBody // toRequestBody 기능을 가져옴
import java.time.Duration // Duration 기능을 가져옴
import java.time.OffsetDateTime // OffsetDateTime 기능을 가져옴
import java.time.format.DateTimeParseException // DateTimeParseException 기능을 가져옴

private const val COMMUNITY_SUPABASE_URL = "https://gapdntsijwgoucxhnojq.supabase.co" // 커뮤니티 관련 값을 저장함
private const val COMMUNITY_BUCKET = "posts" // 커뮤니티 관련 값을 저장함

data class CommunityUiState( // CommunityUiState 데이터를 묶어둘 클래스 시작
    val posts: List<CommunityPost> = emptyList(), // posts 값을 저장함
    val contests: List<CommunityContest> = emptyList(), // contests 값을 저장함
    val selectedPost: CommunityPost? = null, // selectedPost 값을 저장함
    val currentUserId: String = "", // currentUserId 값을 저장함
    val currentUserRole: String = "user", // currentUserRole 값을 저장함
    val isLoading: Boolean = false, // 로딩 여부를 저장함
    val isSaving: Boolean = false, // isSaving인지 여부를 저장함
    val errorMessage: String? = null // 오류 내용을 저장함
)

data class CommunityContest( // CommunityContest 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val title: String, // 제목을 저장함
    val description: String?, // description 값을 저장함
    val startDate: String, // startDate 값을 저장함
    val endDate: String, // endDate 값을 저장함
    val status: String?, // status 값을 저장함
    val rewardDescription: String? // rewardDescription 값을 저장함
)

class CommunityViewModel : ViewModel() { // CommunityViewModel 기능을 묶어둔 클래스 시작
    private val _uiState = MutableStateFlow(CommunityUiState()) // 화면에서 바뀔 화면 상태를 저장함
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow() // 화면에서 화면 상태를 읽을 수 있게 열어둠

    fun loadPosts() { // 데이터를 불러오는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // 로딩 상태를 정해줌
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val me = runCatching { // me 값을 저장함
                    RetrofitClient.communityApi.getMe() // 서버 통신 도구를 설정함
                }.getOrNull()
                val contests = runCatching { // contests 값을 저장함
                    RetrofitClient.communityApi.listContests().map { it.toUiModel() } // 서버 통신 도구를 설정함
                }.getOrDefault(_uiState.value.contests)
                val posts = loadAllCommunityPosts() // posts 값을 저장함
                _uiState.update { // 이 블록 안의 내용이 시작됨
                    it.copy(
                        posts = posts.map { item -> item.toUiModel() }, // posts 값을 정해줌
                        contests = contests, // contests 값을 contests 값에 넣음
                        currentUserId = me?.id ?: it.currentUserId, // currentUserId 값을 정해줌
                        currentUserRole = me?.role_type ?: it.currentUserRole, // currentUserRole 값을 정해줌
                        isLoading = false // false 값을 로딩 여부에 넣음
                    )
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "loadPosts failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { // 이 블록 안의 내용이 시작됨
                    it.copy(
                        isLoading = false, // false 값을 로딩 여부에 넣음
                        errorMessage = "게시글을 불러오지 못했습니다." // 오류 내용을 정해줌
                    )
                }
            }
        }
    }

    fun selectPost(post: CommunityPost) { // selectPost 함수를 선언함
        _uiState.update { it.copy(selectedPost = post) } // it.copy(selectedPost 값을 정해줌
    }

    fun clearSelectedPost() { // clearSelectedPost 함수를 선언함
        _uiState.update { it.copy(selectedPost = null) } // it.copy(selectedPost 값을 정해줌
    }

    fun loadPostDetail(postId: String) { // 데이터를 불러오는 함수 시작
        if (postId.isBlank()) return // 조건이 맞는지 확인함

        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // 로딩 상태를 정해줌
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val post = RetrofitClient.communityApi.getPost(postId) // post 값을 저장함
                val comments = runCatching { // comments 값을 저장함
                    RetrofitClient.communityApi.listComments(postId) // 서버 통신 도구를 설정함
                }.getOrDefault(emptyList())

                val detail = post.toUiModel( // detail 값을 저장함
                    comments = comments.map { it.toUiModel() } // comments 값을 정해줌
                )

                _uiState.update { state ->
                    state.copy(
                        selectedPost = detail, // detail 값을 selectedPost 값에 넣음
                        posts = state.posts.upsertPost(detail), // posts 값을 정해줌
                        isLoading = false // false 값을 로딩 여부에 넣음
                    )
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "loadPostDetail failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { // 이 블록 안의 내용이 시작됨
                    it.copy(
                        isLoading = false, // false 값을 로딩 여부에 넣음
                        errorMessage = "게시글을 불러오지 못했습니다." // 오류 내용을 정해줌
                    )
                }
            }
        }
    }

    fun createPost( // 데이터를 저장하는 함수 시작
        category: CommunityCategory, // 카테고리를 받음
        title: String, // 제목을 받음
        content: String, // 내용을 받음
        imageUri: Uri? = null, // imageUri 값을 받음
        contentResolver: ContentResolver? = null, // contentResolver 값을 받음
        contestId: String? = null, // contestId 값을 받음
        onSuccess: () -> Unit // 성공했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            _uiState.update { it.copy(isSaving = true, errorMessage = null) } // it.copy(isSaving 값을 정해줌
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                if (category == CommunityCategory.AVATAR_CONTEST && contestId.isNullOrBlank()) { // 조건이 맞는지 확인함
                    throw IllegalArgumentException("아바타 콘테스트 정보를 찾을 수 없습니다.")
                }

                val postType = category.toBackendType() // postType 값을 저장함
                val uploadedImagePath = if ( // uploadedImagePath 값을 저장함
                    imageUri != null && // ! 값을 정해줌
                    contentResolver != null && // ! 값을 정해줌
                    category != CommunityCategory.NOTICE // ! 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    uploadCommunityImage( // 화면에 이미지를 보여줌
                        contentResolver = contentResolver, // contentResolver 값을 contentResolver 값에 넣음
                        uri = imageUri, // imageUri 값을 이미지 주소에 넣음
                        postType = postType, // postType 값을 postType 값에 넣음
                        contestId = contestId // contestId 값을 contestId 값에 넣음
                    )
                } else { // 이 블록 안의 내용이 시작됨
                    null
                }

                var created = RetrofitClient.communityApi.createPost( // 나중에 바뀔 수 있는 created 값을 저장함
                    CreateCommunityPostRequest( // 데이터를 저장하는 함수를 실행함
                        post_type = postType, // postType 값을 post_type 값에 넣음
                        title = title, // 제목을 제목에 넣음
                        contest_id = contestId, // contestId 값을 contest_id 값에 넣음
                        image_url = uploadedImagePath, // uploadedImagePath 값을 image_url 값에 넣음
                        content = content // 내용을 내용에 넣음
                    )
                )

                if ( // 조건이 맞는지 확인함
                    imageUri != null && // ! 값을 정해줌
                    contentResolver != null && // ! 값을 정해줌
                    category == CommunityCategory.NOTICE // 카테고리를 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    val noticeImagePath = uploadCommunityImage( // noticeImagePath 값을 저장함
                        contentResolver = contentResolver, // contentResolver 값을 contentResolver 값에 넣음
                        uri = imageUri, // imageUri 값을 이미지 주소에 넣음
                        postType = postType, // postType 값을 postType 값에 넣음
                        postId = created.id // postId 값을 정해줌
                    )
                    created = RetrofitClient.communityApi.updatePost( // 서버 통신 도구를 설정함
                        id = created.id, // 아이디를 정해줌
                        request = UpdateCommunityPostRequest(image_url = noticeImagePath) // 서버 요청값을 정해줌
                    )
                }

                val createdUi = created.toUiModel() // createdUi 값을 저장함

                _uiState.update { state ->
                    state.copy(
                        posts = listOf(createdUi) + state.posts, // posts 값을 정해줌
                        isSaving = false // false 값을 isSaving인지 여부에 넣음
                    )
                }
                onSuccess() // 성공했을 때 넘겨받은 함수를 실행함
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "createPost failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { // 이 블록 안의 내용이 시작됨
                    it.copy(
                        isSaving = false, // false 값을 isSaving인지 여부에 넣음
                        errorMessage = e.message?.takeIf { message -> message.isNotBlank() } // 오류 내용을 정해줌
                            ?: "게시글 등록에 실패했습니다."
                    )
                }
            }
        }
    }

    fun updatePost(
        updatedPost: CommunityPost,
        onSuccess: () -> Unit = {}
    ) { // 데이터를 수정하는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            _uiState.update { it.copy(isSaving = true, errorMessage = null) } // it.copy(isSaving 값을 정해줌
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val updated = RetrofitClient.communityApi.updatePost( // updated 값을 저장함
                    id = updatedPost.id, // 아이디를 정해줌
                    request = UpdateCommunityPostRequest( // 서버 요청값을 정해줌
                        title = updatedPost.title, // 제목을 정해줌
                        content = updatedPost.fullContent // 내용을 정해줌
                    )
                ).toUiModel(comments = updatedPost.comments) // .toUiModel(comments 값을 정해줌

                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.upsertPost(updated), // posts 값을 정해줌
                        selectedPost = updated, // updated 값을 selectedPost 값에 넣음
                        isSaving = false // false 값을 isSaving인지 여부에 넣음
                    )
                }
                onSuccess()
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "updatePost failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { // 이 블록 안의 내용이 시작됨
                    it.copy(
                        isSaving = false, // false 값을 isSaving인지 여부에 넣음
                        errorMessage = "게시글 수정에 실패했습니다." // 오류 내용을 정해줌
                    )
                }
            }
        }
    }

    fun deletePost(postId: String, onSuccess: () -> Unit) { // 데이터를 삭제하는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            _uiState.update { it.copy(isSaving = true, errorMessage = null) } // it.copy(isSaving 값을 정해줌
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                RetrofitClient.communityApi.deletePost(postId) // 서버 통신 도구를 설정함
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.filterNot { it.id == postId }, // posts 값을 정해줌
                        selectedPost = null, // null 값을 selectedPost 값에 넣음
                        isSaving = false // false 값을 isSaving인지 여부에 넣음
                    )
                }
                onSuccess() // 성공했을 때 넘겨받은 함수를 실행함
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "deletePost failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { // 이 블록 안의 내용이 시작됨
                    it.copy(
                        isSaving = false, // false 값을 isSaving인지 여부에 넣음
                        errorMessage = "게시글 삭제에 실패했습니다." // 오류 내용을 정해줌
                    )
                }
            }
        }
    }

    fun toggleLike(postId: String) { // toggleLike 함수를 선언함
        val current = _uiState.value.posts.find { it.id == postId } // current 값을 저장함
            ?: _uiState.value.selectedPost?.takeIf { it.id == postId } // it.id 값을 정해줌
            ?: return

        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                if (current.isLiked) { // 조건이 맞는지 확인함
                    RetrofitClient.communityApi.unreactPost(postId) // 서버 통신 도구를 설정함
                } else { // 이 블록 안의 내용이 시작됨
                    RetrofitClient.communityApi.reactPost(postId) // 서버 통신 도구를 설정함
                }

                val updated = current.copy( // updated 값을 저장함
                    isLiked = !current.isLiked, // isLiked인지 여부를 정해줌
                    likeCount = if (current.isLiked) { // likeCount 값을 정해줌
                        (current.likeCount - 1).coerceAtLeast(0)
                    } else { // 이 블록 안의 내용이 시작됨
                        current.likeCount + 1
                    }
                )

                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.upsertPost(updated), // posts 값을 정해줌
                        selectedPost = state.selectedPost?.let { // selectedPost 값을 정해줌
                            if (it.id == postId) updated.copy(comments = it.comments) else it // 조건이 맞는지 확인함
                        }
                    )
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "toggleLike failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { it.copy(errorMessage = "좋아요 처리에 실패했습니다.") } // 오류 내용을 정해줌
            }
        }
    }

    fun addComment(
        postId: String,
        content: String,
        onSuccess: () -> Unit = {}
    ) { // addComment 함수를 선언함
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                RetrofitClient.communityApi.createComment( // 서버 통신 도구를 설정함
                    postId = postId, // postId 값을 postId 값에 넣음
                    request = CreateCommunityCommentRequest(content = content) // 서버 요청값을 정해줌
                )
                loadPostDetail(postId) // 데이터를 불러오는 함수를 실행함
                onSuccess()
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "addComment failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { it.copy(errorMessage = "댓글 등록에 실패했습니다.") } // 오류 내용을 정해줌
            }
        }
    }

    fun updateComment(
        postId: String,
        commentId: String,
        content: String,
        onSuccess: () -> Unit = {}
    ) { // 데이터를 수정하는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                RetrofitClient.communityApi.updateComment( // 서버 통신 도구를 설정함
                    commentId = commentId, // commentId 값을 commentId 값에 넣음
                    request = UpdateCommunityCommentRequest(content = content) // 서버 요청값을 정해줌
                )
                loadPostDetail(postId) // 데이터를 불러오는 함수를 실행함
                onSuccess()
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "updateComment failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { it.copy(errorMessage = "댓글 수정에 실패했습니다.") } // 오류 내용을 정해줌
            }
        }
    }

    fun deleteComment(
        postId: String,
        commentId: String,
        onSuccess: () -> Unit = {}
    ) { // 데이터를 삭제하는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                RetrofitClient.communityApi.deleteComment(commentId) // 서버 통신 도구를 설정함
                loadPostDetail(postId) // 데이터를 불러오는 함수를 실행함
                onSuccess()
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "deleteComment failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { it.copy(errorMessage = "댓글 삭제에 실패했습니다.") } // 오류 내용을 정해줌
            }
        }
    }

    fun reportContent( // reportContent 함수를 선언함
        targetType: String, // targetType 값을 받음
        targetId: String, // targetId 값을 받음
        reason: String, // reason 값을 받음
        detail: String? = null // detail 값을 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                RetrofitClient.communityApi.createContentReport( // 서버 통신 도구를 설정함
                    CreateContentReportRequest( // 데이터를 저장하는 함수를 실행함
                        target_type = targetType, // targetType 값을 target_type 값에 넣음
                        target_id = targetId, // targetId 값을 target_id 값에 넣음
                        reason = reason, // reason 값을 reason 값에 넣음
                        detail = detail // detail 값을 detail 값에 넣음
                    )
                )
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("CommunityViewModel", "reportContent failed", e) // 개발자가 확인할 로그를 찍음
                _uiState.update { it.copy(errorMessage = "신고 접수에 실패했습니다.") } // 오류 내용을 정해줌
            }
        }
    }

    fun clearError() { // clearError 함수를 선언함
        _uiState.update { it.copy(errorMessage = null) } // 오류 내용을 정해줌
    }
}

private suspend fun loadAllCommunityPosts(): List<com.ict.spentopia.data.remote.CommunityPostResponse> { // 데이터를 불러오는 함수 시작
    val pageSize = 50 // pageSize 값을 저장함
    val firstPage = RetrofitClient.communityApi.listPosts(page = 1, pageSize = pageSize) // firstPage 값을 저장함
    val items = firstPage.items.toMutableList() // items 값을 저장함
    val totalCount = firstPage.total_count.toInt() // totalCount 값을 저장함
    var page = 2 // 나중에 바뀔 수 있는 page 값을 저장함

    while (items.size < totalCount) { // 조건이 맞는 동안 계속 반복함
        val nextPage = RetrofitClient.communityApi.listPosts(page = page, pageSize = pageSize) // nextPage 값을 저장함
        if (nextPage.items.isEmpty()) break // 조건이 맞는지 확인함
        items += nextPage.items // + 값을 정해줌
        page += 1 // + 값을 정해줌
    }

    return items // 이 값을 함수 결과로 돌려줌
}

private suspend fun uploadCommunityImage( // 데이터를 불러오는 함수 시작
    contentResolver: ContentResolver, // contentResolver 값을 받음
    uri: Uri, // 이미지 주소를 받음
    postType: String, // postType 값을 받음
    contestId: String? = null, // contestId 값을 받음
    postId: String? = null // postId 값을 받음
): String { // 이 블록 안의 내용이 시작됨
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg" // mimeType 값을 저장함
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } // bytes 값을 저장함
        ?: throw IllegalArgumentException("첨부 이미지를 읽을 수 없습니다.")
    if (bytes.isEmpty()) { // 조건이 맞는지 확인함
        throw IllegalArgumentException("첨부 이미지가 비어 있습니다.")
    }

    val extension = when (mimeType) { // extension 값을 저장함
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg" // 위 조건이 아니면 이쪽을 실행함
    }
    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull()) // 본문을 저장함
    val filePart = MultipartBody.Part.createFormData( // filePart 값을 저장함
        name = "file", // name 값을 정해줌
        filename = "community-image.$extension", // filename 값을 정해줌
        body = body // 본문을 본문에 넣음
    )

    return RetrofitClient.communityApi.uploadPostImage( // 이 값을 함수 결과로 돌려줌
        file = filePart, // filePart 값을 파일에 넣음
        postType = postType.toPlainRequestBody(), // postType 값을 정해줌
        contestId = contestId?.toPlainRequestBody(), // contestId 값을 정해줌
        postId = postId?.toPlainRequestBody() // postId 값을 정해줌
    ).path
}

private fun String.toPlainRequestBody(): RequestBody { // String 함수를 선언함
    return toRequestBody("text/plain".toMediaType()) // 이 값을 함수 결과로 돌려줌
}

private fun com.ict.spentopia.data.remote.CommunityContestResponse.toUiModel(): CommunityContest { // com 함수를 선언함
    return CommunityContest( // 이 값을 함수 결과로 돌려줌
        id = id, // 아이디를 아이디에 넣음
        title = title, // 제목을 제목에 넣음
        description = description, // description 값을 description 값에 넣음
        startDate = start_date.toDateText(), // 화면에 글자를 보여줌
        endDate = end_date.toDateText(), // 화면에 글자를 보여줌
        status = status, // status 값을 status 값에 넣음
        rewardDescription = reward_description // reward_description 값을 rewardDescription 값에 넣음
    )
}

private fun com.ict.spentopia.data.remote.CommunityPostResponse.toUiModel( // com 함수를 선언함
    comments: List<CommunityComment> = emptyList() // comments 값을 받음
): CommunityPost { // 이 블록 안의 내용이 시작됨
    val fullContent = content.orEmpty() // fullContent 값을 저장함
    return CommunityPost( // 이 값을 함수 결과로 돌려줌
        id = id, // 아이디를 아이디에 넣음
        title = title, // 제목을 제목에 넣음
        content = fullContent.take(60), // 내용을 정해줌
        fullContent = fullContent, // fullContent 값을 fullContent 값에 넣음
        authorId = user_id, // user_id 값을 authorId 값에 넣음
        author = author_nickname ?: "사용자", // author 값을 정해줌
        timeText = created_at.toRelativeTimeText(), // 화면에 글자를 보여줌
        likeCount = reaction_count ?: 0, // likeCount 값을 정해줌
        commentCount = comments.size, // commentCount 값을 정해줌
        tagText = post_type.toCategory().label, // tagText 값을 정해줌
        category = post_type.toCategory(), // 카테고리를 정해줌
        viewCount = view_count, // view_count 값을 viewCount 값에 넣음
        detailDateText = created_at.toDetailDateText(), // 화면에 글자를 보여줌
        comments = comments, // comments 값을 comments 값에 넣음
        isLiked = is_reacted, // is_reacted인지 여부를 isLiked인지 여부에 넣음
        imageUrl = image_url.toCommunityImageUrl() // imageUrl 값을 정해줌
    )
}

private fun String.toDateText(): String { // String 함수를 선언함
    return try { // 이 값을 함수 결과로 돌려줌
        OffsetDateTime.parse(this).toLocalDate().toString().replace("-", ".")
    } catch (_: DateTimeParseException) { // 이 블록 안의 내용이 시작됨
        take(10).replace("-", ".") // take 함수를 실행함
    }
}

private fun String?.toCommunityImageUrl(): String? { // String 함수를 선언함
    if (isNullOrBlank()) return null // 조건이 맞는지 확인함
    if (startsWith("http")) return this // 조건이 맞는지 확인함
    return "$COMMUNITY_SUPABASE_URL/storage/v1/object/public/$COMMUNITY_BUCKET/$this" // 이 값을 함수 결과로 돌려줌
}

private fun String?.toDetailDateText(): String { // String 함수를 선언함
    if (isNullOrBlank()) return "" // 조건이 맞는지 확인함

    return try { // 이 값을 함수 결과로 돌려줌
        val createdAt = OffsetDateTime.parse(this) // createdAt 값을 저장함
        val date = createdAt.toLocalDate() // 날짜을 저장함
        val time = createdAt.toLocalTime() // time 값을 저장함
        "%04d.%02d.%02d %02d:%02d".format(
            date.year,
            date.monthValue,
            date.dayOfMonth,
            time.hour,
            time.minute
        )
    } catch (_: DateTimeParseException) { // 이 블록 안의 내용이 시작됨
        take(16).replace("-", ".").replace("T", " ") // take 함수를 실행함
    }
}

private fun com.ict.spentopia.data.remote.CommunityCommentResponse.toUiModel(): CommunityComment { // com 함수를 선언함
    return CommunityComment( // 이 값을 함수 결과로 돌려줌
        id = id, // 아이디를 아이디에 넣음
        authorId = user_id, // user_id 값을 authorId 값에 넣음
        author = author_nickname ?: "사용자", // author 값을 정해줌
        content = content, // 내용을 내용에 넣음
        timeText = created_at.toRelativeTimeText() // 화면에 글자를 보여줌
    )
}

private fun List<CommunityPost>.upsertPost(post: CommunityPost): List<CommunityPost> { // List 함수를 선언함
    val index = indexOfFirst { it.id == post.id } // index 값을 저장함
    if (index == -1) return listOf(post) + this // 조건이 맞는지 확인함
    return toMutableList().also { it[index] = post } // 이 값을 함수 결과로 돌려줌
}

private fun CommunityCategory.toBackendType(): String { // CommunityCategory 함수를 선언함
    return when (this) { // 이 값을 함수 결과로 돌려줌
        CommunityCategory.NOTICE -> "notice"
        CommunityCategory.AVATAR_CONTEST -> "contest"
        CommunityCategory.REQUEST -> "request"
        CommunityCategory.FREE_BOARD -> "free"
    }
}

private fun String.toCategory(): CommunityCategory { // String 함수를 선언함
    return when (lowercase()) { // 이 값을 함수 결과로 돌려줌
        "notice" -> CommunityCategory.NOTICE
        "contest" -> CommunityCategory.AVATAR_CONTEST
        "request" -> CommunityCategory.REQUEST
        else -> CommunityCategory.FREE_BOARD // 위 조건이 아니면 이쪽을 실행함
    }
}

private fun String?.toRelativeTimeText(): String { // String 함수를 선언함
    if (isNullOrBlank()) return "" // 조건이 맞는지 확인함

    return try { // 이 값을 함수 결과로 돌려줌
        val createdAt = OffsetDateTime.parse(this) // createdAt 값을 저장함
        val duration = Duration.between(createdAt, OffsetDateTime.now()) // duration 값을 저장함
        when { // 값 종류에 따라 실행할 코드를 나눔
            duration.toMinutes() < 1 -> "방금 전"
            duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
            duration.toDays() < 1 -> "${duration.toHours()}시간 전"
            duration.toDays() < 7 -> "${duration.toDays()}일 전"
            else -> createdAt.toLocalDate().toString() // 위 조건이 아니면 이쪽을 실행함
        }
    } catch (_: DateTimeParseException) { // 이 블록 안의 내용이 시작됨
        ""
    }
}
