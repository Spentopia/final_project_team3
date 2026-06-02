package com.ict.spentopia.feature.analysis // 이 파일이 속한 패키지 위치를 적음

import android.content.ContentValues // ContentValues 기능을 가져옴
import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.content.Intent // Intent 기능을 가져옴
import android.net.Uri // 딥링크 주소 타입을 가져옴
import android.os.Environment // Environment 기능을 가져옴
import android.provider.MediaStore // MediaStore 기능을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import androidx.compose.animation.core.LinearEasing // 일정한 속도 애니메이션을 가져옴
import androidx.compose.animation.core.RepeatMode // 반복 방향 설정을 가져옴
import androidx.compose.animation.core.animateFloat // Float 애니메이션을 가져옴
import androidx.compose.animation.core.infiniteRepeatable // 무한 반복 애니메이션을 가져옴
import androidx.compose.animation.core.rememberInfiniteTransition // 반복 애니메이션 상태를 기억함
import androidx.compose.animation.core.tween // 시간 기반 애니메이션을 가져옴
import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.Canvas // Canvas 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.PaddingValues // PaddingValues 기능을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.layout.width // width 기능을 가져옴
import androidx.compose.foundation.layout.widthIn // widthIn 기능을 가져옴
import androidx.compose.foundation.rememberScrollState // rememberScrollState 기능을 가져옴
import androidx.compose.foundation.shape.CircleShape // CircleShape 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.verticalScroll // verticalScroll 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.CircularProgressIndicator // 원형 로딩 표시를 가져옴
import androidx.compose.material3.HorizontalDivider // HorizontalDivider 기능을 가져옴
import androidx.compose.material3.LinearProgressIndicator // LinearProgressIndicator 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.collectAsState // collectAsState 기능을 가져옴
import androidx.compose.runtime.mutableIntStateOf // mutableIntStateOf 기능을 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.foundation.interaction.MutableInteractionSource // MutableInteractionSource 기능을 가져옴
import androidx.compose.foundation.interaction.collectIsPressedAsState // collectIsPressedAsState 기능을 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.clip // clip 기능을 가져옴
import androidx.compose.ui.draw.shadow // shadow 기능을 가져옴
import androidx.compose.ui.geometry.Offset // Offset 기능을 가져옴
import androidx.compose.ui.graphics.Brush // Brush 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.graphics.Path // 주소 중간에 들어갈 값 표시를 가져옴
import androidx.compose.ui.graphics.PathEffect // PathEffect 기능을 가져옴
import androidx.compose.ui.graphics.StrokeCap // StrokeCap 기능을 가져옴
import androidx.compose.ui.graphics.drawscope.Stroke // Stroke 기능을 가져옴
import androidx.compose.ui.graphics.graphicsLayer // graphicsLayer 기능을 가져옴
import androidx.compose.ui.input.pointer.pointerInput // pointerInput 기능을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.text.style.TextAlign // TextAlign 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.compose.ui.window.Dialog // 팝업 다이얼로그를 가져옴
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel 연결하는 도구를 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // Solana 지갑 타입을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground // 앱 다크모드 배경색을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple // SpentopiaGlowPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple // SpentopiaNavyPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors // SpentopiaWalletGradientColors 기능을 가져옴
import com.ict.spentopia.ui.toast.AppToastType
import com.ict.spentopia.ui.toast.showAppToast
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender // 지갑 앱 호출 도구를 가져옴
import kotlin.math.max // max 기능을 가져옴
import kotlin.math.roundToInt // roundToInt 기능을 가져옴

@Composable
private fun isAnalysisDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background == SpentopiaDarkBackground
}

@Composable
private fun analysisSoftCardColor(): Color {
    return if (isAnalysisDarkTheme()) Color(0xFF171A2B) else Color(0xFFF7FBFF)
}

@Composable
private fun analysisSoftCardBorderColor(): Color {
    return if (isAnalysisDarkTheme()) Color(0xFF4C3B7A) else Color(0xFF7DD3FC)
}

