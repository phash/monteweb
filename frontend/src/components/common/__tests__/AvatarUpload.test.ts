import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AvatarUpload from '@/components/common/AvatarUpload.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      common: { removeAvatar: 'Avatar entfernen' },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'size'],
    emits: ['click'],
  },
}

function mountAvatar(props = {}) {
  return mount(AvatarUpload, {
    props,
    global: { plugins: [i18n], stubs },
  })
}

describe('AvatarUpload', () => {
  it('should render default icon when no image', () => {
    const wrapper = mountAvatar()
    expect(wrapper.find('.pi-user').exists()).toBe(true)
  })

  it('should render image when imageUrl provided', () => {
    const wrapper = mountAvatar({ imageUrl: 'https://example.com/avatar.png' })
    expect(wrapper.find('.avatar-img').exists()).toBe(true)
    expect(wrapper.find('.avatar-img').attributes('src')).toBe('https://example.com/avatar.png')
  })

  it('should show remove button when editable and has image', () => {
    const wrapper = mountAvatar({ imageUrl: 'https://example.com/avatar.png', editable: true })
    expect(wrapper.find('.button-stub').exists()).toBe(true)
  })

  it('should not show remove button when not editable', () => {
    const wrapper = mountAvatar({ imageUrl: 'https://example.com/avatar.png', editable: false })
    expect(wrapper.find('.button-stub').exists()).toBe(false)
  })

  it('should emit remove when remove button clicked', async () => {
    const wrapper = mountAvatar({ imageUrl: 'https://example.com/avatar.png', editable: true })
    await wrapper.find('.button-stub').trigger('click')
    expect(wrapper.emitted('remove')).toHaveLength(1)
  })
})
