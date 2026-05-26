package com.ict.spentopia.feature.budget // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.animation.core.LinearEasing // 일정한 속도 애니메이션을 가져옴
import androidx.compose.animation.core.RepeatMode // 반복 방향 설정을 가져옴
import androidx.compose.animation.core.animateFloat // Float 애니메이션을 가져옴
import androidx.compose.animation.core.infiniteRepeatable // 무한 반복 애니메이션을 가져옴
import androidx.compose.animation.core.rememberInfiniteTransition // 반복 애니메이션 상태를 기억함
import androidx.compose.animation.core.tween // 시간 기반 애니메이션을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.PaddingValues // PaddingValues 기능을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.navigationBarsPadding // navigationBarsPadding 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.layout.width // width 기능을 가져옴
import androidx.compose.foundation.rememberScrollState // rememberScrollState 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.verticalScroll // verticalScroll 기능을 가져옴
import androidx.compose.foundation.lazy.LazyColumn // 세로 스크롤 목록을 가져옴
import androidx.compose.foundation.lazy.rememberLazyListState // rememberLazyListState 기능을 가져옴
import androidx.compose.foundation.text.KeyboardOptions // KeyboardOptions 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.filled.AutoAwesome // AutoAwesome 기능을 가져옴
import androidx.compose.material.icons.filled.DateRange // DateRange 기능을 가져옴
import androidx.compose.material.icons.filled.FavoriteBorder // FavoriteBorder 기능을 가져옴
import androidx.compose.material.icons.filled.Home // Home 기능을 가져옴
import androidx.compose.material.icons.filled.KeyboardArrowDown // KeyboardArrowDown 기능을 가져옴
import androidx.compose.material.icons.filled.Lightbulb // Lightbulb 기능을 가져옴
import androidx.compose.material.icons.filled.Restaurant // Restaurant 기능을 가져옴
import androidx.compose.material.icons.filled.Savings // Savings 기능을 가져옴
import androidx.compose.material.icons.filled.SentimentSatisfied // SentimentSatisfied 기능을 가져옴
import androidx.compose.material.icons.filled.Subway // Subway 기능을 가져옴
import androidx.compose.material.icons.filled.TrendingUp // TrendingUp 기능을 가져옴
import androidx.compose.material.icons.filled.VolunteerActivism // VolunteerActivism 기능을 가져옴
import androidx.compose.material.icons.filled.Warning // Warning 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.CircularProgressIndicator // CircularProgressIndicator 기능을 가져옴
import androidx.compose.material3.HorizontalDivider // HorizontalDivider 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.IconButton // 아이콘 버튼 컴포넌트를 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.Slider // Slider 기능을 가져옴
import androidx.compose.material3.SliderDefaults // SliderDefaults 기능을 가져옴
import androidx.compose.material3.Surface // Surface 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.TextButton // TextButton 기능을 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableFloatStateOf // mutableFloatStateOf 기능을 가져옴
import androidx.compose.runtime.mutableIntStateOf // mutableIntStateOf 기능을 가져옴
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.clip // clip 기능을 가져옴
import androidx.compose.ui.draw.shadow // shadow 기능을 가져옴
import androidx.compose.ui.geometry.Offset // Offset 기능을 가져옴
import androidx.compose.ui.graphics.Brush // 그라데이션 색칠 도구를 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.graphics.vector.ImageVector // ImageVector 기능을 가져옴
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.text.style.TextAlign // TextAlign 기능을 가져옴
import androidx.compose.ui.text.input.KeyboardType // KeyboardType 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.window.Dialog // Dialog 기능을 가져옴
import androidx.lifecycle.compose.collectAsStateWithLifecycle // ViewModel 상태를 화면에서 안전하게 받는 도구를 가져옴
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel 연결하는 도구를 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // Solana 지갑 종류를 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground // SpentopiaDarkBackground 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple // SpentopiaGlowPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.spentopiaAppButtonColor
import com.ict.spentopia.ui.theme.spentopiaAppButtonContentColor
import com.ict.spentopia.ui.toast.AppToastType
import com.ict.spentopia.ui.toast.showAppToast
import java.util.Calendar // Calendar 기능을 가져옴

private const val MAX_BUDGET_AMOUNT = 999_999_999_999L // 예산 관련 값을 저장함

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun isBudgetDarkTheme(): Boolean { // isBudgetDarkTheme 함수를 선언함
    return MaterialTheme.colorScheme.background == SpentopiaDarkBackground // 이 값을 함수 결과로 돌려줌
}

@Composable
private fun budgetSoftCardColor(): Color {
    return if (isBudgetDarkTheme()) Color(0xFF171A2B) else Color(0xFFF7FBFF)
}

@Composable
private fun budgetSoftCardBorderColor(): Color {
    return if (isBudgetDarkTheme()) Color(0xFF4C3B7A) else Color(0xFF7DD3FC)
}

@Composable
private fun budgetSoftInnerCardColor(): Color {
    return if (isBudgetDarkTheme()) Color(0xFF1A2233) else Color(0xFFFFFFFF)
}

@Composable
private fun budgetPrimaryButtonColor(): Color {
    return spentopiaAppButtonColor(isBudgetDarkTheme())
}

@Composable
private fun budgetPrimaryButtonContentColor(): Color {
    return spentopiaAppButtonContentColor(isBudgetDarkTheme())
}

