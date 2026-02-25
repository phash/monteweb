import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import MessagesView from '@/views/MessagesView.vue'

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ params: {} })),
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

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

const i18n = createI18n({
  legacy: false,
  locale: 'de',
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
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'disabled'],
    emits: ['click'],
  },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'rows', 'autoResize'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'] },
}

function mountMessages() {
  const pinia = createPinia()
  return mount(MessagesView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('MessagesView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountMessages()
    expect(wrapper.find('.page-title-stub').exists()).toBe(true)
  })

  it('should render new message button', () => {
    const wrapper = mountMessages()
    expect(wrapper.text()).toContain('Neue Nachricht')
  })

  it('should show empty state when no conversations', async () => {
    const wrapper = mountMessages()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should render new message dialog component', () => {
    const wrapper = mountMessages()
    expect(wrapper.find('.new-msg-stub').exists()).toBe(true)
  })

  it('should render messages layout with panels', () => {
    const wrapper = mountMessages()
    expect(wrapper.find('.messages-layout').exists()).toBe(true)
    expect(wrapper.find('.conversations-panel').exists()).toBe(true)
    expect(wrapper.find('.messages-panel').exists()).toBe(true)
  })
})
