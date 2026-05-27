# Spentopia 프로젝트 명세서

Spentopia는 소비 기록, 예산 관리, AI 소비 분석, 커뮤니티, 성실도 보상, 아바타 NFT, SPT 토큰 마켓을 하나로 묶은 소비 습관 개선 서비스입니다. 웹, Android 앱, Rust 백엔드, FastAPI AI 서버, Solana Anchor 스마트컨트랙트로 구성되어 있습니다.

## 1. 프로젝트 개요

### 1.1 서비스 목적

- 사용자의 소비 내역과 예산을 기록하고 분석합니다.
- 영수증 OCR을 통해 실제 지출 인증을 보조합니다.
- AI 챗봇과 리포트로 소비 패턴, 위험도, 절약 조언을 제공합니다.
- 꾸준한 기록과 예산 실천을 성실도 점수로 환산합니다.
- 성실도, 콘테스트, 이벤트 보상으로 SPT 토큰과 아바타 NFT를 지급합니다.
- 지급받은 NFT는 SPT 기반 마켓에서 거래할 수 있습니다.
- Android 앱과 웹 프론트엔드가 동일한 백엔드 API를 사용합니다.

### 1.2 시스템 구성

| 영역 | 경로 | 기술 스택 | 역할 |
| --- | --- | --- | --- |
| Web Frontend | `frontend/` | React, TypeScript, Vite, Tailwind, MUI, Radix UI, Solana wallet adapter | 웹 UI, 지갑 연결, 마켓, 관리자 콘솔 |
| Backend API | `backend/` | Rust, Axum, Tokio, Supabase REST, JWT, Utoipa Swagger | 인증, 도메인 API, 보상 배치, Solana 연동 |
| AI Server | `ai-server/` | Python, FastAPI, OpenAI API | 소비 분석, 챗봇, 영수증 OCR, AI 예산 플랜 |
| Android App | `android/` | Kotlin, Jetpack Compose, Retrofit, Room, DataStore, MWA | 모바일 앱, 지갑/소셜 로그인, 소비 관리 |
| Smart Contract | `smartcontract/` | Solana Anchor, Rust, SPL Token, Metaplex | SPT 토큰, 아바타 NFT, NFT 마켓 거래 |
| Unity | `unity/` | Unity 연동 예정 | 게임/아바타 렌더링 연동 영역 |

## 2. 핵심 기능 명세

### 2.1 인증 및 계정

- 이메일 기반 회원가입/로그인과 프로필 완성 플로우를 제공합니다.
- 카카오 OAuth 로그인과 Android 앱용 카카오 로그인 플로우를 제공합니다.
- Google 로그인은 프론트엔드 및 Android 앱에서 Supabase/Google 인증 흐름과 연결됩니다.
- Solana 지갑 로그인은 nonce 발급, 지갑 서명, 서명 검증 순서로 처리됩니다.
- 웹 access token은 메모리에 저장하고 refresh token은 HttpOnly 쿠키로 관리합니다.
- Android access/refresh token은 앱 로컬 저장소를 사용하며 `TokenAuthenticator`가 401 발생 시 갱신합니다.
- 닉네임, 이메일, 비밀번호 재설정 이메일 검증 API에는 열거 공격 방지용 rate limit이 적용됩니다.
- 회원 탈퇴 후 30일 쿨다운 정리와 5년 후 익명화 배치를 지원합니다.

### 2.2 지갑 연동

- Phantom, Solflare, Backpack 등 Solana 지갑 연결을 지원합니다.
- 웹은 `@solana/wallet-adapter-*` 패키지를 사용합니다.
- Android는 Solana Mobile Wallet Adapter와 딥링크 커넥터를 함께 사용합니다.
- 지갑 주소를 계정에 연결하거나 연결 해제할 수 있습니다.
- Android WebView, Unity 실행을 위해 30초 TTL의 1회용 handoff/webview token을 발급합니다.

### 2.3 소비 내역 및 영수증 OCR

