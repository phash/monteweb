import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const mockActivate = vi.fn()
const mockDeactivate = vi.fn()
const mockSubscribe = vi.fn()

vi.mock('@stomp/stompjs', () => {
  return {
    Client: function (this: any, config: any) {
      this.brokerURL = config.brokerURL
      this.reconnectDelay = config.reconnectDelay
      this.onConnect = null
      this.onStompError = null
      this.activate = mockActivate
      this.deactivate = mockDeactivate
      this.subscribe = mockSubscribe
    },
  }
})

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
    mockActivate.mockClear()
    mockDeactivate.mockClear()
  })

  it('should return connect, disconnect, connected', () => {
    const { connected, connect, disconnect } = useWebSocket()
    expect(connected.value).toBe(false)
    expect(typeof connect).toBe('function')
    expect(typeof disconnect).toBe('function')
  })

  it('should create STOMP client and activate on connect', () => {
    const { connect } = useWebSocket()
    connect('user-1')
    expect(mockActivate).toHaveBeenCalled()
  })

  it('should not disconnect when not connected', () => {
    const { disconnect, connected } = useWebSocket()
    expect(() => disconnect()).not.toThrow()
    expect(connected.value).toBe(false)
  })
})
