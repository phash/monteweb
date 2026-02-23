<script setup lang="ts">
import { ref } from 'vue'

export interface ReactionData {
  emoji: string
  count: number
  userReacted: boolean
}

const EMOJIS = ['👍', '👎', '❤️', '😂', '😢']

const props = defineProps<{
  reactions: ReactionData[]
  compact?: boolean
}>()

const emit = defineEmits<{
  react: [emoji: string]
}>()

const showPicker = ref(false)

function getReaction(emoji: string): ReactionData | undefined {
  return props.reactions.find(r => r.emoji === emoji)
}

function toggleReaction(emoji: string) {
  showPicker.value = false
  emit('react', emoji)
}
</script>

<template>
  <div class="reaction-bar" :class="{ compact }">
    <button
      v-for="r in reactions.filter(r => r.count > 0)"
      :key="r.emoji"
      class="reaction-chip"
      :class="{ active: r.userReacted }"
      @click="toggleReaction(r.emoji)"
      :title="r.emoji"
    >
      <span class="reaction-emoji">{{ r.emoji }}</span>
      <span class="reaction-count">{{ r.count }}</span>
    </button>

    <div class="reaction-add-wrapper">
      <button class="reaction-add" @click="showPicker = !showPicker" title="Reaktion hinzufügen">
        <i class="pi pi-face-smile" />
      </button>
      <div v-if="showPicker" class="reaction-picker" @mouseleave="showPicker = false">
        <button
          v-for="emoji in EMOJIS"
          :key="emoji"
          class="picker-emoji"
          :class="{ active: getReaction(emoji)?.userReacted }"
          @click="toggleReaction(emoji)"
        >
          {{ emoji }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reaction-bar {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.reaction-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.15rem 0.5rem;
  border: 1px solid var(--mw-border-light, #e0e0e0);
  border-radius: 1rem;
  background: var(--mw-bg, #fff);
  cursor: pointer;
  font-size: 0.8rem;
  line-height: 1.4;
  transition: all 0.15s;
}

.reaction-chip:hover {
  border-color: var(--mw-primary, #4caf50);
  background: var(--mw-bg-highlight, #f5f5f5);
}

.reaction-chip.active {
  border-color: var(--mw-primary, #4caf50);
  background: var(--mw-primary-light, #e8f5e9);
}

.reaction-emoji {
  font-size: 0.95rem;
  line-height: 1;
}

.reaction-count {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--mw-text-secondary, #666);
}

.reaction-chip.active .reaction-count {
  color: var(--mw-primary, #4caf50);
}

.reaction-add-wrapper {
  position: relative;
}

.reaction-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: 1px dashed var(--mw-border-light, #e0e0e0);
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  color: var(--mw-text-muted, #999);
  font-size: 0.8rem;
  transition: all 0.15s;
}

.reaction-add:hover {
  border-color: var(--mw-primary, #4caf50);
  color: var(--mw-primary, #4caf50);
  background: var(--mw-bg-highlight, #f5f5f5);
}

.reaction-picker {
  position: absolute;
  bottom: calc(100% + 0.35rem);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 0.25rem;
  padding: 0.35rem;
  background: var(--mw-bg, #fff);
  border: 1px solid var(--mw-border-light, #e0e0e0);
  border-radius: 1.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  z-index: 10;
  white-space: nowrap;
}

.picker-emoji {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  font-size: 1.2rem;
  transition: all 0.12s;
}

.picker-emoji:hover {
  background: var(--mw-bg-highlight, #f0f0f0);
  transform: scale(1.2);
}

.picker-emoji.active {
  background: var(--mw-primary-light, #e8f5e9);
}

.compact .reaction-chip {
  padding: 0.1rem 0.35rem;
  font-size: 0.75rem;
}

.compact .reaction-emoji {
  font-size: 0.85rem;
}

.compact .reaction-add {
  width: 1.5rem;
  height: 1.5rem;
  font-size: 0.7rem;
}
</style>
