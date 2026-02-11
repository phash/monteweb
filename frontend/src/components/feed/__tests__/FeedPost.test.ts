import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FeedPostComponent from '@/components/feed/FeedPost.vue'
import type { FeedPost } from '@/types/feed'

vi.mock('@/api/feed.api', () => ({
  feedApi: {
    getFeed: vi.fn(),
    createPost: vi.fn(),
    deletePost: vi.fn(),
    addComment: vi.fn(),
    getComments: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      feed: {
        pinned: 'Angeheftet',
        commentPlaceholder: 'Kommentar schreiben...',
        confirmDeleteTitle: 'Post löschen',
        confirmDeleteMessage: 'Möchten Sie diesen Post wirklich löschen?',
      },
      common: { delete: 'Löschen', cancel: 'Abbrechen' },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'disabled', 'ariaLabel'],
    emits: ['click'],
  },
  Textarea: {
    template: '<textarea class="textarea-stub" />',
    props: ['modelValue', 'placeholder', 'autoResize', 'rows'],
  },
  Tag: {
    template: '<span class="tag-stub">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
}

const mockPost: FeedPost = {
  id: 'post-1',
  authorId: 'user-1',
  authorName: 'Max Muster',
  sourceType: 'ROOM',
  sourceId: 'room-1',
  sourceName: 'Klasse 3a',
  title: 'Test Title',
  content: 'Test content',
  pinned: false,
  attachments: [],
  commentCount: 3,
  createdAt: '2025-01-01T12:00:00Z',
  updatedAt: null,
}

function mountFeedPost(post: FeedPost = mockPost) {
  const pinia = createPinia()
  return mount(FeedPostComponent, {
    props: { post },
    global: { plugins: [i18n, pinia], stubs },
  })
}

describe('FeedPost', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render post content', () => {
    const wrapper = mountFeedPost()
    expect(wrapper.text()).toContain('Test content')
    expect(wrapper.text()).toContain('Max Muster')
  })

  it('should render post title when provided', () => {
    const wrapper = mountFeedPost()
    expect(wrapper.find('.post-title').text()).toBe('Test Title')
  })

  it('should show pinned tag when post is pinned', () => {
    const wrapper = mountFeedPost({ ...mockPost, pinned: true })
    expect(wrapper.find('.tag-stub').exists()).toBe(true)
  })

  it('should show source name', () => {
    const wrapper = mountFeedPost()
    expect(wrapper.text()).toContain('Klasse 3a')
  })
})
