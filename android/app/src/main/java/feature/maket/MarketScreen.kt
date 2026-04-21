package com.ict.spentopia.feature.market

import androidx.compose.foundation.background // 수정: 배경 표현에 사용
import androidx.compose.foundation.layout.Arrangement // 수정: 정렬과 간격 지정에 사용
import androidx.compose.foundation.layout.Box // 수정: 강조 박스와 배지 표현에 사용
import androidx.compose.foundation.layout.Column // 수정: 세로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.ExperimentalLayoutApi // 수정: FlowRow 사용에 필요합니다.
import androidx.compose.foundation.layout.FlowRow // 수정: 카드 줄바꿈 배치에 사용
import androidx.compose.foundation.layout.Row // 수정: 가로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.Spacer // 수정: 여백 추가에 사용
import androidx.compose.foundation.layout.fillMaxWidth // 수정: 가로 전체 사용에 사용
import androidx.compose.foundation.layout.height // 수정: 높이 여백 지정에 사용
import androidx.compose.foundation.layout.padding // 수정: 내부 여백 적용에 사용
import androidx.compose.foundation.rememberScrollState // 수정: 세로 스크롤 상태 기억에 사용
import androidx.compose.foundation.shape.RoundedCornerShape // 수정: 둥근 카드 모양에 사용
import androidx.compose.foundation.verticalScroll // 수정: 전체 화면 세로 스크롤에 사용
import androidx.compose.material3.Button // 수정: 액션 버튼에 사용
import androidx.compose.material3.ButtonDefaults // 수정: 버튼 색상 지정에 사용
import androidx.compose.material3.Card // 수정: 카드 UI 구성에 사용
import androidx.compose.material3.CardDefaults // 수정: 카드 스타일 지정에 사용
import androidx.compose.material3.Text // 수정: 텍스트 출력에 사용
import androidx.compose.material3.TextButton // 수정: 탭 버튼에 사용
import androidx.compose.runtime.Composable // 기존 유지
import androidx.compose.ui.Alignment // 수정: 내부 정렬에 사용
import androidx.compose.ui.Modifier // 기존 유지
import androidx.compose.ui.graphics.Brush // 수정: 그라데이션 카드 배경에 사용
import androidx.compose.ui.graphics.Color // 수정: 색상 지정에 사용
import androidx.compose.ui.text.font.FontWeight // 수정: 제목 강조에 사용
import androidx.compose.ui.unit.dp // 기존 유지
import androidx.compose.ui.unit.sp // 수정: 폰트 크기 지정에 사용
import androidx.lifecycle.viewmodel.compose.viewModel // 수정: MarketViewModel을 화면에서 주입받기 위해 사용

