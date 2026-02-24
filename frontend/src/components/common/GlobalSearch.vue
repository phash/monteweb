<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import ProgressSpinner from 'primevue/progressspinner'
import { searchApi, type SearchResult, type SearchType } from '@/api/search.api'

const { t } = useI18n()
const router = useRouter()

const visible = ref(false)
const query = ref('')
const results = ref<SearchResult[]>([])
const loading = ref(false)
const selectedIndex = ref(-1)
const activeFilter = ref<SearchType>('ALL')
const searchInput = ref<{ $el: HTMLElement } | null>(null)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

const filters: { key: SearchType; label: string; icon: string }[] = [
  { key: 'ALL', label: 'search.filterAll', icon: 'pi pi-search' },
  { key: 'USER', label: 'search.filterUsers', icon: 'pi pi-users' },
  { key: 'ROOM', label: 'search.filterRooms', icon: 'pi pi-home' },
  { key: 'POST', label: 'search.filterPosts', icon: 'pi pi-file' },
  { key: 'EVENT', label: 'search.filterEvents', icon: 'pi pi-calendar' },
  { key: 'FILE', label: 'search.filterFiles', icon: 'pi pi-paperclip' },
  { key: 'WIKI', label: 'search.filterWiki', icon: 'pi pi-book' },
  { key: 'TASK', label: 'search.filterTasks', icon: 'pi pi-check-square' },
]

const filteredResults = computed(() => results.value)

const groupedResults = computed(() => {
  const groups: Record<string, SearchResult[]> = {}
  for (const result of filteredResults.value) {
    if (!groups[result.type]) {
      groups[result.type] = []
    }
    groups[result.type]!.push(result)
  }
  return groups
})

const flatResults = computed(() => filteredResults.value)

function open() {
  visible.value = true
  query.value = ''
  results.value = []
  selectedIndex.value = -1
  activeFilter.value = 'ALL'
  nextTick(() => {
    const input = searchInput.value?.$el?.querySelector?.('input') ?? searchInput.value?.$el
    if (input && 'focus' in input) {
      ;(input as HTMLElement).focus()
    }
  })
}

function close() {
  visible.value = false
}

defineExpose({ open, close })

watch(query, (newVal) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!newVal || newVal.trim().length < 2) {
    results.value = []
    loading.value = false
    return
  }
  loading.value = true
  debounceTimer = setTimeout(() => {
    performSearch()
  }, 300)
})

watch(activeFilter, () => {
  if (query.value.trim().length >= 2) {
    loading.value = true
    performSearch()
  }
})

