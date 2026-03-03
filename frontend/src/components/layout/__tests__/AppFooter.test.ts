import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AppFooter from '@/components/layout/AppFooter.vue'

// Mock the global constants that AppFooter uses
vi.stubGlobal('__APP_VERSION__', '2.1.0')
vi.stubGlobal('__BUILD_TIME__', '2026-01-15T10:30:00Z')
vi.stubGlobal('__GIT_BRANCH__', 'main')

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      footer: {
        deployed: 'Deployed',
      },
    },
  },
})

function createWrapper() {
  return mount(AppFooter, {
    global: {
      plugins: [i18n],
    },
  })
}

describe('AppFooter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render footer element', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.app-footer').exists()).toBe(true)
  })

  it('should display MonteWeb text', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('MonteWeb')
  })

  it('should display version number', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('2.1.0')
  })

  it('should display git branch', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('main')
  })

  it('should display deployed label', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('Deployed')
  })

  it('should display build time in German format', () => {
    const wrapper = createWrapper()
    // The date is formatted with de-DE locale
    expect(wrapper.text()).toContain('15.01.2026')
  })

  it('should display powered by link', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('Powered by')
  })

  it('should link to GitHub repository', () => {
    const wrapper = createWrapper()
    const link = wrapper.find('.powered a')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('https://github.com/phash/monteweb')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
  })

  it('should have footer-content wrapper', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.footer-content').exists()).toBe(true)
  })
})
