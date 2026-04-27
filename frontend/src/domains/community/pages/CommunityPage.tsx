import { useState, useMemo, useEffect } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import AiChatbotDialog from "@/components/chat/AiChatbotDialog";
import { MessageCircle, Search, Send, Eye, ChevronUp, ChevronDown, Link2 } from "lucide-react";
import { toast } from "sonner";

// ── 타입 ─────────────────────────────────────────────────────

type PostCategory = "notice" | "contest" | "item" | "tip";
type TabKey = "all" | PostCategory;

interface Post {
  id: number;
  category: PostCategory;
  title: string;
  author: string;
  date: string;
  isNew?: boolean;
  views: number;
  content: string;
}

// ── 탭 정의 ──────────────────────────────────────────────────

const TABS: { key: TabKey; label: string }[] = [
  { key: "all",     label: "전체" },
  { key: "contest", label: "아바타 콘테스트" },
  { key: "item",    label: "이거 만들어주세요" },
  { key: "tip",     label: "절약 꿀팁" },
  { key: "notice",  label: "공지사항" },
];

// ── 뱃지 스타일 ───────────────────────────────────────────────

const BADGE_STYLE: Record<PostCategory, { bg: string; text: string; label: string }> = {
  notice:  { bg: "bg-sky-100 dark:bg-sky-900/40",       text: "text-sky-700 dark:text-sky-300",       label: "공지" },
  contest: { bg: "bg-yellow-100 dark:bg-yellow-900/40", text: "text-yellow-800 dark:text-yellow-300", label: "콘테스트" },
  item:    { bg: "bg-purple-100 dark:bg-purple-900/40", text: "text-purple-800 dark:text-purple-300", label: "아이템 요청" },
  tip:     { bg: "bg-rose-100 dark:bg-rose-900/40",     text: "text-rose-700 dark:text-rose-300",     label: "꿀팁" },
};

// ── 목 데이터 (실제 연동 시 API로 교체) ───────────────────────

