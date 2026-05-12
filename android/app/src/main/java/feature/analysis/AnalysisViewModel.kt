package com.ict.spentopia.feature.analysis // 이 파일이 속한 패키지 위치를 적음

import android.app.Application // 앱 전체 정보 타입을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.lifecycle.AndroidViewModel // AndroidViewModel 기능을 가져옴
import androidx.lifecycle.viewModelScope // viewModelScope 기능을 가져옴
import com.ict.spentopia.data.local.ExpenseEntity // ExpenseEntity 기능을 가져옴
import com.ict.spentopia.data.local.ExpenseDatabase // ExpenseDatabase 기능을 가져옴
import com.ict.spentopia.data.remote.AnalyzeCategoryDataRequest // AnalyzeCategoryDataRequest 기능을 가져옴
import com.ict.spentopia.data.remote.AnalyzeMonthlyDataRequest // AnalyzeMonthlyDataRequest 기능을 가져옴
import com.ict.spentopia.data.remote.AnalyzeReportRequest // AnalyzeReportRequest 기능을 가져옴
import com.ict.spentopia.data.remote.AnalyzeTransactionRequest // AnalyzeTransactionRequest 기능을 가져옴
import com.ict.spentopia.data.remote.AnalyzeWeeklyDataRequest // AnalyzeWeeklyDataRequest 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.repository.ExpenseRepository // ExpenseRepository 기능을 가져옴
import com.ict.spentopia.feature.budget.BudgetDataStore // BudgetDataStore 기능을 가져옴
import kotlinx.coroutines.flow.MutableStateFlow // 바뀌는 상태값 도구를 가져옴
import kotlinx.coroutines.flow.StateFlow // 읽기 전용 상태값 도구를 가져옴
import kotlinx.coroutines.flow.asStateFlow // asStateFlow 기능을 가져옴
import kotlinx.coroutines.flow.combine // combine 기능을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import retrofit2.HttpException // 서버 오류 타입을 가져옴
import java.text.SimpleDateFormat // SimpleDateFormat 기능을 가져옴
import java.util.Calendar // Calendar 기능을 가져옴
import java.util.Locale // Locale 기능을 가져옴

// 카테고리별 지출 데이터 클래스임
// 도넛 차트/상세 리스트용
data class CategorySpendUiModel( // CategorySpendUiModel 데이터를 묶어둘 클래스 시작

    // 카테고리 이름
    val name: String, // name 값을 저장함

    // 카테고리 총 지출 금액
    val amount: Int, // 금액을 저장함

    // 전체 지출 중 해당 카테고리 비율
    // 예: 0.45f = 45%
    val ratio: Float, // ratio 값을 저장함

    // 화면에 표시할 색상
    val color: Color // color 값을 저장함
)

// AI 분석 카드 데이터 클래스임
// 짧은 안내 카드용
data class AnalysisTipUiModel( // AnalysisTipUiModel 데이터를 묶어둘 클래스 시작

    // 카드 제목
    val title: String, // 제목을 저장함

    // 카드 설명
    val description: String, // description 값을 저장함

    // 카드 왼쪽 아이콘 대신 사용할 이모지
    val emoji: String, // emoji 값을 저장함

    // 카드 테두리 색상
    val borderColor: Color // borderColor 값을 저장함
)

// 소비 패턴 진행률 데이터 클래스임
// 비율 표시용
data class PatternProgressUiModel( // PatternProgressUiModel 데이터를 묶어둘 클래스 시작

    // 항목 이름
    val label: String, // label 값을 저장함

    // 비율
    // 예: 0.75f = 75%
    val ratio: Float // ratio 값을 저장함
)

data class AiConsumptionReportUiModel( // AiConsumptionReportUiModel 데이터를 묶어둘 클래스 시작
    val good: String = "", // good 값을 저장함
    val warning: String = "", // warning 값을 저장함
    val advice: String = "", // advice 값을 저장함
    val prediction: String = "", // prediction 값을 저장함
    val pattern: String = "", // pattern 값을 저장함
    val improvement: String = "" // improvement 값을 저장함
)

