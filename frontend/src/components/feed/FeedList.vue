<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFeedStore } from '@/stores/feed'
import FeedPostComponent from './FeedPost.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Button from 'primevue/button'

const { t } = useI18n()
const feed = useFeedStore()

onMounted(() => {
  feed.fetchFeed(true)
})
</script>

<template>
  <div class="feed-list">
    <LoadingSpinner v-if="feed.loading && !feed.posts.length" />

    <EmptyState
      v-else-if="!feed.posts.length"
      icon="pi pi-megaphone"
      :message="t('feed.noPosts')"
    />

    <template v-else>
      <FeedPostComponent
        v-for="post in feed.posts"
        :key="post.id"
        :post="post"
      />

      <div v-if="feed.hasMore" class="load-more">
        <Button
          :label="t('feed.loadMore')"
          :loading="feed.loading"
          severity="secondary"
          text
          @click="feed.fetchFeed()"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.load-more {
  display: flex;
  justify-content: center;
  padding: 1rem;
}
</style>