// 기존 주석 유지
// NFT 마켓 화면
@OptIn(ExperimentalLayoutApi::class) // 수정: FlowRow 사용을 위해 OptIn을 적용합니다.
@Composable
fun MarketScreen(
    marketViewModel: MarketViewModel = viewModel() // 수정: 화면에서 사용할 MarketViewModel을 주입받습니다.
) {
    val uiState = marketViewModel.uiState // 수정: ViewModel의 현재 UI 상태를 읽어옵니다.

    Column(
        modifier = Modifier
            .fillMaxWidth() // 수정: 화면 전체 너비를 사용합니다.
            .verticalScroll(rememberScrollState()) // 수정: 마켓 화면 전체를 세로 스크롤 가능하게 설정합니다.
            .padding(vertical = 8.dp) // 수정: 상하 기본 여백을 적용합니다.
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), // 수정: 제목 영역과 버튼 영역을 한 줄에 배치합니다.
            horizontalArrangement = Arrangement.SpaceBetween, // 수정: 양 끝 정렬을 적용합니다.
            verticalAlignment = Alignment.Top // 수정: 상단 기준 정렬을 적용합니다.
        ) {
            Column(
                modifier = Modifier.weight(1f) // 수정: 제목 영역이 남은 공간을 차지하도록 설정합니다.
            ) {
                Text(
                    text = "NFT 마켓플레이스", // 수정: 화면 제목을 표시합니다.
                    fontSize = 28.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF11243D) // 수정: 제목 색상을 지정합니다.
                )

                Spacer(modifier = Modifier.height(4.dp)) // 수정: 제목과 설명 사이 여백을 추가합니다.

                Text(
                    text = "아바타 아이템을 자유롭게 거래해보세요", // 수정: 화면 설명 문구를 표시합니다.
                    fontSize = 15.sp, // 수정: 설명 텍스트 크기를 지정합니다.
                    color = Color(0xFF5C6B80) // 수정: 보조 텍스트 색상을 지정합니다.
                )
            }

            Spacer(modifier = Modifier.padding(horizontal = 6.dp)) // 수정: 제목 영역과 버튼 사이 간격을 추가합니다.

            Button(
                onClick = { }, // 수정: 현재는 더미 버튼 동작으로 비워둡니다.
                shape = RoundedCornerShape(12.dp), // 수정: 둥근 버튼 모양을 적용합니다.
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA855F7), // 수정: 보라색 계열 버튼 배경을 적용합니다.
                    contentColor = Color.White // 수정: 흰색 버튼 텍스트를 적용합니다.
                )
            ) {
                Text(
                    text = "👛 지갑 연결하기", // 수정: 지갑 연결 버튼 텍스트를 표시합니다.
                    fontSize = 13.sp, // 수정: 버튼 텍스트 크기를 지정합니다.
                    fontWeight = FontWeight.Bold // 수정: 버튼 텍스트를 강조합니다.
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 상단 영역과 경고 배너 사이 여백을 추가합니다.

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 경고 배너가 가로 전체를 사용하도록 설정합니다.
            shape = RoundedCornerShape(16.dp), // 수정: 둥근 카드 모양을 적용합니다.
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)) // 수정: 연한 주황 배경을 적용합니다.
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
                verticalArrangement = Arrangement.spacedBy(6.dp) // 수정: 내부 항목 간격을 지정합니다.
            ) {
                Text(
                    text = "⚠️ 지갑 연결이 필요해요", // 수정: 배너 제목을 표시합니다.
                    fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFFEA580C) // 수정: 주황색 강조 텍스트를 적용합니다.
                )

                Text(
                    text = "지갑을 연결하면 아이템을 NFT로 발행하고 거래할 수 있어요. 지갑이 없어도 개인 소장은 가능합니다.", // 수정: 안내 문구를 표시합니다.
                    fontSize = 14.sp, // 수정: 설명 텍스트 크기를 지정합니다.
                    color = Color(0xFF9A3412) // 수정: 설명 텍스트 색상을 지정합니다.
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 배너와 통계 카드 사이 여백을 추가합니다.

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 카드 사이 가로 간격을 지정합니다.
            verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 카드 사이 세로 간격을 지정합니다.
        ) {
            MarketStatCard(
                title = "총 거래량", // 수정: 첫 번째 통계 제목을 표시합니다.
                value = "12,345", // 수정: 첫 번째 통계 수치를 표시합니다.
                subText = "+15% 이번 주", // 수정: 첫 번째 통계 부가 설명을 표시합니다.
                bgColor = Color.White, // 수정: 기본 흰색 카드 배경을 적용합니다.
                subTextColor = Color(0xFF16A34A) // 수정: 상승 텍스트 색상을 적용합니다.
            )

            MarketStatCard(
                title = "등록된 아이템", // 수정: 두 번째 통계 제목을 표시합니다.
                value = "248", // 수정: 두 번째 통계 수치를 표시합니다.
                subText = "현재 판매중", // 수정: 두 번째 통계 부가 설명을 표시합니다.
                bgColor = Color.White, // 수정: 기본 흰색 카드 배경을 적용합니다.
                subTextColor = Color(0xFF6B7280) // 수정: 기본 회색 설명 색상을 적용합니다.
            )

            MarketStatCard(
                title = "평균 거래가", // 수정: 세 번째 통계 제목을 표시합니다.
                value = "650 SPT", // 수정: 세 번째 통계 수치를 표시합니다.
                subText = "+8% 상승", // 수정: 세 번째 통계 부가 설명을 표시합니다.
                bgColor = Color.White, // 수정: 기본 흰색 카드 배경을 적용합니다.
                subTextColor = Color(0xFF16A34A) // 수정: 상승 텍스트 색상을 적용합니다.
            )

            MarketStatCard(
                title = "내 잔액", // 수정: 네 번째 통계 제목을 표시합니다.
                value = "1,250 SPT", // 수정: 네 번째 통계 수치를 표시합니다.
                subText = "사용 가능", // 수정: 네 번째 통계 부가 설명을 표시합니다.
                bgColor = Color.Transparent, // 수정: 내부 그라데이션을 사용하기 위해 투명 카드로 처리합니다.
                subTextColor = Color.White, // 수정: 흰색 설명 텍스트를 적용합니다.
                gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA855F7), // 수정: 시작 보라색을 적용합니다.
                        Color(0xFFEC4899) // 수정: 끝 핑크색을 적용합니다.
                    )
                ) // 수정: 잔액 카드를 그라데이션으로 강조합니다.
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 통계 카드와 상단 탭 사이 여백을 추가합니다.

        Row(
            modifier = Modifier
                .background(
                    color = Color(0xFFF1EFEE), // 수정: 연한 회색 탭 배경을 적용합니다.
                    shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 탭 배경을 적용합니다.
                )
                .padding(4.dp), // 수정: 탭 내부 여백을 적용합니다.
            horizontalArrangement = Arrangement.spacedBy(4.dp) // 수정: 탭 간 간격을 지정합니다.
        ) {
            MarketTabButton(
                text = "마켓", // 수정: 첫 번째 탭 텍스트를 표시합니다.
                selected = uiState.selectedTab == MarketTab.MARKET, // 수정: ViewModel의 현재 마켓 탭 선택 상태를 반영합니다.
                onClick = { marketViewModel.onTabChange(MarketTab.MARKET) } // 수정: 클릭 시 ViewModel을 통해 마켓 탭으로 전환합니다.
            )

            MarketTabButton(
                text = "내 판매 목록", // 수정: 두 번째 탭 텍스트를 표시합니다.
                selected = uiState.selectedTab == MarketTab.MY_SELL, // 수정: ViewModel의 현재 내 판매 목록 탭 상태를 반영합니다.
                onClick = { marketViewModel.onTabChange(MarketTab.MY_SELL) } // 수정: 클릭 시 ViewModel을 통해 내 판매 목록 탭으로 전환합니다.
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 탭과 검색 영역 사이 여백을 추가합니다.

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp), // 수정: 검색/필터 카드 사이 가로 간격을 지정합니다.
            verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 검색/필터 카드 사이 세로 간격을 지정합니다.
        ) {
            FilterBox(
                text = if (uiState.searchText.isBlank()) "🔍 아이템 검색..." else "🔍 ${uiState.searchText}", // 수정: 검색어 상태를 화면에 반영합니다.
                modifier = Modifier.fillMaxWidth(1f) // 수정: 검색창은 한 줄 전체를 먼저 사용하도록 설정합니다.
            )

            FilterBox(
                text = "전체 희귀도 ⌄", // 수정: 희귀도 필터 텍스트를 표시합니다.
                modifier = Modifier.fillMaxWidth(0.47f) // 수정: 필터 박스 너비를 설정합니다.
            )

            FilterBox(
                text = "최신순 ⌄", // 수정: 정렬 필터 텍스트를 표시합니다.
                modifier = Modifier.fillMaxWidth(0.47f) // 수정: 필터 박스 너비를 설정합니다.
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 검색/필터와 아이템 리스트 사이 여백을 추가합니다.

        val itemsToShow = if (uiState.selectedTab == MarketTab.MARKET) uiState.marketItems else uiState.mySellItems // 수정: 현재 선택된 탭에 따라 ViewModel의 아이템 목록을 분기합니다.

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp), // 수정: 카드 사이 가로 간격을 지정합니다.
            verticalArrangement = Arrangement.spacedBy(14.dp) // 수정: 카드 사이 세로 간격을 지정합니다.
        ) {
            itemsToShow.forEach { item ->
                MarketItemCard(item = item) // 수정: 각 아이템 카드를 출력합니다.
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 수정: 화면 하단 여백을 추가합니다.
    }
}

