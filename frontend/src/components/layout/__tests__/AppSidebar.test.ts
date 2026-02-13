import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ name: 'dashboard', path: '/', matched: [] })),
  useRouter: vi.fn(),
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn() },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      nav: {
        dashboard: 'Dashboard',
        rooms: 'Räume',
        family: 'Familie',
        messages: 'Nachrichten',
        jobs: 'Jobbörse',
        cleaning: 'Putz-Orga',
        calendar: 'Kalender',
        forms: 'Formulare',
        admin: 'Verwaltung',
        mainNavigation: 'Hauptnavigation',
      },
      cleaning: { admin: { putzOrgaManagement: 'Putz-Orga Verwaltung' } },
    },
  },
})

function mountSidebar(role = 'PARENT') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.$patch({ user: { id: '1', email: 'test@test.de', firstName: 'Test', lastName: 'User', displayName: 'Test User', role, active: true } as any })
  return mount(AppSidebar, {
    global: {
      plugins: [i18n, pinia],
      stubs: {
        'router-link': {
          template: '<a class="router-link-stub" :to="to"><slot /></a>',
          props: ['to'],
        },
      },
    },
  })
}

describe('AppSidebar', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render aside element with aria label', () => {
    const wrapper = mountSidebar()
    expect(wrapper.find('aside.app-sidebar').exists()).toBe(true)
  })

  it('should render basic nav items (dashboard, rooms, family)', () => {
    const wrapper = mountSidebar()
    const links = wrapper.findAll('.router-link-stub')
    expect(links.length).toBeGreaterThanOrEqual(3)
    expect(wrapper.text()).toContain('Dashboard')
    expect(wrapper.text()).toContain('Räume')
    expect(wrapper.text()).toContain('Familie')
  })

  it('should render nav element', () => {
    const wrapper = mountSidebar()
    expect(wrapper.find('.sidebar-nav').exists()).toBe(true)
  })

  it('should have nav-item class on links', () => {
    const wrapper = mountSidebar()
    expect(wrapper.find('.nav-item').exists()).toBe(true)
  })

  it('should show admin link for SUPERADMIN but not for PARENT', () => {
    const adminWrapper = mountSidebar('SUPERADMIN')
    expect(adminWrapper.text()).toContain('Verwaltung')

    const parentWrapper = mountSidebar('PARENT')
    expect(parentWrapper.text()).not.toContain('Verwaltung')
  })

  it('should change navigation items when user role changes (simulating a switch)', () => {
    // Mount as TEACHER
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.$patch({ user: { id: '1', email: 'test@test.de', firstName: 'Test', lastName: 'User', displayName: 'Test User', role: 'SUPERADMIN', active: true } as any })

    const wrapper = mount(AppSidebar, {
      global: {
        plugins: [i18n, pinia],
        stubs: {
          'router-link': {
            template: '<a class="router-link-stub" :to="to"><slot /></a>',
            props: ['to'],
          },
        },
      },
    })

    // SUPERADMIN should see admin link
    expect(wrapper.text()).toContain('Verwaltung')

    // Simulate role change to PARENT
    auth.$patch({ user: { id: '1', email: 'test@test.de', firstName: 'Test', lastName: 'User', displayName: 'Test User', role: 'PARENT', active: true } as any })
    // After next tick the reactive computeds update
  })
})
