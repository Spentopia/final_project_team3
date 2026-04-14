package com.ict.spentopia.navigation // 네비게이션 관련 코드를 모아두는 패키지

import androidx.compose.foundation.layout.padding // Scaffold 안쪽 패딩을 적용할 때 사용
import androidx.compose.material.icons.Icons // 기본 아이콘 묶음
import androidx.compose.material.icons.filled.Menu // 햄버거 메뉴 아이콘
import androidx.compose.material.icons.filled.NotificationsNone // 알림 아이콘
import androidx.compose.material.icons.filled.Settings // 설정 아이콘
import androidx.compose.material3.CenterAlignedTopAppBar // 가운데 정렬된 상단 앱바
import androidx.compose.material3.ExperimentalMaterial3Api // TopAppBar 사용 시 필요한 실험용 어노테이션
import androidx.compose.material3.Icon // 아이콘 표시
import androidx.compose.material3.IconButton // 아이콘 버튼
import androidx.compose.material3.ModalNavigationDrawer // 왼쪽에서 나오는 드로어 레이아웃
import androidx.compose.material3.ModalDrawerSheet // 드로어 안쪽 시트 UI
import androidx.compose.material3.Scaffold // 상단바/하단바/본문을 나누는 레이아웃
import androidx.compose.material3.Text // 텍스트 표시
import androidx.compose.material3.TopAppBarDefaults // TopAppBar 기본 설정
import androidx.compose.material3.rememberDrawerState // 드로어 상태를 기억하는 함수
import androidx.compose.material3.DrawerValue // 드로어 열림/닫힘 상태 값
import androidx.compose.runtime.Composable // Compose UI 함수 표시
import androidx.compose.runtime.rememberCoroutineScope // 코루틴 스코프 생성
import androidx.compose.ui.Modifier // UI 수정자
import androidx.navigation.compose.NavHost // 여러 화면을 연결하는 네비게이션 호스트
import androidx.navigation.compose.composable // 각 route별 화면 등록
import androidx.navigation.compose.currentBackStackEntryAsState // 현재 route 확인
import androidx.navigation.compose.rememberNavController // NavController 생성
import com.ict.spentopia.feature.analysis.AnalysisScreen // 소비 분석 화면
import com.ict.spentopia.feature.avatar.AvatarScreen // 아바타 화면
import com.ict.spentopia.feature.budget.BudgetScreen // 예산 설정 화면
import com.ict.spentopia.feature.community.CommunityScreen // 커뮤니티 화면
import com.ict.spentopia.feature.home.HomeScreen // 홈 화면
import com.ict.spentopia.feature.market.MarketScreen // NFT 마켓 화면
import com.ict.spentopia.feature.mypage.MyPageScreen // 마이페이지 화면
import com.ict.spentopia.feature.plaza.PlazaScreen // 광장 화면
import kotlinx.coroutines.launch // 드로어 열고 닫을 때 코루틴으로 실행

