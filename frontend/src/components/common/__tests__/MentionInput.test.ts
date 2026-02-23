import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import MentionInput from '@/components/common/MentionInput.vue'

vi.mock('@/api/users.api', () => ({
  usersApi: {
    search: vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [
            { id: '550e8400-e29b-41d4-a716-446655440000', displayName: 'Max Mustermann', role: 'PARENT', email: 'max@test.de', firstName: 'Max', lastName: 'Mustermann', phone: null, avatarUrl: null, specialRoles: [], assignedRoles: [], active: true },
            { id: '660e8400-e29b-41d4-a716-446655440001', displayName: 'Anna Schmidt', role: 'TEACHER', email: 'anna@test.de', firstName: 'Anna', lastName: 'Schmidt', phone: null, avatarUrl: null, specialRoles: [], assignedRoles: [], active: true },
          ],
        },
      },
    }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      mentions: {
        noResults: 'Keine Benutzer gefunden',
      },
    },
  },
})

const TextareaStub = {
  template: '<textarea class="textarea-stub" :value="modelValue" @input="$emit(\'input\', $event)" @keydown="$emit(\'keydown\', $event)" />',
  props: ['modelValue', 'placeholder', 'autoResize', 'rows'],
  emits: ['input', 'keydown'],
}

describe('MentionInput', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it('should render a textarea', () => {
    const wrapper = mount(MentionInput, {
      props: { modelValue: '', placeholder: 'Type here...' },
      global: {
        plugins: [i18n],
        stubs: { Textarea: TextareaStub },
      },
    })
    expect(wrapper.find('.mention-input-wrapper').exists()).toBe(true)
    expect(wrapper.find('.textarea-stub').exists()).toBe(true)
  })

  it('should emit update:modelValue on input', async () => {
    const wrapper = mount(MentionInput, {
      props: { modelValue: '' },
      global: {
        plugins: [i18n],
        stubs: { Textarea: TextareaStub },
      },
    })

    const textarea = wrapper.find('.textarea-stub')
    const nativeEl = textarea.element as HTMLTextAreaElement
    nativeEl.value = 'Hello'
    await textarea.trigger('input')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('should accept placeholder prop', () => {
    const wrapper = mount(MentionInput, {
      props: { modelValue: '', placeholder: 'Write something...' },
      global: {
        plugins: [i18n],
        stubs: { Textarea: TextareaStub },
      },
    })
    expect(wrapper.find('.mention-input-wrapper').exists()).toBe(true)
  })

  it('should accept rows prop', () => {
    const wrapper = mount(MentionInput, {
      props: { modelValue: '', rows: 5 },
      global: {
        plugins: [i18n],
        stubs: { Textarea: TextareaStub },
      },
    })
    expect(wrapper.find('.mention-input-wrapper').exists()).toBe(true)
  })

  it('should forward keydown events', async () => {
    const wrapper = mount(MentionInput, {
      props: { modelValue: '' },
      global: {
        plugins: [i18n],
        stubs: { Textarea: TextareaStub },
      },
    })

    const textarea = wrapper.find('.textarea-stub')
    await textarea.trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('keydown')).toBeTruthy()
  })

  it('should not show dropdown initially', () => {
    const wrapper = mount(MentionInput, {
      props: { modelValue: '' },
      global: {
        plugins: [i18n],
        stubs: { Textarea: TextareaStub },
      },
    })
    expect(document.querySelector('.mention-dropdown')).toBeNull()
  })
})
