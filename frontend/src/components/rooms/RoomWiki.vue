<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { wikiApi } from '@/api/wiki.api'
import type {
  WikiPageResponse,
  WikiPageSummary,
  WikiPageVersionResponse,
} from '@/types/wiki'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { sanitizeHtml } from '@/utils/sanitize'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import { useToast } from 'primevue/usetoast'

const props = defineProps<{ roomId: string; isLeader: boolean }>()
const { t } = useI18n()
const { formatDateTime } = useLocaleDate()
const toast = useToast()

// State
const pageTree = ref<WikiPageSummary[]>([])
const currentPage = ref<WikiPageResponse | null>(null)
const versions = ref<WikiPageVersionResponse[]>([])
const selectedVersion = ref<WikiPageVersionResponse | null>(null)
const loading = ref(false)
const pageLoading = ref(false)

// View mode: 'tree' | 'view' | 'edit' | 'history' | 'version'
const viewMode = ref<'tree' | 'view' | 'edit' | 'history' | 'version'>('tree')
const showPreview = ref(false)

// Create/Edit form
const showCreateDialog = ref(false)
const createForm = ref<{ title: string; content: string; parentId: string | null }>({
  title: '',
  content: '',
  parentId: null,
})
const creating = ref(false)

const editForm = ref<{ title: string; content: string }>({
  title: '',
  content: '',
})
const saving = ref(false)

// Search
const searchQuery = ref('')
const searchResults = ref<WikiPageSummary[] | null>(null)
const searching = ref(false)
let searchTimeout: ReturnType<typeof setTimeout> | null = null

// Delete
const showDeleteDialog = ref(false)
const deleting = ref(false)

// Expanded tree nodes
const expandedIds = ref<Set<string>>(new Set())

// Computed: root pages and child map
const rootPages = computed(() => {
  const source = searchResults.value ?? pageTree.value
  return source.filter(p => p.parentId === null)
})

const childMap = computed(() => {
  const source = searchResults.value ?? pageTree.value
  const map = new Map<string, WikiPageSummary[]>()
  for (const page of source) {
    if (page.parentId) {
      if (!map.has(page.parentId)) map.set(page.parentId, [])
      map.get(page.parentId)!.push(page)
    }
  }
  return map
})

const parentOptions = computed(() => {
  const opts = [{ label: t('wiki.noParent'), value: null as string | null }]
  for (const page of pageTree.value) {
    if (!currentPage.value || page.id !== currentPage.value.id) {
      const prefix = page.parentId ? '  ' : ''
      opts.push({ label: prefix + page.title, value: page.id })
    }
  }
  return opts
})

// ---- Load ----

