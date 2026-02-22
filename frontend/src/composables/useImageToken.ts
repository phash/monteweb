import { ref } from 'vue'
import client from '@/api/client'

/**
 * Manages a short-lived (5 min) image token for authenticated image access.
 * Replaces using the full JWT access token in image URL query parameters.
 * The token is refreshed automatically every 4 minutes.
 */
let imageToken = ref<string | null>(null)
let refreshTimer: ReturnType<typeof setTimeout> | null = null

export function useImageToken() {
  async function fetchImageToken(): Promise<string | null> {
    try {
      const res = await client.get<{ data: { token: string } }>('/image-token')
      imageToken.value = res.data.data.token
      scheduleRefresh()
      return imageToken.value
    } catch {
      imageToken.value = null
      return null
    }
  }

  function scheduleRefresh() {
    if (refreshTimer) clearTimeout(refreshTimer)
    // Refresh 1 minute before expiry (token lives 5 min, refresh at 4 min)
    refreshTimer = setTimeout(() => {
      fetchImageToken()
    }, 4 * 60 * 1000)
  }

  function getImageToken(): string | null {
    return imageToken.value
  }

  function clearImageToken() {
    imageToken.value = null
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  return {
    imageToken,
    fetchImageToken,
    getImageToken,
    clearImageToken,
  }
}

/**
 * Builds an authenticated image URL using the short-lived image token.
 */
export function authenticatedImageUrl(path: string): string {
  const token = imageToken.value
  return token ? `${path}?token=${encodeURIComponent(token)}` : path
}
