import { useState, useMemo, useEffect, type FormEvent } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import AiChatbotDialog from "@/components/chat/AiChatbotDialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import { Search, Send, Eye, Heart, ChevronUp, ChevronDown, Link2, Pencil, Trash2, X, Paperclip } from "lucide-react";
import { toast } from "sonner";
import { AxiosError } from "axios";
import {
  createComment,
  createCommunityPost,
  deleteComment,
  deleteCommunityPost,
  getCommunityMe,
  getCommunityPost,
  listComments,
  listCommunityPosts,
  listContests,
  reactCommunityPost,
  unreactCommunityPost,
  updateComment,
  updateCommunityPost,
  uploadCommunityImage,
  type CommentResponse,
  type ContestResponse,
  type CommunityPostResponse,
  type ContentReportTargetType,
  type PostType,
  type PostSort,
} from "@/domains/community/api/communityApi";
import ReportDialog from "@/components/report/ReportDialog.tsx";
import {Siren} from "lucide-react";

// ── 타입 ─────────────────────────────────────────────────────

type PostCategory = "notice" | "contest" | "item" | "free";
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
  detailDate: string;
  isNew?: boolean;
  // reaction_count를 화면에서는 기존처럼 likes라는 이름으로 써도 됨.
  // contest면 투표 수, free/request면 좋아요 수.
  likes: number;

  // 현재 로그인한 사용자가 이미 눌렀는지 여부.
  // contest면 투표 완료 여부,
  // free/request면 좋아요 여부.
  isReacted: boolean;
  views: number;
  content: string;
  image_url?: string | null;
}

interface Comment {
  id: string;
  post_id: string;
  parent_id: string | null;
  user_id: string;
  author: string;
  authorProfileImageUrl: string | null;
  content: string;
  date: string;
  updatedAt: string | null;
}

// ── 탭 정의 ──────────────────────────────────────────────────

const TABS: { key: TabKey; label: string }[] = [
  { key: "all",     label: "전체" },
  { key: "notice",  label: "공지사항" },
  { key: "contest", label: "아바타 콘테스트" },
  { key: "item",    label: "이거 만들어주세요" },
  { key: "free",    label: "자유" },
];

// ── 뱃지 스타일 ───────────────────────────────────────────────

const BADGE_STYLE: Record<PostCategory, { bg: string; text: string; label: string }> = {
  notice:  { bg: "bg-sky-100 dark:bg-sky-900/40",       text: "text-sky-700 dark:text-sky-300",       label: "공지" },
  contest: { bg: "bg-yellow-100 dark:bg-yellow-900/40", text: "text-yellow-800 dark:text-yellow-300", label: "콘테스트" },
  item:    { bg: "bg-purple-100 dark:bg-purple-900/40", text: "text-purple-800 dark:text-purple-300", label: "아이템 요청" },
  free:    { bg: "bg-emerald-100 dark:bg-emerald-900/40", text: "text-emerald-700 dark:text-emerald-300", label: "자유" },
};

const PER_PAGE = 10;
const POST_TITLE_MAX_LENGTH = 200;
const POST_CONTENT_MAX_LENGTH = 500;

function formatPostDate(value: string | null): string {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}.${month}.${day}`;
}

function formatPostDateTime(value: string | null): string {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${year}.${month}.${day} ${hours}:${minutes}`;
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
      if (message.includes("value too long for type character varying(500)")) {
        return `내용은 ${POST_CONTENT_MAX_LENGTH}자 이내로 입력해주세요`;
      }
      if (message.includes("value too long for type character varying(200)")) {
        return `제목은 ${POST_TITLE_MAX_LENGTH}자 이내로 입력해주세요`;
      }

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
    detailDate: formatPostDateTime(post.created_at),
    isNew: isNewPost(post.created_at),
    likes: post.reaction_count ?? 0,
    isReacted: post.is_reacted,
    views: post.view_count,
    content: post.content ?? "",
    image_url: buildImageUrl(post.image_url),
  };
}

function toComment(comment: CommentResponse): Comment {
  return {
    id: comment.id,
    post_id: comment.post_id,
    parent_id: comment.parent_id,
    user_id: comment.user_id,
    author: comment.author_nickname ?? "익명",
    authorProfileImageUrl: comment.author_profile_image_url,
    content: comment.content,
    date: formatPostDate(comment.created_at),
    updatedAt: comment.updated_at,
  };
}

// ── 컴포넌트 ─────────────────────────────────────────────────

