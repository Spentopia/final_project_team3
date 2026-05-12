package com.ict.spentopia.feature.avatar // 이 파일이 속한 패키지 위치를 적음

// 카테고리 타입
enum class AvatarCategory( // AvatarCategory에서 고를 수 있는 값들을 묶음
    val label: String // label 값을 저장함
) { // 이 블록 안의 내용이 시작됨
    ALL("전체"), // ALL 함수를 실행함
    BODY("몸"), // BODY 함수를 실행함
    HAIR("헤어"), // HAIR 함수를 실행함
    FACE("표정"), // FACE 함수를 실행함
    CLOTHES("옷"), // CLOTHES 함수를 실행함
    ACCESSORY("액세서리") // ACCESSORY 함수를 실행함
}

// 액션 버튼 모델
data class AvatarActionUi( // AvatarActionUi 데이터를 묶어둘 클래스 시작
    val text: String, // text 값을 저장함
    val highlighted: Boolean = false // highlighted 값을 저장함
)

// 미리보기 모델
data class AvatarPreviewUi( // AvatarPreviewUi 데이터를 묶어둘 클래스 시작
    val bodyEmoji: String, // bodyEmoji 값을 저장함
    val hairEmoji: String, // hairEmoji 값을 저장함
    val faceEmoji: String, // faceEmoji 값을 저장함
    val clothesEmoji: String, // clothesEmoji 값을 저장함
    val accessoryEmoji: String // accessoryEmoji 값을 저장함
)

// 요약 정보 모델
data class AvatarSummaryUi( // AvatarSummaryUi 데이터를 묶어둘 클래스 시작
    val totalRarity: String, // totalRarity 값을 저장함
    val equippedItemCount: String, // equippedItemCount 값을 저장함
    val acquiredDate: String // acquiredDate 값을 저장함
)

// 보상 정보 모델
data class AvatarRewardUi( // AvatarRewardUi 데이터를 묶어둘 클래스 시작
    val title: String, // 제목을 저장함
    val progress: Float, // progress 값을 저장함
    val description: String // description 값을 저장함
)

// 아이템 모델
data class AvatarItemUi( // AvatarItemUi 데이터를 묶어둘 클래스 시작
    val emoji: String, // emoji 값을 저장함
    val name: String, // name 값을 저장함
    val rarity: String, // rarity 값을 저장함
    val selected: Boolean, // selected 값을 저장함
    val locked: Boolean // locked 값을 저장함
)

// 아이템 섹션 모델
data class AvatarItemSectionUi( // AvatarItemSectionUi 데이터를 묶어둘 클래스 시작
    val category: AvatarCategory, // 카테고리을 저장함
    val title: String, // 제목을 저장함
    val items: List<AvatarItemUi> // items 값을 저장함
)

// 컬렉션 진행도 모델
data class AvatarCollectionProgressUi( // AvatarCollectionProgressUi 데이터를 묶어둘 클래스 시작
    val title: String, // 제목을 저장함
    val value: String, // 입력값을 저장함
    val progress: Float // progress 값을 저장함
)

// 획득 방법 모델
data class AvatarMethodUi( // AvatarMethodUi 데이터를 묶어둘 클래스 시작
    val icon: String, // icon 값을 저장함
    val title: String, // 제목을 저장함
    val desc: String // desc 값을 저장함
)

