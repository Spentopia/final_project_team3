package com.ict.spentopia.feature.home

// 날짜 선택 다이얼로그를 위한 import입니다.
import android.app.DatePickerDialog

// Compose foundation 관련 import입니다.
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

// 아이콘 관련 import입니다.
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings

// Material3 관련 import입니다.
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

// Compose runtime 관련 import입니다.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// UI 관련 import입니다.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ViewModel을 Compose에서 사용하기 위한 import입니다.
import androidx.lifecycle.viewmodel.compose.viewModel

// Flow를 Compose 상태로 안전하게 수집하기 위한 import입니다.
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// BudgetViewModel import입니다.
import com.ict.spentopia.feature.budget.BudgetViewModel

// Room Entity import입니다.
import com.ict.spentopia.data.local.ExpenseEntity

// 숫자 포맷 및 날짜 계산 관련 import입니다.
import java.text.DecimalFormat
import java.util.Calendar
import kotlin.math.abs

// --------------------------------------------------
// UI에서 사용할 소비 항목 데이터 클래스입니다.
// 기존 HomeScreen UI 구조를 최대한 유지하기 위해 남겨둡니다.
// 즉, DB의 ExpenseEntity를 바로 화면에 쓰지 않고,
// 화면에서 쓰기 좋은 ExpenseItemData로 변환해서 사용합니다.
// --------------------------------------------------
data class ExpenseItemData(
    val id: Long, // 항목 고유 ID
    val date: String, // 소비 날짜
    val title: String, // 소비 제목
    val category: String, // 소비 카테고리
    val amount: Int, // 소비 금액
    val memo: String, // 구매 메모
    val receiptImageName: String, // 영수증 이미지 이름
    val diary: String // 한줄 소비 일기
)

// 달력에서 사용할 날짜 데이터 클래스입니다.
data class CalendarDateData(
    val fullDate: String, // 실제 날짜 값 yyyy-MM-dd
    val dayText: String, // 화면에 보여줄 날짜 텍스트
    val isCurrentMonth: Boolean // 현재 달 포함 여부
)

