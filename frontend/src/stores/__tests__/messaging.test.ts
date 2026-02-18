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
    imageUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}/thumbnail`),
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
    expect(store.replyToMessage).toBeNull()
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
      { id: '3', content: 'Newest', conversationId: '1', images: [], replyTo: null },
      { id: '2', content: 'Middle', conversationId: '1', images: [], replyTo: null },
      { id: '1', content: 'Oldest', conversationId: '1', images: [], replyTo: null },
    ]

    vi.mocked(messagingApi.getMessages).mockResolvedValue({
      data: { data: { content: mockMessages } },
    } as any)

    await store.fetchMessages('1')

    expect(store.messages).toHaveLength(3)
    expect(store.messages[0].content).toBe('Oldest')
    expect(store.messages[2].content).toBe('Newest')
  })

  it('should send text message and append to list', async () => {
    const store = useMessagingStore()
    const sentMessage = { id: '5', content: 'Hello!', conversationId: '1', images: [], replyTo: null }

    vi.mocked(messagingApi.sendMessage).mockResolvedValue({
      data: { data: sentMessage },
    } as any)

    const result = await store.sendMessage('1', 'Hello!')

    expect(result.content).toBe('Hello!')
    expect(store.messages).toHaveLength(1)
    expect(messagingApi.sendMessage).toHaveBeenCalledWith('1', 'Hello!', undefined, undefined)
  })

  it('should send message with image', async () => {
    const store = useMessagingStore()
    const file = new File(['test'], 'photo.jpg', { type: 'image/jpeg' })
    const sentMessage = {
      id: '6', content: null, conversationId: '1',
      images: [{ imageId: 'img-1', originalFilename: 'photo.jpg', contentType: 'image/jpeg', fileSize: 4 }],
      replyTo: null,
    }

    vi.mocked(messagingApi.sendMessage).mockResolvedValue({
      data: { data: sentMessage },
    } as any)

    const result = await store.sendMessage('1', undefined, file)

    expect(result.images).toHaveLength(1)
    expect(messagingApi.sendMessage).toHaveBeenCalledWith('1', undefined, file, undefined)
  })

  it('should send reply message', async () => {
    const store = useMessagingStore()
    const replyTarget = {
      id: 'msg-1', content: 'Original', conversationId: '1', senderId: 'u1', senderName: 'User 1',
      createdAt: '', images: [], replyTo: null,
    } as any
    store.setReplyTo(replyTarget)
    expect(store.replyToMessage).toBeTruthy()

    const sentMessage = {
      id: '7', content: 'My reply', conversationId: '1', images: [],
      replyTo: { messageId: 'msg-1', senderId: 'u1', senderName: 'User 1', contentPreview: 'Original', hasImage: false },
    }

    vi.mocked(messagingApi.sendMessage).mockResolvedValue({
      data: { data: sentMessage },
    } as any)

    await store.sendMessage('1', 'My reply', undefined, 'msg-1')

    // replyToMessage should be cleared after send
    expect(store.replyToMessage).toBeNull()
  })

  it('should set and clear reply', () => {
    const store = useMessagingStore()
    const msg = { id: 'msg-1', content: 'Test', conversationId: '1' } as any

    store.setReplyTo(msg)
    expect(store.replyToMessage?.id).toBe('msg-1')

    store.setReplyTo(null)
    expect(store.replyToMessage).toBeNull()
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

  it('should handle incoming text messages', () => {
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
      images: [],
      replyTo: null,
    } as any)

    expect(store.messages).toHaveLength(1)
    expect(store.messages[0].content).toBe('Incoming!')
    expect(store.unreadCount).toBe(1)
    expect(store.conversations[0].lastMessage).toBe('Incoming!')
  })

  it('should handle incoming image-only messages', () => {
    const store = useMessagingStore()
    store.conversations = [
      { id: 'conv-1', unreadCount: 0, lastMessage: '', lastMessageAt: '' },
    ] as any
    store.currentConversation = { id: 'conv-1' } as any

    store.addIncomingMessage({
      id: 'msg-2',
      content: null,
      conversationId: 'conv-1',
      createdAt: new Date().toISOString(),
      images: [{ imageId: 'img-1', originalFilename: 'photo.jpg', contentType: 'image/jpeg', fileSize: 1000 }],
      replyTo: null,
    } as any)

    expect(store.messages).toHaveLength(1)
    expect(store.conversations[0].lastMessage).toBe('\uD83D\uDDBC Bild')
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
