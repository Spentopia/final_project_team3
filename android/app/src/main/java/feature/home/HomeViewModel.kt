package com.ict.spentopia.feature.home // 이 파일이 속한 패키지 위치를 적음

// 일반 ViewModel import입니다.
import androidx.lifecycle.ViewModel // ViewModel 기능을 가져옴

// viewModelScope는 ViewModel 내부에서 코루틴을 실행할 때 사용합니다.
import androidx.lifecycle.viewModelScope // viewModelScope 기능을 가져옴

// 현재 프로젝트의 Room Entity import입니다.
import com.ict.spentopia.data.local.ExpenseEntity // ExpenseEntity 기능을 가져옴

// 백엔드 expenses 테이블에 기록을 저장하기 위한 API 모델입니다.
import com.ict.spentopia.data.remote.CreateExpenseRequest // CreateExpenseRequest 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴

// Repository import입니다.
import com.ict.spentopia.data.repository.ExpenseRepository // ExpenseRepository 기능을 가져옴

// Flow 관련 import입니다.
import kotlinx.coroutines.flow.MutableStateFlow // 바뀌는 상태값 도구를 가져옴
import kotlinx.coroutines.flow.SharingStarted // SharingStarted 기능을 가져옴
import kotlinx.coroutines.flow.StateFlow // 읽기 전용 상태값 도구를 가져옴
import kotlinx.coroutines.flow.flatMapLatest // flatMapLatest 기능을 가져옴
import kotlinx.coroutines.flow.map // map 기능을 가져옴
import kotlinx.coroutines.flow.stateIn // stateIn 기능을 가져옴

// 코루틴 실행을 위한 launch import입니다.
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import retrofit2.HttpException // 서버 오류 타입을 가져옴

// 날짜 계산을 위한 Calendar import입니다.
import java.util.Calendar // Calendar 기능을 가져옴

data class WeeklyScoreUiState( // WeeklyScoreUiState 데이터를 묶어둘 클래스 시작
    val totalScore: Int = 0, // totalScore 값을 저장함
    val recordDaysScore: Int = 0, // recordDaysScore 값을 저장함
    val receiptScore: Int = 0, // receiptScore 값을 저장함
    val diaryScore: Int = 0, // diaryScore 값을 저장함
    val budgetScore: Int = 0, // 예산 관련 값을 저장함
    val streakScore: Int = 0, // streakScore 값을 저장함
    val isLoading: Boolean = false, // 로딩 여부를 저장함
    val errorMessage: String = "" // 오류 내용을 저장함
)

