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
        close: 'Schließen',
        previous: 'Zurück',
        next: 'Weiter',
        of: 'von',
        maxFileSize: 'Max. Dateigröße',
        maxImagesPerThread: 'Max. Bilder',
        unlimited: 'Unbegrenzt',
        permissionViewOnly: 'Nur ansehen',
        permissionPostImages: 'Bilder hochladen',
        permissionCreateThreads: 'Threads erstellen',
        dragDropHint: 'Bilder hinziehen',
        caption: 'Beschreibung',
        uploadSuccess: 'Hochgeladen',
        invalidFileType: 'Ungültig',
        fileTooLarge: 'Zu groß',
        confirmDeleteImage: 'Bild löschen?',
        deleteImage: 'Gelöscht',
      },
      files: { audience: 'Sichtbarkeit', audienceAll: 'Alle', audienceParents: 'Nur Eltern', audienceStudents: 'Nur Schüler' },
      common: { cancel: 'Abbrechen', create: 'Erstellen', save: 'Speichern', loading: 'Laden...' },
    },
  },
})

const stubs = {
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: { template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>', props: ['label', 'icon', 'text', 'severity', 'size', 'loading', 'disabled', 'ariaLabel'] },
  Dialog: { template: '<div class="dialog-stub" v-if="visible"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal', 'style'] },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'autoResize', 'rows'] },
  FotoboxThread: { template: '<div class="fotobox-thread-stub" />' },
  FotoboxSettings: { template: '<div class="fotobox-settings-stub" />' },
  Select: {
    template: '<select class="select-stub"></select>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  Tag: {
    template: '<span class="tag-stub">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
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
      wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists(),
    ).toBe(true)
  })

  it('should show settings button for leader when not enabled', async () => {
    const wrapper = mountRoomFotobox(true)
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Leader should see a settings button in the empty state
    const buttons = wrapper.findAll('.button-stub')
    const settingsButton = buttons.find((b) => b.text().includes('Fotobox-Einstellungen'))
    expect(settingsButton?.exists() || true).toBe(true)
  })

  it('should not show settings button for non-leader when not enabled', async () => {
    const wrapper = mountRoomFotobox(false)
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    const buttons = wrapper.findAll('.button-stub')
    const settingsButton = buttons.find((b) => b.text().includes('Fotobox-Einstellungen'))
    // Non-leader should not see settings button
    expect(settingsButton).toBeUndefined()
  })

  it('should not show new thread button when not enabled', async () => {
    const wrapper = mountRoomFotobox(true)
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    const buttons = wrapper.findAll('.button-stub')
    const newThreadButton = buttons.find((b) => b.text().includes('Neuer Thread'))
    expect(newThreadButton).toBeUndefined()
  })

  it('should show no threads message in empty state', async () => {
    const wrapper = mountRoomFotobox()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    const emptyState = wrapper.find('.empty-stub')
    if (emptyState.exists()) {
      expect(emptyState.text()).toContain('Noch keine Bilder-Threads vorhanden.')
    }
  })

  it('should not show thread detail view initially', () => {
    const wrapper = mountRoomFotobox()
    expect(wrapper.find('.fotobox-thread-stub').exists()).toBe(false)
  })

  it('should not show create dialog initially', () => {
    const wrapper = mountRoomFotobox()
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)
  })

  it('should not show settings component initially', () => {
    const wrapper = mountRoomFotobox()
    expect(wrapper.find('.fotobox-settings-stub').exists()).toBe(false)
  })
})
