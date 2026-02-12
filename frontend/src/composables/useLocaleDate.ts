import { useI18n } from 'vue-i18n'

/**
 * Composable that returns locale-aware date formatting functions.
 * Uses the current i18n locale instead of hardcoded 'de-DE'.
 */
export function useLocaleDate() {
  const { locale } = useI18n()

  function getLocale(): string {
    return locale.value === 'de' ? 'de-DE' : 'en-US'
  }

  function formatDate(date: string | Date, options?: Intl.DateTimeFormatOptions): string {
    const d = typeof date === 'string' ? new Date(date) : date
    return d.toLocaleDateString(getLocale(), options)
  }

  function formatDateTime(date: string | Date, options?: Intl.DateTimeFormatOptions): string {
    const d = typeof date === 'string' ? new Date(date) : date
    return d.toLocaleString(getLocale(), options)
  }

  function formatTime(date: string | Date, options?: Intl.DateTimeFormatOptions): string {
    const d = typeof date === 'string' ? new Date(date) : date
    return d.toLocaleTimeString(getLocale(), options)
  }

  function formatShortDate(date: string | Date): string {
    return formatDate(date)
  }

  function formatFullDate(date: string | Date): string {
    return formatDate(date, {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    })
  }

  function formatCompactDateTime(date: string | Date): string {
    return formatDateTime(date, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  function formatMonthYear(date: Date): string {
    return date.toLocaleDateString(getLocale(), { month: 'long', year: 'numeric' })
  }

  function formatEventDate(date: string | Date): string {
    return formatDate(date, { weekday: 'short', day: '2-digit', month: '2-digit' })
  }

  return {
    getLocale,
    formatDate,
    formatDateTime,
    formatTime,
    formatShortDate,
    formatFullDate,
    formatCompactDateTime,
    formatMonthYear,
    formatEventDate,
  }
}