async function loadTree() {
  loading.value = true
  try {
    const res = await wikiApi.getPageTree(props.roomId)
    pageTree.value = res.data.data
  } catch {
    pageTree.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => loadTree())

async function loadPage(slug: string) {
  pageLoading.value = true
  try {
    const res = await wikiApi.getPage(props.roomId, slug)
    currentPage.value = res.data.data
    viewMode.value = 'view'
    showPreview.value = false
  } catch {
    toast.add({ severity: 'error', summary: t('wiki.loadError'), life: 5000 })
  } finally {
    pageLoading.value = false
  }
}

// ---- Create ----

function openCreateDialog(parentId?: string) {
  createForm.value = {
    title: '',
    content: '',
    parentId: parentId || null,
  }
  showCreateDialog.value = true
}

async function createPage() {
  if (!createForm.value.title.trim()) return
  creating.value = true
  try {
    const data: { title: string; content?: string; parentId?: string } = {
      title: createForm.value.title.trim(),
      content: createForm.value.content || undefined,
      parentId: createForm.value.parentId || undefined,
    }
    const res = await wikiApi.createPage(props.roomId, data)
    showCreateDialog.value = false
    toast.add({ severity: 'success', summary: t('wiki.created'), life: 3000 })
    await loadTree()
    currentPage.value = res.data.data
    viewMode.value = 'view'
  } catch {
    toast.add({ severity: 'error', summary: t('wiki.createError'), life: 5000 })
  } finally {
    creating.value = false
  }
}

// ---- Edit ----

function startEdit() {
  if (!currentPage.value) return
  editForm.value = {
    title: currentPage.value.title,
    content: currentPage.value.content,
  }
  viewMode.value = 'edit'
  showPreview.value = false
}

async function savePage() {
  if (!currentPage.value || !editForm.value.title.trim()) return
  saving.value = true
  try {
    const res = await wikiApi.updatePage(props.roomId, currentPage.value.id, {
      title: editForm.value.title.trim(),
      content: editForm.value.content,
    })
    currentPage.value = res.data.data
    viewMode.value = 'view'
    toast.add({ severity: 'success', summary: t('wiki.saved'), life: 3000 })
    await loadTree()
  } catch {
    toast.add({ severity: 'error', summary: t('wiki.saveError'), life: 5000 })
  } finally {
    saving.value = false
  }
}

function cancelEdit() {
  viewMode.value = 'view'
  showPreview.value = false
}

// ---- Delete ----

async function deletePage() {
  if (!currentPage.value) return
  deleting.value = true
  try {
    await wikiApi.deletePage(props.roomId, currentPage.value.id)
    showDeleteDialog.value = false
    currentPage.value = null
    viewMode.value = 'tree'
    toast.add({ severity: 'success', summary: t('wiki.deleted'), life: 3000 })
    await loadTree()
  } catch {
    toast.add({ severity: 'error', summary: t('wiki.deleteError'), life: 5000 })
  } finally {
    deleting.value = false
  }
}

// ---- History ----

async function showHistory() {
  if (!currentPage.value) return
  pageLoading.value = true
  try {
    const res = await wikiApi.getVersions(props.roomId, currentPage.value.id)
    versions.value = res.data.data
    viewMode.value = 'history'
  } catch {
    toast.add({ severity: 'error', summary: t('wiki.loadError'), life: 5000 })
  } finally {
    pageLoading.value = false
  }
}

async function viewVersion(versionId: string) {
  pageLoading.value = true
  try {
    const res = await wikiApi.getVersion(props.roomId, versionId)
    selectedVersion.value = res.data.data
    viewMode.value = 'version'
  } catch {
    toast.add({ severity: 'error', summary: t('wiki.loadError'), life: 5000 })
  } finally {
    pageLoading.value = false
  }
}

function restoreVersion() {
  if (!selectedVersion.value) return
  editForm.value = {
    title: selectedVersion.value.title,
    content: selectedVersion.value.content,
  }
  viewMode.value = 'edit'
  showPreview.value = false
}

// ---- Search ----

function onSearch() {
  if (searchTimeout) clearTimeout(searchTimeout)
  const q = searchQuery.value.trim()
  if (q.length < 2) {
    searchResults.value = null
    return
  }
  searchTimeout = setTimeout(async () => {
    searching.value = true
    try {
      const res = await wikiApi.searchPages(props.roomId, q)
      searchResults.value = res.data.data
    } catch {
      searchResults.value = null
    } finally {
      searching.value = false
    }
  }, 300)
}

watch(searchQuery, () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = null
  }
})

// ---- Tree helpers ----

function toggleExpand(pageId: string) {
  if (expandedIds.value.has(pageId)) {
    expandedIds.value.delete(pageId)
  } else {
    expandedIds.value.add(pageId)
  }
  // Force reactivity
  expandedIds.value = new Set(expandedIds.value)
}

function goToTree() {
  currentPage.value = null
  viewMode.value = 'tree'
  searchQuery.value = ''
  searchResults.value = null
}

// ---- Markdown rendering ----

