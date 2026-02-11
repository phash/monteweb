import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import InviteMemberDialog from '@/components/family/InviteMemberDialog.vue'

vi.mock('@/api/users.api', () => ({
  usersApi: {
    search: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
  },
}))

vi.mock('@/api/family.api', () => ({
  familyApi: {
    inviteMember: vi.fn().mockResolvedValue({ data: { data: {} } }),
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      family: {
        inviteMember: 'Mitglied einladen',
        searchMember: 'Mitglied suchen',
        sendInvitation: 'Einladung senden',
        roles: { PARENT: 'Elternteil', CHILD: 'Kind' },
      },
      messages: { searchUserPlaceholder: 'Benutzer suchen...' },
      common: { cancel: 'Abbrechen' },
      error: { unexpected: 'Ein Fehler ist aufgetreten' },
    },
  },
})

const stubs = {
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  AutoComplete: {
    template: '<input class="autocomplete-stub" />',
    props: ['modelValue', 'suggestions', 'optionLabel', 'placeholder'],
    emits: ['complete', 'update:modelValue'],
  },
  Select: {
    template: '<select class="select-stub" />',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
  },
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'severity', 'text', 'disabled', 'loading'],
    emits: ['click'],
  },
}

function mountDialog(visible = true) {
  return mount(InviteMemberDialog, {
    props: { visible, familyId: 'fam-1' },
    global: { plugins: [i18n], stubs },
  })
}

describe('InviteMemberDialog', () => {
  it('should render when visible', () => {
    const wrapper = mountDialog(true)
    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should not render when not visible', () => {
    const wrapper = mountDialog(false)
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)
  })

  it('should render autocomplete and select', () => {
    const wrapper = mountDialog()
    expect(wrapper.find('.autocomplete-stub').exists()).toBe(true)
    expect(wrapper.find('.select-stub').exists()).toBe(true)
  })

  it('should render cancel and invite buttons', () => {
    const wrapper = mountDialog()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons.length).toBe(2)
    expect(buttons[0].text()).toContain('Abbrechen')
    expect(buttons[1].text()).toContain('Einladung senden')
  })
})