@Composable
private fun BudgetPaymentRequiredDialog(
    isWalletConnected: Boolean,
    onDismiss: () -> Unit,
    onConnectPhantomClick: () -> Unit,
    onConnectSolflareClick: () -> Unit,
    onPaymentClick: () -> Unit
) {
    val isDark = isBudgetDarkTheme()
    val surfaceColor = if (isDark) Color(0xFF171A2B) else Color(0xFFFFFFFF)
    val innerSurfaceColor = if (isDark) Color(0xFF101323) else Color(0xFFF7FBFF)
    val borderColor = if (isDark) Color(0xFF6D5AA8) else Color(0xFF93C5FD)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val bodyColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val primaryButtonColor = budgetPrimaryButtonColor()
    val primaryButtonContentColor = budgetPrimaryButtonContentColor()
    val secondaryButtonColor = if (isDark) Color(0xFF25243A) else Color(0xFFEFF6FF)
    val secondaryTextColor = if (isDark) Color(0xFFEDE9FE) else Color(0xFF1D4ED8)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWalletConnected) "AI 추천 결제" else "지갑 연결 필요",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )

                    Text(
                        text = "닫기",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(innerSurfaceColor, RoundedCornerShape(10.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(innerSurfaceColor, RoundedCornerShape(18.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isWalletConnected) "결제 승인" else "지갑 연결",
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isWalletConnected) {
                                "추가 분석을 하기 위해서는 결제가 필요합니다. 결제를 진행해주세요."
                            } else {
                                "추가 분석을 하기 위해서는 결제가 필요합니다. 먼저 지갑을 연결해주세요."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isWalletConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = secondaryButtonColor,
                                contentColor = secondaryTextColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("결제 취소", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPaymentClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryButtonColor,
                                contentColor = primaryButtonContentColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("결제하기", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onConnectPhantomClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryButtonColor,
                                contentColor = primaryButtonContentColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Phantom 지갑 연결", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onConnectSolflareClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = secondaryButtonColor,
                                contentColor = secondaryTextColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Solflare 지갑 연결", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 예산 설정 화면임
// AI 추천 플랜/직접 조절/저장 흐름
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun BudgetScreen( // BudgetScreen 함수를 선언함
    // ViewModel을 연결해서 화면 상태를 가져옴
    viewModel: BudgetViewModel = viewModel(), // 화면 데이터 관리자를 받음
    isWalletConnected: Boolean = false, // 지갑 연결 여부를 받음
    onWalletConnectClick: (SolanaWalletType) -> Unit = {} // 지갑 연결 요청을 받음
) { // 이 블록 안의 내용이 시작됨
    // 세로 스크롤 상태 저장
    val scrollState = rememberScrollState() // 화면이 다시 그려져도 scrollState 값을 기억함

    val context = LocalContext.current // 예산 저장 결과 토스트를 표시할 현재 화면 정보를 가져옴

    // ViewModel의 budgetState를 화면에서 안전하게 구독
    val budgetState by viewModel.budgetState.collectAsStateWithLifecycle() // 예산 관련 값을 저장함

    // 저장 성공 여부 상태를 화면에서 안전하게 구독
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle() // 저장 성공 여부를 저장함

    // 예산 저장 실패 문구를 구독해서 오류 토스트로 표시할 때 사용합니다.
    val saveError by viewModel.saveError.collectAsStateWithLifecycle() // 오류 내용을 저장함

    val aiPlanList by viewModel.aiPlanList.collectAsStateWithLifecycle() // AI 추천 플랜 목록을 저장함

    val isAiPlanLoading by viewModel.isAiPlanLoading.collectAsStateWithLifecycle() // 로딩 상태를 저장함

    val aiPlanError by viewModel.aiPlanError.collectAsStateWithLifecycle() // 오류 내용을 저장함

    val isPaymentRequired by viewModel.isPaymentRequired.collectAsStateWithLifecycle() // 결제 팝업 표시 여부를 저장함

    val currentCalendar = remember { Calendar.getInstance() } // 화면이 다시 그려져도 currentCalendar 값을 기억함
    val currentYear = remember { currentCalendar.get(Calendar.YEAR) } // 화면이 다시 그려져도 currentYear 값을 기억함
    val currentMonth = remember { currentCalendar.get(Calendar.MONTH) + 1 } // 화면이 다시 그려져도 currentMonth 값을 기억함
    var selectedYear by remember { mutableIntStateOf(currentYear) } // 화면이 다시 그려져도 selectedYear 값을 기억함
    var selectedMonth by remember { mutableIntStateOf(currentMonth) } // 화면이 다시 그려져도 selectedMonth 값을 기억함
    var isMonthDialogOpen by remember { mutableStateOf(false) } // 화면에서 바뀔 월 선택창이 열렸는지 저장함
    var pendingApplyPlan by remember { mutableStateOf<BudgetPlanUiData?>(null) } // 적용 확인 대기 중인 플랜을 저장함
    val selectedMonthKey = "%04d-%02d".format(selectedYear, selectedMonth) // 선택 월 키를 저장함
    val currentMonthKey = "%04d-%02d".format(currentYear, currentMonth) // 현재 월 키를 저장함
    val canEditBudget = selectedMonthKey == currentMonthKey && budgetState.lockedMonthKey != currentMonthKey // 이번 달 1회만 수정 가능함

    // 총 지출 예정 금액 계산
    // 식비 + 교통비 + 생활비 + 취미를 다 합쳐서
    // 이번 달 카테고리 예산이 얼마나 되는지 먼저 봅니다.
    val totalExpense = budgetState.foodBudget + // 소비 내역 값을 저장함
            budgetState.transportBudget +
            budgetState.livingBudget +
            budgetState.hobbyBudget

    // 남는 금액 계산
    // 월 수입에서 카테고리 예산과 저축 목표를 빼서
    // 실제로 자유롭게 쓸 수 있는 돈이 얼마인지 구합니다.
    val remainingAmount = budgetState.monthlyIncome - totalExpense - budgetState.savingGoal // remainingAmount 값을 저장함

    // 저장 결과는 등록/저장 성공용 체크 토스트와 오류 토스트로 표시합니다.
    LaunchedEffect(saveSuccess) { // 화면이 열리거나 값이 바뀔 때 실행함
        if (saveSuccess) { // 조건이 맞는지 확인함
            showAppToast(context, "예산 설정이 저장되었어요.", AppToastType.SUCCESS) // 저장 완료는 체크 아이콘 토스트로 보여줌
            viewModel.resetSaveSuccess()
        }
    }

    LaunchedEffect(saveError) { // 화면이 열리거나 값이 바뀔 때 실행함
        if (saveError.isNotBlank()) { // 조건이 맞는지 확인함
            showAppToast(context, saveError, AppToastType.ERROR) // 저장 실패는 오류 아이콘 토스트로 보여줌
            viewModel.resetSaveError()
        }
    }

    if (isPaymentRequired) { // AI 예산 플랜 결제가 필요할 때 결제 안내 팝업을 표시함
        BudgetPaymentRequiredDialog( // 지갑 연결 또는 결제 진행을 선택할 수 있는 팝업을 보여줌
            isWalletConnected = isWalletConnected, // 현재 지갑 연결 상태를 팝업에 전달함
            onDismiss = { viewModel.dismissPaymentDialog() }, // 닫기 동작 시 결제 필요 팝업 상태를 해제함
            onConnectPhantomClick = { onWalletConnectClick(SolanaWalletType.PHANTOM) }, // Phantom 연결 선택을 바깥 흐름으로 전달함
            onConnectSolflareClick = { onWalletConnectClick(SolanaWalletType.SOLFLARE) }, // Solflare 연결 선택을 바깥 흐름으로 전달함
            onPaymentClick = { // 모바일 결제가 준비되지 않은 경우 안내 문구를 상태에 기록함
                viewModel.showMobilePaymentNotReadyMessage() // 현재 지원되지 않는 결제 안내를 사용자에게 보여주게 함
                viewModel.dismissPaymentDialog() // 안내 처리 후 결제 요청 팝업은 닫음
            }
        )
    }

    // 화면 전체 배경
    Surface( // Surface 함수를 실행함
        modifier = Modifier.fillMaxSize(), // UI 크기나 여백 같은 모양을 정함
        color = MaterialTheme.colorScheme.background // color 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier.fillMaxSize() // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp) // .padding(horizontal 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                // 상단 제목 영역
                BudgetTopSection() // Budget Top Section 함수를 실행함

                Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

                MonthSelectorCard( // 내용을 카드 모양으로 묶어서 보여줌
                    year = selectedYear, // selectedYear 값을 year 값에 넣음
                    month = selectedMonth, // selectedMonth 값을 month 값에 넣음
                    onOpenClick = { // onOpenClick 때 실행할 함수를 정해줌
                        isMonthDialogOpen = true // true 값을 isMonthDialogOpen인지 여부에 넣음
                    }
                )

                Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

                // AI 추천 플랜 제목 + 수동 요청 버튼
                AiPlanSectionHeader( // Ai Plan Section Header 함수를 실행함
                    title = "AI 추천 플랜", // 제목을 정해줌
                    icon = "✨", // icon 값을 정해줌
                    isLoading = isAiPlanLoading, // 로딩 상태를 로딩 여부에 넣음
                    enabled = canEditBudget, // 이번 달 예산 적용 가능 여부를 넣음
                    onAiRecommendClick = { // onAiRecommendClick 때 실행할 함수를 정해줌
                        viewModel.requestAiRecommendedPlans()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

                if (isAiPlanLoading && aiPlanList.isEmpty()) { // 조건이 맞는지 확인함
                    AiPlanStatusCard( // 내용을 카드 모양으로 묶어서 보여줌
                        message = "AI가 예산 플랜을 생성하고 있어요.", // 메시지를 정해줌
                        isLoading = true, // true 값을 로딩 여부에 넣음
                        onRetryClick = {} // onRetryClick 때 실행할 함수를 정해줌
                    )
                    Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함
                } else if (aiPlanError.isNotBlank() && aiPlanList.isEmpty()) { // 이 블록 안의 내용이 시작됨
                    AiPlanStatusCard( // 내용을 카드 모양으로 묶어서 보여줌
                        message = aiPlanError, // 오류 내용을 메시지에 넣음
                        isLoading = false, // false 값을 로딩 여부에 넣음
                        onRetryClick = { // onRetryClick 때 실행할 함수를 정해줌
                            viewModel.requestAiRecommendedPlans()
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함
                } else { // 이 블록 안의 내용이 시작됨
                    // 서버 AI 추천 플랜 목록 출력
                    aiPlanList.forEach { plan ->
                        BudgetPlanCard( // 내용을 카드 모양으로 묶어서 보여줌
                            plan = plan, // 추천 플랜을 추천 플랜에 넣음
                            enabled = canEditBudget, // 적용 가능 여부를 넣음
                            onApplyClick = { // onApplyClick 때 실행할 함수를 정해줌
                                pendingApplyPlan = plan // 플랜 적용 확인창을 열어줌
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함
                    }
                }

                Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

                // 맞춤 예산 설정 제목
                SectionHeader( // Section Header 함수를 실행함
                    title = "맞춤 예산 설정", // 제목을 정해줌
                    icon = "◎" // icon 값을 정해줌
                )

                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

                // 직접 슬라이더로 예산 조절하는 카드
                CustomBudgetSettingCard( // 내용을 카드 모양으로 묶어서 보여줌
                    monthlyIncome = budgetState.monthlyIncome, // 월 수입을 정해줌
                    savingGoal = budgetState.savingGoal, // 저축 목표를 정해줌
                    foodBudget = budgetState.foodBudget, // 식비 예산을 정해줌
                    transportBudget = budgetState.transportBudget, // 교통비 예산을 정해줌
                    livingBudget = budgetState.livingBudget, // 생활비 예산을 정해줌
                    hobbyBudget = budgetState.hobbyBudget, // 취미 예산을 정해줌
                    onMonthlyIncomeChange = viewModel::updateMonthlyIncome, // onMonthlyIncomeChange 때 실행할 함수를 정해줌
                    onSavingGoalChange = viewModel::updateSavingGoal, // onSavingGoalChange 때 실행할 함수를 정해줌
                    onFoodBudgetChange = viewModel::updateFoodBudget, // 예산 관련 값을 정해줌
                    onTransportBudgetChange = viewModel::updateTransportBudget, // 예산 관련 값을 정해줌
                    onLivingBudgetChange = viewModel::updateLivingBudget, // 예산 관련 값을 정해줌
                    onHobbyBudgetChange = viewModel::updateHobbyBudget, // 예산 관련 값을 정해줌
                    isSaveEnabled = canEditBudget, // 저장 가능 여부를 넣음
                    onSaveClick = { // onSaveClick 때 실행할 함수를 정해줌
                        // 저장 버튼 클릭 시 실행
                        viewModel.saveBudgetSettings()
                    }
                )

                Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

                CurrentMonthlyBudgetCard( // 내용을 카드 모양으로 묶어서 보여줌
                    year = selectedYear, // selectedYear 값을 year 값에 넣음
                    month = selectedMonth, // selectedMonth 값을 month 값에 넣음
                    monthlyBudget = budgetState.monthlyIncome // 예산 관련 값을 정해줌
                )

                Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

                // 예산 요약 카드
                BudgetSummaryCard( // 내용을 카드 모양으로 묶어서 보여줌
                    monthlyIncome = budgetState.monthlyIncome, // 월 수입을 정해줌
                    totalExpense = totalExpense, // 소비 내역 값을 소비 내역 값에 넣음
                    savingGoal = budgetState.savingGoal, // 저축 목표를 정해줌
                    remainingAmount = remainingAmount // remainingAmount 값을 remainingAmount 값에 넣음
                )

                Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

                BudgetCommentCard( // 내용을 카드 모양으로 묶어서 보여줌
                    monthlyIncome = budgetState.monthlyIncome, // 월 수입을 정해줌
                    totalExpense = totalExpense, // 소비 내역 값을 소비 내역 값에 넣음
                    savingGoal = budgetState.savingGoal, // 저축 목표를 정해줌
                    remainingAmount = remainingAmount // remainingAmount 값을 remainingAmount 값에 넣음
                )

                Spacer(modifier = Modifier.height(24.dp)) // UI 크기나 여백 같은 모양을 정함
            }

            if (isMonthDialogOpen) { // 조건이 맞는지 확인함
                MonthPickerDialog( // Month Picker Dialog 함수를 실행함
                    selectedYear = selectedYear, // selectedYear 값을 selectedYear 값에 넣음
                    selectedMonth = selectedMonth, // selectedMonth 값을 selectedMonth 값에 넣음
                    onDismiss = { // 닫을 때 실행할 함수를 정해줌
                        isMonthDialogOpen = false // false 값을 isMonthDialogOpen인지 여부에 넣음
                    },
                    onYearMonthSelected = { year, month -> // onYearMonthSelected 때 실행할 함수를 정해줌
                        selectedYear = year // year 값을 selectedYear 값에 넣음
                        selectedMonth = month // month 값을 selectedMonth 값에 넣음
                        isMonthDialogOpen = false // false 값을 isMonthDialogOpen인지 여부에 넣음
                    }
                )
            }

            pendingApplyPlan?.let { plan -> // 조건이 맞는지 확인함
                BudgetApplyPlanDialog( // Budget Apply Plan Dialog 함수를 실행함
                    plan = plan, // 선택한 플랜을 넣음
                    onDismiss = { pendingApplyPlan = null }, // 닫을 때 실행할 함수를 정해줌
                    onConfirm = {
                        viewModel.applyPlan(plan) // 선택한 플랜을 적용함
                        pendingApplyPlan = null // 확인 대기 플랜을 비움
                    }
                )
            }

        }
    }
}

// 화면 상단 제목 영역
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetTopSection() { // BudgetTopSection 함수를 선언함
    val isDark = isBudgetDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val guideColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF53657D) // 안내 문구 색을 모드별로 분리함
    val guideStrongColor = if (isDark) Color(0xFFD8B4FE) else Color(0xFF2563EB) // 강조 문구 색을 모드별로 분리함
    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = "예산 설정", // text 값을 정해줌
            style = MaterialTheme.typography.headlineMedium, // style 값을 정해줌
            fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
            color = titleColor // color 값을 정해줌
        )

        Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

        Row( // 안쪽 UI를 가로로 배치함
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "AI가 추천하는 플랜으로 시작하거나 ", // text 값을 정해줌
                style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
                color = guideColor // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = "직접 설정해보세요", // text 값을 정해줌
                style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = guideStrongColor // color 값을 정해줌
            )
        }
    }
}

// 섹션 제목 공통 UI
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SectionHeader( // SectionHeader 함수를 선언함
    title: String, // 제목을 받음
    icon: String // icon 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme()
    val sectionColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    Row( // 안쪽 UI를 가로로 배치함
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = icon, // icon 값을 text 값에 넣음
            style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
            color = sectionColor
        )

        Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = title, // 제목을 text 값에 넣음
            style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
            color = sectionColor // color 값을 정해줌
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MonthSelectorCard( // MonthSelectorCard 함수를 선언함
    year: Int, // year 값을 받음
    month: Int, // month 값을 받음
    onOpenClick: () -> Unit // onOpenClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = if (isDark) budgetSoftCardColor() else Color(0xFFF8FBFF) // containerColor 값을 정해줌
        ),
        border = if (isDark) BorderStroke(1.dp, budgetSoftCardBorderColor()) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp), // .padding(horizontal 값을 정해줌
            horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Default.DateRange, // imageVector 값을 정해줌
                    contentDescription = "월 선택", // contentDescription 값을 정해줌
                    tint = MaterialTheme.colorScheme.primary // tint 값을 정해줌
                )

                Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = "${year}년 ${month}월", // text 값을 정해줌
                    style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }

            IconButton(onClick = onOpenClick) { // 누를 수 있는 버튼을 만듦
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Default.KeyboardArrowDown, // imageVector 값을 정해줌
                    contentDescription = "월 선택 열기", // contentDescription 값을 정해줌
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                )
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MonthPickerDialog( // MonthPickerDialog 함수를 선언함
    selectedYear: Int, // selectedYear 값을 받음
    selectedMonth: Int, // selectedMonth 값을 받음
    onDismiss: () -> Unit, // 닫을 때 실행할 함수를 받음
    onYearMonthSelected: (Int, Int) -> Unit
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함
    val listState = rememberLazyListState() // 화면이 다시 그려져도 listState 값을 기억함
    var dialogYear by remember(selectedYear) { mutableIntStateOf(selectedYear) } // 화면이 다시 그려져도 dialogYear 값을 기억함

    LaunchedEffect(selectedMonth) { // 화면이 열리거나 값이 바뀔 때 실행함
        listState.scrollToItem((selectedMonth - 1).coerceIn(0, 11))
    }

    Dialog(onDismissRequest = onDismiss) { // Dialog(onDismissRequest 값을 정해줌
        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(22.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = if (isDark) budgetSoftCardColor() else MaterialTheme.colorScheme.surface), // colors 값을 정해줌
            border = if (isDark) BorderStroke(1.dp, budgetSoftCardBorderColor()) else null
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .padding(20.dp)
            ) { // 이 블록 안의 내용이 시작됨
                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                    verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "년 / 월 선택", // text 값을 정해줌
                        style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                    )

                    Row( // 안쪽 UI를 가로로 배치함
                        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { dialogYear -= 1 } // - 값을 정해줌
                                .padding(horizontal = 10.dp, vertical = 6.dp), // .padding(horizontal 값을 정해줌
                            text = "이전", // text 값을 정해줌
                            style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                            color = MaterialTheme.colorScheme.primary // color 값을 정해줌
                        )

                        Text( // 화면에 글자를 보여줌
                            text = "${dialogYear}년", // text 값을 정해줌
                            style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                            fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                        )

                        Text( // 화면에 글자를 보여줌
                            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { dialogYear += 1 } // + 값을 정해줌
                                .padding(horizontal = 10.dp, vertical = 6.dp), // .padding(horizontal 값을 정해줌
                            text = "다음", // text 값을 정해줌
                            style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                            color = MaterialTheme.colorScheme.primary // color 값을 정해줌
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            color = if (isDark) budgetSoftInnerCardColor() else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f), // color 값을 정해줌
                            shape = RoundedCornerShape(16.dp) // shape 값을 정해줌
                        )
                        .padding(vertical = 8.dp) // .padding(vertical 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    LazyColumn( // 안쪽 UI를 세로로 배치함
                        state = listState, // listState 값을 상태값에 넣음
                        modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
                    ) { // 이 블록 안의 내용이 시작됨
                        items(12) { index -> // items 함수를 실행함
                            val month = index + 1 // month 값을 저장함
                            val selected = month == selectedMonth // selected 값을 저장함
                            Row( // 안쪽 UI를 가로로 배치함
                                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp) // .padding(horizontal 값을 정해줌
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        color = if (selected) { // color 값을 정해줌
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else { // 이 블록 안의 내용이 시작됨
                                            Color.Transparent
                                        }
                                    )
                                    .clickable { // 이 블록 안의 내용이 시작됨
                                        onYearMonthSelected(dialogYear, month) // on Year Month Selected 함수를 실행함
                                    }
                                    .padding(horizontal = 16.dp), // .padding(horizontal 값을 정해줌
                                horizontalArrangement = Arrangement.Center, // horizontalArrangement 값을 정해줌
                                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                            ) { // 이 블록 안의 내용이 시작됨
                                Text( // 화면에 글자를 보여줌
                                    text = "${month}월", // text 값을 정해줌
                                    style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium, // fontWeight 값을 정해줌
                                    color = if (selected) { // color 값을 정해줌
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else { // 이 블록 안의 내용이 시작됨
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

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetApplyPlanDialog( // BudgetApplyPlanDialog 함수를 선언함
    plan: BudgetPlanUiData, // 추천 플랜을 받음
    onDismiss: () -> Unit, // 닫을 때 실행할 함수를 받음
    onConfirm: () -> Unit // 적용할 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val progressBrush = Brush.linearGradient( // 성실도 팝업과 같은 느낌의 진행 색상임
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            SpentopiaGlowPurple
        )
    )

    Dialog(onDismissRequest = onDismiss) { // Dialog 함수를 실행함
        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(24.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = budgetSoftCardColor()), // colors 값을 정해줌
            border = BorderStroke(1.dp, budgetSoftCardBorderColor()) // border 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                    verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "예산 플랜 적용", // text 값을 정해줌
                        style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                        fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                    )

                    TextButton(onClick = onDismiss) { // 누를 수 있는 버튼을 만듦
                        Text(text = "닫기") // text 값을 정해줌
                    }
                }

                Text( // 화면에 글자를 보여줌
                    text = plan.title, // text 값을 정해줌
                    style = MaterialTheme.typography.headlineSmall, // style 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.primary // color 값을 정해줌
                )

                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(999.dp)
                        )
                ) { // 이 블록 안의 내용이 시작됨
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                brush = progressBrush,
                                shape = RoundedCornerShape(999.dp)
                            )
                    )
                }

                InfoValueCard( // 내용을 카드 모양으로 묶어서 보여줌
                    label = "월 예산", // label 값을 정해줌
                    value = formatWon(plan.monthlyBudget), // 입력값을 정해줌
                    containerColor = budgetSoftInnerCardColor(), // containerColor 값을 정해줌
                    valueColor = MaterialTheme.colorScheme.onSurface // valueColor 값을 정해줌
                )

                InfoValueCard( // 내용을 카드 모양으로 묶어서 보여줌
                    label = "목표 저축", // label 값을 정해줌
                    value = formatWon(plan.savingGoal), // 입력값을 정해줌
                    containerColor = budgetSoftInnerCardColor(), // containerColor 값을 정해줌
                    valueColor = MaterialTheme.colorScheme.onSurface // valueColor 값을 정해줌
                )

                Text( // 화면에 글자를 보여줌
                    text = "예산 설정은 월 1회만 적용 가능합니다. 이 플랜을 적용하시겠습니까?", // text 값을 정해줌
                    style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )

                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Button( // 누를 수 있는 버튼을 만듦
                        onClick = onDismiss, // onDismiss 값을 눌렀을 때 실행할 함수에 넣음
                        modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { // 이 블록 안의 내용이 시작됨
                        Text(text = "취소", fontWeight = FontWeight.Bold) // text 값을 정해줌
                    }

                    Button( // 누를 수 있는 버튼을 만듦
                        onClick = onConfirm, // onConfirm 값을 눌렀을 때 실행할 함수에 넣음
                        modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                            containerColor = budgetPrimaryButtonColor(),
                            contentColor = budgetPrimaryButtonContentColor()
                        )
                    ) { // 이 블록 안의 내용이 시작됨
                        Text(text = "적용", fontWeight = FontWeight.Bold) // text 값을 정해줌
                    }
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AiPlanSectionHeader( // AiPlanSectionHeader 함수를 선언함
    title: String, // 제목을 받음
    icon: String, // icon 값을 받음
    isLoading: Boolean, // 로딩 여부를 받음
    enabled: Boolean = true, // 버튼 활성화 여부를 받음
    onAiRecommendClick: () -> Unit // onAiRecommendClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val buttonColor = budgetPrimaryButtonColor() // 오늘의 소비일기 카드와 어울리는 버튼 색을 씀
    val buttonContentColor = budgetPrimaryButtonContentColor()
    val isButtonEnabled = enabled && !isLoading // 실제 버튼 활성화 여부를 저장함
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = icon, // icon 값을 text 값에 넣음
                style = MaterialTheme.typography.titleLarge // style 값을 정해줌
            )

            Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onBackground // color 값을 정해줌
            )
        }

        Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함

        Button( // 누를 수 있는 버튼을 만듦
            onClick = onAiRecommendClick, // onAiRecommendClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
            enabled = isButtonEnabled, // enabled 값을 정해줌
            modifier = Modifier.height(40.dp), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
            colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                containerColor = buttonColor, // containerColor 값을 정해줌
                contentColor = buttonContentColor, // contentColor 값을 정해줌
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, // disabledContainerColor 값을 정해줌
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant // disabledContentColor 값을 정해줌
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp), // contentPadding 값을 정해줌
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) // elevation 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            if (isLoading) { // 조건이 맞는지 확인함
                CircularProgressIndicator( // Circular Progress Indicator 함수를 실행함
                modifier = Modifier.size(14.dp), // UI 크기나 여백 같은 모양을 정함
                strokeWidth = 2.dp, // strokeWidth 값을 정해줌
                    color = if (isButtonEnabled) buttonContentColor else MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
                Spacer(modifier = Modifier.width(6.dp)) // UI 크기나 여백 같은 모양을 정함
            }

            Text( // 화면에 글자를 보여줌
                text = if (isLoading) "추천 중" else "AI 추천", // text 값을 정해줌
                style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = if (isButtonEnabled) buttonContentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 추천 플랜 카드 UI
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AiPlanStatusCard( // AiPlanStatusCard 함수를 선언함
    message: String, // 메시지를 받음
    isLoading: Boolean, // 로딩 여부를 받음
    onRetryClick: () -> Unit // onRetryClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val cardColor = budgetSoftCardColor() // AI 추천 상태 카드를 오늘의 소비일기 카드 계열로 맞춤
    val cardBorderColor = budgetSoftCardBorderColor() // AI 추천 상태 카드 테두리색을 맞춤
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = cardColor), // colors 값을 정해줌
        border = BorderStroke(1.dp, cardBorderColor), // AI 추천 카드 테두리색을 맞춤
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            if (isLoading) { // 조건이 맞는지 확인함
                CircularProgressIndicator( // Circular Progress Indicator 함수를 실행함
                    color = MaterialTheme.colorScheme.primary, // color 값을 정해줌
                    strokeWidth = 2.dp // strokeWidth 값을 정해줌
                )
                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
            }

            Text( // 화면에 글자를 보여줌
                text = message, // 메시지를 text 값에 넣음
                style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                fontWeight = FontWeight.Medium // fontWeight 값을 정해줌
            )

            if (!isLoading) { // 조건이 맞는지 확인함
                Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함
                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onRetryClick, // onRetryClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = budgetPrimaryButtonColor(), // containerColor 값을 정해줌
                        contentColor = budgetPrimaryButtonContentColor() // contentColor 값을 정해줌
                    )
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "다시 요청", // text 값을 정해줌
                        fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                    )
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetPlanCard( // BudgetPlanCard 함수를 선언함
    plan: BudgetPlanUiData, // 추천 플랜을 받음
    enabled: Boolean = true, // 적용 버튼 활성화 여부를 받음
    onApplyClick: () -> Unit // onApplyClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함
    val cardColor = budgetSoftCardColor() // AI 추천 플랜 카드를 오늘의 소비일기 카드 계열로 맞춤
    val cardBorderColor = budgetSoftCardBorderColor() // AI 추천 플랜 카드 테두리색을 맞춤
    val buttonColor = budgetPrimaryButtonColor() // 플랜 적용 버튼색을 통일함
    val buttonContentColor = budgetPrimaryButtonContentColor()

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = cardColor // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, cardBorderColor), // AI 추천 카드 테두리색을 맞춤
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(16.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = plan.title, // text 값을 정해줌
                style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = plan.description, // text 값을 정해줌
                style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            InfoValueCard( // 내용을 카드 모양으로 묶어서 보여줌
                label = "월 예산", // label 값을 정해줌
                value = formatWon(plan.monthlyBudget), // 입력값을 정해줌
                containerColor = budgetSoftInnerCardColor(), // containerColor 값을 정해줌
                valueColor = MaterialTheme.colorScheme.onSurface // valueColor 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            InfoValueCard( // 내용을 카드 모양으로 묶어서 보여줌
                label = "목표 저축", // label 값을 정해줌
                value = formatWon(plan.savingGoal), // 입력값을 정해줌
                containerColor = budgetSoftInnerCardColor(), // containerColor 값을 정해줌
                valueColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0E9F4B) // valueColor 값을 정해줌
            )

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            BudgetLineItem(Icons.Default.Restaurant, "식비", plan.food) // Budget Line Item 함수를 실행함
            BudgetLineItem(Icons.Default.Subway, "교통비", plan.transport) // Budget Line Item 함수를 실행함
            BudgetLineItem(Icons.Default.Home, "생활비", plan.living) // Budget Line Item 함수를 실행함
            BudgetLineItem(Icons.Default.FavoriteBorder, "여가/취미", plan.hobby) // Budget Line Item 함수를 실행함
            BudgetLineItem(Icons.Default.Savings, "저축", plan.saving) // Budget Line Item 함수를 실행함

            Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

            Button( // 누를 수 있는 버튼을 만듦
                onClick = onApplyClick, // onApplyClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                enabled = enabled, // enabled 값을 정해줌
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                    containerColor = buttonColor, // containerColor 값을 정해줌
                    contentColor = buttonContentColor, // contentColor 값을 정해줌
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, // disabledContainerColor 값을 정해줌
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant // disabledContentColor 값을 정해줌
                ),
                contentPadding = PaddingValues(vertical = 12.dp), // contentPadding 값을 정해줌
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) // elevation 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "이 플랜 적용하기", // text 값을 정해줌
                    style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
                    fontWeight = FontWeight.Medium // fontWeight 값을 정해줌
                )
            }
        }
    }
}

