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

// MonteWeb golden-yellow theme. Yellow is light, so the primary contrast colour
// is BLACK (#111) in both light and dark mode — white text on yellow is
// unreadable (~2:1). The focus ring uses a dark amber so it stays visible
// against both yellow controls and white surfaces (≥3:1, WCAG 2.4.7 / 1.4.11).
const MontePreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#fffbeb', 100: '#fef3c7', 200: '#fde68a', 300: '#fcd34d', 400: '#fbbf24',
      500: '#f5b400', 600: '#dba300', 700: '#b88600', 800: '#946a00', 900: '#7a5800', 950: '#463300',
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.700}',
      offset: '2px',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.500}',
          contrastColor: '#111111',
          hoverColor: '{primary.600}',
          activeColor: '{primary.700}',
        },
      },
      dark: {
        primary: {
          color: '{primary.400}',
          contrastColor: '#111111',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
      },
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