function renderMarkdown(md: string): string {
  if (!md) return ''
  let html = md

  // Escape HTML
  html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // Code blocks (```)
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_m, _lang, code) => {
    return `<pre><code>${code.trim()}</code></pre>`
  })

  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  // Headings
  html = html.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>')
  html = html.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>')
  html = html.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')

  // Bold and italic
  html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // Links
  html = html.replace(
    /\[([^\]]+)\]\(([^)]+)\)/g,
    '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>',
  )

  // Unordered lists
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
  html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>')

  // Ordered lists
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li>$1</li>')

  // Horizontal rule
  html = html.replace(/^---$/gm, '<hr />')

  // Blockquote
  html = html.replace(/^&gt;\s+(.+)$/gm, '<blockquote>$1</blockquote>')

  // Paragraphs (double newline)
  html = html.replace(/\n\n+/g, '</p><p>')
  html = '<p>' + html + '</p>'

  // Clean up empty paragraphs
  html = html.replace(/<p>\s*<\/p>/g, '')
  html = html.replace(/<p>\s*(<h[1-6]>)/g, '$1')
  html = html.replace(/(<\/h[1-6]>)\s*<\/p>/g, '$1')
  html = html.replace(/<p>\s*(<pre>)/g, '$1')
  html = html.replace(/(<\/pre>)\s*<\/p>/g, '$1')
  html = html.replace(/<p>\s*(<ul>)/g, '$1')
  html = html.replace(/(<\/ul>)\s*<\/p>/g, '$1')
  html = html.replace(/<p>\s*(<blockquote>)/g, '$1')
  html = html.replace(/(<\/blockquote>)\s*<\/p>/g, '$1')
  html = html.replace(/<p>\s*(<hr \/>)/g, '$1')
  html = html.replace(/(<hr \/>)\s*<\/p>/g, '$1')

  return html
}
</script>

