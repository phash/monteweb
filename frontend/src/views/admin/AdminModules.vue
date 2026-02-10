<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAdminStore } from '@/stores/admin'
import PageTitle from '@/components/common/PageTitle.vue'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Message from 'primevue/message'

const { t } = useI18n()
const admin = useAdminStore()

const modules = ref<Record<string, boolean>>({})
const saved = ref(false)

function moduleDescription(name: string): string {
  const key = `admin.moduleDescriptions.${name}`
  return t(key)
}

onMounted(async () => {
  await admin.fetchConfig()
  if (admin.config?.modules) {
    modules.value = { ...admin.config.modules }
  }
})

async function save() {
  await admin.updateModules(modules.value)
  saved.value = true
  setTimeout(() => { saved.value = false }, 3000)
}
</script>

<template>
  <div>
    <PageTitle :title="t('admin.modules')" :subtitle="t('admin.moduleSubtitle')" />

    <Message v-if="saved" severity="success" :closable="false">
      {{ t('admin.moduleSaved') }}
    </Message>

    <div class="modules-list card">
      <div v-for="(enabled, name) in modules" :key="name" class="module-item">
        <div class="module-info">
          <h3>{{ name }}</h3>
          <p>{{ moduleDescription(name as string) }}</p>
        </div>
        <ToggleSwitch v-model="modules[name as string]" />
      </div>
    </div>

    <Button :label="t('common.save')" @click="save" class="mt-1" />
  </div>
</template>

<style scoped>
.modules-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
}

.module-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.module-item:last-child {
  border-bottom: none;
}

.module-info h3 {
  font-size: var(--mw-font-size-md);
  text-transform: capitalize;
}

.module-info p {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-top: 0.125rem;
}

.mt-1 {
  margin-top: 1rem;
}
</style>