// 라벨 + 값 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun InfoValueCard( // InfoValueCard 함수를 선언함
    label: String, // label 값을 받음
    value: String, // 입력값을 받음
    containerColor: Color, // containerColor 값을 받음
    valueColor: Color // valueColor 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .background(
                color = containerColor, // containerColor 값을 color 값에 넣음
                shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
            )
            .padding(horizontal = 14.dp, vertical = 14.dp), // .padding(horizontal 값을 정해줌
        horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = label, // label 값을 text 값에 넣음
            style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = value, // 입력값을 text 값에 넣음
            style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
            color = valueColor // valueColor 값을 color 값에 넣음
        )
    }
}

// 예산 항목 한 줄 UI
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetLineItem( // BudgetLineItem 함수를 선언함
    icon: ImageVector, // icon 값을 받음
    label: String, // label 값을 받음
    amount: Long // 금액을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .padding(vertical = 5.dp), // .padding(vertical 값을 정해줌
        horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row(verticalAlignment = Alignment.CenterVertically) { // 안쪽 UI를 가로로 배치함
            Icon( // 화면에 아이콘을 보여줌
                imageVector = icon, // icon 값을 imageVector 값에 넣음
                contentDescription = label, // label 값을 contentDescription 값에 넣음
                tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
            )

            Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = label, // label 값을 text 값에 넣음
                style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }

        Text( // 화면에 글자를 보여줌
            text = formatWon(amount), // text 값을 정해줌
            style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )
    }
}

