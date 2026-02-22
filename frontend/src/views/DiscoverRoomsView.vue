<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomsStore } from '@/stores/rooms'
import { useI18n } from 'vue-i18n'
import { roomsApi } from '@/api/rooms.api'
import type { RoomInfo } from '@/types/room'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Chips from 'primevue/chips'
import Textarea from 'primevue/textarea'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const router = useRouter()
const roomsStore = useRoomsStore()
const toast = useToast()

const searchQuery = ref('')
const showCreateDialog = ref(false)
const newRoom = ref({ name: '', description: '', tags: [] as string[] })

// Browse (all non-member rooms)
const browseRooms = ref<RoomInfo[]>([])
const browseTotalPages = ref(0)
const browsePage = ref(0)
const browseLoading = ref(false)

// Join request dialog
const showJoinRequestDialog = ref(false)
const joinRequestRoomId = ref('')
const joinRequestRoomName = ref('')
const joinRequestMessage = ref('')
const joinRequestLoading = ref(false)

onMounted(() => {
  roomsStore.discoverRooms()
  loadBrowseRooms()
})

function search() {
  roomsStore.discoverRooms(searchQuery.value || undefined)
  loadBrowseRooms(searchQuery.value || undefined)
}

async function loadBrowseRooms(query?: string, page = 0) {
  browseLoading.value = true
  try {
    const res = await roomsApi.browse({ q: query, page, size: 20 })
    browseRooms.value = res.data.data.content
    browseTotalPages.value = res.data.data.totalPages
    browsePage.value = page
  } catch {
    browseRooms.value = []
  } finally {
    browseLoading.value = false
  }
}