// 소비분석 메인 화면임
// 요약/비중/AI리포트/공유/다운로드 한 화면
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AnalysisScreen( // AnalysisScreen 함수를 선언함
    viewModel: AnalysisViewModel = viewModel(), // 화면 데이터 관리자를 받음
    isWalletConnected: Boolean = false, // 지갑 연결 여부를 받음
    walletAddress: String = "", // 지갑 주소를 받음
    walletProvider: String = "", // 지갑 종류를 받음
    walletActivityResultSender: ActivityResultSender? = null, // 지갑 앱 호출 도구를 받음
    walletCallbackUri: Uri? = null, // 지갑 결제 콜백 주소를 받음
    onWalletCallbackConsumed: () -> Unit = {}, // 지갑 콜백 처리 완료 함수를 받음
    onWalletConnectClick: (SolanaWalletType) -> Unit = {} // 지갑 연결 요청을 받음
) { // 이 블록 안의 내용이 시작됨
    val uiState by viewModel.uiState.collectAsState() // 화면 상태를 저장함
    val trendExpenseList = viewModel.getCurrentTrendList() // 소비 내역 값을 저장함
    val context = LocalContext.current // 현재 화면 정보를 저장함
    val requiredPaymentAmountText = remember(uiState.paymentRequiredBody) {
        formatRequiredPaymentAmount(uiState.paymentRequiredBody)
    }

    LaunchedEffect(walletCallbackUri, walletProvider) {
        walletCallbackUri?.let { uri ->
            Log.d("SpentopiaPayment", "analysis wallet callback received provider=$walletProvider uri=$uri")
            val handled = viewModel.handleWalletPaymentCallback(
                context = context,
                walletProvider = walletProvider,
                callbackUri = uri
            )
            Log.d("SpentopiaPayment", "analysis wallet callback handled=$handled")
            if (handled) {
                onWalletCallbackConsumed()
            }
        }
    }

    val reportText = buildAnalysisReportText( // reportText 값을 저장함
        totalExpense = uiState.totalExpense, // 소비 내역 값을 정해줌
        averageDailyExpense = uiState.averageDailyExpense, // 소비 내역 값을 정해줌
        budgetUsageRate = uiState.budgetUsageRate, // 예산 관련 값을 정해줌
        topCategoryName = uiState.topCategoryName, // topCategoryName 값을 정해줌
        topCategoryRatio = uiState.topCategoryRatio, // topCategoryRatio 값을 정해줌
        selectedPeriod = uiState.selectedPeriod, // selectedPeriod 값을 정해줌
        trendExpenseList = trendExpenseList, // 소비 내역 값을 소비 내역 값에 넣음
        categoryList = uiState.categoryList, // categoryList 값을 정해줌
        tipList = uiState.tipList, // tipList 값을 정해줌
        aiAnalysisText = uiState.aiAnalysisText, // aiAnalysisText 값을 정해줌
        timePatternList = uiState.timePatternList, // timePatternList 값을 정해줌
        weekdayAverageText = uiState.weekdayAverageText, // weekdayAverageText 값을 정해줌
        weekendAverageText = uiState.weekendAverageText, // weekendAverageText 값을 정해줌
        weekendComment = uiState.weekendComment, // weekendComment 값을 정해줌
        paymentPatternList = uiState.paymentPatternList // paymentPatternList 값을 정해줌
    )

    LaunchedEffect(uiState.paymentToastMessage) {
        if (uiState.paymentToastMessage.isNotBlank()) {
            val toastType = if (uiState.paymentToastMessage.contains("확인")) {
                AppToastType.SUCCESS
            } else {
                AppToastType.ERROR
            }
            showAppToast(context, uiState.paymentToastMessage, toastType)
            viewModel.consumePaymentToast()
        }
    }

    if (uiState.isPaymentRequired) {
        AiPaymentRequiredDialog(
            isWalletConnected = isWalletConnected,
            isPaymentLoading = uiState.isPaymentLoading,
            paymentAmountText = requiredPaymentAmountText,
            onDismiss = { viewModel.dismissPaymentDialog() },
            onConnectPhantomClick = { onWalletConnectClick(SolanaWalletType.PHANTOM) },
            onConnectSolflareClick = { onWalletConnectClick(SolanaWalletType.SOLFLARE) },
            onPaymentClick = {
                val sender = walletActivityResultSender
                if (sender != null) {
                    showAppToast(context, "결제 중...", AppToastType.WALLET)
                    val walletAuthToken = context
                        .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .getString("wallet_auth_token_${walletProvider}", "")
                        .orEmpty()
                    viewModel.payForAiAnalysis(
                        context = context,
                        walletActivityResultSender = sender,
                        walletProvider = walletProvider,
                        walletAddress = walletAddress,
                        walletAuthToken = walletAuthToken
                    )
                } else {
                    showAppToast(context, "지갑 결제 화면을 열 수 없습니다.", AppToastType.ERROR)
                }
            }
        )
    }

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp), // .padding(horizontal 값을 정해줌
        verticalArrangement = Arrangement.spacedBy(16.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        // 상단 제목 + 공유/다운로드
        AnalysisHeaderSection( // Analysis Header Section 함수를 실행함
            onShareClick = { // onShareClick 때 실행할 함수를 정해줌
                shareAnalysisReport(context, reportText) // share Analysis Report 함수를 실행함
            },
            onDownloadClick = { // onDownloadClick 때 실행할 함수를 정해줌
                val fileName = "spentopia_analysis_report_${System.currentTimeMillis()}.txt" // fileName 값을 저장함

                val isSaved = saveAnalysisReportToDownloads( // 저장이 성공했는지 저장함
                    context = context, // 현재 화면 정보를 현재 화면 정보에 넣음
                    fileName = fileName, // fileName 값을 fileName 값에 넣음
                    content = reportText // reportText 값을 내용에 넣음
                )

                showAppToast(
                    context = context,
                    message = if (isSaved) "리포트가 다운로드 폴더에 저장되었어요." else "리포트 저장에 실패했어요.",
                    type = if (isSaved) AppToastType.SUCCESS else AppToastType.ERROR
                )
            }
        )

        // 이번 달 핵심 수치 먼저 보여줌
        SummaryCardSection( // Summary Card Section 함수를 실행함
            totalExpense = uiState.totalExpense, // 소비 내역 값을 정해줌
            averageDailyExpense = uiState.averageDailyExpense, // 소비 내역 값을 정해줌
            budgetUsageRate = uiState.budgetUsageRate // 예산 관련 값을 정해줌
        )

        TopCategoryCard( // 내용을 카드 모양으로 묶어서 보여줌
            categoryName = uiState.topCategoryName, // categoryName 값을 정해줌
            ratio = uiState.topCategoryRatio // ratio 값을 정해줌
        )

        PeriodToggleSection( // Period Toggle Section 함수를 실행함
            selectedPeriod = uiState.selectedPeriod, // selectedPeriod 값을 정해줌
            onSelectPeriod = { selectedPeriod -> // onSelectPeriod 때 실행할 함수를 정해줌
                viewModel.selectPeriod(selectedPeriod)
            }
        )

        ExpenseTrendCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = if (uiState.selectedPeriod == "주간") "주간 소비 추이" else "월간 소비 추이", // 제목을 정해줌
            expenseList = trendExpenseList, // 소비 내역 값을 소비 내역 값에 넣음
            selectedPeriod = uiState.selectedPeriod // selectedPeriod 값을 정해줌
        )

        CategoryPieChartCard( // 내용을 카드 모양으로 묶어서 보여줌
            categoryList = uiState.categoryList // categoryList 값을 정해줌
        )

        CategoryDetailCard( // 내용을 카드 모양으로 묶어서 보여줌
            categoryList = uiState.categoryList // categoryList 값을 정해줌
        )

        // AI 분석 리포트 영역
        AiAnalysisReportSection( // Ai Analysis Report Section 함수를 실행함
            totalExpense = uiState.totalExpense, // 소비 내역 값을 정해줌
            aiReport = uiState.aiConsumptionReport, // aiReport 값을 정해줌
            isLoading = uiState.isAiAnalysisLoading, // 로딩 여부를 정해줌
            errorMessage = uiState.aiAnalysisError, // 오류 내용을 정해줌
            onRequestAiAnalysis = { // onRequestAiAnalysis 때 실행할 함수를 정해줌
                viewModel.requestAiAnalysisReport()
            }
        )

        ConsumptionPatternCard( // 내용을 카드 모양으로 묶어서 보여줌
            aiReport = uiState.aiConsumptionReport, // aiReport 값을 정해줌
            isLoading = uiState.isAiAnalysisLoading // 로딩 여부를 정해줌
        )

        Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함
    }
}

// 상단 제목 섹션
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AnalysisHeaderSection( // AnalysisHeaderSection 함수를 선언함
    onShareClick: () -> Unit, // onShareClick 때 실행할 함수를 받음
    onDownloadClick: () -> Unit // onDownloadClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = "소비 패턴 분석", // text 값을 정해줌
            fontSize = 28.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onBackground, // color 값을 정해줌
            lineHeight = 34.sp // lineHeight 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = "AI가 분석한 소비 습관을 확인해보세요.", // text 값을 정해줌
            fontSize = 15.sp, // fontSize 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
            lineHeight = 22.sp // lineHeight 값을 정해줌
        )

        Row( // 안쪽 UI를 가로로 배치함
            horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Button( // 누를 수 있는 버튼을 만듦
                onClick = onShareClick, // onShareClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer // contentColor 값을 정해줌
                ),
                shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), // contentPadding 값을 정해줌
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .height(40.dp)
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "공유", // text 값을 정해줌
                    fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                )
            }

            Button( // 누를 수 있는 버튼을 만듦
                onClick = onDownloadClick, // onDownloadClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer // contentColor 값을 정해줌
                ),
                shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), // contentPadding 값을 정해줌
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .height(40.dp)
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "리포트 다운로드", // text 값을 정해줌
                    fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                )
            }
        }
    }
}

private fun formatRequiredPaymentAmount(
    body: com.ict.spentopia.data.remote.Solana402Body?
): String {
    val rawAmount = body
        ?.accepts
        ?.firstOrNull()
        ?.maxAmountRequired
        ?.toDoubleOrNull()
        ?: 100000.0

    return String.format(java.util.Locale.US, "%.2f", rawAmount / 1_000_000.0)
}

