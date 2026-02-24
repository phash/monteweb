<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { adminApi } from '@/api/admin.api'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const { t } = useI18n()
const toast = useToast()

const loading = ref(true)
const stats = ref<Record<string, number>>({})

const statCards = [
  { key: 'totalUsers', icon: 'pi pi-users', labelKey: 'admin.analytics.totalUsers' },
  { key: 'activeUsers', icon: 'pi pi-user-plus', labelKey: 'admin.analytics.activeUsers' },
  { key: 'rooms', icon: 'pi pi-th-large', labelKey: 'admin.analytics.rooms' },
  { key: 'posts', icon: 'pi pi-file-edit', labelKey: 'admin.analytics.posts' },
  { key: 'events', icon: 'pi pi-calendar', labelKey: 'admin.analytics.events' },
  { key: 'messages', icon: 'pi pi-comments', labelKey: 'admin.analytics.messages' },
  { key: 'postsThisMonth', icon: 'pi pi-chart-bar', labelKey: 'admin.analytics.postsThisMonth' },
  { key: 'newThisWeek', icon: 'pi pi-star', labelKey: 'admin.analytics.newThisWeek' },
]

onMounted(async () => {
  try {
    const res = await adminApi.getAnalytics()
    stats.value = res.data.data
  } catch {
    toast.add({ severity: 'error', summary: t('common.error'), life: 3000 })
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageTitle :title="t('admin.analytics.title')" :subtitle="t('admin.analytics.subtitle')" />

    <LoadingSpinner v-if="loading" />

    <div v-else class="analytics-grid">
      <div v-for="card in statCards" :key="card.key" class="card stat-card">
        <i :class="card.icon" class="stat-icon" />
        <span class="stat-label">{{ t(card.labelKey) }}</span>
        <span class="stat-value">{{ stats[card.key] ?? 0 }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analytics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 1.5rem 1rem;
  gap: 0.5rem;
}

.stat-icon {
  font-size: 1.75rem;
  color: var(--mw-primary);
}

.stat-label {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--mw-text);
}
</style>
