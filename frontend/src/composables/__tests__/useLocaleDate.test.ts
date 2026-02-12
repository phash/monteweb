import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref } from 'vue'

const mockLocale = ref('de')

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    locale: mockLocale,
    t: (key: string) => key,
  }),
}))

import { useLocaleDate } from '../useLocaleDate'

describe('useLocaleDate', () => {
  beforeEach(() => {
    mockLocale.value = 'de'
  })

  describe('getLocale', () => {
    it('should return de-DE for German locale', () => {
      const { getLocale } = useLocaleDate()
      expect(getLocale()).toBe('de-DE')
    })

    it('should return en-US for English locale', () => {
      mockLocale.value = 'en'
      const { getLocale } = useLocaleDate()
      expect(getLocale()).toBe('en-US')
    })
  })

  describe('formatDate', () => {
    it('should format a Date object', () => {
      const { formatDate } = useLocaleDate()
      const result = formatDate(new Date(2026, 0, 15))
      expect(result).toBeTruthy()
      expect(typeof result).toBe('string')
    })

    it('should format an ISO date string', () => {
      const { formatDate } = useLocaleDate()
      const result = formatDate('2026-01-15T10:00:00Z')
      expect(result).toBeTruthy()
    })

    it('should accept custom options', () => {
      const { formatDate } = useLocaleDate()
      const result = formatDate(new Date(2026, 0, 15), { weekday: 'long' })
      expect(result).toBeTruthy()
    })
  })

  describe('formatDateTime', () => {
    it('should format date and time', () => {
      const { formatDateTime } = useLocaleDate()
      const result = formatDateTime(new Date(2026, 5, 15, 14, 30))
      expect(result).toBeTruthy()
    })

    it('should work with string input', () => {
      const { formatDateTime } = useLocaleDate()
      const result = formatDateTime('2026-06-15T14:30:00Z')
      expect(result).toBeTruthy()
    })
  })

  describe('formatTime', () => {
    it('should format time only', () => {
      const { formatTime } = useLocaleDate()
      const result = formatTime(new Date(2026, 0, 15, 9, 45))
      expect(result).toBeTruthy()
    })
  })

  describe('formatShortDate', () => {
    it('should return a short date format', () => {
      const { formatShortDate } = useLocaleDate()
      const result = formatShortDate(new Date(2026, 2, 20))
      expect(result).toBeTruthy()
    })
  })

  describe('formatFullDate', () => {
    it('should return full date with weekday in German', () => {
      const { formatFullDate } = useLocaleDate()
      const result = formatFullDate(new Date(2026, 2, 20))
      // German full date should contain weekday and month name
      expect(result).toContain('2026')
    })

    it('should return full date in English', () => {
      mockLocale.value = 'en'
      const { formatFullDate } = useLocaleDate()
      const result = formatFullDate(new Date(2026, 2, 20))
      expect(result).toContain('2026')
    })
  })

  describe('formatCompactDateTime', () => {
    it('should return compact date time format', () => {
      const { formatCompactDateTime } = useLocaleDate()
      const result = formatCompactDateTime(new Date(2026, 5, 15, 14, 30))
      expect(result).toBeTruthy()
    })
  })

  describe('formatMonthYear', () => {
    it('should return month and year in German', () => {
      const { formatMonthYear } = useLocaleDate()
      const result = formatMonthYear(new Date(2026, 0, 1))
      expect(result).toContain('2026')
    })

    it('should return month and year in English', () => {
      mockLocale.value = 'en'
      const { formatMonthYear } = useLocaleDate()
      const result = formatMonthYear(new Date(2026, 0, 1))
      expect(result).toContain('2026')
      expect(result).toContain('January')
    })
  })

  describe('formatEventDate', () => {
    it('should format event date with short weekday', () => {
      const { formatEventDate } = useLocaleDate()
      const result = formatEventDate(new Date(2026, 5, 15))
      expect(result).toBeTruthy()
    })
  })
})