@Composable
private fun AiPaymentRequiredDialog(
    isWalletConnected: Boolean,
    isPaymentLoading: Boolean,
    paymentAmountText: String,
    onDismiss: () -> Unit,
    onConnectPhantomClick: () -> Unit,
    onConnectSolflareClick: () -> Unit,
    onPaymentClick: () -> Unit
) {
    val isDark = isAnalysisDarkTheme()
    val surfaceColor = if (isDark) Color(0xFF171A2B) else Color(0xFFFFFFFF)
    val innerSurfaceColor = if (isDark) Color(0xFF101323) else Color(0xFFF7FBFF)
    val borderColor = if (isDark) Color(0xFF6D5AA8) else Color(0xFF93C5FD)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val bodyColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val primaryButtonColor = if (isDark) Color(0xFF7C3AED) else Color(0xFF2563EB)
    val secondaryButtonColor = if (isDark) Color(0xFF25243A) else Color(0xFFEFF6FF)
    val secondaryTextColor = if (isDark) Color(0xFFEDE9FE) else Color(0xFF1D4ED8)

    Dialog(onDismissRequest = { if (!isPaymentLoading) onDismiss() }) {
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
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWalletConnected) "AI 분석 결제" else "지갑 연결 필요",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
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
                            text = if (isWalletConnected) "무료 분석 횟수 소진" else "지갑 연결 필요",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isWalletConnected) {
                                "무료 분석 횟수를 모두 사용했어요.\n${paymentAmountText} USDC 결제가 필요합니다."
                            } else {
                                "추가 분석을 하기 위해서는 결제가 필요합니다. 먼저 지갑을 연결해주세요."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
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
                            enabled = !isPaymentLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = secondaryButtonColor,
                                contentColor = secondaryTextColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("닫기", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPaymentClick,
                            enabled = !isPaymentLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryButtonColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isPaymentLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("결제 중", fontWeight = FontWeight.Bold)
                            } else {
                                Text("결제하기", fontWeight = FontWeight.Bold)
                            }
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
                                contentColor = Color.White
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

// 요약 카드 묶음
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun SummaryCardSection( // SummaryCardSection 함수를 선언함
    totalExpense: Int, // 소비 내역 값을 받음
    averageDailyExpense: Int, // 소비 내역 값을 받음
    budgetUsageRate: Float // 예산 관련 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        GradientSummaryCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = "이번 달 총 지출", // 제목을 정해줌
            valueText = "${formatWon(totalExpense)}원", // valueText 값을 정해줌
            subText = "지난 달 대비 -12%" // subText 값을 정해줌
        )

        WhiteSummaryCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = "일 평균 지출", // 제목을 정해줌
            valueText = "${formatWon(averageDailyExpense)}원", // valueText 값을 정해줌
            subText = "약 -5% 절약중" // subText 값을 정해줌
        )

        BudgetUsageCard( // 내용을 카드 모양으로 묶어서 보여줌
            usageRate = budgetUsageRate // 예산 관련 값을 usageRate 값에 넣음
        )
    }
}

// 보라색 그라데이션 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun GradientSummaryCard( // GradientSummaryCard 함수를 선언함
    title: String, // 제목을 받음
    valueText: String, // valueText 값을 받음
    subText: String // subText 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAnalysisDarkTheme()
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val valueColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, // elevation 값을 정해줌
                shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
                ambientColor = SpentopiaGlowPurple.copy(alpha = 0.14f), // ambientColor 값을 정해줌
                spotColor = SpentopiaGlowPurple.copy(alpha = 0.18f) // spotColor 값을 정해줌
        ),
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .border(
                    width = 1.dp, // width 값을 정해줌
                    color = MaterialTheme.colorScheme.outlineVariant, // color 값을 정해줌
                    shape = RoundedCornerShape(18.dp) // shape 값을 정해줌
                )
                .padding(18.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = title, // 제목을 text 값에 넣음
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor // color 값을 정해줌
                )

                Text( // 화면에 글자를 보여줌
                    text = valueText, // valueText 값을 text 값에 넣음
                    fontSize = 34.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = valueColor // color 값을 정해줌
                )

                Text( // 화면에 글자를 보여줌
                    text = subText, // subText 값을 text 값에 넣음
                    fontSize = 12.sp, // fontSize 값을 정해줌
                    color = subTextColor // color 값을 정해줌
                )
            }
        }
    }
}

// 흰색 일반 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun WhiteSummaryCard( // WhiteSummaryCard 함수를 선언함
    title: String, // 제목을 받음
    valueText: String, // valueText 값을 받음
    subText: String // subText 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAnalysisDarkTheme()
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val valueColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                fontSize = 13.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold,
                color = titleColor // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = valueText, // valueText 값을 text 값에 넣음
                fontSize = 22.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = valueColor // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = subText, // subText 값을 text 값에 넣음
                fontSize = 12.sp, // fontSize 값을 정해줌
                color = subTextColor // color 값을 정해줌
            )
        }
    }
}

// 예산 사용률 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun BudgetUsageCard( // BudgetUsageCard 함수를 선언함
    usageRate: Float // usageRate 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val percentText = (usageRate * 100).roundToInt() // percentText 값을 저장함
    val progress = usageRate.coerceIn(0f, 1f) // progress 값을 저장함
    val isDark = isAnalysisDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val percentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val waveShift by rememberInfiniteTransition().animateFloat( // 게이지 안의 빛이 계속 흐르게 만듦
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val progressBrush = Brush.linearGradient( // 성실도 게이지처럼 울렁이는 진행 색상임
        colors = if (isDark) {
            listOf(
                SpentopiaMutedPurple,
                MaterialTheme.colorScheme.primary,
                SpentopiaGlowPurple.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.primary
            )
        } else {
            listOf(
                Color(0xFF93C5FD),
                MaterialTheme.colorScheme.primary,
                Color(0xFF38BDF8),
                MaterialTheme.colorScheme.primary
            )
        },
        start = Offset(-220f + waveShift * 260f, 0f),
        end = Offset(260f + waveShift * 260f, 0f)
    )

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "예산 사용률", // text 값을 정해줌
                fontSize = 13.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold,
                color = titleColor // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = "${percentText}%", // text 값을 정해줌
                fontSize = 22.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = percentColor // color 값을 정해줌
            )

            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isDark) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color(0xFFE0F2FE)
                        }
                    )
                    .border(
                        width = 1.dp, // width 값을 정해줌
                        color = MaterialTheme.colorScheme.outlineVariant, // color 값을 정해줌
                        shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                    )
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth(progress)
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(progressBrush)
                )
            }
        }
    }
}

// 최대 소비 카테고리 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun TopCategoryCard( // TopCategoryCard 함수를 선언함
    categoryName: String, // categoryName 값을 받음
    ratio: Float // ratio 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAnalysisDarkTheme()
    val hasCategoryData = categoryName.isNotBlank() && ratio > 0f
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val valueColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val ratioColor = if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1)
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "최대 소비 카테고리", // text 값을 정해줌
                fontSize = 13.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold,
                color = titleColor // color 값을 정해줌
            )

            Row( // 안쪽 UI를 가로로 배치함
                verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
                horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = if (hasCategoryData) "•" else "—", // text 값을 정해줌
                    fontSize = 26.sp, // fontSize 값을 정해줌
                    color = if (hasCategoryData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text( // 화면에 글자를 보여줌
                    text = if (hasCategoryData) categoryName else "아직 데이터 없음", // categoryName 값을 text 값에 넣음
                    fontSize = 22.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = valueColor // color 값을 정해줌
                )
            }

            Text( // 화면에 글자를 보여줌
                text = if (hasCategoryData) "전체의 ${(ratio * 100).roundToInt()}%" else "가계부에 소비 기록을 입력하면 표시됩니다.", // text 값을 정해줌
                fontSize = 13.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold,
                color = ratioColor // color 값을 정해줌
            )
        }
    }
}

// 주간 / 월간 토글 전체
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun PeriodToggleSection( // PeriodToggleSection 함수를 선언함
    selectedPeriod: String, // selectedPeriod 값을 받음
    onSelectPeriod: (String) -> Unit // onSelectPeriod 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.outlineVariant, // color 값을 정해줌
                shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp) // horizontalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        PeriodToggleButton( // 누를 수 있는 버튼을 만듦
            text = "주간", // text 값을 정해줌
            isSelected = selectedPeriod == "주간", // isSelected인지 여부를 정해줌
            onClick = { onSelectPeriod("주간") }, // 눌렀을 때 실행할 함수를 정해줌
            modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
        )

        PeriodToggleButton( // 누를 수 있는 버튼을 만듦
            text = "월간", // text 값을 정해줌
            isSelected = selectedPeriod == "월간", // isSelected인지 여부를 정해줌
            onClick = { onSelectPeriod("월간") }, // 눌렀을 때 실행할 함수를 정해줌
            modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
        )
    }
}

