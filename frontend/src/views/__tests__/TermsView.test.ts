import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock privacy API
vi.mock('@/api/privacy.api', () => ({
  privacyApi: {
    getTerms: vi.fn().mockResolvedValue({
      data: { data: { text: '<p>Terms content here</p>', version: '1.0' } },
    }),
    getTermsStatus: vi.fn().mockResolvedValue({
      data: { data: { accepted: false } },
    }),
    acceptTerms: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

// --- mock auth store
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => ({
    isAuthenticated: true,
    user: { id: 'user-1' },
  })),
}))

// --- mock termsCache
vi.mock('@/utils/termsCache', () => ({
  markTermsAccepted: vi.fn(),
}))

// --- mock sanitize
vi.mock('@/utils/sanitize', () => ({
  sanitizeHtml: vi.fn((html: string) => html),
}))

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), replace: vi.fn() })),
  useRoute: vi.fn(() => ({ params: {}, query: {} })),
}))

vi.mock('primevue/usetoast', () => ({
  useToast: vi.fn(() => ({ add: vi.fn() })),
}))

import TermsView from '@/views/TermsView.vue'
import { privacyApi } from '@/api/privacy.api'
import { markTermsAccepted } from '@/utils/termsCache'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      privacy: {
        termsOfService: 'Nutzungsbedingungen',
        noTermsConfigured: 'Keine Nutzungsbedingungen konfiguriert',
        acceptTerms: 'Akzeptieren',
        termsAccepted: 'Akzeptiert',
      },
      error: {
        unexpected: 'Unerwarteter Fehler',
      },
    },
  },
})

const globalStubs = {
  Button: {
    template: '<button @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon'],
    emits: ['click'],
  },
}

function mountView() {
  return mount(TermsView, {
    global: { plugins: [i18n], stubs: globalStubs },
  })
}

describe('TermsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // Reset auth store mock to authenticated
    vi.mocked(useAuthStore).mockReturnValue({
      isAuthenticated: true,
      user: { id: 'user-1' },
    } as any)
    vi.mocked(privacyApi.getTerms).mockResolvedValue({
      data: { data: { text: '<p>Terms content here</p>', version: '1.0' } },
    } as any)
    vi.mocked(privacyApi.getTermsStatus).mockResolvedValue({
      data: { data: { accepted: false } },
    } as any)
    vi.mocked(privacyApi.acceptTerms).mockResolvedValue({ data: { data: {} } } as any)
  })

  it('should mount without crashing', () => {
    const wrapper = mountView()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render the title', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.terms-title').text()).toBe('Nutzungsbedingungen')
    wrapper.unmount()
  })

  it('should show loading spinner initially', () => {
    const wrapper = mountView()
    expect(wrapper.find('.pi-spinner').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should fetch terms and status on mount', async () => {
    mountView()
    await flushPromises()
    expect(privacyApi.getTerms).toHaveBeenCalled()
    expect(privacyApi.getTermsStatus).toHaveBeenCalled()
  })

  it('should render terms content after loading', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.terms-content').exists()).toBe(true)
    expect(wrapper.html()).toContain('Terms content here')
    wrapper.unmount()
  })

  it('should show version when available', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.terms-version').text()).toContain('1.0')
    wrapper.unmount()
  })

  it('should show accept button when authenticated and not yet accepted', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.terms-actions').exists()).toBe(true)
    expect(wrapper.text()).toContain('Akzeptieren')
    wrapper.unmount()
  })

  it('should not show accept button when already accepted', async () => {
    vi.mocked(privacyApi.getTermsStatus).mockResolvedValue({
      data: { data: { accepted: true } },
    } as any)
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.terms-actions').exists()).toBe(false)
    wrapper.unmount()
  })

  it('should show accepted message when terms are accepted', async () => {
    vi.mocked(privacyApi.getTermsStatus).mockResolvedValue({
      data: { data: { accepted: true } },
    } as any)
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.terms-accepted').exists()).toBe(true)
    expect(wrapper.text()).toContain('Akzeptiert')
    wrapper.unmount()
  })

  it('should show no-terms message when text is null', async () => {
    vi.mocked(privacyApi.getTerms).mockResolvedValue({
      data: { data: { text: null, version: null } },
    } as any)
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Nutzungsbedingungen konfiguriert')
    wrapper.unmount()
  })

  it('should call acceptTerms and mark cache on accept', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleAccept()
    await flushPromises()
    expect(privacyApi.acceptTerms).toHaveBeenCalled()
    expect(markTermsAccepted).toHaveBeenCalled()
  })

  it('should show error toast when accept fails', async () => {
    vi.mocked(privacyApi.acceptTerms).mockRejectedValue(new Error('fail'))
    const mockAdd = vi.fn()
    vi.mocked(useToast).mockReturnValue({ add: mockAdd } as any)
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleAccept()
    await flushPromises()
    expect(mockAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  it('should not fetch terms status when not authenticated', async () => {
    vi.mocked(useAuthStore).mockReturnValue({
      isAuthenticated: false,
      user: null,
    } as any)
    vi.clearAllMocks()
    mountView()
    await flushPromises()
    expect(privacyApi.getTerms).toHaveBeenCalled()
    expect(privacyApi.getTermsStatus).not.toHaveBeenCalled()
  })
})
