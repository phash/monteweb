<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ url: string }>()
const { t } = useI18n()

const embedUrl = computed<string | null>(() => {
  const url = props.url

  // YouTube: youtube.com/watch?v=ID or youtu.be/ID
  const ytMatch = url.match(
    /(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]{11})/
  )
  if (ytMatch) {
    return `https://www.youtube-nocookie.com/embed/${ytMatch[1]}`
  }

  // Vimeo: vimeo.com/ID
  const vimeoMatch = url.match(/vimeo\.com\/(\d+)/)
  if (vimeoMatch) {
    return `https://player.vimeo.com/video/${vimeoMatch[1]}`
  }

  return null
})
</script>

<template>
  <div v-if="embedUrl" class="video-embed" :aria-label="t('feed.videoEmbed')">
    <iframe
      :src="embedUrl"
      frameborder="0"
      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
      allowfullscreen
      loading="lazy"
      :title="t('feed.videoEmbed')"
    />
  </div>
</template>

<style scoped>
.video-embed {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%; /* 16:9 */
  margin-top: 0.5rem;
  border-radius: var(--mw-border-radius, 8px);
  overflow: hidden;
  background: #000;
}

.video-embed iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: none;
}
</style>