// 직접 슬라이더로 값 조절하는 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CustomBudgetSettingCard( // CustomBudgetSettingCard 함수를 선언함
    monthlyIncome: Long, // 월 수입을 받음
    savingGoal: Long, // 저축 목표를 받음
    foodBudget: Long, // 식비 예산을 받음
    transportBudget: Long, // 교통비 예산을 받음
    livingBudget: Long, // 생활비 예산을 받음
    hobbyBudget: Long, // 취미 예산을 받음
    onMonthlyIncomeChange: (Long) -> Unit, // onMonthlyIncomeChange 때 실행할 함수를 받음
    onSavingGoalChange: (Long) -> Unit, // onSavingGoalChange 때 실행할 함수를 받음
    onFoodBudgetChange: (Long) -> Unit, // 예산 관련 값을 받음
    onTransportBudgetChange: (Long) -> Unit, // 예산 관련 값을 받음
    onLivingBudgetChange: (Long) -> Unit, // 예산 관련 값을 받음
    onHobbyBudgetChange: (Long) -> Unit, // 예산 관련 값을 받음
    isSaveEnabled: Boolean = true, // 저장 버튼 활성화 여부를 받음
    onSaveClick: () -> Unit // onSaveClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함
    val monthlySliderMax = dynamicBudgetMax(5000000L, monthlyIncome) // monthlySliderMax 값을 저장함
    val savingSliderMax = dynamicBudgetMax(500000L, monthlyIncome, savingGoal) // savingSliderMax 값을 저장함
    val categorySliderMax = dynamicBudgetMax( // categorySliderMax 값을 저장함
        10000000L,
        monthlyIncome,
        foodBudget,
        transportBudget,
        livingBudget,
        hobbyBudget
    )

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = if (isDark) budgetSoftCardColor() else Color(0xFFF7F8FA) // containerColor 값을 정해줌
        ),
        border = if (isDark) BorderStroke(1.dp, budgetSoftCardBorderColor()) else null
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp) // .padding(horizontal 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            // 월 예산 슬라이더
            BudgetSliderItem( // Budget Slider Item 함수를 실행함
                title = "월 예산", // 제목을 정해줌
                value = monthlyIncome, // 월 수입을 입력값에 넣음
                minValue = 0L, // minValue 값을 정해줌
                maxValue = monthlySliderMax, // monthlySliderMax 값을 maxValue 값에 넣음
                steps = 0, // steps 값을 정해줌
                icon = null, // null 값을 icon 값에 넣음
                valueColor = MaterialTheme.colorScheme.onSurface, // valueColor 값을 정해줌
                onValueChange = onMonthlyIncomeChange // onMonthlyIncomeChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
            )

            Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

            // 저축 목표 슬라이더
            BudgetSliderItem( // Budget Slider Item 함수를 실행함
                title = "저축 목표", // 제목을 정해줌
                value = savingGoal, // 저축 목표를 입력값에 넣음
                minValue = 0L, // minValue 값을 정해줌
                maxValue = savingSliderMax, // savingSliderMax 값을 maxValue 값에 넣음
                steps = 0, // steps 값을 정해줌
                icon = null, // null 값을 icon 값에 넣음
                valueColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF16A34A), // valueColor 값을 정해줌
                onValueChange = onSavingGoalChange // onSavingGoalChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            // 상단 공통 항목과 카테고리 항목을 시각적으로 분리
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // HorizontalDivider(color 값을 정해줌

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            // 식비 슬라이더
            BudgetSliderItem( // Budget Slider Item 함수를 실행함
                title = "식비", // 제목을 정해줌
                value = foodBudget, // 식비 예산을 입력값에 넣음
                minValue = 0L, // minValue 값을 정해줌
                maxValue = categorySliderMax, // categorySliderMax 값을 maxValue 값에 넣음
                steps = 0, // steps 값을 정해줌
                icon = Icons.Default.Restaurant, // icon 값을 정해줌
                valueColor = MaterialTheme.colorScheme.onSurface, // valueColor 값을 정해줌
                onValueChange = onFoodBudgetChange // 예산 관련 값을 onValueChange 때 실행할 함수에 넣음
            )

            Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

            // 교통비 슬라이더
            BudgetSliderItem( // Budget Slider Item 함수를 실행함
                title = "교통비", // 제목을 정해줌
                value = transportBudget, // 교통비 예산을 입력값에 넣음
                minValue = 0L, // minValue 값을 정해줌
                maxValue = categorySliderMax, // categorySliderMax 값을 maxValue 값에 넣음
                steps = 0, // steps 값을 정해줌
                icon = Icons.Default.Subway, // icon 값을 정해줌
                valueColor = MaterialTheme.colorScheme.onSurface, // valueColor 값을 정해줌
                onValueChange = onTransportBudgetChange // 예산 관련 값을 onValueChange 때 실행할 함수에 넣음
            )

            Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

            // 생활비 슬라이더
            BudgetSliderItem( // Budget Slider Item 함수를 실행함
                title = "생활비", // 제목을 정해줌
                value = livingBudget, // 생활비 예산을 입력값에 넣음
                minValue = 0L, // minValue 값을 정해줌
                maxValue = categorySliderMax, // categorySliderMax 값을 maxValue 값에 넣음
                steps = 0, // steps 값을 정해줌
                icon = Icons.Default.Home, // icon 값을 정해줌
                valueColor = MaterialTheme.colorScheme.onSurface, // valueColor 값을 정해줌
                onValueChange = onLivingBudgetChange // 예산 관련 값을 onValueChange 때 실행할 함수에 넣음
            )

            Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

            // 여가/취미 슬라이더
            BudgetSliderItem( // Budget Slider Item 함수를 실행함
                title = "여가/취미", // 제목을 정해줌
                value = hobbyBudget, // 취미 예산을 입력값에 넣음
                minValue = 0L, // minValue 값을 정해줌
                maxValue = categorySliderMax, // categorySliderMax 값을 maxValue 값에 넣음
                steps = 0, // steps 값을 정해줌
                icon = Icons.Default.FavoriteBorder, // icon 값을 정해줌
                valueColor = MaterialTheme.colorScheme.onSurface, // valueColor 값을 정해줌
                onValueChange = onHobbyBudgetChange // 예산 관련 값을 onValueChange 때 실행할 함수에 넣음
            )

            Spacer(modifier = Modifier.height(26.dp)) // UI 크기나 여백 같은 모양을 정함

            // 설정 저장 버튼
            Button( // 누를 수 있는 버튼을 만듦
                onClick = onSaveClick, // onSaveClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                enabled = isSaveEnabled, // enabled 값을 정해줌
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                    containerColor = budgetPrimaryButtonColor(), // containerColor 값을 정해줌
                    contentColor = budgetPrimaryButtonContentColor(), // contentColor 값을 정해줌
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, // disabledContainerColor 값을 정해줌
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant // disabledContentColor 값을 정해줌
                ),
                contentPadding = PaddingValues(vertical = 12.dp) // contentPadding 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "설정 저장", // text 값을 정해줌
                    style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = if (isSaveEnabled) budgetPrimaryButtonContentColor() else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 슬라이더 한 줄 UI
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetSliderItem( // BudgetSliderItem 함수를 선언함
    title: String, // 제목을 받음
    value: Long, // 입력값을 받음
    minValue: Long, // minValue 값을 받음
    maxValue: Long, // maxValue 값을 받음
    steps: Int, // steps 값을 받음
    icon: ImageVector? = null, // icon 값을 받음
    valueColor: Color = Color.Unspecified, // valueColor 값을 받음
    onValueChange: (Long) -> Unit // onValueChange 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함
    val safeMinValue = minValue.coerceAtLeast(0L) // safeMinValue 값을 저장함
    val safeMaxValue = maxValue.coerceAtLeast(safeMinValue + 1L) // safeMaxValue 값을 저장함
    val sliderRange = safeMinValue.toFloat()..safeMaxValue.toFloat() // sliderRange 값을 저장함
    val normalizedValue = value.coerceIn(safeMinValue, safeMaxValue) // normalizedValue 값을 저장함
    // 슬라이더 현재 위치 상태
    var sliderPosition by remember(normalizedValue, safeMaxValue) { // 화면이 다시 그려져도 sliderPosition 값을 기억함
        mutableFloatStateOf(normalizedValue.toFloat()) // mutable Float State Of 함수를 실행함
    }
    var inputText by remember { mutableStateOf(formatWonWithoutSuffix(value)) } // 화면에서 바뀔 inputText 값을 저장함

    LaunchedEffect(value) { // 화면이 열리거나 값이 바뀔 때 실행함
        sliderPosition = normalizedValue.toFloat() // sliderPosition 값을 정해줌
        val formattedValue = formatWonWithoutSuffix(value) // formattedValue 값을 저장함
        if (inputText != formattedValue) { // 조건이 맞는지 확인함
            inputText = formattedValue // formattedValue 값을 inputText 값에 넣음
        }
    }

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
    ) { // 이 블록 안의 내용이 시작됨
        // 제목과 숫자 입력 필드를 함께 배치
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                if (icon != null) { // 조건이 맞는지 확인함
                    Icon( // 화면에 아이콘을 보여줌
                        imageVector = icon, // icon 값을 imageVector 값에 넣음
                        contentDescription = title, // 제목을 contentDescription 값에 넣음
                        tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                    )

                    Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함
                }

                Text( // 화면에 글자를 보여줌
                    text = title, // 제목을 text 값에 넣음
                    style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }

            Spacer(modifier = Modifier.width(12.dp)) // UI 크기나 여백 같은 모양을 정함

            OutlinedTextField( // 사용자가 입력할 칸을 만듦
                value = inputText, // inputText 값을 입력값에 넣음
                onValueChange = { changedText -> // onValueChange 때 실행할 함수를 정해줌
                    val onlyDigits = changedText.filter { it.isDigit() }.trimStart('0').take(15) // 숫자만 남긴 값을 저장함
                    val changedValue = onlyDigits.toLongOrNull()?.coerceAtMost(MAX_BUDGET_AMOUNT) ?: 0L // changedValue 값을 저장함

                    inputText = if (changedText.isBlank() || onlyDigits.isBlank()) { // inputText 값을 정해줌
                        ""
                    } else { // 이 블록 안의 내용이 시작됨
                        formatWonWithoutSuffix(changedValue) // format Won Without Suffix 함수를 실행함
                    }

                    sliderPosition = changedValue.coerceIn(safeMinValue, safeMaxValue).toFloat() // sliderPosition 값을 정해줌
                    onValueChange(changedValue) // on Value Change 함수를 실행함
                },
                modifier = Modifier.width(152.dp), // UI 크기나 여백 같은 모양을 정함
                singleLine = true, // true 값을 singleLine 값에 넣음
                suffix = { // suffix 값을 정해줌
                    Text( // 화면에 글자를 보여줌
                        text = "원", // text 값을 정해줌
                        style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // keyboardOptions 값을 정해줌
                textStyle = MaterialTheme.typography.titleMedium.copy( // textStyle 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = valueColor // valueColor 값을 color 값에 넣음
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

        // 일반형 슬라이더
        // 사용자가 원하는 첫 번째 스크린샷 스타일에 맞춰
        // 점 없는 기본 슬라이더 모양으로 사용
        Slider( // Slider 함수를 실행함
            value = sliderPosition, // sliderPosition 값을 입력값에 넣음
            onValueChange = { changedValue -> // onValueChange 때 실행할 함수를 정해줌
                // 슬라이더를 움직이는 동안 현재 위치 갱신
                val clampedValue = changedValue.toLong().coerceIn(safeMinValue, safeMaxValue) // clampedValue 값을 저장함
                sliderPosition = clampedValue.toFloat() // sliderPosition 값을 정해줌
                inputText = formatWonWithoutSuffix(clampedValue) // inputText 값을 정해줌

                onValueChange(clampedValue) // on Value Change 함수를 실행함
            },
            valueRange = sliderRange, // sliderRange 값을 valueRange 값에 넣음

            // 첫 번째 스타일처럼 연속형 슬라이더로 사용
            steps = steps, // steps 값을 steps 값에 넣음

            colors = SliderDefaults.colors( // colors 값을 정해줌
                // 손잡이 색상
                thumbColor = Color.White, // thumbColor 값을 정해줌

                // 채워진 구간 색상
                activeTrackColor = if (isDark) SpentopiaGlowPurple else MaterialTheme.colorScheme.primary, // activeTrackColor 값을 정해줌

                // 비어있는 구간 색상
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant, // inactiveTrackColor 값을 정해줌

                // 점 표시를 눈에 띄지 않게 트랙 색과 동일하게 설정
                activeTickColor = if (isDark) SpentopiaGlowPurple else MaterialTheme.colorScheme.primary, // activeTickColor 값을 정해줌
                inactiveTickColor = MaterialTheme.colorScheme.outlineVariant // inactiveTickColor 값을 정해줌
            )
        )

        Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

        // 최소값 / 최대값 표시
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            horizontalArrangement = Arrangement.SpaceBetween // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = formatWonWithoutSuffix(safeMinValue), // text 값을 정해줌
                style = MaterialTheme.typography.bodySmall, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = formatWonWithoutSuffix(safeMaxValue), // text 값을 정해줌
                style = MaterialTheme.typography.bodySmall, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

// 예산 요약 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetSummaryCard( // BudgetSummaryCard 함수를 선언함
    monthlyIncome: Long, // 월 수입을 받음
    totalExpense: Long, // 소비 내역 값을 받음
    savingGoal: Long, // 저축 목표를 받음
    remainingAmount: Long // remainingAmount 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val categoryRatio = if (monthlyIncome <= 0) { // categoryRatio 값을 저장함
        0f
    } else { // 이 블록 안의 내용이 시작됨
        (totalExpense.toDouble() / monthlyIncome.toDouble()).toFloat().coerceIn(0f, 1f)
    }
    // 월 수입 대비 카테고리 합계가 몇 퍼센트인지 계산합니다.
    val categoryPercent = if (monthlyIncome <= 0) 0L else (totalExpense * 100.0 / monthlyIncome).toLong() // categoryPercent 값을 저장함
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함
    val waveShift by rememberInfiniteTransition().animateFloat( // 예산 게이지 안쪽 빛이 흐르도록 저장함
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val progressBrush = Brush.linearGradient( // 이번주 성실도 바와 맞춘 게임 느낌의 진행 색상임
        colors = if (categoryPercent <= 100) {
            if (isDark) {
                listOf(
                    SpentopiaGlowPurple,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    MaterialTheme.colorScheme.primaryContainer,
                    SpentopiaGlowPurple
                )
            } else {
                listOf(
                    Color(0xFF93C5FD),
                    MaterialTheme.colorScheme.primary,
                    Color(0xFF38BDF8),
                    MaterialTheme.colorScheme.primary
                )
            }
        } else {
            listOf(
                MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.error,
                Color(0xFFF97316),
                MaterialTheme.colorScheme.error
            )
        },
        start = Offset(-220f + waveShift * 260f, 0f),
        end = Offset(260f + waveShift * 260f, 0f)
    )
    val statusIcon = if (remainingAmount >= 0) { // statusIcon 값을 저장함
        Icons.Default.SentimentSatisfied
    } else { // 이 블록 안의 내용이 시작됨
        Icons.Default.Warning
    }
    val statusIconTint = if (remainingAmount >= 0) { // statusIconTint 값을 저장함
        MaterialTheme.colorScheme.primary
    } else { // 이 블록 안의 내용이 시작됨
        MaterialTheme.colorScheme.error
    }
    val summaryTitleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val summaryLabelColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val summaryValueColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val summaryPercentColor = if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1)
    val summaryMessageBackground = if (isDark) Color(0xFF211B35) else Color(0xFFE0F2FE)
    val summaryMessageColor = if (isDark) Color(0xFFE5E7EB) else Color(0xFF334155)

    // 남는 금액이 0 이상이면 긍정 메시지, 아니면 초과 메시지 출력
    val message = if (remainingAmount >= 0) { // 메시지를 저장함
        "균형잡힌 예산이에요! 남은 금액: ${formatWon(remainingAmount)}"
    } else { // 이 블록 안의 내용이 시작됨
        "현재 예산이 ${formatWon(-remainingAmount)} 초과 상태예요"
    }

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp, // elevation 값을 정해줌
                shape = RoundedCornerShape(22.dp), // shape 값을 정해줌
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), // ambientColor 값을 정해줌
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) // spotColor 값을 정해줌
        ),
        shape = RoundedCornerShape(22.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(
            containerColor = budgetSoftCardColor()
        ), // colors 값을 정해줌
        border = BorderStroke(1.dp, budgetSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .border(
                    width = 1.dp, // width 값을 정해줌
                    color = budgetSoftCardBorderColor(), // color 값을 정해줌
                    shape = RoundedCornerShape(22.dp) // shape 값을 정해줌
                )
                .padding(18.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Column(modifier = Modifier.fillMaxWidth()) { // 안쪽 UI를 세로로 배치함
                Text( // 화면에 글자를 보여줌
                    text = "예산 요약", // text 값을 정해줌
                    style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = summaryTitleColor // color 값을 정해줌
                )

                Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

                SummaryRow("저장된 월 예산", formatWon(monthlyIncome), summaryLabelColor, summaryValueColor) // 안쪽 UI를 가로로 배치함
                SummaryRow("카테고리 합계", formatWon(totalExpense), summaryLabelColor, summaryValueColor) // 안쪽 UI를 가로로 배치함
                SummaryRow("목표 저축액 포함", formatWon(totalExpense + savingGoal), summaryLabelColor, summaryValueColor) // 안쪽 UI를 가로로 배치함

                Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // HorizontalDivider(color 값을 정해줌

                Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth()
                        .background(
                            color = summaryMessageBackground, // color 값을 정해줌
                            shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                        )
                        .padding(14.dp)
                ) { // 이 블록 안의 내용이 시작됨
                    Row(verticalAlignment = Alignment.CenterVertically) { // 안쪽 UI를 가로로 배치함
                        Icon( // 화면에 아이콘을 보여줌
                            imageVector = statusIcon, // statusIcon 값을 imageVector 값에 넣음
                            contentDescription = "요약 상태", // contentDescription 값을 정해줌
                            tint = statusIconTint // statusIconTint 값을 tint 값에 넣음
                        )

                        Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

                        Text( // 화면에 글자를 보여줌
                            text = message, // 메시지를 text 값에 넣음
                            style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                            color = summaryMessageColor // color 값을 정해줌
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = "${categoryPercent}%", // text 값을 정해줌
                    style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = summaryPercentColor // color 값을 정해줌
                )

                Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.62f) else Color(0xFFDFF1FF)) // .background(MaterialTheme.colorScheme.surface.copy(alpha 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .fillMaxWidth(categoryRatio)
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(progressBrush)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = "카테고리 예산은 월 전체 예산 안에서만 배분되며, 목표 저축액은 별도로 관리됩니다.", // text 값을 정해줌
                    style = MaterialTheme.typography.bodySmall, // style 값을 정해줌
                    color = summaryLabelColor // color 값을 정해줌
                )
            }
        }
    }
}

