<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useJobboardStore } from '@/stores/jobboard'
import { useCalendarStore } from '@/stores/calendar'
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
const { formatShortDate } = useLocaleDate()
const router = useRouter()
const auth = useAuthStore()
const admin = useAdminStore()
const jobboard = useJobboardStore()
const calendar = useCalendarStore()
const activeTab = ref('0')
const selectedCategory = ref<string | null>(null)
const selectedEventId = ref<string | null>(null)
const calendarEnabled = admin.isModuleEnabled('calendar')

onMounted(async () => {
  const promises: Promise<void>[] = [
    jobboard.fetchJobs(true),
    jobboard.fetchCategories(),
    jobboard.fetchMyAssignments(),
  ]
  if (calendarEnabled) {
    const now = new Date()
    const from = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const to = new Date(now.getFullYear(), now.getMonth() + 6, 0)
    const formatDate = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    promises.push(calendar.fetchEvents(formatDate(from), formatDate(to)))
  }
  await Promise.all(promises)
})

function applyFilters() {
  jobboard.fetchJobs(true, selectedCategory.value ?? undefined, selectedEventId.value ?? undefined)
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
  return formatShortDate(date)
}
</script>

<template>
  <div>
    <div class="page-header">
      <PageTitle :title="t('jobboard.title')" />
      <Button
        v-if="!auth.isStudent"
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
              @change="applyFilters"
              class="category-filter"
            />
            <Select
              v-if="calendarEnabled && calendar.events.length"
              v-model="selectedEventId"
              :options="[{ label: t('jobboard.allEvents'), value: null }, ...calendar.events.map(e => ({ label: e.title, value: e.id }))]"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('jobboard.filterByEvent')"
              @change="applyFilters"
              class="event-filter"
            />
          </div>

          <LoadingSpinner v-if="jobboard.loading && !jobboard.jobs.length" />
          <EmptyState
            v-else-if="!jobboard.jobs.length"
            icon="pi pi-briefcase"
            :message="t('jobboard.noJobs')"
          />

          <div v-else class="job-list">
            <router-link
              v-for="job in jobboard.jobs"
              :key="job.id"
              :to="{ name: 'job-detail', params: { id: job.id } }"
              class="job-card card"
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
                <span v-if="job.eventTitle"><i class="pi pi-calendar-plus" /> {{ job.eventTitle }}</span>
              </div>
              <div class="job-card-footer">
                <span class="assignees">{{ job.currentAssignees }}/{{ job.maxAssignees }} {{ t('jobboard.assignees') }}</span>
                <span class="creator">{{ job.creatorName }}</span>
              </div>
            </router-link>

            <div v-if="jobboard.hasMore" class="load-more">
              <Button
                :label="t('feed.loadMore')"
                :loading="jobboard.loading"
                severity="secondary"
                text
                @click="jobboard.fetchJobs(false, selectedCategory ?? undefined, selectedEventId ?? undefined)"
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
            <router-link
              v-for="a in jobboard.myAssignments"
              :key="a.id"
              :to="{ name: 'job-detail', params: { id: a.jobId } }"
              class="assignment-card card"
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
            </router-link>
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
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.category-filter,
.event-filter {
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
  text-decoration: none;
  color: inherit;
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
  text-decoration: none;
  color: inherit;
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
