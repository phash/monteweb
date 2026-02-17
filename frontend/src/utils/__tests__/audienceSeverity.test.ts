import { describe, it, expect } from 'vitest'
import { audienceSeverity } from '@/utils/audienceSeverity'

describe('audienceSeverity', () => {
  it('should return "warn" for PARENTS_ONLY', () => {
    expect(audienceSeverity('PARENTS_ONLY')).toBe('warn')
  })

  it('should return "warn" for ELTERN', () => {
    expect(audienceSeverity('ELTERN')).toBe('warn')
  })

  it('should return "info" for STUDENTS_ONLY', () => {
    expect(audienceSeverity('STUDENTS_ONLY')).toBe('info')
  })

  it('should return "info" for KINDER', () => {
    expect(audienceSeverity('KINDER')).toBe('info')
  })

  it('should return "secondary" for ALL', () => {
    expect(audienceSeverity('ALL')).toBe('secondary')
  })

  it('should return "secondary" for unknown value', () => {
    expect(audienceSeverity('UNKNOWN')).toBe('secondary')
  })

  it('should return "secondary" for empty string', () => {
    expect(audienceSeverity('')).toBe('secondary')
  })
})
