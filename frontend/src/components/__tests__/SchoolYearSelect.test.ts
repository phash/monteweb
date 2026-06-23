import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import SchoolYearSelect from '@/components/common/SchoolYearSelect.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: { de: { jobboard: { schoolYear: 'Schuljahr' } } },
})

function mountSelect(props = {}) {
  return mount(SchoolYearSelect, {
    props: { modelValue: null, options: [], ...props },
    global: {
      plugins: [i18n],
      stubs: {
        Select: {
          template: '<select class="select-stub" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
          props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'],
          emits: ['update:modelValue'],
        },
      },
    },
  })
}

describe('SchoolYearSelect', () => {
  it('renders a select', () => {
    const wrapper = mountSelect({
      options: [{ id: 'p1', name: 'Schuljahr 2025/2026', startDate: '', endDate: '', active: true }],
    })
    expect(wrapper.find('.select-stub').exists()).toBe(true)
  })

  it('emits update:modelValue when the selection changes', async () => {
    const wrapper = mountSelect({
      modelValue: 'p1',
      options: [{ id: 'p1', name: 'A', startDate: '', endDate: '', active: true }],
    })
    await wrapper.find('.select-stub').setValue('p1')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })
})
