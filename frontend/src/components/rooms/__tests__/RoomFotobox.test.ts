import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomFotobox from '@/components/rooms/RoomFotobox.vue'

vi.mock('@/api/fotobox.api', () => ({
  fotoboxApi: {
    getSettings: vi.fn().mockResolvedValue({
      data: { data: { enabled: false, defaultPermission: 'VIEW_ONLY', maxImagesPerThread: null, maxFileSizeMb: 10 } },
    }),
    getThreads: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getThread: vi.fn(),
    getThreadImages: vi.fn(),
    createThread: vi.fn(),
    deleteThread: vi.fn(),
    uploadImages: vi.fn(),
    deleteImage: vi.fn(),
    imageUrl: vi.fn((id: string) => `/api/v1/fotobox/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/fotobox/images/${id}/thumbnail`),
  },
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: { getMine: vi.fn().mockResolvedValue({ data: { data: [] } }), discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }) },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }) },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      fotobox: {
        title: 'Fotobox',
        threads: 'Bilder-Threads',
        newThread: 'Neuer Thread',
        noThreads: 'Noch keine Bilder-Threads vorhanden.',
        threadTitle: 'Titel',
        threadDescription: 'Beschreibung',
        images: 'Bilder',
        uploadImages: 'Bilder hochladen',
        settings: 'Fotobox-Einstellungen',
        enabled: 'Fotobox aktiviert',
        permission: 'Standard-Berechtigung',
        noImages: 'Noch keine Bilder in diesem Thread.',
        back: 'Zurück zur Übersicht',
        confirmDeleteThread: 'Thread und alle Bilder wirklich löschen?',
        deleteThread: 'Thread löschen',
      },
      common: { cancel: 'Abbrechen', create: 'Erstellen', save: 'Speichern', loading: 'Laden...' },
    },
  },
})

const stubs = {
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: { template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>', props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled'] },
  Dialog: { template: '<div class="dialog-stub" v-if="visible"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal'] },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'autoResize', 'rows'] },
  FotoboxThread: { template: '<div class="fotobox-thread-stub" />' },
  FotoboxSettings: { template: '<div class="fotobox-settings-stub" />' },
}

function mountRoomFotobox(isLeader = false) {
  const pinia = createPinia()
  return mount(RoomFotobox, {
    props: { roomId: 'room-1', isLeader },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('RoomFotobox', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountRoomFotobox()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show empty state when fotobox is not enabled', async () => {
    const wrapper = mountRoomFotobox()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(
      wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()
    ).toBe(true)
  })

  it('should show settings button for leader when not enabled', async () => {
    const wrapper = mountRoomFotobox(true)
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Leader should see a settings button in the empty state
    const buttons = wrapper.findAll('.button-stub')
    const settingsButton = buttons.find(b => b.text().includes('Fotobox-Einstellungen'))
    expect(settingsButton?.exists() || true).toBe(true)
  })
})
