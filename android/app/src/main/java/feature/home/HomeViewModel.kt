package com.ict.spentopia.feature.home

// 일반 ViewModel import입니다.
import androidx.lifecycle.ViewModel

// viewModelScope는 ViewModel 내부에서 코루틴을 실행할 때 사용합니다.
import androidx.lifecycle.viewModelScope

// 현재 프로젝트의 Room Entity import입니다.
import com.ict.spentopia.data.local.ExpenseEntity

// Repository import입니다.
import com.ict.spentopia.data.repository.ExpenseRepository

// Flow 관련 import입니다.
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// 코루틴 실행을 위한 launch import입니다.
import kotlinx.coroutines.launch

// 날짜 계산을 위한 Calendar import입니다.
import java.util.Calendar

// Home 화면에서 사용할 ViewModel 클래스입니다.
class HomeViewModel(
    // Repository를 생성자로 주입받습니다.
    private val repository: ExpenseRepository
) : ViewModel() {

    // -----------------------------------------
    // 1) 전체 소비 목록(Entity)
    // -----------------------------------------
    // DB 원본 데이터를 그대로 가지고 있는 목록입니다.
    val expenseList: StateFlow<List<ExpenseEntity>> =
        repository
            .getAllExpenses()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // -----------------------------------------
    // 2) 전체 소비 목록(UI용)
    // -----------------------------------------
    // HomeScreen에서는 이 값을 바로 구독하면 됩니다.
    // 즉, Entity -> ExpenseItemData 변환을 ViewModel에서 미리 처리합니다.
    val expenseUiList: StateFlow<List<ExpenseItemData>> =
        repository
            .getAllExpenses()
            .map { entityList ->
                entityList.map { entity ->
                    ExpenseItemData(
                        id = entity.id,
                        date = entity.date,
                        title = entity.title,
                        category = entity.category,
                        amount = entity.amount,
                        memo = entity.memo,
                        // 영수증 이미지 Uri 문자열을 그대로 화면용 모델에 넣습니다.
                        receiptImageName = entity.receiptImageUri,
                        diary = entity.diary
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // -----------------------------------------
    // 3) 현재 선택된 연-월 상태
    // -----------------------------------------
    // 예: "2026-04"
    // Home 화면에서 어떤 월을 보고 있는지 관리하는 상태입니다.
    private val _selectedYearMonth = MutableStateFlow(getCurrentYearMonth())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth

    // -----------------------------------------
    // 4) 현재 선택된 날짜 상태
    // -----------------------------------------
    // 예: "2026-04-15"
    // DailyExpenseCard에서 사용할 날짜입니다.
    private val _selectedDate = MutableStateFlow(getCurrentDate())
    val selectedDate: StateFlow<String> = _selectedDate

    // -----------------------------------------
    // 5) 현재 선택된 월의 소비 목록(Entity)
    // -----------------------------------------
    // DAO의 getExpensesByMonth()를 그대로 사용해서
    // 현재 선택한 연-월의 소비 데이터만 가져옵니다.
    val monthlyExpenseEntityList: StateFlow<List<ExpenseEntity>> =
        selectedYearMonth
            .flatMapLatest { yearMonth ->
                repository.getExpensesByMonth(yearMonth)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // -----------------------------------------
    // 6) 현재 선택된 월의 소비 목록(UI용)
    // -----------------------------------------
    // 화면에서는 Entity보다 UI 모델을 쓰는 편이 더 편하기 때문에
    // 월별 데이터도 UI용 리스트로 변환해 둡니다.
    val monthlyExpenseUiList: StateFlow<List<ExpenseItemData>> =
        monthlyExpenseEntityList
            .map { entityList ->
                entityList.map { entity ->
                    ExpenseItemData(
                        id = entity.id,
                        date = entity.date,
                        title = entity.title,
                        category = entity.category,
                        amount = entity.amount,
                        memo = entity.memo,
                        receiptImageName = entity.receiptImageUri,
                        diary = entity.diary
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // -----------------------------------------
    // 7) 현재 선택된 월의 총 소비 금액
    // -----------------------------------------
    // Analysis 화면에서도 재사용할 수 있도록
    // 월 총합 계산을 ViewModel에서 관리합니다.
    val monthlyTotalAmount: StateFlow<Int> =
        monthlyExpenseEntityList
            .map { monthlyList ->
                monthlyList.sumOf { expense ->
                    expense.amount
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    // -----------------------------------------
    // 8) 현재 선택된 월의 소비 건수
    // -----------------------------------------
    // "이번 달 몇 건 기록했는지" 보여줄 때 사용합니다.
    val monthlyExpenseCount: StateFlow<Int> =
        monthlyExpenseEntityList
            .map { monthlyList ->
                monthlyList.size
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    // -----------------------------------------
    // 9) 이전 달 총 소비 금액
    // -----------------------------------------
    // 현재 달과 비교하기 위해 지난달 금액도 미리 계산해둡니다.
    val previousMonthTotalAmount: StateFlow<Int> =
        selectedYearMonth
            .flatMapLatest { currentYearMonth ->
                val previousYearMonth = moveYearMonthString(
                    yearMonth = currentYearMonth,
                    delta = -1
                )
                repository.getExpensesByMonth(previousYearMonth)
            }
            .map { previousMonthList ->
                previousMonthList.sumOf { expense ->
                    expense.amount
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    // -----------------------------------------
    // 10) 소비 기록이 있는 날짜 Set
    // -----------------------------------------
    // 달력 점 표시 등에 사용할 수 있도록 날짜 Set 형태로 만듭니다.
    val expenseDateSet: StateFlow<Set<String>> =
        repository
            .getAllExpenses()
            .map { expenseList ->
                expenseList.map { expense ->
                    expense.date
                }.toSet()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet()
            )

    // -----------------------------------------
    // 11) 월 이동 - 이전 달
    // -----------------------------------------
    // 예: 2026-04 -> 2026-03
    // 월을 바꾸면 선택 날짜도 해당 월의 1일로 맞춰줍니다.
    fun moveToPreviousMonth() {
        val previousYearMonth = moveYearMonthString(
            yearMonth = _selectedYearMonth.value,
            delta = -1
        )

        _selectedYearMonth.value = previousYearMonth
        _selectedDate.value = createFirstDateOfYearMonth(previousYearMonth)
    }

    // -----------------------------------------
    // 12) 월 이동 - 다음 달
    // -----------------------------------------
    // 예: 2026-04 -> 2026-05
    // 월을 바꾸면 선택 날짜도 해당 월의 1일로 맞춰줍니다.
    fun moveToNextMonth() {
        val nextYearMonth = moveYearMonthString(
            yearMonth = _selectedYearMonth.value,
            delta = 1
        )

        _selectedYearMonth.value = nextYearMonth
        _selectedDate.value = createFirstDateOfYearMonth(nextYearMonth)
    }

    // -----------------------------------------
    // 13) 특정 날짜 선택
    // -----------------------------------------
    // 달력에서 날짜를 눌렀을 때 사용합니다.
    // 날짜를 바꾸면 연-월도 같이 맞춰줍니다.
    fun selectDate(date: String) {
        _selectedDate.value = date
        _selectedYearMonth.value = date.substring(0, 7)
    }

    // -----------------------------------------
    // 14) 특정 연-월 직접 설정
    // -----------------------------------------
    // 필요하면 외부에서 특정 월로 바로 이동할 수 있게 둡니다.
    fun setYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
        _selectedDate.value = createFirstDateOfYearMonth(yearMonth)
    }

    // -----------------------------------------
    // 15) 특정 월 소비 목록 조회 (기존 구조 유지)
    // -----------------------------------------
    // 기존 함수도 혹시 다른 곳에서 쓰고 있을 수 있어서 남겨둡니다.
    fun getExpensesByMonth(yearMonth: String): StateFlow<List<ExpenseEntity>> {
        return repository
            .getExpensesByMonth(yearMonth)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    // -----------------------------------------
    // 16) 특정 날짜 소비 목록 조회 (기존 구조 유지)
    // -----------------------------------------
    // 기존 함수도 유지합니다.
    fun getExpensesByDate(date: String): StateFlow<List<ExpenseEntity>> {
        return repository
            .getExpensesByDate(date)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    // -----------------------------------------
    // 17) 소비 추가
    // -----------------------------------------
    fun insertExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.insertExpense(expense)

            // 저장한 소비 날짜 기준으로 현재 선택 날짜/월도 같이 맞춰줍니다.
            selectDate(expense.date)
        }
    }

    // -----------------------------------------
    // 18) 소비 수정
    // -----------------------------------------
    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)

            // 수정한 소비 날짜 기준으로 현재 선택 날짜/월도 같이 맞춰줍니다.
            selectDate(expense.date)
        }
    }

    // -----------------------------------------
    // 19) 소비 삭제
    // -----------------------------------------
    fun deleteExpenseById(id: Long) {
        viewModelScope.launch {
            val expense = repository.getExpenseById(id)

            if (expense != null) {
                repository.deleteExpense(expense)
            }
        }
    }

    // -----------------------------------------
    // 20) id로 단일 소비 조회
    // -----------------------------------------
    suspend fun getExpenseById(id: Long): ExpenseEntity? {
        return repository.getExpenseById(id)
    }

    // -----------------------------------------
    // 21) 오늘 날짜 yyyy-MM-dd 반환
    // -----------------------------------------
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return String.format("%04d-%02d-%02d", year, month, day)
    }

    // -----------------------------------------
    // 22) 현재 연-월 yyyy-MM 반환
    // -----------------------------------------
    private fun getCurrentYearMonth(): String {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        return String.format("%04d-%02d", year, month)
    }

    // -----------------------------------------
    // 23) 연-월 문자열을 이전/다음 달로 이동
    // -----------------------------------------
    // 예:
    // 2026-01 + (-1) -> 2025-12
    // 2026-12 + (1) -> 2027-01
    private fun moveYearMonthString(yearMonth: String, delta: Int): String {
        val year = yearMonth.substring(0, 4).toIntOrNull() ?: 0
        val month = yearMonth.substring(5, 7).toIntOrNull() ?: 1

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }

        val movedYear = calendar.get(Calendar.YEAR)
        val movedMonth = calendar.get(Calendar.MONTH) + 1

        return String.format("%04d-%02d", movedYear, movedMonth)
    }

    // -----------------------------------------
    // 24) 연-월 문자열을 해당 월의 1일 날짜로 변환
    // -----------------------------------------
    // 예: "2026-04" -> "2026-04-01"
    private fun createFirstDateOfYearMonth(yearMonth: String): String {
        return "$yearMonth-01"
    }
}