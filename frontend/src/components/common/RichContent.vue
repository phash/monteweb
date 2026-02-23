<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { calendarApi } from '@/api/calendar.api'
import { jobboardApi } from '@/api/jobboard.api'
import { roomsApi } from '@/api/rooms.api'

const props = defineProps<{ content: string }>()
const { t } = useI18n()
const router = useRouter()

interface ResolvedLink {
  url: string
  type: 'event' | 'job' | 'room'
  label: string
  icon: string
  route: string
}

const resolvedLinks = ref<Map<string, ResolvedLink>>(new Map())

// Pattern to detect internal URLs with UUIDs
const UUID_PATTERN = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
const URL_REGEX = new RegExp(
  `(https?://[^\\s]+/(calendar/events|jobs|rooms)/(${UUID_PATTERN}))`,
  'gi'
)

// Also match relative paths like /calendar/events/UUID
const RELATIVE_URL_REGEX = new RegExp(
  `(?:^|\\s)(/(calendar/events|jobs|rooms)/(${UUID_PATTERN}))`,
  'gi'
)

function resolveType(pathType: string): 'event' | 'job' | 'room' {
  if (pathType === 'calendar/events') return 'event'
  if (pathType === 'jobs') return 'job'
  return 'room'
}

const linkUrls = computed(() => {
  const urls: { url: string; type: 'event' | 'job' | 'room'; id: string }[] = []
  const content = props.content

  let match: RegExpExecArray | null
  const fullRegex = new RegExp(URL_REGEX.source, 'gi')
  while ((match = fullRegex.exec(content)) !== null) {
    const urlStr = match[1] ?? ''
    const pathType = match[2] ?? ''
    const id = match[3] ?? ''
    urls.push({ url: urlStr, type: resolveType(pathType), id })
  }

  const relRegex = new RegExp(RELATIVE_URL_REGEX.source, 'gi')
  while ((match = relRegex.exec(content)) !== null) {
    const urlStr = (match[1] ?? '').trim()
    const pathType = match[2] ?? ''
    const id = match[3] ?? ''
    if (!urls.some(u => u.id === id)) {
      urls.push({ url: urlStr, type: resolveType(pathType), id })
    }
  }

  return urls
})

function formatEventDate(event: { startDate: string; startTime?: string | null; endTime?: string | null; allDay: boolean }): string {
  const date = new Date(event.startDate + 'T00:00:00')
  const dayName = date.toLocaleDateString('de-DE', { weekday: 'long' })
  const dateStr = date.toLocaleDateString('de-DE', { day: 'numeric', month: 'long', year: 'numeric' })
  let result = `${dayName}, ${dateStr}`
  if (!event.allDay && event.startTime && event.endTime) {
    result += ` ${event.startTime.substring(0, 5)} - ${event.endTime.substring(0, 5)}`
  }
  return result
}

async function resolveLinks() {
  for (const link of linkUrls.value) {
    try {
      if (link.type === 'event') {
        const res = await calendarApi.getEvent(link.id)
        const event = res.data.data
        const scopeName = event.scopeName ? ` ${event.scopeName}` : ''
        resolvedLinks.value.set(link.url, {
          url: link.url,
          type: 'event',
          label: `${event.title} ${t('common.am', 'am')} ${formatEventDate(event)}${scopeName}`,
          icon: 'pi pi-calendar',
          route: `/calendar/events/${link.id}`,
        })
      } else if (link.type === 'job') {
        const res = await jobboardApi.getJob(link.id)
        const job = res.data.data
        const dateStr = job.scheduledDate
          ? new Date(job.scheduledDate + 'T00:00:00').toLocaleDateString('de-DE', { day: 'numeric', month: 'long', year: 'numeric' })
          : ''
        resolvedLinks.value.set(link.url, {
          url: link.url,
          type: 'job',
          label: `${job.title}${dateStr ? ` (${dateStr})` : ''}`,
          icon: 'pi pi-briefcase',
          route: `/jobs/${link.id}`,
        })
      } else if (link.type === 'room') {
        const res = await roomsApi.getById(link.id)
        const room = res.data.data
        resolvedLinks.value.set(link.url, {
          url: link.url,
          type: 'room',
          label: room.name,
          icon: 'pi pi-home',
          route: `/rooms/${link.id}`,
        })
      }
    } catch {
      // Link resolution failed — will render as plain text
    }
  }
}

// Mention pattern: @[uuid:Display Name]
const MENTION_REGEX_SRC = '@\\[([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}):([^\\]]+)\\]'