async function joinRoom(roomId: string) {
  try {
    await roomsStore.joinRoom(roomId)
    toast.add({ severity: 'success', summary: t('discover.joined'), life: 3000 })
    await roomsStore.discoverRooms(searchQuery.value || undefined)
    await loadBrowseRooms(searchQuery.value || undefined)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}

function openJoinRequestDialog(room: RoomInfo) {
  joinRequestRoomId.value = room.id
  joinRequestRoomName.value = room.name
  joinRequestMessage.value = ''
  showJoinRequestDialog.value = true
}

async function submitJoinRequest() {
  joinRequestLoading.value = true
  try {
    await roomsApi.requestJoin(joinRequestRoomId.value, joinRequestMessage.value || undefined)
    toast.add({ severity: 'success', summary: t('rooms.joinRequestSent'), life: 3000 })
    showJoinRequestDialog.value = false
    await loadBrowseRooms(searchQuery.value || undefined)
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  } finally {
    joinRequestLoading.value = false
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
      <Button icon="pi pi-search" :aria-label="t('common.search')" @click="search" />
    </div>

    <!-- Discoverable Rooms (open join) -->
    <div v-if="roomsStore.loading" class="text-center p-8">
      <i class="pi pi-spin pi-spinner text-2xl"></i>
    </div>

    <div v-else-if="roomsStore.discoverableRooms.length === 0 && browseRooms.length === 0" class="text-center p-8 text-gray-500">
      {{ t('discover.noRooms') }}
    </div>

    <template v-else>
      <!-- Open rooms (discoverable) -->
      <div v-if="roomsStore.discoverableRooms.length > 0" class="mb-6">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="room in roomsStore.discoverableRooms" :key="room.id"
               class="border rounded-lg p-4 hover:shadow-md transition-shadow">
            <div class="flex justify-between items-start mb-2">
              <div class="flex items-center gap-2">
                <div class="discover-avatar">
                  <img v-if="room.avatarUrl" :src="room.avatarUrl" :alt="room.name" class="discover-avatar-img" />
                  <i v-else class="pi pi-home" />
                </div>
                <h3 class="font-semibold text-lg cursor-pointer hover:text-blue-600"
                    tabindex="0" role="link"
                    @click="router.push({ name: 'room-detail', params: { id: room.id } })"
                    @keydown.enter="router.push({ name: 'room-detail', params: { id: room.id } })">
                  {{ room.name }}
                </h3>
              </div>
              <Tag :value="t('rooms.types.' + room.type)" severity="info" />
            </div>

            <p v-if="room.publicDescription || room.description" class="text-sm text-gray-600 mb-3 line-clamp-2">
              {{ room.publicDescription || room.description }}
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
      </div>

      <!-- Pagination for discoverable rooms -->
      <div v-if="roomsStore.discoverTotalPages > 1" class="flex justify-center gap-2 mt-4 mb-6">
        <Button :label="t('common.previous')" icon="pi pi-chevron-left" text
                :disabled="roomsStore.discoverPage === 0"
                @click="roomsStore.discoverRooms(searchQuery || undefined, roomsStore.discoverPage - 1)" />
        <Button :label="t('common.next')" icon="pi pi-chevron-right" iconPos="right" text
                :disabled="roomsStore.discoverPage >= roomsStore.discoverTotalPages - 1"
                @click="roomsStore.discoverRooms(searchQuery || undefined, roomsStore.discoverPage + 1)" />
      </div>

      <!-- Closed rooms (non-discoverable, non-member) -->
      <div v-if="browseRooms.length > 0">
        <h2 class="text-xl font-semibold mb-3">{{ t('rooms.closedRooms') }}</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="room in browseRooms" :key="room.id"
               class="border rounded-lg p-4 hover:shadow-md transition-shadow">
            <div class="flex justify-between items-start mb-2">
              <div class="flex items-center gap-2">
                <div class="discover-avatar">
                  <img v-if="room.avatarUrl" :src="room.avatarUrl" :alt="room.name" class="discover-avatar-img" />
                  <i v-else class="pi pi-home" />
                </div>
                <h3 class="font-semibold text-lg cursor-pointer hover:text-blue-600"
                    tabindex="0" role="link"
                    @click="router.push({ name: 'room-detail', params: { id: room.id } })"
                    @keydown.enter="router.push({ name: 'room-detail', params: { id: room.id } })">
                  {{ room.name }}
                </h3>
              </div>
              <Tag :value="t('rooms.types.' + room.type)" severity="info" />
            </div>

            <p v-if="room.publicDescription" class="text-sm text-gray-600 mb-3 line-clamp-2">
              {{ room.publicDescription }}
            </p>

            <div class="flex justify-between items-center text-sm text-gray-500">
              <span><i class="pi pi-users mr-1"></i>{{ room.memberCount }} {{ t('discover.members') }}</span>
              <Button v-if="room.joinPolicy === 'OPEN'" :label="t('discover.join')" icon="pi pi-sign-in" size="small"
                      @click="joinRoom(room.id)" />
              <Button v-else-if="room.joinPolicy === 'REQUEST'" :label="t('rooms.requestJoin')" icon="pi pi-send" size="small" severity="secondary"
                      @click="openJoinRequestDialog(room)" />
              <Tag v-else :value="t('rooms.inviteOnly')" severity="warn" />
            </div>
          </div>
        </div>

        <!-- Pagination for browse rooms -->
        <div v-if="browseTotalPages > 1" class="flex justify-center gap-2 mt-4">
          <Button :label="t('common.previous')" icon="pi pi-chevron-left" text
                  :disabled="browsePage === 0"
                  @click="loadBrowseRooms(searchQuery || undefined, browsePage - 1)" />
          <Button :label="t('common.next')" icon="pi pi-chevron-right" iconPos="right" text
                  :disabled="browsePage >= browseTotalPages - 1"
                  @click="loadBrowseRooms(searchQuery || undefined, browsePage + 1)" />
        </div>
      </div>
    </template>

    <!-- Create Interest Room Dialog -->
    <Dialog v-model:visible="showCreateDialog" :header="t('discover.createRoom')" modal :style="{ width: '500px', maxWidth: '90vw' }">
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
        <Button :label="t('common.cancel')" severity="secondary" text @click="showCreateDialog = false" />
        <Button :label="t('common.create')" icon="pi pi-check" @click="createInterestRoom"
                :disabled="!newRoom.name" />
      </template>
    </Dialog>

    <!-- Join Request Dialog -->
    <Dialog v-model:visible="showJoinRequestDialog" :header="t('rooms.requestJoin')" modal :style="{ width: '450px', maxWidth: '90vw' }">
      <div class="flex flex-col gap-3">
        <p>{{ t('rooms.joinRequestMessage', { room: joinRequestRoomName }) }}</p>
        <Textarea v-model="joinRequestMessage" :placeholder="t('rooms.joinRequestPlaceholder')"
                  class="w-full" rows="3" />
      </div>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" text @click="showJoinRequestDialog = false" />
        <Button :label="t('rooms.requestJoin')" icon="pi pi-send"
                :loading="joinRequestLoading" @click="submitJoinRequest" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.discover-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mw-bg, #f3f4f6);
  color: var(--mw-text-muted, #9ca3af);
  flex-shrink: 0;
}

.discover-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
