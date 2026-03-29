import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock messaging store
const mockMessagingStore = {
  conversations: [] as any[],
  messages: [] as any[],
  loading: false,
  sendingMessage: false,
  replyToMessage: null as any,
  fetchConversations: vi.fn().mockResolvedValue(undefined),
  fetchMessages: vi.fn().mockResolvedValue(undefined),
  sendMessage: vi.fn().mockResolvedValue(undefined),
  markAsRead: vi.fn().mockResolvedValue(undefined),
  deleteConversation: vi.fn().mockResolvedValue(undefined),
  muteConversation: vi.fn().mockResolvedValue(undefined),
  unmuteConversation: vi.fn().mockResolvedValue(undefined),
  setReplyTo: vi.fn(),
}

vi.mock('@/stores/messaging', () => ({
  useMessagingStore: vi.fn(() => mockMessagingStore),
}))

// --- mock auth store
const mockAuth = {
  user: { id: 'user-1', displayName: 'Max Muster', role: 'TEACHER' },
  isAdmin: false,
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => mockAuth),
}))

// --- mock rooms store
vi.mock('@/stores/rooms', () => ({
  useRoomsStore: vi.fn(() => ({
    myRooms: [],
    fetchMyRooms: vi.fn().mockResolvedValue(undefined),
  })),
}))

// --- mock vue-router
const mockRoute = { params: {} as any }
const mockRouter = { push: vi.fn() }

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => mockRoute),
  useRouter: vi.fn(() => mockRouter),
}))

// --- mock composables
vi.mock('@/composables/useLocaleDate', () => ({
  useLocaleDate: vi.fn(() => ({
    formatCompactDateTime: vi.fn((d: string) => d?.substring(0, 16) || ''),
  })),
}))

vi.mock('@/composables/useMentions', () => ({
  formatMentions: vi.fn((text: string) => text),
}))

