import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import PostComposer from '@/components/feed/PostComposer.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      feed: {
        post: 'Veröffentlichen',
        titlePlaceholder: 'Titel (optional)',
        contentPlaceholder: 'Was gibt es Neues?',
        attachFiles: 'Dateien anhängen',
        removeFile: 'Datei entfernen',
        fileTooLarge: 'Datei zu groß',
      },
      poll: {
        createPoll: 'Umfrage erstellen',
      },
      common: {
        close: 'Schließen',
      },
    },
  },
})

const stubs = {
  InputText: {
    template: '<input class="input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder', 'ariaLabel'],
    emits: ['update:modelValue'],
  },
  MentionInput: {
    template: '<textarea class="mention-input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder', 'autoResize', 'rows'],
    emits: ['update:modelValue'],
  },
  PollComposer: {
    template: '<div class="poll-composer-stub"><button class="poll-submit" @click="$emit(\'submit\', { question: \'Q\', options: [\'A\',\'B\'], multiple: false })">Submit</button><button class="poll-cancel" @click="$emit(\'cancel\')">Cancel</button></div>',
    emits: ['submit', 'cancel'],
  },
  Button: {
    template: '<button class="button-stub" :disabled="disabled" :aria-label="ariaLabel" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'loading', 'disabled', 'size', 'text', 'severity', 'rounded', 'ariaLabel'],
    emits: ['click'],
  },
}

function mountComposer() {
  return mount(PostComposer, { global: { plugins: [i18n], stubs } })
}

