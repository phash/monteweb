import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMessagingStore } from '@/stores/messaging'

vi.mock('@/api/messaging.api', () => ({
  messagingApi: {
    getConversations: vi.fn(),
    getConversation: vi.fn(),
    getMessages: vi.fn(),
    sendMessage: vi.fn(),
    startConversation: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
    deleteConversation: vi.fn(),
    muteConversation: vi.fn(),
    unmuteConversation: vi.fn(),
  },
}))

import { messagingApi } from '@/api/messaging.api'

describe('Messaging Store - Error Handling', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should expose error state', () => {
    const store = useMessagingStore()
    expect(store.error).toBeNull()
  })

  it('should set error on fetchConversations failure', async () => {
    const store = useMessagingStore()
    vi.mocked(messagingApi.getConversations).mockRejectedValue({
      response: { data: { message: 'Unauthorized' } },
    })

    await store.fetchConversations()
    expect(store.error).toBe('Unauthorized')
    expect(store.conversations).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('should set fallback error message', async () => {
    const store = useMessagingStore()
    vi.mocked(messagingApi.getConversations).mockRejectedValue(new Error('Network'))

    await store.fetchConversations()
    expect(store.error).toBe('Failed to load conversations')
  })

  it('should clear error on successful fetch', async () => {
    const store = useMessagingStore()
    store.error = 'old error' as any

    vi.mocked(messagingApi.getConversations).mockResolvedValue({
      data: { data: [{ id: '1' }] },
    } as any)

    await store.fetchConversations()
    expect(store.error).toBeNull()
  })

  it('should set error on fetchConversation failure', async () => {
    const store = useMessagingStore()
    vi.mocked(messagingApi.getConversation).mockRejectedValue({
      response: { data: { message: 'Not found' } },
    })

    await expect(store.fetchConversation('1')).rejects.toBeTruthy()
    expect(store.error).toBe('Not found')
    expect(store.currentConversation).toBeNull()
  })

  it('should set error on fetchMessages failure', async () => {
    const store = useMessagingStore()
    vi.mocked(messagingApi.getMessages).mockRejectedValue({
      response: { data: { message: 'Server error' } },
    })

    await expect(store.fetchMessages('1')).rejects.toBeTruthy()
    expect(store.error).toBe('Server error')
    expect(store.messages).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('should set error on sendMessage failure', async () => {
    const store = useMessagingStore()
    vi.mocked(messagingApi.sendMessage).mockRejectedValue({
      response: { data: { message: 'Rate limited' } },
    })

    await expect(store.sendMessage('1', 'Hello')).rejects.toBeTruthy()
    expect(store.error).toBe('Rate limited')
    // Message should not be added on error
    expect(store.messages).toHaveLength(0)
  })
})