// 예산 요약 행
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SummaryRow( // SummaryRow 함수를 선언함
    label: String, // label 값을 받음
    value: String, // 입력값을 받음
    labelColor: Color = Color.White, // labelColor 값을 받음
    valueColor: Color = Color.White // valueColor 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.SpaceBetween // horizontalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = label, // label 값을 text 값에 넣음
            style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
            color = labelColor // labelColor 값을 color 값에 넣음
        )

        Text( // 화면에 글자를 보여줌
            text = value, // 입력값을 text 값에 넣음
            style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
            color = valueColor // valueColor 값을 color 값에 넣음
        )
    }

    Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CurrentMonthlyBudgetCard( // CurrentMonthlyBudgetCard 함수를 선언함
    year: Int, // year 값을 받음
    month: Int, // month 값을 받음
    monthlyBudget: Long // 예산 관련 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val dateColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = if (isDark) budgetSoftCardColor() else Color(0xFFF7F8FA) // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, budgetSoftCardBorderColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "현재 설정된 월 예산", // text 값을 정해줌
                style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = titleColor // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "${year}년 ${month}월 기준", // text 값을 정해줌
                style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                color = dateColor // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = formatWon(monthlyBudget), // text 값을 정해줌
                style = MaterialTheme.typography.headlineLarge, // style 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetCommentCard( // BudgetCommentCard 함수를 선언함
    monthlyIncome: Long, // 월 수입을 받음
    totalExpense: Long, // 소비 내역 값을 받음
    savingGoal: Long, // 저축 목표를 받음
    remainingAmount: Long // remainingAmount 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함
    val plannedAmount = totalExpense + savingGoal // plannedAmount 값을 저장함
    // 실제로 월 예산에서 얼마나 써버렸는지 보는 지표입니다.
    val usedPercent = if (monthlyIncome <= 0) { // usedPercent 값을 저장함
        0L
    } else { // 이 블록 안의 내용이 시작됨
        (plannedAmount * 100.0 / monthlyIncome).toLong().coerceAtLeast(0L)
    }
    val comment = when { // comment 값을 저장함
        monthlyIncome <= 0 -> "월 예산을 입력하면 카테고리별 계획을 더 정확하게 맞출 수 있어요." // < 값을 정해줌
        totalExpense == 0L -> "카테고리 예산을 입력하면 이번 달 소비 계획을 한눈에 볼 수 있어요." // 소비 내역 값을 정해줌
        remainingAmount < 0 -> "월 예산보다 ${formatWon(-remainingAmount)} 초과됐어요. 카테고리 예산이나 저축 목표를 조금 낮추면 균형이 맞아요."
        savingGoal <= 0 -> "소비 계획은 예산 안에 있어요. 남은 ${formatWon(remainingAmount)} 중 일부를 저축 목표로 잡아두면 더 안정적이에요." // < 값을 정해줌
        usedPercent >= 95 -> "예산 안에 들어오긴 했지만 남은 금액이 ${formatWon(remainingAmount)}라 여유가 적어요. 변동 지출을 조금만 줄여보세요." // > 값을 정해줌
        usedPercent >= 80 -> "현재 계획은 월 예산의 ${usedPercent}%를 사용해요. 남은 ${formatWon(remainingAmount)}로 비상 지출까지 관리할 수 있어요." // > 값을 정해줌
        else -> "현재 계획은 여유 있게 예산 안에 있어요. 남은 ${formatWon(remainingAmount)}는 추가 저축이나 다음 달 준비금으로 돌려도 좋아요." // 위 조건이 아니면 이쪽을 실행함
    }
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val commentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .border(
                width = 1.dp, // width 값을 정해줌
                color = budgetSoftCardBorderColor(), // color 값을 정해줌
                shape = RoundedCornerShape(20.dp) // shape 값을 정해줌
            ),
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = if (isDark) budgetSoftCardColor() else Color(0xFFF8FBFF) // containerColor 값을 정해줌
        )
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "예산 설정 한마디", // text 값을 정해줌
                style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = titleColor // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = comment, // comment 값을 text 값에 넣음
                style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
                color = commentColor // color 값을 정해줌
            )
        }
    }
}

