import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock store
const mockStore = {
  items: [] as any[],
  loading: false,
  fetchItems: vi.fn().mockResolvedValue(undefined),
  createItem: vi.fn(),
  updateItem: vi.fn(),
  deleteItem: vi.fn().mockResolvedValue(undefined),
  claimItem: vi.fn(),
  uploadImages: vi.fn().mockResolvedValue(undefined),
}

vi.mock('@/stores/fundgrube', () => ({
  useFundgrubeStore: vi.fn(() => mockStore),
}))

// --- mock auth store
const mockAuth = {
  isAdmin: false,
  isSectionAdmin: false,
  user: { id: 'user-1', specialRoles: [] as string[] },
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => mockAuth),
}))

// --- mock sections API
vi.mock('@/api/sections.api', () => ({
  sectionsApi: {
    getAll: vi.fn().mockResolvedValue({
      data: { data: [{ id: 'sec-1', name: 'Grundstufe' }] },
    }),
  },
}))

// --- mock fundgrube API
vi.mock('@/api/fundgrube.api', () => ({
  fundgrubeApi: {
    thumbnailUrl: vi.fn((id: string) => `/api/v1/fundgrube/thumbnails/${id}`),
    imageUrl: vi.fn((id: string) => `/api/v1/fundgrube/images/${id}`),
  },
}))

const mockToastAdd = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: vi.fn(() => ({ add: mockToastAdd })),
}))

const mockConfirmRequire = vi.fn()
vi.mock('primevue/useconfirm', () => ({
  useConfirm: vi.fn(() => ({ require: mockConfirmRequire })),
}))

import FundgrubeView from '@/views/FundgrubeView.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      fundgrube: {
        title: 'Fundgrube',
        subtitle: 'Verlorene Gegenstände',
        allSections: 'Alle Bereiche',
        newItem: 'Gegenstand melden',
        editItem: 'Gegenstand bearbeiten',
        itemTitle: 'Bezeichnung',
        section: 'Bereich',
        noSection: 'Kein Bereich',
        images: 'Bilder',
        chooseImages: 'Bilder auswählen',
        filesSelected: 'Datei(en) ausgewählt',
        empty: 'Keine Fundstücke vorhanden',
        postedBy: 'Gemeldet von',
        claimedBy: 'Abgeholt von',
        expiresAt: 'Wird entfernt am',
        claimed: 'Abgeholt',
        claimButton: 'Das gehört mir!',
        claimTitle: 'Beanspruchen',
        claimDescription: 'Bitte bestätige...',
        claimComment: 'Kommentar',
        claimCommentPlaceholder: 'z.B. ...',
        claimSuccess: 'Erfolgreich beansprucht',
        itemCreated: 'Fundstück gemeldet',
        itemUpdated: 'Fundstück aktualisiert',
        deleteConfirm: 'Wirklich löschen?',
      },
      common: {
        save: 'Speichern',
        cancel: 'Abbrechen',
        confirm: 'Bestätigen',
        delete: 'Löschen',
        edit: 'Bearbeiten',
        description: 'Beschreibung',
        success: 'Erfolg',
        error: 'Fehler',
        deleted: 'Gelöscht',
        errorGeneric: 'Fehler aufgetreten',
      },
    },
  },
})

const globalStubs = {
  PageTitle: { template: '<div />', props: ['title', 'subtitle'] },
  ProgressSpinner: { template: '<div class="spinner" />' },
  Button: {
    template: '<button @click="$emit(\'click\')" :disabled="disabled">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'loading', 'disabled'],
    emits: ['click'],
  },
  Select: {
    template: '<select @change="$emit(\'change\')" />',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'showClear'],
    emits: ['update:modelValue', 'change'],
  },
  Dialog: {
    template: '<div v-if="visible"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
    emits: ['update:visible', 'hide'],
  },
  InputText: {
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'maxlength', 'autofocus'],
    emits: ['update:modelValue'],
  },
  Textarea: {
    template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'rows', 'maxlength', 'placeholder'],
    emits: ['update:modelValue'],
  },
  Tag: { template: '<span>{{ value }}</span>', props: ['value', 'severity'] },
  FileUpload: {
    template: '<div class="file-upload-stub" />',
    props: ['mode', 'accept', 'multiple', 'maxFileSize', 'chooseLabel', 'auto', 'customUpload'],
    emits: ['select'],
  },
  ConfirmDialog: { template: '<div />' },
}

