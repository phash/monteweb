<script setup lang="ts">
defineProps<{
  icon?: string
  message: string
}>()
</script>

<template>
  <div class="empty-state">
    <i v-if="icon" :class="icon" class="empty-icon" aria-hidden="true" />
    <p class="empty-message">{{ message }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  color: var(--mw-text-muted);
  margin-bottom: 1rem;
}

.empty-message {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-md);
}
</style>
