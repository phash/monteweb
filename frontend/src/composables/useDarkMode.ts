import { ref, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users.api'

export type DarkModePreference = 'SYSTEM' | 'LIGHT' | 'DARK'

const preference = ref<DarkModePreference>(
  (localStorage.getItem('darkMode') as DarkModePreference) || 'SYSTEM',
)

const systemDark = ref(false)
let mediaQuery: MediaQueryList | null = null

function applyDarkClass() {
  const shouldBeDark =
    preference.value === 'DARK' || (preference.value === 'SYSTEM' && systemDark.value)

  document.documentElement.classList.toggle('dark', shouldBeDark)
}

/**
 * Composable to manage dark mode preference.
 * - Persists to localStorage for immediate pre-login use
 * - Syncs to backend when authenticated
 * - Respects system preference when set to SYSTEM
 */
export function useDarkMode() {
  const auth = useAuthStore()

  onMounted(() => {
    // Listen for OS-level dark mode changes
    if (!mediaQuery) {
      mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      systemDark.value = mediaQuery.matches
      mediaQuery.addEventListener('change', (e) => {
        systemDark.value = e.matches
        applyDarkClass()
      })
    }
    applyDarkClass()
  })

  watch(preference, (val) => {
    localStorage.setItem('darkMode', val)
    applyDarkClass()
  })

  // Load from user profile when authenticated
  async function loadFromUser() {
    if (!auth.isAuthenticated || !auth.user) return
    const mode = auth.user.darkMode
    if (mode && ['SYSTEM', 'LIGHT', 'DARK'].includes(mode)) {
      preference.value = mode as DarkModePreference
    }
  }

  async function setPreference(mode: DarkModePreference) {
    preference.value = mode
    if (auth.isAuthenticated) {
      try {
        await usersApi.updateDarkMode(mode)
      } catch {
        // Ignore — localStorage is the fallback
      }
    }
  }

  return {
    preference,
    loadFromUser,
    setPreference,
  }
}

// Apply dark mode immediately on module load (before Vue mounts)
// so there's no flash of wrong theme
try {
  const stored = localStorage.getItem('darkMode') as DarkModePreference | null
  if (stored === 'DARK') {
    document.documentElement.classList.add('dark')
  } else if (stored !== 'LIGHT' && typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    document.documentElement.classList.add('dark')
  }
} catch {
  // Ignore in SSR/test environments
}
