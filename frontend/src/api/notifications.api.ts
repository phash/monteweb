import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { NotificationInfo } from '@/types/notification'

export const notificationsApi = {
  getNotifications(page = 0, size = 20) {
    return client.get<ApiResponse<PageResponse<NotificationInfo>>>('/notifications', { params: { page, size } })
  },

  getUnreadCount() {
    return client.get<ApiResponse<{ count: number }>>('/notifications/unread-count')
  },

  markAsRead(id: string) {
    return client.put<ApiResponse<void>>(`/notifications/${id}/read`)
  },

  markAllAsRead() {
    return client.put<ApiResponse<void>>('/notifications/read-all')
  },

  deleteNotification(id: string) {
    return client.delete<ApiResponse<void>>(`/notifications/${id}`)
  },
}