@Composable
fun HomeScreen(
    onLedgerClick: () -> Unit = {},
    onMyPageClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {},
    onAnalysisClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onMarketClick: () -> Unit = {},
    onPlazaClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {}
) {
    // 예산 설정 화면에서 저장한 값을 읽어오기 위한 ViewModel입니다.
    val budgetViewModel: BudgetViewModel = viewModel()

    // 홈 화면에서 Room 소비 데이터를 가져오기 위한 ViewModel입니다.
    val context = LocalContext.current

    val homeViewModel: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val database = com.ict.spentopia.data.local.ExpenseDatabase.getDatabase(context)
                val repository = com.ict.spentopia.data.repository.ExpenseRepository(
                    database.expenseDao()
                )
                return HomeViewModel(repository) as T
            }
        }
    )

    // 저장된 예산 상태를 lifecycle-aware 방식으로 구독합니다.
    val budgetState by budgetViewModel.budgetState.collectAsStateWithLifecycle()

    // HomeViewModel의 전체 UI용 소비 목록을 구독합니다.
    // DailyExpenseCard는 "선택한 날짜" 기준으로 여기서 다시 필터링합니다.
    val expenseList by homeViewModel.expenseUiList.collectAsStateWithLifecycle()

    // HomeViewModel에서 현재 선택한 연-월을 구독합니다.
    // 예: "2026-04"
    val selectedYearMonth by homeViewModel.selectedYearMonth.collectAsStateWithLifecycle()

    // HomeViewModel에서 현재 선택한 날짜를 구독합니다.
    // 예: "2026-04-15"
    val selectedDate by homeViewModel.selectedDate.collectAsStateWithLifecycle()

    // HomeViewModel에서 현재 선택한 월의 소비 목록(UI용)을 구독합니다.
    val monthlyExpenseList by homeViewModel.monthlyExpenseUiList.collectAsStateWithLifecycle()

    // HomeViewModel에서 현재 선택한 월의 총 소비 금액을 구독합니다.
    val currentMonthTotalExpense by homeViewModel.monthlyTotalAmount.collectAsStateWithLifecycle()

    // HomeViewModel에서 현재 선택한 월의 소비 건수를 구독합니다.
    val currentMonthExpenseCount by homeViewModel.monthlyExpenseCount.collectAsStateWithLifecycle()

    // HomeViewModel에서 이전 달 총 소비 금액을 구독합니다.
    val previousMonthTotalExpense by homeViewModel.previousMonthTotalAmount.collectAsStateWithLifecycle()

    // 소비 기록이 존재하는 날짜 목록을 Set으로 관리합니다.
    val expenseDateSet by homeViewModel.expenseDateSet.collectAsStateWithLifecycle()

    // 현재 선택된 연-월 문자열에서 연도와 월을 분리합니다.
    // selectedYearMonth는 "yyyy-MM" 형식이므로 substring으로 안전하게 꺼냅니다.
    val currentYear = selectedYearMonth.substring(0, 4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = selectedYearMonth.substring(5, 7).toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)

    // 수정 중인 소비 항목 상태입니다.
    var editingExpense by remember { mutableStateOf<ExpenseItemData?>(null) }

    // 예산 설정 화면에서 저장한 값으로 월 예산을 계산합니다.
    // 현재 프로젝트 구조에서는
    // "월 수입 - 저축 목표"를 실제 사용 가능한 한 달 예산으로 사용하고 있습니다.
    val monthlyBudget = remember(budgetState) {
        budgetState.monthlyIncome - budgetState.savingGoal
    }

    // 오늘 날짜를 yyyy-MM-dd 형식으로 생성합니다.
    val today = remember {
        val cal = Calendar.getInstance()
        formatDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // 현재 달의 남은 예산 계산입니다.
    // 예산보다 소비가 많으면 음수가 될 수 있습니다.
    val remainingBudget = monthlyBudget - currentMonthTotalExpense

    // 사용률 텍스트 계산입니다.
    // 기존에는 Int로 바로 잘라서 0.2% 같은 값이 0%가 되는 문제가 있었기 때문에
    // 이제는 문자열로 만들어서 0.3% 같은 값도 보이게 처리합니다.
    val usageRateText = remember(currentMonthTotalExpense, monthlyBudget) {
        createUsageRateText(
            currentAmount = currentMonthTotalExpense,
            monthlyBudget = monthlyBudget
        )
    }

    // 지난달 대비 증감 문구 계산입니다.
    val changeRateText = remember(currentMonthTotalExpense, previousMonthTotalExpense) {
        createChangeRateText(
            currentAmount = currentMonthTotalExpense,
            previousAmount = previousMonthTotalExpense
        )
    }

    // 드로어는 AppNavGraph에서 처리하므로 HomeScreen에서는 본문만 그립니다.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FA))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            TopHeaderSection()
        }

        item {
            MonthlySummaryCard(
                currentYear = currentYear,
                currentMonth = currentMonth,
                currentMonthTotalExpense = currentMonthTotalExpense,
                currentMonthExpenseCount = currentMonthExpenseCount,
                monthlyBudget = monthlyBudget,
                remainingBudget = remainingBudget,
                usageRateText = usageRateText,
                changeRateText = changeRateText,
                onPrevMonth = {
                    // 이전 달로 이동합니다.
                    // 선택 날짜도 자동으로 해당 월 1일로 맞춰집니다.
                    homeViewModel.moveToPreviousMonth()
                },
                onNextMonth = {
                    // 다음 달로 이동합니다.
                    // 선택 날짜도 자동으로 해당 월 1일로 맞춰집니다.
                    homeViewModel.moveToNextMonth()
                }
            )
        }

        // 현재는 달력 UI를 주석 처리한 상태지만,
        // 나중에 다시 활성화할 때도 ViewModel의 selectDate()를 그대로 쓰면 됩니다.
        //
        // item {
        //     CalendarCard(
        //         currentYear = currentYear,
        //         currentMonth = currentMonth,
        //         selectedDate = selectedDate,
        //         expenseDateSet = expenseDateSet,
        //         today = today,
        //         onPrevMonth = {
        //             homeViewModel.moveToPreviousMonth()
        //         },
        //         onNextMonth = {
        //             homeViewModel.moveToNextMonth()
        //         },
        //         onDateSelected = { clickedDate ->
        //             homeViewModel.selectDate(clickedDate)
        //         }
        //     )
        // }

        item {
            DailyExpenseCard(
                expenseList = expenseList,
                selectedDate = selectedDate,
                onEditExpense = { expense ->
                    // 수정 버튼을 누르면 수정 모드로 바꾸고,
                    // 선택 날짜도 해당 항목 날짜로 맞춰줍니다.
                    editingExpense = expense
                    homeViewModel.selectDate(expense.date)
                },
                onDeleteExpense = { expenseId ->
                    // 실제 Room DB에서 삭제합니다.
                    homeViewModel.deleteExpenseById(expenseId)

                    // 삭제한 항목이 수정 중이던 항목이면 수정 상태도 비웁니다.
                    if (editingExpense?.id == expenseId) {
                        editingExpense = null
                    }
                }
            )
        }

        item {
            WeeklyScoreCard()
        }

        item {
            ExpenseWriteCard(
                selectedDate = selectedDate,
                editingExpense = editingExpense,
                onSaveExpense = { savedExpense ->
                    // 화면용 모델을 DB용 Entity로 변환합니다.
                    val entity = savedExpense.toEntity()

                    // 수정 중이 아니면 insert, 수정 중이면 update를 호출합니다.
                    if (editingExpense == null) {
                        homeViewModel.insertExpense(entity)
                    } else {
                        homeViewModel.updateExpense(entity)
                    }

                    // 저장 후 수정 상태를 초기화합니다.
                    editingExpense = null
                },
                onCancelEdit = {
                    editingExpense = null
                }
            )
        }

        item {
            RewardGuideCard()
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun TopHeaderSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "👋 오늘도 알뜰한 소비 하세요",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2A37)
                )
                Text(
                    text = "이번 달 소비 흐름을 한눈에 확인해보세요",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Row {
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "settings")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = "notification")
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    currentYear: Int,
    currentMonth: Int,
    currentMonthTotalExpense: Int,
    currentMonthExpenseCount: Int,
    monthlyBudget: Int,
    remainingBudget: Int,
    usageRateText: String,
    changeRateText: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6FA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalendarArrowButton(
                            text = "‹",
                            onClick = onPrevMonth
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "${currentYear}년 ${currentMonth}월",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2A37)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        CalendarArrowButton(
                            text = "›",
                            onClick = onNextMonth
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "이번 달 소비 내역 · ${currentMonthExpenseCount}건",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatAmount(currentMonthTotalExpense)}원",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2A37)
                    )
                    Text(
                        text = changeRateText,
                        fontSize = 13.sp,
                        color = getChangeRateColor(changeRateText)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMiniCard(
                    title = "예산",
                    value = "${formatAmount(monthlyBudget)}원",
                    bgColor = Color(0xFFDDF3F7),
                    modifier = Modifier.weight(1f)
                )

                SummaryMiniCard(
                    title = if (remainingBudget >= 0) "남은 예산" else "초과 예산",
                    value = "${formatAmount(abs(remainingBudget))}원",
                    bgColor = if (remainingBudget >= 0) Color(0xFFE1EAFF) else Color(0xFFFFE3E3),
                    modifier = Modifier.weight(1f)
                )

                SummaryMiniCard(
                    title = "사용률",
                    value = usageRateText,
                    bgColor = Color(0xFFDFF2EC),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(
    title: String,
    value: String,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF315072)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22406A)
            )
        }
    }
}

