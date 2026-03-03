import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

function createMockConfig() {
  return {
    id: 'tenant-1',
    schoolName: 'Montessori Schule',
    defaultLanguage: 'de',
    availableLanguages: ['de', 'en'],
    requireUserApproval: true,
    requireAssignmentConfirmation: true,
    twoFactorMode: 'DISABLED',
    twoFactorGraceDeadline: null,
    targetHoursPerFamily: 30,
    targetCleaningHours: 3,
    bundesland: 'BY',
    schoolVacations: [],
    parentToParentMessaging: false,
    studentToStudentMessaging: false,
    soleCustodyEnabled: false,
    requireFamilySwitchApproval: false,
    ldapUrl: '',
    ldapBaseDn: '',
    ldapBindDn: '',
    ldapUserSearchFilter: '(uid={0})',
    ldapAttrEmail: 'mail',
    ldapAttrFirstName: 'givenName',
    ldapAttrLastName: 'sn',
    ldapDefaultRole: 'PARENT',
    ldapUseSsl: false,
    ldapConfigured: false,
    maintenanceMessage: '',
    clamavHost: 'clamav',
    clamavPort: 3310,
    jitsiServerUrl: 'https://meet.jit.si',
    wopiOfficeUrl: '',
    modules: {
      messaging: true,
      maintenance: false,
      ldap: false,
      directoryAdminOnly: false,
      impersonation: false,
      jitsi: false,
      wopi: false,
      clamav: false,
    },
  }
}

// --- mock admin API (inline, no external const refs in factory)
vi.mock('@/api/admin.api', () => ({
  adminApi: {
    getConfig: vi.fn(),
    getPublicConfig: vi.fn(),
    updateConfig: vi.fn(),
    updateModules: vi.fn(),
    updateMaintenance: vi.fn(),
    updateTheme: vi.fn().mockResolvedValue({ data: { data: {} } }),
    testLdapConnection: vi.fn(),
  },
}))

vi.mock('@/api/auth.api', () => ({ authApi: {} }))

vi.mock('@/data/schoolVacations', () => ({
  predefinedVacations: {
    BY: [
      { name: 'Herbstferien', from: '2025-10-26', to: '2025-10-30' },
      { name: 'Weihnachtsferien', from: '2025-12-22', to: '2026-01-05' },
    ],
  },
}))

vi.mock('primevue/usetoast', () => ({
  useToast: vi.fn(() => ({ add: vi.fn() })),
}))

