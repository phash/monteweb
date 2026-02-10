<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { usersApi } from '@/api/users.api'
import type { UserInfo } from '@/types/user'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Button from 'primevue/button'

const { t } = useI18n()

const users = ref<UserInfo[]>([])
const totalRecords = ref(0)
const loading = ref(false)
const page = ref(0)
const rows = ref(20)

async function loadUsers() {
  loading.value = true
  try {
    const res = await usersApi.list({ page: page.value, size: rows.value })
    users.value = res.data.data.content
    totalRecords.value = res.data.data.totalElements
  } finally {
    loading.value = false
  }
}

function onPage(event: { page: number; rows: number }) {
  page.value = event.page
  rows.value = event.rows
  loadUsers()
}

function roleSeverity(role: string): string {
  const map: Record<string, string> = {
    SUPERADMIN: 'danger',
    SECTION_ADMIN: 'warn',
    TEACHER: 'info',
    PARENT: 'success',
    STUDENT: 'secondary',
  }
  return map[role] ?? 'secondary'
}

onMounted(loadUsers)
</script>

<template>
  <div>
    <PageTitle :title="t('admin.users')" />

    <LoadingSpinner v-if="loading && users.length === 0" />

    <DataTable
      v-else
      :value="users"
      :loading="loading"
      :paginator="true"
      :rows="rows"
      :totalRecords="totalRecords"
      :lazy="true"
      @page="onPage"
      stripedRows
      class="card"
    >
      <Column field="displayName" :header="t('common.name')" />
      <Column field="email" :header="t('auth.email')" />
      <Column field="role" :header="t('admin.columnRole')">
        <template #body="{ data }">
          <Tag :value="data.role" :severity="roleSeverity(data.role) as any" />
        </template>
      </Column>
      <Column field="active" :header="t('common.status')">
        <template #body="{ data }">
          <Tag :value="data.active ? t('common.active') : t('common.inactive')" :severity="data.active ? 'success' : 'danger'" />
        </template>
      </Column>
      <Column :header="t('common.actions')" style="width: 100px">
        <template #body>
          <Button icon="pi pi-pencil" severity="secondary" text size="small" />
        </template>
      </Column>
    </DataTable>
  </div>
</template>
