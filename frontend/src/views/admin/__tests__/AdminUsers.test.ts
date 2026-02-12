import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminUsers from '@/views/admin/AdminUsers.vue'

vi.mock('@/api/users.api', () => ({
  usersApi: {
    list: vi.fn().mockResolvedValue({ data: { data: { content: [], last: true, totalElements: 0 } } }),
    updateRole: vi.fn(),
    setActive: vi.fn(),
    adminUpdateProfile: vi.fn(),
    getUserRooms: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getUserFamilies: vi.fn().mockResolvedValue({ data: { data: [] } }),
    addUserToFamily: vi.fn(),
    removeUserFromFamily: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))
vi.mock('@/api/rooms.api', () => ({
  roomsApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    discover: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    getAll: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    addMember: vi.fn(),
    removeMember: vi.fn(),
  },
}))
vi.mock('@/api/family.api', () => ({
  familyApi: {
    getAll: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      admin: {
        users: 'Benutzer',
        usersTitle: 'Benutzerverwaltung',
        searchUsers: 'Benutzer suchen...',
        role: 'Rolle',
        email: 'E-Mail',
        name: 'Name',
        actions: 'Aktionen',
        editUser: 'Bearbeiten',
        saveRoles: 'Speichern',
        columnRole: 'Rolle',
        tabProfile: 'Profil',
        tabRooms: 'Räume',
        tabFamily: 'Familie',
        searchRoom: 'Raum suchen...',
        searchUser: 'Benutzer suchen...',
        addMember: 'Hinzufügen',
        selectFamily: 'Familie auswählen',
        addToFamily: 'Hinzufügen',
        noRoomMemberships: 'Keine Raum-Mitgliedschaften',
        noFamilyMemberships: 'Keine Familien-Mitgliedschaften',
      },
      auth: { email: 'E-Mail' },
      common: { loading: 'Laden...', save: 'Speichern', cancel: 'Abbrechen', name: 'Name', status: 'Status', actions: 'Aktionen', edit: 'Bearbeiten', delete: 'Löschen' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: { template: '<button class="button-stub">{{ label }}</button>', props: ['label', 'icon', 'text', 'severity', 'size'] },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'placeholder'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  Dialog: { template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>', props: ['visible', 'header', 'modal'] },
  Select: { template: '<select class="select-stub" />', props: ['modelValue', 'options'] },
  DataTable: { template: '<div class="datatable-stub"><slot /></div>', props: ['value', 'loading', 'paginator', 'rows', 'totalRecords', 'lazy'] },
  Column: { template: '<div class="column-stub"><slot /></div>', props: ['field', 'header', 'sortable'] },
  Checkbox: { template: '<input type="checkbox" class="checkbox-stub" />', props: ['modelValue', 'binary'] },
  Textarea: { template: '<textarea class="textarea-stub" />', props: ['modelValue', 'rows'] },
  TabView: { template: '<div class="tabview-stub"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel-stub"><slot /></div>', props: ['header'] },
  Tabs: { template: '<div class="tabs-stub"><slot /></div>', props: ['modelValue'] },
  TabList: { template: '<div class="tablist-stub"><slot /></div>' },
  Tab: { template: '<div class="tab-stub"><slot /></div>', props: ['value'] },
  TabPanels: { template: '<div class="tabpanels-stub"><slot /></div>' },
}

function mountAdminUsers() {
  const pinia = createPinia()
  return mount(AdminUsers, {
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('AdminUsers', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render component', () => {
    const wrapper = mountAdminUsers()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render page title', () => {
    const wrapper = mountAdminUsers()
    expect(wrapper.find('.page-title-stub').exists()).toBe(true)
  })

  it('should render datatable', () => {
    const wrapper = mountAdminUsers()
    expect(wrapper.find('.datatable-stub').exists()).toBe(true)
  })
})
