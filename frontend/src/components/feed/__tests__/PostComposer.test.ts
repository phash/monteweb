import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import PostComposer from '@/components/feed/PostComposer.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      feed: {
        post: 'Veröffentlichen',
        titlePlaceholder: 'Titel (optional)',
        contentPlaceholder: 'Was gibt es Neues?',
      },
    },
  },
})

const stubs = {
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder', 'ariaLabel'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'placeholder', 'ariaLabel', 'autoResize', 'rows'] },
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'loading', 'disabled', 'size'],
    emits: ['click'],
  },
}

describe('PostComposer', () => {
  it('should render title input and content textarea', () => {
    const wrapper = mount(PostComposer, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.input-stub').exists()).toBe(true)
    expect(wrapper.find('.textarea-stub').exists()).toBe(true)
  })

  it('should render post button', () => {
    const wrapper = mount(PostComposer, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.button-stub').text()).toContain('Veröffentlichen')
  })

  it('should have card class', () => {
    const wrapper = mount(PostComposer, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.post-composer').exists()).toBe(true)
  })
})
