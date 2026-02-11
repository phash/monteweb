import { createI18n } from 'vue-i18n'
import de from './de'
import en from './en'

function detectLocale(): string {
  // Check localStorage first
  const stored = localStorage.getItem('monteweb-locale')
  if (stored && (stored === 'de' || stored === 'en')) {
    return stored
  }
  // Detect from browser
  const browserLang = navigator.language.split('-')[0]
  return browserLang === 'de' ? 'de' : 'en'
}

const i18n = createI18n({
  legacy: false,
  locale: detectLocale(),
  fallbackLocale: 'de',
  messages: { de, en },
})

export default i18n
