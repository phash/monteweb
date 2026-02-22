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
    imageUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}/thumbnail`),
  },
}))

import { messagingApi } from '@/api/messaging.api'

describe('Messaging Store - Extended', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchConversations error handling', () => {
    it('should clear conversations on error', async () => {
      const store = useMessagingStore()
      store.conversations = [{ id: 'old' }] as any

      vi.mocked(messagingApi.getConversations).mockRejectedValue(new Error('Network'))

      await store.fetchConversations()

      expect(store.conversations).toEqual([])
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchMessages loading state', () => {
    it('should set loading during fetch and reset after', async () => {
      const store = useMessagingStore()

      let resolveGet!: Function
      vi.mocked(messagingApi.getMessages).mockReturnValue(
        new Promise((resolve) => { resolveGet = resolve }) as any
      )

      const promise = store.fetchMessages('conv-1')
      expect(store.loading).toBe(true)

      resolveGet({ data: { data: { content: [] } } })
      await promise

      expect(store.loading).toBe(false)
    })

    it('should reset loading even on error', async () => {
      const store = useMessagingStore()

      vi.mocked(messagingApi.getMessages).mockRejectedValue(new Error('Error'))

      try {
        await store.fetchMessages('conv-1')
      } catch {
        // Expected
      }

      expect(store.loading).toBe(false)
    })
  })

  describe('addIncomingMessage edge cases', () => {
    it('should not add to messages if different conversation is active', () => {
      const store = useMessagingStore()
      store.currentConversation = { id: 'other-conv' } as any
      store.conversations = [
        { id: 'conv-1', unreadCount: 0, lastMessage: '', lastMessageAt: '' },
      ] as any

      store.addIncomingMessage({
        id: 'msg-1',
        content: 'Hello',
        conversationId: 'conv-1',
        createdAt: '2026-01-01T00:00:00Z',
      } as any)

      expect(store.messages).toHaveLength(0) // Not added to messages list
      expect(store.conversations[0].unreadCount).toBe(1) // But conversation updated
      expect(store.unreadCount).toBe(1)
    })

    it('should not update conversation if not in list', () => {
      const store = useMessagingStore()
      store.currentConversation = null
      store.conversations = []

      store.addIncomingMessage({
        id: 'msg-1',
        content: 'Hello',
        conversationId: 'unknown-conv',
        createdAt: '2026-01-01T00:00:00Z',
      } as any)

      expect(store.messages).toHaveLength(0)
      expect(store.unreadCount).toBe(0)
    })

    it('should update lastMessage and lastMessageAt on conversation', () => {
      const store = useMessagingStore()
      store.conversations = [
        { id: 'conv-1', unreadCount: 0, lastMessage: 'Old', lastMessageAt: '2025-01-01' },
      ] as any

      store.addIncomingMessage({
        id: 'msg-1',
        content: 'New message',
        conversationId: 'conv-1',
        createdAt: '2026-02-12T10:00:00Z',
      } as any)

      expect(store.conversations[0].lastMessage).toBe('New message')
      expect(store.conversations[0].lastMessageAt).toBe('2026-02-12T10:00:00Z')
    })
  })

  describe('fetchUnreadCount', () => {
    it('should set unread count on success', async () => {
      const store = useMessagingStore()

      vi.mocked(messagingApi.getUnreadCount).mockResolvedValue({
        data: { data: { count: 7 } },
      } as any)

      await store.fetchUnreadCount()

      expect(store.unreadCount).toBe(7)
    })

    it('should silently handle errors', async () => {
      const store = useMessagingStore()
      store.unreadCount = 3

      vi.mocked(messagingApi.getUnreadCount).mockRejectedValue(new Error('Network'))

      await store.fetchUnreadCount()

      // Should not throw and value should remain unchanged
      expect(store.unreadCount).toBe(3)
    })
  })

  describe('markAsRead edge cases', () => {
    it('should not go below 0 for unreadCount', async () => {
      const store = useMessagingStore()
      store.conversations = [
        { id: 'conv-1', unreadCount: 10 },
      ] as any
      store.unreadCount = 5 // Less than conversation unread

      vi.mocked(messagingApi.markAsRead).mockResolvedValue({} as any)

      await store.markAsRead('conv-1')

      expect(store.unreadCount).toBe(0) // Math.max(0, 5 - 10) = 0
    })

    it('should do nothing for unknown conversation', async () => {
      const store = useMessagingStore()
      store.conversations = []
      store.unreadCount = 5

      vi.mocked(messagingApi.markAsRead).mockResolvedValue({} as any)

      await store.markAsRead('unknown')

      expect(store.unreadCount).toBe(5) // Unchanged
    })
  })

  describe('fetchConversation', () => {
    it('should set currentConversation', async () => {
      const store = useMessagingStore()
      const conv = { id: 'conv-1', name: 'Test Conv' }

      vi.mocked(messagingApi.getConversation).mockResolvedValue({
        data: { data: conv },
      } as any)

      await store.fetchConversation('conv-1')

      expect(store.currentConversation).toEqual(conv)
    })
  })
})
