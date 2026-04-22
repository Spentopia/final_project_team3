package com.ict.spentopia.feature.avatar

// 카테고리 타입
enum class AvatarCategory(
    val label: String // 표시 이름
) {
    ALL("전체"), // 전체
    BODY("몸"), // 몸
    HAIR("헤어"), // 헤어
    FACE("표정"), // 표정
    CLOTHES("옷"), // 옷
    ACCESSORY("액세서리") // 액세서리
}

// 액션 버튼 모델
data class AvatarActionUi(
    val text: String, // 버튼 문구
    val highlighted: Boolean = false // 강조 여부
)

// 미리보기 모델
data class AvatarPreviewUi(
    val bodyEmoji: String, // 몸 이모지
    val hairEmoji: String, // 헤어 이모지
    val faceEmoji: String, // 표정 이모지
    val clothesEmoji: String, // 옷 이모지
    val accessoryEmoji: String // 액세서리 이모지
)

// 요약 정보 모델
data class AvatarSummaryUi(
    val totalRarity: String, // 총 희귀도
    val equippedItemCount: String, // 착용 수
    val acquiredDate: String // 획득 날짜
)

// 보상 정보 모델
data class AvatarRewardUi(
    val title: String, // 제목
    val progress: Float, // 진행률
    val description: String // 설명
)

// 아이템 모델
data class AvatarItemUi(
    val emoji: String, // 아이템 이모지
    val name: String, // 아이템 이름
    val rarity: String, // 희귀도
    val selected: Boolean, // 선택 여부
    val locked: Boolean // 잠금 여부
)

// 아이템 섹션 모델
data class AvatarItemSectionUi(
    val category: AvatarCategory, // 섹션 카테고리
    val title: String, // 섹션 제목
    val items: List<AvatarItemUi> // 아이템 목록
)

// 컬렉션 진행도 모델
data class AvatarCollectionProgressUi(
    val title: String, // 등급명
    val value: String, // 진행 수치
    val progress: Float // 진행률
)

// 획득 방법 모델
data class AvatarMethodUi(
    val icon: String, // 아이콘
    val title: String, // 제목
    val desc: String // 설명
)

// 화면 상태
data class AvatarUiState(
    val screenTitle: String = "내 아바타", // 화면 제목
    val ownedItemCount: Int = 10, // 보유 수
    val totalItemCount: Int = 19, // 전체 수
    val selectedCategory: AvatarCategory = AvatarCategory.ALL, // 선택 카테고리
    val actions: List<AvatarActionUi> = emptyList(), // 버튼 목록
    val preview: AvatarPreviewUi = AvatarPreviewUi( // 미리보기
        bodyEmoji = "🧍", // 몸
        hairEmoji = "👱", // 헤어
        faceEmoji = "😊", // 표정
        clothesEmoji = "👕", // 옷
        accessoryEmoji = "✨" // 액세서리
    ),
    val summary: AvatarSummaryUi = AvatarSummaryUi( // 요약 정보
        totalRarity = "에픽", // 희귀도
        equippedItemCount = "5개", // 착용 수
        acquiredDate = "2026.04.08" // 날짜
    ),
    val reward: AvatarRewardUi = AvatarRewardUi( // 보상 정보
        title = "🎁 다음 보상까지", // 보상 제목
        progress = 0.63f, // 진행률
        description = "성실도 점수 25점만 더 모으면 랜덤 아바타!" // 설명
    ),
    val categories: List<AvatarCategory> = AvatarCategory.entries, // 카테고리 목록
    val itemSections: List<AvatarItemSectionUi> = emptyList(), // 섹션 목록
    val collectionProgressList: List<AvatarCollectionProgressUi> = emptyList(), // 컬렉션 목록
    val methodList: List<AvatarMethodUi> = emptyList() // 획득 방법 목록
) {
    // 보유 수 텍스트
    val ownedItemText: String
        get() = "보유 아이템: $ownedItemCount/$totalItemCount"

    // 노출 섹션
    val visibleSections: List<AvatarItemSectionUi>
        get() = if (selectedCategory == AvatarCategory.ALL) {
            itemSections // 전체 노출
        } else {
            itemSections.filter { it.category == selectedCategory } // 카테고리 필터
        }
}

