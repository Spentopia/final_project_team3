package com.ict.spentopia.feature.home // 패키지 선언: 이 파일이 속한 패키지를 정의

import android.app.DatePickerDialog
import androidx.compose.foundation.* // Compose의 기본 구성요소를 가져옴
import androidx.compose.foundation.layout.* // 레이아웃을 위한 구성요소를 가져옴
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.* // Material Design 3 구성요소를 가져옴
import androidx.compose.runtime.Composable // Composable 함수를 사용하기 위한 임포트
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment // 정렬을 위한 클래스
import androidx.compose.ui.Modifier // 수정자를 위한 클래스
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color // 색상 랜더링을 위한 클래스
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight // 폰트 두께 설정을 위한 클래스
import androidx.compose.ui.unit.dp // dp 단위를 사용하기 위한 클래스
import androidx.compose.ui.unit.sp // sp 단위를 사용하기 위한 클래스
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar
import kotlin.math.abs

data class ExpenseItemData( // 소비 항목 데이터를 담는 데이터 클래스
    val id: Long, // 항목 고유 ID
    val date: String, // 소비 날짜
    val title: String, // 소비 제목
    val category: String, // 소비 카테고리
    val amount: Int, // 소비 금액
    val memo: String, // 구매 메모
    val receiptImageName: String, // 영수증 이미지 이름
    val diary: String // 한줄 소비 일기
)

data class CalendarDateData( // 캘린더 날짜 정보를 담는 데이터 클래스
    val fullDate: String, // 실제 날짜 값 yyyy-MM-dd
    val dayText: String, // 화면에 보여줄 날짜 텍스트
    val isCurrentMonth: Boolean // 현재 달 포함 여부
)

data class HomeMenuItemData( // 홈 드로어 메뉴 항목 데이터를 담는 데이터 클래스
    val title: String, // 메뉴 제목
    val emoji: String, // 메뉴 앞 이모지
    val onClick: () -> Unit // 메뉴 클릭 시 실행할 콜백
)

