<script setup lang="ts">
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { usersApi } from '@/api/users.api'
import type { UserInfo } from '@/types/user'
import Textarea from 'primevue/textarea'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  rows?: number
  autoResize?: boolean
}>(), {
  placeholder: '',
  rows: 3,
  autoResize: true,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'keydown': [event: KeyboardEvent]
}>()

const { t } = useI18n()

const textareaRef = ref<InstanceType<typeof Textarea> | null>(null)
const showDropdown = ref(false)
const dropdownPosition = ref({ top: 0, left: 0 })
const searchResults = ref<UserInfo[]>([])
const selectedIndex = ref(0)
const mentionQuery = ref('')
const mentionStartPos = ref(-1)
const searching = ref(false)
let searchTimeout: ReturnType<typeof setTimeout> | null = null

function getTextareaEl(): HTMLTextAreaElement | null {
  if (!textareaRef.value) return null
  // PrimeVue Textarea wraps a native textarea
  const el = (textareaRef.value as unknown as { $el: HTMLElement }).$el
  if (el instanceof HTMLTextAreaElement) return el
  return el.querySelector('textarea')
}

function onInput(event: Event) {
  const textarea = event.target as HTMLTextAreaElement
  emit('update:modelValue', textarea.value)
  checkForMention(textarea)
}

function checkForMention(textarea: HTMLTextAreaElement) {
  const cursorPos = textarea.selectionStart
  const text = textarea.value
  const textBefore = text.substring(0, cursorPos)

  // Find the last @ that could be a mention trigger
  const atIndex = textBefore.lastIndexOf('@')

  if (atIndex === -1) {
    closeDropdown()
    return
  }

  // The @ must be at the start or preceded by a space/newline
  if (atIndex > 0 && !/\s/.test(textBefore.charAt(atIndex - 1))) {
    closeDropdown()
    return
  }

  // Check that between @ and cursor there's no space break that would end the mention
  const query = textBefore.substring(atIndex + 1)

  // If query contains a ] it means the mention was already completed
  if (query.includes(']')) {
    closeDropdown()
    return
  }

  // If query starts with [ it's already a formatted mention in progress
  if (query.startsWith('[')) {
    closeDropdown()
    return
  }

  mentionStartPos.value = atIndex
  mentionQuery.value = query

  if (query.length >= 1) {
    debouncedSearch(query)
  } else {
    closeDropdown()
  }
}

function debouncedSearch(query: string) {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    performSearch(query)
  }, 250)
}

async function performSearch(query: string) {
  if (query.length < 1) {
    closeDropdown()
    return
  }

  searching.value = true
  try {
    const res = await usersApi.search(query, 0, 8)
    const results = res.data.data?.content ?? []
    searchResults.value = results
    selectedIndex.value = 0

    if (results.length > 0) {
      showDropdown.value = true
      positionDropdown()
    } else {
      showDropdown.value = true
      positionDropdown()
    }
  } catch {
    closeDropdown()
  } finally {
    searching.value = false
  }
}

function positionDropdown() {
  const textarea = getTextareaEl()
  if (!textarea) return

  const rect = textarea.getBoundingClientRect()
  // Position below the textarea
  dropdownPosition.value = {
    top: rect.bottom + window.scrollY + 4,
    left: rect.left + window.scrollX,
  }
}

function selectUser(user: UserInfo) {
  const textarea = getTextareaEl()
  if (!textarea) return

  const text = props.modelValue
  const before = text.substring(0, mentionStartPos.value)
  const after = text.substring(textarea.selectionStart)
  const mentionText = `@[${user.id}:${user.displayName}] `
  const newValue = before + mentionText + after

  emit('update:modelValue', newValue)
  closeDropdown()

  // Restore cursor position after the inserted mention
  nextTick(() => {
    const newCursorPos = before.length + mentionText.length
    textarea.focus()
    textarea.setSelectionRange(newCursorPos, newCursorPos)
  })
}

function closeDropdown() {
  showDropdown.value = false
  searchResults.value = []
  selectedIndex.value = 0
  mentionQuery.value = ''
  mentionStartPos.value = -1
}

function onKeydown(event: KeyboardEvent) {
  if (showDropdown.value && searchResults.value.length > 0) {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, searchResults.value.length - 1)
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
      return
    }
    if (event.key === 'Enter' || event.key === 'Tab') {
      event.preventDefault()
      event.stopPropagation()
      const user = searchResults.value[selectedIndex.value]
      if (user) selectUser(user)
      return
    }
    if (event.key === 'Escape') {
      event.preventDefault()
      closeDropdown()
      return
    }
  }

  // Forward the event for other handlers (e.g. enter to send)
  emit('keydown', event)
}

function onClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.mention-dropdown')) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
  if (searchTimeout) clearTimeout(searchTimeout)
})

function getInitials(name: string) {
  return name
    .split(' ')
    .map((w) => w[0])
    .join('')
    .substring(0, 2)
    .toUpperCase()
}
</script>

<template>
  <div class="mention-input-wrapper">
    <Textarea
      ref="textareaRef"
      :modelValue="modelValue"
      :placeholder="placeholder"
      :autoResize="autoResize"
      :rows="rows"
      @input="onInput"
      @keydown="onKeydown"
    />
    <Teleport to="body">
      <div
        v-if="showDropdown"
        class="mention-dropdown"
        :style="{ top: dropdownPosition.top + 'px', left: dropdownPosition.left + 'px' }"
      >
        <div v-if="searchResults.length === 0" class="mention-no-results">
          {{ t('mentions.noResults') }}
        </div>
        <div
          v-for="(user, index) in searchResults"
          :key="user.id"
          class="mention-item"
          :class="{ 'mention-item--selected': index === selectedIndex }"
          @mousedown.prevent="selectUser(user)"
          @mouseenter="selectedIndex = index"
        >
          <div class="mention-avatar">{{ getInitials(user.displayName) }}</div>
          <div class="mention-user-info">
            <span class="mention-user-name">{{ user.displayName }}</span>
            <span class="mention-user-role">{{ user.role }}</span>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.mention-input-wrapper {
  position: relative;
  width: 100%;
}

.mention-input-wrapper :deep(textarea) {
  width: 100%;
}
</style>

<style>
.mention-dropdown {
  position: absolute;
  z-index: 1100;
  background: var(--mw-bg-card, #fff);
  border: 1px solid var(--mw-border-light, #e5e7eb);
  border-radius: var(--mw-border-radius, 8px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  min-width: 240px;
  max-width: 360px;
  max-height: 240px;
  overflow-y: auto;
}

.mention-no-results {
  padding: 0.75rem 1rem;
  font-size: var(--mw-font-size-sm, 0.875rem);
  color: var(--mw-text-muted, #9ca3af);
  text-align: center;
}

.mention-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  transition: background 0.1s;
}

.mention-item:hover,
.mention-item--selected {
  background: var(--mw-bg-hover, #f3f4f6);
}

.mention-avatar {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  background: var(--mw-primary, #3b82f6);
  color: white;
  font-size: 0.625rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.mention-user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.mention-user-name {
  font-size: var(--mw-font-size-sm, 0.875rem);
  font-weight: 500;
  color: var(--mw-text, #111827);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mention-user-role {
  font-size: var(--mw-font-size-xs, 0.75rem);
  color: var(--mw-text-muted, #9ca3af);
}
</style>