// 개별 토글 버튼
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun PeriodToggleButton( // PeriodToggleButton 함수를 선언함
    text: String, // text 값을 받음
    isSelected: Boolean, // isSelected인지 여부를 받음
    onClick: () -> Unit, // 눌렀을 때 실행할 함수를 받음
    modifier: Modifier = Modifier // modifier 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val interactionSource = remember { MutableInteractionSource() } // 화면이 다시 그려져도 interactionSource 값을 기억함
    val pressed by interactionSource.collectIsPressedAsState() // pressed 값을 저장함
    Button( // 누를 수 있는 버튼을 만듦
        onClick = onClick, // 눌렀을 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        interactionSource = interactionSource, // interactionSource 값을 interactionSource 값에 넣음
        modifier = modifier // modifier 값을 modifier 값에 넣음
            .height(40.dp)
            .graphicsLayer { // 이 블록 안의 내용이 시작됨
                scaleX = if (pressed) 0.985f else 1f // scaleX 값을 정해줌
                scaleY = if (pressed) 0.985f else 1f // scaleY 값을 정해줌
            },
        shape = RoundedCornerShape(999.dp), // shape 값을 정해줌
        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, // containerColor 값을 정해줌
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface // contentColor 값을 정해줌
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
        )
    }
}

// 소비 추이 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun ExpenseTrendCard( // ExpenseTrendCard 함수를 선언함
    title: String, // 제목을 받음
    expenseList: List<Pair<String, Int>>,
    selectedPeriod: String // selectedPeriod 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(18.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                fontSize = 20.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            if (selectedPeriod == "월간") { // 조건이 맞는지 확인함
                MonthlyLineChart( // Monthly Line Chart 함수를 실행함
                    expenseList = expenseList // 소비 내역 값을 소비 내역 값에 넣음
                )
            } else { // 이 블록 안의 내용이 시작됨
                SimpleBarChart( // Simple Bar Chart 함수를 실행함
                    expenseList = expenseList // 소비 내역 값을 소비 내역 값에 넣음
                )
            }
        }
    }
}

// 월간 소비 추이 라인 차트
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun MonthlyLineChart( // MonthlyLineChart 함수를 선언함
    expenseList: List<Pair<String, Int>>
) { // 이 블록 안의 내용이 시작됨
    var selectedMonthIndex by remember { mutableIntStateOf(-1) } // 화면이 다시 그려져도 selectedMonthIndex 값을 기억함
    val normalizedList = remember(expenseList) { // 화면이 다시 그려져도 normalizedList 값을 기억함
        val amountMap = expenseList.toMap() // amountMap 값을 저장함
        (1..12).map { month ->
            "${month}월" to (amountMap["${month}월"] ?: 0)
        }
    }
    val maxAmount = normalizedList.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1 // maxAmount 값을 저장함
    val yAxisSteps = remember(maxAmount) { // 화면이 다시 그려져도 yAxisSteps 값을 기억함
        val topAmount = max(60000, ((maxAmount + 9999) / 10000) * 10000) // topAmount 값을 저장함
        listOf(0, topAmount / 4, topAmount / 2, topAmount * 3 / 4, topAmount) // list Of 함수를 실행함
    }
    val isDark = isAnalysisDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val gridLineColor = if (isDark) Color(0xFF6B7280) else MaterialTheme.colorScheme.outlineVariant // gridLineColor 값을 저장함
    val axisTextColor = if (isDark) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.onSurfaceVariant // axisTextColor 값을 저장함
    val lineColor = if (isDark) SpentopiaGlowPurple else MaterialTheme.colorScheme.primary // lineColor 값을 저장함
    val pointFillColor = if (isDark) Color(0xFF111827) else MaterialTheme.colorScheme.surface // pointFillColor 값을 저장함
    val chartTopAmount = yAxisSteps.last().coerceAtLeast(1) // chartTopAmount 값을 저장함

    Column(modifier = Modifier.fillMaxWidth()) { // 안쪽 UI를 세로로 배치함
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .height(240.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .width(48.dp)
                    .height(200.dp),
                verticalArrangement = Arrangement.SpaceBetween // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                yAxisSteps.reversed().forEach { value ->
                    Text( // 화면에 글자를 보여줌
                        text = formatCompactAmount(value), // text 값을 정해줌
                        fontSize = 10.sp, // fontSize 값을 정해줌
                        color = axisTextColor // axisTextColor 값을 color 값에 넣음
                    )
                }
            }

            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .height(232.dp)
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(normalizedList) { // 이 블록 안의 내용이 시작됨
                            awaitPointerEventScope { // 이 블록 안의 내용이 시작됨
                                while (true) { // 조건이 맞는 동안 계속 반복함
                                    val event = awaitPointerEvent() // event 값을 저장함
                                    val change = event.changes.firstOrNull() // change 값을 저장함
                                    if (change != null && change.pressed) { // 조건이 맞는지 확인함
                                        val chartWidth = size.width.toFloat().coerceAtLeast(1f) // chartWidth 값을 저장함
                                        val xRatio = (change.position.x / chartWidth).coerceIn(0f, 1f) // xRatio 값을 저장함
                                        selectedMonthIndex = (xRatio * normalizedList.lastIndex) // selectedMonthIndex 값을 정해줌
                                            .roundToInt()
                                            .coerceIn(0, normalizedList.lastIndex)
                                    }
                                }
                            }
                        }
                ) { // 이 블록 안의 내용이 시작됨
                    Canvas(modifier = Modifier.matchParentSize()) { // UI 크기나 여백 같은 모양을 정함
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) // dash 값을 저장함

                        for (i in 0 until 5) { // 목록을 하나씩 돌면서 실행함
                            val y = size.height * i / 4f // y 값을 저장함
                            drawLine( // draw Line 함수를 실행함
                                color = gridLineColor, // gridLineColor 값을 color 값에 넣음
                                start = Offset(0f, y), // start 값을 정해줌
                                end = Offset(size.width, y), // end 값을 정해줌
                                pathEffect = dash, // dash 값을 pathEffect 값에 넣음
                                strokeWidth = if (isDark) 1.6f else 1f // strokeWidth 값을 정해줌
                            )
                        }

                        val horizontalGap = size.width / 11f // horizontalGap 값을 저장함
                        val points = normalizedList.mapIndexed { index, item -> // points 값을 저장함
                            val x = horizontalGap * index // x 값을 저장함
                            val ratio = item.second.toFloat() / chartTopAmount.toFloat() // ratio 값을 저장함
                            val y = size.height - (size.height * ratio.coerceIn(0f, 1f)) // y 값을 저장함
                            Offset(x, y) // Offset 함수를 실행함
                        }

                        if (points.size >= 2) { // 조건이 맞는지 확인함
                            val path = Path().apply { // path 값을 저장함
                                moveTo(points.first().x, points.first().y) // move To 함수를 실행함
                                for (index in 1 until points.size) { // 목록을 하나씩 돌면서 실행함
                                    val previous = points[index - 1] // previous 값을 저장함
                                    val current = points[index] // current 값을 저장함
                                    val controlX = (previous.x + current.x) / 2f // controlX 값을 저장함
                                    cubicTo( // cubic To 함수를 실행함
                                        controlX,
                                        previous.y,
                                        controlX,
                                        current.y,
                                        current.x,
                                        current.y
                                    )
                                }
                            }

                            drawPath( // draw Path 함수를 실행함
                                path = path, // path 값을 path 값에 넣음
                                color = lineColor, // lineColor 값을 color 값에 넣음
                                style = Stroke(width = 5f) // style 값을 정해줌
                            )
                        }

                        points.forEachIndexed { index, point ->
                            val amount = normalizedList[index].second // 금액을 저장함
                            val selected = index == selectedMonthIndex // selected 값을 저장함
                            if (amount > 0) { // 조건이 맞는지 확인함
                                drawCircle( // draw Circle 함수를 실행함
                                    color = lineColor.copy(alpha = 0.18f), // color 값을 정해줌
                                    radius = if (selected) 17f else 12f, // radius 값을 정해줌
                                    center = point // point 값을 center 값에 넣음
                                )
                            }

                            drawCircle( // draw Circle 함수를 실행함
                                color = lineColor, // lineColor 값을 color 값에 넣음
                                radius = when { // radius 값을 정해줌
                                    selected -> 9f
                                    amount > 0 -> 7f
                                    else -> 4.5f // 위 조건이 아니면 이쪽을 실행함
                                },
                                center = point // point 값을 center 값에 넣음
                            )
                            drawCircle( // draw Circle 함수를 실행함
                                color = pointFillColor, // pointFillColor 값을 color 값에 넣음
                                radius = if (selected) 4.5f else if (amount > 0) 3.5f else 2.2f, // radius 값을 정해줌
                                center = point // point 값을 center 값에 넣음
                            )
                        }
                    }

                }

                Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.SpaceBetween // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    normalizedList.forEach { item ->
                        Text( // 화면에 글자를 보여줌
                            text = item.first, // text 값을 정해줌
                            fontSize = 9.sp, // fontSize 값을 정해줌
                            color = axisTextColor // axisTextColor 값을 color 값에 넣음
                        )
                    }
                }
            }
        }

        val selectedMonth = normalizedList.getOrNull(selectedMonthIndex) // selectedMonth 값을 저장함
        if (selectedMonth != null) { // 조건이 맞는지 확인함
            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함
            Text( // 화면에 글자를 보여줌
                text = "${selectedMonth.first} ${formatAmount(selectedMonth.second)}", // text 값을 정해줌
                fontSize = 12.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.primary // color 값을 정해줌
            )
        }
    }
}

