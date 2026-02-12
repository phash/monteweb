import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomDiscussions from '@/components/rooms/RoomDiscussions.vue'

vi.mock('@/api/discussions.api', () => ({
  discussionsApi: {
    getThreads: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    createThread: vi.fn(),
    getReplies: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
  },
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: { getMine: vi.fn().mockResolvedValue({ data: { data: [] } }), discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }) },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      discussions: {
        title: 'Diskussionen',
        create: 'Erstellen',
        createTitle: 'Diskussion erstellen',
        noThreads: 'Keine Diskussionen.',
        threadTitle: 'Titel',
        threadContent: 'Inhalt',
        titleLabel: 'Titel',
        titlePlaceholder: 'Thema der Diskussion...',
        contentLabel: 'Beschreibung (optional)',
        contentPlaceholder: 'Worum geht es in dieser Diskussion?',
        audienceAlle: 'Alle',
        audienceEltern: 'Eltern',
        audienceKinder: 'Kinder',
        audience: 'Zielgruppe',
        active: 'Aktiv',
        archived: 'Archiviert',
        replies: 'Antworten',
      },
      common: { loading: 'Laden...', save: 'Speichern', cancel: 'Abbrechen', back: 'Zurück' },
    },
  },
})

const stubs = {
  DiscussionThreadView: { template: '<div class="thread-view-stub" />', props: ['threadId', 'roomId'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: { template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>', props: ['label', 'icon', 'text', 'severity', 'size'], emits: ['click'] },
  Dialog: { template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal'] },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'rows'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue'] },
}

function mountDiscussions() {
  const pinia = createPinia()
  return mount(RoomDiscussions, {
    props: { roomId: 'room-1' },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('RoomDiscussions', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountDiscussions()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show empty state or loading', async () => {
    const wrapper = mountDiscussions()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })
})