const sampleItem = {
  id: 'item-1',
  title: 'Blaue Jacke',
  description: 'Gefunden vor der Schule',
  sectionId: 'sec-1',
  sectionName: 'Grundstufe',
  createdBy: 'user-2',
  createdByName: 'Max Mustermann',
  createdAt: '2025-01-15T10:00:00Z',
  updatedAt: '2025-01-15T10:00:00Z',
  claimedBy: null,
  claimedByName: null,
  claimedAt: null,
  expiresAt: null,
  claimed: false,
  images: [],
}

function mountView() {
  return mount(FundgrubeView, {
    global: { plugins: [i18n], stubs: globalStubs },
  })
}

describe('FundgrubeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Object.assign(mockStore, {
      items: [],
      loading: false,
      fetchItems: vi.fn().mockResolvedValue(undefined),
      createItem: vi.fn(),
      updateItem: vi.fn(),
      deleteItem: vi.fn().mockResolvedValue(undefined),
      claimItem: vi.fn().mockResolvedValue(undefined),
      uploadImages: vi.fn().mockResolvedValue(undefined),
    })
    Object.assign(mockAuth, {
      isAdmin: false,
      isSectionAdmin: false,
      user: { id: 'user-1', specialRoles: [] },
    })
  })

  // --- Mount & lifecycle ---

  it('should mount and call fetchItems on mount', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(mockStore.fetchItems).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should show loading spinner when store is loading', async () => {
    mockStore.loading = true
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.spinner').exists()).toBe(true)
    wrapper.unmount()
  })

  // --- Empty state ---

  it('should render empty state when no items', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Fundstücke vorhanden')
    wrapper.unmount()
  })

  // --- Items grid ---

  it('should render items grid with item data', async () => {
    mockStore.items = [sampleItem]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Blaue Jacke')
    expect(wrapper.text()).toContain('Grundstufe')
    wrapper.unmount()
  })

  it('should show claimed tag for claimed items', async () => {
    mockStore.items = [{ ...sampleItem, claimed: true, claimedBy: 'user-3', claimedByName: 'Anna' }]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Abgeholt')
    wrapper.unmount()
  })

  it('should show description when present', async () => {
    mockStore.items = [sampleItem]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Gefunden vor der Schule')
    wrapper.unmount()
  })

  // --- openCreate ---

  it('openCreate should reset form and show dialog', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.form.title = 'old'
    vm.openCreate()
    expect(vm.form.title).toBe('')
    expect(vm.editMode).toBe(false)
    expect(vm.showCreateDialog).toBe(true)
    wrapper.unmount()
  })

  // --- openEdit ---

  it('openEdit should populate form with item data', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit(sampleItem)
    expect(vm.form.title).toBe('Blaue Jacke')
    expect(vm.form.description).toBe('Gefunden vor der Schule')
    expect(vm.form.sectionId).toBe('sec-1')
    expect(vm.editMode).toBe(true)
    expect(vm.showCreateDialog).toBe(true)
    expect(vm.showDetailDialog).toBe(false)
    wrapper.unmount()
  })

  // --- submitForm ---

  it('should not call createItem when form title is empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.form.title = '   '
    await vm.submitForm()
    expect(mockStore.createItem).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call createItem when form is valid', async () => {
    const newItem = { ...sampleItem, id: 'new-1', title: 'Rucksack' }
    mockStore.createItem = vi.fn().mockResolvedValue(newItem)

    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.form.title = 'Rucksack'
    vm.form.description = 'Blauer Rucksack'
    await vm.submitForm()
    expect(mockStore.createItem).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Rucksack', description: 'Blauer Rucksack' }),
    )
    expect(vm.showCreateDialog).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('should call updateItem in edit mode', async () => {
    const updatedItem = { ...sampleItem, title: 'Rote Jacke' }
    mockStore.updateItem = vi.fn().mockResolvedValue(updatedItem)

    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.editMode = true
    vm.selectedItem = sampleItem
    vm.form.title = 'Rote Jacke'
    vm.form.description = ''
    vm.form.sectionId = null
    await vm.submitForm()
    expect(mockStore.updateItem).toHaveBeenCalledWith('item-1', expect.objectContaining({ title: 'Rote Jacke' }))
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ detail: 'Fundstück aktualisiert' }),
    )
    wrapper.unmount()
  })

  it('should upload images after creating item', async () => {
    const newItem = { ...sampleItem, id: 'new-2' }
    mockStore.createItem = vi.fn().mockResolvedValue(newItem)

    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.form.title = 'Test'
    vm.pendingFiles = [new File(['img'], 'photo.jpg')]
    await vm.submitForm()
    expect(mockStore.uploadImages).toHaveBeenCalledWith('new-2', [expect.any(File)])
    wrapper.unmount()
  })

  it('should show error toast when submitForm fails', async () => {
    mockStore.createItem = vi.fn().mockRejectedValue(new Error('fail'))

    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.form.title = 'Test'
    await vm.submitForm()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    expect(vm.submitting).toBe(false)
    wrapper.unmount()
  })

  // --- openDetail ---

  it('openDetail should set selectedItem and show dialog', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openDetail(sampleItem)
    expect(vm.selectedItem).toEqual(sampleItem)
    expect(vm.showDetailDialog).toBe(true)
    wrapper.unmount()
  })

  // --- submitClaim ---

  it('submitClaim should call claimItem with comment', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.claimingItem = { id: 'item-claim' }
    vm.claimComment = 'My item'
    await vm.submitClaim()
    expect(mockStore.claimItem).toHaveBeenCalledWith('item-claim', { comment: 'My item' })
    expect(vm.showClaimDialog).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('submitClaim should send undefined comment when empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.claimingItem = { id: 'item-x' }
    vm.claimComment = ''
    await vm.submitClaim()
    expect(mockStore.claimItem).toHaveBeenCalledWith('item-x', { comment: undefined })
    wrapper.unmount()
  })

  it('submitClaim should do nothing if claimingItem is null', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.claimingItem = null
    await vm.submitClaim()
    expect(mockStore.claimItem).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('submitClaim should show error toast on failure', async () => {
    mockStore.claimItem = vi.fn().mockRejectedValue({ response: { data: { message: 'Already claimed' } } })
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.claimingItem = { id: 'item-err' }
    await vm.submitClaim()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error', detail: 'Already claimed' }))
    expect(vm.claimSubmitting).toBe(false)
    wrapper.unmount()
  })

  it('submitClaim should fallback to generic error message', async () => {
    mockStore.claimItem = vi.fn().mockRejectedValue(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.claimingItem = { id: 'item-err2' }
    await vm.submitClaim()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: 'Fehler aufgetreten' }))
    wrapper.unmount()
  })

  // --- confirmDelete ---

  it('confirmDelete should invoke confirm.require', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.confirmDelete(sampleItem)
    expect(mockConfirmRequire).toHaveBeenCalledWith(expect.objectContaining({
      message: 'Wirklich löschen?',
    }))
    wrapper.unmount()
  })

  it('confirmDelete accept callback should call deleteItem', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.confirmDelete(sampleItem)
    const callArgs = mockConfirmRequire.mock.calls[0][0]
    await callArgs.accept()
    expect(mockStore.deleteItem).toHaveBeenCalledWith('item-1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Gelöscht' }))
    wrapper.unmount()
  })

  it('confirmDelete should close detail dialog if deleting shown item', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.showDetailDialog = true
    vm.selectedItem = sampleItem
    vm.confirmDelete(sampleItem)
    const callArgs = mockConfirmRequire.mock.calls[0][0]
    await callArgs.accept()
    expect(vm.showDetailDialog).toBe(false)
    wrapper.unmount()
  })

  // --- canEditItem ---

  it('canEditItem returns true for admin', async () => {
    mockAuth.isAdmin = true
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canEditItem({ createdBy: 'other', sectionId: null })).toBe(true)
    wrapper.unmount()
  })

  it('canEditItem returns true for item creator', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canEditItem({ createdBy: 'user-1', sectionId: null })).toBe(true)
    wrapper.unmount()
  })

  it('canEditItem returns true for section admin of items section', async () => {
    mockAuth.isSectionAdmin = true
    mockAuth.user.specialRoles = ['SECTION_ADMIN:sec-1']
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canEditItem({ createdBy: 'other', sectionId: 'sec-1' })).toBe(true)
    wrapper.unmount()
  })

  it('canEditItem returns false for section admin of different section', async () => {
    mockAuth.isSectionAdmin = true
    mockAuth.user.specialRoles = ['SECTION_ADMIN:sec-2']
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canEditItem({ createdBy: 'other', sectionId: 'sec-1' })).toBe(false)
    wrapper.unmount()
  })

  it('canEditItem returns false for unrelated user', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canEditItem({ createdBy: 'other', sectionId: null })).toBe(false)
    wrapper.unmount()
  })

  // --- canClaimItem ---

  it('canClaimItem returns false for claimed item', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canClaimItem({ claimed: true, createdBy: 'other' })).toBe(false)
    wrapper.unmount()
  })

  it('canClaimItem returns false for own item', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canClaimItem({ claimed: false, createdBy: 'user-1' })).toBe(false)
    wrapper.unmount()
  })

  it('canClaimItem returns true for unclaimed other-user item', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.canClaimItem({ claimed: false, createdBy: 'other' })).toBe(true)
    wrapper.unmount()
  })

  // --- applyFilter ---

  it('applyFilter should call fetchItems with sectionId', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    const vm = wrapper.vm as any
    await vm.applyFilter('sec-1')
    expect(mockStore.fetchItems).toHaveBeenCalledWith('sec-1')
    expect(vm.selectedSectionId).toBe('sec-1')
    wrapper.unmount()
  })

  it('applyFilter with null should call fetchItems without sectionId', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    const vm = wrapper.vm as any
    await vm.applyFilter(null)
    expect(mockStore.fetchItems).toHaveBeenCalledWith(undefined)
    wrapper.unmount()
  })

  // --- openClaim ---

  it('openClaim should set state and show dialog', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const item = { id: 'i1' } as any
    vm.openClaim(item)
    expect(vm.claimingItem).toEqual(item)
    expect(vm.showClaimDialog).toBe(true)
    expect(vm.claimComment).toBe('')
    wrapper.unmount()
  })

  // --- formatDate ---

  it('formatDate should return formatted date string', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const result = vm.formatDate('2025-01-15T10:00:00Z')
    expect(result).toContain('2025')
    expect(result).toContain('15')
    wrapper.unmount()
  })

  // --- onFilesSelected ---

  it('onFilesSelected should store files', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const files = [new File(['a'], 'a.jpg'), new File(['b'], 'b.jpg')]
    vm.onFilesSelected({ files })
    expect(vm.pendingFiles).toHaveLength(2)
    wrapper.unmount()
  })

  // --- thumbnailUrl ---

  it('thumbnailUrl should delegate to fundgrubeApi', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const url = vm.thumbnailUrl('img-123')
    expect(url).toContain('img-123')
    wrapper.unmount()
  })
})
