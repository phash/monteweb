import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { helpContent, type RoleHelp } from '@/data/helpContent'

export function useContextHelp() {
  const route = useRoute()
  const auth = useAuthStore()
  const { t } = useI18n()

  const currentRouteName = computed(() => {
    return (route.name as string) ?? ''
  })

  const pageHelp = computed(() => {
    return helpContent[currentRouteName.value] ?? null
  })

  const roleHelp = computed<RoleHelp | null>(() => {
    if (!pageHelp.value) return null

    const role = auth.user?.role
    if (role && pageHelp.value.roles[role]) {
      return pageHelp.value.roles[role]!
    }

    if (pageHelp.value.general) {
      return pageHelp.value.general
    }

    return null
  })

  const pageTitle = computed(() => {
    if (!pageHelp.value) return t('help.contextHelp')
    return t(pageHelp.value.pageTitle)
  })

  const actions = computed(() => {
    if (!roleHelp.value) return []
    return roleHelp.value.actions.map(key => t(key))
  })

  const tips = computed(() => {
    if (!roleHelp.value) return []
    return roleHelp.value.tips.map(key => t(key))
  })

  const hasHelp = computed(() => {
    return actions.value.length > 0 || tips.value.length > 0
  })

  return {
    pageTitle,
    actions,
    tips,
    hasHelp,
  }
}
