<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useBookmarkStore } from '@/stores/bookmarks'
import { useLocaleDate } from '@/composables/useLocaleDate'
import type { BookmarkContentType } from '@/types/bookmark'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const bookmarkStore = useBookmarkStore()

const activeTab = ref<BookmarkContentType | ''>('')
const page = ref(0)

const tabs = [
  { label: 'bookmarks.tabs.all', value: '' },
  { label: 'bookmarks.tabs.posts', value: 'POST' },
  { label: 'bookmarks.tabs.events', value: 'EVENT' },
  { label: 'bookmarks.tabs.jobs', value: 'JOB' },
  { label: 'bookmarks.tabs.wiki', value: 'WIKI_PAGE' },
] as const

function typeIcon(type: string) {
  switch (type) {
    case 'POST': return 'pi pi-file-edit'
    case 'EVENT': return 'pi pi-calendar'
    case 'JOB': return 'pi pi-briefcase'
    case 'WIKI_PAGE': return 'pi pi-book'
    default: return 'pi pi-bookmark'
  }
}

function typeRoute(type: string, id: string) {
  switch (type) {
    case 'POST': return { name: 'dashboard' }
    case 'EVENT': return { name: 'event-detail', params: { id } }
    case 'JOB': return { name: 'job-detail', params: { id } }
    case 'WIKI_PAGE': return { name: 'dashboard' }
    default: return { name: 'dashboard' }
  }
}

function typeSeverity(type: string) {
  switch (type) {
    case 'POST': return 'info'
    case 'EVENT': return 'warn'
    case 'JOB': return 'success'
    case 'WIKI_PAGE': return 'secondary'
    default: return undefined
  }
}

async function loadBookmarks() {
  const type = activeTab.value || undefined
  await bookmarkStore.fetchBookmarks(type as BookmarkContentType | undefined, page.value)
}

async function removeBookmark(contentType: string, contentId: string) {
  await bookmarkStore.toggle(contentType as BookmarkContentType, contentId)
  await loadBookmarks()
}

watch(activeTab, () => {
  page.value = 0
  loadBookmarks()
})

onMounted(() => {
  loadBookmarks()
})
</script>

<template>
  <div class="bookmarks-view">
    <PageTitle :title="t('bookmarks.title')" :subtitle="t('bookmarks.subtitle')" />

    <div class="bookmark-tabs">
      <Button
        v-for="tab in tabs"
        :key="tab.value"
        :label="t(tab.label)"
        :class="{ 'tab-active': activeTab === tab.value }"
        text
        size="small"
        @click="activeTab = tab.value as any"
      />
    </div>

    <LoadingSpinner v-if="bookmarkStore.loading" />

    <div v-else-if="bookmarkStore.bookmarks.length === 0" class="empty-state">
      <i class="pi pi-bookmark" />
      <p>{{ t('bookmarks.empty') }}</p>
    </div>

    <div v-else class="bookmark-list">
      <div
        v-for="bm in bookmarkStore.bookmarks"
        :key="bm.id"
        class="bookmark-item card"
      >
        <div class="bookmark-content">
          <i :class="typeIcon(bm.contentType)" />
          <div class="bookmark-info">
            <Tag :value="t(`bookmarks.types.${bm.contentType}`)" :severity="typeSeverity(bm.contentType)" size="small" />
            <span class="bookmark-date">{{ formatCompactDateTime(bm.createdAt) }}</span>
          </div>
        </div>
        <div class="bookmark-actions">
          <router-link :to="typeRoute(bm.contentType, bm.contentId)">
            <Button icon="pi pi-external-link" text rounded size="small" />
          </router-link>
          <Button
            icon="pi pi-trash"
            text
            rounded
            size="small"
            severity="danger"
            @click="removeBookmark(bm.contentType, bm.contentId)"
          />
        </div>
      </div>

      <div v-if="bookmarkStore.totalElements > 20" class="pagination">
        <Button
          :label="t('common.previous')"
          :disabled="page === 0"
          text
          size="small"
          @click="page--; loadBookmarks()"
        />
        <span class="page-info">{{ page + 1 }} / {{ Math.ceil(bookmarkStore.totalElements / 20) }}</span>
        <Button
          :label="t('common.next')"
          :disabled="(page + 1) * 20 >= bookmarkStore.totalElements"
          text
          size="small"
          @click="page++; loadBookmarks()"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.bookmarks-view {
  max-width: 800px;
  margin: 0 auto;
}

.bookmark-tabs {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.tab-active {
  font-weight: 600;
  border-bottom: 2px solid var(--mw-primary);
  border-radius: 0;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--mw-text-muted);
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.bookmark-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.bookmark-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
}

.bookmark-content {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.bookmark-content > i {
  color: var(--mw-text-muted);
  font-size: 1.25rem;
}

.bookmark-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.bookmark-date {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.bookmark-actions {
  display: flex;
  gap: 0.25rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
}

.page-info {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-secondary);
}
</style>