- 사용자는 소비 내역을 등록, 조회, 삭제할 수 있습니다.
- 영수증 이미지 업로드 시 AI 서버가 날짜, 총액, 승인번호, 영수증 여부, 손글씨 의심 여부 등을 추출합니다.
- 백엔드는 OCR 결과와 사용자가 입력한 금액/날짜를 비교해 검증합니다.
- 업로드 제한은 백엔드 OCR 라우트 기준 10MB입니다.

### 2.4 예산 및 가계부

- 예산 생성, 조회, 수정, 카테고리별 예산 수정을 제공합니다.
- AI 서버를 호출해 예산 계획을 자동 생성할 수 있습니다.

### 2.5 AI 분석 및 리포트

- AI 서버는 소비 텍스트를 분석해 점수, 카테고리, 패턴, 위험도, 조언을 JSON으로 반환합니다.
- 주간/월간 리포트 생성 기능을 제공합니다.
- x402 스타일 Solana 결제 검증을 통해 무료 사용량 초과 분석 결제를 처리합니다.
- 웹/Android 모두 분석 서버 또는 백엔드 프록시 API를 통해 AI 기능을 사용합니다.

### 2.6 커뮤니티

- 게시글 목록/상세/작성/수정/삭제를 지원합니다.
- 게시글 이미지 업로드를 지원합니다.
- 댓글 작성/수정/삭제를 지원합니다.
- 게시글 반응 등록/취소를 지원합니다.
- 부적절한 게시글/댓글 신고 API와 관리자 처리 플로우를 제공합니다.
- 콘테스트 목록 조회와 콘테스트 글쓰기 플로우를 제공합니다.

### 2.7 알림

- 알림 목록 조회, 단건 읽음 처리, 전체 읽음 처리를 지원합니다.
- 사용자 설정에서 예산, 보상, 스트릭, 전체 알림 수신 여부를 관리합니다.
- 매일 KST 21:00 스트릭 리마인드 배치를 실행합니다.

### 2.8 보상 및 성실도 점수

- 현재/과거 월간 성실도 점수와 주간 점수를 조회합니다.
- 월간 점수는 기록일수, 영수증, 일기, 예산, 스트릭 점수로 구성됩니다.
- 매일 KST 03:00 배치에서 이전 월 보상을 확정합니다.
- Unity 보상 박스 개수 조회와 박스 열기 API를 제공합니다.
- 콘테스트 종료 보상 배치를 통해 수상자에게 보상을 지급합니다.

### 2.9 아바타 및 NFT

- 사용자 아바타 아이템 목록, 게임용 아이템 목록, 장착 상태를 조회합니다.
- 아이템 장착/해제 기능을 제공합니다.
- 백엔드가 관리자 키페어로 아바타 NFT 민팅/전송을 수행합니다.
- 지갑 보유 NFT 동기화와 NFT 목록 조회를 지원합니다.
- Unity 인벤토리와 웹 아바타 페이지가 같은 백엔드 API를 사용합니다.

### 2.10 NFT 마켓

- 판매 등록 목록 조회, 판매 등록 생성, escrow 정보 갱신을 지원합니다.
- 판매 등록 취소와 pending listing 포기를 지원합니다.
- 구매 API는 온체인 거래 결과와 백엔드 거래 기록을 연결합니다.
- 프론트엔드는 Solana 트랜잭션을 생성/서명하고 백엔드에 결과를 전달합니다.
- Android 앱은 WebView로 웹 마켓을 열 수 있습니다.

### 2.11 관리자 콘솔

- 관리자 대시보드 통계와 트렌드 조회를 제공합니다.
- 신고 목록, 신고 대상 상세, 감사 로그, 처리/반려/제재 적용을 지원합니다.
- 사용자 목록, 탈퇴 사용자 목록, 사용자 활성화 상태 변경을 지원합니다.
- 공지사항 생성/수정/삭제를 지원합니다.
- 콘테스트 생성/수정/상태 변경을 지원합니다.
- 시스템 상태 공지를 수정할 수 있습니다.

## 3. 디렉토리 구조

