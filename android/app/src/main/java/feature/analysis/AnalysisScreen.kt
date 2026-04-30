package com.ict.spentopia.feature.analysis

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple
import com.ict.spentopia.ui.theme.SpentopiaActionGradientColors
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors
import kotlin.math.roundToInt

// 소비분석 메인 화면임
// 요약/비중/AI리포트/공유/다운로드 한 화면
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trendExpenseList = viewModel.getCurrentTrendList()
    val context = LocalContext.current

    val reportText = buildAnalysisReportText(
        totalExpense = uiState.totalExpense,
        averageDailyExpense = uiState.averageDailyExpense,
        budgetUsageRate = uiState.budgetUsageRate,
        topCategoryName = uiState.topCategoryName,
        topCategoryRatio = uiState.topCategoryRatio,
        selectedPeriod = uiState.selectedPeriod,
        trendExpenseList = trendExpenseList,
        categoryList = uiState.categoryList,
        tipList = uiState.tipList,
        aiAnalysisText = uiState.aiAnalysisText,
        timePatternList = uiState.timePatternList,
        weekdayAverageText = uiState.weekdayAverageText,
        weekendAverageText = uiState.weekendAverageText,
        weekendComment = uiState.weekendComment,
        paymentPatternList = uiState.paymentPatternList
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상단 제목 + 공유/다운로드
        AnalysisHeaderSection(
            onShareClick = {
                shareAnalysisReport(context, reportText)
            },
            onDownloadClick = {
                val fileName = "spentopia_analysis_report_${System.currentTimeMillis()}.txt"

                val isSaved = saveAnalysisReportToDownloads(
                    context = context,
                    fileName = fileName,
                    content = reportText
                )

                Toast.makeText(
                    context,
                    if (isSaved) "리포트가 다운로드 폴더에 저장되었어요." else "리포트 저장에 실패했어요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        // 이번 달 핵심 수치 먼저 보여줌
        SummaryCardSection(
            totalExpense = uiState.totalExpense,
            averageDailyExpense = uiState.averageDailyExpense,
            budgetUsageRate = uiState.budgetUsageRate
        )

        TopCategoryCard(
            categoryName = uiState.topCategoryName,
            ratio = uiState.topCategoryRatio
        )

        PeriodToggleSection(
            selectedPeriod = uiState.selectedPeriod,
            onSelectPeriod = { selectedPeriod ->
                viewModel.selectPeriod(selectedPeriod)
            }
        )

        ExpenseTrendCard(
            title = if (uiState.selectedPeriod == "주간") "주간 소비 추이" else "월간 소비 추이",
            expenseList = trendExpenseList
        )

        CategoryPieChartCard(
            categoryList = uiState.categoryList
        )

        CategoryDetailCard(
            categoryList = uiState.categoryList
        )

        // AI 분석 리포트 영역
        AiAnalysisReportSection(
            tipList = uiState.tipList,
            totalExpense = uiState.totalExpense,
            aiAnalysisText = uiState.aiAnalysisText,
            isLoading = uiState.isAiAnalysisLoading,
            errorMessage = uiState.aiAnalysisError,
            onRequestAiAnalysis = {
                viewModel.requestAiAnalysisReport()
            }
        )

        ConsumptionPatternCard(
            timePatternList = uiState.timePatternList,
            weekdayAverageText = uiState.weekdayAverageText,
            weekendAverageText = uiState.weekendAverageText,
            weekendComment = uiState.weekendComment,
            paymentPatternList = uiState.paymentPatternList
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// 상단 제목 섹션
@Composable
fun AnalysisHeaderSection(
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "소비 패턴 분석",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )

        Text(
            text = "AI가 분석한 소비 습관을 확인해보세요.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onShareClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(SpentopiaActionGradientColors),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "공유",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(SpentopiaActionGradientColors),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "리포트 다운로드",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// 요약 카드 묶음
@Composable
fun SummaryCardSection(
    totalExpense: Int,
    averageDailyExpense: Int,
    budgetUsageRate: Float
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GradientSummaryCard(
            title = "이번 달 총 지출",
            valueText = "${formatWon(totalExpense)}원",
            subText = "지난 달 대비 -12%"
        )

        WhiteSummaryCard(
            title = "일 평균 지출",
            valueText = "${formatWon(averageDailyExpense)}원",
            subText = "약 -5% 절약중"
        )

        BudgetUsageCard(
            usageRate = budgetUsageRate
        )
    }
}

// 보라색 그라데이션 카드
@Composable
fun GradientSummaryCard(
    title: String,
    valueText: String,
    subText: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = SpentopiaGlowPurple.copy(alpha = 0.14f),
                spotColor = SpentopiaGlowPurple.copy(alpha = 0.18f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = SpentopiaActionGradientColors
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color = SpentopiaGlowPurple.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.92f)
                )

                Text(
                    text = valueText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

// 흰색 일반 카드
@Composable
fun WhiteSummaryCard(
    title: String,
    valueText: String,
    subText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = valueText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// 예산 사용률 카드
@Composable
fun BudgetUsageCard(
    usageRate: Float
) {
    val percentText = (usageRate * 100).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "예산 사용률",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${percentText}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { usageRate.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = SpentopiaMutedPurple,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

// 최대 소비 카테고리 카드
@Composable
fun TopCategoryCard(
    categoryName: String,
    ratio: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "최대 소비 카테고리",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🍔",
                    fontSize = 26.sp
                )

                Text(
                    text = categoryName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "전체의 ${(ratio * 100).roundToInt()}%",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 주간 / 월간 토글 전체
@Composable
fun PeriodToggleSection(
    selectedPeriod: String,
    onSelectPeriod: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PeriodToggleButton(
            text = "주간",
            isSelected = selectedPeriod == "주간",
            onClick = { onSelectPeriod("주간") },
            modifier = Modifier.weight(1f)
        )

        PeriodToggleButton(
            text = "월간",
            isSelected = selectedPeriod == "월간",
            onClick = { onSelectPeriod("월간") },
            modifier = Modifier.weight(1f)
        )
    }
}

// 개별 토글 버튼
@Composable
fun PeriodToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// 소비 추이 카드
@Composable
fun ExpenseTrendCard(
    title: String,
    expenseList: List<Pair<String, Int>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            SimpleBarChart(
                expenseList = expenseList
            )
        }
    }
}

// 간단한 막대 그래프
@Composable
fun SimpleBarChart(
    expenseList: List<Pair<String, Int>>
) {
    val maxAmount = (expenseList.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val yAxisSteps = listOf(0, 15000, 30000, 45000, 60000)
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .height(200.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yAxisSteps.reversed().forEach { value ->
                    Text(
                        text = value.toString(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    for (i in 0 until 5) {
                        val y = size.height * i / 4f
                        drawLine(
                            color = outlineVariantColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = dash,
                            strokeWidth = 1f
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    expenseList.forEach { item ->
                        BarChartItem(
                            label = item.first,
                            amount = item.second,
                            maxAmount = maxAmount
                        )
                    }
                }
            }
        }
    }
}

// 개별 막대
@Composable
fun BarChartItem(
    label: String,
    amount: Int,
    maxAmount: Int
) {
    val ratio = amount.toFloat() / maxAmount.toFloat()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(28.dp)
    ) {
        Box(
            modifier = Modifier
                .height((150 * ratio).dp)
                .width(16.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = SpentopiaWalletGradientColors
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 카테고리별 지출 카드
@Composable
fun CategoryPieChartCard(
    categoryList: List<CategorySpendUiModel>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "카테고리별 지출",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CategoryPieChart(
                    categoryList = categoryList
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categoryList.forEach { item ->
                        PieLegendItem(item = item)
                    }
                }
            }
        }
    }
}

// 도넛 차트
@Composable
fun CategoryPieChart(
    categoryList: List<CategorySpendUiModel>
) {
    val totalRatio = categoryList.sumOf { it.ratio.toDouble() }.toFloat().coerceAtLeast(1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        Canvas(
            modifier = Modifier.size(220.dp)
        ) {
            var startAngle = -90f

            categoryList.forEach { item ->
                val sweepAngle = (item.ratio / totalRatio) * 360f

                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 56f)
                )

                startAngle += sweepAngle
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "총 지출",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${formatWon(categoryList.sumOf { it.amount })}원",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// 도넛 차트 범례 1줄
@Composable
fun PieLegendItem(
    item: CategorySpendUiModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(item.color, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = item.name,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${(item.ratio * 100).roundToInt()}%",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 카테고리 상세 카드
@Composable
fun CategoryDetailCard(
    categoryList: List<CategorySpendUiModel>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "카테고리 상세",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            categoryList.forEachIndexed { index, item ->
                CategoryDetailItem(item = item)

                if (index != categoryList.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = Color.Transparent
                    )
                }
            }
        }
    }
}

// 카테고리 상세 1줄
@Composable
fun CategoryDetailItem(
    item: CategorySpendUiModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${formatWon(item.amount)}원",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { item.ratio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp),
                color = item.color,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${(item.ratio * 100).roundToInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 32.dp)
            )
        }
    }
}

// AI 리포트 섹션
@Composable
fun AiAnalysisReportSection(
    tipList: List<AnalysisTipUiModel>,
    totalExpense: Int,
    aiAnalysisText: String,
    isLoading: Boolean,
    errorMessage: String,
    onRequestAiAnalysis: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI 소비 분석 리포트",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onRequestAiAnalysis,
                enabled = !isLoading && totalExpense > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                if (isLoading || totalExpense > 0) {
                                    SpentopiaActionGradientColors
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoading) "분석 중" else "AI 분석",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        AiGeneratedReportCard(
            totalExpense = totalExpense,
            aiAnalysisText = aiAnalysisText,
            isLoading = isLoading,
            errorMessage = errorMessage
        )

        tipList.forEach { tip ->
            AnalysisTipCard(tip = tip)
        }
    }
}

@Composable
fun AiGeneratedReportCard(
    totalExpense: Int,
    aiAnalysisText: String,
    isLoading: Boolean,
    errorMessage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI 소비 코멘트",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val message = when {
                totalExpense <= 0 -> "아직 분석할 소비 데이터가 없습니다. Home 화면에서 소비 기록을 먼저 입력해주세요."
                isLoading -> "AI가 이번 기간의 소비 기록을 분석하고 있습니다."
                errorMessage.isNotBlank() -> errorMessage
                aiAnalysisText.isNotBlank() -> aiAnalysisText
                else -> "AI 분석 버튼을 누르면 이번 기간의 소비 데이터를 바탕으로 맞춤 리포트를 생성합니다."
            }

            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = if (errorMessage.isNotBlank()) {
                    Color(0xFFE53935)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

// AI 리포트 카드 1개
@Composable
fun AnalysisTipCard(
    tip: AnalysisTipUiModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = tip.borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tip.emoji,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = tip.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = tip.description,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 소비 패턴 분석 카드
@Composable
fun ConsumptionPatternCard(
    timePatternList: List<PatternProgressUiModel>,
    weekdayAverageText: String,
    weekendAverageText: String,
    weekendComment: String,
    paymentPatternList: List<PatternProgressUiModel>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "소비 패턴 분석",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "시간대별 소비",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                timePatternList.forEach { item ->
                    PatternProgressRow(item = item)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "요일별 소비",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                PatternCompareRow(
                    leftLabel = "평일",
                    leftValue = weekdayAverageText,
                    rightLabel = "주말",
                    rightValue = weekendAverageText
                )

                Text(
                    text = weekendComment,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "결제 방법",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                paymentPatternList.forEach { item ->
                    PatternProgressRow(item = item)
                }
            }
        }
    }
}

// 소비 패턴 진행률 1줄
@Composable
fun PatternProgressRow(
    item: PatternProgressUiModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )

        LinearProgressIndicator(
            progress = { item.ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = SpentopiaMutedPurple,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${(item.ratio * 100).roundToInt()}%",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 34.dp)
        )
    }
}

// 평일 / 주말 비교 행
@Composable
fun PatternCompareRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallCompareCard(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f)
        )

        SmallCompareCard(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f)
        )
    }
}

// 비교용 작은 카드
@Composable
fun SmallCompareCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// 공유 함수
fun shareAnalysisReport(
    context: Context,
    reportText: String
) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Spentopia 소비 패턴 분석 리포트")
        putExtra(Intent.EXTRA_TEXT, reportText)
    }

    val chooser = Intent.createChooser(sendIntent, "리포트 공유")
    context.startActivity(chooser)
}

// 다운로드 저장 함수
fun saveAnalysisReportToDownloads(
    context: Context,
    fileName: String,
    content: String
): Boolean {
    return try {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
            outputStream.flush()
        } ?: return false

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// 리포트 텍스트 생성
fun buildAnalysisReportText(
    totalExpense: Int,
    averageDailyExpense: Int,
    budgetUsageRate: Float,
    topCategoryName: String,
    topCategoryRatio: Float,
    selectedPeriod: String,
    trendExpenseList: List<Pair<String, Int>>,
    categoryList: List<CategorySpendUiModel>,
    tipList: List<AnalysisTipUiModel>,
    aiAnalysisText: String,
    timePatternList: List<PatternProgressUiModel>,
    weekdayAverageText: String,
    weekendAverageText: String,
    weekendComment: String,
    paymentPatternList: List<PatternProgressUiModel>
): String {
    val trendText = trendExpenseList.joinToString("\n") { (label, amount) ->
        "- $label: ${formatWon(amount)}원"
    }

    val categoryText = categoryList.joinToString("\n") { item ->
        "- ${item.name}: ${formatWon(item.amount)}원 (${(item.ratio * 100).roundToInt()}%)"
    }

    val tipText = tipList.joinToString("\n") { tip ->
        "- ${tip.title}: ${tip.description}"
    }

    val timePatternText = timePatternList.joinToString("\n") { item ->
        "- ${item.label}: ${(item.ratio * 100).roundToInt()}%"
    }

    val paymentPatternText = paymentPatternList.joinToString("\n") { item ->
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
fun formatWon(value: Int): String {
    return "%,d".format(value)
}
