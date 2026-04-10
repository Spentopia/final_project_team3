package com.ict.spentopia.navigation // 앱의 화면 이동(네비게이션) 관련 코드를 모아두는 패키지

import androidx.compose.runtime.Composable // Compose UI 함수임을 나타내는 어노테이션
import androidx.navigation.compose.NavHost // 여러 화면의 이동 흐름을 관리하는 네비게이션 컨테이너
import androidx.navigation.compose.composable // 각 화면(목적지)을 네비게이션 그래프에 등록할 때 사용
import androidx.navigation.compose.rememberNavController // 화면 이동을 제어하는 NavController를 생성하고 기억함
import com.ict.spentopia.feature.analysis.AnalysisScreen // 소비 분석 화면 컴포저블 가져오기
import com.ict.spentopia.feature.auth.LoginScreen // 로그인 화면 컴포저블 가져오기
import com.ict.spentopia.feature.avatar.AvatarScreen // 내 아바타 화면 컴포저블 가져오기
import com.ict.spentopia.feature.budget.BudgetScreen // 예산 설정 화면 컴포저블 가져오기
import com.ict.spentopia.feature.community.CommunityScreen // 커뮤니티 화면 컴포저블 가져오기
import com.ict.spentopia.feature.home.HomeScreen // 홈 화면 컴포저블 가져오기
import com.ict.spentopia.feature.ledger.LedgerScreen // 가계부 화면 컴포저블 가져오기
import com.ict.spentopia.feature.market.MarketScreen // NFT 마켓 화면 컴포저블 가져오기
import com.ict.spentopia.feature.mypage.MyPageScreen // 마이페이지 화면 컴포저블 가져오기
import com.ict.spentopia.feature.plaza.PlazaScreen // 광장 화면 컴포저블 가져오기
import com.ict.spentopia.feature.signup.SignUpStep1Screen // 회원가입 1단계 화면 컴포저블 가져오기
import com.ict.spentopia.feature.signup.SignUpStep2Screen // 회원가입 2단계 화면 컴포저블 가져오기
import com.ict.spentopia.feature.signup.SignUpStep3Screen // 회원가입 3단계 화면 컴포저블 가져오기

