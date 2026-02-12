import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FotoboxSettings from '@/components/rooms/FotoboxSettings.vue'

vi.mock('@/api/fotobox.api', () => ({
  fotoboxApi: {
    getSettings: vi.fn().mockResolvedValue({
      data: { data: { enabled: true, defaultPermission: 'POST_IMAGES', maxImagesPerThread: 50, maxFileSizeMb: 15 } },
    }),
    updateSettings: vi.fn().mockResolvedValue({
      data: { data: { enabled: true, defaultPermission: 'POST_IMAGES', maxImagesPerThread: 50, maxFileSizeMb: 15 } },
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
        settings: 'Fotobox-Einstellungen',
        enabled: 'Fotobox aktiviert',
        permission: 'Standard-Berechtigung',
        maxFileSize: 'Maximale Dateigröße',
        maxImagesPerThread: 'Max. Bilder pro Thread',
        unlimited: 'Unbegrenzt',
        permissionViewOnly: 'Nur ansehen',
        permissionPostImages: 'Bilder hochladen',
        permissionCreateThreads: 'Threads erstellen',
      },
      common: { cancel: 'Abbrechen', save: 'Speichern' },
    },
  },
})

const stubs = {
  Dialog: {
    template: '<div class="dialog-stub" v-if="visible"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
    emits: ['update:visible'],
  },
  ToggleSwitch: {
    template: '<input type="checkbox" class="toggle-stub" :checked="modelValue" @change="$emit(\'update:modelValue\', !modelValue)" />',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  Select: {
    template: '<select class="select-stub"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
    emits: ['update:modelValue'],
  },
  InputNumber: {
    template: '<input type="number" class="input-number-stub" :value="modelValue" />',
    props: ['modelValue', 'min', 'max', 'suffix', 'placeholder'],
    emits: ['update:modelValue'],
  },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'severity', 'text', 'loading'],
  },
}

function mountSettings(props?: { visible?: boolean }) {
  const pinia = createPinia()
  return mount(FotoboxSettings, {
    props: {
      roomId: 'room-1',
      visible: props?.visible ?? true,
    },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FotoboxSettings', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render when visible', () => {
    const wrapper = mountSettings({ visible: true })
    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should not render when not visible', () => {
    const wrapper = mountSettings({ visible: false })
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)
  })

  it('should show settings form fields', () => {
    const wrapper = mountSettings()
    expect(wrapper.find('.toggle-stub').exists()).toBe(true)
    expect(wrapper.find('.select-stub').exists()).toBe(true)
    expect(wrapper.findAll('.input-number-stub').length).toBeGreaterThanOrEqual(1)
  })

  it('should show permission options', () => {
    const wrapper = mountSettings()
    const options = wrapper.findAll('.select-stub option')
    expect(options.length).toBe(3)
    expect(options[0].text()).toContain('Nur ansehen')
    expect(options[1].text()).toContain('Bilder hochladen')
    expect(options[2].text()).toContain('Threads erstellen')
  })

  it('should show cancel and save buttons', () => {
    const wrapper = mountSettings()
    const buttons = wrapper.findAll('.button-stub')
    const cancelBtn = buttons.find((b) => b.text().includes('Abbrechen'))
    const saveBtn = buttons.find((b) => b.text().includes('Speichern'))
    expect(cancelBtn?.exists()).toBe(true)
    expect(saveBtn?.exists()).toBe(true)
  })

  it('should emit update:visible false on cancel', async () => {
    const wrapper = mountSettings()
    const buttons = wrapper.findAll('.button-stub')
    const cancelBtn = buttons.find((b) => b.text().includes('Abbrechen'))
    await cancelBtn!.trigger('click')
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')![0]).toEqual([false])
  })

  it('should show enabled toggle label', () => {
    const wrapper = mountSettings()
    expect(wrapper.text()).toContain('Fotobox aktiviert')
  })

  it('should show permission label', () => {
    const wrapper = mountSettings()
    expect(wrapper.text()).toContain('Standard-Berechtigung')
  })

  it('should show max file size label', () => {
    const wrapper = mountSettings()
    expect(wrapper.text()).toContain('Maximale Dateigröße')
  })

  it('should show max images per thread label', () => {
    const wrapper = mountSettings()
    expect(wrapper.text()).toContain('Max. Bilder pro Thread')
  })

  it('should show unlimited hint', () => {
    const wrapper = mountSettings()
    expect(wrapper.text()).toContain('Unbegrenzt')
  })
})
