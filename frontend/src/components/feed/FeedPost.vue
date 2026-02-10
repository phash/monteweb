<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useFeedStore } from '@/stores/feed'
import type { FeedPost } from '@/types/feed'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'

const props = defineProps<{ post: FeedPost }>()
const { t } = useI18n()
const auth = useAuthStore()
const feed = useFeedStore()

const showComments = ref(false)
const commentText = ref('')

function formatDate(date: string) {
  return new Date(date).toLocaleString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function toggleComments() {
  showComments.value = !showComments.value
  if (showComments.value) {
    await feed.fetchComments(props.post.id)
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return
  await feed.addComment(props.post.id, commentText.value.trim())
  commentText.value = ''
}

function handleDelete() {
  feed.deletePost(props.post.id)
}
</script>

<template>
  <div class="feed-post card" :class="{ pinned: post.pinned }">
    <div class="post-header">
      <div class="post-meta">
        <i class="pi pi-user" />
        <strong>{{ post.authorName }}</strong>
        <span class="post-source" v-if="post.sourceName">
          in <em>{{ post.sourceName }}</em>
        </span>
        <Tag v-if="post.pinned" :value="t('feed.pinned')" severity="warn" size="small" />
      </div>
      <span class="post-date">{{ formatDate(post.createdAt) }}</span>
    </div>

    <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
    <p class="post-content">{{ post.content }}</p>

    <div class="post-footer">
      <Button
        icon="pi pi-comment"
        :label="`${post.commentCount}`"
        text
        size="small"
        @click="toggleComments"
      />
      <Button
        v-if="post.authorId === auth.user?.id || auth.isAdmin"
        icon="pi pi-trash"
        text
        severity="danger"
        size="small"
        @click="handleDelete"
      />
    </div>

    <div v-if="showComments" class="comments-section">
      <div v-for="comment in feed.comments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <strong>{{ comment.authorName }}</strong>
          <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
        </div>
        <p>{{ comment.content }}</p>
      </div>

      <div class="comment-input">
        <Textarea
          v-model="commentText"
          :placeholder="t('feed.commentPlaceholder')"
          :autoResize="true"
          rows="2"
          class="comment-textarea"
        />
        <Button
          icon="pi pi-send"
          :disabled="!commentText.trim()"
          @click="submitComment"
          size="small"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.feed-post {
  margin-bottom: 0.75rem;
}

.feed-post.pinned {
  border-left: 3px solid var(--mw-warning);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-sm);
}

.post-meta i {
  color: var(--mw-text-muted);
}

.post-source {
  color: var(--mw-text-secondary);
}

.post-date {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  white-space: nowrap;
}

.post-title {
  font-size: var(--mw-font-size-md);
  margin-bottom: 0.5rem;
}

.post-content {
  white-space: pre-wrap;
  margin-bottom: 0.5rem;
}

.post-footer {
  display: flex;
  gap: 0.5rem;
  border-top: 1px solid var(--mw-border-light);
  padding-top: 0.5rem;
}

.comments-section {
  border-top: 1px solid var(--mw-border-light);
  padding-top: 0.75rem;
  margin-top: 0.5rem;
}

.comment-item {
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mw-border-light);
}

.comment-item:last-of-type {
  border-bottom: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.25rem;
}

.comment-date {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.comment-input {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
  align-items: flex-end;
}

.comment-textarea {
  flex: 1;
}
</style>
