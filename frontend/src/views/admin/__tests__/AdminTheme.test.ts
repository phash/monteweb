import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminTheme from '@/views/admin/AdminTheme.vue'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'tenant-1',
          schoolName: 'Montessori Schule',
          logoUrl: '/logo.png',
          theme: {
            primaryColor: '#3B82F6',
            primaryHover: '#2563EB',
            bgMain: '#F9FAFB',
            bgCard: '#FFFFFF',
            bgSidebar: '#FFFFFF',
            textColor: '#111827',
            textSecondary: '#6B7280',
            borderLight: '#E5E7EB',
          },
          modules: {},
          targetHoursPerFamily: 30,
          targetCleaningHours: 3,
          bundesland: 'BY',
          schoolVacations: [],
        },
      },
    }),
    updateConfig: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'tenant-1',
          schoolName: 'Montessori Schule',
          targetHoursPerFamily: 30,
          targetCleaningHours: 3,
          bundesland: 'BY',
          schoolVacations: [],
        },
      },
    }),
    updateTheme: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'tenant-1',
          theme: { primaryColor: '#3B82F6' },
        },
      },
    }),
    updateModules: vi.fn().mockResolvedValue({ data: { data: {} } }),
    uploadLogo: vi.fn().mockResolvedValue({ data: { data: { logoUrl: '/new-logo.png' } } }),
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
        themeTitle: 'Design & Einstellungen',
        logo: 'Logo',
        hoursConfig: 'Stundenkonfiguration',
        totalHoursTarget: 'Stundenziel gesamt',
        cleaningHoursTarget: 'Putzstunden-Ziel',
        saveHoursConfig: 'Stunden speichern',
        hoursConfigSaved: 'Stundenkonfiguration gespeichert',
        colorScheme: 'Farbschema',
        preview: 'Vorschau',
        themeSaved: 'Design gespeichert',
        logoUploaded: 'Logo hochgeladen',
        theme: {
          primaryColor: 'Primärfarbe',
          primaryHover: 'Primär-Hover',
          background: 'Hintergrund',
          cardBg: 'Karten-Hintergrund',
          sidebarBg: 'Sidebar-Hintergrund',
          textColor: 'Textfarbe',
          secondaryText: 'Sekundärer Text',
          borderColor: 'Rahmenfarbe',
          uploadLogo: 'Logo hochladen',
          schoolNameFallback: 'Schule',
          previewText: 'Vorschautext',
          primaryButton: 'Primär',
          secondaryButton: 'Sekundär',
        },
      },
      common: { save: 'Speichern' },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'loading'],
    emits: ['click'],
  },
  InputText: {
    template: '<input class="input-stub" :value="modelValue" />',
    props: ['modelValue'],
  },
  InputNumber: {
    template: '<input class="input-number-stub" :value="modelValue" />',
    props: ['modelValue', 'min', 'max', 'minFractionDigits', 'maxFractionDigits'],
  },
  FileUpload: {
    template: '<div class="fileupload-stub" />',
    props: ['mode', 'accept', 'maxFileSize', 'auto', 'chooseLabel'],
    emits: ['select'],
  },
  Select: {
    template: '<select class="select-stub"><option>{{ modelValue }}</option></select>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
  },
  DatePicker: {
    template: '<input class="datepicker-stub" />',
    props: ['modelValue', 'dateFormat'],
  },
  DataTable: {
    template: '<table class="datatable-stub"><slot /></table>',
    props: ['value', 'stripedRows'],
  },
  Column: {
    template: '<td class="column-stub"><slot /></td>',
    props: ['field', 'header'],
  },
}

function mountComponent() {
  const pinia = createPinia()
  return mount(AdminTheme, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminTheme', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render title', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Design & Einstellungen')
  })

  it('should render logo section', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Logo')
  })

  it('should render hours configuration section', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Stundenkonfiguration')
    expect(wrapper.text()).toContain('Stundenziel gesamt')
    expect(wrapper.text()).toContain('Putzstunden-Ziel')
  })

  it('should render color scheme section', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Farbschema')
  })

  it('should render preview section', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Vorschau')
    expect(wrapper.text()).toContain('Vorschautext')
  })

  it('should fetch config on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(adminApi.getPublicConfig).toHaveBeenCalled()
  })

  it('should render color input fields after loading', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const inputs = wrapper.findAll('.input-stub')
    // 8 color fields
    expect(inputs.length).toBe(8)
  })

  it('should render hours config input numbers', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const numberInputs = wrapper.findAll('.input-number-stub')
    // targetHoursPerFamily + targetCleaningHours
    expect(numberInputs.length).toBe(2)
  })

  it('should render FileUpload for logo', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.fileupload-stub').exists()).toBe(true)
  })

  it('should render save theme button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const saveBtn = buttons.find(b => b.text().includes('Speichern'))
    expect(saveBtn).toBeTruthy()
  })

  it('should render save hours button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const hoursBtn = buttons.find(b => b.text().includes('Stunden speichern'))
    expect(hoursBtn).toBeTruthy()
  })

  it('should call updateTheme when save theme is clicked', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const saveBtn = buttons[buttons.length - 1]
    await saveBtn.trigger('click')
    await flushPromises()

    expect(adminApi.updateTheme).toHaveBeenCalled()
  })

  it('should call updateConfig when save hours is clicked', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const hoursBtn = buttons.find(b => b.text().includes('Stunden speichern'))
    if (hoursBtn) {
      await hoursBtn.trigger('click')
      await flushPromises()
      expect(adminApi.updateConfig).toHaveBeenCalledWith({
        targetHoursPerFamily: 30,
        targetCleaningHours: 3,
      })
    }
  })

  it('should display logo image when config has logoUrl', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/logo.png')
  })

  it('should render color preview boxes', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const divs = wrapper.findAll('div[style]')
    expect(divs.length).toBeGreaterThan(0)
  })

  it('should render preview card with theme colors', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('Primär')
    expect(wrapper.text()).toContain('Sekundär')
  })
})