interface ContentSegment {
  type: 'text' | 'link' | 'mention'
  value: string
  mentionName?: string
  mentionId?: string
}

// Split content into text segments, link segments, and mention segments
const segments = computed((): ContentSegment[] => {
  const content = props.content
  if (!content) return []

  // First pass: split by mentions
  const mentionSegments: ContentSegment[] = []
  let lastIndex = 0
  const mentionRegex = new RegExp(MENTION_REGEX_SRC, 'gi')
  let mentionMatch
  while ((mentionMatch = mentionRegex.exec(content)) !== null) {
    if (mentionMatch.index > lastIndex) {
      mentionSegments.push({ type: 'text', value: content.slice(lastIndex, mentionMatch.index) })
    }
    mentionSegments.push({
      type: 'mention',
      value: mentionMatch[0],
      mentionId: mentionMatch[1],
      mentionName: mentionMatch[2],
    })
    lastIndex = mentionMatch.index + mentionMatch[0].length
  }
  if (lastIndex < content.length) {
    mentionSegments.push({ type: 'text', value: content.slice(lastIndex) })
  }

  // If no links, return with mentions already parsed
  if (linkUrls.value.length === 0) return mentionSegments

  // Second pass: split text segments by link URLs
  const allUrls = linkUrls.value.map(l => l.url).sort((a, b) => b.length - a.length)
  const escaped = allUrls.map(u => u.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
  const combinedRegex = new RegExp(`(${escaped.join('|')})`, 'g')

  const result: ContentSegment[] = []
  for (const seg of mentionSegments) {
    if (seg.type !== 'text') {
      result.push(seg)
      continue
    }

    let segLastIndex = 0
    let match
    const regex = new RegExp(combinedRegex.source, 'g')
    while ((match = regex.exec(seg.value)) !== null) {
      if (match.index > segLastIndex) {
        result.push({ type: 'text', value: seg.value.slice(segLastIndex, match.index) })
      }
      result.push({ type: 'link', value: match[1] ?? match[0] })
      segLastIndex = match.index + match[0].length
    }
    if (segLastIndex < seg.value.length) {
      result.push({ type: 'text', value: seg.value.slice(segLastIndex) })
    }
  }

  return result
})

function navigate(route: string) {
  router.push(route)
}

onMounted(() => {
  if (linkUrls.value.length > 0) {
    resolveLinks()
  }
})
</script>

<template>
  <span class="rich-content">
    <template v-for="(seg, i) in segments" :key="i">
      <span v-if="seg.type === 'text'" class="text-segment">{{ seg.value }}</span>
      <span
        v-else-if="seg.type === 'mention'"
        class="mention-tag"
        :title="seg.mentionName"
      >@{{ seg.mentionName }}</span>
      <a
        v-else-if="seg.type === 'link' && resolvedLinks.has(seg.value)"
        class="rich-link"
        :class="resolvedLinks.get(seg.value)!.type"
        href="#"
        @click.prevent="navigate(resolvedLinks.get(seg.value)!.route)"
      >
        <i :class="resolvedLinks.get(seg.value)!.icon" />
        {{ resolvedLinks.get(seg.value)!.label }}
      </a>
      <span v-else class="text-segment">{{ seg.value }}</span>
    </template>
  </span>
</template>

<style scoped>
.rich-content {
  white-space: pre-wrap;
}

.text-segment {
  white-space: pre-wrap;
}

.rich-link {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  text-decoration: none;
  font-size: 0.9em;
  font-weight: 500;
  transition: background 0.15s;
}

.rich-link.event {
  background: var(--p-blue-50, #eff6ff);
  color: var(--p-blue-700, #1d4ed8);
  border: 1px solid var(--p-blue-200, #bfdbfe);
}
.rich-link.event:hover {
  background: var(--p-blue-100, #dbeafe);
}

.rich-link.job {
  background: var(--p-green-50, #f0fdf4);
  color: var(--p-green-700, #15803d);
  border: 1px solid var(--p-green-200, #bbf7d0);
}
.rich-link.job:hover {
  background: var(--p-green-100, #dcfce7);
}

.rich-link.room {
  background: var(--p-purple-50, #faf5ff);
  color: var(--p-purple-700, #7e22ce);
  border: 1px solid var(--p-purple-200, #e9d5ff);
}
.rich-link.room:hover {
  background: var(--p-purple-100, #f3e8ff);
}

.rich-link i {
  font-size: 0.85em;
}

.mention-tag {
  display: inline;
  color: var(--mw-primary, #3b82f6);
  font-weight: 600;
  cursor: default;
}
</style>