@Composable
private fun CalendarCard(
    currentYear: Int,
    currentMonth: Int,
    selectedDate: String,
    expenseDateSet: Set<String>,
    today: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
    val calendarDates = generateCalendarDates(currentYear, currentMonth)
    val rows = calendarDates.chunked(7)
    val cellWidth = 40.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarArrowButton(
                    text = "‹",
                    onClick = onPrevMonth
                )

                Text(
                    text = "${currentMonth}월 ${currentYear}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2A37)
                )

                CalendarArrowButton(
                    text = "›",
                    onClick = onNextMonth
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                daysOfWeek.forEach { day ->
                    Box(
                        modifier = Modifier.width(cellWidth),
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

            Spacer(modifier = Modifier.height(14.dp))

            rows.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    week.forEach { dateItem ->
                        val isSelected = dateItem.fullDate == selectedDate
                        val hasExpense = expenseDateSet.contains(dateItem.fullDate)
                        val isToday = dateItem.fullDate == today

                        Box(
                            modifier = Modifier
                                .width(cellWidth)
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

                                Spacer(modifier = Modifier.height(3.dp))

                                if (hasExpense) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                color = if (isSelected) Color.White else Color(0xFF2F7DF6),
                                                shape = CircleShape
                                            )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
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
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF6B7280),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DailyExpenseCard(
    expenseList: List<ExpenseItemData>,
    selectedDate: String,
    onEditExpense: (ExpenseItemData) -> Unit,
    onDeleteExpense: (Long) -> Unit
) {
    val filteredList = expenseList.filter { it.date == selectedDate }
    val totalAmount = filteredList.sumOf { it.amount }
    val diaryText = filteredList.firstOrNull { it.diary.isNotBlank() }?.diary ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = formatDisplayDate(selectedDate),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2A37)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "총 ${formatAmount(totalAmount)}원 · ${filteredList.size}건",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (filteredList.isEmpty()) {
                Text(
                    text = "이 날짜에는 아직 저장된 소비 내역이 없어요",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
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
                            onEditExpense(item)
                        },
                        onDeleteClick = {
                            onDeleteExpense(item.id)
                        }
                    )

                    if (index != filteredList.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (diaryText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF7FBFE)
                        ),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF86D4FF)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "오늘의 소비 일기",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22406A)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = diaryText,
                                fontSize = 14.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseItemCard(
    emoji: String,
    title: String,
    category: String,
    amount: String,
    tag: String?,
    iconColors: List<Color>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD)),
        border = BorderStroke(
            1.dp,
            Color(0xFFE5E7EB)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.horizontalGradient(iconColors),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2A37)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category,
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )

                        if (tag != null) {
                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFEFFCF3),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = amount,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2A37)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