// 간단한 막대 그래프
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun SimpleBarChart( // SimpleBarChart 함수를 선언함
    expenseList: List<Pair<String, Int>>
) { // 이 블록 안의 내용이 시작됨
    val maxAmount = (expenseList.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1) // maxAmount 값을 저장함
    val yAxisSteps = listOf(0, 15000, 30000, 45000, 60000) // yAxisSteps 값을 저장함
    val isDark = isAnalysisDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val gridLineColor = if (isDark) Color(0xFF6B7280) else MaterialTheme.colorScheme.outlineVariant // gridLineColor 값을 저장함
    val axisTextColor = if (isDark) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.onSurfaceVariant // axisTextColor 값을 저장함

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .height(220.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .width(42.dp)
                    .height(200.dp),
                verticalArrangement = Arrangement.SpaceBetween // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                yAxisSteps.reversed().forEach { value ->
                    Text( // 화면에 글자를 보여줌
                        text = value.toString(), // text 값을 정해줌
                        fontSize = 10.sp, // fontSize 값을 정해줌
                        color = axisTextColor // axisTextColor 값을 color 값에 넣음
                    )
                }
            }

            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .height(200.dp)
            ) { // 이 블록 안의 내용이 시작됨
                Canvas( // Canvas 함수를 실행함
                    modifier = Modifier.matchParentSize() // UI 크기나 여백 같은 모양을 정함
                ) { // 이 블록 안의 내용이 시작됨
                    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) // dash 값을 저장함

                    for (i in 0 until 5) { // 목록을 하나씩 돌면서 실행함
                        val y = size.height * i / 4f // y 값을 저장함
                        drawLine( // draw Line 함수를 실행함
                            color = gridLineColor, // gridLineColor 값을 color 값에 넣음
                            start = Offset(0f, y), // start 값을 정해줌
                            end = Offset(size.width, y), // end 값을 정해줌
                            pathEffect = dash, // dash 값을 pathEffect 값에 넣음
                            strokeWidth = if (isDark) 1.6f else 1f // strokeWidth 값을 정해줌
                        )
                    }
                }

                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxSize()
                        .padding(horizontal = 6.dp), // .padding(horizontal 값을 정해줌
                    horizontalArrangement = Arrangement.SpaceEvenly, // horizontalArrangement 값을 정해줌
                    verticalAlignment = Alignment.Bottom // verticalAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    expenseList.forEach { item ->
                        BarChartItem( // Bar Chart Item 함수를 실행함
                            label = item.first, // label 값을 정해줌
                            amount = item.second, // 금액을 정해줌
                            maxAmount = maxAmount, // maxAmount 값을 maxAmount 값에 넣음
                            isDark = isDark // isDark인지 여부를 isDark인지 여부에 넣음
                        )
                    }
                }
            }
        }
    }
}

// 개별 막대
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun BarChartItem( // BarChartItem 함수를 선언함
    label: String, // label 값을 받음
    amount: Int, // 금액을 받음
    maxAmount: Int, // maxAmount 값을 받음
    isDark: Boolean // isDark인지 여부를 받음
) { // 이 블록 안의 내용이 시작됨
    val ratio = amount.toFloat() / maxAmount.toFloat() // ratio 값을 저장함
    val chartColor = if (isDark) SpentopiaGlowPurple else MaterialTheme.colorScheme.primary // chartColor 값을 저장함
    val barColors = if (isDark) { // barColors 값을 저장함
        listOf(chartColor.copy(alpha = 0.92f), chartColor) // listOf(chartColor.copy(alpha 값을 정해줌
    } else { // 이 블록 안의 내용이 시작됨
        listOf(chartColor.copy(alpha = 0.72f), chartColor) // listOf(chartColor.copy(alpha 값을 정해줌
    }
    val labelColor = if (isDark) Color(0xFFF9FAFB) else MaterialTheme.colorScheme.onSurfaceVariant // labelColor 값을 저장함

    Column( // 안쪽 UI를 세로로 배치함
        horizontalAlignment = Alignment.CenterHorizontally, // horizontalAlignment 값을 정해줌
        verticalArrangement = Arrangement.Bottom, // verticalArrangement 값을 정해줌
        modifier = Modifier.width(28.dp) // UI 크기나 여백 같은 모양을 정함
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .height((150 * ratio).dp)
                .width(16.dp)
                .background(
                    brush = Brush.verticalGradient( // brush 값을 정해줌
                        colors = barColors // barColors 값을 colors 값에 넣음
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp) // shape 값을 정해줌
                )
        )

        Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = label, // label 값을 text 값에 넣음
            fontSize = 11.sp, // fontSize 값을 정해줌
            color = labelColor // labelColor 값을 color 값에 넣음
        )
    }
}

// 카테고리별 지출 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CategoryPieChartCard( // CategoryPieChartCard 함수를 선언함
    categoryList: List<CategorySpendUiModel> // categoryList 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val totalAmount = categoryList.sumOf { it.amount } // totalAmount 값을 저장함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(18.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "카테고리별 지출", // text 값을 정해줌
                fontSize = 20.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalAlignment = Alignment.CenterHorizontally, // horizontalAlignment 값을 정해줌
                verticalArrangement = Arrangement.spacedBy(18.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                CategoryPieChart( // Category Pie Chart 함수를 실행함
                    categoryList = categoryList, // categoryList 값을 categoryList 값에 넣음
                    totalAmount = totalAmount // totalAmount 값을 totalAmount 값에 넣음
                )

                Column( // 안쪽 UI를 세로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    categoryList.forEach { item ->
                        PieLegendItem(item = item) // PieLegendItem(item 값을 정해줌
                    }
                }
            }
        }
    }
}