// 분석 화면 전체 상태임
// 화면 값 한 번에 묶음
data class AnalysisUiState( // AnalysisUiState 데이터를 묶어둘 클래스 시작

    // 현재 선택된 기간
    // "주간" 또는 "월간"
    val selectedPeriod: String = "주간", // selectedPeriod 값을 저장함

    // 이번 달 총 지출
    val totalExpense: Int = 0, // 소비 내역 값을 저장함

    // 일 평균 지출
    val averageDailyExpense: Int = 0, // 소비 내역 값을 저장함

    // 예산 사용률
    // 예: 0.60f = 60%
    val budgetUsageRate: Float = 0f, // 예산 관련 값을 저장함

    // 최대 소비 카테고리 이름
    val topCategoryName: String = "", // topCategoryName 값을 저장함

    // 최대 소비 카테고리 비율
    val topCategoryRatio: Float = 0f, // topCategoryRatio 값을 저장함

    // 주간 그래프 데이터
    val weeklyExpenseList: List<Pair<String, Int>> = emptyList(), // 소비 내역 값을 저장함

    // 월간 그래프 데이터
    val monthlyExpenseList: List<Pair<String, Int>> = emptyList(), // 소비 내역 값을 저장함

    // 카테고리별 지출 데이터
    val categoryList: List<CategorySpendUiModel> = emptyList(), // categoryList 값을 저장함

    // AI 분석 리포트 데이터
    val tipList: List<AnalysisTipUiModel> = emptyList(), // tipList 값을 저장함

    // 백엔드 AI 리포트 문장입니다.
    val aiAnalysisText: String = "", // aiAnalysisText 값을 저장함

    val aiConsumptionReport: AiConsumptionReportUiModel? = null, // aiConsumptionReport 값을 저장함

    // AI 리포트 요청 중인지 표시합니다.
    val isAiAnalysisLoading: Boolean = false, // 로딩 상태를 저장함

    // AI 리포트 실패 메시지입니다.
    val aiAnalysisError: String = "", // 오류 내용을 저장함

    // 시간대별 소비 패턴 데이터
    val timePatternList: List<PatternProgressUiModel> = emptyList(), // timePatternList 값을 저장함

    // 평일 평균 소비 텍스트
    val weekdayAverageText: String = "", // weekdayAverageText 값을 저장함

    // 주말 평균 소비 텍스트
    val weekendAverageText: String = "", // weekendAverageText 값을 저장함

    // 요일별 소비 설명 문구
    val weekendComment: String = "", // weekendComment 값을 저장함

    // 결제 방법 패턴 데이터
    val paymentPatternList: List<PatternProgressUiModel> = emptyList() // paymentPatternList 값을 저장함
)

