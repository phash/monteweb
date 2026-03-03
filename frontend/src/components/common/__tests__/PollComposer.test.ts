import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import PollComposer from '@/components/common/PollComposer.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      poll: {
        createPoll: 'Umfrage erstellen',
        questionPlaceholder: 'Frage eingeben...',
        optionPlaceholder: 'Option {n}',
        addOption: 'Option hinzufügen',
        multipleChoice: 'Mehrfachauswahl',
      },
      common: {
        close: 'Schließen',
        cancel: 'Abbrechen',
        delete: 'Löschen',
      },
    },
  },
})

const stubs = {
  InputText: {
    template: '<input class="input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" :placeholder="placeholder" />',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  Button: {
    template: '<button class="btn-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}<i :class="icon" /></button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'disabled', 'ariaLabel'],
    emits: ['click'],
  },
  Checkbox: {
    template: '<input type="checkbox" class="checkbox-stub" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
    props: ['modelValue', 'binary'],
    emits: ['update:modelValue'],
  },
}

function createWrapper() {
  return mount(PollComposer, {
    global: {
      plugins: [i18n],
      stubs,
    },
  })
}

describe('PollComposer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render poll composer header', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('Umfrage erstellen')
  })

  it('should render question input', () => {
    const wrapper = createWrapper()
    const inputs = wrapper.findAll('.input-stub')
    expect(inputs.length).toBeGreaterThanOrEqual(1)
  })

  it('should render two option inputs by default', () => {
    const wrapper = createWrapper()
    const optionRows = wrapper.findAll('.poll-option-row')
    expect(optionRows).toHaveLength(2)
  })

  it('should add option when add option button is clicked', async () => {
    const wrapper = createWrapper()
    const addBtn = wrapper.findAll('.btn-stub').find(b => b.text().includes('Option hinzufügen'))
    expect(addBtn).toBeTruthy()
    await addBtn!.trigger('click')
    const optionRows = wrapper.findAll('.poll-option-row')
    expect(optionRows).toHaveLength(3)
  })

  it('should not allow more than 10 options', async () => {
    const wrapper = createWrapper()
    const addBtn = wrapper.findAll('.btn-stub').find(b => b.text().includes('Option hinzufügen'))
    // Add 8 more to reach 10
    for (let i = 0; i < 8; i++) {
      await addBtn!.trigger('click')
    }
    expect(wrapper.findAll('.poll-option-row')).toHaveLength(10)
    // Add button should disappear at 10
    const addBtnAfter = wrapper.findAll('.btn-stub').find(b => b.text().includes('Option hinzufügen'))
    expect(addBtnAfter).toBeUndefined()
  })

  it('should not show remove buttons when only 2 options', () => {
    const wrapper = createWrapper()
    // Delete buttons are only shown when > 2 options (aria-label: Löschen)
    const deleteBtns = wrapper.findAll('.poll-option-row .btn-stub')
    expect(deleteBtns).toHaveLength(0)
  })

  it('should show remove buttons when more than 2 options', async () => {
    const wrapper = createWrapper()
    const addBtn = wrapper.findAll('.btn-stub').find(b => b.text().includes('Option hinzufügen'))
    await addBtn!.trigger('click')
    const deleteBtns = wrapper.findAll('.poll-option-row .btn-stub')
    expect(deleteBtns.length).toBeGreaterThan(0)
  })

  it('should emit cancel when cancel button is clicked', async () => {
    const wrapper = createWrapper()
    const cancelBtn = wrapper.findAll('.btn-stub').find(b => b.text().includes('Abbrechen'))
    expect(cancelBtn).toBeTruthy()
    await cancelBtn!.trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('should emit cancel when close button in header is clicked', async () => {
    const wrapper = createWrapper()
    // The close button is in the header — it's the first button with pi-times icon
    const headerCloseBtn = wrapper.find('.poll-composer-header .btn-stub')
    await headerCloseBtn.trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('should have submit disabled when question and options are empty', () => {
    const wrapper = createWrapper()
    const submitBtn = wrapper.findAll('.poll-composer-actions .btn-stub').find(b => b.text().includes('Umfrage erstellen'))
    expect(submitBtn).toBeTruthy()
    expect(submitBtn!.attributes('disabled')).toBeDefined()
  })

  it('should render multiple choice checkbox', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.checkbox-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('Mehrfachauswahl')
  })
})
