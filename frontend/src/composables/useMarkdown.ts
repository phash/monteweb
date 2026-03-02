import { sanitizeHtml } from '@/utils/sanitize'

export function useMarkdown() {
  /**
   * Converts a Markdown string to sanitized HTML.
   * Uses regex-based parsing for common Markdown constructs
   * and DOMPurify sanitization for XSS prevention.
   */
  function renderMarkdown(md: string): string {
    if (!md) return ''
    let html = md

    // Escape HTML
    html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

    // Code blocks (```)
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_m, _lang, code) => {
      return `<pre><code>${code.trim()}</code></pre>`
    })

    // Inline code
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

    // Headings
    html = html.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>')
    html = html.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>')
    html = html.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>')
    html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
    html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
    html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')

    // Bold and italic
    html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

    // Links
    html = html.replace(
      /\[([^\]]+)\]\(([^)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>',
    )

    // Unordered lists
    html = html.replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
    html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>')

    // Ordered lists
    html = html.replace(/^\d+\.\s+(.+)$/gm, '<li>$1</li>')

    // Horizontal rule
    html = html.replace(/^---$/gm, '<hr />')

    // Blockquote
    html = html.replace(/^&gt;\s+(.+)$/gm, '<blockquote>$1</blockquote>')

    // Paragraphs (double newline)
    html = html.replace(/\n\n+/g, '</p><p>')
    html = '<p>' + html + '</p>'

    // Clean up empty paragraphs
    html = html.replace(/<p>\s*<\/p>/g, '')
    html = html.replace(/<p>\s*(<h[1-6]>)/g, '$1')
    html = html.replace(/(<\/h[1-6]>)\s*<\/p>/g, '$1')
    html = html.replace(/<p>\s*(<pre>)/g, '$1')
    html = html.replace(/(<\/pre>)\s*<\/p>/g, '$1')
    html = html.replace(/<p>\s*(<ul>)/g, '$1')
    html = html.replace(/(<\/ul>)\s*<\/p>/g, '$1')
    html = html.replace(/<p>\s*(<blockquote>)/g, '$1')
    html = html.replace(/(<\/blockquote>)\s*<\/p>/g, '$1')
    html = html.replace(/<p>\s*(<hr \/>)/g, '$1')
    html = html.replace(/(<hr \/>)\s*<\/p>/g, '$1')

    return sanitizeHtml(html)
  }

  /**
   * Replaces letter template variables with sample preview data.
   * Used for live preview in the parent letter editor.
   */
  function resolveVariablesPreview(content: string, currentUserName: string): string {
    return content
      .replace(/\{Familie\}/g, 'Familie Müller')
      .replace(/\{NameKind\}/g, 'Max')
      .replace(/\{Anrede\}/g, 'Sehr geehrte Frau Müller')
      .replace(/\{LehrerName\}/g, currentUserName)
  }

  return { renderMarkdown, resolveVariablesPreview }
}