// 도넛 차트
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CategoryPieChart( // CategoryPieChart 함수를 선언함
    categoryList: List<CategorySpendUiModel>, // categoryList 값을 받음
    totalAmount: Int // totalAmount 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val totalRatio = categoryList.sumOf { it.ratio.toDouble() }.toFloat().coerceAtLeast(1f) // totalRatio 값을 저장함
    val isEmpty = totalAmount <= 0 || categoryList.isEmpty() // 비어있는 상태인지 저장함
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f) // trackColor 값을 저장함

    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        contentAlignment = Alignment.Center, // contentAlignment 값을 정해줌
        modifier = Modifier.size(230.dp) // UI 크기나 여백 같은 모양을 정함
    ) { // 이 블록 안의 내용이 시작됨
        Canvas( // Canvas 함수를 실행함
            modifier = Modifier.size(230.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            var startAngle = -90f // 나중에 바뀔 수 있는 startAngle 값을 저장함
            val strokeWidth = 44f // strokeWidth 값을 저장함

            drawArc( // draw Arc 함수를 실행함
                color = trackColor, // trackColor 값을 color 값에 넣음
                startAngle = -90f, // startAngle 값을 정해줌
                sweepAngle = 360f, // sweepAngle 값을 정해줌
                useCenter = false, // false 값을 useCenter 값에 넣음
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round) // style 값을 정해줌
            )

            if (!isEmpty) { // 조건이 맞는지 확인함
                categoryList.forEach { item ->
                    val sweepAngle = (item.ratio / totalRatio) * 360f // sweepAngle 값을 저장함

                    drawArc( // draw Arc 함수를 실행함
                        color = item.color, // color 값을 정해줌
                        startAngle = startAngle, // startAngle 값을 startAngle 값에 넣음
                        sweepAngle = (sweepAngle - 2f).coerceAtLeast(0f), // sweepAngle 값을 정해줌
                        useCenter = false, // false 값을 useCenter 값에 넣음
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round) // style 값을 정해줌
                    )

                    startAngle += sweepAngle // + 값을 정해줌
                }
            }
        }

        Column( // 안쪽 UI를 세로로 배치함
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "총 지출", // text 값을 정해줌
                fontSize = 12.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = "${formatWon(totalAmount)}원", // text 값을 정해줌
                fontSize = 20.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }
    }
}

// 도넛 차트 범례 1줄
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun PieLegendItem( // PieLegendItem 함수를 선언함
    item: CategorySpendUiModel // item 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val percent = (item.ratio * 100).roundToInt() // percent 값을 저장함

    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f), // color 값을 정해줌
                shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
            )
            .padding(horizontal = 12.dp, vertical = 10.dp), // .padding(horizontal 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .size(12.dp)
                .background(item.color, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = item.name, // text 값을 정해줌
            fontSize = 14.sp, // fontSize 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
            modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
        )

        Text( // 화면에 글자를 보여줌
            text = "${percent}%", // text 값을 정해줌
            fontSize = 13.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
        )
    }
}

// 카테고리 상세 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CategoryDetailCard( // CategoryDetailCard 함수를 선언함
    categoryList: List<CategorySpendUiModel> // categoryList 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(18.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "카테고리 상세", // text 값을 정해줌
                fontSize = 20.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            categoryList.forEachIndexed { index, item ->
                CategoryDetailItem(item = item) // CategoryDetailItem(item 값을 정해줌

                if (index != categoryList.lastIndex) { // 조건이 맞는지 확인함
                    HorizontalDivider( // Horizontal Divider 함수를 실행함
                        thickness = 0.6.dp, // thickness 값을 정해줌
                        color = Color.Transparent // color 값을 정해줌
                    )
                }
            }
        }
    }
}

// 카테고리 상세 1줄
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CategoryDetailItem( // CategoryDetailItem 함수를 선언함
    item: CategorySpendUiModel // item 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val percent = (item.ratio * 100).roundToInt() // percent 값을 저장함

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), // color 값을 정해줌
                shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = item.name, // text 값을 정해줌
                fontSize = 15.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
            )

            Text( // 화면에 글자를 보여줌
                text = "${formatWon(item.amount)}원", // text 값을 정해줌
                fontSize = 15.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }

        CategoryProgressBar( // Category Progress Bar 함수를 실행함
            ratio = item.ratio, // ratio 값을 정해줌
            color = item.color // color 값을 정해줌
        )

        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            horizontalArrangement = Arrangement.End // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "${percent}%", // text 값을 정해줌
                fontSize = 12.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                textAlign = TextAlign.End // textAlign 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CategoryProgressBar( // CategoryProgressBar 함수를 선언함
    ratio: Float, // ratio 값을 받음
    color: Color // color 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .height(10.dp)
            .background(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f), // color 값을 정해줌
                shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
            )
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(10.dp)
                .background(
                    color = color, // color 값을 color 값에 넣음
                    shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                )
        )
    }
}

