<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomsStore } from '@/stores/rooms'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Chips from 'primevue/chips'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const router = useRouter()
const roomsStore = useRoomsStore()
const authStore = useAuthStore()
const toast = useToast()

const searchQuery = ref('')
const showCreateDialog = ref(false)
const newRoom = ref({ name: '', description: '', tags: [] as string[] })

onMounted(() => {
  roomsStore.discoverRooms()
})

function search() {
  roomsStore.discoverRooms(searchQuery.value || undefined)
}

async function joinRoom(roomId: string) {
  try {
    await roomsStore.joinRoom(roomId)
    toast.add({ severity: 'success', summary: t('discover.joined'), life: 3000 })
    await roomsStore.discoverRooms(searchQuery.value || undefined)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

async function createInterestRoom() {
  try {
    const room = await roomsStore.createInterestRoom({
      name: newRoom.value.name,
      description: newRoom.value.description || undefined,
      tags: newRoom.value.tags.length > 0 ? newRoom.value.tags : undefined,
    })
    showCreateDialog.value = false
    newRoom.value = { name: '', description: '', tags: [] }
    toast.add({ severity: 'success', summary: t('discover.created'), life: 3000 })
    router.push({ name: 'room-detail', params: { id: room.id } })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}
</script>

<template>
  <div class="p-4">
    <div class="flex justify-between items-center mb-4">
      <h1 class="text-2xl font-bold">{{ t('discover.title') }}</h1>
      <Button :label="t('discover.createRoom')" icon="pi pi-plus" @click="showCreateDialog = true" />
    </div>

    <!-- Search -->
    <div class="flex gap-2 mb-4">
      <InputText v-model="searchQuery" :placeholder="t('discover.searchPlaceholder')"
                 class="flex-1" @keyup.enter="search" />
      <Button icon="pi pi-search" @click="search" />
    </div>

    <!-- Room Grid -->
    <div v-if="roomsStore.loading" class="text-center p-8">
      <i class="pi pi-spin pi-spinner text-2xl"></i>
    </div>

    <div v-else-if="roomsStore.discoverableRooms.length === 0" class="text-center p-8 text-gray-500">
      {{ t('discover.noRooms') }}
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="room in roomsStore.discoverableRooms" :key="room.id"
           class="border rounded-lg p-4 hover:shadow-md transition-shadow">
        <div class="flex justify-between items-start mb-2">
          <h3 class="font-semibold text-lg cursor-pointer hover:text-blue-600"
              @click="router.push({ name: 'room-detail', params: { id: room.id } })">
            {{ room.name }}
          </h3>
          <Tag :value="t('rooms.types.' + room.type)" severity="info" />
        </div>

        <p v-if="room.description" class="text-sm text-gray-600 mb-3 line-clamp-2">
          {{ room.description }}
        </p>

        <div v-if="room.tags && room.tags.length > 0" class="flex flex-wrap gap-1 mb-3">
          <Tag v-for="tag in room.tags" :key="tag" :value="tag" severity="secondary" class="text-xs" />
        </div>

        <div class="flex justify-between items-center text-sm text-gray-500">
          <span><i class="pi pi-users mr-1"></i>{{ room.memberCount }} {{ t('discover.members') }}</span>
          <Button :label="t('discover.join')" icon="pi pi-sign-in" size="small"
                  @click="joinRoom(room.id)" />
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="roomsStore.discoverTotalPages > 1" class="flex justify-center gap-2 mt-4">
      <Button :label="t('common.previous')" icon="pi pi-chevron-left" text
              :disabled="roomsStore.discoverPage === 0"
              @click="roomsStore.discoverRooms(searchQuery || undefined, roomsStore.discoverPage - 1)" />
      <Button :label="t('common.next')" icon="pi pi-chevron-right" iconPos="right" text
              :disabled="roomsStore.discoverPage >= roomsStore.discoverTotalPages - 1"
              @click="roomsStore.discoverRooms(searchQuery || undefined, roomsStore.discoverPage + 1)" />
    </div>

    <!-- Create Interest Room Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('discover.createRoom')" modal style="width: 500px">
      <div class="flex flex-col gap-4">
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('rooms.name') }}</label>
          <InputText v-model="newRoom.name" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('rooms.description') }}</label>
          <InputText v-model="newRoom.description" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium mb-1">{{ t('discover.tags') }}</label>
          <Chips v-model="newRoom.tags" :placeholder="t('discover.tagsPlaceholder')" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" text @click="showCreateDialog = false" />
        <Button :label="t('common.create')" icon="pi pi-check" @click="createInterestRoom"
                :disabled="!newRoom.name" />
      </template>
    </Dialog>
  </div>
</template>
