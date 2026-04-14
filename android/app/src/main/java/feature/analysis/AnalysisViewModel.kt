package com.ict.spentopia.feature.analysis

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 카테고리별 지출 데이터 클래스
// 도넛 차트, 카테고리 상세 리스트에서 함께 사용
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

// AI 분석 카드 데이터 클래스
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

// 소비 패턴 진행률 데이터 클래스
data class PatternProgressUiModel(

    // 항목 이름
    val label: String,

    // 비율
    // 예: 0.75f = 75%
    val ratio: Float
)

// 분석 화면 전체 상태를 담는 데이터 클래스
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

// 분석 화면 ViewModel
class AnalysisViewModel : ViewModel() {

    // 내부에서 수정 가능한 상태
    private val _uiState = MutableStateFlow(createDummyUiState())

    // 외부에서는 읽기만 가능하도록 공개
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    // 주간 / 월간 선택 변경 함수
    fun selectPeriod(period: String) {
        _uiState.value = _uiState.value.copy(
            selectedPeriod = period
        )
    }

    // 현재 선택 상태에 따라 그래프 데이터를 반환하는 함수
    fun getCurrentTrendList(): List<Pair<String, Int>> {
        return if (_uiState.value.selectedPeriod == "주간") {
            _uiState.value.weeklyExpenseList
        } else {
            _uiState.value.monthlyExpenseList
        }
    }

    // 현재는 더미 데이터로 초기 상태를 생성
    // 나중에는 Room / DataStore 값을 읽어서 여기 대신 실제 계산 함수로 바꾸면 됨
    private fun createDummyUiState(): AnalysisUiState {

        // 카테고리별 지출 더미 데이터
        val categoryList = listOf(
            CategorySpendUiModel(
                name = "식비",
                amount = 135000,
                ratio = 0.45f,
                color = Color(0xFFFF7A00)
            ),
            CategorySpendUiModel(
                name = "교통",
                amount = 60000,
                ratio = 0.20f,
                color = Color(0xFF4D8DFF)
            ),
            CategorySpendUiModel(
                name = "쇼핑",
                amount = 45000,
                ratio = 0.15f,
                color = Color(0xFFE84AA8)
            ),
            CategorySpendUiModel(
                name = "여가",
                amount = 36000,
                ratio = 0.12f,
                color = Color(0xFFA14CFF)
            ),
            CategorySpendUiModel(
                name = "기타",
                amount = 24000,
                ratio = 0.08f,
                color = Color(0xFF6B7280)
            )
        )

        // AI 분석 카드 더미 데이터
        val tipList = listOf(
            AnalysisTipUiModel(
                title = "잘하고 있어요!",
                description = "식비 지출이 지난달 대비 15% 줄었어요. 지금의 절약 흐름을 유지해보세요.",
                emoji = "✅",
                borderColor = Color(0xFFB7E4C7)
            ),
            AnalysisTipUiModel(
                title = "절약 습관 형성",
                description = "대중교통 이용이 늘어나서 교통비 비율이 안정적이에요. 아주 좋은 변화예요.",
                emoji = "💡",
                borderColor = Color(0xFFD6C8FF)
            ),
            AnalysisTipUiModel(
                title = "주의가 필요해요",
                description = "여가/취미 지출이 예산 대비 높아요. 다음 주에는 소소한 소비를 조금 줄여보세요.",
                emoji = "⚠️",
                borderColor = Color(0xFFFFD166)
            ),
            AnalysisTipUiModel(
                title = "목표 달성 예상",
                description = "현재 소비 흐름이라면 이번 달 저축 목표를 무난하게 지킬 가능성이 높아요.",
                emoji = "📈",
                borderColor = Color(0xFFA7C7FF)
            )
        )

        // 시간대별 소비 패턴 더미 데이터
        val timePatternList = listOf(
            PatternProgressUiModel("오전 (06-12시)", 0.30f),
            PatternProgressUiModel("오후 (12-18시)", 0.50f),
            PatternProgressUiModel("저녁 (18-24시)", 0.20f)
        )

        // 결제 방법 더미 데이터
        val paymentPatternList = listOf(
            PatternProgressUiModel("카드", 0.75f),
            PatternProgressUiModel("현금", 0.15f),
            PatternProgressUiModel("기타", 0.10f)
        )

        // 최종 UI 상태 반환
        return AnalysisUiState(
            selectedPeriod = "주간",
            totalExpense = 300000,
            averageDailyExpense = 23571,
            budgetUsageRate = 0.60f,
            topCategoryName = "식비",
            topCategoryRatio = 0.45f,
            weeklyExpenseList = listOf(
                "월" to 15000,
                "화" to 8000,
                "수" to 22000,
                "목" to 12000,
                "금" to 35000,
                "토" to 45000,
                "일" to 28000
            ),
            monthlyExpenseList = listOf(
                "1주" to 65000,
                "2주" to 72000,
                "3주" to 94000,
                "4주" to 69000
            ),
            categoryList = categoryList,
            tipList = tipList,
            timePatternList = timePatternList,
            weekdayAverageText = "65,000원",
            weekendAverageText = "100,000원",
            weekendComment = "주말 소비가 54% 더 많아요",
            paymentPatternList = paymentPatternList
        )
    }
}