// 수정: 상단 탭 버튼 UI를 구성합니다.
@Composable
private fun MarketTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = { onClick() }, // 수정: 탭 클릭 시 전달받은 동작을 실행합니다.
        modifier = Modifier
            .background(
                color = if (selected) Color.White else Color.Transparent, // 수정: 선택된 탭만 흰색 배경을 적용합니다.
                shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 버튼 모양을 적용합니다.
            )
    ) {
        Text(
            text = text, // 수정: 탭 텍스트를 표시합니다.
            color = Color(0xFF111827), // 수정: 탭 텍스트 색상을 지정합니다.
            fontSize = 13.sp, // 수정: 탭 텍스트 크기를 지정합니다.
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium // 수정: 선택된 탭을 굵게 표시합니다.
        )
    }
}

// 수정: 마켓 통계 카드를 구성합니다.
@Composable
private fun MarketStatCard(
    title: String,
    value: String,
    subText: String,
    bgColor: Color,
    subTextColor: Color,
    gradient: Brush? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f), // 수정: 두 칸 배치 느낌으로 카드 너비를 설정합니다.
        shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = bgColor) // 수정: 카드 배경색을 적용합니다.
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = gradient ?: Brush.linearGradient(listOf(bgColor, bgColor)), // 수정: 그라데이션이 있으면 적용하고 없으면 단색 배경을 유지합니다.
                    shape = RoundedCornerShape(18.dp) // 수정: 배경에도 둥근 모서리를 적용합니다.
                )
                .padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
            verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 내부 요소 간격을 지정합니다.
        ) {
            Text(
                text = title, // 수정: 통계 제목을 표시합니다.
                fontSize = 15.sp, // 수정: 제목 크기를 지정합니다.
                color = if (gradient != null) Color.White.copy(alpha = 0.9f) else Color(0xFF6B7280), // 수정: 강조 카드 여부에 따라 제목 색상을 다르게 적용합니다.
                fontWeight = FontWeight.Medium // 수정: 제목을 약간 강조합니다.
            )

            Text(
                text = value, // 수정: 통계 수치를 표시합니다.
                fontSize = 24.sp, // 수정: 수치 크기를 크게 지정합니다.
                color = if (gradient != null) Color.White else Color(0xFF111827), // 수정: 강조 카드 여부에 따라 수치 색상을 다르게 적용합니다.
                fontWeight = FontWeight.Bold // 수정: 수치를 강조합니다.
            )

            Text(
                text = subText, // 수정: 통계 부가 설명을 표시합니다.
                fontSize = 14.sp, // 수정: 설명 텍스트 크기를 지정합니다.
                color = subTextColor, // 수정: 전달받은 설명 색상을 적용합니다.
                fontWeight = FontWeight.SemiBold // 수정: 설명을 약간 강조합니다.
            )
        }
    }
}

