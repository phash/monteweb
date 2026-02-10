<script setup lang="ts">
import type { SystemBanner } from '@/types/feed'
import Message from 'primevue/message'
import { useRouter } from 'vue-router'

const props = defineProps<{ banners: SystemBanner[] }>()
const router = useRouter()

function navigate(link: string | null) {
  if (link) router.push(link)
}
</script>

<template>
  <div v-if="banners.length" class="system-banners">
    <Message
      v-for="banner in banners"
      :key="banner.id"
      severity="warn"
      :closable="false"
      class="banner-item"
      @click="navigate(banner.link)"
    >
      <div class="banner-content">
        <strong>{{ banner.title }}</strong>
        <p>{{ banner.message }}</p>
      </div>
    </Message>
  </div>
</template>

<style scoped>
.system-banners {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.banner-item {
  cursor: pointer;
}

.banner-content p {
  margin-top: 0.25rem;
  font-size: var(--mw-font-size-sm);
}
</style>
