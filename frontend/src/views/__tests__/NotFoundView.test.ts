import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import NotFoundView from '@/views/NotFoundView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      error: {
        notFound: '404 - Seite nicht gefunden',
        notFoundMessage: 'Die angeforderte Seite existiert nicht.',
        backHome: 'Zurück zum Dashboard',
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon'],
    emits: ['click'],
  },
}

describe('NotFoundView', () => {
  it('should render 404 heading', () => {
    const wrapper = mount(NotFoundView, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('h1').text()).toContain('404')
  })

  it('should render back home button', () => {
    const wrapper = mount(NotFoundView, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.button-stub').text()).toContain('Dashboard')
  })
})
