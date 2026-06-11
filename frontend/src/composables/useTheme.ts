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

    // Map theme keys to CSS custom properties. These --mw-* variables are the
    // single source of truth for the whole UI — PrimeVue components are bridged
    // to them in main.ts — so editing any of these recolours every element type
    // (custom + PrimeVue). --mw-primary-active/-border follow --mw-primary-dark.
    const mapping: Record<string, string> = {
      // Primary / accent
      primaryColor: '--mw-primary',
      primaryHover: '--mw-primary-hover',
      primaryDark: '--mw-primary-dark',
      primaryContrast: '--mw-primary-contrast',
      secondaryColor: '--mw-secondary',
      linkColor: '--mw-link',
      focusRing: '--mw-focus-ring',
      // Backgrounds
      bgMain: '--mw-bg',
      bgCard: '--mw-bg-card',
      bgSidebar: '--mw-bg-sidebar',
      bgActive: '--mw-bg-active',
      bgHighlight: '--mw-bg-highlight',
      // Text
      textColor: '--mw-text',
      textSecondary: '--mw-text-secondary',
      textMuted: '--mw-text-muted',
      // Borders
      borderColor: '--mw-border',
      borderLight: '--mw-border-light',
      // Status
      successColor: '--mw-success',
      warningColor: '--mw-warning',
      dangerColor: '--mw-danger',
      infoColor: '--mw-info',
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
