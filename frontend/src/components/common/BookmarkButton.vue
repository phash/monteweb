<script setup lang="ts">
import { computed } from 'vue'
import { useBookmarkStore } from '@/stores/bookmarks'
import type { BookmarkContentType } from '@/types/bookmark'
import Button from 'primevue/button'

const props = defineProps<{
  contentType: BookmarkContentType
  contentId: string
}>()

const bookmarkStore = useBookmarkStore()

const bookmarked = computed(() => bookmarkStore.isBookmarked(props.contentType, props.contentId))

async function toggleBookmark() {
  await bookmarkStore.toggle(props.contentType, props.contentId)
}
</script>

<template>
  <Button
    :icon="bookmarked ? 'pi pi-bookmark-fill' : 'pi pi-bookmark'"
    :class="['bookmark-btn', { 'bookmark-active': bookmarked }]"
    text
    rounded
    size="small"
    @click.stop="toggleBookmark"
  />
</template>

<style scoped>
.bookmark-btn {
  color: var(--mw-text-muted);
  padding: 0.25rem;
  width: 2rem;
  height: 2rem;
}

.bookmark-btn:hover {
  color: var(--mw-primary);
}

.bookmark-active {
  color: var(--mw-primary);
}
</style>
