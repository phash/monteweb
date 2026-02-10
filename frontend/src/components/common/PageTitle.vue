<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<template>
  <div class="page-title-wrapper">
    <h1 class="page-title">{{ title }}</h1>
    <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
  </div>
</template>

<style scoped>
.page-title-wrapper {
  margin-bottom: 1.5rem;
}

.page-subtitle {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
  margin-top: 0.25rem;
}
</style>
