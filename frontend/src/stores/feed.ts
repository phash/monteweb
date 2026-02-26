import { defineStore } from 'pinia'
import { ref } from 'vue'
import { feedApi } from '@/api/feed.api'
import type { FeedPost, FeedComment, SystemBanner, CreatePostRequest } from '@/types/feed'

export const useFeedStore = defineStore('feed', () => {
  const posts = ref<FeedPost[]>([])
  const banners = ref<SystemBanner[]>([])
  const currentPost = ref<FeedPost | null>(null)
  const commentsByPost = ref<Record<string, FeedComment[]>>({})
  const loading = ref(false)
  const error = ref<string | null>(null)
  const hasMore = ref(true)
  const page = ref(0)

  async function fetchFeed(reset = false) {
    if (reset) {
      page.value = 0
      hasMore.value = true
      posts.value = []
    }
    if (!hasMore.value) return

    loading.value = true
    error.value = null
    try {
      const res = await feedApi.getFeed(page.value)
      const data = res.data.data
      posts.value = reset ? data.content : [...posts.value, ...data.content]
      hasMore.value = !data.last
      page.value++
    } catch (e: any) {
      error.value = e?.response?.data?.message || 'Failed to load feed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchBanners() {
    try {
      const res = await feedApi.getBanners()
      banners.value = res.data.data
    } catch {
      banners.value = []
    }
  }

  async function createPost(data: CreatePostRequest) {
    error.value = null
    try {
      const res = await feedApi.createPost(data)
      posts.value.unshift(res.data.data)
      return res.data.data
    } catch (e: any) {
      error.value = e?.response?.data?.message || 'Failed to create post'
      throw e
    }
  }

  async function deletePost(id: string) {
    error.value = null
    try {
      await feedApi.deletePost(id)
      posts.value = posts.value.filter(p => p.id !== id)
    } catch (e: any) {
      error.value = e?.response?.data?.message || 'Failed to delete post'
      throw e
    }
  }

  async function pinPost(id: string) {
    await feedApi.pinPost(id)
    const post = posts.value.find(p => p.id === id)
    if (post) post.pinned = !post.pinned
  }

  async function fetchComments(postId: string) {
    const res = await feedApi.getComments(postId)
    commentsByPost.value[postId] = res.data.data.content
  }

  async function addComment(postId: string, content: string) {
    error.value = null
    try {
      const res = await feedApi.addComment(postId, { content })
      if (!commentsByPost.value[postId]) {
        commentsByPost.value[postId] = []
      }
      commentsByPost.value[postId].push(res.data.data)
      const post = posts.value.find(p => p.id === postId)
      if (post) post.commentCount++
    } catch (e: any) {
      error.value = e?.response?.data?.message || 'Failed to add comment'
      throw e
    }
  }

  return {
    posts,
    banners,
    currentPost,
    commentsByPost,
    loading,
    error,
    hasMore,
    fetchFeed,
    fetchBanners,
    createPost,
    deletePost,
    pinPost,
    fetchComments,
    addComment,
  }
})
