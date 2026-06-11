import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'
import Aura from '@primevue/themes/aura'
import { definePreset } from '@primevue/themes'
import router from './router'
import i18n from './i18n'
import App from './App.vue'
import { reportError } from './composables/useErrorReporting'

import 'primeicons/primeicons.css'
import './assets/styles/global.css'

// MonteWeb golden-yellow theme. Every PrimeVue colour token points at the
// --mw-* CSS variables, so the WHOLE UI (PrimeVue + custom) follows ONE source
// of truth: the admin-editable theme. This is applied to BOTH colour schemes so
// PrimeVue can never render its own dark surfaces on the light app, and so an
// admin can recolour every element type. Yellow is light → primary contrast is
// BLACK; the focus ring is dark so it stays visible on yellow + white.
const mwScheme = {
  primary: {
    color: 'var(--mw-primary)',
    contrastColor: 'var(--mw-primary-contrast)',
    hoverColor: 'var(--mw-primary-hover)',
    activeColor: 'var(--mw-primary-active)',
  },
  text: {
    color: 'var(--mw-text)',
    hoverColor: 'var(--mw-text)',
    mutedColor: 'var(--mw-text-muted)',
    hoverMutedColor: 'var(--mw-text-secondary)',
  },
  content: {
    background: 'var(--mw-bg-card)',
    hoverBackground: 'var(--mw-bg-hover)',
    borderColor: 'var(--mw-border)',
    color: 'var(--mw-text)',
    hoverColor: 'var(--mw-text)',
  },
  formField: {
    background: 'var(--mw-bg-card)',
    disabledBackground: 'var(--mw-bg-hover)',
    filledBackground: 'var(--mw-bg-hover)',
    filledHoverBackground: 'var(--mw-bg-hover)',
    filledFocusBackground: 'var(--mw-bg-card)',
    borderColor: 'var(--mw-border)',
    hoverBorderColor: 'var(--mw-primary-dark)',
    focusBorderColor: 'var(--mw-primary)',
    invalidBorderColor: 'var(--mw-danger)',
    color: 'var(--mw-text)',
    disabledColor: 'var(--mw-text-muted)',
    placeholderColor: 'var(--mw-text-muted)',
    iconColor: 'var(--mw-text-muted)',
    floatLabelColor: 'var(--mw-text-muted)',
    floatLabelActiveColor: 'var(--mw-text-secondary)',
    floatLabelFocusColor: 'var(--mw-primary-dark)',
  },
  overlay: {
    select: { background: 'var(--mw-bg-card)', borderColor: 'var(--mw-border)', color: 'var(--mw-text)' },
    popover: { background: 'var(--mw-bg-card)', borderColor: 'var(--mw-border)', color: 'var(--mw-text)' },
    modal: { background: 'var(--mw-bg-card)', borderColor: 'var(--mw-border)', color: 'var(--mw-text)' },
  },
  list: {
    option: {
      color: 'var(--mw-text)',
      focusColor: 'var(--mw-text)',
      selectedColor: 'var(--mw-primary-contrast)',
      focusBackground: 'var(--mw-bg-hover)',
      selectedBackground: 'var(--mw-primary)',
      selectedFocusBackground: 'var(--mw-primary-hover)',
      selectedFocusColor: 'var(--mw-primary-contrast)',
    },
  },
  navigation: {
    item: {
      color: 'var(--mw-text)',
      focusColor: 'var(--mw-text)',
      activeColor: 'var(--mw-text)',
      focusBackground: 'var(--mw-bg-hover)',
      activeBackground: 'var(--mw-bg-active)',
      icon: {
        color: 'var(--mw-text-muted)',
        focusColor: 'var(--mw-text)',
        activeColor: 'var(--mw-text)',
      },
    },
  },
}

const MontePreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#fffbeb', 100: '#fef3c7', 200: '#fde68a', 300: '#fcd34d', 400: '#fbbf24',
      500: '#f5b400', 600: '#dba300', 700: '#b88600', 800: '#946a00', 900: '#7a5800', 950: '#463300',
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: 'var(--mw-focus-ring)',
      offset: '2px',
    },
    colorScheme: {
      light: { ...mwScheme },
      dark: { ...mwScheme },
    },
  },
})

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(PrimeVue, {
  theme: {
    preset: MontePreset,
    options: {
      prefix: 'p',
      darkModeSelector: '.dark',
    },
  },
})
app.use(ToastService)
app.directive('tooltip', Tooltip)

// Global Vue error handler
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue Error:', err)
  const error = err as Error
  reportError({
    source: 'FRONTEND',
    errorType: error?.constructor?.name || 'Error',
    message: String(err),
    stackTrace: error?.stack,
    location: `${(instance as any)?.$options?.__name || 'unknown'} (${info})`,
  })
}

// Catch unhandled promise rejections
window.addEventListener('unhandledrejection', (event) => {
  reportError({
    source: 'FRONTEND',
    errorType: 'UnhandledRejection',
    message: String(event.reason),
    stackTrace: event.reason?.stack,
    location: window.location.pathname,
  })
})

// Catch server errors dispatched from the API client
window.addEventListener('monteweb:server-error', ((event: CustomEvent) => {
  reportError({
    source: 'FRONTEND',
    errorType: `HTTP ${event.detail.status}`,
    message: event.detail.message,
    location: event.detail.url,
  })
}) as EventListener)

app.mount('#app')
