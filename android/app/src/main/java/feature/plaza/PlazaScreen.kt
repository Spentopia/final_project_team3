package com.example.spentopia.feature.plaza

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.ui.toast.AppToastType
import com.ict.spentopia.ui.toast.showAppToast
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlazaScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // 현재 화면 정보를 저장함
    val scope = rememberCoroutineScope() // API 요청을 실행할 코루틴 범위를 저장함
    var gameLoginCode by remember { mutableStateOf<String?>(null) } // 발급받은 게임 로그인 코드를 저장함
    var gameLoginExpiresIn by remember { mutableStateOf(60) } // 코드 만료 시간을 저장함
    var gameLoginRemainingSeconds by remember { mutableStateOf<Int?>(null) } // 화면에 보여줄 남은 시간을 저장함
    var isCreatingGameLoginCode by remember { mutableStateOf(false) } // 코드 생성 중인지 저장함
    var showGameLoginCodeDialog by remember { mutableStateOf(false) } // 코드 팝업 표시 여부를 저장함

    LaunchedEffect(gameLoginRemainingSeconds) {
        val remaining = gameLoginRemainingSeconds ?: return@LaunchedEffect
        if (remaining <= 0) return@LaunchedEffect
        delay(1_000) // 1초마다 남은 시간을 줄임
        gameLoginRemainingSeconds = remaining - 1 // 남은 시간을 1초 감소시킴
    }

    fun createGameLoginCode() { // 게임 로그인 코드를 생성하는 함수임
        if (isCreatingGameLoginCode) return

        scope.launch {
            try {
                isCreatingGameLoginCode = true // 중복 클릭을 막기 위해 생성 중 상태로 바꿈
                val response = RetrofitClient.authApi.issueGameLoginCode() // 백엔드에서 게임 로그인 코드를 발급받음
                gameLoginCode = response.handoff_token // 발급받은 코드를 화면 상태에 저장함
                gameLoginExpiresIn = response.expires_in // 코드 만료 시간을 저장함
                gameLoginRemainingSeconds = response.expires_in // 카운트다운 시간을 초기화함
                showGameLoginCodeDialog = true // 코드 생성 후 팝업을 표시함
                showAppToast(context, "게임 로그인 코드가 생성되었습니다.")
            } catch (error: Exception) {
                showAppToast(
                    context,
                    error.message ?: "게임 로그인 코드를 생성하지 못했습니다.",
                    AppToastType.ERROR
                )
            } finally {
                isCreatingGameLoginCode = false // 요청이 끝나면 생성 중 상태를 해제함
            }
        }
    }

    LazyColumn( // 광장 화면 내용을 세로 스크롤로 배치함
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { PlazaHeaderSection() } // 화면 상단 제목 영역을 보여줌

        item {
            PlazaHeroCard( // 게임 코드 생성 버튼이 있는 큰 카드 영역을 보여줌
                isCreatingGameLoginCode = isCreatingGameLoginCode,
                onCreateCodeClick = { createGameLoginCode() }
            )
        }

        item {
            TwoColumnLikeSection( // 게임 기능과 시스템 요구사항 카드를 세로로 보여줌
                first = { PlazaFeatureCard() },
                second = { PlazaRequirementCard() }
            )
        }

        item {
            TwoColumnLikeSection( // 설치 가이드와 이용 팁 카드를 세로로 보여줌
                first = { PlazaInstallGuideCard() },
                second = { PlazaTipsCard() }
            )
        }

        item { PlazaUpcomingCard() } // 곧 추가될 기능 카드를 보여줌

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showGameLoginCodeDialog) {
        GameLoginCodeDialog( // 게임 로그인 코드 팝업을 보여줌
            code = gameLoginCode,
            remainingSeconds = gameLoginRemainingSeconds,
            expiresIn = gameLoginExpiresIn,
            isCreating = isCreatingGameLoginCode,
            onRefreshClick = { createGameLoginCode() },
            onDismiss = { showGameLoginCodeDialog = false }
        )
    }
}