// 화면 상태
data class AvatarUiState( // AvatarUiState 데이터를 묶어둘 클래스 시작
    val screenTitle: String = "내 아바타", // screenTitle 값을 저장함
    val ownedItemCount: Int = 10, // ownedItemCount 값을 저장함
    val totalItemCount: Int = 19, // totalItemCount 값을 저장함
    val selectedCategory: AvatarCategory = AvatarCategory.ALL, // selectedCategory 값을 저장함
    val actions: List<AvatarActionUi> = emptyList(), // actions 값을 저장함
    val preview: AvatarPreviewUi = AvatarPreviewUi( // preview 값을 저장함
        bodyEmoji = "🧍", // bodyEmoji 값을 정해줌
        hairEmoji = "👱", // hairEmoji 값을 정해줌
        faceEmoji = "😊", // faceEmoji 값을 정해줌
        clothesEmoji = "👕", // clothesEmoji 값을 정해줌
        accessoryEmoji = "✨" // accessoryEmoji 값을 정해줌
    ),
    val summary: AvatarSummaryUi = AvatarSummaryUi( // summary 값을 저장함
        totalRarity = "에픽", // totalRarity 값을 정해줌
        equippedItemCount = "5개", // equippedItemCount 값을 정해줌
        acquiredDate = "2026.04.08" // acquiredDate 값을 정해줌
    ),
    val reward: AvatarRewardUi = AvatarRewardUi( // reward 값을 저장함
        title = "🎁 다음 보상까지", // 제목을 정해줌
        progress = 0.63f, // progress 값을 정해줌
        description = "성실도 점수 25점만 더 모으면 랜덤 아바타!" // description 값을 정해줌
    ),
    val categories: List<AvatarCategory> = AvatarCategory.entries, // categories 값을 저장함
    val itemSections: List<AvatarItemSectionUi> = emptyList(), // itemSections 값을 저장함
    val collectionProgressList: List<AvatarCollectionProgressUi> = emptyList(), // collectionProgressList 값을 저장함
    val methodList: List<AvatarMethodUi> = emptyList() // methodList 값을 저장함
) { // 이 블록 안의 내용이 시작됨
    // 보유 수 텍스트
    val ownedItemText: String // ownedItemText 값을 저장함
        get() = "보유 아이템: $ownedItemCount/$totalItemCount" // get 값을 정해줌

    // 노출 섹션
    val visibleSections: List<AvatarItemSectionUi> // visibleSections 값을 저장함
        get() = if (selectedCategory == AvatarCategory.ALL) { // get 값을 정해줌
            itemSections
        } else { // 이 블록 안의 내용이 시작됨
            itemSections.filter { it.category == selectedCategory } // it.category 값을 정해줌
        }
}

