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

  // Push notifications
  getPushPublicKey() {
    return client.get<ApiResponse<{ publicKey: string }>>('/notifications/push/public-key')
  },

  pushSubscribe(data: { endpoint: string; p256dh?: string; auth?: string }) {
    return client.post<ApiResponse<void>>('/notifications/push/subscribe', data)
  },

  pushUnsubscribe(data: { endpoint: string }) {
    return client.post<ApiResponse<void>>('/notifications/push/unsubscribe', data)
  },
}
