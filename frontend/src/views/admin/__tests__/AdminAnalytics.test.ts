import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createPinia, setActivePinia } from 'pinia'
import AdminAnalytics from '@/views/admin/AdminAnalytics.vue'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getAnalytics: vi.fn().mockResolvedValue({
      data: {
        data: {
          totalUsers: 120,
          activeUsers: 98,
          rooms: 8,
          posts: 350,
          events: 42,
          messages: 1500,
          postsThisMonth: 25,
          newThisWeek: 3,
        },
      },
    }),
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      admin: {
        analytics: {
          title: 'Statistiken',
          subtitle: 'Nutzungsübersicht',
          totalUsers: 'Nutzer gesamt',
          activeUsers: 'Aktive Nutzer',
          rooms: 'Räume',
          posts: 'Beiträge',
          events: 'Termine',
          messages: 'Nachrichten',
          postsThisMonth: 'Beiträge diesen Monat',
          newThisWeek: 'Neu diese Woche',
        },
      },
      common: {
        error: 'Fehler',
        loading: 'Laden...',
        loadingTimeout: 'Laden dauert ungewöhnlich lange',
        retry: 'Erneut versuchen',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }} {{ subtitle }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
}

describe('AdminAnalytics', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render page title', async () => {
    const wrapper = mount(AdminAnalytics, { global: { plugins: [i18n], stubs } })
    await flushPromises()
    expect(wrapper.find('.page-title-stub').text()).toContain('Statistiken')
  })

  it('should render stat cards after loading', async () => {
    const wrapper = mount(AdminAnalytics, { global: { plugins: [i18n], stubs } })
    await flushPromises()
    expect(wrapper.findAll('.stat-card')).toHaveLength(8)
  })

  it('should display stat values', async () => {
    const wrapper = mount(AdminAnalytics, { global: { plugins: [i18n], stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('120')
    expect(wrapper.text()).toContain('98')
    expect(wrapper.text()).toContain('350')
  })

  it('should display stat labels', async () => {
    const wrapper = mount(AdminAnalytics, { global: { plugins: [i18n], stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('Nutzer gesamt')
    expect(wrapper.text()).toContain('Aktive Nutzer')
    expect(wrapper.text()).toContain('Räume')
  })
})