private fun WeeklyScoreCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FBFF)
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFFDCEBFF)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "이번 주 성실도",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2A37)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "소비 기록을 꾸준히 남긴 정도를 보여줘요",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "85%",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2F7DF6)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "아주 잘하고 있어요!",
                        fontSize = 14.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFBFE0FF),
                                    Color(0xFF2F7DF6)
                                )
                            ),
                            shape = RoundedCornerShape(100.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌟",
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "주간 기록",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22406A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeekDayItem(day = "월", checked = true)
                WeekDayItem(day = "화", checked = true)
                WeekDayItem(day = "수", checked = true)
                WeekDayItem(day = "목", checked = true)
                WeekDayItem(day = "금", checked = true)
                WeekDayItem(day = "토", checked = false)
                WeekDayItem(day = "일", checked = false)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 22.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "5일 연속 기록 중",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2A37)
                        )

                        Text(
                            text = "조금만 더 힘내면 주간 목표 달성이에요",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayItem(
    day: String,
    checked: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (checked) Color(0xFF2F7DF6) else Color(0xFFE5EAF2),
                    shape = RoundedCornerShape(100.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (checked) "✓" else "",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = day,
            fontSize = 13.sp,
            color = Color(0xFF5B6573)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseWriteCard(
    selectedDate: String,
    editingExpense: ExpenseItemData?,
    onSaveExpense: (ExpenseItemData) -> Unit,
    onCancelEdit: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var formDate by remember { mutableStateOf(selectedDate) }
    var selectedCategory by remember { mutableStateOf("식비") }
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var receiptImageName by remember { mutableStateOf("") }
    var diary by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val categoryList = listOf("식비", "교통", "쇼핑", "카페", "기타")

    LaunchedEffect(editingExpense?.id, selectedDate) {
        if (editingExpense != null) {
            formDate = editingExpense.date
            selectedCategory = editingExpense.category
            amount = editingExpense.amount.toString()
            memo = editingExpense.memo
            receiptImageName = editingExpense.receiptImageName
            diary = editingExpense.diary
        } else {
            formDate = selectedDate
            selectedCategory = "식비"
            amount = ""
            memo = ""
            receiptImageName = ""
            diary = ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = if (editingExpense == null) "소비 기록하기" else "소비 기록 수정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2A37)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (editingExpense == null)
                    "날짜와 카테고리를 선택해서 소비를 기록해보세요"
                else
                    "선택한 소비 내역을 수정할 수 있어요",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "날짜",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF163D8F)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = formDate,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "금액",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF163D8F)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "카테고리",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF163D8F)
            )

            Spacer(modifier = Modifier.height(8.dp))

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

                DropdownMenu(
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "메모",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF163D8F)
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "영수증 인증 ",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF163D8F)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    receiptImageName = "receipt_sample.jpg"
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "한줄 소비 일기 ",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF163D8F)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = diary,
                onValueChange = { diary = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
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

            Spacer(modifier = Modifier.height(24.dp))

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
                            val amountInt = amount.toIntOrNull() ?: 0

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
                                )

                                onSaveExpense(updatedExpense)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
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
                        val amountInt = amount.toIntOrNull() ?: 0

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
                            )

                            onSaveExpense(newExpense)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
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
private fun RewardGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF4EEDB)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁",
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "보상 안내",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                RewardPointRow(
                    title = "기본 기록",
                    point = "+10 SPT"
                )

                Spacer(modifier = Modifier.height(8.dp))

                RewardPointRow(
                    title = "영수증 인증",
                    point = "+20 SPT"
                )

                Spacer(modifier = Modifier.height(8.dp))

                RewardPointRow(
                    title = "일기 작성",
                    point = "+15 SPT"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFF0C244),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "주간 성실도 90점 이상 시",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A4B00)
                        )

                        Text(
                            text = "랜덤 아바타 + 보너스 SPT!",
                            fontSize = 11.sp,
                            color = Color(0xFFB06A00)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .size(52.dp)
                    .background(
                        color = Color(0xFF2196F3),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Q",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "소비백과",
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
    title: String,
    point: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color(0xFF444444)
        )

        Text(
            text = point,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE67E22)
        )
    }
}

