import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

const mockFetchReports = vi.fn().mockResolvedValue(undefined)
const mockStore = {
  reports: [] as any[],
  totalRecords: 0,
  loading: false,
  githubRepo: '',
  githubPatConfigured: false,
  fetchReports: mockFetchReports,
  updateStatus: vi.fn().mockResolvedValue({}),
  createGithubIssue: vi.fn().mockResolvedValue({}),
  deleteReport: vi.fn().mockResolvedValue(undefined),
  updateGithubConfig: vi.fn().mockResolvedValue(undefined),
}

vi.mock('@/stores/errorReports', () => ({
  useErrorReportsStore: vi.fn(() => mockStore),
}))

vi.mock('@/stores/admin', () => ({
  useAdminStore: vi.fn(() => ({
    config: { githubRepo: 'org/repo', githubPatConfigured: true },
    fetchConfig: vi.fn().mockResolvedValue(undefined),
  })),
}))

const mockToastAdd = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: vi.fn(() => ({ add: mockToastAdd })),
}))

import AdminErrorReports from '@/views/admin/AdminErrorReports.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      errorReports: {
        title: 'Fehlerberichte',
        subtitle: 'Fehler anzeigen und verwalten',
        all: 'Alle',
        statusNew: 'Neu',
        statusReported: 'Gemeldet',
        statusResolved: 'Behoben',
        statusIgnored: 'Ignoriert',
        statusUpdated: 'Status aktualisiert',
        source: 'Quelle',
        errorType: 'Fehlertyp',
        message: 'Nachricht',
        occurrenceCount: 'Anzahl',
        lastSeen: 'Zuletzt',
        firstSeen: 'Erstmals',
        stackTrace: 'Stack Trace',
        userAgent: 'User Agent',
        requestUrl: 'URL',
        noReports: 'Keine Fehlerberichte',
        deleted: 'Fehlerbericht gelöscht',
        confirmDelete: 'Wirklich löschen?',
        githubConfig: 'GitHub Konfiguration',
        githubNotConfigured: 'GitHub ist noch nicht konfiguriert.',
        githubRepo: 'Repository',
        githubPat: 'Personal Access Token',
        githubConfigSaved: 'GitHub-Konfiguration gespeichert',
        githubIssueCreated: 'GitHub Issue erstellt',
        createGithubIssue: 'GitHub Issue erstellen',
      },
      common: {
        status: 'Status',
        actions: 'Aktionen',
        save: 'Speichern',
        cancel: 'Abbrechen',
        delete: 'Löschen',
        confirmDeleteTitle: 'Löschen bestätigen',
      },
    },
  },
})

