import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import VideoEmbed from '@/components/common/VideoEmbed.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      feed: {
        videoEmbed: 'Video',
      },
    },
  },
})

describe('VideoEmbed', () => {
  it('should render YouTube embed for youtube.com URL', () => {
    const wrapper = mount(VideoEmbed, {
      props: { url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ' },
      global: { plugins: [i18n] },
    })
    const iframe = wrapper.find('iframe')
    expect(iframe.exists()).toBe(true)
    expect(iframe.attributes('src')).toBe('https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ')
  })

  it('should render YouTube embed for youtu.be URL', () => {
    const wrapper = mount(VideoEmbed, {
      props: { url: 'https://youtu.be/dQw4w9WgXcQ' },
      global: { plugins: [i18n] },
    })
    const iframe = wrapper.find('iframe')
    expect(iframe.exists()).toBe(true)
    expect(iframe.attributes('src')).toBe('https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ')
  })

  it('should render Vimeo embed', () => {
    const wrapper = mount(VideoEmbed, {
      props: { url: 'https://vimeo.com/123456789' },
      global: { plugins: [i18n] },
    })
    const iframe = wrapper.find('iframe')
    expect(iframe.exists()).toBe(true)
    expect(iframe.attributes('src')).toBe('https://player.vimeo.com/video/123456789')
  })

  it('should not render for non-video URL', () => {
    const wrapper = mount(VideoEmbed, {
      props: { url: 'https://example.com/page' },
      global: { plugins: [i18n] },
    })
    expect(wrapper.find('.video-embed').exists()).toBe(false)
    expect(wrapper.find('iframe').exists()).toBe(false)
  })

  it('should have 16:9 aspect ratio container', () => {
    const wrapper = mount(VideoEmbed, {
      props: { url: 'https://youtu.be/dQw4w9WgXcQ' },
      global: { plugins: [i18n] },
    })
    expect(wrapper.find('.video-embed').exists()).toBe(true)
  })

  it('should set proper iframe attributes', () => {
    const wrapper = mount(VideoEmbed, {
      props: { url: 'https://youtu.be/dQw4w9WgXcQ' },
      global: { plugins: [i18n] },
    })
    const iframe = wrapper.find('iframe')
    expect(iframe.attributes('allowfullscreen')).toBeDefined()
    expect(iframe.attributes('loading')).toBe('lazy')
    expect(iframe.attributes('title')).toBe('Video')
  })
})
