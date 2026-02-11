<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoomsStore } from '@/stores/rooms'
import { useAuthStore } from '@/stores/auth'
import { useAdminStore } from '@/stores/admin'
import { feedApi } from '@/api/feed.api'
import type { FeedPost } from '@/types/feed'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
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
import { useRouter } from 'vue-router'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const rooms = useRoomsStore()
const auth = useAuthStore()
const admin = useAdminStore()
const router = useRouter()

const roomPosts = ref<FeedPost[]>([])
const postsLoading = ref(false)
const activeTab = ref('0')

const filesEnabled = admin.config?.modules?.files ?? false
const calendarEnabled = admin.config?.modules?.calendar ?? false
const messagingEnabled = admin.config?.modules?.messaging ?? false
const chatEnabled = computed(() =>
  messagingEnabled && rooms.currentRoom?.settings?.chatEnabled !== false
)

onMounted(async () => {
  await rooms.fetchRoom(props.id)
  loadPosts()
})

async function loadPosts() {
  postsLoading.value = true
  try {
    const res = await feedApi.getRoomPosts(props.id)
    roomPosts.value = res.data.data.content
  } finally {
    postsLoading.value = false
  }
}

async function handlePost(data: { title?: string; content: string }) {
  const res = await feedApi.createRoomPost(props.id, data)
  roomPosts.value.unshift(res.data.data)
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

    <template v-else-if="rooms.currentRoom">
      <div class="room-header">
        <PageTitle :title="rooms.currentRoom.name" />
        <Tag
          :value="t(`rooms.types.${rooms.currentRoom.type}`)"
          severity="info"
        />
      </div>

      <div class="room-info card" v-if="rooms.currentRoom.description">
        <p>{{ rooms.currentRoom.description }}</p>
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
                <i class="pi pi-user" />
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
.mb-1 {
  margin-bottom: 1rem;
}

.room-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.room-info {
  margin-bottom: 1rem;
}

.text-muted {
  color: var(--mw-text-muted);
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

.member-item i {
  color: var(--mw-text-muted);
}
</style>
