import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      common: {
        loading: 'Laden...',
        loadingTimeout: 'Das Laden dauert länger als erwartet.',
        retry: 'Erneut versuchen',
      },
    },
  },
})

function mountSpinner(props = {}) {
  return mount(LoadingSpinner, {
    props,
    global: {
      plugins: [i18n],
      stubs: {
        ProgressSpinner: { template: '<div class="spinner-stub" />' },
        Button: {
          template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
          props: ['label', 'icon', 'text', 'size'],
          emits: ['click'],
        },
      },
    },
  })
}

describe('LoadingSpinner', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it('should render spinner', () => {
    const wrapper = mountSpinner()
    expect(wrapper.find('.spinner-stub').exists()).toBe(true)
    vi.useRealTimers()
  })

  it('should show timeout message after timeout', async () => {
    const wrapper = mountSpinner({ timeout: 500 })
    expect(wrapper.find('.timeout-message').exists()).toBe(false)
    vi.advanceTimersByTime(600)
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.timeout-message').exists()).toBe(true)
    vi.useRealTimers()
  })

  it('should emit retry and reset timeout on button click', async () => {
    const wrapper = mountSpinner({ timeout: 500 })
    vi.advanceTimersByTime(600)
    await wrapper.vm.$nextTick()
    await wrapper.find('.button-stub').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.timeout-message').exists()).toBe(false)
    vi.useRealTimers()
  })
})
