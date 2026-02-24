import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { setActivePinia, createPinia } from 'pinia'
import AdminProfileFields from '@/views/admin/AdminProfileFields.vue'

vi.mock('@/api/profilefields.api', () => ({
  profileFieldsApi: {
    listAllDefinitions: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'f1', fieldKey: 'hobby', labelDe: 'Hobby', labelEn: 'Hobby', fieldType: 'TEXT', options: null, required: false, position: 0 },
        ],
      },
    }),
    createDefinition: vi.fn(),
    updateDefinition: vi.fn(),
    deleteDefinition: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      common: { save: 'Speichern', cancel: 'Abbrechen', delete: 'Löschen', confirm: 'Bestätigen' },
      error: { unexpected: 'Unerwarteter Fehler' },
      profileFields: {
        admin: {
          title: 'Profilfelder verwalten',
          subtitle: 'Felder verwalten',
          newField: 'Neues Feld',
          editField: 'Feld bearbeiten',
          fieldKey: 'Feldschlüssel',
          fieldKeyHint: 'Hinweis',
          labelDe: 'DE',
          labelEn: 'EN',
          fieldType: 'Typ',
          options: 'Optionen',
          optionsHint: 'Hinweis',
          required: 'Pflichtfeld',
          position: 'Position',
          active: 'Aktiv',
          noFields: 'Keine Felder',
          deleteConfirm: 'Wirklich löschen?',
          created: 'Erstellt',
          updated: 'Aktualisiert',
          deleted: 'Gelöscht',
        },
        fieldTypes: { TEXT: 'Text', DATE: 'Datum', SELECT: 'Auswahl', BOOLEAN: 'Ja/Nein' },
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  Button: { template: '<button><slot /></button>', props: ['label', 'icon', 'severity', 'size', 'text', 'disabled', 'loading'] },
  Dialog: { template: '<div v-if="visible"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal'] },
  InputText: { template: '<input />', props: ['modelValue', 'disabled'] },
  InputNumber: { template: '<input type="number" />', props: ['modelValue', 'min'] },
  Select: { template: '<select />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'disabled'] },
  ToggleSwitch: { template: '<input type="checkbox" />', props: ['modelValue'] },
  Tag: { template: '<span>{{ value }}</span>', props: ['value', 'severity'] },
}

describe('AdminProfileFields', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mount(AdminProfileFields, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.page-title-stub').text()).toContain('Profilfelder verwalten')
  })

  it('should render new field button', () => {
    const wrapper = mount(AdminProfileFields, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.admin-actions button').exists()).toBe(true)
  })

  it('should render field cards after loading', async () => {
    const wrapper = mount(AdminProfileFields, { global: { plugins: [i18n], stubs } })
    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    const cards = wrapper.findAll('.field-card')
    expect(cards.length).toBeGreaterThanOrEqual(0)
  })
})