// AI 리포트 섹션
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AiAnalysisReportSection( // AiAnalysisReportSection 함수를 선언함
    totalExpense: Int, // 소비 내역 값을 받음
    aiReport: AiConsumptionReportUiModel?, // aiReport 값을 받음
    isLoading: Boolean, // 로딩 여부를 받음
    errorMessage: String, // 오류 내용을 받음
    onRequestAiAnalysis: () -> Unit // onRequestAiAnalysis 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "AI 소비 분석 리포트", // text 값을 정해줌
                fontSize = 22.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Button( // 누를 수 있는 버튼을 만듦
                onClick = onRequestAiAnalysis, // onRequestAiAnalysis 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                enabled = !isLoading && totalExpense > 0 && aiReport == null, // enabled 값을 정해줌
                shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer, // contentColor 값을 정해줌
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, // disabledContainerColor 값을 정해줌
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant // disabledContentColor 값을 정해줌
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp) // contentPadding 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = when { // text 값을 정해줌
                        isLoading -> "분석 중"
                        aiReport != null -> "분석 완료" // ! 값을 정해줌
                        else -> "AI 분석" // 위 조건이 아니면 이쪽을 실행함
                    },
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                )
            }
        }

        AiGeneratedReportCard( // 내용을 카드 모양으로 묶어서 보여줌
            totalExpense = totalExpense, // 소비 내역 값을 소비 내역 값에 넣음
            aiReport = aiReport, // aiReport 값을 aiReport 값에 넣음
            isLoading = isLoading, // 로딩 여부를 로딩 여부에 넣음
            errorMessage = errorMessage // 오류 내용을 오류 내용에 넣음
        )

    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AiGeneratedReportCard( // AiGeneratedReportCard 함수를 선언함
    totalExpense: Int, // 소비 내역 값을 받음
    aiReport: AiConsumptionReportUiModel?, // aiReport 값을 받음
    isLoading: Boolean, // 로딩 여부를 받음
    errorMessage: String // 오류 내용을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .border(
                    width = 1.dp, // width 값을 정해줌
                    color = MaterialTheme.colorScheme.outlineVariant, // color 값을 정해줌
                    shape = RoundedCornerShape(16.dp) // shape 값을 정해줌
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            when { // 값 종류에 따라 실행할 코드를 나눔
                totalExpense <= 0 -> { // < 값을 정해줌
                    AiReportStatusText( // 화면에 글자를 보여줌
                        title = "AI 소비 코멘트", // 제목을 정해줌
                        message = "아직 분석할 소비 데이터가 없습니다. 가계부에서 소비 기록을 먼저 입력해주세요." // 메시지를 정해줌
                    )
                }
                isLoading -> { // 이 블록 안의 내용이 시작됨
                    AiReportStatusText( // 화면에 글자를 보여줌
                        title = "AI 소비 코멘트", // 제목을 정해줌
                        message = "AI가 이번 기간의 소비 기록을 분석하고 있습니다." // 메시지를 정해줌
                    )
                }
                errorMessage.isNotBlank() -> { // 이 블록 안의 내용이 시작됨
                    AiReportStatusText( // 화면에 글자를 보여줌
                        title = "AI 소비 코멘트", // 제목을 정해줌
                        message = errorMessage, // 오류 내용을 메시지에 넣음
                        isError = true // true 값을 오류 여부에 넣음
                    )
                }
                aiReport != null -> { // ! 값을 정해줌
                    AiReportGrid(report = aiReport) // AiReportGrid(report 값을 정해줌
                }
                else -> { // 위 조건이 아니면 이쪽을 실행함
                    AiReportStatusText( // 화면에 글자를 보여줌
                        title = "AI 소비 코멘트", // 제목을 정해줌
                        message = "AI 분석 버튼을 누르면 이번 기간의 소비 데이터를 바탕으로 맞춤 리포트를 생성합니다." // 메시지를 정해줌
                    )
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AiReportStatusText( // AiReportStatusText 함수를 선언함
    title: String, // 제목을 받음
    message: String, // 메시지를 받음
    isError: Boolean = false // 오류 여부를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAnalysisDarkTheme()
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val messageColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    Text( // 화면에 글자를 보여줌
        text = title, // 제목을 text 값에 넣음
        fontSize = 16.sp, // fontSize 값을 정해줌
        fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
        color = titleColor // color 값을 정해줌
    )

    Text( // 화면에 글자를 보여줌
        text = message, // 메시지를 text 값에 넣음
        fontSize = 14.sp, // fontSize 값을 정해줌
        lineHeight = 22.sp, // lineHeight 값을 정해줌
        color = if (isError) { // color 값을 정해줌
            Color(0xFFE53935) // Color 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            messageColor
        }
    )
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AiReportGrid( // AiReportGrid 함수를 선언함
    report: AiConsumptionReportUiModel // report 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        AiReportItemCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = "좋은 점", // 제목을 정해줌
            emoji = "👍", // emoji 값을 정해줌
            message = report.good, // 메시지를 정해줌
            borderColor = Color(0xFFB7E4C7) // borderColor 값을 정해줌
        )
        AiReportItemCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = "주의", // 제목을 정해줌
            emoji = "⚠️", // emoji 값을 정해줌
            message = report.warning, // 메시지를 정해줌
            borderColor = Color(0xFFFFD166) // borderColor 값을 정해줌
        )
        AiReportItemCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = "조언", // 제목을 정해줌
            emoji = "💡", // emoji 값을 정해줌
            message = report.advice, // 메시지를 정해줌
            borderColor = Color(0xFFD6C8FF) // borderColor 값을 정해줌
        )
        AiReportItemCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = "예측", // 제목을 정해줌
            emoji = "📈", // emoji 값을 정해줌
            message = report.prediction, // 메시지를 정해줌
            borderColor = Color(0xFFA7C7FF) // borderColor 값을 정해줌
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AiReportItemCard( // AiReportItemCard 함수를 선언함
    title: String, // 제목을 받음
    emoji: String, // emoji 값을 받음
    message: String, // 메시지를 받음
    borderColor: Color // borderColor 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .border(
                    width = 1.dp, // width 값을 정해줌
                    color = borderColor, // borderColor 값을 color 값에 넣음
                    shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                )
                .padding(horizontal = 14.dp, vertical = 13.dp), // .padding(horizontal 값을 정해줌
            verticalArrangement = Arrangement.spacedBy(6.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = emoji, // emoji 값을 text 값에 넣음
                    fontSize = 17.sp // fontSize 값을 정해줌
                )

                Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = title, // 제목을 text 값에 넣음
                    fontSize = 15.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }

            Text( // 화면에 글자를 보여줌
                text = message.ifBlank { "분석 결과가 비어 있습니다." }, // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                lineHeight = 21.sp, // lineHeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

// AI 리포트 카드 1개
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AnalysisTipCard( // AnalysisTipCard 함수를 선언함
    tip: AnalysisTipUiModel // tip 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .border(
                    width = 1.dp, // width 값을 정해줌
                    color = tip.borderColor, // color 값을 정해줌
                    shape = RoundedCornerShape(16.dp) // shape 값을 정해줌
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = tip.emoji, // text 값을 정해줌
                    fontSize = 18.sp // fontSize 값을 정해줌
                )

                Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = tip.title, // text 값을 정해줌
                    fontSize = 16.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }

            Text( // 화면에 글자를 보여줌
                text = tip.description, // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                lineHeight = 22.sp, // lineHeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

// 소비 패턴 분석 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun ConsumptionPatternCard( // ConsumptionPatternCard 함수를 선언함
    aiReport: AiConsumptionReportUiModel?, // aiReport 값을 받음
    isLoading: Boolean // 로딩 여부를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAnalysisDarkTheme()
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val bodyColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(20.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "소비 패턴 분석", // text 값을 정해줌
                fontSize = 20.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = titleColor // color 값을 정해줌
            )

            when { // 값 종류에 따라 실행할 코드를 나눔
                isLoading -> { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "AI가 소비 패턴을 분석하고 있습니다.", // text 값을 정해줌
                        fontSize = 14.sp, // fontSize 값을 정해줌
                        lineHeight = 22.sp, // lineHeight 값을 정해줌
                        color = bodyColor // color 값을 정해줌
                    )
                }
                aiReport != null -> { // ! 값을 정해줌
                    ConsumptionTextReportCard( // 내용을 카드 모양으로 묶어서 보여줌
                        title = "분석", // 제목을 정해줌
                        emoji = "📊", // emoji 값을 정해줌
                        message = aiReport.pattern // 메시지를 정해줌
                            .replace("소비 패턴 분석:", "")
                            .trim()
                            .ifBlank { "분석 결과가 비어 있습니다." }
                    )

                    ConsumptionTextReportCard( // 내용을 카드 모양으로 묶어서 보여줌
                        title = "개선 방안", // 제목을 정해줌
                        emoji = "💡", // emoji 값을 정해줌
                        message = aiReport.improvement.ifBlank { "개선 방안이 비어 있습니다." } // 메시지를 정해줌
                    )
                }
                else -> { // 위 조건이 아니면 이쪽을 실행함
                    Text( // 화면에 글자를 보여줌
                        text = "AI 분석 버튼을 누르면 소비 패턴 분석과 개선 방안을 확인할 수 있습니다.", // text 값을 정해줌
                        fontSize = 14.sp, // fontSize 값을 정해줌
                        lineHeight = 22.sp, // lineHeight 값을 정해줌
                        color = bodyColor // color 값을 정해줌
                    )
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun ConsumptionTextReportCard( // ConsumptionTextReportCard 함수를 선언함
    title: String, // 제목을 받음
    emoji: String, // emoji 값을 받음
    message: String // 메시지를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAnalysisDarkTheme()
    val titleColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    val messageColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp), // .padding(horizontal 값을 정해줌
            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = emoji, // emoji 값을 text 값에 넣음
                    fontSize = 17.sp // fontSize 값을 정해줌
                )

                Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = title, // 제목을 text 값에 넣음
                    fontSize = 16.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = titleColor // color 값을 정해줌
                )
            }

            Text( // 화면에 글자를 보여줌
                text = message, // 메시지를 text 값에 넣음
                fontSize = 14.sp, // fontSize 값을 정해줌
                lineHeight = 22.sp, // lineHeight 값을 정해줌
                color = messageColor // color 값을 정해줌
            )
        }
    }
}

