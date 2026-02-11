import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/notifications.api', () => ({
  notificationsApi: {
    getNotifications: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    getUnreadCount: vi.fn().mockResolvedValue({ data: { data: { count: 0 } } }),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}))

vi.mock('@/api/messaging.api', () => ({
  messagingApi: {
    getConversations: vi.fn(),
    sendMessage: vi.fn(),
  },
}))

import { useWebSocket } from '@/composables/useWebSocket'

describe('useWebSocket', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should return connect, disconnect, connected', () => {
    const { connected, connect, disconnect } = useWebSocket()
    expect(connected.value).toBe(false)
    expect(typeof connect).toBe('function')
    expect(typeof disconnect).toBe('function')
  })

  it('should warn when SockJS not available', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const { connect } = useWebSocket()
    connect('user-1')
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('WebSocket libraries not loaded')
    )
    warnSpy.mockRestore()
  })

  it('should not disconnect when not connected', () => {
    const { disconnect, connected } = useWebSocket()
    expect(() => disconnect()).not.toThrow()
    expect(connected.value).toBe(false)
  })
})