// 더미 상태
fun avatarDummyUiState(): AvatarUiState {
    return AvatarUiState(
        actions = listOf(
            AvatarActionUi(
                text = "🔀 랜덤 코디" // 버튼 문구
            ),
            AvatarActionUi(
                text = "📷 스크린샷" // 버튼 문구
            ),
            AvatarActionUi(
                text = "🔗 공유하기", // 버튼 문구
                highlighted = true // 강조
            )
        ),
        itemSections = listOf(
            AvatarItemSectionUi(
                category = AvatarCategory.BODY, // 카테고리
                title = "몸", // 섹션 제목
                items = listOf(
                    AvatarItemUi(
                        emoji = "🧍", // 이모지
                        name = "기본 몸", // 이름
                        rarity = "일반", // 희귀도
                        selected = true, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "💪", // 이모지
                        name = "근육질", // 이름
                        rarity = "레어", // 희귀도
                        selected = false, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🦴", // 이모지
                        name = "전사 몸", // 이름
                        rarity = "레어", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    )
                )
            ),
            AvatarItemSectionUi(
                category = AvatarCategory.HAIR, // 카테고리
                title = "헤어", // 섹션 제목
                items = listOf(
                    AvatarItemUi(
                        emoji = "👱", // 이모지
                        name = "기본 헤어", // 이름
                        rarity = "일반", // 희귀도
                        selected = true, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🧑", // 이모지
                        name = "긴 머리", // 이름
                        rarity = "일반", // 희귀도
                        selected = false, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🦱", // 이모지
                        name = "금발 머리", // 이름
                        rarity = "레어", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🧓", // 이모지
                        name = "붉은 머리", // 이름
                        rarity = "에픽", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    )
                )
            ),
            AvatarItemSectionUi(
                category = AvatarCategory.FACE, // 카테고리
                title = "표정", // 섹션 제목
                items = listOf(
                    AvatarItemUi(
                        emoji = "😊", // 이모지
                        name = "미소", // 이름
                        rarity = "일반", // 희귀도
                        selected = true, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "😎", // 이모지
                        name = "쿨", // 이름
                        rarity = "일반", // 희귀도
                        selected = false, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "😍", // 이모지
                        name = "하트 눈", // 이름
                        rarity = "레어", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🤩", // 이모지
                        name = "스타", // 이름
                        rarity = "에픽", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    )
                )
            ),
            AvatarItemSectionUi(
                category = AvatarCategory.CLOTHES, // 카테고리
                title = "옷", // 섹션 제목
                items = listOf(
                    AvatarItemUi(
                        emoji = "👕", // 이모지
                        name = "티셔츠", // 이름
                        rarity = "일반", // 희귀도
                        selected = true, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "👔", // 이모지
                        name = "정장", // 이름
                        rarity = "레어", // 희귀도
                        selected = false, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "👗", // 이모지
                        name = "드레스", // 이름
                        rarity = "에픽", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🛡️", // 이모지
                        name = "갑옷", // 이름
                        rarity = "전설", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    )
                )
            ),
            AvatarItemSectionUi(
                category = AvatarCategory.ACCESSORY, // 카테고리
                title = "액세서리", // 섹션 제목
                items = listOf(
                    AvatarItemUi(
                        emoji = "✨", // 이모지
                        name = "없음", // 이름
                        rarity = "일반", // 희귀도
                        selected = true, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🎩", // 이모지
                        name = "모자", // 이름
                        rarity = "일반", // 희귀도
                        selected = false, // 선택
                        locked = false // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "👑", // 이모지
                        name = "왕관", // 이름
                        rarity = "전설", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    ),
                    AvatarItemUi(
                        emoji = "🦋", // 이모지
                        name = "나비", // 이름
                        rarity = "에픽", // 희귀도
                        selected = false, // 선택
                        locked = true // 잠금
                    )
                )
            )
        ),
        collectionProgressList = listOf(
            AvatarCollectionProgressUi(
                title = "일반", // 등급
                value = "8/10", // 수치
                progress = 0.80f // 진행률
            ),
            AvatarCollectionProgressUi(
                title = "레어", // 등급
                value = "5/8", // 수치
                progress = 0.62f // 진행률
            ),
            AvatarCollectionProgressUi(
                title = "에픽", // 등급
                value = "2/6", // 수치
                progress = 0.34f // 진행률
            ),
            AvatarCollectionProgressUi(
                title = "전설", // 등급
                value = "0/3", // 수치
                progress = 0.08f // 진행률
            )
        ),
        methodList = listOf(
            AvatarMethodUi(
                icon = "✅", // 아이콘
                title = "성실도 보상", // 제목
                desc = "주간 성실도 70점 이상 달성 시 랜덤 아바타 지급" // 설명
            ),
            AvatarMethodUi(
                icon = "📥", // 아이콘
                title = "NFT 마켓", // 제목
                desc = "다른 유저와 아이템을 SPT로 거래할 수 있어요" // 설명
            ),
            AvatarMethodUi(
                icon = "✨", // 아이콘
                title = "특별 이벤트", // 제목
                desc = "시즌 이벤트와 콘테스트에서 한정 아이템 획득" // 설명
            )
        )
    )
}