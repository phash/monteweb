import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import CleaningView from '@/views/CleaningView.vue'

vi.mock('@/api/cleaning.api', () => ({
  getUpcomingSlots: vi.fn().mockResolvedValue({ data: { data: { content: [], totalPages: 0 } } }),
  getMySlots: vi.fn().mockResolvedValue({ data: { data: [] } }),
  getSlotById: vi.fn(),
  registerForSlot: vi.fn(),
  unregisterFromSlot: vi.fn(),
  offerSwap: vi.fn(),
  checkIn: vi.fn(),
  checkOut: vi.fn(),
  getConfigs: vi.fn(),
  createConfig: vi.fn(),
  generateSlots: vi.fn(),
  cancelSlot: vi.fn(),
  getDashboard: vi.fn(),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      cleaning: {
        title: 'Putz-Orga',
        upcomingSlots: 'Anstehende Termine',
        mySlots: 'Meine Termine',
        noSlots: 'Keine anstehenden Putztermine.',
        noMySlots: 'Sie haben sich für keine Termine eingetragen.',
        register: 'Anmelden',
        unregister: 'Abmelden',
        statusOpen: 'Offen',
        spots: '{n} Plätze',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['modelValue'] },
  TabList: { template: '<div class="tablist-stub"><slot /></div>' },
  Tab: { template: '<div class="tab-stub"><slot /></div>', props: ['value'] },
  TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel-stub"><slot /></div>', props: ['value'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  ProgressBar: { template: '<div class="progress-stub" />', props: ['value'] },
}

function mountCleaning() {
  const pinia = createPinia()
  return mount(CleaningView, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('CleaningView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountCleaning()
    expect(wrapper.find('.page-title-stub').text()).toContain('Putz-Orga')
  })

  it('should render tabs', () => {
    const wrapper = mountCleaning()
    expect(wrapper.find('.tabs-stub').exists()).toBe(true)
  })

  it('should show upcoming and my slots tabs', () => {
    const wrapper = mountCleaning()
    expect(wrapper.text()).toContain('Anstehende Termine')
    expect(wrapper.text()).toContain('Meine Termine')
  })
})
