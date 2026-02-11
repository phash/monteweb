import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import NewMessageDialog from '@/components/messaging/NewMessageDialog.vue'

vi.mock('@/api/users.api', () => ({
  usersApi: {
    search: vi.fn(),
  },
}))

vi.mock('@/stores/messaging', () => ({
  useMessagingStore: vi.fn(() => ({
    startDirectConversation: vi.fn(),
  })),
}))

import { usersApi } from '@/api/users.api'
import { useMessagingStore } from '@/stores/messaging'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      messages: {
        newMessage: 'Neue Nachricht',
        searchUser: 'Benutzer suchen',
        searchUserPlaceholder: 'Name eingeben...',
        startConversation: 'Nachricht senden',
        communicationNotAllowed: 'Kommunikation nicht erlaubt',
      },
      common: { cancel: 'Abbrechen' },
    },
  },
})

function mountDialog(props = {}) {
  return mount(NewMessageDialog, {
    props: { visible: true, ...props },
    global: {
      plugins: [i18n, createPinia()],
      stubs: {
        Dialog: {
          template: '<div class="dialog-stub"><slot /><slot name="footer" /></div>',
          props: ['visible', 'header', 'modal'],
        },
        AutoComplete: {
          template: '<input class="autocomplete-stub" />',
          props: ['modelValue', 'suggestions', 'optionLabel', 'placeholder'],
          emits: ['update:modelValue', 'complete'],
        },
        Button: {
          template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')"><slot />{{ label }}</button>',
          props: ['label', 'disabled', 'loading', 'severity', 'text'],
          emits: ['click'],
        },
      },
    },
  })
}

describe('NewMessageDialog', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render dialog with header', () => {
    const wrapper = mountDialog()
    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should render autocomplete and buttons', () => {
    const wrapper = mountDialog()
    expect(wrapper.find('.autocomplete-stub').exists()).toBe(true)
    expect(wrapper.findAll('.button-stub')).toHaveLength(2)
  })

  it('should disable send button when no user selected', () => {
    const wrapper = mountDialog()
    const buttons = wrapper.findAll('.button-stub')
    const sendButton = buttons[1]
    expect(sendButton.attributes('disabled')).toBeDefined()
  })

  it('should emit update:visible on cancel click', async () => {
    const wrapper = mountDialog()
    const cancelButton = wrapper.findAll('.button-stub')[0]
    await cancelButton.trigger('click')

    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')![0]).toEqual([false])
  })

  it('should render error text slot', () => {
    const wrapper = mountDialog()
    // No error initially
    expect(wrapper.find('.error-text').exists()).toBe(false)
  })
})
