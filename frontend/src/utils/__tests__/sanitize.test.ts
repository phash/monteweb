import { describe, it, expect } from 'vitest'
import { sanitizeHtml, sanitizeSearchSnippet } from '../sanitize'

describe('sanitizeHtml', () => {
  describe('allowed tags', () => {
    it('should allow headings', () => {
      expect(sanitizeHtml('<h1>Title</h1>')).toBe('<h1>Title</h1>')
      expect(sanitizeHtml('<h2>Sub</h2>')).toBe('<h2>Sub</h2>')
      expect(sanitizeHtml('<h3>Sub</h3>')).toBe('<h3>Sub</h3>')
    })

    it('should allow paragraphs and line breaks', () => {
      expect(sanitizeHtml('<p>Text</p>')).toBe('<p>Text</p>')
      expect(sanitizeHtml('Line1<br>Line2')).toBe('Line1<br>Line2')
      expect(sanitizeHtml('<hr>')).toBe('<hr>')
    })

    it('should allow text formatting tags', () => {
      expect(sanitizeHtml('<strong>bold</strong>')).toBe('<strong>bold</strong>')
      expect(sanitizeHtml('<b>bold</b>')).toBe('<b>bold</b>')
      expect(sanitizeHtml('<em>italic</em>')).toBe('<em>italic</em>')
      expect(sanitizeHtml('<i>italic</i>')).toBe('<i>italic</i>')
      expect(sanitizeHtml('<u>underline</u>')).toBe('<u>underline</u>')
      expect(sanitizeHtml('<s>strike</s>')).toBe('<s>strike</s>')
      expect(sanitizeHtml('<del>deleted</del>')).toBe('<del>deleted</del>')
    })

    it('should allow links with permitted attributes', () => {
      const link = '<a href="https://example.com" target="_blank" rel="noopener">Link</a>'
      expect(sanitizeHtml(link)).toBe(link)
    })

    it('should allow images with permitted attributes', () => {
      const img = '<img src="image.jpg" alt="Photo" title="My Photo">'
      expect(sanitizeHtml(img)).toContain('src="image.jpg"')
      expect(sanitizeHtml(img)).toContain('alt="Photo"')
    })

    it('should allow lists', () => {
      expect(sanitizeHtml('<ul><li>Item</li></ul>')).toBe('<ul><li>Item</li></ul>')
      expect(sanitizeHtml('<ol><li>First</li></ol>')).toBe('<ol><li>First</li></ol>')
    })

    it('should allow code blocks', () => {
      expect(sanitizeHtml('<pre><code>const x = 1</code></pre>')).toBe(
        '<pre><code>const x = 1</code></pre>',
      )
      expect(sanitizeHtml('<blockquote>Quote</blockquote>')).toBe(
        '<blockquote>Quote</blockquote>',
      )
    })

    it('should allow tables', () => {
      const table = '<table><thead><tr><th>Header</th></tr></thead><tbody><tr><td>Cell</td></tr></tbody></table>'
      expect(sanitizeHtml(table)).toBe(table)
    })

    it('should allow div, span, and mark', () => {
      expect(sanitizeHtml('<div>Block</div>')).toBe('<div>Block</div>')
      expect(sanitizeHtml('<span>Inline</span>')).toBe('<span>Inline</span>')
      expect(sanitizeHtml('<mark>Highlighted</mark>')).toBe('<mark>Highlighted</mark>')
    })

    it('should allow class attribute', () => {
      expect(sanitizeHtml('<span class="highlight">Text</span>')).toBe(
        '<span class="highlight">Text</span>',
      )
    })
  })

  describe('XSS prevention', () => {
    it('should strip script tags', () => {
      const result = sanitizeHtml('<script>alert("XSS")</script>')
      expect(result).not.toContain('<script>')
      expect(result).not.toContain('alert')
    })

    it('should strip iframe tags', () => {
      const result = sanitizeHtml('<iframe src="https://evil.com"></iframe>')
      expect(result).not.toContain('<iframe')
    })

    it('should strip event handler attributes', () => {
      const result = sanitizeHtml('<img src="x" onerror="alert(1)">')
      expect(result).not.toContain('onerror')
    })

    it('should strip onclick attributes', () => {
      const result = sanitizeHtml('<div onclick="alert(1)">Click</div>')
      expect(result).not.toContain('onclick')
    })

    it('should strip javascript: URLs in href', () => {
      const result = sanitizeHtml('<a href="javascript:alert(1)">Link</a>')
      expect(result).not.toContain('javascript:')
    })

    it('should strip data attributes', () => {
      const result = sanitizeHtml('<div data-evil="payload">Content</div>')
      expect(result).not.toContain('data-evil')
    })

    it('should strip form and input elements', () => {
      const result = sanitizeHtml('<form action="/steal"><input type="password"></form>')
      expect(result).not.toContain('<form')
      expect(result).not.toContain('<input')
    })

    it('should strip style attributes', () => {
      const result = sanitizeHtml('<p style="background:url(evil)">Text</p>')
      expect(result).not.toContain('style=')
    })

    it('should strip object and embed tags', () => {
      const result = sanitizeHtml('<object data="evil.swf"></object><embed src="evil.swf">')
      expect(result).not.toContain('<object')
      expect(result).not.toContain('<embed')
    })
  })

  describe('edge cases', () => {
    it('should handle empty string', () => {
      expect(sanitizeHtml('')).toBe('')
    })

    it('should handle plain text without tags', () => {
      expect(sanitizeHtml('Hello World')).toBe('Hello World')
    })

    it('should handle nested disallowed tags', () => {
      const result = sanitizeHtml('<div><script>alert(1)</script><p>Safe</p></div>')
      expect(result).not.toContain('<script>')
      expect(result).toContain('<p>Safe</p>')
    })
  })
})

describe('sanitizeSearchSnippet', () => {
  it('should allow mark tags', () => {
    expect(sanitizeSearchSnippet('<mark>match</mark>')).toBe('<mark>match</mark>')
  })

  it('should allow b, em, and strong tags', () => {
    expect(sanitizeSearchSnippet('<b>bold</b>')).toBe('<b>bold</b>')
    expect(sanitizeSearchSnippet('<em>italic</em>')).toBe('<em>italic</em>')
    expect(sanitizeSearchSnippet('<strong>bold</strong>')).toBe('<strong>bold</strong>')
  })

  it('should strip disallowed tags', () => {
    const result = sanitizeSearchSnippet('<script>alert(1)</script><mark>safe</mark>')
    expect(result).not.toContain('<script>')
    expect(result).toContain('<mark>safe</mark>')
  })

  it('should strip all attributes', () => {
    const result = sanitizeSearchSnippet('<mark class="hl">text</mark>')
    expect(result).not.toContain('class=')
    expect(result).toContain('<mark>text</mark>')
  })

  it('should strip links', () => {
    const result = sanitizeSearchSnippet('<a href="evil.com">click</a>')
    expect(result).not.toContain('<a')
    expect(result).toContain('click')
  })

  it('should handle empty string', () => {
    expect(sanitizeSearchSnippet('')).toBe('')
  })

  it('should handle plain text', () => {
    expect(sanitizeSearchSnippet('plain text result')).toBe('plain text result')
  })
})
