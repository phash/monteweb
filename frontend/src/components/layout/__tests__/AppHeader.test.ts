import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import AppHeader from '../AppHeader.vue'

vi.mock('@/api/auth.api', () => ({
  authApi: { login: vi.fn(), register: vi.fn(), logout: vi.fn() },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getConfig: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: { getMe: vi.fn() },
}))

vi.mock('@/api/notifications.api', () => ({
  notificationsApi: {
    getNotifications: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    getUnreadCount: vi.fn().mockResolvedValue({ data: { data: { count: 0 } } }),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      nav: { profile: 'Profil', logout: 'Abmelden' },
    },
  },
})

function createWrapper() {
  return mount(AppHeader, {
    global: {
      plugins: [createPinia(), i18n],
      stubs: {
        'router-link': { template: '<a><slot /></a>' },
        Button: { template: '<button><slot /></button>', props: ['label', 'icon', 'severity', 'text', 'aria-label'] },
        Menu: { template: '<div />', methods: { toggle: vi.fn() } },
        NotificationBell: { template: '<div class="notification-bell" />' },
        LanguageSwitcher: { template: '<div class="language-switcher" />' },
      },
    },
  })
}

describe('AppHeader', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render header element', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.app-header').exists()).toBe(true)
  })

  it('should render the school name or MonteWeb default', () => {
    const wrapper = createWrapper()
    // Admin config not loaded, so falls back to 'MonteWeb'
    expect(wrapper.text()).toContain('MonteWeb')
  })

  it('should render NotificationBell component', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.notification-bell').exists()).toBe(true)
  })

  it('should render LanguageSwitcher component', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.language-switcher').exists()).toBe(true)
  })

  it('should have a logo link', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.header-logo').exists()).toBe(true)
  })

  it('should have header-right section with controls', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.header-right').exists()).toBe(true)
  })
})
