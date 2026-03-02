import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import MarkdownLetterEditor from '../MarkdownLetterEditor.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      parentLetters: {
        content: 'Inhalt',
        preview: 'Vorschau',
      },
    },
  },
})

function mountEditor(props: { modelValue: string; userName: string; placeholder?: string }) {
  return mount(MarkdownLetterEditor, {
    props,
    global: { plugins: [i18n] },
  })
}

describe('MarkdownLetterEditor', () => {
  it('renders textarea and preview panes', () => {
    const wrapper = mountEditor({ modelValue: '# Test', userName: 'Lehrer' })
    expect(wrapper.find('.editor-pane').exists()).toBe(true)
    expect(wrapper.find('.preview-pane').exists()).toBe(true)
  })

  it('renders markdown in preview', () => {
    const wrapper = mountEditor({ modelValue: '**bold**', userName: 'Lehrer' })
    expect(wrapper.find('.preview-pane').html()).toContain('<strong>bold</strong>')
  })

  it('resolves variables in preview', () => {
    const wrapper = mountEditor({ modelValue: 'Hallo {NameKind}', userName: 'Herr Schmidt' })
    expect(wrapper.find('.preview-pane').html()).toContain('Max')
  })

  it('emits update:modelValue on input', async () => {
    const wrapper = mountEditor({ modelValue: '', userName: 'Lehrer' })
    await wrapper.find('textarea').setValue('new content')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['new content'])
  })

  it('exposes insertAtCursor method', () => {
    const wrapper = mountEditor({ modelValue: 'hello', userName: 'Lehrer' })
    expect(typeof wrapper.vm.insertAtCursor).toBe('function')
  })
})
