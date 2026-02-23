import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import { axe } from 'vitest-axe'
import de from '@/i18n/de'
import en from '@/i18n/en'
import PrimeVue from 'primevue/config'
import { handbookContent } from '@/data/helpContent'

// Mock APIs and router
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
vi.mock('@/api/fundgrube.api', () => ({ fundgrubeApi: {} }))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), currentRoute: { value: { name: 'help' } } }),
  useRoute: () => ({ params: {}, query: {}, name: 'help' }),
}))

// Mock matchMedia for PrimeVue
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

function createTestPlugins() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const i18n = createI18n({
    legacy: false,
    locale: 'de',
    messages: { de, en },
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

describe('Help Page (#102)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('all handbook i18n keys compile without SyntaxError in German', () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'de',
      messages: { de },
    })
    const t = i18n.global.t

    for (const [role, chapters] of Object.entries(handbookContent)) {
      for (const chapter of chapters) {
        expect(() => t(chapter.title)).not.toThrow()
        for (const section of chapter.sections) {
          expect(() => t(section.title)).not.toThrow()
          for (const contentKey of section.content) {
            expect(() => t(contentKey), `Failed on key: ${contentKey} (role: ${role})`).not.toThrow()
          }
        }
      }
    }
  })

  it('all handbook i18n keys compile without SyntaxError in English', () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    const t = i18n.global.t

    for (const [role, chapters] of Object.entries(handbookContent)) {
      for (const chapter of chapters) {
        expect(() => t(chapter.title)).not.toThrow()
        for (const section of chapter.sections) {
          expect(() => t(section.title)).not.toThrow()
          for (const contentKey of section.content) {
            expect(() => t(contentKey), `Failed on key: ${contentKey} (role: ${role})`).not.toThrow()
          }
        }
      }
    }
  })

  it('HelpView renders without errors for parent role', async () => {
    const { default: HelpView } = await import('@/views/HelpView.vue')
    const plugins = createTestPlugins()

    // Set up auth store with parent role
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    auth.user = { id: '1', email: 'test@test.de', firstName: 'Test', lastName: 'User', role: 'PARENT' } as any

    const wrapper = mount(HelpView, plugins)
    // Should render chapters
    expect(wrapper.find('.help-chapters').exists()).toBe(true)
    expect(wrapper.findAll('.help-chapter').length).toBeGreaterThan(0)
    wrapper.unmount()
  })

  it('HelpView renders without errors for admin role', async () => {
    const { default: HelpView } = await import('@/views/HelpView.vue')
    const plugins = createTestPlugins()

    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    auth.user = { id: '1', email: 'admin@test.de', firstName: 'Admin', lastName: 'User', role: 'SUPERADMIN' } as any

    const wrapper = mount(HelpView, plugins)
    expect(wrapper.find('.help-chapters').exists()).toBe(true)
    expect(wrapper.findAll('.help-chapter').length).toBeGreaterThan(0)
    wrapper.unmount()
  })

  it('HelpView has no critical accessibility violations', { timeout: 15000 }, async () => {
    const { default: HelpView } = await import('@/views/HelpView.vue')
    const plugins = createTestPlugins()

    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    auth.user = { id: '1', email: 'test@test.de', firstName: 'Test', lastName: 'User', role: 'PARENT' } as any

    const wrapper = mount(HelpView, plugins)
    const results = await axe(wrapper.element)
    const serious = results.violations.filter(
      (v: any) => v.impact === 'critical' || v.impact === 'serious',
    )
    expect(serious).toEqual([])
    wrapper.unmount()
  })
})
