import { useState, useMemo, useEffect, type FormEvent } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import AiChatbotDialog from "@/components/chat/AiChatbotDialog";
import { Search, Send, Eye, Heart, ChevronUp, ChevronDown, Link2, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { AxiosError } from "axios";
import {
  createCommunityPost,
  deleteCommunityPost,
  getCommunityMe,
  getCommunityPost,
  listCommunityPosts,
  listContests,
  updateCommunityPost,
  uploadCommunityImage,
  type ContestResponse,
  type CommunityPostResponse,
  type PostType,
} from "@/domains/community/api/communityApi";

// ── 타입 ─────────────────────────────────────────────────────

type PostCategory = "notice" | "contest" | "item" | "tip";
type TabKey = "all" | PostCategory;

interface Post {
  id: string;
  user_id: string;
  contest_id: string | null;
  category: PostCategory;
  title: string;
  author: string;
  authorProfileImageUrl: string | null;
  date: string;
  isNew?: boolean;
  likes: number;
  views: number;
  content: string;
  image_url?: string | null;
}

// ── 탭 정의 ──────────────────────────────────────────────────

const TABS: { key: TabKey; label: string }[] = [
  { key: "all",     label: "전체" },
  { key: "notice",  label: "공지사항" },
  { key: "contest", label: "아바타 콘테스트" },
  { key: "item",    label: "이거 만들어주세요" },
  { key: "tip",     label: "절약 꿀팁" },
];

// ── 뱃지 스타일 ───────────────────────────────────────────────

const BADGE_STYLE: Record<PostCategory, { bg: string; text: string; label: string }> = {
  notice:  { bg: "bg-sky-100 dark:bg-sky-900/40",       text: "text-sky-700 dark:text-sky-300",       label: "공지" },
  contest: { bg: "bg-yellow-100 dark:bg-yellow-900/40", text: "text-yellow-800 dark:text-yellow-300", label: "콘테스트" },
  item:    { bg: "bg-purple-100 dark:bg-purple-900/40", text: "text-purple-800 dark:text-purple-300", label: "아이템 요청" },
  tip:     { bg: "bg-rose-100 dark:bg-rose-900/40",     text: "text-rose-700 dark:text-rose-300",     label: "꿀팁" },
};

const PER_PAGE = 10;

function formatPostDate(value: string | null): string {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}.${month}.${day}`;
}

function isNewPost(value: string | null): boolean {
  if (!value) return false;

  const createdAt = new Date(value).getTime();
  if (Number.isNaN(createdAt)) return false;

  return Date.now() - createdAt <= 3 * 24 * 60 * 60 * 1000;
}

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL as string;
const COMMUNITY_BUCKET = "posts";
const COMMUNITY_READ_POSTS_KEY_PREFIX = "community:read-posts";

function buildImageUrl(path: string | null | undefined): string | null {
  if (!path) return null;
  if (path.startsWith("http")) return path;
  return `${SUPABASE_URL}/storage/v1/object/public/${COMMUNITY_BUCKET}/${path}`;
}

function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof AxiosError) {
    const message = error.response?.data;
    if (typeof message === "string" && message.trim()) {
      return message;
    }
  }

  return fallback;
}

function toPost(post: CommunityPostResponse): Post {
  return {
    id: post.id,
    user_id: post.user_id,
    contest_id: post.contest_id,
    category: post.post_type === "request" ? "item" : post.post_type,
    title: post.title,
    author: post.author_nickname ?? "익명",
    authorProfileImageUrl: post.author_profile_image_url,
    date: formatPostDate(post.created_at),
    isNew: isNewPost(post.created_at),
    likes: post.vote_count ?? 0,
    views: post.view_count,
    content: post.content ?? "",
    image_url: buildImageUrl(post.image_url),
  };
}

// ── 컴포넌트 ─────────────────────────────────────────────────

export default function Community() {
  const [activeTab, setActiveTab]         = useState<TabKey>("all");
  const [searchQuery, setSearchQuery]     = useState("");
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState("");
  const [currentPage, setCurrentPage]     = useState(1);
  const [posts, setPosts]                 = useState<Post[]>([]);
  const [totalCount, setTotalCount]       = useState(0);
  const [selectedPost, setSelectedPost]   = useState<Post | null>(null);
  const [readPostIds, setReadPostIds]     = useState<Set<string>>(() => new Set());
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);
  const [isWriteOpen, setIsWriteOpen]     = useState(false);
  const [isSubmitting, setIsSubmitting]   = useState(false);
  const [contests, setContests]           = useState<ContestResponse[]>([]);
  const [myUserId, setMyUserId]           = useState("");
  const [myRoleType, setMyRoleType]       = useState("user");
  const [writeType, setWriteType]         = useState<PostType>("request");
  const [writeTitle, setWriteTitle]       = useState("");
  const [writeContent, setWriteContent]   = useState("");
  const [writeContestId, setWriteContestId] = useState("");
  const [writeFile, setWriteFile]         = useState<File | null>(null);
  const [isEditOpen, setIsEditOpen]       = useState(false);
  const [editTitle, setEditTitle]         = useState("");
  const [editContent, setEditContent]     = useState("");
  const [editFile, setEditFile]           = useState<File | null>(null);

  const readPostsStorageKey = myUserId
    ? `${COMMUNITY_READ_POSTS_KEY_PREFIX}:${myUserId}`
    : "";

  useEffect(() => {
    let ignore = false;

    async function fetchMe() {
      try {
        const data = await getCommunityMe();
        if (!ignore) {
          setMyUserId(data.id);
          setMyRoleType(data.role_type ?? "user");
        }
      } catch (error) {
        if (!ignore) {
          console.error("내 권한 조회 실패:", error);
        }
      }
    }

    void fetchMe();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!readPostsStorageKey) return;

    try {
      const stored = window.localStorage.getItem(readPostsStorageKey);
      const ids = stored ? (JSON.parse(stored) as unknown) : [];

      if (Array.isArray(ids)) {
        setReadPostIds(new Set(ids.filter((id): id is string => typeof id === "string")));
      }
    } catch (error) {
      console.error("읽은 게시글 목록 로드 실패:", error);
      setReadPostIds(new Set());
    }
  }, [readPostsStorageKey]);

  useEffect(() => {
    let ignore = false;

    async function fetchPosts() {
      try {
        const data = await listCommunityPosts({
          sort: "date",
          title: debouncedSearchQuery || undefined,
          page: currentPage,
          pageSize: PER_PAGE,
        });
        if (!ignore) {
          setPosts(data.items.map(toPost));
          setTotalCount(data.total_count);
        }
      } catch (error) {
        if (!ignore) {
          toast.error("게시글 목록을 불러오지 못했습니다");
          console.error("게시글 목록 조회 실패:", error);
        }
      }
    }

    void fetchPosts();

    return () => {
      ignore = true;
    };
  }, [currentPage, debouncedSearchQuery]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim());
      setCurrentPage(1);
    }, 300);

    return () => window.clearTimeout(timer);
  }, [searchQuery]);

  useEffect(() => {
    let ignore = false;

    async function fetchContests() {
      try {
        const data = await listContests();
        if (!ignore) {
          setContests(data);
          setWriteContestId((prev) => prev || data[0]?.id || "");
        }
      } catch (error) {
        if (!ignore) {
          console.error("콘테스트 목록 조회 실패:", error);
        }
      }
    }

    void fetchContests();

    return () => {
      ignore = true;
    };
  }, []);

  // 필터링
  const filtered = useMemo(() => {
    return posts.filter((p) => {
      const tabMatch    = activeTab === "all" || p.category === activeTab;
      return tabMatch;
    });
  }, [activeTab, posts]);

  // 페이지네이션
  const totalPages = Math.max(1, Math.ceil(totalCount / PER_PAGE));
  const paginated  = filtered;

  // 이전글 / 다음글 (전체 목록 id 순서 기준)
  const allIds       = posts.map((p) => p.id);
  const currentIdx   = selectedPost ? allIds.indexOf(selectedPost.id) : -1;
  const prevPost     = currentIdx > 0 ? posts[currentIdx - 1] : null;
  const nextPost     = currentIdx < posts.length - 1 ? posts[currentIdx + 1] : null;

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

  const handlePostClick = async (post: Post) => {
    setReadPostIds((prev) => {
      const next = new Set(prev);
      next.add(post.id);

      if (readPostsStorageKey) {
        window.localStorage.setItem(readPostsStorageKey, JSON.stringify([...next]));
      }

      return next;
    });

    try {
      const detail = toPost(await getCommunityPost(post.id));
      setPosts((prev) => prev.map((item) => (item.id === detail.id ? detail : item)));
      setSelectedPost(detail);
    } catch (error) {
      setSelectedPost(post);
      toast.error("게시글 상세 조회에 실패했습니다");
      console.error("게시글 상세 조회 실패:", error);
    }

    window.history.pushState({ postId: post.id }, "", "");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const resetWriteForm = () => {
    setWriteType("request");
    setWriteTitle("");
    setWriteContent("");
    setWriteContestId(contests[0]?.id || "");
    setWriteFile(null);
  };

  const handleWriteSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const title = writeTitle.trim();
    const content = writeContent.trim();

    if (!title) {
      toast.error("제목을 입력해주세요");
      return;
    }

    if (writeType === "notice" && myRoleType !== "admin") {
      toast.error("운영자만 가능합니다");
      return;
    }

    if (writeType === "contest" && !writeContestId) {
      toast.error("콘테스트를 선택해주세요");
      return;
    }

    setIsSubmitting(true);

    try {
      let created: CommunityPostResponse;

      if (writeType === "notice") {
        created = await createCommunityPost({
          post_type: "notice",
          title,
          contest_id: null,
          image_url: null,
          content: content || null,
        });

        if (writeFile) {
          const uploaded = await uploadCommunityImage({
            file: writeFile,
            target: { postType: "notice", postId: created.id },
          });
          created = await updateCommunityPost(created.id, { image_url: uploaded.path });
        }
      } else {
        let imagePath: string | null = null;

        if (writeFile) {
          const uploaded = await uploadCommunityImage({
            file: writeFile,
            target:
              writeType === "contest"
                ? { postType: "contest", contestId: writeContestId }
                : { postType: "request" },
          });
          imagePath = uploaded.path;
        }

        created = await createCommunityPost({
          post_type: writeType,
          title,
          contest_id: writeType === "contest" ? writeContestId : null,
          image_url: imagePath,
          content: content || null,
        });
      }

      setPosts((prev) =>
        [toPost(created), ...prev.filter((post) => post.id !== created.id)].slice(0, PER_PAGE)
      );
      setTotalCount((prev) => prev + 1);
      setCurrentPage(1);
      setActiveTab("all");
      setIsWriteOpen(false);
      resetWriteForm();
      toast.success("게시글이 등록되었습니다");
    } catch (error) {
      toast.error(getErrorMessage(error, "게시글 등록에 실패했습니다"));
      console.error("게시글 등록 실패:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const openEditForm = () => {
    if (!selectedPost) return;

    setEditTitle(selectedPost.title);
    setEditContent(selectedPost.content);
    setEditFile(null);
    setIsEditOpen(true);
  };

  const handleEditSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedPost) return;

    const title = editTitle.trim();
    const content = editContent.trim();

    if (!title) {
      toast.error("제목을 입력해주세요");
      return;
    }

    setIsSubmitting(true);

    try {
      let imagePath: string | undefined;

      if (editFile) {
        if (selectedPost.category === "contest" && !selectedPost.contest_id) {
          toast.error("콘테스트 정보를 찾을 수 없습니다");
          return;
        }

        const uploaded = await uploadCommunityImage({
          file: editFile,
          target:
            selectedPost.category === "contest"
              ? { postType: "contest", contestId: selectedPost.contest_id! }
              : selectedPost.category === "notice"
                ? { postType: "notice", postId: selectedPost.id }
                : { postType: "request" },
        });

        imagePath = uploaded.path;
      }

      const updated = await updateCommunityPost(selectedPost.id, {
        title,
        content,
        ...(imagePath ? { image_url: imagePath } : {}),
      });

      const nextPost = toPost(updated);
      setPosts((prev) => prev.map((post) => (post.id === nextPost.id ? nextPost : post)));
      setSelectedPost(nextPost);
      setIsEditOpen(false);
      setEditFile(null);
      toast.success("게시글이 수정되었습니다");
    } catch (error) {
      toast.error(getErrorMessage(error, "게시글 수정에 실패했습니다"));
      console.error("게시글 수정 실패:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const deletePostById = async (postId: string) => {
    setIsSubmitting(true);

    try {
      await deleteCommunityPost(postId);
      setPosts((prev) => prev.filter((post) => post.id !== postId));
      setTotalCount((prev) => Math.max(0, prev - 1));
      setSelectedPost((current) => (current?.id === postId ? null : current));
      setIsEditOpen(false);
      toast.success("게시글이 삭제되었습니다");
    } catch (error) {
      toast.error("게시글 삭제에 실패했습니다");
      console.error("게시글 삭제 실패:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeletePost = () => {
    if (!selectedPost) return;

    const postId = selectedPost.id;

    toast.warning("게시글을 삭제하시겠습니까?", {
      action: {
        label: "삭제",
        onClick: () => {
          void deletePostById(postId);
        },
      },
      duration: 5000,
    });
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
    const canModifySelectedPost =
      selectedPost.user_id === myUserId ||
      (selectedPost.category === "notice" && myRoleType === "admin");

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
                <div className="flex items-center gap-6 text-sm text-gray-400 dark:text-gray-500">
                  <span className="flex items-center gap-2 font-medium text-gray-600 dark:text-gray-400">
                    {selectedPost.authorProfileImageUrl ? (
                      <img
                        src={selectedPost.authorProfileImageUrl}
                        alt={selectedPost.author}
                        className="h-6 w-6 rounded-full object-cover"
                      />
                    ) : (
                      <span className="flex h-6 w-6 items-center justify-center rounded-full bg-gray-200 text-xs text-gray-500 dark:bg-gray-700 dark:text-gray-300">
                        {selectedPost.author.slice(0, 1)}
                      </span>
                    )}
                    <span>{selectedPost.author}</span>
                  </span>
                  <span className="flex items-center gap-1">
                    <Heart className="h-3.5 w-3.5" />
                    {selectedPost.likes.toLocaleString()}
                  </span>
                  <span>{selectedPost.date}</span>
                  <span className="flex items-center gap-1">
                    <Eye className="h-3.5 w-3.5" />
                    {selectedPost.views.toLocaleString()}
                  </span>
                </div>
                <div className="flex items-center gap-4 text-sm text-gray-400 dark:text-gray-500">
                  <button
                      onClick={() => {
                        void navigator.clipboard.writeText(window.location.href);
                        toast.success("링크가 복사되었습니다");
                      }}
                      className="flex items-center gap-1 hover:text-cyan-500 transition-colors"
                  >
                    <Link2 className="h-3.5 w-3.5" />
                  </button>
                  {canModifySelectedPost && (
                    <>
                      <button
                        type="button"
                        onClick={openEditForm}
                        className="flex items-center gap-1 hover:text-cyan-500 transition-colors"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                        수정
                      </button>
                      <button
                        type="button"
                        onClick={handleDeletePost}
                        className="flex items-center gap-1 hover:text-red-500 transition-colors disabled:opacity-50"
                        disabled={isSubmitting}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        삭제
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>

            {/* 본문 */}
            <div className="px-8 py-8 min-h-[240px] space-y-6">
              {selectedPost.image_url && (
                <img
                  src={selectedPost.image_url}
                  alt="게시글 이미지"
                  className="max-w-full rounded-lg"
                />
              )}
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

          {isEditOpen && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
              <Card className="w-full max-w-xl border-none bg-white dark:bg-gray-800 p-6 shadow-2xl">
                <div className="mb-5">
                  <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">게시글 수정</h2>
                </div>

                <form onSubmit={handleEditSubmit} className="space-y-4">
                  <label className="block space-y-1.5">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">제목</span>
                    <Input
                      value={editTitle}
                      onChange={(event) => setEditTitle(event.target.value)}
                      placeholder="제목을 입력하세요"
                      className="h-11 text-base"
                    />
                  </label>

                  <label className="block space-y-1.5">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">내용</span>
                    <textarea
                      value={editContent}
                      onChange={(event) => setEditContent(event.target.value)}
                      placeholder="내용을 입력하세요"
                      rows={7}
                      className="w-full resize-none rounded-md border border-gray-200 bg-white px-3 py-3 text-base text-gray-900 outline-none focus:border-cyan-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
                    />
                  </label>

                  {selectedPost.image_url && (
                    <div className="space-y-1.5">
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300">현재 이미지</span>
                      <img
                        src={selectedPost.image_url}
                        alt="현재 게시글 이미지"
                        className="max-h-40 rounded-md object-contain"
                      />
                    </div>
                  )}

                  <label className="block space-y-1.5">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">이미지 변경</span>
                    <Input
                      type="file"
                      accept="image/png,image/jpeg,image/webp"
                      onChange={(event) => setEditFile(event.target.files?.[0] ?? null)}
                      className="h-11 text-base"
                    />
                  </label>

                  <div className="flex justify-end gap-2 pt-2">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => {
                        setIsEditOpen(false);
                        setEditFile(null);
                      }}
                      disabled={isSubmitting}
                    >
                      취소
                    </Button>
                    <Button
                      type="submit"
                      disabled={isSubmitting}
                      className="bg-gradient-to-r from-cyan-500 to-blue-500"
                    >
                      {isSubmitting ? "수정 중" : "수정"}
                    </Button>
                  </div>
                </form>
              </Card>
            </div>
          )}

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
                onClick={() => setIsWriteOpen(true)}
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
                          {post.isNew && post.user_id !== myUserId && !readPostIds.has(post.id) && (
                              <span className="w-2 h-2 rounded-full bg-orange-400 flex-shrink-0 inline-block" />
                          )}
                  </span>

                        {/* 작성자 · 좋아요 · 날짜 · 조회수 */}
                        <span className="grid grid-cols-[110px_60px_100px_72px] items-center text-sm text-gray-400 dark:text-gray-500 flex-shrink-0 min-w-[325px]">
                          <span className="truncate text-left">{post.author}</span>

                          <span className="flex items-center justify-center gap-1">
                            <Heart className="h-3.5 w-3.5" />
                            <span className="tabular-nums">{post.likes.toLocaleString()}</span>
                          </span>

                          <span className="text-center tabular-nums">{post.date}</span>
                            <span className="grid grid-cols-[16px_30px] items-center justify-end gap-1">
                              <Eye className="h-3.5 w-3.5" />
                              <span className="tabular-nums text-left">
                                {post.views.toLocaleString()}
                              </span>
                            </span>
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

        {isWriteOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
            <Card className="max-h-[90vh] w-full max-w-3xl overflow-y-auto border-none bg-white/95 shadow-2xl backdrop-blur-xl dark:bg-gray-800/95">
              <form onSubmit={handleWriteSubmit}>
                <div className="border-b border-gray-100 px-8 pb-6 pt-8 dark:border-gray-700">
                  <div className="mb-4 flex flex-wrap items-end gap-3">
                    <label className="space-y-1.5">
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300">게시판</span>
                      <select
                        value={writeType}
                        onChange={(event) => {
                          const nextType = event.target.value as PostType;

                          if (nextType === "notice" && myRoleType !== "admin") {
                            toast.error("운영자만 가능합니다");
                            return;
                          }

                          setWriteType(nextType);
                        }}
                        className={`h-9 min-w-[150px] rounded border-0 px-3 text-sm font-medium outline-none focus:ring-2 focus:ring-cyan-400 ${
                          BADGE_STYLE[writeType === "request" ? "item" : writeType].bg
                        } ${BADGE_STYLE[writeType === "request" ? "item" : writeType].text}`}
                      >
                        <option value="request">이거 만들어주세요</option>
                        <option value="contest">아바타 콘테스트</option>
                        <option value="notice">공지사항</option>
                      </select>
                    </label>

                    {writeType === "contest" && (
                      <label className="min-w-0 flex-1 space-y-1.5">
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">콘테스트</span>
                        <select
                          value={writeContestId}
                          onChange={(event) => setWriteContestId(event.target.value)}
                          className="h-9 w-full rounded-md border border-gray-200 bg-white px-3 text-sm text-gray-900 outline-none focus:border-cyan-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
                        >
                          {contests.length === 0 ? (
                            <option value="">등록된 콘테스트 없음</option>
                          ) : (
                            contests.map((contest) => (
                              <option key={contest.id} value={contest.id}>
                                {contest.title}
                              </option>
                            ))
                          )}
                        </select>
                      </label>
                    )}
                  </div>

                  <Input
                    value={writeTitle}
                    onChange={(event) => setWriteTitle(event.target.value)}
                    placeholder="제목을 입력하세요"
                    className="h-auto border-0 bg-transparent px-0 py-1 text-2xl font-bold leading-snug text-gray-900 shadow-none outline-none placeholder:text-gray-300 focus-visible:ring-0 dark:text-gray-100 dark:placeholder:text-gray-600"
                  />

                  <div className="mt-5 flex items-center gap-6 text-sm text-gray-400 dark:text-gray-500">
                    <span className="font-medium text-gray-600 dark:text-gray-400">작성 중</span>
                    <span className="flex items-center gap-1">
                      <Heart className="h-3.5 w-3.5" />0
                    </span>
                    <span>{formatPostDate(new Date().toISOString())}</span>
                    <span className="flex items-center gap-1">
                      <Eye className="h-3.5 w-3.5" />0
                    </span>
                  </div>
                </div>

                <div className="min-h-[260px] space-y-5 px-8 py-8">
                  <textarea
                    value={writeContent}
                    onChange={(event) => setWriteContent(event.target.value)}
                    placeholder="내용을 입력하세요"
                    rows={10}
                    className="w-full resize-none border-0 bg-transparent p-0 text-base leading-8 text-gray-700 outline-none placeholder:text-gray-300 focus:ring-0 dark:text-gray-300 dark:placeholder:text-gray-600"
                  />

                  <label className="block space-y-1.5 rounded-lg border border-dashed border-gray-200 bg-gray-50/70 px-4 py-3 dark:border-gray-700 dark:bg-gray-900/40">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">이미지</span>
                    <Input
                      type="file"
                      accept="image/png,image/jpeg,image/webp"
                      onChange={(event) => setWriteFile(event.target.files?.[0] ?? null)}
                      className="h-11 bg-white text-base dark:bg-gray-900"
                    />
                  </label>
                </div>

                <div className="flex justify-center gap-2 border-t border-gray-100 px-8 py-5 dark:border-gray-700">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setIsWriteOpen(false);
                      resetWriteForm();
                    }}
                    disabled={isSubmitting}
                    className="h-11 px-10 text-base"
                  >
                    취소
                  </Button>
                  <Button
                    type="submit"
                    disabled={isSubmitting}
                    className="h-11 bg-gradient-to-r from-cyan-500 to-blue-500 px-10 text-base"
                  >
                    {isSubmitting ? "등록 중" : "등록"}
                  </Button>
                </div>
              </form>
            </Card>
          </div>
        )}

        <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />
      </div>
  );
}