@Composable
fun HomeScreen(
    onLedgerClick: () -> Unit = {}, // 가계부 클릭 이벤트를 위한 콜백
    onMyPageClick: () -> Unit = {}, // 마이페이지 클릭 이벤트를 위한 콜백
    onBudgetClick: () -> Unit = {}, // 예산 설정 클릭 이벤트를 위한 콜백
    onAnalysisClick: () -> Unit = {}, // 소비 분석 클릭 이벤트를 위한 콜백
    onAvatarClick: () -> Unit = {}, // 내 아바타 클릭 이벤트를 위한 콜백
    onMarketClick: () -> Unit = {}, // NFT 마켓 클릭 이벤트를 위한 콜백
    onPlazaClick: () -> Unit = {}, // 광장 클릭 이벤트를 위한 콜백
    onCommunityClick: () -> Unit = {} // 커뮤니티 클릭 이벤트를 위한 콜백
) {
    var expenseList by remember { // 소비 목록 상태를 관리
        mutableStateOf(
            listOf(
                ExpenseItemData(
                    id = 1L,
                    date = "2026-04-10",
                    title = "점심 식사",
                    category = "식비",
                    amount = 12000,
                    memo = "친구들이랑 점심 먹음",
                    receiptImageName = "lunch_receipt.jpg",
                    diary = "오늘은 친구들과 맛있는 점심을 먹었다"
                ),
                ExpenseItemData(
                    id = 2L,
                    date = "2026-04-10",
                    title = "택시",
                    category = "교통",
                    amount = 3500,
                    memo = "집까지 이동",
                    receiptImageName = "",
                    diary = ""
                )
            )
        )
    }

    var selectedDate by remember { mutableStateOf("2026-04-10") } // 현재 선택된 날짜 상태
    var currentYear by remember { mutableStateOf(2026) } // 현재 달력에 표시할 연도 상태
    var currentMonth by remember { mutableStateOf(4) } // 현재 달력에 표시할 월 상태
    var editingExpense by remember { mutableStateOf<ExpenseItemData?>(null) } // 수정 중인 소비 항목 상태

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) // 사이드 메뉴 드로어 상태를 관리
    val scope = rememberCoroutineScope() // 드로어 열기/닫기를 위한 코루틴 스코프

    val monthlyBudget = 500000 // 이번 달 기본 예산

    val expenseDateSet = remember(expenseList) { // 소비 기록이 존재하는 날짜 목록을 Set으로 관리
        expenseList.map { it.date }.toSet()
    }

    val today = remember { // 오늘 날짜를 yyyy-MM-dd 형식으로 생성
        val cal = Calendar.getInstance()
        formatDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val currentMonthExpenseList = remember(expenseList, currentYear, currentMonth) { // 현재 보고 있는 달의 소비 목록
        expenseList.filter { expense ->
            val year = expense.date.substring(0, 4).toIntOrNull() ?: 0
            val month = expense.date.substring(5, 7).toIntOrNull() ?: 0
            year == currentYear && month == currentMonth
        }
    }

    val currentMonthTotalExpense = remember(currentMonthExpenseList) { // 이번 달 총 소비 금액
        currentMonthExpenseList.sumOf { it.amount }
    }

    val previousMonthInfo = remember(currentYear, currentMonth) { // 지난달 연도와 월 계산
        moveMonth(currentYear, currentMonth, -1)
    }

    val previousMonthExpenseList = remember(expenseList, previousMonthInfo) { // 지난달 소비 목록
        expenseList.filter { expense ->
            val year = expense.date.substring(0, 4).toIntOrNull() ?: 0
            val month = expense.date.substring(5, 7).toIntOrNull() ?: 0
            year == previousMonthInfo.first && month == previousMonthInfo.second
        }
    }

    val previousMonthTotalExpense = remember(previousMonthExpenseList) { // 지난달 총 소비 금액
        previousMonthExpenseList.sumOf { it.amount }
    }

    val remainingBudget = monthlyBudget - currentMonthTotalExpense // 남은 예산 계산

    val usageRate = if (monthlyBudget > 0) { // 사용률 계산
        ((currentMonthTotalExpense.toFloat() / monthlyBudget.toFloat()) * 100).toInt()
    } else {
        0
    }

    val changeRateText = remember(currentMonthTotalExpense, previousMonthTotalExpense) { // 지난달 대비 증감 문구 계산
        createChangeRateText(
            currentAmount = currentMonthTotalExpense,
            previousAmount = previousMonthTotalExpense
        )
    }

    val menuItemList = remember( // 드로어에 표시할 메뉴 목록을 구성
        onLedgerClick,
        onBudgetClick,
        onAnalysisClick,
        onAvatarClick,
        onMarketClick,
        onPlazaClick,
        onCommunityClick,
        onMyPageClick
    ) {
        listOf(
            HomeMenuItemData(
                title = "가계부",
                emoji = "📒",
                onClick = onLedgerClick
            ),
            HomeMenuItemData(
                title = "예산 설정",
                emoji = "💰",
                onClick = onBudgetClick
            ),
            HomeMenuItemData(
                title = "소비 분석",
                emoji = "📊",
                onClick = onAnalysisClick
            ),
            HomeMenuItemData(
                title = "내 아바타",
                emoji = "🧍",
                onClick = onAvatarClick
            ),
            HomeMenuItemData(
                title = "NFT 마켓",
                emoji = "🖼️",
                onClick = onMarketClick
            ),
            HomeMenuItemData(
                title = "광장",
                emoji = "🏛️",
                onClick = onPlazaClick
            ),
            HomeMenuItemData(
                title = "커뮤니티",
                emoji = "💬",
                onClick = onCommunityClick
            ),
            HomeMenuItemData(
                title = "마이페이지",
                emoji = "👤",
                onClick = onMyPageClick
            )
        )
    }

    ModalNavigationDrawer( // 사이드 메뉴가 열리는 드로어 레이아웃
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp), // 드로어 너비 설정
                drawerContainerColor = Color.White // 드로어 배경 색상 설정
            ) {
                HomeDrawerContent( // 홈 드로어 메뉴 내용을 표시
                    menuItemList = menuItemList,
                    onCloseClick = {
                        scope.launch {
                            drawerState.close() // 닫기 버튼 클릭 시 드로어 닫기
                        }
                    },
                    onMenuItemClick = { menuItem ->
                        scope.launch {
                            drawerState.close() // 메뉴 클릭 시 먼저 드로어 닫기
                        }
                        menuItem.onClick() // 각 메뉴에 연결된 화면 이동 콜백 실행
                    }
                )
            }
        }
    ) {
        LazyColumn( // 스크롤 가능한 열 구성 요소입니다.
            modifier = Modifier
                .fillMaxSize() // 가능한 최대 크기로 확장
                .background(Color(0xFFF3F6FA)) // 배경 색상을 설정
                .padding(horizontal = 16.dp), // 좌우 패딩을 16dp로 설정
            verticalArrangement = Arrangement.spacedBy(16.dp) // 아이템 간의 수직 간격을 16dp로 설정.
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) } // 위쪽 여백을 추가

            item {
                TopHeaderSection(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open() // 상단 메뉴 버튼 클릭 시 드로어 열기
                        }
                    }
                ) // 상단 헤더 섹션을 추가합니다.
            }

            item {
                MonthlySummaryCard( // 월간 요약 카드에 실제 데이터 전달
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    currentMonthTotalExpense = currentMonthTotalExpense,
                    monthlyBudget = monthlyBudget,
                    remainingBudget = remainingBudget,
                    usageRate = usageRate,
                    changeRateText = changeRateText
                ) // 월간 요약 카드 추가
            }

            item {
                CalendarCard(
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    expenseDateSet = expenseDateSet,
                    today = today,
                    onPrevMonth = {
                        val previous = moveMonth(currentYear, currentMonth, -1)
                        currentYear = previous.first
                        currentMonth = previous.second
                    },
                    onNextMonth = {
                        val next = moveMonth(currentYear, currentMonth, 1)
                        currentYear = next.first
                        currentMonth = next.second
                    },
                    onDateSelected = { clickedDate ->
                        selectedDate = clickedDate // 캘린더에서 선택한 날짜로 상태 변경
                        currentYear = clickedDate.substring(0, 4).toInt() // 선택한 날짜의 연도로 이동
                        currentMonth = clickedDate.substring(5, 7).toInt() // 선택한 날짜의 월로 이동
                    }
                ) // 캘린더 카드 추가
            }

            item {
                DailyExpenseCard(
                    expenseList = expenseList,
                    selectedDate = selectedDate,
                    onEditExpense = { expense ->
                        editingExpense = expense // 수정할 항목 설정
                        selectedDate = expense.date // 수정할 항목 날짜로 이동
                        currentYear = expense.date.substring(0, 4).toInt() // 수정할 항목 연도로 이동
                        currentMonth = expense.date.substring(5, 7).toInt() // 수정할 항목 월로 이동
                    },
                    onDeleteExpense = { expenseId ->
                        expenseList = expenseList.filter { it.id != expenseId } // 선택한 항목 삭제
                        if (editingExpense?.id == expenseId) {
                            editingExpense = null // 수정 중이던 항목이 삭제되면 수정 상태 해제
                        }
                    }
                ) // 일일 소비 내역 카드 추가
            }

            item {
                WeeklyScoreCard() // 주간 성적 카드 추가
            }

            item {
                ExpenseWriteCard(
                    selectedDate = selectedDate,
                    editingExpense = editingExpense,
                    onSaveExpense = { savedExpense ->
                        if (editingExpense == null) {
                            expenseList = listOf(savedExpense) + expenseList // 새 항목을 맨 위에 추가
                        } else {
                            expenseList = expenseList.map { item ->
                                if (item.id == savedExpense.id) savedExpense else item
                            } // 기존 항목 수정 반영
                        }

                        editingExpense = null // 저장 후 수정 상태 해제
                        selectedDate = savedExpense.date // 저장한 날짜로 선택 상태 변경
                        currentYear = savedExpense.date.substring(0, 4).toInt() // 저장한 날짜 연도로 이동
                        currentMonth = savedExpense.date.substring(5, 7).toInt() // 저장한 날짜 월로 이동
                    },
                    onCancelEdit = {
                        editingExpense = null // 수정 취소
                    }
                ) // 소비 기록하기 카드 추가
            }

            item {
                RewardGuideCard() // 보상 안내 카드 추가
            }

            item { Spacer(modifier = Modifier.height(24.dp)) } // 아래쪽 여백을 추가
        }
    }
}

@Composable
private fun HomeDrawerContent(
    menuItemList: List<HomeMenuItemData>, // 드로어에 표시할 메뉴 목록
    onCloseClick: () -> Unit, // 닫기 버튼 클릭 시 호출할 콜백
    onMenuItemClick: (HomeMenuItemData) -> Unit // 메뉴 클릭 시 호출할 콜백
) { // 홈 드로어 내용을 정의하는 Composable 함수
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // 양쪽 정렬
            verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
        ) {
            Column {
                Text(
                    text = "Spentopia", // 드로어 상단 앱 제목
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1F2A37)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "원하는 메뉴로 바로 이동해보세요", // 안내 문구
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            TextButton(onClick = onCloseClick) { // 닫기 버튼
                Text(
                    text = "닫기",
                    color = Color(0xFF2F7DF6),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 아래쪽 여백 추가

        menuItemList.forEachIndexed { index, menuItem ->
            HomeDrawerMenuItem(
                emoji = menuItem.emoji,
                title = menuItem.title,
                onClick = {
                    onMenuItemClick(menuItem) // 선택한 메뉴 항목 전달
                }
            )

            if (index != menuItemList.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp)) // 메뉴 사이 여백 추가
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // 하단 영역 밀어내기

        Card(
            modifier = Modifier.fillMaxWidth(), // 하단 안내 카드 너비 설정
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF4F8FF)
            ),
            border = BorderStroke(1.dp, Color(0xFFDDE8FF))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "오늘의 한마디", // 하단 카드 제목
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF22406A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "기록이 쌓일수록 소비 습관이 더 또렷하게 보여요.", // 하단 카드 설명
                    fontSize = 13.sp,
                    color = Color(0xFF5B6573),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun HomeDrawerMenuItem(
    emoji: String, // 메뉴 앞에 표시할 이모지
    title: String, // 메뉴 제목
    onClick: () -> Unit // 메뉴 클릭 시 호출할 콜백
) { // 드로어 메뉴 한 줄을 정의하는 Composable 함수
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // 메뉴 클릭 시 콜백 실행
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFBFCFE)
        ),
        border = BorderStroke(1.dp, Color(0xFFE8EDF5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = Color(0xFFEAF3FF),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 19.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp)) // 오른쪽 여백 추가

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2A37)
            )
        }
    }
}

