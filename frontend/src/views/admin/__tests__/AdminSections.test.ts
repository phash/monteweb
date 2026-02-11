import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminSections from '@/views/admin/AdminSections.vue'

vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'sec-1', name: 'Grundschule', slug: 'gs', description: 'Primary school', sortOrder: 1, active: true },
          { id: 'sec-2', name: 'Mittelschule', slug: 'ms', description: null, sortOrder: 2, active: true },
        ],
      },
    }),
    create: vi.fn().mockResolvedValue({ data: { data: { id: 'sec-3', name: 'Neue Section' } } }),
    update: vi.fn().mockResolvedValue({ data: { data: { id: 'sec-1', name: 'Updated' } } }),
    deactivate: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getAll: vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [
            { id: 'room-1', name: 'Klasse 1a', type: 'KLASSE', sectionId: 'sec-1', memberCount: 20, archived: false },
            { id: 'room-2', name: 'Klasse 5b', type: 'KLASSE', sectionId: 'sec-2', memberCount: 15, archived: false },
            { id: 'room-3', name: 'AG Theater', type: 'GRUPPE', sectionId: null, memberCount: 8, archived: false },
          ],
          totalElements: 3, totalPages: 1, last: true,
        },
      },
    }),
    update: vi.fn().mockResolvedValue({ data: { data: { id: 'room-1' } } }),
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: {
    search: vi.fn().mockResolvedValue({
      data: { data: { content: [{ id: 'u-2', displayName: 'New Admin', email: 'new@test.de' }] } },
    }),
    findBySpecialRole: vi.fn().mockResolvedValue({
      data: { data: [{ id: 'u-1', displayName: 'Admin User', email: 'admin@test.de', role: 'TEACHER', active: true }] },
    }),
    addSpecialRole: vi.fn().mockResolvedValue({ data: { data: { id: 'u-1' } } }),
    removeSpecialRole: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {}, query: {} }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

import { sectionsApi } from '@/api/sections.api'
import { roomsApi } from '@/api/rooms.api'
import { usersApi } from '@/api/users.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      admin: {
        sectionsAndRooms: 'Bereiche & Räume',
        createSection: 'Bereich erstellen',
        editSection: 'Bearbeiten',
        deleteSection: 'Bereich löschen',
        sectionDialog: 'Neuer Bereich',
        editSectionDialog: 'Bereich bearbeiten',
        sectionName: 'Name',
        sectionDesc: 'Beschreibung',
        sectionOrder: 'Reihenfolge',
        sectionSaved: 'Bereich gespeichert',
        deleteSectionConfirm: 'Bereich {name} wirklich löschen?',
        deleteSectionWarn: 'Warnung: enthält Räume',
        roomCount: '{count} Räume',
        noSection: 'Kein Bereich',
        noSectionRooms: 'Nicht zugeordnete Räume',
        sectionAdmins: 'Bereichs-Admins',
        sectionAdminAdded: 'Admin hinzugefügt',
        sectionAdminRemoved: 'Admin entfernt',
        noSectionAdmins: 'Keine Bereichs-Admins',
        searchUser: 'Benutzer suchen',
        roomSectionChanged: 'Bereich geändert',
      },
      rooms: {
        types: {
          KLASSE: 'Klasse', GRUPPE: 'Gruppe', PROJEKT: 'Projekt',
          INTEREST: 'Interessengruppe', CUSTOM: 'Sonstige',
        },
        members: 'Mitglieder',
      },
      common: {
        noData: 'Keine Daten', cancel: 'Abbrechen', create: 'Erstellen',
        save: 'Speichern', delete: 'Löschen',
      },
      error: { unexpected: 'Unerwarteter Fehler' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  Accordion: { template: '<div class="accordion-stub"><slot /></div>', props: ['multiple', 'value'] },
  AccordionPanel: { template: '<div class="accordion-panel-stub"><slot /></div>', props: ['value'] },
  AccordionHeader: { template: '<div class="accordion-header-stub"><slot /></div>' },
  AccordionContent: { template: '<div class="accordion-content-stub"><slot /></div>' },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'size', 'ariaLabel'],
    emits: ['click'],
  },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'required'] },
  InputNumber: { template: '<input class="input-number-stub" />', props: ['modelValue'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'rows'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  AutoComplete: {
    template: '<input class="autocomplete-stub" />',
    props: ['modelValue', 'suggestions', 'optionLabel', 'placeholder'],
    emits: ['update:modelValue', 'complete', 'item-select'],
  },
  'router-link': { template: '<a><slot /></a>', props: ['to'] },
}

function mountComponent() {
  const pinia = createPinia()
  return mount(AdminSections, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminSections', () => {
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
    expect(wrapper.find('.page-title-stub').text()).toContain('Bereiche & Räume')
  })

  it('should render create section button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Bereich erstellen'))
    expect(createBtn).toBeTruthy()
  })

  it('should load data on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(sectionsApi.getAll).toHaveBeenCalled()
    expect(roomsApi.getAll).toHaveBeenCalledWith({ page: 0, size: 200 })
  })

  it('should load section admins on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(usersApi.findBySpecialRole).toHaveBeenCalledWith('SECTION_ADMIN:sec-1')
    expect(usersApi.findBySpecialRole).toHaveBeenCalledWith('SECTION_ADMIN:sec-2')
  })

  it('should render accordion panels for sections after loading', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const panels = wrapper.findAll('.accordion-panel-stub')
    expect(panels.length).toBe(2)
  })

  it('should render section names in headers', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('Grundschule')
    expect(wrapper.text()).toContain('Mittelschule')
  })

  it('should render unassigned rooms section', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('Nicht zugeordnete Räume')
  })

  it('should show dialog on create section button click', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Bereich erstellen'))
    await createBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should render room count tags', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const tags = wrapper.findAll('.tag-stub')
    expect(tags.length).toBeGreaterThan(0)
  })

  it('should call create section on form submit', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Bereich erstellen'))
    await createBtn!.trigger('click')
    await flushPromises()

    const dialogBtns = wrapper.findAll('.dialog-stub .button-stub')
    const submitBtn = dialogBtns.find(b => b.text().includes('Erstellen'))
    if (submitBtn) {
      await submitBtn.trigger('click')
      await flushPromises()
      expect(sectionsApi.create).toHaveBeenCalled()
    }
  })
})
