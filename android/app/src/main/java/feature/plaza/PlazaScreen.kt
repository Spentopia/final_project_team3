package com.example.spentopia.feature.plaza // 이 파일이 속한 패키지 위치를 적음

// 광장 화면임
// 이벤트/길드/미니게임 허브

import android.widget.Toast // 짧은 알림 메시지 기능을 가져옴
import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.BoxScope // BoxScope 기능을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.PaddingValues // PaddingValues 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.layout.width // width 기능을 가져옴
import androidx.compose.foundation.lazy.LazyColumn // 세로 스크롤 목록을 가져옴
import androidx.compose.foundation.shape.CircleShape // CircleShape 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.outlined.Campaign // Campaign 기능을 가져옴
import androidx.compose.material.icons.outlined.ChatBubbleOutline // ChatBubbleOutline 기능을 가져옴
import androidx.compose.material.icons.outlined.EmojiEvents // EmojiEvents 기능을 가져옴
import androidx.compose.material.icons.outlined.Extension // Extension 기능을 가져옴
import androidx.compose.material.icons.outlined.Games // Games 기능을 가져옴
import androidx.compose.material.icons.outlined.Groups // Groups 기능을 가져옴
import androidx.compose.material.icons.outlined.Person // Person 기능을 가져옴
import androidx.compose.material.icons.outlined.PlayArrow // PlayArrow 기능을 가져옴
import androidx.compose.material.icons.outlined.Star // Star 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Surface // Surface 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.geometry.Offset // 그라데이션 위치 값을 가져옴
import androidx.compose.ui.graphics.Brush // 그라데이션 색칠 도구를 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground // 앱 다크모드 배경색을 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun PlazaScreen( // PlazaScreen 함수를 선언함
    modifier: Modifier = Modifier // modifier 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val context = LocalContext.current // 현재 화면 정보를 저장함

    LazyColumn( // 안쪽 UI를 세로로 배치함
        modifier = modifier // modifier 값을 modifier 값에 넣음
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp), // .padding(horizontal 값을 정해줌
        verticalArrangement = Arrangement.spacedBy(16.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        item { Spacer(modifier = Modifier.height(8.dp)) } // UI 크기나 여백 같은 모양을 정함

        item { // 이 블록 안의 내용이 시작됨
            PlazaHeaderSection() // Plaza Header Section 함수를 실행함
        }

        item { // 이 블록 안의 내용이 시작됨
            PlazaHeroCard( // 내용을 카드 모양으로 묶어서 보여줌
                onEnterClick = { // onEnterClick 때 실행할 함수를 정해줌
                    Toast.makeText( // 화면에 글자를 보여줌
                        context,
                        "모바일에서는 광장 기능을 이용할 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        item { // 이 블록 안의 내용이 시작됨
            PlazaFeatureCard() // 내용을 카드 모양으로 묶어서 보여줌
        }

        item { // 이 블록 안의 내용이 시작됨
            PlazaRequirementCard() // 내용을 카드 모양으로 묶어서 보여줌
        }

        item { // 이 블록 안의 내용이 시작됨
            PlazaOnlineUsersCard() // 내용을 카드 모양으로 묶어서 보여줌
        }

        item { // 이 블록 안의 내용이 시작됨
            PlazaTipsCard() // 내용을 카드 모양으로 묶어서 보여줌
        }

        item { // 이 블록 안의 내용이 시작됨
            PlazaUpcomingCard() // 내용을 카드 모양으로 묶어서 보여줌
        }

        item { // 이 블록 안의 내용이 시작됨
            MobileNoticeCard() // 내용을 카드 모양으로 묶어서 보여줌
        }

        item { Spacer(modifier = Modifier.height(24.dp)) } // UI 크기나 여백 같은 모양을 정함
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PlazaHeaderSection() { // PlazaHeaderSection 함수를 선언함
    Column { // 안쪽 UI를 세로로 배치함
        Row( // 안쪽 UI를 가로로 배치함
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "광장", // text 값을 정해줌
                style = MaterialTheme.typography.headlineSmall, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer, // color 값을 정해줌
                        shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp) // .padding(horizontal 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "PC 전용", // text 값을 정해줌
                    style = MaterialTheme.typography.labelSmall, // style 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onPrimaryContainer // color 값을 정해줌
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = "아바타와 함께 다른 유저들을 만나보세요", // text 값을 정해줌
            style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PlazaHeroCard( // PlazaHeroCard 함수를 선언함
    onEnterClick: () -> Unit // onEnterClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isPlazaDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val heroBackgroundBrush = if (isDark) { // hero 배경을 모드별로 분리함
        Brush.linearGradient(
            colors = listOf(Color(0xFF070A18), Color(0xFF111827), Color(0xFF211A45)),
            start = Offset.Zero,
            end = Offset(800f, 800f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FBFF), Color(0xFFE0F2FE)),
            start = Offset.Zero,
            end = Offset(800f, 800f)
        )
    }
    val heroBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE) // hero 테두리색을 모드별로 분리함
    val heroContentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A) // heroContentColor 값을 저장함
    val heroMutedContentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF53657D) // heroMutedContentColor 값을 저장함
    val pointColor = if (isDark) Color(0xFF6D5BD0) else Color(0xFF3B82F6) // 버튼/아이콘 포인트 색을 모드별로 분리함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = Color.Transparent // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, heroBorderColor), // border 값을 정해줌
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 1.dp else 2.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .background(heroBackgroundBrush)
                .padding(horizontal = 20.dp, vertical = 28.dp) // .padding(horizontal 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            if (isDark) {
                PlazaDarkDecorations()
            } else {
                PlazaLightDecorations()
            }
            Column( // 안쪽 UI를 세로로 배치함
                horizontalAlignment = Alignment.CenterHorizontally, // horizontalAlignment 값을 정해줌
                modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Surface( // Surface 함수를 실행함
                    shape = CircleShape, // CircleShape 값을 shape 값에 넣음
                    color = pointColor // iconSurfaceColor 값을 color 값에 넣음
                ) { // 이 블록 안의 내용이 시작됨
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .size(72.dp),
                        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Icon( // 화면에 아이콘을 보여줌
                            imageVector = Icons.Outlined.Games, // imageVector 값을 정해줌
                            contentDescription = null, // null 값을 contentDescription 값에 넣음
                            tint = Color.White, // iconContentColor 값을 tint 값에 넣음
                            modifier = Modifier.size(36.dp) // UI 크기나 여백 같은 모양을 정함
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = "Unity WebGL 광장", // text 값을 정해줌
                    style = MaterialTheme.typography.headlineSmall, // style 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = heroContentColor // heroContentColor 값을 color 값에 넣음
                )

                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = "PC 웹에서 Unity 기반의 3D 가상 공간을 체험하세요", // text 값을 정해줌
                    style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
                    color = heroMutedContentColor // heroMutedContentColor 값을 color 값에 넣음
                )

                Spacer(modifier = Modifier.height(24.dp)) // UI 크기나 여백 같은 모양을 정함

                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onEnterClick, // onEnterClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = pointColor, // containerColor 값을 정해줌
                        contentColor = Color.White // contentColor 값을 정해줌
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp), // contentPadding 값을 정해줌
                    modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
                ) { // 이 블록 안의 내용이 시작됨
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) { // 안쪽 UI를 가로로 배치함
                        Icon( // 화면에 아이콘을 보여줌
                            imageVector = Icons.Outlined.PlayArrow, // imageVector 값을 정해줌
                            contentDescription = null // null 값을 contentDescription 값에 넣음
                        )
                        Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함
                        Text( // 화면에 글자를 보여줌
                            text = "광장 입장하기", // text 값을 정해줌
                            fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PlazaLightDecorations() {
    PlazaGlowOrb(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(112.dp),
        colors = listOf(
            Color(0xFF93C5FD).copy(alpha = 0.38f),
            Color(0xFFDBEAFE).copy(alpha = 0.18f),
            Color.Transparent
        )
    )
    PlazaGlowOrb(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .size(92.dp),
        colors = listOf(
            Color(0xFFBAE6FD).copy(alpha = 0.44f),
            Color(0xFFE0F2FE).copy(alpha = 0.2f),
            Color.Transparent
        )
    )
    PlazaGlowOrb(
        modifier = Modifier
            .align(Alignment.TopStart)
            .size(74.dp),
        colors = listOf(
            Color.White.copy(alpha = 0.58f),
            Color(0xFFDBEAFE).copy(alpha = 0.18f),
            Color.Transparent
        )
    )
    PlazaSparkle(
        modifier = Modifier
            .align(Alignment.CenterEnd),
        color = Color(0xFF60A5FA).copy(alpha = 0.32f)
    )
}

@Composable
private fun BoxScope.PlazaDarkDecorations() {
    PlazaGlowOrb(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(118.dp),
        colors = listOf(
            Color(0xFF6D5BD0).copy(alpha = 0.34f),
            Color(0xFF312E81).copy(alpha = 0.18f),
            Color.Transparent
        )
    )
    PlazaGlowOrb(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .size(96.dp),
        colors = listOf(
            Color(0xFF22D3EE).copy(alpha = 0.24f),
            Color(0xFF1E1B4B).copy(alpha = 0.16f),
            Color.Transparent
        )
    )
    PlazaSparkle(
        modifier = Modifier
            .align(Alignment.TopStart),
        color = Color(0xFFE0E7FF).copy(alpha = 0.64f)
    )
    PlazaSparkle(
        modifier = Modifier
            .align(Alignment.CenterEnd),
        color = Color(0xFFC4B5FD).copy(alpha = 0.55f)
    )
}

@Composable
private fun PlazaGlowOrb(
    modifier: Modifier = Modifier,
    colors: List<Color>
) {
    Box(
        modifier = modifier.background(
            brush = Brush.radialGradient(colors = colors),
            shape = CircleShape
        )
    )
}

@Composable
private fun PlazaSparkle(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(modifier = modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(color, RoundedCornerShape(999.dp))
        )
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(4.dp)
                .background(color.copy(alpha = 0.82f), RoundedCornerShape(999.dp))
        )
    }
}


