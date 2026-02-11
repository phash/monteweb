<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'
import { useFeedStore } from '@/stores/feed'
import { useAdminStore } from '@/stores/admin'
import { useFamilyStore } from '@/stores/family'
import PageTitle from '@/components/common/PageTitle.vue'
import SystemBanner from '@/components/feed/SystemBanner.vue'
import PostComposer from '@/components/feed/PostComposer.vue'
import FeedList from '@/components/feed/FeedList.vue'
import FamilyHoursWidget from '@/components/family/FamilyHoursWidget.vue'

const { t } = useI18n()
const toast = useToast()
const auth = useAuthStore()
const feed = useFeedStore()
const adminStore = useAdminStore()
const familyStore = useFamilyStore()

const jobboardEnabled = adminStore.isModuleEnabled('jobboard')

onMounted(() => {
  feed.fetchBanners()
  if (jobboardEnabled && !familyStore.hasFamily) {
    familyStore.fetchFamilies()
  }
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

    <FamilyHoursWidget
      v-if="jobboardEnabled && familyStore.primaryFamily"
      :familyId="familyStore.primaryFamily.id"
    />

    <PostComposer
      v-if="auth.isTeacher || auth.isAdmin"
      @submit="handlePost"
    />

    <FeedList />
  </div>
</template>
