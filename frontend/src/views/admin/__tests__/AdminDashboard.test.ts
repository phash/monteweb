import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getPublicConfig: vi.fn().mockResolvedValue({ data: { data: { modules: {} } } }),
    getDashboardStats: vi.fn().mockResolvedValue({ data: { data: {} } }),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))
vi.mock('@/api/users.api', () => ({ usersApi: {} }))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      admin: {
        title: 'Verwaltung',
        dashboard: {
          users: 'Benutzer', usersDesc: 'Benutzer verwalten',
          rooms: 'Räume', roomsDesc: 'Räume verwalten',
          sections: 'Bereiche', sectionsDesc: 'Bereiche verwalten',
          families: 'Familien', familiesDesc: 'Familien verwalten',
          billing: 'Jahresabrechnung', billingDesc: 'Abrechnung',
          modules: 'Module', modulesDesc: 'Module verwalten',
          theme: 'Design & Einstellungen', themeDesc: 'Konfiguration',
          settings: 'Einstellungen', settingsDesc: 'Sprache, Registrierung und Jobbörse',
        },
        analytics: { title: 'Statistiken', subtitle: 'Nutzungsübersicht' },
      },
      profileFields: {
        admin: { title: 'Profilfelder', subtitle: 'Benutzerdefinierte Profilfelder verwalten' },
      },
      calendar: {
        ical: { title: 'iCal-Abonnements' },
      },
      errorReports: {
        title: 'Fehlermeldungen', subtitle: 'Automatisch erfasste Fehler verwalten',
      },
      privacy: {
        adminPrivacy: 'Datenschutz', adminPrivacyDesc: 'Datenschutz-Verwaltung',
      },
      csvImport: {
        title: 'CSV-Import', subtitle: 'Benutzer und Familien per CSV importieren',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  'router-link': { template: '<a class="router-link-stub"><slot /></a>', props: ['to'] },
}

describe('AdminDashboard', () => {
  it('should render page title', () => {
    const wrapper = mount(AdminDashboard, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.page-title-stub').text()).toContain('Verwaltung')
  })

  it('should render admin tiles', () => {
    const wrapper = mount(AdminDashboard, { global: { plugins: [i18n], stubs } })
    expect(wrapper.findAll('.router-link-stub')).toHaveLength(14)
  })

  it('should render tile labels', () => {
    const wrapper = mount(AdminDashboard, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('Benutzer')
    expect(wrapper.text()).toContain('Räume')
  })
})