import AdminSettings from '@/views/admin/AdminSettings.vue'
import { adminApi } from '@/api/admin.api'
import { useToast } from 'primevue/usetoast'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      admin: {
        settings: {
          title: 'Einstellungen',
          saved: 'Einstellungen gespeichert',
          language: 'Sprache',
          defaultLanguage: 'Standardsprache',
          availableLanguages: 'Verfügbare Sprachen',
          availableLanguagesHint: 'Hinweis',
          registration: 'Registrierung',
          requireUserApproval: 'Benutzer-Freigabe',
          requireUserApprovalHint: 'Hinweis',
          directory: 'Verzeichnis',
          directoryAdminOnly: 'Nur für Admins',
          directoryAdminOnlyHint: 'Hinweis',
          communication: 'Kommunikation',
          parentToParentMessaging: 'Eltern-Nachrichten',
          parentToParentMessagingHint: 'Hinweis',
          studentToStudentMessaging: 'Schüler-Nachrichten',
          studentToStudentMessagingHint: 'Hinweis',
          jobboard: 'Jobbörse',
          family: 'Familie',
          soleCustodyEnabled: 'Alleiniges Sorgerecht',
          soleCustodyEnabledHint: 'Hinweis',
          requireFamilySwitchApproval: 'Familienwechsel',
          requireFamilySwitchApprovalHint: 'Hinweis',
          groups: {
            general: 'Allgemein',
            communication: 'Kommunikation',
            integration: 'Integrationen',
            security: 'Sicherheit',
            hours: 'Stunden',
          },
          ldap: {
            title: 'LDAP / Active Directory',
            enabled: 'LDAP aktiviert',
            enabledHint: 'Hinweis',
            saved: 'LDAP gespeichert',
            url: 'LDAP URL',
            urlPlaceholder: 'ldap://...',
            urlHint: 'Hinweis',
            baseDn: 'Base DN',
            baseDnPlaceholder: 'dc=example',
            baseDnHint: 'Hinweis',
            bindDn: 'Bind DN',
            bindDnPlaceholder: 'cn=admin',
            bindDnHint: 'Hinweis',
            bindPassword: 'Passwort',
            passwordNotShown: '***',
            bindPasswordPlaceholder: 'Passwort',
            bindPasswordHint: 'Hinweis',
            userSearchFilter: 'Suchfilter',
            userSearchFilterHint: 'Hinweis',
            useSsl: 'SSL verwenden',
            useSslHint: 'Hinweis',
            attrMapping: 'Attribut-Mapping',
            attrEmail: 'E-Mail',
            attrFirstName: 'Vorname',
            attrLastName: 'Nachname',
            defaultRole: 'Standard-Rolle',
            defaultRoleHint: 'Hinweis',
            testConnection: 'Verbindung testen',
            testSuccess: 'Verbindung erfolgreich',
            testFailed: 'Verbindung fehlgeschlagen',
          },
        },
        maintenance: {
          title: 'Wartungsmodus',
          enabled: 'Wartungsmodus aktiviert',
          warning: 'Warnung!',
          message: 'Nachricht',
          messagePlaceholder: 'Wartung...',
          saved: 'Wartung gespeichert',
        },
        requireConfirmation: 'Bestätigung erforderlich',
        requireConfirmationHint: 'Hinweis',
        holidaysAndVacations: 'Feiertage & Ferien',
        bundesland: 'Bundesland',
        bundeslandHint: 'Hinweis',
        schoolVacations: 'Schulferien',
        loadVacations: 'Ferien laden',
        loadVacationsHint: 'Hinweis',
        vacationName: 'Name',
        vacationFrom: 'Von',
        vacationTo: 'Bis',
        addVacation: 'Ferien hinzufügen',
        vacationsSaved: 'Ferien gespeichert',
        totalHoursTarget: 'Gesamtstunden',
        cleaningHoursTarget: 'Putzstunden',
        saveHoursConfig: 'Stunden speichern',
        hoursConfigSaved: 'Stunden gespeichert',
        jitsi: {
          title: 'Jitsi Video',
          hint: 'Hinweis',
          serverUrl: 'Server URL',
          saved: 'Jitsi gespeichert',
        },
        clamav: {
          title: 'ClamAV',
          hint: 'Hinweis',
          host: 'Host',
          port: 'Port',
          saved: 'ClamAV gespeichert',
        },
      },
      twoFactor: {
        title: '2-Faktor-Auth.',
        adminMode: 'Modus',
        adminModeHint: 'Hinweis',
        graceDeadline: 'Frist: {date}',
        modes: {
          DISABLED: 'Deaktiviert',
          OPTIONAL: 'Optional',
          MANDATORY: 'Verpflichtend',
        },
      },
      wopi: {
        title: 'ONLYOFFICE',
        hint: 'Hinweis',
        officeUrl: 'Office URL',
        officeUrlHint: 'Hinweis',
        saved: 'WOPI gespeichert',
      },
      auth: {
        impersonation: {
          toggle: 'Impersonation',
          dangerWarning: 'Gefährlich!',
          toggleDescription: 'Benutzer imitieren',
        },
      },
      common: {
        save: 'Speichern',
        noData: 'Keine Daten',
        actions: 'Aktionen',
        delete: 'Löschen',
        active: 'Aktiv',
      },
    },
  },
})

