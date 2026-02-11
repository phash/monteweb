import { defineStore } from 'pinia'
import { ref } from 'vue'
import { discussionsApi } from '@/api/discussions.api'
import type { DiscussionThread, DiscussionReply } from '@/types/discussion'

export const useDiscussionsStore = defineStore('discussions', () => {
  const threads = ref<DiscussionThread[]>([])
  const currentThread = ref<DiscussionThread | null>(null)
  const replies = ref<DiscussionReply[]>([])
  const loading = ref(false)
  const totalThreads = ref(0)

  async function fetchThreads(roomId: string, status?: string, page = 0) {
    loading.value = true
    try {
      const res = await discussionsApi.getThreads(roomId, status, page)
      threads.value = res.data.data.content
      totalThreads.value = res.data.data.totalElements
    } finally {
      loading.value = false
    }
  }

  async function fetchThread(roomId: string, threadId: string) {
    const res = await discussionsApi.getThread(roomId, threadId)
    currentThread.value = res.data.data
  }

  async function createThread(roomId: string, title: string, content?: string) {
    const res = await discussionsApi.createThread(roomId, title, content)
    threads.value.unshift(res.data.data)
    return res.data.data
  }

  async function archiveThread(roomId: string, threadId: string) {
    const res = await discussionsApi.archiveThread(roomId, threadId)
    const idx = threads.value.findIndex(t => t.id === threadId)
    if (idx !== -1) threads.value[idx] = res.data.data
    if (currentThread.value?.id === threadId) currentThread.value = res.data.data
  }

  async function deleteThread(roomId: string, threadId: string) {
    await discussionsApi.deleteThread(roomId, threadId)
    threads.value = threads.value.filter(t => t.id !== threadId)
    if (currentThread.value?.id === threadId) currentThread.value = null
  }

  async function fetchReplies(roomId: string, threadId: string, page = 0) {
    loading.value = true
    try {
      const res = await discussionsApi.getReplies(roomId, threadId, page)
      replies.value = res.data.data.content
    } finally {
      loading.value = false
    }
  }

  async function addReply(roomId: string, threadId: string, content: string) {
    const res = await discussionsApi.addReply(roomId, threadId, content)
    replies.value.push(res.data.data)
    // Update reply count on thread
    const thread = threads.value.find(t => t.id === threadId)
    if (thread) thread.replyCount++
    if (currentThread.value?.id === threadId) currentThread.value.replyCount++
    return res.data.data
  }

  return {
    threads,
    currentThread,
    replies,
    loading,
    totalThreads,
    fetchThreads,
    fetchThread,
    createThread,
    archiveThread,
    deleteThread,
    fetchReplies,
    addReply,
  }
})