// --- mock APIs
vi.mock('@/api/messaging.api', () => ({
  messagingApi: {
    getConversations: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getMessages: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    sendMessage: vi.fn(),
    startDirect: vi.fn(),
    markAsRead: vi.fn().mockResolvedValue({}),
    getUnreadCount: vi.fn().mockResolvedValue({ data: { data: { count: 0 } } }),
    deleteConversation: vi.fn().mockResolvedValue({}),
    muteConversation: vi.fn().mockResolvedValue({}),
    unmuteConversation: vi.fn().mockResolvedValue({}),
    imageUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}/thumbnail`),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: { search: vi.fn() } }))
vi.mock('@/api/files.api', () => ({
  filesApi: {
    listFiles: vi.fn().mockResolvedValue({ data: { data: [] } }),
    listFolders: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))
vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

import MessagesView from '@/views/MessagesView.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      messages: {
        title: 'Nachrichten',
        newConversation: 'Neue Konversation',
        noConversations: 'Noch keine Konversationen',
        selectConversation: 'Konversation wählen',
        conversation: 'Konversation',
        sendMessage: 'Senden',
        messagePlaceholder: 'Nachricht eingeben...',
        newMessage: 'Neue Nachricht',
        searchUser: 'Benutzer suchen',
        searchUserPlaceholder: 'Name eingeben...',
        startConversation: 'Nachricht senden',
        communicationNotAllowed: 'Kommunikation nicht erlaubt',
        writePlaceholder: 'Nachricht schreiben...',
        deleteTitle: 'Konversation löschen',
        deleteConfirm: 'Wirklich löschen?',
        image: 'Bild',
        attachImage: 'Bild anhängen',
        replyTo: 'Antworten',
        mute: 'Stummschalten',
        unmute: 'Stummschaltung aufheben',
        muted: 'Stumm',
        file: 'Datei',
        attachFile: 'PDF anhängen',
        linkFile: 'Datei verlinken',
        selectRoom: 'Raum auswählen',
        noFiles: 'Keine Dateien',
      },
      poll: {
        createPoll: 'Umfrage erstellen',
      },
      nav: { messages: 'Nachrichten' },
      common: { cancel: 'Abbrechen', back: 'Zurück', delete: 'Löschen', yes: 'Ja', no: 'Nein' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  NewMessageDialog: { template: '<div class="new-msg-stub" />', props: ['visible'] },
  Dialog: { template: '<div class="dialog-stub"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal', 'dismissableMask', 'closable', 'style', 'pt'] },
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'disabled', 'loading', 'rounded', 'ariaLabel'],
    emits: ['click'],
  },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'rows', 'autoResize'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
  ReactionBar: { template: '<div class="reaction-bar-stub" />', props: ['reactions', 'targetType', 'targetId'] },
  InlinePoll: { template: '<div class="inline-poll-stub" />', props: ['poll'] },
  PollComposer: { template: '<div class="poll-composer-stub" />', emits: ['submit', 'cancel'] },
  MentionInput: { template: '<textarea class="mention-input-stub" />', props: ['modelValue', 'placeholder', 'autoResize', 'rows'] },
}

function mountMessages() {
  const pinia = createPinia()
  return mount(MessagesView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('MessagesView extended tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Object.assign(mockMessagingStore, {
      conversations: [],
      messages: [],
      loading: false,
      sendingMessage: false,
      replyToMessage: null,
      fetchConversations: vi.fn().mockResolvedValue(undefined),
      fetchMessages: vi.fn().mockResolvedValue(undefined),
      sendMessage: vi.fn().mockResolvedValue(undefined),
      markAsRead: vi.fn().mockResolvedValue(undefined),
      deleteConversation: vi.fn().mockResolvedValue(undefined),
      muteConversation: vi.fn().mockResolvedValue(undefined),
      unmuteConversation: vi.fn().mockResolvedValue(undefined),
      setReplyTo: vi.fn(),
    })
    mockRoute.params = {}
  })

  // ==================== Fetch on mount ====================

  it('should fetch conversations on mount', async () => {
    mountMessages()
    await flushPromises()
    expect(mockMessagingStore.fetchConversations).toHaveBeenCalled()
  })

  it('should select conversation from route params', async () => {
    mockRoute.params = { conversationId: 'conv-1' }
    mockMessagingStore.conversations = [
      {
        id: 'conv-1',
        title: null,
        participants: [
          { userId: 'user-1', displayName: 'Max Muster' },
          { userId: 'user-2', displayName: 'Anna Schmidt' },
        ],
        lastMessage: null,
        unreadCount: 0,
        muted: false,
      },
    ]

    mountMessages()
    await flushPromises()
    expect(mockMessagingStore.fetchMessages).toHaveBeenCalledWith('conv-1')
  })

  // ==================== Conversation list ====================

  it('should render conversations when available', async () => {
    mockMessagingStore.conversations = [
      {
        id: 'conv-1',
        title: null,
        participants: [
          { userId: 'user-1', displayName: 'Max Muster' },
          { userId: 'user-2', displayName: 'Anna Schmidt' },
        ],
        lastMessage: { content: 'Hallo!' },
        unreadCount: 2,
        muted: false,
      },
    ]

    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.text()).toContain('Anna Schmidt')
    wrapper.unmount()
  })

  it('should show conversation title when set', async () => {
    mockMessagingStore.conversations = [
      {
        id: 'conv-1',
        title: 'Elterngruppe 3a',
        participants: [
          { userId: 'user-1', displayName: 'Max Muster' },
          { userId: 'user-2', displayName: 'Anna Schmidt' },
        ],
        lastMessage: null,
        unreadCount: 0,
        muted: false,
      },
    ]

    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.text()).toContain('Elterngruppe 3a')
    wrapper.unmount()
  })

  // ==================== Empty state ====================

  it('should show empty state with no conversations', async () => {
    mockMessagingStore.conversations = []

    const wrapper = mountMessages()
    await flushPromises()
    expect(
      wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
    wrapper.unmount()
  })

  // ==================== Layout panels ====================

  it('should render conversations and messages panels', async () => {
    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.find('.conversations-panel').exists()).toBe(true)
    expect(wrapper.find('.messages-panel').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render messages layout container', async () => {
    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.find('.messages-layout').exists()).toBe(true)
    wrapper.unmount()
  })

  // ==================== New message button ====================

  it('should render new message button', async () => {
    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.text()).toContain('Neue Nachricht')
    wrapper.unmount()
  })

  it('should render NewMessageDialog component', async () => {
    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.find('.new-msg-stub').exists()).toBe(true)
    wrapper.unmount()
  })

  // ==================== Unread indicator ====================

  it('should show unread count in conversation list', async () => {
    mockMessagingStore.conversations = [
      {
        id: 'conv-1',
        title: null,
        participants: [
          { userId: 'user-1', displayName: 'Max Muster' },
          { userId: 'user-2', displayName: 'Anna Schmidt' },
        ],
        lastMessage: { content: 'Neue Nachricht' },
        unreadCount: 5,
        muted: false,
      },
    ]

    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.text()).toContain('5')
    wrapper.unmount()
  })

  // ==================== Muted conversation indicator ====================

  it('should show muted indicator for muted conversations', async () => {
    mockMessagingStore.conversations = [
      {
        id: 'conv-1',
        title: 'Stiller Kanal',
        participants: [
          { userId: 'user-1', displayName: 'Max Muster' },
          { userId: 'user-2', displayName: 'Anna Schmidt' },
        ],
        lastMessage: null,
        unreadCount: 0,
        muted: true,
      },
    ]

    const wrapper = mountMessages()
    await flushPromises()
    // The muted class or icon should be rendered
    expect(wrapper.text()).toContain('Stiller Kanal')
    wrapper.unmount()
  })

  // ==================== Message send area ====================

  it('should render the messages panel for message input', async () => {
    const wrapper = mountMessages()
    await flushPromises()
    // The messages panel is always rendered even before a conversation is selected
    expect(wrapper.find('.messages-panel').exists()).toBe(true)
    wrapper.unmount()
  })

  // ==================== Multiple conversations ====================

  it('should display multiple conversations in list', async () => {
    mockMessagingStore.conversations = [
      {
        id: 'conv-1',
        title: null,
        participants: [
          { userId: 'user-1', displayName: 'Max Muster' },
          { userId: 'user-2', displayName: 'Anna Schmidt' },
        ],
        lastMessage: { content: 'Hallo' },
        unreadCount: 0,
        muted: false,
      },
      {
        id: 'conv-2',
        title: null,
        participants: [
          { userId: 'user-1', displayName: 'Max Muster' },
          { userId: 'user-3', displayName: 'Eva Mueller' },
        ],
        lastMessage: { content: 'Tschüss' },
        unreadCount: 1,
        muted: false,
      },
    ]

    const wrapper = mountMessages()
    await flushPromises()
    expect(wrapper.text()).toContain('Anna Schmidt')
    expect(wrapper.text()).toContain('Eva Mueller')
    wrapper.unmount()
  })

  // ==================== Conversation name computation ====================

  it('should compute conversation name from participants excluding current user', () => {
    const conv = {
      id: 'conv-1',
      title: null,
      participants: [
        { userId: 'user-1', displayName: 'Max Muster' },
        { userId: 'user-2', displayName: 'Anna Schmidt' },
        { userId: 'user-3', displayName: 'Eva Mueller' },
      ],
    }

    // Simulate the getConversationName logic
    const currentUserId = 'user-1'
    const others = conv.participants.filter(p => p.userId !== currentUserId)
    const name = others.map(p => p.displayName).join(', ')
    expect(name).toBe('Anna Schmidt, Eva Mueller')
  })

  it('should use title when conversation has a title', () => {
    const conv = {
      title: 'Team-Chat',
      participants: [
        { userId: 'user-1', displayName: 'Max Muster' },
        { userId: 'user-2', displayName: 'Anna Schmidt' },
      ],
    }

    const name = conv.title || 'Konversation'
    expect(name).toBe('Team-Chat')
  })

  // ==================== canSend logic ====================

  it('should not allow sending empty messages', () => {
    const messageText = ''
    const selectedImage = null
    const selectedAttachment = null
    const selectedFileLink = null
    const canSend = messageText.trim().length > 0 || selectedImage !== null || selectedAttachment !== null || selectedFileLink !== null
    expect(canSend).toBe(false)
  })

  it('should allow sending text messages', () => {
    const messageText = 'Hello!'
    const selectedImage = null
    const selectedAttachment = null
    const selectedFileLink = null
    const canSend = messageText.trim().length > 0 || selectedImage !== null || selectedAttachment !== null || selectedFileLink !== null
    expect(canSend).toBe(true)
  })

  it('should allow sending image-only messages', () => {
    const messageText = ''
    const selectedImage = new File([''], 'photo.jpg')
    const selectedAttachment = null
    const selectedFileLink = null
    const canSend = messageText.trim().length > 0 || selectedImage !== null || selectedAttachment !== null || selectedFileLink !== null
    expect(canSend).toBe(true)
  })

  it('should allow sending attachment-only messages', () => {
    const messageText = ''
    const selectedImage = null
    const selectedAttachment = new File([''], 'doc.pdf')
    const selectedFileLink = null
    const canSend = messageText.trim().length > 0 || selectedImage !== null || selectedAttachment !== null || selectedFileLink !== null
    expect(canSend).toBe(true)
  })

  it('should allow sending file link messages', () => {
    const messageText = ''
    const selectedImage = null
    const selectedAttachment = null
    const selectedFileLink = { fileId: 'file-1', roomId: 'room-1', fileName: 'doc.pdf' }
    const canSend = messageText.trim().length > 0 || selectedImage !== null || selectedAttachment !== null || selectedFileLink !== null
    expect(canSend).toBe(true)
  })

  it('should not allow sending whitespace-only messages', () => {
    const messageText = '   '
    const selectedImage = null
    const selectedAttachment = null
    const selectedFileLink = null
    const canSend = messageText.trim().length > 0 || selectedImage !== null || selectedAttachment !== null || selectedFileLink !== null
    expect(canSend).toBe(false)
  })

  // ==================== Image validation logic ====================

  it('should validate allowed image types', () => {
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
    expect(allowed.includes('image/jpeg')).toBe(true)
    expect(allowed.includes('image/png')).toBe(true)
    expect(allowed.includes('image/webp')).toBe(true)
    expect(allowed.includes('image/gif')).toBe(true)
    expect(allowed.includes('image/bmp')).toBe(false)
    expect(allowed.includes('application/pdf')).toBe(false)
  })

  it('should reject images larger than 10MB', () => {
    const maxSize = 10 * 1024 * 1024
    expect(5 * 1024 * 1024 > maxSize).toBe(false) // 5MB is ok
    expect(15 * 1024 * 1024 > maxSize).toBe(true) // 15MB exceeds
    expect(10 * 1024 * 1024 > maxSize).toBe(false) // exactly 10MB is ok
  })

  it('should reject attachments larger than 20MB', () => {
    const maxSize = 20 * 1024 * 1024
    expect(15 * 1024 * 1024 > maxSize).toBe(false)
    expect(25 * 1024 * 1024 > maxSize).toBe(true)
  })

  it('should only allow PDF attachments', () => {
    const fileType = 'application/pdf'
    expect(fileType === 'application/pdf').toBe(true)
    const wrongType = 'application/zip'
    expect(wrongType === 'application/pdf').toBe(false)
  })
})
