<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useFeedStore } from '@/stores/feed'
import PageTitle from '@/components/common/PageTitle.vue'
import SystemBanner from '@/components/feed/SystemBanner.vue'
import PostComposer from '@/components/feed/PostComposer.vue'
import FeedList from '@/components/feed/FeedList.vue'

const { t } = useI18n()
const auth = useAuthStore()
const feed = useFeedStore()

onMounted(() => {
  feed.fetchBanners()
})

async function handlePost(data: { title?: string; content: string }) {
  await feed.createPost({
    sourceType: 'SCHOOL',
    ...data,
  })
}
</script>

<template>
  <div>
    <PageTitle
      :title="t('nav.dashboard')"
      :subtitle="t('dashboard.welcome', { name: auth.user?.firstName ?? '' })"
    />

    <SystemBanner :banners="feed.banners" />

    <PostComposer
      v-if="auth.isTeacher || auth.isAdmin"
      @submit="handlePost"
    />

    <FeedList />
  </div>
</template>
