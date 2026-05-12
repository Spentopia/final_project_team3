package com.ict.spentopia.feature.avatar // 이 파일이 속한 패키지 위치를 적음

import androidx.lifecycle.ViewModel // ViewModel 기능을 가져옴
import kotlinx.coroutines.flow.MutableStateFlow // 바뀌는 상태값 도구를 가져옴
import kotlinx.coroutines.flow.StateFlow // 읽기 전용 상태값 도구를 가져옴
import kotlinx.coroutines.flow.asStateFlow // asStateFlow 기능을 가져옴
import kotlinx.coroutines.flow.update // update 기능을 가져옴

// 아바타 화면 상태 관리
class AvatarViewModel : ViewModel() { // AvatarViewModel 기능을 묶어둔 클래스 시작

    // 내부 상태
    private val _uiState = MutableStateFlow(avatarDummyUiState()) // 화면에서 바뀔 화면 상태를 저장함

    // 외부 상태
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow() // 화면에서 화면 상태를 읽을 수 있게 열어둠

    // 카테고리 변경
    fun selectCategory(category: AvatarCategory) { // selectCategory 함수를 선언함
        _uiState.update { currentState ->
            currentState.copy(
                selectedCategory = category // 카테고리를 selectedCategory 값에 넣음
            )
        }
    }

    // 랜덤 코디
    fun randomizeAvatar() { // randomizeAvatar 함수를 선언함
        _uiState.update { currentState ->
            currentState.copy(
                preview = AvatarPreviewUi( // preview 값을 정해줌
                    bodyEmoji = randomSelectedEmoji(currentState, AvatarCategory.BODY, currentState.preview.bodyEmoji), // bodyEmoji 값을 정해줌
                    hairEmoji = randomSelectedEmoji(currentState, AvatarCategory.HAIR, currentState.preview.hairEmoji), // hairEmoji 값을 정해줌
                    faceEmoji = randomSelectedEmoji(currentState, AvatarCategory.FACE, currentState.preview.faceEmoji), // faceEmoji 값을 정해줌
                    clothesEmoji = randomSelectedEmoji(currentState, AvatarCategory.CLOTHES, currentState.preview.clothesEmoji), // clothesEmoji 값을 정해줌
                    accessoryEmoji = randomSelectedEmoji(currentState, AvatarCategory.ACCESSORY, currentState.preview.accessoryEmoji) // accessoryEmoji 값을 정해줌
                )
            )
        }
    }

    // 스크린샷
    fun captureAvatar() { // captureAvatar 함수를 선언함
        // 추후 연결
    }

    // 공유하기
    fun shareAvatar() { // shareAvatar 함수를 선언함
        // 추후 연결
    }

    // 아이템 선택
    fun selectItem(category: AvatarCategory, itemName: String) { // selectItem 함수를 선언함
        _uiState.update { currentState ->
            val updatedSections = currentState.itemSections.map { section -> // updatedSections 값을 저장함
                if (section.category == category) { // 조건이 맞는지 확인함
                    section.copy(
                        items = section.items.map { item -> // items 값을 정해줌
                            if (item.locked) { // 조건이 맞는지 확인함
                                item
                            } else { // 이 블록 안의 내용이 시작됨
                                item.copy(
                                    selected = item.name == itemName // selected 값을 정해줌
                                )
                            }
                        }
                    )
                } else { // 이 블록 안의 내용이 시작됨
                    section
                }
            }

            val updatedPreview = buildPreviewFromSections( // updatedPreview 값을 저장함
                sections = updatedSections, // updatedSections 값을 sections 값에 넣음
                currentPreview = currentState.preview // currentPreview 값을 정해줌
            )

            currentState.copy(
                itemSections = updatedSections, // updatedSections 값을 itemSections 값에 넣음
                preview = updatedPreview // updatedPreview 값을 preview 값에 넣음
            )
        }
    }

    // 랜덤 이모지 추출
    private fun randomSelectedEmoji( // randomSelectedEmoji 함수를 선언함
        state: AvatarUiState, // 상태값을 받음
        category: AvatarCategory, // 카테고리를 받음
        fallback: String // fallback 값을 받음
    ): String { // 이 블록 안의 내용이 시작됨
        val targetSection = state.itemSections.firstOrNull { it.category == category } ?: return fallback // targetSection 값을 저장함

        val availableItems = targetSection.items.filter { !it.locked } // availableItems 값을 저장함

        if (availableItems.isEmpty()) return fallback // 조건이 맞는지 확인함

        return availableItems.random().emoji // 이 값을 함수 결과로 돌려줌
    }

    // 미리보기 생성
    private fun buildPreviewFromSections( // buildPreviewFromSections 함수를 선언함
        sections: List<AvatarItemSectionUi>, // sections 값을 받음
        currentPreview: AvatarPreviewUi // currentPreview 값을 받음
    ): AvatarPreviewUi { // 이 블록 안의 내용이 시작됨
        return AvatarPreviewUi( // 이 값을 함수 결과로 돌려줌
            bodyEmoji = selectedEmoji(sections, AvatarCategory.BODY, currentPreview.bodyEmoji), // bodyEmoji 값을 정해줌
            hairEmoji = selectedEmoji(sections, AvatarCategory.HAIR, currentPreview.hairEmoji), // hairEmoji 값을 정해줌
            faceEmoji = selectedEmoji(sections, AvatarCategory.FACE, currentPreview.faceEmoji), // faceEmoji 값을 정해줌
            clothesEmoji = selectedEmoji(sections, AvatarCategory.CLOTHES, currentPreview.clothesEmoji), // clothesEmoji 값을 정해줌
            accessoryEmoji = selectedEmoji(sections, AvatarCategory.ACCESSORY, currentPreview.accessoryEmoji) // accessoryEmoji 값을 정해줌
        )
    }

    // 선택 이모지 조회
    private fun selectedEmoji( // selectedEmoji 함수를 선언함
        sections: List<AvatarItemSectionUi>, // sections 값을 받음
        category: AvatarCategory, // 카테고리를 받음
        fallback: String // fallback 값을 받음
    ): String { // 이 블록 안의 내용이 시작됨
        val section = sections.firstOrNull { it.category == category } ?: return fallback // section 값을 저장함

        val selectedItem = section.items.firstOrNull { it.selected } ?: return fallback // selectedItem 값을 저장함

        return selectedItem.emoji // 이 값을 함수 결과로 돌려줌
    }
}