<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Breadcrumb from 'primevue/breadcrumb'

const route = useRoute()
const { t } = useI18n()

const home = computed(() => ({
  icon: 'pi pi-home',
  route: { name: 'dashboard' },
}))

const items = computed(() => {
  const result: { label: string; route?: { name: string } }[] = []

  for (let i = 0; i < route.matched.length; i++) {
    const matched = route.matched[i]!
    const label = matched.meta?.breadcrumbLabel as string | undefined
    if (!label) continue

    const isLast = i === route.matched.length - 1
    const routeName = matched.name as string | undefined

    result.push({
      label: t(label),
      route: isLast || !routeName ? undefined : { name: routeName },
    })
  }
  return result
})

const showBreadcrumb = computed(() => items.value.length > 0)
</script>

<template>
  <Breadcrumb v-if="showBreadcrumb" :home="home" :model="items" class="app-breadcrumb">
    <template #item="{ item }">
      <router-link v-if="item.route" :to="item.route" class="breadcrumb-link">
        <span v-if="item.icon" :class="item.icon" />
        <span v-if="item.label">{{ item.label }}</span>
      </router-link>
      <span v-else class="breadcrumb-current">
        <span v-if="item.icon" :class="item.icon" />
        <span v-if="item.label">{{ item.label }}</span>
      </span>
    </template>
  </Breadcrumb>
</template>

<style scoped>
.app-breadcrumb {
  margin-bottom: 1rem;
  background: transparent;
  border: none;
  padding: 0;
}

.breadcrumb-link {
  color: var(--p-primary-color);
  text-decoration: none;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.breadcrumb-current {
  color: var(--p-text-color);
}
</style>
