<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { useJobboardStore } from '@/stores/jobboard'
import { jobboardApi } from '@/api/jobboard.api'
import { useCalendarStore } from '@/stores/calendar'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
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
const selectedFromDate = ref<Date | null>(null)
const selectedToDate = ref<Date | null>(null)
const calendarEnabled = admin.isModuleEnabled('calendar')
const completedJobs = ref<import('@/types/jobboard').JobInfo[]>([])
const completedLoading = ref(false)
const canSeeCompleted = computed(() => auth.isAdmin || auth.isSectionAdmin || auth.isTeacher)
const toast = useToast()
const showPendingTab = computed(() =>
  canSeeCompleted.value && admin.config?.requireAssignmentConfirmation !== false
)
const pendingLoaded = ref(false)
const confirmingId = ref<string | null>(null)
const rejectingId = ref<string | null>(null)

watch(activeTab, (val) => {
  if (val === 'pending' && !pendingLoaded.value) {
    pendingLoaded.value = true
    jobboard.fetchPendingConfirmations()
  }
})

async function handleConfirm(assignmentId: string) {
  confirmingId.value = assignmentId
  try {
    await jobboard.confirmAssignment(assignmentId)
    toast.add({ severity: 'success', summary: t('jobboard.hoursConfirmed'), life: 3000 })
  } finally {
    confirmingId.value = null
  }
}

async function handleReject(assignmentId: string) {
  rejectingId.value = assignmentId
  try {
    await jobboard.rejectAssignment(assignmentId)
    toast.add({ severity: 'warn', summary: t('jobboard.hoursRejected'), life: 3000 })
  } finally {
    rejectingId.value = null
  }
}

onMounted(async () => {
  const promises: Promise<any>[] = [
    jobboard.fetchJobs(true),
    jobboard.fetchMyAssignments(),
  ]
  if (calendarEnabled) {
    const now = new Date()
    const from = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const to = new Date(now.getFullYear(), now.getMonth() + 6, 0)
    const formatDate = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    promises.push(calendar.fetchEvents(formatDate(from), formatDate(to)))
  }
  if (canSeeCompleted.value) {
    promises.push(fetchCompletedJobs())
  }
  await Promise.all(promises)
})