// AI 분석 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun BudgetAnalysisCard( // BudgetAnalysisCard 함수를 선언함
    foodBudget: Long, // 식비 예산을 받음
    totalExpense: Long, // 소비 내역 값을 받음
    savingGoal: Long, // 저축 목표를 받음
    aiAnalysisText: String // aiAnalysisText 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함

    // 식비가 전체 지출에서 차지하는 비율 계산
    val foodRatio = if (totalExpense == 0L) 0L else (foodBudget * 100.0 / totalExpense).toLong() // foodRatio 값을 저장함

    // 첫 번째 메시지
    val firstMessage = when { // firstMessage 값을 저장함
        foodRatio <= 30 -> "식비 비중이 적정 수준이에요. 건강한 소비 습관이에요!" // < 값을 정해줌
        foodRatio <= 40 -> "식비 비중이 조금 높아요. 외식 횟수를 줄이면 더 좋아요." // < 값을 정해줌
        else -> "식비 비중이 높은 편이에요. 식비 관리가 핵심 포인트예요." // 위 조건이 아니면 이쪽을 실행함
    }

    // 두 번째 메시지
    val secondMessage = when { // secondMessage 값을 저장함
        savingGoal >= 100000 -> "저축 비율이 높아요! 목표 달성 가능성이 좋아요." // > 값을 정해줌
        savingGoal >= 50000 -> "저축 비율이 안정적이에요. 꾸준히 유지해보세요." // > 값을 정해줌
        else -> "저축 목표를 조금만 더 높이면 미래 준비에 도움이 돼요." // 위 조건이 아니면 이쪽을 실행함
    }

    // 세 번째 메시지
    val thirdMessage = when { // thirdMessage 값을 저장함
        savingGoal >= 150000 -> "이 속도면 장기 목표 달성에 훨씬 가까워질 수 있어요!" // > 값을 정해줌
        savingGoal >= 50000 -> "이 흐름이면 꾸준한 자산 형성이 가능해요!" // > 값을 정해줌
        else -> "소액이라도 꾸준히 쌓이면 분명 큰 차이를 만들 수 있어요!" // 위 조건이 아니면 이쪽을 실행함
    }

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = if (isDark) budgetSoftCardColor() else Color(0xFFF8FBFF) // containerColor 값을 정해줌
        ),
        border = if (isDark) BorderStroke(1.dp, budgetSoftCardBorderColor()) else null
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "AI 분석", // text 값을 정해줌
                style = MaterialTheme.typography.headlineSmall, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            if (aiAnalysisText.isNotBlank()) { // 조건이 맞는지 확인함
                AnalysisTextRow(Icons.Default.AutoAwesome, aiAnalysisText) // 안쪽 UI를 가로로 배치함
                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
            }

            AnalysisTextRow(Icons.Default.TrendingUp, firstMessage) // 안쪽 UI를 가로로 배치함
            Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
            AnalysisTextRow(Icons.Default.VolunteerActivism, secondMessage) // 안쪽 UI를 가로로 배치함
            Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
            AnalysisTextRow(Icons.Default.Savings, thirdMessage) // 안쪽 UI를 가로로 배치함
        }
    }
}

