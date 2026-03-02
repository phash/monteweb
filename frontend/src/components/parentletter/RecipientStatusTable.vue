<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import type { ParentLetterRecipientInfo, RecipientStatus } from '@/types/parentletter'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

const props = defineProps<{
  recipients: ParentLetterRecipientInfo[]
}>()

const { t } = useI18n()
const { formatCompactDateTime, formatShortDate } = useLocaleDate()

function statusSeverity(status: RecipientStatus): 'danger' | 'info' | 'success' {
  switch (status) {
    case 'OPEN': return 'danger'
    case 'READ': return 'info'
    case 'CONFIRMED': return 'success'
  }
}

const confirmedCount = computed(() =>
  props.recipients.filter(r => r.status === 'CONFIRMED').length
)

const totalCount = computed(() => props.recipients.length)

const readCount = computed(() =>
  props.recipients.filter(r => r.status === 'READ' || r.status === 'CONFIRMED').length
)
</script>

<template>
  <div class="recipient-table-wrapper">
    <!-- Summary row -->
    <div class="summary-bar">
      <span class="summary-item">
        <i class="pi pi-users" />
        {{ t('parentLetters.recipientTable.total') }}: <strong>{{ totalCount }}</strong>
      </span>
      <span class="summary-item">
        <i class="pi pi-eye" />
        {{ t('parentLetters.recipientTable.read') }}: <strong>{{ readCount }}</strong>
      </span>
      <span class="summary-item">
        <i class="pi pi-check-circle" />
        {{ t('parentLetters.recipientTable.confirmed') }}: <strong>{{ confirmedCount }}</strong>
      </span>
    </div>

    <DataTable
      :value="recipients"
      size="small"
      stripedRows
      :paginator="recipients.length > 20"
      :rows="20"
      class="recipient-datatable"
      :emptyMessage="t('parentLetters.recipientTable.empty')"
    >
      <Column field="studentName" :header="t('parentLetters.recipientTable.student')" sortable>
        <template #body="{ data }">
          <span class="name-cell">
            <i class="pi pi-user name-icon" />
            {{ data.studentName }}
          </span>
        </template>
      </Column>

      <Column field="parentName" :header="t('parentLetters.recipientTable.parent')" sortable>
        <template #body="{ data }">
          {{ data.parentName }}
        </template>
      </Column>

      <Column field="familyName" :header="t('parentLetters.recipientTable.family')" sortable>
        <template #body="{ data }">
          {{ data.familyName }}
        </template>
      </Column>

      <Column field="status" :header="t('parentLetters.recipientTable.status')" sortable style="width: 120px">
        <template #body="{ data }">
          <Tag
            :value="t(`parentLetters.recipientStatuses.${data.status}`)"
            :severity="statusSeverity(data.status)"
            size="small"
          />
        </template>
      </Column>

      <Column field="readAt" :header="t('parentLetters.recipientTable.readAt')" sortable style="width: 160px">
        <template #body="{ data }">
          <span v-if="data.readAt" class="date-cell">
            {{ formatCompactDateTime(data.readAt) }}
          </span>
          <span v-else class="not-yet">—</span>
        </template>
      </Column>

      <Column field="confirmedAt" :header="t('parentLetters.recipientTable.confirmedAt')" sortable style="width: 160px">
        <template #body="{ data }">
          <span v-if="data.confirmedAt" class="date-cell confirmed-date">
            {{ formatCompactDateTime(data.confirmedAt) }}
            <span v-if="data.confirmedByName" class="confirmed-by">
              ({{ data.confirmedByName }})
            </span>
          </span>
          <span v-else class="not-yet">—</span>
        </template>
      </Column>

      <Column field="reminderSentAt" :header="t('parentLetters.recipientTable.reminderSent')" style="width: 130px">
        <template #body="{ data }">
          <span v-if="data.reminderSentAt" class="date-cell">
            <i class="pi pi-bell" style="margin-right: 0.25rem;" />
            {{ formatShortDate(data.reminderSentAt) }}
          </span>
          <span v-else class="not-yet">—</span>
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.recipient-table-wrapper {
  overflow: auto;
}

.summary-bar {
  display: flex;
  gap: 2rem;
  padding: 0.75rem 1rem;
  background: var(--mw-bg-hover);
  border-radius: var(--mw-border-radius, 6px);
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

.summary-item i {
  color: var(--mw-text-muted);
}

.summary-item strong {
  color: var(--mw-text-primary);
}

.recipient-datatable {
  width: 100%;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.name-icon {
  color: var(--mw-text-muted);
  font-size: 0.8rem;
}

.date-cell {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-secondary);
}

.confirmed-date {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.confirmed-by {
  font-size: 0.7rem;
  color: var(--mw-text-muted);
}

.not-yet {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
}
</style>
