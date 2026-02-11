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
  },
}))

import { messagingApi } from '@/api/messaging.api'

describe('Messaging Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should start with empty state', () => {
    const store = useMessagingStore()
    expect(store.conversations).toEqual([])
    expect(store.messages).toEqual([])
    expect(store.unreadCount).toBe(0)
  })

  it('should fetch conversations', async () => {
    const store = useMessagingStore()
    const mockConversations = [
      { id: '1', name: 'Conv 1', unreadCount: 2 },
      { id: '2', name: 'Conv 2', unreadCount: 0 },
    ]

    vi.mocked(messagingApi.getConversations).mockResolvedValue({
      data: { data: mockConversations },
    } as any)

    await store.fetchConversations()

    expect(store.conversations).toHaveLength(2)
    expect(store.conversations[0].id).toBe('1')
  })

  it('should fetch and reverse messages', async () => {
    const store = useMessagingStore()
    const mockMessages = [
      { id: '3', content: 'Newest', conversationId: '1' },
      { id: '2', content: 'Middle', conversationId: '1' },
      { id: '1', content: 'Oldest', conversationId: '1' },
    ]

    vi.mocked(messagingApi.getMessages).mockResolvedValue({
      data: { data: { content: mockMessages } },
    } as any)

    await store.fetchMessages('1')

    expect(store.messages).toHaveLength(3)
    expect(store.messages[0].content).toBe('Oldest')
    expect(store.messages[2].content).toBe('Newest')
  })

  it('should send message and append to list', async () => {
    const store = useMessagingStore()
    const sentMessage = { id: '5', content: 'Hello!', conversationId: '1' }

    vi.mocked(messagingApi.sendMessage).mockResolvedValue({
      data: { data: sentMessage },
    } as any)

    const result = await store.sendMessage('1', 'Hello!')

    expect(result.content).toBe('Hello!')
    expect(store.messages).toHaveLength(1)
  })

  it('should start direct conversation', async () => {
    const store = useMessagingStore()
    const newConv = { id: 'new-1', name: 'Direct', unreadCount: 0 }

    vi.mocked(messagingApi.startConversation).mockResolvedValue({
      data: { data: newConv },
    } as any)

    const result = await store.startDirectConversation('user-123')

    expect(result.id).toBe('new-1')
    expect(store.conversations).toHaveLength(1)
  })

  it('should not duplicate conversation on start', async () => {
    const store = useMessagingStore()
    const existingConv = { id: 'existing-1', name: 'Existing', unreadCount: 0 }
    store.conversations = [existingConv] as any

    vi.mocked(messagingApi.startConversation).mockResolvedValue({
      data: { data: existingConv },
    } as any)

    await store.startDirectConversation('user-456')

    expect(store.conversations).toHaveLength(1)
  })

  it('should handle incoming messages', () => {
    const store = useMessagingStore()
    store.conversations = [
      { id: 'conv-1', unreadCount: 0, lastMessage: '', lastMessageAt: '' },
    ] as any
    store.currentConversation = { id: 'conv-1' } as any

    store.addIncomingMessage({
      id: 'msg-1',
      content: 'Incoming!',
      conversationId: 'conv-1',
      createdAt: new Date().toISOString(),
    } as any)

    expect(store.messages).toHaveLength(1)
    expect(store.messages[0].content).toBe('Incoming!')
    expect(store.unreadCount).toBe(1)
  })

  it('should mark conversation as read', async () => {
    const store = useMessagingStore()
    store.conversations = [
      { id: 'conv-1', unreadCount: 3 },
    ] as any
    store.unreadCount = 5

    vi.mocked(messagingApi.markAsRead).mockResolvedValue({} as any)

    await store.markAsRead('conv-1')

    expect(store.unreadCount).toBe(2)
    expect(store.conversations[0].unreadCount).toBe(0)
  })
})
