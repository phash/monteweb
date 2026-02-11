<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoomsStore } from '@/stores/rooms'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { feedApi } from '@/api/feed.api'
import { roomsApi } from '@/api/rooms.api'
import type { FeedPost } from '@/types/feed'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AvatarUpload from '@/components/common/AvatarUpload.vue'
import PostComposer from '@/components/feed/PostComposer.vue'
import FeedPostComponent from '@/components/feed/FeedPost.vue'
import RoomFiles from '@/components/rooms/RoomFiles.vue'
import RoomChat from '@/components/rooms/RoomChat.vue'
import RoomDiscussions from '@/components/rooms/RoomDiscussions.vue'
import RoomEvents from '@/components/rooms/RoomEvents.vue'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Textarea from 'primevue/textarea'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const rooms = useRoomsStore()
const auth = useAuthStore()
const admin = useAdminStore()
const router = useRouter()
const toast = useToast()

const roomPosts = ref<FeedPost[]>([])
const postsLoading = ref(false)
const activeTab = ref('0')
const editingPublicDesc = ref(false)
const publicDescDraft = ref('')

const filesEnabled = admin.config?.modules?.files ?? false
const calendarEnabled = admin.config?.modules?.calendar ?? false
const messagingEnabled = admin.config?.modules?.messaging ?? false
const chatEnabled = computed(() =>
  messagingEnabled && rooms.currentRoom?.settings?.chatEnabled !== false
)

const isMember = computed(() => rooms.currentRoom?.members != null)
const isLeader = computed(() => {
  if (!rooms.currentRoom?.members) return false
  return rooms.currentRoom.members.some(
    m => m.userId === auth.user?.id && m.role === 'LEADER'
  )
})
const canEditRoom = computed(() => isLeader.value || auth.isAdmin)

onMounted(async () => {
  try {
    await rooms.fetchRoom(props.id)
    if (rooms.currentRoom) {
      loadPosts()
    }
  } catch {
    // Room not accessible
  }
})

async function loadPosts() {
  postsLoading.value = true
  try {
    const res = await feedApi.getRoomPosts(props.id)
    roomPosts.value = res.data.data.content
  } catch {
    roomPosts.value = []
  } finally {
    postsLoading.value = false
  }
}

async function handlePost(data: { title?: string; content: string }) {
  const res = await feedApi.createRoomPost(props.id, data)
  roomPosts.value.unshift(res.data.data)
}

async function handleRoomAvatarUpload(file: File) {
  await roomsApi.uploadAvatar(props.id, file)
  await rooms.fetchRoom(props.id)
}

async function handleRoomAvatarRemove() {
  await roomsApi.removeAvatar(props.id)
  await rooms.fetchRoom(props.id)
}

function startEditPublicDesc() {
  publicDescDraft.value = rooms.currentRoom?.publicDescription ?? ''
  editingPublicDesc.value = true
}

async function savePublicDesc() {
  await roomsApi.update(props.id, { publicDescription: publicDescDraft.value })
  await rooms.fetchRoom(props.id)
  editingPublicDesc.value = false
  toast.add({ severity: 'success', summary: t('common.save'), life: 2000 })
}

