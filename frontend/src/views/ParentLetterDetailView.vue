<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useMarkdown } from '@/composables/useMarkdown'
import { useParentLetterStore } from '@/stores/parentletter'
import { useAuthStore } from '@/stores/auth'
import { parentLetterApi } from '@/api/parentletter.api'
import type { ParentLetterStatus, ParentLetterAttachmentInfo } from '@/types/parentletter'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import RecipientStatusTable from '@/components/parentletter/RecipientStatusTable.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import Divider from 'primevue/divider'
import FileUpload from 'primevue/fileupload'

const { t } = useI18n()
const { formatShortDate, formatCompactDateTime } = useLocaleDate()
const router = useRouter()
const route = useRoute()
const toast = useToast()
const store = useParentLetterStore()
const auth = useAuthStore()
const { renderMarkdown } = useMarkdown()

const letterId = computed(() => route.params.id as string)

const confirmingStudentId = ref<string | null>(null)
const sendingLetter = ref(false)
const closingLetter = ref(false)
const attachments = ref<ParentLetterAttachmentInfo[]>([])
const uploadingFiles = ref(false)

const letter = computed(() => store.currentLetter)

const renderedContent = computed(() => renderMarkdown(letter.value?.content ?? ''))

// Is the current user the creator or an admin?
const isCreatorOrAdmin = computed(() =>
  auth.isAdmin || (letter.value?.createdBy === auth.user?.id)
)

// Parent view: user is a parent and NOT the creator
const isParentView = computed(() =>
  !isCreatorOrAdmin.value && auth.user?.role === 'PARENT'
)

// Which recipients belong to the current parent
const myRecipients = computed(() => {
  if (!letter.value || !isParentView.value) return []
  return letter.value.recipients.filter(r => r.parentId === auth.user?.id)
})

const confirmProgress = computed(() => {
  if (!letter.value || letter.value.totalRecipients === 0) return 0
  return Math.round((letter.value.confirmedCount / letter.value.totalRecipients) * 100)
})

function statusSeverity(status: ParentLetterStatus): 'secondary' | 'info' | 'success' | 'warn' {
  switch (status) {
    case 'DRAFT': return 'secondary'
    case 'SCHEDULED': return 'info'
    case 'SENT': return 'success'
    case 'CLOSED': return 'warn'
  }
}

async function fetchAttachments() {
  try {
    const res = await parentLetterApi.getAttachments(letterId.value)
    attachments.value = res.data.data
  } catch {
    attachments.value = []
  }
}

onMounted(async () => {
  await store.fetchLetter(letterId.value)
  // Fetch attachments
  await fetchAttachments()
  // Mark as read if parent view and letter was loaded
  if (isParentView.value && letter.value?.status === 'SENT') {
    try {
      await store.markAsRead(letterId.value)
    } catch {
      // Non-critical — ignore
    }
  }
})

