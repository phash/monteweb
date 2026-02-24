<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { filesApi } from '@/api/files.api'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const roomId = computed(() => route.params.roomId as string)
const fileId = computed(() => route.params.fileId as string)

const iframeSrc = ref('')
const loading = ref(true)
const error = ref('')
const fileName = ref('')

/**
 * Maps file extension to the correct ONLYOFFICE editor app URL path.
 */
function getEditorPath(name: string): string {
  const ext = name.split('.').pop()?.toLowerCase() || ''
  const spreadsheet = ['xlsx', 'xls', 'ods', 'csv']
  const presentation = ['pptx', 'ppt', 'odp']
  // Default to document editor for doc/docx/odt and anything else
  if (spreadsheet.includes(ext)) {
    return '/web-apps/apps/spreadsheeteditor/main/index.html'
  }
  if (presentation.includes(ext)) {
    return '/web-apps/apps/presentationeditor/main/index.html'
  }
  return '/web-apps/apps/documenteditor/main/index.html'
}

onMounted(async () => {
  try {
    // First get file info to determine the editor type
    const filesRes = await filesApi.listFiles(roomId.value)
    const file = filesRes.data.data.find((f: any) => f.id === fileId.value)
    if (file) {
      fileName.value = file.originalName
    }

    // Create WOPI session
    const res = await filesApi.createWopiSession(roomId.value, fileId.value)
    const session = res.data.data

    // Build the ONLYOFFICE iframe URL
    const editorPath = getEditorPath(fileName.value)
    const wopiSrcAbsolute = `${window.location.origin}${session.wopiSrc}`
    const encodedSrc = encodeURIComponent(wopiSrcAbsolute)

    iframeSrc.value = `${session.officeUrl}${editorPath}?WOPISrc=${encodedSrc}&access_token=${session.token}`
  } catch (e: any) {
    error.value = e.response?.data?.message || t('wopi.errorLoading')
  } finally {
    loading.value = false
  }
})

function goBack() {
  router.push({ name: 'room-detail', params: { id: roomId.value } })
}
</script>

<template>
  <div class="wopi-editor">
    <div class="wopi-toolbar">
      <Button
        icon="pi pi-arrow-left"
        :label="t('wopi.backToFiles')"
        severity="secondary"
        size="small"
        @click="goBack"
      />
      <span v-if="fileName" class="wopi-filename">{{ fileName }}</span>
    </div>

    <div v-if="loading" class="wopi-loading">
      <ProgressSpinner />
    </div>

    <div v-else-if="error" class="wopi-error">
      <i class="pi pi-exclamation-triangle" />
      <p>{{ error }}</p>
      <Button :label="t('wopi.backToFiles')" @click="goBack" />
    </div>

    <iframe
      v-else
      :src="iframeSrc"
      class="wopi-iframe"
      frameborder="0"
      allowfullscreen
      :title="t('wopi.editorTitle')"
    />
  </div>
</template>

<style scoped>
.wopi-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 4rem);
}

.wopi-toolbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: var(--mw-bg-card, #fff);
  border-bottom: 1px solid var(--mw-border-light);
}

.wopi-filename {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wopi-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
}

.wopi-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  flex: 1;
  color: var(--mw-text-secondary);
}

.wopi-error i {
  font-size: 2rem;
  color: var(--mw-warning);
}

.wopi-iframe {
  flex: 1;
  width: 100%;
  border: none;
}
</style>
