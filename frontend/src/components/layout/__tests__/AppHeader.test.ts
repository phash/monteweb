import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import AppHeader from '../AppHeader.vue'
import { useAuthStore } from '@/stores/auth'

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
  usersApi: { getMe: vi.fn(), switchActiveRole: vi.fn() },
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
      profile: {
        roleLabels: {
          SUPERADMIN: 'Superadmin',
          SECTION_ADMIN: 'Bereichsleitung',
          TEACHER: 'Lehrkraft',
          PARENT: 'Elternteil',
          STUDENT: 'Schüler/in',
        },
        switchRole: 'Rolle wechseln',
        roleSwitched: 'Rolle gewechselt zu {role}',
      },
      error: { unexpected: 'Fehler' },
    },
  },
})

function createWrapper(role = 'PARENT', assignedRoles: string[] = []) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.$patch({
    user: {
      id: '1',
      email: 'test@test.de',
      firstName: 'Test',
      lastName: 'User',
      displayName: 'Test User',
      role,
      assignedRoles,
      active: true,
    } as any,
  })
  return mount(AppHeader, {
    global: {
      plugins: [pinia, i18n],
      stubs: {
        'router-link': { template: '<a><slot /></a>' },
        Button: { template: '<button><slot /></button>', props: ['label', 'icon', 'severity', 'text', 'aria-label'] },
        Menu: { template: '<div />', methods: { toggle: vi.fn() } },
        Tag: { template: '<span class="tag-stub" :class="severity">{{ value }}</span>', props: ['value', 'severity'] },
        Popover: { template: '<div class="popover-stub"><slot /></div>', methods: { toggle: vi.fn(), hide: vi.fn() } },
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

  describe('role badge', () => {
    it('should display active role badge', () => {
      const wrapper = createWrapper('TEACHER', ['TEACHER'])
      expect(wrapper.text()).toContain('Lehrkraft')
    })

    it('should display correct role name for PARENT', () => {
      const wrapper = createWrapper('PARENT', ['PARENT'])
      expect(wrapper.text()).toContain('Elternteil')
    })

    it('should display correct role name for SUPERADMIN', () => {
      const wrapper = createWrapper('SUPERADMIN', [])
      expect(wrapper.text()).toContain('Superadmin')
    })

    it('should have clickable class when user can switch roles', () => {
      const wrapper = createWrapper('TEACHER', ['TEACHER', 'PARENT'])
      expect(wrapper.find('.role-badge.clickable').exists()).toBe(true)
    })

    it('should not have clickable class for single-role user', () => {
      const wrapper = createWrapper('TEACHER', ['TEACHER'])
      expect(wrapper.find('.role-badge.clickable').exists()).toBe(false)
    })

    it('should render role switcher popover when canSwitchRole is true', () => {
      const wrapper = createWrapper('TEACHER', ['TEACHER', 'PARENT'])
      expect(wrapper.find('.popover-stub').exists()).toBe(true)
    })

    it('should not render role switcher popover when canSwitchRole is false', () => {
      const wrapper = createWrapper('TEACHER', ['TEACHER'])
      expect(wrapper.find('.popover-stub').exists()).toBe(false)
    })
  })
})
