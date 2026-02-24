<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useAuthStore } from '@/stores/auth'
import { useFeedStore } from '@/stores/feed'
import type { FeedPost } from '@/types/feed'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import RichContent from '@/components/common/RichContent.vue'
import ReactionBar from '@/components/common/ReactionBar.vue'
import InlinePoll from '@/components/common/InlinePoll.vue'
import MentionInput from '@/components/common/MentionInput.vue'
import { feedApi } from '@/api/feed.api'

const props = defineProps<{ post: FeedPost }>()
const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const auth = useAuthStore()
const feed = useFeedStore()

const showComments = ref(false)
const commentText = ref('')
const showDeleteConfirm = ref(false)
const postComments = computed(() => feed.commentsByPost[props.post.id] || [])

function formatDate(date: string) {
  return formatCompactDateTime(date)
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

function confirmDelete() {
  showDeleteConfirm.value = true
}

function handleDelete() {
  showDeleteConfirm.value = false
  feed.deletePost(props.post.id)
}

async function handlePostReaction(emoji: string) {
  try {
    const res = await feedApi.togglePostReaction(props.post.id, emoji)
    props.post.reactions = res.data.data
  } catch { /* ignore */ }
}

async function handleCommentReaction(commentId: string, emoji: string) {
  try {
    const res = await feedApi.toggleCommentReaction(commentId, emoji)
    const comment = postComments.value.find(c => c.id === commentId)
    if (comment) comment.reactions = res.data.data
  } catch { /* ignore */ }
}

async function handlePollVote(optionIds: string[]) {
  try {
    const res = await feedApi.votePoll(props.post.id, optionIds)
    props.post.poll = res.data.data
  } catch { /* ignore */ }
}

async function handlePollClose() {
  try {
    const res = await feedApi.closePoll(props.post.id)
    props.post.poll = res.data.data
  } catch { /* ignore */ }
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
    <p v-if="post.content" class="post-content"><RichContent :content="post.content" /></p>

    <InlinePoll
      v-if="post.poll"
      :poll="post.poll"
      :authorId="post.authorId"
      @vote="handlePollVote"
      @close="handlePollClose"
    />

    <ReactionBar
      :reactions="post.reactions || []"
      @react="handlePostReaction"
    />

    <div class="post-footer">
      <Button
        icon="pi pi-comment"
        :label="`${post.commentCount}`"
        text
        size="small"
        @click="toggleComments"
      />
      <Button
        v-if="auth.isAdmin || auth.isSectionAdmin"
        :icon="post.pinned ? 'pi pi-thumbtack' : 'pi pi-thumbtack'"
        :label="post.pinned ? t('feed.unpin') : t('feed.pin')"
        text
        size="small"
        :severity="post.pinned ? 'warn' : 'secondary'"
        @click="feed.pinPost(post.id)"
      />
      <Button
        v-if="post.authorId === auth.user?.id || auth.isAdmin"
        icon="pi pi-trash"
        text
        severity="danger"
        size="small"
        :aria-label="t('common.delete')"
        @click="confirmDelete"
      />
    </div>

    <div v-if="showComments" class="comments-section">
      <div v-for="comment in postComments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <strong>{{ comment.authorName }}</strong>
          <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
        </div>
        <p>{{ comment.content }}</p>
        <ReactionBar
          :reactions="comment.reactions || []"
          compact
          @react="(emoji: string) => handleCommentReaction(comment.id, emoji)"
        />
      </div>

      <div class="comment-input">
        <MentionInput
          v-model="commentText"
          :placeholder="t('feed.commentPlaceholder')"
          :autoResize="true"
          :rows="2"
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

    <!-- Delete confirmation -->
    <div v-if="showDeleteConfirm" class="delete-confirm" role="alertdialog" :aria-label="t('feed.confirmDeleteTitle')">
      <p>{{ t('feed.confirmDeleteMessage') }}</p>
      <div class="delete-confirm-actions">
        <Button :label="t('common.cancel')" severity="secondary" text size="small" @click="showDeleteConfirm = false" />
        <Button :label="t('common.delete')" severity="danger" size="small" @click="handleDelete" />
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
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
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

.delete-confirm {
  background: var(--mw-bg-highlight, rgba(239, 68, 68, 0.05));
  border: 1px solid var(--mw-danger, #ef4444);
  border-radius: var(--mw-border-radius);
  padding: 0.75rem;
  margin-top: 0.5rem;
}

.delete-confirm p {
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.5rem;
}

.delete-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

@media (max-width: 767px) {
  .post-header {
    flex-direction: column;
    gap: 0.25rem;
  }
  .post-date {
    white-space: normal;
  }
}
</style>
