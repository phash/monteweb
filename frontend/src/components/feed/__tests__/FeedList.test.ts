import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FeedList from '../FeedList.vue'

vi.mock('@/api/feed.api', () => ({
  feedApi: {
    getFeed: vi.fn().mockResolvedValue({
      data: { data: { content: [], last: true } },
    }),
    getBanners: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getComments: vi.fn().mockResolvedValue({ data: { data: { content: [] } } }),
    addComment: vi.fn().mockResolvedValue({ data: { data: {} } }),
    deletePost: vi.fn().mockResolvedValue({ data: { data: null } }),
    pinPost: vi.fn().mockResolvedValue({ data: { data: null } }),
    createPost: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/auth.api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: {
    getMe: vi.fn().mockResolvedValue({
      data: { data: { id: 'user-1', displayName: 'Test User', role: 'PARENT' } },
    }),
  },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      feed: {
        noPosts: 'Keine Beitraege vorhanden',
        loadMore: 'Mehr laden',
        pinned: 'Angeheftet',
        commentPlaceholder: 'Kommentar...',
        confirmDeleteTitle: 'Beitrag loeschen',
        confirmDeleteMessage: 'Wirklich loeschen?',
      },
      common: {
        loading: 'Laden...',
        cancel: 'Abbrechen',
        delete: 'Loeschen',
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" :disabled="disabled" :loading="loading" @click="$emit(\'click\')"><slot />{{ label }}</button>',
    props: ['label', 'disabled', 'loading', 'icon', 'severity', 'text', 'size', 'ariaLabel'],
    emits: ['click'],
  },
  FeedPostComponent: {
    template: '<div class="feedpost-stub">{{ post.title || post.content }}</div>',
    props: ['post'],
  },
  LoadingSpinner: {
    template: '<div class="loading-stub">Loading...</div>',
  },
  EmptyState: {
    template: '<div class="emptystate-stub">{{ message }}</div>',
    props: ['icon', 'message'],
  },
  Tag: {
    template: '<span class="tag-stub">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
  Textarea: {
    template: '<textarea class="textarea-stub"></textarea>',
    props: ['modelValue', 'placeholder', 'autoResize', 'rows'],
    emits: ['update:modelValue'],
  },
}

function mountComponent() {
  return mount(FeedList, {
    global: {
      plugins: [i18n, createPinia()],
      stubs,
    },
  })
}

describe('FeedList', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render the feed list container', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.feed-list').exists()).toBe(true)
  })

  it('should show empty state when no posts exist', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.find('.emptystate-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('Keine Beitraege vorhanden')
  })

  it('should call fetchFeed on mount with reset=true', async () => {
    const { feedApi } = await import('@/api/feed.api')
    mountComponent()
    await flushPromises()

    expect(feedApi.getFeed).toHaveBeenCalledWith(0)
  })

  it('should display posts when feed store has posts', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const { feedApi } = await import('@/api/feed.api')
    vi.mocked(feedApi.getFeed).mockResolvedValueOnce({
      data: {
        data: {
          content: [
            {
              id: 'post-1', authorId: 'user-1', authorName: 'Teacher', sourceType: 'ROOM',
              sourceId: 'room-1', sourceName: 'Class A', title: 'Welcome Post',
              content: 'Hello everyone!', pinned: false, attachments: [], commentCount: 3,
              createdAt: '2025-01-10T08:00:00Z', updatedAt: null,
            },
            {
              id: 'post-2', authorId: 'user-2', authorName: 'Admin', sourceType: 'SCHOOL',
              sourceId: null, sourceName: null, title: null,
              content: 'School update', pinned: true, attachments: [], commentCount: 0,
              createdAt: '2025-01-11T08:00:00Z', updatedAt: null,
            },
          ],
          last: true,
        },
      },
    } as any)

    const wrapper = mount(FeedList, {
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    const posts = wrapper.findAll('.feedpost-stub')
    expect(posts).toHaveLength(2)
    expect(wrapper.text()).toContain('Welcome Post')
    expect(wrapper.text()).toContain('School update')
  })

  it('should show load more button when hasMore is true', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const { feedApi } = await import('@/api/feed.api')
    vi.mocked(feedApi.getFeed).mockResolvedValueOnce({
      data: {
        data: {
          content: [
            {
              id: 'post-1', authorId: 'user-1', authorName: 'Teacher', sourceType: 'ROOM',
              sourceId: 'room-1', sourceName: 'Class A', title: 'Post',
              content: 'Content', pinned: false, attachments: [], commentCount: 0,
              createdAt: '2025-01-10T08:00:00Z', updatedAt: null,
            },
          ],
          last: false,
        },
      },
    } as any)

    const wrapper = mount(FeedList, {
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })
    await flushPromises()

    const loadMoreBtn = wrapper.findAll('.button-stub').find(b => b.text().includes('Mehr laden'))
    expect(loadMoreBtn).toBeDefined()
  })

  it('should not show load more button when all posts are loaded', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const loadMoreBtn = wrapper.findAll('.button-stub').find(b => b.text().includes('Mehr laden'))
    expect(loadMoreBtn).toBeUndefined()
  })

  it('should show loading spinner when loading and no posts', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    // Return a never-resolving promise to keep loading state
    const { feedApi } = await import('@/api/feed.api')
    vi.mocked(feedApi.getFeed).mockReturnValueOnce(new Promise(() => {}))

    const wrapper = mount(FeedList, {
      global: {
        plugins: [i18n, pinia],
        stubs,
      },
    })

    // Store should be in loading state
    const { useFeedStore } = await import('@/stores/feed')
    const feedStore = useFeedStore()

    // Manually set loading to verify the conditional rendering
    feedStore.loading = true
    feedStore.posts = []
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.loading-stub').exists()).toBe(true)
  })
})
