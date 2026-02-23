import { describe, it, expect } from 'vitest'
import { formatMentions, extractMentionedUserIds } from '../useMentions'

describe('useMentions', () => {
  describe('formatMentions', () => {
    it('should return empty string for null/undefined', () => {
      expect(formatMentions(null)).toBe('')
      expect(formatMentions(undefined)).toBe('')
    })

    it('should return original text when no mentions present', () => {
      expect(formatMentions('Hello world')).toBe('Hello world')
    })

    it('should replace mention format with @DisplayName', () => {
      const text = 'Hey @[550e8400-e29b-41d4-a716-446655440000:Max Mustermann] check this out!'
      expect(formatMentions(text)).toBe('Hey @Max Mustermann check this out!')
    })

    it('should handle multiple mentions', () => {
      const text = '@[550e8400-e29b-41d4-a716-446655440000:Max Mustermann] and @[660e8400-e29b-41d4-a716-446655440001:Anna Schmidt] please review'
      expect(formatMentions(text)).toBe('@Max Mustermann and @Anna Schmidt please review')
    })

    it('should handle mention at start of text', () => {
      const text = '@[550e8400-e29b-41d4-a716-446655440000:Max Mustermann] bitte lesen'
      expect(formatMentions(text)).toBe('@Max Mustermann bitte lesen')
    })

    it('should handle mention at end of text', () => {
      const text = 'Das ist fuer @[550e8400-e29b-41d4-a716-446655440000:Max Mustermann]'
      expect(formatMentions(text)).toBe('Das ist fuer @Max Mustermann')
    })

    it('should handle text with no valid UUID in mention', () => {
      const text = 'Hello @[not-a-uuid:Max] world'
      expect(formatMentions(text)).toBe('Hello @[not-a-uuid:Max] world')
    })

    it('should handle empty content', () => {
      expect(formatMentions('')).toBe('')
    })

    it('should preserve surrounding whitespace', () => {
      const text = '  @[550e8400-e29b-41d4-a716-446655440000:Max Mustermann]  '
      expect(formatMentions(text)).toBe('  @Max Mustermann  ')
    })

    it('should handle mention with special characters in name', () => {
      const text = '@[550e8400-e29b-41d4-a716-446655440000:Anna-Maria O\'Brien]'
      expect(formatMentions(text)).toBe("@Anna-Maria O'Brien")
    })
  })

  describe('extractMentionedUserIds', () => {
    it('should return empty array for null/undefined', () => {
      expect(extractMentionedUserIds(null)).toEqual([])
      expect(extractMentionedUserIds(undefined)).toEqual([])
    })

    it('should return empty array when no mentions present', () => {
      expect(extractMentionedUserIds('Hello world')).toEqual([])
    })

    it('should extract single user ID', () => {
      const text = 'Hey @[550e8400-e29b-41d4-a716-446655440000:Max Mustermann]'
      expect(extractMentionedUserIds(text)).toEqual(['550e8400-e29b-41d4-a716-446655440000'])
    })

    it('should extract multiple user IDs', () => {
      const text = '@[550e8400-e29b-41d4-a716-446655440000:Max] and @[660e8400-e29b-41d4-a716-446655440001:Anna]'
      expect(extractMentionedUserIds(text)).toEqual([
        '550e8400-e29b-41d4-a716-446655440000',
        '660e8400-e29b-41d4-a716-446655440001',
      ])
    })

    it('should return empty array for empty text', () => {
      expect(extractMentionedUserIds('')).toEqual([])
    })

    it('should ignore invalid mention formats', () => {
      const text = 'Hello @Max and @[not-a-uuid:Test]'
      expect(extractMentionedUserIds(text)).toEqual([])
    })
  })
})