// 더미 상태
fun avatarDummyUiState(): AvatarUiState { // avatarDummyUiState 함수를 선언함
    return AvatarUiState( // 이 값을 함수 결과로 돌려줌
        actions = listOf( // actions 값을 정해줌
            AvatarActionUi( // Avatar Action Ui 함수를 실행함
                text = "🔀 랜덤 코디" // text 값을 정해줌
            ),
            AvatarActionUi( // Avatar Action Ui 함수를 실행함
                text = "📷 스크린샷" // text 값을 정해줌
            ),
            AvatarActionUi( // Avatar Action Ui 함수를 실행함
                text = "🔗 공유하기", // text 값을 정해줌
                highlighted = true // true 값을 highlighted 값에 넣음
            )
        ),
        itemSections = listOf( // itemSections 값을 정해줌
            AvatarItemSectionUi( // Avatar Item Section Ui 함수를 실행함
                category = AvatarCategory.BODY, // 카테고리를 정해줌
                title = "몸", // 제목을 정해줌
                items = listOf( // items 값을 정해줌
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🧍", // emoji 값을 정해줌
                        name = "기본 몸", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = true, // true 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "💪", // emoji 값을 정해줌
                        name = "근육질", // name 값을 정해줌
                        rarity = "레어", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🦴", // emoji 값을 정해줌
                        name = "전사 몸", // name 값을 정해줌
                        rarity = "레어", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    )
                )
            ),
            AvatarItemSectionUi( // Avatar Item Section Ui 함수를 실행함
                category = AvatarCategory.HAIR, // 카테고리를 정해줌
                title = "헤어", // 제목을 정해줌
                items = listOf( // items 값을 정해줌
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "👱", // emoji 값을 정해줌
                        name = "기본 헤어", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = true, // true 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🧑", // emoji 값을 정해줌
                        name = "긴 머리", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🦱", // emoji 값을 정해줌
                        name = "금발 머리", // name 값을 정해줌
                        rarity = "레어", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🧓", // emoji 값을 정해줌
                        name = "붉은 머리", // name 값을 정해줌
                        rarity = "에픽", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    )
                )
            ),
            AvatarItemSectionUi( // Avatar Item Section Ui 함수를 실행함
                category = AvatarCategory.FACE, // 카테고리를 정해줌
                title = "표정", // 제목을 정해줌
                items = listOf( // items 값을 정해줌
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "😊", // emoji 값을 정해줌
                        name = "미소", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = true, // true 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "😎", // emoji 값을 정해줌
                        name = "쿨", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "😍", // emoji 값을 정해줌
                        name = "하트 눈", // name 값을 정해줌
                        rarity = "레어", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🤩", // emoji 값을 정해줌
                        name = "스타", // name 값을 정해줌
                        rarity = "에픽", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    )
                )
            ),
            AvatarItemSectionUi( // Avatar Item Section Ui 함수를 실행함
                category = AvatarCategory.CLOTHES, // 카테고리를 정해줌
                title = "옷", // 제목을 정해줌
                items = listOf( // items 값을 정해줌
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "👕", // emoji 값을 정해줌
                        name = "티셔츠", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = true, // true 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "👔", // emoji 값을 정해줌
                        name = "정장", // name 값을 정해줌
                        rarity = "레어", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "👗", // emoji 값을 정해줌
                        name = "드레스", // name 값을 정해줌
                        rarity = "에픽", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🛡️", // emoji 값을 정해줌
                        name = "갑옷", // name 값을 정해줌
                        rarity = "전설", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    )
                )
            ),
            AvatarItemSectionUi( // Avatar Item Section Ui 함수를 실행함
                category = AvatarCategory.ACCESSORY, // 카테고리를 정해줌
                title = "액세서리", // 제목을 정해줌
                items = listOf( // items 값을 정해줌
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "✨", // emoji 값을 정해줌
                        name = "없음", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = true, // true 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🎩", // emoji 값을 정해줌
                        name = "모자", // name 값을 정해줌
                        rarity = "일반", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = false // false 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "👑", // emoji 값을 정해줌
                        name = "왕관", // name 값을 정해줌
                        rarity = "전설", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    ),
                    AvatarItemUi( // Avatar Item Ui 함수를 실행함
                        emoji = "🦋", // emoji 값을 정해줌
                        name = "나비", // name 값을 정해줌
                        rarity = "에픽", // rarity 값을 정해줌
                        selected = false, // false 값을 selected 값에 넣음
                        locked = true // true 값을 locked 값에 넣음
                    )
                )
            )
        ),
        collectionProgressList = listOf( // collectionProgressList 값을 정해줌
            AvatarCollectionProgressUi( // Avatar Collection Progress Ui 함수를 실행함
                title = "일반", // 제목을 정해줌
                value = "8/10", // 입력값을 정해줌
                progress = 0.80f // progress 값을 정해줌
            ),
            AvatarCollectionProgressUi( // Avatar Collection Progress Ui 함수를 실행함
                title = "레어", // 제목을 정해줌
                value = "5/8", // 입력값을 정해줌
                progress = 0.62f // progress 값을 정해줌
            ),
            AvatarCollectionProgressUi( // Avatar Collection Progress Ui 함수를 실행함
                title = "에픽", // 제목을 정해줌
                value = "2/6", // 입력값을 정해줌
                progress = 0.34f // progress 값을 정해줌
            ),
            AvatarCollectionProgressUi( // Avatar Collection Progress Ui 함수를 실행함
                title = "전설", // 제목을 정해줌
                value = "0/3", // 입력값을 정해줌
                progress = 0.08f // progress 값을 정해줌
            )
        ),
        methodList = listOf( // methodList 값을 정해줌
            AvatarMethodUi( // Avatar Method Ui 함수를 실행함
                icon = "✅", // icon 값을 정해줌
                title = "성실도 보상", // 제목을 정해줌
                desc = "주간 성실도 70점 이상 달성 시 랜덤 아바타 지급" // desc 값을 정해줌
            ),
            AvatarMethodUi( // Avatar Method Ui 함수를 실행함
                icon = "📥", // icon 값을 정해줌
                title = "NFT 마켓", // 제목을 정해줌
                desc = "다른 유저와 아이템을 SPT로 거래할 수 있어요" // desc 값을 정해줌
            ),
            AvatarMethodUi( // Avatar Method Ui 함수를 실행함
                icon = "✨", // icon 값을 정해줌
                title = "특별 이벤트", // 제목을 정해줌
                desc = "시즌 이벤트와 콘테스트에서 한정 아이템 획득" // desc 값을 정해줌
            )
        )
    )
}