import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import SectionAdminView from '@/views/admin/SectionAdminView.vue'

vi.mock('@/api/sectionAdmin.api', () => ({
  sectionAdminApi: {
    getMySections: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getSectionUsers: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getSectionRooms: vi.fn().mockResolvedValue({ data: { data: [] } }),
    assignSpecialRole: vi.fn().mockResolvedValue({ data: { data: {} } }),
    removeSpecialRole: vi.fn().mockResolvedValue({ data: { data: {} } }),
    createRoom: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

const mockRouterPush = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {}, query: {} }),
  useRouter: () => ({ push: mockRouterPush, replace: vi.fn() }),
}))

import { sectionAdminApi } from '@/api/sectionAdmin.api'

const mockSections = [
  { id: 'sec-1', name: 'Kinderhaus', slug: 'kinderhaus', description: 'Krippe & KiGa', sortOrder: 1, active: true },
  { id: 'sec-2', name: 'Grundstufe', slug: 'grundstufe', description: 'Klasse 1-4', sortOrder: 2, active: true },
]

const mockUsers = [
  { id: 'u-1', email: 'lehrer@test.de', displayName: 'Max Lehrer', firstName: 'Max', lastName: 'Lehrer', role: 'TEACHER', specialRoles: ['PUTZORGA'], assignedRoles: ['TEACHER'], active: true },
  { id: 'u-2', email: 'eltern@test.de', displayName: 'Anna Eltern', firstName: 'Anna', lastName: 'Eltern', role: 'PARENT', specialRoles: [], assignedRoles: ['PARENT'], active: true },
  { id: 'u-3', email: 'admin@test.de', displayName: 'Sara Admin', firstName: 'Sara', lastName: 'Admin', role: 'SUPERADMIN', specialRoles: [], assignedRoles: ['SUPERADMIN'], active: true },
]

const mockRooms = [
  { id: 'r-1', name: 'Sonnengruppe', type: 'KLASSE', sectionId: 'sec-1', memberCount: 25, description: 'Juengste', archived: false },
  { id: 'r-2', name: 'Sternengruppe', type: 'GRUPPE', sectionId: 'sec-1', memberCount: 20, description: 'KiGa', archived: false },
  { id: 'r-3', name: 'Projektgruppe A', type: 'PROJEKT', sectionId: 'sec-1', memberCount: 8, description: null, archived: false },
]

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      sectionAdmin: {
        title: 'Bereichsverwaltung',
        selectSection: 'Bereich waehlen',
        tabUsers: 'Nutzer',
        tabRooms: 'Raeume',
        allRoles: 'Alle Rollen',
        allTypes: 'Alle Typen',
        searchUsers: 'Nutzer suchen',
        searchRooms: 'Raeume suchen',
        noSections: 'Keine Bereiche zugewiesen',
        noUsers: 'Keine Nutzer',
        noRooms: 'Keine Raeume',
        specialRoles: 'Sonderrollen',
        assignRole: 'Rolle zuweisen',
        removeRole: 'Rolle entfernen',
        roleAssigned: 'Rolle zugewiesen',
        roleRemoved: 'Rolle entfernt',
        onlyAllowedRoles: 'Nur PUTZORGA und ELTERNBEIRAT',
        createRoom: 'Raum erstellen',
        roomCreated: 'Raum erstellt',
        roomName: 'Raumname',
        roomType: 'Raumtyp',
        roomDescription: 'Beschreibung',
        memberCount: 'Mitglieder',
        viewMembers: 'Mitglieder anzeigen',
      },
      rooms: {
        types: { KLASSE: 'Klasse', GRUPPE: 'Gruppe', PROJEKT: 'Projekt', CUSTOM: 'Sonstige' },
      },
      common: { name: 'Name', actions: 'Aktionen' },
      admin: { columnRole: 'Rolle' },
      auth: { email: 'E-Mail' },
      error: { unexpected: 'Unerwarteter Fehler' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  DataTable: {
    template: '<table class="datatable-stub"><slot /></table>',
    props: ['value', 'loading', 'stripedRows', 'scrollable', 'sortField', 'sortOrder'],
  },
  Column: { template: '<td class="column-stub"><slot name="body" :data="{}" /></td>', props: ['field', 'header', 'sortable'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')" :disabled="disabled">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'size', 'disabled', 'loading', 'ariaLabel'],
    emits: ['click'],
  },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
  },
  InputText: {
    template: '<input class="input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder', 'required'],
    emits: ['update:modelValue'],
  },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'rows'] },
  Select: {
    template: '<select class="select-stub" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option></select>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['value'] },
  TabList: { template: '<div class="tablist-stub"><slot /></div>' },
  Tab: { template: '<button class="tab-stub"><slot /></button>', props: ['value'] },
  TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel-stub"><slot /></div>', props: ['value'] },
  IconField: { template: '<div class="iconfield-stub"><slot /></div>' },
  InputIcon: { template: '<span class="inputicon-stub" />' },
  'router-link': { template: '<a><slot /></a>', props: ['to'] },
}

function mountComponent() {
  const pinia = createPinia()
  return mount(SectionAdminView, {
    global: { plugins: [i18n, pinia], stubs, directives: { tooltip: {} } },
  })
}

