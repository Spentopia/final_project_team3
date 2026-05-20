package com.ict.spentopia.feature.avatar // 이 파일이 속한 패키지 위치를 적음

import androidx.lifecycle.ViewModel // ViewModel 기능을 가져옴
import androidx.lifecycle.viewModelScope // ViewModel 코루틴 범위를 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // 서버 통신 도구를 가져옴
import com.ict.spentopia.data.remote.UserAvatarItemResponse // 아바타 아이템 응답을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
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

    init {
        loadUserItems() // 화면이 처음 만들어질 때 서버에서 내 아바타 아이템을 불러옴
    }

    private fun loadUserItems() { // 서버에서 내 아바타 아이템을 불러옴
        viewModelScope.launch {
            try {
                val items = RetrofitClient.avatarApi.getUserItems()
                if (items.isNotEmpty()) {
                    applyUserItems(items)
                }
            } catch (_: Exception) {
                // 실패 시 기존 더미 화면을 유지한다.
            }
        }
    }

    private fun applyUserItems(items: List<UserAvatarItemResponse>) { // 서버 아이템을 화면 상태로 바꿈
        val mappedItems = items.map { item ->
            val category = toAvatarCategory(item.category)
            AvatarItemUi(
                name = item.name,
                rarity = if (item.is_nft == true) "NFT" else "일반",
                selected = item.is_equipped == true,
                locked = false,
                id = item.id,
                imageUrl = item.image_url.orEmpty(),
                categoryKey = item.category,
                categoryLabel = category.label,
                acquiredAt = item.acquired_at?.take(10).orEmpty(),
                isNft = item.is_nft == true,
                mintAddress = item.nft_mint_address.orEmpty(),
                metadataUri = item.metadata_uri.orEmpty()
            )
        }

        val sections = items
            .groupBy { toAvatarCategory(it.category) }
            .map { (category, categoryItems) ->
                AvatarItemSectionUi(
                    category = category,
                    title = category.label,
                    items = categoryItems.mapIndexed { index, item ->
                        AvatarItemUi(
                            emoji = emojiForCategory(category),
                            name = item.name,
                            rarity = if (item.is_nft == true) "NFT" else "일반",
                            selected = item.is_equipped == true || index == 0,
                            locked = false
                        )
                    }
                )
            }
            .sortedBy { it.category.ordinal }

        _uiState.update { currentState ->
            currentState.copy(
                allItems = mappedItems,
                ownedItemCount = items.size,
                totalItemCount = items.size,
                itemSections = sections,
                preview = buildPreviewFromSections(sections, currentState.preview),
                summary = currentState.summary.copy(
                    equippedItemCount = "${items.count { it.is_equipped == true }}개",
                    acquiredDate = items.mapNotNull { it.acquired_at?.take(10) }.maxOrNull() ?: "-"
                )
            )
        }
    }

    private fun toAvatarCategory(category: String): AvatarCategory { // 서버 카테고리를 화면 카테고리로 바꿈
        return when (category.lowercase()) {
            "body" -> AvatarCategory.BODY
            "hair" -> AvatarCategory.HAIR
            "hat" -> AvatarCategory.HAT
            "face" -> AvatarCategory.FACE
            "top" -> AvatarCategory.TOP
            "bottom" -> AvatarCategory.BOTTOM
            "shoes" -> AvatarCategory.SHOES
            "clothes" -> AvatarCategory.CLOTHES
            "weapon" -> AvatarCategory.WEAPON
            "accessory" -> AvatarCategory.ACCESSORY
            else -> AvatarCategory.ACCESSORY
        }
    }

    private fun emojiForCategory(category: AvatarCategory): String { // 카테고리 기본 아이콘을 돌려줌
        return when (category) {
            AvatarCategory.BODY -> "🧍"
            AvatarCategory.HAIR -> "👱"
            AvatarCategory.FACE -> "😊"
            AvatarCategory.CLOTHES -> "👕"
            AvatarCategory.ACCESSORY -> "✨"
            AvatarCategory.TOP -> "👕"
            AvatarCategory.BOTTOM -> "👖"
            AvatarCategory.SHOES -> "신발"
            AvatarCategory.WEAPON -> "무기"
            AvatarCategory.HAT -> "모자"
            AvatarCategory.ALL -> "🎒"
        }
    }

    fun selectMainTab(tab: AvatarMainTab) { // 일반/NFT 탭을 바꿈
        _uiState.update { currentState ->
            currentState.copy(
                selectedMainTab = tab,
                selectedCategory = AvatarCategory.ALL
            )
        }
    }

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
