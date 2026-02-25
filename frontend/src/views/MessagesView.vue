<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useAuthStore } from '@/stores/auth'
import { useMessagingStore } from '@/stores/messaging'
import { useRoomsStore } from '@/stores/rooms'
import { messagingApi } from '@/api/messaging.api'
import { filesApi } from '@/api/files.api'
import { formatMentions } from '@/composables/useMentions'
import type { MessageInfo } from '@/types/messaging'
import type { FileInfo } from '@/types/files'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import NewMessageDialog from '@/components/messaging/NewMessageDialog.vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import ReactionBar from '@/components/common/ReactionBar.vue'
import InlinePoll from '@/components/common/InlinePoll.vue'
import PollComposer from '@/components/common/PollComposer.vue'
import MentionInput from '@/components/common/MentionInput.vue'

const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const route = useRoute()
const auth = useAuthStore()
const messaging = useMessagingStore()
const rooms = useRoomsStore()

const selectedConversationId = ref<string | null>(null)
const messageText = ref('')
const showNewMessage = ref(false)
const showMessages = ref(false)
const showDeleteDialog = ref(false)
const selectedImage = ref<File | null>(null)
const imagePreviewUrl = ref<string | null>(null)
const fullSizeImageUrl = ref<string | null>(null)
const showFullImage = ref(false)
const showPollComposer = ref(false)
const selectedAttachment = ref<File | null>(null)
const showFileLinkDialog = ref(false)
const fileLinkRoomId = ref<string | null>(null)
const fileLinkFiles = ref<FileInfo[]>([])
const fileLinkLoading = ref(false)
const selectedFileLink = ref<{ fileId: string; roomId: string; fileName: string } | null>(null)

onMounted(async () => {
  await messaging.fetchConversations()
  const convId = route.params.conversationId as string | undefined
  if (convId) {
    await selectConversation(convId)
  }
})

watch(() => route.params.conversationId, async (newId) => {
  if (newId && typeof newId === 'string') {
    await selectConversation(newId)
  }
})

const selectedConversation = computed(() =>
  messaging.conversations.find(c => c.id === selectedConversationId.value)
)

const canSend = computed(() =>
  (messageText.value.trim().length > 0 || selectedImage.value !== null || selectedAttachment.value !== null || selectedFileLink.value !== null)
)

function getConversationName(conv: typeof messaging.conversations[0]) {
  if (conv.title) return conv.title
  const others = conv.participants.filter(p => p.userId !== auth.user?.id)
  return others.map(p => p.displayName).join(', ') || t('messages.conversation')
}

async function selectConversation(id: string) {
  selectedConversationId.value = id
  showMessages.value = true
  messaging.setReplyTo(null)
  clearImage()
  clearAttachment()
  clearFileLink()
  await messaging.fetchMessages(id)
  messaging.markAsRead(id)
}

function goBackToList() {
  showMessages.value = false
}

async function sendMessage() {
  if (!canSend.value || !selectedConversationId.value) return
  const content = messageText.value.trim() || undefined
  const image = selectedImage.value || undefined
  const replyToId = messaging.replyToMessage?.id || undefined
  const attachment = selectedAttachment.value || undefined
  const linkedFileId = selectedFileLink.value?.fileId || undefined
  const linkedRoomId = selectedFileLink.value?.roomId || undefined
  const linkedFileName = selectedFileLink.value?.fileName || undefined
  await messaging.sendMessage(selectedConversationId.value, content, image, replyToId, attachment, linkedFileId, linkedRoomId, linkedFileName)
  messageText.value = ''
  clearImage()
  clearAttachment()
  clearFileLink()
}

function onImageSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowed.includes(file.type)) {
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    return
  }

  selectedImage.value = file
  imagePreviewUrl.value = URL.createObjectURL(file)
}

function clearImage() {
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value)
  }
  selectedImage.value = null
  imagePreviewUrl.value = null
}

function triggerImageSelect() {
  const input = document.getElementById('msg-image-input') as HTMLInputElement
  input?.click()
}

function onAttachmentSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (file.type !== 'application/pdf') return
  if (file.size > 20 * 1024 * 1024) return

  selectedAttachment.value = file
}