async function performSearch() {
  try {
    const response = await searchApi.search(query.value.trim(), activeFilter.value, 20)
    results.value = response.data.data
    selectedIndex.value = -1
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function navigateToResult(result: SearchResult) {
  close()
  if (result.url) {
    router.push(result.url)
  }
}

function onKeyDown(event: KeyboardEvent) {
  const total = flatResults.value.length
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    selectedIndex.value = total > 0 ? (selectedIndex.value + 1) % total : -1
    scrollToSelected()
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    selectedIndex.value = total > 0 ? (selectedIndex.value - 1 + total) % total : -1
    scrollToSelected()
  } else if (event.key === 'Enter' && selectedIndex.value >= 0) {
    event.preventDefault()
    const result = flatResults.value[selectedIndex.value]
    if (result) navigateToResult(result)
  } else if (event.key === 'Escape') {
    close()
  }
}

function scrollToSelected() {
  nextTick(() => {
    const el = document.querySelector('.search-result-item.selected')
    el?.scrollIntoView({ block: 'nearest' })
  })
}

function typeIcon(type: string): string {
  switch (type) {
    case 'USER': return 'pi pi-user'
    case 'ROOM': return 'pi pi-home'
    case 'POST': return 'pi pi-file'
    case 'EVENT': return 'pi pi-calendar'
    case 'FILE': return 'pi pi-paperclip'
    case 'WIKI': return 'pi pi-book'
    case 'TASK': return 'pi pi-check-square'
    default: return 'pi pi-search'
  }
}

function typeLabel(type: string): string {
  switch (type) {
    case 'USER': return t('search.typeUser')
    case 'ROOM': return t('search.typeRoom')
    case 'POST': return t('search.typePost')
    case 'EVENT': return t('search.typeEvent')
    case 'FILE': return t('search.typeFile')
    case 'WIKI': return t('search.typeWiki')
    case 'TASK': return t('search.typeTask')
    default: return type
  }
}

// Global keyboard shortcut: Ctrl+K / Cmd+K
function onGlobalKeyDown(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
    event.preventDefault()
    open()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onGlobalKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onGlobalKeyDown)
})
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="t('search.title')"
    :style="{ width: '600px', maxWidth: '95vw' }"
    :pt="{
      content: { style: 'padding: 0' },
      header: { style: 'padding: 1rem 1.25rem 0.5rem' },
    }"
    class="global-search-dialog"
    @keydown="onKeyDown"
  >
    <div class="search-container">
      <div class="search-input-wrapper">
        <i class="pi pi-search search-icon" />
        <InputText
          ref="searchInput"
          v-model="query"
          :placeholder="t('search.placeholder')"
          class="search-input"
          autofocus
        />
        <kbd class="search-shortcut" v-if="!query">
          <span class="shortcut-key">Esc</span>
        </kbd>
      </div>

      <div class="search-filters">
        <button
          v-for="filter in filters"
          :key="filter.key"
          :class="['filter-chip', { active: activeFilter === filter.key }]"
          @click="activeFilter = filter.key"
        >
          <i :class="filter.icon" />
          <span>{{ t(filter.label) }}</span>
        </button>
      </div>

      <div class="search-results" v-if="query.trim().length >= 2">
        <div v-if="loading" class="search-loading">
          <ProgressSpinner
            style="width: 30px; height: 30px"
            strokeWidth="4"
          />
        </div>

        <div v-else-if="results.length === 0" class="search-empty">
          <i class="pi pi-search" />
          <p>{{ t('search.noResults') }}</p>
        </div>

        <div v-else class="search-results-list">
          <template v-for="(group, type) in groupedResults" :key="type">
            <div class="result-group-header">
              <i :class="typeIcon(type as string)" />
              <span>{{ typeLabel(type as string) }}</span>
            </div>
            <button
              v-for="result in group"
              :key="result.id"
              :class="[
                'search-result-item',
                { selected: selectedIndex === flatResults.indexOf(result) }
              ]"
              @click="navigateToResult(result)"
              @mouseenter="selectedIndex = flatResults.indexOf(result)"
            >
              <div class="result-icon">
                <i :class="typeIcon(result.type)" />
              </div>
              <div class="result-content">
                <div class="result-title" v-html="result.snippet && result.type === 'FILE' ? result.title : result.title"></div>
                <div class="result-subtitle" v-if="result.subtitle">
                  {{ result.subtitle }}
                </div>
                <div class="result-snippet" v-if="result.snippet" v-html="result.snippet"></div>
              </div>
              <div class="result-meta" v-if="result.timestamp">
                <i class="pi pi-clock" />
              </div>
            </button>
          </template>
        </div>
      </div>

      <div v-else class="search-hint">
        <p>{{ t('search.hint') }}</p>
        <div class="search-shortcut-hint">
          <kbd><span class="shortcut-key">Ctrl</span></kbd>
          <span>+</span>
          <kbd><span class="shortcut-key">K</span></kbd>
          <span>{{ t('search.shortcutHint') }}</span>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.search-container {
  display: flex;
  flex-direction: column;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 0.5rem 1.25rem;
  gap: 0.5rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.search-icon {
  color: var(--mw-text-secondary);
  font-size: 1rem;
}

.search-input {
  flex: 1;
  border: none;
  box-shadow: none !important;
  font-size: 1rem;
  padding: 0.5rem 0;
  background: transparent;
}

.search-input:focus {
  outline: none;
}

.search-shortcut {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: var(--p-surface-100);
  border: 1px solid var(--mw-border-light);
  border-radius: 4px;
  padding: 0.125rem 0.375rem;
  font-size: 0.7rem;
  color: var(--mw-text-secondary);
}

.shortcut-key {
  font-family: monospace;
}

.search-filters {
  display: flex;
  gap: 0.375rem;
  padding: 0.5rem 1.25rem;
  border-bottom: 1px solid var(--mw-border-light);
  overflow-x: auto;
}

.filter-chip {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: var(--p-surface-50);
  border: 1px solid var(--mw-border-light);
  border-radius: 1rem;
  padding: 0.25rem 0.75rem;
  font-size: 0.8rem;
  cursor: pointer;
  white-space: nowrap;
  color: var(--mw-text-secondary);
  transition: all 0.15s ease;
}

.filter-chip:hover {
  background: var(--p-surface-100);
}

.filter-chip.active {
  background: var(--mw-primary);
  color: white;
  border-color: var(--mw-primary);
}

.filter-chip i {
  font-size: 0.75rem;
}

.search-results {
  max-height: 400px;
  overflow-y: auto;
}

.search-loading {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--mw-text-secondary);
}

.search-empty i {
  font-size: 2rem;
  opacity: 0.5;
}

.result-group-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--mw-text-secondary);
}

.result-group-header i {
  font-size: 0.7rem;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.625rem 1.25rem;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.1s ease;
}

.search-result-item:hover,
.search-result-item.selected {
  background: var(--p-surface-50);
}

.result-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.375rem;
  background: var(--p-surface-100);
  color: var(--mw-text-secondary);
  flex-shrink: 0;
}

.result-icon i {
  font-size: 0.875rem;
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--mw-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-subtitle {
  font-size: 0.775rem;
  color: var(--mw-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 0.125rem;
}

.result-snippet {
  font-size: 0.775rem;
  color: var(--mw-text-secondary);
  margin-top: 0.25rem;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.3;
}

.result-snippet :deep(mark) {
  background: var(--mw-highlight, #fff3cd);
  color: inherit;
  padding: 0 0.125rem;
  border-radius: 2px;
}

.result-meta {
  flex-shrink: 0;
  color: var(--mw-text-secondary);
  font-size: 0.75rem;
}

.search-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2rem 1.25rem;
  color: var(--mw-text-secondary);
}

.search-hint p {
  margin: 0;
  font-size: 0.875rem;
}

.search-shortcut-hint {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
}

.search-shortcut-hint kbd {
  background: var(--p-surface-100);
  border: 1px solid var(--mw-border-light);
  border-radius: 4px;
  padding: 0.125rem 0.375rem;
  font-size: 0.7rem;
}

@media (max-width: 767px) {
  .search-filters {
    padding: 0.375rem 0.75rem;
    gap: 0.25rem;
  }

  .filter-chip {
    padding: 0.2rem 0.5rem;
    font-size: 0.75rem;
  }
}
</style>
