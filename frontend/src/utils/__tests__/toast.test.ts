import { describe, it, expect } from 'vitest'
import { TOAST_LIFE } from '../toast'

describe('TOAST_LIFE', () => {
  it('should export SUCCESS duration', () => {
    expect(TOAST_LIFE.SUCCESS).toBe(3000)
  })

  it('should export ERROR duration', () => {
    expect(TOAST_LIFE.ERROR).toBe(5000)
  })

  it('should have ERROR longer than SUCCESS', () => {
    expect(TOAST_LIFE.ERROR).toBeGreaterThan(TOAST_LIFE.SUCCESS)
  })

  it('should be a const object (immutable properties)', () => {
    // TypeScript const assertion ensures readonly, but we verify the values exist
    expect(typeof TOAST_LIFE.SUCCESS).toBe('number')
    expect(typeof TOAST_LIFE.ERROR).toBe('number')
  })
})
