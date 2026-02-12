import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import BottomNav from '@/components/layout/BottomNav.vue'

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ name: 'dashboard', path: '/', matched: [] })),
  useRouter: vi.fn(() => ({ push: vi.fn() })),
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
        profile: 'Profil',
        admin: 'Verwaltung',
        more: 'Mehr',
        mainNavigation: 'Hauptnavigation',
      },
      cleaning: { admin: { putzOrgaManagement: 'Putz-Orga Verwaltung' } },
    },
  },
})

function mountBottomNav() {
  const pinia = createPinia()
  return mount(BottomNav, {
    global: {
      plugins: [i18n, pinia],
      stubs: {
        'router-link': {
          template: '<a class="router-link-stub nav-item"><slot /></a>',
          props: ['to'],
        },
      },
    },
  })
}

describe('BottomNav', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render bottom nav bar', () => {
    const wrapper = mountBottomNav()
    expect(wrapper.find('.bottom-nav').exists()).toBe(true)
  })

  it('should render primary nav items', () => {
    const wrapper = mountBottomNav()
    expect(wrapper.text()).toContain('Dashboard')
    expect(wrapper.text()).toContain('Räume')
    expect(wrapper.text()).toContain('Familie')
  })

  it('should render more button', () => {
    const wrapper = mountBottomNav()
    expect(wrapper.text()).toContain('Mehr')
  })

  it('should toggle more menu on click', async () => {
    const wrapper = mountBottomNav()
    expect(wrapper.find('.more-menu').exists()).toBe(false)
    const moreBtn = wrapper.find('.bottom-nav-item:last-child')
    await moreBtn.trigger('click')
    expect(wrapper.find('.more-menu').exists()).toBe(true)
  })
})
