import { ref, onMounted, onUnmounted } from 'vue'

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

const deferredPrompt = ref<BeforeInstallPromptEvent | null>(null)
const isInstallable = ref(false)
const isInstalled = ref(false)
const dismissed = ref(false)

export function usePwaInstall() {
  function onBeforeInstallPrompt(e: Event) {
    e.preventDefault()
    deferredPrompt.value = e as BeforeInstallPromptEvent
    isInstallable.value = true
    // Check if user previously dismissed
    const dismissedAt = localStorage.getItem('pwa-install-dismissed')
    if (dismissedAt) {
      const daysSince = (Date.now() - parseInt(dismissedAt)) / (1000 * 60 * 60 * 24)
      if (daysSince < 7) {
        dismissed.value = true
      } else {
        localStorage.removeItem('pwa-install-dismissed')
      }
    }
  }

  function onAppInstalled() {
    isInstalled.value = true
    isInstallable.value = false
    deferredPrompt.value = null
  }

  async function install() {
    if (!deferredPrompt.value) return
    await deferredPrompt.value.prompt()
    const { outcome } = await deferredPrompt.value.userChoice
    if (outcome === 'accepted') {
      isInstalled.value = true
      isInstallable.value = false
    }
    deferredPrompt.value = null
  }

  function dismiss() {
    dismissed.value = true
    localStorage.setItem('pwa-install-dismissed', Date.now().toString())
  }

  onMounted(() => {
    // Check if already running as PWA
    if (window.matchMedia('(display-mode: standalone)').matches) {
      isInstalled.value = true
      return
    }
    window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)
    window.addEventListener('appinstalled', onAppInstalled)
  })

  onUnmounted(() => {
    window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
    window.removeEventListener('appinstalled', onAppInstalled)
  })

  return {
    isInstallable,
    isInstalled,
    dismissed,
    install,
    dismiss,
    get showBanner() {
      return isInstallable.value && !isInstalled.value && !dismissed.value
    },
  }
}
