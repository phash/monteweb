import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationsStore } from '@/stores/notifications'

vi.mock('@/api/notifications.api', () => ({
  notificationsApi: {
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}))

import { notificationsApi } from '@/api/notifications.api'

describe('Notifications Store - Extended', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('addNotification with read=true', () => {
    it('should not increment unreadCount for already-read notifications', () => {
      const store = useNotificationsStore()
      store.unreadCount = 5

      store.addNotification({
        id: 'n-read',
        message: 'Already read',
        read: true,
      } as any)

      expect(store.notifications).toHaveLength(1)
      expect(store.unreadCount).toBe(5) // Unchanged
    })

    it('should prepend notification to the beginning of the list', () => {
      const store = useNotificationsStore()
      store.notifications = [{ id: 'existing' }] as any

      store.addNotification({
        id: 'new',
        message: 'New!',
        read: false,
      } as any)

      expect(store.notifications[0].id).toBe('new')
      expect(store.notifications[1].id).toBe('existing')
    })
  })

  describe('fetchNotifications loading state', () => {
    it('should set loading during fetch', async () => {
      const store = useNotificationsStore()

      let resolveGet!: Function
      vi.mocked(notificationsApi.getNotifications).mockReturnValue(
        new Promise((resolve) => { resolveGet = resolve }) as any
      )

      const promise = store.fetchNotifications()
      expect(store.loading).toBe(true)

      resolveGet({ data: { data: { content: [] } } })
      await promise

      expect(store.loading).toBe(false)
    })

    it('should reset loading even on error', async () => {
      const store = useNotificationsStore()

      vi.mocked(notificationsApi.getNotifications).mockRejectedValue(new Error('Error'))

      try {
        await store.fetchNotifications()
      } catch {
        // Expected
      }

      expect(store.loading).toBe(false)
    })
  })

  describe('fetchUnreadCount error handling', () => {
    it('should silently handle errors', async () => {
      const store = useNotificationsStore()
      store.unreadCount = 10

      vi.mocked(notificationsApi.getUnreadCount).mockRejectedValue(new Error('Network'))

      await store.fetchUnreadCount()

      // Should not throw and count stays the same
      expect(store.unreadCount).toBe(10)
    })
  })

  describe('markAsRead edge cases', () => {
    it('should not decrement below 0', async () => {
      const store = useNotificationsStore()
      store.notifications = [
        { id: 'n1', read: false },
      ] as any
      store.unreadCount = 0

      vi.mocked(notificationsApi.markAsRead).mockResolvedValue({} as any)

      await store.markAsRead('n1')

      // Math.max(0, -1) = 0
      expect(store.unreadCount).toBe(0)
    })

    it('should not modify state for unknown notification id', async () => {
      const store = useNotificationsStore()
      store.notifications = [
        { id: 'n1', read: false },
      ] as any
      store.unreadCount = 1

      vi.mocked(notificationsApi.markAsRead).mockResolvedValue({} as any)

      await store.markAsRead('unknown')

      expect(store.unreadCount).toBe(1) // Unchanged
      expect(store.notifications[0].read).toBe(false)
    })
  })
})
