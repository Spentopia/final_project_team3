package com.ict.spentopia.feature.home // 이 파일이 속한 패키지 위치를 적음

// 날짜 선택 다이얼로그를 위한 import
import android.app.DatePickerDialog // 날짜 선택창 기능을 가져옴
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri // 이미지 주소 같은 Uri 타입을 가져옴
import android.widget.Toast

// Activity Result 관련 import
import androidx.activity.compose.rememberLauncherForActivityResult // 외부 앱 결과를 받는 도구를 가져옴
import androidx.activity.result.contract.ActivityResultContracts // 갤러리 열기 같은 실행 규칙을 가져옴

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

// Compose foundation 관련 import
import androidx.compose.foundation.BorderStroke // 테두리 선 스타일을 가져옴
import androidx.compose.foundation.Image // 이미지 표시 컴포넌트를 가져옴
import androidx.compose.foundation.background // 배경을 꾸미는 기능을 가져옴
import androidx.compose.foundation.border // 테두리를 그리는 기능을 가져옴
import androidx.compose.foundation.clickable // 클릭 가능하게 만드는 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // 가로세로 배치 간격 설정 기능을 가져옴
import androidx.compose.foundation.layout.Box // Box 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.PaddingValues // 패딩 값을 묶는 타입을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // 빈 공간을 넣는 컴포넌트를 가져옴
import androidx.compose.foundation.layout.fillMaxSize // 부모 크기를 꽉 채우는 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // 가로를 꽉 채우는 기능을 가져옴
import androidx.compose.foundation.layout.height // 높이 지정 기능을 가져옴
import androidx.compose.foundation.layout.padding // 여백 주는 기능을 가져옴
import androidx.compose.foundation.layout.size // 크기 지정 기능을 가져옴
import androidx.compose.foundation.layout.width // 너비 지정 기능을 가져옴
import androidx.compose.foundation.lazy.LazyColumn // 세로 스크롤 리스트를 가져옴
import androidx.compose.foundation.shape.CircleShape // 원 모양을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // 둥근 모서리 모양을 가져옴

// 아이콘 관련 import
import androidx.compose.material.icons.Icons // 아이콘 묶음을 가져옴
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth // 달력 아이콘을 가져옴
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart

// Material3 관련 import
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // 버튼 기본 스타일 도구를 가져옴
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card // 카드 UI를 가져옴
import androidx.compose.material3.CardDefaults // 카드 기본 스타일 도구를 가져옴
import androidx.compose.material3.DropdownMenu // 드롭다운 메뉴를 가져옴
import androidx.compose.material3.DropdownMenuItem // 드롭다운 메뉴 한 줄을 가져옴
import androidx.compose.material3.ExperimentalMaterial3Api // 실험용 Material3 기능 표시를 가져옴
import androidx.compose.material3.ExposedDropdownMenuBox // 펼침형 드롭다운 박스를 가져옴
import androidx.compose.material3.ExposedDropdownMenuDefaults // 드롭다운 기본 아이콘을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.OutlinedButton // 외곽선 버튼을 가져옴
import androidx.compose.material3.OutlinedTextField // 외곽선 입력칸을 가져옴
import androidx.compose.material3.OutlinedTextFieldDefaults // 입력칸 기본 스타일 도구를 가져옴
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.TextButton // 글자형 버튼을 가져옴

// Compose runtime 관련 import
import androidx.compose.runtime.Composable // Compose 함수 표시용 어노테이션을 가져옴
import androidx.compose.runtime.LaunchedEffect // 상태가 바뀔 때 실행할 효과를 가져옴
import androidx.compose.runtime.getValue // by 문법으로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 재구성돼도 값을 기억하게 해줌
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue // by 문법으로 상태를 바꾸게 해줌

// UI 관련 import
import androidx.compose.ui.Alignment // 정렬 기준을 가져옴
import androidx.compose.ui.Modifier // UI 크기·색·여백 설정 도구를 가져옴
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush // 그라데이션 같은 색칠 도구를 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.layout.ContentScale // 이미지 채우는 방식을 가져옴
import androidx.compose.ui.platform.LocalContext // 현재 화면 Context를 가져오는 도구를 가져옴
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight // 글자 두께 설정을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.compose.ui.window.Dialog // 팝업 창 컴포넌트를 가져옴

// Coil 이미지 로딩 관련 import입니
import coil.compose.rememberAsyncImagePainter // Uri 이미지를 그리는 도구를 가져옴

// ViewModel을 Compose에서 사용하기 위한 import
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel을 연결하는 도구를 가져옴

// Flow를 Compose 상태로 안전하게 수집하기 위한 import
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Flow 값을 안전하게 화면 상태로 받는 도구를 가져옴

// BudgetViewModel import
import com.ict.spentopia.feature.budget.BudgetViewModel // 예산 화면용 ViewModel을 가져옴

// 지갑 선택 다이얼로그 관련 import입
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog // 솔라나 지갑 선택 다이얼로그를 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // 선택한 솔라나 지갑 종류를 가져옴

// Room Entity import
import com.ict.spentopia.data.local.ExpenseEntity // DB에 저장되는 소비 데이터 타입을 가져옴
import com.ict.spentopia.data.remote.CreateExpenseRequest
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.R
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavy
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import com.ict.spentopia.ui.theme.spentopiaAppButtonColor
import com.ict.spentopia.ui.theme.spentopiaAppButtonContentColor

// 숫자 포맷 및 날짜 계산 관련 import
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat // 숫자를 쉼표 형식으로 바꾸는 도구를 가져옴
import java.util.Calendar // 날짜 계산용 객체를 가져옴
import kotlin.math.abs // 절댓값 함수 가져옴

@Composable //이  함수가 jetpack Compose 의 Composavle 함수임 표시 // 즉, Compose UI 상태나 테마 등에 접근 가능
private fun isHomeDarkTheme(): Boolean { // 다크 테마인지 true/false로 반환
    return MaterialTheme.colorScheme.background == SpentopiaDarkBackground
}

@Composable
private fun homeSoftCardColor(): Color {
    return if (isHomeDarkTheme()) Color(0xFF111A2A) else Color(0xFFF7FBFF)
}

@Composable
private fun homeSoftCardBorderColor(): Color {
    return if (isHomeDarkTheme()) Color(0xFF8B5CF6).copy(alpha = 0.45f) else Color(0xFF7DD3FC)
}

@Composable
private fun homeStatCardColor(): Color {
    return if (isHomeDarkTheme()) Color(0xFF1A2233) else Color(0xFFF7FBFF)
}

