import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createRouter, createMemoryHistory } from 'vue-router'
import GlobalSearch from '@/components/common/GlobalSearch.vue'

vi.mock('@/api/search.api', () => ({
  searchApi: {
    search: vi.fn(),
  },
}))

import { searchApi } from '@/api/search.api'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      search: {
        title: 'Suche',
        placeholder: 'Suchen...',
        noResults: 'Keine Ergebnisse gefunden',
        hint: 'Mindestens 2 Zeichen eingeben',
        shortcutHint: 'zum Suchen',
        filterAll: 'Alle',
        filterUsers: 'Benutzer',
        filterRooms: 'Raeume',
        filterPosts: 'Beitraege',
        filterEvents: 'Termine',
        filterFiles: 'Dateien',
        filterWiki: 'Wiki',
        filterTasks: 'Aufgaben',
        typeUser: 'Benutzer',
        typeRoom: 'Raeume',
        typePost: 'Beitraege',
        typeEvent: 'Termine',
        typeFile: 'Dateien',
        typeWiki: 'Wiki-Seiten',
        typeTask: 'Aufgaben',
      },
    },
  },
})

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', component: { template: '<div />' } },
    { path: '/users/:id', component: { template: '<div />' } },
    { path: '/rooms/:id', component: { template: '<div />' } },
    { path: '/feed', component: { template: '<div />' } },
    { path: '/calendar', component: { template: '<div />' } },
  ],
})

function mountGlobalSearch() {
  return mount(GlobalSearch, {
    global: {
      plugins: [i18n, router],
      stubs: {
        Dialog: {
          template: '<div class="dialog-stub" v-if="visible"><slot /></div>',
          props: ['visible', 'header', 'modal', 'pt'],
        },
        InputText: {
          template: '<input class="search-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
          props: ['modelValue', 'placeholder', 'autofocus'],
          emits: ['update:modelValue'],
        },
        ProgressSpinner: {
          template: '<div class="spinner-stub" />',
        },
      },
    },
  })
}

describe('GlobalSearch', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should render dialog when opened', async () => {
    const wrapper = mountGlobalSearch()
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)

    wrapper.vm.open()
    await flushPromises()

    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should show hint when query is empty', async () => {
    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    expect(wrapper.find('.search-hint').exists()).toBe(true)
    expect(wrapper.find('.search-hint').text()).toContain('Mindestens 2 Zeichen eingeben')
  })

  it('should show filter chips', async () => {
    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const chips = wrapper.findAll('.filter-chip')
    expect(chips.length).toBe(8)
    expect(chips[0].text()).toBe('Alle')
    expect(chips[1].text()).toBe('Benutzer')
    expect(chips[2].text()).toBe('Raeume')
    expect(chips[3].text()).toBe('Beitraege')
    expect(chips[4].text()).toBe('Termine')
    expect(chips[5].text()).toBe('Dateien')
    expect(chips[6].text()).toBe('Wiki')
    expect(chips[7].text()).toBe('Aufgaben')
  })

  it('should not search with less than 2 characters', async () => {
    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('a')
    vi.advanceTimersByTime(500)
    await flushPromises()

    expect(searchApi.search).not.toHaveBeenCalled()
  })

  it('should search with 2+ characters after debounce', async () => {
    vi.mocked(searchApi.search).mockResolvedValue({
      data: {
        data: [
          {
            id: '1',
            type: 'USER',
            title: 'Anna Mueller',
            subtitle: 'anna@test.de',
            snippet: null,
            url: '/users/1',
            timestamp: null,
          },
        ],
        success: true,
        message: null,
        timestamp: new Date().toISOString(),
      },
    } as any)

    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('Anna')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(searchApi.search).toHaveBeenCalledWith('Anna', 'ALL', 20)
  })

  it('should display results grouped by type', async () => {
    vi.mocked(searchApi.search).mockResolvedValue({
      data: {
        data: [
          {
            id: '1',
            type: 'USER',
            title: 'Anna Mueller',
            subtitle: 'anna@test.de',
            snippet: null,
            url: '/users/1',
            timestamp: null,
          },
          {
            id: '2',
            type: 'ROOM',
            title: 'Sonnengruppe',
            subtitle: 'KLASSE - 5 Mitglieder',
            snippet: null,
            url: '/rooms/2',
            timestamp: null,
          },
        ],
        success: true,
        message: null,
        timestamp: new Date().toISOString(),
      },
    } as any)

    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('test')
    vi.advanceTimersByTime(300)
    await flushPromises()

    const headers = wrapper.findAll('.result-group-header')
    expect(headers.length).toBe(2)
    expect(headers[0].text()).toBe('Benutzer')
    expect(headers[1].text()).toBe('Raeume')

    const items = wrapper.findAll('.search-result-item')
    expect(items.length).toBe(2)
    expect(items[0].find('.result-title').text()).toBe('Anna Mueller')
    expect(items[1].find('.result-title').text()).toBe('Sonnengruppe')
  })

  it('should show no results message when empty', async () => {
    vi.mocked(searchApi.search).mockResolvedValue({
      data: { data: [], success: true, message: null, timestamp: new Date().toISOString() },
    } as any)

    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('xyznotfound')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.search-empty').exists()).toBe(true)
    expect(wrapper.find('.search-empty').text()).toContain('Keine Ergebnisse gefunden')
  })

  it('should navigate to result on click', async () => {
    vi.mocked(searchApi.search).mockResolvedValue({
      data: {
        data: [
          {
            id: '1',
            type: 'USER',
            title: 'Anna Mueller',
            subtitle: 'anna@test.de',
            snippet: null,
            url: '/users/1',
            timestamp: null,
          },
        ],
        success: true,
        message: null,
        timestamp: new Date().toISOString(),
      },
    } as any)

    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('Anna')
    vi.advanceTimersByTime(300)
    await flushPromises()

    await wrapper.find('.search-result-item').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/users/1')
  })

  it('should change filter and re-search', async () => {
    vi.mocked(searchApi.search).mockResolvedValue({
      data: { data: [], success: true, message: null, timestamp: new Date().toISOString() },
    } as any)

    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('test')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(searchApi.search).toHaveBeenCalledWith('test', 'ALL', 20)

    // Click on "Users" filter chip
    const chips = wrapper.findAll('.filter-chip')
    await chips[1].trigger('click')
    await flushPromises()

    expect(searchApi.search).toHaveBeenCalledWith('test', 'USER', 20)
  })

  it('should handle Ctrl+K keyboard shortcut', async () => {
    const wrapper = mountGlobalSearch()

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true }))
    await flushPromises()

    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
  })

  it('should show loading spinner while searching', async () => {
    vi.mocked(searchApi.search).mockReturnValue(new Promise(() => {}))

    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('test')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.search-loading').exists()).toBe(true)
  })

  it('should handle API error gracefully', async () => {
    vi.mocked(searchApi.search).mockRejectedValue(new Error('Network error'))

    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('test')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.search-empty').exists()).toBe(true)
  })

  it('should close when calling close', async () => {
    const wrapper = mountGlobalSearch()
    wrapper.vm.open()
    await flushPromises()
    expect(wrapper.find('.dialog-stub').exists()).toBe(true)

    wrapper.vm.close()
    await flushPromises()
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)
  })
})