function clearAttachment() {
  selectedAttachment.value = null
  const input = document.getElementById('msg-attachment-input') as HTMLInputElement
  if (input) input.value = ''
}

function triggerAttachmentSelect() {
  const input = document.getElementById('msg-attachment-input') as HTMLInputElement
  input?.click()
}

async function openFileLinkDialog() {
  showFileLinkDialog.value = true
  if (!rooms.myRooms.length) {
    await rooms.fetchMyRooms()
  }
}

async function onFileLinkRoomChange() {
  if (!fileLinkRoomId.value) {
    fileLinkFiles.value = []
    return
  }
  fileLinkLoading.value = true
  try {
    const res = await filesApi.listFiles(fileLinkRoomId.value)
    fileLinkFiles.value = res.data.data
  } catch {
    fileLinkFiles.value = []
  } finally {
    fileLinkLoading.value = false
  }
}

function selectFileLink(file: FileInfo) {
  selectedFileLink.value = {
    fileId: file.id,
    roomId: file.roomId,
    fileName: file.originalName,
  }
  showFileLinkDialog.value = false
  fileLinkRoomId.value = null
  fileLinkFiles.value = []
}

function clearFileLink() {
  selectedFileLink.value = null
}

function formatFileSize(bytes: number | null): string {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getFileIcon(contentType: string | null): string {
  if (!contentType) return 'pi pi-file'
  if (contentType === 'application/pdf') return 'pi pi-file-pdf'
  if (contentType.startsWith('image/')) return 'pi pi-image'
  if (contentType.includes('word') || contentType.includes('document')) return 'pi pi-file-word'
  if (contentType.includes('spreadsheet') || contentType.includes('excel')) return 'pi pi-file-excel'
  return 'pi pi-file'
}

function setReplyTo(msg: MessageInfo) {
  messaging.setReplyTo(msg)
}

async function handleMsgReaction(msg: MessageInfo, emoji: string) {
  try {
    const res = await messagingApi.toggleMessageReaction(msg.id, emoji)
    msg.reactions = res.data.data
  } catch { /* ignore */ }
}

function clearReply() {
  messaging.setReplyTo(null)
}

function openFullImage(imageId: string) {
  fullSizeImageUrl.value = messagingApi.imageUrl(imageId)
  showFullImage.value = true
}

function getMessagePreview(msg: MessageInfo) {
  if (msg.content) return msg.content
  if (msg.images?.length) return '\uD83D\uDDBC ' + t('messages.image')
  if (msg.attachments?.length) {
    const att = msg.attachments[0]!
    return '\uD83D\uDCCE ' + (att.originalFilename || att.linkedFileName || t('messages.file'))
  }
  if (msg.poll) return '\uD83D\uDCCA ' + msg.poll.question
  return ''
}

async function sendPollMessage(data: { question: string; options: string[]; multiple: boolean }) {
  if (!selectedConversationId.value) return
  await messagingApi.sendPollMessage(selectedConversationId.value, {
    question: data.question,
    options: data.options,
    multiple: data.multiple,
  })
  showPollComposer.value = false
  await messaging.fetchMessages(selectedConversationId.value)
}

async function handleMsgPollVote(msg: MessageInfo, optionIds: string[]) {
  try {
    const res = await messagingApi.voteMessagePoll(msg.id, optionIds)
    msg.poll = res.data.data
  } catch { /* ignore */ }
}

async function handleMsgPollClose(msg: MessageInfo) {
  try {
    const res = await messagingApi.closeMessagePoll(msg.id)
    msg.poll = res.data.data
  } catch { /* ignore */ }
}

async function toggleMute() {
  if (!selectedConversationId.value || !selectedConversation.value) return
  if (selectedConversation.value.muted) {
    await messaging.unmuteConversation(selectedConversationId.value)
  } else {
    await messaging.muteConversation(selectedConversationId.value)
  }
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
              <div class="conv-badges">
                <i v-if="conv.muted" class="pi pi-volume-off muted-icon" :title="t('messages.muted')" />
                <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</span>
              </div>
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
              :icon="selectedConversation?.muted ? 'pi pi-volume-off' : 'pi pi-volume-up'"
              text
              :severity="selectedConversation?.muted ? 'warn' : 'secondary'"
              size="small"
              :aria-label="selectedConversation?.muted ? t('messages.unmute') : t('messages.mute')"
              @click="toggleMute"
            />
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

                <!-- Reply reference -->
                <div v-if="msg.replyTo" class="reply-block" @click.stop>
                  <div class="reply-sender">{{ msg.replyTo.senderName }}</div>
                  <div class="reply-content">
                    <i v-if="msg.replyTo.hasImage" class="pi pi-image reply-image-icon" />
                    <i v-if="msg.replyTo.hasAttachment" class="pi pi-paperclip reply-image-icon" />
                    {{ msg.replyTo.contentPreview || (msg.replyTo.hasImage ? t('messages.image') : (msg.replyTo.hasAttachment ? t('messages.file') : '')) }}
                  </div>
                </div>

                <!-- Image -->
                <div v-if="msg.images?.length" class="message-image-container">
                  <img
                    v-for="img in msg.images"
                    :key="img.imageId"
                    :src="messagingApi.thumbnailUrl(img.imageId)"
                    :alt="img.originalFilename"
                    class="message-image"
                    @click="openFullImage(img.imageId)"
                  />
                </div>

                <!-- Attachments -->
                <div v-if="msg.attachments?.length" class="message-attachments">
                  <a
                    v-for="att in msg.attachments"
                    :key="att.id"
                    class="attachment-chip"
                    :href="att.attachmentType === 'FILE' ? messagingApi.attachmentUrl(att.id) : `/rooms/${att.linkedRoomId}/files`"
                    :target="att.attachmentType === 'FILE_LINK' ? '_blank' : undefined"
                    @click.stop
                  >
                    <i :class="att.attachmentType === 'FILE_LINK' ? 'pi pi-external-link' : getFileIcon(att.contentType)" />
                    <span class="attachment-name">{{ att.originalFilename || att.linkedFileName || t('messages.file') }}</span>
                    <span v-if="att.fileSize" class="attachment-size">{{ formatFileSize(att.fileSize) }}</span>
                  </a>
                </div>

                <p v-if="msg.content">{{ formatMentions(msg.content) }}</p>

                <InlinePoll
                  v-if="msg.poll"
                  :poll="msg.poll"
                  :authorId="msg.senderId"
                  @vote="(optionIds: string[]) => handleMsgPollVote(msg, optionIds)"
                  @close="handleMsgPollClose(msg)"
                />

                <ReactionBar
                  :reactions="msg.reactions || []"
                  compact
                  @react="(emoji: string) => handleMsgReaction(msg, emoji)"
                />

                <div class="message-footer">
                  <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
                  <button
                    class="reply-button"
                    :aria-label="t('messages.replyTo')"
                    @click.stop="setReplyTo(msg)"
                  >
                    <i class="pi pi-reply" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Reply preview bar -->
          <div v-if="messaging.replyToMessage" class="reply-preview-bar">
            <div class="reply-preview-content">
              <span class="reply-preview-sender">{{ messaging.replyToMessage.senderName }}</span>
              <span class="reply-preview-text">{{ getMessagePreview(messaging.replyToMessage) }}</span>
            </div>
            <Button
              icon="pi pi-times"
              text
              severity="secondary"
              size="small"
              @click="clearReply"
            />
          </div>

          <!-- Image preview bar -->
          <div v-if="imagePreviewUrl" class="image-preview-bar">
            <img :src="imagePreviewUrl" class="image-preview-thumb" alt="Preview" />
            <span class="image-preview-name">{{ selectedImage?.name }}</span>
            <Button
              icon="pi pi-times"
              text
              severity="secondary"
              size="small"
              @click="clearImage"
            />
          </div>

          <!-- Attachment preview bar -->
          <div v-if="selectedAttachment" class="image-preview-bar">
            <i class="pi pi-file-pdf attachment-preview-icon" />
            <span class="image-preview-name">{{ selectedAttachment.name }}</span>
            <span class="attachment-size">{{ formatFileSize(selectedAttachment.size) }}</span>
            <Button
              icon="pi pi-times"
              text
              severity="secondary"
              size="small"
              @click="clearAttachment"
            />
          </div>

          <!-- File link preview bar -->
          <div v-if="selectedFileLink" class="image-preview-bar">
            <i class="pi pi-external-link attachment-preview-icon" />
            <span class="image-preview-name">{{ selectedFileLink.fileName }}</span>
            <Button
              icon="pi pi-times"
              text
              severity="secondary"
              size="small"
              @click="clearFileLink"
            />
          </div>

          <!-- Poll composer -->
          <div v-if="showPollComposer" class="poll-composer-bar">
            <PollComposer
              @submit="sendPollMessage"
              @cancel="showPollComposer = false"
            />
          </div>

          <div class="message-input">
            <input
              id="msg-image-input"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              class="hidden-file-input"
              @change="onImageSelect"
            />
            <input
              id="msg-attachment-input"
              type="file"
              accept="application/pdf"
              class="hidden-file-input"
              @change="onAttachmentSelect"
            />
            <Button
              icon="pi pi-camera"
              text
              severity="secondary"
              :aria-label="t('messages.attachImage')"
              @click="triggerImageSelect"
            />
            <Button
              icon="pi pi-paperclip"
              text
              severity="secondary"
              :aria-label="t('messages.attachFile')"
              @click="triggerAttachmentSelect"
            />
            <Button
              icon="pi pi-link"
              text
              severity="secondary"
              :aria-label="t('messages.linkFile')"
              @click="openFileLinkDialog"
            />
            <Button
              icon="pi pi-chart-bar"
              text
              severity="secondary"
              :aria-label="t('poll.createPoll')"
              @click="showPollComposer = !showPollComposer"
            />
            <MentionInput
              v-model="messageText"
              :placeholder="t('messages.writePlaceholder')"
              :autoResize="true"
              :rows="2"
              class="input-field"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <Button
              icon="pi pi-send"
              :disabled="!canSend"
              :aria-label="t('messages.send')"
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

    <!-- Full-size image dialog -->
    <Dialog
      v-model:visible="showFullImage"
      modal
      dismissableMask
      :closable="true"
      :style="{ width: '90vw', maxWidth: '900px' }"
      :pt="{ content: { style: 'padding: 0; display: flex; justify-content: center;' } }"
    >
      <img
        v-if="fullSizeImageUrl"
        :src="fullSizeImageUrl"
        class="full-size-image"
        alt="Full size"
      />
    </Dialog>

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

    <!-- File Link Dialog -->
    <Dialog
      v-model:visible="showFileLinkDialog"
      :header="t('messages.linkFile')"
      modal
      :style="{ width: '500px', maxWidth: '90vw' }"
    >
      <div class="file-link-dialog">
        <Select
          v-model="fileLinkRoomId"
          :options="rooms.myRooms"
          optionLabel="name"
          optionValue="id"
          :placeholder="t('messages.selectRoom')"
          class="w-full mb-3"
          @change="onFileLinkRoomChange"
        />
        <LoadingSpinner v-if="fileLinkLoading" />
        <div v-else-if="fileLinkFiles.length" class="file-link-list">
          <div
            v-for="file in fileLinkFiles"
            :key="file.id"
            class="file-link-item"
            role="button"
            tabindex="0"
            @click="selectFileLink(file)"
            @keydown.enter="selectFileLink(file)"
          >
            <i :class="getFileIcon(file.contentType)" />
            <div class="file-link-info">
              <span class="file-link-name">{{ file.originalName }}</span>
              <span class="file-link-meta">{{ formatFileSize(file.fileSize) }}</span>
            </div>
          </div>
        </div>
        <EmptyState
          v-else-if="fileLinkRoomId"
          icon="pi pi-folder-open"
          :message="t('messages.noFiles')"
        />
      </div>
    </Dialog>
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
    height: calc(100vh - var(--mw-header-height) - var(--mw-bottom-nav-height) - 3rem);
  }
  .messages-panel {
    display: none;
  }
  .messages-panel.mobile-visible {
    display: flex;
    position: fixed;
    top: var(--mw-header-height);
    left: 0;
    right: 0;
    bottom: var(--mw-bottom-nav-height);
    z-index: 50;
    border-radius: 0;
    margin: 0;
  }
  .conversations-panel.mobile-hidden {
    display: none;
  }
  .back-button {
    display: inline-flex;
  }
  .message-bubble {
    max-width: 85%;
  }
  .page-header {
    flex-wrap: wrap;
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

.conv-badges {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.muted-icon {
  font-size: 0.75rem;
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
  position: relative;
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

/* Reply block */
.reply-block {
  background: rgba(0, 0, 0, 0.08);
  border-left: 3px solid var(--mw-primary);
  border-radius: 0 4px 4px 0;
  padding: 0.25rem 0.5rem;
  margin-bottom: 0.375rem;
  font-size: var(--mw-font-size-xs);
}

.message-item.own .reply-block {
  background: rgba(255, 255, 255, 0.15);
  border-left-color: rgba(255, 255, 255, 0.6);
}

.reply-sender {
  font-weight: 600;
  margin-bottom: 0.125rem;
}

.reply-content {
  opacity: 0.85;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.reply-image-icon {
  font-size: 0.75rem;
  margin-right: 0.25rem;
}

/* Message images */
.message-image-container {
  margin: 0.25rem 0;
}

.message-image {
  max-width: 100%;
  max-height: 250px;
  border-radius: 6px;
  cursor: pointer;
  display: block;
}

.message-image:hover {
  opacity: 0.9;
}

/* Message footer with time + reply button */
.message-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.25rem;
  gap: 0.5rem;
}

.message-time {
  font-size: 0.625rem;
  opacity: 0.7;
}

.reply-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.125rem;
  opacity: 0;
  transition: opacity 0.15s;
  color: inherit;
  font-size: 0.75rem;
  line-height: 1;
}

.message-bubble:hover .reply-button {
  opacity: 0.6;
}

.reply-button:hover {
  opacity: 1 !important;
}

/* Reply preview bar */
.reply-preview-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--mw-bg-hover);
  border-top: 1px solid var(--mw-border-light);
  border-left: 3px solid var(--mw-primary);
}

.reply-preview-content {
  flex: 1;
  min-width: 0;
  font-size: var(--mw-font-size-xs);
}

.reply-preview-sender {
  font-weight: 600;
  margin-right: 0.5rem;
}

.reply-preview-text {
  opacity: 0.75;
}

/* Image preview bar */
.image-preview-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-top: 1px solid var(--mw-border-light);
  background: var(--mw-bg-hover);
}

