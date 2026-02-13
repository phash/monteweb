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
    try {
      const res = await feedApi.getFeed(page.value)
      const data = res.data.data
      posts.value = reset ? data.content : [...posts.value, ...data.content]
      hasMore.value = !data.last
      page.value++
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
    const res = await feedApi.createPost(data)
    posts.value.unshift(res.data.data)
    return res.data.data
  }

  async function deletePost(id: string) {
    await feedApi.deletePost(id)
    posts.value = posts.value.filter(p => p.id !== id)
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
    const res = await feedApi.addComment(postId, { content })
    if (!commentsByPost.value[postId]) {
      commentsByPost.value[postId] = []
    }
    commentsByPost.value[postId].push(res.data.data)
    const post = posts.value.find(p => p.id === postId)
    if (post) post.commentCount++
  }

  return {
    posts,
    banners,
    currentPost,
    commentsByPost,
    loading,
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
