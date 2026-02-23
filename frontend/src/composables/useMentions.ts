/**
 * Utility for parsing and formatting @mentions in text content.
 * Mention format: @[userId:displayName]
 */

const MENTION_REGEX_SRC = '@\\[([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}):([^\\]]+)\\]'

/**
 * Strips the mention format from text, replacing @[uuid:Name] with @Name.
 * Useful for plain text display where rich rendering is not available.
 */
export function formatMentions(content: string | null | undefined): string {
  if (!content) return ''
  return content.replace(new RegExp(MENTION_REGEX_SRC, 'gi'), '@$2')
}

/**
 * Extracts all mentioned user IDs from the given content.
 */
export function extractMentionedUserIds(content: string | null | undefined): string[] {
  if (!content) return []
  const ids: string[] = []
  const regex = new RegExp(MENTION_REGEX_SRC, 'gi')
  let match
  while ((match = regex.exec(content)) !== null) {
    if (match[1]) ids.push(match[1])
  }
  return ids
}