function formatIsoDate(d: Date | null): string | undefined {
  if (!d) return undefined
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function applyFilters() {
  jobboard.fetchJobs(
    true,
    selectedCategory.value ?? undefined,
    selectedEventId.value ?? undefined,
    undefined,
    formatIsoDate(selectedFromDate.value),
    formatIsoDate(selectedToDate.value),
  )
}

function clearDateFilter() {
  selectedFromDate.value = null
  selectedToDate.value = null
  applyFilters()
}

async function fetchCompletedJobs() {
  completedLoading.value = true
  try {
    const res = await jobboardApi.listJobs(0, 100, undefined, ['COMPLETED'] as any)
    completedJobs.value = res.data.data.content
  } catch {
    completedJobs.value = []
  } finally {
    completedLoading.value = false
  }
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

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="0">{{ t('jobboard.openJobs') }}</Tab>
        <Tab value="1">{{ t('jobboard.myAssignments') }}</Tab>
        <Tab v-if="showPendingTab" value="pending">
          {{ t('jobboard.pendingTab') }}
          <span v-if="jobboard.pendingConfirmations.length" class="pending-badge">{{ jobboard.pendingConfirmations.length }}</span>
        </Tab>
        <Tab v-if="canSeeCompleted" value="2">{{ t('jobboard.completedJobs') }}</Tab>
      </TabList>
      <TabPanels>
        <!-- Open Jobs -->
        <TabPanel value="0">
          <div class="filter-bar">
            <Select
              v-model="selectedCategory"
              :options="[
                { label: t('jobboard.allCategories'), value: null },
                { label: t('jobboard.categoryNormal'), value: 'Normal' },
                { label: t('jobboard.categoryReinigung'), value: 'Reinigung' }
              ]"
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
            <DatePicker
              v-model="selectedFromDate"
              :placeholder="t('jobboard.fromDate')"
              dateFormat="dd.mm.yy"
              showIcon
              :showButtonBar="true"
              @date-select="applyFilters"
              @clear-click="applyFilters"
              class="date-filter"
            />
            <DatePicker
              v-model="selectedToDate"
              :placeholder="t('jobboard.toDate')"
              dateFormat="dd.mm.yy"
              showIcon
              :showButtonBar="true"
              :minDate="selectedFromDate ?? undefined"
              @date-select="applyFilters"
              @clear-click="applyFilters"
              class="date-filter"
            />
            <Button
              v-if="selectedFromDate || selectedToDate"
              :label="t('jobboard.clearDateFilter')"
              icon="pi pi-times"
              severity="secondary"
              text
              size="small"
              @click="clearDateFilter"
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
                @click="jobboard.fetchJobs(false, selectedCategory ?? undefined, selectedEventId ?? undefined, undefined, formatIsoDate(selectedFromDate), formatIsoDate(selectedToDate))"
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

        <!-- Pending Confirmations -->
        <TabPanel v-if="showPendingTab" value="pending">
          <LoadingSpinner v-if="!pendingLoaded" />
          <EmptyState
            v-else-if="!jobboard.pendingConfirmations.length"
            icon="pi pi-check-circle"
            :message="t('jobboard.noPendingConfirmations')"
          />
          <div v-else class="assignments-list">
            <div
              v-for="a in jobboard.pendingConfirmations"
              :key="a.id"
              class="assignment-card card"
            >
              <div class="assignment-header">
                <router-link :to="{ name: 'job-detail', params: { id: a.jobId } }" class="job-link">
                  <h3>{{ a.jobTitle }}</h3>
                </router-link>
              </div>
              <div class="assignment-meta">
                <span><i class="pi pi-user" /> {{ a.userName }}</span>
                <span><i class="pi pi-users" /> {{ a.familyName }}</span>
                <span v-if="a.actualHours"><i class="pi pi-clock" /> {{ a.actualHours }}h</span>
                <span v-if="a.completedAt"><i class="pi pi-calendar" /> {{ formatDate(a.completedAt) }}</span>
              </div>
              <div class="pending-actions">
                <Button
                  :label="t('jobboard.reject')"
                  icon="pi pi-times"
                  size="small"
                  severity="danger"
                  outlined
                  :loading="rejectingId === a.id"
                  @click="handleReject(a.id)"
                />
                <Button
                  :label="t('common.confirm')"
                  icon="pi pi-check"
                  size="small"
                  :loading="confirmingId === a.id"
                  @click="handleConfirm(a.id)"
                />
              </div>
            </div>
          </div>
        </TabPanel>

        <!-- Completed Jobs (Admin/Teacher) -->
        <TabPanel v-if="canSeeCompleted" value="2">
          <LoadingSpinner v-if="completedLoading" />
          <EmptyState
            v-else-if="!completedJobs.length"
            icon="pi pi-check-circle"
            :message="t('jobboard.noCompletedJobs')"
          />
          <div v-else class="job-list">
            <router-link
              v-for="job in completedJobs"
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
              </div>
              <div class="job-card-footer">
                <span class="assignees">{{ job.currentAssignees }}/{{ job.maxAssignees }} {{ t('jobboard.assignees') }}</span>
                <span class="creator">{{ job.creatorName }}</span>
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

.date-filter {
  min-width: 150px;
  max-width: 180px;
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

.pending-badge {
  background: var(--p-orange-500);
  color: white;
  border-radius: 999px;
  font-size: var(--mw-font-size-xs);
  padding: 0.1rem 0.45rem;
  margin-left: 0.35rem;
  font-weight: 600;
}

.pending-actions {
  margin-top: 0.5rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.job-link {
  text-decoration: none;
  color: inherit;
}

.job-link:hover h3 {
  text-decoration: underline;
}
</style>
