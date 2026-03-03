import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// --- mock bookmark store
const mockBookmarkStore = {
  bookmarks: [] as any[],
  totalElements: 0,
  loading: false,
  fetchBookmarks: vi.fn().mockResolvedValue(undefined),
  toggle: vi.fn().mockResolvedValue(false),
}

vi.mock('@/stores/bookmarks', () => ({
  useBookmarkStore: vi.fn(() => mockBookmarkStore),
}))

// --- mock useLocaleDate
vi.mock('@/composables/useLocaleDate', () => ({
  useLocaleDate: vi.fn(() => ({
    formatCompactDateTime: vi.fn((d: string) => d),
  })),
}))

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), replace: vi.fn() })),
  useRoute: vi.fn(() => ({ params: {}, query: {} })),
  RouterLink: { template: '<a><slot /></a>', props: ['to'] },
}))

import BookmarksView from '@/views/BookmarksView.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      bookmarks: {
        title: 'Lesezeichen',
        subtitle: 'Gespeicherte Inhalte',
        empty: 'Keine Lesezeichen vorhanden',
        tabs: {
          all: 'Alle',
          posts: 'Beiträge',
          events: 'Termine',
          jobs: 'Jobs',
          wiki: 'Wiki',
        },
        types: {
          POST: 'Beitrag',
          EVENT: 'Termin',
          JOB: 'Job',
          WIKI_PAGE: 'Wiki',
        },
      },
      common: {
        previous: 'Zurück',
        next: 'Weiter',
      },
    },
  },
})

const globalStubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  LoadingSpinner: { template: '<div class="spinner" />' },
  Button: {
    template: '<button @click="$emit(\'click\')" :disabled="disabled" :class="{ \'tab-active\': $attrs.class?.includes?.(\'tab-active\') }">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'loading', 'disabled', 'text', 'rounded', 'size'],
    emits: ['click'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
  RouterLink: { template: '<a class="router-link-stub"><slot /></a>', props: ['to'] },
}

const sampleBookmark = {
  id: 'bm-1',
  userId: 'user-1',
  contentType: 'POST',
  contentId: 'post-1',
  createdAt: '2025-06-01T10:00:00Z',
}

function mountView() {
  return mount(BookmarksView, {
    global: { plugins: [i18n], stubs: globalStubs },
  })
}

describe('BookmarksView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Object.assign(mockBookmarkStore, {
      bookmarks: [],
      totalElements: 0,
      loading: false,
      fetchBookmarks: vi.fn().mockResolvedValue(undefined),
      toggle: vi.fn().mockResolvedValue(false),
    })
  })

  it('should mount without crashing', () => {
    const wrapper = mountView()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render page title', () => {
    const wrapper = mountView()
    expect(wrapper.find('.page-title-stub').text()).toContain('Lesezeichen')
    wrapper.unmount()
  })

  it('should call fetchBookmarks on mount', async () => {
    mountView()
    await flushPromises()
    expect(mockBookmarkStore.fetchBookmarks).toHaveBeenCalled()
  })

  it('should show loading spinner when loading', async () => {
    mockBookmarkStore.loading = true
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.spinner').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should show empty state when no bookmarks', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Keine Lesezeichen vorhanden')
    wrapper.unmount()
  })

  it('should render filter tabs', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Alle')
    expect(wrapper.text()).toContain('Beiträge')
    expect(wrapper.text()).toContain('Termine')
    expect(wrapper.text()).toContain('Jobs')
    expect(wrapper.text()).toContain('Wiki')
    wrapper.unmount()
  })

  it('should render bookmark items when present', async () => {
    mockBookmarkStore.bookmarks = [sampleBookmark]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.bookmark-item').exists()).toBe(true)
    expect(wrapper.find('.tag-stub').text()).toContain('Beitrag')
    wrapper.unmount()
  })

  it('should render multiple bookmark items', async () => {
    mockBookmarkStore.bookmarks = [
      sampleBookmark,
      { ...sampleBookmark, id: 'bm-2', contentType: 'EVENT', contentId: 'evt-1' },
    ]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.findAll('.bookmark-item')).toHaveLength(2)
    wrapper.unmount()
  })

  it('should reload bookmarks when tab is changed', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    const vm = wrapper.vm as any
    vm.activeTab = 'POST'
    await flushPromises()
    expect(mockBookmarkStore.fetchBookmarks).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should reset page to 0 when tab changes', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.page = 2
    vm.activeTab = 'EVENT'
    await flushPromises()
    expect(vm.page).toBe(0)
    wrapper.unmount()
  })

  it('should call toggle and reload when removeBookmark is called', async () => {
    mockBookmarkStore.bookmarks = [sampleBookmark]
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.removeBookmark('POST', 'post-1')
    expect(mockBookmarkStore.toggle).toHaveBeenCalledWith('POST', 'post-1')
    expect(mockBookmarkStore.fetchBookmarks).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should not show pagination when totalElements <= 20', async () => {
    mockBookmarkStore.bookmarks = [sampleBookmark]
    mockBookmarkStore.totalElements = 5
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.pagination').exists()).toBe(false)
    wrapper.unmount()
  })

  it('should show pagination when totalElements > 20', async () => {
    mockBookmarkStore.bookmarks = [sampleBookmark]
    mockBookmarkStore.totalElements = 25
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.pagination').exists()).toBe(true)
    wrapper.unmount()
  })

  it('should have correct type icons via typeIcon', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.typeIcon('POST')).toBe('pi pi-file-edit')
    expect(vm.typeIcon('EVENT')).toBe('pi pi-calendar')
    expect(vm.typeIcon('JOB')).toBe('pi pi-briefcase')
    expect(vm.typeIcon('WIKI_PAGE')).toBe('pi pi-book')
    expect(vm.typeIcon('UNKNOWN')).toBe('pi pi-bookmark')
    wrapper.unmount()
  })
})
