package com.ict.spentopia.feature.budget

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple
import java.util.Calendar

// 예산 설정 화면임
// AI 추천 플랜/직접 조절/저장 흐름
@Composable
fun BudgetScreen(
    // ViewModel을 연결해서 화면 상태를 가져옴
    viewModel: BudgetViewModel = viewModel()
) {
    // 세로 스크롤 상태 저장
    val scrollState = rememberScrollState()

    // 스낵바 상태 저장
    val snackbarHostState = remember { SnackbarHostState() }

    // ViewModel의 budgetState를 화면에서 안전하게 구독
    val budgetState by viewModel.budgetState.collectAsStateWithLifecycle()

    // 저장 성공 여부 상태를 화면에서 안전하게 구독
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    val saveError by viewModel.saveError.collectAsStateWithLifecycle()

    val aiPlanList by viewModel.aiPlanList.collectAsStateWithLifecycle()

    val isAiPlanLoading by viewModel.isAiPlanLoading.collectAsStateWithLifecycle()

    val aiPlanError by viewModel.aiPlanError.collectAsStateWithLifecycle()

    val currentCalendar = remember { Calendar.getInstance() }
    val currentYear = remember { currentCalendar.get(Calendar.YEAR) }
    var selectedMonth by remember { mutableIntStateOf(currentCalendar.get(Calendar.MONTH) + 1) }
    var isMonthDialogOpen by remember { mutableStateOf(false) }

    // 총 지출 예정 금액 계산
    val totalExpense = budgetState.foodBudget +
            budgetState.transportBudget +
            budgetState.livingBudget +
            budgetState.hobbyBudget

    // 남는 금액 계산
    // 월 수입 - 총 지출 - 저축 목표
    val remainingAmount = budgetState.monthlyIncome - totalExpense - budgetState.savingGoal

    // 저장 성공하면 스낵바 메시지 띄우기
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("예산 설정이 저장되었어요.")
            viewModel.resetSaveSuccess()
        }
    }

    LaunchedEffect(saveError) {
        if (saveError.isNotBlank()) {
            snackbarHostState.showSnackbar(saveError)
            viewModel.resetSaveError()
        }
    }

    // 화면 전체 배경
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // 상단 제목 영역
                BudgetTopSection()

                Spacer(modifier = Modifier.height(20.dp))

                MonthSelectorCard(
                    year = currentYear,
                    month = selectedMonth,
                    onOpenClick = {
                        isMonthDialogOpen = true
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // AI 추천 플랜 제목 + 수동 요청 버튼
                AiPlanSectionHeader(
                    title = "AI 추천 플랜",
                    icon = "✨",
                    isLoading = isAiPlanLoading,
                    onAiRecommendClick = {
                        viewModel.requestAiRecommendedPlans()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isAiPlanLoading && aiPlanList.isEmpty()) {
                    AiPlanStatusCard(
                        message = "AI가 예산 플랜을 생성하고 있어요.",
                        isLoading = true,
                        onRetryClick = {}
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                } else if (aiPlanError.isNotBlank() && aiPlanList.isEmpty()) {
                    AiPlanStatusCard(
                        message = aiPlanError,
                        isLoading = false,
                        onRetryClick = {
                            viewModel.requestAiRecommendedPlans()
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    // 서버 AI 추천 플랜 목록 출력
                    aiPlanList.forEach { plan ->
                        BudgetPlanCard(
                            plan = plan,
                            onApplyClick = {
                                // 플랜 적용 버튼 누르면 ViewModel에 전달
                                viewModel.applyPlan(plan)
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 맞춤 예산 설정 제목
                SectionHeader(
                    title = "맞춤 예산 설정",
                    icon = "◎"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 직접 슬라이더로 예산 조절하는 카드
                CustomBudgetSettingCard(
                    monthlyIncome = budgetState.monthlyIncome,
                    savingGoal = budgetState.savingGoal,
                    foodBudget = budgetState.foodBudget,
                    transportBudget = budgetState.transportBudget,
                    livingBudget = budgetState.livingBudget,
                    hobbyBudget = budgetState.hobbyBudget,
                    onMonthlyIncomeChange = viewModel::updateMonthlyIncome,
                    onSavingGoalChange = viewModel::updateSavingGoal,
                    onFoodBudgetChange = viewModel::updateFoodBudget,
                    onTransportBudgetChange = viewModel::updateTransportBudget,
                    onLivingBudgetChange = viewModel::updateLivingBudget,
                    onHobbyBudgetChange = viewModel::updateHobbyBudget,
                    onSaveClick = {
                        // 저장 버튼 클릭 시 실행
                        viewModel.saveBudgetSettings()
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                CurrentMonthlyBudgetCard(
                    year = currentYear,
                    month = selectedMonth,
                    monthlyBudget = budgetState.monthlyIncome
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 예산 요약 카드
                BudgetSummaryCard(
                    monthlyIncome = budgetState.monthlyIncome,
                    totalExpense = totalExpense,
                    savingGoal = budgetState.savingGoal,
                    remainingAmount = remainingAmount
                )

                Spacer(modifier = Modifier.height(18.dp))

                BudgetCommentCard(
                    monthlyIncome = budgetState.monthlyIncome,
                    totalExpense = totalExpense,
                    savingGoal = budgetState.savingGoal,
                    remainingAmount = remainingAmount
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isMonthDialogOpen) {
                MonthPickerDialog(
                    selectedMonth = selectedMonth,
                    onDismiss = {
                        isMonthDialogOpen = false
                    },
                    onMonthSelected = { month ->
                        selectedMonth = month
                        isMonthDialogOpen = false
                    }
                )
            }

            // 하단 스낵바 출력
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

// 화면 상단 제목 영역
@Composable
private fun BudgetTopSection() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "예산 설정",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI가 추천하는 플랜으로 시작하거나 ",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF334155)
            )

            Text(
                text = "직접 설정해보세요",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// 섹션 제목 공통 UI
@Composable
private fun SectionHeader(
    title: String,
    icon: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun MonthSelectorCard(
    year: Int,
    month: Int,
    onOpenClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF7F8FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "월 선택",
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "${year}년 ${month}월",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onOpenClick) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "월 선택 열기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthPickerDialog(
    selectedMonth: Int,
    onDismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedMonth) {
        listState.scrollToItem((selectedMonth - 1).coerceIn(0, 11))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "월 선택",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(12) { index ->
                            val month = index + 1
                            val selected = month == selectedMonth
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        onMonthSelected(month)
                                    }
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${month}월",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPlanSectionHeader(
    title: String,
    icon: String,
    isLoading: Boolean,
    onAiRecommendClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Button(
            onClick = onAiRecommendClick,
            enabled = !isLoading,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = if (isLoading) "추천 중" else "AI 추천",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 추천 플랜 카드 UI
@Composable
private fun AiPlanStatusCard(
    message: String,
    isLoading: Boolean,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            if (!isLoading) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRetryClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "다시 요청",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetPlanCard(
    plan: BudgetPlanUiData,
    onApplyClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF7F8FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            InfoValueCard(
                label = "월 예산",
                value = formatWon(plan.monthlyBudget),
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF7EEF9),
                valueColor = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            InfoValueCard(
                label = "목표 저축",
                value = formatWon(plan.savingGoal),
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE9F7EE),
                valueColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0E9F4B)
            )

            Spacer(modifier = Modifier.height(18.dp))

            BudgetLineItem(Icons.Default.Restaurant, "식비", plan.food)
            BudgetLineItem(Icons.Default.Subway, "교통비", plan.transport)
            BudgetLineItem(Icons.Default.Home, "생활비", plan.living)
            BudgetLineItem(Icons.Default.FavoriteBorder, "여가/취미", plan.hobby)
            BudgetLineItem(Icons.Default.Savings, "저축", plan.saving)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onApplyClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "이 플랜 적용하기",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 라벨 + 값 카드
@Composable
private fun InfoValueCard(
    label: String,
    value: String,
    containerColor: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = containerColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// 예산 항목 한 줄 UI
@Composable
private fun BudgetLineItem(
    icon: ImageVector,
    label: String,
    amount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = formatWon(amount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 직접 슬라이더로 값 조절하는 카드
@Composable
private fun CustomBudgetSettingCard(
    monthlyIncome: Int,
    savingGoal: Int,
    foodBudget: Int,
    transportBudget: Int,
    livingBudget: Int,
    hobbyBudget: Int,
    onMonthlyIncomeChange: (Int) -> Unit,
    onSavingGoalChange: (Int) -> Unit,
    onFoodBudgetChange: (Int) -> Unit,
    onTransportBudgetChange: (Int) -> Unit,
    onLivingBudgetChange: (Int) -> Unit,
    onHobbyBudgetChange: (Int) -> Unit,
    onSaveClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF7F8FA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // 월 예산 슬라이더
            BudgetSliderItem(
                title = "월 예산",
                value = monthlyIncome,
                valueRange = 100000f..5000000f,
                steps = 0,
                icon = null,
                valueColor = MaterialTheme.colorScheme.onSurface,
                onValueChange = onMonthlyIncomeChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 저축 목표 슬라이더
            BudgetSliderItem(
                title = "저축 목표",
                value = savingGoal,
                valueRange = 0f..500000f,
                steps = 0,
                icon = null,
                valueColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF16A34A),
                onValueChange = onSavingGoalChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 상단 공통 항목과 카테고리 항목을 시각적으로 분리
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(18.dp))

            // 식비 슬라이더
            BudgetSliderItem(
                title = "식비",
                value = foodBudget,
                valueRange = 0f..10000000f,
                steps = 0,
                icon = Icons.Default.Restaurant,
                valueColor = MaterialTheme.colorScheme.onSurface,
                onValueChange = onFoodBudgetChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 교통비 슬라이더
            BudgetSliderItem(
                title = "교통비",
                value = transportBudget,
                valueRange = 0f..10000000f,
                steps = 0,
                icon = Icons.Default.Subway,
                valueColor = MaterialTheme.colorScheme.onSurface,
                onValueChange = onTransportBudgetChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 생활비 슬라이더
            BudgetSliderItem(
                title = "생활비",
                value = livingBudget,
                valueRange = 0f..10000000f,
                steps = 0,
                icon = Icons.Default.Home,
                valueColor = MaterialTheme.colorScheme.onSurface,
                onValueChange = onLivingBudgetChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 여가/취미 슬라이더
            BudgetSliderItem(
                title = "여가/취미",
                value = hobbyBudget,
                valueRange = 0f..10000000f,
                steps = 0,
                icon = Icons.Default.FavoriteBorder,
                valueColor = MaterialTheme.colorScheme.onSurface,
                onValueChange = onHobbyBudgetChange
            )

            Spacer(modifier = Modifier.height(26.dp))

            // 설정 저장 버튼
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "설정 저장",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 슬라이더 한 줄 UI
@Composable
private fun BudgetSliderItem(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: ImageVector? = null,
    valueColor: Color = Color.Unspecified,
    onValueChange: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // 슬라이더 현재 위치 상태
    var sliderPosition by remember(value) { mutableFloatStateOf(value.toFloat()) }
    var inputText by remember { mutableStateOf(value.toString()) }
    val minValue = valueRange.start.toInt()
    val maxValue = valueRange.endInclusive.toInt()

    LaunchedEffect(value) {
        sliderPosition = value.toFloat()
        if (inputText != value.toString()) {
            inputText = value.toString()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 제목과 숫자 입력 필드를 함께 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { changedText ->
                    val onlyDigits = changedText.filter { it.isDigit() }.take(9)
                    inputText = onlyDigits

                    val changedValue = onlyDigits.toIntOrNull()
                    if (changedValue != null) {
                        val clampedValue = changedValue.coerceIn(minValue, maxValue)
                        sliderPosition = clampedValue.toFloat()
                        onValueChange(clampedValue)
                    }
                },
                modifier = Modifier.width(152.dp),
                singleLine = true,
                suffix = {
                    Text(
                        text = "원",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 일반형 슬라이더
        // 사용자가 원하는 첫 번째 스크린샷 스타일에 맞춰
        // 점 없는 기본 슬라이더 모양으로 사용
        Slider(
            value = sliderPosition,
            onValueChange = { changedValue ->
                // 슬라이더를 움직이는 동안 현재 위치 갱신
                val clampedValue = changedValue.toInt().coerceIn(minValue, maxValue)
                sliderPosition = clampedValue.toFloat()
                inputText = clampedValue.toString()

                // Int 값으로 변환해서 상위로 전달
                onValueChange(clampedValue)
            },
            valueRange = valueRange,

            // 첫 번째 스타일처럼 연속형 슬라이더로 사용
            steps = steps,

            colors = SliderDefaults.colors(
                // 손잡이 색상
                thumbColor = Color.White,

                // 채워진 구간 색상
                activeTrackColor = if (isDark) SpentopiaGlowPurple else MaterialTheme.colorScheme.primary,

                // 비어있는 구간 색상
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,

                // 점 표시를 눈에 띄지 않게 트랙 색과 동일하게 설정
                activeTickColor = if (isDark) SpentopiaGlowPurple else MaterialTheme.colorScheme.primary,
                inactiveTickColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 최소값 / 최대값 표시
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatWonWithoutSuffix(valueRange.start.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = formatWonWithoutSuffix(valueRange.endInclusive.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 예산 요약 카드
@Composable
private fun BudgetSummaryCard(
    monthlyIncome: Int,
    totalExpense: Int,
    savingGoal: Int,
    remainingAmount: Int
) {
    val categoryRatio = if (monthlyIncome <= 0) {
        0f
    } else {
        (totalExpense.toFloat() / monthlyIncome.toFloat()).coerceIn(0f, 1f)
    }
    val categoryPercent = if (monthlyIncome <= 0) 0 else (totalExpense * 100 / monthlyIncome)
    val progressColor = if (categoryPercent <= 100) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val statusIcon = if (remainingAmount >= 0) {
        Icons.Default.SentimentSatisfied
    } else {
        Icons.Default.Warning
    }
    val statusIconTint = if (remainingAmount >= 0) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    // 남는 금액이 0 이상이면 긍정 메시지, 아니면 초과 메시지 출력
    val message = if (remainingAmount >= 0) {
        "균형잡힌 예산이에요! 남은 금액: ${formatWon(remainingAmount)}"
    } else {
        "현재 예산이 ${formatWon(-remainingAmount)} 초과 상태예요"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "예산 요약",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(18.dp))

                SummaryRow("저장된 월 예산", formatWon(monthlyIncome), MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                SummaryRow("카테고리 합계", formatWon(totalExpense), MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                SummaryRow("목표 저축액 포함", formatWon(totalExpense + savingGoal), MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "요약 상태",
                            tint = statusIconTint
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "${categoryPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(categoryRatio)
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(progressColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "카테고리 예산은 월 전체 예산 안에서만 배분되며, 목표 저축액은 별도로 관리됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                )
            }
        }
    }
}

// 예산 요약 행
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    labelColor: Color = Color.White,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = labelColor
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun CurrentMonthlyBudgetCard(
    year: Int,
    month: Int,
    monthlyBudget: Int
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF7F8FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "현재 설정된 월 예산",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${year}년 ${month}월 기준",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = formatWon(monthlyBudget),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BudgetCommentCard(
    monthlyIncome: Int,
    totalExpense: Int,
    savingGoal: Int,
    remainingAmount: Int
) {
    val isDark = isSystemInDarkTheme()
    val comment = when {
        monthlyIncome <= 0 -> "월 예산을 입력하면 카테고리별 계획을 더 정확하게 맞출 수 있어요."
        remainingAmount < 0 -> "지출과 저축 목표가 월 예산보다 ${formatWon(-remainingAmount)} 많아요. 카테고리 금액을 조금 낮춰보세요."
        savingGoal <= 0 -> "저축 목표를 함께 잡아두면 이번 달 예산 흐름을 더 안정적으로 관리할 수 있어요."
        totalExpense == 0 -> "카테고리 예산을 입력하면 이번 달 소비 계획을 한눈에 볼 수 있어요."
        else -> "현재 계획은 예산 안에서 움직이고 있어요. 저장하면 이번 달 기준으로 예산 설정이 반영됩니다."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF4F6FB)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "예산 설정 한마디",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = comment,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// AI 분석 카드
@Composable
private fun BudgetAnalysisCard(
    foodBudget: Int,
    totalExpense: Int,
    savingGoal: Int,
    aiAnalysisText: String
) {
    val isDark = isSystemInDarkTheme()

    // 식비가 전체 지출에서 차지하는 비율 계산
    val foodRatio = if (totalExpense == 0) 0 else (foodBudget * 100 / totalExpense)

    // 첫 번째 메시지
    val firstMessage = when {
        foodRatio <= 30 -> "식비 비중이 적정 수준이에요. 건강한 소비 습관이에요!"
        foodRatio <= 40 -> "식비 비중이 조금 높아요. 외식 횟수를 줄이면 더 좋아요."
        else -> "식비 비중이 높은 편이에요. 식비 관리가 핵심 포인트예요."
    }

    // 두 번째 메시지
    val secondMessage = when {
        savingGoal >= 100000 -> "저축 비율이 높아요! 목표 달성 가능성이 좋아요."
        savingGoal >= 50000 -> "저축 비율이 안정적이에요. 꾸준히 유지해보세요."
        else -> "저축 목표를 조금만 더 높이면 미래 준비에 도움이 돼요."
    }

    // 세 번째 메시지
    val thirdMessage = when {
        savingGoal >= 150000 -> "이 속도면 장기 목표 달성에 훨씬 가까워질 수 있어요!"
        savingGoal >= 50000 -> "이 흐름이면 꾸준한 자산 형성이 가능해요!"
        else -> "소액이라도 꾸준히 쌓이면 분명 큰 차이를 만들 수 있어요!"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF7F8FA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "AI 분석",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (aiAnalysisText.isNotBlank()) {
                AnalysisTextRow(Icons.Default.AutoAwesome, aiAnalysisText)
                Spacer(modifier = Modifier.height(12.dp))
            }

            AnalysisTextRow(Icons.Default.TrendingUp, firstMessage)
            Spacer(modifier = Modifier.height(12.dp))
            AnalysisTextRow(Icons.Default.VolunteerActivism, secondMessage)
            Spacer(modifier = Modifier.height(12.dp))
            AnalysisTextRow(Icons.Default.Savings, thirdMessage)
        }
    }
}

// AI 분석 한 줄
@Composable
private fun AnalysisTextRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 절약 팁 카드
@Composable
private fun SavingTipCard(
    foodBudget: Int,
    transportBudget: Int
) {
    val isDark = isSystemInDarkTheme()

    // 식비 관련 팁
    val foodTip = if (foodBudget >= 150000) {
        "식비는 외식을 줄이면 월 5만원 이상 절약 가능해요"
    } else {
        "현재 식비는 비교적 안정적이에요. 유지해도 좋아요"
    }

    // 교통비 관련 팁
    val transportTip = if (transportBudget >= 80000) {
        "대중교통 정기권으로 교통비 20% 절감할 수 있어요"
    } else {
        "현재 교통비는 무난한 편이에요. 고정비만 잘 관리해도 충분해요"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE6DDC8),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF8F1DD)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "절약 팁",
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "절약 팁",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TipBullet(text = foodTip)
            Spacer(modifier = Modifier.height(10.dp))
            TipBullet(text = transportTip)
            Spacer(modifier = Modifier.height(10.dp))
            TipBullet(text = "구독 서비스를 정리하면 월 3만원 안팎 절약할 수 있어요")
        }
    }
}

// 절약 팁 점 목록 한 줄
@Composable
private fun TipBullet(
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// AI 추천 플랜 하나를 담는 데이터 클래스
data class BudgetPlanUiData(
    val title: String,
    val description: String,
    val monthlyBudget: Int,
    val savingGoal: Int,
    val food: Int,
    val transport: Int,
    val living: Int,
    val hobby: Int,
    val saving: Int
)

// 숫자를 "1,000" 형태로 바꿔주는 함수
private fun formatWonWithoutSuffix(amount: Int): String {
    return "%,d".format(amount)
}

// 숫자를 "1,000원" 형태로 바꿔주는 함수
private fun formatWon(amount: Int): String {
    return "%,d원".format(amount)
}