// 수정: 검색/필터 박스를 구성합니다.
@Composable
private fun FilterBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFFF5F7FA), // 수정: 연한 회색 배경을 적용합니다.
                shape = RoundedCornerShape(12.dp) // 수정: 둥근 박스 모양을 적용합니다.
            )
            .padding(horizontal = 14.dp, vertical = 14.dp) // 수정: 박스 내부 여백을 적용합니다.
    ) {
        Text(
            text = text, // 수정: 검색/필터 텍스트를 표시합니다.
            fontSize = 14.sp, // 수정: 텍스트 크기를 지정합니다.
            color = Color(0xFF6B7280) // 수정: 보조 텍스트 색상을 적용합니다.
        )
    }
}

// 수정: 개별 마켓 아이템 카드를 구성합니다.
@Composable
private fun MarketItemCard(item: MarketItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f), // 수정: 두 칸 배치 느낌으로 카드 너비를 설정합니다.
        shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = Color.White), // 수정: 흰색 카드 배경을 적용합니다.
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 수정: 살짝 떠 있는 느낌을 적용합니다.
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth() // 수정: 상단 이미지 영역이 가로 전체를 사용하도록 설정합니다.
                    .height(160.dp) // 수정: 이미지 영역 높이를 지정합니다.
                    .background(
                        color = Color(0xFFF6EAF8), // 수정: 연한 보라색 이미지 배경을 적용합니다.
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp) // 수정: 상단만 둥근 모서리를 적용합니다.
                    ),
                contentAlignment = Alignment.Center // 수정: 이모지를 중앙 정렬합니다.
            ) {
                Text(
                    text = item.emoji, // 수정: 아이템 대표 이모지를 표시합니다.
                    fontSize = 60.sp // 수정: 이모지 크기를 크게 지정합니다.
                )
            }

            Column(
                modifier = Modifier.padding(16.dp), // 수정: 카드 하단 정보 영역 내부 여백을 적용합니다.
                verticalArrangement = Arrangement.spacedBy(8.dp) // 수정: 내부 요소 간격을 지정합니다.
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), // 수정: 제목과 희귀도 배지를 양 끝 배치합니다.
                    horizontalArrangement = Arrangement.SpaceBetween, // 수정: 양 끝 정렬을 적용합니다.
                    verticalAlignment = Alignment.CenterVertically // 수정: 세로 중앙 정렬을 적용합니다.
                ) {
                    Text(
                        text = item.title, // 수정: 아이템 이름을 표시합니다.
                        fontSize = 20.sp, // 수정: 제목 크기를 지정합니다.
                        fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                        color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = marketRarityColor(item.rarity), // 수정: 희귀도별 배경색을 적용합니다.
                                shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 배지 모양을 적용합니다.
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp) // 수정: 배지 내부 여백을 적용합니다.
                    ) {
                        Text(
                            text = item.rarity, // 수정: 희귀도 텍스트를 표시합니다.
                            color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
                            fontSize = 12.sp, // 수정: 희귀도 텍스트 크기를 지정합니다.
                            fontWeight = FontWeight.Bold // 수정: 희귀도 텍스트를 강조합니다.
                        )
                    }
                }

                Text(
                    text = item.seller, // 수정: 판매자 정보를 표시합니다.
                    fontSize = 14.sp, // 수정: 판매자 텍스트 크기를 지정합니다.
                    color = Color(0xFF6B7280) // 수정: 보조 텍스트 색상을 적용합니다.
                )

                Row(
                    modifier = Modifier.fillMaxWidth(), // 수정: 가격과 시간 정보를 양 끝 배치합니다.
                    horizontalArrangement = Arrangement.SpaceBetween, // 수정: 양 끝 정렬을 적용합니다.
                    verticalAlignment = Alignment.CenterVertically // 수정: 세로 중앙 정렬을 적용합니다.
                ) {
                    Text(
                        text = "🪙 ${item.price}", // 수정: 가격 텍스트를 표시합니다.
                        fontSize = 22.sp, // 수정: 가격 텍스트 크기를 지정합니다.
                        fontWeight = FontWeight.Bold, // 수정: 가격을 강조합니다.
                        color = Color(0xFF111827) // 수정: 가격 색상을 지정합니다.
                    )

                    Text(
                        text = item.time, // 수정: 등록 시간을 표시합니다.
                        fontSize = 13.sp, // 수정: 시간 텍스트 크기를 지정합니다.
                        color = Color(0xFF6B7280) // 수정: 보조 텍스트 색상을 적용합니다.
                    )
                }

                Button(
                    onClick = { }, // 수정: 현재는 더미 버튼 동작으로 비워둡니다.
                    modifier = Modifier.fillMaxWidth(), // 수정: 구매 버튼이 가로 전체를 사용하도록 설정합니다.
                    shape = RoundedCornerShape(12.dp), // 수정: 둥근 버튼 모양을 적용합니다.
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC4899), // 수정: 핑크색 구매 버튼 배경을 적용합니다.
                        contentColor = Color.White // 수정: 흰색 버튼 텍스트를 적용합니다.
                    )
                ) {
                    Text(
                        text = "구매하기", // 수정: 구매 버튼 텍스트를 표시합니다.
                        fontSize = 15.sp, // 수정: 버튼 텍스트 크기를 지정합니다.
                        fontWeight = FontWeight.Bold // 수정: 버튼 텍스트를 강조합니다.
                    )
                }
            }
        }
    }
}

// 수정: 희귀도에 따른 색상을 반환합니다.
private fun marketRarityColor(rarity: String): Color {
    return when (rarity) {
        "일반" -> Color(0xFF6B7280) // 수정: 일반 등급 색상을 반환합니다.
        "레어" -> Color(0xFF3B82F6) // 수정: 레어 등급 색상을 반환합니다.
        "에픽" -> Color(0xFFA855F7) // 수정: 에픽 등급 색상을 반환합니다.
        "전설" -> Color(0xFFF59E0B) // 수정: 전설 등급 색상을 반환합니다.
        else -> Color(0xFF6B7280) // 수정: 기본값으로 일반 색상을 반환합니다.
    }
}