```text
.
├── README.md
├── frontend/
│   ├── src/app/              # 앱 진입점, 라우터, Provider
│   ├── src/domains/          # 인증, 대시보드, 예산, 분석, 아바타, 마켓 등 도메인
│   ├── src/components/       # 챗봇, OCR, 리포트, 시스템 배너 등 공용 컴포넌트
│   ├── src/shared/           # API 클라이언트, 레이아웃, UI, hooks, lib
│   └── public/               # 이미지, favicon 등 정적 자산
├── backend/
│   ├── src/main.rs           # Axum 서버 진입점
│   ├── src/route.rs          # 전체 API 라우팅
│   ├── src/config.rs         # 서버 설정 로딩
│   ├── src/*/handler.rs      # 도메인별 HTTP 핸들러
│   ├── src/*/service.rs      # 도메인별 비즈니스 로직
│   ├── src/jobs/             # 배치 작업
│   └── sql/                  # DB 마이그레이션 SQL
├── ai-server/
│   └── app/
│       ├── main.py           # FastAPI 진입점
│       ├── api/v1/endpoints/ # AI/OCR/챗봇 엔드포인트
│       ├── services/         # AI 서비스 로직
│       └── clients/          # OpenAI 클라이언트
├── android/
│   └── app/src/main/java/
│       ├── com/ict/spentopia # 앱 엔트리, 네트워크, 로컬 DB, 테마
│       ├── feature/          # 화면별 Compose 기능
│       └── navigation/       # 앱 네비게이션
├── smartcontract/
│   ├── programs/spentopia/   # Anchor 프로그램
│   ├── scripts/              # 초기화/민팅 스크립트
│   └── tests/                # Anchor 테스트
└── unity/                    # Unity 연동 파일 영역
```

## 4. 웹 프론트엔드 명세

### 4.1 주요 라우트

| 경로 | 화면 |
| --- | --- |
| `/` | 대시보드 |
| `/budget` | 예산 관리 |
| `/analytics` | 소비 분석 |
| `/avatar`, `/profile/items`, `/avatar-items` | 아바타/아이템 |
| `/marketplace`, `/nft-market` | NFT 마켓 |
| `/profile` | 내 프로필 |
| `/community` | 커뮤니티 |
| `/plaza` | 광장 |
| `/guide` | 가이드 |
| `/admin` | 관리자 콘솔 |
| `/login`, `/signup`, `/complete-profile` | 인증/프로필 |
| `/forgot-password`, `/find-email`, `/reset-password` | 계정 찾기/비밀번호 재설정 |
| `/auth/kakao/callback`, `/auth/google/callback` | OAuth 콜백 |
| `/receipt-test` | 영수증 OCR 테스트 화면 |

### 4.2 주요 라이브러리

- React 18, TypeScript, Vite
- React Router
- Axios
- Tailwind CSS 4
- Radix UI, MUI, lucide-react
- Supabase JS
- Solana Web3, SPL Token, Anchor, wallet adapter
- jsPDF, html2canvas, Recharts

## 5. 백엔드 API 명세

### 5.1 서버 특성

- Axum 기반 HTTP API 서버입니다.
- `/health`, `/api/system/status`는 rate limit 없이 제공됩니다.
- Swagger UI는 `/swagger-ui`에서 확인할 수 있습니다.
- OpenAPI JSON은 `/api-docs/openapi.json`에서 제공합니다.
- CORS는 웹 refresh token 쿠키 전송을 위해 credentials를 허용합니다.
- 로그인 전 주요 API는 IP 기준 rate limit을 적용합니다.
- 로그인 후 일반 API는 user_id 기준 rate limit을 적용합니다.
- 파일 로그는 `logs/spentopia.log.YYYY-MM-DD` 형태로 error 이상을 기록합니다.

### 5.2 주요 API 그룹

