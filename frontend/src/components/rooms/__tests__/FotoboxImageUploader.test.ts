import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FotoboxImageUploader from '@/components/rooms/FotoboxImageUploader.vue'

vi.mock('@/api/fotobox.api', () => ({
  fotoboxApi: {
    getSettings: vi.fn().mockResolvedValue({ data: { data: { enabled: true, defaultPermission: 'POST_IMAGES', maxImagesPerThread: null, maxFileSizeMb: 10 } } }),
    getThreads: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getThread: vi.fn(),
    getThreadImages: vi.fn(),
    createThread: vi.fn(),
    deleteThread: vi.fn(),
    uploadImages: vi.fn().mockResolvedValue({ data: { data: [{ id: 'i1', threadId: 't1', originalFilename: 'photo.jpg' }] } }),
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
        dragDropHint: 'Bilder hier hinziehen oder klicken',
        uploadImages: 'Bilder hochladen',
        caption: 'Beschreibung',
        uploadSuccess: '{count} Bild(er) hochgeladen',
        invalidFileType: 'Ungültiger Dateityp',
        fileTooLarge: 'Datei zu groß (max: {max} MB)',
      },
      common: { loading: 'Laden...' },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'rounded', 'size', 'loading'],
  },
  InputText: {
    template: '<input class="input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', ($event as InputEvent).target)" />',
    props: ['modelValue', 'placeholder'],
  },
  ProgressBar: { template: '<div class="progress-stub" />', props: ['mode'] },
}

function mountUploader(props?: { maxFileSizeMb?: number }) {
  const pinia = createPinia()
  return mount(FotoboxImageUploader, {
    props: {
      roomId: 'room-1',
      threadId: 'thread-1',
      maxFileSizeMb: props?.maxFileSizeMb,
    },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FotoboxImageUploader', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render drop zone', () => {
    const wrapper = mountUploader()
    expect(wrapper.find('.drop-zone').exists()).toBe(true)
  })

  it('should show drag-drop hint text', () => {
    const wrapper = mountUploader()
    expect(wrapper.text()).toContain('Bilder hier hinziehen oder klicken')
  })

  it('should have hidden file input', () => {
    const wrapper = mountUploader()
    const input = wrapper.find('input[type="file"]')
    expect(input.exists()).toBe(true)
    expect(input.classes()).toContain('hidden-input')
  })

  it('should accept correct file types', () => {
    const wrapper = mountUploader()
    const input = wrapper.find('input[type="file"]')
    expect(input.attributes('accept')).toBe('image/jpeg,image/png,image/webp,image/gif')
  })

  it('should support multiple file selection', () => {
    const wrapper = mountUploader()
    const input = wrapper.find('input[type="file"]')
    expect(input.attributes('multiple')).toBeDefined()
  })

  it('should not show preview grid when no files selected', () => {
    const wrapper = mountUploader()
    expect(wrapper.find('.preview-grid').exists()).toBe(false)
  })

  it('should not show upload actions when no files selected', () => {
    const wrapper = mountUploader()
    expect(wrapper.find('.upload-actions').exists()).toBe(false)
  })

  it('should add dragging class on dragover', async () => {
    const wrapper = mountUploader()
    const dropZone = wrapper.find('.drop-zone')
    await dropZone.trigger('dragover')
    expect(dropZone.classes()).toContain('dragging')
  })

  it('should remove dragging class on dragleave', async () => {
    const wrapper = mountUploader()
    const dropZone = wrapper.find('.drop-zone')
    await dropZone.trigger('dragover')
    expect(dropZone.classes()).toContain('dragging')
    await dropZone.trigger('dragleave')
    expect(dropZone.classes()).not.toContain('dragging')
  })

  it('should not show progress bar when not uploading', () => {
    const wrapper = mountUploader()
    expect(wrapper.find('.progress-stub').exists()).toBe(false)
  })
})
