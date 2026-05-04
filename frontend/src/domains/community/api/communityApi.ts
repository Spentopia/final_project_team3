import { apiClient } from "@/shared/api/client.ts";

export type PostSort = "date" | "likes" | "views";
export type PostType = "notice" | "request" | "contest" | "free";

export interface CommunityPostResponse {
  id: string;
  user_id: string;
  author_nickname: string | null;
  author_profile_image: string | null;
  author_profile_image_url: string | null;
  contest_id: string | null;
  post_type: PostType;
  title: string;
  image_url: string | null;
  content: string | null;
  reaction_count: number | null;
  // 현재 로그인한 사용자가 이미 반응했는지 여부.
  // 프론트에서 버튼 상태를 바꾸기 위해 필요함.
  is_reacted: boolean;
  view_count: number;
  created_at: string | null;
}

export interface CommunityPostListResponse {
  items: CommunityPostResponse[];
  total_count: number;
}

export interface ContestResponse {
  id: string;
  title: string;
  description: string | null;
  start_date: string;
  end_date: string;
  status: string | null;
  reward_description: string | null;
}

export interface CreateCommunityPostRequest {
  post_type: PostType;
  title: string;
  contest_id?: string | null;
  image_url?: string | null;
  content?: string | null;
}

export interface UpdateCommunityPostRequest {
  title?: string;
  image_url?: string;
  content?: string;
}

export interface CommunityMeResponse {
  id: string;
  role_type: "user" | "admin" | string;
}

export interface CommentResponse {
  id: string;
  post_id: string;
  parent_id : string | null;
  user_id: string;
  author_nickname: string | null;
  author_profile_image: string | null;
  author_profile_image_url: string | null;
  content: string;
  created_at: string | null;
  updated_at: string | null;
}

interface ListPostsParams {
  sort: PostSort;
  postType?: PostType;
  contestId?: string;
  title?: string;
  page?: number;
  pageSize?: number;
}

export async function listCommunityPosts({
  sort,
  postType,
  contestId,
  title,
  page,
  pageSize,
}: ListPostsParams): Promise<CommunityPostListResponse> {
  const res = await apiClient.get<CommunityPostListResponse>("/api/posts", {
    params: {
      sort,
      post_type: postType,
      contest_id: contestId,
      title,
      page,
      page_size: pageSize,
    },
  });

  return res.data;
}

export async function getCommunityPost(postId: string): Promise<CommunityPostResponse> {
  const res = await apiClient.get<CommunityPostResponse>(`/api/posts/${postId}`);
  return res.data;
}

export async function getCommunityMe(): Promise<CommunityMeResponse> {
  const res = await apiClient.get<CommunityMeResponse>("/me");
  return res.data;
}

export async function listContests(): Promise<ContestResponse[]> {
  const res = await apiClient.get<ContestResponse[]>("/api/contests");
  return res.data;
}

export async function createCommunityPost(
  payload: CreateCommunityPostRequest
): Promise<CommunityPostResponse> {
  const res = await apiClient.post<CommunityPostResponse>("/api/posts", payload);
  return res.data;
}

export async function updateCommunityPost(
  postId: string,
  payload: UpdateCommunityPostRequest
): Promise<CommunityPostResponse> {
  const res = await apiClient.patch<CommunityPostResponse>(`/api/posts/${postId}`, payload);
  return res.data;
}

export async function deleteCommunityPost(postId: string): Promise<void> {
  await apiClient.delete(`/api/posts/${postId}`);
}

// 게시글 반응 등록
// - contest 게시글: 투표 등록
// - free/request 게시글: 좋아요 등록
export async function reactCommunityPost(postId: string): Promise<void> {
  await apiClient.post(`/api/posts/${postId}/react`);
}

// 게시글 반응 취소
// - free/request 게시글: 좋아요 취소
// - contest 게시글: 백엔드 정책상 취소 불가
export async function unreactCommunityPost(postId: string): Promise<void> {
  await apiClient.delete(`/api/posts/${postId}/react`);
}

export async function listComments(postId: string): Promise<CommentResponse[]> {
  const res = await apiClient.get<CommentResponse[]>(`/api/posts/${postId}/comments`);
  return res.data;
}

export async function createComment(
  postId: string,
  content: string,
  parentId?: string | null
): Promise<CommentResponse> {
  const res = await apiClient.post<CommentResponse>(`/api/posts/${postId}/comments`, {
    content,
    parent_id: parentId ?? null
  });
  return res.data;
}

export async function updateComment(
  commentId: string,
  content: string
): Promise<CommentResponse> {
  const res = await apiClient.patch<CommentResponse>(`/api/comments/${commentId}`, {
    content,
  });
  return res.data;
}

export async function deleteComment(commentId: string): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`);
}

type CommunityUploadTarget =
  | {
      postType: "contest";
      contestId: string;
    }
  | {
      postType: "notice";
      postId: string;
    }
  | {
      postType: "request";
    }
  | {
      postType: "free";
    };

interface UploadCommunityImageParams {
  file: File;
  target: CommunityUploadTarget;
}

interface UploadCommunityImageResult {
  path: string;
}

export async function uploadCommunityImage({
  file,
  target,
}: UploadCommunityImageParams): Promise<UploadCommunityImageResult> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("post_type", target.postType);

  if (target.postType === "contest") {
    formData.append("contest_id", target.contestId);
  }

  if (target.postType === "notice") {
    formData.append("post_id", target.postId);
  }

  const res = await apiClient.post<UploadCommunityImageResult>(
    "/api/posts/image/upload",
    formData
  );

  return res.data;
}
