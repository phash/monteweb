<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useFundgrubeStore } from '@/stores/fundgrube'
import { useAuthStore } from '@/stores/auth'
import { sectionsApi } from '@/api/sections.api'
import { fundgrubeApi } from '@/api/fundgrube.api'
import type { SchoolSectionInfo } from '@/types/family'
import type { FundgrubeItemInfo, CreateFundgrubeItemRequest } from '@/types/fundgrube'
import PageTitle from '@/components/common/PageTitle.vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'
import ConfirmDialog from 'primevue/confirmdialog'
import { useConfirm } from 'primevue/useconfirm'
import FileUpload from 'primevue/fileupload'

const { t } = useI18n()
const toast = useToast()
const confirm = useConfirm()
const store = useFundgrubeStore()
const auth = useAuthStore()

const sections = ref<SchoolSectionInfo[]>([])
const selectedSectionId = ref<string | null>(null)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const selectedItem = ref<FundgrubeItemInfo | null>(null)
const editMode = ref(false)

// Form state
const form = ref({ title: '', description: '', sectionId: null as string | null })
const pendingFiles = ref<File[]>([])
const submitting = ref(false)
const claimComment = ref('')
const showClaimDialog = ref(false)
const claimingItem = ref<FundgrubeItemInfo | null>(null)
const claimSubmitting = ref(false)

const sectionOptions = computed(() => [
  { label: t('fundgrube.allSections'), value: null },
  ...sections.value.map((s) => ({ label: s.name, value: s.id })),
])

const sectionSelectOptions = computed(() =>
  sections.value.map((s) => ({ label: s.name, value: s.id })),
)

onMounted(async () => {
  const [sectionsRes] = await Promise.all([
    sectionsApi.getAll().catch(() => ({ data: { data: [] } })),
    store.fetchItems(),
  ])
  sections.value = sectionsRes.data.data
})

async function applyFilter(sectionId: string | null) {
  selectedSectionId.value = sectionId
  await store.fetchItems(sectionId ?? undefined)
}

function openCreate() {
  form.value = { title: '', description: '', sectionId: null }
  pendingFiles.value = []
  editMode.value = false
  showCreateDialog.value = true
}

function openEdit(item: FundgrubeItemInfo) {
  form.value = {
    title: item.title,
    description: item.description ?? '',
    sectionId: item.sectionId,
  }
  pendingFiles.value = []
  editMode.value = true
  selectedItem.value = item
  showDetailDialog.value = false
  showCreateDialog.value = true
}

async function submitForm() {
  if (!form.value.title.trim()) return
  submitting.value = true
  try {
    let item: FundgrubeItemInfo
    if (editMode.value && selectedItem.value) {
      item = await store.updateItem(selectedItem.value.id, {
        title: form.value.title,
        description: form.value.description || undefined,
        sectionId: form.value.sectionId ?? undefined,
      })
    } else {
      const req: CreateFundgrubeItemRequest = {
        title: form.value.title,
        description: form.value.description || undefined,
        sectionId: form.value.sectionId ?? undefined,
      }
      item = await store.createItem(req)
    }
    // Upload images if any
    if (pendingFiles.value.length > 0) {
      await store.uploadImages(item.id, pendingFiles.value)
    }
    showCreateDialog.value = false
    toast.add({ severity: 'success', summary: t('common.success'), life: 3000,
      detail: editMode.value ? t('fundgrube.itemUpdated') : t('fundgrube.itemCreated') })
  } catch {
    toast.add({ severity: 'error', summary: t('common.error'), detail: t('common.errorGeneric'), life: 3000 })
  } finally {
    submitting.value = false
  }
}

function openDetail(item: FundgrubeItemInfo) {
  selectedItem.value = item
  showDetailDialog.value = true
}

function openClaim(item: FundgrubeItemInfo) {
  claimingItem.value = item
  claimComment.value = ''
  showClaimDialog.value = true
}

async function submitClaim() {
  if (!claimingItem.value) return
  claimSubmitting.value = true
  try {
    await store.claimItem(claimingItem.value.id, { comment: claimComment.value || undefined })
    showClaimDialog.value = false
    toast.add({ severity: 'success', summary: t('fundgrube.claimed'), life: 3000,
      detail: t('fundgrube.claimSuccess') })
  } catch (err: any) {
    const msg = err?.response?.data?.message ?? t('common.errorGeneric')
    toast.add({ severity: 'error', summary: t('common.error'), detail: msg, life: 4000 })
  } finally {
    claimSubmitting.value = false
  }
}

