<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoomsStore } from '@/stores/rooms'
import { useMessagingStore } from '@/stores/messaging'
import { useAuthStore } from '@/stores/auth'
import { messagingApi } from '@/api/messaging.api'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import type { MessageInfo } from '@/types/messaging'
import type { ChannelType } from '@/types/room'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import SelectButton from 'primevue/selectbutton'
import Dialog from 'primevue/dialog'

const props = defineProps<{ roomId: string }>()

const { t } = useI18n()
const { formatTime: localeFormatTime, formatShortDate } = useLocaleDate()
const roomsStore = useRoomsStore()
const messagingStore = useMessagingStore()
const authStore = useAuthStore()

const activeChannel = ref<ChannelType>('MAIN')
const messageText = ref('')
const messagesContainer = ref<HTMLElement>()
const isMuted = ref(false)
const selectedImage = ref<File | null>(null)
const imagePreviewUrl = ref<string | null>(null)
const fullSizeImageUrl = ref<string | null>(null)
const showFullImage = ref(false)

const channelOptions = computed(() => {
  return roomsStore.chatChannels.map(ch => ({
    label: t('chat.channels.' + ch.channelType),
    value: ch.channelType
  }))
})

const currentChannel = computed(() => {
  return roomsStore.chatChannels.find(ch => ch.channelType === activeChannel.value)
})

const canSend = computed(() =>
  messageText.value.trim().length > 0 || selectedImage.value !== null
)

// Group messages with date separators
const groupedMessages = computed(() => {
  const groups: Array<{ type: 'separator'; label: string } | { type: 'message'; msg: MessageInfo }> = []
  let lastDate = ''

  for (const msg of messagingStore.messages) {
    const msgDate = new Date(msg.createdAt).toLocaleDateString()
    if (msgDate !== lastDate) {
      lastDate = msgDate
      groups.push({ type: 'separator', label: formatDateLabel(msg.createdAt) })
    }
    groups.push({ type: 'message', msg })
  }
  return groups
})

onMounted(async () => {
  await roomsStore.fetchChatChannels(props.roomId)
  if (roomsStore.chatChannels.length === 0) {
    await roomsStore.getOrCreateChatChannel(props.roomId, 'MAIN')
    await roomsStore.fetchChatChannels(props.roomId)
  }
  if (currentChannel.value) {
    await messagingStore.fetchMessages(currentChannel.value.conversationId)
    await messagingStore.fetchConversation(currentChannel.value.conversationId)
    isMuted.value = messagingStore.currentConversation?.muted ?? false
    scrollToBottom()
  }
})

watch(() => messagingStore.messages.length, () => {
  scrollToBottom()
})

watch(activeChannel, async () => {
  if (currentChannel.value) {
    messagingStore.setReplyTo(null)
    clearImage()
    await messagingStore.fetchMessages(currentChannel.value.conversationId)
    await messagingStore.fetchConversation(currentChannel.value.conversationId)
    isMuted.value = messagingStore.currentConversation?.muted ?? false
  }
})

async function sendMessage() {
  if (!canSend.value || !currentChannel.value) return
  const content = messageText.value.trim() || undefined
  const image = selectedImage.value || undefined
  const replyToId = messagingStore.replyToMessage?.id || undefined
  await messagingStore.sendMessage(currentChannel.value.conversationId, content, image, replyToId)
  messageText.value = ''
  clearImage()
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    setTimeout(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTo({
          top: messagesContainer.value.scrollHeight,
          behavior: 'smooth'
        })
      }
    }, 50)
  })
}

async function toggleMute() {
  if (!currentChannel.value) return
  const convId = currentChannel.value.conversationId
  if (isMuted.value) {
    await messagingStore.unmuteConversation(convId)
  } else {
    await messagingStore.muteConversation(convId)
  }
  isMuted.value = !isMuted.value
}

function isOwnMessage(senderId: string) {
  return authStore.user?.id === senderId
}

function formatTime(dateStr: string) {
  return localeFormatTime(dateStr, { hour: '2-digit', minute: '2-digit' })
}

function formatDateLabel(dateStr: string) {
  const date = new Date(dateStr)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const msgDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diff = today.getTime() - msgDay.getTime()
  const dayMs = 86400000

  if (diff === 0) return t('chat.today')
  if (diff === dayMs) return t('chat.yesterday')
  return formatShortDate(dateStr)
}