| 그룹 | 주요 경로 | 설명 |
| --- | --- | --- |
| Health/System | `GET /health`, `GET /api/system/status` | 상태 확인, 시스템 공지 |
| Auth | `/auth/*` | 토큰 교환, refresh, logout, 카카오, 지갑 로그인, handoff |
| Profile/User | `/me`, `/profile/*`, `/api/user/*` | 내 정보, 프로필, 설정, 비밀번호 |
| Wallet | `/wallet/link`, `/wallet/unlink` | 지갑 연결/해제 |
| Expense | `/api/expenses`, `/api/receipt/ocr` | 소비 내역, 영수증 OCR 검증 |
| Budget | `/api/budget` | 예산 및 AI 예산 플랜 |
| Ledger | `/api/ledgers` | 가계부 |
| Community | `/api/posts`, `/api/comments`, `/api/contests`, `/api/content-reports` | 커뮤니티, 댓글, 신고 |
| Notification | `/api/notifications` | 알림 |
| Payment | `/api/payments` | 결제 생성/확인 |
| Report | `/api/reports` | 리포트 생성/조회 |
| Reward | `/api/rewards/*`, `/api/unity/rewards/*` | 성실도, 스트릭, 보상 박스 |
| Avatar | `/api/avatar/*`, `/api/unity/avatar/*` | 아바타 아이템, NFT, 장착 |
| Market | `/api/market/*` | NFT 판매/구매 |
| Admin | `/api/admin/*` | 관리자 대시보드, 신고, 유저, 공지, 콘테스트 |

## 6. AI 서버 명세

### 6.1 엔드포인트

| 경로 | 메서드 | 설명 |
| --- | --- | --- |
| `/` | GET | AI 서버 헬스체크 |
| `/api/v1/analyze/` | POST | 소비 텍스트 분석 |
| `/api/v1/analyze/report` | POST | 리포트 생성 |
| `/api/v1/history/` | GET | 분석 이력 조회용 |
| `/api/v1/chat`, `/api/v1/chat/` | POST | 소비 상담 챗봇 |
| `/api/v1/receipt/ocr` | POST | 영수증 이미지 OCR |
| `/api/v1/budget-plan` | POST | AI 예산 플랜 생성 |

## 7. Android 앱 명세

### 7.1 앱 정보

| 항목 | 값 |
| --- | --- |
| Application ID | `com.ict.spentopia` |
| minSdk | 26 |
| targetSdk | 35 |
| compileSdk | 36 |
| Kotlin | 2.0.21 |
| AGP | 8.9.1 |
| UI | Jetpack Compose |

### 7.2 주요 화면

| Route | 화면 |
| --- | --- |
| `splash` | 스플래시 |
| `login` | 로그인 |
| `find_email` | 이메일 찾기 |
| `find_password` | 비밀번호 찾기 |
| `home` | 홈 |
| `ledger` | 가계부 |
| `mypage` | 마이페이지 |
| `profile_avatar` | 프로필 + 아바타 |
| `budget` | 예산 |
| `analysis` | 소비 분석 |
| `avatar` | 아바타 |
| `market` | NFT 마켓 WebView |
| `plaza` | 광장 |
| `community` | 커뮤니티 |
| `chatbot` | AI 챗봇 |
| `community_write` | 커뮤니티 글쓰기 |

### 7.3 딥링크

| Scheme/Host | 용도 |
| --- | --- |
| `spentopia://wallet-callback` | Solana 지갑 콜백 |
| `spentopia://kakao-callback` | 백엔드 카카오 콜백 후 앱 복귀 |
| 카카오 SDK OAuth 콜백 | 카카오 SDK 로그인 콜백 |

## 8. Solana 스마트컨트랙트 명세

### 8.1 프로그램 정보

| 항목 | 값 |
| --- | --- |
| Framework | Anchor |
| Cluster | devnet |
| Program ID | `9s5Z96GSLVgVsnj5NAZ1HoxPvaF8Re8B1LeSmcBKQv61` |
| SPT decimals | 6 |
| 기본 수수료 예시 | 500 bps = 5% |

### 8.2 온체인 계정

