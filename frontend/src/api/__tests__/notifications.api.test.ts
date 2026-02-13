import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: {} } }),
    post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import client from '../client'
import { notificationsApi } from '../notifications.api'

describe('notificationsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getNotifications', () => {
    it('should GET /notifications with default pagination', async () => {
      await notificationsApi.getNotifications()
      expect(client.get).toHaveBeenCalledWith('/notifications', {
        params: { page: 0, size: 20 },
      })
    })

    it('should GET /notifications with custom pagination', async () => {
      await notificationsApi.getNotifications(3, 50)
      expect(client.get).toHaveBeenCalledWith('/notifications', {
        params: { page: 3, size: 50 },
      })
    })
  })

  describe('getUnreadCount', () => {
    it('should GET /notifications/unread-count', async () => {
      await notificationsApi.getUnreadCount()
      expect(client.get).toHaveBeenCalledWith('/notifications/unread-count')
    })
  })

  describe('markAsRead', () => {
    it('should PUT /notifications/{id}/read', async () => {
      await notificationsApi.markAsRead('notif-1')
      expect(client.put).toHaveBeenCalledWith('/notifications/notif-1/read')
    })
  })

  describe('markAllAsRead', () => {
    it('should PUT /notifications/read-all', async () => {
      await notificationsApi.markAllAsRead()
      expect(client.put).toHaveBeenCalledWith('/notifications/read-all')
    })
  })

  describe('deleteNotification', () => {
    it('should DELETE /notifications/{id}', async () => {
      await notificationsApi.deleteNotification('notif-1')
      expect(client.delete).toHaveBeenCalledWith('/notifications/notif-1')
    })
  })
})