// 소비 패턴 진행률 1줄
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun PatternProgressRow( // PatternProgressRow 함수를 선언함
    item: PatternProgressUiModel // item 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = item.label, // text 값을 정해줌
            fontSize = 14.sp, // fontSize 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
            modifier = Modifier.width(110.dp) // UI 크기나 여백 같은 모양을 정함
        )

        LinearProgressIndicator( // Linear Progress Indicator 함수를 실행함
            progress = { item.ratio.coerceIn(0f, 1f) }, // progress 값을 정해줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .weight(1f)
                .height(8.dp),
            color = SpentopiaMutedPurple, // SpentopiaMutedPurple 값을 color 값에 넣음
            trackColor = MaterialTheme.colorScheme.outlineVariant // trackColor 값을 정해줌
        )

        Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = "${(item.ratio * 100).roundToInt()}%", // text 값을 정해줌
            fontSize = 13.sp, // fontSize 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
            fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
            modifier = Modifier.widthIn(min = 34.dp) // UI 크기나 여백 같은 모양을 정함
        )
    }
}

// 평일 / 주말 비교 행
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun PatternCompareRow( // PatternCompareRow 함수를 선언함
    leftLabel: String, // leftLabel 값을 받음
    leftValue: String, // leftValue 값을 받음
    rightLabel: String, // rightLabel 값을 받음
    rightValue: String // rightValue 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.spacedBy(12.dp) // horizontalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        SmallCompareCard( // 내용을 카드 모양으로 묶어서 보여줌
            label = leftLabel, // leftLabel 값을 label 값에 넣음
            value = leftValue, // leftValue 값을 입력값에 넣음
            modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
        )

        SmallCompareCard( // 내용을 카드 모양으로 묶어서 보여줌
            label = rightLabel, // rightLabel 값을 label 값에 넣음
            value = rightValue, // rightValue 값을 입력값에 넣음
            modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
        )
    }
}

// 비교용 작은 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun SmallCompareCard( // SmallCompareCard 함수를 선언함
    label: String, // label 값을 받음
    value: String, // 입력값을 받음
    modifier: Modifier = Modifier // modifier 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = modifier, // modifier 값을 modifier 값에 넣음
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = analysisSoftCardColor()), // colors 값을 정해줌
        border = BorderStroke(1.dp, analysisSoftCardBorderColor())
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(14.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = label, // label 값을 text 값에 넣음
                fontSize = 13.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = value, // 입력값을 text 값에 넣음
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }
    }
}

// 공유 함수
fun shareAnalysisReport( // shareAnalysisReport 함수를 선언함
    context: Context, // 현재 화면 정보를 받음
    reportText: String // reportText 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val sendIntent = Intent(Intent.ACTION_SEND).apply { // sendIntent 값을 저장함
        type = "text/plain" // type 값을 정해줌
        putExtra(Intent.EXTRA_SUBJECT, "Spentopia 소비 패턴 분석 리포트") // put Extra 함수를 실행함
        putExtra(Intent.EXTRA_TEXT, reportText) // put Extra 함수를 실행함
    }

    val chooser = Intent.createChooser(sendIntent, "리포트 공유") // chooser 값을 저장함
    context.startActivity(chooser)
}

// 다운로드 저장 함수
fun saveAnalysisReportToDownloads( // 데이터를 불러오는 함수 시작
    context: Context, // 현재 화면 정보를 받음
    fileName: String, // fileName 값을 받음
    content: String // 내용을 받음
): Boolean { // 이 블록 안의 내용이 시작됨
    return try { // 이 값을 함수 결과로 돌려줌
        val resolver = context.contentResolver // resolver 값을 저장함

        val values = ContentValues().apply { // values 값을 저장함
            put(MediaStore.Downloads.DISPLAY_NAME, fileName) // put 함수를 실행함
            put(MediaStore.Downloads.MIME_TYPE, "text/plain") // put 함수를 실행함
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // put 함수를 실행함
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) // 이미지 주소를 저장함
            ?: return false

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
            outputStream.flush()
        } ?: return false

        true
    } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
        e.printStackTrace()
        false
    }
}

// 리포트 텍스트 생성
fun buildAnalysisReportText( // buildAnalysisReportText 함수를 선언함
    totalExpense: Int, // 소비 내역 값을 받음
    averageDailyExpense: Int, // 소비 내역 값을 받음
    budgetUsageRate: Float, // 예산 관련 값을 받음
    topCategoryName: String, // topCategoryName 값을 받음
    topCategoryRatio: Float, // topCategoryRatio 값을 받음
    selectedPeriod: String, // selectedPeriod 값을 받음
    trendExpenseList: List<Pair<String, Int>>,
    categoryList: List<CategorySpendUiModel>, // categoryList 값을 받음
    tipList: List<AnalysisTipUiModel>, // tipList 값을 받음
    aiAnalysisText: String, // aiAnalysisText 값을 받음
    timePatternList: List<PatternProgressUiModel>, // timePatternList 값을 받음
    weekdayAverageText: String, // weekdayAverageText 값을 받음
    weekendAverageText: String, // weekendAverageText 값을 받음
    weekendComment: String, // weekendComment 값을 받음
    paymentPatternList: List<PatternProgressUiModel> // paymentPatternList 값을 받음
): String { // 이 블록 안의 내용이 시작됨
    val trendText = trendExpenseList.joinToString("\n") { (label, amount) -> // trendText 값을 저장함
        "- $label: ${formatWon(amount)}원"
    }

    val categoryText = categoryList.joinToString("\n") { item -> // categoryText 값을 저장함
        "- ${item.name}: ${formatWon(item.amount)}원 (${(item.ratio * 100).roundToInt()}%)"
    }

    val tipText = tipList.joinToString("\n") { tip -> // tipText 값을 저장함
        "- ${tip.title}: ${tip.description}"
    }

    val timePatternText = timePatternList.joinToString("\n") { item -> // timePatternText 값을 저장함
        "- ${item.label}: ${(item.ratio * 100).roundToInt()}%"
    }

    val paymentPatternText = paymentPatternList.joinToString("\n") { item -> // paymentPatternText 값을 저장함
        "- ${item.label}: ${(item.ratio * 100).roundToInt()}%"
    }

    return """
        [Spentopia 소비 패턴 분석 리포트]

        1. 요약
        - 이번 달 총 지출: ${formatWon(totalExpense)}원
        - 일 평균 지출: ${formatWon(averageDailyExpense)}원
        - 예산 사용률: ${(budgetUsageRate * 100).roundToInt()}%

        2. 최대 소비 카테고리
        - $topCategoryName (${(topCategoryRatio * 100).roundToInt()}%)

        3. ${selectedPeriod} 소비 추이
        $trendText

        4. 카테고리별 지출
        $categoryText

        5. AI 소비 분석
        $tipText

        5-1. AI 소비 코멘트
        ${aiAnalysisText.ifBlank { "아직 AI 소비 리포트를 생성하지 않았습니다." }}

        6. 시간대별 소비
        $timePatternText

        7. 요일별 소비
        - 평일 평균: $weekdayAverageText
        - 주말 평균: $weekendAverageText
        - 코멘트: $weekendComment

        8. 결제 방법
        $paymentPatternText
    """.trimIndent()
}

// 금액 포맷 함수
fun formatWon(value: Int): String { // formatWon 함수를 선언함
    return "%,d".format(value) // 이 값을 함수 결과로 돌려줌
}

fun formatAmount(value: Int): String { // formatAmount 함수를 선언함
    return "${formatWon(value)}원" // 이 값을 함수 결과로 돌려줌
}

fun formatCompactAmount(value: Int): String { // formatCompactAmount 함수를 선언함
    return when { // 이 값을 함수 결과로 돌려줌
        value >= 10000 -> "${value / 10000}만" // > 값을 정해줌
        else -> value.toString() // 위 조건이 아니면 이쪽을 실행함
    }
}