export default function Community() {
  const [activeTab, setActiveTab]         = useState<TabKey>("all");
  const [searchQuery, setSearchQuery]     = useState("");
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState("");
  const [currentPage, setCurrentPage]     = useState(1);
  const [sort, setSort]                   = useState<PostSort>("date");
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
  const [reactingPostId, setReactingPostId] = useState<string | null>(null);
  const [comments, setComments]           = useState<Comment[]>([]);
  const [commentContent, setCommentContent] = useState("");
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editingCommentContent, setEditingCommentContent] = useState("");
  const [isCommentSubmitting, setIsCommentSubmitting] = useState(false);
  const [postDeleteTarget, setPostDeleteTarget] = useState<Post | null>(null);
  const [commentDeleteTarget, setCommentDeleteTarget] = useState<Comment | null>(null);

  // 대댓글 상태
  // replyingToCommentId: 현재 어느 댓글에 답글을 달고 있는지 저장
  // replyContent: 대댓글 입력값
  const [replyingToCommentId, setReplyingToCommentId] = useState<string | null>(null);
  const [replyContent, setReplyContent] = useState("");

  // 신고 모달 상태
  //
  // 게시글 상세 상단의 "신고" 버튼을 누르면
  // 아래 배열에 신고 가능한 대상들을 넣는다.
  //
  // 예시:
  // [
  //   { type: "post", id: "...", label: "게시글" },
  //   { type: "user_nickname", id: "...", label: "닉네임" },
  //   { type: "user_profile", id: "...", label: "프로필 사진" },
  // ]
  //
  // 댓글 신고 버튼은 comment 하나만 넣음.
  //
  // null이면 모달 닫힘 상태.
  const [reportTargets, setReportTargets] = useState<
      {
        type: ContentReportTargetType;
        id: string;
        label: string;
      }[] | null
  >(null);


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
          sort,
          postType: activeTab === "all" ? undefined : activeTab === "item" ? "request" : activeTab,
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
  }, [activeTab, currentPage, debouncedSearchQuery, sort]);

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

  useEffect(() => {
    if (!selectedPost || selectedPost.category === "notice") {
      setComments([]);
      setCommentContent("");
      setEditingCommentId(null);
      setEditingCommentContent("");
      setCommentDeleteTarget(null);
      setReplyingToCommentId(null);
      setReplyContent("");
      return;
    }

    const postId = selectedPost.id;
    let ignore = false;

    async function fetchComments() {
      try {
        const data = await listComments(postId);
        if (!ignore) {
          setComments(data.map(toComment));
        }
      } catch (error) {
        if (!ignore) {
          toast.error("댓글을 불러오지 못했습니다");
          console.error("댓글 목록 조회 실패:", error);
        }
      }
    }

    void fetchComments();

    return () => {
      ignore = true;
    };
  }, [selectedPost?.id, selectedPost?.category]);

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

    if (title.length > POST_TITLE_MAX_LENGTH) {
      toast.error(`제목은 ${POST_TITLE_MAX_LENGTH}자 이내로 입력해주세요`);
      return;
    }

    if (!content) {
      toast.error("내용을 입력해주세요");
      return;
    }

    if (content.length > POST_CONTENT_MAX_LENGTH) {
      toast.error(`내용은 ${POST_CONTENT_MAX_LENGTH}자 이내로 입력해주세요`);
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
          content,
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
                : { postType: writeType },
          });
          imagePath = uploaded.path;
        }

        created = await createCommunityPost({
          post_type: writeType,
          title,
          contest_id: writeType === "contest" ? writeContestId : null,
          image_url: imagePath,
          content,
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

    if (title.length > POST_TITLE_MAX_LENGTH) {
      toast.error(`제목은 ${POST_TITLE_MAX_LENGTH}자 이내로 입력해주세요`);
      return;
    }

    if (!content) {
      toast.error("내용을 입력해주세요");
      return;
    }

    if (content.length > POST_CONTENT_MAX_LENGTH) {
      toast.error(`내용은 ${POST_CONTENT_MAX_LENGTH}자 이내로 입력해주세요`);
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
                : selectedPost.category === "free"
                  ? { postType: "free" }
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
      setPostDeleteTarget(null);
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

    setPostDeleteTarget(selectedPost);
  };

  // 신고 모달 열기
  //
  // targets 배열을 그대로 ReportDialog에 넘긴다.
  //
  // 게시글 신고 버튼:
  // [
  //   post,
  //   user_nickname,
  //   user_profile
  // ]
  //
  // 댓글 신고 버튼:
  // [
  //   comment
  // ]
  const openReportDialog = (
      targets: {
        type: ContentReportTargetType;
        id: string;
        label: string;
      }[]
  ) => {
    setReportTargets(targets);
  };

  const handleReactPost = async (post: Post) => {
    if (reactingPostId === post.id) return;

    // 공지사항은 백엔드에서도 막지만,
    // 프론트에서도 버튼 동작을 막아두면 UX가 더 좋음.
    if (post.category === "notice") {
      toast.error("공지사항에는 반응할 수 없습니다");
      return;
    }

    // 콘테스트는 투표 취소 불가 정책.
    // 이미 투표한 상태에서 다시 누르면 DELETE를 보내지 않고 막음.
    if (post.category === "contest" && post.isReacted) {
      toast.error("아바타 콘테스트 투표는 취소할 수 없습니다");
      return;
    }

    setReactingPostId(post.id);

    try {
      if (post.isReacted) {
        // free/request 좋아요 취소
        await unreactCommunityPost(post.id);
      } else {
        // contest 투표 등록 또는 free/request 좋아요 등록
        await reactCommunityPost(post.id);
      }

      const nextIsReacted = !post.isReacted;
      const nextLikes = post.isReacted
          ? Math.max(0, post.likes - 1)
          : post.likes + 1;

      // 목록 상태 갱신
      setPosts((prev) =>
          prev.map((item) =>
              item.id === post.id
                  ? {
                    ...item,
                    likes: nextLikes,
                    isReacted: nextIsReacted,
                  }
                  : item
          )
      );

      // 상세 페이지 상태 갱신
      setSelectedPost((current) =>
          current?.id === post.id
              ? {
                ...current,
                likes: nextLikes,
                isReacted: nextIsReacted,
              }
              : current
      );

      toast.success(
          post.category === "contest"
              ? "투표가 완료되었습니다"
              : post.isReacted
                  ? "좋아요를 취소했습니다"
                  : "좋아요를 눌렀습니다"
      );
    } catch (error) {
      toast.error(getErrorMessage(error, "반응 처리에 실패했습니다"));
      console.error("게시글 반응 처리 실패:", error);
    } finally {
      setReactingPostId((current) => (current === post.id ? null : current));
    }
  };

  const handleCreateComment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedPost || selectedPost.category === "notice") return;

    const content = commentContent.trim();

    if (!content) {
      toast.error("댓글을 입력해주세요");
      return;
    }

    setIsCommentSubmitting(true);

    try {
      const created = await createComment(selectedPost.id, content);
      setComments((prev) => [...prev, toComment(created)]);
      setCommentContent("");
      toast.success("댓글이 등록되었습니다");
    } catch (error) {
      toast.error(getErrorMessage(error, "댓글 등록에 실패했습니다"));
      console.error("댓글 등록 실패:", error);
    } finally {
      setIsCommentSubmitting(false);
    }
  };

  const handleCreateReply = async (parentCommentId: string) => {
    if (!selectedPost || selectedPost.category === "notice") return;

    const content = replyContent.trim();

    if (!content) {
      toast.error("답글을 입력해주세요");
      return;
    }

    setIsCommentSubmitting(true);

    try {
      const created = await createComment(selectedPost.id, content, parentCommentId);
      setComments((prev) => [...prev, toComment(created)]);
      setReplyContent("");
      setReplyingToCommentId(null);
      toast.success("답글이 등록되었습니다");
    } catch (error) {
      toast.error(getErrorMessage(error, "답글 등록에 실패했습니다"));
      console.error("답글 등록 실패:", error);
    } finally {
      setIsCommentSubmitting(false);
    }
  };

  const openEditComment = (comment: Comment) => {
    setEditingCommentId(comment.id);
    setEditingCommentContent(comment.content);
    setReplyingToCommentId(null);
    setReplyContent("");
  };

  const handleUpdateComment = async (commentId: string) => {
    const content = editingCommentContent.trim();

    if (!content) {
      toast.error("댓글을 입력해주세요");
      return;
    }

    setIsCommentSubmitting(true);

    try {
      const updated = await updateComment(commentId, content);
      const nextComment = toComment(updated);
      setComments((prev) =>
        prev.map((comment) => (comment.id === commentId ? nextComment : comment))
      );
      setEditingCommentId(null);
      setEditingCommentContent("");
      toast.success("댓글이 수정되었습니다");
    } catch (error) {
      toast.error(getErrorMessage(error, "댓글 수정에 실패했습니다"));
      console.error("댓글 수정 실패:", error);
    } finally {
      setIsCommentSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    setIsCommentSubmitting(true);

    try {
      await deleteComment(commentId);

      // 백엔드에서 부모 댓글 삭제 시 자식 대댓글도 soft delete 처리하므로
      // 프론트에서도 부모 댓글과 그 자식 대댓글을 같이 제거
      setComments((prev) =>
          prev.filter((comment) => comment.id !== commentId && comment.parent_id !== commentId)
      );

      if (editingCommentId === commentId) {
        setEditingCommentId(null);
        setEditingCommentContent("");
      }

      if (replyingToCommentId === commentId) {
        setReplyingToCommentId(null);
        setReplyContent("");
      }

      setCommentDeleteTarget(null);
      toast.success("댓글이 삭제되었습니다");
    } catch (error) {
      toast.error("댓글 삭제에 실패했습니다");
      console.error("댓글 삭제 실패:", error);
    } finally {
      setIsCommentSubmitting(false);
    }
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

    // ─────────────────────────────────────────────
    // 게시글 수정/삭제 가능 여부
    // ─────────────────────────────────────────────
    //
    // 일반 게시글:
    // - 작성자 본인만 수정/삭제 가능
    //
    // 공지사항:
    // - 운영자만 수정/삭제 가능
    const canModifySelectedPost =
        selectedPost.user_id === myUserId ||
        (selectedPost.category === "notice" && myRoleType === "admin");

    // ─────────────────────────────────────────────
    // 게시글 신고 가능 여부
    // ─────────────────────────────────────────────
    //
    // 신고 버튼은 일반 사용자 글에만 보여준다.
    //
    // 막는 대상:
    // 1. 내 글
    //    - 본인 글 신고 불가
    //
    // 2. 공지사항
    //    - 공지사항은 운영자가 작성한 공식 글이므로 신고 대상에서 제외
    //
    // 참고:
    // - 댓글 영역 자체가 공지사항에서는 렌더링되지 않으므로
    //   공지사항 댓글 신고 버튼은 현재 구조상 표시되지 않는다.
    const canReportSelectedPost =
        selectedPost.user_id !== myUserId &&
        selectedPost.category !== "notice";

    // 일반 댓글만 먼저 렌더링
    const rootComments = comments.filter((comment) => !comment.parent_id);

    // parent_id 기준으로 대댓글 묶기
    const repliesByParentId = comments.reduce<Record<string, Comment[]>>((acc, comment) => {
      if (!comment.parent_id) return acc;

      acc[comment.parent_id] ??= [];
      acc[comment.parent_id].push(comment);

      return acc;
    }, {});

    return (
        <div className="space-y-6">
          {/* 헤더 */}
          <div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">커뮤니티</h1>
            <p className="text-gray-600 dark:text-gray-300">다른 사용자들과 소통하고 경험을 나눠보세요</p>
          </div>

          <Card className="border-none spentopia-surface-card backdrop-blur-xl overflow-hidden">
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
              <div className="flex items-center justify-between flex-wrap gap-3">
                <div className="flex items-center gap-3">
                  <span className="flex-shrink-0">
                    {selectedPost.authorProfileImageUrl ? (
                      <img
                        src={selectedPost.authorProfileImageUrl}
                        alt={selectedPost.author}
                        className="h-12 w-12 rounded-full object-cover"
                      />
                    ) : (
                      <span className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-200 text-base font-semibold text-gray-500 dark:bg-gray-700 dark:text-gray-300">
                        {selectedPost.author.slice(0, 1)}
                      </span>
                    )}
                  </span>
                  <div className="flex min-w-0 flex-col gap-1">
                    <span className="truncate text-base font-semibold text-gray-900 dark:text-gray-100">
                      {selectedPost.author}
                    </span>
                    <span className="flex items-center gap-4 text-sm text-gray-400 dark:text-gray-500">
                      <span>{selectedPost.detailDate}</span>
                      <span className="flex items-center gap-1">
                        <Eye className="relative top-px h-3.5 w-3.5" />
                        {selectedPost.views.toLocaleString()}
                      </span>
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-5 text-sm text-gray-400 dark:text-gray-500">
                  <button
                      onClick={() => {
                        void navigator.clipboard.writeText(window.location.href);
                        toast.success("링크가 복사되었습니다");
                      }}
                      className="flex items-center gap-1 hover:text-cyan-500 transition-colors"
                  >
                    <Link2 className="relative top-0.5 h-5 w-5" />
                  </button>
                  {/* 신고 버튼
    ------------------------------------------------
    게시글 상세에서만 보이는 신고 버튼.

    클릭 시 신고 대상 선택 모달이 열린다.

    신고 가능 대상:
    - 게시글
    - 작성자 닉네임
    - 작성자 프로필 사진

    신고 버튼 숨김 조건:
    1. 내 글이면 숨김
    2. 공지사항이면 숨김

    이유:
    - 공지사항은 운영자가 작성한 공식 게시글이므로
      일반 사용자가 신고할 수 없게 처리한다.
------------------------------------------------ */}
                  {canReportSelectedPost && (
                      <button
                          type="button"
                          onClick={() =>
                              openReportDialog([
                                {
                                  type: "post",
                                  id: selectedPost.id,
                                  label: "게시글",
                                },
                                {
                                  type: "user_nickname",
                                  id: selectedPost.user_id,
                                  label: "작성자 닉네임",
                                },
                                {
                                  type: "user_profile",
                                  id: selectedPost.user_id,
                                  label: "작성자 프로필 사진",
                                },
                              ])
                          }
                          className="flex items-center gap-1 hover:text-red-500 transition-colors"
                      >
                        <Siren className="h-5 w-5" />
                      </button>
                  )}
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

            {selectedPost.category !== "notice" && (
              <div className="flex justify-center px-8 py-5">
                <button
                    type="button"
                    onClick={() => void handleReactPost(selectedPost)}
                    disabled={
                      reactingPostId === selectedPost.id ||
                      (selectedPost.category === "contest" && selectedPost.isReacted)
                    }
                    className={`inline-flex h-10 items-center gap-2 rounded-md border px-4 text-sm font-medium transition-colors ${
                        selectedPost.isReacted
                            ? "border-red-200 bg-red-50 text-red-500 dark:border-red-900/60 dark:bg-red-950/30"
                            : "border-gray-200 text-gray-500 hover:border-red-200 hover:text-red-500 dark:border-gray-700 dark:text-gray-400 dark:hover:border-red-900/60"
                    } disabled:cursor-not-allowed disabled:opacity-70`}
                    title={
                      selectedPost.category === "contest"
                          ? selectedPost.isReacted
                              ? "이미 투표했습니다"
                              : "투표하기"
                          : selectedPost.isReacted
                              ? "좋아요 취소"
                              : "좋아요"
                    }
                >
                  <Heart
                      className={`h-4 w-4 ${
                          selectedPost.isReacted ? "fill-current" : ""
                      }`}
                  />
                  <span>
                    {selectedPost.category === "contest" ? "투표" : "좋아요"}
                  </span>
                  <span className="tabular-nums">
                    {selectedPost.likes.toLocaleString()}
                  </span>
                </button>
              </div>
            )}

            {selectedPost.category !== "notice" && (
              <div className="border-t border-gray-100 dark:border-gray-700 px-8 py-6">
                <h3 className="mb-4 text-base font-bold text-gray-900 dark:text-gray-100">
                  댓글 {comments.length}
                </h3>

                <form onSubmit={handleCreateComment} className="mb-6 flex gap-2">
                  <Input
                    value={commentContent}
                    onChange={(event) => setCommentContent(event.target.value)}
                    placeholder="댓글을 입력하세요"
                    className="h-10 text-sm"
                    disabled={isCommentSubmitting}
                  />
                  <Button
                    type="submit"
                    disabled={isCommentSubmitting}
                    className="h-10 px-5"
                  >
                    등록
                  </Button>
                </form>

                <div className="space-y-4">
                  {comments.length === 0 ? (
                      <p className="text-sm text-gray-400 dark:text-gray-500">아직 댓글이 없습니다</p>
                  ) : (
                      rootComments.map((comment) => (
                          <div
                              key={comment.id}
                              className="border-b border-gray-100 pb-4 last:border-b-0 dark:border-gray-700"
                          >
                            <div className="mb-2 flex items-center justify-between gap-3">
                              <div className="flex min-w-0 items-center gap-2">
                                {comment.authorProfileImageUrl ? (
                                    <img
                                        src={comment.authorProfileImageUrl}
                                        alt={comment.author}
                                        className="h-6 w-6 rounded-full object-cover"
                                    />
                                ) : (
                                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-gray-200 text-xs text-gray-500 dark:bg-gray-700 dark:text-gray-300">
                              {comment.author.slice(0, 1)}
                            </span>
                                )}

                                <span className="truncate text-sm font-medium text-gray-700 dark:text-gray-300">
                            {comment.author}
                          </span>

                                {comment.user_id === selectedPost.user_id && (
                                    <span className="rounded bg-cyan-50 px-1.5 py-0.5 text-[10px] font-medium text-cyan-600 dark:bg-cyan-900/30 dark:text-cyan-300">
                                          작성자
                                        </span>
                                )}

                                <span className="relative top-px text-xs text-gray-400 dark:text-gray-500">
                            {comment.date}
                          </span>
                              </div>

                              <div className="flex flex-shrink-0 items-center gap-3 text-xs text-gray-400 dark:text-gray-500">

                                {/* 본인 댓글 */}
                                {comment.user_id === myUserId ? (
                                    <>
                                      <button
                                          type="button"
                                          onClick={() => openEditComment(comment)}
                                          className="hover:text-cyan-500 transition-colors"
                                          disabled={isCommentSubmitting}
                                      >
                                        수정
                                      </button>

                                      <span className="text-gray-300 dark:text-gray-600">|</span>

                                      <button
                                          type="button"
                                          onClick={() => setCommentDeleteTarget(comment)}
                                          className="hover:text-red-500 transition-colors"
                                          disabled={isCommentSubmitting}
                                      >
                                        삭제
                                      </button>
                                    </>
                                ) : (
                                    /* 남 댓글 */
                                    <button
                                        type="button"
                                        onClick={() =>
                                            openReportDialog([
                                              {
                                                type: "comment",
                                                id: comment.id,
                                                label: "댓글",
                                              },
                                            ])
                                        }
                                        className="hover:text-red-500 transition-colors"
                                        disabled={isCommentSubmitting}
                                    >
                                      <Siren className="h-5 w-5"/>
                                    </button>
                                )}
                              </div>
                            </div>

                            {editingCommentId === comment.id ? (
                                <div className="space-y-2">
                          <textarea
                              value={editingCommentContent}
                              onChange={(event) => setEditingCommentContent(event.target.value)}
                              rows={3}
                              className="w-full resize-none rounded-md border border-gray-200 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-cyan-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
                              disabled={isCommentSubmitting}
                          />

                                  <div className="flex justify-end gap-2">
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        onClick={() => {
                                          setEditingCommentId(null);
                                          setEditingCommentContent("");
                                        }}
                                        disabled={isCommentSubmitting}
                                    >
                                      취소
                                    </Button>

                                    <Button
                                        type="button"
                                        size="sm"
                                        disabled={isCommentSubmitting}
                                        onClick={() => void handleUpdateComment(comment.id)}
                                    >
                                      저장
                                    </Button>
                                  </div>
                                </div>
                            ) : (
                                <>
                                  <p className="ml-0.5 whitespace-pre-wrap text-sm leading-6 text-gray-700 dark:text-gray-300">
                                    {comment.content}
                                  </p>

                                  <button
                                      type="button"
                                      onClick={() => {
                                        setReplyingToCommentId(comment.id);
                                        setReplyContent("");
                                        setEditingCommentId(null);
                                        setEditingCommentContent("");
                                      }}
                                      className="mt-2 text-xs text-gray-400 hover:text-cyan-500"
                                      disabled={isCommentSubmitting}
                                  >
                                    답글
                                  </button>
                                </>
                            )}

                            {replyingToCommentId === comment.id && (
                                <div className="mt-3 ml-8 flex gap-2">
                                  <Input
                                      value={replyContent}
                                      onChange={(event) => setReplyContent(event.target.value)}
                                      placeholder="답글을 입력하세요"
                                      className="h-9 text-sm"
                                      disabled={isCommentSubmitting}
                                  />

                                  <Button
                                      type="button"
                                      size="sm"
                                      disabled={isCommentSubmitting}
                                      onClick={() => void handleCreateReply(comment.id)}
                                  >
                                    등록
                                  </Button>

                                  <Button
                                      type="button"
                                      variant="outline"
                                      size="sm"
                                      disabled={isCommentSubmitting}
                                      onClick={() => {
                                        setReplyingToCommentId(null);
                                        setReplyContent("");
                                      }}
                                  >
                                    취소
                                  </Button>
                                </div>
                            )}

                            <div className="mt-4 ml-8 space-y-4">
                              {(repliesByParentId[comment.id] ?? []).map((reply) => (
                                  <div
                                      key={reply.id}
                                      className="border-l-2 border-gray-100 pl-4 dark:border-gray-700"
                                  >
                                    <div className="mb-2 flex items-center justify-between gap-3">
                                      <div className="flex min-w-0 items-center gap-2">
                                        {reply.authorProfileImageUrl ? (
                                            <img
                                                src={reply.authorProfileImageUrl}
                                                alt={reply.author}
                                                className="h-6 w-6 rounded-full object-cover"
                                            />
                                        ) : (
                                            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-gray-200 text-xs text-gray-500 dark:bg-gray-700 dark:text-gray-300">
                                    {reply.author.slice(0, 1)}
                                  </span>
                                        )}

                                        <span className="truncate text-sm font-medium text-gray-700 dark:text-gray-300">
                                  {reply.author}
                                </span>

                                        <span className="relative top-px text-xs text-gray-400 dark:text-gray-500">
                                  {reply.date}
                                </span>
                                      </div>

                                      <div className="flex flex-shrink-0 items-center gap-3 text-xs text-gray-400 dark:text-gray-500">

                                        {/* 본인 대댓글 */}
                                        {reply.user_id === myUserId ? (
                                            <>
                                              <button
                                                  type="button"
                                                  onClick={() => openEditComment(reply)}
                                                  className="hover:text-cyan-500 transition-colors"
                                                  disabled={isCommentSubmitting}
                                              >
                                                수정
                                              </button>

                                              <span className="text-gray-300 dark:text-gray-600">|</span>

                                              <button
                                                  type="button"
                                                  onClick={() => setCommentDeleteTarget(reply)}
                                                  className="hover:text-red-500 transition-colors"
                                                  disabled={isCommentSubmitting}
                                              >
                                                삭제
                                              </button>
                                            </>
                                        ) : (
                                            /* 남 대댓글 */
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    openReportDialog([
                                                      {
                                                        type: "comment",
                                                        id: reply.id,
                                                        label: "댓글",
                                                      },
                                                    ])
                                                }
                                                className="hover:text-red-500 transition-colors"
                                                disabled={isCommentSubmitting}
                                            >
                                              <Siren className="h-5 w-5"/>
                                            </button>
                                        )}
                                      </div>
                                    </div>

                                    {editingCommentId === reply.id ? (
                                        <div className="space-y-2">
                                <textarea
                                    value={editingCommentContent}
                                    onChange={(event) => setEditingCommentContent(event.target.value)}
                                    rows={3}
                                    className="w-full resize-none rounded-md border border-gray-200 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-cyan-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
                                    disabled={isCommentSubmitting}
                                />

                                          <div className="flex justify-end gap-2">
                                            <Button
                                                type="button"
                                                variant="outline"
                                                size="sm"
                                                onClick={() => {
                                                  setEditingCommentId(null);
                                                  setEditingCommentContent("");
                                                }}
                                                disabled={isCommentSubmitting}
                                            >
                                              취소
                                            </Button>

                                            <Button
                                                type="button"
                                                size="sm"
                                                disabled={isCommentSubmitting}
                                                onClick={() => void handleUpdateComment(reply.id)}
                                            >
                                              저장
                                            </Button>
                                          </div>
                                        </div>
                                    ) : (
                                        <p className="ml-0.5 whitespace-pre-wrap text-sm leading-6 text-gray-700 dark:text-gray-300">
                                          {reply.content}
                                        </p>
                                    )}
                                  </div>
                              ))}
                            </div>
                          </div>
                      ))
                  )}
                </div>
              </div>
            )}

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

          <AlertDialog
            open={postDeleteTarget !== null}
            onOpenChange={(open) => {
              if (!open && !isSubmitting) {
                setPostDeleteTarget(null);
              }
            }}
          >
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>게시글을 삭제하시겠습니까?</AlertDialogTitle>
                <AlertDialogDescription>
                  삭제한 게시글은 다시 복구할 수 없습니다.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel disabled={isSubmitting}>취소</AlertDialogCancel>
                <AlertDialogAction
                  disabled={isSubmitting || !postDeleteTarget}
                  onClick={() => {
                    if (postDeleteTarget) {
                      void deletePostById(postDeleteTarget.id);
                    }
                  }}
                  className="bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40"
                >
                  {isSubmitting ? "삭제 중..." : "삭제"}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>

          <AlertDialog
            open={commentDeleteTarget !== null}
            onOpenChange={(open) => {
              if (!open && !isCommentSubmitting) {
                setCommentDeleteTarget(null);
              }
            }}
          >
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>댓글을 삭제하시겠습니까?</AlertDialogTitle>
                <AlertDialogDescription>
                  삭제한 댓글은 다시 복구할 수 없습니다.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel disabled={isCommentSubmitting}>취소</AlertDialogCancel>
                <AlertDialogAction
                  disabled={isCommentSubmitting || !commentDeleteTarget}
                  onClick={() => {
                    if (commentDeleteTarget) {
                      void handleDeleteComment(commentDeleteTarget.id);
                    }
                  }}
                  className="bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40"
                >
                  {isCommentSubmitting ? "삭제 중..." : "삭제"}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>

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
                      maxLength={POST_TITLE_MAX_LENGTH}
                      className="h-11 text-base"
                    />
                    <span className="block text-right text-xs text-gray-400 dark:text-gray-500">
                      {editTitle.length}/{POST_TITLE_MAX_LENGTH}
                    </span>
                  </label>

                  <label className="block space-y-1.5">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">내용</span>
                    <textarea
                      value={editContent}
                      onChange={(event) => setEditContent(event.target.value)}
                      placeholder="내용을 입력하세요"
                      rows={7}
                      maxLength={POST_CONTENT_MAX_LENGTH}
                      className="w-full resize-none rounded-md border border-gray-200 bg-white px-3 py-3 text-base text-gray-900 outline-none focus:border-cyan-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
                    />
                    <span className="block text-right text-xs text-gray-400 dark:text-gray-500">
                      {editContent.length}/{POST_CONTENT_MAX_LENGTH}
                    </span>
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
                      className="spentopia-primary-button"
                    >
                      {isSubmitting ? "수정 중" : "수정"}
                    </Button>
                  </div>
                </form>
              </Card>
            </div>
          )}

          <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />
          {/* 신고 모달
              ------------------------------------------------
              reportTargets에 값이 있으면 모달 오픈.

              게시글 신고:
              - 게시글
              - 닉네임
              - 프로필 사진

              댓글 신고:
              - 댓글
          ------------------------------------------------ */}
          {reportTargets && (
              <ReportDialog
                  open={!!reportTargets}
                  onOpenChange={(open) => {
                    if (!open) {
                      setReportTargets(null);
                    }
                  }}
                  targets={reportTargets}
              />
          )}
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
                className="spentopia-primary-button h-11 px-5 text-base"
            >
              <Send className="mr-2 h-4 w-4" />
              글쓰기
            </Button>
          </div>

          {/* 탭 바 */}
          <div className="flex border-b-2 border-gray-200 dark:border-gray-700">
            {TABS.map((tab) => (
              <button
                key={tab.key}
                onClick={() => handleTabChange(tab.key)}
                className={`flex-1 text-center py-3 text-sm font-medium border-b-2 -mb-0.5 transition-colors whitespace-nowrap ${
                  activeTab === tab.key
                    ? "border-gray-900 dark:border-gray-100 text-gray-900 dark:text-gray-100 font-bold"
                    : "border-transparent text-gray-400 dark:text-gray-500 hover:text-gray-700 dark:hover:text-gray-300"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* 정렬 */}
          <div className="flex justify-end items-center py-3 pr-3 gap-1">
            {(
              [
                { key: "date"  as PostSort, label: "최신순" },
                { key: "likes" as PostSort, label: "추천순" },
                { key: "views" as PostSort, label: "조회순" },
              ] as { key: PostSort; label: string }[]
            ).map(({ key, label }) => (
              <button
                key={key}
                onClick={() => { setSort(key); setCurrentPage(1); }}
                className={`text-sm px-3 py-1.5 transition-colors ${
                  sort === key
                    ? "text-gray-800 dark:text-gray-100 font-bold"
                    : "text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300"
                }`}
              >
                {sort === key ? `✓ ${label}` : label}
              </button>
            ))}
          </div>

          {/* 게시글 리스트 */}
          <div className="border border-t-0 border-gray-200 dark:border-gray-700 rounded-b-xl overflow-hidden spentopia-surface-card backdrop-blur-xl">
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

                          {/* 주황색 점 */}
                          {post.isNew && post.user_id !== myUserId && !readPostIds.has(post.id) && (
                              <span className="w-2 h-2 rounded-full bg-orange-400 flex-shrink-0 inline-block" />
                          )}</span>

                        {/* 작성자 · 좋아요 · 날짜 · 조회수 */}
                        <span className="grid h-5 grid-cols-[200px_270px] items-center text-sm text-gray-400 dark:text-gray-500 flex-shrink-0 min-w-[470px]">
                          <span className="block h-5 truncate text-left leading-5">{post.author}</span>

                          <span className="flex h-5 items-center justify-end gap-8">
                            <span className="grid h-5 grid-cols-[16px_40px] items-center gap-1">
                              <Heart
                                  className={`relative top-px block h-3.5 w-3.5 ${
                                      post.isReacted ? "fill-current text-red-500" : ""
                                  }`}
                              />
                              <span className="block h-5 tabular-nums text-left leading-5">{post.likes.toLocaleString()}</span>
                            </span>

                            <span className="block h-5 -translate-x-2 text-center tabular-nums leading-5">{post.date}</span>

                            <span className="grid h-5 grid-cols-[16px_40px] items-center gap-1">
                              <Eye className="relative top-px block h-3.5 w-3.5" />
                              <span className="block h-5 tabular-nums text-left leading-5">
                                {post.views.toLocaleString()}
                              </span>
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
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
            <div className="w-full max-w-2xl bg-white dark:bg-gray-900 rounded-lg shadow-2xl flex flex-col max-h-[90vh] border border-gray-200 dark:border-gray-700">

              {/* 헤더 */}
              <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 rounded-t-lg">
                <h2 className="text-sm font-bold text-gray-800 dark:text-gray-100">글쓰기</h2>
                <button
                  type="button"
                  onClick={() => { setIsWriteOpen(false); resetWriteForm(); }}
                  disabled={isSubmitting}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                >
                  <X className="h-4.5 w-4.5" />
                </button>
              </div>

              <form onSubmit={handleWriteSubmit} className="flex flex-col flex-1 min-h-0">
                <div className="flex-1 overflow-y-auto divide-y divide-gray-100 dark:divide-gray-700/60">

                  {/* 게시판 선택 */}
                  <div className="flex min-h-[52px]">
                    <span className="w-24 flex-shrink-0 flex items-center justify-center text-xs font-semibold text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700">
                      게시판
                    </span>
                    <div className="flex-1 flex items-center flex-wrap gap-2 px-4 py-3">
                      {(
                        [
                          { type: "request" as PostType, label: "이거 만들어주세요", badgeKey: "item" as PostCategory },
                          { type: "contest" as PostType, label: "아바타 콘테스트",   badgeKey: "contest" as PostCategory },
                          { type: "free" as PostType, label: "자유", badgeKey: "free" as PostCategory },
                          ...(myRoleType === "admin"
                            ? [{ type: "notice" as PostType, label: "공지사항", badgeKey: "notice" as PostCategory }]
                            : []),
                        ] as { type: PostType; label: string; badgeKey: PostCategory }[]
                      ).map(({ type, label, badgeKey }) => {
                        const badge = BADGE_STYLE[badgeKey];
                        const active = writeType === type;
                        return (
                          <button
                            key={type}
                            type="button"
                            onClick={() => setWriteType(type)}
                            className={`px-3 py-1 rounded text-xs font-medium border transition-all ${
                              active
                                ? `${badge.bg} ${badge.text} border-transparent`
                                : "bg-white dark:bg-gray-900 text-gray-400 border-gray-200 dark:border-gray-600 hover:border-gray-400 dark:hover:border-gray-500 hover:text-gray-600 dark:hover:text-gray-300"
                            }`}
                          >
                            {label}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* 콘테스트 선택 */}
                  {writeType === "contest" && (
                    <div className="flex min-h-[52px]">
                      <span className="w-24 flex-shrink-0 flex items-center justify-center text-xs font-semibold text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700">
                        콘테스트
                      </span>
                      <div className="flex-1 flex items-center px-4 py-3">
                        <select
                          value={writeContestId}
                          onChange={(event) => setWriteContestId(event.target.value)}
                          className="h-8 w-full max-w-xs rounded border border-gray-200 bg-white px-2 text-sm text-gray-900 outline-none focus:border-cyan-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
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
                      </div>
                    </div>
                  )}

                  {/* 제목 */}
                  <div className="flex min-h-[52px]">
                    <span className="w-24 flex-shrink-0 flex items-center justify-center text-xs font-semibold text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700">
                      제목
                    </span>
                    <input
                      type="text"
                      value={writeTitle}
                      onChange={(event) => setWriteTitle(event.target.value)}
                      placeholder="제목을 입력하세요"
                      maxLength={POST_TITLE_MAX_LENGTH}
                      className="flex-1 px-4 py-3.5 text-sm text-gray-900 dark:text-gray-100 bg-transparent outline-none placeholder:text-gray-300 dark:placeholder:text-gray-600"
                    />
                    <span className="flex w-16 flex-shrink-0 items-center justify-end pr-4 text-xs text-gray-400 dark:text-gray-500">
                      {writeTitle.length}/{POST_TITLE_MAX_LENGTH}
                    </span>
                  </div>

                  {/* 내용 */}
                  <div className="flex flex-col">
                    <textarea
                      value={writeContent}
                      onChange={(event) => setWriteContent(event.target.value)}
                      placeholder="내용을 입력하세요"
                      rows={13}
                      maxLength={POST_CONTENT_MAX_LENGTH}
                      className="w-full resize-none px-5 py-4 text-sm leading-7 text-gray-700 dark:text-gray-300 bg-transparent outline-none placeholder:text-gray-300 dark:placeholder:text-gray-600"
                    />
                    <span className="px-5 pb-3 text-right text-xs text-gray-400 dark:text-gray-500">
                      {writeContent.length}/{POST_CONTENT_MAX_LENGTH}
                    </span>
                  </div>

                  {/* 첨부파일 */}
                  <div className="flex min-h-[52px]">
                    <span className="w-24 flex-shrink-0 flex items-center justify-center text-xs font-semibold text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700">
                      첨부파일
                    </span>
                    <div className="flex-1 flex items-center gap-3 px-4 py-3">
                      <label className="flex items-center gap-1.5 cursor-pointer px-3 py-1.5 rounded border border-gray-200 dark:border-gray-600 text-xs text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors select-none">
                        <Paperclip className="h-3.5 w-3.5" />
                        이미지 첨부
                        <input
                          type="file"
                          accept="image/png,image/jpeg,image/webp"
                          className="sr-only"
                          onChange={(event) => setWriteFile(event.target.files?.[0] ?? null)}
                        />
                      </label>
                      {writeFile && (
                        <div className="flex items-center gap-1.5 min-w-0">
                          <span className="text-xs text-cyan-600 dark:text-cyan-400 truncate max-w-[180px]">
                            {writeFile.name}
                          </span>
                          <button
                            type="button"
                            onClick={() => setWriteFile(null)}
                            className="text-gray-300 hover:text-red-400 transition-colors flex-shrink-0"
                          >
                            <X className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                </div>

                {/* 하단 버튼 */}
                <div className="flex items-center justify-end gap-2 px-5 py-3.5 bg-gray-50 dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 rounded-b-lg">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => { setIsWriteOpen(false); resetWriteForm(); }}
                    disabled={isSubmitting}
                    className="h-9 px-5 text-sm"
                  >
                    취소
                  </Button>
                  <Button
                    type="submit"
                    disabled={isSubmitting}
                    className="h-9 spentopia-primary-button px-5 text-sm"
                  >
                    {isSubmitting ? "등록 중..." : "등록"}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        )}

        <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />
      </div>
  );
}
