import { watch } from 'vue'
import { useAdminStore } from '@/stores/admin'

/**
 * Applies theme CSS variables from the tenant config to the document root.
 * Call once in App.vue setup to enable dynamic theming.
 */
export function useTheme() {
  const adminStore = useAdminStore()

  function applyTheme(theme: Record<string, unknown> | null | undefined) {
    if (!theme) return
    const root = document.documentElement

    // Map theme keys to CSS custom properties
    const mapping: Record<string, string> = {
      primaryColor: '--mw-primary',
      primaryHover: '--mw-primary-hover',
      bgMain: '--mw-bg-main',
      bgCard: '--mw-bg-card',
      bgSidebar: '--mw-bg-sidebar',
      textColor: '--mw-text',
      textSecondary: '--mw-text-secondary',
      textMuted: '--mw-text-muted',
      borderLight: '--mw-border-light',
      successColor: '--mw-success',
      warningColor: '--mw-warning',
      dangerColor: '--mw-danger',
    }

    for (const [key, cssVar] of Object.entries(mapping)) {
      if (theme[key] && typeof theme[key] === 'string') {
        root.style.setProperty(cssVar, theme[key] as string)
      }
    }
  }

  // Apply theme whenever config changes
  watch(
    () => adminStore.config?.theme,
    (theme) => applyTheme(theme as Record<string, unknown> | null),
    { immediate: true }
  )

  return { applyTheme }
}
