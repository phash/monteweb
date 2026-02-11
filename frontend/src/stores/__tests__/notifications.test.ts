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

describe('Notifications Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useNotificationsStore()
    expect(store.notifications).toEqual([])
    expect(store.unreadCount).toBe(0)
    expect(store.loading).toBe(false)
  })

  it('should fetch notifications', async () => {
    const store = useNotificationsStore()
    const mockNotifications = [
      { id: 'n1', message: 'New post', read: false },
      { id: 'n2', message: 'New reply', read: true },
    ]

    vi.mocked(notificationsApi.getNotifications).mockResolvedValue({
      data: { data: { content: mockNotifications } },
    } as any)

    await store.fetchNotifications()

    expect(store.notifications).toHaveLength(2)
  })

  it('should fetch unread count', async () => {
    const store = useNotificationsStore()

    vi.mocked(notificationsApi.getUnreadCount).mockResolvedValue({
      data: { data: { count: 5 } },
    } as any)

    await store.fetchUnreadCount()

    expect(store.unreadCount).toBe(5)
  })

  it('should mark as read and decrement counter', async () => {
    const store = useNotificationsStore()
    store.notifications = [
      { id: 'n1', message: 'Unread', read: false },
      { id: 'n2', message: 'Read', read: true },
    ] as any
    store.unreadCount = 3

    vi.mocked(notificationsApi.markAsRead).mockResolvedValue({} as any)

    await store.markAsRead('n1')

    expect(store.notifications[0].read).toBe(true)
    expect(store.unreadCount).toBe(2)
  })

  it('should not decrement counter for already-read notification', async () => {
    const store = useNotificationsStore()
    store.notifications = [
      { id: 'n1', message: 'Already read', read: true },
    ] as any
    store.unreadCount = 2

    vi.mocked(notificationsApi.markAsRead).mockResolvedValue({} as any)

    await store.markAsRead('n1')

    expect(store.unreadCount).toBe(2)
  })

  it('should mark all as read and reset counter', async () => {
    const store = useNotificationsStore()
    store.notifications = [
      { id: 'n1', read: false },
      { id: 'n2', read: false },
    ] as any
    store.unreadCount = 2

    vi.mocked(notificationsApi.markAllAsRead).mockResolvedValue({} as any)

    await store.markAllAsRead()

    expect(store.notifications.every(n => n.read)).toBe(true)
    expect(store.unreadCount).toBe(0)
  })

  it('should add notification and increment counter', () => {
    const store = useNotificationsStore()
    store.unreadCount = 1

    store.addNotification({
      id: 'n-new',
      message: 'New!',
      read: false,
    } as any)

    expect(store.notifications).toHaveLength(1)
    expect(store.notifications[0].id).toBe('n-new')
    expect(store.unreadCount).toBe(2)
  })
})
