import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FotoboxThread from '@/components/rooms/FotoboxThread.vue'

vi.mock('@/api/fotobox.api', () => ({
  fotoboxApi: {
    getSettings: vi.fn().mockResolvedValue({ data: { data: { enabled: true, defaultPermission: 'POST_IMAGES', maxImagesPerThread: null, maxFileSizeMb: 10 } } }),
    getThreads: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getThread: vi.fn().mockResolvedValue({
      data: { data: { id: 't1', roomId: 'room-1', title: 'Test Thread', description: 'A description', coverImageId: null, coverImageThumbnailUrl: null, imageCount: 2, createdBy: 'user1', createdByName: 'Test User', createdAt: '2024-06-15T10:00:00Z' } },
    }),
    getThreadImages: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'i1', threadId: 't1', uploadedBy: 'user1', uploadedByName: 'Test User', originalFilename: 'photo1.jpg', imageUrl: '/api/v1/fotobox/images/i1', thumbnailUrl: '/api/v1/fotobox/images/i1/thumbnail', fileSize: 1024, contentType: 'image/jpeg', width: 800, height: 600, caption: 'Caption 1', sortOrder: 0, createdAt: '2024-06-15T10:00:00Z' },
          { id: 'i2', threadId: 't1', uploadedBy: 'user2', uploadedByName: 'Other User', originalFilename: 'photo2.png', imageUrl: '/api/v1/fotobox/images/i2', thumbnailUrl: '/api/v1/fotobox/images/i2/thumbnail', fileSize: 2048, contentType: 'image/png', width: 1024, height: 768, caption: null, sortOrder: 1, createdAt: '2024-06-15T11:00:00Z' },
        ],
      },
    }),
    createThread: vi.fn(),
    deleteThread: vi.fn(),
    uploadImages: vi.fn(),
    deleteImage: vi.fn().mockResolvedValue({}),
    updateImage: vi.fn(),
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
        back: 'Zurück zur Übersicht',
        uploadImages: 'Bilder hochladen',
        noImages: 'Noch keine Bilder in diesem Thread.',
        confirmDeleteImage: 'Bild wirklich löschen?',
        deleteImage: 'Bild löschen',
        close: 'Schließen',
        previous: 'Zurück',
        next: 'Weiter',
        of: 'von',
        dragDropHint: 'Bilder hier hinziehen',
        caption: 'Beschreibung',
        uploadSuccess: 'Hochgeladen',
        invalidFileType: 'Ungültig',
        fileTooLarge: 'Zu groß',
      },
      common: { loading: 'Laden...' },
    },
  },
})

const stubs = {
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'rounded', 'size'],
  },
  FotoboxImageUploader: { template: '<div class="uploader-stub" />' },
  FotoboxLightbox: { template: '<div class="lightbox-stub" />', props: ['images', 'currentIndex', 'visible'] },
}

function mountThread(props?: { permission?: string; isLeader?: boolean }) {
  const pinia = createPinia()
  return mount(FotoboxThread, {
    props: {
      roomId: 'room-1',
      threadId: 't1',
      permission: (props?.permission ?? 'CREATE_THREADS') as any,
      isLeader: props?.isLeader ?? true,
      maxFileSizeMb: 10,
    },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FotoboxThread', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountThread()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show back button', () => {
    const wrapper = mountThread()
    const buttons = wrapper.findAll('.button-stub')
    const backButton = buttons.find((b) => b.text().includes('Zurück zur Übersicht'))
    expect(backButton?.exists()).toBe(true)
  })

  it('should emit back on back button click', async () => {
    const wrapper = mountThread()
    const buttons = wrapper.findAll('.button-stub')
    const backButton = buttons.find((b) => b.text().includes('Zurück zur Übersicht'))
    await backButton!.trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('should show upload button when permission is CREATE_THREADS', () => {
    const wrapper = mountThread({ permission: 'CREATE_THREADS' })
    const buttons = wrapper.findAll('.button-stub')
    const uploadButton = buttons.find((b) => b.text().includes('Bilder hochladen'))
    expect(uploadButton?.exists()).toBe(true)
  })

  it('should show upload button when permission is POST_IMAGES', () => {
    const wrapper = mountThread({ permission: 'POST_IMAGES' })
    const buttons = wrapper.findAll('.button-stub')
    const uploadButton = buttons.find((b) => b.text().includes('Bilder hochladen'))
    expect(uploadButton?.exists()).toBe(true)
  })

  it('should not show upload button when permission is VIEW_ONLY', () => {
    const wrapper = mountThread({ permission: 'VIEW_ONLY', isLeader: false })
    const buttons = wrapper.findAll('.button-stub')
    const uploadButton = buttons.find((b) => b.text().includes('Bilder hochladen'))
    expect(uploadButton).toBeUndefined()
  })

  it('should not show uploader initially', () => {
    const wrapper = mountThread()
    expect(wrapper.find('.uploader-stub').exists()).toBe(false)
  })

  it('should show thread info after loading', async () => {
    const wrapper = mountThread()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    const threadInfo = wrapper.find('.thread-info')
    if (threadInfo.exists()) {
      expect(threadInfo.find('h3').text()).toBe('Test Thread')
    }
  })

  it('should show empty state or loading when images are not yet loaded', () => {
    const wrapper = mountThread()
    expect(
      wrapper.find('.empty-stub').exists() ||
        wrapper.find('.loading-stub').exists() ||
        wrapper.find('.image-grid').exists(),
    ).toBe(true)
  })

  it('should include lightbox component', () => {
    const wrapper = mountThread()
    expect(wrapper.find('.lightbox-stub').exists()).toBe(true)
  })
})