// --------------------------------------------------
// 아래 2개 변환 함수가 이번 Room 연결의 핵심입니다.
// --------------------------------------------------

// DB용 Entity를 화면용 UI 모델로 변환합니다.
private fun ExpenseEntity.toUiModel(): ExpenseItemData {
    return ExpenseItemData(
        id = id,
        date = date,
        title = title,
        category = category,
        amount = amount,
        memo = memo,
        receiptImageName = "",
        diary = diary
    )
}

private fun ExpenseItemData.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        date = date,
        title = title,
        category = category,
        amount = amount,
        memo = memo,
        diary = diary
    )
}

private fun getCategoryEmoji(category: String): String {
    return when (category) {
        "식비" -> "🍔"
        "교통" -> "🚕"
        "쇼핑" -> "🛍️"
        "카페" -> "☕"
        else -> "💸"
    }
}

private fun getCategoryColors(category: String): List<Color> {
    return when (category) {
        "식비" -> listOf(Color(0xFFFF8A00), Color(0xFFFF5C00))
        "교통" -> listOf(Color(0xFF4C8DFF), Color(0xFF2F6BFF))
        "쇼핑" -> listOf(Color(0xFFFF6BAA), Color(0xFFFF4D8D))
        "카페" -> listOf(Color(0xFF9C6BFF), Color(0xFF7A4DFF))
        else -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
    }
}

