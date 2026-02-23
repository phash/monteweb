<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { feedApi } from '@/api/feed.api'
import type { LinkPreviewInfo } from '@/types/feed'
import Skeleton from 'primevue/skeleton'

const props = defineProps<{ url: string }>()
const { t } = useI18n()

const preview = ref<LinkPreviewInfo | null>(null)
const loading = ref(true)
const hasError = ref(false)

onMounted(async () => {
  try {
    const res = await feedApi.getLinkPreview(props.url)
    if (res.data.data && (res.data.data.title || res.data.data.description)) {
      preview.value = res.data.data
    } else {
      hasError.value = true
    }
  } catch {
    hasError.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-if="loading" class="link-preview-skeleton">
    <Skeleton width="80px" height="80px" />
    <div class="link-preview-skeleton-text">
      <Skeleton width="60%" height="1rem" />
      <Skeleton width="90%" height="0.85rem" />
      <Skeleton width="40%" height="0.75rem" />
    </div>
  </div>
  <a
    v-else-if="preview && !hasError"
    :href="preview.url"
    target="_blank"
    rel="noopener noreferrer"
    class="link-preview"
    :aria-label="t('feed.linkPreview')"
  >
    <img
      v-if="preview.imageUrl"
      :src="preview.imageUrl"
      :alt="preview.title || ''"
      class="link-preview-image"
      loading="lazy"
      @error="($event.target as HTMLImageElement).style.display = 'none'"
    />
    <div class="link-preview-content">
      <div v-if="preview.siteName" class="link-preview-site">{{ preview.siteName }}</div>
      <div v-if="preview.title" class="link-preview-title">{{ preview.title }}</div>
      <div v-if="preview.description" class="link-preview-description">{{ preview.description }}</div>
      <div class="link-preview-url">{{ preview.url }}</div>
    </div>
  </a>
</template>

<style scoped>
.link-preview-skeleton {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid var(--mw-border-light, #e5e7eb);
  border-radius: var(--mw-border-radius, 8px);
  margin-top: 0.5rem;
}

.link-preview-skeleton-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  justify-content: center;
}

.link-preview {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid var(--mw-border-light, #e5e7eb);
  border-radius: var(--mw-border-radius, 8px);
  margin-top: 0.5rem;
  text-decoration: none;
  color: inherit;
  transition: background 0.15s, border-color 0.15s;
  overflow: hidden;
}

.link-preview:hover {
  background: var(--mw-bg-subtle, #f9fafb);
  border-color: var(--mw-border, #d1d5db);
}

.link-preview-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.link-preview-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.link-preview-site {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted, #6b7280);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.link-preview-title {
  font-size: var(--mw-font-size-sm, 0.875rem);
  font-weight: 600;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.link-preview-description {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-secondary, #4b5563);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.link-preview-url {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted, #6b7280);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 480px) {
  .link-preview {
    flex-direction: column;
  }
  .link-preview-image {
    width: 100%;
    height: 120px;
  }
}
</style>
