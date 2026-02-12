import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FotoboxLightbox from '@/components/rooms/FotoboxLightbox.vue'
import type { FotoboxImageInfo } from '@/types/fotobox'

vi.mock('@/api/fotobox.api', () => ({
  fotoboxApi: {
    imageUrl: vi.fn((id: string) => `/api/v1/fotobox/images/${id}`),
    thumbnailUrl: vi.fn((id: string) => `/api/v1/fotobox/images/${id}/thumbnail`),
  },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      fotobox: {
        close: 'Schließen',
        previous: 'Zurück',
        next: 'Weiter',
        of: 'von',
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')"><slot /></button>',
    props: ['icon', 'severity', 'text', 'rounded', 'ariaLabel'],
  },
  Teleport: { template: '<div class="teleport-stub"><slot /></div>' },
}

function createImages(count: number): FotoboxImageInfo[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `img-${i}`,
    threadId: 't1',
    uploadedBy: 'user1',
    uploadedByName: 'Test User',
    originalFilename: `photo${i}.jpg`,
    imageUrl: `/api/v1/fotobox/images/img-${i}`,
    thumbnailUrl: `/api/v1/fotobox/images/img-${i}/thumbnail`,
    fileSize: 1024,
    contentType: 'image/jpeg',
    width: 800,
    height: 600,
    caption: i === 0 ? 'First image caption' : null,
    sortOrder: i,
    createdAt: '2024-01-01T00:00:00Z',
  }))
}

function mountLightbox(props: {
  images?: FotoboxImageInfo[]
  currentIndex?: number
  visible?: boolean
}) {
  return mount(FotoboxLightbox, {
    props: {
      images: props.images ?? createImages(3),
      currentIndex: props.currentIndex ?? 0,
      visible: props.visible ?? true,
    },
    global: { plugins: [i18n], stubs },
  })
}

describe('FotoboxLightbox', () => {
  it('should render when visible with images', () => {
    const wrapper = mountLightbox({ visible: true })
    expect(wrapper.find('.lightbox-overlay').exists()).toBe(true)
  })

  it('should not render overlay when not visible', () => {
    const wrapper = mountLightbox({ visible: false })
    expect(wrapper.find('.lightbox-overlay').exists()).toBe(false)
  })

  it('should not render overlay when images are empty', () => {
    const wrapper = mountLightbox({ visible: true, images: [] })
    expect(wrapper.find('.lightbox-overlay').exists()).toBe(false)
  })

  it('should display the current image', () => {
    const images = createImages(3)
    const wrapper = mountLightbox({ images, currentIndex: 1 })
    const img = wrapper.find('.lightbox-image')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/api/v1/fotobox/images/img-1')
  })

  it('should display caption when present', () => {
    const images = createImages(3)
    const wrapper = mountLightbox({ images, currentIndex: 0 })
    expect(wrapper.find('.lightbox-caption').exists()).toBe(true)
    expect(wrapper.find('.lightbox-caption').text()).toBe('First image caption')
  })

  it('should not display caption when absent', () => {
    const images = createImages(3)
    const wrapper = mountLightbox({ images, currentIndex: 1 })
    expect(wrapper.find('.lightbox-caption').exists()).toBe(false)
  })

  it('should display counter', () => {
    const images = createImages(5)
    const wrapper = mountLightbox({ images, currentIndex: 2 })
    expect(wrapper.find('.lightbox-counter').text()).toContain('3')
    expect(wrapper.find('.lightbox-counter').text()).toContain('5')
  })

  it('should emit close on close button click', async () => {
    const wrapper = mountLightbox({ visible: true })
    const closeBtn = wrapper.find('.close-btn')
    await closeBtn.trigger('click')
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')![0]).toEqual([false])
  })

  it('should emit next index on next button click', async () => {
    const wrapper = mountLightbox({ currentIndex: 0 })
    const navRight = wrapper.find('.nav-right')
    expect(navRight.exists()).toBe(true)
    await navRight.trigger('click')
    expect(wrapper.emitted('update:currentIndex')).toBeTruthy()
    expect(wrapper.emitted('update:currentIndex')![0]).toEqual([1])
  })

  it('should emit previous index on prev button click', async () => {
    const wrapper = mountLightbox({ currentIndex: 1 })
    const navLeft = wrapper.find('.nav-left')
    expect(navLeft.exists()).toBe(true)
    await navLeft.trigger('click')
    expect(wrapper.emitted('update:currentIndex')).toBeTruthy()
    expect(wrapper.emitted('update:currentIndex')![0]).toEqual([0])
  })

  it('should not show prev button on first image', () => {
    const wrapper = mountLightbox({ currentIndex: 0 })
    expect(wrapper.find('.nav-left').exists()).toBe(false)
  })

  it('should not show next button on last image', () => {
    const images = createImages(3)
    const wrapper = mountLightbox({ images, currentIndex: 2 })
    expect(wrapper.find('.nav-right').exists()).toBe(false)
  })

  it('should close on overlay click (click.self)', async () => {
    const wrapper = mountLightbox({ visible: true })
    await wrapper.find('.lightbox-overlay').trigger('click')
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')![0]).toEqual([false])
  })

  it('should respond to Escape key', async () => {
    const wrapper = mountLightbox({ visible: true })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:visible')).toBeTruthy()
  })

  it('should respond to ArrowRight key', async () => {
    const wrapper = mountLightbox({ visible: true, currentIndex: 0 })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:currentIndex')).toBeTruthy()
    expect(wrapper.emitted('update:currentIndex')![0]).toEqual([1])
  })

  it('should respond to ArrowLeft key', async () => {
    const wrapper = mountLightbox({ visible: true, currentIndex: 1 })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:currentIndex')).toBeTruthy()
    expect(wrapper.emitted('update:currentIndex')![0]).toEqual([0])
  })

  it('should not navigate past last image with ArrowRight', async () => {
    const images = createImages(2)
    const wrapper = mountLightbox({ visible: true, images, currentIndex: 1 })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:currentIndex')).toBeFalsy()
  })

  it('should not navigate before first image with ArrowLeft', async () => {
    const wrapper = mountLightbox({ visible: true, currentIndex: 0 })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:currentIndex')).toBeFalsy()
  })

  it('should ignore keyboard events when not visible', async () => {
    const wrapper = mountLightbox({ visible: false, currentIndex: 0 })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:currentIndex')).toBeFalsy()
    expect(wrapper.emitted('update:visible')).toBeFalsy()
  })

  it('should use alt from caption or filename', () => {
    const images = createImages(3)
    const wrapper = mountLightbox({ images, currentIndex: 0 })
    const img = wrapper.find('.lightbox-image')
    expect(img.attributes('alt')).toBe('First image caption')
  })
})
