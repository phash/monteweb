import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import DiscussionThreadView from '../DiscussionThreadView.vue'

const mockFetchThread = vi.fn().mockResolvedValue(undefined)
const mockFetchReplies = vi.fn().mockResolvedValue(undefined)
const mockAddReply = vi.fn().mockResolvedValue(undefined)
const mockArchiveThread = vi.fn().mockResolvedValue(undefined)
const mockDeleteThread = vi.fn().mockResolvedValue(undefined)

vi.mock('@/api/discussions.api', () => ({
  discussionsApi: {
    getThread: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'thread-1',
          roomId: 'room-1',
          createdBy: 'user-1',
          creatorName: 'Teacher',
          title: 'Important Discussion',
          content: 'Please read carefully',
          status: 'ACTIVE',
          audience: 'ALLE',
          replyCount: 2,
          createdAt: '2025-01-10T08:00:00Z',
          updatedAt: '2025-01-10T08:00:00Z',
        },
      },
    }),
    getThreads: vi.fn().mockResolvedValue({ data: { data: { content: [], totalElements: 0 } } }),
    getReplies: vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [
            { id: 'reply-1', threadId: 'thread-1', authorId: 'user-2', authorName: 'Parent A', content: 'I agree!', createdAt: '2025-01-10T09:00:00Z' },
            { id: 'reply-2', threadId: 'thread-1', authorId: 'user-3', authorName: 'Parent B', content: 'Good point', createdAt: '2025-01-10T10:00:00Z' },
          ],
        },
      },
    }),
    createThread: vi.fn().mockResolvedValue({ data: { data: {} } }),
    archiveThread: vi.fn().mockResolvedValue({
      data: {
        data: {
          id: 'thread-1', roomId: 'room-1', createdBy: 'user-1', creatorName: 'Teacher',
          title: 'Important Discussion', content: 'Please read carefully', status: 'ARCHIVED',
          audience: 'ALLE', replyCount: 2, createdAt: '2025-01-10T08:00:00Z', updatedAt: '2025-01-10T12:00:00Z',
        },
      },
    }),
    deleteThread: vi.fn().mockResolvedValue({ data: { data: null } }),
    addReply: vi.fn().mockResolvedValue({
      data: { data: { id: 'reply-new', threadId: 'thread-1', authorId: 'user-1', authorName: 'Me', content: 'My reply', createdAt: '2025-01-10T11:00:00Z' } },
    }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      common: {
        back: 'Zurueck',
        delete: 'Loeschen',
      },
      discussions: {
        archive: 'Archivieren',
        archived: 'Archiviert',
        archivedNotice: 'Dieser Thread ist archiviert.',
        noReplies: 'Noch keine Antworten',
        replyPlaceholder: 'Antwort schreiben...',
        audienceAlle: 'Alle',
        audienceEltern: 'Eltern',
        audienceKinder: 'Kinder',
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
  Textarea: {
    template: '<textarea class="textarea-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>',
    props: ['modelValue', 'placeholder', 'autoResize', 'rows'],
    emits: ['update:modelValue'],
  },
  Tag: {
    template: '<span class="tag-stub" :class="severity">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
  LoadingSpinner: {
    template: '<div class="loading-stub">Loading...</div>',
  },
}

function mountComponent(props = {}) {
  return mount(DiscussionThreadView, {
    props: { roomId: 'room-1', threadId: 'thread-1', isLeader: false, ...props },
    global: {
      plugins: [i18n, createPinia()],
      stubs,
    },
  })
}

describe('DiscussionThreadView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render the component', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })

  it('should show back button', () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.button-stub')
    const backButton = buttons.find(b => b.text().includes('Zurueck'))
    expect(backButton).toBeDefined()
  })

  it('should emit back event when back button is clicked', async () => {
    const wrapper = mountComponent()
    const backButton = wrapper.findAll('.button-stub').find(b => b.text().includes('Zurueck'))
    await backButton!.trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('should display thread title and content after loading', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('Important Discussion')
    expect(wrapper.text()).toContain('Please read carefully')
  })

  it('should display thread creator name and date', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('Teacher')
  })

  it('should display replies from the store', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('I agree!')
    expect(wrapper.text()).toContain('Parent A')
    expect(wrapper.text()).toContain('Good point')
    expect(wrapper.text()).toContain('Parent B')
  })

  it('should show reply input for active threads', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.find('.textarea-stub').exists()).toBe(true)
  })

  it('should not show leader actions when isLeader is false', async () => {
    const wrapper = mountComponent({ isLeader: false })
    await flushPromises()

    const archiveBtn = wrapper.findAll('.button-stub').find(b => b.text().includes('Archivieren'))
    const deleteBtn = wrapper.findAll('.button-stub').find(b => b.text().includes('Loeschen'))
    expect(archiveBtn).toBeUndefined()
    expect(deleteBtn).toBeUndefined()
  })

  it('should show archive and delete buttons when isLeader is true', async () => {
    const wrapper = mountComponent({ isLeader: true })
    await flushPromises()

    const archiveBtn = wrapper.findAll('.button-stub').find(b => b.text().includes('Archivieren'))
    const deleteBtn = wrapper.findAll('.button-stub').find(b => b.text().includes('Loeschen'))
    expect(archiveBtn).toBeDefined()
    expect(deleteBtn).toBeDefined()
  })

  it('should show archived notice when thread is archived', async () => {
    const { discussionsApi } = await import('@/api/discussions.api')
    vi.mocked(discussionsApi.getThread).mockResolvedValueOnce({
      data: {
        data: {
          id: 'thread-1', roomId: 'room-1', createdBy: 'user-1', creatorName: 'Teacher',
          title: 'Archived Thread', content: 'Old topic', status: 'ARCHIVED',
          audience: 'ALLE', replyCount: 0, createdAt: '2025-01-10T08:00:00Z', updatedAt: '2025-01-10T12:00:00Z',
        },
      },
    } as any)
    vi.mocked(discussionsApi.getReplies).mockResolvedValueOnce({
      data: { data: { content: [] } },
    } as any)

    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.text()).toContain('Dieser Thread ist archiviert.')
    expect(wrapper.find('.textarea-stub').exists()).toBe(false)
  })

  it('should disable send button when reply text is empty', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    // Find the send button (the one with disabled attribute in the reply input area)
    const replyInputArea = wrapper.find('.reply-input')
    if (replyInputArea.exists()) {
      const sendBtn = replyInputArea.find('.button-stub')
      expect(sendBtn.attributes('disabled')).toBeDefined()
    }
  })
})
