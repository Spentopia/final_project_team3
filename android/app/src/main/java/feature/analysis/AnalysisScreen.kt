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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.roundToInt

// 소비분석 메인 화면
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
        timePatternList = uiState.timePatternList,
        weekdayAverageText = uiState.weekdayAverageText,
        weekendAverageText = uiState.weekendAverageText,
        weekendComment = uiState.weekendComment,
        paymentPatternList = uiState.paymentPatternList
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FAFD))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        AiAnalysisReportSection(
            tipList = uiState.tipList
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
            color = Color(0xFF0F172A),
            lineHeight = 34.sp
        )

        Text(
            text = "AI가 분석한 소비 습관을 확인해보세요.",
            fontSize = 15.sp,
            color = Color(0xFF475569),
            lineHeight = 22.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onShareClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF111827)
                ),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "공유",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD946EF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "리포트 다운로드",
                    fontWeight = FontWeight.SemiBold
                )
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFFD946EF)
                        )
                    )
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )

            Text(
                text = valueText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
            )

            Text(
                text = subText,
                fontSize = 12.sp,
                color = Color(0xFF16A34A)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "예산 사용률",
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )

            Text(
                text = "${percentText}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
            )

            LinearProgressIndicator(
                progress = { usageRate.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFFD946EF),
                trackColor = Color(0xFFE5E7EB)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "최대 소비 카테고리",
                fontSize = 13.sp,
                color = Color(0xFF475569)
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
                    color = Color(0xFF111827)
                )
            }

            Text(
                text = "전체의 ${(ratio * 100).roundToInt()}%",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
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
                color = Color(0xFFE5E7EB),
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
            containerColor = if (isSelected) Color.White else Color.Transparent,
            contentColor = Color(0xFF111827)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
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
                        color = Color(0xFF6B7280)
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
                            color = Color(0xFFE5E7EB),
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
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFFD946EF)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF475569)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "카테고리별 지출",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
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
                color = Color(0xFF64748B)
            )

            Text(
                text = "${formatWon(categoryList.sumOf { it.amount })}원",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
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
            color = Color(0xFF111827),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${(item.ratio * 100).roundToInt()}%",
            fontSize = 13.sp,
            color = Color(0xFF64748B)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "카테고리 상세",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
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
                color = Color(0xFF111827),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${formatWon(item.amount)}원",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
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
                trackColor = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${(item.ratio * 100).roundToInt()}%",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.widthIn(min = 32.dp)
            )
        }
    }
}

// AI 리포트 섹션
@Composable
fun AiAnalysisReportSection(
    tipList: List<AnalysisTipUiModel>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "AI 소비 분석 리포트",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF111827)
        )

        tipList.forEach { tip ->
            AnalysisTipCard(tip = tip)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
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
                    color = Color(0xFF111827)
                )
            }

            Text(
                text = tip.description,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color(0xFF475569)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "소비 패턴 분석",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "시간대별 소비",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
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
                    color = Color(0xFF111827)
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
                    color = Color(0xFF64748B)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "결제 방법",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
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
            color = Color(0xFF475569),
            modifier = Modifier.width(110.dp)
        )

        LinearProgressIndicator(
            progress = { item.ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = Color(0xFFA855F7),
            trackColor = Color(0xFFE5E7EB)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${(item.ratio * 100).roundToInt()}%",
            fontSize = 13.sp,
            color = Color(0xFF111827),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
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