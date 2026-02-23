<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'
import { useFeedStore } from '@/stores/feed'
import { useAdminStore } from '@/stores/admin'
import PageTitle from '@/components/common/PageTitle.vue'
import SystemBanner from '@/components/feed/SystemBanner.vue'
import PostComposer from '@/components/feed/PostComposer.vue'
import FeedList from '@/components/feed/FeedList.vue'
import DashboardFormsWidget from '@/components/forms/DashboardFormsWidget.vue'

const { t } = useI18n()
const toast = useToast()
const auth = useAuthStore()
const feed = useFeedStore()
const adminStore = useAdminStore()

const formsEnabled = adminStore.isModuleEnabled('forms')

onMounted(() => {
  feed.fetchBanners()
})

async function handlePost(data: { title?: string; content: string }) {
  await feed.createPost({
    sourceType: 'SCHOOL',
    ...data,
  })
  toast.add({ severity: 'success', summary: t('feed.postCreated'), life: 3000 })
}
</script>

<template>
  <div>
    <PageTitle
      :title="t('nav.dashboard')"
      :subtitle="t('dashboard.welcome', { name: auth.user?.firstName ?? '' })"
    />

    <SystemBanner :banners="feed.banners" />

    <DashboardFormsWidget v-if="formsEnabled" />

    <PostComposer
      v-if="auth.isTeacher || auth.isAdmin"
      @submit="handlePost"
    />

    <FeedList />
  </div>
</template>
