import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import LinkPreview from '@/components/common/LinkPreview.vue'

vi.mock('@/api/feed.api', () => ({
  feedApi: {
    getLinkPreview: vi.fn(),
  },
}))

import { feedApi } from '@/api/feed.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      feed: {
        linkPreview: 'Link-Vorschau',
      },
    },
  },
})

describe('LinkPreview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should show skeleton while loading', () => {
    vi.mocked(feedApi.getLinkPreview).mockReturnValue(new Promise(() => {}))
    const wrapper = mount(LinkPreview, {
      props: { url: 'https://example.com' },
      global: { plugins: [i18n] },
    })
    expect(wrapper.find('.link-preview-skeleton').exists()).toBe(true)
  })

  it('should render preview data after loading', async () => {
    vi.mocked(feedApi.getLinkPreview).mockResolvedValue({
      data: {
        data: {
          url: 'https://example.com',
          title: 'Example Site',
          description: 'A great website',
          imageUrl: 'https://example.com/image.jpg',
          siteName: 'Example',
        },
        success: true,
        message: null,
      },
    } as any)

    const wrapper = mount(LinkPreview, {
      props: { url: 'https://example.com' },
      global: { plugins: [i18n] },
    })

    await flushPromises()

    expect(wrapper.find('.link-preview-skeleton').exists()).toBe(false)
    expect(wrapper.find('.link-preview').exists()).toBe(true)
    expect(wrapper.find('.link-preview-title').text()).toBe('Example Site')
    expect(wrapper.find('.link-preview-description').text()).toBe('A great website')
    expect(wrapper.find('.link-preview-site').text()).toBe('Example')
    expect(wrapper.find('.link-preview-image').attributes('src')).toBe('https://example.com/image.jpg')
  })

  it('should not render if preview is null', async () => {
    vi.mocked(feedApi.getLinkPreview).mockResolvedValue({
      data: { data: null, success: true, message: null },
    } as any)

    const wrapper = mount(LinkPreview, {
      props: { url: 'https://example.com' },
      global: { plugins: [i18n] },
    })

    await flushPromises()

    expect(wrapper.find('.link-preview').exists()).toBe(false)
    expect(wrapper.find('.link-preview-skeleton').exists()).toBe(false)
  })

  it('should not render on API error', async () => {
    vi.mocked(feedApi.getLinkPreview).mockRejectedValue(new Error('Network error'))

    const wrapper = mount(LinkPreview, {
      props: { url: 'https://example.com' },
      global: { plugins: [i18n] },
    })

    await flushPromises()

    expect(wrapper.find('.link-preview').exists()).toBe(false)
    expect(wrapper.find('.link-preview-skeleton').exists()).toBe(false)
  })

  it('should render preview without image', async () => {
    vi.mocked(feedApi.getLinkPreview).mockResolvedValue({
      data: {
        data: {
          url: 'https://example.com',
          title: 'Example',
          description: 'Description text',
          imageUrl: null,
          siteName: null,
        },
        success: true,
        message: null,
      },
    } as any)

    const wrapper = mount(LinkPreview, {
      props: { url: 'https://example.com' },
      global: { plugins: [i18n] },
    })

    await flushPromises()

    expect(wrapper.find('.link-preview').exists()).toBe(true)
    expect(wrapper.find('.link-preview-image').exists()).toBe(false)
    expect(wrapper.find('.link-preview-site').exists()).toBe(false)
    expect(wrapper.find('.link-preview-title').text()).toBe('Example')
  })

  it('should open URL in new tab', async () => {
    vi.mocked(feedApi.getLinkPreview).mockResolvedValue({
      data: {
        data: {
          url: 'https://example.com',
          title: 'Example',
          description: null,
          imageUrl: null,
          siteName: null,
        },
        success: true,
        message: null,
      },
    } as any)

    const wrapper = mount(LinkPreview, {
      props: { url: 'https://example.com' },
      global: { plugins: [i18n] },
    })

    await flushPromises()

    const link = wrapper.find('.link-preview')
    expect(link.attributes('href')).toBe('https://example.com')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
  })
})