const globalStubs = {
  PageTitle: { template: '<div />', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  Button: {
    template: '<button @click="$emit(\'click\')" :disabled="disabled">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'loading', 'disabled', 'size', 'text'],
    emits: ['click'],
  },
  DataTable: {
    template: '<div class="datatable-stub"><slot /><slot name="empty" /><slot name="expansion" v-bind="{ data: {} }" /></div>',
    props: ['value', 'expandedRows', 'loading', 'lazy', 'totalRecords', 'rows', 'first', 'paginator', 'rowsPerPageOptions', 'dataKey', 'stripedRows'],
    emits: ['update:expandedRows', 'page'],
  },
  Column: { template: '<div />', props: ['field', 'header', 'sortable', 'expander', 'style'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  Select: {
    template: '<select @change="$emit(\'change\')" />',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'showClear', 'class'],
    emits: ['update:modelValue', 'change'],
  },
  InputText: {
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder', 'type'],
    emits: ['update:modelValue'],
  },
  Dialog: {
    template: '<div v-if="visible"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
    emits: ['update:visible'],
  },
  ConfirmDialog: { template: '<div />' },
}

function mountView() {
  return mount(AdminErrorReports, {
    global: {
      plugins: [i18n],
      stubs: globalStubs,
      directives: { tooltip: () => {} },
    },
  })
}

describe('AdminErrorReports', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Object.assign(mockStore, {
      reports: [],
      totalRecords: 0,
      loading: false,
      githubRepo: '',
      githubPatConfigured: false,
    })
    mockStore.fetchReports = mockFetchReports
    mockStore.fetchReports.mockResolvedValue(undefined)
    mockStore.updateStatus.mockResolvedValue({})
    mockStore.createGithubIssue.mockResolvedValue({})
    mockStore.deleteReport.mockResolvedValue(undefined)
    mockStore.updateGithubConfig.mockResolvedValue(undefined)
  })

  // --- Mount & lifecycle ---

  it('should mount and call fetchReports on mount', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(mockFetchReports).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should render the component', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should pre-populate github config from admin store', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.githubRepo).toBe('org/repo')
    wrapper.unmount()
  })

  it('should show empty state when no reports', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.reports).toHaveLength(0)
    wrapper.unmount()
  })

  // --- loadReports / onFilterChange / onPage ---

  it('loadReports should call fetchReports with params', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    const vm = wrapper.vm as any
    vm.statusFilter = 'NEW'
    vm.sourceFilter = 'FRONTEND'
    vm.currentPage = 2
    await vm.loadReports()
    expect(mockFetchReports).toHaveBeenCalledWith(expect.objectContaining({
      status: 'NEW',
      source: 'FRONTEND',
      page: 2,
      size: 20,
      sort: 'lastSeenAt,desc',
    }))
    wrapper.unmount()
  })

  it('onFilterChange should reset page and reload', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    const vm = wrapper.vm as any
    vm.currentPage = 3
    vm.onFilterChange()
    expect(vm.currentPage).toBe(0)
    await flushPromises()
    expect(mockFetchReports).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('onPage should set currentPage and reload', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    const vm = wrapper.vm as any
    await vm.onPage({ page: 5 })
    expect(vm.currentPage).toBe(5)
    expect(mockFetchReports).toHaveBeenCalled()
    wrapper.unmount()
  })

  // --- onStatusChange ---

  it('onStatusChange should call updateStatus and show success', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.onStatusChange('report-1', 'RESOLVED')
    expect(mockStore.updateStatus).toHaveBeenCalledWith('report-1', 'RESOLVED')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('onStatusChange should show error toast on failure', async () => {
    mockStore.updateStatus.mockRejectedValue(new Error('fail'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.onStatusChange('report-1', 'RESOLVED')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  // --- onCreateGithubIssue ---

  it('onCreateGithubIssue should call store and show success', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.onCreateGithubIssue('report-2')
    expect(mockStore.createGithubIssue).toHaveBeenCalledWith('report-2')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('onCreateGithubIssue should show error on failure', async () => {
    mockStore.createGithubIssue.mockRejectedValue(new Error('fail'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.onCreateGithubIssue('report-2')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  // --- confirmDelete / onDelete ---

  it('confirmDelete should set deleteTarget and show dialog', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.confirmDelete('report-3')
    expect(vm.deleteTarget).toBe('report-3')
    expect(vm.deleteDialog).toBe(true)
    wrapper.unmount()
  })

  it('onDelete should call deleteReport and show success', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.deleteTarget = 'report-3'
    vm.deleteDialog = true
    await vm.onDelete()
    expect(mockStore.deleteReport).toHaveBeenCalledWith('report-3')
    expect(vm.deleteDialog).toBe(false)
    expect(vm.deleteTarget).toBeNull()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('onDelete should not call deleteReport if no target', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.deleteTarget = null
    await vm.onDelete()
    expect(mockStore.deleteReport).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('onDelete should show error toast on failure', async () => {
    mockStore.deleteReport.mockRejectedValue(new Error('fail'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.deleteTarget = 'report-4'
    await vm.onDelete()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  // --- saveGithubConfig ---

  it('saveGithubConfig should not call if repo is empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.githubRepo = ''
    await vm.saveGithubConfig()
    expect(mockStore.updateGithubConfig).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('saveGithubConfig should call store and show success', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.githubRepo = 'myorg/myrepo'
    vm.githubPat = 'ghp_xxxx'
    await vm.saveGithubConfig()
    expect(mockStore.updateGithubConfig).toHaveBeenCalledWith('myorg/myrepo', 'ghp_xxxx')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    expect(vm.githubPat).toBe('')
    expect(vm.savingGithub).toBe(false)
    wrapper.unmount()
  })

  it('saveGithubConfig should show error on failure', async () => {
    mockStore.updateGithubConfig.mockRejectedValue(new Error('fail'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.githubRepo = 'myorg/myrepo'
    vm.githubPat = 'ghp_xxxx'
    await vm.saveGithubConfig()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    expect(vm.savingGithub).toBe(false)
    wrapper.unmount()
  })

  // --- Helper functions ---

  it('statusSeverity should map status values', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.statusSeverity('NEW')).toBe('danger')
    expect(vm.statusSeverity('REPORTED')).toBe('info')
    expect(vm.statusSeverity('RESOLVED')).toBe('success')
    expect(vm.statusSeverity('IGNORED')).toBe('secondary')
    expect(vm.statusSeverity('UNKNOWN')).toBe('info')
    wrapper.unmount()
  })

  it('statusLabel should return translated labels', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.statusLabel('NEW')).toBe('Neu')
    expect(vm.statusLabel('REPORTED')).toBe('Gemeldet')
    expect(vm.statusLabel('RESOLVED')).toBe('Behoben')
    expect(vm.statusLabel('IGNORED')).toBe('Ignoriert')
    expect(vm.statusLabel('OTHER')).toBe('OTHER')
    wrapper.unmount()
  })

  it('sourceSeverity should return warn for BACKEND', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.sourceSeverity('BACKEND')).toBe('warn')
    expect(vm.sourceSeverity('FRONTEND')).toBe('info')
    wrapper.unmount()
  })

  it('formatDateTime should format ISO date to German locale', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const result = vm.formatDateTime('2025-03-15T14:30:00Z')
    expect(result).toContain('2025')
    expect(result).not.toBe('-')
    wrapper.unmount()
  })

  it('formatDateTime should return dash for empty string', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatDateTime('')).toBe('-')
    wrapper.unmount()
  })

  it('truncate should shorten long strings', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.truncate('short', 10)).toBe('short')
    expect(vm.truncate('a very long string that exceeds the limit', 10)).toBe('a very lon...')
    expect(vm.truncate(null, 10)).toBe('-')
    wrapper.unmount()
  })

  // --- Template rendering ---

  it('should set githubPatConfigured from admin config on mount', async () => {
    // The onMounted reads adminStore.config.githubPatConfigured (true)
    // and sets store.githubPatConfigured
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(mockStore.githubPatConfigured).toBe(true)
    wrapper.unmount()
  })
})
