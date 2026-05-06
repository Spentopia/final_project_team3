package com.ict.spentopia.feature.analysis

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ict.spentopia.data.local.ExpenseEntity
import com.ict.spentopia.data.local.ExpenseDatabase
import com.ict.spentopia.data.remote.AnalyzeCategoryDataRequest
import com.ict.spentopia.data.remote.AnalyzeMonthlyDataRequest
import com.ict.spentopia.data.remote.AnalyzeReportRequest
import com.ict.spentopia.data.remote.AnalyzeTransactionRequest
import com.ict.spentopia.data.remote.AnalyzeWeeklyDataRequest
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.repository.ExpenseRepository
import com.ict.spentopia.feature.budget.BudgetDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// 카테고리별 지출 데이터 클래스임
// 도넛 차트/상세 리스트용
data class CategorySpendUiModel(

    // 카테고리 이름
    val name: String,

    // 카테고리 총 지출 금액
    val amount: Int,

    // 전체 지출 중 해당 카테고리 비율
    // 예: 0.45f = 45%
    val ratio: Float,

    // 화면에 표시할 색상
    val color: Color
)

// AI 분석 카드 데이터 클래스임
// 짧은 안내 카드용
data class AnalysisTipUiModel(

    // 카드 제목
    val title: String,

    // 카드 설명
    val description: String,

    // 카드 왼쪽 아이콘 대신 사용할 이모지
    val emoji: String,

    // 카드 테두리 색상
    val borderColor: Color
)

// 소비 패턴 진행률 데이터 클래스임
// 비율 표시용
data class PatternProgressUiModel(

    // 항목 이름
    val label: String,

    // 비율
    // 예: 0.75f = 75%
    val ratio: Float
)

data class AiConsumptionReportUiModel(
    val good: String = "",
    val warning: String = "",
    val advice: String = "",
    val prediction: String = "",
    val pattern: String = "",
    val improvement: String = ""
)

// 분석 화면 전체 상태임
// 화면 값 한 번에 묶음
data class AnalysisUiState(

    // 현재 선택된 기간
    // "주간" 또는 "월간"
    val selectedPeriod: String = "주간",

    // 이번 달 총 지출
    val totalExpense: Int = 0,

    // 일 평균 지출
    val averageDailyExpense: Int = 0,

    // 예산 사용률
    // 예: 0.60f = 60%
    val budgetUsageRate: Float = 0f,

    // 최대 소비 카테고리 이름
    val topCategoryName: String = "",

    // 최대 소비 카테고리 비율
    val topCategoryRatio: Float = 0f,

    // 주간 그래프 데이터
    val weeklyExpenseList: List<Pair<String, Int>> = emptyList(),

    // 월간 그래프 데이터
    val monthlyExpenseList: List<Pair<String, Int>> = emptyList(),

    // 카테고리별 지출 데이터
    val categoryList: List<CategorySpendUiModel> = emptyList(),

    // AI 분석 리포트 데이터
    val tipList: List<AnalysisTipUiModel> = emptyList(),

    // 백엔드 AI 리포트 문장입니다.
    val aiAnalysisText: String = "",

    val aiConsumptionReport: AiConsumptionReportUiModel? = null,

    // AI 리포트 요청 중인지 표시합니다.
    val isAiAnalysisLoading: Boolean = false,

    // AI 리포트 실패 메시지입니다.
    val aiAnalysisError: String = "",

    // 시간대별 소비 패턴 데이터
    val timePatternList: List<PatternProgressUiModel> = emptyList(),

    // 평일 평균 소비 텍스트
    val weekdayAverageText: String = "",

    // 주말 평균 소비 텍스트
    val weekendAverageText: String = "",

    // 요일별 소비 설명 문구
    val weekendComment: String = "",

    // 결제 방법 패턴 데이터
    val paymentPatternList: List<PatternProgressUiModel> = emptyList()
)

