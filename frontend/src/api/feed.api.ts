import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { FeedPost, FeedComment, SystemBanner, CreatePostRequest, CreateCommentRequest, ReactionSummary, LinkPreviewInfo } from '@/types/feed'

export const feedApi = {
  getFeed(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<FeedPost>>>('/feed', { params: { page, size } })
  },

  getBanners() {
    return client.get<ApiResponse<SystemBanner[]>>('/feed/banners')
  },

  getPost(id: string) {
    return client.get<ApiResponse<FeedPost>>(`/feed/posts/${id}`)
  },

  createPost(data: CreatePostRequest) {
    return client.post<ApiResponse<FeedPost>>('/feed/posts', data)
  },

  updatePost(id: string, data: { title?: string; content: string }) {
    return client.put<ApiResponse<FeedPost>>(`/feed/posts/${id}`, data)
  },

  deletePost(id: string) {
    return client.delete<ApiResponse<void>>(`/feed/posts/${id}`)
  },

  pinPost(id: string) {
    return client.post<ApiResponse<void>>(`/feed/posts/${id}/pin`)
  },

  getComments(postId: string, page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<FeedComment>>>(`/feed/posts/${postId}/comments`, { params: { page, size } })
  },

  addComment(postId: string, data: CreateCommentRequest) {
    return client.post<ApiResponse<FeedComment>>(`/feed/posts/${postId}/comments`, data)
  },

  getRoomPosts(roomId: string, page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<FeedPost>>>(`/feed/rooms/${roomId}/posts`, { params: { page, size } })
  },

  createRoomPost(roomId: string, data: { title?: string; content: string }) {
    return client.post<ApiResponse<FeedPost>>(`/feed/rooms/${roomId}/posts`, data)
  },

  togglePostReaction(postId: string, emoji: string) {
    return client.post<ApiResponse<ReactionSummary[]>>(`/feed/posts/${postId}/reactions`, { emoji })
  },

  toggleCommentReaction(commentId: string, emoji: string) {
    return client.post<ApiResponse<ReactionSummary[]>>(`/feed/comments/${commentId}/reactions`, { emoji })
  },

  getLinkPreview(url: string) {
    return client.get<ApiResponse<LinkPreviewInfo>>('/feed/link-preview', { params: { url } })
  },
}