@Composable // Compose에서 호출되는 UI 함수라는 뜻
fun AppNavGraph() { // 앱 전체의 화면 이동 경로를 정의하는 함수
    val navController = rememberNavController() // 화면 이동을 관리하는 NavController 객체를 생성하고 유지함

    NavHost( // 네비게이션 그래프를 시작하는 컨테이너
        navController = navController, // 위에서 만든 navController를 연결
        startDestination = Route.Login.route // 앱이 처음 실행됐을 때 가장 먼저 보여줄 시작 화면을 로그인 화면으로 지정
    ) {
        composable(route = Route.Login.route) { // 로그인 화면 경로를 등록
            LoginScreen( // 로그인 화면을 표시
                onLoginClick = { // 로그인 버튼을 눌렀을 때 실행
                    navController.navigate(Route.Home.route) // 홈 화면으로 이동
                },
                onSignUpClick = { // 회원가입 버튼을 눌렀을 때 실행
                    navController.navigate(Route.SignUpStep1.route) // 회원가입 1단계 화면으로 이동
                },
                onKakaoClick = { }, // 카카오 로그인 기능은 아직 미구현
                onNaverClick = { }, // 네이버 로그인 기능은 아직 미구현
                onGoogleClick = { } // 구글 로그인 기능은 아직 미구현
            )
        }

        composable(route = Route.SignUpStep1.route) { // 회원가입 1단계 화면 경로를 등록
            SignUpStep1Screen( // 회원가입 1단계 화면 표시
                onNextClick = { // 다음 버튼 클릭 시 실행
                    navController.navigate(Route.SignUpStep2.route) // 회원가입 2단계 화면으로 이동
                },
                onBackClick = { // 뒤로가기 버튼 클릭 시 실행
                    navController.popBackStack() // 현재 화면을 스택에서 제거하고 이전 화면으로 돌아감
                }
            )
        }

        composable(route = Route.SignUpStep2.route) { // 회원가입 2단계 화면 경로를 등록
            SignUpStep2Screen( // 회원가입 2단계 화면 표시
                onNextClick = { // 다음 버튼 클릭 시 실행
                    navController.navigate(Route.SignUpStep3.route) // 회원가입 3단계 화면으로 이동
                },
                onBackClick = { // 뒤로가기 버튼 클릭 시 실행
                    navController.popBackStack() // 이전 화면으로 돌아감
                }
            )
        }

        composable(route = Route.SignUpStep3.route) { // 회원가입 3단계 화면 경로를 등록
            SignUpStep3Screen( // 회원가입 3단계 화면 표시
                onFinishClick = { // 회원가입 완료 버튼 클릭 시 실행
                    navController.navigate(Route.Home.route) // 완료 후 홈 화면으로 이동
                },
                onBackClick = { // 뒤로가기 버튼 클릭 시 실행
                    navController.popBackStack() // 이전 화면으로 돌아감
                }
            )
        }

        composable(route = Route.Home.route) { // 홈 화면 경로를 등록
            HomeScreen(
                onLedgerClick = { // 가계부 이동 버튼 클릭 시 실행
                    navController.navigate(Route.Ledger.route) // 가계부 화면으로 이동
                },
                onMyPageClick = { // 마이페이지 이동 버튼 클릭 시 실행
                    navController.navigate(Route.MyPage.route) // 마이페이지 화면으로 이동
                },
                onBudgetClick = { // 예산 설정 이동 버튼 클릭 시 실행
                    navController.navigate(Route.Budget.route) // 예산 설정 화면으로 이동
                },
                onAnalysisClick = { // 소비 분석 이동 버튼 클릭 시 실행
                    navController.navigate(Route.Analysis.route) // 소비 분석 화면으로 이동
                },
                onAvatarClick = { // 내 아바타 이동 버튼 클릭 시 실행
                    navController.navigate(Route.Avatar.route) // 내 아바타 화면으로 이동
                },
                onMarketClick = { // NFT 마켓 이동 버튼 클릭 시 실행
                    navController.navigate(Route.Market.route) // NFT 마켓 화면으로 이동
                },
                onPlazaClick = { // 광장 이동 버튼 클릭 시 실행
                    navController.navigate(Route.Plaza.route) // 광장 화면으로 이동
                },
                onCommunityClick = { // 커뮤니티 이동 버튼 클릭 시 실행
                    navController.navigate(Route.Community.route) // 커뮤니티 화면으로 이동
                }
            )
        }

        composable(route = Route.Ledger.route) { // 가계부 화면 경로를 등록
            LedgerScreen() // 가계부 화면 표시
        }

        composable(route = Route.MyPage.route) { // 마이페이지 화면 경로를 등록
            MyPageScreen() // 마이페이지 화면 표시
        }

        composable(route = Route.Budget.route) { // 예산 설정 화면 경로를 등록
            BudgetScreen() // 예산 설정 화면 표시
        }

        composable(route = Route.Analysis.route) { // 소비 분석 화면 경로를 등록
            AnalysisScreen() // 소비 분석 화면 표시
        }

        composable(route = Route.Avatar.route) { // 내 아바타 화면 경로를 등록
            AvatarScreen() // 내 아바타 화면 표시
        }

        composable(route = Route.Market.route) { // NFT 마켓 화면 경로를 등록
            MarketScreen() // NFT 마켓 화면 표시
        }

        composable(route = Route.Plaza.route) { // 광장 화면 경로를 등록
            PlazaScreen() // 광장 화면 표시
        }

        composable(route = Route.Community.route) { // 커뮤니티 화면 경로를 등록
            CommunityScreen() // 커뮤니티 화면 표시
        }
    }
}