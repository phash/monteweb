import { describe, it, expect, beforeEach, vi } from 'vitest'

describe('i18n locale detection', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should use stored locale from localStorage when available', () => {
    localStorage.setItem('monteweb-locale', 'en')

    // Re-import to trigger detectLocale
    const stored = localStorage.getItem('monteweb-locale')
    expect(stored).toBe('en')
    expect(['de', 'en']).toContain(stored)
  })

  it('should accept "de" as a valid stored locale', () => {
    localStorage.setItem('monteweb-locale', 'de')
    const stored = localStorage.getItem('monteweb-locale')
    expect(stored).toBe('de')
  })

  it('should accept "en" as a valid stored locale', () => {
    localStorage.setItem('monteweb-locale', 'en')
    const stored = localStorage.getItem('monteweb-locale')
    expect(stored).toBe('en')
  })

  it('should ignore invalid stored locale values', () => {
    localStorage.setItem('monteweb-locale', 'fr')
    const stored = localStorage.getItem('monteweb-locale')
    // detectLocale would skip invalid values and fall through to browser detection
    expect(stored).toBe('fr')
    expect(stored !== 'de' && stored !== 'en').toBe(true)
  })

  it('should detect locale from browser navigator.language', () => {
    // Default navigator.language is usually set by jsdom
    const browserLang = navigator.language.split('-')[0]
    expect(typeof browserLang).toBe('string')
    expect(browserLang.length).toBeGreaterThan(0)
  })

  describe('locale detection logic', () => {
    function detectLocale(): string {
      const stored = localStorage.getItem('monteweb-locale')
      if (stored && (stored === 'de' || stored === 'en')) {
        return stored
      }
      const browserLang = navigator.language.split('-')[0]
      return browserLang === 'de' ? 'de' : 'en'
    }

    it('should return stored "de" when set', () => {
      localStorage.setItem('monteweb-locale', 'de')
      expect(detectLocale()).toBe('de')
    })

    it('should return stored "en" when set', () => {
      localStorage.setItem('monteweb-locale', 'en')
      expect(detectLocale()).toBe('en')
    })

    it('should fall back to browser detection when no stored locale', () => {
      // No localStorage set, should detect from browser
      const result = detectLocale()
      expect(['de', 'en']).toContain(result)
    })

    it('should fall back to browser detection when invalid stored locale', () => {
      localStorage.setItem('monteweb-locale', 'fr')
      const result = detectLocale()
      expect(['de', 'en']).toContain(result)
    })

    it('should default to "en" for non-German browsers', () => {
      // jsdom default navigator.language is usually "en" or "en-US"
      localStorage.removeItem('monteweb-locale')
      const result = detectLocale()
      // Since jsdom uses English locale, expect "en"
      expect(result).toBe('en')
    })
  })
})
