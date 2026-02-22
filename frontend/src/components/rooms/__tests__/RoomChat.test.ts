import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomChat from '../RoomChat.vue'

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getChatChannels: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getOrCreateChatChannel: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'ch-1',
          roomId: 'room-1',
          conversationId: 'conv-1',
          channelType: 'MAIN',
          lastMessage: null,
          unreadCount: 0,
        },
      },
    }),
  },
}))

vi.mock('@/api/messaging.api', () => ({
  messagingApi: {
    getMessages: vi.fn().mockResolvedValue({
      data: { data: { content: [], last: true } },
    }),
    sendMessage: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'msg-new',
          conversationId: 'conv-1',
          senderId: 'user-1',
          senderName: 'Test User',
          content: 'Hello world',
          createdAt: '2025-01-15T10:00:00Z',
          images: [],
          replyTo: null,
        },
      },
    }),
    getConversations: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getConversation: vi.fn().mockResolvedValue({ data: { data: null } }),
    getUnreadCount: vi.fn().mockResolvedValue({ data: { data: { count: 0 } } }),
    markAsRead: vi.fn().mockResolvedValue({ data: { data: null } }),
    startConversation: vi.fn().mockResolvedValue({ data: { data: null } }),
    imageUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/messages/images/${id}/thumbnail`),
  },
}))

vi.mock('@/api/auth.api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: {
    getMe: vi.fn().mockResolvedValue({
      data: { data: { id: 'user-1', displayName: 'Test User', role: 'PARENT' } },
    }),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      chat: {
        title: 'Chat',
        noMessages: 'Keine Nachrichten',
        placeholder: 'Nachricht schreiben...',
        mute: 'Stummschalten',
        unmute: 'Stummschaltung aufheben',
        muted: 'Stumm',
        today: 'Heute',
        yesterday: 'Gestern',
        image: 'Bild',
        attachImage: 'Bild anhängen',
        replyTo: 'Antworten',
        channels: {
          MAIN: 'Allgemein',
          PARENTS: 'Eltern',
          STUDENTS: 'Schueler',
        },
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')"><slot />{{ label }}</button>',
    props: ['label', 'disabled', 'icon', 'severity', 'text', 'size'],
    emits: ['click'],
  },
  Textarea: {
    template: '<textarea class="textarea-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>',
    props: ['modelValue', 'placeholder', 'autoResize', 'rows'],
    emits: ['update:modelValue'],
  },
  SelectButton: {
    template: '<div class="selectbutton-stub"></div>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
    emits: ['update:modelValue'],
  },
  Dialog: {
    template: '<div class="dialog-stub"><slot /></div>',
    props: ['visible', 'modal', 'dismissableMask', 'closable', 'style', 'pt'],
    emits: ['update:visible'],
  },
}

function mountComponent(props = {}) {
  return mount(RoomChat, {
    props: { roomId: 'room-1', ...props },
    global: {
      plugins: [i18n, createPinia()],
      stubs,
    },
  })
}

describe('RoomChat', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render the chat container', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.rc-container').exists()).toBe(true)
  })

  it('should show no messages text when messages array is empty', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Nachrichten')
  })

  it('should render the message input and send button', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.textarea-stub').exists()).toBe(true)
    expect(wrapper.findAll('.button-stub').length).toBeGreaterThanOrEqual(1)
  })

  it('should render with a different roomId prop', () => {
    const wrapper = mountComponent({ roomId: 'room-42' })
    expect(wrapper.exists()).toBe(true)
  })

  it('should display messages when the store has messages', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(RoomChat, {
      props: { roomId: 'room-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    const { useMessagingStore } = await import('@/stores/messaging')
    const messagingStore = useMessagingStore()
    messagingStore.messages = [
      {
        id: 'msg-1',
        conversationId: 'conv-1',
        senderId: 'user-2',
        senderName: 'Other User',
        content: 'Hello there!',
        createdAt: '2025-01-15T10:00:00Z',
        images: [],
        replyTo: null,
      },
    ]
    await flushPromises()

    expect(wrapper.text()).toContain('Hello there!')
    expect(wrapper.text()).toContain('Other User')
  })

  it('should show own messages aligned to the right', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(RoomChat, {
      props: { roomId: 'room-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()
    authStore.user = { id: 'user-1', displayName: 'Me', role: 'PARENT' } as any

    const { useMessagingStore } = await import('@/stores/messaging')
    const messagingStore = useMessagingStore()
    messagingStore.messages = [
      {
        id: 'msg-1',
        conversationId: 'conv-1',
        senderId: 'user-1',
        senderName: 'Me',
        content: 'My own message',
        createdAt: '2025-01-15T10:00:00Z',
        images: [],
        replyTo: null,
      },
    ]
    await flushPromises()

    const ownMsg = wrapper.find('.rc-message--own')
    expect(ownMsg.exists()).toBe(true)
    expect(ownMsg.text()).toContain('My own message')
  })

  it('should show sender avatar for other users', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(RoomChat, {
      props: { roomId: 'room-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()
    authStore.user = { id: 'user-1', displayName: 'Me', role: 'PARENT' } as any

    const { useMessagingStore } = await import('@/stores/messaging')
    const messagingStore = useMessagingStore()
    messagingStore.messages = [
      {
        id: 'msg-1',
        conversationId: 'conv-1',
        senderId: 'user-2',
        senderName: 'Anna Müller',
        content: 'Test message',
        createdAt: '2025-01-15T10:00:00Z',
        images: [],
        replyTo: null,
      },
    ]
    await flushPromises()

    const avatar = wrapper.find('.rc-avatar')
    expect(avatar.exists()).toBe(true)
    expect(avatar.text()).toBe('AM')
  })

  it('should show date separators', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(RoomChat, {
      props: { roomId: 'room-1' },
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    const { useMessagingStore } = await import('@/stores/messaging')
    const messagingStore = useMessagingStore()
    messagingStore.messages = [
      {
        id: 'msg-1',
        conversationId: 'conv-1',
        senderId: 'user-2',
        senderName: 'Other',
        content: 'Old message',
        createdAt: '2025-01-14T10:00:00Z',
        images: [],
        replyTo: null,
      },
      {
        id: 'msg-2',
        conversationId: 'conv-1',
        senderId: 'user-2',
        senderName: 'Other',
        content: 'New message',
        createdAt: '2025-01-15T10:00:00Z',
        images: [],
        replyTo: null,
      },
    ]
    await flushPromises()

    const separators = wrapper.findAll('.rc-date-separator')
    expect(separators.length).toBe(2)
  })

  it('should render empty state with icon', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.find('.rc-empty').exists()).toBe(true)
    expect(wrapper.find('.rc-empty-icon').exists()).toBe(true)
  })
})
