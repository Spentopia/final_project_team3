package com.ict.spentopia.feature.avatar

import androidx.lifecycle.ViewModel // 뷰모델
import kotlinx.coroutines.flow.MutableStateFlow // 상태 저장
import kotlinx.coroutines.flow.StateFlow // 상태 노출
import kotlinx.coroutines.flow.asStateFlow // 읽기 전용 변환
import kotlinx.coroutines.flow.update // 상태 갱신

// 아바타 화면 상태 관리
class AvatarViewModel : ViewModel() {

    // 내부 상태
    private val _uiState = MutableStateFlow(avatarDummyUiState())

    // 외부 상태
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    // 카테고리 변경
    fun selectCategory(category: AvatarCategory) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedCategory = category // 선택 반영
            )
        }
    }

    // 랜덤 코디
    fun randomizeAvatar() {
        _uiState.update { currentState ->
            currentState.copy(
                preview = AvatarPreviewUi(
                    bodyEmoji = randomSelectedEmoji(currentState, AvatarCategory.BODY, currentState.preview.bodyEmoji), // 몸 변경
                    hairEmoji = randomSelectedEmoji(currentState, AvatarCategory.HAIR, currentState.preview.hairEmoji), // 헤어 변경
                    faceEmoji = randomSelectedEmoji(currentState, AvatarCategory.FACE, currentState.preview.faceEmoji), // 표정 변경
                    clothesEmoji = randomSelectedEmoji(currentState, AvatarCategory.CLOTHES, currentState.preview.clothesEmoji), // 옷 변경
                    accessoryEmoji = randomSelectedEmoji(currentState, AvatarCategory.ACCESSORY, currentState.preview.accessoryEmoji) // 액세서리 변경
                )
            )
        }
    }

    // 스크린샷
    fun captureAvatar() {
        // 추후 연결
    }

    // 공유하기
    fun shareAvatar() {
        // 추후 연결
    }

    // 아이템 선택
    fun selectItem(category: AvatarCategory, itemName: String) {
        _uiState.update { currentState ->
            val updatedSections = currentState.itemSections.map { section ->
                if (section.category == category) {
                    section.copy(
                        items = section.items.map { item ->
                            if (item.locked) {
                                item // 잠금 유지
                            } else {
                                item.copy(
                                    selected = item.name == itemName // 선택 반영
                                )
                            }
                        }
                    )
                } else {
                    section // 기존 유지
                }
            }

            val updatedPreview = buildPreviewFromSections(
                sections = updatedSections, // 섹션 전달
                currentPreview = currentState.preview // 현재 미리보기
            )

            currentState.copy(
                itemSections = updatedSections, // 섹션 반영
                preview = updatedPreview // 미리보기 반영
            )
        }
    }

    // 랜덤 이모지 추출
    private fun randomSelectedEmoji(
        state: AvatarUiState,
        category: AvatarCategory,
        fallback: String
    ): String {
        val targetSection = state.itemSections.firstOrNull { it.category == category } ?: return fallback // 섹션 찾기

        val availableItems = targetSection.items.filter { !it.locked } // 잠금 제외

        if (availableItems.isEmpty()) return fallback // 예외 처리

        return availableItems.random().emoji // 랜덤 반환
    }

    // 미리보기 생성
    private fun buildPreviewFromSections(
        sections: List<AvatarItemSectionUi>,
        currentPreview: AvatarPreviewUi
    ): AvatarPreviewUi {
        return AvatarPreviewUi(
            bodyEmoji = selectedEmoji(sections, AvatarCategory.BODY, currentPreview.bodyEmoji), // 몸 반영
            hairEmoji = selectedEmoji(sections, AvatarCategory.HAIR, currentPreview.hairEmoji), // 헤어 반영
            faceEmoji = selectedEmoji(sections, AvatarCategory.FACE, currentPreview.faceEmoji), // 표정 반영
            clothesEmoji = selectedEmoji(sections, AvatarCategory.CLOTHES, currentPreview.clothesEmoji), // 옷 반영
            accessoryEmoji = selectedEmoji(sections, AvatarCategory.ACCESSORY, currentPreview.accessoryEmoji) // 액세서리 반영
        )
    }

    // 선택 이모지 조회
    private fun selectedEmoji(
        sections: List<AvatarItemSectionUi>,
        category: AvatarCategory,
        fallback: String
    ): String {
        val section = sections.firstOrNull { it.category == category } ?: return fallback // 섹션 찾기

        val selectedItem = section.items.firstOrNull { it.selected } ?: return fallback // 선택 아이템 찾기

        return selectedItem.emoji // 이모지 반환
    }
}