@Composable
private fun TopHeaderSection(
    onMenuClick: () -> Unit // 상단 메뉴 버튼 클릭 이벤트
) { // 상단 헤더 섹션을 정의하는 Composable 함수입니다.
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정합니다.
        shape = RoundedCornerShape(24.dp), // 카드 모서리를 둥글게 만듭니다.
        colors = CardDefaults.cardColors(containerColor = Color.White) // 카드 색상을 흰색으로 설정
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth() // 가로로 최대 너비를 채웁니다.
                .padding(horizontal = 14.dp, vertical = 12.dp), // 카드 내부 패딩을 설정
            verticalAlignment = Alignment.Top, // 아이템을 위쪽 정렬
            horizontalArrangement = Arrangement.SpaceBetween // 가로 방향으로 아이템들 사이에 공간을 두기
        ) {
            IconButton(onClick = onMenuClick) { // 메뉴 아이콘 버튼 클릭 시 드로어 열기
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "menu"
                )
            }

            Column( // 가운데 텍스트 섹션
                modifier = Modifier.weight(1f) // 남은 공간을 차지하도록 설정
            ) {
                Text(
                    text = "👋 오늘도 알뜰한 소비 하세요", // 인사 메시지
                    fontSize = 16.sp, // 글자 크기
                    fontWeight = FontWeight.Bold, // 글자 두께
                    color = Color(0xFF1F2A37) // 글자 색상
                )
                Text(
                    text = "오늘도 알뜰한 소비 하세요", // 추가 메시지
                    fontSize = 13.sp, // 글자 크기
                    color = Color(0xFF6B7280) // 글자 색상
                )
            }

            Row { // 오른쪽 아이콘 버튼 섹션
                IconButton(onClick = { }) { // 설정 아이콘 버튼
                    Icon(Icons.Outlined.Settings, contentDescription = "settings") // 설정 아이콘 추가
                }
                IconButton(onClick = { }) { // 알림 아이콘 버튼
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = "notification") // 알림 아이콘 추가
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard( // 월간 요약 카드를 정의하는 Composable 함수
    currentYear: Int, // 현재 표시 중인 연도
    currentMonth: Int, // 현재 표시 중인 월
    currentMonthTotalExpense: Int, // 이번 달 총 소비 금액
    monthlyBudget: Int, // 예산 금액
    remainingBudget: Int, // 남은 예산 금액
    usageRate: Int, // 예산 사용률
    changeRateText: String // 지난달 대비 변화 텍스트
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
        shape = RoundedCornerShape(24.dp), // 카드 모서리를 둥글게 만듬
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6FA)) // 카드 색상을 연한 회색으로 설정함
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // 카드 내부에 패딩을 추가
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // 가로 방향으로 최대 너비를 채움
                horizontalArrangement = Arrangement.SpaceBetween // 아이템들 사이에 공간을 둠
            ) {
                Column { // 왼쪽 요약 섹션
                    Text(
                        text = "${currentYear}년 ${currentMonth}월", // 현재 월 표시
                        fontSize = 16.sp, // 글자 크기
                        fontWeight = FontWeight.Bold, // 글자 두께
                        color = Color(0xFF1F2A37) // 글자 색상
                    )
                    Text(
                        text = "이번 달 소비 내역", // 소비 내역 제목
                        fontSize = 13.sp, // 글자 크기
                        color = Color(0xFF6B7280) // 글자 색상
                    )
                }

                Column(horizontalAlignment = Alignment.End) { // 오른쪽 금액 섹션
                    Text(
                        text = "${formatAmount(currentMonthTotalExpense)}원", // 실제 소비 금액
                        fontSize = 28.sp, // 글자 크기
                        fontWeight = FontWeight.Bold, // 글자 두께
                        color = Color(0xFF1F2A37) // 글자 색상
                    )
                    Text(
                        text = changeRateText, // 지난달 대비 소비 변화량
                        fontSize = 13.sp, // 글자 크기
                        color = getChangeRateColor(changeRateText) // 변화 방향에 따른 색상
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Row(
                modifier = Modifier.fillMaxWidth(), // 가로 방향으로 최대 너비를 채움
                horizontalArrangement = Arrangement.spacedBy(12.dp) // 아이템 간의 간격을 12dp로 설정
            ) {
                SummaryMiniCard("예산", "${formatAmount(monthlyBudget)}원", Color(0xFFDDF3F7), Modifier.weight(1f)) // 예산 카드
                SummaryMiniCard("남은 예산", "${formatAmount(remainingBudget)}원", Color(0xFFE1EAFF), Modifier.weight(1f)) // 남은 예산 카드
                SummaryMiniCard("사용률", "${usageRate}%", Color(0xFFDFF2EC), Modifier.weight(1f)) // 소비 사용률 카드
            }
        }
    }
}

@Composable
private fun SummaryMiniCard( // 요약 미니 카드를 정의하는 Composable 함수
    title: String, // 카드 제목
    value: String, // 카드 값
    bgColor: Color, // 카드 배경 색상
    modifier: Modifier = Modifier // 수정자
) {
    Card(
        modifier = modifier, // 수정자 적용
        shape = RoundedCornerShape(16.dp), // 카드 모서리를 둥글게 만듬
        colors = CardDefaults.cardColors(containerColor = bgColor) // 카드 배경 색상
    ) {
        Column(
            modifier = Modifier.padding(14.dp) // 카드 내부에 패딩 추가
        ) {
            Text(
                text = title, // 카드 제목
                fontSize = 12.sp, // 글자 크기
                color = Color(0xFF315072) // 글자 색상
            )
            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가
            Text(
                text = value, // 카드 값
                fontSize = 20.sp, // 글자 크기
                fontWeight = FontWeight.Bold, // 글자 두께
                color = Color(0xFF22406A) // 글자 색상
            )
        }
    }
}

