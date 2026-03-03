import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock admin API (inline, no external const refs)
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    updateConfig: vi.fn().mockResolvedValue({
      data: { data: { id: 'tenant-1' } },
    }),
    getPublicConfig: vi.fn().mockResolvedValue({
      data: { data: { id: 'tenant-1' } },
    }),
    getConfig: vi.fn().mockResolvedValue({
      data: { data: { id: 'tenant-1' } },
    }),
    updateModules: vi.fn().mockResolvedValue({ data: { data: {} } }),
    updateTheme: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))

vi.mock('primevue/usetoast', () => ({
  useToast: vi.fn(() => ({ add: vi.fn() })),
}))

import AdminPrivacyView from '@/views/admin/AdminPrivacyView.vue'
import { adminApi } from '@/api/admin.api'
import { useToast } from 'primevue/usetoast'

const defaultConfig = {
  privacyPolicyText: '<p>Datenschutz</p>',
  privacyPolicyVersion: '1.5',
  termsText: '<p>AGB</p>',
  termsVersion: '2.0',
  dataRetentionDaysNotifications: 90,
  dataRetentionDaysAudit: 365,
  modules: {},
}

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      privacy: {
        adminPrivacy: 'Datenschutz & Nutzungsbedingungen',
        privacyPolicy: 'Datenschutzerklärung',
        policyVersion: 'Version',
        policyText: 'Datenschutztext',
        policyTextHint: 'HTML erlaubt',
        termsOfService: 'Nutzungsbedingungen',
        termsVersion: 'Version',
        termsText: 'Nutzungstext',
        termsTextHint: 'HTML erlaubt',
        retention: 'Aufbewahrung',
        retentionNotificationDays: 'Benachrichtigungen (Tage)',
        retentionNotificationDaysHint: 'Tage Aufbewahrung',
        retentionAuditDays: 'Audit-Log (Tage)',
        retentionAuditDaysHint: 'Tage Aufbewahrung',
        saved: 'Gespeichert',
      },
      common: {
        save: 'Speichern',
      },
    },
  },
})

const globalStubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')" :disabled="loading">{{ label }}</button>',
    props: ['label', 'icon', 'loading'],
    emits: ['click'],
  },
  InputText: {
    template: '<input class="input-text-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  InputNumber: {
    template: '<input class="input-number-stub" type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
    props: ['modelValue', 'min', 'max'],
    emits: ['update:modelValue'],
  },
  Textarea: {
    template: '<textarea class="textarea-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'rows', 'autoResize'],
    emits: ['update:modelValue'],
  },
}

function mountView() {
  const pinia = createPinia()
  return mount(AdminPrivacyView, {
    global: { plugins: [i18n, pinia], stubs: globalStubs },
  })
}

describe('AdminPrivacyView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(adminApi.getPublicConfig).mockResolvedValue({
      data: { data: { ...defaultConfig } },
    } as any)
    vi.mocked(adminApi.updateConfig).mockResolvedValue({
      data: { data: { ...defaultConfig } },
    } as any)
  })

  it('should mount without crashing', () => {
    const wrapper = mountView()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render the page title', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('h1').text()).toContain('Datenschutz & Nutzungsbedingungen')
    wrapper.unmount()
  })

  it('should render privacy policy section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Datenschutzerklärung')
    wrapper.unmount()
  })

  it('should render terms of service section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Nutzungsbedingungen')
    wrapper.unmount()
  })

  it('should render data retention section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Aufbewahrung')
    wrapper.unmount()
  })

  it('should render save button', async () => {
    const wrapper = mountView()
    await flushPromises()
    const btn = wrapper.find('.button-stub')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('Speichern')
    wrapper.unmount()
  })

  it('should populate form fields from config on mount', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.privacyPolicyText).toBe('<p>Datenschutz</p>')
    expect(vm.privacyPolicyVersion).toBe('1.5')
    expect(vm.termsText).toBe('<p>AGB</p>')
    expect(vm.termsVersion).toBe('2.0')
    expect(vm.retentionNotificationDays).toBe(90)
    expect(vm.retentionAuditDays).toBe(365)
    wrapper.unmount()
  })

  it('should fetch config if not loaded on mount', async () => {
    mountView()
    await flushPromises()
    // Uses admin store which calls getPublicConfig
    expect(adminApi.getPublicConfig).toHaveBeenCalled()
  })

  it('should call updateConfig when save is clicked', async () => {
    const wrapper = mountView()
    await flushPromises()
    const btn = wrapper.find('.button-stub')
    await btn.trigger('click')
    await flushPromises()
    expect(adminApi.updateConfig).toHaveBeenCalledWith(
      expect.objectContaining({
        privacyPolicyText: '<p>Datenschutz</p>',
        privacyPolicyVersion: '1.5',
        termsText: '<p>AGB</p>',
        termsVersion: '2.0',
      }),
    )
    wrapper.unmount()
  })

  it('should show success toast on successful save', async () => {
    const mockAdd = vi.fn()
    vi.mocked(useToast).mockReturnValue({ add: mockAdd } as any)
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('.button-stub').trigger('click')
    await flushPromises()
    expect(mockAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('should show error toast when save fails', async () => {
    vi.mocked(adminApi.updateConfig).mockRejectedValue({ response: { data: { message: 'Validation error' } } })
    const mockAdd = vi.fn()
    vi.mocked(useToast).mockReturnValue({ add: mockAdd } as any)
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('.button-stub').trigger('click')
    await flushPromises()
    expect(mockAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  it('should render textarea fields for policy and terms text', async () => {
    const wrapper = mountView()
    await flushPromises()
    const textareas = wrapper.findAll('.textarea-stub')
    expect(textareas.length).toBe(2)
    wrapper.unmount()
  })

  it('should render input fields for versions', async () => {
    const wrapper = mountView()
    await flushPromises()
    const inputs = wrapper.findAll('.input-text-stub')
    expect(inputs.length).toBe(2)
    wrapper.unmount()
  })
})
