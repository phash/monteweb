import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import SystemBanner from '../SystemBanner.vue'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ params: {}, query: {} }),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: { de: {} },
})

const stubs = {
  Message: {
    template: '<div class="message-stub" :severity="severity" @click="$emit(\'click\')"><slot /></div>',
    props: ['severity', 'closable'],
    emits: ['click'],
  },
}

function mountComponent(props = {}) {
  return mount(SystemBanner, {
    props: { banners: [], ...props },
    global: {
      plugins: [i18n],
      stubs,
    },
  })
}

describe('SystemBanner', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should not render anything when banners array is empty', () => {
    const wrapper = mountComponent({ banners: [] })
    expect(wrapper.find('.system-banners').exists()).toBe(false)
  })

  it('should render banners when provided', () => {
    const banners = [
      { id: 'banner-1', bannerType: 'CLEANING', title: 'Putztermin', content: 'Naechster Putztermin am Freitag', link: null, expiresAt: null },
    ]
    const wrapper = mountComponent({ banners })

    expect(wrapper.find('.system-banners').exists()).toBe(true)
    expect(wrapper.findAll('.message-stub')).toHaveLength(1)
    expect(wrapper.text()).toContain('Putztermin')
    expect(wrapper.text()).toContain('Naechster Putztermin am Freitag')
  })

  it('should render multiple banners', () => {
    const banners = [
      { id: 'banner-1', bannerType: 'CLEANING', title: 'Putztermin', content: 'Freitag putzen', link: null, expiresAt: null },
      { id: 'banner-2', bannerType: 'INFO', title: 'Schulinfo', content: 'Elternabend naechste Woche', link: '/calendar', expiresAt: null },
    ]
    const wrapper = mountComponent({ banners })

    expect(wrapper.findAll('.message-stub')).toHaveLength(2)
    expect(wrapper.text()).toContain('Putztermin')
    expect(wrapper.text()).toContain('Schulinfo')
  })

  it('should navigate when a banner with link is clicked', async () => {
    const banners = [
      { id: 'banner-1', bannerType: 'INFO', title: 'Link Banner', content: 'Klicke hier', link: '/calendar', expiresAt: null },
    ]
    const wrapper = mountComponent({ banners })

    await wrapper.find('.message-stub').trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/calendar')
  })

  it('should not navigate when a banner without link is clicked', async () => {
    const banners = [
      { id: 'banner-1', bannerType: 'INFO', title: 'No Link', content: 'No navigation', link: null, expiresAt: null },
    ]
    const wrapper = mountComponent({ banners })

    await wrapper.find('.message-stub').trigger('click')
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('should display banner title in bold and content in paragraph', () => {
    const banners = [
      { id: 'banner-1', bannerType: 'WARNING', title: 'Bold Title', content: 'Description text', link: null, expiresAt: null },
    ]
    const wrapper = mountComponent({ banners })

    expect(wrapper.find('strong').text()).toBe('Bold Title')
    expect(wrapper.find('p').text()).toBe('Description text')
  })
})
