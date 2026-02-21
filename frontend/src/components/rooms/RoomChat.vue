<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoomsStore } from '@/stores/rooms'
import { useMessagingStore } from '@/stores/messaging'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import { useLocaleDate } from '@/composables/useLocaleDate'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import SelectButton from 'primevue/selectbutton'
import type { ChannelType } from '@/types/room'

const props = defineProps<{ roomId: string }>()

const { t } = useI18n()
const { formatTime: localeFormatTime } = useLocaleDate()
const roomsStore = useRoomsStore()
const messagingStore = useMessagingStore()
const authStore = useAuthStore()

const activeChannel = ref<ChannelType>('MAIN')
const messageText = ref('')
const messagesContainer = ref<HTMLElement>()

const channelOptions = computed(() => {
  return roomsStore.chatChannels.map(ch => ({
    label: t('chat.channels.' + ch.channelType),
    value: ch.channelType
  }))
})

const currentChannel = computed(() => {
  return roomsStore.chatChannels.find(ch => ch.channelType === activeChannel.value)
})

onMounted(async () => {
  await roomsStore.fetchChatChannels(props.roomId)
  if (roomsStore.chatChannels.length === 0) {
    // Create main channel on first access
    await roomsStore.getOrCreateChatChannel(props.roomId, 'MAIN')
    await roomsStore.fetchChatChannels(props.roomId)
  }
  if (currentChannel.value) {
    await messagingStore.fetchMessages(currentChannel.value.conversationId)
    scrollToBottom()
  }
})

// Auto-scroll when new messages arrive via WebSocket
watch(() => messagingStore.messages.length, () => {
  scrollToBottom()
})

watch(activeChannel, async () => {
  if (currentChannel.value) {
    await messagingStore.fetchMessages(currentChannel.value.conversationId)
  }
})

async function sendMessage() {
  if (!messageText.value.trim() || !currentChannel.value) return
  await messagingStore.sendMessage(currentChannel.value.conversationId, messageText.value)
  messageText.value = ''
  scrollToBottom()
}

function scrollToBottom() {
  setTimeout(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }, 50)
}

function isOwnMessage(senderId: string) {
  return authStore.user?.id === senderId
}

function formatTime(dateStr: string) {
  return localeFormatTime(dateStr, { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="flex flex-col h-full" style="min-height: 400px">
    <!-- Channel Selector -->
    <div v-if="channelOptions.length > 1" class="p-2 border-b">
      <SelectButton v-model="activeChannel" :options="channelOptions"
                    optionLabel="label" optionValue="value" />
    </div>

    <!-- Messages -->
    <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-3">
      <div v-if="messagingStore.messages.length === 0" class="text-center text-gray-400 py-8">
        {{ t('chat.noMessages') }}
      </div>

      <div v-for="msg in messagingStore.messages" :key="msg.id"
           class="flex" :class="isOwnMessage(msg.senderId) ? 'justify-end' : 'justify-start'">
        <div class="max-w-xs lg:max-w-md rounded-lg px-3 py-2"
             :class="isOwnMessage(msg.senderId)
               ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-800'">
          <div v-if="!isOwnMessage(msg.senderId)" class="text-xs font-semibold mb-1 opacity-75">
            {{ msg.senderName }}
          </div>
          <p class="text-sm whitespace-pre-wrap">{{ msg.content }}</p>
          <div class="text-xs mt-1 opacity-60 text-right">
            {{ formatTime(msg.createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Input -->
    <div class="p-3 border-t flex gap-2">
      <InputText v-model="messageText" :placeholder="t('chat.placeholder')"
                 class="flex-1" @keyup.enter="sendMessage" />
      <Button icon="pi pi-send" @click="sendMessage" :disabled="!messageText.trim()" />
    </div>
  </div>
</template>