@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PlazaFeatureCard() { // PlazaFeatureCard 함수를 선언함
    SectionCard(title = "광장 기능") { // 내용을 카드 모양으로 묶어서 보여줌
        FeatureRow( // 안쪽 UI를 가로로 배치함
            icon = { // icon 값을 정해줌
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Outlined.Groups, // imageVector 값을 정해줌
                    contentDescription = null, // null 값을 contentDescription 값에 넣음
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                )
            },
            iconBg = MaterialTheme.colorScheme.surfaceVariant, // iconBg 값을 정해줌
            title = "아바타 이동 & 채팅", // 제목을 정해줌
            description = "내 아바타를 움직이며 다른 유저들과 실시간 채팅을 즐겨보세요" // description 값을 정해줌
        )

        Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

        FeatureRow( // 안쪽 UI를 가로로 배치함
            icon = { // icon 값을 정해줌
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Outlined.Extension, // imageVector 값을 정해줌
                    contentDescription = null, // null 값을 contentDescription 값에 넣음
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                )
            },
            iconBg = MaterialTheme.colorScheme.surfaceVariant, // iconBg 값을 정해줌
            title = "커스터마이징 반영", // 제목을 정해줌
            description = "내 아바타에 적용한 모든 아이템이 3D로 표현돼요" // description 값을 정해줌
        )

        Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

        FeatureRow( // 안쪽 UI를 가로로 배치함
            icon = { // icon 값을 정해줌
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Outlined.EmojiEvents, // imageVector 값을 정해줌
                    contentDescription = null, // null 값을 contentDescription 값에 넣음
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                )
            },
            iconBg = MaterialTheme.colorScheme.surfaceVariant, // iconBg 값을 정해줌
            title = "프리미엄 공간", // 제목을 정해줌
            description = "특별한 칭호와 전용 부스를 획득할 수 있어요" // description 값을 정해줌
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PlazaRequirementCard() { // PlazaRequirementCard 함수를 선언함
    SectionCard(title = "시스템 요구사항") { // 내용을 카드 모양으로 묶어서 보여줌
        RequirementText("브라우저", "Chrome, Edge, Firefox (최신 버전)") // 화면에 글자를 보여줌
        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
        RequirementText("운영체제", "Windows 10 이상, macOS 10.15 이상") // 화면에 글자를 보여줌
        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
        RequirementText("메모리", "최소 4GB RAM (8GB 권장)") // 화면에 글자를 보여줌
        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
        RequirementText("그래픽", "WebGL 2.0 지원") // 화면에 글자를 보여줌
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PlazaOnlineUsersCard() { // PlazaOnlineUsersCard 함수를 선언함
    val users = listOf( // users 값을 저장함
        Triple("절약왕", "광장 중앙", "💰"), // Triple 함수를 실행함
        Triple("패션왕", "프리미엄 존", "👗"), // Triple 함수를 실행함
        Triple("목표달성", "채팅 중", "🎯"), // Triple 함수를 실행함
        Triple("알뜰맨", "광장 입구", "🏃") // Triple 함수를 실행함
    )

    SectionCard(title = "현재 접속 중") { // 내용을 카드 모양으로 묶어서 보여줌
        Row( // 안쪽 UI를 가로로 배치함
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .size(10.dp)
                    .background(Color(0xFF6FCF97), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함
            Text( // 화면에 글자를 보여줌
                text = "124명 접속 중", // text 값을 정해줌
                style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }

        Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

        users.forEachIndexed { index, item ->
            OnlineUserRow( // 안쪽 UI를 가로로 배치함
                emoji = item.third, // emoji 값을 정해줌
                name = item.first, // name 값을 정해줌
                status = item.second // status 값을 정해줌
            )

            if (index != users.lastIndex) { // 조건이 맞는지 확인함
                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PlazaTipsCard() { // PlazaTipsCard 함수를 선언함
    SectionCard( // 내용을 카드 모양으로 묶어서 보여줌
        title = "광장 이용 팁" // 제목을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        TipText("WASD 키로 아바타를 움직일 수 있어요") // 화면에 글자를 보여줌
        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
        TipText("다른 유저 클릭 시 1:1 채팅이 가능해요") // 화면에 글자를 보여줌
        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
        TipText("특정 구역에서는 미니게임을 즐길 수 있어요") // 화면에 글자를 보여줌
        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
        TipText("성실도 점수가 높으면 특별한 공간이 열려요") // 화면에 글자를 보여줌
        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
        TipText("친구 추가 기능으로 함께 즐겨보세요") // 화면에 글자를 보여줌
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PlazaUpcomingCard() { // PlazaUpcomingCard 함수를 선언함
    Column { // 안쪽 UI를 세로로 배치함
        Text( // 화면에 글자를 보여줌
            text = "곧 추가될 기능", // text 값을 정해줌
            style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
            modifier = Modifier.padding(start = 4.dp) // UI 크기나 여백 같은 모양을 정함
        )

        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

        UpcomingItem( // Upcoming Item 함수를 실행함
            icon = { // icon 값을 정해줌
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Outlined.Star, // imageVector 값을 정해줌
                    contentDescription = null, // null 값을 contentDescription 값에 넣음
                    tint = MaterialTheme.colorScheme.onPrimaryContainer // tint 값을 정해줌
                )
            },
            title = "미니게임", // 제목을 정해줌
            description = "다양한 미니게임으로 SPT 획득", // description 값을 정해줌
            borderColor = plazaSoftCardBorderColor(), // borderColor 값을 정해줌
            backgroundColor = plazaSoftCardColor() // backgroundColor 값을 정해줌
        )

        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

        UpcomingItem( // Upcoming Item 함수를 실행함
            icon = { // icon 값을 정해줌
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Outlined.Groups, // imageVector 값을 정해줌
                    contentDescription = null, // null 값을 contentDescription 값에 넣음
                    tint = MaterialTheme.colorScheme.onPrimaryContainer // tint 값을 정해줌
                )
            },
            title = "길드 시스템", // 제목을 정해줌
            description = "친구들과 길드를 만들어보세요", // description 값을 정해줌
            borderColor = plazaSoftCardBorderColor(), // borderColor 값을 정해줌
            backgroundColor = plazaSoftCardColor() // backgroundColor 값을 정해줌
        )

        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

        UpcomingItem( // Upcoming Item 함수를 실행함
            icon = { // icon 값을 정해줌
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Outlined.Campaign, // imageVector 값을 정해줌
                    contentDescription = null, // null 값을 contentDescription 값에 넣음
                    tint = MaterialTheme.colorScheme.onPrimaryContainer // tint 값을 정해줌
                )
            },
            title = "이벤트 홀", // 제목을 정해줌
            description = "특별 이벤트 전용 공간", // description 값을 정해줌
            borderColor = plazaSoftCardBorderColor(), // borderColor 값을 정해줌
            backgroundColor = plazaSoftCardColor() // backgroundColor 값을 정해줌
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MobileNoticeCard() { // MobileNoticeCard 함수를 선언함
    val cardColor = plazaSoftCardColor()
    val cardBorderColor = plazaSoftCardBorderColor()
            Card( // 내용을 카드 모양으로 묶어서 보여줌
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
                colors = CardDefaults.cardColors( // colors 값을 정해줌
                    containerColor = cardColor // containerColor 값을 정해줌
        ),
                border = BorderStroke(1.dp, cardBorderColor)
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
            verticalAlignment = Alignment.Top // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "💡", // text 값을 정해줌
                style = MaterialTheme.typography.titleMedium // style 값을 정해줌
            )

            Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "모바일에서는 광장 기능을 이용할 수 없습니다.\nPC 환경에서 이용해주세요.", // text 값을 정해줌
                style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                fontWeight = FontWeight.Medium // fontWeight 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SectionCard( // SectionCard 함수를 선언함
    title: String, // 제목을 받음
    containerColor: Color? = null, // containerColor 값을 받음
    content: @Composable () -> Unit // 내용을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isPlazaDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val defaultCardColor = plazaSoftCardColor() // 일반 카드 색을 모드별로 분리함
    val cardBorderColor = plazaSoftCardBorderColor() // 일반 카드 테두리색을 모드별로 분리함
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = containerColor ?: defaultCardColor // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, cardBorderColor)
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier
                .padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            content() // content 함수를 실행함
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun isPlazaDarkTheme(): Boolean { // 앱 테마 기준으로 광장 다크모드 여부를 확인함
    return MaterialTheme.colorScheme.background == SpentopiaDarkBackground // 시스템 설정이 아니라 앱 설정 기준으로 판단함
}

@Composable
private fun plazaSoftCardColor(): Color {
    return if (isPlazaDarkTheme()) Color(0xFF171A2B) else Color(0xFFF8FBFF)
}

@Composable
private fun plazaSoftCardBorderColor(): Color {
    return if (isPlazaDarkTheme()) Color(0xFF4C3B7A) else Color(0xFFBFDBFE)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun FeatureRow( // FeatureRow 함수를 선언함
    icon: @Composable () -> Unit, // icon 값을 받음
    iconBg: Color, // iconBg 값을 받음
    title: String, // 제목을 받음
    description: String // description 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        verticalAlignment = Alignment.Top // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            icon() // icon 함수를 실행함
        }

        Spacer(modifier = Modifier.width(12.dp)) // UI 크기나 여백 같은 모양을 정함

        Column { // 안쪽 UI를 세로로 배치함
            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
            Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함
            Text( // 화면에 글자를 보여줌
                text = description, // description 값을 text 값에 넣음
                style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun RequirementText( // RequirementText 함수를 선언함
    label: String, // label 값을 받음
    value: String // 입력값을 받음
) { // 이 블록 안의 내용이 시작됨
    Column { // 안쪽 UI를 세로로 배치함
        Text( // 화면에 글자를 보여줌
            text = label, // label 값을 text 값에 넣음
            style = MaterialTheme.typography.titleSmall, // style 값을 정해줌
            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )
        Spacer(modifier = Modifier.height(2.dp)) // UI 크기나 여백 같은 모양을 정함
        Text( // 화면에 글자를 보여줌
            text = value, // 입력값을 text 값에 넣음
            style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun OnlineUserRow( // OnlineUserRow 함수를 선언함
    emoji: String, // emoji 값을 받음
    name: String, // name 값을 받음
    status: String // status 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), // color 값을 정해줌
                shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
            )
            .padding(horizontal = 14.dp, vertical = 14.dp), // .padding(horizontal 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = emoji, // emoji 값을 text 값에 넣음
            style = MaterialTheme.typography.titleLarge // style 값을 정해줌
        )

        Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = name, // name 값을 text 값에 넣음
            modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
            style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
            fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )

        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .border(
                    width = 1.dp, // width 값을 정해줌
                    color = MaterialTheme.colorScheme.outlineVariant, // color 값을 정해줌
                    shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                )
                .padding(horizontal = 10.dp, vertical = 6.dp) // .padding(horizontal 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = status, // status 값을 text 값에 넣음
                style = MaterialTheme.typography.labelMedium, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun TipText(text: String) { // TipText 함수를 선언함
    Text( // 화면에 글자를 보여줌
        text = "• $text", // text 값을 정해줌
        style = MaterialTheme.typography.bodyLarge, // style 값을 정해줌
        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
    )
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun UpcomingItem( // UpcomingItem 함수를 선언함
    icon: @Composable () -> Unit, // icon 값을 받음
    title: String, // 제목을 받음
    description: String, // description 값을 받음
    borderColor: Color, // borderColor 값을 받음
    backgroundColor: Color // backgroundColor 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = backgroundColor // backgroundColor 값을 containerColor 값에 넣음
        ),
        border = BorderStroke(1.dp, borderColor) // border 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp), // .padding(vertical 값을 정해줌
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier.size(40.dp), // UI 크기나 여백 같은 모양을 정함
                contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                icon() // icon 함수를 실행함
            }

            Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                style = MaterialTheme.typography.titleLarge, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = description, // description 값을 text 값에 넣음
                style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}