const MOCK_POSTS: Post[] = [
  {
    id: 1, category: "notice", title: "4월 아바타 콘테스트 참여 방법 안내",
    author: "운영자", date: "2026.04.27", isNew: true, views: 1024,
    content: `안녕하세요, Spentopia 운영팀입니다.

4월 아바타 콘테스트 참여 방법을 안내드립니다.

[참여 방법]
1. 커뮤니티 > 아바타 콘테스트 탭으로 이동합니다.
2. 글쓰기 버튼을 눌러 본인의 아바타 스크린샷을 첨부합니다.
3. 제목과 간단한 소개를 작성하고 게시하면 참여 완료!

[심사 기준]
○ 좋아요 수 (게시물 좋아요로 투표)
○ 콘테스트 기간: 2026년 4월 1일 ~ 4월 30일

[보상]
- 1위: SPT 500개 + 한정 아바타 1종
- 2위: SPT 300개
- 3위: SPT 100개

많은 참여 부탁드립니다!`,
  },
  {
    id: 2, category: "notice", title: "Spentopia 이용약관 변경 안내",
    author: "운영자", date: "2026.04.23", isNew: false, views: 812,
    content: `안녕하세요, Spentopia 운영팀입니다.

이용약관이 2026년 5월 28일자로 변경됩니다.
회원 여러분께서는 아래의 내용을 참고하시어 서비스 이용에 불편이 없으시길 바랍니다.

[변경 내용]
○ 제10조 (환불 정책 개정)
- 기존 환불 조항에 세부 항목 추가

○ 시행 일시: 2026년 5월 28일 (금)
- 시행 전까지 이용약관에 별도 의사표시가 없을 경우 동의한 것으로 보아 개정된 이용약관이 적용됩니다.

이용에 불편이 없으시길 바랍니다.`,
  },
  {
    id: 3, category: "notice", title: "서비스 점검 안내 (4/28 새벽 2시~4시)",
    author: "운영자", date: "2026.04.22", isNew: true, views: 634,
    content: `안녕하세요, Spentopia 운영팀입니다.

안정적인 서비스 제공을 위해 아래와 같이 서버 점검을 진행합니다.

[점검 일정]
- 일시: 2026년 4월 28일 (화) 새벽 02:00 ~ 04:00 (약 2시간)
- 점검 유형: 정기 서버 점검 및 보안 패치

[점검 중 제한 사항]
- 서비스 전체 이용 불가 (로그인, 가계부 기록 등)

점검 시간 동안 불편을 드려 죄송합니다. 더 나은 서비스로 보답하겠습니다.`,
  },
  {
    id: 4, category: "notice", title: "친구 초대 이벤트 — 초대당 SPT 100개 지급!",
    author: "운영자", date: "2026.04.18", isNew: false, views: 2341,
    content: `안녕하세요, Spentopia 운영팀입니다.

친구를 초대하고 SPT를 받아가세요!

[이벤트 내용]
- 초대한 친구가 회원가입 완료 시 초대자에게 SPT 100개 지급
- 초대받은 친구에게도 가입 축하 SPT 50개 지급
- 초대 인원 제한 없음 (무제한 적립 가능)

[참여 방법]
마이페이지 > 친구 초대 메뉴에서 초대 링크를 복사하여 공유하세요.

[이벤트 기간]
2026년 4월 18일 ~ 2026년 5월 31일`,
  },
  {
    id: 5, category: "contest", title: "나만의 힙스터 아바타 공개합니다 🕶️",
    author: "패션왕", date: "2026.04.27", isNew: true, views: 318,
    content: `안녕하세요! 드디어 완성한 힙스터 컨셉 아바타를 공개합니다.

베레모 + 선글라스 + 오버핏 자켓 조합으로 만들었어요.
아이템 조합이 생각보다 잘 어울려서 너무 만족스러워요 😎

좋아요 눌러주시면 감사하겠습니다!`,
  },
  {
    id: 6, category: "contest", title: "핑크핑크 러블리 공주님 컨셉 코디 🎀",
    author: "큐티", date: "2026.04.27", isNew: true, views: 245,
    content: `핑크 드레스에 왕관까지 완성한 공주님 컨셉이에요~!

분홍색 계열 아이템만 모아서 코디해봤는데 생각보다 이쁘게 나왔어요 ㅎㅎ
투표 많이 해주세요! 🌸`,
  },
  {
    id: 7, category: "contest", title: "RPG 용사 갑옷 세트 완성했어요 ⚔️",
    author: "전사", date: "2026.04.26", isNew: false, views: 189,
    content: `RPG 게임 주인공처럼 갑옷 풀세트를 맞췄습니다.

투구 + 갑옷 + 방패 + 검 조합으로 완성!
용사 느낌 물씬 나지 않나요? 😄`,
  },
  {
    id: 8, category: "contest", title: "봄봄 파스텔 아바타 어때요?",
    author: "봄이", date: "2026.04.25", isNew: false, views: 156,
    content: `봄 느낌 물씬 나는 파스텔 컬러 아바타예요!

연두색 원피스에 꽃 머리핀 조합이 포인트입니다 🌷
좋아요 많이 눌러주세요~`,
  },
  {
    id: 9, category: "item", title: "이 운동화 아바타 아이템으로 만들어주세요!",
    author: "스니커즈러버", date: "2026.04.27", isNew: true, views: 412,
    content: `사진 첨부했습니다!

제가 요즘 너무 좋아하는 흰색 청키 스니커즈인데요,
아바타 아이템으로 있으면 진짜 매일 신을 것 같아요 👟

비슷한 스타일 원하시는 분들 좋아요 눌러주세요!`,
  },
  {
    id: 10, category: "item", title: "제가 입은 체크무늬 코트 아이템화 부탁드려요 🧥",
    author: "코트사랑", date: "2026.04.27", isNew: true, views: 287,
    content: `겨울마다 꺼내 입는 블랙 체크 코트예요.

아바타한테도 입혀주고 싶어서 요청드려요!
클래식한 느낌이라 다양한 코디에 어울릴 것 같아요 🖤`,
  },
  {
    id: 11, category: "item", title: "고양이 귀 머리띠 아이템 추가해 주세요 🐱",
    author: "냥집사", date: "2026.04.26", isNew: false, views: 534,
    content: `고양이 귀 머리띠가 너무 갖고 싶어요!

분홍색이랑 검정색 두 가지 색상으로 만들어주시면 더 좋을 것 같아요.
냥집사 여러분 같이 투표해요~ 🐾`,
  },
  {
    id: 12, category: "item", title: "할로윈 호박 모자 시즌 아이템으로 요청드려요 🎃",
    author: "호박유령", date: "2026.04.24", isNew: false, views: 198,
    content: `할로윈 시즌 한정으로 호박 모자 아이템 추가해주세요!

매년 할로윈에 꺼내 쓸 수 있게 시즌 아이템으로 만들어주시면 좋겠어요.
다음 할로윈 준비 미리미리 해요 👻`,
  },
  {
    id: 13, category: "item", title: "캐주얼 후드집업 아이템으로 만들어주세요",
    author: "후디러버", date: "2026.04.22", isNew: false, views: 167,
    content: `편안한 후드집업 아이템이 있었으면 해요.

회색이나 네이비 계열로 만들어주시면 데일리 코디로 딱일 것 같아요!
집에서 입는 느낌 물씬 나는 편한 스타일이요 😊`,
  },
  {
    id: 14, category: "tip", title: "집밥 도시락으로 월 20만원 절약하는 법 🍱",
    author: "절약고수", date: "2026.04.25", isNew: false, views: 1823,
    content: `외식 대신 도시락을 싸면 한 달에 평균 20만원을 아낄 수 있어요.

[핵심 팁]
1. 주말에 반찬 5가지 미리 만들어두기
2. 냉동 보관 가능한 반찬 위주로 구성
3. 장볼 때 주간 메뉴 미리 계획하고 장보기

저는 이 방법으로 3개월째 식비를 30만원 이하로 유지 중이에요!
처음엔 귀찮지만 익숙해지면 오히려 더 편해요 😄`,
  },
  {
    id: 15, category: "tip", title: "대중교통 정기권으로 교통비 30% 아끼기",
    author: "알뜰맨", date: "2026.04.24", isNew: false, views: 967,
    content: `매일 출퇴근하시나요? 정기권을 이용하면 교통비를 30% 이상 절약할 수 있어요.

[정기권 종류]
- 지하철 정기권: 44회 사용 가능, 약 15% 절약
- 버스+지하철 환승 정기권: 구간에 따라 최대 30% 절약

한 달 기준 약 3~5만원 절약 효과가 있어요.
티끌 모아 태산이라고, 1년이면 36~60만원이에요!`,
  },
  {
    id: 16, category: "tip", title: "안 쓰는 구독 서비스 정리하고 월 5만원 아끼기",
    author: "구독킬러", date: "2026.04.22", isNew: false, views: 756,
    content: `OTT, 음악 스트리밍, 클라우드 등 안 쓰는 구독은 과감히 정리하세요!

[구독 점검 방법]
1. 신용카드 명세서에서 정기 결제 항목 전부 확인
2. 최근 한 달간 실제 사용 여부 체크
3. 안 쓴 것은 즉시 해지

저는 이 방법으로 월 5만 3천원을 아꼈어요.
1년이면 63만원이에요! 작은 것들이 쌓이면 엄청나죠 💪`,
  },
  {
    id: 17, category: "tip", title: "카드 포인트 현금처럼 쓰는 꿀팁 총정리",
    author: "포인트왕", date: "2026.04.21", isNew: false, views: 1234,
    content: `쌓인 카드 포인트, 그냥 두지 마세요!

[포인트 활용법]
1. 통신비 자동납부 할인 전환 (매달 자동 차감)
2. 편의점/마트 할인 쿠폰으로 전환
3. 카드사 앱에서 현금 캐시백으로 전환
4. 항공 마일리지로 전환 (일부 카드 가능)

저는 방치해뒀던 포인트로 작년에 항공권 보조금으로 15만원 썼어요!`,
  },
  {
    id: 18, category: "tip", title: "이번 달 50만원으로 살기 성공 후기 + 비법 공개",
    author: "절약왕", date: "2026.04.20", isNew: false, views: 2156,
    content: `드디어 한 달 50만원으로 생활하기 성공했습니다! 🎉

[항목별 지출 내역]
- 식비: 18만원 (자취 + 도시락 병행)
- 교통비: 6만원 (정기권 사용)
- 통신비: 3만원 (알뜰폰 전환)
- 생활용품: 3만원
- 여가/문화: 5만원
- 예비비: 15만원

포인트는 딱 필요한 것만 사고, 외식은 주 1회로 제한했어요.
처음엔 너무 힘들었는데 3개월 지나니 오히려 즐거워요!`,
  },
];

