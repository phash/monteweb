import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RichContent from '@/components/common/RichContent.vue'

vi.mock('@/api/calendar.api', () => ({
  calendarApi: { getEvent: vi.fn() },
}))
vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: { getJob: vi.fn() },
}))
vi.mock('@/api/rooms.api', () => ({
  roomsApi: { getById: vi.fn() },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      common: { am: 'am' },
    },
  },
})

describe('RichContent', () => {
  it('should render plain text without mentions', () => {
    const wrapper = mount(RichContent, {
      props: { content: 'Hello world' },
      global: { plugins: [i18n] },
    })
    expect(wrapper.text()).toBe('Hello world')
  })

  it('should render mention as @DisplayName with mention-tag class', () => {
    const content = 'Hey @[550e8400-e29b-41d4-a716-446655440000:Max Mustermann] check this'
    const wrapper = mount(RichContent, {
      props: { content },
      global: { plugins: [i18n] },
    })
    const mentionTag = wrapper.find('.mention-tag')
    expect(mentionTag.exists()).toBe(true)
    expect(mentionTag.text()).toBe('@Max Mustermann')
    expect(wrapper.text()).toContain('Hey')
    expect(wrapper.text()).toContain('check this')
  })

  it('should render multiple mentions', () => {
    const content = '@[550e8400-e29b-41d4-a716-446655440000:Max Mustermann] and @[660e8400-e29b-41d4-a716-446655440001:Anna Schmidt]'
    const wrapper = mount(RichContent, {
      props: { content },
      global: { plugins: [i18n] },
    })
    const mentions = wrapper.findAll('.mention-tag')
    expect(mentions.length).toBe(2)
    expect(mentions[0].text()).toBe('@Max Mustermann')
    expect(mentions[1].text()).toBe('@Anna Schmidt')
  })

  it('should handle content with only mentions', () => {
    const content = '@[550e8400-e29b-41d4-a716-446655440000:Max Mustermann]'
    const wrapper = mount(RichContent, {
      props: { content },
      global: { plugins: [i18n] },
    })
    const mentionTag = wrapper.find('.mention-tag')
    expect(mentionTag.exists()).toBe(true)
    expect(mentionTag.text()).toBe('@Max Mustermann')
  })

  it('should handle empty content', () => {
    const wrapper = mount(RichContent, {
      props: { content: '' },
      global: { plugins: [i18n] },
    })
    expect(wrapper.text()).toBe('')
  })

  it('should not render mention-tag for invalid mention format', () => {
    const content = 'Hello @[invalid:Name] world'
    const wrapper = mount(RichContent, {
      props: { content },
      global: { plugins: [i18n] },
    })
    const mentionTag = wrapper.find('.mention-tag')
    expect(mentionTag.exists()).toBe(false)
    expect(wrapper.text()).toContain('@[invalid:Name]')
  })

  it('should set title attribute on mention-tag', () => {
    const content = '@[550e8400-e29b-41d4-a716-446655440000:Max Mustermann]'
    const wrapper = mount(RichContent, {
      props: { content },
      global: { plugins: [i18n] },
    })
    const mentionTag = wrapper.find('.mention-tag')
    expect(mentionTag.attributes('title')).toBe('Max Mustermann')
  })
})