// 분석 화면 VM임
// Room 데이터 + 예산 + AI 요청 연결
class AnalysisViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Room Repository 생성
    // 로컬 소비 데이터 읽음
    private val repository = ExpenseRepository(
        ExpenseDatabase.getDatabase(application).expenseDao()
    )

    // BudgetDataStore 생성
    // 예산 사용률 계산용
    private val budgetDataStore = BudgetDataStore(application)

    // 내부 수정 상태
    // UI는 구독만 함
    private val _uiState = MutableStateFlow(AnalysisUiState())

    // 외부 읽기 전용
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var latestExpenseOnlyList: List<ExpenseEntity> = emptyList()
    private var latestMonthlyBudget: Int = 0

    init {
        observeAnalysisData()
    }

    // 주간 / 월간 선택 변경 함수입니다.
    fun selectPeriod(period: String) {
        _uiState.value = _uiState.value.copy(
            selectedPeriod = period
        )
    }

    // 현재 선택 상태에 따라 그래프 데이터를 반환하는 함수입니다.
    fun getCurrentTrendList(): List<Pair<String, Int>> {
        return if (_uiState.value.selectedPeriod == "주간") {
            _uiState.value.weeklyExpenseList
        } else {
            _uiState.value.monthlyExpenseList
        }
    }

    fun requestAiAnalysisReport() {
        val currentState = _uiState.value

        if (currentState.isAiAnalysisLoading || currentState.aiConsumptionReport != null) {
            return
        }

        if (currentState.totalExpense <= 0) {
            _uiState.value = currentState.copy(
                aiAnalysisText = "",
                aiConsumptionReport = null,
                aiAnalysisError = "분석할 소비 데이터가 없습니다."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAiAnalysisLoading = true,
                aiAnalysisError = ""
            )

            try {
                val report = RetrofitClient.aiAnalyzeApi.analyzeReport(
                    buildAnalyzeReportRequest(_uiState.value)
                )

                val uiReport = AiConsumptionReportUiModel(
                    good = report.good,
                    warning = report.warning,
                    advice = report.advice,
                    prediction = report.prediction,
                    pattern = report.pattern.orEmpty(),
                    improvement = report.improvement.orEmpty()
                )

                _uiState.value = _uiState.value.copy(
                    aiAnalysisText = uiReport.toReportText(),
                    aiConsumptionReport = uiReport,
                    isAiAnalysisLoading = false,
                    aiAnalysisError = ""
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isAiAnalysisLoading = false,
                    aiAnalysisError = when (e.code()) {
                        401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                        500, 502 -> "AI 분석 서버 응답을 불러오지 못했습니다."
                        else -> "AI 분석 요청에 실패했습니다. (${e.code()})"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAiAnalysisLoading = false,
                    aiAnalysisError = "AI 분석 요청에 실패했습니다. 잠시 후 다시 시도해주세요."
                )
            }
        }
    }

    // 실제 Room + DataStore 데이터를 계속 관찰해서
    // 분석 화면 상태를 업데이트하는 함수입니다.
    private fun observeAnalysisData() {
        viewModelScope.launch {
            combine(
                repository.getExpensesByMonth(getCurrentYearMonth()),
                budgetDataStore.budgetSettingsFlow
            ) { expenseList, budgetSettings ->

                // 수입 카테고리는 소비 분석에서 제외합니다.
                // 예: 월급, 용돈, 부수입, 환급, 기타수입은
                // 이번 달 총 지출 / 카테고리별 지출 / 소비 추이 / 예산 사용률 계산에 들어가면 안 됩니다.
                val expenseOnlyList = expenseList.filter { expense ->
                    isExpenseEntity(expense)
                }

                // 이번 달 총 지출입니다.
                // 수입 항목을 제외한 실제 지출 금액만 더합니다.
                val totalExpense = expenseOnlyList.sumOf { it.amount }

                // 이번 달 일 평균 지출입니다.
                // 초보자 기준으로 이해하기 쉽게 "오늘 날짜 기준"으로 나눕니다.
                // 예: 4월 15일이면 15일로 나눔
                val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                val averageDailyExpense =
                    if (currentDayOfMonth > 0) totalExpense / currentDayOfMonth else 0

                // 현재 프로젝트 기준 월 예산입니다.
                // Home과 동일하게 "월 수입 - 저축 목표"를 사용합니다.
                val monthlyBudget = budgetSettings.monthlyIncome - budgetSettings.savingGoal
                latestExpenseOnlyList = expenseOnlyList
                latestMonthlyBudget = monthlyBudget

                // 예산 사용률입니다.
                // AnalysisScreen에서는 0.60f = 60% 구조를 기대하고 있으므로
                // 0~1 사이 Float 값으로 계산합니다.
                val budgetUsageRate =
                    if (monthlyBudget > 0) {
                        totalExpense.toFloat() / monthlyBudget.toFloat()
                    } else {
                        0f
                    }

                // 카테고리별로 소비를 묶습니다.
                val categoryAmountMap = expenseOnlyList
                    .groupBy { expense ->
                        expense.category
                    }
                    .mapValues { entry ->
                        entry.value.sumOf { expense ->
                            expense.amount
                        }
                    }

                // 카테고리별 UI 리스트를 만듭니다.
                val categoryList = categoryAmountMap
                    .map { (categoryName, categoryAmount) ->
                        CategorySpendUiModel(
                            name = categoryName,
                            amount = categoryAmount,
                            ratio = if (totalExpense > 0) {
                                categoryAmount.toFloat() / totalExpense.toFloat()
                            } else {
                                0f
                            },
                            color = getCategoryColor(categoryName)
                        )
                    }
                    // 금액 큰 순서대로 정렬하면 화면에서 보기 편합니다.
                    .sortedByDescending { it.amount }

                // 가장 많이 쓴 카테고리입니다.
                val topCategory = categoryList.firstOrNull()

                // 주간 그래프 데이터입니다.
                // 현재 달의 소비를 "요일별"이 아니라 "최근 7일 라벨 형태"로 단순화하지 않고,
                // 지금 프로젝트 구조에 맞게 "월~일" 기준 합계로 보여줍니다.
                val weeklyExpenseList = createWeeklyExpenseList(expenseOnlyList)

                // 월간 그래프 데이터입니다.
                // 이번 달 소비를 1주, 2주, 3주, 4주, 5주 단위로 묶습니다.
                val monthlyExpenseList = createMonthlyExpenseList(expenseOnlyList)

                // AI 분석 카드입니다.
                // 아직 완전한 AI 분석은 아니지만,
                // 실제 데이터 기반 안내 문구가 뜨도록 만듭니다.
                val tipList = createAnalysisTipList(
                    totalExpense = totalExpense,
                    monthlyBudget = monthlyBudget,
                    topCategory = topCategory
                )

                // 현재 ExpenseEntity에는 시간대 / 결제수단 정보가 없어서
                // 이 부분은 일단 빈 리스트로 둡니다.
                val timePatternList = emptyList<PatternProgressUiModel>()
                val paymentPatternList = emptyList<PatternProgressUiModel>()

                // 현재 ExpenseEntity에는 평일/주말 세부 패턴 계산용 데이터가 부족하므로
                // 이 부분은 우선 기본 문구로 둡니다.
                val weekdayAverageText = ""
                val weekendAverageText = ""
                val weekendComment = ""

                // 최종 UI 상태를 반환합니다.
                AnalysisUiState(
                    // 기본 진입은 월간으로 두는 편이 지금 구조상 더 자연스럽습니다.
                    selectedPeriod = _uiState.value.selectedPeriod,
                    totalExpense = totalExpense,
                    averageDailyExpense = averageDailyExpense,
                    budgetUsageRate = budgetUsageRate,
                    topCategoryName = topCategory?.name ?: "",
                    topCategoryRatio = topCategory?.ratio ?: 0f,
                    weeklyExpenseList = weeklyExpenseList,
                    monthlyExpenseList = monthlyExpenseList,
                    categoryList = categoryList,
                    tipList = tipList,
                    aiAnalysisText = _uiState.value.aiAnalysisText,
                    aiConsumptionReport = _uiState.value.aiConsumptionReport,
                    isAiAnalysisLoading = _uiState.value.isAiAnalysisLoading,
                    aiAnalysisError = _uiState.value.aiAnalysisError,
                    timePatternList = timePatternList,
                    weekdayAverageText = weekdayAverageText,
                    weekendAverageText = weekendAverageText,
                    weekendComment = weekendComment,
                    paymentPatternList = paymentPatternList
                )
            }.collect { newUiState ->
                _uiState.value = newUiState
            }
        }
    }

    // 카테고리가 "수입"인지 판별하는 함수입니다.
    // 이 목록에 들어있는 카테고리는 소비 분석 계산에서 제외합니다.
    private fun isIncomeCategory(category: String): Boolean {
        return category in listOf(
            "월급",
            "용돈",
            "부수입",
            "환급",
            "기타수입"
        )
    }

    // ExpenseEntity 하나가 수입 항목인지 판별하는 함수입니다.
    private fun isIncomeEntity(
        expense: com.ict.spentopia.data.local.ExpenseEntity
    ): Boolean {
        return isIncomeCategory(expense.category)
    }

    // ExpenseEntity 하나가 지출 항목인지 판별하는 함수입니다.
    // 수입이 아니면 모두 지출로 처리합니다.
    private fun isExpenseEntity(
        expense: com.ict.spentopia.data.local.ExpenseEntity
    ): Boolean {
        return !isIncomeEntity(expense)
    }

    // 현재 연-월 문자열을 구하는 함수입니다.
    // 예: 2026-04
    private fun getCurrentYearMonth(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    // AI 리포트 생성에 사용할 기간입니다.
    // 주간 선택 시 최근 7일, 월간 선택 시 이번 달 1일~오늘을 보냅니다.
    private fun createReportPeriodRange(period: String): Pair<String, String> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val end = Calendar.getInstance()
        val start = Calendar.getInstance()

        if (period == "주간") {
            start.add(Calendar.DAY_OF_MONTH, -6)
        } else {
            start.set(Calendar.DAY_OF_MONTH, 1)
        }

        return formatter.format(start.time) to formatter.format(end.time)
    }

    private fun buildAnalyzeReportRequest(state: AnalysisUiState): AnalyzeReportRequest {
        val categoryData = state.categoryList.map { category ->
            AnalyzeCategoryDataRequest(
                name = category.name,
                amount = category.amount.toFloat(),
                value = (category.ratio * 100f),
                key = category.name
            )
        }

        return AnalyzeReportRequest(
            transactions = latestExpenseOnlyList.map { expense ->
                AnalyzeTransactionRequest(
                    date = expense.date,
                    amount = expense.amount,
                    category = expense.category,
                    type = "expense"
                )
            },
            totalExpense = state.totalExpense.toFloat(),
            budget = latestMonthlyBudget.toFloat(),
            topCategory = state.topCategoryName,
            topCategoryPercent = state.topCategoryRatio * 100f,
            dailyAverage = state.averageDailyExpense.toFloat(),
            expenseChangeRate = 0f,
            budgetUsage = state.budgetUsageRate * 100f,
            weeklyData = state.weeklyExpenseList.map { (day, amount) ->
                AnalyzeWeeklyDataRequest(
                    day = day,
                    amount = amount.toFloat()
                )
            },
            monthlyData = state.monthlyExpenseList.map { (month, amount) ->
                AnalyzeMonthlyDataRequest(
                    month = month,
                    amount = amount.toFloat()
                )
            },
            categoryData = categoryData
        )
    }

    private fun AiConsumptionReportUiModel.toReportText(): String {
        return listOf(
            "좋은 점: $good",
            "주의: $warning",
            "조언: $advice",
            "예측: $prediction"
        ).joinToString("\n\n")
    }

    // 카테고리별 색상 반환 함수입니다.
    private fun getCategoryColor(category: String): Color {
        return when (category) {
            "식비" -> Color(0xFFFF7A00)
            "교통" -> Color(0xFF334155)
            "쇼핑" -> Color(0xFFE84AA8)
            "카페" -> Color(0xFF1E1B4B)
            "여가" -> Color(0xFF312E81)
            "생활비" -> Color(0xFF475569)
            else -> Color(0xFF6B7280)
        }
    }

    // 주간 그래프 데이터 생성 함수입니다.
    // 월~일 순서로 요일별 합계를 만듭니다.
    private fun createWeeklyExpenseList(
        expenseList: List<com.ict.spentopia.data.local.ExpenseEntity>
    ): List<Pair<String, Int>> {

        // 요일별 기본값을 0으로 먼저 넣어둡니다.
        val dayMap = linkedMapOf(
            "월" to 0,
            "화" to 0,
            "수" to 0,
            "목" to 0,
            "금" to 0,
            "토" to 0,
            "일" to 0
        )

        expenseList.forEach { expense ->
            try {
                val parts = expense.date.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                }

                val dayLabel = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "월"
                    Calendar.TUESDAY -> "화"
                    Calendar.WEDNESDAY -> "수"
                    Calendar.THURSDAY -> "목"
                    Calendar.FRIDAY -> "금"
                    Calendar.SATURDAY -> "토"
                    Calendar.SUNDAY -> "일"
                    else -> ""
                }

                if (dayLabel.isNotBlank()) {
                    dayMap[dayLabel] = (dayMap[dayLabel] ?: 0) + expense.amount
                }
            } catch (_: Exception) {
                // 날짜 파싱 실패 시 그냥 넘어갑니다.
            }
        }

        return dayMap.toList()
    }

    // 월간 그래프 데이터 생성 함수입니다.
    // 이번 달 소비를 주차별로 묶어서 1주~5주 리스트를 만듭니다.
    private fun createMonthlyExpenseList(
        expenseList: List<com.ict.spentopia.data.local.ExpenseEntity>
    ): List<Pair<String, Int>> {

        val weekMap = linkedMapOf(
            "1주" to 0,
            "2주" to 0,
            "3주" to 0,
            "4주" to 0,
            "5주" to 0
        )

        expenseList.forEach { expense ->
            try {
                val day = expense.date.split("-")[2].toInt()

                val weekLabel = when (day) {
                    in 1..7 -> "1주"
                    in 8..14 -> "2주"
                    in 15..21 -> "3주"
                    in 22..28 -> "4주"
                    else -> "5주"
                }

                weekMap[weekLabel] = (weekMap[weekLabel] ?: 0) + expense.amount
            } catch (_: Exception) {
                // 날짜 파싱 실패 시 그냥 넘어갑니다.
            }
        }

        return weekMap.toList()
    }

    // 분석 팁 리스트 생성 함수입니다.
    // 아직 완전한 AI 분석은 아니지만,
    // 실제 금액/예산/최대 카테고리 기준 문구를 만듭니다.
    private fun createAnalysisTipList(
        totalExpense: Int,
        monthlyBudget: Int,
        topCategory: CategorySpendUiModel?
    ): List<AnalysisTipUiModel> {

        val result = mutableListOf<AnalysisTipUiModel>()

        if (totalExpense == 0) {
            result.add(
                AnalysisTipUiModel(
                    title = "기록을 시작해보세요",
                    description = "아직 이번 달 소비 기록이 없어요. Home 화면에서 소비를 입력하면 분석이 자동으로 시작돼요.",
                    emoji = "📝",
                    borderColor = Color(0xFFA7C7FF)
                )
            )
            return result
        }

        if (monthlyBudget > 0) {
            val usageRate = totalExpense.toFloat() / monthlyBudget.toFloat()

            if (usageRate < 0.5f) {
                result.add(
                    AnalysisTipUiModel(
                        title = "좋은 흐름이에요",
                        description = "현재 예산의 절반 이하만 사용했어요. 지금의 소비 흐름을 유지하면 목표를 지키기 좋아요.",
                        emoji = "✅",
                        borderColor = Color(0xFFB7E4C7)
                    )
                )
            } else if (usageRate < 1f) {
                result.add(
                    AnalysisTipUiModel(
                        title = "예산 안에서 잘 쓰고 있어요",
                        description = "현재 예산 범위 안에서 소비 중이에요. 남은 기간 동안 큰 지출만 조심하면 좋아요.",
                        emoji = "💡",
                        borderColor = Color(0xFFD6C8FF)
                    )
                )
            } else {
                result.add(
                    AnalysisTipUiModel(
                        title = "예산 초과에 주의해요",
                        description = "이번 달 예산을 이미 넘겼어요. 고정 지출이 아닌 소비부터 먼저 줄여보는 게 좋아요.",
                        emoji = "⚠️",
                        borderColor = Color(0xFFFFD166)
                    )
                )
            }
        }

        if (topCategory != null) {
            result.add(
                AnalysisTipUiModel(
                    title = "가장 큰 지출 카테고리",
                    description = "이번 달에는 ${topCategory.name} 소비가 가장 컸어요. 전체 소비의 ${(topCategory.ratio * 100).toInt()}%를 차지하고 있어요.",
                    emoji = "📊",
                    borderColor = Color(0xFFA7C7FF)
                )
            )
        }

        if (result.size < 3) {
            result.add(
                AnalysisTipUiModel(
                    title = "소비 기록을 이어가보세요",
                    description = "기록이 쌓일수록 더 정확한 소비 패턴을 볼 수 있어요. 지금처럼 꾸준히 입력하는 게 가장 중요해요.",
                    emoji = "🌱",
                    borderColor = Color(0xFFD6C8FF)
                )
            )
        }

        return result
    }
}
