import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import BookmarkButton from '@/components/common/BookmarkButton.vue'

vi.mock('@/api/bookmarks.api', () => ({
  bookmarksApi: {
    toggle: vi.fn().mockResolvedValue({ data: { data: { bookmarked: true } } }),
    getBookmarkedIds: vi.fn().mockResolvedValue({ data: { data: [] } }),
  },
}))

const stubs = {
  Button: {
    template: '<button class="btn-stub" :class="$attrs.class" @click="$emit(\'click\', $event)"><i :class="$attrs.icon" /></button>',
    props: ['icon', 'text', 'rounded', 'size'],
  },
}

describe('BookmarkButton', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render bookmark icon', () => {
    const wrapper = mount(BookmarkButton, {
      props: { contentType: 'POST', contentId: 'p1' },
      global: { plugins: [createPinia()], stubs },
    })
    expect(wrapper.find('.btn-stub').exists()).toBe(true)
  })

  it('should show unfilled bookmark when not bookmarked', () => {
    const wrapper = mount(BookmarkButton, {
      props: { contentType: 'POST', contentId: 'p1' },
      global: { plugins: [createPinia()], stubs },
    })
    expect(wrapper.find('.bookmark-active').exists()).toBe(false)
  })

  it('should toggle bookmark on click', async () => {
    const { bookmarksApi } = await import('@/api/bookmarks.api')
    const wrapper = mount(BookmarkButton, {
      props: { contentType: 'EVENT', contentId: 'e1' },
      global: { plugins: [createPinia()], stubs },
    })
    await wrapper.find('.btn-stub').trigger('click')
    expect(bookmarksApi.toggle).toHaveBeenCalledWith('EVENT', 'e1')
  })
})
