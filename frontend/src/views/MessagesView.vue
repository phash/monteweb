<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useAuthStore } from '@/stores/auth'
import { useMessagingStore } from '@/stores/messaging'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import NewMessageDialog from '@/components/messaging/NewMessageDialog.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'

const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const route = useRoute()
const auth = useAuthStore()
const messaging = useMessagingStore()

const selectedConversationId = ref<string | null>(null)
const messageText = ref('')
const showNewMessage = ref(false)
const showMessages = ref(false)
const showDeleteDialog = ref(false)

onMounted(async () => {
  await messaging.fetchConversations()
  // Deep-link support: open conversation from route param
  const convId = route.params.conversationId as string | undefined
  if (convId) {
    await selectConversation(convId)
  }
})

// Watch for route param changes
watch(() => route.params.conversationId, async (newId) => {
  if (newId && typeof newId === 'string') {
    await selectConversation(newId)
  }
})

const selectedConversation = computed(() =>
  messaging.conversations.find(c => c.id === selectedConversationId.value)
)

function getConversationName(conv: typeof messaging.conversations[0]) {
  if (conv.title) return conv.title
  const others = conv.participants.filter(p => p.userId !== auth.user?.id)
  return others.map(p => p.displayName).join(', ') || t('messages.conversation')
}

async function selectConversation(id: string) {
  selectedConversationId.value = id
  showMessages.value = true
  await messaging.fetchMessages(id)
  messaging.markAsRead(id)
}

function goBackToList() {
  showMessages.value = false
}

async function sendMessage() {
  if (!messageText.value.trim() || !selectedConversationId.value) return
  await messaging.sendMessage(selectedConversationId.value, messageText.value.trim())
  messageText.value = ''
}

async function handleDeleteConversation() {
  if (!selectedConversationId.value) return
  await messaging.deleteConversation(selectedConversationId.value)
  selectedConversationId.value = null
  showMessages.value = false
  showDeleteDialog.value = false
}

function onConversationStarted(conversationId: string) {
  selectConversation(conversationId)
}

function formatTime(date: string | null) {
  if (!date) return ''
  return formatCompactDateTime(date)
}
</script>

