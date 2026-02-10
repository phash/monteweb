<script setup lang="ts">
import ProgressSpinner from 'primevue/progressspinner'
</script>

<template>
  <div class="loading-wrapper">
    <ProgressSpinner style="width: 40px; height: 40px" />
  </div>
</template>

<style scoped>
.loading-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}
</style>