private fun createExpenseTitle(category: String, memo: String): String {
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

private fun formatAmount(amount: Int): String {
    val formatter = DecimalFormat("#,###")
    return formatter.format(amount)
}

private fun formatDisplayDate(date: String): String {
    return try {
        val yearMonthDay = date.split("-")
        val month = yearMonthDay[1].toInt()
        val day = yearMonthDay[2].toInt()
        "${month}월 ${day}일 소비 내역"
    } catch (e: Exception) {
        "소비 내역"
    }
}

private fun moveMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
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

private fun generateCalendarDates(year: Int, month: Int): List<CalendarDateData> {
    val result = mutableListOf<CalendarDateData>()

    val currentCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val firstDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK)
    val daysInCurrentMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingDays = firstDayOfWeek - 1

    val previousCalendar = currentCalendar.clone() as Calendar
    previousCalendar.add(Calendar.MONTH, -1)
    val previousYear = previousCalendar.get(Calendar.YEAR)
    val previousMonth = previousCalendar.get(Calendar.MONTH) + 1
    val daysInPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (day in (daysInPreviousMonth - leadingDays + 1)..daysInPreviousMonth) {
        result.add(
            CalendarDateData(
                fullDate = formatDate(previousYear, previousMonth, day),
                dayText = day.toString(),
                isCurrentMonth = false
            )
        )
    }

    for (day in 1..daysInCurrentMonth) {
        result.add(
            CalendarDateData(
                fullDate = formatDate(year, month, day),
                dayText = day.toString(),
                isCurrentMonth = true
            )
        )
    }

    val remain = result.size % 7
    val trailingDays = if (remain == 0) 0 else 7 - remain

    val nextCalendar = currentCalendar.clone() as Calendar
    nextCalendar.add(Calendar.MONTH, 1)
    val nextYear = nextCalendar.get(Calendar.YEAR)
    val nextMonth = nextCalendar.get(Calendar.MONTH) + 1

    for (day in 1..trailingDays) {
        result.add(
            CalendarDateData(
                fullDate = formatDate(nextYear, nextMonth, day),
                dayText = day.toString(),
                isCurrentMonth = false
            )
        )
    }

    return result
}

private fun formatDate(year: Int, month: Int, day: Int): String {
    return String.format("%04d-%02d-%02d", year, month, day)
}

private fun createChangeRateText(currentAmount: Int, previousAmount: Int): String {
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

private fun getChangeRateColor(changeRateText: String): Color {
    return when {
        changeRateText.contains("↗") -> Color(0xFFE53935)
        changeRateText.contains("↘") -> Color(0xFF16A34A)
        else -> Color(0xFF6B7280)
    }
}

// 사용률 텍스트를 만드는 함수입니다.
// 기존에는 Int로 바로 잘라서 0.2% 같은 값이 0%가 되었기 때문에
// 이제는 소수 1자리까지 보여주도록 문자열로 만듭니다.
private fun createUsageRateText(currentAmount: Int, monthlyBudget: Int): String {
    // 예산이 0 이하이면 나눗셈이 불가능하므로 0%로 처리합니다.
    if (monthlyBudget <= 0) {
        return "0%"
    }

    // 사용률을 Double로 계산합니다.
    val rate = (currentAmount.toDouble() / monthlyBudget.toDouble()) * 100.0

    // 정수로 딱 떨어지면 소수점 없이 보여줍니다.
    return if (rate % 1.0 == 0.0) {
        "${rate.toInt()}%"
    } else {
        String.format("%.1f%%", rate)
    }
}