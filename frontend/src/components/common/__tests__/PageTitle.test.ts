import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PageTitle from '@/components/common/PageTitle.vue'

describe('PageTitle', () => {
  it('should render title', () => {
    const wrapper = mount(PageTitle, { props: { title: 'Test Title' } })
    expect(wrapper.find('h1').text()).toBe('Test Title')
  })

  it('should render subtitle when provided', () => {
    const wrapper = mount(PageTitle, { props: { title: 'Title', subtitle: 'Subtitle text' } })
    expect(wrapper.find('.page-subtitle').text()).toBe('Subtitle text')
  })

  it('should not render subtitle when not provided', () => {
    const wrapper = mount(PageTitle, { props: { title: 'Title' } })
    expect(wrapper.find('.page-subtitle').exists()).toBe(false)
  })
})