@Composable
private fun CalendarCard(
    currentYear: Int, // 현재 달력에 표시할 연도
    currentMonth: Int, // 현재 달력에 표시할 월
    selectedDate: String, // 현재 선택된 날짜
    expenseDateSet: Set<String>, // 소비 기록이 있는 날짜 집합
    today: String, // 오늘 날짜
    onPrevMonth: () -> Unit, // 이전 달 이동 콜백
    onNextMonth: () -> Unit, // 다음 달 이동 콜백
    onDateSelected: (String) -> Unit // 날짜 클릭 시 호출할 콜백
) { // 캘린더 카드를 정의하는 Composable 함수
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토") // 요일 목록
    val calendarDates = generateCalendarDates(currentYear, currentMonth) // 현재 달 기준 날짜 목록 생성
    val rows = calendarDates.chunked(7) // 7개씩 묶어서 주 단위로 분리
    val cellWidth = 40.dp // 날짜 한 칸의 고정 너비

    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
        shape = RoundedCornerShape(24.dp), // 카드 모서리를 둥글게 설정
        colors = CardDefaults.cardColors(containerColor = Color.White) // 카드 배경 색상
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // 카드 내부 패딩
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
                horizontalArrangement = Arrangement.SpaceBetween, // 양쪽 정렬
                verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
            ) {
                CalendarArrowButton(
                    text = "‹",
                    onClick = onPrevMonth
                ) // 이전 달 버튼

                Text(
                    text = "${currentMonth}월 ${currentYear}", // 현재 달 표시
                    fontSize = 18.sp, // 글자 크기
                    fontWeight = FontWeight.Medium, // 글자 두께
                    color = Color(0xFF1F2A37) // 글자 색상
                )

                CalendarArrowButton(
                    text = "›",
                    onClick = onNextMonth
                ) // 다음 달 버튼
            }

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                daysOfWeek.forEach { day ->
                    Box(
                        modifier = Modifier.width(cellWidth), // 요일 칸 고정 너비
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 13.sp,
                            color = Color(0xFF9AA4B2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp)) // 아래쪽 여백 추가

            rows.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    week.forEach { dateItem ->
                        val isSelected = dateItem.fullDate == selectedDate // 현재 선택된 날짜인지 확인
                        val hasExpense = expenseDateSet.contains(dateItem.fullDate) // 소비 기록이 있는 날짜인지 확인
                        val isToday = dateItem.fullDate == today // 오늘 날짜인지 확인

                        Box(
                            modifier = Modifier
                                .width(cellWidth) // 날짜 칸 고정 너비
                                .height(42.dp)
                                .clickable {
                                    onDateSelected(dateItem.fullDate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
                                                color = Color(0xFF0F172A),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dateItem.dayText,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = dateItem.dayText,
                                        color = when {
                                            !dateItem.isCurrentMonth -> Color(0xFF9AA4B2)
                                            isToday -> Color(0xFF2F7DF6)
                                            else -> Color(0xFF1F2A37)
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp)) // 날짜와 점 사이 여백

                                if (hasExpense) { // 소비 기록이 있는 날짜면 dot 표시
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                color = if (isSelected) Color.White else Color(0xFF2F7DF6),
                                                shape = CircleShape
                                            )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp)) // dot이 없을 때도 높이 유지
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarArrowButton(
    text: String, // 화살표 버튼 텍스트
    onClick: () -> Unit // 버튼 클릭 이벤트
) { // 달력 이동 화살표 버튼을 정의하는 Composable 함수
    Box(
        modifier = Modifier
            .size(28.dp) // 버튼 크기
            .background(
                color = Color(0xFFF3F4F6), // 버튼 배경 색상
                shape = RoundedCornerShape(8.dp) // 둥근 모서리
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center // 가운데 정렬
    ) {
        Text(
            text = text, // 화살표 텍스트
            color = Color(0xFF6B7280), // 글자 색상
            fontSize = 16.sp, // 글자 크기
            fontWeight = FontWeight.Medium // 글자 두께
        )
    }
}

@Composable
private fun DailyExpenseCard(
    expenseList: List<ExpenseItemData>, // 화면에 표시할 소비 목록
    selectedDate: String, // 캘린더에서 선택된 날짜
    onEditExpense: (ExpenseItemData) -> Unit, // 수정 버튼 클릭 시 호출할 콜백
    onDeleteExpense: (Long) -> Unit // 삭제 버튼 클릭 시 호출할 콜백
) { // 일일 소비 내역 카드를 정의하는 Composable 함수
    val filteredList = expenseList.filter { it.date == selectedDate } // 선택된 날짜의 소비 내역만 필터링
    val totalAmount = filteredList.sumOf { it.amount } // 총 소비 금액 계산
    val diaryText = filteredList.firstOrNull { it.diary.isNotBlank() }?.diary ?: "" // 한줄 소비 일기 추출

    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
        shape = RoundedCornerShape(24.dp), // 카드 모서리를 둥글게 설정
        colors = CardDefaults.cardColors(containerColor = Color.White) // 카드 배경 색상
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // 카드 내부 패딩
        ) {
            Text(
                text = formatDisplayDate(selectedDate), // 카드 제목
                fontSize = 22.sp, // 글자 크기
                fontWeight = FontWeight.Bold, // 글자 두께
                color = Color(0xFF1F2A37) // 글자 색상
            )

            Spacer(modifier = Modifier.height(6.dp)) // 아래쪽 여백 추가

            Text(
                text = "총 ${formatAmount(totalAmount)}원 · ${filteredList.size}건", // 총 소비 금액 및 건수
                fontSize = 14.sp, // 글자 크기
                color = Color(0xFF6B7280) // 글자 색상
            )

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            if (filteredList.isEmpty()) {
                Text(
                    text = "이 날짜에는 아직 저장된 소비 내역이 없어요", // 빈 상태 문구
                    fontSize = 14.sp, // 글자 크기
                    color = Color(0xFF6B7280) // 글자 색상
                )
            } else {
                filteredList.forEachIndexed { index, item ->
                    ExpenseItemCard(
                        emoji = getCategoryEmoji(item.category),
                        title = item.title,
                        category = item.category,
                        amount = "${formatAmount(item.amount)}원",
                        tag = when {
                            item.receiptImageName.isNotBlank() -> "영수증"
                            item.memo.isNotBlank() -> "메모"
                            else -> null
                        },
                        iconColors = getCategoryColors(item.category),
                        onEditClick = {
                            onEditExpense(item) // 수정 이벤트 전달
                        },
                        onDeleteClick = {
                            onDeleteExpense(item.id) // 삭제 이벤트 전달
                        }
                    ) // 소비 항목 카드 표시

                    if (index != filteredList.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp)) // 카드 간 여백 추가
                    }
                }

                if (diaryText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

                    Card(
                        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
                        shape = RoundedCornerShape(16.dp), // 카드 모서리를 둥글게 설정
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF7FBFE) // 카드 배경 색상
                        ),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF86D4FF)
                        ) // 테두리 설정
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp) // 카드 내부 패딩
                        ) {
                            Text(
                                text = "오늘의 소비 일기", // 소제목
                                fontSize = 15.sp, // 글자 크기
                                fontWeight = FontWeight.Bold, // 글자 두께
                                color = Color(0xFF22406A) // 글자 색상
                            )

                            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

                            Text(
                                text = diaryText, // 일기 내용
                                fontSize = 14.sp, // 글자 크기
                                color = Color(0xFF475569) // 글자 색상
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseItemCard( // 소비 항목 카드를 정의하는 Composable 함수
    emoji: String, // 아이콘 대신 사용할 이모지
    title: String, // 소비 제목
    category: String, // 소비 카테고리
    amount: String, // 소비 금액
    tag: String?, // 선택적으로 표시할 태그
    iconColors: List<Color>, // 아이콘 배경 그라데이션 색상 목록
    onEditClick: () -> Unit, // 수정 버튼 클릭 이벤트
    onDeleteClick: () -> Unit // 삭제 버튼 클릭 이벤트
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
        shape = RoundedCornerShape(18.dp), // 카드 모서리를 둥글게 설정
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD)), // 카드 배경 색상
        border = BorderStroke(
            1.dp,
            Color(0xFFE5E7EB)
        ) // 카드 테두리 설정
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
                verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp) // 아이콘 박스 크기
                        .background(
                            brush = Brush.horizontalGradient(iconColors), // 그라데이션 배경 적용
                            shape = RoundedCornerShape(12.dp) // 둥근 모서리
                        ),
                    contentAlignment = Alignment.Center // 가운데 정렬
                ) {
                    Text(
                        text = emoji, // 이모지 표시
                        fontSize = 18.sp // 글자 크기
                    )
                }

                Spacer(modifier = Modifier.width(12.dp)) // 오른쪽 여백 추가

                Column(
                    modifier = Modifier.weight(1f) // 남은 공간 차지
                ) {
                    Text(
                        text = title, // 소비 제목
                        fontSize = 16.sp, // 글자 크기
                        fontWeight = FontWeight.Bold, // 글자 두께
                        color = Color(0xFF1F2A37) // 글자 색상
                    )

                    Spacer(modifier = Modifier.height(4.dp)) // 아래쪽 여백 추가

                    Row(verticalAlignment = Alignment.CenterVertically) { // 카테고리와 태그를 가로 배치
                        Text(
                            text = category, // 카테고리 텍스트
                            fontSize = 13.sp, // 글자 크기
                            color = Color(0xFF6B7280) // 글자 색상
                        )

                        if (tag != null) {
                            Spacer(modifier = Modifier.width(8.dp)) // 태그 앞 여백 추가

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFEFFCF3), // 태그 배경 색상
                                        shape = RoundedCornerShape(20.dp) // 둥근 모서리
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp), // 태그 내부 패딩
                                contentAlignment = Alignment.Center // 가운데 정렬
                            ) {
                                Text(
                                    text = tag, // 태그 텍스트
                                    fontSize = 11.sp, // 글자 크기
                                    color = Color(0xFF16A34A), // 글자 색상
                                    fontWeight = FontWeight.Medium // 글자 두께
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp)) // 오른쪽 금액과의 여백 추가

                Text(
                    text = amount, // 금액 텍스트
                    fontSize = 16.sp, // 글자 크기
                    fontWeight = FontWeight.Bold, // 글자 두께
                    color = Color(0xFF1F2A37) // 글자 색상
                )
            }

            Spacer(modifier = Modifier.height(10.dp)) // 아래쪽 여백 추가

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEditClick
                ) {
                    Text(
                        text = "수정",
                        color = Color(0xFF2F7DF6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onDeleteClick
                ) {
                    Text(
                        text = "삭제",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyScoreCard() { // 주간 성실도 카드를 정의하는 Composable 함수
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
        shape = RoundedCornerShape(24.dp), // 카드 모서리를 둥글게 설정
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FBFF) // 카드 배경 색상을 연한 블루톤으로 설정
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFFDCEBFF)
        ) // 카드 테두리 설정
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // 카드 내부 패딩
        ) {
            Text(
                text = "이번 주 성실도", // 카드 제목
                fontSize = 20.sp, // 글자 크기
                fontWeight = FontWeight.Bold, // 글자 두께
                color = Color(0xFF1F2A37) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            Text(
                text = "소비 기록을 꾸준히 남긴 정도를 보여줘요", // 안내 문구
                fontSize = 14.sp, // 글자 크기
                color = Color(0xFF6B7280) // 글자 색상
            )

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Row(
                modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
                horizontalArrangement = Arrangement.SpaceBetween, // 양쪽 정렬
                verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
            ) {
                Column {
                    Text(
                        text = "85%", // 성실도 수치
                        fontSize = 34.sp, // 글자 크기
                        fontWeight = FontWeight.ExtraBold, // 글자 두께
                        color = Color(0xFF2F7DF6) // 강조 색상
                    )

                    Spacer(modifier = Modifier.height(4.dp)) // 아래쪽 여백 추가

                    Text(
                        text = "아주 잘하고 있어요!", // 칭찬 문구
                        fontSize = 14.sp, // 글자 크기
                        color = Color(0xFF16A34A), // 글자 색상
                        fontWeight = FontWeight.SemiBold // 글자 두께
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp) // 원 크기
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFBFE0FF),
                                    Color(0xFF2F7DF6)
                                )
                            ),
                            shape = RoundedCornerShape(100.dp) // 원형에 가까운 둥근 모서리
                        ),
                    contentAlignment = Alignment.Center // 가운데 정렬
                ) {
                    Text(
                        text = "🌟", // 임시 아이콘
                        fontSize = 28.sp // 글자 크기
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // 아래쪽 여백 추가

            Text(
                text = "주간 기록", // 소제목
                fontSize = 15.sp, // 글자 크기
                fontWeight = FontWeight.Bold, // 글자 두께
                color = Color(0xFF22406A) // 글자 색상
            )

            Spacer(modifier = Modifier.height(14.dp)) // 아래쪽 여백 추가

            Row(
                modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
                horizontalArrangement = Arrangement.SpaceBetween // 아이템들 사이 간격 균등
            ) {
                WeekDayItem(day = "월", checked = true)
                WeekDayItem(day = "화", checked = true)
                WeekDayItem(day = "수", checked = true)
                WeekDayItem(day = "목", checked = true)
                WeekDayItem(day = "금", checked = true)
                WeekDayItem(day = "토", checked = false)
                WeekDayItem(day = "일", checked = false)
            }

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Card(
                modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
                shape = RoundedCornerShape(16.dp), // 카드 모서리를 둥글게 설정
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // 카드 배경 색상
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth() // 가로 최대 너비 사용
                        .padding(horizontal = 16.dp, vertical = 14.dp), // 내부 패딩
                    verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
                ) {
                    Text(
                        text = "🔥", // 아이콘
                        fontSize = 22.sp // 글자 크기
                    )

                    Spacer(modifier = Modifier.width(10.dp)) // 오른쪽 여백 추가

                    Column {
                        Text(
                            text = "5일 연속 기록 중", // 연속 기록 문구
                            fontSize = 15.sp, // 글자 크기
                            fontWeight = FontWeight.Bold, // 글자 두께
                            color = Color(0xFF1F2A37) // 글자 색상
                        )

                        Text(
                            text = "조금만 더 힘내면 주간 목표 달성이에요", // 하단 설명
                            fontSize = 13.sp, // 글자 크기
                            color = Color(0xFF6B7280) // 글자 색상
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayItem( // 요일 아이템을 정의하는 Composable 함수
    day: String, // 요일 텍스트
    checked: Boolean // 체크 여부
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally // 가운데 정렬
    ) {
        Box(
            modifier = Modifier
                .size(36.dp) // 동그라미 크기
                .background(
                    color = if (checked) Color(0xFF2F7DF6) else Color(0xFFE5EAF2), // 체크 여부에 따라 색상 변경
                    shape = RoundedCornerShape(100.dp) // 원형에 가까운 둥근 모서리
                ),
            contentAlignment = Alignment.Center // 가운데 정렬
        ) {
            Text(
                text = if (checked) "✓" else "", // 체크된 경우 체크 표시
                color = Color.White, // 글자 색상
                fontSize = 16.sp, // 글자 크기
                fontWeight = FontWeight.Bold // 글자 두께
            )
        }

        Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

        Text(
            text = day, // 요일 텍스트
            fontSize = 13.sp, // 글자 크기
            color = Color(0xFF5B6573) // 글자 색상
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseWriteCard(
    selectedDate: String, // 현재 선택된 날짜
    editingExpense: ExpenseItemData?, // 수정 중인 항목
    onSaveExpense: (ExpenseItemData) -> Unit, // 저장 버튼 클릭 시 호출할 콜백
    onCancelEdit: () -> Unit // 수정 취소 버튼 클릭 시 호출할 콜백
) { // 소비 기록하기 카드를 정의하는 Composable 함수
    val context = LocalContext.current // 현재 Context를 가져옴
    val calendar = Calendar.getInstance() // 현재 날짜 정보를 가져옴

    var formDate by remember { mutableStateOf(selectedDate) } // 폼의 날짜 상태
    var selectedCategory by remember { mutableStateOf("식비") } // 선택된 카테고리 상태
    var amount by remember { mutableStateOf("") } // 금액 입력 상태
    var memo by remember { mutableStateOf("") } // 메모 입력 상태
    var receiptImageName by remember { mutableStateOf("") } // 영수증 이미지 이름 상태
    var diary by remember { mutableStateOf("") } // 한줄 소비 일기 상태
    var expanded by remember { mutableStateOf(false) } // 드롭다운 열림 여부 상태
    val categoryList = listOf("식비", "교통", "쇼핑", "카페", "기타") // 카테고리 목록

    LaunchedEffect(editingExpense?.id, selectedDate) {
        if (editingExpense != null) {
            formDate = editingExpense.date // 수정 모드일 때 기존 날짜 반영
            selectedCategory = editingExpense.category // 수정 모드일 때 기존 카테고리 반영
            amount = editingExpense.amount.toString() // 수정 모드일 때 기존 금액 반영
            memo = editingExpense.memo // 수정 모드일 때 기존 메모 반영
            receiptImageName = editingExpense.receiptImageName // 수정 모드일 때 기존 영수증 반영
            diary = editingExpense.diary // 수정 모드일 때 기존 일기 반영
        } else {
            formDate = selectedDate // 새 기록 모드일 때 선택 날짜 반영
            selectedCategory = "식비" // 기본 카테고리 설정
            amount = "" // 금액 초기화
            memo = "" // 메모 초기화
            receiptImageName = "" // 영수증 초기화
            diary = "" // 일기 초기화
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
        shape = RoundedCornerShape(24.dp), // 카드 모서리를 둥글게 설정
        colors = CardDefaults.cardColors(
            containerColor = Color.White // 카드 배경 색상
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // 카드 내부 패딩
        ) {
            Text(
                text = if (editingExpense == null) "소비 기록하기" else "소비 기록 수정", // 카드 제목
                fontSize = 20.sp, // 글자 크기
                fontWeight = FontWeight.Bold, // 글자 두께
                color = Color(0xFF1F2A37) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            Text(
                text = if (editingExpense == null)
                    "날짜와 카테고리를 선택해서 소비를 기록해보세요"
                else
                    "선택한 소비 내역을 수정할 수 있어요", // 안내 문구
                fontSize = 14.sp, // 글자 크기
                color = Color(0xFF6B7280) // 글자 색상
            )

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Text(
                text = "날짜", // 날짜 라벨
                fontSize = 15.sp, // 글자 크기
                fontWeight = FontWeight.SemiBold, // 글자 두께
                color = Color(0xFF163D8F) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            OutlinedTextField(
                value = formDate,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
                trailingIcon = {
                    Text(
                        text = "📅",
                        fontSize = 18.sp
                    )
                },
                placeholder = {
                    Text(
                        text = "날짜를 선택하세요",
                        color = Color(0xFF9AA4B2)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7FAFC),
                    unfocusedContainerColor = Color(0xFFF7FAFC),
                    focusedBorderColor = Color(0xFFDCE7F3),
                    unfocusedBorderColor = Color(0xFFDCE7F3),
                    cursorColor = Color(0xFF2F7DF6)
                )
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            TextButton(
                onClick = {
                    val dateParts = formDate.split("-")
                    val initYear = dateParts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                    val initMonth = (dateParts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
                    val initDay = dateParts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val formattedMonth = String.format("%02d", month + 1)
                            val formattedDay = String.format("%02d", dayOfMonth)
                            formDate = "$year-$formattedMonth-$formattedDay"
                        },
                        initYear,
                        initMonth,
                        initDay
                    ).show()
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "날짜 선택하기",
                    color = Color(0xFF2F7DF6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Text(
                text = "금액", // 금액 라벨
                fontSize = 15.sp, // 글자 크기
                fontWeight = FontWeight.SemiBold, // 글자 두께
                color = Color(0xFF163D8F) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() } },
                modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
                placeholder = {
                    Text(
                        text = "예: 12000",
                        color = Color(0xFF9AA4B2)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7FAFC),
                    unfocusedContainerColor = Color(0xFFF7FAFC),
                    focusedBorderColor = Color(0xFFDCE7F3),
                    unfocusedBorderColor = Color(0xFFDCE7F3),
                    cursorColor = Color(0xFF2F7DF6)
                )
            )

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Text(
                text = "카테고리", // 카테고리 라벨
                fontSize = 15.sp, // 글자 크기
                fontWeight = FontWeight.SemiBold, // 글자 두께
                color = Color(0xFF163D8F) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "카테고리를 선택하세요",
                            color = Color(0xFF9AA4B2)
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF7FAFC),
                        unfocusedContainerColor = Color(0xFFF7FAFC),
                        focusedBorderColor = Color(0xFFDCE7F3),
                        unfocusedBorderColor = Color(0xFFDCE7F3),
                        cursorColor = Color(0xFF2F7DF6)
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categoryList.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(text = category)
                            },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Text(
                text = "메모", // 메모 라벨
                fontSize = 15.sp, // 글자 크기
                fontWeight = FontWeight.SemiBold, // 글자 두께
                color = Color(0xFF163D8F) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "무엇을 구매했나요?",
                        color = Color(0xFF9AA4B2)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7FAFC),
                    unfocusedContainerColor = Color(0xFFF7FAFC),
                    focusedBorderColor = Color(0xFFDCE7F3),
                    unfocusedBorderColor = Color(0xFFDCE7F3),
                    cursorColor = Color(0xFF2F7DF6)
                )
            )

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Text(
                text = "영수증 인증 (+20 SPT)", // 영수증 인증 라벨
                fontSize = 15.sp, // 글자 크기
                fontWeight = FontWeight.SemiBold, // 글자 두께
                color = Color(0xFF163D8F) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            OutlinedButton(
                onClick = {
                    receiptImageName = "receipt_sample.jpg" // 임시 업로드 상태 표시
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFDCE7F3)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = if (receiptImageName.isBlank()) "업로드" else receiptImageName,
                    color = Color(0xFF1F2A37),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp)) // 아래쪽 여백 추가

            Text(
                text = "한줄 소비 일기 (+15 SPT)", // 소비 일기 라벨
                fontSize = 15.sp, // 글자 크기
                fontWeight = FontWeight.SemiBold, // 글자 두께
                color = Color(0xFF163D8F) // 글자 색상
            )

            Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

            OutlinedTextField(
                value = diary,
                onValueChange = { diary = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp), // 높이 지정
                placeholder = {
                    Text(
                        text = "오늘 소비에 대한 생각을 기록해보세요",
                        color = Color(0xFF9AA4B2)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7FAFC),
                    unfocusedContainerColor = Color(0xFFF7FAFC),
                    focusedBorderColor = Color(0xFFDCE7F3),
                    unfocusedBorderColor = Color(0xFFDCE7F3),
                    cursorColor = Color(0xFF2F7DF6)
                )
            )

            Spacer(modifier = Modifier.height(24.dp)) // 아래쪽 여백 추가

            if (editingExpense != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCancelEdit() },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFDCE7F3)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Text(
                            text = "취소",
                            color = Color(0xFF5B6573),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val amountInt = amount.toIntOrNull() ?: 0 // 입력한 금액을 숫자로 변환

                            if (amountInt > 0) {
                                val updatedExpense = ExpenseItemData(
                                    id = editingExpense.id,
                                    date = formDate,
                                    title = createExpenseTitle(selectedCategory, memo),
                                    category = selectedCategory,
                                    amount = amountInt,
                                    memo = memo,
                                    receiptImageName = receiptImageName,
                                    diary = diary
                                ) // 수정된 소비 항목 생성

                                onSaveExpense(updatedExpense) // 상위 HomeScreen으로 수정 데이터 전달
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp), // 버튼 높이
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF16B8D9),
                                            Color(0xFF2F7DF6)
                                        )
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "수정 완료",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        val amountInt = amount.toIntOrNull() ?: 0 // 입력한 금액을 숫자로 변환

                        if (amountInt > 0) {
                            val newExpense = ExpenseItemData(
                                id = System.currentTimeMillis(),
                                date = formDate,
                                title = createExpenseTitle(selectedCategory, memo),
                                category = selectedCategory,
                                amount = amountInt,
                                memo = memo,
                                receiptImageName = receiptImageName,
                                diary = diary
                            ) // 새 소비 항목 생성

                            onSaveExpense(newExpense) // 상위 HomeScreen으로 데이터 전달
                        }
                    }, // 기록 완료 버튼 클릭 이벤트
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp), // 버튼 높이
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF16B8D9),
                                        Color(0xFF2F7DF6)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "기록 완료",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip( // 카테고리 선택 칩을 정의하는 Composable 함수
    text: String, // 칩 텍스트
    selected: Boolean, // 선택 여부
    modifier: Modifier = Modifier // 수정자
) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) Color(0xFFDCEBFF) else Color(0xFFF7FAFC), // 선택 여부에 따른 배경색
                shape = RoundedCornerShape(12.dp) // 둥근 모서리
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF7CB3FF) else Color(0xFFDCE7F3), // 선택 여부에 따른 테두리색
                shape = RoundedCornerShape(12.dp) // 둥근 모서리
            )
            .padding(vertical = 12.dp), // 내부 패딩
        contentAlignment = Alignment.Center // 가운데 정렬
    ) {
        Text(
            text = text, // 칩 텍스트
            color = if (selected) Color(0xFF2F7DF6) else Color(0xFF5B6573), // 선택 여부에 따른 글자색
            fontSize = 14.sp, // 글자 크기
            fontWeight = FontWeight.Medium // 글자 두께
        )
    }
}

@Composable
private fun RewardGuideCard() { // 보상 안내 카드를 정의하는 Composable 함수
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드의 너비를 최대 너비로 설정
        shape = RoundedCornerShape(20.dp), // 카드 모서리를 둥글게 설정
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF4EEDB) // 카드 배경 색상을 연한 베이지 톤으로 설정
        )
    ) {
        Box( // 카드 내부를 겹쳐 배치하기 위한 Box
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp) // 카드 내부 패딩
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
                    horizontalArrangement = Arrangement.SpaceBetween, // 양쪽 정렬
                    verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
                    ) {
                        Text(
                            text = "🎁", // 제목 왼쪽 아이콘
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(6.dp)) // 오른쪽 여백 추가

                        Text(
                            text = "보상 안내", // 카드 제목
                            fontSize = 15.sp, // 글자 크기
                            fontWeight = FontWeight.Bold, // 글자 두께
                            color = Color(0xFF222222) // 글자 색상
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp)) // 아래쪽 여백 추가

                RewardPointRow(
                    title = "기본 기록",
                    point = "+10 SPT"
                ) // 첫 번째 보상 항목

                Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

                RewardPointRow(
                    title = "영수증 인증",
                    point = "+20 SPT"
                ) // 두 번째 보상 항목

                Spacer(modifier = Modifier.height(8.dp)) // 아래쪽 여백 추가

                RewardPointRow(
                    title = "일기 작성",
                    point = "+15 SPT"
                ) // 세 번째 보상 항목

                Spacer(modifier = Modifier.height(14.dp)) // 아래쪽 여백 추가

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFF0C244), // 안내 박스 테두리 색상
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 10.dp), // 안내 박스 내부 패딩
                    contentAlignment = Alignment.Center // 가운데 정렬
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally // 가로 가운데 정렬
                    ) {
                        Text(
                            text = "주간 성실도 90점 이상 시", // 첫 줄 안내 문구
                            fontSize = 12.sp, // 글자 크기
                            fontWeight = FontWeight.Bold, // 글자 두께
                            color = Color(0xFF8A4B00) // 글자 색상
                        )

                        Text(
                            text = "랜덤 아바타 + 보너스 SPT!", // 두 번째 줄 안내 문구
                            fontSize = 11.sp, // 글자 크기
                            color = Color(0xFFB06A00) // 글자 색상
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd) // 우하단 정렬
                    .padding(end = 10.dp, bottom = 10.dp) // 바깥 여백
                    .size(52.dp) // 원형 버튼 크기
                    .background(
                        color = Color(0xFF2196F3), // 파란색 버튼 배경
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center // 가운데 정렬
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally // 가운데 정렬
                ) {
                    Text(
                        text = "Q", // 버튼 내부 아이콘 대체 텍스트
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "소비백과", // 버튼 텍스트
                        fontSize = 8.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardPointRow(
    title: String, // 보상 항목 제목
    point: String // 보상 포인트 텍스트
) {
    Row(
        modifier = Modifier.fillMaxWidth(), // 가로 최대 너비 사용
        horizontalArrangement = Arrangement.SpaceBetween, // 양쪽 정렬
        verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
    ) {
        Text(
            text = title, // 왼쪽 항목명
            fontSize = 13.sp, // 글자 크기
            color = Color(0xFF444444) // 글자 색상
        )

        Text(
            text = point, // 오른쪽 포인트
            fontSize = 13.sp, // 글자 크기
            fontWeight = FontWeight.Bold, // 글자 두께
            color = Color(0xFFE67E22) // 주황색 포인트 텍스트
        )
    }
}

private fun getCategoryEmoji(category: String): String { // 카테고리에 맞는 이모지를 반환하는 함수
    return when (category) {
        "식비" -> "🍔"
        "교통" -> "🚕"
        "쇼핑" -> "🛍️"
        "카페" -> "☕"
        else -> "💸"
    }
}

private fun getCategoryColors(category: String): List<Color> { // 카테고리에 맞는 색상 목록을 반환하는 함수
    return when (category) {
        "식비" -> listOf(Color(0xFFFF8A00), Color(0xFFFF5C00))
        "교통" -> listOf(Color(0xFF4C8DFF), Color(0xFF2F6BFF))
        "쇼핑" -> listOf(Color(0xFFFF6BAA), Color(0xFFFF4D8D))
        "카페" -> listOf(Color(0xFF9C6BFF), Color(0xFF7A4DFF))
        else -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
    }
}

private fun createExpenseTitle(category: String, memo: String): String { // 카테고리와 메모를 바탕으로 제목을 생성하는 함수
    return if (memo.isNotBlank()) {
        memo
    } else {
        when (category) {
            "식비" -> "식사"
            "교통" -> "이동"
            "쇼핑" -> "쇼핑"
            "카페" -> "카페"
            else -> "기타 지출"
        }
    }
}

private fun formatAmount(amount: Int): String { // 금액을 세 자리마다 콤마 형식으로 변환하는 함수
    val formatter = DecimalFormat("#,###")
    return formatter.format(amount)
}

private fun formatDisplayDate(date: String): String { // yyyy-MM-dd 형식의 날짜를 M월 d일 소비 내역 형식으로 변환하는 함수
    return try {
        val yearMonthDay = date.split("-")
        val month = yearMonthDay[1].toInt()
        val day = yearMonthDay[2].toInt()
        "${month}월 ${day}일 소비 내역"
    } catch (e: Exception) {
        "소비 내역"
    }
}

private fun moveMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> { // 이전달/다음달 이동을 계산하는 함수
    var newYear = year
    var newMonth = month + delta

    while (newMonth < 1) {
        newMonth += 12
        newYear -= 1
    }

    while (newMonth > 12) {
        newMonth -= 12
        newYear += 1
    }

    return Pair(newYear, newMonth)
}

private fun generateCalendarDates(year: Int, month: Int): List<CalendarDateData> { // 달력에 표시할 날짜 데이터를 생성하는 함수
    val result = mutableListOf<CalendarDateData>() // 최종 날짜 목록

    val currentCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    } // 현재 달의 첫째 날로 설정

    val firstDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK) // 현재 달 1일의 요일 정보
    val daysInCurrentMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) // 현재 달의 마지막 날짜
    val leadingDays = firstDayOfWeek - 1 // 첫 주 앞쪽에 채울 이전 달 날짜 개수

    val previousCalendar = currentCalendar.clone() as Calendar
    previousCalendar.add(Calendar.MONTH, -1) // 이전 달로 이동
    val previousYear = previousCalendar.get(Calendar.YEAR) // 이전 달 연도
    val previousMonth = previousCalendar.get(Calendar.MONTH) + 1 // 이전 달 월
    val daysInPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) // 이전 달 마지막 날짜

    for (day in (daysInPreviousMonth - leadingDays + 1)..daysInPreviousMonth) {
        result.add(
            CalendarDateData(
                fullDate = formatDate(previousYear, previousMonth, day),
                dayText = day.toString(),
                isCurrentMonth = false
            )
        )
    } // 앞쪽 이전 달 날짜 추가

    for (day in 1..daysInCurrentMonth) {
        result.add(
            CalendarDateData(
                fullDate = formatDate(year, month, day),
                dayText = day.toString(),
                isCurrentMonth = true
            )
        )
    } // 현재 달 날짜 추가

    val remain = result.size % 7 // 마지막 줄에서 비어 있는 칸 수 계산
    val trailingDays = if (remain == 0) 0 else 7 - remain // 다음 달에서 채워야 할 날짜 수

    val nextCalendar = currentCalendar.clone() as Calendar
    nextCalendar.add(Calendar.MONTH, 1) // 다음 달로 이동
    val nextYear = nextCalendar.get(Calendar.YEAR) // 다음 달 연도
    val nextMonth = nextCalendar.get(Calendar.MONTH) + 1 // 다음 달 월

    for (day in 1..trailingDays) {
        result.add(
            CalendarDateData(
                fullDate = formatDate(nextYear, nextMonth, day),
                dayText = day.toString(),
                isCurrentMonth = false
            )
        )
    } // 마지막 줄을 맞추기 위한 다음 달 날짜 추가

    return result
}

private fun formatDate(year: Int, month: Int, day: Int): String { // 연/월/일을 yyyy-MM-dd 문자열로 변환하는 함수
    return String.format("%04d-%02d-%02d", year, month, day)
}

private fun createChangeRateText(currentAmount: Int, previousAmount: Int): String { // 지난달 대비 증감 텍스트를 생성하는 함수
    return if (previousAmount == 0) {
        when {
            currentAmount == 0 -> "지난달과 동일 0%"
            else -> "↗ 지난달 대비 신규 소비"
        }
    } else {
        val rate = (((currentAmount - previousAmount).toFloat() / previousAmount.toFloat()) * 100).toInt()
        when {
            rate > 0 -> "↗ 지난달 대비 +${rate}%"
            rate < 0 -> "↘ 지난달 대비 -${abs(rate)}%"
            else -> "→ 지난달 대비 0%"
        }
    }
}

private fun getChangeRateColor(changeRateText: String): Color { // 지난달 대비 변화 문구에 따른 색상을 반환하는 함수
    return when {
        changeRateText.contains("↗") -> Color(0xFFE53935)
        changeRateText.contains("↘") -> Color(0xFF16A34A)
        else -> Color(0xFF6B7280)
    }
}