@Composable
private fun PlazaHeaderSection() {
    val colors = plazaColors() // 현재 테마에 맞는 광장 색상을 가져옴
    Row( // 제목과 PC 전용 뱃지를 가로로 배치함
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box( // 다운로드 제목 앞에 게임 아이콘 박스를 보여줌
                modifier = Modifier
                    .size(42.dp)
                    .background(colors.iconSurface, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SportsEsports,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column { // 제목과 설명 문구를 세로로 보여줌
                Text(
                    text = "다운로드",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.title
                )
                Text(
                    text = "아바타와 함께 다른 유저들을 만나보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.body
                )
            }
        }

        BadgeText(text = "PC 전용") // PC 전용 뱃지를 보여줌
    }
}

@Composable
private fun PlazaHeroCard(
    isCreatingGameLoginCode: Boolean,
    onCreateCodeClick: () -> Unit
) {
    val colors = plazaColors() // 현재 테마에 맞는 광장 색상을 가져옴
    Card( // 게임 코드 생성 영역을 카드로 묶어 보여줌
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 1.dp else 2.dp)
    ) {
        Box( // 히어로 카드 배경과 안쪽 콘텐츠를 배치함
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.heroBrush)
                .padding(horizontal = 20.dp, vertical = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column( // 게임 아이콘, 설명, 버튼을 세로로 배치함
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box( // 큰 게임 아이콘 바깥 원형 배경을 보여줌
                    modifier = Modifier
                        .size(104.dp)
                        .background(colors.heroIconBrush, CircleShape)
                        .border(1.dp, colors.heroIconBorder, CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box( // 큰 게임 아이콘 안쪽 원형 배경을 보여줌
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.heroIconInnerSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon( // 웹뷰 다운로드 화면처럼 게임패드 아이콘을 보여줌
                            imageVector = Icons.Outlined.SportsEsports,
                            contentDescription = null,
                            tint = colors.heroIconTint,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Spentopia 세계에 오신것을 환영합니다",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.title,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "PC에서 게임을 실행한 뒤 아래 코드를 입력해 Spentopia 계정과 연결하세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.body,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                GameCodeButton( // 게임 로그인 코드 생성 버튼을 보여줌
                    text = if (isCreatingGameLoginCode) "코드 생성 중..." else "게임 코드 생성",
                    enabled = !isCreatingGameLoginCode,
                    onClick = onCreateCodeClick
                )
            }
        }
    }
}

@Composable
private fun GameCodeButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = plazaColors() // 라이트모드/다크모드에 맞는 광장 색상 값을 가져옴
    val waveShift by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing), // 빛이 너무 빠르지 않게 천천히 움직이게 함
            repeatMode = RepeatMode.Reverse // 배경 빛이 왕복하게 해서 끊기는 선을 없앰
        )
    )
    val buttonBrush = Brush.linearGradient( // 버튼 기본 배경 그라데이션을 정함
        colors = colors.gameButtonColors,
        start = Offset(-260f + waveShift * 420f, 0f),
        end = Offset(720f + waveShift * 420f, 220f)
    )
    val shape = RoundedCornerShape(18.dp) // 게임 코드 버튼 모서리 모양을 정함

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape) // 빛 효과가 버튼 밖으로 튀어나오지 않게 자름
            .background(
                brush = if (enabled) buttonBrush else Brush.linearGradient(
                    listOf(colors.secondaryButton, colors.secondaryButton)
                ),
                shape = shape
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = colors.gameButtonContent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = colors.gameButtonContent,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GameLoginCodeDialog(
    code: String?,
    remainingSeconds: Int?,
    expiresIn: Int,
    isCreating: Boolean,
    onRefreshClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = plazaColors() // 코드 팝업에 사용할 색상을 가져옴
    Dialog(onDismissRequest = onDismiss) { // 게임 로그인 코드를 팝업으로 보여줌
        Card( // 팝업 내용을 카드 형태로 묶음
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column( // 팝업 안쪽 내용을 세로로 배치함
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row( // 팝업 제목과 닫기 버튼을 가로로 배치함
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text( // 팝업 제목을 보여줌
                        text = "게임 로그인 코드",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.title
                    )
                    Text( // 닫기 버튼을 텍스트 버튼처럼 보여줌
                        text = "닫기",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(colors.innerSurface, RoundedCornerShape(10.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box( // 발급된 게임 코드를 강조해서 보여주는 영역임
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.codeBox, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon( // 코드 영역 위에 키 아이콘을 보여줌
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text( // 게임 로그인 코드를 한 줄로 보여줌
                            text = code ?: "--------",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = when {
                                remainingSeconds == 0 -> colors.danger
                                code != null -> colors.accent
                                else -> colors.body
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text( // 코드 상태와 남은 시간을 보여줌
                            text = when {
                                code == null -> "게임 코드 생성 버튼을 눌러 1회용 로그인 코드를 발급하세요."
                                remainingSeconds == 0 -> "코드가 만료되었습니다. 새 코드를 생성해 주세요."
                                else -> "남은 시간: ${remainingSeconds ?: expiresIn}초"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingSeconds == 0) colors.danger else colors.body,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button( // 새 게임 코드를 다시 발급하는 버튼임
                            onClick = onRefreshClick,
                            enabled = !isCreating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.secondaryButton,
                                contentColor = colors.secondaryButtonContent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isCreating) "생성 중" else "코드 다시 생성")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.fillMaxWidth()) { // 게임 코드 사용 방법을 안내함
                    NumberedText("Steam 라이브러리에서 Spentopia 게임을 실행합니다.")
                    NumberedText("게임 로그인 화면의 입력창에 위 코드를 입력합니다.")
                    NumberedText("인증이 완료되면 게임에서 유저 정보와 아이템 정보를 불러옵니다.")
                }
            }
        }
    }
}

@Composable
private fun PlazaFeatureCard() {
    val colors = plazaColors() // 현재 테마에 맞는 색상을 가져옴
    val featureIconTint = if (colors.isDark) Color(0xFFC4B5FD) else Color(0xFF334155) // 기능 아이콘 색상을 모드별로 정함
    val featureIconSurface = if (colors.isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9) // 기능 아이콘 배경색을 모드별로 정함

    SectionCard(title = "게임 기능") { // 게임 기능 목록을 카드로 보여줌
        FeatureRow(
            icon = Icons.Outlined.Groups,
            iconTint = featureIconTint,
            iconSurface = featureIconSurface,
            title = "아바타 이동 & 채팅",
            description = "내 아바타를 움직이며 다른 유저들과 실시간 채팅을 즐겨보세요"
        )
        FeatureRow(
            icon = Icons.Outlined.AutoAwesome,
            iconTint = featureIconTint,
            iconSurface = featureIconSurface,
            title = "커스터마이징 반영",
            description = "내 아바타가 획득한 코디 아이템을 착용할 수 있어요"
        )
        FeatureRow(
            icon = Icons.Outlined.WorkspacePremium,
            iconTint = featureIconTint,
            iconSurface = featureIconSurface,
            title = "프리미엄 아이템",
            description = "성실하게 가계부를 작성하면 특별한 코디 아이템을 받을 수 있어요"
        )
    }
}

@Composable
private fun PlazaRequirementCard() {
    SectionCard(title = "시스템 요구사항") {
        RequirementText("브라우저", "Chrome, Edge, Firefox 최신 버전")
        RequirementText("운영체제", "Windows 10 이상, macOS 10.15 이상")
        RequirementText("메모리", "최소 4GB RAM, 8GB 권장")
        RequirementText("그래픽", "WebGL 2.0 지원")

        Spacer(modifier = Modifier.height(14.dp))

        NoticeBox( // 모바일 앱에서는 게임 실행이 안 된다는 안내를 보여줌
            title = "모바일에서는 게임 플레이가 불가능합니다."
        )
    }
}

@Composable
private fun PlazaInstallGuideCard() {
    SectionCard(title = "게임 설치 가이드") {
        NoticeBox(text = "STEAM 클라이언트 설치 및 로그인이 필요합니다.") // STEAM 설치 안내를 강조해서 보여줌
        Spacer(modifier = Modifier.height(14.dp))
        StepRow(Icons.Outlined.Download, Color(0xFF3B82F6), "STEP 01", "클라이언트 다운로드", "PC에서 설치 파일을 다운로드하고 실행합니다.") // 다운로드 단계 안내를 보여줌
        StepRow(Icons.Outlined.Key, Color(0xFF8B5CF6), "STEP 02", "스팀 계정 연동 및 인증", "스팀 라이브러리에 게임을 등록합니다.") // 인증 단계 안내를 보여줌
        StepRow(Icons.Outlined.PlayArrow, Color(0xFF10B981), "STEP 03", "게임 실행", "Spentopia 세계로 입장합니다.") // 실행 단계 안내를 보여줌
        StepRow(Icons.Outlined.CheckCircle, Color(0xFFF59E0B), "STEP 04", "오류 발생 시", "Steam 외부 게임 추가에서 .exe 파일을 등록합니다.") // 오류 발생 시 처리 방법을 보여줌
    }
}

@Composable
private fun PlazaTipsCard() {
    SectionCard(title = "광장 이용 팁") {
        TipRow(Icons.Outlined.OpenWith, Color(0xFF3B82F6), "방향키로 아바타를 움직일 수 있어요") // 이동 팁을 아이콘과 함께 보여줌
        TipRow(Icons.Outlined.Groups, Color(0xFF10B981), "다른 유저와 같은 서버에서 채팅이 가능해요") // 채팅 팁을 아이콘과 함께 보여줌
        TipRow(Icons.Outlined.PersonAdd, Color(0xFF6366F1), "STEAM 친구 기능으로 자유롭게 친구를 초대할 수 있어요") // 친구 초대 팁을 아이콘과 함께 보여줌
        TipRow(Icons.Outlined.Checkroom, Color(0xFFEC4899), "내가 꾸민 캐릭터를 저장해서 친구들에게 자랑할 수 있어요") // 캐릭터 꾸미기 팁을 아이콘과 함께 보여줌
    }
}

@Composable
private fun PlazaUpcomingCard() {
    SectionCard(title = "곧 추가될 기능") { // 추후 추가될 기능 목록을 보여줌
        UpcomingItem(Icons.Outlined.Star, "미니게임", "다양한 미니게임으로 SPT 획득")
        UpcomingItem(Icons.Outlined.Groups, "길드 시스템", "친구들과 길드를 만들어보세요")
        UpcomingItem(Icons.Outlined.Campaign, "이벤트 홀", "특별 이벤트 전용 공간")
    }
}

@Composable
private fun TwoColumnLikeSection(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // 모바일 화면에서 카드들을 세로로 배치함
        first()
        second()
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = plazaColors() // 섹션 카드에 사용할 색상을 가져옴
    Card( // 공통 섹션 카드 모양을 정함
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 1.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) { // 카드 안쪽 내용을 세로로 배치함
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconTint: Color,
    iconSurface: Color,
    title: String,
    description: String
) {
    val colors = plazaColors() // 기능 행에 사용할 색상을 가져옴
    Row( // 아이콘과 설명을 가로로 배치함
        modifier = Modifier.padding(bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        IconBox(icon = icon, tint = iconTint, surface = iconSurface) // 기능 아이콘 박스를 보여줌
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = colors.title)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.body)
        }
    }
}

@Composable
private fun StepRow(
    icon: ImageVector,
    iconTint: Color,
    step: String,
    title: String,
    description: String
) {
    val colors = plazaColors() // 설치 단계 행에 사용할 색상을 가져옴
    Row( // 설치 단계 내용을 가로로 배치함
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(colors.innerSurface, RoundedCornerShape(14.dp))
            .border(1.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = icon, tint = iconTint, surface = iconTint.copy(alpha = if (colors.isDark) 0.22f else 0.14f)) // 단계별 아이콘 색상을 보여줌
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = colors.title)
            Text(description, style = MaterialTheme.typography.bodySmall, color = colors.body)
        }
        BadgeText(step)
    }
}

@Composable
private fun RequirementText(label: String, value: String) {
    val colors = plazaColors() // 요구사항 문구 색상을 가져옴
    Column(modifier = Modifier.padding(bottom = 12.dp)) { // 요구사항 제목과 값을 세로로 보여줌
        Text(label, fontWeight = FontWeight.Bold, color = colors.title)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.body)
    }
}

@Composable
private fun TipRow(
    icon: ImageVector,
    iconTint: Color,
    text: String
) {
    val colors = plazaColors() // 현재 테마에 맞는 색상을 가져옴
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(colors.innerSurface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(
            icon = icon,
            tint = iconTint,
            surface = iconTint.copy(alpha = if (colors.isDark) 0.22f else 0.13f) // 라이트/다크에 맞춰 아이콘 배경 농도를 정함
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.body,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumberedText(text: String) {
    val colors = plazaColors() // 안내 문구 색상을 가져옴
    Text( // 게임 코드 사용 방법 문구를 보여줌
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.body,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun UpcomingItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = plazaColors() // 추가 예정 항목 색상을 가져옴
    Column( // 추가 예정 기능 하나를 카드처럼 보여줌
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(colors.innerSurface, RoundedCornerShape(16.dp))
            .border(1.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(30.dp)) // 추가 예정 기능 아이콘을 보여줌
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, color = colors.title)
        Text(description, style = MaterialTheme.typography.bodySmall, color = colors.body, textAlign = TextAlign.Center)
    }
}

@Composable
private fun IconBox(
    icon: ImageVector,
    tint: Color? = null,
    surface: Color? = null
) {
    val colors = plazaColors() // 아이콘 박스 기본 색상을 가져옴
    val borderColor = if (colors.isDark) {
        colors.accent.copy(alpha = 0.58f)
    } else {
        colors.border
    }
    Box( // 아이콘을 둥근 사각형 안에 넣어 보여줌
        modifier = Modifier
            .size(42.dp)
            .background(surface ?: colors.iconSurface, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: colors.accent, modifier = Modifier.size(22.dp)) // 전달받은 아이콘을 보여줌
    }
}

@Composable
private fun BadgeText(text: String) {
    val colors = plazaColors() // 뱃지 색상을 가져옴
    Box( // 작은 뱃지 영역을 보여줌
        modifier = Modifier
            .background(colors.badgeSurface, RoundedCornerShape(999.dp))
            .border(1.dp, colors.border.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.badgeText
        )
    }
}

@Composable
private fun NoticeBox(
    text: String? = null,
    title: String? = null
) {
    val colors = plazaColors() // 안내 박스도 라이트/다크모드 색상을 따르게 함
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.noticeSurface, RoundedCornerShape(14.dp))
            .border(1.dp, colors.noticeBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(colors.noticeIconSurface, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!", // 안내 문구 앞에 강조 표시를 넣음
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = colors.noticeIconText
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = colors.noticeTitle // 제목은 진하게 보이도록 제목 색상을 사용함
                )
                if (text != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
            if (text != null) {
                Text(
                    text = text,
                    style = if (title != null) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = if (title != null) FontWeight.SemiBold else FontWeight.Black,
                    color = if (title != null) colors.noticeText else colors.noticeTitle // 제목 없는 안내도 잘 보이게 진한 색을 사용함
                )
            }
        }
    }
}

@Composable
private fun plazaColors(): PlazaColors {
    val isDark = MaterialTheme.colorScheme.background == SpentopiaDarkBackground // 현재 앱이 다크모드인지 확인함
    return if (isDark) {
        PlazaColors( // 다크모드에서 사용할 광장 색상 모음임
            isDark = true,
            title = Color(0xFFF8FAFC),
            body = Color(0xFFCBD5E1),
            surface = Color(0xFF111A2A),
            innerSurface = Color(0xFF0F172A).copy(alpha = 0.82f),
            iconSurface = Color(0xFF1E1B4B).copy(alpha = 0.78f),
            accent = Color(0xFFC4B5FD),
            border = Color(0xFF8B5CF6).copy(alpha = 0.42f),
            secondaryButton = Color(0xFF1E293B),
            secondaryButtonContent = Color(0xFFE0E7FF),
            badgeSurface = Color(0xFF090B16),
            badgeText = Color.White,
            codeBox = Color(0xFF0B1220),
            danger = Color(0xFFFCA5A5),
            noticeSurface = Color(0xFF0B1220),
            noticeBorder = Color(0xFF8B5CF6).copy(alpha = 0.42f),
            noticeTitle = Color.White,
            noticeText = Color(0xFFE2E8F0),
            noticeIconSurface = Color(0xFF1E1B4B),
            noticeIconText = Color.White,
            heroIconBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E1B4B), Color(0xFF1E1B4B)),
                start = Offset.Zero,
                end = Offset(360f, 360f)
            ),
            heroIconBorder = Color(0xFFA78BFA).copy(alpha = 0.72f),
            heroIconInnerSurface = Color(0xFF0B1220).copy(alpha = 0.88f),
            heroIconTint = Color(0xFFE0F2FE),
            gameButtonColors = listOf(
                Color(0xFF111827),
                Color(0xFF312E81),
                Color(0xFF4C1D95),
                Color(0xFF111827)
            ),
            gameButtonBorder = Color(0xFFA78BFA).copy(alpha = 0.55f),
            gameButtonContent = Color.White,
            heroBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF070A18), Color(0xFF111827), Color(0xFF211A45)),
                start = Offset.Zero,
                end = Offset(900f, 900f)
            )
        )
    } else {
        PlazaColors( // 라이트모드에서 사용할 광장 색상 모음임
            isDark = false,
            title = Color(0xFF0F172A),
            body = Color(0xFF53657D),
            surface = Color(0xFFF8FBFF),
            innerSurface = Color.White.copy(alpha = 0.78f),
            iconSurface = Color(0xFFE0F2FE),
            accent = Color(0xFF2563EB),
            border = Color(0xFF7DD3FC),
            secondaryButton = Color(0xFFE0F2FE),
            secondaryButtonContent = Color(0xFF1E3A8A),
            badgeSurface = Color.White,
            badgeText = Color(0xFF0F172A),
            codeBox = Color.White,
            danger = Color(0xFFDC2626),
            noticeSurface = Color(0xFFEFF6FF),
            noticeBorder = Color(0xFF7DD3FC),
            noticeTitle = Color(0xFF0F172A),
            noticeText = Color(0xFF334155),
            noticeIconSurface = Color(0xFFDBEAFE),
            noticeIconText = Color(0xFF2563EB),
            heroIconBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFE0F2FE), Color(0xFFE0F2FE)),
                start = Offset.Zero,
                end = Offset(360f, 360f)
            ),
            heroIconBorder = Color(0xFF38BDF8),
            heroIconInnerSurface = Color.White.copy(alpha = 0.92f),
            heroIconTint = Color(0xFF2563EB),
            gameButtonColors = listOf(
                Color(0xFF2563EB),
                Color(0xFF1D4ED8),
                Color(0xFF4F46E5),
                Color(0xFF2563EB)
            ),
            gameButtonBorder = Color(0xFF60A5FA),
            gameButtonContent = Color.White,
            heroBrush = Brush.linearGradient(
                colors = listOf(Color.White, Color(0xFFF8FBFF), Color(0xFFE0F2FE)),
                start = Offset.Zero,
                end = Offset(900f, 900f)
            )
        )
    }
}

private data class PlazaColors(
    val isDark: Boolean,
    val title: Color,
    val body: Color,
    val surface: Color,
    val innerSurface: Color,
    val iconSurface: Color,
    val accent: Color,
    val border: Color,
    val secondaryButton: Color,
    val secondaryButtonContent: Color,
    val badgeSurface: Color,
    val badgeText: Color,
    val codeBox: Color,
    val danger: Color,
    val noticeSurface: Color,
    val noticeBorder: Color,
    val noticeTitle: Color,
    val noticeText: Color,
    val noticeIconSurface: Color,
    val noticeIconText: Color,
    val heroIconBrush: Brush,
    val heroIconBorder: Color,
    val heroIconInnerSurface: Color,
    val heroIconTint: Color,
    val gameButtonColors: List<Color>,
    val gameButtonBorder: Color,
    val gameButtonContent: Color,
    val heroBrush: Brush
)
