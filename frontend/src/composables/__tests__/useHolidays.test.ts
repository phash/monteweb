import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/admin.api', () => ({
  adminApi: { getPublicConfig: vi.fn() },
}))
vi.mock('@/api/client', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}))

import { useAdminStore } from '@/stores/admin'
import { useHolidays } from '../useHolidays'

describe('useHolidays', () => {
  let adminStore: ReturnType<typeof useAdminStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    adminStore = useAdminStore()
    adminStore.config = { bundesland: 'BY' } as any
  })

  describe('Easter calculation (Gauss algorithm)', () => {
    // Well-known Easter dates for verification
    const knownEasterDates: [number, string][] = [
      [2020, '2020-04-12'],
      [2021, '2021-04-04'],
      [2022, '2022-04-17'],
      [2023, '2023-04-09'],
      [2024, '2024-03-31'],
      [2025, '2025-04-20'],
      [2026, '2026-04-05'],
      [2027, '2027-03-28'],
      [2030, '2030-04-21'],
    ]

    it.each(knownEasterDates)(
      'should calculate correct Easter-based Ostermontag for year %i',
      (year, easterDate) => {
        const yearRef = ref(year)
        const { holidays } = useHolidays(yearRef)

        // Ostermontag is Easter Sunday + 1
        const easterSunday = new Date(easterDate)
        const expectedOstermontag = new Date(easterSunday)
        expectedOstermontag.setDate(expectedOstermontag.getDate() + 1)
        const expectedStr = `${expectedOstermontag.getFullYear()}-${String(expectedOstermontag.getMonth() + 1).padStart(2, '0')}-${String(expectedOstermontag.getDate()).padStart(2, '0')}`

        const ostermontag = holidays.value.find(
          h => h.name === 'Ostermontag' && h.date.startsWith(String(year)),
        )
        expect(ostermontag).toBeDefined()
        expect(ostermontag!.date).toBe(expectedStr)
      },
    )

    it.each(knownEasterDates)(
      'should calculate correct Karfreitag for year %i (Easter - 2)',
      (year, easterDate) => {
        const yearRef = ref(year)
        const { holidays } = useHolidays(yearRef)

        const easterSunday = new Date(easterDate)
        const expectedKarfreitag = new Date(easterSunday)
        expectedKarfreitag.setDate(expectedKarfreitag.getDate() - 2)
        const expectedStr = `${expectedKarfreitag.getFullYear()}-${String(expectedKarfreitag.getMonth() + 1).padStart(2, '0')}-${String(expectedKarfreitag.getDate()).padStart(2, '0')}`

        const karfreitag = holidays.value.find(
          h => h.name === 'Karfreitag' && h.date.startsWith(String(year)),
        )
        expect(karfreitag).toBeDefined()
        expect(karfreitag!.date).toBe(expectedStr)
      },
    )

    it('should calculate Christi Himmelfahrt as Easter + 39', () => {
      const yearRef = ref(2025)
      const { holidays } = useHolidays(yearRef)
      // Easter 2025 = April 20, so Himmelfahrt = May 29
      const himmelfahrt = holidays.value.find(
        h => h.name === 'Christi Himmelfahrt' && h.date.startsWith('2025'),
      )
      expect(himmelfahrt).toBeDefined()
      expect(himmelfahrt!.date).toBe('2025-05-29')
    })

    it('should calculate Pfingstmontag as Easter + 50', () => {
      const yearRef = ref(2025)
      const { holidays } = useHolidays(yearRef)
      // Easter 2025 = April 20, so Pfingstmontag = June 9
      const pfingstmontag = holidays.value.find(
        h => h.name === 'Pfingstmontag' && h.date.startsWith('2025'),
      )
      expect(pfingstmontag).toBeDefined()
      expect(pfingstmontag!.date).toBe('2025-06-09')
    })
  })

  describe('fixed holidays (all states)', () => {
    it('should include Neujahr', () => {
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const neujahr = holidays.value.find(h => h.date === '2026-01-01')
      expect(neujahr).toBeDefined()
      expect(neujahr!.name).toBe('Neujahr')
    })

    it('should include Tag der Arbeit', () => {
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const mai = holidays.value.find(h => h.date === '2026-05-01')
      expect(mai).toBeDefined()
      expect(mai!.name).toBe('Tag der Arbeit')
    })

    it('should include Tag der Deutschen Einheit', () => {
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const einheit = holidays.value.find(h => h.date === '2026-10-03')
      expect(einheit).toBeDefined()
      expect(einheit!.name).toBe('Tag der Deutschen Einheit')
    })

    it('should include both Weihnachtsfeiertage', () => {
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const w1 = holidays.value.find(h => h.date === '2026-12-25')
      const w2 = holidays.value.find(h => h.date === '2026-12-26')
      expect(w1).toBeDefined()
      expect(w1!.name).toBe('1. Weihnachtsfeiertag')
      expect(w2).toBeDefined()
      expect(w2!.name).toBe('2. Weihnachtsfeiertag')
    })
  })

  describe('Bundesland-specific holidays', () => {
    it('should include Heilige Drei Koenige for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const dreiKoenige = holidays.value.find(h => h.date === '2026-01-06')
      expect(dreiKoenige).toBeDefined()
      expect(dreiKoenige!.name).toBe('Heilige Drei Könige')
    })

    it('should NOT include Heilige Drei Koenige for HH', () => {
      adminStore.config = { bundesland: 'HH' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const dreiKoenige = holidays.value.find(
        h => h.date === '2026-01-06' && h.name === 'Heilige Drei Könige',
      )
      expect(dreiKoenige).toBeUndefined()
    })

    it('should include Fronleichnam for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2025)
      const { holidays } = useHolidays(yearRef)
      // Easter 2025 = April 20, Fronleichnam = Easter + 60 = June 19
      const fronleichnam = holidays.value.find(
        h => h.name === 'Fronleichnam' && h.date.startsWith('2025'),
      )
      expect(fronleichnam).toBeDefined()
      expect(fronleichnam!.date).toBe('2025-06-19')
    })

    it('should NOT include Fronleichnam for HH', () => {
      adminStore.config = { bundesland: 'HH' } as any
      const yearRef = ref(2025)
      const { holidays } = useHolidays(yearRef)
      const fronleichnam = holidays.value.find(
        h => h.name === 'Fronleichnam' && h.date.startsWith('2025'),
      )
      expect(fronleichnam).toBeUndefined()
    })

    it('should include Mariae Himmelfahrt for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const himmelfahrt = holidays.value.find(h => h.date === '2026-08-15')
      expect(himmelfahrt).toBeDefined()
      expect(himmelfahrt!.name).toBe('Mariä Himmelfahrt')
    })

    it('should NOT include Mariae Himmelfahrt for NW', () => {
      adminStore.config = { bundesland: 'NW' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const himmelfahrt = holidays.value.find(h => h.date === '2026-08-15')
      expect(himmelfahrt).toBeUndefined()
    })

    it('should include Reformationstag for BB', () => {
      adminStore.config = { bundesland: 'BB' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const reformationstag = holidays.value.find(h => h.date === '2026-10-31')
      expect(reformationstag).toBeDefined()
      expect(reformationstag!.name).toBe('Reformationstag')
    })

    it('should NOT include Reformationstag for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const reformationstag = holidays.value.find(
        h => h.date === '2026-10-31' && h.name === 'Reformationstag',
      )
      expect(reformationstag).toBeUndefined()
    })

    it('should include Allerheiligen for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const allerheiligen = holidays.value.find(h => h.date === '2026-11-01')
      expect(allerheiligen).toBeDefined()
      expect(allerheiligen!.name).toBe('Allerheiligen')
    })

    it('should NOT include Allerheiligen for HH', () => {
      adminStore.config = { bundesland: 'HH' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const allerheiligen = holidays.value.find(h => h.date === '2026-11-01')
      expect(allerheiligen).toBeUndefined()
    })

    it('should include Internationaler Frauentag for BE only', () => {
      adminStore.config = { bundesland: 'BE' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const frauentag = holidays.value.find(
        h => h.date === '2026-03-08' && h.name === 'Internationaler Frauentag',
      )
      expect(frauentag).toBeDefined()
    })

    it('should NOT include Internationaler Frauentag for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const frauentag = holidays.value.find(
        h => h.date === '2026-03-08' && h.name === 'Internationaler Frauentag',
      )
      expect(frauentag).toBeUndefined()
    })

    it('should include Weltkindertag for TH only', () => {
      adminStore.config = { bundesland: 'TH' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const weltkindertag = holidays.value.find(
        h => h.date === '2026-09-20' && h.name === 'Weltkindertag',
      )
      expect(weltkindertag).toBeDefined()
    })

    it('should NOT include Weltkindertag for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const weltkindertag = holidays.value.find(
        h => h.name === 'Weltkindertag' && h.date.startsWith('2026'),
      )
      expect(weltkindertag).toBeUndefined()
    })
  })

  describe('Buss-und-Bettag (Saxony only)', () => {
    it('should include Buss-und-Bettag for SN', () => {
      adminStore.config = { bundesland: 'SN' } as any
      const yearRef = ref(2025)
      const { holidays } = useHolidays(yearRef)
      const bussUndBettag = holidays.value.find(
        h => h.name === 'Buß- und Bettag' && h.date.startsWith('2025'),
      )
      expect(bussUndBettag).toBeDefined()
      // 2025: Nov 20 (Wednesday before last Sunday before Advent)
      expect(bussUndBettag!.date).toBe('2025-11-20')
    })

    it('should calculate Buss-und-Bettag correctly for 2026', () => {
      adminStore.config = { bundesland: 'SN' } as any
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)
      const bussUndBettag = holidays.value.find(
        h => h.name === 'Buß- und Bettag' && h.date.startsWith('2026'),
      )
      expect(bussUndBettag).toBeDefined()
      // 2026: Nov 19
      expect(bussUndBettag!.date).toBe('2026-11-19')
    })

    it('should NOT include Buss-und-Bettag for BY', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2025)
      const { holidays } = useHolidays(yearRef)
      const bussUndBettag = holidays.value.find(
        h => h.name === 'Buß- und Bettag' && h.date.startsWith('2025'),
      )
      expect(bussUndBettag).toBeUndefined()
    })
  })

  describe('multi-year computation', () => {
    it('should include holidays from previous, current, and next year', () => {
      const yearRef = ref(2026)
      const { holidays } = useHolidays(yearRef)

      const has2025 = holidays.value.some(h => h.date.startsWith('2025'))
      const has2026 = holidays.value.some(h => h.date.startsWith('2026'))
      const has2027 = holidays.value.some(h => h.date.startsWith('2027'))

      expect(has2025).toBe(true)
      expect(has2026).toBe(true)
      expect(has2027).toBe(true)
    })
  })

  describe('bundesland default', () => {
    it('should default to BY when config has no bundesland', () => {
      adminStore.config = {} as any
      const yearRef = ref(2026)
      const { bundesland } = useHolidays(yearRef)
      expect(bundesland.value).toBe('BY')
    })

    it('should default to BY when config is null', () => {
      adminStore.config = null as any
      const yearRef = ref(2026)
      const { bundesland } = useHolidays(yearRef)
      expect(bundesland.value).toBe('BY')
    })
  })

  describe('isHoliday', () => {
    it('should return holiday name when date is a holiday', () => {
      const yearRef = ref(2026)
      const { isHoliday } = useHolidays(yearRef)
      const result = isHoliday(new Date(2026, 0, 1)) // Jan 1
      expect(result).toBe('Neujahr')
    })

    it('should return null when date is not a holiday', () => {
      const yearRef = ref(2026)
      const { isHoliday } = useHolidays(yearRef)
      const result = isHoliday(new Date(2026, 0, 2)) // Jan 2
      expect(result).toBeNull()
    })
  })

  describe('vacations', () => {
    it('should return school vacations from admin config', () => {
      adminStore.config = {
        bundesland: 'BY',
        schoolVacations: [
          { name: 'Sommerferien', from: '2026-07-30', to: '2026-09-14' },
        ],
      } as any
      const yearRef = ref(2026)
      const { vacations } = useHolidays(yearRef)
      expect(vacations.value).toHaveLength(1)
      expect(vacations.value[0].name).toBe('Sommerferien')
    })

    it('should return empty array when no vacations configured', () => {
      adminStore.config = { bundesland: 'BY' } as any
      const yearRef = ref(2026)
      const { vacations } = useHolidays(yearRef)
      expect(vacations.value).toEqual([])
    })
  })

  describe('isVacation', () => {
    it('should return vacation name when date is within vacation range', () => {
      adminStore.config = {
        bundesland: 'BY',
        schoolVacations: [
          { name: 'Sommerferien', from: '2026-07-30', to: '2026-09-14' },
        ],
      } as any
      const yearRef = ref(2026)
      const { isVacation } = useHolidays(yearRef)
      const result = isVacation(new Date(2026, 7, 15)) // Aug 15
      expect(result).toBe('Sommerferien')
    })

    it('should return vacation name on boundary dates', () => {
      adminStore.config = {
        bundesland: 'BY',
        schoolVacations: [
          { name: 'Herbstferien', from: '2026-10-26', to: '2026-10-30' },
        ],
      } as any
      const yearRef = ref(2026)
      const { isVacation } = useHolidays(yearRef)
      expect(isVacation(new Date(2026, 9, 26))).toBe('Herbstferien') // start
      expect(isVacation(new Date(2026, 9, 30))).toBe('Herbstferien') // end
    })

    it('should return null when date is not in any vacation', () => {
      adminStore.config = {
        bundesland: 'BY',
        schoolVacations: [
          { name: 'Sommerferien', from: '2026-07-30', to: '2026-09-14' },
        ],
      } as any
      const yearRef = ref(2026)
      const { isVacation } = useHolidays(yearRef)
      const result = isVacation(new Date(2026, 0, 15)) // Jan 15
      expect(result).toBeNull()
    })
  })

  describe('getDateClass', () => {
    it('should return "mw-holiday" for a holiday', () => {
      const yearRef = ref(2026)
      const { getDateClass } = useHolidays(yearRef)
      // Jan 1 = Neujahr (month is 0-indexed in PrimeVue date object)
      const result = getDateClass({ day: 1, month: 0, year: 2026 })
      expect(result).toBe('mw-holiday')
    })

    it('should return "mw-vacation" for a vacation day', () => {
      adminStore.config = {
        bundesland: 'BY',
        schoolVacations: [
          { name: 'Sommerferien', from: '2026-08-01', to: '2026-08-31' },
        ],
      } as any
      const yearRef = ref(2026)
      const { getDateClass } = useHolidays(yearRef)
      // Use Aug 10, not Aug 15 (Mariae Himmelfahrt in BY)
      const result = getDateClass({ day: 10, month: 7, year: 2026 })
      expect(result).toBe('mw-vacation')
    })

    it('should return empty string for a normal day', () => {
      const yearRef = ref(2026)
      const { getDateClass } = useHolidays(yearRef)
      const result = getDateClass({ day: 2, month: 0, year: 2026 })
      expect(result).toBe('')
    })

    it('should prioritize holiday over vacation', () => {
      adminStore.config = {
        bundesland: 'BY',
        schoolVacations: [
          { name: 'Weihnachtsferien', from: '2026-12-23', to: '2027-01-06' },
        ],
      } as any
      const yearRef = ref(2026)
      const { getDateClass } = useHolidays(yearRef)
      // Dec 25 is both a holiday and within Weihnachtsferien
      const result = getDateClass({ day: 25, month: 11, year: 2026 })
      expect(result).toBe('mw-holiday')
    })
  })

  describe('getDateTooltip', () => {
    it('should return holiday name for a holiday', () => {
      const yearRef = ref(2026)
      const { getDateTooltip } = useHolidays(yearRef)
      const result = getDateTooltip({ day: 1, month: 0, year: 2026 })
      expect(result).toBe('Neujahr')
    })

    it('should return vacation name for a vacation day', () => {
      adminStore.config = {
        bundesland: 'BY',
        schoolVacations: [
          { name: 'Sommerferien', from: '2026-08-01', to: '2026-08-31' },
        ],
      } as any
      const yearRef = ref(2026)
      const { getDateTooltip } = useHolidays(yearRef)
      // Use Aug 10, not Aug 15 (Mariae Himmelfahrt in BY)
      const result = getDateTooltip({ day: 10, month: 7, year: 2026 })
      expect(result).toBe('Sommerferien')
    })

    it('should return null for a normal day', () => {
      const yearRef = ref(2026)
      const { getDateTooltip } = useHolidays(yearRef)
      const result = getDateTooltip({ day: 2, month: 0, year: 2026 })
      expect(result).toBeNull()
    })
  })
})