const globalStubs = {
  Button: {
    template: '<button class="button-stub" @click="$emit(\'click\')" :disabled="loading">{{ label }}</button>',
    props: ['label', 'icon', 'severity', 'loading', 'size', 'ariaLabel'],
    emits: ['click'],
  },
  InputText: {
    template: '<input class="input-text-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  InputNumber: {
    template: '<input class="input-number-stub" type="number" />',
    props: ['modelValue', 'min', 'max', 'minFractionDigits', 'maxFractionDigits'],
    emits: ['update:modelValue'],
  },
  Select: {
    template: '<select class="select-stub" />',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
    emits: ['update:modelValue'],
  },
  MultiSelect: {
    template: '<div class="multiselect-stub" />',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
    emits: ['update:modelValue'],
  },
  DataTable: {
    template: '<table class="datatable-stub"><slot /></table>',
    props: ['value', 'stripedRows'],
  },
  Column: {
    template: '<td class="column-stub"><slot /></td>',
    props: ['header'],
  },
  ToggleSwitch: {
    template: '<input class="toggle-stub" type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', !modelValue)" />',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  Message: {
    template: '<div class="message-stub"><slot /></div>',
    props: ['severity', 'closable'],
  },
  Password: {
    template: '<input class="password-stub" type="password" />',
    props: ['modelValue', 'placeholder', 'feedback', 'toggleMask', 'inputClass'],
    emits: ['update:modelValue'],
  },
  Accordion: {
    template: '<div class="accordion-stub"><slot /></div>',
    props: ['multiple', 'value'],
  },
  AccordionPanel: {
    template: '<div class="accordion-panel-stub"><slot /></div>',
    props: ['value'],
  },
  AccordionHeader: {
    template: '<div class="accordion-header-stub"><slot /></div>',
  },
  AccordionContent: {
    template: '<div class="accordion-content-stub"><slot /></div>',
  },
}

function mountView() {
  const pinia = createPinia()
  return mount(AdminSettings, {
    global: { plugins: [i18n, pinia], stubs: globalStubs },
  })
}

