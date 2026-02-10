<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { filesApi } from '@/api/files.api'
import type { FileInfo, FolderInfo } from '@/types/files'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import FileUpload from 'primevue/fileupload'

const props = defineProps<{ roomId: string }>()
const { t } = useI18n()

const files = ref<FileInfo[]>([])
const folders = ref<FolderInfo[]>([])
const currentFolderId = ref<string | undefined>(undefined)
const folderPath = ref<{ id: string | undefined; name: string }[]>([{ id: undefined, name: 'Dateien' }])
const loading = ref(false)
const showNewFolder = ref(false)
const newFolderName = ref('')

onMounted(() => loadContent())

async function loadContent() {
  loading.value = true
  try {
    const [filesRes, foldersRes] = await Promise.all([
      filesApi.listFiles(props.roomId, currentFolderId.value),
      filesApi.listFolders(props.roomId, currentFolderId.value),
    ])
    files.value = filesRes.data.data
    folders.value = foldersRes.data.data
  } finally {
    loading.value = false
  }
}

async function openFolder(folder: FolderInfo) {
  currentFolderId.value = folder.id
  folderPath.value.push({ id: folder.id, name: folder.name })
  await loadContent()
}

async function navigateTo(index: number) {
  const target = folderPath.value[index]
  currentFolderId.value = target.id
  folderPath.value = folderPath.value.slice(0, index + 1)
  await loadContent()
}

async function createFolder() {
  if (!newFolderName.value.trim()) return
  await filesApi.createFolder(props.roomId, newFolderName.value.trim(), currentFolderId.value)
  newFolderName.value = ''
  showNewFolder.value = false
  await loadContent()
}

async function handleUpload(event: any) {
  const file = event.files?.[0]
  if (!file) return
  await filesApi.uploadFile(props.roomId, file, currentFolderId.value)
  await loadContent()
}

async function downloadFile(file: FileInfo) {
  const res = await filesApi.downloadFile(props.roomId, file.id)
  const url = window.URL.createObjectURL(new Blob([res.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = file.originalName
  a.click()
  window.URL.revokeObjectURL(url)
}

async function deleteFile(file: FileInfo) {
  await filesApi.deleteFile(props.roomId, file.id)
  await loadContent()
}

async function deleteFolder(folder: FolderInfo) {
  await filesApi.deleteFolder(props.roomId, folder.id)
  await loadContent()
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('de-DE')
}
</script>

<template>
  <div class="room-files">
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <span
        v-for="(crumb, i) in folderPath"
        :key="i"
        class="breadcrumb-item"
        :class="{ active: i === folderPath.length - 1 }"
        @click="i < folderPath.length - 1 && navigateTo(i)"
      >
        {{ crumb.name }}
        <i v-if="i < folderPath.length - 1" class="pi pi-angle-right" />
      </span>
    </div>

    <!-- Actions -->
    <div class="file-actions">
      <FileUpload
        mode="basic"
        :auto="true"
        :customUpload="true"
        @uploader="handleUpload"
        :chooseLabel="t('files.upload')"
        class="upload-btn"
      />
      <Button
        icon="pi pi-folder-plus"
        :label="t('files.newFolder')"
        severity="secondary"
        size="small"
        @click="showNewFolder = true"
      />
    </div>

    <!-- Folders -->
    <div v-if="folders.length" class="file-list">
      <div v-for="folder in folders" :key="folder.id" class="file-item folder" @click="openFolder(folder)">
        <i class="pi pi-folder" />
        <span class="file-name">{{ folder.name }}</span>
        <span class="file-date">{{ formatDate(folder.createdAt) }}</span>
        <Button icon="pi pi-trash" text severity="danger" size="small" @click.stop="deleteFolder(folder)" />
      </div>
    </div>

    <!-- Files -->
    <div v-if="files.length" class="file-list">
      <div v-for="file in files" :key="file.id" class="file-item" @click="downloadFile(file)">
        <i class="pi pi-file" />
        <span class="file-name">{{ file.originalName }}</span>
        <span class="file-size">{{ formatSize(file.fileSize) }}</span>
        <span class="file-date">{{ formatDate(file.createdAt) }}</span>
        <span class="file-uploader">{{ file.uploaderName }}</span>
        <Button icon="pi pi-trash" text severity="danger" size="small" @click.stop="deleteFile(file)" />
      </div>
    </div>

    <p v-if="!folders.length && !files.length && !loading" class="empty-state">
      {{ t('files.noFiles') }}
    </p>

    <!-- New Folder Dialog -->
    <Dialog v-model:visible="showNewFolder" :header="t('files.newFolder')" modal :style="{ width: '400px' }">
      <InputText v-model="newFolderName" :placeholder="t('files.folderName')" class="folder-input" />
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showNewFolder = false" />
        <Button :label="t('common.create')" :disabled="!newFolderName.trim()" @click="createFolder" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.75rem;
}

.breadcrumb-item {
  cursor: pointer;
  color: var(--mw-text-secondary);
}

.breadcrumb-item.active {
  color: var(--mw-text-primary);
  font-weight: 600;
  cursor: default;
}

.breadcrumb-item i {
  font-size: 0.75rem;
  margin: 0 0.25rem;
}

.file-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.file-list {
  display: flex;
  flex-direction: column;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--mw-border-light);
  cursor: pointer;
  transition: background 0.15s;
}

.file-item:hover {
  background: var(--mw-bg-hover);
}

.file-item.folder i {
  color: var(--mw-warning);
}

.file-name {
  flex: 1;
  font-size: var(--mw-font-size-sm);
}

.file-size,
.file-date,
.file-uploader {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  white-space: nowrap;
}

.empty-state {
  text-align: center;
  color: var(--mw-text-muted);
  padding: 2rem;
}

.folder-input {
  width: 100%;
}
</style>
