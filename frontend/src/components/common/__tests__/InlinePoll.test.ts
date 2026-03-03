import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import InlinePoll from '@/components/common/InlinePoll.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth.api', () => ({
  authApi: { login: vi.fn(), register: vi.fn(), logout: vi.fn() },
}))

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getConfig: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/users.api', () => ({
  usersApi: { getMe: vi.fn() },
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      poll: {
        vote: 'Abstimmen',
        totalVotes: '{count} Stimmen',
        closed: 'Geschlossen',
        closePoll: 'Umfrage schließen',
      },
    },
  },
})

const stubs = {
  Button: {
    template: '<button class="btn-stub" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'text', 'severity', 'size', 'disabled'],
    emits: ['click'],
  },
  ProgressBar: {
    template: '<div class="progressbar-stub" :data-value="value"></div>',
    props: ['value', 'showValue', 'pt'],
  },
}

function makePoll(overrides = {}) {
  return {
    id: 'poll-1',
    question: 'Welche Farbe?',
    multiple: false,
    closed: false,
    totalVotes: 10,
    closesAt: null,
    options: [
      { id: 'opt-1', label: 'Rot', voteCount: 6, userVoted: false },
      { id: 'opt-2', label: 'Blau', voteCount: 4, userVoted: false },
    ],
    ...overrides,
  }
}

function createWrapper(poll = makePoll(), authorId?: string) {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(InlinePoll, {
    props: { poll, ...(authorId ? { authorId } : {}) },
    global: {
      plugins: [pinia, i18n],
      stubs,
    },
  })
}

describe('InlinePoll', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render poll question', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('Welche Farbe?')
  })

  it('should render all poll options', () => {
    const wrapper = createWrapper()
    const options = wrapper.findAll('.poll-option')
    expect(options).toHaveLength(2)
    expect(wrapper.text()).toContain('Rot')
    expect(wrapper.text()).toContain('Blau')
  })

  it('should render total votes count', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('10 Stimmen')
  })

  it('should emit vote with option id on single-choice click', async () => {
    const wrapper = createWrapper()
    const options = wrapper.findAll('.poll-option')
    await options[0].trigger('click')
    expect(wrapper.emitted('vote')).toBeTruthy()
    expect(wrapper.emitted('vote')![0]).toEqual([['opt-1']])
  })

  it('should show results when user has voted', () => {
    const poll = makePoll({
      options: [
        { id: 'opt-1', label: 'Rot', voteCount: 6, userVoted: true },
        { id: 'opt-2', label: 'Blau', voteCount: 4, userVoted: false },
      ],
    })
    const wrapper = createWrapper(poll)
    expect(wrapper.findAll('.option-result')).toHaveLength(2)
    expect(wrapper.find('.progressbar-stub').exists()).toBe(true)
  })

  it('should show results with vote counts when poll is closed', () => {
    const poll = makePoll({ closed: true })
    const wrapper = createWrapper(poll)
    expect(wrapper.findAll('.option-result')).toHaveLength(2)
    expect(wrapper.text()).toContain('Geschlossen')
  })

  it('should display check icon on voted option', () => {
    const poll = makePoll({
      options: [
        { id: 'opt-1', label: 'Rot', voteCount: 6, userVoted: true },
        { id: 'opt-2', label: 'Blau', voteCount: 4, userVoted: false },
      ],
    })
    const wrapper = createWrapper(poll)
    expect(wrapper.find('.voted-check').exists()).toBe(true)
  })

  it('should allow multiple selection in multi-choice mode', async () => {
    const poll = makePoll({ multiple: true })
    const wrapper = createWrapper(poll)
    const options = wrapper.findAll('.poll-option')
    await options[0].trigger('click')
    await options[1].trigger('click')
    // Should not emit vote on click for multi — only on submit
    expect(wrapper.emitted('vote')).toBeFalsy()
    // Should show submit button
    expect(wrapper.find('.poll-submit').exists()).toBe(true)
  })

  it('should emit vote with multiple options on submit', async () => {
    const poll = makePoll({ multiple: true })
    const wrapper = createWrapper(poll)
    const options = wrapper.findAll('.poll-option')
    await options[0].trigger('click')
    await options[1].trigger('click')
    const submitBtn = wrapper.find('.poll-submit .btn-stub')
    await submitBtn.trigger('click')
    expect(wrapper.emitted('vote')).toBeTruthy()
    const emitted = wrapper.emitted('vote')![0][0] as string[]
    expect(emitted).toContain('opt-1')
    expect(emitted).toContain('opt-2')
  })

  it('should show close button for author', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.$patch({
      user: { id: 'user-1', role: 'PARENT' } as any,
    })
    const wrapper = mount(InlinePoll, {
      props: { poll: makePoll(), authorId: 'user-1' },
      global: { plugins: [pinia, i18n], stubs },
    })
    expect(wrapper.text()).toContain('Umfrage schließen')
  })

  it('should emit close when close button is clicked', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.$patch({
      user: { id: 'user-1', role: 'SUPERADMIN' } as any,
    })
    const wrapper = mount(InlinePoll, {
      props: { poll: makePoll(), authorId: 'other-user' },
      global: { plugins: [pinia, i18n], stubs },
    })
    const closeBtn = wrapper.findAll('.btn-stub').find(b => b.text().includes('Umfrage schließen'))
    expect(closeBtn).toBeTruthy()
    await closeBtn!.trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('should not show close button when poll is already closed', () => {
    const poll = makePoll({ closed: true })
    const wrapper = createWrapper(poll, 'user-1')
    const closeBtn = wrapper.findAll('.btn-stub').find(b => b.text().includes('Umfrage schließen'))
    expect(closeBtn).toBeUndefined()
  })

  it('should not allow clicking options after voting', async () => {
    const poll = makePoll({
      options: [
        { id: 'opt-1', label: 'Rot', voteCount: 6, userVoted: true },
        { id: 'opt-2', label: 'Blau', voteCount: 4, userVoted: false },
      ],
    })
    const wrapper = createWrapper(poll)
    const options = wrapper.findAll('.poll-option')
    await options[1].trigger('click')
    // Should not emit vote because results are shown
    expect(wrapper.emitted('vote')).toBeFalsy()
  })
})
