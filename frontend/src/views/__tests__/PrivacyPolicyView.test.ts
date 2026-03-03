import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock privacy API (inline to avoid hoisting issues)
vi.mock('@/api/privacy.api', () => ({
  privacyApi: {
    getPrivacyPolicy: vi.fn().mockResolvedValue({
      data: { data: { text: '<p>Privacy policy text</p>', version: '2.0' } },
    }),
  },
}))

// --- mock sanitize
vi.mock('@/utils/sanitize', () => ({
  sanitizeHtml: vi.fn((html: string) => html),
}))

import PrivacyPolicyView from '@/views/PrivacyPolicyView.vue'
import { privacyApi } from '@/api/privacy.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      privacy: {
        privacyPolicy: 'Datenschutz',
        noPrivacyPolicy: 'Keine Datenschutzerklärung konfiguriert',
      },
    },
  },
})

function mountView() {
  return mount(PrivacyPolicyView, {
    global: { plugins: [i18n] },
  })
}

describe('PrivacyPolicyView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(privacyApi.getPrivacyPolicy).mockResolvedValue({
      data: { data: { text: '<p>Privacy policy text</p>', version: '2.0' } },
    } as any)
  })

  it('should mount without crashing', () => {
    const wrapper = mountView()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render the title', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.privacy-title').text()).toBe('Datenschutz')
    wrapper.unmount()
  })

  it('should show loading spinner initially', () => {
    const wrapper = mountView()
    expect(wrapper.find('.pi-spinner').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should fetch privacy policy on mount', async () => {
    mountView()
    await flushPromises()
    expect(privacyApi.getPrivacyPolicy).toHaveBeenCalled()
  })

  it('should render privacy content after loading', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.privacy-content').exists()).toBe(true)
    expect(wrapper.html()).toContain('Privacy policy text')
    wrapper.unmount()
  })

  it('should show version when available', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.privacy-version').text()).toContain('2.0')
    wrapper.unmount()
  })

  it('should show no-policy message when text is null', async () => {
    vi.mocked(privacyApi.getPrivacyPolicy).mockResolvedValue({
      data: { data: { text: null, version: null } },
    } as any)
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Datenschutzerklärung konfiguriert')
    wrapper.unmount()
  })

  it('should not show version when version is null', async () => {
    vi.mocked(privacyApi.getPrivacyPolicy).mockResolvedValue({
      data: { data: { text: '<p>Some text</p>', version: null } },
    } as any)
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.privacy-version').exists()).toBe(false)
    wrapper.unmount()
  })

  it('should hide spinner after loading completes', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.pi-spinner').exists()).toBe(false)
    wrapper.unmount()
  })

  it('should not show content or version when both are null', async () => {
    vi.mocked(privacyApi.getPrivacyPolicy).mockResolvedValue({
      data: { data: { text: null, version: null } },
    } as any)
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.privacy-content').exists()).toBe(false)
    expect(wrapper.find('.privacy-version').exists()).toBe(false)
    wrapper.unmount()
  })
})
