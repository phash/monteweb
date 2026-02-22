import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ConversationInfo, MessageInfo, StartConversationRequest } from '@/types/messaging'
import { authenticatedImageUrl } from '@/composables/useImageToken'

export const messagingApi = {
  getConversations() {
    return client.get<ApiResponse<ConversationInfo[]>>('/messages/conversations')
  },

  getConversation(id: string) {
    return client.get<ApiResponse<ConversationInfo>>(`/messages/conversations/${id}`)
  },

  startConversation(data: StartConversationRequest) {
    return client.post<ApiResponse<ConversationInfo>>('/messages/conversations', data)
  },

  getMessages(conversationId: string, page = 0, size = 50) {
    return client.get<ApiResponse<PageResponse<MessageInfo>>>(`/messages/conversations/${conversationId}/messages`, {
      params: { page, size },
    })
  },

  sendMessage(conversationId: string, content?: string, image?: File, replyToId?: string) {
    const formData = new FormData()
    if (content) formData.append('content', content)
    if (image) formData.append('image', image)
    if (replyToId) formData.append('replyToId', replyToId)
    return client.post<ApiResponse<MessageInfo>>(
      `/messages/conversations/${conversationId}/messages`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
  },

  markAsRead(conversationId: string) {
    return client.put<ApiResponse<void>>(`/messages/conversations/${conversationId}/read`)
  },

  deleteConversation(conversationId: string) {
    return client.delete<ApiResponse<void>>(`/messages/conversations/${conversationId}`)
  },

  getUnreadCount() {
    return client.get<ApiResponse<{ count: number }>>('/messages/unread-count')
  },

  muteConversation(conversationId: string) {
    return client.post<ApiResponse<void>>(`/messages/conversations/${conversationId}/mute`)
  },

  unmuteConversation(conversationId: string) {
    return client.post<ApiResponse<void>>(`/messages/conversations/${conversationId}/unmute`)
  },

  imageUrl(imageId: string) {
    return authenticatedImageUrl(`/api/v1/messages/images/${imageId}`)
  },

  thumbnailUrl(imageId: string) {
    return authenticatedImageUrl(`/api/v1/messages/images/${imageId}/thumbnail`)
  },
}
