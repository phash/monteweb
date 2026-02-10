<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useJobboardStore } from '@/stores/jobboard'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const jobboard = useJobboardStore()
const activeTab = ref('0')
const selectedCategory = ref<string | null>(null)

onMounted(async () => {
  await Promise.all([
    jobboard.fetchJobs(true),
    jobboard.fetchCategories(),
    jobboard.fetchMyAssignments(),
  ])
})

function filterByCategory() {
  jobboard.fetchJobs(true, selectedCategory.value ?? undefined)
}

function statusSeverity(status: string) {
  switch (status) {
    case 'OPEN': return 'success'
    case 'ASSIGNED': return 'info'
    case 'IN_PROGRESS': return 'warn'
    case 'COMPLETED': return 'secondary'
    case 'CANCELLED': return 'danger'
    default: return 'info'
  }
}

function formatDate(date: string | null) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('de-DE')
}
</script>

<template>
  <div>
    <div class="page-header">
      <PageTitle :title="t('jobboard.title')" />
      <Button
        v-if="auth.isTeacher || auth.isAdmin"
        :label="t('jobboard.create')"
        icon="pi pi-plus"
        @click="router.push({ name: 'job-create' })"
      />
    </div>

    <Tabs :value="activeTab">
      <TabList>
        <Tab value="0">{{ t('jobboard.openJobs') }}</Tab>
        <Tab value="1">{{ t('jobboard.myAssignments') }}</Tab>
      </TabList>
      <TabPanels>
        <!-- Open Jobs -->
        <TabPanel value="0">
          <div class="filter-bar">
            <Select
              v-model="selectedCategory"
              :options="[{ label: t('jobboard.allCategories'), value: null }, ...jobboard.categories.map(c => ({ label: c, value: c }))]"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('jobboard.filterCategory')"
              @change="filterByCategory"
              class="category-filter"
            />
          </div>

          <LoadingSpinner v-if="jobboard.loading && !jobboard.jobs.length" />
          <EmptyState
            v-else-if="!jobboard.jobs.length"
            icon="pi pi-briefcase"
            :message="t('jobboard.noJobs')"
          />

          <div v-else class="job-list">
            <div
              v-for="job in jobboard.jobs"
              :key="job.id"
              class="job-card card"
              @click="router.push({ name: 'job-detail', params: { id: job.id } })"
            >
              <div class="job-card-header">
                <h3>{{ job.title }}</h3>
                <Tag :value="t(`jobboard.statuses.${job.status}`)" :severity="statusSeverity(job.status)" size="small" />
              </div>
              <div class="job-card-meta">
                <span><i class="pi pi-tag" /> {{ job.category }}</span>
                <span v-if="job.location"><i class="pi pi-map-marker" /> {{ job.location }}</span>
                <span><i class="pi pi-clock" /> {{ job.estimatedHours }}h</span>
                <span v-if="job.scheduledDate"><i class="pi pi-calendar" /> {{ formatDate(job.scheduledDate) }}</span>
              </div>
              <div class="job-card-footer">
                <span class="assignees">{{ job.currentAssignees }}/{{ job.maxAssignees }} {{ t('jobboard.assignees') }}</span>
                <span class="creator">{{ job.creatorName }}</span>
              </div>
            </div>

            <div v-if="jobboard.hasMore" class="load-more">
              <Button
                :label="t('feed.loadMore')"
                :loading="jobboard.loading"
                severity="secondary"
                text
                @click="jobboard.fetchJobs()"
              />
            </div>
          </div>
        </TabPanel>

        <!-- My Assignments -->
        <TabPanel value="1">
          <EmptyState
            v-if="!jobboard.myAssignments.length"
            icon="pi pi-check-circle"
            :message="t('jobboard.noAssignments')"
          />
          <div v-else class="assignments-list">
            <div
              v-for="a in jobboard.myAssignments"
              :key="a.id"
              class="assignment-card card"
              @click="router.push({ name: 'job-detail', params: { id: a.jobId } })"
            >
              <div class="assignment-header">
                <h3>{{ a.jobTitle }}</h3>
                <Tag :value="t(`jobboard.assignmentStatuses.${a.status}`)" :severity="statusSeverity(a.status)" size="small" />
              </div>
              <div class="assignment-meta">
                <span v-if="a.actualHours">{{ a.actualHours }}h {{ t('jobboard.hoursWorked') }}</span>
                <Tag v-if="a.confirmed" :value="t('jobboard.confirmed')" severity="success" size="small" />
                <Tag v-else-if="a.status === 'COMPLETED'" :value="t('jobboard.pendingConfirmation')" severity="warn" size="small" />
              </div>
            </div>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.filter-bar {
  margin-bottom: 1rem;
}

.category-filter {
  min-width: 200px;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.job-card {
  cursor: pointer;
  transition: box-shadow 0.15s;
}

.job-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.job-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.job-card-header h3 {
  font-size: var(--mw-font-size-md);
}

.job-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-bottom: 0.5rem;
}

.job-card-meta i {
  margin-right: 0.25rem;
}

.job-card-footer {
  display: flex;
  justify-content: space-between;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.assignments-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.assignment-card {
  cursor: pointer;
}

.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.assignment-header h3 {
  font-size: var(--mw-font-size-md);
}

.assignment-meta {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-top: 0.5rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 1rem;
}
</style>