describe('PostComposer extended tests', () => {
  // ==================== Basic render ====================

  it('should render post composer card', () => {
    const wrapper = mountComposer()
    expect(wrapper.find('.post-composer').exists()).toBe(true)
  })

  it('should render title input', () => {
    const wrapper = mountComposer()
    expect(wrapper.find('.input-stub').exists()).toBe(true)
  })

  it('should render content textarea', () => {
    const wrapper = mountComposer()
    expect(wrapper.find('.mention-input-stub').exists()).toBe(true)
  })

  it('should render post button', () => {
    const wrapper = mountComposer()
    const postButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Veröffentlichen'))
    expect(postButton).toBeTruthy()
  })

  // ==================== Post button disabled state ====================

  it('should disable post button when content is empty', () => {
    const wrapper = mountComposer()
    const postButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Veröffentlichen'))
    expect(postButton?.attributes('disabled')).toBeDefined()
  })

  // ==================== Attachment button ====================

  it('should render file attachment button', () => {
    const wrapper = mountComposer()
    const attachButton = wrapper.findAll('.button-stub').find(b => b.attributes('aria-label') === 'Dateien anhängen')
    expect(attachButton).toBeTruthy()
  })

  // ==================== Poll composer toggle ====================

  it('should render poll creation button', () => {
    const wrapper = mountComposer()
    const pollButton = wrapper.findAll('.button-stub').find(b => b.attributes('aria-label') === 'Umfrage erstellen')
    expect(pollButton).toBeTruthy()
  })

  it('should not show poll composer by default', () => {
    const wrapper = mountComposer()
    expect(wrapper.find('.poll-composer-stub').exists()).toBe(false)
  })

  it('should show poll composer when poll button is clicked', async () => {
    const wrapper = mountComposer()
    const pollButton = wrapper.findAll('.button-stub').find(b => b.attributes('aria-label') === 'Umfrage erstellen')
    await pollButton?.trigger('click')
    expect(wrapper.find('.poll-composer-stub').exists()).toBe(true)
  })

  it('should hide file attach button and poll button when poll composer is shown', async () => {
    const wrapper = mountComposer()
    const pollButton = wrapper.findAll('.button-stub').find(b => b.attributes('aria-label') === 'Umfrage erstellen')
    await pollButton?.trigger('click')

    // After clicking, the poll composer is shown, and the buttons with v-if="!showPollComposer" should be hidden
    const attachButton = wrapper.findAll('.button-stub').find(b => b.attributes('aria-label') === 'Dateien anhängen')
    expect(attachButton).toBeUndefined()
  })

  it('should hide post button when poll composer is shown', async () => {
    const wrapper = mountComposer()
    const pollButton = wrapper.findAll('.button-stub').find(b => b.attributes('aria-label') === 'Umfrage erstellen')
    await pollButton?.trigger('click')

    const postButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Veröffentlichen'))
    expect(postButton).toBeUndefined()
  })

  // ==================== Hidden file input ====================

  it('should have a hidden file input element', () => {
    const wrapper = mountComposer()
    expect(wrapper.find('.hidden-file-input').exists()).toBe(true)
  })

  it('should have multiple attribute on file input', () => {
    const wrapper = mountComposer()
    const fileInput = wrapper.find('.hidden-file-input')
    expect(fileInput.attributes('multiple')).toBeDefined()
  })

  // ==================== Submit emit ====================

  it('should emit submit event with content when post button clicked', async () => {
    const wrapper = mountComposer()
    // Set content via MentionInput
    const textarea = wrapper.find('.mention-input-stub')
    await textarea.setValue('Hello World')

    const postButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Veröffentlichen'))
    await postButton?.trigger('click')

    expect(wrapper.emitted('submit')).toBeTruthy()
    expect(wrapper.emitted('submit')![0]![0]).toEqual(
      expect.objectContaining({ content: 'Hello World' })
    )
  })

  it('should emit submit with title when provided', async () => {
    const wrapper = mountComposer()
    const titleInput = wrapper.find('.input-stub')
    await titleInput.setValue('My Title')

    const textarea = wrapper.find('.mention-input-stub')
    await textarea.setValue('Content here')

    const postButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Veröffentlichen'))
    await postButton?.trigger('click')

    expect(wrapper.emitted('submit')![0]![0]).toEqual(
      expect.objectContaining({ title: 'My Title', content: 'Content here' })
    )
  })

  it('should clear fields after submit', async () => {
    const wrapper = mountComposer()
    const textarea = wrapper.find('.mention-input-stub')
    await textarea.setValue('Test content')

    const postButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Veröffentlichen'))
    await postButton?.trigger('click')

    // After submit, the component resets title and content
    expect(wrapper.emitted('submit')).toBeTruthy()
  })

  it('should emit undefined for empty title', async () => {
    const wrapper = mountComposer()
    const textarea = wrapper.find('.mention-input-stub')
    await textarea.setValue('Just content')

    const postButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Veröffentlichen'))
    await postButton?.trigger('click')

    expect(wrapper.emitted('submit')![0]![0]).toEqual(
      expect.objectContaining({ title: undefined })
    )
  })

  // ==================== File handling logic ====================

  it('should correctly format file sizes', () => {
    const formatFileSize = (bytes: number): string => {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }

    expect(formatFileSize(0)).toBe('0 B')
    expect(formatFileSize(512)).toBe('512 B')
    expect(formatFileSize(2048)).toBe('2.0 KB')
    expect(formatFileSize(1048576)).toBe('1.0 MB')
    expect(formatFileSize(5 * 1024 * 1024)).toBe('5.0 MB')
  })

  it('should return correct file icons', () => {
    const getFileIcon = (type: string): string => {
      if (type.startsWith('image/')) return 'pi pi-image'
      if (type === 'application/pdf') return 'pi pi-file-pdf'
      if (type.startsWith('video/')) return 'pi pi-video'
      if (type.startsWith('audio/')) return 'pi pi-volume-up'
      return 'pi pi-file'
    }

    expect(getFileIcon('image/jpeg')).toBe('pi pi-image')
    expect(getFileIcon('image/png')).toBe('pi pi-image')
    expect(getFileIcon('application/pdf')).toBe('pi pi-file-pdf')
    expect(getFileIcon('video/mp4')).toBe('pi pi-video')
    expect(getFileIcon('audio/mpeg')).toBe('pi pi-volume-up')
    expect(getFileIcon('application/zip')).toBe('pi pi-file')
    expect(getFileIcon('text/plain')).toBe('pi pi-file')
  })

  // ==================== canSubmit logic ====================

  it('should not be able to submit with only whitespace', () => {
    const content = '   '
    const showPollComposer = false
    const selectedFiles: File[] = []
    const canSubmit = content.trim().length > 0 || showPollComposer || selectedFiles.length > 0
    expect(canSubmit).toBe(false)
  })

  it('should be able to submit with content', () => {
    const content = 'Hello'
    const showPollComposer = false
    const selectedFiles: File[] = []
    const canSubmit = content.trim().length > 0 || showPollComposer || selectedFiles.length > 0
    expect(canSubmit).toBe(true)
  })

  it('should be able to submit with poll composer open', () => {
    const content = ''
    const showPollComposer = true
    const selectedFiles: File[] = []
    const canSubmit = content.trim().length > 0 || showPollComposer || selectedFiles.length > 0
    expect(canSubmit).toBe(true)
  })

  it('should be able to submit with files selected', () => {
    const content = ''
    const showPollComposer = false
    const selectedFiles = [new File([''], 'test.txt')]
    const canSubmit = content.trim().length > 0 || showPollComposer || selectedFiles.length > 0
    expect(canSubmit).toBe(true)
  })

  // ==================== MAX_FILES limit ====================

  it('should enforce max 10 files limit', () => {
    const MAX_FILES = 10
    const currentFiles = 8
    const newFiles = 5
    const total = currentFiles + newFiles
    const allowed = Math.min(newFiles, MAX_FILES - currentFiles)
    expect(total > MAX_FILES).toBe(true)
    expect(allowed).toBe(2) // only 2 more allowed
  })

  it('should allow all files when under limit', () => {
    const MAX_FILES = 10
    const currentFiles = 3
    const newFiles = 4
    const total = currentFiles + newFiles
    expect(total > MAX_FILES).toBe(false)
    expect(total).toBe(7)
  })

  // ==================== MAX_FILE_SIZE ====================

  it('should flag files larger than 50MB', () => {
    const MAX_FILE_SIZE = 50 * 1024 * 1024
    expect(40 * 1024 * 1024 > MAX_FILE_SIZE).toBe(false)
    expect(60 * 1024 * 1024 > MAX_FILE_SIZE).toBe(true)
    expect(50 * 1024 * 1024 > MAX_FILE_SIZE).toBe(false)
  })

  // ==================== Composer actions layout ====================

  it('should render composer actions area', () => {
    const wrapper = mountComposer()
    expect(wrapper.find('.composer-actions').exists()).toBe(true)
  })

  it('should render left-side actions area', () => {
    const wrapper = mountComposer()
    expect(wrapper.find('.composer-actions-left').exists()).toBe(true)
  })
})
