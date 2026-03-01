import axios from 'axios'

const recentFingerprints = new Set<string>()
const ERROR_REPORT_URL = '/api/v1/error-reports'

function simpleHash(str: string): string {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash |= 0
  }
  return String(hash)
}

export function reportError(error: {
  source: string
  errorType?: string
  message: string
  stackTrace?: string
  location?: string
}) {
  const fp = simpleHash(error.source + (error.errorType || '') + (error.message || '').substring(0, 200))
  if (recentFingerprints.has(fp)) return
  recentFingerprints.add(fp)
  setTimeout(() => recentFingerprints.delete(fp), 30000)

  // Fire-and-forget, using raw axios to avoid interceptor loops
  // Only send pathname (no query params/hash) to avoid leaking PD in URLs
  axios.post(ERROR_REPORT_URL, {
    ...error,
    requestUrl: window.location.pathname,
  }).catch(() => {
    // Silently ignore - don't let error reporting cause more errors
  })
}
