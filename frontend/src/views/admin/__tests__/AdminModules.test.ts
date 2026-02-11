import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminModules from '@/views/admin/AdminModules.vue'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'tenant-1',
          schoolName: 'Montessori Schule',
          logoUrl: null,
          theme: {},
          modules: {
            messaging: true,
            files: true,
            jobboard: true,
            cleaning: false,
            calendar: true,
          },
          targetHoursPerFamily: 30,
          targetCleaningHours: 3,
        },
      },
    }),
    updateModules: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'tenant-1',
          schoolName: 'Montessori Schule',
          modules: { messaging: true, files: true, jobboard: true, cleaning: false, calendar: true },
        },
      },
    }),
    updateTheme: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))

import { adminApi } from '@/api/admin.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      admin: {
        modules: 'Module',
        moduleSubtitle: 'Module aktivieren/deaktivieren',
        moduleSaved: 'Module gespeichert',
        moduleDescriptions: {
          messaging: 'Direktnachrichten',
          files: 'Dateiablage',
          jobboard: 'Jobbörse',
          cleaning: 'Putzorganisation',
          calendar: 'Kalender',
        },
      },
      common: { save: 'Speichern' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }} {{ subtitle }}</div>', props: ['title', 'subtitle'] },
  ToggleSwitch: {
    template: '<input type="checkbox" class="toggle-stub" :checked="modelValue" @change="$emit(\'update:modelValue\', !modelValue)" />',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label'],
    emits: ['click'],
  },
  Message: {
    template: '<div class="message-stub"><slot /></div>',
    props: ['severity', 'closable'],
  },
}

function mountComponent() {
  const pinia = createPinia()
  return mount(AdminModules, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminModules', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title', () => {
    const wrapper = mountComponent()
    const title = wrapper.find('.page-title-stub')
    expect(title.exists()).toBe(true)
    expect(title.text()).toContain('Module')
  })

  it('should render save button', () => {
    const wrapper = mountComponent()
    const btn = wrapper.find('.button-stub')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('Speichern')
  })

  it('should fetch config on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(adminApi.getPublicConfig).toHaveBeenCalled()
  })

  it('should render module toggles after config loads', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const toggles = wrapper.findAll('.toggle-stub')
    // 5 modules: messaging, files, jobboard, cleaning, calendar
    expect(toggles.length).toBe(5)
  })

  it('should render module names after config loads', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('messaging')
    expect(wrapper.text()).toContain('files')
    expect(wrapper.text()).toContain('jobboard')
    expect(wrapper.text()).toContain('cleaning')
    expect(wrapper.text()).toContain('calendar')
  })

  it('should render module descriptions', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('Direktnachrichten')
    expect(wrapper.text()).toContain('Dateiablage')
    expect(wrapper.text()).toContain('Jobbörse')
  })

  it('should call updateModules when save is clicked', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const saveBtn = wrapper.find('.button-stub')
    await saveBtn.trigger('click')
    await flushPromises()

    expect(adminApi.updateModules).toHaveBeenCalledWith({
      messaging: true,
      files: true,
      jobboard: true,
      cleaning: false,
      calendar: true,
    })
  })

  it('should show success message after save', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const saveBtn = wrapper.find('.button-stub')
    await saveBtn.trigger('click')
    await flushPromises()

    expect(wrapper.find('.message-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('Module gespeichert')
  })

  it('should not show success message initially', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.message-stub').exists()).toBe(false)
  })
})