function getInitials(name: string) {
  return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase()
}

function getInitialColor(name: string) {
  const colors = [
    '#2E7D32', '#1565C0', '#6A1B9A', '#C62828',
    '#EF6C00', '#00838F', '#4E342E', '#37474F',
    '#AD1457', '#283593', '#00695C', '#9E9D24'
  ]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

// Image handling
function onImageSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowed.includes(file.type) || file.size > 10 * 1024 * 1024) return

  selectedImage.value = file
  imagePreviewUrl.value = URL.createObjectURL(file)
}

function clearImage() {
  if (imagePreviewUrl.value) URL.revokeObjectURL(imagePreviewUrl.value)
  selectedImage.value = null
  imagePreviewUrl.value = null
}

function triggerImageSelect() {
  const input = document.getElementById('room-chat-image-input') as HTMLInputElement
  input?.click()
}

function openFullImage(imageId: string) {
  fullSizeImageUrl.value = messagingApi.imageUrl(imageId)
  showFullImage.value = true
}

function setReplyTo(msg: MessageInfo) {
  messagingStore.setReplyTo(msg)
}

function clearReply() {
  messagingStore.setReplyTo(null)
}

function getMessagePreview(msg: MessageInfo) {
  if (msg.content) return msg.content
  if (msg.images?.length) return '\uD83D\uDDBC ' + t('chat.image')
  return ''
}
</script>

<template>
  <div class="rc-container">
    <!-- Header -->
    <div class="rc-header">
      <div class="rc-header-left">
        <SelectButton
          v-if="channelOptions.length > 1"
          v-model="activeChannel"
          :options="channelOptions"
          optionLabel="label"
          optionValue="value"
        />
        <span v-else class="rc-channel-label">
          <i class="pi pi-comments" />
          {{ t('chat.title') }}
        </span>
      </div>
      <div class="rc-header-actions">
        <span v-if="isMuted" class="rc-muted-badge">
          <i class="pi pi-volume-off" />
          {{ t('chat.muted') }}
        </span>
        <Button
          :icon="isMuted ? 'pi pi-volume-off' : 'pi pi-volume-up'"
          text
          :severity="isMuted ? 'warn' : 'secondary'"
          size="small"
          :aria-label="isMuted ? t('chat.unmute') : t('chat.mute')"
          @click="toggleMute"
        />
      </div>
    </div>

    <!-- Messages Area -->
    <div ref="messagesContainer" class="rc-messages">
      <!-- Empty State -->
      <div v-if="messagingStore.messages.length === 0" class="rc-empty">
        <div class="rc-empty-icon">
          <i class="pi pi-comments" />
        </div>
        <p class="rc-empty-text">{{ t('chat.noMessages') }}</p>
      </div>

      <!-- Messages with date separators -->
      <template v-for="(item, index) in groupedMessages" :key="index">
        <div v-if="item.type === 'separator'" class="rc-date-separator">
          <span>{{ item.label }}</span>
        </div>

        <div
          v-else-if="item.type === 'message'"
          class="rc-message"
          :class="{ 'rc-message--own': isOwnMessage(item.msg.senderId) }"
        >
          <!-- Sender avatar for other people's messages -->
          <div
            v-if="!isOwnMessage(item.msg.senderId)"
            class="rc-avatar"
            :style="{ background: getInitialColor(item.msg.senderName) }"
          >
            {{ getInitials(item.msg.senderName) }}
          </div>

          <div class="rc-bubble">
            <span v-if="!isOwnMessage(item.msg.senderId)" class="rc-sender">
              {{ item.msg.senderName }}
            </span>

            <!-- Reply reference -->
            <div v-if="item.msg.replyTo" class="rc-reply-ref">
              <span class="rc-reply-ref-sender">{{ item.msg.replyTo.senderName }}</span>
              <span class="rc-reply-ref-text">
                <i v-if="item.msg.replyTo.hasImage" class="pi pi-image rc-reply-img-icon" />
                {{ item.msg.replyTo.contentPreview || (item.msg.replyTo.hasImage ? t('chat.image') : '') }}
              </span>
            </div>

            <!-- Image -->
            <div v-if="item.msg.images?.length" class="rc-image-wrap">
              <img
                v-for="img in item.msg.images"
                :key="img.imageId"
                :src="messagingApi.thumbnailUrl(img.imageId)"
                :alt="img.originalFilename"
                class="rc-msg-image"
                @click="openFullImage(img.imageId)"
              />
            </div>

            <p v-if="item.msg.content" class="rc-content">{{ item.msg.content }}</p>

            <div class="rc-meta">
              <span class="rc-time">{{ formatTime(item.msg.createdAt) }}</span>
              <button
                class="rc-reply-btn"
                :title="t('chat.replyTo')"
                @click.stop="setReplyTo(item.msg)"
              >
                <i class="pi pi-reply" />
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Reply preview bar -->
    <div v-if="messagingStore.replyToMessage" class="rc-reply-bar">
      <div class="rc-reply-bar-content">
        <span class="rc-reply-bar-sender">{{ messagingStore.replyToMessage.senderName }}</span>
        <span class="rc-reply-bar-text">{{ getMessagePreview(messagingStore.replyToMessage) }}</span>
      </div>
      <Button
        icon="pi pi-times"
        text
        severity="secondary"
        size="small"
        :aria-label="t('common.close')"
        @click="clearReply"
      />
    </div>

    <!-- Image preview bar -->
    <div v-if="imagePreviewUrl" class="rc-image-bar">
      <img :src="imagePreviewUrl" class="rc-image-bar-thumb" alt="Preview" />
      <span class="rc-image-bar-name">{{ selectedImage?.name }}</span>
      <Button
        icon="pi pi-times"
        text
        severity="secondary"
        size="small"
        :aria-label="t('common.close')"
        @click="clearImage"
      />
    </div>

    <!-- Input -->
    <div class="rc-input-area">
      <input
        id="room-chat-image-input"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        class="rc-hidden-input"
        @change="onImageSelect"
      />
      <Button
        icon="pi pi-camera"
        text
        severity="secondary"
        size="small"
        :aria-label="t('chat.attachImage')"
        @click="triggerImageSelect"
      />
      <Textarea
        v-model="messageText"
        :placeholder="t('chat.placeholder')"
        :autoResize="true"
        rows="1"
        class="rc-textarea"
        @keydown.enter.exact.prevent="sendMessage"
      />
      <Button
        icon="pi pi-send"
        :disabled="!canSend"
        size="small"
        class="rc-send-btn"
        @click="sendMessage"
      />
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
        class="rc-full-image"
        alt="Full size"
      />
    </Dialog>
  </div>
