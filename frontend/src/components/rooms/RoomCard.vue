<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RoomInfo } from '@/types/room'
import Tag from 'primevue/tag'

const { t } = useI18n()

defineProps<{
  room: RoomInfo
}>()

function typeColor(type: string): string {
  const map: Record<string, string> = {
    KLASSE: 'success',
    GRUPPE: 'info',
    PROJEKT: 'warn',
    INTEREST: 'secondary',
    CUSTOM: 'contrast',
  }
  return (map[type] ?? 'secondary') as string
}
</script>

<template>
  <router-link :to="{ name: 'room-detail', params: { id: room.id } }" class="room-card">
    <div class="room-card-header">
      <div class="room-card-title">
        <div class="room-card-avatar">
          <img v-if="room.avatarUrl" :src="room.avatarUrl" alt="" class="room-card-avatar-img" />
          <i v-else class="pi pi-home" />
        </div>
        <h3 class="room-name">{{ room.name }}</h3>
      </div>
      <Tag :value="t(`rooms.types.${room.type}`)" :severity="typeColor(room.type) as any" />
    </div>
    <p v-if="room.description" class="room-desc">{{ room.description }}</p>
    <div class="room-meta">
      <span class="room-members">
        <i class="pi pi-users" />
        {{ room.memberCount }} {{ t('rooms.members') }}
      </span>
    </div>
  </router-link>
</template>

<style scoped>
.room-card {
  display: block;
  background: var(--mw-bg-card);
  border-radius: var(--mw-border-radius);
  box-shadow: var(--mw-shadow);
  padding: 1.25rem;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.15s, transform 0.15s;
}

.room-card:hover {
  box-shadow: var(--mw-shadow-md);
  transform: translateY(-1px);
  text-decoration: none;
}

.room-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.room-card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.room-card-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mw-bg);
  color: var(--mw-text-muted);
  flex-shrink: 0;
  font-size: 0.875rem;
}

.room-card-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.room-name {
  font-size: var(--mw-font-size-md);
  font-weight: 600;
}

.room-desc {
  color: var(--mw-text-secondary);
  font-size: var(--mw-font-size-sm);
  margin-bottom: 0.75rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.room-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.room-members {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
