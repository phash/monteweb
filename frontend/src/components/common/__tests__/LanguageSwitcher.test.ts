import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { setActivePinia, createPinia } from 'pinia'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import { useAdminStore } from '@/stores/admin'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({
      data: { data: { modules: {}, multilanguageEnabled: true, defaultLanguage: 'de' } },
    }),
  },
}))
vi.mock('@/api/auth.api', () => ({ authApi: {} }))

function createTestI18n() {
  return createI18n({
    legacy: false,
    locale: 'de',
    messages: { de: {}, en: {} },
  })
}

function mountSwitcher(i18n = createTestI18n()) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const store = useAdminStore()
  store.config = {
    id: '1',
    schoolName: 'Test',
    logoUrl: null,
    theme: {},
    modules: {},
    targetHoursPerFamily: 30,
    targetCleaningHours: 3,
    bundesland: 'BY',
    schoolVacations: [],
    requireAssignmentConfirmation: true,
    multilanguageEnabled: true,
    defaultLanguage: 'de',
    requireUserApproval: true,
  }

  return mount(LanguageSwitcher, {
    global: {
      plugins: [i18n, pinia],
      stubs: {
        Select: {
          template: '<select class="select-stub" @change="$emit(\'change\', { value: $event.target.value })"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
          props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
          emits: ['change'],
        },
      },
    },
  })
}

describe('LanguageSwitcher', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should render select with language options', () => {
    const wrapper = mountSwitcher()
    expect(wrapper.find('.select-stub').exists()).toBe(true)
    expect(wrapper.findAll('option')).toHaveLength(2)
  })

  it('should show Deutsch and English options', () => {
    const wrapper = mountSwitcher()
    const options = wrapper.findAll('option')
    expect(options[0].text()).toBe('Deutsch')
    expect(options[1].text()).toBe('English')
  })

  it('should save locale to localStorage on change', async () => {
    const i18n = createTestI18n()
    const wrapper = mountSwitcher(i18n)
    const select = wrapper.find('.select-stub')
    await select.setValue('en')
    expect(localStorage.getItem('monteweb-locale')).toBe('en')
  })
})
