import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import AppLayout from '../AppLayout.vue'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getConfig: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/auth.api', () => ({
  authApi: { login: vi.fn(), register: vi.fn(), logout: vi.fn() },
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
      common: { skipToContent: 'Zum Inhalt springen' },
      nav: { profile: 'Profil', logout: 'Abmelden', dashboard: 'Dashboard', rooms: 'Raeume' },
    },
  },
})

function createWrapper() {
  return mount(AppLayout, {
    global: {
      plugins: [createPinia(), i18n],
      stubs: {
        AppHeader: { template: '<header class="app-header-stub" />' },
        AppSidebar: { template: '<aside class="sidebar-stub" />' },
        BottomNav: { template: '<nav class="bottomnav-stub" />' },
        AppBreadcrumb: { template: '<nav class="breadcrumb-stub" />' },
        ErrorBoundary: { template: '<div class="error-boundary-stub"><slot /></div>' },
        PwaInstallBanner: { template: '<div class="pwa-install-stub" />' },
        OfflineBanner: { template: '<div class="offline-banner-stub" />' },
        'router-view': { template: '<div class="router-view-stub" />' },
      },
    },
  })
}

describe('AppLayout', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render app-layout container', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.app-layout').exists()).toBe(true)
  })

  it('should render skip link for accessibility', () => {
    const wrapper = createWrapper()
    const skipLink = wrapper.find('.skip-link')
    expect(skipLink.exists()).toBe(true)
    expect(skipLink.attributes('href')).toBe('#main-content')
  })

  it('should render AppHeader', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.app-header-stub').exists()).toBe(true)
  })

  it('should render AppSidebar', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.sidebar-stub').exists()).toBe(true)
  })

  it('should render BottomNav', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.bottomnav-stub').exists()).toBe(true)
  })

  it('should render main content area with correct id', () => {
    const wrapper = createWrapper()
    const main = wrapper.find('#main-content')
    expect(main.exists()).toBe(true)
    expect(main.attributes('tabindex')).toBe('-1')
  })

  it('should wrap router-view in ErrorBoundary', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.error-boundary-stub').exists()).toBe(true)
    expect(wrapper.find('.error-boundary-stub .router-view-stub').exists()).toBe(true)
  })

  it('should render AppBreadcrumb inside main', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('#main-content .breadcrumb-stub').exists()).toBe(true)
  })
})
