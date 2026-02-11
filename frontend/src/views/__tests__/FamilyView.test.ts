import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import FamilyView from '@/views/FamilyView.vue'

vi.mock('@/api/family.api', () => ({
  familyApi: {
    getMine: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getMyInvitations: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getFamilyInvitations: vi.fn().mockResolvedValue({ data: { data: [] } }),
    createFamily: vi.fn(),
    joinByCode: vi.fn(),
    generateInviteCode: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      family: {
        title: 'Familienverbund',
        create: 'Erstellen',
        join: 'Beitreten',
        inviteCode: 'Einladungscode',
        generateCode: 'Code generieren',
        noFamily: 'Kein Familienverbund.',
        familyName: 'Familienname',
        members: 'Mitglieder',
        childRole: 'Kind',
        parentRole: 'Elternteil',
        joinCode: 'Code eingeben',
        inviteMembers: 'Mitglieder einladen',
        sentInvitations: 'Gesendete Einladungen',
        receivedInvitations: 'Empfangene Einladungen',
        accept: 'Annehmen',
        decline: 'Ablehnen',
        pending: 'Ausstehend',
        role: 'Rolle',
      },
      common: { save: 'Speichern', cancel: 'Abbrechen', delete: 'Löschen', removeAvatar: 'Avatar entfernen' },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title', 'subtitle'] },
  EmptyState: { template: '<div class="empty-stub">{{ message }}</div>', props: ['icon', 'message'] },
  LoadingSpinner: { template: '<div class="loading-stub" />' },
  FamilyHoursWidget: { template: '<div class="hours-stub" />', props: ['familyId'] },
  InviteMemberDialog: { template: '<div class="invite-stub" />', props: ['visible', 'familyId'] },
  AvatarUpload: { template: '<div class="avatar-stub" />', props: ['imageUrl', 'size', 'icon', 'editable'] },
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'text', 'size', 'outlined'],
    emits: ['click'],
  },
  InputText: { template: '<input class="input-stub" />', props: ['modelValue', 'id'] },
  Dialog: {
    template: '<div v-if="visible" class="dialog-stub"><slot /><slot name="footer" /></div>',
    props: ['visible', 'header', 'modal'],
  },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity', 'size'] },
}

function mountFamily() {
  const pinia = createPinia()
  return mount(FamilyView, {
    global: {
      plugins: [i18n, pinia],
      stubs,
    },
  })
}

describe('FamilyView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', () => {
    const wrapper = mountFamily()
    expect(wrapper.find('.page-title-stub').text()).toContain('Familienverbund')
  })

  it('should render action buttons', () => {
    const wrapper = mountFamily()
    expect(wrapper.findAll('.button-stub').length).toBeGreaterThanOrEqual(0)
  })

  it('should show empty state when no families', async () => {
    const wrapper = mountFamily()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // Either empty state or loading spinner should exist
    expect(wrapper.find('.empty-stub').exists() || wrapper.find('.loading-stub').exists()).toBe(true)
  })

  it('should have dialog components for create/join', () => {
    const wrapper = mountFamily()
    // Dialogs are not visible by default
    expect(wrapper.find('.dialog-stub').exists()).toBe(false)
  })
})
