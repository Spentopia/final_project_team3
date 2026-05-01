package com.ict.spentopia.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple
import com.ict.spentopia.ui.theme.SpentopiaNavy
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors
import com.ict.spentopia.ui.theme.spentopiaCtaBorderColor
import com.ict.spentopia.ui.theme.spentopiaCtaContentColor
import com.ict.spentopia.ui.theme.spentopiaCtaGradientColors

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

    // 총 지출 예정 금액 계산
    val totalExpense = budgetState.foodBudget +
            budgetState.transportBudget +
            budgetState.livingBudget +
            budgetState.hobbyBudget

    // 남는 금액 계산
    // 월 수입 - 총 지출 - 저축 목표
    val remainingAmount = budgetState.monthlyIncome - totalExpense - budgetState.savingGoal

    // AI 추천 플랜 샘플 데이터임
    // 서버 추천 전 예시값
    val aiPlanList = listOf(
        BudgetPlanUiData(
            title = "월 50만원 생활 플랜",
            description = "합리적인 소비와 저축을 위한 균형잡힌 플랜",
            monthlyBudget = 500000,
            savingGoal = 50000,
            food = 150000,
            transport = 80000,
            living = 120000,
            hobby = 100000,
            saving = 50000
        ),
        BudgetPlanUiData(
            title = "7년 1억 만들기",
            description = "목표 지향적인 저축 중심 플랜",
            monthlyBudget = 400000,
            savingGoal = 150000,
            food = 100000,
            transport = 60000,
            living = 90000,
            hobby = 50000,
            saving = 150000
        ),
        BudgetPlanUiData(
            title = "자유로운 소비 플랜",
            description = "현재의 삶을 즐기면서도 미래를 준비하는 플랜",
            monthlyBudget = 700000,
            savingGoal = 30000,
            food = 200000,
            transport = 100000,
            living = 200000,
            hobby = 170000,
            saving = 30000
        )
    )

    // 저장 성공하면 스낵바 메시지 띄우기
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("예산 설정이 저장되었어요.")
            viewModel.resetSaveSuccess()
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

                // AI 추천 플랜 제목
                SectionHeader(
                    title = "AI 추천 플랜",
                    icon = "✨"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 추천 플랜 목록 출력
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

                // 예산 요약 카드
                BudgetSummaryCard(
                    monthlyIncome = budgetState.monthlyIncome,
                    totalExpense = totalExpense,
                    savingGoal = budgetState.savingGoal,
                    remainingAmount = remainingAmount
                )

                Spacer(modifier = Modifier.height(18.dp))

                // AI 분석 카드
                BudgetAnalysisCard(
                    foodBudget = budgetState.foodBudget,
                    totalExpense = totalExpense,
                    savingGoal = budgetState.savingGoal
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 절약 팁 카드
                SavingTipCard(
                    foodBudget = budgetState.foodBudget,
                    transportBudget = budgetState.transportBudget
                )

                Spacer(modifier = Modifier.height(24.dp))
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

// 추천 플랜 카드 UI
@Composable
private fun BudgetPlanCard(
    plan: BudgetPlanUiData,
    onApplyClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val ctaContentColor = spentopiaCtaContentColor(isDark)

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

            val applyInteractionSource = remember { MutableInteractionSource() }
            val applyPressed by applyInteractionSource.collectIsPressedAsState()
            Button(
                onClick = onApplyClick,
                interactionSource = applyInteractionSource,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = ctaContentColor
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = if (applyPressed) 0.985f else 1f
                            scaleY = if (applyPressed) 0.985f else 1f
                        }
                        .background(
                            brush = Brush.horizontalGradient(spentopiaCtaGradientColors(isDark)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이 플랜 적용하기",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = ctaContentColor
                    )
                }
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
    val ctaContentColor = spentopiaCtaContentColor(isDark)

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
            // 월 수입 슬라이더
            BudgetSliderItem(
                title = "월 수입",
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
            val saveInteractionSource = remember { MutableInteractionSource() }
            val savePressed by saveInteractionSource.collectIsPressedAsState()
            Button(
                onClick = onSaveClick,
                interactionSource = saveInteractionSource,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = ctaContentColor
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = if (savePressed) 0.985f else 1f
                            scaleY = if (savePressed) 0.985f else 1f
                        }
                        .background(
                            brush = Brush.horizontalGradient(spentopiaCtaGradientColors(isDark)),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "설정 저장",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ctaContentColor
                    )
                }
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

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 제목과 현재 값을 한 줄에 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 제목 영역
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아이콘이 있는 항목만 아이콘 출력
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

            // 오른쪽 현재 금액 출력
            Text(
                text = formatWon(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
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
                sliderPosition = changedValue

                // Int 값으로 변환해서 상위로 전달
                onValueChange(changedValue.toInt())
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
    val isDark = isSystemInDarkTheme()
    val ctaContentColor = spentopiaCtaContentColor(isDark)
    val ctaMutedContentColor = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant

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
                ambientColor = SpentopiaGlowPurple.copy(alpha = 0.18f),
                spotColor = SpentopiaGlowPurple.copy(alpha = 0.22f)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = spentopiaCtaGradientColors(isDark)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = spentopiaCtaBorderColor(isDark),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "예산 요약",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ctaContentColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                SummaryRow("월 수입", formatWon(monthlyIncome), ctaMutedContentColor, ctaContentColor)
                SummaryRow("총 지출 예정", formatWon(totalExpense), ctaMutedContentColor, ctaContentColor)
                SummaryRow("저축 목표", formatWon(savingGoal), ctaMutedContentColor, ctaContentColor)

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(10.dp))

                SummaryRow("합계", formatWon(totalExpense + savingGoal), ctaMutedContentColor, ctaContentColor)

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "요약 상태",
                            tint = ctaContentColor
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ctaContentColor
                        )
                    }
                }
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

// AI 분석 카드
@Composable
private fun BudgetAnalysisCard(
    foodBudget: Int,
    totalExpense: Int,
    savingGoal: Int
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

            AnalysisTextRow(Icons.Default.TrendingUp, firstMessage)
            Spacer(modifier = Modifier.height(12.dp))
            AnalysisTextRow(Icons.Default.AutoAwesome, secondMessage)
            Spacer(modifier = Modifier.height(12.dp))
            AnalysisTextRow(Icons.Default.VolunteerActivism, thirdMessage)
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
