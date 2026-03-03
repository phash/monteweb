import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ReactionBar from '@/components/common/ReactionBar.vue'

const sampleReactions = [
  { emoji: '\uD83D\uDC4D', count: 3, userReacted: false },
  { emoji: '\u2764\uFE0F', count: 1, userReacted: true },
]

function createWrapper(reactions = sampleReactions, compact = false) {
  return mount(ReactionBar, {
    props: { reactions, compact },
  })
}

describe('ReactionBar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render reaction chips with count > 0', () => {
    const wrapper = createWrapper()
    const chips = wrapper.findAll('.reaction-chip')
    expect(chips).toHaveLength(2)
  })

  it('should not render reactions with count 0', () => {
    const reactions = [
      { emoji: '\uD83D\uDC4D', count: 0, userReacted: false },
      { emoji: '\u2764\uFE0F', count: 2, userReacted: false },
    ]
    const wrapper = createWrapper(reactions)
    const chips = wrapper.findAll('.reaction-chip')
    expect(chips).toHaveLength(1)
  })

  it('should display emoji and count in reaction chip', () => {
    const wrapper = createWrapper()
    const firstChip = wrapper.findAll('.reaction-chip')[0]
    expect(firstChip.find('.reaction-emoji').text()).toBe('\uD83D\uDC4D')
    expect(firstChip.find('.reaction-count').text()).toBe('3')
  })

  it('should mark user-reacted chips as active', () => {
    const wrapper = createWrapper()
    const chips = wrapper.findAll('.reaction-chip')
    expect(chips[0].classes()).not.toContain('active')
    expect(chips[1].classes()).toContain('active')
  })

  it('should emit react event when clicking a reaction chip', async () => {
    const wrapper = createWrapper()
    const chip = wrapper.findAll('.reaction-chip')[0]
    await chip.trigger('click')
    expect(wrapper.emitted('react')).toBeTruthy()
    expect(wrapper.emitted('react')![0]).toEqual(['\uD83D\uDC4D'])
  })

  it('should render add reaction button', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.reaction-add').exists()).toBe(true)
  })

  it('should toggle emoji picker on add button click', async () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.reaction-picker').exists()).toBe(false)
    await wrapper.find('.reaction-add').trigger('click')
    expect(wrapper.find('.reaction-picker').exists()).toBe(true)
  })

  it('should show 5 emoji options in picker', async () => {
    const wrapper = createWrapper()
    await wrapper.find('.reaction-add').trigger('click')
    const emojis = wrapper.findAll('.picker-emoji')
    expect(emojis).toHaveLength(5)
  })

  it('should emit react event when clicking emoji in picker', async () => {
    const wrapper = createWrapper()
    await wrapper.find('.reaction-add').trigger('click')
    const emojis = wrapper.findAll('.picker-emoji')
    await emojis[0].trigger('click')
    expect(wrapper.emitted('react')).toBeTruthy()
    expect(wrapper.emitted('react')![0]).toEqual(['\uD83D\uDC4D'])
  })

  it('should close picker after selecting an emoji', async () => {
    const wrapper = createWrapper()
    await wrapper.find('.reaction-add').trigger('click')
    expect(wrapper.find('.reaction-picker').exists()).toBe(true)
    const emojis = wrapper.findAll('.picker-emoji')
    await emojis[0].trigger('click')
    expect(wrapper.find('.reaction-picker').exists()).toBe(false)
  })

  it('should apply compact class when compact prop is true', () => {
    const wrapper = createWrapper(sampleReactions, true)
    expect(wrapper.find('.reaction-bar').classes()).toContain('compact')
  })

  it('should render empty state with no reactions but still show add button', () => {
    const wrapper = createWrapper([])
    expect(wrapper.findAll('.reaction-chip')).toHaveLength(0)
    expect(wrapper.find('.reaction-add').exists()).toBe(true)
  })
})