describe('AdminSettings', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    const cfg = createMockConfig()
    vi.mocked(adminApi.getConfig).mockResolvedValue({ data: { data: cfg } } as any)
    vi.mocked(adminApi.getPublicConfig).mockResolvedValue({ data: { data: cfg } } as any)
    vi.mocked(adminApi.updateConfig).mockResolvedValue({ data: { data: cfg } } as any)
    vi.mocked(adminApi.updateModules).mockResolvedValue({ data: { data: cfg } } as any)
    vi.mocked(adminApi.updateMaintenance).mockResolvedValue({ data: { data: cfg } } as any)
    vi.mocked(adminApi.testLdapConnection).mockResolvedValue({
      data: { data: { success: true, message: 'OK' } },
    } as any)
  })

  it('should mount without crashing', () => {
    const wrapper = mountView()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('should render the page title', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('h1').text()).toContain('Einstellungen')
    wrapper.unmount()
  })

  it('should fetch admin config on mount', async () => {
    mountView()
    await flushPromises()
    expect(adminApi.getConfig).toHaveBeenCalled()
  })

  it('should render accordion sections', async () => {
    const wrapper = mountView()
    await flushPromises()
    const panels = wrapper.findAll('.accordion-panel-stub')
    expect(panels.length).toBe(5) // general, communication, integration, security, hours
    wrapper.unmount()
  })

  it('should render section headers', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Allgemein')
    expect(wrapper.text()).toContain('Kommunikation')
    expect(wrapper.text()).toContain('Integrationen')
    expect(wrapper.text()).toContain('Sicherheit')
    expect(wrapper.text()).toContain('Stunden')
    wrapper.unmount()
  })

  it('should render language settings', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Sprache')
    expect(wrapper.text()).toContain('Standardsprache')
    wrapper.unmount()
  })

  it('should render registration settings', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Registrierung')
    expect(wrapper.text()).toContain('Benutzer-Freigabe')
    wrapper.unmount()
  })

  it('should render maintenance section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Wartungsmodus')
    wrapper.unmount()
  })

  it('should render 2FA section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('2-Faktor-Auth.')
    wrapper.unmount()
  })

  it('should render hours section with fields', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Gesamtstunden')
    expect(wrapper.text()).toContain('Putzstunden')
    wrapper.unmount()
  })

  it('should render LDAP section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('LDAP / Active Directory')
    wrapper.unmount()
  })

  it('should render Bundesland / vacations section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Bundesland')
    expect(wrapper.text()).toContain('Schulferien')
    wrapper.unmount()
  })

  it('should populate form fields from config after mount', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.defaultLanguage).toBe('de')
    expect(vm.availableLanguages).toEqual(['de', 'en'])
    expect(vm.requireUserApproval).toBe(true)
    expect(vm.targetHoursPerFamily).toBe(30)
    expect(vm.targetCleaningHours).toBe(3)
    expect(vm.bundesland).toBe('BY')
    expect(vm.twoFactorMode).toBe('DISABLED')
    wrapper.unmount()
  })

  it('should call updateModules when saveSettings is invoked', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.saveSettings()
    await flushPromises()
    expect(adminApi.updateModules).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('should call updateMaintenance when saveMaintenance is invoked', async () => {
    const mockAdd = vi.fn()
    vi.mocked(useToast).mockReturnValue({ add: mockAdd } as any)
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.maintenanceEnabled = true
    vm.maintenanceMessage = 'We are updating'
    await vm.saveMaintenance()
    await flushPromises()
    expect(adminApi.updateMaintenance).toHaveBeenCalledWith(true, 'We are updating')
    expect(mockAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    wrapper.unmount()
  })

  it('should show error toast when saveMaintenance fails', async () => {
    vi.mocked(adminApi.updateMaintenance).mockRejectedValue({ response: { data: { message: 'Error' } } })
    const mockAdd = vi.fn()
    vi.mocked(useToast).mockReturnValue({ add: mockAdd } as any)
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.saveMaintenance()
    await flushPromises()
    expect(mockAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    wrapper.unmount()
  })

  it('should call updateConfig when saveHoursConfig is invoked', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.saveHoursConfig()
    await flushPromises()
    expect(adminApi.updateConfig).toHaveBeenCalledWith(
      expect.objectContaining({
        targetHoursPerFamily: 30,
        targetCleaningHours: 3,
      }),
    )
    wrapper.unmount()
  })

  it('should add a vacation entry when addVacation is called', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = vm.schoolVacations.length
    vm.addVacation()
    expect(vm.schoolVacations.length).toBe(before + 1)
    wrapper.unmount()
  })

  it('should remove a vacation entry when removeVacation is called', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schoolVacations = [{ name: 'Test', from: '2025-01-01', to: '2025-01-10' }]
    vm.removeVacation(0)
    expect(vm.schoolVacations.length).toBe(0)
    wrapper.unmount()
  })

  it('should load predefined vacations for bundesland', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.bundesland = 'BY'
    vm.loadVacationsForBundesland()
    expect(vm.schoolVacations.length).toBe(2)
    expect(vm.schoolVacations[0].name).toBe('Herbstferien')
    wrapper.unmount()
  })

  it('should render impersonation toggle in security section', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Impersonation')
    expect(wrapper.text()).toContain('Benutzer imitieren')
    wrapper.unmount()
  })

  it('should call updateModules when saveImpersonationToggle is invoked', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.saveImpersonationToggle(true)
    await flushPromises()
    expect(adminApi.updateModules).toHaveBeenCalledWith(expect.objectContaining({ impersonation: true }))
    wrapper.unmount()
  })

  it('should render save buttons', async () => {
    const wrapper = mountView()
    await flushPromises()
    const buttons = wrapper.findAll('.button-stub')
    expect(buttons.length).toBeGreaterThan(0)
    wrapper.unmount()
  })
})
