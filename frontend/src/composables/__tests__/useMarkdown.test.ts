import { describe, it, expect } from 'vitest'
import { useMarkdown } from '@/composables/useMarkdown'

describe('useMarkdown', () => {
  const { renderMarkdown } = useMarkdown()

  it('returns empty string for empty input', () => {
    expect(renderMarkdown('')).toBe('')
  })

  it('renders h1 headings', () => {
    expect(renderMarkdown('# Hello')).toContain('<h1>Hello</h1>')
  })

  it('renders h2 headings', () => {
    expect(renderMarkdown('## Subtitle')).toContain('<h2>Subtitle</h2>')
  })

  it('renders h3 headings', () => {
    expect(renderMarkdown('### Section')).toContain('<h3>Section</h3>')
  })

  it('renders bold text', () => {
    expect(renderMarkdown('**bold**')).toContain('<strong>bold</strong>')
  })

  it('renders italic text', () => {
    expect(renderMarkdown('*italic*')).toContain('<em>italic</em>')
  })

  it('renders bold italic text', () => {
    expect(renderMarkdown('***both***')).toContain('<strong><em>both</em></strong>')
  })

  it('renders links with target and rel attributes', () => {
    const result = renderMarkdown('[link](http://example.com)')
    expect(result).toContain('<a href="http://example.com"')
    expect(result).toContain('target="_blank"')
    expect(result).toContain('rel="noopener noreferrer"')
    expect(result).toContain('>link</a>')
  })

  it('renders unordered lists', () => {
    const result = renderMarkdown('- item 1\n- item 2')
    expect(result).toContain('<li>item 1</li>')
    expect(result).toContain('<li>item 2</li>')
    expect(result).toContain('<ul>')
  })

  it('renders inline code', () => {
    expect(renderMarkdown('`code`')).toContain('<code>code</code>')
  })

  it('renders code blocks', () => {
    const result = renderMarkdown('```js\nconst x = 1\n```')
    expect(result).toContain('<pre><code>')
    expect(result).toContain('const x = 1')
    expect(result).toContain('</code></pre>')
  })

  it('renders blockquotes', () => {
    expect(renderMarkdown('> quote')).toContain('<blockquote>quote</blockquote>')
  })

  it('renders horizontal rules', () => {
    expect(renderMarkdown('---')).toContain('<hr')
  })

  it('sanitizes script tags (XSS prevention)', () => {
    expect(renderMarkdown('<script>alert(1)</script>')).not.toContain('<script>')
  })

  it('handles paragraphs from double newlines', () => {
    const result = renderMarkdown('first\n\nsecond')
    expect(result).toContain('<p>')
    expect(result).toContain('first')
    expect(result).toContain('second')
  })

  it('resolves letter variables with sample data', () => {
    const { resolveVariablesPreview } = useMarkdown()
    const result = resolveVariablesPreview(
      'Liebe {Anrede}, Ihr Kind {NameKind} aus {Familie}. Grüße, {LehrerName}',
      'Max Mustermann',
    )
    expect(result).toContain('Familie Müller')
    expect(result).toContain('Max')
    expect(result).toContain('Max Mustermann')
  })
})