function confirmDelete(item: FundgrubeItemInfo) {
  confirm.require({
    message: t('fundgrube.deleteConfirm'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.delete'),
    rejectLabel: t('common.cancel'),
    acceptClass: 'p-button-danger',
    accept: async () => {
      await store.deleteItem(item.id)
      if (showDetailDialog.value && selectedItem.value?.id === item.id) {
        showDetailDialog.value = false
      }
      toast.add({ severity: 'success', summary: t('common.deleted'), life: 2000 })
    },
  })
}

function canEditItem(item: FundgrubeItemInfo): boolean {
  if (auth.isAdmin) return true
  if (item.createdBy === auth.user?.id) return true
  if (auth.isSectionAdmin && item.sectionId) {
    const adminSectionIds = auth.user?.specialRoles
      ?.filter((r: string) => r.startsWith('SECTION_ADMIN:'))
      .map((r: string) => r.replace('SECTION_ADMIN:', ''))
    return adminSectionIds?.includes(item.sectionId) ?? false
  }
  return false
}

function canClaimItem(item: FundgrubeItemInfo): boolean {
  return !item.claimed && item.createdBy !== auth.user?.id
}

function thumbnailUrl(imageId: string) {
  return fundgrubeApi.thumbnailUrl(imageId)
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function onFilesSelected(event: { files: File[] }) {
  pendingFiles.value = event.files
}
</script>

<template>
  <div class="fundgrube-view">
    <ConfirmDialog />

    <PageTitle :title="t('fundgrube.title')" :subtitle="t('fundgrube.subtitle')" />

    <!-- Filter + New button -->
    <div class="toolbar">
      <Select
        v-model="selectedSectionId"
        :options="sectionOptions"
        option-label="label"
        option-value="value"
        :placeholder="t('fundgrube.allSections')"
        class="section-filter"
        @change="applyFilter(selectedSectionId)"
      />
      <Button :label="t('fundgrube.newItem')" icon="pi pi-plus" @click="openCreate" />
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="center-spinner">
      <ProgressSpinner />
    </div>

    <!-- Empty state -->
    <div v-else-if="store.items.length === 0" class="empty-state">
      <i class="pi pi-search" style="font-size: 2.5rem; color: var(--mw-text-secondary)" />
      <p>{{ t('fundgrube.empty') }}</p>
    </div>

    <!-- Items grid -->
    <div v-else class="items-grid">
      <div
        v-for="item in store.items"
        :key="item.id"
        class="item-card card"
        :class="{ claimed: item.claimed }"
        @click="openDetail(item)"
      >
        <!-- Thumbnail -->
        <div class="item-image">
          <img
            v-if="item.images.length > 0"
            :src="thumbnailUrl(item.images[0]!.id)"
            :alt="item.title"
            loading="lazy"
          />
          <div v-else class="no-image">
            <i class="pi pi-box" />
          </div>
        </div>

        <!-- Info -->
        <div class="item-info">
          <div class="item-header">
            <span class="item-title">{{ item.title }}</span>
            <Tag v-if="item.claimed" :value="t('fundgrube.claimed')" severity="warn" />
          </div>
          <p v-if="item.description" class="item-desc">{{ item.description }}</p>
          <div class="item-meta">
            <span v-if="item.sectionName" class="section-badge">{{ item.sectionName }}</span>
            <span class="item-date">{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Create / Edit Dialog -->
    <Dialog
      v-model:visible="showCreateDialog"
      :header="editMode ? t('fundgrube.editItem') : t('fundgrube.newItem')"
      modal
      :style="{ width: '520px' }"
      @hide="showCreateDialog = false"
    >
      <div class="dialog-form">
        <div class="field">
          <label>{{ t('fundgrube.itemTitle') }} *</label>
          <InputText v-model="form.title" class="w-full" :maxlength="300" autofocus />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label>
          <Textarea v-model="form.description" class="w-full" :rows="3" :maxlength="2000" />
        </div>
        <div class="field">
          <label>{{ t('fundgrube.section') }}</label>
          <Select
            v-model="form.sectionId"
            :options="sectionSelectOptions"
            option-label="label"
            option-value="value"
            :placeholder="t('fundgrube.noSection')"
            class="w-full"
            show-clear
          />
        </div>
        <div class="field">
          <label>{{ t('fundgrube.images') }}</label>
          <FileUpload
            mode="basic"
            accept="image/*"
            :multiple="true"
            :max-file-size="10000000"
            :choose-label="t('fundgrube.chooseImages')"
            :auto="false"
            custom-upload
            @select="onFilesSelected"
          />
          <small v-if="pendingFiles.length > 0" class="upload-hint">
            {{ pendingFiles.length }} {{ t('fundgrube.filesSelected') }}
          </small>
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showCreateDialog = false" />
        <Button
          :label="t('common.save')"
          :loading="submitting"
          :disabled="!form.title.trim()"
          @click="submitForm"
        />
      </template>
    </Dialog>

    <!-- Detail Dialog -->
    <Dialog
      v-model:visible="showDetailDialog"
      :header="selectedItem?.title"
      modal
      :style="{ width: '600px' }"
      @hide="showDetailDialog = false"
    >
      <div v-if="selectedItem" class="detail-content">
        <!-- Images -->
        <div v-if="selectedItem.images.length > 0" class="detail-images">
          <img
            v-for="img in selectedItem.images"
            :key="img.id"
            :src="thumbnailUrl(img.id)"
            :alt="img.originalFilename"
            class="detail-thumb"
            loading="lazy"
          />
        </div>

        <!-- Description -->
        <p v-if="selectedItem.description" class="detail-description">
          {{ selectedItem.description }}
        </p>

        <!-- Metadata -->
        <div class="detail-meta">
          <div class="meta-row">
            <i class="pi pi-user" />
            <span>{{ t('fundgrube.postedBy') }}: {{ selectedItem.createdByName }}</span>
          </div>
          <div class="meta-row">
            <i class="pi pi-calendar" />
            <span>{{ formatDate(selectedItem.createdAt) }}</span>
          </div>
          <div v-if="selectedItem.sectionName" class="meta-row">
            <i class="pi pi-building" />
            <span>{{ selectedItem.sectionName }}</span>
          </div>
          <div v-if="selectedItem.claimed" class="meta-row claimed-info">
            <i class="pi pi-check-circle" />
            <span>
              {{ t('fundgrube.claimedBy') }}: {{ selectedItem.claimedByName }}
              <template v-if="selectedItem.expiresAt">
                · {{ t('fundgrube.expiresAt') }}: {{ formatDate(selectedItem.expiresAt) }}
              </template>
            </span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="detail-footer">
          <div class="detail-actions-left">
            <Button
              v-if="selectedItem && canEditItem(selectedItem)"
              :label="t('common.edit')"
              icon="pi pi-pencil"
              severity="secondary"
              @click="openEdit(selectedItem!)"
            />
            <Button
              v-if="selectedItem && canEditItem(selectedItem)"
              :label="t('common.delete')"
              icon="pi pi-trash"
              severity="danger"
              @click="confirmDelete(selectedItem!)"
            />
          </div>
          <Button
            v-if="selectedItem && canClaimItem(selectedItem)"
            :label="t('fundgrube.claimButton')"
            icon="pi pi-hand-pointer"
            @click="openClaim(selectedItem!)"
          />
        </div>
      </template>
    </Dialog>

    <!-- Claim Dialog -->
    <Dialog
      v-model:visible="showClaimDialog"
      :header="t('fundgrube.claimTitle')"
      modal
      :style="{ width: '440px' }"
    >
      <div class="dialog-form">
        <p>{{ t('fundgrube.claimDescription') }}</p>
        <div class="field">
          <label>{{ t('fundgrube.claimComment') }}</label>
          <Textarea
            v-model="claimComment"
            class="w-full"
            :rows="3"
            :placeholder="t('fundgrube.claimCommentPlaceholder')"
          />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" @click="showClaimDialog = false" />
        <Button
          :label="t('fundgrube.claimButton')"
          icon="pi pi-check"
          :loading="claimSubmitting"
          @click="submitClaim"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.fundgrube-view {
  max-width: 1100px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.section-filter {
  min-width: 200px;
}

.center-spinner {
  display: flex;
  justify-content: center;
  padding: 3rem;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--mw-text-secondary);
}

.empty-state p {
  margin-top: 1rem;
  font-size: var(--mw-font-size-lg);
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.item-card {
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.15s, box-shadow 0.15s;
  display: flex;
  flex-direction: column;
}

.item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.item-card.claimed {
  opacity: 0.6;
}

.item-image {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: var(--mw-border-light);
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mw-text-secondary);
  font-size: 2.5rem;
}

.item-info {
  padding: 0.75rem 1rem 1rem;
  flex: 1;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: space-between;
  margin-bottom: 0.375rem;
}

.item-title {
  font-weight: 600;
  font-size: var(--mw-font-size-md);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-desc {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-bottom: 0.5rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.section-badge {
  background: var(--mw-primary);
  color: white;
  font-size: 0.75rem;
  padding: 0.125rem 0.5rem;
  border-radius: 12px;
}

.item-date {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  margin-left: auto;
}

/* Detail dialog */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.detail-images {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.detail-thumb {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--mw-border-light);
}

.detail-description {
  color: var(--mw-text-color);
  white-space: pre-wrap;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}

.meta-row i {
  width: 16px;
}

.claimed-info {
  color: var(--p-yellow-600);
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 0.5rem;
}

.detail-actions-left {
  display: flex;
  gap: 0.5rem;
}

/* Form */
.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.w-full {
  width: 100%;
}

.upload-hint {
  color: var(--mw-text-secondary);
}
</style>
