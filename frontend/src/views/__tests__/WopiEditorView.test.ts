import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({
    params: { roomId: 'room-1', fileId: 'file-1' },
  })),
  useRouter: vi.fn(() => ({ push: mockPush })),
}))

vi.mock('@/api/files.api', () => ({
  filesApi: {
    listFiles: vi.fn().mockResolvedValue({
      data: {
        data: [
          { id: 'file-1', originalName: 'document.docx' },
        ],
      },
    }),
    createWopiSession: vi.fn().mockResolvedValue({
      data: {
        data: {
          wopiSrc: '/wopi/files/abc123token',
          token: 'abc123token',
          officeUrl: 'https://office.example.com',
        },
      },
    }),
  },
}))

import { filesApi } from '@/api/files.api'
import WopiEditorView from '@/views/WopiEditorView.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      wopi: {
        backToFiles: 'Zur\u00fcck zu Dateien',
        editorTitle: 'Dokumenteditor',
        errorLoading: 'Fehler beim Laden des Editors',
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'size'],
    emits: ['click'],
  },
  ProgressSpinner: {
    template: '<div class="spinner-stub" />',
  },
}

describe('WopiEditorView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(filesApi.listFiles).mockResolvedValue({
      data: {
        data: [
          { id: 'file-1', originalName: 'document.docx' },
        ],
      },
    } as any)
    vi.mocked(filesApi.createWopiSession).mockResolvedValue({
      data: {
        data: {
          wopiSrc: '/wopi/files/abc123token',
          token: 'abc123token',
          officeUrl: 'https://office.example.com',
        },
      },
    } as any)
  })

  it('should call createWopiSession on mount', async () => {
    mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()
    expect(filesApi.createWopiSession).toHaveBeenCalledWith('room-1', 'file-1')
  })

  it('should render iframe with correct ONLYOFFICE URL for docx', async () => {
    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    const iframe = wrapper.find('iframe')
    expect(iframe.exists()).toBe(true)
    const src = iframe.attributes('src') || ''
    // Should use documenteditor for docx
    expect(src).toContain('/web-apps/apps/documenteditor/main/index.html')
    expect(src).toContain('WOPISrc=')
    expect(src).toContain('access_token=abc123token')
    expect(src).toContain('https://office.example.com')
  })

  it('should use spreadsheeteditor for xlsx files', async () => {
    vi.mocked(filesApi.listFiles).mockResolvedValue({
      data: {
        data: [
          { id: 'file-1', originalName: 'budget.xlsx' },
        ],
      },
    } as any)

    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    const iframe = wrapper.find('iframe')
    const src = iframe.attributes('src') || ''
    expect(src).toContain('/web-apps/apps/spreadsheeteditor/main/index.html')
  })

  it('should use presentationeditor for pptx files', async () => {
    vi.mocked(filesApi.listFiles).mockResolvedValue({
      data: {
        data: [
          { id: 'file-1', originalName: 'slides.pptx' },
        ],
      },
    } as any)

    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    const iframe = wrapper.find('iframe')
    const src = iframe.attributes('src') || ''
    expect(src).toContain('/web-apps/apps/presentationeditor/main/index.html')
  })

  it('should use documenteditor for odt files', async () => {
    vi.mocked(filesApi.listFiles).mockResolvedValue({
      data: {
        data: [
          { id: 'file-1', originalName: 'letter.odt' },
        ],
      },
    } as any)

    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    const iframe = wrapper.find('iframe')
    const src = iframe.attributes('src') || ''
    expect(src).toContain('/web-apps/apps/documenteditor/main/index.html')
  })

  it('should render back button', async () => {
    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    const backBtn = wrapper.find('.button-stub')
    expect(backBtn.exists()).toBe(true)
    expect(backBtn.text()).toContain('Dateien')
  })

  it('should navigate back when back button is clicked', async () => {
    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    await wrapper.find('.button-stub').trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'room-detail', params: { id: 'room-1' } })
  })

  it('should show error state when API fails', async () => {
    vi.mocked(filesApi.createWopiSession).mockRejectedValue({
      response: { data: { message: 'WOPI not enabled' } },
    })

    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    expect(wrapper.find('.wopi-error').exists()).toBe(true)
    expect(wrapper.find('.wopi-error').text()).toContain('WOPI not enabled')
    expect(wrapper.find('iframe').exists()).toBe(false)
  })

  it('should show default error message when no response message', async () => {
    vi.mocked(filesApi.createWopiSession).mockRejectedValue(new Error('Network error'))

    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    expect(wrapper.find('.wopi-error').exists()).toBe(true)
    expect(wrapper.find('.wopi-error').text()).toContain('Fehler beim Laden des Editors')
  })

  it('should display the filename in toolbar', async () => {
    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    await flushPromises()

    expect(wrapper.find('.wopi-filename').text()).toBe('document.docx')
  })

  it('should show loading spinner initially', () => {
    const wrapper = mount(WopiEditorView, { global: { plugins: [i18n], stubs } })
    // Before flushPromises, should show loading
    expect(wrapper.find('.spinner-stub').exists()).toBe(true)
  })
})