| 계정 | 설명 |
| --- | --- |
| `PlatformConfig` | 관리자, 수수료율, SPT mint/authority bump, 총 민팅량, 최대 공급량, 컬렉션 mint 저장 |
| `ListingAccount` | 판매자, NFT mint, 가격, listing/escrow bump 저장 |

### 8.3 Instruction

| Instruction | 설명 |
| --- | --- |
| `init_spt_token` | SPT Token mint PDA와 메타데이터 초기화 |
| `init_platform` | 플랫폼 설정 PDA 초기화 |
| `init_avatar_collection` | 프로젝트 전용 아바타 컬렉션 NFT 초기화 |
| `mint_spt_to_user` | 관리자 권한으로 사용자에게 SPT 민팅 |
| `mint_avatar_nft` | 관리자 권한으로 아바타 NFT 민팅 |
| `list_nft` | 사용자가 NFT를 판매 등록하고 escrow에 예치 |
| `buy_nft` | SPT 결제 후 NFT를 구매자에게 이전 |
| `cancel_listing` | 판매 등록 취소 및 NFT 반환 |
| `transfer_avatar_nft` | 관리자 보유 NFT를 사용자에게 직접 전송 |

## 9. 데이터베이스 명세

### 9.1 Supabase 사용

- 백엔드는 Supabase service role key로 서버 권한 API를 호출합니다.
- 프론트엔드는 publishable key로 Supabase 클라이언트를 초기화합니다.
- 프로필 이미지와 커뮤니티 이미지는 Supabase Storage bucket을 사용합니다.
- `public.users`를 중심으로 인증 사용자, 지갑, 프로필, 설정, 탈퇴 상태를 관리합니다.

### 9.2 monthly_scores 테이블

`backend/sql/20260526_create_monthly_scores.sql`은 월간 성실도 점수 테이블을 생성합니다.

| 컬럼 | 설명 |
| --- | --- |
| `user_id` | 사용자 ID |
| `month_start` | 월 시작일. 반드시 1일 |
| `record_days_score` | 기록일수 점수, 0~30 |
| `receipt_score` | 영수증 점수, 0~25 |
| `diary_score` | 일기 점수, 0~20 |
| `budget_score` | 예산 점수, 0~15 |
| `streak_score` | 스트릭 점수, 0~10 |
| `total_score` | 총점, 0~100 |
| `reward_granted` | 보상 지급 여부 |
| `finalized_at` | 확정 시각 |

RLS는 본인 조회만 허용하며 insert/update/delete 정책은 만들지 않습니다. 점수 생성과 수정은 백엔드 service role에서만 수행합니다.

## 10. 배치 및 백그라운드 작업

| 작업 | 주기 | 설명 |
| --- | --- | --- |
| nonce 정리 | 5분마다 | 지갑 로그인 nonce 만료 데이터 제거 |
| handoff token 정리 | 30초마다 | Unity 1회용 로그인 교환권 제거 |
| webview token 정리 | 30초마다 | Android WebView 1회용 교환권 제거 |
| 스트릭 리마인드 | 매일 KST 21:00 | 기록 누락 방지 알림 생성 |
| 일일 정리 배치 | 매일 KST 03:00 | 월간 보상 확정, 콘테스트 보상, 탈퇴자 정리 |

## 11. 보안 및 운영 정책

- refresh token은 웹에서 HttpOnly 쿠키로 보관합니다.
- access token 만료 시 동시 401 요청은 프론트엔드 큐 패턴으로 refresh 1회만 수행합니다.
- 인증/열거 위험 API는 별도 rate limit을 적용합니다.
- 로그인 후 API는 user_id 기준 rate limit을 적용해 공용 IP 환경의 오탐을 줄입니다.
- CORS는 credentials 사용을 위해 허용 origin을 명시합니다.
- 민감 헤더와 토큰 값은 Android 디버그 네트워크 로그에서 마스킹합니다.
- 관리자 API는 JWT 검증 후 admin role 검증을 추가로 수행합니다.
- Solana 관리자 키페어와 Supabase service role key는 서버에서만 관리합니다.

