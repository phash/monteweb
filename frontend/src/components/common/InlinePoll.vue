<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'

interface PollOption {
  id: string
  label: string
  voteCount: number
  userVoted: boolean
}

interface Poll {
  id: string
  question: string
  multiple: boolean
  closed: boolean
  totalVotes: number
  options: PollOption[]
  closesAt: string | null
}

const props = defineProps<{
  poll: Poll
  authorId?: string
}>()

const emit = defineEmits<{
  vote: [optionIds: string[]]
  close: []
}>()

const { t } = useI18n()
const auth = useAuthStore()

const selectedOptions = ref<Set<string>>(new Set())
const hasVoted = computed(() => props.poll.options.some(o => o.userVoted))
const showResults = computed(() => hasVoted.value || props.poll.closed)
const canClose = computed(() =>
  !props.poll.closed && (props.authorId === auth.user?.id || auth.isAdmin)
)

function toggleOption(optionId: string) {
  if (showResults.value) return

  if (props.poll.multiple) {
    if (selectedOptions.value.has(optionId)) {
      selectedOptions.value.delete(optionId)
    } else {
      selectedOptions.value.add(optionId)
    }
  } else {
    // Single choice — vote immediately
    emit('vote', [optionId])
  }
}

function submitMultiple() {
  if (selectedOptions.value.size === 0) return
  emit('vote', Array.from(selectedOptions.value))
}

function getPercentage(option: PollOption) {
  if (props.poll.totalVotes === 0) return 0
  return Math.round((option.voteCount / props.poll.totalVotes) * 100)
}
</script>

<template>
  <div class="inline-poll">
    <div class="poll-question">
      <i class="pi pi-chart-bar poll-icon" />
      <strong>{{ poll.question }}</strong>
    </div>

    <div class="poll-options">
      <div
        v-for="option in poll.options"
        :key="option.id"
        class="poll-option"
        :class="{
          'voted': option.userVoted,
          'selectable': !showResults,
          'selected': selectedOptions.has(option.id)
        }"
        role="button"
        :tabindex="showResults ? -1 : 0"
        @click="toggleOption(option.id)"
        @keydown.enter="toggleOption(option.id)"
      >
        <template v-if="showResults">
          <div class="option-result">
            <div class="option-label-row">
              <span class="option-label">
                <i v-if="option.userVoted" class="pi pi-check voted-check" />
                {{ option.label }}
              </span>
              <span class="option-count">{{ option.voteCount }}</span>
            </div>
            <ProgressBar
              :value="getPercentage(option)"
              :showValue="false"
              class="option-bar"
              :pt="{ value: { class: option.userVoted ? 'voted-bar' : '' } }"
            />
          </div>
        </template>
        <template v-else>
          <span v-if="poll.multiple" class="option-checkbox">
            <i :class="selectedOptions.has(option.id) ? 'pi pi-check-square' : 'pi pi-stop'" />
          </span>
          <span class="option-label">{{ option.label }}</span>
        </template>
      </div>
    </div>

    <div v-if="poll.multiple && !showResults && selectedOptions.size > 0" class="poll-submit">
      <Button
        :label="t('poll.vote')"
        size="small"
        @click="submitMultiple"
      />
    </div>

    <div class="poll-footer">
      <span class="poll-total">{{ t('poll.totalVotes', { count: poll.totalVotes }) }}</span>
      <span v-if="poll.closed" class="poll-closed">{{ t('poll.closed') }}</span>
      <Button
        v-if="canClose"
        :label="t('poll.closePoll')"
        text
        size="small"
        severity="secondary"
        @click="emit('close')"
      />
    </div>
  </div>
</template>

<style scoped>
.inline-poll {
  background: var(--mw-bg-hover);
  border-radius: var(--mw-border-radius);
  padding: 0.75rem;
  margin: 0.5rem 0;
}

.poll-question {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  font-size: var(--mw-font-size-sm);
}

.poll-icon {
  color: var(--mw-primary);
  margin-top: 0.125rem;
}

.poll-options {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.poll-option {
  border: 1px solid var(--mw-border-light);
  border-radius: var(--mw-border-radius-sm);
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg);
  transition: all 0.15s;
}

.poll-option.selectable {
  cursor: pointer;
}

.poll-option.selectable:hover {
  border-color: var(--mw-primary);
  background: rgba(59, 130, 246, 0.04);
}

.poll-option.selected {
  border-color: var(--mw-primary);
  background: rgba(59, 130, 246, 0.08);
}

.poll-option.voted {
  border-color: var(--mw-primary);
}

.option-checkbox {
  margin-right: 0.5rem;
  color: var(--mw-text-muted);
}

.selected .option-checkbox {
  color: var(--mw-primary);
}

.option-result {
  width: 100%;
}

.option-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.25rem;
  font-size: var(--mw-font-size-sm);
}

.option-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.voted-check {
  color: var(--mw-primary);
  font-size: 0.75rem;
}

.option-count {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.option-bar {
  height: 0.375rem;
}

:deep(.voted-bar) {
  background: var(--mw-primary) !important;
}

.poll-submit {
  margin-top: 0.5rem;
  display: flex;
  justify-content: flex-end;
}

.poll-footer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.poll-closed {
  color: var(--mw-danger, #ef4444);
  font-weight: 600;
}
</style>
