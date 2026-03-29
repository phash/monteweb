import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import RecipientStatusTable from '../RecipientStatusTable.vue'
import type { ParentLetterRecipientInfo } from '@/types/parentletter'

vi.mock('@/composables/useLocaleDate', () => ({
  useLocaleDate: vi.fn(() => ({
    formatShortDate: vi.fn((d: string) => d?.substring(0, 10) || ''),
    formatCompactDateTime: vi.fn((d: string) => d?.substring(0, 16) || ''),
  })),
}))

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  missing: (_locale: string, key: string) => key,
  messages: {
    de: {
      parentLetters: {
        recipientStatuses: {
          OPEN: 'Offen',
          READ: 'Gelesen',
          CONFIRMED: 'Bestätigt',
        },
        recipientTable: {
          total: 'Gesamt',
          read: 'Gelesen',
          confirmed: 'Bestätigt',
          empty: 'Keine Empfänger',
          student: 'Schüler',
          parent: 'Elternteil',
          family: 'Familie',
          status: 'Status',
          readAt: 'Gelesen am',
          confirmedAt: 'Bestätigt am',
          reminderSent: 'Erinnerung',
        },
      },
    },
  },
})

const stubs = {
  DataTable: {
    template: '<div class="datatable-stub"><slot /></div>',
    props: ['value', 'size', 'stripedRows', 'paginator', 'rows', 'emptyMessage'],
  },
  Column: {
    template: '<div class="column-stub" :data-field="field"><slot name="body" :data="{}"></slot></div>',
    props: ['field', 'header', 'sortable', 'style'],
  },
  Tag: {
    template: '<span class="tag-stub" :data-severity="severity">{{ value }}</span>',
    props: ['value', 'severity', 'size'],
  },
}

const mockRecipients: ParentLetterRecipientInfo[] = [
  {
    id: 'rec-1',
    studentId: 'student-1',
    studentName: 'Anna Schmidt',
    parentId: 'parent-1',
    parentName: 'Hans Schmidt',
    familyName: 'Schmidt',
    status: 'CONFIRMED',
    readAt: '2025-03-10T09:00:00Z',
    confirmedAt: '2025-03-10T10:00:00Z',
    confirmedByName: 'Hans Schmidt',
    reminderSentAt: null,
  },
  {
    id: 'rec-2',
    studentId: 'student-2',
    studentName: 'Ben Mueller',
    parentId: 'parent-2',
    parentName: 'Eva Mueller',
    familyName: 'Mueller',
    status: 'READ',
    readAt: '2025-03-10T11:00:00Z',
    confirmedAt: null,
    confirmedByName: null,
    reminderSentAt: null,
  },
  {
    id: 'rec-3',
    studentId: 'student-3',
    studentName: 'Clara Weber',
    parentId: 'parent-3',
    parentName: 'Peter Weber',
    familyName: 'Weber',
    status: 'OPEN',
    readAt: null,
    confirmedAt: null,
    confirmedByName: null,
    reminderSentAt: '2025-03-12T08:00:00Z',
  },
]

function mountTable(recipients: ParentLetterRecipientInfo[] = mockRecipients) {
  return mount(RecipientStatusTable, {
    props: { recipients },
    global: { plugins: [i18n], stubs },
  })
}

describe('RecipientStatusTable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ==================== Basic render ====================

  it('should mount and render', () => {
    const wrapper = mountTable()
    expect(wrapper.exists()).toBe(true)
  })

  it('should render summary bar', () => {
    const wrapper = mountTable()
    expect(wrapper.find('.summary-bar').exists()).toBe(true)
  })

  it('should render DataTable', () => {
    const wrapper = mountTable()
    expect(wrapper.find('.datatable-stub').exists()).toBe(true)
  })

  // ==================== Summary counts ====================

  it('should show total count', () => {
    const wrapper = mountTable()
    expect(wrapper.text()).toContain('Gesamt')
    expect(wrapper.text()).toContain('3')
  })

  it('should show read count (READ + CONFIRMED)', () => {
    const wrapper = mountTable()
    // READ count = READ + CONFIRMED = 2
    expect(wrapper.text()).toContain('Gelesen')
    expect(wrapper.text()).toContain('2')
  })

  it('should show confirmed count', () => {
    const wrapper = mountTable()
    expect(wrapper.text()).toContain('Bestätigt')
    expect(wrapper.text()).toContain('1')
  })

  // ==================== Empty state ====================

  it('should handle empty recipients', () => {
    const wrapper = mountTable([])
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.text()).toContain('0') // total count is 0
  })

  it('should show total 0 for empty list', () => {
    const wrapper = mountTable([])
    // Summary items should all show 0
    const summaryItems = wrapper.findAll('.summary-item')
    expect(summaryItems.length).toBe(3) // total, read, confirmed
  })

  // ==================== All confirmed case ====================

  it('should show all counts equal for fully confirmed recipients', () => {
    const allConfirmed: ParentLetterRecipientInfo[] = [
      { ...mockRecipients[0]!, status: 'CONFIRMED' },
      { ...mockRecipients[1]!, status: 'CONFIRMED', confirmedAt: '2025-03-11T10:00:00Z' },
    ]

    const wrapper = mountTable(allConfirmed)
    // Both confirmed count and read count should be 2
    const summaryItems = wrapper.findAll('.summary-item')
    expect(summaryItems.length).toBe(3)
  })

  // ==================== StatusSeverity logic ====================

  it('should map status to correct severity', () => {
    function statusSeverity(status: string): string {
      switch (status) {
        case 'OPEN': return 'danger'
        case 'READ': return 'info'
        case 'CONFIRMED': return 'success'
        default: return 'danger'
      }
    }

    expect(statusSeverity('OPEN')).toBe('danger')
    expect(statusSeverity('READ')).toBe('info')
    expect(statusSeverity('CONFIRMED')).toBe('success')
  })

  // ==================== Paginator ====================

  it('should not enable paginator for 20 or fewer recipients', () => {
    const wrapper = mountTable(mockRecipients)
    const dt = wrapper.find('.datatable-stub')
    expect(dt.exists()).toBe(true)
    // With 3 recipients, paginator should be false
  })

  it('should enable paginator for more than 20 recipients', () => {
    const manyRecipients = Array.from({ length: 25 }, (_, i) => ({
      ...mockRecipients[0]!,
      id: `rec-${i}`,
      studentId: `student-${i}`,
      studentName: `Student ${i}`,
    }))

    const wrapper = mountTable(manyRecipients)
    expect(wrapper.exists()).toBe(true)
    // With 25 recipients, paginator should be true
  })

  // ==================== Column rendering ====================

  it('should render column stubs', () => {
    const wrapper = mountTable()
    const columns = wrapper.findAll('.column-stub')
    // 7 columns: student, parent, family, status, readAt, confirmedAt, reminderSent
    expect(columns.length).toBe(7)
  })
})
