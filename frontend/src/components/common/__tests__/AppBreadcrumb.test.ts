import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({
    matched: [
      { meta: { breadcrumbLabel: 'nav.rooms' }, name: 'rooms' },
      { meta: { breadcrumbLabel: 'nav.profile' }, name: 'profile' },
    ],
  })),
  useRouter: vi.fn(),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      nav: { rooms: 'Räume', profile: 'Profil' },
    },
  },
})

function mountBreadcrumb() {
  return mount(AppBreadcrumb, {
    global: {
      plugins: [i18n],
      stubs: {
        Breadcrumb: {
          template: '<nav class="breadcrumb-stub"><slot name="item" v-for="item in model" :item="item" /></nav>',
          props: ['home', 'model'],
        },
        'router-link': {
          template: '<a class="router-link-stub"><slot /></a>',
          props: ['to'],
        },
      },
    },
  })
}

describe('AppBreadcrumb', () => {
  it('should render breadcrumb when route has labels', () => {
    const wrapper = mountBreadcrumb()
    expect(wrapper.find('.breadcrumb-stub').exists()).toBe(true)
  })

  it('should render translated labels', () => {
    const wrapper = mountBreadcrumb()
    expect(wrapper.text()).toContain('Räume')
  })

  it('should render last item as current (not a link)', () => {
    const wrapper = mountBreadcrumb()
    expect(wrapper.find('.breadcrumb-current').exists()).toBe(true)
  })
})
