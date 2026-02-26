<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFamilyStore } from '@/stores/family'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'
import type { FamilyInvitationInfo } from '@/types/family'
import type { CalendarEvent } from '@/types/calendar'
import PageTitle from '@/components/common/PageTitle.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import FamilyHoursWidget from '@/components/family/FamilyHoursWidget.vue'
import InviteMemberDialog from '@/components/family/InviteMemberDialog.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import Checkbox from 'primevue/checkbox'
import { familyApi } from '@/api/family.api'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const family = useFamilyStore()
const admin = useAdminStore()
const auth = useAuthStore()
const toast = useToast()

const calendarEnabled = computed(() => admin.config?.modules?.calendar ?? false)
const soleCustodyEnabled = computed(() => admin.config?.soleCustodyEnabled ?? false)
const isAdminOrSectionAdmin = computed(() => auth.isAdmin || auth.isSectionAdmin)

const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const showInviteDialog = ref(false)
const showLeaveDialog = ref(false)
const leaveFamilyId = ref('')
const inviteFamilyId = ref('')
const familyName = ref('')
const inviteCode = ref('')
const generatedCode = ref('')

// Invitations
const myInvitations = ref<FamilyInvitationInfo[]>([])
const sentInvitations = ref<FamilyInvitationInfo[]>([])

// Calendar
const familyCalendarEvents = ref<Record<string, CalendarEvent[]>>({})
const calendarLoading = ref(false)

onMounted(async () => {
  await family.fetchFamilies()
  await loadInvitations()
  await loadFamilyCalendars()
})

async function loadInvitations() {
  try {
    const res = await familyApi.getMyInvitations()
    myInvitations.value = res.data.data
  } catch {
    myInvitations.value = []
  }

  // Load sent invitations for each family
  sentInvitations.value = []
  for (const fam of family.families) {
    try {
      const res = await familyApi.getFamilyInvitations(fam.id)
      sentInvitations.value.push(...res.data.data)
    } catch {
      // ignore
    }
  }
}

async function createFamily() {
  if (!familyName.value.trim()) return
  await family.createFamily(familyName.value.trim())
  familyName.value = ''
  showCreateDialog.value = false
}

async function joinFamily() {
  if (!inviteCode.value.trim()) return
  await family.joinFamily(inviteCode.value.trim())
  inviteCode.value = ''
  showJoinDialog.value = false
}

async function generateCode(familyId: string) {
  const res = await familyApi.generateInviteCode(familyId)
  generatedCode.value = res.data.data.inviteCode
}

function copyCode() {
  window.navigator.clipboard.writeText(generatedCode.value)
}

function openInviteDialog(familyId: string) {
  inviteFamilyId.value = familyId
  showInviteDialog.value = true
}

async function onInviteSent() {
  toast.add({ severity: 'success', summary: t('family.invitationSent'), life: 3000 })
  await loadInvitations()
}

