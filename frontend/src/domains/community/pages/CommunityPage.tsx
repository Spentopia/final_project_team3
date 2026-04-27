import { useState, useMemo } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import AiChatbotDialog from "@/components/chat/AiChatbotDialog";
import { MessageCircle, Search, Send } from "lucide-react";
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
}

// ── 탭 정의 ──────────────────────────────────────────────────

const TABS: { key: TabKey; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "contest", label: "아바타 콘테스트" },
  { key: "item", label: "이거 만들어주세요" },
  { key: "tip", label: "절약 꿀팁" },
  { key: "notice", label: "공지사항" },
];

// ── 뱃지 스타일 ───────────────────────────────────────────────

const BADGE_STYLE: Record<PostCategory, { bg: string; text: string; label: string }> = {
  notice:  { bg: "bg-sky-100 dark:bg-sky-900/40",    text: "text-sky-700 dark:text-sky-300",    label: "공지" },
  contest: { bg: "bg-yellow-100 dark:bg-yellow-900/40", text: "text-yellow-800 dark:text-yellow-300", label: "콘테스트" },
  item:    { bg: "bg-purple-100 dark:bg-purple-900/40", text: "text-purple-800 dark:text-purple-300", label: "아이템 요청" },
  tip:     { bg: "bg-rose-100 dark:bg-rose-900/40",   text: "text-rose-700 dark:text-rose-300",   label: "꿀팁" },
};

// ── 목 데이터 (실제 연동 시 API로 교체) ───────────────────────

const MOCK_POSTS: Post[] = [
  { id: 1,  category: "notice",  title: "4월 아바타 콘테스트 참여 방법 안내",               author: "운영자",      date: "PM 02:00",   isNew: true  },
  { id: 2,  category: "notice",  title: "Spentopia 이용약관 변경 안내",                     author: "운영자",      date: "2026.04.23", isNew: false },
  { id: 3,  category: "notice",  title: "서비스 점검 안내 (4/28 새벽 2시~4시)",             author: "운영자",      date: "2026.04.22", isNew: true  },
  { id: 4,  category: "notice",  title: "친구 초대 이벤트 — 초대당 SPT 100개 지급!",        author: "운영자",      date: "2026.04.18", isNew: false },
  { id: 5,  category: "contest", title: "나만의 힙스터 아바타 공개합니다 🕶️",               author: "패션왕",      date: "2시간 전",   isNew: true  },
  { id: 6,  category: "contest", title: "핑크핑크 러블리 공주님 컨셉 코디 🎀",              author: "큐티",        date: "5시간 전",   isNew: true  },
  { id: 7,  category: "contest", title: "RPG 용사 갑옷 세트 완성했어요 ⚔️",                author: "전사",        date: "1일 전",     isNew: false },
  { id: 8,  category: "contest", title: "봄봄 파스텔 아바타 어때요?",                       author: "봄이",        date: "2일 전",     isNew: false },
  { id: 9,  category: "item",    title: "이 운동화 아바타 아이템으로 만들어주세요!",          author: "스니커즈러버", date: "1시간 전",   isNew: true  },
  { id: 10, category: "item",    title: "제가 입은 체크무늬 코트 아이템화 부탁드려요 🧥",    author: "코트사랑",    date: "3시간 전",   isNew: true  },
  { id: 11, category: "item",    title: "고양이 귀 머리띠 아이템 추가해 주세요 🐱",          author: "냥집사",      date: "1일 전",     isNew: false },
  { id: 12, category: "item",    title: "할로윈 호박 모자 시즌 아이템으로 요청드려요 🎃",    author: "호박유령",    date: "3일 전",     isNew: false },
  { id: 13, category: "item",    title: "캐주얼 후드집업 아이템으로 만들어주세요",            author: "후디러버",    date: "5일 전",     isNew: false },
  { id: 14, category: "tip",     title: "집밥 도시락으로 월 20만원 절약하는 법 🍱",          author: "절약고수",    date: "2일 전",     isNew: false },
  { id: 15, category: "tip",     title: "대중교통 정기권으로 교통비 30% 아끼기",             author: "알뜰맨",      date: "3일 전",     isNew: false },
  { id: 16, category: "tip",     title: "안 쓰는 구독 서비스 정리하고 월 5만원 아끼기",      author: "구독킬러",    date: "5일 전",     isNew: false },
  { id: 17, category: "tip",     title: "카드 포인트 현금처럼 쓰는 꿀팁 총정리",            author: "포인트왕",    date: "1주 전",     isNew: false },
  { id: 18, category: "tip",     title: "이번 달 50만원으로 살기 성공 후기 + 비법 공개",     author: "절약왕",      date: "1주 전",     isNew: false },
];

const PER_PAGE = 10;

// ── 컴포넌트 ─────────────────────────────────────────────────

export default function Community() {
  const [activeTab, setActiveTab] = useState<TabKey>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);

  // 필터링
  const filtered = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return MOCK_POSTS.filter((p) => {
      const tabMatch = activeTab === "all" || p.category === activeTab;
      const searchMatch = !q || p.title.toLowerCase().includes(q);
      return tabMatch && searchMatch;
    });
  }, [activeTab, searchQuery]);

  // 페이지네이션
  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const paginated = filtered.slice((currentPage - 1) * PER_PAGE, currentPage * PER_PAGE);

  const handleTabChange = (tab: TabKey) => {
    setActiveTab(tab);
    setCurrentPage(1);
    setSearchQuery("");
  };

  const handleSearch = (value: string) => {
    setSearchQuery(value);
    setCurrentPage(1);
  };

  const handleWrite = () => {
    toast.info("글쓰기 기능은 준비 중입니다");
  };

  return (
      <div className="space-y-6">
        {/* 헤더 */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
              커뮤니티
            </h1>
            <p className="text-gray-600 dark:text-gray-300">
              다른 사용자들과 소통하고 경험을 나눠보세요
            </p>
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
                  className="pl-9 w-56 h-9 text-sm"
                  value={searchQuery}
                  onChange={(e) => handleSearch(e.target.value)}
              />
            </div>
            <Button
                onClick={handleWrite}
                className="bg-gradient-to-r from-cyan-500 to-blue-500 h-9 text-sm"
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
                    className={`px-5 py-2.5 text-sm border-b-2 -mb-px transition-colors whitespace-nowrap ${
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
                          className="flex items-center gap-3 px-4 py-3.5 border-b last:border-b-0 border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 cursor-pointer transition-colors"
                      >
                        {/* 뱃지 */}
                        <span
                            className={`inline-flex items-center justify-center px-2 py-0.5 rounded text-[11px] font-medium flex-shrink-0 min-w-[58px] ${badge.bg} ${badge.text}`}
                        >
                    {badge.label}
                  </span>

                        {/* 제목 */}
                        <span className="flex-1 flex items-center gap-1.5 min-w-0">
                    <span className="text-sm text-gray-900 dark:text-gray-100 truncate">
                      {post.title}
                    </span>
                          {post.isNew && (
                              <span className="w-1.5 h-1.5 rounded-full bg-orange-400 flex-shrink-0 inline-block" />
                          )}
                  </span>

                        {/* 작성자 · 날짜 */}
                        <span className="text-xs text-gray-400 dark:text-gray-500 flex-shrink-0 min-w-[100px] text-right">
                    {post.author} · {post.date}
                  </span>
                      </div>
                  );
                })
            )}
          </div>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
              <div className="mt-4 flex items-center justify-center gap-1">
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                    <button
                        key={page}
                        onClick={() => setCurrentPage(page)}
                        className={`w-8 h-8 flex items-center justify-center rounded-lg text-sm border transition-colors ${
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