.image-preview-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}

.image-preview-name {
  flex: 1;
  min-width: 0;
  font-size: var(--mw-font-size-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hidden-file-input {
  display: none;
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

/* Poll composer bar */
.poll-composer-bar {
  padding: 0.5rem 1rem;
  border-top: 1px solid var(--mw-border-light);
}

/* Message attachments */
.message-attachments {
  margin: 0.25rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.attachment-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.625rem;
  border-radius: var(--mw-border-radius-sm);
  background: rgba(0, 0, 0, 0.06);
  color: inherit;
  text-decoration: none;
  font-size: var(--mw-font-size-xs);
  cursor: pointer;
  transition: background 0.15s;
  max-width: 100%;
}

.attachment-chip:hover {
  background: rgba(0, 0, 0, 0.12);
}

.message-item.own .attachment-chip {
  background: rgba(255, 255, 255, 0.15);
}

.message-item.own .attachment-chip:hover {
  background: rgba(255, 255, 255, 0.25);
}

.attachment-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.attachment-size {
  opacity: 0.6;
  white-space: nowrap;
}

.attachment-preview-icon {
  font-size: 1.25rem;
  color: var(--mw-primary);
}

/* File link dialog */
.file-link-dialog {
  min-height: 200px;
}

.file-link-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  max-height: 300px;
  overflow-y: auto;
}

.file-link-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--mw-border-radius-sm);
  cursor: pointer;
  transition: background 0.15s;
}

.file-link-item:hover {
  background: var(--mw-bg-hover);
}

.file-link-item i {
  font-size: 1.25rem;
  color: var(--mw-primary);
}

.file-link-info {
  flex: 1;
  min-width: 0;
}

.file-link-name {
  display: block;
  font-size: var(--mw-font-size-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-link-meta {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-secondary);
}

/* Full-size image dialog */
.full-size-image {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
}
</style>