<template>
  <div class="room-wiki">
    <LoadingSpinner v-if="loading" />

    <template v-else>
      <!-- Tree View -->
      <template v-if="viewMode === 'tree'">
        <div class="wiki-header">
          <Button
            :label="t('wiki.newPage')"
            icon="pi pi-plus"
            size="small"
            @click="openCreateDialog()"
          />
          <div class="wiki-search">
            <InputText
              v-model="searchQuery"
              :placeholder="t('wiki.search')"
              size="small"
              class="wiki-search-input"
              @input="onSearch"
            />
            <i v-if="searching" class="pi pi-spinner pi-spin" />
          </div>
        </div>

        <div v-if="searchResults !== null" class="wiki-search-results">
          <h3 class="wiki-section-title">{{ t('wiki.searchResults') }}</h3>
          <div v-if="searchResults.length" class="wiki-page-list">
            <div
              v-for="page in searchResults"
              :key="page.id"
              class="wiki-page-item"
              @click="loadPage(page.slug)"
            >
              <i class="pi pi-file" />
              <span>{{ page.title }}</span>
            </div>
          </div>
          <p v-else class="text-muted text-sm">{{ t('wiki.noSearchResults') }}</p>
        </div>

        <template v-else>
          <div v-if="pageTree.length" class="wiki-tree">
            <template v-for="page in rootPages" :key="page.id">
              <div class="wiki-tree-item">
                <div class="wiki-tree-row" @click="loadPage(page.slug)">
                  <Button
                    v-if="page.hasChildren || childMap.has(page.id)"
                    :icon="expandedIds.has(page.id) ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
                    text
                    rounded
                    size="small"
                    class="tree-toggle"
                    @click.stop="toggleExpand(page.id)"
                  />
                  <span v-else class="tree-toggle-spacer" />
                  <i class="pi pi-file" />
                  <span class="wiki-tree-title">{{ page.title }}</span>
                </div>
                <div v-if="expandedIds.has(page.id) && childMap.has(page.id)" class="wiki-tree-children">
                  <div
                    v-for="child in childMap.get(page.id)"
                    :key="child.id"
                    class="wiki-tree-row child-row"
                    @click="loadPage(child.slug)"
                  >
                    <span class="tree-toggle-spacer" />
                    <i class="pi pi-file" />
                    <span class="wiki-tree-title">{{ child.title }}</span>
                  </div>
                </div>
              </div>
            </template>
          </div>
          <div v-else class="wiki-empty">
            <i class="pi pi-book" style="font-size: 2rem; color: var(--mw-text-muted);" />
            <p class="text-muted">{{ t('wiki.noPages') }}</p>
            <p class="text-muted text-sm">{{ t('wiki.noPagesHint') }}</p>
          </div>
        </template>
      </template>

      <!-- Page View -->
      <template v-if="viewMode === 'view' && currentPage">
        <LoadingSpinner v-if="pageLoading" />
        <template v-else>
          <div class="wiki-page-header">
            <Button
              icon="pi pi-arrow-left"
              :label="t('wiki.backToTree')"
              severity="secondary"
              text
              size="small"
              @click="goToTree"
            />
            <div class="wiki-page-actions">
              <Button
                :label="t('wiki.edit')"
                icon="pi pi-pencil"
                size="small"
                severity="secondary"
                @click="startEdit"
              />
              <Button
                :label="t('wiki.history')"
                icon="pi pi-history"
                size="small"
                severity="secondary"
                text
                @click="showHistory"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                size="small"
                @click="showDeleteDialog = true"
              />
            </div>
          </div>

          <div class="wiki-page-content">
            <h1 class="wiki-page-title">{{ currentPage.title }}</h1>
            <div class="wiki-page-meta">
              <span v-if="currentPage.lastEditedByName" class="text-muted text-sm">
                {{ t('wiki.lastEdited', { name: currentPage.lastEditedByName }) }}
                &middot;
                {{ formatDateTime(currentPage.updatedAt) }}
              </span>
              <span v-else class="text-muted text-sm">
                {{ t('wiki.createdBy', { name: currentPage.createdByName }) }}
                &middot;
                {{ formatDateTime(currentPage.createdAt) }}
              </span>
            </div>

            <div class="wiki-rendered-content" v-html="sanitizeHtml(renderMarkdown(currentPage.content))" />

            <!-- Child pages -->
            <div v-if="currentPage.children.length" class="wiki-children-section">
              <h3>{{ t('wiki.childPages') }}</h3>
              <div class="wiki-page-list">
                <div
                  v-for="child in currentPage.children"
                  :key="child.id"
                  class="wiki-page-item"
                  @click="loadPage(child.slug)"
                >
                  <i class="pi pi-file" />
                  <span>{{ child.title }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </template>

      <!-- Edit Mode -->
      <template v-if="viewMode === 'edit' && currentPage">
        <div class="wiki-page-header">
          <Button
            icon="pi pi-arrow-left"
            :label="t('common.cancel')"
            severity="secondary"
            text
            size="small"
            @click="cancelEdit"
          />
          <div class="wiki-page-actions">
            <Button
              :label="showPreview ? t('wiki.edit') : t('wiki.preview')"
              :icon="showPreview ? 'pi pi-pencil' : 'pi pi-eye'"
              size="small"
              severity="secondary"
              text
              @click="showPreview = !showPreview"
            />
            <Button
              :label="t('common.save')"
              icon="pi pi-check"
              size="small"
              :loading="saving"
              :disabled="!editForm.title.trim()"
              @click="savePage"
            />
          </div>
        </div>

        <div class="wiki-edit-form">
          <div class="form-field">
            <label>{{ t('wiki.pageTitle') }} *</label>
            <InputText
              v-model="editForm.title"
              :placeholder="t('wiki.titlePlaceholder')"
              class="w-full"
            />
          </div>

          <template v-if="showPreview">
            <div class="wiki-preview-label">{{ t('wiki.preview') }}</div>
            <div class="wiki-rendered-content wiki-preview-box" v-html="sanitizeHtml(renderMarkdown(editForm.content))" />
          </template>
          <template v-else>
            <div class="form-field">
              <label>{{ t('wiki.content') }}</label>
              <Textarea
                v-model="editForm.content"
                :placeholder="t('wiki.contentPlaceholder')"
                class="w-full wiki-editor"
                rows="20"
                autoResize
              />
            </div>
          </template>
        </div>
      </template>

      <!-- History Mode -->
      <template v-if="viewMode === 'history' && currentPage">
        <div class="wiki-page-header">
          <Button
            icon="pi pi-arrow-left"
            :label="currentPage.title"
            severity="secondary"
            text
            size="small"
            @click="viewMode = 'view'"
          />
        </div>

        <h2 class="wiki-section-title">{{ t('wiki.versions') }}</h2>
        <div class="wiki-versions-list">
          <div
            v-for="(version, idx) in versions"
            :key="version.id"
            class="wiki-version-item"
            @click="viewVersion(version.id)"
          >
            <div class="version-info">
              <span class="version-title">{{ version.title }}</span>
              <span class="text-muted text-sm">
                {{ t('wiki.versionBy', { name: version.editedByName }) }}
                &middot;
                {{ formatDateTime(version.createdAt) }}
              </span>
            </div>
            <span v-if="idx === 0" class="version-badge">{{ t('wiki.currentVersion') }}</span>
          </div>
        </div>
      </template>

      <!-- Version View -->
      <template v-if="viewMode === 'version' && selectedVersion && currentPage">
        <div class="wiki-page-header">
          <Button
            icon="pi pi-arrow-left"
            :label="t('wiki.history')"
            severity="secondary"
            text
            size="small"
            @click="viewMode = 'history'"
          />
          <div class="wiki-page-actions">
            <Button
              :label="t('wiki.restoreVersion')"
              icon="pi pi-replay"
              size="small"
              severity="secondary"
              @click="restoreVersion"
            />
          </div>
        </div>

        <div class="wiki-page-content">
          <h1 class="wiki-page-title">{{ selectedVersion.title }}</h1>
          <div class="wiki-page-meta">
            <span class="text-muted text-sm">
              {{ t('wiki.versionBy', { name: selectedVersion.editedByName }) }}
              &middot;
              {{ formatDateTime(selectedVersion.createdAt) }}
            </span>
          </div>
          <div class="wiki-rendered-content" v-html="sanitizeHtml(renderMarkdown(selectedVersion.content))" />
        </div>
      </template>
    </template>

    <!-- Create Page Dialog -->
    <Dialog
      v-model:visible="showCreateDialog"
      :header="t('wiki.newPage')"
      modal
      :style="{ width: '600px', maxWidth: '90vw' }"
    >
      <div class="wiki-create-form">
        <div class="form-field">
          <label>{{ t('wiki.pageTitle') }} *</label>
          <InputText
            v-model="createForm.title"
            :placeholder="t('wiki.titlePlaceholder')"
            class="w-full"
            @keydown.enter="createPage"
          />
        </div>
        <div class="form-field">
          <label>{{ t('wiki.parentPage') }}</label>
          <Select
            v-model="createForm.parentId"
            :options="parentOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('wiki.content') }}</label>
          <Textarea
            v-model="createForm.content"
            :placeholder="t('wiki.contentPlaceholder')"
            class="w-full"
            rows="10"
            autoResize
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          text
          @click="showCreateDialog = false"
        />
        <Button
          :label="t('common.create')"
          icon="pi pi-plus"
          :loading="creating"
          :disabled="!createForm.title.trim()"
          @click="createPage"
        />
      </template>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog
      v-model:visible="showDeleteDialog"
      :header="t('wiki.deletePage')"
      modal
      :style="{ width: '450px', maxWidth: '90vw' }"
    >
      <p>{{ t('wiki.deleteConfirm') }}</p>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          text
          @click="showDeleteDialog = false"
        />
        <Button
          :label="t('common.delete')"
          severity="danger"
          icon="pi pi-trash"
          :loading="deleting"
          @click="deletePage"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.room-wiki {
  padding-top: 0.5rem;
}

.wiki-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.wiki-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  max-width: 300px;
}

