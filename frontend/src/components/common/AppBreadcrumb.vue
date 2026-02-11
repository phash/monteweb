<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Breadcrumb from 'primevue/breadcrumb'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const home = computed(() => ({
  icon: 'pi pi-home',
  command: () => router.push({ name: 'dashboard' }),
}))

const items = computed(() => {
  const result: { label: string; command?: () => void }[] = []

  for (let i = 0; i < route.matched.length; i++) {
    const matched = route.matched[i]
    const label = matched.meta?.breadcrumbLabel as string | undefined
    if (!label) continue

    const isLast = i === route.matched.length - 1
    const routeName = matched.name as string | undefined

    result.push({
      label: t(label),
      command: isLast || !routeName ? undefined : () => router.push({ name: routeName }),
    })
  }
  return result
})

const showBreadcrumb = computed(() => items.value.length > 0)
</script>

<template>
  <Breadcrumb v-if="showBreadcrumb" :home="home" :model="items" class="app-breadcrumb" />
</template>

<style scoped>
.app-breadcrumb {
  margin-bottom: 1rem;
  background: transparent;
  border: none;
  padding: 0;
}
</style>
