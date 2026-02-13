<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useRoomsStore } from '@/stores/rooms'
import PageTitle from '@/components/common/PageTitle.vue'
import RoomCard from '@/components/rooms/RoomCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'

const { t } = useI18n()
const router = useRouter()
const rooms = useRoomsStore()

onMounted(() => {
  rooms.fetchMyRooms()
})
</script>

<template>
  <div>
    <div class="rooms-header">
      <PageTitle :title="t('rooms.title')" />
      <Button
        :label="t('discover.title')"
        icon="pi pi-search"
        severity="secondary"
        size="small"
        @click="router.push({ name: 'discover-rooms' })"
      />
    </div>

    <LoadingSpinner v-if="rooms.loading" />

    <EmptyState
      v-else-if="rooms.myRooms.length === 0"
      icon="pi pi-th-large"
      :message="t('rooms.noRooms')"
    />

    <div v-else class="rooms-grid">
      <RoomCard v-for="room in rooms.myRooms" :key="room.id" :room="room" />
    </div>
  </div>
</template>

<style scoped>
.rooms-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}
</style>
