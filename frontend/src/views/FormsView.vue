<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useFormsStore } from '@/stores/forms'
import { useAuthStore } from '@/stores/auth'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import ProgressBar from 'primevue/progressbar'

const { t } = useI18n()
const router = useRouter()
const forms = useFormsStore()
const auth = useAuthStore()

const activeTab = ref('0')

onMounted(() => {
  forms.fetchAvailableForms()
  forms.fetchMyForms()
})

function typeSeverity(type: string): 'info' | 'warn' {
  return type === 'CONSENT' ? 'warn' : 'info'
}

function scopeSeverity(scope: string): 'info' | 'success' | 'warn' | 'secondary' {
  switch (scope) {
    case 'SCHOOL': return 'warn'
    case 'SECTION': return 'info'
    case 'ROOM': return 'success'
    default: return 'secondary'
  }
}

function statusSeverity(status: string): 'info' | 'success' | 'warn' | 'danger' | 'secondary' {
  switch (status) {
    case 'DRAFT': return 'secondary'
    case 'PUBLISHED': return 'success'
    case 'CLOSED': return 'warn'
    case 'ARCHIVED': return 'danger'
    default: return 'secondary'
  }
}

function responseProgress(form: { responseCount: number; targetCount: number }) {
  if (form.targetCount === 0) return 0
  return Math.round((form.responseCount / form.targetCount) * 100)
}
</script>

<template>
  <div>
    <div class="forms-header">
      <PageTitle :title="t('forms.title')" />
      <Button
        :label="t('forms.create')"
        icon="pi pi-plus"
        @click="router.push({ name: 'form-create' })"
      />
    </div>

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="0">{{ t('forms.tabs.available') }}</Tab>
        <Tab value="1">{{ t('forms.tabs.mine') }}</Tab>
      </TabList>
      <TabPanels>
        <!-- Available forms tab -->
        <TabPanel value="0">
      <LoadingSpinner v-if="forms.loading && !forms.availableForms.length" />
      <EmptyState
        v-else-if="!forms.availableForms.length"
        icon="pi pi-list-check"
        :message="t('forms.noForms')"
      />
      <div v-else class="form-list">
        <router-link
          v-for="form in forms.availableForms"
          :key="form.id"
          :to="{ name: 'form-detail', params: { id: form.id } }"
          class="form-item card"
        >
          <div class="form-info">
            <div class="form-title-row">
              <strong>{{ form.title }}</strong>
              <Tag :value="t(`forms.types.${form.type}`)" :severity="typeSeverity(form.type)" size="small" />
              <Tag :value="t(`forms.scopes.${form.scope}`)" :severity="scopeSeverity(form.scope)" size="small" />
              <Tag v-if="form.hasUserResponded" :value="t('forms.responded')" severity="success" size="small" icon="pi pi-check" />
              <Tag v-if="form.anonymous" :value="t('forms.anonymous')" severity="secondary" size="small" icon="pi pi-eye-slash" />
            </div>
            <div class="form-meta">
              <span v-if="form.scopeName">{{ form.scopeName }}</span>
              <span v-if="form.deadline" class="separator">·</span>
              <span v-if="form.deadline"><i class="pi pi-calendar" /> {{ new Date(form.deadline).toLocaleDateString('de-DE') }}</span>
              <span class="separator">·</span>
              <span>{{ form.questionCount }} {{ t('forms.questionsCount') }}</span>
              <span class="separator">·</span>
              <span>{{ form.responseCount }} {{ t('forms.responsesCount') }}</span>
            </div>
            <ProgressBar
              v-if="form.targetCount > 0"
              :value="responseProgress(form)"
              :showValue="true"
              class="response-progress"
            />
          </div>
          <i class="pi pi-chevron-right form-arrow" />
        </router-link>
      </div>
        </TabPanel>

        <!-- My forms tab -->
        <TabPanel value="1">
      <LoadingSpinner v-if="forms.loading && !forms.myForms.length" />
      <EmptyState
        v-else-if="!forms.myForms.length"
        icon="pi pi-list-check"
        :message="t('forms.noMyForms')"
      />
      <div v-else class="form-list">
        <router-link
          v-for="form in forms.myForms"
          :key="form.id"
          :to="{ name: 'form-detail', params: { id: form.id } }"
          class="form-item card"
        >
          <div class="form-info">
            <div class="form-title-row">
              <strong>{{ form.title }}</strong>
              <Tag :value="t(`forms.types.${form.type}`)" :severity="typeSeverity(form.type)" size="small" />
              <Tag :value="t(`forms.statuses.${form.status}`)" :severity="statusSeverity(form.status)" size="small" />
            </div>
            <div class="form-meta">
              <span>{{ form.responseCount }} {{ t('forms.responsesCount') }}</span>
              <span v-if="form.deadline" class="separator">·</span>
              <span v-if="form.deadline"><i class="pi pi-calendar" /> {{ new Date(form.deadline).toLocaleDateString('de-DE') }}</span>
            </div>
          </div>
          <i class="pi pi-chevron-right form-arrow" />
        </router-link>
      </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.forms-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.form-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.15s;
  gap: 1rem;
  text-decoration: none;
  color: inherit;
}

.form-item:hover {
  background: var(--mw-bg-hover);
}

.form-info {
  flex: 1;
  min-width: 0;
}

.form-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.form-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  margin-top: 0.25rem;
}

.separator {
  margin: 0 0.25rem;
}

.form-arrow {
  color: var(--mw-text-muted);
  font-size: 0.75rem;
}

.response-progress {
  margin-top: 0.5rem;
  height: 0.5rem;
}
</style>
