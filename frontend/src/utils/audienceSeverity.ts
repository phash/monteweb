/**
 * Maps audience values to PrimeVue Tag severity.
 * Supports both folder/fotobox values (PARENTS_ONLY, STUDENTS_ONLY)
 * and discussion values (ELTERN, KINDER).
 */
export function audienceSeverity(audience: string): 'info' | 'warn' | 'secondary' {
  switch (audience) {
    case 'PARENTS_ONLY':
    case 'ELTERN':
      return 'warn'
    case 'STUDENTS_ONLY':
    case 'KINDER':
      return 'info'
    default:
      return 'secondary'
  }
}