// 분석 화면 VM임
// Room 데이터 + 예산 + AI 요청 연결
class AnalysisViewModel( // AnalysisViewModel 기능을 묶어둔 클래스 시작
    application: Application // 앱 전체 정보를 받음
) : AndroidViewModel(application) { // 이 블록 안의 내용이 시작됨

    // Room Repository 생성
    // 로컬 소비 데이터 읽음
    private val repository = ExpenseRepository( // 데이터 처리 객체을 저장함
        ExpenseDatabase.getDatabase(application).expenseDao()
    )

    // BudgetDataStore 생성
    // 예산 사용률 계산용
    private val budgetDataStore = BudgetDataStore(application) // 예산 관련 값을 저장함

    // 내부 수정 상태
    // UI는 구독만 함
    private val _uiState = MutableStateFlow(AnalysisUiState()) // 화면에서 바뀔 화면 상태를 저장함

    // 외부 읽기 전용
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow() // 화면에서 화면 상태를 읽을 수 있게 열어둠

    private var latestExpenseOnlyList: List<ExpenseEntity> = emptyList() // 나중에 바뀔 수 있는 소비 내역 값을 저장함
    private var latestMonthlyBudget: Long = 0L // 나중에 바뀔 수 있는 예산 관련 값을 저장함

    init { // 이 블록 안의 내용이 시작됨
        observeAnalysisData() // observe Analysis Data 함수를 실행함
    }

    // 주간 / 월간 선택 변경 함수입니다.
    fun selectPeriod(period: String) { // selectPeriod 함수를 선언함
        _uiState.value = _uiState.value.copy( // uiState.value 값을 정해줌
            selectedPeriod = period // period 값을 selectedPeriod 값에 넣음
        )
    }

    // 현재 선택 상태에 따라 그래프 데이터를 반환하는 함수입니다.
    fun getCurrentTrendList(): List<Pair<String, Int>> { // 데이터를 불러오는 함수 시작
        return if (_uiState.value.selectedPeriod == "주간") { // 이 값을 함수 결과로 돌려줌
            _uiState.value.weeklyExpenseList
        } else { // 이 블록 안의 내용이 시작됨
            _uiState.value.monthlyExpenseList
        }
    }

    fun requestAiAnalysisReport() { // requestAiAnalysisReport 함수를 선언함
        val currentState = _uiState.value // currentState 값을 저장함

        if (currentState.isAiAnalysisLoading || currentState.aiConsumptionReport != null) { // 조건이 맞는지 확인함
            return
        }

        if (currentState.totalExpense <= 0) { // 조건이 맞는지 확인함
            _uiState.value = currentState.copy( // uiState.value 값을 정해줌
                aiAnalysisText = "", // aiAnalysisText 값을 정해줌
                aiConsumptionReport = null, // null 값을 aiConsumptionReport 값에 넣음
                aiAnalysisError = "분석할 소비 데이터가 없습니다." // 오류 내용을 정해줌
            )
            return
        }

        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            _uiState.value = _uiState.value.copy( // uiState.value 값을 정해줌
                isAiAnalysisLoading = true, // true 값을 로딩 상태에 넣음
                aiAnalysisError = "" // 오류 내용을 정해줌
            )

            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val report = RetrofitClient.aiAnalyzeApi.analyzeReport( // report 값을 저장함
                    buildAnalyzeReportRequest(_uiState.value) // build Analyze Report Request 함수를 실행함
                )

                val uiReport = AiConsumptionReportUiModel( // uiReport 값을 저장함
                    good = report.good, // good 값을 정해줌
                    warning = report.warning, // warning 값을 정해줌
                    advice = report.advice, // advice 값을 정해줌
                    prediction = report.prediction, // prediction 값을 정해줌
                    pattern = report.pattern.orEmpty(), // pattern 값을 정해줌
                    improvement = report.improvement.orEmpty() // improvement 값을 정해줌
                )

                _uiState.value = _uiState.value.copy( // uiState.value 값을 정해줌
                    aiAnalysisText = uiReport.toReportText(), // 화면에 글자를 보여줌
                    aiConsumptionReport = uiReport, // uiReport 값을 aiConsumptionReport 값에 넣음
                    isAiAnalysisLoading = false, // false 값을 로딩 상태에 넣음
                    aiAnalysisError = "" // 오류 내용을 정해줌
                )
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                _uiState.value = _uiState.value.copy( // uiState.value 값을 정해줌
                    isAiAnalysisLoading = false, // false 값을 로딩 상태에 넣음
                    aiAnalysisError = when (e.code()) { // 오류 내용을 정해줌
                        401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                        500, 502 -> "AI 분석 서버 응답을 불러오지 못했습니다."
                        else -> "AI 분석 요청에 실패했습니다. (${e.code()})" // 위 조건이 아니면 이쪽을 실행함
                    }
                )
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                _uiState.value = _uiState.value.copy( // uiState.value 값을 정해줌
                    isAiAnalysisLoading = false, // false 값을 로딩 상태에 넣음
                    aiAnalysisError = "AI 분석 요청에 실패했습니다. 잠시 후 다시 시도해주세요." // 오류 내용을 정해줌
                )
            }
        }
    }

    // 실제 Room + DataStore 데이터를 계속 관찰해서
    // 분석 화면 상태를 업데이트하는 함수입니다.
    private fun observeAnalysisData() { // observeAnalysisData 함수를 선언함
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            combine( // combine 함수를 실행함
                repository.getExpensesByMonth(getCurrentYearMonth()),
                budgetDataStore.budgetSettingsFlow
            ) { expenseList, budgetSettings ->

                // 수입 카테고리는 소비 분석에서 제외합니다.
                // 예: 월급, 용돈, 부수입, 환급, 기타수입은
                // 이번 달 총 지출 / 카테고리별 지출 / 소비 추이 / 예산 사용률 계산에 들어가면 안 됩니다.
                val expenseOnlyList = expenseList.filter { expense -> // 소비 내역 값을 저장함
                    isExpenseEntity(expense) // is Expense Entity 함수를 실행함
                }

                // 이번 달 총 지출입니다.
                // 수입 항목을 제외한 실제 지출 금액만 더합니다.
                val totalExpense = expenseOnlyList.sumOf { it.amount } // 소비 내역 값을 저장함

                // 이번 달 일 평균 지출입니다.
                // 초보자 기준으로 이해하기 쉽게 "오늘 날짜 기준"으로 나눕니다.
                // 예: 4월 15일이면 15일로 나눔
                val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) // currentDayOfMonth 값을 저장함
                val averageDailyExpense = // 소비 내역 값을 저장함
                    if (currentDayOfMonth > 0) totalExpense / currentDayOfMonth else 0 // 조건이 맞는지 확인함

                // 현재 프로젝트 기준 월 예산입니다.
                // Home과 동일하게 "월 수입 - 저축 목표"를 사용합니다.
                val monthlyBudget = budgetSettings.monthlyIncome - budgetSettings.savingGoal // 예산 관련 값을 저장함
                latestExpenseOnlyList = expenseOnlyList // 소비 내역 값을 소비 내역 값에 넣음
                latestMonthlyBudget = monthlyBudget // 예산 관련 값을 예산 관련 값에 넣음

                // 예산 사용률입니다.
                // AnalysisScreen에서는 0.60f = 60% 구조를 기대하고 있으므로
                // 0~1 사이 Float 값으로 계산합니다.
                val budgetUsageRate = // 예산 관련 값을 저장함
                    if (monthlyBudget > 0) { // 조건이 맞는지 확인함
                        totalExpense.toFloat() / monthlyBudget.toFloat()
                    } else { // 이 블록 안의 내용이 시작됨
                        0f
                    }

                // 카테고리별로 소비를 묶습니다.
                val categoryAmountMap = expenseOnlyList // categoryAmountMap 값을 저장함
                    .groupBy { expense ->
                        expense.category
                    }
                    .mapValues { entry ->
                        entry.value.sumOf { expense ->
                            expense.amount
                        }
                    }

                // 카테고리별 UI 리스트를 만듭니다.
                val categoryList = categoryAmountMap // categoryList 값을 저장함
                    .map { (categoryName, categoryAmount) ->
                        CategorySpendUiModel( // Category Spend Ui Model 함수를 실행함
                            name = categoryName, // categoryName 값을 name 값에 넣음
                            amount = categoryAmount, // categoryAmount 값을 금액에 넣음
                            ratio = if (totalExpense > 0) { // ratio 값을 정해줌
                                categoryAmount.toFloat() / totalExpense.toFloat()
                            } else { // 이 블록 안의 내용이 시작됨
                                0f
                            },
                            color = getCategoryColor(categoryName) // color 값을 정해줌
                        )
                    }
                    // 금액 큰 순서대로 정렬하면 화면에서 보기 편합니다.
                    .sortedByDescending { it.amount }

                // 가장 많이 쓴 카테고리입니다.
                val topCategory = categoryList.firstOrNull() // topCategory 값을 저장함

                // 주간 그래프 데이터입니다.
                // 현재 달의 소비를 "요일별"이 아니라 "최근 7일 라벨 형태"로 단순화하지 않고,
                // 지금 프로젝트 구조에 맞게 "월~일" 기준 합계로 보여줍니다.
                val weeklyExpenseList = createWeeklyExpenseList(expenseOnlyList) // 소비 내역 값을 저장함

                // 월간 그래프 데이터입니다.
                // 1월부터 12월까지 월별 소비 합계를 만듭니다.
                val monthlyExpenseList = createMonthlyExpenseList(expenseOnlyList) // 소비 내역 값을 저장함

                // AI 분석 카드입니다.
                // 아직 완전한 AI 분석은 아니지만,
                // 실제 데이터 기반 안내 문구가 뜨도록 만듭니다.
                val tipList = createAnalysisTipList( // tipList 값을 저장함
                    totalExpense = totalExpense, // 소비 내역 값을 소비 내역 값에 넣음
                    monthlyBudget = monthlyBudget, // 예산 관련 값을 예산 관련 값에 넣음
                    topCategory = topCategory // topCategory 값을 topCategory 값에 넣음
                )

                // 현재 ExpenseEntity에는 시간대 / 결제수단 정보가 없어서
                // 이 부분은 일단 빈 리스트로 둡니다.
                val timePatternList = emptyList<PatternProgressUiModel>() // timePatternList 값을 저장함
                val paymentPatternList = emptyList<PatternProgressUiModel>() // paymentPatternList 값을 저장함

                // 현재 ExpenseEntity에는 평일/주말 세부 패턴 계산용 데이터가 부족하므로
                // 이 부분은 우선 기본 문구로 둡니다.
                val weekdayAverageText = "" // weekdayAverageText 값을 저장함
                val weekendAverageText = "" // weekendAverageText 값을 저장함
                val weekendComment = "" // weekendComment 값을 저장함

                // 최종 UI 상태를 반환합니다.
                AnalysisUiState( // Analysis Ui State 함수를 실행함
                    // 기본 진입은 월간으로 두는 편이 지금 구조상 더 자연스럽습니다.
                    selectedPeriod = _uiState.value.selectedPeriod, // selectedPeriod 값을 정해줌
                    totalExpense = totalExpense, // 소비 내역 값을 소비 내역 값에 넣음
                    averageDailyExpense = averageDailyExpense, // 소비 내역 값을 소비 내역 값에 넣음
                    budgetUsageRate = budgetUsageRate, // 예산 관련 값을 예산 관련 값에 넣음
                    topCategoryName = topCategory?.name ?: "", // topCategoryName 값을 정해줌
                    topCategoryRatio = topCategory?.ratio ?: 0f, // topCategoryRatio 값을 정해줌
                    weeklyExpenseList = weeklyExpenseList, // 소비 내역 값을 소비 내역 값에 넣음
                    monthlyExpenseList = monthlyExpenseList, // 소비 내역 값을 소비 내역 값에 넣음
                    categoryList = categoryList, // categoryList 값을 categoryList 값에 넣음
                    tipList = tipList, // tipList 값을 tipList 값에 넣음
                    aiAnalysisText = _uiState.value.aiAnalysisText, // aiAnalysisText 값을 정해줌
                    aiConsumptionReport = _uiState.value.aiConsumptionReport, // aiConsumptionReport 값을 정해줌
                    isAiAnalysisLoading = _uiState.value.isAiAnalysisLoading, // 로딩 상태를 정해줌
                    aiAnalysisError = _uiState.value.aiAnalysisError, // 오류 내용을 정해줌
                    timePatternList = timePatternList, // timePatternList 값을 timePatternList 값에 넣음
                    weekdayAverageText = weekdayAverageText, // weekdayAverageText 값을 weekdayAverageText 값에 넣음
                    weekendAverageText = weekendAverageText, // weekendAverageText 값을 weekendAverageText 값에 넣음
                    weekendComment = weekendComment, // weekendComment 값을 weekendComment 값에 넣음
                    paymentPatternList = paymentPatternList // paymentPatternList 값을 paymentPatternList 값에 넣음
                )
            }.collect { newUiState ->
                _uiState.value = newUiState // uiState.value 값을 정해줌
            }
        }
    }

    // 카테고리가 "수입"인지 판별하는 함수입니다.
    // 이 목록에 들어있는 카테고리는 소비 분석 계산에서 제외합니다.
    private fun isIncomeCategory(category: String): Boolean { // isIncomeCategory 함수를 선언함
        return category in listOf( // 이 값을 함수 결과로 돌려줌
            "월급",
            "용돈",
            "부수입",
            "환급",
            "기타수입"
        )
    }

    // ExpenseEntity 하나가 수입 항목인지 판별하는 함수입니다.
    private fun isIncomeEntity( // isIncomeEntity 함수를 선언함
        expense: com.ict.spentopia.data.local.ExpenseEntity // 소비 내역 값을 받음
    ): Boolean { // 이 블록 안의 내용이 시작됨
        return isIncomeCategory(expense.category) // 이 값을 함수 결과로 돌려줌
    }

    // ExpenseEntity 하나가 지출 항목인지 판별하는 함수입니다.
    // 수입이 아니면 모두 지출로 처리합니다.
    private fun isExpenseEntity( // isExpenseEntity 함수를 선언함
        expense: com.ict.spentopia.data.local.ExpenseEntity // 소비 내역 값을 받음
    ): Boolean { // 이 블록 안의 내용이 시작됨
        return !isIncomeEntity(expense) // 이 값을 함수 결과로 돌려줌
    }

    // 현재 연-월 문자열을 구하는 함수입니다.
    // 예: 2026-04
    private fun getCurrentYearMonth(): String { // 데이터를 불러오는 함수 시작
        val calendar = Calendar.getInstance() // calendar 값을 저장함
        val year = calendar.get(Calendar.YEAR) // year 값을 저장함
        val month = calendar.get(Calendar.MONTH) + 1 // month 값을 저장함
        return String.format("%04d-%02d", year, month) // 이 값을 함수 결과로 돌려줌
    }

    // AI 리포트 생성에 사용할 기간입니다.
    // 주간 선택 시 최근 7일, 월간 선택 시 이번 달 1일~오늘을 보냅니다.
    private fun createReportPeriodRange(period: String): Pair<String, String> { // 데이터를 저장하는 함수 시작
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US) // formatter 값을 저장함
        val end = Calendar.getInstance() // end 값을 저장함
        val start = Calendar.getInstance() // start 값을 저장함

        if (period == "주간") { // 조건이 맞는지 확인함
            start.add(Calendar.DAY_OF_MONTH, -6)
        } else { // 이 블록 안의 내용이 시작됨
            start.set(Calendar.DAY_OF_MONTH, 1)
        }

        return formatter.format(start.time) to formatter.format(end.time) // 이 값을 함수 결과로 돌려줌
    }

    private fun buildAnalyzeReportRequest(state: AnalysisUiState): AnalyzeReportRequest { // buildAnalyzeReportRequest 함수를 선언함
        val categoryData = state.categoryList.map { category -> // categoryData 값을 저장함
            AnalyzeCategoryDataRequest( // Analyze Category Data Request 함수를 실행함
                name = category.name, // name 값을 정해줌
                amount = category.amount.toFloat(), // 금액을 정해줌
                value = (category.ratio * 100f), // 입력값을 정해줌
                key = category.name // key 값을 정해줌
            )
        }

        return AnalyzeReportRequest( // 이 값을 함수 결과로 돌려줌
            transactions = latestExpenseOnlyList.map { expense -> // transactions 값을 정해줌
                AnalyzeTransactionRequest( // Analyze Transaction Request 함수를 실행함
                    date = expense.date, // 날짜를 정해줌
                    amount = expense.amount, // 금액을 정해줌
                    category = expense.category, // 카테고리를 정해줌
                    type = "expense" // type 값을 정해줌
                )
            },
            totalExpense = state.totalExpense.toFloat(), // 소비 내역 값을 정해줌
            budget = latestMonthlyBudget.toFloat(), // 예산 관련 값을 정해줌
            topCategory = state.topCategoryName, // topCategory 값을 정해줌
            topCategoryPercent = state.topCategoryRatio * 100f, // topCategoryPercent 값을 정해줌
            dailyAverage = state.averageDailyExpense.toFloat(), // dailyAverage 값을 정해줌
            expenseChangeRate = 0f, // 소비 내역 값을 정해줌
            budgetUsage = state.budgetUsageRate * 100f, // 예산 관련 값을 정해줌
            weeklyData = state.weeklyExpenseList.map { (day, amount) -> // weeklyData 값을 정해줌
                AnalyzeWeeklyDataRequest( // Analyze Weekly Data Request 함수를 실행함
                    day = day, // day 값을 day 값에 넣음
                    amount = amount.toFloat() // 금액을 정해줌
                )
            },
            monthlyData = state.monthlyExpenseList.map { (month, amount) -> // monthlyData 값을 정해줌
                AnalyzeMonthlyDataRequest( // Analyze Monthly Data Request 함수를 실행함
                    month = month, // month 값을 month 값에 넣음
                    amount = amount.toFloat() // 금액을 정해줌
                )
            },
            categoryData = categoryData // categoryData 값을 categoryData 값에 넣음
        )
    }

    private fun AiConsumptionReportUiModel.toReportText(): String { // AiConsumptionReportUiModel 함수를 선언함
        return listOf( // 이 값을 함수 결과로 돌려줌
            "좋은 점: $good",
            "주의: $warning",
            "조언: $advice",
            "예측: $prediction"
        ).joinToString("\n\n")
    }

    // 카테고리별 색상 반환 함수입니다.
    private fun getCategoryColor(category: String): Color { // 데이터를 불러오는 함수 시작
        return when (category) { // 이 값을 함수 결과로 돌려줌
            "식비" -> Color(0xFFFF7A00)
            "교통" -> Color(0xFF334155)
            "쇼핑" -> Color(0xFFE84AA8)
            "카페" -> Color(0xFF1E1B4B)
            "여가" -> Color(0xFF312E81)
            "생활비" -> Color(0xFF475569)
            else -> Color(0xFF6B7280) // 위 조건이 아니면 이쪽을 실행함
        }
    }

    // 주간 그래프 데이터 생성 함수입니다.
    // 월~일 순서로 요일별 합계를 만듭니다.
    private fun createWeeklyExpenseList( // 데이터를 저장하는 함수 시작
        expenseList: List<com.ict.spentopia.data.local.ExpenseEntity> // 소비 내역 값을 받음
    ): List<Pair<String, Int>> { // 이 블록 안의 내용이 시작됨

        // 요일별 기본값을 0으로 먼저 넣어둡니다.
        val dayMap = linkedMapOf( // dayMap 값을 저장함
            "월" to 0,
            "화" to 0,
            "수" to 0,
            "목" to 0,
            "금" to 0,
            "토" to 0,
            "일" to 0
        )

        expenseList.forEach { expense ->
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val parts = expense.date.split("-") // parts 값을 저장함
                val year = parts[0].toInt() // year 값을 저장함
                val month = parts[1].toInt() // month 값을 저장함
                val day = parts[2].toInt() // day 값을 저장함

                val calendar = Calendar.getInstance().apply { // calendar 값을 저장함
                    set(Calendar.YEAR, year) // set 함수를 실행함
                    set(Calendar.MONTH, month - 1) // set 함수를 실행함
                    set(Calendar.DAY_OF_MONTH, day) // set 함수를 실행함
                }

                val dayLabel = when (calendar.get(Calendar.DAY_OF_WEEK)) { // dayLabel 값을 저장함
                    Calendar.MONDAY -> "월"
                    Calendar.TUESDAY -> "화"
                    Calendar.WEDNESDAY -> "수"
                    Calendar.THURSDAY -> "목"
                    Calendar.FRIDAY -> "금"
                    Calendar.SATURDAY -> "토"
                    Calendar.SUNDAY -> "일"
                    else -> "" // 위 조건이 아니면 이쪽을 실행함
                }

                if (dayLabel.isNotBlank()) { // 조건이 맞는지 확인함
                    dayMap[dayLabel] = (dayMap[dayLabel] ?: 0) + expense.amount // dayMap[dayLabel] 값을 정해줌
                }
            } catch (_: Exception) { // 이 블록 안의 내용이 시작됨
                // 날짜 파싱 실패 시 그냥 넘어갑니다.
            }
        }

        return dayMap.toList() // 이 값을 함수 결과로 돌려줌
    }

    // 월간 그래프 데이터 생성 함수입니다.
    // 1월부터 12월까지 월별 소비 합계를 만듭니다.
    private fun createMonthlyExpenseList( // 데이터를 저장하는 함수 시작
        expenseList: List<com.ict.spentopia.data.local.ExpenseEntity> // 소비 내역 값을 받음
    ): List<Pair<String, Int>> { // 이 블록 안의 내용이 시작됨

        val monthMap = linkedMapOf<String, Int>().apply { // monthMap 값을 저장함
            for (month in 1..12) { // 목록을 하나씩 돌면서 실행함
                put("${month}월", 0) // put 함수를 실행함
            }
        }

        expenseList.forEach { expense ->
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val month = expense.date.split("-")[1].toInt() // month 값을 저장함
                val monthLabel = "${month}월" // monthLabel 값을 저장함

                if (month in 1..12) { // 조건이 맞는지 확인함
                    monthMap[monthLabel] = (monthMap[monthLabel] ?: 0) + expense.amount // monthMap[monthLabel] 값을 정해줌
                }
            } catch (_: Exception) { // 이 블록 안의 내용이 시작됨
                // 날짜 파싱 실패 시 그냥 넘어갑니다.
            }
        }

        return monthMap.toList() // 이 값을 함수 결과로 돌려줌
    }

    // 분석 팁 리스트 생성 함수입니다.
    // 아직 완전한 AI 분석은 아니지만,
    // 실제 금액/예산/최대 카테고리 기준 문구를 만듭니다.
    private fun createAnalysisTipList( // 데이터를 저장하는 함수 시작
        totalExpense: Int, // 소비 내역 값을 받음
        monthlyBudget: Long, // 예산 관련 값을 받음
        topCategory: CategorySpendUiModel? // topCategory 값을 받음
    ): List<AnalysisTipUiModel> { // 이 블록 안의 내용이 시작됨

        val result = mutableListOf<AnalysisTipUiModel>() // result 값을 저장함

        if (totalExpense == 0) { // 조건이 맞는지 확인함
            result.add(
                AnalysisTipUiModel( // Analysis Tip Ui Model 함수를 실행함
                    title = "기록을 시작해보세요", // 제목을 정해줌
                    description = "아직 이번 달 소비 기록이 없어요. Home 화면에서 소비를 입력하면 분석이 자동으로 시작돼요.", // description 값을 정해줌
                    emoji = "📝", // emoji 값을 정해줌
                    borderColor = Color(0xFFA7C7FF) // borderColor 값을 정해줌
                )
            )
            return result // 이 값을 함수 결과로 돌려줌
        }

        if (monthlyBudget > 0) { // 조건이 맞는지 확인함
            val usageRate = totalExpense.toFloat() / monthlyBudget.toFloat() // usageRate 값을 저장함

            if (usageRate < 0.5f) { // 조건이 맞는지 확인함
                result.add(
                    AnalysisTipUiModel( // Analysis Tip Ui Model 함수를 실행함
                        title = "좋은 흐름이에요", // 제목을 정해줌
                        description = "현재 예산의 절반 이하만 사용했어요. 지금의 소비 흐름을 유지하면 목표를 지키기 좋아요.", // description 값을 정해줌
                        emoji = "✅", // emoji 값을 정해줌
                        borderColor = Color(0xFFB7E4C7) // borderColor 값을 정해줌
                    )
                )
            } else if (usageRate < 1f) { // 이 블록 안의 내용이 시작됨
                result.add(
                    AnalysisTipUiModel( // Analysis Tip Ui Model 함수를 실행함
                        title = "예산 안에서 잘 쓰고 있어요", // 제목을 정해줌
                        description = "현재 예산 범위 안에서 소비 중이에요. 남은 기간 동안 큰 지출만 조심하면 좋아요.", // description 값을 정해줌
                        emoji = "💡", // emoji 값을 정해줌
                        borderColor = Color(0xFFD6C8FF) // borderColor 값을 정해줌
                    )
                )
            } else { // 이 블록 안의 내용이 시작됨
                result.add(
                    AnalysisTipUiModel( // Analysis Tip Ui Model 함수를 실행함
                        title = "예산 초과에 주의해요", // 제목을 정해줌
                        description = "이번 달 예산을 이미 넘겼어요. 고정 지출이 아닌 소비부터 먼저 줄여보는 게 좋아요.", // description 값을 정해줌
                        emoji = "⚠️", // emoji 값을 정해줌
                        borderColor = Color(0xFFFFD166) // borderColor 값을 정해줌
                    )
                )
            }
        }

        if (topCategory != null) { // 조건이 맞는지 확인함
            result.add(
                AnalysisTipUiModel( // Analysis Tip Ui Model 함수를 실행함
                    title = "가장 큰 지출 카테고리", // 제목을 정해줌
                    description = "이번 달에는 ${topCategory.name} 소비가 가장 컸어요. 전체 소비의 ${(topCategory.ratio * 100).toInt()}%를 차지하고 있어요.", // description 값을 정해줌
                    emoji = "📊", // emoji 값을 정해줌
                    borderColor = Color(0xFFA7C7FF) // borderColor 값을 정해줌
                )
            )
        }

        if (result.size < 3) { // 조건이 맞는지 확인함
            result.add(
                AnalysisTipUiModel( // Analysis Tip Ui Model 함수를 실행함
                    title = "소비 기록을 이어가보세요", // 제목을 정해줌
                    description = "기록이 쌓일수록 더 정확한 소비 패턴을 볼 수 있어요. 지금처럼 꾸준히 입력하는 게 가장 중요해요.", // description 값을 정해줌
                    emoji = "🌱", // emoji 값을 정해줌
                    borderColor = Color(0xFFD6C8FF) // borderColor 값을 정해줌
                )
            )
        }

        return result // 이 값을 함수 결과로 돌려줌
    }
}