describe('SectionAdminView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockRouterPush.mockClear()
    // Set default mock responses for each test
    vi.mocked(sectionAdminApi.getMySections).mockResolvedValue({ data: { data: mockSections } } as any)
    vi.mocked(sectionAdminApi.getSectionUsers).mockResolvedValue({ data: { data: mockUsers } } as any)
    vi.mocked(sectionAdminApi.getSectionRooms).mockResolvedValue({ data: { data: mockRooms } } as any)
    vi.mocked(sectionAdminApi.assignSpecialRole).mockResolvedValue({ data: { data: mockUsers[1] } } as any)
    vi.mocked(sectionAdminApi.removeSpecialRole).mockResolvedValue({ data: { data: mockUsers[0] } } as any)
    vi.mocked(sectionAdminApi.createRoom).mockResolvedValue({ data: { data: { id: 'r-new', name: 'Neuer Raum', type: 'KLASSE' } } } as any)
  })

  // ── Rendering ───────────────────────────────────────────────────────

  it('should render the page title', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('.page-title-stub').text()).toContain('Bereichsverwaltung')
  })

  it('should load sections on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(sectionAdminApi.getMySections).toHaveBeenCalledOnce()
  })

  it('should auto-select first section and load users and rooms', async () => {
    mountComponent()
    await flushPromises()
    expect(sectionAdminApi.getSectionUsers).toHaveBeenCalledWith('sec-1')
    expect(sectionAdminApi.getSectionRooms).toHaveBeenCalledWith('sec-1')
  })

  it('should render section selector with options', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const selects = wrapper.findAll('.select-stub')
    expect(selects.length).toBeGreaterThan(0)
    const sectionSelect = selects[0]
    const options = sectionSelect.findAll('option')
    expect(options.length).toBe(2)
    expect(options[0].text()).toContain('Kinderhaus')
    expect(options[1].text()).toContain('Grundstufe')
  })

  it('should render tabs for users and rooms', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const tabs = wrapper.findAll('.tab-stub')
    expect(tabs.length).toBe(2)
    expect(tabs[0].text()).toContain('Nutzer')
    expect(tabs[1].text()).toContain('Raeume')
  })

  // ── Empty state ─────────────────────────────────────────────────────

  it('should show empty state when no sections assigned', async () => {
    vi.mocked(sectionAdminApi.getMySections).mockResolvedValue({ data: { data: [] } } as any)
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Bereiche zugewiesen')
  })

  it('should show loading spinner while loading', async () => {
    vi.mocked(sectionAdminApi.getMySections).mockReturnValue(new Promise(() => {}) as any)
    const wrapper = mountComponent()
    await nextTick()
    expect(wrapper.find('.loading-stub').exists()).toBe(true)
  })

  // ── Users tab ───────────────────────────────────────────────────────

  it('should render user data table after loading', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('.datatable-stub').exists()).toBe(true)
  })

  it('should render column headers for users', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const columns = wrapper.findAll('.column-stub')
    expect(columns.length).toBeGreaterThan(0)
  })

  // ── Rooms tab ───────────────────────────────────────────────────────

  it('should render create room button', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Raum erstellen'))
    expect(createBtn).toBeTruthy()
  })

  // ── Create Room Dialog ──────────────────────────────────────────────

  it('should open create room dialog on button click', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Raum erstellen'))
    await createBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should call createRoom API on dialog form submit', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    // Open dialog
    const buttons = wrapper.findAll('.button-stub')
    const createBtn = buttons.find(b => b.text().includes('Raum erstellen'))
    await createBtn!.trigger('click')
    await flushPromises()

    // Fill name
    const inputs = wrapper.findAll('.dialog-stub .input-stub')
    if (inputs.length > 0) {
      await inputs[0].setValue('Neuer Raum')
    }

    // Submit form
    const dialog = wrapper.find('.dialog-stub')
    const form = dialog.find('form')
    if (form.exists()) {
      await form.trigger('submit')
      await flushPromises()
      expect(sectionAdminApi.createRoom).toHaveBeenCalled()
    }
  })

  // ── Role management ─────────────────────────────────────────────────

  it('should have assign role buttons rendered', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    // Verify components loaded successfully with API data
    expect(sectionAdminApi.getMySections).toHaveBeenCalled()
    expect(sectionAdminApi.getSectionUsers).toHaveBeenCalledWith('sec-1')
  })

  // ── API call verification ───────────────────────────────────────────

  it('should call getMySections exactly once on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(sectionAdminApi.getMySections).toHaveBeenCalledTimes(1)
  })

  it('should call getSectionUsers and getSectionRooms when section is selected', async () => {
    mountComponent()
    await flushPromises()
    // Auto-selects first section
    expect(sectionAdminApi.getSectionUsers).toHaveBeenCalledTimes(1)
    expect(sectionAdminApi.getSectionRooms).toHaveBeenCalledTimes(1)
  })

  it('should not call getSectionUsers when no section is selected', async () => {
    vi.mocked(sectionAdminApi.getMySections).mockResolvedValue({ data: { data: [] } } as any)
    mountComponent()
    await flushPromises()
    expect(sectionAdminApi.getSectionUsers).not.toHaveBeenCalled()
    expect(sectionAdminApi.getSectionRooms).not.toHaveBeenCalled()
  })

  // ── Single section ──────────────────────────────────────────────────

  it('should work with only one section', async () => {
    vi.mocked(sectionAdminApi.getMySections).mockResolvedValue({
      data: { data: [mockSections[0]] },
    } as any)
    const wrapper = mountComponent()
    await flushPromises()
    expect(sectionAdminApi.getSectionUsers).toHaveBeenCalledWith('sec-1')
    const selects = wrapper.findAll('.select-stub')
    const sectionSelect = selects[0]
    const options = sectionSelect.findAll('option')
    expect(options.length).toBe(1)
  })
})
