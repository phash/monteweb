import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ConversationInfo, MessageInfo, StartConversationRequest } from '@/types/messaging'

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

  sendMessage(conversationId: string, content: string) {
    return client.post<ApiResponse<MessageInfo>>(`/messages/conversations/${conversationId}/messages`, { content })
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
}
