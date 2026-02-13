import { ref, computed, watch } from 'vue'
import type { Ref } from 'vue'
import { useAdminStore } from '@/stores/admin'

interface Holiday {
  date: string // YYYY-MM-DD
  name: string
}

interface Vacation {
  name: string
  from: string // YYYY-MM-DD
  to: string   // YYYY-MM-DD
}

// Bundesland codes and which holidays they observe
const FRONLEICHNAM_STATES = ['BY', 'BW', 'HE', 'NW', 'RP', 'SL', 'SN', 'TH']
const DREI_KOENIGE_STATES = ['BY', 'BW', 'ST']
const MARIAE_HIMMELFAHRT_STATES = ['BY', 'SL']
const REFORMATIONSTAG_STATES = ['BB', 'HB', 'HH', 'MV', 'NI', 'SN', 'ST', 'SH', 'TH']
const ALLERHEILIGEN_STATES = ['BY', 'BW', 'NW', 'RP', 'SL']
const BUSS_BETTAG_STATES = ['SN']
// Internationaler Frauentag only in Berlin
const FRAUENTAG_STATES = ['BE']
// Weltkindertag only in Thuringia
const WELTKINDERTAG_STATES = ['TH']

/**
 * Gauss Easter algorithm: returns Easter Sunday for a given year.
 */
function computeEasterSunday(year: number): Date {
  const a = year % 19
  const b = Math.floor(year / 100)
  const c = year % 100
  const d = Math.floor(b / 4)
  const e = b % 4
  const f = Math.floor((b + 8) / 25)
  const g = Math.floor((b - f + 1) / 3)
  const h = (19 * a + b - d - g + 15) % 30
  const i = Math.floor(c / 4)
  const k = c % 4
  const l = (32 + 2 * e + 2 * i - h - k) % 7
  const m = Math.floor((a + 11 * h + 22 * l) / 451)
  const month = Math.floor((h + l - 7 * m + 114) / 31)
  const day = ((h + l - 7 * m + 114) % 31) + 1
  return new Date(year, month - 1, day)
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/**
 * Compute Buss-und-Bettag: Wednesday before the last Sunday before Advent
 * (i.e., 11 days before the first Advent Sunday, which is the 4th Sunday before Dec 25)
 */
function computeBussUndBettag(year: number): Date {
  // Find the first Advent Sunday: the Sunday closest to Nov 30
  const dec25 = new Date(year, 11, 25)
  const dayOfWeek = dec25.getDay() // 0=Sun
  // 4th Sunday before Dec 25
  const advent1 = new Date(year, 11, 25 - ((dayOfWeek + 6) % 7) - 21)
  // Buss-und-Bettag is 11 days before 1st Advent
  return addDays(advent1, -11)
}

function getHolidaysForYear(year: number, bundesland: string): Holiday[] {
  const holidays: Holiday[] = []

  // Fixed holidays (all states)
  holidays.push({ date: `${year}-01-01`, name: 'Neujahr' })
  holidays.push({ date: `${year}-05-01`, name: 'Tag der Arbeit' })
  holidays.push({ date: `${year}-10-03`, name: 'Tag der Deutschen Einheit' })
  holidays.push({ date: `${year}-12-25`, name: '1. Weihnachtsfeiertag' })
  holidays.push({ date: `${year}-12-26`, name: '2. Weihnachtsfeiertag' })

  // State-specific fixed holidays
  if (DREI_KOENIGE_STATES.includes(bundesland)) {
    holidays.push({ date: `${year}-01-06`, name: 'Heilige Drei Könige' })
  }
  if (FRAUENTAG_STATES.includes(bundesland)) {
    holidays.push({ date: `${year}-03-08`, name: 'Internationaler Frauentag' })
  }
  if (MARIAE_HIMMELFAHRT_STATES.includes(bundesland)) {
    holidays.push({ date: `${year}-08-15`, name: 'Mariä Himmelfahrt' })
  }
  if (WELTKINDERTAG_STATES.includes(bundesland)) {
    holidays.push({ date: `${year}-09-20`, name: 'Weltkindertag' })
  }
  if (REFORMATIONSTAG_STATES.includes(bundesland)) {
    holidays.push({ date: `${year}-10-31`, name: 'Reformationstag' })
  }
  if (ALLERHEILIGEN_STATES.includes(bundesland)) {
    holidays.push({ date: `${year}-11-01`, name: 'Allerheiligen' })
  }

  // Easter-based holidays
  const easter = computeEasterSunday(year)
  holidays.push({ date: formatDate(addDays(easter, -2)), name: 'Karfreitag' })
  holidays.push({ date: formatDate(addDays(easter, 1)), name: 'Ostermontag' })
  holidays.push({ date: formatDate(addDays(easter, 39)), name: 'Christi Himmelfahrt' })
  holidays.push({ date: formatDate(addDays(easter, 50)), name: 'Pfingstmontag' })

  if (FRONLEICHNAM_STATES.includes(bundesland)) {
    holidays.push({ date: formatDate(addDays(easter, 60)), name: 'Fronleichnam' })
  }

  // Buss-und-Bettag (Saxony only)
  if (BUSS_BETTAG_STATES.includes(bundesland)) {
    holidays.push({ date: formatDate(computeBussUndBettag(year)), name: 'Buß- und Bettag' })
  }

  return holidays
}

export function useHolidays(year: Ref<number>) {
  const adminStore = useAdminStore()
  const bundesland = computed(() => adminStore.config?.bundesland || 'BY')

  const holidays = computed(() => {
    const bl = bundesland.value
    const y = year.value
    // Compute for current year and adjacent years for date pickers
    return [
      ...getHolidaysForYear(y - 1, bl),
      ...getHolidaysForYear(y, bl),
      ...getHolidaysForYear(y + 1, bl),
    ]
  })

  const vacations = computed<Vacation[]>(() => {
    return (adminStore.config?.schoolVacations || []) as Vacation[]
  })

  function isHoliday(date: Date): string | null {
    const dateStr = formatDate(date)
    const h = holidays.value.find(h => h.date === dateStr)
    return h ? h.name : null
  }

  function isVacation(date: Date): string | null {
    const dateStr = formatDate(date)
    for (const v of vacations.value) {
      if (dateStr >= v.from && dateStr <= v.to) {
        return v.name
      }
    }
    return null
  }

  /**
   * Returns props for PrimeVue DatePicker date template slot.
   * Usage in template: use the `date` slot to highlight holidays/vacations.
   */
  function getDateClass(date: { day: number; month: number; year: number }): string {
    const d = new Date(date.year, date.month, date.day)
    if (isHoliday(d)) return 'mw-holiday'
    if (isVacation(d)) return 'mw-vacation'
    return ''
  }

  function getDateTooltip(date: { day: number; month: number; year: number }): string | null {
    const d = new Date(date.year, date.month, date.day)
    return isHoliday(d) || isVacation(d)
  }

  return {
    holidays,
    vacations,
    bundesland,
    isHoliday,
    isVacation,
    getDateClass,
    getDateTooltip,
  }
}
