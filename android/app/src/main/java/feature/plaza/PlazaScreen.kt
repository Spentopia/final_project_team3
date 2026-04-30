package com.example.spentopia.feature.plaza

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavy
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors

@Composable
fun PlazaScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            PlazaHeaderSection()
        }

        item {
            PlazaHeroCard(
                onEnterClick = {
                    Toast.makeText(
                        context,
                        "모바일에서는 광장 기능을 이용할 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        item {
            PlazaFeatureCard()
        }

        item {
            PlazaRequirementCard()
        }

        item {
            PlazaOnlineUsersCard()
        }

        item {
            PlazaTipsCard()
        }

        item {
            PlazaUpcomingCard()
        }

        item {
            MobileNoticeCard()
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun PlazaHeaderSection() {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "광장",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFFFB020),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PC 전용",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "아바타와 함께 다른 유저들을 만나보세요",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF475467)
        )
    }
}

@Composable
private fun PlazaHeroCard(
    onEnterClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = SpentopiaWalletGradientColors
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Games,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Unity WebGL 광장",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "PC 웹에서 Unity 기반의 3D 가상 공간을 체험하세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.95f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onEnterClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "광장 입장하기",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlazaFeatureCard() {
    SectionCard(title = "광장 기능") {
        FeatureRow(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = SpentopiaMutedPurple
                )
            },
            iconBg = Color(0xFFF3E8FF),
            title = "아바타 이동 & 채팅",
            description = "내 아바타를 움직이며 다른 유저들과 실시간 채팅을 즐겨보세요"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureRow(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = SpentopiaMutedPurple
                )
            },
            iconBg = Color(0xFFFCE7F3),
            title = "커스터마이징 반영",
            description = "내 아바타에 적용한 모든 아이템이 3D로 표현돼요"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureRow(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD97706)
                )
            },
            iconBg = Color(0xFFFEF3C7),
            title = "프리미엄 공간",
            description = "특별한 칭호와 전용 부스를 획득할 수 있어요"
        )
    }
}

@Composable
private fun PlazaRequirementCard() {
    SectionCard(title = "시스템 요구사항") {
        RequirementText("브라우저", "Chrome, Edge, Firefox (최신 버전)")
        Spacer(modifier = Modifier.height(12.dp))
        RequirementText("운영체제", "Windows 10 이상, macOS 10.15 이상")
        Spacer(modifier = Modifier.height(12.dp))
        RequirementText("메모리", "최소 4GB RAM (8GB 권장)")
        Spacer(modifier = Modifier.height(12.dp))
        RequirementText("그래픽", "WebGL 2.0 지원")
    }
}

@Composable
private fun PlazaOnlineUsersCard() {
    val users = listOf(
        Triple("절약왕", "광장 중앙", "💰"),
        Triple("패션왕", "프리미엄 존", "👗"),
        Triple("목표달성", "채팅 중", "🎯"),
        Triple("알뜰맨", "광장 입구", "🏃")
    )

    SectionCard(title = "현재 접속 중") {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF6FCF97), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "124명 접속 중",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        users.forEachIndexed { index, item ->
            OnlineUserRow(
                emoji = item.third,
                name = item.first,
                status = item.second
            )

            if (index != users.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PlazaTipsCard() {
    SectionCard(
        title = "광장 이용 팁",
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        TipText("WASD 키로 아바타를 움직일 수 있어요")
        Spacer(modifier = Modifier.height(10.dp))
        TipText("다른 유저 클릭 시 1:1 채팅이 가능해요")
        Spacer(modifier = Modifier.height(10.dp))
        TipText("특정 구역에서는 미니게임을 즐길 수 있어요")
        Spacer(modifier = Modifier.height(10.dp))
        TipText("성실도 점수가 높으면 특별한 공간이 열려요")
        Spacer(modifier = Modifier.height(10.dp))
        TipText("친구 추가 기능으로 함께 즐겨보세요")
    }
}

@Composable
private fun PlazaUpcomingCard() {
    Column {
        Text(
            text = "곧 추가될 기능",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        UpcomingItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = SpentopiaMutedPurple
                )
            },
            title = "미니게임",
            description = "다양한 미니게임으로 SPT 획득",
            borderColor = Color(0xFFD8B4FE),
            backgroundColor = Color(0xFFFAF5FF)
        )

        Spacer(modifier = Modifier.height(12.dp))

        UpcomingItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = SpentopiaMutedPurple
                )
            },
            title = "길드 시스템",
            description = "친구들과 길드를 만들어보세요",
            borderColor = Color(0xFFF9A8D4),
            backgroundColor = Color(0xFFFDF2F8)
        )

        Spacer(modifier = Modifier.height(12.dp))

        UpcomingItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = Color(0xFFD97706)
                )
            },
            title = "이벤트 홀",
            description = "특별 이벤트 전용 공간",
            borderColor = Color(0xFFFCD34D),
            backgroundColor = Color(0xFFFFFBEB)
        )
    }
}

@Composable
private fun MobileNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFF6FF)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "💡",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "모바일에서는 광장 기능을 이용할 수 없습니다.\nPC 환경에서 이용해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1D4ED8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    containerColor: Color = Color(0xFFF8FAFC),
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            content()
        }
    }
}

@Composable
private fun FeatureRow(
    icon: @Composable () -> Unit,
    iconBg: Color,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RequirementText(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnlineUserRow(
    emoji: String,
    name: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TipText(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun UpcomingItem(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    borderColor: Color,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