<template>
  <div>
    <div class="page-header">
      <PageTitle :title="t('nav.messages')" />
      <Button
        :label="t('messages.newMessage')"
        icon="pi pi-plus"
        @click="showNewMessage = true"
      />
    </div>

    <div class="messages-layout">
      <!-- Conversation list -->
      <div class="conversations-panel card" :class="{ 'mobile-hidden': showMessages }">
        <LoadingSpinner v-if="messaging.loading && !messaging.conversations.length" />
        <EmptyState
          v-else-if="!messaging.conversations.length"
          icon="pi pi-comments"
          :message="t('messages.noConversations')"
        />
        <div v-else class="conversation-list">
          <div
            v-for="conv in messaging.conversations"
            :key="conv.id"
            class="conversation-item"
            :class="{ active: conv.id === selectedConversationId, unread: conv.unreadCount > 0 }"
            role="button"
            tabindex="0"
            @click="selectConversation(conv.id)"
            @keydown.enter="selectConversation(conv.id)"
          >
            <div class="conv-info">
              <strong>{{ getConversationName(conv) }}</strong>
              <p v-if="conv.lastMessage" class="last-message">{{ conv.lastMessage }}</p>
            </div>
            <div class="conv-meta">
              <span class="conv-time">{{ formatTime(conv.lastMessageAt) }}</span>
              <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Message area -->
      <div class="messages-panel card" :class="{ 'mobile-visible': showMessages }">
        <template v-if="selectedConversationId">
          <div class="messages-header">
            <Button
              icon="pi pi-arrow-left"
              text
              severity="secondary"
              size="small"
              class="back-button"
              :aria-label="t('common.back')"
              @click="goBackToList"
            />
            <strong class="header-title">{{ selectedConversation ? getConversationName(selectedConversation) : '' }}</strong>
            <Button
              icon="pi pi-trash"
              text
              severity="danger"
              size="small"
              :aria-label="t('common.delete')"
              @click="showDeleteDialog = true"
            />
          </div>

          <div class="messages-list">
            <div
              v-for="msg in messaging.messages"
              :key="msg.id"
              class="message-item"
              :class="{ own: msg.senderId === auth.user?.id }"
            >
              <div class="message-bubble">
                <span v-if="msg.senderId !== auth.user?.id" class="sender-name">{{ msg.senderName }}</span>
                <p>{{ msg.content }}</p>
                <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div class="message-input">
            <Textarea
              v-model="messageText"
              :placeholder="t('messages.writePlaceholder')"
              :autoResize="true"
              rows="2"
              class="input-field"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <Button
              icon="pi pi-send"
              :disabled="!messageText.trim()"
              @click="sendMessage"
            />
          </div>
        </template>

        <EmptyState
          v-else
          icon="pi pi-comments"
          :message="t('messages.selectConversation')"
        />
      </div>
    </div>

    <Dialog v-model:visible="showDeleteDialog" :header="t('messages.deleteTitle')" modal :style="{ width: '400px', maxWidth: '90vw' }">
      <p>{{ t('messages.deleteConfirm') }}</p>
      <template #footer>
        <Button :label="t('common.no')" severity="secondary" text @click="showDeleteDialog = false" />
        <Button :label="t('common.yes')" severity="danger" @click="handleDeleteConversation" />
      </template>
    </Dialog>

    <NewMessageDialog
      v-model:visible="showNewMessage"
      @conversation-started="onConversationStarted"
    />
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.messages-layout {
  display: grid;
  grid-template-columns: minmax(240px, 320px) 1fr;
  gap: 1rem;
  height: calc(100vh - var(--mw-header-height) - 8rem);
}

@media (max-width: 767px) {
  .messages-layout {
    grid-template-columns: 1fr;
  }
  .messages-panel {
    display: none;
  }
  .messages-panel.mobile-visible {
    display: flex;
  }
  .conversations-panel.mobile-hidden {
    display: none;
  }
  .back-button {
    display: inline-flex;
  }
}

.conversations-panel {
  overflow-y: auto;
  padding: 0;
}

.conversation-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--mw-border-light);
  cursor: pointer;
  transition: background 0.15s;
}

.conversation-item:hover {
  background: var(--mw-bg-hover);
}

.conversation-item.active {
  background: var(--mw-bg-highlight, rgba(59, 130, 246, 0.08));
}

.conversation-item.unread .conv-info strong {
  color: var(--mw-primary);
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-info strong {
  font-size: var(--mw-font-size-sm);
}

.last-message {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 0.125rem;
}

.conv-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.conv-time {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.unread-badge {
  background: var(--mw-primary);
  color: white;
  font-size: 0.625rem;
  font-weight: 700;
  padding: 0.125rem 0.375rem;
  border-radius: 999px;
  min-width: 1.25rem;
  text-align: center;
}

.messages-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--mw-border-light);
}

.header-title {
  flex: 1;
}

.back-button {
  display: none;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.message-item {
  display: flex;
}

.message-item.own {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 0.5rem 0.75rem;
  border-radius: var(--mw-border-radius-sm);
  background: var(--mw-bg-hover);
}

.message-item.own .message-bubble {
  background: var(--mw-primary);
  color: white;
}

.sender-name {
  font-size: var(--mw-font-size-xs);
  font-weight: 600;
  display: block;
  margin-bottom: 0.125rem;
}

.message-time {
  font-size: 0.625rem;
  opacity: 0.7;
  display: block;
  text-align: right;
  margin-top: 0.25rem;
}

.message-input {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--mw-border-light);
  align-items: flex-end;
}

.input-field {
  flex: 1;
}
</style>
