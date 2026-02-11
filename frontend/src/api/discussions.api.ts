import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { DiscussionThread, DiscussionReply } from '@/types/discussion'

export const discussionsApi = {
  getThreads(roomId: string, status?: string, page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<DiscussionThread>>>(`/rooms/${roomId}/threads`, {
      params: { status, page, size },
    })
  },

  getThread(roomId: string, threadId: string) {
    return client.get<ApiResponse<DiscussionThread>>(`/rooms/${roomId}/threads/${threadId}`)
  },

  createThread(roomId: string, title: string, content?: string) {
    return client.post<ApiResponse<DiscussionThread>>(`/rooms/${roomId}/threads`, { title, content })
  },

  archiveThread(roomId: string, threadId: string) {
    return client.put<ApiResponse<DiscussionThread>>(`/rooms/${roomId}/threads/${threadId}/archive`)
  },

  deleteThread(roomId: string, threadId: string) {
    return client.delete<ApiResponse<void>>(`/rooms/${roomId}/threads/${threadId}`)
  },

  getReplies(roomId: string, threadId: string, page = 0, size = 50) {
    return client.get<ApiResponse<PageResponse<DiscussionReply>>>(`/rooms/${roomId}/threads/${threadId}/replies`, {
      params: { page, size },
    })
  },

  addReply(roomId: string, threadId: string, content: string) {
    return client.post<ApiResponse<DiscussionReply>>(`/rooms/${roomId}/threads/${threadId}/replies`, { content })
  },
}