@OptIn(ExperimentalMaterial3Api::class) // Material3 실험 API 사용 허용
@Composable // Compose 화면 함수
fun AppNavGraph() { // 앱 전체 화면 이동 구조를 담당하는 함수

    val navController = rememberNavController() // 화면 이동을 제어하는 navController 생성
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) // 드로어 초기 상태를 닫힘으로 설정
    val scope = rememberCoroutineScope() // 드로어 열기/닫기를 위한 코루틴 스코프 생성

    val navBackStackEntry = navController.currentBackStackEntryAsState() // 현재 네비게이션 스택 상태를 가져옴
    val currentRoute = navBackStackEntry.value?.destination?.route // 현재 보고 있는 화면 route를 꺼냄

    val showDrawerScreens = setOf( // 드로어와 상단바를 보여줄 메인 화면 목록
        Route.Home.route, // 홈 화면
        Route.Budget.route, // 예산 설정 화면
        Route.Analysis.route, // 소비 분석 화면
        Route.Avatar.route, // 아바타 화면
        Route.Market.route, // NFT 마켓 화면
        Route.Plaza.route, // 광장 화면
        Route.Community.route, // 커뮤니티 화면
        Route.MyPage.route // 마이페이지 화면
    )

    val shouldShowDrawer = currentRoute in showDrawerScreens // 현재 화면이 메인 화면이면 드로어 표시 여부 true

    ModalNavigationDrawer( // 전체 화면을 드로어로 감싸는 레이아웃 시작
        drawerState = drawerState, // 위에서 만든 드로어 상태 연결
        gesturesEnabled = shouldShowDrawer, // 메인 화면일 때만 손가락 스와이프로 드로어 열기 허용
        drawerContent = { // 왼쪽 메뉴 내용 정의
            if (shouldShowDrawer) { // 현재 화면이 메인 화면일 때만 드로어 내용 표시
                ModalDrawerSheet { // 드로어 안쪽 흰색 시트 UI
                    AppDrawerContent( // 우리가 따로 만든 공통 메뉴 UI 호출
                        onLedgerClick = { // 가계부 메뉴 클릭 시 실행
                            scope.launch { drawerState.close() } // 먼저 드로어 닫기
                            navController.navigate(Route.Home.route) { // 홈 화면으로 이동
                                launchSingleTop = true // 같은 화면 중복 생성 방지
                            }
                        },
                        onBudgetClick = { // 예산 설정 메뉴 클릭 시 실행
                            scope.launch { drawerState.close() } // 먼저 드로어 닫기
                            navController.navigate(Route.Budget.route) { // 예산 설정 화면으로 이동
                                launchSingleTop = true // 같은 화면 중복 생성 방지
                            }
                        },
                        onAnalysisClick = { // 소비 분석 메뉴 클릭 시 실행
                            scope.launch { drawerState.close() } // 먼저 드로어 닫기
                            navController.navigate(Route.Analysis.route) { // 소비 분석 화면으로 이동
                                launchSingleTop = true // 같은 화면 중복 생성 방지
                            }
                        },
                        onAvatarClick = { // 아바타 메뉴 클릭 시 실행
                            scope.launch { drawerState.close() } // 먼저 드로어 닫기
                            navController.navigate(Route.Avatar.route) { // 아바타 화면으로 이동
                                launchSingleTop = true // 같은 화면 중복 생성 방지
                            }
                        },
                        onMarketClick = { // NFT 마켓 메뉴 클릭 시 실행
                            scope.launch { drawerState.close() } // 먼저 드로어 닫기
                            navController.navigate(Route.Market.route) { // 마켓 화면으로 이동
                                launchSingleTop = true // 같은 화면 중복 생성 방지
                            }
                        },
                        onPlazaClick = { // 광장 메뉴 클릭 시 실행
                            scope.launch { drawerState.close() } // 먼저 드로어 닫기
                            navController.navigate(Route.Plaza.route) { // 광장 화면으로 이동
                                launchSingleTop = true // 같은 화면 중복 생성 방지
                            }
                        },
                        onCommunityClick = { // 커뮤니티 메뉴 클릭 시 실행
                            scope.launch { drawerState.close() } // 먼저 드로어 닫기
                            navController.navigate(Route.Community.route) { // 커뮤니티 화면으로 이동
                                launchSingleTop = true // 같은 화면 중복 생성 방지
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold( // 상단바 + 본문 구조를 잡는 Scaffold 시작
            topBar = { // 상단 앱바 정의
                if (shouldShowDrawer) { // 메인 화면일 때만 상단바 표시
                    CenterAlignedTopAppBar( // 가운데 정렬된 상단바 사용
                        title = { // 상단바 제목 영역
                            Text("Spentopia") // 앱 이름 표시
                        },
                        navigationIcon = { // 왼쪽 햄버거 메뉴 버튼 영역
                            IconButton( // 메뉴 버튼
                                onClick = { // 버튼 클릭 시 실행
                                    scope.launch { drawerState.open() } // 드로어 열기
                                }
                            ) {
                                Icon( // 메뉴 아이콘 표시
                                    imageVector = Icons.Default.Menu, // 햄버거 메뉴 아이콘 사용
                                    contentDescription = "메뉴" // 접근성용 설명
                                )
                            }
                        },
                        actions = { // 오른쪽 액션 아이콘 영역
                            IconButton(onClick = { }) { // 설정 버튼 클릭 이벤트는 아직 비워둠
                                Icon( // 설정 아이콘 표시
                                    imageVector = Icons.Default.Settings, // 설정 아이콘
                                    contentDescription = "설정" // 접근성용 설명
                                )
                            }
                            IconButton(onClick = { }) { // 알림 버튼 클릭 이벤트는 아직 비워둠
                                Icon( // 알림 아이콘 표시
                                    imageVector = Icons.Default.NotificationsNone, // 알림 아이콘
                                    contentDescription = "알림" // 접근성용 설명
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors() // 기본 상단바 색상 사용
                    )
                }
            }
        ) { innerPadding -> // Scaffold가 본문에 줄 패딩 값 받기
            NavHost( // 실제 화면 이동을 담당하는 NavHost
                navController = navController, // 위에서 만든 navController 연결
                startDestination = Route.Home.route, // 앱 시작 화면을 홈으로 지정
                modifier = Modifier.padding(innerPadding) // 상단바와 겹치지 않게 패딩 적용
            ) {

                composable(Route.Home.route) { // 홈 route 등록
                    HomeScreen( // 홈 화면 표시
                        onLedgerClick = { // 가계부 클릭 시
                            navController.navigate(Route.Home.route) { // 홈으로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        },
                        onMyPageClick = { // 마이페이지 클릭 시
                            navController.navigate(Route.MyPage.route) { // 마이페이지로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        },
                        onBudgetClick = { // 예산 설정 클릭 시
                            navController.navigate(Route.Budget.route) { // 예산 설정으로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        },
                        onAnalysisClick = { // 소비 분석 클릭 시
                            navController.navigate(Route.Analysis.route) { // 소비 분석으로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        },
                        onAvatarClick = { // 아바타 클릭 시
                            navController.navigate(Route.Avatar.route) { // 아바타 화면으로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        },
                        onMarketClick = { // NFT 마켓 클릭 시
                            navController.navigate(Route.Market.route) { // NFT 마켓 화면으로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        },
                        onPlazaClick = { // 광장 클릭 시
                            navController.navigate(Route.Plaza.route) { // 광장 화면으로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        },
                        onCommunityClick = { // 커뮤니티 클릭 시
                            navController.navigate(Route.Community.route) { // 커뮤니티 화면으로 이동
                                launchSingleTop = true // 중복 방지
                            }
                        }
                    )
                }

                composable(Route.Budget.route) { // 예산 설정 route 등록
                    BudgetScreen() // 예산 설정 화면 표시
                }

                composable(Route.Analysis.route) { // 소비 분석 route 등록
                    AnalysisScreen() // 소비 분석 화면 표시
                }

                composable(Route.Avatar.route) { // 아바타 route 등록
                    AvatarScreen() // 아바타 화면 표시
                }

                composable(Route.Market.route) { // NFT 마켓 route 등록
                    MarketScreen() // NFT 마켓 화면 표시
                }

                composable(Route.Plaza.route) { // 광장 route 등록
                    PlazaScreen() // 광장 화면 표시
                }

                composable(Route.Community.route) { // 커뮤니티 route 등록
                    CommunityScreen() // 커뮤니티 화면 표시
                }

                composable(Route.MyPage.route) { // 마이페이지 route 등록
                    MyPageScreen() // 마이페이지 화면 표시
                }
            }
        }
    }
}