import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import DashboardView from '@/views/DashboardView.vue'

vi.mock('@/api/feed.api', () => ({
  feedApi: {
    getFeed: vi.fn(),
    getBanners: vi.fn().mockResolvedValue({ data: { data: [] } }),
    createPost: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({ adminApi: { getPublicConfig: vi.fn() } }))
vi.mock('@/api/family.api', () => ({
  familyApi: { getMine: vi.fn().mockResolvedValue({ data: { data: [] } }) },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      nav: { dashboard: 'Dashboard' },
      dashboard: { welcome: 'Willkommen, {name}!' },
      feed: { postCreated: 'Beitrag veröffentlicht' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  SystemBanner: { template: '<div class="banner-stub" />', props: ['banners'] },
  PostComposer: { template: '<div class="composer-stub" />' },
  FeedList: { template: '<div class="feed-list-stub" />' },
  FamilyHoursWidget: { template: '<div class="hours-stub" />', props: ['familyId'] },
}

function mountDashboard() {
  const pinia = createPinia()
  return mount(DashboardView, {
    global: {
      plugins: [i18n, pinia],
      stubs,
    },
  })
}

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountDashboard()
    expect(wrapper.find('.page-title-stub').exists()).toBe(true)
  })

  it('should render feed list', () => {
    const wrapper = mountDashboard()
    expect(wrapper.find('.feed-list-stub').exists()).toBe(true)
  })

  it('should render system banner component', () => {
    const wrapper = mountDashboard()
    expect(wrapper.find('.banner-stub').exists()).toBe(true)
  })
})