@Composable
private fun homeInputFieldColor(): Color {
    return if (isHomeDarkTheme()) Color(0xFF1A2A3D) else MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun homeInputBorderColor(): Color {
    return if (isHomeDarkTheme()) Color(0xFF8B5CF6).copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
}

@Composable
private fun homeDarkActionSurfaceColor(): Color {
    return if (isHomeDarkTheme()) Color(0xFF1A2233) else MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun homePrimaryButtonColor(): Color {
    return spentopiaAppButtonColor(isHomeDarkTheme())
}

@Composable
private fun homePrimaryButtonContentColor(): Color {
    return spentopiaAppButtonContentColor(isHomeDarkTheme())
}

// --------------------------------------------------
// UI에서 사용할 소비 항목 데이터 클래스입니다.
// 기존 HomeScreen UI 구조를 최대한 유지하기 위해 남겨둡니다.
// 즉, DB의 ExpenseEntity를 바로 화면에 쓰지 않고,
// 화면에서 쓰기 좋은 ExpenseItemData로 변환해서 사용합니다.
// --------------------------------------------------
data class ExpenseItemData( // ExpenseItemData 데이터를 묶어둘 클래스 시작
    val id: Long, // 이 데이터에 저장할 id 값을 받음
    val date: String, // 이 데이터에 저장할 date 값을 받음
    val title: String, // 이 데이터에 저장할 title 값을 받음
    val category: String, // 이 데이터에 저장할 category 값을 받음
    val amount: Int, // 이 데이터에 저장할 amount 값을 받음
    val memo: String, // 이 데이터에 저장할 memo 값을 받음
    val receiptImageName: String, // 이 데이터에 저장할 receiptImageName 값을 받음
    val diary: String, // 이 데이터에 저장할 diary 값을 받음
    val serverExpenseId: String = "", // 백엔드 expenses 테이블의 UUID입니다. OCR 인증 때 다시 사용
    val receiptVerified: Boolean = false // 서버에서 영수증 인증이 성공했는지 저장
)

// 달력에서 사용할 날짜 데이터 클래스입니다.
data class CalendarDateData( // CalendarDateData 데이터를 묶어둘 클래스 시작
    val fullDate: String, // 이 데이터에 저장할 fullDate 값을 받음
    val dayText: String, // 이 데이터에 저장할 dayText 값을 받음
    val isCurrentMonth: Boolean // 이 데이터에 저장할 isCurrentMonth 값을 받음
)

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun HomeScreen( // HomeScreen 함수 선언 시작
    isWalletConnected: Boolean = false, // 지갑 연결 여부를 받음
    walletAddress: String = "", // 연결된 지갑 주소를 받음
    walletProvider: String = "", //연결된 지갑 주소 이름
    onWalletDisconnectClick: () -> Unit = {}, // 지갑 연결 해제 버튼함수
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}, // 지갑 연결 버튼을 눌러 선택한 지갑 종류를 넘기는 함수
    onLedgerClick: () -> Unit = {}, // Unit 값을 이 함수로 넘김
    onMyPageClick: () -> Unit = {}, // Unit 값을 이 함수로 넘김
    onBudgetClick: () -> Unit = {}, // Unit 값을 이 함수로 넘김
    onAnalysisClick: () -> Unit = {}, // Unit 값을 이 함수로 넘김
    onAvatarClick: () -> Unit = {}, // Unit 값을 이 함수로 넘김
    onMarketClick: () -> Unit = {}, // Unit 값을 이 함수로 넘김
    onPlazaClick: () -> Unit = {}, // Unit 값을 이 함수로 넘김
    onCommunityClick: () -> Unit = {} // 버튼을 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    // 예산 설정 화면에서 저장한 값을 읽어오기 위한 ViewModel입니다.
    val budgetViewModel: BudgetViewModel = viewModel() // 예산 화면의 ViewModel을 연결함

    // 홈 화면에서 Room 소비 데이터를 가져오기 위한 ViewModel입니다.
    val context = LocalContext.current // 현재 화면의 Context를 가져옴

    val homeViewModel: HomeViewModel = viewModel( // 이 데이터에 저장할 homeViewModel 값을 받음
        factory = object : androidx.lifecycle.ViewModelProvider.Factory { // ViewModel을 직접 만드는 규칙을 여기서 정의함
            @Suppress("UNCHECKED_CAST") // 경고 메시지를 잠시 숨김
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T { // ViewModel을 실제로 만드는 함수를 새로 정의함
                val database = com.ict.spentopia.data.local.ExpenseDatabase.getDatabase(context) // 앱의 Room 데이터베이스를 가져옴
                val repository = com.ict.spentopia.data.repository.ExpenseRepository( // DB 접근을 맡길 Repository를 만듦
                    database.expenseDao() // Unit 값을 이 함수로 넘김
                )
                return HomeViewModel(repository) as T // 계산한 결과를 바깥으로 돌려줌
            } // 블록 끝
        } // 블록 끝
    )

    // 저장된 예산 상태를 lifecycle-aware 방식으로 구독합니다.
    val budgetState by budgetViewModel.budgetState.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음

    // HomeViewModel의 전체 UI용 소비 목록을 구독합니다.
    // DailyExpenseCard는 "선택한 날짜" 기준으로 여기서 다시 필터링합니다.
    val expenseList by homeViewModel.expenseUiList.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음

    // HomeViewModel에서 현재 선택한 연-월을 구독합니다.
    // 예: "2026-04"
    val selectedYearMonth by homeViewModel.selectedYearMonth.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음

    // HomeViewModel에서 현재 선택한 날짜를 구독합니다.
    // 예: "2026-04-15"
    val selectedDate by homeViewModel.selectedDate.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음

    // HomeViewModel에서 현재 선택한 월의 총 소비 금액을 구독합니다.
    val currentMonthTotalExpense by homeViewModel.monthlyTotalAmount.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음

    // HomeViewModel에서 현재 선택한 월의 소비 건수를 구독합니다.
    val currentMonthExpenseCount by homeViewModel.monthlyExpenseCount.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음

    // HomeViewModel에서 이전 달 총 소비 금액을 구독합니다.
    val previousMonthTotalExpense by homeViewModel.previousMonthTotalAmount.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음

    // 소비 기록이 존재하는 날짜 목록을 Set으로 관리합니다.
    val expenseDateSet by homeViewModel.expenseDateSet.collectAsStateWithLifecycle() // Flow 값을 화면에서 바로 쓸 수 있는 상태로 받음
    val weeklyScoreState by homeViewModel.weeklyScoreState.collectAsStateWithLifecycle()

    // 현재 선택된 연-월 문자열에서 연도와 월을 분리합니다.
    // selectedYearMonth는 "yyyy-MM" 형식이므로 substring으로 안전하게 꺼냅니다.
    val currentYear = selectedYearMonth.substring(0, 4).toIntOrNull() // 문자열의 일부만 잘라냄
        ?: Calendar.getInstance().get(Calendar.YEAR) // 현재 날짜/시간 정보를 가진 Calendar 객체를 만듦
    val currentMonth = selectedYearMonth.substring(5, 7).toIntOrNull() // 문자열의 일부만 잘라냄
        ?: (Calendar.getInstance().get(Calendar.MONTH) + 1) // 현재 날짜/시간 정보를 가진 Calendar 객체를 만듦

    // 수정 중인 소비 항목 상태입니다.
    var editingExpense by remember { mutableStateOf<ExpenseItemData?>(null) } // 화면이 다시 그려져도 유지되는 상태값을 만듦
    var showWalletDisconnectDialog by remember { mutableStateOf(false) } // 지갑 연결 해제 팝업창을 띄울지 말지 결정하는 스위치
    var showWalletDialog by remember { mutableStateOf(false) } // 지갑 선택 팝업창을 띄울지 말지 결정하는 스위치
    var showWeeklyScoreDialog by remember { mutableStateOf(false) }
    // (화면이 새로고침되어도 상태 유지)

    // 월 달력 팝업 표시 여부 상태입니다.
    var showCalendarDialog by remember { mutableStateOf(false) } // 화면이 다시 그려져도 유지되는 상태값을 만듦

    // 현재 선택된 달의 입력된 수입 금액을 계산합니다.
    // 예산 설정의 기본 월 수입과 별개로, 홈에서 수입 입력한 금액을 수입 카드에 함께 반영합니다.
    val fixedCurrentMonthInputIncome = remember(expenseList, selectedYearMonth) { // 이 블록의 내용이 여기서 시작됨
        expenseList
            .filter { it.date.startsWith(selectedYearMonth) }
            .filter { isIncomeItem(it) }
            .sumOf { it.amount }
    } // 블록 끝

    // 이번 달 총 수입입니다.
    // 예산 설정의 월 수입 + 이번 달 수입 입력 금액을 합쳐서 보여줍니다.
    val fixedCurrentMonthTotalIncome = remember(budgetState, fixedCurrentMonthInputIncome) { // 이 블록의 내용이 여기서 시작됨
        budgetState.monthlyIncome + fixedCurrentMonthInputIncome // 기본 월 수입과 추가 입력 수입을 합산함
    } // 블록 끝

    // 예산 설정 화면에서 저장한 저축 목표와 이번 달 총 수입으로 월 예산을 계산합니다.
    // "이번 달 총 수입 - 저축 목표"를 실제 사용 가능한 한 달 예산으로 사용합니다.
    val monthlyBudget = remember(fixedCurrentMonthTotalIncome, budgetState) { // 이 블록의 내용이 여기서 시작됨
        fixedCurrentMonthTotalIncome - budgetState.savingGoal // 이번 달 총 수입에서 저축 목표를 뺌
    } // 블록 끝

    // 오늘 날짜를 yyyy-MM-dd 형식으로 생성합니다.
    val today = remember { // 처음 계산한 값을 기억해둠
        val cal = Calendar.getInstance() // 현재 날짜/시간 정보를 가진 Calendar 객체를 만듦
        formatDate( // monthlyBudget 값을 이 함수로 넘김
            cal.get(Calendar.YEAR), // monthlyBudget 값을 이 함수로 넘김
            cal.get(Calendar.MONTH) + 1, // 바로 앞 설정을 이어서 적음
            cal.get(Calendar.DAY_OF_MONTH) // 바로 앞 설정을 이어서 적음
        )
    } // 블록 끝

    // 현재 달의 남은 예산 계산입니다.
// 예산보다 소비가 많으면 음수가 될 수 있습니다.

// 현재 선택된 달의 소비만 다시 계산합니다.
// 수입 같은 항목은 제외하고 소비 항목만 합산합니다.
    val fixedCurrentMonthTotalExpense = remember(expenseList, selectedYearMonth) { // 이 블록의 내용이 여기서 시작됨
        expenseList
            .filter { it.date.startsWith(selectedYearMonth) }
            .filter { isExpenseItem(it) }
            .sumOf { it.amount }
    } // 블록 끝

    // 현재 선택된 달의 소비 건수만 다시 계산합니다.
// 수입 항목은 홈 상단의 소비 건수에 포함하지 않습니다.
    val fixedCurrentMonthExpenseCount = remember(expenseList, selectedYearMonth) { // 이 블록의 내용이 여기서 시작됨
        expenseList
            .filter { it.date.startsWith(selectedYearMonth) }
            .count { isExpenseItem(it) }
    } // 블록 끝

    // 지난달 대비 계산에서도 수입 항목을 제외하기 위해 이전 달 소비 금액을 다시 계산합니다.
// ViewModel의 previousMonthTotalExpense 값에 수입이 섞여 있을 수 있어서 홈 화면에서 한 번 더 안전하게 필터링합니다.
    val fixedPreviousMonthTotalExpense = remember(expenseList, selectedYearMonth) { // 이 블록의 내용이 여기서 시작됨
        val selectedYear = selectedYearMonth.substring(0, 4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR) // 선택 연도를 숫자로 바꿈
        val selectedMonth = selectedYearMonth.substring(5, 7).toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1) // 선택 월을 숫자로 바꿈
        val previousCalendar = Calendar.getInstance() // 이전 달 계산을 위한 Calendar 객체를 만듦
        previousCalendar.set(selectedYear, selectedMonth - 1, 1) // 선택된 달의 1일로 맞춤
        previousCalendar.add(Calendar.MONTH, -1) // 한 달 전으로 이동함
        val previousYearMonth = "%04d-%02d".format( // yyyy-MM 형식 문자열을 만듦
            previousCalendar.get(Calendar.YEAR), // 이전 달의 연도를 가져옴
            previousCalendar.get(Calendar.MONTH) + 1 // 이전 달의 월을 가져옴
        )

        expenseList
            .filter { it.date.startsWith(previousYearMonth) }
            .filter { isExpenseItem(it) }
            .sumOf { it.amount }
    } // 블록 끝

    val remainingBudget = monthlyBudget - fixedCurrentMonthTotalExpense // remainingBudget 값을 계산해서 저장함

// 사용률 텍스트 계산입니다.
// 기존에는 Int로 바로 잘라서 0.2% 같은 값이 0%가 되는 문제가 있었기 때문에
// 이제는 문자열로 만들어서 0.3% 같은 값도 보이게 처리합니다.
    val usageRateText = remember(fixedCurrentMonthTotalExpense, monthlyBudget) { // 이 블록의 내용이 여기서 시작됨
        createUsageRateText( // 글자를 화면에 보여주기 시작함
            currentAmount = fixedCurrentMonthTotalExpense, // currentAmount 값을 이 함수로 넘김
            monthlyBudget = monthlyBudget // monthlyBudget 값을 이 함수로 넘김
        )
    } // 블록 끝

    // 지난달 대비 증감 문구 계산입니다.
    val changeRateText = remember(fixedCurrentMonthTotalExpense, fixedPreviousMonthTotalExpense) { // 이 블록의 내용이 여기서 시작됨
        createChangeRateText( // 글자를 화면에 보여주기 시작함
            currentAmount = fixedCurrentMonthTotalExpense, // 수입을 제외한 현재 달 소비 금액을 넘김
            previousAmount = fixedPreviousMonthTotalExpense // 수입을 제외한 지난달 소비 금액을 넘김
        )
    } // 블록 끝

    // 드로어는 AppNavGraph에서 처리하므로 HomeScreen에서는 본문만 그립니다.
    LazyColumn( // 세로로 스크롤되는 목록 UI를 시작함
        modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
            .fillMaxSize() // 부모가 허용하는 공간을 전부 채움
            .background(MaterialTheme.colorScheme.background) // 배경색이나 그라데이션을 넣음
            .padding(horizontal = 16.dp), // 안쪽이나 바깥 여백을 줌
        verticalArrangement = Arrangement.spacedBy(16.dp) // 바로 앞 설정을 이어서 적음
    ) { // 이 블록 안의 내용이 시작됨
        item { Spacer(modifier = Modifier.height(12.dp)) } // 리스트 안에 들어갈 한 칸을 시작함

        item { // 리스트 안에 들어갈 한 칸을 시작함
            TopHeaderSection( // 바로 앞 설정을 이어서 적음
                isWalletConnected = isWalletConnected, // 지갑 연결 여부를 넘김
                walletAddress = walletAddress, // 지갑 주소를 넘김
                walletProvider = walletProvider, // 지갑 제공자 이름을 넘김
                onWalletConnectClick = { // verticalArrangement 값을 이 함수로 넘김
                    if (isWalletConnected) {
                        showWalletDisconnectDialog = true
                    } else {
                        showWalletDialog = true
                    }
                }
            )
        } // 블록 끝

        item { // 리스트 안에 들어갈 한 칸을 시작함
            MonthlySummaryCard( // 카드 모양 UI를 시작함
                currentYear = currentYear, // verticalArrangement 값을 이 함수로 넘김
                currentMonth = currentMonth, // currentMonth 값을 이 함수로 넘김
                currentMonthTotalExpense = fixedCurrentMonthTotalExpense, // 수입을 제외한 현재 달 소비 금액을 넘김
                currentMonthExpenseCount = fixedCurrentMonthExpenseCount, // 수입을 제외한 현재 달 소비 건수를 넘김
                monthlyBudget = monthlyBudget, // monthlyBudget 값을 이 함수로 넘김
                monthlyIncome = fixedCurrentMonthTotalIncome, // 기본 월 수입과 이번 달 입력 수입을 합친 값을 넘김
                remainingBudget = remainingBudget, // remainingBudget 값을 이 함수로 넘김
                usageRateText = usageRateText, // usageRateText 값을 이 함수로 넘김
                changeRateText = changeRateText, // changeRateText 값을 이 함수로 넘김
                onPrevMonth = { // 이 이벤트가 일어났을 때 실행할 코드를 시작함
                    // 이전 달로 이동합니다.
                    // 선택 날짜도 자동으로 해당 월 1일로 맞춰집니다.
                    homeViewModel.moveToPreviousMonth() // onPrevMonth 값을 이 함수로 넘김
                },
                onNextMonth = { // 이 이벤트가 일어났을 때 실행할 코드를 시작함
                    // 다음 달로 이동합니다.
                    // 선택 날짜도 자동으로 해당 월 1일로 맞춰집니다.
                    homeViewModel.moveToNextMonth() // onNextMonth 값을 이 함수로 넘김
                },
                onCalendarClick = { // 이 이벤트가 일어났을 때 실행할 코드를 시작함
                    // 월 표시 영역의 달력 버튼을 누르면 팝업 달력을 엽니다.
                    showCalendarDialog = true // onCalendarClick 값을 이 함수로 넘김
                } // 블록 끝
            )
        }

        item { // 리스트 안에 들어갈 한 칸을 시작함
            DailyExpenseCard( // 카드 모양 UI를 시작함
                expenseList = expenseList, // onCalendarClick 값을 이 함수로 넘김
                selectedDate = selectedDate, // selectedDate 값을 이 함수로 넘김
                onEditExpense = { expense -> // onEditExpense 값을 이 함수로 넘김
                    // 수정 버튼을 누르면 수정 모드로 바꾸고,
                    // 선택 날짜도 해당 항목 날짜로 맞춰줍니다.
                    editingExpense = expense // 바로 앞 설정을 이어서 적음
                    homeViewModel.selectDate(expense.date) // editingExpense 값을 이 함수로 넘김
                },
                onDeleteExpense = { expenseId -> // 바로 앞 설정을 이어서 적음
                    // 실제 Room DB에서 삭제합니다.
                    homeViewModel.deleteExpenseById(expenseId) // 바로 앞 설정을 이어서 적음

                    // 삭제한 항목이 수정 중이던 항목이면 수정 상태도 비웁니다.
                    if (editingExpense?.id == expenseId) { // 조건이 참일 때만 아래 코드를 실행함
                        editingExpense = null // 바로 앞 설정을 이어서 적음
                    } // 블록 끝
                } // 블록 끝
            )
        } // 블록 끝

        item { // 리스트 안에 들어갈 한 칸을 시작함
            WeeklyScoreCard( // 카드 모양 UI를 시작함
                scoreState = weeklyScoreState,
                onClick = {
                    showWeeklyScoreDialog = true
                    homeViewModel.loadWeeklyScore()
                }
            )
        } // 블록 끝

        item { // 리스트 안에 들어갈 한 칸을 시작함
            ExpenseWriteCard( // 카드 모양 UI를 시작함
                selectedDate = selectedDate, // onDeleteExpense 값을 이 함수로 넘김
                editingExpense = editingExpense, // editingExpense 값을 이 함수로 넘김
                onSaveExpense = { savedExpense -> // onSaveExpense 값을 이 함수로 넘김
                    // 화면용 모델을 DB용 Entity로 변환합니다.
                    val entity = savedExpense.toEntity() // entity 값을 계산해서 저장함

                    // 수정 중이 아니면 insert, 수정 중이면 update를 호출합니다.
                    if (editingExpense == null) { // 조건이 참일 때만 아래 코드를 실행함
                        homeViewModel.insertExpense(
                            expense = entity,
                            onSuccess = {
                                editingExpense = null
                                val message = if (isIncomeItem(savedExpense)) {
                                    "수입 기록이 등록되었어요."
                                } else {
                                    "소비 기록이 등록되었어요."
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            onError = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                        homeViewModel.updateExpense(entity) // 바로 앞 설정을 이어서 적음
                        editingExpense = null // 수정 저장은 로컬 DB 업데이트라 바로 수정 상태를 닫습니다.
                        val message = if (isIncomeItem(savedExpense)) {
                            "수입 기록이 수정되었어요."
                        } else {
                            "소비 기록이 수정되었어요."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    } // 블록 끝
                },
                onCancelEdit = { // 이 이벤트가 일어났을 때 실행할 코드를 시작함
                    editingExpense = null // 바로 앞 설정을 이어서 적음
                } // 블록 끝
            )
        } // 블록 끝

        item { // 리스트 안에 들어갈 한 칸을 시작함
            RewardGuideCard() // 카드 모양 UI를 시작함
        } // 블록 끝

        item { Spacer(modifier = Modifier.height(24.dp)) } // 리스트 안에 들어갈 한 칸을 시작함
    } // 블록 끝

    if (showWeeklyScoreDialog) {
        WeeklyScoreDetailDialog(
            scoreState = weeklyScoreState,
            onDismiss = {
                showWeeklyScoreDialog = false
            }
        )
    }

    // 월 상단에서 날짜를 빠르게 바꾸기 위한 팝업 달력입니다.
    if (showCalendarDialog) { // 조건이 참일 때만 아래 코드를 실행함
        Dialog( // 팝업 창을 띄우는 영역을 시작함
            onDismissRequest = { // 팝업이 닫힐 때 실행할 코드를 시작함
                showCalendarDialog = false // onCancelEdit 값을 이 함수로 넘김
            } // 블록 끝
        ) { // 이 블록 안의 내용이 시작됨
            Card( // 카드 모양 UI를 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                shape = RoundedCornerShape(24.dp), // 모서리 모양을 정함
                colors = CardDefaults.cardColors(containerColor = homeSoftCardColor()), // 색상 스타일을 정함
                border = BorderStroke(1.dp, homeSoftCardBorderColor())
            ) { // 이 블록 안의 내용이 시작됨
                CalendarCard( // 카드 모양 UI를 시작함
                    currentYear = currentYear, // colors 값을 이 함수로 넘김
                    currentMonth = currentMonth, // currentMonth 값을 이 함수로 넘김
                    selectedDate = selectedDate, // selectedDate 값을 이 함수로 넘김
                    expenseDateSet = expenseDateSet, // expenseDateSet 값을 이 함수로 넘김
                    today = today, // today 값을 이 함수로 넘김
                    onPrevMonth = { // 이 이벤트가 일어났을 때 실행할 코드를 시작함
                        homeViewModel.moveToPreviousMonth() // onPrevMonth 값을 이 함수로 넘김
                    },
                    onNextMonth = { // 이 이벤트가 일어났을 때 실행할 코드를 시작함
                        homeViewModel.moveToNextMonth() // onNextMonth 값을 이 함수로 넘김
                    },
                    onDateSelected = { clickedDate -> // 바로 앞 설정을 이어서 적음
                        homeViewModel.selectDate(clickedDate) // 바로 앞 설정을 이어서 적음
                        showCalendarDialog = false // onDateSelected 값을 이 함수로 넘김
                    } // 블록 끝
                )
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝

    if (showWalletDialog) { // 조건이 참일 때만 아래 코드를 실행함
        SolanaWalletDialog( // 솔라나 지갑 선택 팝업을 띄움
            onDismiss = { // 팝업이 닫힐 때 실행할 코드를 시작함
                showWalletDialog = false // 지갑 선택 팝업을 닫음
            },
            onSelectWallet = { walletType -> // 지갑을 선택했을 때 실행할 코드를 시작함
                showWalletDialog = false // 지갑 선택 팝업을 닫음
                onWalletConnectClick(walletType) // 선택한 지갑 종류를 AppNavGraph로 넘김
            }
        )
    } // 블록 끝

    if (showWalletDisconnectDialog) { // 조건이 참일 때만 아래 코드를 실행함
        AlertDialog( // 확인 팝업 창을 띄우는 영역을 시작함
            onDismissRequest = { // 팝업이 닫힐 때 실행할 코드를 시작함
                showWalletDisconnectDialog = false // 지갑 해제 팝업을 닫음
            }, // 설정 구분
            title = { // 제목 영역을 시작함
                Text(text = "지갑을 해제하시겠습니까?") // 제목 텍스트를 표시함
            }, // 설정 구분
            text = { // 본문 영역을 시작함
                Column { // 세로로 내용을 배치함
                    Text(text = "현재 연결된 지갑: ${formatWalletAddress(walletAddress)}") // 현재 지갑 주소를 표시함

                    Spacer(modifier = Modifier.height(8.dp)) // 위아래 간격을 추가함

                    Text(text = "해제 후 기존 지갑 재등록 및 새로운 지갑 등록이 가능합니다.") // 안내 문구를 표시함
                } // 블록 끝
            }, // 설정 구분
            confirmButton = { // 확인 버튼 영역을 시작함
                val buttonColor = homePrimaryButtonColor()
                TextButton( // 텍스트 버튼을 시작함
                    onClick = { // 버튼을 눌렀을 때 실행할 코드를 시작함
                        showWalletDisconnectDialog = false // 지갑 해제 팝업을 닫음
                        onWalletDisconnectClick() // 지갑 해제 동작을 실행함
                    } // 블록 끝
                ) { // 버튼 안의 내용이 시작됨
                    Text( // 버튼 텍스트를 시작함
                        text = "지갑 해제", // 버튼에 표시할 글자
                        color = buttonColor, // 지갑 해제 버튼도 지정한 버튼 톤에 맞춤
                        fontWeight = FontWeight.Bold // 글자를 굵게 설정함
                    ) // 블록 끝
                } // 블록 끝
            }, // 설정 구분
            dismissButton = { // 취소 버튼 영역을 시작함
                TextButton( // 텍스트 버튼을 시작함
                    onClick = { // 버튼을 눌렀을 때 실행할 코드를 시작함
                        showWalletDisconnectDialog = false // 지갑 해제 팝업을 닫음
                    } // 블록 끝
                ) { // 버튼 안의 내용이 시작됨
                    Text(text = "취소") // 취소 버튼 글자를 표시함
                } // 블록 끝
            } // 블록 끝
        ) // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun TopHeaderSection( // TopHeaderSection 함수 선언 시작
    isWalletConnected: Boolean, // 지갑 연결 여부를 받음
    walletAddress: String, // 지갑 주소를 받음
    walletProvider: String, // 지갑 제공자 이름을 받음
    onWalletConnectClick: () -> Unit = {} // 버튼을 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val cardColor = homeSoftCardColor()
    val cardBorderColor = homeSoftCardBorderColor()
    val buttonColor = homePrimaryButtonColor()
    Card( // 카드 모양 UI를 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(24.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ), // 색상 스타일을 정함
        border = BorderStroke(1.dp, cardBorderColor)
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 가로로 배치하는 영역을 시작함
            modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                .fillMaxWidth() // 가로 너비를 꽉 채움
                .padding(horizontal = 14.dp, vertical = 14.dp), // 안쪽이나 바깥 여백을 줌
            verticalAlignment = Alignment.CenterVertically // 세로 방향 정렬을 정함
        ) { // 이 블록 안의 내용이 시작됨


            Spacer(modifier = Modifier.width(12.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Column( // 세로로 배치하는 영역을 시작함
                modifier = Modifier.weight(1f) // 남는 공간을 비율대로 차지하게 함
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 글자를 화면에 보여주기 시작함
                    text = if (isWalletConnected) "지갑 연결됨" else "NFT 거래 및 토큰 교환 지갑", // 연결 상태에 따라 제목을 정함
                    fontSize = 16.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.Bold, // 글자 두께를 정함
                    color = MaterialTheme.colorScheme.onSurface // 색상을 정함
                )
                Text( // 글자를 화면에 보여주기 시작함
                    text = if (isWalletConnected) { // 조건이 참일 때 연결 정보를 보여줌
                        "${walletProvider} · ${formatWalletAddress(walletAddress)}" // 지갑 제공자와 주소를 함께 보여줌
                    } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                        "팬텀 지갑 연결이 필요합니다" // 연결 안내 문구를 보여줌
                    },
                    fontSize = 13.sp, // 글자 크기를 정함
                    color = if (isWalletConnected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant // 연결 여부에 따라 글자색을 정함
                )
            } // 블록 끝

            Spacer(modifier = Modifier.width(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Button( // 눌렀을 때 동작하는 버튼을 만듦
                onClick = onWalletConnectClick, // color 값을 이 함수로 넘김
                shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                colors = ButtonDefaults.buttonColors( // 색상 스타일을 정함
                    containerColor = buttonColor,
                    contentColor = homePrimaryButtonContentColor()
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp) // 버튼 안쪽 여백을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 글자를 화면에 보여주기 시작함
                    text = if (isWalletConnected) "연결 완료" else "지갑 연결", // 연결 상태에 따라 제목을 정함
                    fontSize = 13.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.Bold, // 글자 두께를 정함
                    color = homePrimaryButtonContentColor()
                )
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MonthlySummaryCard( // MonthlySummaryCard 함수 선언 시작
    currentYear: Int, // currentYear 값을 함수 밖에서 받아옴
    currentMonth: Int, // currentMonth 값을 함수 밖에서 받아옴
    currentMonthTotalExpense: Int, // currentMonthTotalExpense 값을 함수 밖에서 받아옴
    currentMonthExpenseCount: Int, // currentMonthExpenseCount 값을 함수 밖에서 받아옴
    monthlyBudget: Long, // monthlyBudget 값을 함수 밖에서 받아옴
    monthlyIncome: Long, // monthlyIncome 값을 함수 밖에서 받아옴
    remainingBudget: Long, // remainingBudget 값을 함수 밖에서 받아옴
    usageRateText: String, // usageRateText 값을 함수 밖에서 받아옴
    changeRateText: String, // changeRateText 값을 함수 밖에서 받아옴
    onPrevMonth: () -> Unit, // onPrevMonth 는 눌렀을 때 실행할 동작을 받음
    onNextMonth: () -> Unit, // onNextMonth 는 눌렀을 때 실행할 동작을 받음
    onCalendarClick: () -> Unit // onCalendarClick 는 눌렀을 때 실행할 동작을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isHomeDarkTheme()
    val cardColor = homeSoftCardColor()
    val cardBorderColor = homeSoftCardBorderColor()
    Card( // 카드 모양 UI를 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(24.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors(containerColor = cardColor), // 색상 스타일을 정함
        border = BorderStroke(1.dp, cardBorderColor)
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 세로로 배치하는 영역을 시작함
            modifier = Modifier.padding(20.dp) // 안쪽이나 바깥 여백을 줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                horizontalArrangement = Arrangement.SpaceBetween, // 가로 방향 간격과 정렬을 정함
                verticalAlignment = Alignment.Top // 세로 방향 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Column { // verticalAlignment 값을 이 함수로 넘김
                    Row( // 가로로 배치하는 영역을 시작함
                        verticalAlignment = Alignment.CenterVertically // 세로 방향 정렬을 정함
                    ) { // 이 블록 안의 내용이 시작됨
                        CalendarArrowButton( // 눌렀을 때 동작하는 버튼을 만듦
                            text = "‹", // 화면에 보여줄 글자를 정함
                            onClick = onPrevMonth // onClick 값을 이 함수로 넘김
                        )

                        Spacer(modifier = Modifier.width(10.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                        Text( // 글자를 화면에 보여주기 시작함
                            text = "${currentYear}년 ${currentMonth}월", // 화면에 보여줄 글자를 정함
                            fontSize = 16.sp, // 글자 크기를 정함
                            fontWeight = FontWeight.Bold, // 글자 두께를 정함
                            color = MaterialTheme.colorScheme.onSurface // 색상을 정함
                        )

                        Spacer(modifier = Modifier.width(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                        Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                            modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                                .size(30.dp) // 가로세로 크기를 한 번에 정함
                                .background( // 배경색이나 그라데이션을 넣음
                                    color = homeDarkActionSurfaceColor(), // 색상을 정함
                                    shape = RoundedCornerShape(10.dp) // 모서리 모양을 정함
                                )
                                .clickable { onCalendarClick() }, // 눌렀을 때 반응하도록 만듦
                            contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                        ) { // 이 블록 안의 내용이 시작됨
                            Icon( // 아이콘을 화면에 보여줌
                                imageVector = Icons.Filled.CalendarMonth, // 어떤 아이콘을 쓸지 정함
                                contentDescription = "calendar", // 접근성용 설명 글을 넣음
                                tint = SpentopiaMutedPurple, // tint 값을 이 함수로 넘김
                                modifier = Modifier.size(18.dp) // 가로세로 크기를 한 번에 정함
                            )
                        } // 블록 끝

                        Spacer(modifier = Modifier.width(10.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                        CalendarArrowButton( // 눌렀을 때 동작하는 버튼을 만듦
                            text = "›", // 화면에 보여줄 글자를 정함
                            onClick = onNextMonth // onClick 값을 이 함수로 넘김
                        )
                    } // 블록 끝

                    Spacer(modifier = Modifier.height(6.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                    Text( // 글자를 화면에 보여주기 시작함
                        text = "이번 달 소비 내역 · ${currentMonthExpenseCount}건", // 화면에 보여줄 글자를 정함
                        fontSize = 13.sp, // 글자 크기를 정함
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 색상을 정함
                    )
                } // 블록 끝

                Column(horizontalAlignment = Alignment.End) { // 이 블록의 내용이 여기서 시작됨
                    Text( // 글자를 화면에 보여주기 시작함
                        text = "${formatAmount(currentMonthTotalExpense)}원", // 화면에 보여줄 글자를 정함
                        fontSize = 28.sp, // 글자 크기를 정함
                        fontWeight = FontWeight.Bold, // 글자 두께를 정함
                        color = MaterialTheme.colorScheme.onSurface // 색상을 정함
                    )
                    Text( // 글자를 화면에 보여주기 시작함
                        text = changeRateText, // 화면에 보여줄 글자를 정함
                        fontSize = 13.sp, // 글자 크기를 정함
                        color = getChangeRateColor(changeRateText) // 색상을 정함
                    )
                } // 블록 끝
            } // 블록 끝

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                horizontalArrangement = Arrangement.spacedBy(12.dp) // 가로 방향 간격과 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                SummaryMiniCard( // 카드 모양 UI를 시작함
                    title = "예산", // horizontalArrangement 값을 이 함수로 넘김
                    value = "${formatAmount(monthlyBudget)}원", // value 값을 이 함수로 넘김
                    bgColor = if (isDark) homeStatCardColor() else Color(0xFFDDF3F7), // 배경색 값을 넘김
                    modifier = Modifier.weight(1f) // 남는 공간을 비율대로 차지하게 함
                )

                SummaryMiniCard( // 카드 모양 UI를 시작함
                    title = "수입", // 수입 제목
                    value = "${formatAmount(monthlyIncome)}원", // 수입 값
                    bgColor = if (isDark) homeStatCardColor() else Color(0xFFE8F7E8), // 배경색 값을 넘김
                    modifier = Modifier.weight(1f) // 남는 공간을 비율대로 차지하게 함
                )

                SummaryMiniCard( // 카드 모양 UI를 시작함
                    title = if (remainingBudget >= 0) "남은 예산" else "초과 예산", // modifier 값을 이 함수로 넘김
                    value = "${formatAmount(abs(remainingBudget))}원", // 음수를 양수로 바꿔 절댓값으로 만듦
                    bgColor = if (isDark) homeStatCardColor() else if (remainingBudget >= 0) Color(0xFFE1EAFF) else Color(0xFFFFE3E3), // 배경색 값을 넘김
                    modifier = Modifier.weight(1f) // 남는 공간을 비율대로 차지하게 함
                )

                SummaryMiniCard( // 카드 모양 UI를 시작함
                    title = "사용률", // modifier 값을 이 함수로 넘김
                    value = usageRateText, // value 값을 이 함수로 넘김
                    bgColor = if (isDark) homeStatCardColor() else Color(0xFFDFF2EC), // 배경색 값을 넘김
                    modifier = Modifier.weight(1f) // 남는 공간을 비율대로 차지하게 함
                )
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SummaryMiniCard( // SummaryMiniCard 함수 선언 시작
    title: String, // title 값을 함수 밖에서 받아옴
    value: String, // value 값을 함수 밖에서 받아옴
    bgColor: Color, // bgColor 값을 함수 밖에서 받아옴
    modifier: Modifier = Modifier // Modifier 값을 이 함수로 넘김
) { // 이 블록 안의 내용이 시작됨
    val isDark = isHomeDarkTheme()
    Card( // 카드 모양 UI를 시작함
        modifier = modifier, // 이 UI의 크기·여백·배경 설정을 시작함
        shape = RoundedCornerShape(16.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors(containerColor = bgColor), // 색상 스타일을 정함
        border = if (isDark) BorderStroke(1.dp, homeSoftCardBorderColor()) else null // 다크모드 미니 카드 테두리색을 맞춤
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 세로로 배치하는 영역을 시작함
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp) // 안쪽 여백 줄임
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 글자를 화면에 보여주기 시작함
                text = title, // 화면에 보여줄 글자를 정함
                fontSize = 11.sp, // 글자 크기를 줄임
                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF315072), // 색상을 정함
                maxLines = 1 // 한 줄만 표시
            )
            Spacer(modifier = Modifier.height(6.dp)) // 컴포넌트 사이에 빈 공간을 넣음
            Text( // 글자를 화면에 보여주기 시작함
                text = value, // 화면에 보여줄 글자를 정함
                fontSize = 14.sp, // 글자 크기를 줄임
                fontWeight = FontWeight.Bold, // 글자 두께를 정함
                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF22406A), // 색상을 정함
                lineHeight = 18.sp // 줄 높이 줄임
            )
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

// 카테고리가 "수입"인지 판별하는 함수
private fun isIncomeCategory(category: String): Boolean {
    return category in listOf(
        "월급",
        "용돈",
        "부수입",
        "환급",
        "기타수입"
    )
}

// 하나의 소비 항목이 "수입"인지 판별
private fun isIncomeItem(item: ExpenseItemData): Boolean {
    return isIncomeCategory(item.category)
}

// 하나의 소비 항목이 "지출"인지 판별
// (수입이 아니면 모두 지출로 처리)
private fun isExpenseItem(item: ExpenseItemData): Boolean {
    return !isIncomeItem(item)
}
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CalendarCard( // CalendarCard 함수 선언 시작
    currentYear: Int, // currentYear 값을 함수 밖에서 받아옴
    currentMonth: Int, // currentMonth 값을 함수 밖에서 받아옴
    selectedDate: String, // selectedDate 값을 함수 밖에서 받아옴
    expenseDateSet: Set<String>, // expenseDateSet 값을 함수 밖에서 받아옴
    today: String, // today 값을 함수 밖에서 받아옴
    onPrevMonth: () -> Unit, // onPrevMonth 는 눌렀을 때 실행할 동작을 받음
    onNextMonth: () -> Unit, // onNextMonth 는 눌렀을 때 실행할 동작을 받음
    onDateSelected: (String) -> Unit // onDateSelected 는 눌렀을 때 실행할 동작을 받음
) { // 이 블록 안의 내용이 시작됨
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토") // 값 여러 개를 묶은 목록을 만듦
    val calendarDates = generateCalendarDates(currentYear, currentMonth) // calendarDates 값을 계산해서 저장함
    val rows = calendarDates.chunked(7) // 7개씩 끊어서 한 주 단위로 나눔
    val cellWidth = 40.dp // cellWidth 값을 계산해서 저장함

    Card( // 카드 모양 UI를 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(24.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors(containerColor = homeSoftCardColor()), // 색상 스타일을 정함
        border = BorderStroke(1.dp, homeSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 세로로 배치하는 영역을 시작함
            modifier = Modifier.padding(20.dp) // 안쪽이나 바깥 여백을 줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                horizontalArrangement = Arrangement.SpaceBetween, // 가로 방향 간격과 정렬을 정함
                verticalAlignment = Alignment.CenterVertically // 세로 방향 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                CalendarArrowButton( // 눌렀을 때 동작하는 버튼을 만듦
                    text = "‹", // 화면에 보여줄 글자를 정함
                    onClick = onPrevMonth // onClick 값을 이 함수로 넘김
                )

                Text( // 글자를 화면에 보여주기 시작함
                    text = "${currentMonth}월 ${currentYear}", // 화면에 보여줄 글자를 정함
                    fontSize = 18.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.Medium, // 글자 두께를 정함
                    color = MaterialTheme.colorScheme.onSurface // 색상을 정함
                )

                CalendarArrowButton( // 눌렀을 때 동작하는 버튼을 만듦
                    text = "›", // 화면에 보여줄 글자를 정함
                    onClick = onNextMonth // onClick 값을 이 함수로 넘김
                )
            } // 블록 끝

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                horizontalArrangement = Arrangement.Start // 가로 방향 간격과 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                daysOfWeek.forEach { day -> // 목록이나 범위를 하나씩 돌면서 처리함
                    Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                        modifier = Modifier.width(cellWidth), // 가로 길이를 정함
                        contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 글자를 화면에 보여주기 시작함
                            text = day, // 화면에 보여줄 글자를 정함
                            fontSize = 13.sp, // 글자 크기를 정함
                            color = MaterialTheme.colorScheme.onSurfaceVariant // 색상을 정함
                        )
                    } // 블록 끝
                } // 블록 끝
            } // 블록 끝

            Spacer(modifier = Modifier.height(14.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            rows.forEach { week -> // 목록이나 범위를 하나씩 돌면서 처리함
                Row( // 가로로 배치하는 영역을 시작함
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .fillMaxWidth() // 가로 너비를 꽉 채움
                        .padding(vertical = 6.dp), // 안쪽이나 바깥 여백을 줌
                    horizontalArrangement = Arrangement.Start // 가로 방향 간격과 정렬을 정함
                ) { // 이 블록 안의 내용이 시작됨
                    week.forEach { dateItem -> // 목록이나 범위를 하나씩 돌면서 처리함
                        val isSelected = dateItem.fullDate == selectedDate // isSelected 값을 계산해서 저장함
                        val hasExpense = expenseDateSet.contains(dateItem.fullDate) // 그 값이 들어있는지 확인함
                        val isToday = dateItem.fullDate == today // isToday 값을 계산해서 저장함

                        Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                            modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                                .width(cellWidth) // 가로 길이를 정함
                                .height(42.dp) // 세로 길이를 정함
                                .clickable { // 눌렀을 때 반응하도록 만듦
                                    onDateSelected(dateItem.fullDate) // 함수를 호출해 값을 넣음
                                },
                            contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                        ) { // 이 블록 안의 내용이 시작됨
                            Column( // 세로로 배치하는 영역을 시작함
                                horizontalAlignment = Alignment.CenterHorizontally, // contentAlignment 값을 이 함수로 넘김
                                verticalArrangement = Arrangement.Center // verticalArrangement 값을 이 함수로 넘김
                            ) { // 이 블록 안의 내용이 시작됨
                                if (isSelected) { // 조건이 참일 때만 아래 코드를 실행함
                                    Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                                        modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                                            .size(30.dp) // 가로세로 크기를 한 번에 정함
                                            .background( // 배경색이나 그라데이션을 넣음
                                                color = MaterialTheme.colorScheme.onBackground, // 색상을 정함
                                                shape = RoundedCornerShape(8.dp) // 모서리 모양을 정함
                                            ),
                                        contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                                    ) { // 이 블록 안의 내용이 시작됨
                                        Text( // 글자를 화면에 보여주기 시작함
                                            text = dateItem.dayText, // 화면에 보여줄 글자를 정함
                                            color = Color.White, // 색상을 정함
                                            fontSize = 13.sp, // 글자 크기를 정함
                                            fontWeight = FontWeight.Medium // 글자 두께를 정함
                                        )
                                    } // 블록 끝
                                } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                                    Text( // 글자를 화면에 보여주기 시작함
                                        text = dateItem.dayText, // 화면에 보여줄 글자를 정함
                                        color = when { // 색상을 정함
                                            !dateItem.isCurrentMonth -> Color(0xFF9AA4B2) // 바로 앞 설정을 이어서 적음
                                            isToday -> SpentopiaMutedPurple // 바로 앞 설정을 이어서 적음
                                            else -> Color(0xFF1F2A37) // color 값을 이 함수로 넘김
                                        },
                                        fontSize = 14.sp, // 글자 크기를 정함
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal // 글자 두께를 정함
                                    )
                                } // 블록 끝

                                Spacer(modifier = Modifier.height(3.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                                if (hasExpense) { // 조건이 참일 때만 아래 코드를 실행함
                                    Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                                        modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                                            .size(4.dp) // 가로세로 크기를 한 번에 정함
                                            .background( // 배경색이나 그라데이션을 넣음
                                                color = if (isSelected) Color.White else SpentopiaMutedPurple, // 색상을 정함
                                                shape = CircleShape // 모서리 모양을 정함
                                            )
                                    )
                                } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                                    Spacer(modifier = Modifier.height(4.dp)) // 컴포넌트 사이에 빈 공간을 넣음
                                } // 블록 끝
                            } // 블록 끝
                        } // 블록 끝
                    } // 블록 끝
                } // 블록 끝
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CalendarArrowButton( // CalendarArrowButton 함수 선언 시작
    text: String, // text 값을 함수 밖에서 받아옴
    onClick: () -> Unit // onClick 는 눌렀을 때 실행할 동작을 받음
) { // 이 블록 안의 내용이 시작됨
    Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
        modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
            .size(28.dp) // 가로세로 크기를 한 번에 정함
            .background( // 배경색이나 그라데이션을 넣음
                color = homeDarkActionSurfaceColor(), // 색상을 정함
                shape = RoundedCornerShape(8.dp) // 모서리 모양을 정함
            )
            .clickable { onClick() }, // 눌렀을 때 반응하도록 만듦
        contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 글자를 화면에 보여주기 시작함
            text = text, // 화면에 보여줄 글자를 정함
            color = MaterialTheme.colorScheme.onSurfaceVariant, // 색상을 정함
            fontSize = 16.sp, // 글자 크기를 정함
            fontWeight = FontWeight.Medium // 글자 두께를 정함
        )
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun DailyExpenseCard( // DailyExpenseCard 함수 선언 시작
    expenseList: List<ExpenseItemData>, // expenseList 값을 함수 밖에서 받아옴
    selectedDate: String, // selectedDate 값을 함수 밖에서 받아옴
    onEditExpense: (ExpenseItemData) -> Unit, // onEditExpense 는 눌렀을 때 실행할 동작을 받음
    onDeleteExpense: (Long) -> Unit // onDeleteExpense 는 눌렀을 때 실행할 동작을 받음
) { // 이 블록 안의 내용이 시작됨
    val cardColor = homeSoftCardColor() // 소비내역 카드 배경색을 오늘의 소비일기와 맞춤
    val cardBorderColor = homeSoftCardBorderColor() // 소비내역 카드 테두리색을 오늘의 소비일기와 맞춤
    val diaryCardColor = homeSoftCardColor() // 소비일기 카드 배경색을 정함
    val diaryBorderColor = homeSoftCardBorderColor() // 소비일기 카드 테두리색을 정함
    val filteredList = expenseList.filter { it.date == selectedDate } // 조건에 맞는 항목만 남김
    val totalAmount = filteredList // 선택한 날짜의 지출 항목 금액만 더함
        .filter { isExpenseItem(it) } // 수입 항목은 제외하고 지출 항목만 남김
        .sumOf { it.amount } // 지출 항목의 금액만 합산함
    val diaryText = filteredList.firstOrNull { it.diary.isNotBlank() }?.diary ?: "" // 조건에 맞는 첫 항목을 찾되 없으면 null을 줌

    Card( // 카드 모양 UI를 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(24.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors(containerColor = cardColor), // 색상 스타일을 정함
        border = BorderStroke(1.dp, cardBorderColor) // 카드 테두리색을 정함
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 세로로 배치하는 영역을 시작함
            modifier = Modifier.padding(20.dp) // 안쪽이나 바깥 여백을 줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 글자를 화면에 보여주기 시작함
                text = formatDisplayDate(selectedDate), // 화면에 보여줄 글자를 정함
                fontSize = 22.sp, // 글자 크기를 정함
                fontWeight = FontWeight.Bold, // 글자 두께를 정함
                color = MaterialTheme.colorScheme.onSurface // 색상을 정함
            )

            Spacer(modifier = Modifier.height(6.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Text( // 글자를 화면에 보여주기 시작함
                text = "총 ${formatAmount(totalAmount)}원 · ${filteredList.count { isExpenseItem(it) }}건", // 화면에 보여줄 글자를 정함
                fontSize = 14.sp, // 글자 크기를 정함
                color = MaterialTheme.colorScheme.onSurfaceVariant // 색상을 정함
            )

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            if (filteredList.isEmpty()) { // 조건이 참일 때만 아래 코드를 실행함
                Text( // 글자를 화면에 보여주기 시작함
                    text = "이 날짜에는 아직 저장된 소비 내역이 없어요", // 화면에 보여줄 글자를 정함
                    fontSize = 14.sp, // 글자 크기를 정함
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 색상을 정함
                )
            } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                filteredList.forEachIndexed { index, item -> // 목록이나 범위를 하나씩 돌면서 처리함
                    ExpenseItemCard( // 카드 모양 UI를 시작함
                        emoji = getCategoryEmoji(item.category), // emoji 값을 이 함수로 넘김
                        title = item.title, // title 값을 이 함수로 넘김
                        category = item.category, // category 값을 이 함수로 넘김
                        amount = if (isIncomeItem(item)) { // 수입 항목이면 금액 앞에 + 표시를 붙임
                            "+${formatAmount(item.amount)}원" // 수입 금액 표시
                        } else { // 지출 항목이면 기존처럼 금액만 표시함
                            "${formatAmount(item.amount)}원" // 지출 금액 표시
                        }, // amount 값을 이 함수로 넘김
                        tag = if (isExpenseItem(item)) {
                            if (item.receiptVerified) "인증됨" else "미인증"
                        } else {
                            null
                        },
                        iconColors = getCategoryColors(item.category), // iconColors 값을 이 함수로 넘김
                        onEditClick = { // onEditClick 값을 이 함수로 넘김
                            onEditExpense(item) // 함수를 호출해 값을 넣음
                        },
                        onDeleteClick = { // onDeleteClick 값을 이 함수로 넘김
                            onDeleteExpense(item.id) // 함수를 호출해 값을 넣음
                        } // 블록 끝
                    )

                    if (index != filteredList.lastIndex) { // 조건이 참일 때만 아래 코드를 실행함
                        Spacer(modifier = Modifier.height(12.dp)) // 컴포넌트 사이에 빈 공간을 넣음
                    } // 블록 끝
                } // 블록 끝

                if (diaryText.isNotBlank()) { // 조건이 참일 때만 아래 코드를 실행함
                    Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                    Card( // 카드 모양 UI를 시작함
                        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                        shape = RoundedCornerShape(16.dp), // 모서리 모양을 정함
                        colors = CardDefaults.cardColors( // 색상 스타일을 정함
                            containerColor = diaryCardColor // 배경색을 정함
                        ),
                        border = BorderStroke( // border 값을 이 함수로 넘김
                            1.dp, // border 값을 이 함수로 넘김
                            diaryBorderColor // 사용할 색상 값을 넣음
                        )
                    ) { // 이 블록 안의 내용이 시작됨
                        Column( // 세로로 배치하는 영역을 시작함
                            modifier = Modifier.padding(16.dp) // 안쪽이나 바깥 여백을 줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Text( // 글자를 화면에 보여주기 시작함
                                text = "오늘의 소비 일기", // 화면에 보여줄 글자를 정함
                                fontSize = 15.sp, // 글자 크기를 정함
                                fontWeight = FontWeight.Bold, // 글자 두께를 정함
                                color = MaterialTheme.colorScheme.onSurface // 색상을 정함
                            )

                            Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                            Text( // 글자를 화면에 보여주기 시작함
                                text = diaryText, // 화면에 보여줄 글자를 정함
                                fontSize = 14.sp, // 글자 크기를 정함
                                color = MaterialTheme.colorScheme.onSurfaceVariant // 색상을 정함
                            )
                        } // 블록 끝
                    } // 블록 끝
                } // 블록 끝
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ExpenseItemCard( // ExpenseItemCard 함수 선언 시작
    emoji: String, // emoji 값을 함수 밖에서 받아옴
    title: String, // title 값을 함수 밖에서 받아옴
    category: String, // category 값을 함수 밖에서 받아옴
    amount: String, // amount 값을 함수 밖에서 받아옴
    tag: String?, // tag 값을 함수 밖에서 받아옴
    iconColors: List<Color>, // iconColors 값을 함수 밖에서 받아옴
    onEditClick: () -> Unit, // onEditClick 는 눌렀을 때 실행할 동작을 받음
    onDeleteClick: () -> Unit // onDeleteClick 는 눌렀을 때 실행할 동작을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isHomeDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val itemCardColor = if (isDark) homeStatCardColor() else Color(0xFFF7FBFF) // 소비 항목 카드 배경색을 오늘의 소비일기와 맞춤
    val itemBorderColor = if (isDark) homeSoftCardBorderColor() else Color(0xFF7DD3FC) // 소비 항목 카드 테두리색을 오늘의 소비일기와 맞춤
    val editColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB) // 수정 버튼 색을 모드별로 분리함
    val expenseAmountColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E3A8A) // 지출 금액 색을 모드별로 분리함
    Card( // 카드 모양 UI를 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(18.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors(containerColor = itemCardColor), // 색상 스타일을 정함
        border = BorderStroke( // border 값을 이 함수로 넘김
            1.dp, // border 값을 이 함수로 넘김
            itemBorderColor // 사용할 색상 값을 넣음
        )
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 세로로 배치하는 영역을 시작함
            modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                .fillMaxWidth() // 가로 너비를 꽉 채움
                .padding(horizontal = 14.dp, vertical = 14.dp) // 안쪽이나 바깥 여백을 줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                verticalAlignment = Alignment.CenterVertically // 세로 방향 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .size(40.dp) // 가로세로 크기를 한 번에 정함
                        .background( // 배경색이나 그라데이션을 넣음
                            brush = Brush.horizontalGradient(iconColors), // 왼쪽에서 오른쪽으로 색이 바뀌는 배경을 만듦
                            shape = RoundedCornerShape(12.dp) // 모서리 모양을 정함
                        ),
                    contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 글자를 화면에 보여주기 시작함
                        text = emoji, // 화면에 보여줄 글자를 정함
                        fontSize = 18.sp // 글자 크기를 정함
                    )
                } // 블록 끝

                Spacer(modifier = Modifier.width(12.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                Column( // 세로로 배치하는 영역을 시작함
                    modifier = Modifier.weight(1f) // 남는 공간을 비율대로 차지하게 함
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 글자를 화면에 보여주기 시작함
                        text = title, // 화면에 보여줄 글자를 정함
                        fontSize = 16.sp, // 글자 크기를 정함
                        fontWeight = FontWeight.Bold, // 글자 두께를 정함
                        color = MaterialTheme.colorScheme.onSurface // 색상을 정함
                    )

                    Spacer(modifier = Modifier.height(4.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                    Row(verticalAlignment = Alignment.CenterVertically) { // 이 블록의 내용이 여기서 시작됨
                        Text( // 글자를 화면에 보여주기 시작함
                            text = category, // 화면에 보여줄 글자를 정함
                            fontSize = 13.sp, // 글자 크기를 정함
                            color = MaterialTheme.colorScheme.onSurfaceVariant // 색상을 정함
                        )

                        if (tag != null) { // 조건이 참일 때만 아래 코드를 실행함
                            Spacer(modifier = Modifier.width(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                            Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                                modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                                    .background( // 배경색이나 그라데이션을 넣음
                                    color = when (tag) {
                                        "인증됨" -> if (isDark) Color(0xFF143524) else Color(0xFFEFFCF3)
                                        "미인증" -> if (isDark) Color(0xFF3A2418) else Color(0xFFFFF7ED)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                        shape = RoundedCornerShape(20.dp) // 모서리 모양을 정함
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp), // 안쪽이나 바깥 여백을 줌
                                contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                            ) { // 이 블록 안의 내용이 시작됨
                                Text( // 글자를 화면에 보여주기 시작함
                                    text = tag, // 화면에 보여줄 글자를 정함
                                    fontSize = 11.sp, // 글자 크기를 정함
                                    color = when (tag) {
                                        "인증됨" -> Color(0xFF16A34A)
                                        "미인증" -> Color(0xFFC2410C)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Medium // 글자 두께를 정함
                                )
                            } // 블록 끝
                        } // 블록 끝
                    } // 블록 끝
                } // 블록 끝

                Spacer(modifier = Modifier.width(12.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                Text( // 글자를 화면에 보여주기 시작함
                    text = amount, // 화면에 보여줄 글자를 정함
                    fontSize = 16.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.Bold, // 글자 두께를 정함
                    color = if (amount.startsWith("+")) Color(0xFF00C896) else expenseAmountColor // 수입이면 초록색, 지출이면 기본색으로 표시함
                )
            } // 블록 끝

            Spacer(modifier = Modifier.height(10.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                horizontalArrangement = Arrangement.End // 가로 방향 간격과 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                TextButton( // 눌렀을 때 동작하는 버튼을 만듦
                    onClick = onEditClick // horizontalArrangement 값을 이 함수로 넘김
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 글자를 화면에 보여주기 시작함
                        text = "수정", // 화면에 보여줄 글자를 정함
                        color = editColor, // 색상을 정함
                        fontSize = 13.sp, // 글자 크기를 정함
                        fontWeight = FontWeight.SemiBold // 글자 두께를 정함
                    )
                } // 블록 끝

                TextButton( // 눌렀을 때 동작하는 버튼을 만듦
                    onClick = onDeleteClick // fontWeight 값을 이 함수로 넘김
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 글자를 화면에 보여주기 시작함
                        text = "삭제", // 화면에 보여줄 글자를 정함
                        color = Color(0xFFE53935), // 색상을 정함
                        fontSize = 13.sp, // 글자 크기를 정함
                        fontWeight = FontWeight.SemiBold // 글자 두께를 정함
                    )
                } // 블록 끝
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

private fun resolveReceiptVerificationError(error: Exception): String {
    if (error is HttpException) {
        val serverMessage = error.response()?.errorBody()?.string()?.trim().orEmpty()
        if (serverMessage.isNotBlank()) {
            return serverMessage
        }

        return when (error.code()) {
            400 -> "영수증 이미지, 날짜, 금액 정보를 확인해 주세요."
            401 -> "로그인이 만료되었습니다. 다시 로그인해 주세요."
            409 -> "이미 인증된 소비 내역입니다."
            429 -> "오늘 영수증 인증은 최대 3건까지 가능합니다."
            503 -> "AI 서버에 연결하지 못했습니다. 백엔드의 AI_SERVER_URL과 AI 서버 실행 상태를 확인해 주세요."
            else -> "영수증 인증에 실패했습니다. (${error.code()})"
        }
    }

    return error.message ?: "영수증 인증에 실패했습니다."
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun WeeklyScoreCard( // WeeklyScoreCard 함수 선언 시작
    scoreState: WeeklyScoreUiState,
    onClick: () -> Unit
) { // 이 블록 안의 내용이 시작됨
    val totalScore = scoreState.totalScore.coerceIn(0, 100)
    val cardColor = homeSoftCardColor()
    val cardBorderColor = homeSoftCardBorderColor()

    Card( // 카드 모양 UI를 시작함
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(24.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors( // 색상 스타일을 정함
            containerColor = cardColor // 배경색을 정함
        ),
        border = BorderStroke( // border 값을 이 함수로 넘김
            1.dp, // border 값을 이 함수로 넘김
            cardBorderColor // 사용할 색상 값을 넣음
        )
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 세로로 배치하는 영역을 시작함
            modifier = Modifier.padding(20.dp) // 안쪽이나 바깥 여백을 줌
        ) { // 이 블록 안의 내용이 시작됨
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lucide_flame),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text( // 글자를 화면에 보여주기 시작함
                    text = "이번 주 성실도", // 화면에 보여줄 글자를 정함
                    fontSize = 20.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.Bold, // 글자 두께를 정함
                    color = MaterialTheme.colorScheme.onSurface // 색상을 정함
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                horizontalArrangement = Arrangement.SpaceBetween, // 가로 방향 간격과 정렬을 정함
                verticalAlignment = Alignment.Bottom // 세로 방향 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Column { // verticalAlignment 값을 이 함수로 넘김
                    Text(
                        text = "총점",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text( // 글자를 화면에 보여주기 시작함
                        text = "${totalScore}점", // 화면에 보여줄 글자를 정함
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } // 블록 끝

                Text(
                    text = if (scoreState.isLoading) "불러오는 중" else "상세 보기",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } // 블록 끝

            Spacer(modifier = Modifier.height(12.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            WeeklyScoreProgressBar(
                progress = totalScore / 100f
            )

            if (scoreState.errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = scoreState.errorMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable
private fun WeeklyScoreDetailDialog(
    scoreState: WeeklyScoreUiState,
    onDismiss: () -> Unit
) {
    val totalScore = scoreState.totalScore.coerceIn(0, 100)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = homeSoftCardColor()),
            border = BorderStroke(1.dp, homeSoftCardBorderColor())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이번 주 성실도",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = onDismiss) {
                        Text(text = "닫기")
                    }
                } // 블록 끝

                Text(
                    text = "${totalScore}점",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                WeeklyScoreProgressBar(
                    progress = totalScore / 100f
                )

                WeeklyScoreDetailRow("소비 기록", scoreState.recordDaysScore, 30)
                WeeklyScoreDetailRow("영수증 인증", scoreState.receiptScore, 25)
                WeeklyScoreDetailRow("일기 작성", scoreState.diaryScore, 20)
                WeeklyScoreDetailRow("예산 체크", scoreState.budgetScore, 15)
                WeeklyScoreDetailRow(
                    label = "연속 활동",
                    score = scoreState.streakScore,
                    maxScore = 10,
                    leadingValue = "${scoreState.streakScore.coerceIn(0, 10)}일"
                )

                if (scoreState.errorMessage.isNotBlank()) {
                    Text(
                        text = scoreState.errorMessage,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyScoreDetailRow(
    label: String,
    score: Int,
    maxScore: Int,
    leadingValue: String? = null
) {
    val progress = if (maxScore > 0) score.toFloat() / maxScore.toFloat() else 0f
    val scoreText = listOfNotNull(
        leadingValue,
        "${score.coerceAtLeast(0)} / $maxScore"
    ).joinToString("  ")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = scoreText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        WeeklyScoreProgressBar(
            progress = progress
        )
    }
}

@Composable
private fun WeeklyScoreProgressBar(
    progress: Float
) {
    val colorScheme = MaterialTheme.colorScheme
    val waveShift by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val progressBrush = Brush.linearGradient(
        colors = listOf(
            colorScheme.primary,
            colorScheme.primary.copy(alpha = 0.72f),
            colorScheme.primaryContainer,
            colorScheme.primary
        ),
        start = Offset(-220f + waveShift * 260f, 0f),
        end = Offset(260f + waveShift * 260f, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                shape = RoundedCornerShape(999.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .background(
                    brush = progressBrush,
                    shape = RoundedCornerShape(999.dp)
                )
        )
    }
}

private fun saveBitmapToCacheUri(
    context: Context,
    bitmap: Bitmap,
    filePrefix: String
): Uri? {
    return try {
        val cacheDir = File(context.cacheDir, "receipt_camera").apply {
            if (!exists()) mkdirs()
        }
        val file = File(cacheDir, "${filePrefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class) // 실험 기능을 쓰겠다고 표시
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ExpenseWriteCard( // ExpenseWriteCard 함수 선언 시작
    selectedDate: String, // selectedDate 값을 함수 밖에서 받아옴
    editingExpense: ExpenseItemData?, // editingExpense 값을 함수 밖에서 받아옴
    onSaveExpense: (ExpenseItemData) -> Unit, // onSaveExpense 는 눌렀을 때 실행할 동작을 받음
    onCancelEdit: () -> Unit // onCancelEdit 는 눌렀을 때 실행할 동작을 받음
) { // 이 블록 안의 내용이 시작됨
    val context = LocalContext.current // 현재 화면의 Context를 가져옴
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance() // 현재 날짜/시간 정보를 가진 Calendar 객체를 만듦
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isHomeDarkTheme()
    val primaryButtonColor = homePrimaryButtonColor()
    val primaryButtonContentColor = homePrimaryButtonContentColor()
    val formCardColor = homeSoftCardColor()
    val formSurfaceColor = homeInputFieldColor()
    val formButtonSurfaceColor = homeDarkActionSurfaceColor()
    val formBorderColor = homeInputBorderColor()
    val formPrimaryTextColor = colorScheme.onSurface
    val formSecondaryTextColor = colorScheme.onSurfaceVariant
    val formAccentColor = colorScheme.primary
    val formAccentContainerColor = colorScheme.primaryContainer
    val formAccentContainerTextColor = colorScheme.onPrimaryContainer
    val incomeTabSelectedBackground = if (isDark) Color(0xFF142238) else Color(0xFFE0F2FE)
    val incomeTabSelectedContentColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)

    var formDate by remember { mutableStateOf(selectedDate) } // 화면이 다시 그려져도 유지되는 상태값을 만듦
    var isExpenseTab by remember { mutableStateOf(true) } // 입력 탭 상태를 보관함
    var selectedExpenseCategory by remember { mutableStateOf("식비") } // 소비 카테고리 상태를 보관함
    var selectedIncomeCategory by remember { mutableStateOf("월급") } // 수입 카테고리 상태를 보관함
    var amount by remember { mutableStateOf("") } // 화면이 다시 그려져도 유지되는 상태값을 만듦
    var memo by remember { mutableStateOf("") } // 화면이 다시 그려져도 유지되는 상태값을 만듦
    var receiptImageName by remember { mutableStateOf("") } // 화면이 다시 그려져도 유지되는 상태값을 만듦
    var isReceiptVerifying by remember { mutableStateOf(false) } // OCR 인증 중인지 저장합니다.
    var receiptVerificationMessage by remember { mutableStateOf("") } // OCR 결과 안내 문구를 저장합니다.
    var isReceiptVerified by remember { mutableStateOf(false) } // 현재 영수증이 인증 성공했는지 저장합니다.
    var pendingServerExpenseId by remember { mutableStateOf("") } // 새 기록 저장 전 OCR을 위해 먼저 만든 서버 UUID입니다.
    var diary by remember { mutableStateOf("") } // 화면이 다시 그려져도 유지되는 상태값을 만듦
    var expanded by remember { mutableStateOf(false) } // 화면이 다시 그려져도 유지되는 상태값을 만듦
    val expenseCategoryList = listOf("식비", "교통", "쇼핑", "카페", "기타") // 소비 카테고리 목록을 만듦
    val incomeCategoryList = listOf("월급", "용돈", "부수입", "환급", "기타수입") // 수입 카테고리 목록을 만듦

    val galleryLauncher = rememberLauncherForActivityResult( // 갤러리 같은 외부 화면 결과를 받을 준비를 함
        contract = ActivityResultContracts.GetContent() // 파일이나 이미지를 하나 고르는 규칙을 씀
    ) { uri: Uri? -> // 바로 앞 설정을 이어서 적음
        if (uri != null) { // 조건이 참일 때만 아래 코드를 실행함
            // 갤러리에서 고른 이미지를 문자열로 저장합니다.
            // 이 값은 영수증 미리보기와 OCR 업로드에서 다시 사용됩니다.
            receiptImageName = uri.toString() // galleryLauncher 값을 이 함수로 넘김
            receiptVerificationMessage = ""
            isReceiptVerified = false
        } // 블록 끝
    } // 블록 끝

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            // 카메라로 찍은 Bitmap을 임시 파일 Uri로 바꿉니다.
            // 갤러리에서 고른 이미지와 동일한 방식으로 다루기 위해서입니다.
            saveBitmapToCacheUri(context, it, "receipt_camera")?.let { uri ->
                receiptImageName = uri.toString()
                receiptVerificationMessage = ""
                isReceiptVerified = false
            }
        }
    }

    LaunchedEffect(editingExpense?.id, selectedDate) { // 이 블록의 내용이 여기서 시작됨
        if (editingExpense != null) { // 조건이 참일 때만 아래 코드를 실행함
            formDate = editingExpense.date // 바로 앞 설정을 이어서 적음
            amount = editingExpense.amount.toString() // 바로 앞 설정을 이어서 적음
            memo = editingExpense.memo // 바로 앞 설정을 이어서 적음
            receiptImageName = editingExpense.receiptImageName // 바로 앞 설정을 이어서 적음
            diary = editingExpense.diary // 바로 앞 설정을 이어서 적음
            pendingServerExpenseId = editingExpense.serverExpenseId // 수정 중인 기록의 서버 UUID를 가져옵니다.
            isReceiptVerified = editingExpense.receiptVerified // 기존 인증 상태를 화면에 맞춥니다.

            if (incomeCategoryList.contains(editingExpense.category)) { // 조건이 참일 때만 아래 코드를 실행함
                isExpenseTab = false // 수입 탭으로 맞춤
                selectedIncomeCategory = editingExpense.category // 수입 카테고리를 채움
                selectedExpenseCategory = "식비" // 소비 카테고리를 기본값으로 맞춤
            } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                isExpenseTab = true // 소비 탭으로 맞춤
                selectedExpenseCategory = editingExpense.category // 소비 카테고리를 채움
                selectedIncomeCategory = "월급" // 수입 카테고리를 기본값으로 맞춤
            } // 블록 끝
        } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
            formDate = selectedDate // 바로 앞 설정을 이어서 적음
            isExpenseTab = true // 기본 탭을 소비로 맞춤
            selectedExpenseCategory = "식비" // 바로 앞 설정을 이어서 적음
            selectedIncomeCategory = "월급" // 바로 앞 설정을 이어서 적음
            amount = "" // 바로 앞 설정을 이어서 적음
            memo = "" // 바로 앞 설정을 이어서 적음
            receiptImageName = "" // 바로 앞 설정을 이어서 적음
            pendingServerExpenseId = "" // 새 입력을 시작하면 이전 서버 UUID를 비웁니다.
            isReceiptVerified = false // 새 입력을 시작하면 인증 상태도 초기화합니다.
            receiptVerificationMessage = "" // 이전 OCR 결과 문구를 지웁니다.
            diary = "" // formDate 값을 이 함수로 넘김
        } // 블록 끝

        expanded = false // 드롭다운 상태를 닫음
    } // 블록 끝

    Card( // 카드 모양 UI를 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(24.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors( // 색상 스타일을 정함
            containerColor = formCardColor // 배경색을 정함
        ),
        border = BorderStroke(1.dp, homeSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 세로로 배치하는 영역을 시작함
            modifier = Modifier.padding(20.dp) // 안쪽이나 바깥 여백을 줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 글자를 화면에 보여주기 시작함
                text = if (editingExpense == null) "기록 입력하기" else "기록 수정하기", // 화면에 보여줄 글자를 정함
                fontSize = 20.sp, // 글자 크기를 정함
                fontWeight = FontWeight.Bold, // 글자 두께를 정함
                color = MaterialTheme.colorScheme.onSurface // 색상을 정함
            )

            Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Text( // 글자를 화면에 보여주기 시작함
                text = if (editingExpense == null) // 화면에 보여줄 글자를 정함
                    "소비 또는 수입을 선택해서 기록해보세요" // 바로 앞 설정을 이어서 적음
                else // color 값을 이 함수로 넘김
                    "선택한 기록을 수정할 수 있어요", // color 값을 이 함수로 넘김
                fontSize = 14.sp, // 글자 크기를 정함
                color = MaterialTheme.colorScheme.onSurfaceVariant // 색상을 정함
            )

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            // 입력 탭
            Row( // 가로로 배치하는 영역을 시작함
                modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                    .fillMaxWidth() // 가로 너비를 꽉 채움
                    .background( // 배경색이나 그라데이션을 넣음
                        color = formSurfaceColor, // 색상을 정함
                        shape = RoundedCornerShape(14.dp) // 모서리 모양을 정함
                    )
                    .border(
                        width = 1.dp,
                        color = formBorderColor,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(4.dp), // 안쪽이나 바깥 여백을 줌
                horizontalArrangement = Arrangement.spacedBy(8.dp) // 가로 방향 간격과 정렬을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .weight(1f) // 남는 공간을 비율대로 차지하게 함
                        .background( // 배경색이나 그라데이션을 넣음
                            color = if (isExpenseTab) formAccentContainerColor else Color.Transparent, // 색상을 정함
                            shape = RoundedCornerShape(12.dp) // 모서리 모양을 정함
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) formBorderColor else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { // 눌렀을 때 반응하도록 만듦
                            isExpenseTab = true // 소비 탭으로 바꿈
                            expanded = false // 드롭다운을 닫음
                        }
                        .padding(vertical = 12.dp), // 안쪽이나 바깥 여백을 줌
                    contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                ) { // 이 블록 안의 내용이 시작됨
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "소비 입력",
                            modifier = Modifier.size(17.dp),
                            tint = if (isExpenseTab) formAccentContainerTextColor else formSecondaryTextColor
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text( // 글자를 화면에 보여주기 시작함
                            text = "소비 입력", // 화면에 보여줄 글자를 정함
                            color = if (isExpenseTab) formAccentContainerTextColor else formSecondaryTextColor, // 색상을 정함
                            fontSize = 14.sp, // 글자 크기를 정함
                            fontWeight = FontWeight.SemiBold // 글자 두께를 정함
                        )
                    }
                } // 블록 끝

                Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .weight(1f) // 남는 공간을 비율대로 차지하게 함
                        .background( // 배경색이나 그라데이션을 넣음
                            color = if (!isExpenseTab) incomeTabSelectedBackground else Color.Transparent, // 색상을 정함
                            shape = RoundedCornerShape(12.dp) // 모서리 모양을 정함
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) formBorderColor else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { // 눌렀을 때 반응하도록 만듦
                            isExpenseTab = false // 수입 탭으로 바꿈
                            expanded = false // 드롭다운을 닫음
                        }
                        .padding(vertical = 12.dp), // 안쪽이나 바깥 여백을 줌
                    contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                ) { // 이 블록 안의 내용이 시작됨
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "수입 입력",
                            modifier = Modifier.size(17.dp),
                            tint = if (!isExpenseTab) incomeTabSelectedContentColor else formSecondaryTextColor
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text( // 글자를 화면에 보여주기 시작함
                            text = "수입 입력", // 화면에 보여줄 글자를 정함
                            color = if (!isExpenseTab) incomeTabSelectedContentColor else formSecondaryTextColor, // 색상을 정함
                            fontSize = 14.sp, // 글자 크기를 정함
                            fontWeight = FontWeight.SemiBold // 글자 두께를 정함
                        )
                    }
                } // 블록 끝
            } // 블록 끝

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Text( // 글자를 화면에 보여주기 시작함
                text = "날짜", // 화면에 보여줄 글자를 정함
                fontSize = 15.sp, // 글자 크기를 정함
                fontWeight = FontWeight.SemiBold, // 글자 두께를 정함
                color = formPrimaryTextColor // 색상을 정함
            )

            Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            OutlinedTextField( // 테두리 있는 입력칸을 만듦
                value = formDate, // color 값을 이 함수로 넘김
                onValueChange = { }, // 입력값이 바뀔 때 처리할 코드를 적음
                readOnly = true, // 직접 타이핑은 막고 보기만 하게 함
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                trailingIcon = { // 입력칸 오른쪽에 붙을 아이콘 영역을 만듦
                    Icon( // 글자를 화면에 보여주기 시작함
                        imageVector = Icons.Filled.EditCalendar,
                        contentDescription = null,
                        tint = formAccentColor
                    )
                },
                placeholder = { // 입력값이 없을 때 보여줄 안내문을 넣음
                    Text( // 글자를 화면에 보여주기 시작함
                        text = "날짜를 선택하세요", // 화면에 보여줄 글자를 정함
                        color = formSecondaryTextColor // 색상을 정함
                    )
                },
                singleLine = true, // 한 줄만 입력되게 함
                shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                colors = OutlinedTextFieldDefaults.colors( // 색상 스타일을 정함
                    focusedContainerColor = formSurfaceColor, // 선택됐을 때 입력칸 배경색을 정함
                    unfocusedContainerColor = formSurfaceColor, // 선택 안 됐을 때 입력칸 배경색을 정함
                    focusedBorderColor = formBorderColor, // 선택됐을 때 테두리 색을 정함
                    unfocusedBorderColor = formBorderColor, // 선택 안 됐을 때 테두리 색을 정함
                    focusedTextColor = formPrimaryTextColor,
                    unfocusedTextColor = formPrimaryTextColor,
                    focusedPlaceholderColor = formSecondaryTextColor,
                    unfocusedPlaceholderColor = formSecondaryTextColor,
                    cursorColor = formAccentColor // 커서 색을 정함
                )
            )

            Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            TextButton( // 눌렀을 때 동작하는 버튼을 만듦
                onClick = { // 버튼을 눌렀을 때 실행할 코드를 시작함
                    val dateParts = formDate.split("-") // dateParts 값을 계산해서 저장함
                    val initYear = dateParts.getOrNull(0)?.toIntOrNull() // 숫자로 바꾸되 실패하면 null을 줌
                        ?: calendar.get(Calendar.YEAR) // 바로 앞 설정을 이어서 적음
                    val initMonth = (dateParts.getOrNull(1)?.toIntOrNull() // 숫자로 바꾸되 실패하면 null을 줌
                        ?: (calendar.get(Calendar.MONTH) + 1)) - 1 // 바로 앞 설정을 이어서 적음
                    val initDay = dateParts.getOrNull(2)?.toIntOrNull() // 숫자로 바꾸되 실패하면 null을 줌
                        ?: calendar.get(Calendar.DAY_OF_MONTH) // cursorColor 값을 이 함수로 넘김

                    DatePickerDialog( // 날짜 고르는 팝업 창을 만듦
                        context, // cursorColor 값을 이 함수로 넘김
                        { _, year, month, dayOfMonth -> // 바로 앞 설정을 이어서 적음
                            val formattedMonth = String.format("%02d", month + 1) // 자릿수를 맞춘 문자열을 만듦
                            val formattedDay = String.format("%02d", dayOfMonth) // 자릿수를 맞춘 문자열을 만듦
                            formDate = "$year-$formattedMonth-$formattedDay" // formDate 값을 이 함수로 넘김
                        },
                        initYear, // 바로 앞 설정을 이어서 적음
                        initMonth, // 바로 앞 설정을 이어서 적음
                        initDay // 바로 앞 설정을 이어서 적음
                    ).show()
                },
                contentPadding = PaddingValues(0.dp) // 버튼 안쪽 여백을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 글자를 화면에 보여주기 시작함
                    text = "날짜 선택하기", // 화면에 보여줄 글자를 정함
                    color = formAccentColor, // 색상을 정함
                    fontSize = 14.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.Medium // 글자 두께를 정함
                )
            } // 블록 끝

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Text( // 글자를 화면에 보여주기 시작함
                text = "금액", // 화면에 보여줄 글자를 정함
                fontSize = 15.sp, // 글자 크기를 정함
                fontWeight = FontWeight.SemiBold, // 글자 두께를 정함
                color = formPrimaryTextColor // 색상을 정함
            )

            Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            OutlinedTextField( // 테두리 있는 입력칸을 만듦
                value = amount, // color 값을 이 함수로 넘김
                onValueChange = { amount = it.filter { char -> char.isDigit() } }, // 조건에 맞는 항목만 남김
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                placeholder = { // 입력값이 없을 때 보여줄 안내문을 넣음
                    Text( // 글자를 화면에 보여주기 시작함
                        text = "예: 12000", // 화면에 보여줄 글자를 정함
                        color = formSecondaryTextColor // 색상을 정함
                    )
                },
                singleLine = true, // 한 줄만 입력되게 함
                shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                colors = OutlinedTextFieldDefaults.colors( // 색상 스타일을 정함
                    focusedContainerColor = formSurfaceColor, // 선택됐을 때 입력칸 배경색을 정함
                    unfocusedContainerColor = formSurfaceColor, // 선택 안 됐을 때 입력칸 배경색을 정함
                    focusedBorderColor = formBorderColor, // 선택됐을 때 테두리 색을 정함
                    unfocusedBorderColor = formBorderColor, // 선택 안 됐을 때 테두리 색을 정함
                    focusedTextColor = formPrimaryTextColor,
                    unfocusedTextColor = formPrimaryTextColor,
                    focusedPlaceholderColor = formSecondaryTextColor,
                    unfocusedPlaceholderColor = formSecondaryTextColor,
                    cursorColor = formAccentColor // 커서 색을 정함
                )
            )

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Text( // 글자를 화면에 보여주기 시작함
                text = "카테고리", // 화면에 보여줄 글자를 정함
                fontSize = 15.sp, // 글자 크기를 정함
                fontWeight = FontWeight.SemiBold, // 글자 두께를 정함
                color = formPrimaryTextColor // 색상을 정함
            )

            Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            ExposedDropdownMenuBox( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                expanded = expanded, // 드롭다운이 펼쳐졌는지 상태를 넘김
                onExpandedChange = { expanded = !expanded } // onExpandedChange 값을 이 함수로 넘김
            ) { // 이 블록 안의 내용이 시작됨
                OutlinedTextField( // 테두리 있는 입력칸을 만듦
                    value = if (isExpenseTab) selectedExpenseCategory else selectedIncomeCategory, // 표시할 카테고리를 정함
                    onValueChange = { }, // 입력값이 바뀔 때 처리할 코드를 적음
                    readOnly = true, // 직접 타이핑은 막고 보기만 하게 함
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .menuAnchor() // 드롭다운이 붙을 기준 위치로 설정함
                        .fillMaxWidth(), // 가로 너비를 꽉 채움
                    placeholder = { // 입력값이 없을 때 보여줄 안내문을 넣음
                        Text( // 글자를 화면에 보여주기 시작함
                            text = "카테고리를 선택하세요", // 화면에 보여줄 글자를 정함
                            color = formSecondaryTextColor // 색상을 정함
                        )
                    },
                    trailingIcon = { // 입력칸 오른쪽에 붙을 아이콘 영역을 만듦
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) // 아이콘을 화면에 보여줌
                    },
                    singleLine = true, // 한 줄만 입력되게 함
                    shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                    colors = OutlinedTextFieldDefaults.colors( // 색상 스타일을 정함
                        focusedContainerColor = formSurfaceColor, // 선택됐을 때 입력칸 배경색을 정함
                        unfocusedContainerColor = formSurfaceColor, // 선택 안 됐을 때 입력칸 배경색을 정함
                        focusedBorderColor = formBorderColor, // 선택됐을 때 테두리 색을 정함
                        unfocusedBorderColor = formBorderColor, // 선택 안 됐을 때 테두리 색을 정함
                        focusedTextColor = formPrimaryTextColor,
                        unfocusedTextColor = formPrimaryTextColor,
                        focusedPlaceholderColor = formSecondaryTextColor,
                        unfocusedPlaceholderColor = formSecondaryTextColor,
                        cursorColor = formAccentColor // 커서 색을 정함
                    )
                )

                DropdownMenu( // 펼쳐지는 메뉴를 만듦
                    expanded = expanded, // 드롭다운이 펼쳐졌는지 상태를 넘김
                    onDismissRequest = { expanded = false } // 팝업이 닫힐 때 실행할 코드를 시작함
                ) { // 이 블록 안의 내용이 시작됨
                    if (isExpenseTab) { // 조건이 참일 때만 아래 코드를 실행함
                        expenseCategoryList.forEach { category -> // 목록이나 범위를 하나씩 돌면서 처리함
                            DropdownMenuItem( // 드롭다운 메뉴 항목 하나를 만듦
                                text = { // 화면에 보여줄 글자를 정함
                                    Text(text = category) // 글자를 화면에 보여주기 시작함
                                },
                                onClick = { // 버튼을 눌렀을 때 실행할 코드를 시작함
                                    selectedExpenseCategory = category // 카테고리를 바꿈
                                    expanded = false // 드롭다운이 펼쳐졌는지 상태를 넘김
                                } // 블록 끝
                            )
                        } // 블록 끝
                    } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                        incomeCategoryList.forEach { category -> // 목록이나 범위를 하나씩 돌면서 처리함
                            DropdownMenuItem( // 드롭다운 메뉴 항목 하나를 만듦
                                text = { // 화면에 보여줄 글자를 정함
                                    Text(text = category) // 글자를 화면에 보여주기 시작함
                                },
                                onClick = { // 버튼을 눌렀을 때 실행할 코드를 시작함
                                    selectedIncomeCategory = category // 카테고리를 바꿈
                                    expanded = false // 드롭다운이 펼쳐졌는지 상태를 넘김
                                } // 블록 끝
                            )
                        } // 블록 끝
                    } // 블록 끝
                } // 블록 끝
            } // 블록 끝

            Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            Text( // 글자를 화면에 보여주기 시작함
                text = "메모", // 화면에 보여줄 글자를 정함
                fontSize = 15.sp, // 글자 크기를 정함
                fontWeight = FontWeight.SemiBold, // 글자 두께를 정함
                color = formPrimaryTextColor // 색상을 정함
            )

            Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            OutlinedTextField( // 테두리 있는 입력칸을 만듦
                value = memo, // color 값을 이 함수로 넘김
                onValueChange = { memo = it }, // 입력값이 바뀔 때 처리할 코드를 적음
                modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                placeholder = { // 입력값이 없을 때 보여줄 안내문을 넣음
                    Text( // 글자를 화면에 보여주기 시작함
                        text = if (isExpenseTab) "무엇을 구매했나요?" else "수입 내용을 입력하세요", // 화면에 보여줄 글자를 정함
                        color = formSecondaryTextColor // 색상을 정함
                    )
                },
                singleLine = true, // 한 줄만 입력되게 함
                shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                colors = OutlinedTextFieldDefaults.colors( // 색상 스타일을 정함
                    focusedContainerColor = formSurfaceColor, // 선택됐을 때 입력칸 배경색을 정함
                    unfocusedContainerColor = formSurfaceColor, // 선택 안 됐을 때 입력칸 배경색을 정함
                    focusedBorderColor = formBorderColor, // 선택됐을 때 테두리 색을 정함
                    unfocusedBorderColor = formBorderColor, // 선택 안 됐을 때 테두리 색을 정함
                    focusedTextColor = formPrimaryTextColor,
                    unfocusedTextColor = formPrimaryTextColor,
                    focusedPlaceholderColor = formSecondaryTextColor,
                    unfocusedPlaceholderColor = formSecondaryTextColor,
                    cursorColor = formAccentColor // 커서 색을 정함
                )
            )

            // 소비 탭일 때만 추가 입력을 보여줌
            if (isExpenseTab) { // 조건이 참일 때만 아래 코드를 실행함
                Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                Text( // 글자를 화면에 보여주기 시작함
                    text = "영수증 인증 ", // 화면에 보여줄 글자를 정함
                    fontSize = 15.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.SemiBold, // 글자 두께를 정함
                    color = formPrimaryTextColor // 색상을 정함
                )

                Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, formBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = formButtonSurfaceColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = formAccentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (receiptImageName.isBlank()) "업로드" else "업로드 변경",
                            color = formPrimaryTextColor,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, formBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = formButtonSurfaceColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = formAccentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "카메라",
                            color = formPrimaryTextColor,
                            fontSize = 14.sp
                        )
                    }
                }

                if (receiptImageName.isNotBlank()) { // 조건이 참일 때만 아래 코드를 실행함
                    Spacer(modifier = Modifier.height(12.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                    Image( // 이미지를 화면에 보여줌
                        painter = rememberAsyncImagePainter(receiptImageName), // 어떤 이미지를 그릴지 정함
                        contentDescription = "영수증 이미지", // 접근성용 설명 글을 넣음
                        modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                            .fillMaxWidth() // 가로 너비를 꽉 채움
                            .height(180.dp), // 세로 길이를 정함
                        contentScale = ContentScale.Crop // 이미지를 어떤 비율로 채울지 정함
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val expectedAmount = amount.toIntOrNull()
                            if (expectedAmount == null || expectedAmount <= 0) {
                                Toast.makeText(context, "영수증 인증 전에 금액을 입력해주세요.", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    isReceiptVerifying = true
                                    receiptVerificationMessage = ""
                                    isReceiptVerified = false

                                    try {
                                        val currentCategory = if (isExpenseTab) selectedExpenseCategory else selectedIncomeCategory

                                        // 백엔드 OCR은 expense_id가 있어야 DB의 receipt_verified까지 바꿀 수 있습니다.
                                        // 새 기록은 아직 로컬 DB에 저장 전이라도, 여기서 먼저 서버 기록을 만들고 UUID를 받아둡니다.
                                        val serverExpenseId = pendingServerExpenseId.ifBlank {
                                            val remoteExpense = RetrofitClient.expenseApi.createExpense(
                                                CreateExpenseRequest(
                                                    date = formDate,
                                                    amount = expectedAmount,
                                                    category = currentCategory,
                                                    memo = memo.takeIf { it.isNotBlank() },
                                                    transactionType = if (isExpenseTab) "expense" else "income",
                                                    diary = diary.takeIf { it.isNotBlank() }
                                                )
                                            )

                                            pendingServerExpenseId = remoteExpense.id
                                            remoteExpense.id
                                        }

                                        val imageUri = Uri.parse(receiptImageName)
                                        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                                        val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { input ->
                                            input.readBytes()
                                        }

                                        if (imageBytes == null || imageBytes.isEmpty()) {
                                            Toast.makeText(context, "영수증 이미지를 읽지 못했습니다.", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        val imageRequestBody = imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                                        val imagePart = MultipartBody.Part.createFormData(
                                            name = "image",
                                            filename = "receipt.${mimeType.substringAfter('/', "jpg")}",
                                            body = imageRequestBody
                                        )
                                        val expectedDateBody = formDate.toRequestBody("text/plain".toMediaTypeOrNull())
                                        val expectedAmountBody = expectedAmount.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                                        val response = RetrofitClient.receiptApi.verifyReceiptOcr(
                                            expenseId = serverExpenseId,
                                            image = imagePart,
                                            expectedDate = expectedDateBody,
                                            expectedAmount = expectedAmountBody
                                        )

                                        isReceiptVerified = response.verification.is_verified
                                        if (response.verification.is_verified) {
                                            response.ocr.receipt_date?.let { formDate = it }
                                            response.ocr.total_amount?.let { amount = it.toString() }
                                        }
                                        val merchantText = response.ocr.merchant_name?.takeIf { it.isNotBlank() }
                                        receiptVerificationMessage = if (response.verification.is_verified) {
                                            listOfNotNull(
                                                "영수증 인증 완료!",
                                                "영수증 인증 완료 시 보상이 지급됩니다.",
                                                merchantText?.let { "상호명: $it" },
                                                response.ocr.receipt_date?.let { "결제일: $it" },
                                                response.ocr.total_amount?.let { "총 금액: ${formatAmount(it)}원" }
                                            ).joinToString("\n")
                                        } else {
                                            response.verification.reason
                                        }

                                        Toast.makeText(
                                            context,
                                            if (response.verification.is_verified) "영수증 인증 완료!" else response.verification.reason,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        receiptVerificationMessage = resolveReceiptVerificationError(e)
                                        Toast.makeText(context, receiptVerificationMessage, Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isReceiptVerifying = false
                                    }
                                }
                            }
                        },
                        enabled = !isReceiptVerifying,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    isReceiptVerifying -> "영수증 인증 중..."
                                    isReceiptVerified -> "영수증 인증 완료!"
                                    else -> "영수증 인증하기"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (receiptVerificationMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = receiptVerificationMessage,
                            fontSize = 13.sp,
                            color = if (isReceiptVerified) Color(0xFF16A34A) else Color(0xFFE53935)
                        )
                    }
                } // 블록 끝

                Spacer(modifier = Modifier.height(20.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                Text( // 글자를 화면에 보여주기 시작함
                    text = "한줄 소비 일기 ", // 화면에 보여줄 글자를 정함
                    fontSize = 15.sp, // 글자 크기를 정함
                    fontWeight = FontWeight.SemiBold, // 글자 두께를 정함
                    color = formPrimaryTextColor // 색상을 정함
                )

                Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                OutlinedTextField( // 테두리 있는 입력칸을 만듦
                    value = diary, // color 값을 이 함수로 넘김
                    onValueChange = { diary = it }, // 입력값이 바뀔 때 처리할 코드를 적음
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .fillMaxWidth() // 가로 너비를 꽉 채움
                        .height(96.dp), // 세로 길이를 정함
                    placeholder = { // 입력값이 없을 때 보여줄 안내문을 넣음
                        Text( // 글자를 화면에 보여주기 시작함
                            text = "오늘 소비에 대한 생각을 기록해보세요", // 화면에 보여줄 글자를 정함
                            color = formSecondaryTextColor // 색상을 정함
                        )
                    },
                    shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                    colors = OutlinedTextFieldDefaults.colors( // 색상 스타일을 정함
                        focusedContainerColor = formSurfaceColor, // 선택됐을 때 입력칸 배경색을 정함
                        unfocusedContainerColor = formSurfaceColor, // 선택 안 됐을 때 입력칸 배경색을 정함
                        focusedBorderColor = formBorderColor, // 선택됐을 때 테두리 색을 정함
                        unfocusedBorderColor = formBorderColor, // 선택 안 됐을 때 테두리 색을 정함
                        focusedTextColor = formPrimaryTextColor,
                        unfocusedTextColor = formPrimaryTextColor,
                        focusedPlaceholderColor = formSecondaryTextColor,
                        unfocusedPlaceholderColor = formSecondaryTextColor,
                    cursorColor = formAccentColor // 커서 색을 정함
                    )
                )
            } // 블록 끝

            Spacer(modifier = Modifier.height(24.dp)) // 컴포넌트 사이에 빈 공간을 넣음

            if (editingExpense != null) { // 조건이 참일 때만 아래 코드를 실행함
                Row( // 가로로 배치하는 영역을 시작함
                    modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                    horizontalArrangement = Arrangement.spacedBy(10.dp) // 가로 방향 간격과 정렬을 정함
                ) { // 이 블록 안의 내용이 시작됨
                    OutlinedButton( // 눌렀을 때 동작하는 버튼을 만듦
                        onClick = { onCancelEdit() }, // 버튼을 눌렀을 때 실행할 코드를 시작함
                        modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                            .weight(1f) // 남는 공간을 비율대로 차지하게 함
                            .height(54.dp), // 세로 길이를 정함
                        shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                        border = BorderStroke(1.dp, formBorderColor), // 바로 앞 설정을 이어서 적음
                        colors = ButtonDefaults.outlinedButtonColors( // 색상 스타일을 정함
                            containerColor = homeDarkActionSurfaceColor() // 배경색을 정함
                        )
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 글자를 화면에 보여주기 시작함
                            text = "취소", // 화면에 보여줄 글자를 정함
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // 색상을 정함
                            fontSize = 16.sp, // 글자 크기를 정함
                            fontWeight = FontWeight.SemiBold // 글자 두께를 정함
                        )
                    } // 블록 끝

                    Button( // 눌렀을 때 동작하는 버튼을 만듦
                        onClick = { // 버튼을 눌렀을 때 실행할 코드를 시작함
                            // 수정 저장 흐름입니다.
                            // 수정 중인 값이 화면 입력값으로 다시 포장된 뒤
                            // onSaveExpense로 넘어가서 실제 DB 저장이 됩니다.
                            val amountInt = amount.toIntOrNull() ?: 0 // 숫자로 바꾸되 실패하면 null을 줌
                            val currentCategory = if (isExpenseTab) selectedExpenseCategory else selectedIncomeCategory // 현재 카테고리를 계산함

                            if (amountInt > 0) { // 조건이 참일 때만 아래 코드를 실행함
                                val updatedExpense = ExpenseItemData( // updatedExpense 값을 계산해서 저장함
                                    id = editingExpense.id, // fontWeight 값을 이 함수로 넘김
                                    date = formDate, // date 값을 이 함수로 넘김
                                    title = createExpenseTitle(currentCategory, memo), // 바로 앞 설정을 이어서 적음
                                    category = currentCategory, // category 값을 이 함수로 넘김
                                    amount = amountInt, // amount 값을 이 함수로 넘김
                                    memo = memo, // memo 값을 이 함수로 넘김
                                    receiptImageName = if (isExpenseTab) receiptImageName else "", // receiptImageName 값을 이 함수로 넘김
                                    diary = if (isExpenseTab) diary else "", // diary 값을 이 함수로 넘김
                                    serverExpenseId = pendingServerExpenseId, // OCR 때 만든 서버 UUID를 같이 저장합니다.
                                    receiptVerified = isReceiptVerified // OCR 인증 성공 여부를 같이 저장합니다.
                                )

                                onSaveExpense(updatedExpense) // 함수를 호출해 값을 넣음
                            } // 블록 끝
                        },
                        modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                            .weight(1f) // 남는 공간을 비율대로 차지하게 함
                            .height(54.dp), // 세로 길이를 정함
                        shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                        colors = ButtonDefaults.buttonColors( // 색상 스타일을 정함
                            containerColor = primaryButtonColor,
                            contentColor = primaryButtonContentColor
                        ),
                        contentPadding = PaddingValues(0.dp) // 버튼 안쪽 여백을 정함
                    ) { // 이 블록 안의 내용이 시작됨
                        Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                            modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                                .fillMaxSize() // 부모가 허용하는 공간을 전부 채움
                                .background( // 배경색이나 그라데이션을 넣음
                                    color = primaryButtonColor,
                                    shape = RoundedCornerShape(14.dp) // 모서리 모양을 정함
                                ),
                            contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                        ) { // 이 블록 안의 내용이 시작됨
                            Text( // 글자를 화면에 보여주기 시작함
                                text = if (isExpenseTab) "소비 수정 완료" else "수입 수정 완료", // 화면에 보여줄 글자를 정함
                                color = primaryButtonContentColor, // 색상을 정함
                                fontSize = 17.sp, // 글자 크기를 정함
                                fontWeight = FontWeight.Bold // 글자 두께를 정함
                            )
                        } // 블록 끝
                    } // 블록 끝
                } // 블록 끝
            } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
                Button( // 눌렀을 때 동작하는 버튼을 만듦
                    onClick = { // 버튼을 눌렀을 때 실행할 코드를 시작함
                        // 새 기록 저장 흐름입니다.
                        // 지금 화면에 입력한 값들을 하나의 객체로 묶어서
                        // 바깥의 onSaveExpense에 넘깁니다.
                        val amountInt = amount.toIntOrNull() ?: 0 // 숫자로 바꾸되 실패하면 null을 줌
                        val currentCategory = if (isExpenseTab) selectedExpenseCategory else selectedIncomeCategory // 현재 카테고리를 계산함

                        if (amountInt > 0) { // 조건이 참일 때만 아래 코드를 실행함
                            val newExpense = ExpenseItemData( // newExpense 값을 계산해서 저장함
                                id = 0L, // fontWeight 값을 이 함수로 넘김
                                date = formDate, // date 값을 이 함수로 넘김
                                title = createExpenseTitle(currentCategory, memo), // 바로 앞 설정을 이어서 적음
                                category = currentCategory, // category 값을 이 함수로 넘김
                                amount = amountInt, // amount 값을 이 함수로 넘김
                                memo = memo, // memo 값을 이 함수로 넘김
                                receiptImageName = if (isExpenseTab) receiptImageName else "", // receiptImageName 값을 이 함수로 넘김
                                diary = if (isExpenseTab) diary else "", // diary 값을 이 함수로 넘김
                                serverExpenseId = pendingServerExpenseId, // OCR 때 만든 서버 UUID를 같이 저장합니다.
                                receiptVerified = isReceiptVerified // OCR 인증 성공 여부를 같이 저장합니다.
                            )

                            onSaveExpense(newExpense) // 함수를 호출해 값을 넣음
                        } // 블록 끝
                    },
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .fillMaxWidth() // 가로 너비를 꽉 채움
                        .height(54.dp), // 세로 길이를 정함
                    shape = RoundedCornerShape(14.dp), // 모서리 모양을 정함
                    colors = ButtonDefaults.buttonColors( // 색상 스타일을 정함
                        containerColor = primaryButtonColor,
                        contentColor = primaryButtonContentColor
                    ),
                    contentPadding = PaddingValues(0.dp) // 버튼 안쪽 여백을 정함
                ) { // 이 블록 안의 내용이 시작됨
                        Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                            modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                                .fillMaxSize() // 부모가 허용하는 공간을 전부 채움
                                .background( // 배경색이나 그라데이션을 넣음
                                    color = primaryButtonColor,
                                    shape = RoundedCornerShape(14.dp) // 모서리 모양을 정함
                                ),
                        contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 글자를 화면에 보여주기 시작함
                            text = if (isExpenseTab) "소비 입력 완료" else "수입 입력 완료", // 화면에 보여줄 글자를 정함
                            color = primaryButtonContentColor, // 색상을 정함
                            fontSize = 17.sp, // 글자 크기를 정함
                            fontWeight = FontWeight.Bold // 글자 두께를 정함
                        )
                    } // 블록 끝
                } // 블록 끝
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun RewardGuideCard() { // RewardGuideCard 함수 시작
    val containerColor = homeSoftCardColor()
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val isDark = isHomeDarkTheme()
    val accentContainerColor = if (isDark) homeStatCardColor() else MaterialTheme.colorScheme.primaryContainer
    val accentContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer

    Card( // 카드 모양 UI를 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        shape = RoundedCornerShape(20.dp), // 모서리 모양을 정함
        colors = CardDefaults.cardColors( // 색상 스타일을 정함
            containerColor = containerColor // 배경색을 정함
        ),
        border = BorderStroke(
            1.dp,
            homeSoftCardBorderColor()
        )
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
            modifier = Modifier.fillMaxWidth() // 가로 너비를 꽉 채움
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 세로로 배치하는 영역을 시작함
                modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                    .fillMaxWidth() // 가로 너비를 꽉 채움
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp) // 안쪽이나 바깥 여백을 줌
            ) { // 이 블록 안의 내용이 시작됨
                Row( // 가로로 배치하는 영역을 시작함
                    modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
                    horizontalArrangement = Arrangement.SpaceBetween, // 가로 방향 간격과 정렬을 정함
                    verticalAlignment = Alignment.CenterVertically // 세로 방향 정렬을 정함
                ) { // 이 블록 안의 내용이 시작됨
                    Row( // 가로로 배치하는 영역을 시작함
                        verticalAlignment = Alignment.CenterVertically // 세로 방향 정렬을 정함
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 글자를 화면에 보여주기 시작함
                            text = "🎁", // 화면에 보여줄 글자를 정함
                            fontSize = 14.sp // 글자 크기를 정함
                        )

                        Spacer(modifier = Modifier.width(6.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                        Text( // 글자를 화면에 보여주기 시작함
                            text = "보상 안내", // 화면에 보여줄 글자를 정함
                            fontSize = 15.sp, // 글자 크기를 정함
                            fontWeight = FontWeight.Bold, // 글자 두께를 정함
                            color = titleColor // 색상을 정함
                        )
                    } // 블록 끝
                } // 블록 끝

                Spacer(modifier = Modifier.height(14.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                RewardPointRow( // 가로로 배치하는 영역을 시작함
                    title = "기본 기록", // color 값을 이 함수로 넘김
                    titleColor = bodyColor,
                    pointColor = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                RewardPointRow( // 가로로 배치하는 영역을 시작함
                    title = "영수증 인증 완료", // point 값을 이 함수로 넘김
                    point = "아바타 뽑기권 지급", // point 값을 이 함수로 넘김
                    titleColor = bodyColor,
                    pointColor = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                RewardPointRow( // 가로로 배치하는 영역을 시작함
                    title = "일기 작성", // point 값을 이 함수로 넘김
                    titleColor = bodyColor,
                    pointColor = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp)) // 컴포넌트 사이에 빈 공간을 넣음

                Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                    modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                        .fillMaxWidth() // 가로 너비를 꽉 채움
                        .background(
                            color = accentContainerColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border( // 테두리를 그림
                            width = 1.5.dp, // point 값을 이 함수로 넘김
                            color = if (isDark) homeInputBorderColor() else MaterialTheme.colorScheme.outlineVariant, // 색상을 정함
                            shape = RoundedCornerShape(10.dp) // 모서리 모양을 정함
                        )
                        .padding(vertical = 8.dp, horizontal = 10.dp), // 안쪽이나 바깥 여백을 줌
                    contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
                ) { // 이 블록 안의 내용이 시작됨
                    Column( // 세로로 배치하는 영역을 시작함
                        horizontalAlignment = Alignment.CenterHorizontally // contentAlignment 값을 이 함수로 넘김
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 글자를 화면에 보여주기 시작함
                            text = "주간 성실도 90점 이상 시", // 화면에 보여줄 글자를 정함
                            fontSize = 12.sp, // 글자 크기를 정함
                            fontWeight = FontWeight.Bold, // 글자 두께를 정함
                            color = accentContentColor // 색상을 정함
                        )

                        Text( // 글자를 화면에 보여주기 시작함
                            text = "영수증 인증 완료 시 보상이 지급됩니다.", // 화면에 보여줄 글자를 정함
                            fontSize = 11.sp, // 글자 크기를 정함
                            color = accentContentColor.copy(alpha = 0.78f) // 색상을 정함
                        )
                    } // 블록 끝
                } // 블록 끝
            } // 블록 끝

            Box( // 겹치기나 감싸기에 쓰는 박스 영역을 시작함
                modifier = Modifier // 이 UI의 크기·여백·배경 설정을 시작함
                    .align(Alignment.BottomEnd) // 부모 안에서 위치를 맞춤
                    .padding(end = 10.dp, bottom = 10.dp) // 안쪽이나 바깥 여백을 줌
                    .size(52.dp) // 가로세로 크기를 한 번에 정함
                    .background( // 배경색이나 그라데이션을 넣음
                        color = MaterialTheme.colorScheme.primary, // 색상을 정함
                        shape = CircleShape // 모서리 모양을 정함
                    ),
                contentAlignment = Alignment.Center // 안쪽 내용을 어디에 둘지 정함
            ) { // 이 블록 안의 내용이 시작됨
                Column( // 세로로 배치하는 영역을 시작함
                    horizontalAlignment = Alignment.CenterHorizontally // contentAlignment 값을 이 함수로 넘김
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 글자를 화면에 보여주기 시작함
                        text = "Q", // 화면에 보여줄 글자를 정함
                        fontSize = 13.sp, // 글자 크기를 정함
                        fontWeight = FontWeight.Bold, // 글자 두께를 정함
                        color = Color.White // 색상을 정함
                    )
                    Text( // 글자를 화면에 보여주기 시작함
                        text = "소비백과", // 화면에 보여줄 글자를 정함
                        fontSize = 8.sp, // 글자 크기를 정함
                        color = Color.White // 색상을 정함
                    )
                } // 블록 끝
            } // 블록 끝
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun RewardPointRow( // RewardPointRow 함수 선언 시작
    title: String, // title 값을 함수 밖에서 받아옴
    titleColor: Color,
    pointColor: Color,
    point: String = "" // 바로 앞 설정을 이어서 적음
) { // 이 블록 안의 내용이 시작됨
    Row( // 가로로 배치하는 영역을 시작함
        modifier = Modifier.fillMaxWidth(), // 가로 너비를 꽉 채움
        horizontalArrangement = Arrangement.SpaceBetween, // 가로 방향 간격과 정렬을 정함
        verticalAlignment = Alignment.CenterVertically // 세로 방향 정렬을 정함
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 글자를 화면에 보여주기 시작함
            text = title, // 화면에 보여줄 글자를 정함
            fontSize = 13.sp, // 글자 크기를 정함
            color = titleColor // 색상을 정함
        )

        if (point.isNotBlank()) {
            Text( // 글자를 화면에 보여주기 시작함
                text = point, // 화면에 보여줄 글자를 정함
                fontSize = 13.sp, // 글자 크기를 정함
                fontWeight = FontWeight.Bold, // 글자 두께를 정함
                color = pointColor // 색상을 정함
            )
        }
    } // 블록 끝
} // 블록 끝

// --------------------------------------------------
// 아래 2개 변환 함수가 이번 Room 연결의 핵심입니다.
// --------------------------------------------------

// DB용 Entity를 화면용 UI 모델로 변환합니다.
private fun ExpenseEntity.toUiModel(): ExpenseItemData { // color 값을 이 함수로 넘김
    return ExpenseItemData( // 계산한 결과를 바깥으로 돌려줌
        id = id, // color 값을 이 함수로 넘김
        date = date, // date 값을 이 함수로 넘김
        title = title, // title 값을 이 함수로 넘김
        category = category, // category 값을 이 함수로 넘김
        amount = amount, // amount 값을 이 함수로 넘김
        memo = memo, // memo 값을 이 함수로 넘김
        receiptImageName = receiptImageUri, // receiptImageName 값을 이 함수로 넘김
        diary = diary, // 바로 앞 설정을 이어서 적음
        serverExpenseId = serverExpenseId, // 서버 UUID를 화면 모델로 넘김
        receiptVerified = receiptVerified // OCR 인증 상태를 화면 모델로 넘김
    )
} // 블록 끝

private fun ExpenseItemData.toEntity(): ExpenseEntity { // diary 값을 이 함수로 넘김
    // 화면에서 입력한 데이터를 Room DB가 저장할 수 있는 형태로 바꿉니다.
    return ExpenseEntity( // 계산한 결과를 바깥으로 돌려줌
        id = id, // diary 값을 이 함수로 넘김
        date = date, // date 값을 이 함수로 넘김
        title = title, // title 값을 이 함수로 넘김
        category = category, // category 값을 이 함수로 넘김
        amount = amount, // amount 값을 이 함수로 넘김
        memo = memo, // memo 값을 이 함수로 넘김
        diary = diary, // diary 값을 이 함수로 넘김
        receiptImageUri = receiptImageName, // receiptImageUri 값을 이 함수로 넘김
        serverExpenseId = serverExpenseId, // 백엔드 expenses UUID를 로컬 DB에도 저장함
        receiptVerified = receiptVerified // OCR 인증 상태를 로컬 DB에도 저장함
    )
} // 블록 끝

// 지갑 주소를 앞 4자리와 뒤 4자리만 보이도록 줄여서 표시합니다.
private fun formatWalletAddress(address: String): String { // formatWalletAddress 함수 시작
    return if (address.length <= 10) address // 주소 길이가 짧으면 그대로 돌려줌
    else "${address.take(4)}...${address.takeLast(4)}" // 주소가 길면 앞 4자리와 뒤 4자리만 보여줌
} // 블록 끝

private fun getCategoryEmoji(category: String): String { // getCategoryEmoji 함수 시작
    return when (category) { // 이 블록의 내용이 여기서 시작됨
        "식비" -> "🍔" // 이 조건이면 오른쪽 값을 선택함
        "교통" -> "🚕" // 이 조건이면 오른쪽 값을 선택함
        "쇼핑" -> "🛍️" // 이 조건이면 오른쪽 값을 선택함
        "카페" -> "☕" // 이 조건이면 오른쪽 값을 선택함
        else -> "💸" // 이 조건이면 오른쪽 값을 선택함
    } // 블록 끝
} // 블록 끝

private fun getCategoryColors(category: String): List<Color> { // getCategoryColors 함수 시작
    return when (category) { // 이 블록의 내용이 여기서 시작됨
        "식비" -> listOf(Color(0xFFFF8A00), Color(0xFFFF5C00)) // 값 여러 개를 묶은 목록을 만듦
        "교통" -> listOf(Color(0xFF4C8DFF), Color(0xFF2F6BFF)) // 값 여러 개를 묶은 목록을 만듦
        "쇼핑" -> listOf(Color(0xFFFF6BAA), Color(0xFFFF4D8D)) // 값 여러 개를 묶은 목록을 만듦
        "카페" -> listOf(Color(0xFF9C6BFF), Color(0xFF7A4DFF)) // 값 여러 개를 묶은 목록을 만듦
        else -> listOf(Color(0xFF22C55E), Color(0xFF16A34A)) // 값 여러 개를 묶은 목록을 만듦
    } // 블록 끝
} // 블록 끝

private fun createExpenseTitle(category: String, memo: String): String { // createExpenseTitle 함수 시작
    return if (memo.isNotBlank()) { // 이 블록의 내용이 여기서 시작됨
        memo // 바로 앞 설정을 이어서 적음
    } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
        when (category) { // 값에 따라 경우를 나눔
            "식비" -> "식사" // 이 조건이면 오른쪽 값을 선택함
            "교통" -> "이동" // 이 조건이면 오른쪽 값을 선택함
            "쇼핑" -> "쇼핑" // 이 조건이면 오른쪽 값을 선택함
            "카페" -> "카페" // 이 조건이면 오른쪽 값을 선택함
            "월급" -> "월급" // 수입 카테고리 제목을 수입 이름 그대로 보여줌
            "용돈" -> "용돈" // 수입 카테고리 제목을 수입 이름 그대로 보여줌
            "부수입" -> "부수입" // 수입 카테고리 제목을 수입 이름 그대로 보여줌
            "환급" -> "환급" // 수입 카테고리 제목을 수입 이름 그대로 보여줌
            "기타수입" -> "기타 수입" // 수입 카테고리 제목을 수입 이름 그대로 보여줌
            else -> "기타" // 이 조건이면 오른쪽 값을 선택함
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

private fun formatAmount(amount: Int): String { // formatAmount 함수 시작
    val formatter = DecimalFormat("#,###") // 숫자에 쉼표 형식을 적용할 준비를 함
    return formatter.format(amount) // 계산한 결과를 바깥으로 돌려줌
} // 블록 끝

private fun formatAmount(amount: Long): String { // formatAmount 함수 시작
    val formatter = DecimalFormat("#,###") // 숫자에 쉼표 형식을 적용할 준비를 함
    return formatter.format(amount) // 계산한 결과를 바깥으로 돌려줌
} // 블록 끝

private fun formatDisplayDate(date: String): String { // formatDisplayDate 함수 시작
    return try { // 계산한 결과를 바깥으로 돌려줌
        val yearMonthDay = date.split("-") // yearMonthDay 값을 계산해서 저장함
        val month = yearMonthDay[1].toInt() // month 숫자 값으로 계산함
        val day = yearMonthDay[2].toInt() // day 숫자 값으로 계산함
        "${month}월 ${day}일 소비 내역" // 바로 앞 설정을 이어서 적음
    } catch (e: Exception) { // 이 블록의 내용이 여기서 시작됨
        "소비 내역" // yearMonthDay 값을 이 함수로 넘김
    } // 블록 끝
} // 블록 끝

private fun moveMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> { // moveMonth 함수 시작
    var newYear = year // newYear 값을 계산해서 저장함
    var newMonth = month + delta // newMonth 값을 계산해서 저장함

    while (newMonth < 1) { // 이 블록의 내용이 여기서 시작됨
        newMonth += 12 // 바로 앞 설정을 이어서 적음
        newYear -= 1 // 바로 앞 설정을 이어서 적음
    } // 블록 끝

    while (newMonth > 12) { // 이 블록의 내용이 여기서 시작됨
        newMonth -= 12 // 바로 앞 설정을 이어서 적음
        newYear += 1 // newYear 값을 이 함수로 넘김
    } // 블록 끝

    return Pair(newYear, newMonth) // 계산한 결과를 바깥으로 돌려줌
} // 블록 끝

private fun generateCalendarDates(year: Int, month: Int): List<CalendarDateData> { // generateCalendarDates 함수 시작
    val result = mutableListOf<CalendarDateData>() // 추가·삭제 가능한 목록을 만듦

    val currentCalendar = Calendar.getInstance().apply { // 현재 날짜/시간 정보를 가진 Calendar 객체를 만듦
        set(Calendar.YEAR, year) // 함수를 호출해 값을 넣음
        set(Calendar.MONTH, month - 1) // 함수를 호출해 값을 넣음
        set(Calendar.DAY_OF_MONTH, 1) // 함수를 호출해 값을 넣음
    } // 블록 끝

    val firstDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK) // firstDayOfWeek 값을 계산해서 저장함
    val daysInCurrentMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) // daysInCurrentMonth 값을 계산해서 저장함
    val leadingDays = firstDayOfWeek - 1 // leadingDays 값을 계산해서 저장함

    val previousCalendar = currentCalendar.clone() as Calendar // previousCalendar 값을 계산해서 저장함
    previousCalendar.add(Calendar.MONTH, -1) // 바로 앞 설정을 이어서 적음
    val previousYear = previousCalendar.get(Calendar.YEAR) // previousYear 값을 계산해서 저장함
    val previousMonth = previousCalendar.get(Calendar.MONTH) + 1 // previousMonth 값을 계산해서 저장함
    val daysInPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) // daysInPreviousMonth 값을 계산해서 저장함

    for (day in (daysInPreviousMonth - leadingDays + 1)..daysInPreviousMonth) { // 이 블록의 내용이 여기서 시작됨
        result.add( // 바로 앞 설정을 이어서 적음
            CalendarDateData( // previousYear 값을 이 함수로 넘김
                fullDate = formatDate(previousYear, previousMonth, day), // 바로 앞 설정을 이어서 적음
                dayText = day.toString(), // dayText 값을 이 함수로 넘김
                isCurrentMonth = false // 바로 앞 설정을 이어서 적음
            )
        )
    } // 블록 끝

    for (day in 1..daysInCurrentMonth) { // 이 블록의 내용이 여기서 시작됨
        result.add( // 바로 앞 설정을 이어서 적음
            CalendarDateData( // isCurrentMonth 값을 이 함수로 넘김
                fullDate = formatDate(year, month, day), // 바로 앞 설정을 이어서 적음
                dayText = day.toString(), // dayText 값을 이 함수로 넘김
                isCurrentMonth = true // isCurrentMonth 값을 이 함수로 넘김
            )
        )
    } // 블록 끝

    val remain = result.size % 7 // remain 값을 계산해서 저장함
    val trailingDays = if (remain == 0) 0 else 7 - remain // trailingDays 값을 계산해서 저장함

    val nextCalendar = currentCalendar.clone() as Calendar // nextCalendar 값을 계산해서 저장함
    nextCalendar.add(Calendar.MONTH, 1) // 바로 앞 설정을 이어서 적음
    val nextYear = nextCalendar.get(Calendar.YEAR) // nextYear 값을 계산해서 저장함
    val nextMonth = nextCalendar.get(Calendar.MONTH) + 1 // nextMonth 값을 계산해서 저장함

    for (day in 1..trailingDays) { // 이 블록의 내용이 여기서 시작됨
        result.add( // 바로 앞 설정을 이어서 적음
            CalendarDateData( // nextYear 값을 이 함수로 넘김
                fullDate = formatDate(nextYear, nextMonth, day), // 바로 앞 설정을 이어서 적음
                dayText = day.toString(), // dayText 값을 이 함수로 넘김
                isCurrentMonth = false // isCurrentMonth 값을 이 함수로 넘김
            )
        )
    } // 블록 끝

    return result // 계산한 결과를 바깥으로 돌려줌
} // 블록 끝

private fun formatDate(year: Int, month: Int, day: Int): String { // formatDate 함수 시작
    return String.format("%04d-%02d-%02d", year, month, day) // 계산한 결과를 바깥으로 돌려줌
} // 블록 끝

private fun createChangeRateText(currentAmount: Int, previousAmount: Int): String { // createChangeRateText 함수 시작
    return if (previousAmount == 0) { // 이 블록의 내용이 여기서 시작됨
        when { // 바로 앞 설정을 이어서 적음
            currentAmount == 0 -> "지난달과 동일 0%" // 이 조건이면 오른쪽 값을 선택함
            else -> "↗ 지난달 대비 신규 소비" // 이 조건이면 오른쪽 값을 선택함
        } // 블록 끝
    } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
        val rate = (((currentAmount - previousAmount).toFloat() / previousAmount.toFloat()) * 100).toInt() // rate 숫자 값으로 계산함
        when { // 바로 앞 설정을 이어서 적음
            rate > 0 -> "↗ 지난달 대비 +${rate}%" // 이 조건이면 오른쪽 값을 선택함
            rate < 0 -> "↘ 지난달 대비 -${abs(rate)}%" // 음수를 양수로 바꿔 절댓값으로 만듦
            else -> "→ 지난달 대비 0%" // 이 조건이면 오른쪽 값을 선택함
        } // 블록 끝
    } // 블록 끝
} // 블록 끝

@Composable
private fun getChangeRateColor(changeRateText: String): Color { // getChangeRateColor 함수 시작
    return when { // 계산한 결과를 바깥으로 돌려줌
        changeRateText.contains("↗") -> Color(0xFFE53935) // 그 값이 들어있는지 확인함
        changeRateText.contains("↘") -> Color(0xFF16A34A) // 그 값이 들어있는지 확인함
        else -> MaterialTheme.colorScheme.onSurfaceVariant // currentAmount 값을 이 함수로 넘김
    } // 블록 끝
} // 블록 끝

// 사용률 텍스트를 만드는 함수입니다.
// 기존에는 Int로 바로 잘라서 0.2% 같은 값이 0%가 되었기 때문에
// 이제는 소수 1자리까지 보여주도록 문자열로 만듭니다.
private fun createUsageRateText(currentAmount: Int, monthlyBudget: Long): String { // createUsageRateText 함수 시작
    // 예산이 0 이하이면 나눗셈이 불가능하므로 0%로 처리합니다.
    if (monthlyBudget <= 0) { // 조건이 참일 때만 아래 코드를 실행함
        return "0%" // 계산한 결과를 바깥으로 돌려줌
    } // 블록 끝

    // 사용률을 Double로 계산합니다.
    val rate = (currentAmount.toDouble() / monthlyBudget.toDouble()) * 100.0 // rate 값을 계산해서 저장함

    // 정수로 딱 떨어지면 소수점 없이 보여줍니다.
    return if (rate % 1.0 == 0.0) { // 이 블록의 내용이 여기서 시작됨
        "${rate.toInt()}%" // rate 값을 이 함수로 넘김
    } else { // 조건이 거짓일 때 실행할 부분으로 넘어감
        String.format("%.1f%%", rate) // 자릿수를 맞춘 문자열을 만듦
    } // 블록 끝
} // 블록 끝