.wiki-search-input {
  width: 100%;
}

.wiki-search-results {
  margin-bottom: 1rem;
}

.wiki-section-title {
  font-size: var(--mw-font-size-base, 1rem);
  font-weight: 600;
  color: var(--mw-text-secondary);
  margin-bottom: 0.75rem;
}

.wiki-tree {
  display: flex;
  flex-direction: column;
}

.wiki-tree-item {
  border-bottom: 1px solid var(--mw-border-light);
}

.wiki-tree-item:last-child {
  border-bottom: none;
}

.wiki-tree-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.5rem;
  cursor: pointer;
  border-radius: var(--mw-border-radius-sm);
  transition: background-color 0.15s;
}

.wiki-tree-row:hover {
  background: var(--mw-bg);
}

.child-row {
  padding-left: 2rem;
}

.tree-toggle {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
}

.tree-toggle-spacer {
  width: 1.5rem;
  flex-shrink: 0;
}

.wiki-tree-title {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  flex: 1;
}

.wiki-tree-children {
  margin-left: 0;
}

.wiki-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 3rem 1rem;
  text-align: center;
}

.wiki-page-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.wiki-page-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
  transition: background-color 0.15s;
  font-size: var(--mw-font-size-sm);
}

.wiki-page-item:hover {
  background: var(--mw-bg);
}

