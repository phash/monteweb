import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import EmptyState from '@/components/common/EmptyState.vue'

describe('EmptyState', () => {
  it('should render message', () => {
    const wrapper = mount(EmptyState, { props: { message: 'No items found' } })
    expect(wrapper.find('.empty-message').text()).toBe('No items found')
  })

  it('should render icon when provided', () => {
    const wrapper = mount(EmptyState, { props: { message: 'Empty', icon: 'pi pi-inbox' } })
    expect(wrapper.find('.empty-icon').exists()).toBe(true)
  })

  it('should not render icon when not provided', () => {
    const wrapper = mount(EmptyState, { props: { message: 'Empty' } })
    expect(wrapper.find('.empty-icon').exists()).toBe(false)
  })

  it('should render slot content', () => {
    const wrapper = mount(EmptyState, {
      props: { message: 'Empty' },
      slots: { default: '<button>Add Item</button>' },
    })
    expect(wrapper.find('button').text()).toBe('Add Item')
  })
})