async function acceptInvitation(id: string) {
  try {
    await familyApi.acceptInvitation(id)
    toast.add({ severity: 'success', summary: t('family.invitationAccepted'), life: 3000 })
    await family.fetchFamilies()
    await loadInvitations()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function declineInvitation(id: string) {
  try {
    await familyApi.declineInvitation(id)
    toast.add({ severity: 'success', summary: t('family.invitationDeclined'), life: 3000 })
    await loadInvitations()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function openLeaveDialog(familyId: string) {
  leaveFamilyId.value = familyId
  showLeaveDialog.value = true
}

async function leaveFamily() {
  try {
    await familyApi.leaveFamily(leaveFamilyId.value)
    toast.add({ severity: 'success', summary: t('family.leftFamily'), life: 3000 })
    showLeaveDialog.value = false
    await family.fetchFamilies()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function handleFamilyAvatarUpload(familyId: string, file: File) {
  await familyApi.uploadAvatar(familyId, file)
  await family.fetchFamilies()
}

async function handleFamilyAvatarRemove(familyId: string) {
  await familyApi.removeAvatar(familyId)
  await family.fetchFamilies()
}

// Calendar functions
async function loadFamilyCalendars() {
  if (!calendarEnabled.value) return
  calendarLoading.value = true
  try {
    const now = new Date()
    const from = now.toISOString().split('T')[0]!
    const futureDate = new Date(now)
    futureDate.setDate(futureDate.getDate() + 30)
    const to = futureDate.toISOString().split('T')[0]!

    for (const fam of family.families) {
      try {
        const res = await familyApi.getFamilyCalendar(fam.id, from, to)
        familyCalendarEvents.value[fam.id] = res.data.data
      } catch {
        familyCalendarEvents.value[fam.id] = []
      }
    }
  } finally {
    calendarLoading.value = false
  }
}

async function downloadIcal(familyId: string) {
  try {
    const res = await familyApi.downloadFamilyIcal(familyId)
    const blob = new Blob([res.data], { type: 'text/calendar' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'family-calendar.ics'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch {
    toast.add({ severity: 'error', summary: 'Download failed', life: 5000 })
  }
}

async function toggleSoleCustody(familyId: string, value: boolean) {
  try {
    await familyApi.requestSoleCustody(familyId, value)
    toast.add({ severity: 'success', summary: t('family.soleCustodyRequested'), life: 3000 })
    await family.fetchFamilies()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function approveSoleCustody(familyId: string) {
  try {
    await familyApi.approveSoleCustody(familyId)
    toast.add({ severity: 'success', summary: t('family.soleCustodyApproved'), life: 3000 })
    await family.fetchFamilies()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function formatEventDate(event: CalendarEvent): string {
  const date = new Date(event.startDate + 'T00:00:00')
  const formatted = date.toLocaleDateString(undefined, {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
  if (event.allDay) return formatted
  if (event.startTime) return `${formatted}, ${event.startTime.substring(0, 5)}`
  return formatted
}
</script>

<template>
  <div>
    <PageTitle :title="t('family.title')" />

    <!-- Pending invitations for current user -->
    <div v-if="myInvitations.length > 0" class="invitations-card card">
      <h3>{{ t('family.pendingInvitations') }}</h3>
      <div v-for="inv in myInvitations" :key="inv.id" class="invitation-item">
        <div class="invitation-info">
          <i class="pi pi-envelope" />
          <div>
            <span>{{ t('family.invitedBy', { name: inv.inviterName }) }}</span>
            <p class="text-sm text-muted">{{ inv.familyName }} &middot; {{ t(`family.roles.${inv.role}`) }}</p>
          </div>
        </div>
        <div class="invitation-actions">
          <Button :label="t('family.acceptInvitation')" icon="pi pi-check" size="small"
                  @click="acceptInvitation(inv.id)" />
          <Button :label="t('family.declineInvitation')" icon="pi pi-times" size="small"
                  severity="secondary" text @click="declineInvitation(inv.id)" />
        </div>
      </div>
    </div>

    <LoadingSpinner v-if="family.loading" />

    <template v-else-if="family.hasFamily">
      <div v-for="fam in family.families" :key="fam.id" class="family-card card">
        <AvatarUpload
          :image-url="fam.avatarUrl"
          size="md"
          icon="pi-users"
          :editable="true"
          @upload="(file: File) => handleFamilyAvatarUpload(fam.id, file)"
          @remove="() => handleFamilyAvatarRemove(fam.id)"
        />
        <FamilyHoursWidget :familyId="fam.id" />
        <div class="family-header">
          <h2>{{ fam.name }}</h2>
          <div class="family-header-actions">
            <Button
              icon="pi pi-user-plus"
              :label="t('family.inviteMember')"
              severity="secondary"
              size="small"
              @click="openInviteDialog(fam.id)"
            />
            <Button
              icon="pi pi-link"
              :label="t('family.generateCode')"
              severity="secondary"
              size="small"
              @click="generateCode(fam.id)"
            />
            <Button
              icon="pi pi-sign-out"
              :label="t('family.leave')"
              severity="danger"
              text
              size="small"
              @click="openLeaveDialog(fam.id)"
            />
          </div>
        </div>

        <div v-if="generatedCode" class="invite-code-display">
          <span class="code">{{ generatedCode }}</span>
          <Button
            icon="pi pi-copy"
            severity="secondary"
            text
            size="small"
            @click="copyCode"
          />
        </div>

        <!-- Sent invitations -->
        <div v-if="sentInvitations.filter(i => i.familyId === fam.id).length > 0" class="sent-invitations">
          <h4>{{ t('family.sentInvitations') }}</h4>
          <div v-for="inv in sentInvitations.filter(i => i.familyId === fam.id)" :key="inv.id" class="sent-item">
            <i class="pi pi-send" />
            <span>{{ inv.inviteeName }}</span>
            <Tag :value="t(`family.roles.${inv.role}`)" severity="secondary" size="small" />
            <Tag :value="t('family.pending')" severity="warn" size="small" />
          </div>
        </div>

        <h3>{{ t('family.members') }}</h3>
        <div class="members-list">
          <div v-for="member in fam.members" :key="member.userId" class="member-item">
            <i class="pi pi-user" />
            <span>{{ member.displayName }}</span>
            <Tag :value="t(`family.roles.${member.role}`)" :severity="member.role === 'PARENT' ? 'info' : 'secondary'" size="small" />
          </div>
        </div>

        <!-- Family Calendar -->
        <div v-if="calendarEnabled" class="family-calendar-section">
          <div class="calendar-header">
            <h3><i class="pi pi-calendar" /> {{ t('family.calendar') }}</h3>
            <Button
              icon="pi pi-download"
              :label="t('family.downloadIcal')"
              severity="secondary"
              size="small"
              @click="downloadIcal(fam.id)"
            />
          </div>
          <p class="calendar-desc text-sm text-muted">{{ t('family.calendarDesc') }}</p>

          <div v-if="calendarLoading" class="calendar-loading">
            <LoadingSpinner />
          </div>
          <div v-else-if="(familyCalendarEvents[fam.id] ?? []).length === 0" class="calendar-empty">
            <p class="text-muted">{{ t('family.noCalendarEvents') }}</p>
          </div>
          <div v-else class="calendar-events-list">
            <div
              v-for="event in familyCalendarEvents[fam.id]"
              :key="event.id"
              class="calendar-event-item"
            >
              <div class="event-date-badge">
                <i class="pi pi-calendar" />
              </div>
              <div class="event-details">
                <span class="event-title">{{ event.title }}</span>
                <span class="event-meta text-sm text-muted">
                  {{ formatEventDate(event) }}
                  <template v-if="event.location"> &middot; {{ event.location }}</template>
                </span>
              </div>
              <div class="event-rsvp-counts">
                <Tag
                  v-if="event.attendingCount > 0"
                  :value="`${event.attendingCount}`"
                  severity="success"
                  size="small"
                  icon="pi pi-check"
                />
                <Tag
                  v-if="event.maybeCount > 0"
                  :value="`${event.maybeCount}`"
                  severity="warn"
                  size="small"
                  icon="pi pi-question"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Sole Custody Section -->
        <div v-if="soleCustodyEnabled" class="sole-custody-section">
          <div class="sole-custody-toggle">
            <Checkbox
              :modelValue="fam.soleCustody"
              :binary="true"
              :disabled="fam.soleCustodyApproved"
              @update:modelValue="(val: boolean) => toggleSoleCustody(fam.id, val)"
            />
            <div>
              <label class="sole-custody-label">{{ t('family.soleCustody') }}</label>
              <p class="text-sm text-muted">{{ t('family.soleCustodyHint') }}</p>
            </div>
          </div>
          <div v-if="fam.soleCustody && !fam.soleCustodyApproved" class="sole-custody-status">
            <Tag :value="t('family.soleCustodyPending')" severity="warn" size="small" />
            <Button
              v-if="isAdminOrSectionAdmin"
              :label="t('family.approveSoleCustody')"
              icon="pi pi-check"
              size="small"
              @click="approveSoleCustody(fam.id)"
            />
          </div>
          <div v-if="fam.soleCustody && fam.soleCustodyApproved" class="sole-custody-status">
            <Tag :value="t('family.soleCustodyApprovedLabel')" severity="success" size="small" />
          </div>
        </div>
      </div>
    </template>

    <EmptyState
      v-else
      icon="pi pi-users"
      :message="t('family.noFamily')"
    >
      <div class="empty-actions">
        <Button :label="t('family.create')" @click="showCreateDialog = true" />
        <Button :label="t('family.join')" severity="secondary" @click="showJoinDialog = true" />
      </div>
    </EmptyState>

    <!-- Create Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('family.create')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <div class="form-field">
        <label for="family-name" class="required">{{ t('family.name') }}</label>
        <InputText id="family-name" v-model="familyName" class="w-full" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showCreateDialog = false" />
        <Button :label="t('common.create')" @click="createFamily" />
      </template>
    </Dialog>

    <!-- Join Dialog -->
    <Dialog v-model:visible="showJoinDialog" :header="t('family.join')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <div class="form-field">
        <label for="invite-code" class="required">{{ t('family.inviteCode') }}</label>
        <InputText id="invite-code" v-model="inviteCode" class="w-full" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showJoinDialog = false" />
        <Button :label="t('family.join')" @click="joinFamily" />
      </template>
    </Dialog>

    <!-- Leave Family Dialog -->
    <Dialog v-model:visible="showLeaveDialog" :header="t('family.leaveConfirmTitle')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <p>{{ t('family.leaveConfirmMessage') }}</p>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showLeaveDialog = false" />
        <Button :label="t('family.leave')" severity="danger" @click="leaveFamily" />
      </template>
    </Dialog>

    <!-- Invite Member Dialog -->
    <InviteMemberDialog
      v-model:visible="showInviteDialog"
      :familyId="inviteFamilyId"
      @invited="onInviteSent"
    />
  </div>
</template>

<style scoped>
.family-card {
  margin-bottom: 1.5rem;
}

.family-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.family-header h2 {
  font-size: var(--mw-font-size-xl);
}

.family-header-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.invite-code-display {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg);
  border-radius: var(--mw-border-radius-sm);
  margin-bottom: 1rem;
}

.invite-code-display .code {
  font-family: monospace;
  font-size: var(--mw-font-size-lg);
  font-weight: 600;
  letter-spacing: 0.1em;
}

.invitations-card {
  margin-bottom: 1.5rem;
  border: 2px solid var(--mw-primary, #3b82f6);
}

.invitations-card h3 {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.75rem;
}

.invitation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
  flex-wrap: wrap;
}

.invitation-item:last-child {
  border-bottom: none;
}

.invitation-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.invitation-info i {
  color: var(--mw-primary, #3b82f6);
}

.invitation-actions {
  display: flex;
  gap: 0.375rem;
}

.sent-invitations {
  margin-bottom: 1rem;
  padding: 0.5rem 0;
}

.sent-invitations h4 {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
  color: var(--mw-text-secondary);
  margin-bottom: 0.375rem;
}

.sent-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0;
  font-size: var(--mw-font-size-sm);
}

.sent-item i {
  color: var(--mw-text-muted);
}

h3 {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.5rem;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.375rem 0;
}

.member-item i {
  color: var(--mw-text-muted);
}

.empty-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-bottom: 1rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.text-sm {
  font-size: var(--mw-font-size-sm);
}

.text-muted {
  color: var(--mw-text-muted);
}

.family-calendar-section {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--mw-border-light);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.25rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.calendar-header h3 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0;
}

.calendar-desc {
  margin-bottom: 0.75rem;
}

.calendar-loading {
  padding: 1rem 0;
}

.calendar-empty {
  padding: 1rem 0;
  text-align: center;
}

.calendar-events-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.calendar-event-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg);
  border-radius: var(--mw-border-radius-sm);
}

.event-date-badge {
  color: var(--mw-primary, #3b82f6);
  font-size: 1.1rem;
  flex-shrink: 0;
}

.event-details {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.event-title {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-meta {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-rsvp-counts {
  display: flex;
  gap: 0.25rem;
  flex-shrink: 0;
}

.sole-custody-section {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--mw-border-light);
}

.sole-custody-toggle {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.sole-custody-label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.sole-custody-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  margin-left: 2rem;
}
</style>
