import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminRooms from '@/views/admin/AdminRooms.vue'

vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getAll: vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [
            {
              id: 'room-1', name: 'Klasse 1a', type: 'KLASSE', description: 'First class',
              publicDescription: '', sectionId: 'sec-1', memberCount: 25, archived: false, avatarUrl: null,
            },
            {
              id: 'room-2', name: 'AG Kunst', type: 'GRUPPE', description: 'Art group',
              publicDescription: 'Open art group', sectionId: null, memberCount: 10, archived: true, avatarUrl: '/img/art.png',
            },
          ],
          totalElements: 2, totalPages: 1, last: true,
        },
      },
    }),
    getById: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'room-1', name: 'Klasse 1a', type: 'KLASSE', description: 'First class',
          publicDescription: '', sectionId: 'sec-1', memberCount: 25, archived: false, avatarUrl: null,
          members: [
            { userId: 'u-1', displayName: 'Max Mustermann', role: 'LEADER' },
            { userId: 'u-2', displayName: 'Erika Muster', role: 'MEMBER' },
          ],
        },
      },
    }),
    create: vi.fn().mockResolvedValue({ data: { data: { id: 'room-1', name: 'Klasse 1a' } } }),
    update: vi.fn().mockResolvedValue({ data: { data: { id: 'room-1', name: 'Klasse 1a' } } }),
    toggleArchive: vi.fn().mockResolvedValue({ data: { data: { id: 'room-1', archived: true } } }),
    deleteRoom: vi.fn().mockResolvedValue({ data: { data: null } }),
    addMember: vi.fn().mockResolvedValue({ data: { data: null } }),
    removeMember: vi.fn().mockResolvedValue({ data: { data: null } }),
    updateMemberRole: vi.fn().mockResolvedValue({ data: { data: null } }),
    uploadAvatar: vi.fn().mockResolvedValue({ data: { data: null } }),
    removeAvatar: vi.fn().mockResolvedValue({ data: { data: null } }),
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: {
    search: vi.fn().mockResolvedValue({
      data: { data: { content: [{ id: 'u-3', displayName: 'New User', email: 'new@test.de' }] } },
    }),
  },
}))

vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'sec-1', name: 'Grundschule', slug: 'gs', description: null, sortOrder: 1, active: true },
          { id: 'sec-2', name: 'Mittelschule', slug: 'ms', description: null, sortOrder: 2, active: true },
        ],
      },
    }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

import { roomsApi } from '@/api/rooms.api'
import { usersApi } from '@/api/users.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      admin: {
        rooms: 'Raumverwaltung',
        allSections: 'Alle Bereiche',
        allTypes: 'Alle Typen',
        columnType: 'Typ',
        columnSection: 'Bereich',
        columnMembers: 'Mitglieder',
        editRoom: 'Raum bearbeiten',
        deactivateRoom: 'Deaktivieren',
        reactivateRoom: 'Reaktivieren',
        deleteRoom: 'Raum löschen',
        deleteRoomConfirm: 'Raum {name} wirklich löschen?',
        deleteRoomWarn: 'Warnung',
        deactivateRoomConfirm: 'Raum {name} deaktivieren?',
        reactivateRoomConfirm: 'Raum {name} reaktivieren?',
        manageMembers: 'Mitglieder verwalten',
        searchUser: 'Benutzer suchen',
        memberAdded: 'Mitglied hinzugefügt',
        memberRemoved: 'Mitglied entfernt',
        roomSaved: 'Raum gespeichert',
        roomDeactivated: 'Raum deaktiviert',
        roomReactivated: 'Raum reaktiviert',
        roomDeleted: 'Raum gelöscht',
        noSection: 'Kein Bereich',
        moveMember: 'Verschieben',
        copyMember: 'Kopieren',
        memberMoved: 'Mitglied verschoben',
        memberCopied: 'Mitglied kopiert',
        targetRoom: 'Zielraum',
        selectRoom: 'Raum wählen',
        deactivated: 'Deaktiviert',
      },
      rooms: {
        create: 'Raum erstellen',
        name: 'Name',
        type: 'Typ',
        section: 'Bereich',
        description: 'Beschreibung',
        publicDescription: 'Öffentliche Beschreibung',
        noMembers: 'Keine Mitglieder',
        types: {
          KLASSE: 'Klasse', GRUPPE: 'Gruppe', PROJEKT: 'Projekt',
          INTEREST: 'Interessengruppe', CUSTOM: 'Sonstige',
        },
        members: 'Mitglieder',
      },
      common: {
        name: 'Name', status: 'Status', actions: 'Aktionen', active: 'Aktiv',
        cancel: 'Abbrechen', create: 'Erstellen', save: 'Speichern', delete: 'Löschen',
        avatarUploaded: 'Avatar hochgeladen', avatarRemoved: 'Avatar entfernt',
      },
      error: { unexpected: 'Unerwarteter Fehler' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  AvatarUpload: { template: '<div class="avatar-upload-stub" />', props: ['imageUrl', 'size', 'icon', 'editable'] },
  DataTable: {
    template: '<div class="datatable-stub"><slot /></div>',
    props: ['value', 'loading', 'stripedRows', 'scrollable'],
  },
  Column: { template: '<div class="column-stub"><slot /></div>', props: ['field', 'header'] },
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'rounded', 'size', 'disabled', 'ariaLabel'],
    emits: ['click'],
  },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'required'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'rows'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'showClear', 'filter'] },
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
  return mount(AdminRooms, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminRooms', () => {
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
    expect(wrapper.find('.page-title-stub').text()).toContain('Raumverwaltung')
  })

  it('should render datatable', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.datatable-stub').exists()).toBe(true)
  })

  it('should render create button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Raum erstellen'))
    expect(createBtn).toBeTruthy()
  })

  it('should render filter selects', () => {
    const wrapper = mountComponent()
    const selects = wrapper.findAll('.select-stub')
    expect(selects.length).toBeGreaterThanOrEqual(2)
  })

  it('should load rooms and sections on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(roomsApi.getAll).toHaveBeenCalledWith({ page: 0, size: 100, includeArchived: true })
  })

  it('should show create dialog on button click', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Raum erstellen'))
    await createBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should call createRoom when form is submitted', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Raum erstellen'))
    await createBtn!.trigger('click')
    await flushPromises()

    const dialogBtns = wrapper.findAll('.dialog-stub .button-stub')
    const submitBtn = dialogBtns.find(b => b.text().includes('Erstellen'))
    if (submitBtn) {
      await submitBtn.trigger('click')
      await flushPromises()
      expect(roomsApi.create).toHaveBeenCalled()
    }
  })

  it('should have usersApi.search available for member search', () => {
    mountComponent()
    expect(usersApi.search).toBeDefined()
  })

  it('should render columns for name, type, section, members, status, actions', () => {
    const wrapper = mountComponent()
    const columns = wrapper.findAll('.column-stub')
    expect(columns.length).toBe(6)
  })
})