// Home 화면에서 사용할 ViewModel 클래스입니다.
class HomeViewModel( // HomeViewModel 기능을 묶어둔 클래스 시작
    // Repository를 생성자로 주입받습니다.
    private val repository: ExpenseRepository // 데이터 처리 객체을 저장함
) : ViewModel() { // 이 블록 안의 내용이 시작됨

    private val _weeklyScoreState = MutableStateFlow(WeeklyScoreUiState(isLoading = true)) // 화면에서 바뀔 weeklyScoreState 값을 저장함
    val weeklyScoreState: StateFlow<WeeklyScoreUiState> = _weeklyScoreState // 화면에서 weeklyScoreState 값을 읽을 수 있게 열어둠

    init { // 이 블록 안의 내용이 시작됨
        loadWeeklyScore() // 데이터를 불러오는 함수를 실행함
    }

    fun loadWeeklyScore() { // 데이터를 불러오는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            _weeklyScoreState.value = _weeklyScoreState.value.copy( // weeklyScoreState.value 값을 정해줌
                isLoading = true, // true 값을 로딩 여부에 넣음
                errorMessage = "" // 오류 내용을 정해줌
            )

            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val score = RetrofitClient.rewardApi.getCurrentWeeklyScore() // score 값을 저장함
                _weeklyScoreState.value = WeeklyScoreUiState( // weeklyScoreState.value 값을 정해줌
                    totalScore = score.total_score ?: 0, // totalScore 값을 정해줌
                    recordDaysScore = score.record_days_score ?: 0, // recordDaysScore 값을 정해줌
                    receiptScore = score.receipt_score ?: 0, // receiptScore 값을 정해줌
                    diaryScore = score.diary_score ?: 0, // diaryScore 값을 정해줌
                    budgetScore = score.budget_score ?: 0, // 예산 관련 값을 정해줌
                    streakScore = score.streak_score ?: 0, // streakScore 값을 정해줌
                    isLoading = false, // false 값을 로딩 여부에 넣음
                    errorMessage = "" // 오류 내용을 정해줌
                )
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                _weeklyScoreState.value = WeeklyScoreUiState( // weeklyScoreState.value 값을 정해줌
                    isLoading = false, // false 값을 로딩 여부에 넣음
                    errorMessage = when (e.code()) { // 오류 내용을 정해줌
                        401 -> "로그인이 만료되었습니다."
                        404 -> "이번 달 성실도 데이터가 아직 없습니다."
                        else -> "성실도를 불러오지 못했습니다. (${e.code()})" // 위 조건이 아니면 이쪽을 실행함
                    }
                )
            } catch (_: Exception) { // 이 블록 안의 내용이 시작됨
                _weeklyScoreState.value = WeeklyScoreUiState( // weeklyScoreState.value 값을 정해줌
                    isLoading = false, // false 값을 로딩 여부에 넣음
                    errorMessage = "성실도를 불러오지 못했습니다." // 오류 내용을 정해줌
                )
            }
        }
    }

    // HomeViewModel은 화면이 바로 쓰기 쉬운 형태로 데이터를 가공합니다.
    // UI는 DB를 직접 다루지 않고, 여기서 정리된 StateFlow만 구독하면 됩니다.

    // -----------------------------------------
    // 1) 전체 소비 목록(Entity)
    // -----------------------------------------
    // DB 원본 데이터를 그대로 가지고 있는 목록입니다.
    val expenseList: StateFlow<List<ExpenseEntity>> = // 화면에서 소비 내역 값을 읽을 수 있게 열어둠
        repository
            .getAllExpenses()
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = emptyList() // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 2) 전체 소비 목록(UI용)
    // -----------------------------------------
    // 화면에서는 Entity 대신 UI용 모델을 사용해 렌더링합니다.
    val expenseUiList: StateFlow<List<ExpenseItemData>> = // 화면에서 소비 내역 값을 읽을 수 있게 열어둠
        repository
            .getAllExpenses()
            .map { entityList ->
                entityList.map { entity ->
                    ExpenseItemData( // Expense Item Data 함수를 실행함
                        id = entity.id, // 아이디를 정해줌
                        date = entity.date, // 날짜를 정해줌
                        title = entity.title, // 제목을 정해줌
                        category = entity.category, // 카테고리를 정해줌
                        amount = entity.amount, // 금액을 정해줌
                        memo = entity.memo, // 메모를 정해줌
                        // 영수증 이미지 Uri 문자열을 그대로 화면용 모델에 넣습니다.
                        receiptImageName = entity.receiptImageUri, // receiptImageName 값을 정해줌
                        diary = entity.diary, // diary 값을 정해줌
                        serverExpenseId = entity.serverExpenseId, // 소비 내역 값을 정해줌
                        receiptVerified = entity.receiptVerified // receiptVerified 값을 정해줌
                    )
                }
            }
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = emptyList() // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 3) 현재 선택된 연-월 상태
    // -----------------------------------------
    // 예: "2026-04"
    // Home 화면에서 어떤 월을 보고 있는지 관리하는 상태입니다.
    private val _selectedYearMonth = MutableStateFlow(getCurrentYearMonth()) // 화면에서 바뀔 selectedYearMonth 값을 저장함
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth // 화면에서 selectedYearMonth 값을 읽을 수 있게 열어둠

    // -----------------------------------------
    // 4) 현재 선택된 날짜 상태
    // -----------------------------------------
    // 예: "2026-04-15"
    // DailyExpenseCard에서 사용할 날짜입니다.
    private val _selectedDate = MutableStateFlow(getCurrentDate()) // 화면에서 바뀔 selectedDate 값을 저장함
    val selectedDate: StateFlow<String> = _selectedDate // 화면에서 selectedDate 값을 읽을 수 있게 열어둠

    // -----------------------------------------
    // 5) 현재 선택된 월의 소비 목록(Entity)
    // -----------------------------------------
    // 선택된 월이 바뀔 때마다 해당 월의 데이터만 다시 가져옵니다.
    val monthlyExpenseEntityList: StateFlow<List<ExpenseEntity>> = // 화면에서 소비 내역 값을 읽을 수 있게 열어둠
        selectedYearMonth
            .flatMapLatest { yearMonth ->
                repository.getExpensesByMonth(yearMonth)
            }
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = emptyList() // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 6) 현재 선택된 월의 소비 목록(UI용)
    // -----------------------------------------
    // 화면에서는 Entity보다 UI 모델을 쓰는 편이 더 편하기 때문에
    // 월별 데이터도 UI용 리스트로 변환해 둡니다.
    val monthlyExpenseUiList: StateFlow<List<ExpenseItemData>> = // 화면에서 소비 내역 값을 읽을 수 있게 열어둠
        monthlyExpenseEntityList
            .map { entityList ->
                entityList.map { entity ->
                    ExpenseItemData( // Expense Item Data 함수를 실행함
                        id = entity.id, // 아이디를 정해줌
                        date = entity.date, // 날짜를 정해줌
                        title = entity.title, // 제목을 정해줌
                        category = entity.category, // 카테고리를 정해줌
                        amount = entity.amount, // 금액을 정해줌
                        memo = entity.memo, // 메모를 정해줌
                        receiptImageName = entity.receiptImageUri, // receiptImageName 값을 정해줌
                        diary = entity.diary, // diary 값을 정해줌
                        serverExpenseId = entity.serverExpenseId, // 소비 내역 값을 정해줌
                        receiptVerified = entity.receiptVerified // receiptVerified 값을 정해줌
                    )
                }
            }
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = emptyList() // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 7) 현재 선택된 월의 총 소비 금액
    // -----------------------------------------
    // Analysis 화면에서도 재사용할 수 있도록
    // 월 총합 계산을 ViewModel에서 관리합니다.
    val monthlyTotalAmount: StateFlow<Int> = // 화면에서 monthlyTotalAmount 값을 읽을 수 있게 열어둠
        monthlyExpenseEntityList
            .map { monthlyList ->
                monthlyList.sumOf { expense ->
                    expense.amount
                }
            }
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = 0 // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 8) 현재 선택된 월의 소비 건수
    // -----------------------------------------
    // "이번 달 몇 건 기록했는지" 보여줄 때 사용합니다.
    val monthlyExpenseCount: StateFlow<Int> = // 화면에서 소비 내역 값을 읽을 수 있게 열어둠
        monthlyExpenseEntityList
            .map { monthlyList ->
                monthlyList.size
            }
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = 0 // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 9) 이전 달 총 소비 금액
    // -----------------------------------------
    // 현재 달과 비교하기 위해 지난달 금액도 미리 계산해둡니다.
    val previousMonthTotalAmount: StateFlow<Int> = // 화면에서 previousMonthTotalAmount 값을 읽을 수 있게 열어둠
        selectedYearMonth
            .flatMapLatest { currentYearMonth ->
                val previousYearMonth = moveYearMonthString( // previousYearMonth 값을 저장함
                    yearMonth = currentYearMonth, // currentYearMonth 값을 yearMonth 값에 넣음
                    delta = -1 // delta 값을 정해줌
                )
                repository.getExpensesByMonth(previousYearMonth)
            }
            .map { previousMonthList ->
                previousMonthList.sumOf { expense ->
                    expense.amount
                }
            }
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = 0 // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 10) 소비 기록이 있는 날짜 Set
    // -----------------------------------------
    // 달력 점 표시나 날짜 선택 상태 계산에 사용합니다.
    val expenseDateSet: StateFlow<Set<String>> = // 화면에서 소비 내역 값을 읽을 수 있게 열어둠
        repository
            .getAllExpenses()
            .map { expenseList ->
                expenseList.map { expense ->
                    expense.date
                }.toSet()
            }
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = emptySet() // initialValue 값을 정해줌
            )

    // -----------------------------------------
    // 11) 월 이동 - 이전 달
    // -----------------------------------------
    // 예: 2026-04 -> 2026-03
    // 월을 바꾸면 선택 날짜도 해당 월의 1일로 맞춰줍니다.
    fun moveToPreviousMonth() { // moveToPreviousMonth 함수를 선언함
        val previousYearMonth = moveYearMonthString( // previousYearMonth 값을 저장함
            yearMonth = _selectedYearMonth.value, // yearMonth 값을 정해줌
            delta = -1 // delta 값을 정해줌
        )

        _selectedYearMonth.value = previousYearMonth // selectedYearMonth.value 값을 정해줌
        _selectedDate.value = createFirstDateOfYearMonth(previousYearMonth) // selectedDate.value 값을 정해줌
    }

    // -----------------------------------------
    // 12) 월 이동 - 다음 달
    // -----------------------------------------
    // 예: 2026-04 -> 2026-05
    // 월을 바꾸면 선택 날짜도 해당 월의 1일로 맞춰줍니다.
    fun moveToNextMonth() { // moveToNextMonth 함수를 선언함
        val nextYearMonth = moveYearMonthString( // nextYearMonth 값을 저장함
            yearMonth = _selectedYearMonth.value, // yearMonth 값을 정해줌
            delta = 1 // delta 값을 정해줌
        )

        _selectedYearMonth.value = nextYearMonth // selectedYearMonth.value 값을 정해줌
        _selectedDate.value = createFirstDateOfYearMonth(nextYearMonth) // selectedDate.value 값을 정해줌
    }

    // -----------------------------------------
    // 13) 특정 날짜 선택
    // -----------------------------------------
    // 달력에서 날짜를 눌렀을 때 사용합니다.
    // 날짜를 바꾸면 연-월도 같이 맞춰줍니다.
    fun selectDate(date: String) { // selectDate 함수를 선언함
        _selectedDate.value = date // selectedDate.value 값을 정해줌
        _selectedYearMonth.value = date.substring(0, 7) // selectedYearMonth.value 값을 정해줌
    }

    // -----------------------------------------
    // 14) 특정 연-월 직접 설정
    // -----------------------------------------
    // 필요하면 외부에서 특정 월로 바로 이동할 수 있게 둡니다.
    fun setYearMonth(yearMonth: String) { // setYearMonth 함수를 선언함
        _selectedYearMonth.value = yearMonth // selectedYearMonth.value 값을 정해줌
        _selectedDate.value = createFirstDateOfYearMonth(yearMonth) // selectedDate.value 값을 정해줌
    }

    // -----------------------------------------
    // 15) 특정 월 소비 목록 조회 (기존 구조 유지)
    // -----------------------------------------
    // 기존 함수도 혹시 다른 곳에서 쓰고 있을 수 있어서 남겨둡니다.
    fun getExpensesByMonth(yearMonth: String): StateFlow<List<ExpenseEntity>> { // 데이터를 불러오는 함수 시작
        return repository // 이 값을 함수 결과로 돌려줌
            .getExpensesByMonth(yearMonth)
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = emptyList() // initialValue 값을 정해줌
            )
    }

    // -----------------------------------------
    // 16) 특정 날짜 소비 목록 조회 (기존 구조 유지)
    // -----------------------------------------
    // 기존 함수도 유지합니다.
    fun getExpensesByDate(date: String): StateFlow<List<ExpenseEntity>> { // 데이터를 불러오는 함수 시작
        return repository // 이 값을 함수 결과로 돌려줌
            .getExpensesByDate(date)
            .stateIn(
                scope = viewModelScope, // viewModelScope 값을 코루틴 실행 범위에 넣음
                started = SharingStarted.WhileSubscribed(5_000), // started 값을 정해줌
                initialValue = emptyList() // initialValue 값을 정해줌
            )
    }

    // -----------------------------------------
    // 17) 소비 추가
    // -----------------------------------------
    fun insertExpense( // 데이터를 저장하는 함수 시작
        expense: ExpenseEntity, // 소비 내역 값을 받음
        onSuccess: () -> Unit = {}, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit = {} // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            // 저장 순서:
            // 1) 서버 UUID가 없으면 먼저 백엔드에 기록을 만들고
            // 2) 그 UUID를 로컬 객체에 붙인 다음
            // 3) Room DB에 저장합니다.
            //
            // 왜 이렇게 하냐면 OCR 인증이나 서버 동기화는
            // "서버 기록의 id"를 기준으로 움직이기 때문입니다.
            val expenseForSave = if (expense.serverExpenseId.isBlank()) { // 소비 내역 값을 저장함
                try { // 오류가 날 수 있는 코드를 먼저 시도함
                    // 서버 /api/expenses 로 새 기록을 생성합니다.
                    // 이 요청은 access token이 있으면 AuthInterceptor가 자동으로 붙여줍니다.
                    val remoteExpense = RetrofitClient.expenseApi.createExpense( // 소비 내역 값을 저장함
                        CreateExpenseRequest( // 데이터를 저장하는 함수를 실행함
                            date = expense.date, // 날짜를 정해줌
                            amount = expense.amount, // 금액을 정해줌
                            category = expense.category, // 카테고리를 정해줌
                            memo = expense.memo.takeIf { it.isNotBlank() }, // 메모를 정해줌
                            transactionType = resolveTransactionType(expense.category), // transactionType 값을 정해줌
                            diary = expense.diary.takeIf { it.isNotBlank() } // diary 값을 정해줌
                        )
                    )

                    // 서버에서 받은 UUID와 영수증 인증 여부를
                    // 로컬 저장용 Entity에 다시 넣습니다.
                    expense.copy(
                        serverExpenseId = remoteExpense.id, // 소비 내역 값을 정해줌
                        receiptVerified = remoteExpense.receiptVerified ?: expense.receiptVerified // receiptVerified 값을 정해줌
                    )
                } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                    onError(e.message ?: "백엔드 DB에 기록을 저장하지 못했습니다.") // 실패했을 때 넘겨받은 함수를 실행함
                    return@launch
                }
            } else { // 이 블록 안의 내용이 시작됨
                expense
            }

            // 최종적으로 Android 로컬 DB(Room)에 저장합니다.
            repository.insertExpense(expenseForSave)

            // 화면 날짜를 저장한 날짜로 맞춰서
            // 달력, 목록, 월 합계가 같은 기준을 보게 합니다.
            selectDate(expenseForSave.date) // select Date 함수를 실행함

            // 기록이 추가되었으니 보상/성실도도 다시 불러옵니다.
            loadWeeklyScore() // 데이터를 불러오는 함수를 실행함

            // 저장 성공 콜백을 화면에 돌려줍니다.
            onSuccess() // 성공했을 때 넘겨받은 함수를 실행함
        }
    }

    // -----------------------------------------
    // 18) 소비 수정
    // -----------------------------------------
    fun updateExpense(expense: ExpenseEntity) { // 데이터를 수정하는 함수 시작
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            // 수정은 로컬 Room DB를 바로 갱신합니다.
            // 서버 수정 API는 아직 이 경로에서 따로 붙지 않으므로
            // 현재는 앱 내부 기록 중심으로 반영됩니다.
            repository.updateExpense(expense)

            // 수정된 날짜로 선택 상태도 같이 맞춥니다.
            selectDate(expense.date) // select Date 함수를 실행함
        }
    }

    // -----------------------------------------
    // 19) 소비 삭제
    // -----------------------------------------
    fun deleteExpenseById(id: Long, onSuccess: () -> Unit = {}) { // 삭제가 끝난 뒤 화면에서 완료 토스트를 띄울 수 있게 콜백을 받음
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            // 먼저 로컬 DB에서 삭제할 대상을 찾습니다.
            val expense = repository.getExpenseById(id) // 소비 내역 값을 저장함

            if (expense != null) { // 조건이 맞는지 확인함
                // 찾았으면 삭제합니다.
                repository.deleteExpense(expense)
                onSuccess() // 실제 삭제에 성공했을 때만 완료 안내를 실행함
            }
        }
    }

    // -----------------------------------------
    // 20) id로 단일 소비 조회
    // -----------------------------------------
    suspend fun getExpenseById(id: Long): ExpenseEntity? { // 데이터를 불러오는 함수 시작
        return repository.getExpenseById(id) // 이 값을 함수 결과로 돌려줌
    }

    // -----------------------------------------
    // 21) 오늘 날짜 yyyy-MM-dd 반환
    // -----------------------------------------
    private fun getCurrentDate(): String { // 데이터를 불러오는 함수 시작
        val calendar = Calendar.getInstance() // calendar 값을 저장함

        val year = calendar.get(Calendar.YEAR) // year 값을 저장함
        val month = calendar.get(Calendar.MONTH) + 1 // month 값을 저장함
        val day = calendar.get(Calendar.DAY_OF_MONTH) // day 값을 저장함

        return String.format("%04d-%02d-%02d", year, month, day) // 이 값을 함수 결과로 돌려줌
    }

    // -----------------------------------------
    // 22) 현재 연-월 yyyy-MM 반환
    // -----------------------------------------
    private fun getCurrentYearMonth(): String { // 데이터를 불러오는 함수 시작
        val calendar = Calendar.getInstance() // calendar 값을 저장함

        val year = calendar.get(Calendar.YEAR) // year 값을 저장함
        val month = calendar.get(Calendar.MONTH) + 1 // month 값을 저장함

        return String.format("%04d-%02d", year, month) // 이 값을 함수 결과로 돌려줌
    }

    // -----------------------------------------
    // 23) 연-월 문자열을 이전/다음 달로 이동
    // -----------------------------------------
    // 예:
    // 2026-01 + (-1) -> 2025-12
    // 2026-12 + (1) -> 2027-01
    private fun moveYearMonthString(yearMonth: String, delta: Int): String { // moveYearMonthString 함수를 선언함
        val year = yearMonth.substring(0, 4).toIntOrNull() ?: 0 // year 값을 저장함
        val month = yearMonth.substring(5, 7).toIntOrNull() ?: 1 // month 값을 저장함

        val calendar = Calendar.getInstance().apply { // calendar 값을 저장함
            set(Calendar.YEAR, year) // set 함수를 실행함
            set(Calendar.MONTH, month - 1) // set 함수를 실행함
            set(Calendar.DAY_OF_MONTH, 1) // set 함수를 실행함
            add(Calendar.MONTH, delta) // add 함수를 실행함
        }

        val movedYear = calendar.get(Calendar.YEAR) // movedYear 값을 저장함
        val movedMonth = calendar.get(Calendar.MONTH) + 1 // movedMonth 값을 저장함

        return String.format("%04d-%02d", movedYear, movedMonth) // 이 값을 함수 결과로 돌려줌
    }

    // 카테고리만 보고 백엔드에 보낼 수입/소비 타입을 정합니다.
    // HomeScreen의 수입 카테고리 목록과 같은 기준입니다.
    private fun resolveTransactionType(category: String): String { // resolveTransactionType 함수를 선언함
        // 수입으로 취급할 카테고리를 따로 묶어둡니다.
        val incomeCategories = setOf("월급", "용돈", "부수입", "환급") // incomeCategories 값을 저장함

        // 해당 카테고리면 income, 아니면 expense로 보냅니다.
        return if (incomeCategories.contains(category)) "income" else "expense" // 이 값을 함수 결과로 돌려줌
    }

    // -----------------------------------------
    // 24) 연-월 문자열을 해당 월의 1일 날짜로 변환
    // -----------------------------------------
    // 예: "2026-04" -> "2026-04-01"
    private fun createFirstDateOfYearMonth(yearMonth: String): String { // 데이터를 저장하는 함수 시작
        return "$yearMonth-01" // 이 값을 함수 결과로 돌려줌
    }
}
