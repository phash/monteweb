import { describe, it, expect, beforeEach } from 'vitest'

// We need to reset module state between tests
let markTermsAccepted: typeof import('../termsCache').markTermsAccepted
let resetTermsCache: typeof import('../termsCache').resetTermsCache
let isTermsChecked: typeof import('../termsCache').isTermsChecked
let getTermsAccepted: typeof import('../termsCache').getTermsAccepted
let setTermsStatus: typeof import('../termsCache').setTermsStatus

describe('termsCache', () => {
  beforeEach(async () => {
    // Reset module to get fresh state for each test
    vi.resetModules()
    const mod = await import('../termsCache')
    markTermsAccepted = mod.markTermsAccepted
    resetTermsCache = mod.resetTermsCache
    isTermsChecked = mod.isTermsChecked
    getTermsAccepted = mod.getTermsAccepted
    setTermsStatus = mod.setTermsStatus
  })

  describe('initial state', () => {
    it('should start with termsChecked = false', () => {
      expect(isTermsChecked()).toBe(false)
    })

    it('should start with termsAccepted = true (optimistic default)', () => {
      expect(getTermsAccepted()).toBe(true)
    })
  })

  describe('markTermsAccepted', () => {
    it('should set termsChecked to true', () => {
      markTermsAccepted()
      expect(isTermsChecked()).toBe(true)
    })

    it('should set termsAccepted to true', () => {
      // First set to false via setTermsStatus
      setTermsStatus(true, false)
      expect(getTermsAccepted()).toBe(false)

      markTermsAccepted()
      expect(getTermsAccepted()).toBe(true)
    })
  })

  describe('resetTermsCache', () => {
    it('should reset termsChecked to false', () => {
      markTermsAccepted()
      expect(isTermsChecked()).toBe(true)

      resetTermsCache()
      expect(isTermsChecked()).toBe(false)
    })

    it('should reset termsAccepted to true', () => {
      setTermsStatus(true, false)
      expect(getTermsAccepted()).toBe(false)

      resetTermsCache()
      expect(getTermsAccepted()).toBe(true)
    })
  })

  describe('setTermsStatus', () => {
    it('should set both checked and accepted to true', () => {
      setTermsStatus(true, true)
      expect(isTermsChecked()).toBe(true)
      expect(getTermsAccepted()).toBe(true)
    })

    it('should set checked=true, accepted=false', () => {
      setTermsStatus(true, false)
      expect(isTermsChecked()).toBe(true)
      expect(getTermsAccepted()).toBe(false)
    })

    it('should set checked=false, accepted=false', () => {
      setTermsStatus(false, false)
      expect(isTermsChecked()).toBe(false)
      expect(getTermsAccepted()).toBe(false)
    })

    it('should allow overwriting previous state', () => {
      setTermsStatus(true, true)
      expect(isTermsChecked()).toBe(true)
      expect(getTermsAccepted()).toBe(true)

      setTermsStatus(false, false)
      expect(isTermsChecked()).toBe(false)
      expect(getTermsAccepted()).toBe(false)
    })
  })

  describe('typical flow', () => {
    it('should handle login -> check -> accept -> logout cycle', () => {
      // Initially unchecked
      expect(isTermsChecked()).toBe(false)

      // Router guard checks terms from API -> not accepted
      setTermsStatus(true, false)
      expect(isTermsChecked()).toBe(true)
      expect(getTermsAccepted()).toBe(false)

      // User accepts terms
      markTermsAccepted()
      expect(isTermsChecked()).toBe(true)
      expect(getTermsAccepted()).toBe(true)

      // User logs out
      resetTermsCache()
      expect(isTermsChecked()).toBe(false)
      expect(getTermsAccepted()).toBe(true) // optimistic default
    })
  })
})
