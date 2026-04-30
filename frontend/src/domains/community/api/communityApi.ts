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
  vote_count: number | null;
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

export async function listComments(postId: string): Promise<CommentResponse[]> {
  const res = await apiClient.get<CommentResponse[]>(`/api/posts/${postId}/comments`);
  return res.data;
}

export async function createComment(
  postId: string,
  content: string
): Promise<CommentResponse> {
  const res = await apiClient.post<CommentResponse>(`/api/posts/${postId}/comments`, {
    content,
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