.wiki-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.wiki-page-actions {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.wiki-page-content {
  padding: 0 0.25rem;
}

.wiki-page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.wiki-page-meta {
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.wiki-rendered-content {
  line-height: 1.7;
  word-break: break-word;
}

.wiki-rendered-content :deep(h1) {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 1.25rem 0 0.625rem;
}

.wiki-rendered-content :deep(h2) {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 1rem 0 0.5rem;
}

.wiki-rendered-content :deep(h3) {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0.875rem 0 0.375rem;
}

.wiki-rendered-content :deep(p) {
  margin: 0.5rem 0;
}

.wiki-rendered-content :deep(code) {
  background: var(--mw-bg);
  padding: 0.125rem 0.375rem;
  border-radius: 3px;
  font-size: var(--mw-font-size-sm);
  font-family: 'Fira Code', 'Consolas', monospace;
}

.wiki-rendered-content :deep(pre) {
  background: var(--mw-bg);
  padding: 0.75rem 1rem;
  border-radius: var(--mw-border-radius-sm);
  overflow-x: auto;
  margin: 0.75rem 0;
}

.wiki-rendered-content :deep(pre code) {
  background: none;
  padding: 0;
}

.wiki-rendered-content :deep(ul) {
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}

.wiki-rendered-content :deep(li) {
  margin: 0.25rem 0;
}

.wiki-rendered-content :deep(blockquote) {
  border-left: 3px solid var(--mw-primary);
  padding-left: 1rem;
  margin: 0.75rem 0;
  color: var(--mw-text-secondary);
}

.wiki-rendered-content :deep(a) {
  color: var(--mw-primary);
  text-decoration: underline;
}

.wiki-rendered-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--mw-border-light);
  margin: 1rem 0;
}

.wiki-rendered-content :deep(strong) {
  font-weight: 600;
}

.wiki-children-section {
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px solid var(--mw-border-light);
}

.wiki-children-section h3 {
  font-size: var(--mw-font-size-base, 1rem);
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.wiki-edit-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.wiki-editor {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: var(--mw-font-size-sm);
  line-height: 1.6;
}

.wiki-preview-label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  color: var(--mw-text-secondary);
  margin-bottom: 0.25rem;
}

.wiki-preview-box {
  padding: 1rem;
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  min-height: 200px;
}

.wiki-create-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-field label {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
  color: var(--mw-text-secondary);
}

.wiki-versions-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.wiki-version-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
  transition: background-color 0.15s;
}

.wiki-version-item:hover {
  background: var(--mw-bg);
}

.version-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.version-title {
  font-size: var(--mw-font-size-sm);
  font-weight: 500;
}

.version-badge {
  font-size: var(--mw-font-size-xs);
  background: var(--mw-primary);
  color: white;
  padding: 0.125rem 0.5rem;
  border-radius: 99px;
  white-space: nowrap;
  flex-shrink: 0;
}

.text-muted {
  color: var(--mw-text-muted);
}

.text-sm {
  font-size: var(--mw-font-size-sm);
}

.text-center {
  text-align: center;
  padding: 2rem;
}

@media (max-width: 767px) {
  .wiki-header {
    flex-direction: column;
    align-items: stretch;
  }

  .wiki-search {
    max-width: none;
  }

  .wiki-page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .wiki-page-actions {
    width: 100%;
  }

  .wiki-page-actions :deep(.p-button) {
    flex: 1;
    justify-content: center;
  }
}
</style>
