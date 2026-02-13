import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RoomFiles from '../RoomFiles.vue'

const mockListFiles = vi.fn().mockResolvedValue({ data: { data: [] } })
const mockListFolders = vi.fn().mockResolvedValue({ data: { data: [] } })
const mockCreateFolder = vi.fn().mockResolvedValue({
  data: { data: { id: 'folder-new', roomId: 'room-1', parentId: null, name: 'New Folder', createdAt: '2025-01-15T10:00:00Z' } },
})
const mockUploadFile = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockDownloadFile = vi.fn().mockResolvedValue({ data: new Blob(['test']) })
const mockDeleteFile = vi.fn().mockResolvedValue({ data: { data: null } })
const mockDeleteFolder = vi.fn().mockResolvedValue({ data: { data: null } })

vi.mock('@/api/files.api', () => ({
  filesApi: {
    listFiles: (...args: any[]) => mockListFiles(...args),
    listFolders: (...args: any[]) => mockListFolders(...args),
    createFolder: (...args: any[]) => mockCreateFolder(...args),
    uploadFile: (...args: any[]) => mockUploadFile(...args),
    downloadFile: (...args: any[]) => mockDownloadFile(...args),
    deleteFile: (...args: any[]) => mockDeleteFile(...args),
    deleteFolder: (...args: any[]) => mockDeleteFolder(...args),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      files: {
        upload: 'Hochladen',
        newFolder: 'Neuer Ordner',
        folderName: 'Ordnername',
        noFiles: 'Keine Dateien vorhanden',
        audience: 'Sichtbarkeit',
        audienceAll: 'Alle',
        audienceParents: 'Nur Eltern',
        audienceStudents: 'Nur Schüler',
      },
      common: {
        cancel: 'Abbrechen',
        create: 'Erstellen',
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')"><slot />{{ label }}</button>',
    props: ['label', 'disabled', 'icon', 'severity', 'text', 'size'],
    emits: ['click'],
  },
  Dialog: {
    template: '<div class="dialog-stub" v-if="visible"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal', 'style'],
    emits: ['update:visible'],
  },
  InputText: {
    template: '<input class="inputtext-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  FileUpload: {
    template: '<div class="fileupload-stub"></div>',
    props: ['mode', 'auto', 'customUpload', 'chooseLabel'],
    emits: ['uploader'],
  },
  Select: {
    template: '<select class="select-stub"></select>',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'size'],
    emits: ['update:modelValue'],
  },
  Tag: {
    template: '<span class="tag-stub">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
}

function mountComponent(props = {}) {
  return mount(RoomFiles, {
    props: { roomId: 'room-1', ...props },
    global: {
      plugins: [i18n, createPinia()],
      stubs,
    },
  })
}

describe('RoomFiles', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render the file manager container', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.room-files').exists()).toBe(true)
  })

  it('should show empty state when no files or folders exist', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.text()).toContain('Keine Dateien vorhanden')
  })

  it('should display breadcrumb with root entry', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.breadcrumb').exists()).toBe(true)
    expect(wrapper.text()).toContain('Dateien')
  })

  it('should render the upload and new folder buttons', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.fileupload-stub').exists()).toBe(true)
    const buttons = wrapper.findAll('.button-stub')
    const newFolderBtn = buttons.find(b => b.text().includes('Neuer Ordner'))
    expect(newFolderBtn).toBeDefined()
  })

  it('should call listFiles and listFolders on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(mockListFiles).toHaveBeenCalledWith('room-1', undefined)
    expect(mockListFolders).toHaveBeenCalledWith('room-1', undefined)
  })

  it('should display folders when they exist', async () => {
    mockListFolders.mockResolvedValueOnce({
      data: {
        data: [
          { id: 'folder-1', roomId: 'room-1', parentId: null, name: 'Documents', createdAt: '2025-01-10T08:00:00Z' },
          { id: 'folder-2', roomId: 'room-1', parentId: null, name: 'Photos', createdAt: '2025-01-11T08:00:00Z' },
        ],
      },
    })

    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('Documents')
    expect(wrapper.text()).toContain('Photos')
  })

  it('should display files when they exist', async () => {
    mockListFiles.mockResolvedValueOnce({
      data: {
        data: [
          {
            id: 'file-1',
            roomId: 'room-1',
            folderId: null,
            originalName: 'report.pdf',
            contentType: 'application/pdf',
            fileSize: 1048576,
            uploadedBy: 'user-1',
            uploaderName: 'Anna Mueller',
            createdAt: '2025-01-12T09:30:00Z',
          },
        ],
      },
    })

    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('report.pdf')
    expect(wrapper.text()).toContain('1.0 MB')
    expect(wrapper.text()).toContain('Anna Mueller')
  })

  it('should format file sizes correctly', async () => {
    mockListFiles.mockResolvedValueOnce({
      data: {
        data: [
          {
            id: 'file-1', roomId: 'room-1', folderId: null, originalName: 'small.txt',
            contentType: 'text/plain', fileSize: 512, uploadedBy: 'u1', uploaderName: 'User',
            createdAt: '2025-01-12T09:30:00Z',
          },
          {
            id: 'file-2', roomId: 'room-1', folderId: null, originalName: 'medium.doc',
            contentType: 'application/msword', fileSize: 51200, uploadedBy: 'u1', uploaderName: 'User',
            createdAt: '2025-01-12T09:30:00Z',
          },
        ],
      },
    })

    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('512 B')
    expect(wrapper.text()).toContain('50.0 KB')
  })

  it('should navigate into a folder when clicked', async () => {
    mockListFolders.mockResolvedValueOnce({
      data: {
        data: [
          { id: 'folder-1', roomId: 'room-1', parentId: null, name: 'SubFolder', createdAt: '2025-01-10T08:00:00Z' },
        ],
      },
    })

    const wrapper = mountComponent()
    await flushPromises()

    // Reset mocks to track the next call
    mockListFiles.mockClear()
    mockListFolders.mockClear()
    mockListFiles.mockResolvedValue({ data: { data: [] } })
    mockListFolders.mockResolvedValue({ data: { data: [] } })

    const folderItem = wrapper.find('.folder')
    await folderItem.trigger('click')
    await flushPromises()

    expect(mockListFiles).toHaveBeenCalledWith('room-1', 'folder-1')
    expect(mockListFolders).toHaveBeenCalledWith('room-1', 'folder-1')
  })

  it('should open new folder dialog when new folder button is clicked', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    // The new folder button is the one with "Neuer Ordner" label
    const newFolderBtn = wrapper.findAll('.button-stub').find(b => b.text().includes('Neuer Ordner'))
    expect(newFolderBtn).toBeDefined()

    // Dialog should not be visible initially
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)

    // Click to open dialog
    await newFolderBtn!.trigger('click')
    await wrapper.vm.$nextTick()

    // Dialog should now be visible
    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should handle API errors gracefully on load', async () => {
    mockListFiles.mockRejectedValueOnce(new Error('Network error'))
    mockListFolders.mockRejectedValueOnce(new Error('Network error'))

    const wrapper = mountComponent()
    await flushPromises()

    // Should show empty state after error
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })
})