async function joinRoom() {
  try {
    await rooms.joinRoom(props.id)
    toast.add({ severity: 'success', summary: t('discover.joined'), life: 3000 })
    await rooms.fetchRoom(props.id)
    if (rooms.currentRoom?.members) loadPosts()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Error', life: 5000 })
  }
}
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      @click="router.push({ name: 'rooms' })"
      class="mb-1"
    />

    <LoadingSpinner v-if="rooms.loading" />

    <!-- Public view (non-member) -->
    <template v-else-if="rooms.currentPublicRoom && !rooms.currentRoom?.members">
      <div class="room-header">
        <div class="room-header-left">
          <AvatarUpload
            :image-url="rooms.currentPublicRoom.avatarUrl"
            size="lg"
            icon="pi-home"
            :editable="false"
          />
          <div>
            <PageTitle :title="rooms.currentPublicRoom.name" />
            <Tag
              :value="t(`rooms.types.${rooms.currentPublicRoom.type}`)"
              severity="info"
            />
          </div>
        </div>
      </div>

      <div class="card room-public-info">
        <p v-if="rooms.currentPublicRoom.publicDescription" class="public-desc">{{ rooms.currentPublicRoom.publicDescription }}</p>
        <p v-else class="text-muted">{{ t('rooms.notMember') }}</p>

        <div class="room-public-meta">
          <span><i class="pi pi-users" /> {{ rooms.currentPublicRoom.memberCount }} {{ t('rooms.members') }}</span>
        </div>

        <div v-if="rooms.currentPublicRoom.tags?.length" class="room-tags">
          <Tag v-for="tag in rooms.currentPublicRoom.tags" :key="tag" :value="tag" severity="secondary" />
        </div>

        <div class="join-section">
          <p class="text-muted">{{ t('rooms.joinToSee') }}</p>
          <Button
            v-if="rooms.currentPublicRoom.discoverable"
            :label="t('discover.join')"
            icon="pi pi-sign-in"
            @click="joinRoom"
          />
        </div>
      </div>
    </template>

    <!-- Full view (member / admin) -->
    <template v-else-if="rooms.currentRoom">
      <div class="room-header">
        <div class="room-header-left">
          <AvatarUpload
            :image-url="rooms.currentRoom.avatarUrl"
            size="lg"
            icon="pi-home"
            :editable="canEditRoom"
            @upload="handleRoomAvatarUpload"
            @remove="handleRoomAvatarRemove"
          />
          <div>
            <PageTitle :title="rooms.currentRoom.name" />
            <Tag
              :value="t(`rooms.types.${rooms.currentRoom.type}`)"
              severity="info"
            />
          </div>
        </div>
      </div>

      <div class="room-info card" v-if="rooms.currentRoom.description">
        <p>{{ rooms.currentRoom.description }}</p>
      </div>

      <!-- Public description (editable by leader) -->
      <div v-if="canEditRoom" class="card public-desc-card">
        <div class="public-desc-header">
          <label>{{ t('rooms.publicDescription') }}</label>
          <Button
            v-if="!editingPublicDesc"
            icon="pi pi-pencil"
            text
            size="small"
            @click="startEditPublicDesc"
          />
        </div>
        <template v-if="editingPublicDesc">
          <Textarea
            v-model="publicDescDraft"
            :placeholder="t('rooms.publicDescPlaceholder')"
            class="w-full"
            rows="3"
          />
          <div class="public-desc-actions">
            <Button :label="t('common.cancel')" text size="small" @click="editingPublicDesc = false" />
            <Button :label="t('common.save')" size="small" @click="savePublicDesc" />
          </div>
        </template>
        <p v-else-if="rooms.currentRoom.publicDescription" class="public-desc-text">
          {{ rooms.currentRoom.publicDescription }}
        </p>
        <p v-else class="text-muted text-sm">{{ t('rooms.publicDescPlaceholder') }}</p>
      </div>

      <Tabs :value="activeTab">
        <TabList>
          <Tab value="0">{{ t('rooms.infoBoard') }}</Tab>
          <Tab value="1">{{ t('rooms.members') }} ({{ rooms.currentRoom.members?.length ?? 0 }})</Tab>
          <Tab value="2">{{ t('discussions.title') }}</Tab>
          <Tab v-if="chatEnabled" value="3">{{ t('chat.title') }}</Tab>
          <Tab v-if="filesEnabled" value="4">{{ t('files.title') }}</Tab>
          <Tab v-if="calendarEnabled" value="5">{{ t('calendar.title') }}</Tab>
        </TabList>
        <TabPanels>
          <!-- Info-Board Tab -->
          <TabPanel value="0">
            <PostComposer
              v-if="auth.isTeacher || auth.isAdmin"
              @submit="handlePost"
            />
            <LoadingSpinner v-if="postsLoading" />
            <template v-else-if="roomPosts.length">
              <FeedPostComponent
                v-for="post in roomPosts"
                :key="post.id"
                :post="post"
              />
            </template>
            <p v-else class="text-muted text-center">{{ t('rooms.noPosts') }}</p>
          </TabPanel>

          <!-- Members Tab -->
          <TabPanel value="1">
            <div v-if="rooms.currentRoom.members?.length" class="members-list">
              <div v-for="member in rooms.currentRoom.members" :key="member.userId" class="member-item">
                <div class="member-avatar">
                  <img v-if="member.avatarUrl" :src="member.avatarUrl" alt="" class="member-avatar-img" />
                  <i v-else class="pi pi-user" />
                </div>
                <span>{{ member.displayName }}</span>
                <Tag :value="t(`rooms.roles.${member.role}`)" severity="secondary" size="small" />
              </div>
            </div>
            <p v-else class="text-muted">{{ t('rooms.noMembers') }}</p>
          </TabPanel>

          <!-- Discussions Tab -->
          <TabPanel value="2">
            <RoomDiscussions :roomId="id" />
          </TabPanel>

          <!-- Chat Tab -->
          <TabPanel v-if="chatEnabled" value="3">
            <RoomChat :roomId="id" />
          </TabPanel>

          <!-- Files Tab -->
          <TabPanel v-if="filesEnabled" value="4">
            <RoomFiles :roomId="id" />
          </TabPanel>

          <!-- Events Tab -->
          <TabPanel v-if="calendarEnabled" value="5">
            <RoomEvents :roomId="id" />
          </TabPanel>
        </TabPanels>
      </Tabs>
    </template>
  </div>
</template>

<style scoped>
.room-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.room-header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.room-info {
  margin-bottom: 1rem;
}

.room-public-info {
  margin-top: 1rem;
}

.public-desc {
  margin-bottom: 1rem;
  line-height: 1.5;
}

.room-public-meta {
  color: var(--mw-text-muted);
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.75rem;
}

.room-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
  margin-bottom: 1rem;
}

.join-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem;
  border-top: 1px solid var(--mw-border-light);
  margin-top: 1rem;
}

.public-desc-card {
  margin-bottom: 1rem;
}

.public-desc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.public-desc-header label {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
  color: var(--mw-text-secondary);
}

.public-desc-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.public-desc-text {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
}

.text-muted {
  color: var(--mw-text-muted);
}

.text-sm {
  font-size: var(--mw-font-size-sm);
}

.text-center {
  text-align: center;
  padding: 2rem;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg-card);
  border-radius: var(--mw-border-radius-sm);
  border: 1px solid var(--mw-border-light);
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mw-bg);
  color: var(--mw-text-muted);
  flex-shrink: 0;
}

.member-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