</template>

<style scoped>
.rc-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 420px;
  background: var(--mw-bg);
  border-radius: var(--mw-border-radius);
  overflow: hidden;
}

/* ── Header ──────────────────────────────── */
.rc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 1rem;
  background: var(--mw-bg-card);
  border-bottom: 1px solid var(--mw-border-light);
  gap: 0.5rem;
}

.rc-header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.rc-channel-label {
  font-size: var(--mw-font-size-sm);
  font-weight: 600;
  color: var(--mw-text);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.rc-channel-label i {
  color: var(--mw-primary);
}

.rc-header-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.rc-muted-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  background: var(--mw-bg-hover);
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
}

.rc-muted-badge i {
  font-size: 0.625rem;
}

/* ── Messages ────────────────────────────── */
.rc-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  scroll-behavior: smooth;
}

/* ── Empty state ─────────────────────────── */
.rc-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem;
}

.rc-empty-icon {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 50%;
  background: var(--mw-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
}

.rc-empty-icon i {
  font-size: 1.25rem;
  color: var(--mw-text-muted);
}

.rc-empty-text {
  font-size: var(--mw-font-size-sm);
  color: var(--mw-text-muted);
  text-align: center;
}

/* ── Date Separator ──────────────────────── */
.rc-date-separator {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

.rc-date-separator::before,
.rc-date-separator::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--mw-border-light);
}