// AI 분석 한 줄
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AnalysisTextRow( // AnalysisTextRow 함수를 선언함
    icon: ImageVector, // icon 값을 받음
    text: String // text 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        verticalAlignment = Alignment.Top // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Icon( // 화면에 아이콘을 보여줌
            imageVector = icon, // icon 값을 imageVector 값에 넣음
            contentDescription = text, // text 값을 contentDescription 값에 넣음
            tint = MaterialTheme.colorScheme.primary // tint 값을 정해줌
        )

        Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )
    }
}

// 절약 팁 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SavingTipCard( // SavingTipCard 함수를 선언함
    foodBudget: Long, // 식비 예산을 받음
    transportBudget: Long // 교통비 예산을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isBudgetDarkTheme() // 다크모드인지 저장함

    // 식비 관련 팁
    val foodTip = if (foodBudget >= 150000) { // foodTip 값을 저장함
        "식비는 외식을 줄이면 월 5만원 이상 절약 가능해요"
    } else { // 이 블록 안의 내용이 시작됨
        "현재 식비는 비교적 안정적이에요. 유지해도 좋아요"
    }

    // 교통비 관련 팁
    val transportTip = if (transportBudget >= 80000) { // transportTip 값을 저장함
        "대중교통 정기권으로 교통비 20% 절감할 수 있어요"
    } else { // 이 블록 안의 내용이 시작됨
        "현재 교통비는 무난한 편이에요. 고정비만 잘 관리해도 충분해요"
    }

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .border(
                width = 1.dp, // width 값을 정해줌
                color = if (isDark) budgetSoftCardBorderColor() else Color(0xFFE6DDC8), // color 값을 정해줌
                shape = RoundedCornerShape(20.dp) // shape 값을 정해줌
            ),
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = if (isDark) budgetSoftCardColor() else Color(0xFFF8F1DD) // containerColor 값을 정해줌
        )
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Default.Lightbulb, // imageVector 값을 정해줌
                    contentDescription = "절약 팁", // contentDescription 값을 정해줌
                    tint = MaterialTheme.colorScheme.primary // tint 값을 정해줌
                )

                Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = "절약 팁", // text 값을 정해줌
                    style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }

            Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함

            TipBullet(text = foodTip) // TipBullet(text 값을 정해줌
            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
            TipBullet(text = transportTip) // TipBullet(text 값을 정해줌
            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
            TipBullet(text = "구독 서비스를 정리하면 월 3만원 안팎 절약할 수 있어요") // TipBullet(text 값을 정해줌
        }
    }
}

