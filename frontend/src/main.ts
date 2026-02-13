import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'
import Aura from '@primevue/themes/aura'
import router from './router'
import i18n from './i18n'
import App from './App.vue'
import { reportError } from './composables/useErrorReporting'

import 'primeicons/primeicons.css'
import './assets/styles/global.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      prefix: 'p',
      darkModeSelector: false,
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
