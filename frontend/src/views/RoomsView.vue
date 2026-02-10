<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoomsStore } from '@/stores/rooms'
import PageTitle from '@/components/common/PageTitle.vue'
import RoomCard from '@/components/rooms/RoomCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const { t } = useI18n()
const rooms = useRoomsStore()

onMounted(() => {
  rooms.fetchMyRooms()
})
</script>

<template>
  <div>
    <PageTitle :title="t('rooms.title')" />

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
.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}
</style>