// 절약 팁 점 목록 한 줄
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun TipBullet( // TipBullet 함수를 선언함
    text: String // text 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        verticalAlignment = Alignment.Top // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = "• ", // text 값을 정해줌
            style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
            color = MaterialTheme.colorScheme.primary // color 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )
    }
}

// AI 추천 플랜 하나를 담는 데이터 클래스
data class BudgetPlanUiData( // BudgetPlanUiData 데이터를 묶어둘 클래스 시작
    val title: String, // 제목을 저장함
    val description: String, // description 값을 저장함
    val monthlyBudget: Long, // 예산 관련 값을 저장함
    val savingGoal: Long, // 저축 목표을 저장함
    val food: Long, // food 값을 저장함
    val transport: Long, // transport 값을 저장함
    val living: Long, // living 값을 저장함
    val hobby: Long, // hobby 값을 저장함
    val saving: Long // saving 값을 저장함
)

// 숫자를 "1,000" 형태로 바꿔주는 함수
private fun formatWonWithoutSuffix(amount: Long): String { // formatWonWithoutSuffix 함수를 선언함
    return "%,d".format(amount) // 이 값을 함수 결과로 돌려줌
}

// 숫자를 "1,000원" 형태로 바꿔주는 함수
private fun formatWon(amount: Long): String { // formatWon 함수를 선언함
    return "%,d원".format(amount) // 이 값을 함수 결과로 돌려줌
}

private fun dynamicBudgetMax(defaultMax: Long, vararg values: Long): Long { // dynamicBudgetMax 함수를 선언함
    return values // 이 값을 함수 결과로 돌려줌
        .fold(defaultMax.coerceAtLeast(1L)) { currentMax, value ->
            maxOf(currentMax, value.coerceAtLeast(0L)) // max Of 함수를 실행함
        }
        .coerceAtMost(MAX_BUDGET_AMOUNT)
}
