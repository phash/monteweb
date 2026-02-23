import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { setActivePinia, createPinia } from 'pinia'
import AdminCsvImport from '@/views/admin/AdminCsvImport.vue'

vi.mock('@/api/admin.api', () => ({
  adminApi: {
    uploadCsv: vi.fn(),
    downloadExampleCsv: vi.fn().mockResolvedValue({ data: new Blob(['test']) }),
  },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: {
    de: {
      common: {
        save: 'Speichern', cancel: 'Abbrechen', delete: 'Loeschen',
        next: 'Weiter', previous: 'Zurueck', description: 'Beschreibung',
      },
      admin: {
        dashboard: { users: 'Benutzer', usersDesc: 'Benutzer verwalten', families: 'Familien' },
      },
      csvImport: {
        title: 'CSV-Import',
        subtitle: 'Benutzer und Familien per CSV importieren',
        stepInstructions: 'Anleitung & Beispiel',
        stepUpload: 'Hochladen & Vorschau',
        stepImport: 'Import & Ergebnis',
        description: 'Mit dem CSV-Import koennen Sie viele Benutzer und Familien auf einmal anlegen.',
        formatTitle: 'CSV-Format',
        formatDescription: 'Die CSV-Datei muss folgende Spalten enthalten:',
        colEmail: 'E-Mail (erforderlich)',
        colFirstName: 'Vorname (erforderlich)',
        colLastName: 'Nachname (erforderlich)',
        colRole: 'Rolle (erforderlich)',
        colFamilyName: 'Familienname (optional)',
        colFamilyRole: 'Familienrolle (optional)',
        colSectionSlug: 'Schulbereich (optional)',
        separatorHint: 'Semikolon als Trennzeichen',
        encodingHint: 'UTF-8 Kodierung',
        passwordHint: 'Initialpasswort: changeme123',
        downloadExample: 'Beispiel-CSV herunterladen',
        exampleTitle: 'Beispiel-Vorschau',
        uploadTitle: 'CSV-Datei hochladen',
        uploadHint: 'Waehlen Sie eine CSV-Datei aus',
        uploadButton: 'Datei auswaehlen',
        dragDrop: 'CSV-Datei hierher ziehen',
        fileSelected: 'Datei: {name}',
        changeFile: 'Andere Datei',
        previewTitle: 'Vorschau',
        previewSummary: '{users} Benutzer und {families} Familien',
        previewErrors: '{count} Fehler',
        previewValid: 'Alle Zeilen gueltig',
        colRow: 'Zeile',
        colName: 'Name',
        colStatus: 'Status',
        statusValid: 'Gueltig',
        statusError: 'Fehler',
        importButton: 'Import starten',
        importing: 'Importiere...',
        resultTitle: 'Import-Ergebnis',
        resultSuccess: 'Import erfolgreich!',
        resultUsersCreated: '{count} Benutzer erstellt',
        resultFamiliesCreated: '{count} Familien erstellt',
        resultErrors: '{count} Fehler',
        resultNoErrors: 'Keine Fehler',
        startOver: 'Neuen Import',
        goToUsers: 'Zur Benutzerverwaltung',
        errorDetails: 'Fehlerdetails',
        noFile: 'Keine Datei',
        validating: 'Validiere...',
      },
    },
  },
})

const stubs = {
  PageTitle: { template: '<div class="page-title-stub">{{ title }}</div>', props: ['title'] },
  Stepper: { template: '<div class="stepper-stub"><slot /></div>', props: ['value', 'linear'] },
  StepList: { template: '<div class="step-list-stub"><slot /></div>' },
  StepPanels: { template: '<div class="step-panels-stub"><slot /></div>' },
  Step: { template: '<div class="step-stub"><slot /></div>', props: ['value'] },
  StepPanel: { template: '<div class="step-panel-stub"><slot v-bind="{ activateCallback: () => {} }" /></div>', props: ['value'] },
  Message: { template: '<div class="message-stub"><slot /></div>', props: ['severity', 'closable'] },
  Tag: { template: '<span class="tag-stub">{{ value }}</span>', props: ['value', 'severity'] },
  Button: { template: '<button class="button-stub" @click="$emit(\'click\')"><slot />{{ label }}</button>', props: ['label', 'icon', 'severity', 'loading', 'iconPos', 'size', 'as'], emits: ['click'] },
  DataTable: { template: '<div class="datatable-stub"><slot /></div>', props: ['value', 'size', 'stripedRows', 'paginator', 'rows', 'rowsPerPageOptions'] },
  Column: { template: '<div class="column-stub"></div>', props: ['field', 'header', 'style'] },
}

describe('AdminCsvImport', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render page title', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.find('.page-title-stub').text()).toContain('CSV-Import')
  })

  it('should render stepper with three steps', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    const steps = wrapper.findAll('.step-stub')
    expect(steps).toHaveLength(3)
  })

  it('should render step panels', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    const panels = wrapper.findAll('.step-panel-stub')
    expect(panels).toHaveLength(3)
  })

  it('should display format description', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('CSV-Format')
    expect(wrapper.text()).toContain('E-Mail (erforderlich)')
  })

  it('should show example download button', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('Beispiel-CSV herunterladen')
  })

  it('should render example table', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('Beispiel-Vorschau')
    const datatables = wrapper.findAll('.datatable-stub')
    expect(datatables.length).toBeGreaterThan(0)
  })

  it('should show password hint', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('changeme123')
  })

  it('should show upload area', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('CSV-Datei hochladen')
    expect(wrapper.find('.drop-zone').exists()).toBe(true)
  })

  it('should show drag and drop text', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('CSV-Datei hierher ziehen')
  })

  it('should display subtitle', () => {
    const wrapper = mount(AdminCsvImport, { global: { plugins: [i18n], stubs } })
    expect(wrapper.text()).toContain('Benutzer und Familien per CSV importieren')
  })
})
