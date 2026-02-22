import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { axe } from 'vitest-axe'
import { createI18n } from 'vue-i18n'
import de from '@/i18n/de'
import PrimeVue from 'primevue/config'

// Reusable i18n + PrimeVue config for mounting
function createTestPlugins() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const i18n = createI18n({
    legacy: false,
    locale: 'de',
    messages: { de },
  })
  return {
    global: {
      plugins: [pinia, i18n, PrimeVue],
      stubs: {
        'router-link': { template: '<a><slot /></a>' },
        'router-view': { template: '<div />' },
        teleport: { template: '<div><slot /></div>' },
      },
    },
  }
}

// Mock modules
vi.mock('@/api/auth.api', () => ({ authApi: { login: vi.fn(), register: vi.fn() } }))
vi.mock('@/api/admin.api', () => ({ adminApi: { getConfig: vi.fn(), getModules: vi.fn() } }))
vi.mock('@/api/rooms.api', () => ({ roomsApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: { getMe: vi.fn() } }))
vi.mock('@/api/feed.api', () => ({ feedApi: {} }))
vi.mock('@/api/messaging.api', () => ({ messagingApi: {} }))
vi.mock('@/api/family.api', () => ({ familyApi: {} }))
vi.mock('@/api/calendar.api', () => ({ calendarApi: {} }))
vi.mock('@/api/jobboard.api', () => ({ jobboardApi: {} }))
vi.mock('@/api/cleaning.api', () => ({ cleaningApi: {} }))
vi.mock('@/api/notification.api', () => ({ notificationApi: {} }))
vi.mock('@/api/forms.api', () => ({ formsApi: {} }))
vi.mock('@/api/fotobox.api', () => ({ fotoboxApi: {} }))
vi.mock('@/api/privacy.api', () => ({ privacyApi: {} }))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), currentRoute: { value: { name: 'home' } } }),
  useRoute: () => ({ params: {}, query: {}, name: 'home' }),
}))

// Mock matchMedia for PrimeVue components
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

describe('Accessibility (axe-core)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('LoginView has no critical accessibility violations', async () => {
    const { default: LoginView } = await import('@/views/LoginView.vue')
    const wrapper = mount(LoginView, createTestPlugins())
    const results = await axe(wrapper.element, {
      rules: {
        // PrimeVue Password component upstream bug: adds aria-expanded on input[type=password]
        'aria-allowed-attr': { enabled: false },
      },
    })
    // Filter to critical/serious only
    const serious = results.violations.filter(
      (v: any) => v.impact === 'critical' || v.impact === 'serious',
    )
    expect(serious).toEqual([])
    wrapper.unmount()
  })

  it('EmptyState component has no accessibility violations', async () => {
    const { default: EmptyState } = await import('@/components/common/EmptyState.vue')
    const wrapper = mount(EmptyState, {
      ...createTestPlugins(),
      props: {
        icon: 'pi pi-inbox',
        title: 'No data',
        message: 'Nothing here yet',
      },
    })
    const results = await axe(wrapper.element)
    const serious = results.violations.filter(
      (v: any) => v.impact === 'critical' || v.impact === 'serious',
    )
    expect(serious).toEqual([])
    wrapper.unmount()
  })
})