const PER_PAGE = 10;

// ── 컴포넌트 ─────────────────────────────────────────────────

export default function Community() {
  const [activeTab, setActiveTab]         = useState<TabKey>("all");
  const [searchQuery, setSearchQuery]     = useState("");
  const [currentPage, setCurrentPage]     = useState(1);
  const [selectedPost, setSelectedPost]   = useState<Post | null>(null);
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);

  // 필터링
  const filtered = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return MOCK_POSTS.filter((p) => {
      const tabMatch    = activeTab === "all" || p.category === activeTab;
      const searchMatch = !q || p.title.toLowerCase().includes(q);
      return tabMatch && searchMatch;
    });
  }, [activeTab, searchQuery]);

  // 페이지네이션
  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const paginated  = filtered.slice((currentPage - 1) * PER_PAGE, currentPage * PER_PAGE);

  // 이전글 / 다음글 (전체 목록 id 순서 기준)
  const allIds       = MOCK_POSTS.map((p) => p.id);
  const currentIdx   = selectedPost ? allIds.indexOf(selectedPost.id) : -1;
  const prevPost     = currentIdx > 0 ? MOCK_POSTS[currentIdx - 1] : null;
  const nextPost     = currentIdx < MOCK_POSTS.length - 1 ? MOCK_POSTS[currentIdx + 1] : null;

  const handleTabChange = (tab: TabKey) => {
    setActiveTab(tab);
    setCurrentPage(1);
    setSearchQuery("");
    setSelectedPost(null);
  };

  const handleSearch = (value: string) => {
    setSearchQuery(value);
    setCurrentPage(1);
    setSelectedPost(null);
  };

  const handlePostClick = (post: Post) => {
    setSelectedPost(post);
    window.history.pushState({ postId: post.id }, "", "");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // 브라우저 뒤로가기 → 목록으로
  useEffect(() => {
    const handlePopState = () => {
      setSelectedPost(null);
    };
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  // ── 상세 뷰 ──────────────────────────────────────────────────
  if (selectedPost) {
    const badge = BADGE_STYLE[selectedPost.category];

    return (
        <div className="space-y-6">
          {/* 헤더 */}
          <div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">커뮤니티</h1>
            <p className="text-gray-600 dark:text-gray-300">다른 사용자들과 소통하고 경험을 나눠보세요</p>
          </div>

          <Card className="border-none bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl overflow-hidden">
            {/* 게시글 헤더 */}
            <div className="px-8 pt-8 pb-6 border-b border-gray-100 dark:border-gray-700">
              <div className="flex items-center gap-2 mb-3">
              <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium ${badge.bg} ${badge.text}`}>
                {badge.label}
              </span>
              </div>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-5 leading-snug">
                {selectedPost.title}
              </h2>
              <div className="flex items-center justify-between flex-wrap gap-2">
              <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
                {selectedPost.author}
              </span>
                <div className="flex items-center gap-4 text-sm text-gray-400 dark:text-gray-500">
                <span className="flex items-center gap-1">
                  <Eye className="h-3.5 w-3.5" />
                  {selectedPost.views.toLocaleString()}
                </span>
                  <span>{selectedPost.date}</span>
                  <button
                      onClick={() => {
                        void navigator.clipboard.writeText(window.location.href);
                        toast.success("링크가 복사되었습니다");
                      }}
                      className="flex items-center gap-1 hover:text-cyan-500 transition-colors"
                  >
                    <Link2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>

            {/* 본문 */}
            <div className="px-8 py-8 min-h-[240px]">
              <p className="text-base text-gray-700 dark:text-gray-300 leading-8 whitespace-pre-wrap">
                {selectedPost.content}
              </p>
            </div>

            {/* 이전글 / 다음글 */}
            <div className="border-t border-gray-100 dark:border-gray-700">
              {nextPost && (
                  <button
                      onClick={() => handlePostClick(nextPost)}
                      className="w-full flex items-center gap-4 px-8 py-4 border-b border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-left"
                  >
                <span className="flex items-center gap-1.5 text-sm text-gray-400 dark:text-gray-500 flex-shrink-0 w-16">
                  <ChevronUp className="h-4 w-4" />
                  다음글
                </span>
                    <span className="flex-1 text-base text-gray-700 dark:text-gray-300 truncate">
                  {nextPost.title}
                </span>
                    <span className="text-sm text-gray-400 dark:text-gray-500 flex-shrink-0">
                  {nextPost.date}
                </span>
                  </button>
              )}
              {prevPost && (
                  <button
                      onClick={() => handlePostClick(prevPost)}
                      className="w-full flex items-center gap-4 px-8 py-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-left"
                  >
                <span className="flex items-center gap-1.5 text-sm text-gray-400 dark:text-gray-500 flex-shrink-0 w-16">
                  <ChevronDown className="h-4 w-4" />
                  이전글
                </span>
                    <span className="flex-1 text-base text-gray-700 dark:text-gray-300 truncate">
                  {prevPost.title}
                </span>
                    <span className="text-sm text-gray-400 dark:text-gray-500 flex-shrink-0">
                  {prevPost.date}
                </span>
                  </button>
              )}
            </div>

            {/* 목록으로 */}
            <div className="px-8 py-5 flex justify-center border-t border-gray-100 dark:border-gray-700">
              <Button
                  variant="outline"
                  onClick={() => window.history.back()}
                  className="px-12 h-11 text-base"
              >
                목록으로
              </Button>
            </div>
          </Card>

          <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />
        </div>
    );
  }

  // ── 목록 뷰 ──────────────────────────────────────────────────
  return (
      <div className="space-y-6">
        {/* 헤더 */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">커뮤니티</h1>
            <p className="text-gray-600 dark:text-gray-300">다른 사용자들과 소통하고 경험을 나눠보세요</p>
          </div>
        </div>

        {/* 게시판 */}
        <div>
          {/* 검색 + 글쓰기 */}
          <div className="mb-4 flex items-center justify-between gap-3 flex-wrap">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                  placeholder="제목 검색"
                  className="pl-10 w-64 h-11 text-base"
                  value={searchQuery}
                  onChange={(e) => handleSearch(e.target.value)}
              />
            </div>
            <Button
                onClick={() => toast.info("글쓰기 기능은 준비 중입니다")}
                className="bg-gradient-to-r from-cyan-500 to-blue-500 h-11 text-base px-5"
            >
              <Send className="mr-2 h-4 w-4" />
              글쓰기
            </Button>
          </div>

          {/* 탭 바 */}
          <div className="flex border-b border-gray-200 dark:border-gray-700">
            {TABS.map((tab) => (
                <button
                    key={tab.key}
                    onClick={() => handleTabChange(tab.key)}
                    className={`px-6 py-3.5 text-base border-b-2 -mb-px transition-colors whitespace-nowrap ${
                        activeTab === tab.key
                            ? "border-cyan-500 text-cyan-600 dark:text-cyan-400 font-medium"
                            : "border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
                    }`}
                >
                  {tab.label}
                </button>
            ))}
          </div>

          {/* 게시글 리스트 */}
          <div className="border border-t-0 border-gray-200 dark:border-gray-700 rounded-b-xl overflow-hidden bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl">
            {paginated.length === 0 ? (
                <div className="py-12 text-center text-sm text-gray-400 dark:text-gray-500">
                  검색 결과가 없습니다
                </div>
            ) : (
                paginated.map((post) => {
                  const badge = BADGE_STYLE[post.category];
                  return (
                      <div
                          key={post.id}
                          onClick={() => handlePostClick(post)}
                          className="flex items-center gap-4 px-6 py-5 border-b last:border-b-0 border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 cursor-pointer transition-colors"
                      >
                        {/* 뱃지 */}
                        <span className={`inline-flex items-center justify-center px-3 py-1 rounded text-xs font-medium flex-shrink-0 min-w-[68px] ${badge.bg} ${badge.text}`}>
                    {badge.label}
                  </span>

                        {/* 제목 */}
                        <span className="flex-1 flex items-center gap-2 min-w-0">
                    <span className="text-base text-gray-900 dark:text-gray-100 truncate">
                      {post.title}
                    </span>
                          {post.isNew && (
                              <span className="w-2 h-2 rounded-full bg-orange-400 flex-shrink-0 inline-block" />
                          )}
                  </span>

                        {/* 조회수 */}
                        <span className="flex items-center gap-1 text-sm text-gray-400 dark:text-gray-500 flex-shrink-0">
                    <Eye className="h-3.5 w-3.5" />
                          {post.views.toLocaleString()}
                  </span>

                        {/* 작성자 · 날짜 */}
                        <span className="text-sm text-gray-400 dark:text-gray-500 flex-shrink-0 min-w-[130px] text-right">
                    {post.author} · {post.date}
                  </span>
                      </div>
                  );
                })
            )}
          </div>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
              <div className="mt-5 flex items-center justify-center gap-1.5">
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                    <button
                        key={page}
                        onClick={() => setCurrentPage(page)}
                        className={`w-10 h-10 flex items-center justify-center rounded-lg text-sm border transition-colors ${
                            page === currentPage
                                ? "bg-cyan-500 text-white border-cyan-500 font-medium"
                                : "bg-white dark:bg-gray-800 text-gray-500 dark:text-gray-400 border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700"
                        }`}
                    >
                      {page}
                    </button>
                ))}
              </div>
          )}
        </div>

        <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />
      </div>
  );
}