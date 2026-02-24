import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminICalSubscriptions from '@/views/admin/AdminICalSubscriptions.vue'

vi.mock('@/api/calendar.api', () => ({
  calendarApi: {
    getICalSubscriptions: vi.fn().mockResolvedValue({
      data: {
        data: [
          {
            id: 'sub-1',
            name: 'School Holidays',
            url: 'https://example.com/holidays.ics',
            color: '#6366f1',
            lastSyncedAt: '2026-02-24T10:00:00Z',
            active: true,
            createdAt: '2026-02-20T08:00:00Z',
          },
        ],
      },
    }),
    createICalSubscription: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'sub-2',
          name: 'New Sub',
          url: 'https://example.com/new.ics',
          color: '#ff0000',
          lastSyncedAt: null,
          active: true,
          createdAt: '2026-02-24T12:00:00Z',
        },
      },
    }),
    deleteICalSubscription: vi.fn().mockResolvedValue({ data: { data: null } }),
    syncICalSubscription: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))

vi.mock('@/composables/useConfirmDialog', () => ({
  useConfirmDialog: () => ({
    confirm: vi.fn().mockResolvedValue(true),
  }),
}))

import { calendarApi } from '@/api/calendar.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      calendar: {
        ical: {
          title: 'iCal-Abonnements',
          addSubscription: 'Abonnement hinzufügen',
          name: 'Name',
          url: 'URL',
          lastSynced: 'Letzte Synchronisierung',
          sync: 'Jetzt synchronisieren',
          synced: 'Synchronisierung gestartet',
          deleted: 'Abonnement gelöscht',
          created: 'Abonnement erstellt',
          noSubscriptions: 'Keine iCal-Abonnements vorhanden.',
          deleteConfirm: 'Abonnement wirklich löschen?',
          namePlaceholder: 'z.B. Schulferien Bayern',
          urlPlaceholder: 'https://example.com/calendar.ics',
          color: 'Farbe',
        },
      },
      common: {
        save: 'Speichern',
        cancel: 'Abbrechen',
        delete: 'Löschen',
        actions: 'Aktionen',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'loading', 'text', 'rounded', 'size', 'outlined', 'ariaLabel'],
    emits: ['click'],
  },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  DataTable: { template: '<div class="datatable-stub"><slot /></div>', props: ['value', 'stripedRows'] },
  Column: { template: '<div class="column-stub"><slot /></div>', props: ['field', 'header', 'style'] },
  ColorPicker: {
    template: '<input type="color" class="colorpicker-stub" />',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  InputText: {
    template: '<input class="input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
}

function mountComponent() {
  const pinia = createPinia()
  return mount(AdminICalSubscriptions, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminICalSubscriptions', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title', () => {
    const wrapper = mountComponent()
    const title = wrapper.find('.page-title-stub')
    expect(title.exists()).toBe(true)
    expect(title.text()).toContain('iCal-Abonnements')
  })

  it('should fetch subscriptions on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(calendarApi.getICalSubscriptions).toHaveBeenCalled()
  })

  it('should render add subscription button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const addBtn = buttons.find(b => b.text().includes('Abonnement hinzufügen'))
    expect(addBtn).toBeDefined()
  })

  it('should show datatable after loading subscriptions', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('.datatable-stub').exists()).toBe(true)
  })

  it('should show empty state when no subscriptions', async () => {
    vi.mocked(calendarApi.getICalSubscriptions).mockResolvedValueOnce({
      data: { data: [] },
    } as any)
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('.empty-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('Keine iCal-Abonnements vorhanden.')
  })
})