async function handleSend() {
  sendingLetter.value = true
  try {
    await store.sendLetter(letterId.value)
    toast.add({ severity: 'success', summary: t('parentLetters.sent'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    sendingLetter.value = false
  }
}

async function handleClose() {
  closingLetter.value = true
  try {
    await store.closeLetter(letterId.value)
    toast.add({ severity: 'success', summary: t('parentLetters.closed'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    closingLetter.value = false
  }
}

async function handleConfirm(studentId: string) {
  confirmingStudentId.value = studentId
  try {
    await store.confirmLetter(letterId.value, studentId)
    toast.add({ severity: 'success', summary: t('parentLetters.confirmSuccess'), life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    confirmingStudentId.value = null
  }
}

function downloadBlob(data: Blob, filename: string) {
  const url = URL.createObjectURL(data)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function downloadPdf() {
  try {
    const res = await parentLetterApi.downloadLetterPdf(letterId.value)
    downloadBlob(res.data, `Elternbrief-${letter.value?.title ?? 'Brief'}.pdf`)
  } catch {
    toast.add({ severity: 'error', summary: t('common.error'), life: 5000 })
  }
}

async function downloadTracking() {
  try {
    const res = await parentLetterApi.downloadTrackingPdf(letterId.value)
    downloadBlob(res.data, `Ruecklauf-${letter.value?.title ?? 'Brief'}.pdf`)
  } catch {
    toast.add({ severity: 'error', summary: t('common.error'), life: 5000 })
  }
}

function getAttachmentIcon(contentType: string): string {
  if (contentType.startsWith('image/')) return 'pi pi-image'
  if (contentType === 'application/pdf') return 'pi pi-file-pdf'
  if (contentType.startsWith('video/')) return 'pi pi-video'
  if (contentType.startsWith('audio/')) return 'pi pi-volume-up'
  return 'pi pi-file'
}

function formatFileSize(bytes: number): string {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function downloadAttachment(att: ParentLetterAttachmentInfo) {
  const url = parentLetterApi.getAttachmentDownloadUrl(letterId.value, att.id)
  const a = document.createElement('a')
  a.href = url
  a.download = att.originalFilename
  a.click()
}

async function handleUpload(event: any) {
  const files: File[] = event.files
  if (!files?.length) return
  uploadingFiles.value = true
  try {
    await parentLetterApi.uploadAttachments(letterId.value, files)
    toast.add({ severity: 'success', summary: t('parentLetters.attachments.uploaded'), life: 3000 })
    await fetchAttachments()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  } finally {
    uploadingFiles.value = false
  }
}

async function handleDeleteAttachment(att: ParentLetterAttachmentInfo) {
  try {
    await parentLetterApi.deleteAttachment(letterId.value, att.id)
    toast.add({ severity: 'success', summary: t('parentLetters.attachments.deleted'), life: 3000 })
    await fetchAttachments()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || t('common.error'), life: 5000 })
  }
}
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      @click="router.back()"
      class="mb-1"
    />

    <LoadingSpinner v-if="store.loading && !letter" />

    <template v-else-if="letter">
      <!-- Header -->
      <div class="detail-header">
        <PageTitle :title="letter.title" />
        <div class="header-tags">
          <Tag
            :value="t(`parentLetters.statuses.${letter.status}`)"
            :severity="statusSeverity(letter.status)"
          />
        </div>
      </div>

      <!-- Meta bar -->
      <div class="meta-bar card">
        <span><i class="pi pi-home" /> {{ letter.roomName }}</span>
        <span><i class="pi pi-user" /> {{ letter.creatorName }}</span>
        <span v-if="letter.sendDate">
          <i class="pi pi-send" /> {{ formatCompactDateTime(letter.sendDate) }}
        </span>
        <span v-if="letter.deadline">
          <i class="pi pi-clock" /> {{ t('parentLetters.deadline') }}: {{ formatShortDate(letter.deadline) }}
        </span>
        <span>
          <i class="pi pi-calendar" /> {{ t('parentLetters.createdAt') }}: {{ formatShortDate(letter.createdAt) }}
        </span>
      </div>

      <!-- Management actions (teacher/admin) -->
      <div v-if="isCreatorOrAdmin" class="management-actions card">
        <Button
          v-if="letter.status === 'DRAFT'"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          severity="secondary"
          size="small"
          @click="router.push({ name: 'parent-letter-edit', params: { id: letter.id } })"
        />
        <Button
          v-if="letter.status === 'DRAFT'"
          :label="t('parentLetters.send')"
          icon="pi pi-send"
          size="small"
          :loading="sendingLetter"
          @click="handleSend"
        />
        <Button
          v-if="letter.status === 'SENT'"
          :label="t('parentLetters.close')"
          icon="pi pi-lock"
          severity="warn"
          size="small"
          :loading="closingLetter"
          @click="handleClose"
        />
        <Button
          :label="t('parentLetters.downloadPdf')"
          icon="pi pi-file-pdf"
          severity="secondary"
          size="small"
          @click="downloadPdf"
        />
        <Button
          v-if="letter.status !== 'DRAFT'"
          :label="t('parentLetters.downloadTracking')"
          icon="pi pi-list"
          severity="secondary"
          size="small"
          @click="downloadTracking"
        />
      </div>

      <!-- Confirmation progress (teacher view) -->
      <div v-if="isCreatorOrAdmin && letter.totalRecipients > 0" class="progress-card card">
        <div class="progress-header">
          <span class="progress-label">
            {{ t('parentLetters.confirmProgress') }}:
            {{ letter.confirmedCount }}/{{ letter.totalRecipients }}
          </span>
          <span class="progress-pct">{{ confirmProgress }}%</span>
        </div>
        <ProgressBar :value="confirmProgress" :showValue="false" class="progress-bar" />
      </div>

      <!-- Letter content -->
      <div class="letter-content card">
        <div class="content-text" v-html="renderedContent" />
      </div>

      <!-- Attachments display -->
      <div v-if="attachments.length > 0" class="attachments-section card">
        <h3 class="attachments-title">
          <i class="pi pi-paperclip" />
          {{ t('parentLetters.attachments.title') }}
          <span class="attachments-count">({{ attachments.length }})</span>
        </h3>
        <div class="attachments-list">
          <div
            v-for="att in attachments"
            :key="att.id"
            class="attachment-item"
          >
            <div class="attachment-info" @click="downloadAttachment(att)">
              <i :class="getAttachmentIcon(att.contentType)" />
              <span class="attachment-name">{{ att.originalFilename }}</span>
              <span class="attachment-size">{{ formatFileSize(att.fileSize) }}</span>
              <i class="pi pi-download attachment-download" />
            </div>
            <Button
              v-if="isCreatorOrAdmin && letter.status === 'DRAFT'"
              icon="pi pi-trash"
              severity="danger"
              text
              size="small"
              @click="handleDeleteAttachment(att)"
            />
          </div>
        </div>
      </div>

      <!-- Attachment upload (DRAFT only, creator/admin) -->
      <div v-if="isCreatorOrAdmin && letter.status === 'DRAFT'" class="upload-section card">
        <h3 class="attachments-title">
          <i class="pi pi-upload" />
          {{ t('parentLetters.attachments.upload') }}
        </h3>
        <p class="upload-hint">{{ t('parentLetters.attachments.uploadHint') }}</p>
        <FileUpload
          mode="basic"
          :multiple="true"
          :auto="true"
          :maxFileSize="10485760"
          :chooseLabel="t('parentLetters.attachments.upload')"
          :disabled="uploadingFiles || attachments.length >= 5"
          @uploader="handleUpload"
          customUpload
        />
      </div>

      <!-- Parent confirmation section -->
      <div v-if="isParentView && myRecipients.length > 0 && letter.status === 'SENT'" class="confirm-section card">
        <h3>{{ t('parentLetters.confirmationRequired') }}</h3>
        <p class="confirm-hint">{{ t('parentLetters.confirmationHint') }}</p>
        <div
          v-for="recipient in myRecipients"
          :key="recipient.id"
          class="confirm-row"
        >
          <div class="confirm-child">
            <i class="pi pi-user" />
            <span>{{ recipient.studentName }}</span>
          </div>
          <Tag
            v-if="recipient.status === 'CONFIRMED'"
            :value="t('parentLetters.recipientStatuses.CONFIRMED')"
            severity="success"
            icon="pi pi-check"
          />
          <Button
            v-else
            :label="t('parentLetters.confirm')"
            icon="pi pi-check"
            size="small"
            :loading="confirmingStudentId === recipient.studentId"
            :disabled="!!confirmingStudentId"
            @click="handleConfirm(recipient.studentId)"
          />
        </div>
      </div>

      <!-- Already confirmed notice for parents -->
      <div
        v-if="isParentView && myRecipients.length > 0 && myRecipients.every(r => r.status === 'CONFIRMED')"
        class="confirmed-banner card"
      >
        <i class="pi pi-check-circle" />
        <span>{{ t('parentLetters.allConfirmed') }}</span>
      </div>

      <Divider v-if="isCreatorOrAdmin" />

      <!-- Recipient tracking table (teacher/admin only) -->
      <div v-if="isCreatorOrAdmin">
        <h3 class="recipients-title">
          {{ t('parentLetters.recipientsTitle') }}
          <span class="recipients-count">({{ letter.recipients.length }})</span>
        </h3>
        <RecipientStatusTable :recipients="letter.recipients" />
      </div>

    </template>

    <div v-else class="not-found">
      <p>{{ t('parentLetters.notFound') }}</p>
    </div>
  </div>
</template>

<style scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.header-tags {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.meta-bar {
  display: flex;
  gap: 1.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  flex-wrap: wrap;
}

.meta-bar i {
  margin-right: 0.25rem;
}

.management-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.progress-card {
  padding: 1rem;
  margin-bottom: 1rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.progress-label {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
}

.progress-pct {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
}

.progress-bar {
  height: 0.6rem;
}

.letter-content {
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.content-text {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: var(--mw-font-size-md);
}

.confirm-section {
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
}

.confirm-section h3 {
  margin: 0 0 0.5rem;
}

.confirm-hint {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  margin-bottom: 1rem;
}

.confirm-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border);
}

.confirm-row:last-child {
  border-bottom: none;
}

.confirm-child {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
}

.confirmed-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  margin-bottom: 1rem;
  color: var(--p-green-600);
  font-weight: 600;
}

.confirmed-banner i {
  font-size: 1.5rem;
}

.recipients-title {
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.recipients-count {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  font-weight: 400;
}

.not-found {
  text-align: center;
  color: var(--mw-text-muted);
  padding: 3rem;
}

.attachments-section {
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
}

.attachments-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 0.75rem;
}

.attachments-count {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  font-weight: 400;
}

.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  padding: 0.5rem 0.625rem;
  background: var(--mw-bg, #f8f9fa);
  border: 1px solid var(--mw-border-light, #dee2e6);
  border-radius: var(--mw-border-radius-sm, 4px);
  cursor: pointer;
  transition: background-color 0.15s;
  font-size: var(--mw-font-size-sm, 0.875rem);
}

.attachment-info:hover {
  background: var(--mw-bg-highlight, #e9ecef);
}

.attachment-info > i:first-child {
  color: var(--mw-primary, #3b82f6);
  flex-shrink: 0;
}

.attachment-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-size {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-xs, 0.75rem);
  flex-shrink: 0;
}

.attachment-download {
  color: var(--mw-text-muted);
  flex-shrink: 0;
  font-size: var(--mw-font-size-sm, 0.875rem);
}

.upload-section {
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
}

.upload-hint {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  margin-bottom: 0.75rem;
}
</style>