.rc-date-separator span {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--mw-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

/* ── Message row ─────────────────────────── */
.rc-message {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
  max-width: 85%;
  animation: rc-fade-in 0.2s ease-out;
}

.rc-message--own {
  margin-left: auto;
  flex-direction: row-reverse;
}

@keyframes rc-fade-in {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ── Sender avatar ───────────────────────── */
.rc-avatar {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  color: white;
  font-size: 0.5625rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  letter-spacing: 0.02em;
}

/* ── Bubble ──────────────────────────────── */
.rc-bubble {
  padding: 0.5rem 0.75rem;
  border-radius: var(--mw-border-radius-lg) var(--mw-border-radius-lg) var(--mw-border-radius-lg) var(--mw-border-radius-sm);
  background: var(--mw-bg-card);
  border: 1px solid var(--mw-border-light);
  position: relative;
  min-width: 4rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.15s;
}

.rc-bubble:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.rc-message--own .rc-bubble {
  background: var(--mw-primary);
  color: white;
  border-color: transparent;
  border-radius: var(--mw-border-radius-lg) var(--mw-border-radius-lg) var(--mw-border-radius-sm) var(--mw-border-radius-lg);
}

.rc-sender {
  display: block;
  font-size: 0.6875rem;
  font-weight: 700;
  margin-bottom: 0.125rem;
  color: var(--mw-primary);
}

.rc-content {
  font-size: var(--mw-font-size-sm);
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

/* ── Reply reference ─────────────────────── */
.rc-reply-ref {
  background: rgba(0, 0, 0, 0.06);
  border-left: 3px solid var(--mw-primary);
  border-radius: 0 var(--mw-border-radius-sm) var(--mw-border-radius-sm) 0;
  padding: 0.25rem 0.5rem;
  margin-bottom: 0.375rem;
  font-size: var(--mw-font-size-xs);
  display: flex;
  flex-direction: column;
  gap: 0.0625rem;
}

.rc-message--own .rc-reply-ref {
  background: rgba(255, 255, 255, 0.15);
  border-left-color: rgba(255, 255, 255, 0.6);
}

.rc-reply-ref-sender {
  font-weight: 600;
}

.rc-reply-ref-text {
  opacity: 0.8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.rc-reply-img-icon {
  font-size: 0.6875rem;
  margin-right: 0.25rem;
}

/* ── Message images ──────────────────────── */
.rc-image-wrap {
  margin: 0.25rem 0;
}

.rc-msg-image {
  max-width: 100%;
  max-height: 220px;
  border-radius: var(--mw-border-radius);
  cursor: pointer;
  display: block;
  transition: opacity 0.15s;
}

.rc-msg-image:hover {
  opacity: 0.9;
}

/* ── Message meta (time + reply btn) ─────── */
.rc-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.25rem;
  gap: 0.5rem;
}

.rc-time {
  font-size: 0.625rem;
  opacity: 0.55;
}

.rc-reply-btn {
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

.rc-bubble:hover .rc-reply-btn {
  opacity: 0.5;
}

.rc-reply-btn:hover {
  opacity: 1 !important;
}

/* ── Reply preview bar ───────────────────── */
.rc-reply-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--mw-bg-card);
  border-top: 1px solid var(--mw-border-light);
  border-left: 3px solid var(--mw-primary);
}

.rc-reply-bar-content {
  flex: 1;
  min-width: 0;
  font-size: var(--mw-font-size-xs);
}

.rc-reply-bar-sender {
  font-weight: 600;
  margin-right: 0.5rem;
}

.rc-reply-bar-text {
  opacity: 0.7;
}

/* ── Image preview bar ───────────────────── */
.rc-image-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-top: 1px solid var(--mw-border-light);
  background: var(--mw-bg-card);
}

.rc-image-bar-thumb {
  width: 2.25rem;
  height: 2.25rem;
  object-fit: cover;
  border-radius: var(--mw-border-radius-sm);
}

.rc-image-bar-name {
  flex: 1;
  min-width: 0;
  font-size: var(--mw-font-size-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--mw-text-secondary);
}

.rc-hidden-input {
  display: none;
}

/* ── Input area ──────────────────────────── */
.rc-input-area {
  display: flex;
  align-items: flex-end;
  gap: 0.375rem;
  padding: 0.625rem 0.75rem;
  border-top: 1px solid var(--mw-border-light);
  background: var(--mw-bg-card);
}

.rc-textarea {
  flex: 1;
  max-height: 6rem;
}

.rc-send-btn {
  flex-shrink: 0;
}

/* ── Full-size image ─────────────────────── */
.rc-full-image {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
}

/* ── Mobile adjustments ──────────────────── */
@media (max-width: 767px) {
  .rc-message {
    max-width: 90%;
  }

  .rc-muted-badge {
    display: none;
  }
}
</style>
