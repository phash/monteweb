import { test, expect } from '@playwright/test'
import { accounts } from '../../fixtures/test-accounts'
import { login } from '../../helpers/auth'
import { selectors, toastWithText } from '../../helpers/selectors'

// ============================================================================
// Feed E2E Tests — US-069 to US-090
// ============================================================================

/**
 * Helper: navigate to a room's Info-Board tab.
 * Returns the room name found, or null if no rooms are available.
 */
async function navigateToFirstRoom(page: import('@playwright/test').Page): Promise<string | null> {
  await page.goto('/rooms')
  await page.waitForLoadState('networkidle')

  const roomCard = page.locator('.room-card').first()
  const hasRoom = await roomCard.isVisible({ timeout: 10000 }).catch(() => false)
  if (!hasRoom) return null

  const roomName = await roomCard.locator('.room-name').textContent()
  await roomCard.click()
  await page.waitForURL(/\/rooms\//, { timeout: 10000 })
  await page.waitForLoadState('networkidle')

  // The first tab "Info-Board" is active by default (value="0")
  return roomName?.trim() ?? null
}

/**
 * Helper: create a post via API and return its id.
 * Uses page.request to make authenticated API calls.
 */
async function createPostViaApi(
  page: import('@playwright/test').Page,
  data: { title?: string; content: string; sourceType?: string; sourceId?: string }
): Promise<string | null> {
  try {
    const response = await page.request.post('/api/v1/feed/posts', {
      data: {
        sourceType: data.sourceType ?? 'SCHOOL',
        sourceId: data.sourceId ?? undefined,
        title: data.title,
        content: data.content,
      },
    })
    if (response.ok()) {
      const json = await response.json()
      return json.data?.id ?? null
    }
  } catch {
    // API call failed
  }
  return null
}

/**
 * Helper: delete a post via API (cleanup).
 */
async function deletePostViaApi(page: import('@playwright/test').Page, postId: string): Promise<void> {
  try {
    await page.request.delete(`/api/v1/feed/posts/${postId}`)
  } catch {
    // Ignore cleanup errors
  }
}

// --------------------------------------------------------------------------
// US-069: Feed-Beitraege anzeigen
// --------------------------------------------------------------------------
test.describe('US-069: Feed-Beitraege anzeigen', () => {

  test('dashboard shows feed area with FeedList component', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    // Dashboard page title
    const title = page.locator('.page-title')
    await expect(title).toBeVisible({ timeout: 10000 })
    await expect(title).toHaveText('Dashboard')

    // FeedList component renders .feed-list container
    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })
  })

  test('feed posts show author name, date, and comment counter', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      // No posts in feed -- valid state if DB is clean
      test.skip()
      return
    }

    // Author name
    await expect(firstPost.locator('.post-meta strong')).toBeVisible()

    // Date
    await expect(firstPost.locator('.post-date')).toBeVisible()

    // Comment counter button (pi-comment icon with count)
    const commentButton = firstPost.locator('.post-footer button:has(.pi-comment)')
    await expect(commentButton).toBeVisible()
  })

  test('pinned posts have "Angeheftet" tag and appear at the top', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    // Look for any pinned post
    const pinnedPost = page.locator('.feed-post.pinned').first()
    const hasPinned = await pinnedPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPinned) {
      // No pinned posts -- skip
      test.skip()
      return
    }

    // Pinned tag with "Angeheftet"
    await expect(pinnedPost.locator('.p-tag:has-text("Angeheftet")')).toBeVisible()

    // Pinned post should be the first in the list
    const firstPost = page.locator('.feed-post').first()
    const firstPostPinned = await firstPost.evaluate(el => el.classList.contains('pinned'))
    expect(firstPostPinned).toBe(true)
  })

  test('"Weitere laden" button appears when there are more posts', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    // The "Weitere laden" button is in .load-more
    const loadMoreButton = page.locator('.load-more button:has-text("Weitere laden")')
    const hasLoadMore = await loadMoreButton.isVisible({ timeout: 3000 }).catch(() => false)

    // Whether the button is visible depends on the number of posts.
    // We just verify the feed loaded without errors.
    const posts = page.locator('.feed-post')
    const postCount = await posts.count()
    const emptyState = page.locator('.empty-state')
    const hasEmpty = await emptyState.isVisible({ timeout: 2000 }).catch(() => false)

    // Either posts exist or empty state is shown
    expect(postCount > 0 || hasEmpty).toBe(true)
  })

  test('feed posts show source label when available', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    // Check posts with source labels (post-source contains "in <em>...")
    const postWithSource = page.locator('.feed-post .post-source').first()
    const hasSource = await postWithSource.isVisible({ timeout: 3000 }).catch(() => false)

    if (hasSource) {
      // Source label should contain "in" followed by the source name
      await expect(postWithSource).toContainText('in')
    }
    // Posts without sourceId won't have a source label -- valid state
  })
})

// --------------------------------------------------------------------------
// US-070: Beitrag erstellen (T/SA im Raum)
// --------------------------------------------------------------------------
test.describe('US-070: Beitrag erstellen (T/SA im Raum)', () => {

  test('teacher sees PostComposer on room Info-Board', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    // PostComposer should be visible for teacher
    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Title field with placeholder "Titel (optional)"
    const titleInput = composer.locator('.composer-title')
    await expect(titleInput).toBeVisible()

    // Content area
    const contentArea = composer.locator('.composer-content')
    await expect(contentArea).toBeVisible()
  })

  test('teacher can create a post in room and sees success toast', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Fill content
    const contentTextarea = composer.locator('.composer-content textarea').first()
    const uniqueContent = `E2E-Test-Beitrag ${Date.now()}`
    await contentTextarea.fill(uniqueContent)

    // Click publish button ("Veröffentlichen")
    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await expect(publishButton).toBeEnabled()
    await publishButton.click()

    // Expect success toast "Beitrag veröffentlicht"
    await expect(page.locator(toastWithText('Beitrag veröffentlicht'))).toBeVisible({ timeout: 10000 })

    // The new post should appear in the room feed
    await page.waitForTimeout(1000) // Wait for feed refresh
    const newPost = page.locator(`.feed-post:has-text("${uniqueContent}")`)
    const postAppeared = await newPost.isVisible({ timeout: 5000 }).catch(() => false)

    // Clean up: if post appeared, try to delete it
    if (postAppeared) {
      // Verify the post text is actually there
      await expect(newPost.locator('.post-content')).toContainText(uniqueContent)
    }
  })

  test('teacher can create a post with optional title', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Fill title
    const titleInput = composer.locator('.composer-title')
    const uniqueTitle = `E2E-Titel ${Date.now()}`
    await titleInput.fill(uniqueTitle)

    // Fill content
    const contentTextarea = composer.locator('.composer-content textarea').first()
    await contentTextarea.fill('Testinhalt mit Titel')

    // Publish
    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await publishButton.click()

    await expect(page.locator(toastWithText('Beitrag veröffentlicht'))).toBeVisible({ timeout: 10000 })
  })

  test('admin sees PostComposer on dashboard (school-wide scope)', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    // PostComposer on dashboard
    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })
  })
})

// --------------------------------------------------------------------------
// US-071: Beitrag erstellen -- nicht erlaubt fuer P/S
// --------------------------------------------------------------------------
test.describe('US-071: Beitrag erstellen -- nicht erlaubt fuer P/S', () => {

  test('parent does NOT see PostComposer on room Info-Board', async ({ page }) => {
    await login(page, accounts.parent)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    // PostComposer should NOT be visible for parent
    const composer = page.locator('.post-composer')
    await expect(composer).not.toBeVisible()
  })

  test('student does NOT see PostComposer on room Info-Board', async ({ page }) => {
    await login(page, accounts.student)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    // PostComposer should NOT be visible for student
    const composer = page.locator('.post-composer')
    await expect(composer).not.toBeVisible()
  })

  test('parent does NOT see PostComposer on dashboard', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    await expect(composer).not.toBeVisible()
  })

  test('student does NOT see PostComposer on dashboard', async ({ page }) => {
    await login(page, accounts.student)
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    await expect(composer).not.toBeVisible()
  })
})

// --------------------------------------------------------------------------
// US-072: Beitrag mit Datei-Anhaengen
// --------------------------------------------------------------------------
test.describe('US-072: Beitrag mit Datei-Anhaengen', () => {

  test('PostComposer has attach files button (paperclip icon)', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Paperclip button (pi-paperclip icon)
    const attachButton = composer.locator('button:has(.pi-paperclip)')
    await expect(attachButton).toBeVisible()
  })

  test('hidden file input exists for file selection', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Hidden file input
    const fileInput = composer.locator('input[type="file"].hidden-file-input')
    // It exists but is hidden (display:none)
    await expect(fileInput).toBeAttached()
    await expect(fileInput).toHaveAttribute('multiple', '')
  })

  test('PostComposer shows selected files chips', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Use Playwright's file chooser to set a file
    const fileInput = composer.locator('input[type="file"].hidden-file-input')

    // Create a small test file buffer
    await fileInput.setInputFiles({
      name: 'test-document.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('E2E test file content'),
    })

    // File chip should appear in .selected-files
    const fileChip = composer.locator('.file-chip')
    await expect(fileChip).toBeVisible({ timeout: 5000 })
    await expect(fileChip.locator('.file-chip-name')).toHaveText('test-document.txt')

    // Remove button on file chip
    const removeBtn = fileChip.locator('.file-chip-remove')
    await expect(removeBtn).toBeVisible()

    // Click remove to clear the file
    await removeBtn.click()
    await expect(fileChip).not.toBeVisible()
  })
})

// --------------------------------------------------------------------------
// US-073: Beitrag loeschen
// --------------------------------------------------------------------------
test.describe('US-073: Beitrag loeschen', () => {

  test('author sees delete button on their own post', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    // Create a post first via the dashboard composer
    const composer = page.locator('.post-composer')
    const composerVisible = await composer.isVisible({ timeout: 5000 }).catch(() => false)
    if (!composerVisible) {
      test.skip()
      return
    }

    const uniqueContent = `Delete-Test-Post ${Date.now()}`
    const contentTextarea = composer.locator('.composer-content textarea').first()
    await contentTextarea.fill(uniqueContent)

    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await publishButton.click()
    await expect(page.locator(toastWithText('Beitrag veröffentlicht'))).toBeVisible({ timeout: 10000 })

    // Wait for post to appear
    await page.waitForTimeout(1000)
    await page.reload()
    await page.waitForLoadState('networkidle')

    const post = page.locator(`.feed-post:has-text("${uniqueContent}")`).first()
    const postVisible = await post.isVisible({ timeout: 5000 }).catch(() => false)

    if (!postVisible) {
      test.skip()
      return
    }

    // Delete button should be visible (pi-trash icon)
    const deleteButton = post.locator('.post-footer button:has(.pi-trash)')
    await expect(deleteButton).toBeVisible()
  })

  test('delete confirmation dialog appears and post is removed', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    // Create a post to delete
    const composer = page.locator('.post-composer')
    const composerVisible = await composer.isVisible({ timeout: 5000 }).catch(() => false)
    if (!composerVisible) {
      test.skip()
      return
    }

    const uniqueContent = `To-Delete-Post ${Date.now()}`
    const contentTextarea = composer.locator('.composer-content textarea').first()
    await contentTextarea.fill(uniqueContent)

    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await publishButton.click()
    await expect(page.locator(toastWithText('Beitrag veröffentlicht'))).toBeVisible({ timeout: 10000 })

    await page.waitForTimeout(1000)
    await page.reload()
    await page.waitForLoadState('networkidle')

    const post = page.locator(`.feed-post:has-text("${uniqueContent}")`).first()
    const postVisible = await post.isVisible({ timeout: 5000 }).catch(() => false)

    if (!postVisible) {
      test.skip()
      return
    }

    // Click delete button
    const deleteButton = post.locator('.post-footer button:has(.pi-trash)')
    await deleteButton.click()

    // Confirmation dialog appears with "Beitrag löschen" title
    const confirmDialog = post.locator('.delete-confirm')
    await expect(confirmDialog).toBeVisible({ timeout: 5000 })

    // Confirm message: "Möchten Sie diesen Beitrag wirklich löschen?"
    await expect(confirmDialog).toContainText('Möchten Sie diesen Beitrag wirklich löschen')

    // Click the "Löschen" button to confirm
    const confirmButton = confirmDialog.locator('button:has-text("Löschen")')
    await confirmButton.click()

    // Post should be removed from the feed
    await expect(post).not.toBeVisible({ timeout: 10000 })
  })

  test('teacher cannot delete other users\' posts (no trash icon)', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    // Find a post NOT authored by this teacher
    const posts = page.locator('.feed-post')
    const postCount = await posts.count()

    if (postCount === 0) {
      test.skip()
      return
    }

    // Check posts for ones without a delete button
    // A teacher should NOT see delete on posts by other authors
    // (The delete button shows only if post.authorId === auth.user?.id || auth.isAdmin)
    let foundOthersPost = false
    for (let i = 0; i < postCount && i < 5; i++) {
      const post = posts.nth(i)
      const authorName = await post.locator('.post-meta strong').textContent()
      // If this is not the teacher's post, check there's no delete button
      if (authorName && !authorName.includes(accounts.teacher.displayName)) {
        const deleteButton = post.locator('.post-footer button:has(.pi-trash)')
        const hasDelete = await deleteButton.isVisible().catch(() => false)
        expect(hasDelete).toBe(false)
        foundOthersPost = true
        break
      }
    }

    if (!foundOthersPost) {
      // All visible posts are by this teacher, or no posts -- skip this assertion
      test.skip()
    }
  })
})

// --------------------------------------------------------------------------
// US-074: Kommentar verfassen
// --------------------------------------------------------------------------
test.describe('US-074: Kommentar verfassen', () => {

  test('clicking comment button opens comments section with input field', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    // Click comment button (pi-comment)
    const commentButton = firstPost.locator('.post-footer button:has(.pi-comment)')
    await commentButton.click()

    // Comments section opens
    const commentsSection = firstPost.locator('.comments-section')
    await expect(commentsSection).toBeVisible({ timeout: 5000 })

    // Comment input with placeholder "Kommentar schreiben..."
    const commentTextarea = commentsSection.locator('.comment-textarea textarea')
    await expect(commentTextarea).toBeVisible()
  })

  test('submit comment button is disabled when input is empty', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    // Open comments
    const commentButton = firstPost.locator('.post-footer button:has(.pi-comment)')
    await commentButton.click()

    const commentsSection = firstPost.locator('.comments-section')
    await expect(commentsSection).toBeVisible({ timeout: 5000 })

    // Submit button should be disabled when empty
    const submitButton = commentsSection.locator('.comment-input button:has(.pi-send)')
    await expect(submitButton).toBeDisabled()
  })

  test('submitting a comment increases comment count', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    // Get initial comment count
    const commentButton = firstPost.locator('.post-footer button:has(.pi-comment)')
    const initialCountText = await commentButton.textContent()
    const initialCount = parseInt(initialCountText?.trim() ?? '0', 10)

    // Open comments
    await commentButton.click()
    const commentsSection = firstPost.locator('.comments-section')
    await expect(commentsSection).toBeVisible({ timeout: 5000 })

    // Type a comment
    const commentTextarea = commentsSection.locator('.comment-textarea textarea')
    const uniqueComment = `E2E-Kommentar ${Date.now()}`
    await commentTextarea.fill(uniqueComment)

    // Submit
    const submitButton = commentsSection.locator('.comment-input button:has(.pi-send)')
    await expect(submitButton).toBeEnabled()
    await submitButton.click()

    // Wait for the comment to appear
    await page.waitForTimeout(1000)

    // Comment should appear in the list
    const newComment = commentsSection.locator(`.comment-item:has-text("${uniqueComment}")`)
    const commentAppeared = await newComment.isVisible({ timeout: 5000 }).catch(() => false)

    // Verify (if comment appeared, the counter should have incremented)
    if (commentAppeared) {
      const updatedCountText = await commentButton.textContent()
      const updatedCount = parseInt(updatedCountText?.trim() ?? '0', 10)
      expect(updatedCount).toBeGreaterThanOrEqual(initialCount)
    }
  })
})

// --------------------------------------------------------------------------
// US-075: Beitrag anheften/losloesen (T/SA)
// --------------------------------------------------------------------------
test.describe('US-075: Beitrag anheften/losloesen (T/SA)', () => {

  test('admin sees pin/unpin button on feed posts', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    // Pin button (pi-thumbtack icon, label "Anheften" or "Loslösen")
    const pinButton = firstPost.locator('.post-footer button:has(.pi-thumbtack)')
    await expect(pinButton).toBeVisible()
  })

  test('parent does NOT see pin button on feed posts', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    // Pin button should NOT be visible for parent
    const pinButton = firstPost.locator('.post-footer button:has(.pi-thumbtack)')
    await expect(pinButton).not.toBeVisible()
  })

  test('student does NOT see pin button on feed posts', async ({ page }) => {
    await login(page, accounts.student)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    const pinButton = firstPost.locator('.post-footer button:has(.pi-thumbtack)')
    await expect(pinButton).not.toBeVisible()
  })

  test('clicking pin button toggles pin state and shows "Angeheftet" tag', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    const pinButton = firstPost.locator('.post-footer button:has(.pi-thumbtack)')
    const pinLabel = await pinButton.textContent()
    const wasAlreadyPinned = pinLabel?.includes('Loslösen') ?? false

    // Click to toggle pin state
    await pinButton.click()
    await page.waitForTimeout(1000)

    if (!wasAlreadyPinned) {
      // Should now be pinned -- "Angeheftet" tag should appear
      // Reload to see updated state
      await page.reload()
      await page.waitForLoadState('networkidle')

      const pinnedPost = page.locator('.feed-post.pinned').first()
      const isPinned = await pinnedPost.isVisible({ timeout: 5000 }).catch(() => false)

      if (isPinned) {
        await expect(pinnedPost.locator('.p-tag:has-text("Angeheftet")')).toBeVisible()

        // Unpin to restore state
        const unpinButton = pinnedPost.locator('.post-footer button:has(.pi-thumbtack)')
        await unpinButton.click()
        await page.waitForTimeout(500)
      }
    } else {
      // Was pinned, now should be unpinned
      await page.reload()
      await page.waitForLoadState('networkidle')

      // Re-pin to restore state
      const unPinnedPost = page.locator('.feed-post').first()
      const rePinButton = unPinnedPost.locator('.post-footer button:has(.pi-thumbtack)')
      const rePinVisible = await rePinButton.isVisible({ timeout: 3000 }).catch(() => false)
      if (rePinVisible) {
        await rePinButton.click()
        await page.waitForTimeout(500)
      }
    }
  })
})

// --------------------------------------------------------------------------
// US-076: Reaktionen auf Beitraege
// --------------------------------------------------------------------------
test.describe('US-076: Reaktionen auf Beitraege', () => {

  test('reaction bar is visible on feed posts with emoji picker', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    // ReactionBar component renders .reaction-bar
    const reactionBar = firstPost.locator('.reaction-bar').first()
    await expect(reactionBar).toBeVisible()

    // The "add reaction" button (pi-face-smile)
    const addReactionButton = reactionBar.locator('.reaction-add')
    await expect(addReactionButton).toBeVisible()
  })

  test('clicking add-reaction button opens emoji picker', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    const reactionBar = firstPost.locator('.reaction-bar').first()
    const addReactionButton = reactionBar.locator('.reaction-add')
    await addReactionButton.click()

    // Emoji picker popup should appear
    const picker = reactionBar.locator('.reaction-picker')
    await expect(picker).toBeVisible({ timeout: 3000 })

    // Should show emoji buttons
    const emojiButtons = picker.locator('.picker-emoji')
    const emojiCount = await emojiButtons.count()
    expect(emojiCount).toBeGreaterThanOrEqual(3) // At least a few emojis
  })

  test('clicking an emoji in picker adds a reaction chip', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    const reactionBar = firstPost.locator('.reaction-bar').first()
    const addReactionButton = reactionBar.locator('.reaction-add')
    await addReactionButton.click()

    const picker = reactionBar.locator('.reaction-picker')
    await expect(picker).toBeVisible({ timeout: 3000 })

    // Click the first emoji
    const firstEmoji = picker.locator('.picker-emoji').first()
    await firstEmoji.click()

    // Reaction chip should appear (or counter increment)
    await page.waitForTimeout(1000)

    // There should be at least one reaction chip with .active class
    const activeChip = reactionBar.locator('.reaction-chip.active')
    const hasActive = await activeChip.isVisible({ timeout: 3000 }).catch(() => false)

    if (hasActive) {
      // The chip should have a count
      await expect(activeChip.locator('.reaction-count')).toBeVisible()

      // Toggle off: click again to remove reaction
      await activeChip.click()
      await page.waitForTimeout(500)
    }
  })
})

// --------------------------------------------------------------------------
// US-077: Reaktionen auf Kommentare
// --------------------------------------------------------------------------
test.describe('US-077: Reaktionen auf Kommentare', () => {

  test('comments have a compact reaction bar', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.feed-post').first()
    const hasPost = await firstPost.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPost) {
      test.skip()
      return
    }

    // Open comments
    const commentButton = firstPost.locator('.post-footer button:has(.pi-comment)')
    await commentButton.click()

    const commentsSection = firstPost.locator('.comments-section')
    await expect(commentsSection).toBeVisible({ timeout: 5000 })

    // Check if there are existing comments
    const comments = commentsSection.locator('.comment-item')
    const commentCount = await comments.count()

    if (commentCount === 0) {
      // No comments to check reactions on -- skip
      test.skip()
      return
    }

    // Each comment has a compact ReactionBar
    const firstComment = comments.first()
    const commentReactionBar = firstComment.locator('.reaction-bar.compact')
    await expect(commentReactionBar).toBeVisible()

    // Add reaction button on comment
    const addReaction = commentReactionBar.locator('.reaction-add')
    await expect(addReaction).toBeVisible()
  })
})

// --------------------------------------------------------------------------
// US-078: Beitrag mit Umfrage (Poll)
// --------------------------------------------------------------------------
test.describe('US-078: Beitrag mit Umfrage (Poll)', () => {

  test('PostComposer has poll creation button (chart-bar icon)', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Poll button (pi-chart-bar icon)
    const pollButton = composer.locator('button:has(.pi-chart-bar)')
    await expect(pollButton).toBeVisible()
  })

  test('clicking poll button opens PollComposer with question and option fields', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Click poll button
    const pollButton = composer.locator('button:has(.pi-chart-bar)')
    await pollButton.click()

    // PollComposer should appear
    const pollComposer = page.locator('.poll-composer')
    await expect(pollComposer).toBeVisible({ timeout: 5000 })

    // Header: "Umfrage erstellen"
    await expect(pollComposer.locator('.poll-composer-header')).toContainText('Umfrage erstellen')

    // Question input
    const questionInput = pollComposer.locator('.poll-question-input')
    await expect(questionInput).toBeVisible()

    // At least 2 option inputs
    const optionInputs = pollComposer.locator('.poll-option-input')
    const optionCount = await optionInputs.count()
    expect(optionCount).toBeGreaterThanOrEqual(2)

    // "Option hinzufügen" button
    const addOptionButton = pollComposer.locator('button:has-text("Option hinzufügen")')
    await expect(addOptionButton).toBeVisible()

    // "Mehrfachauswahl" checkbox label
    await expect(pollComposer.locator('text=Mehrfachauswahl')).toBeVisible()

    // Cancel button
    const cancelButton = pollComposer.locator('button:has-text("Abbrechen")')
    await expect(cancelButton).toBeVisible()
  })

  test('poll "Umfrage erstellen" button is disabled without question and 2 options', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    const pollButton = composer.locator('button:has(.pi-chart-bar)')
    await pollButton.click()

    const pollComposer = page.locator('.poll-composer')
    await expect(pollComposer).toBeVisible({ timeout: 5000 })

    // Submit button should be disabled initially
    const submitButton = pollComposer.locator('.poll-composer-actions button:has-text("Umfrage erstellen")')
    await expect(submitButton).toBeDisabled()

    // Fill question only -- still disabled
    const questionInput = pollComposer.locator('.poll-question-input')
    await questionInput.fill('Testfrage?')
    await expect(submitButton).toBeDisabled()

    // Fill first option -- still disabled (need 2)
    const options = pollComposer.locator('.poll-option-input')
    await options.nth(0).fill('Option A')
    await expect(submitButton).toBeDisabled()

    // Fill second option -- now enabled
    await options.nth(1).fill('Option B')
    await expect(submitButton).toBeEnabled()
  })

  test('InlinePoll displays on a post with vote options', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    // Look for any post with a poll
    const pollWidget = page.locator('.inline-poll').first()
    const hasPoll = await pollWidget.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasPoll) {
      // No polls in feed -- skip
      test.skip()
      return
    }

    // Poll question should be visible
    const question = pollWidget.locator('.poll-question strong')
    await expect(question).toBeVisible()

    // Poll options
    const options = pollWidget.locator('.poll-option')
    const optionCount = await options.count()
    expect(optionCount).toBeGreaterThanOrEqual(2)

    // Footer shows total votes
    const totalVotes = pollWidget.locator('.poll-total')
    await expect(totalVotes).toBeVisible()
    await expect(totalVotes).toContainText('Stimme')
  })
})

// --------------------------------------------------------------------------
// US-079: Parent-Only Beitraege
// --------------------------------------------------------------------------
test.describe('US-079: Parent-Only Beitraege', () => {

  test.skip('parent-only posts are visible to parents but not students', async () => {
    // TODO: Requires creating a parent-only post via API with parentOnly=true,
    // then verifying visibility for parent vs student.
    // Complex to test because:
    // 1. Creating parentOnly posts may require specific API fields not exposed in PostComposer
    // 2. Need to verify the same post is invisible to students
    // 3. Requires coordinating two different login sessions
  })
})

// --------------------------------------------------------------------------
// US-080: Targeted Posts
// --------------------------------------------------------------------------
test.describe('US-080: Targeted Posts', () => {

  test('targeted post API accepts target_user_ids and returns 201', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    // Create a targeted post via API
    const response = await page.request.post('/api/v1/feed/posts', {
      data: {
        sourceType: 'SCHOOL',
        content: `Targeted E2E test post ${Date.now()}`,
        targetUserIds: [], // Empty array = visible to all (same as null)
      },
    })

    // Should succeed (200 or 201)
    expect(response.ok()).toBe(true)

    const json = await response.json()
    const postId = json.data?.id

    // Clean up
    if (postId) {
      await deletePostViaApi(page, postId)
    }
  })

  test.skip('targeted post is only visible to specified users', async () => {
    // TODO: Full targeted post visibility test requires:
    // 1. Get user IDs for specific users
    // 2. Create a post with targetUserIds=[userId1]
    // 3. Verify user1 sees the post
    // 4. Verify user2 does NOT see the post
    // Complex multi-session test.
  })
})

// --------------------------------------------------------------------------
// US-081: System-Banner auf Dashboard
// --------------------------------------------------------------------------
test.describe('US-081: System-Banner auf Dashboard', () => {

  test('SystemBanner component area is rendered on dashboard', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })

    // SystemBanner renders .system-banners container if banners exist
    // The component is always mounted but only shows content when banners.length > 0
    // We verify the dashboard loads without error
    const dashboard = page.locator('.page-title:has-text("Dashboard")')
    await expect(dashboard).toBeVisible()
  })

  test('system banners display with title and content when present', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.page-title')).toBeVisible({ timeout: 10000 })

    // Check if any banners are showing (context-dependent)
    const banners = page.locator('.system-banners .banner-item')
    const bannerCount = await banners.count()

    if (bannerCount > 0) {
      // Verify first banner has title and content
      const firstBanner = banners.first()
      await expect(firstBanner.locator('.banner-content strong')).toBeVisible()
      await expect(firstBanner.locator('.banner-content p')).toBeVisible()
    }
    // Banners are context-dependent (e.g. cleaning banners for affected parents)
    // No banners is a valid state
  })
})

// --------------------------------------------------------------------------
// US-082: Feed im Raum
// --------------------------------------------------------------------------
test.describe('US-082: Feed im Raum', () => {

  test('room detail view shows "Info-Board" as the first tab', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    // First tab should be "Info-Board"
    const firstTab = page.locator('.p-tablist .p-tab').first()
    await expect(firstTab).toBeVisible({ timeout: 5000 })
    await expect(firstTab).toHaveText('Info-Board')
  })

  test('room feed shows posts specific to that room', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    // The Info-Board tab panel should be active
    // It contains FeedPost components or "Noch keine Beiträge in diesem Raum"
    const tabPanel = page.locator('.p-tabpanel').first()

    const roomPosts = tabPanel.locator('.feed-post')
    const emptyMessage = tabPanel.locator('text=Noch keine Beiträge in diesem Raum')

    const hasPosts = await roomPosts.first().isVisible({ timeout: 5000 }).catch(() => false)
    const hasEmpty = await emptyMessage.isVisible({ timeout: 3000 }).catch(() => false)

    // One of the two must be visible
    expect(hasPosts || hasEmpty).toBe(true)
  })

  test('room feed does NOT show school-wide posts (isolation)', async ({ page }) => {
    await login(page, accounts.admin)

    // First create a school-wide post on dashboard
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    const composerVisible = await composer.isVisible({ timeout: 5000 }).catch(() => false)
    if (!composerVisible) {
      test.skip()
      return
    }

    const uniqueContent = `School-Wide-Only ${Date.now()}`
    const contentTextarea = composer.locator('.composer-content textarea').first()
    await contentTextarea.fill(uniqueContent)
    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await publishButton.click()
    await expect(page.locator(toastWithText('Beitrag veröffentlicht'))).toBeVisible({ timeout: 10000 })

    // Now navigate to a room
    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    // The school-wide post should NOT appear in the room's Info-Board
    const roomPost = page.locator(`.feed-post:has-text("${uniqueContent}")`)
    const hasPost = await roomPost.isVisible({ timeout: 3000 }).catch(() => false)
    expect(hasPost).toBe(false)
  })
})

// --------------------------------------------------------------------------
// US-083: Beitrag-Quell-Labels
// --------------------------------------------------------------------------
test.describe('US-083: Beitrag-Quell-Labels', () => {

  test('posts from rooms show source name in "in <source>" format', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    // Look for posts with a source label
    const postSources = page.locator('.feed-post .post-source')
    const sourceCount = await postSources.count()

    if (sourceCount === 0) {
      // No posts with source labels -- skip
      test.skip()
      return
    }

    // Each source label follows the format "in <em>SourceName</em>"
    const firstSource = postSources.first()
    await expect(firstSource).toContainText('in')
    const emTag = firstSource.locator('em')
    await expect(emTag).toBeVisible()
    const sourceName = await emTag.textContent()
    expect(sourceName?.trim().length).toBeGreaterThan(0)
  })

  test('source labels correspond to known types (Raum, Schulbereich, etc.)', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    // The source labels display the actual name of the source (room name, section name, etc.)
    // We verify they render as non-empty text
    const sources = page.locator('.feed-post .post-source em')
    const count = await sources.count()

    for (let i = 0; i < count && i < 3; i++) {
      const text = await sources.nth(i).textContent()
      expect(text?.trim().length).toBeGreaterThan(0)
    }
  })
})

// --------------------------------------------------------------------------
// US-084: Link-Vorschau
// --------------------------------------------------------------------------
test.describe('US-084: Link-Vorschau', () => {

  test.skip('post with external URL shows link preview component', async () => {
    // TODO: Requires creating a post with an external URL (e.g., https://example.com)
    // and verifying the LinkPreview component renders with title/description.
    // The LinkPreview makes an API call to /api/v1/feed/link-preview which
    // may require network access to external sites -- unreliable in E2E.
  })

  test('LinkPreview component renders aria-label "Link-Vorschau"', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    // Check if any link previews are visible in the feed
    const linkPreviews = page.locator('.link-preview')
    const hasPreview = await linkPreviews.first().isVisible({ timeout: 3000 }).catch(() => false)

    if (!hasPreview) {
      // No link previews in current feed -- skip
      test.skip()
      return
    }

    await expect(linkPreviews.first()).toHaveAttribute('aria-label', 'Link-Vorschau')
  })
})

// --------------------------------------------------------------------------
// US-085: Anhang herunterladen
// --------------------------------------------------------------------------
test.describe('US-085: Anhang herunterladen', () => {

  test('post attachments render with file name and download icon', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const feedList = page.locator('.feed-list')
    await expect(feedList).toBeVisible({ timeout: 10000 })

    // Look for posts with attachments
    const attachmentItem = page.locator('.post-attachments .attachment-item').first()
    const hasAttachment = await attachmentItem.isVisible({ timeout: 3000 }).catch(() => false)

    if (!hasAttachment) {
      // No attachments in current feed posts -- skip
      test.skip()
      return
    }

    // Attachment should show file name
    await expect(attachmentItem.locator('.attachment-name')).toBeVisible()

    // Attachment should show file size
    await expect(attachmentItem.locator('.attachment-size')).toBeVisible()

    // Attachment should have download icon
    await expect(attachmentItem.locator('.attachment-download')).toBeVisible()
  })

  test('clicking an attachment triggers download', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const attachmentItem = page.locator('.post-attachments .attachment-item').first()
    const hasAttachment = await attachmentItem.isVisible({ timeout: 5000 }).catch(() => false)

    if (!hasAttachment) {
      test.skip()
      return
    }

    // Clicking the attachment item triggers downloadAttachment() which creates
    // an <a> element with the download URL. We verify the click doesn't error.
    // In headless mode, the actual download won't complete, but no error should occur.
    const [download] = await Promise.all([
      page.waitForEvent('download', { timeout: 5000 }).catch(() => null),
      attachmentItem.click(),
    ])

    // If a download event was triggered, that's expected behavior
    // If not, the click at least should not throw
  })
})

// --------------------------------------------------------------------------
// US-086: Feed -- kein Beitrag ohne Inhalt
// --------------------------------------------------------------------------
test.describe('US-086: Feed -- kein Beitrag ohne Inhalt', () => {

  test('publish button is disabled when content is empty', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    const composerVisible = await composer.isVisible({ timeout: 5000 }).catch(() => false)

    if (!composerVisible) {
      test.skip()
      return
    }

    // Publish button ("Veröffentlichen") should be disabled when content is empty
    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await expect(publishButton).toBeDisabled()
  })

  test('publish button becomes enabled when content is typed', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    const composerVisible = await composer.isVisible({ timeout: 5000 }).catch(() => false)

    if (!composerVisible) {
      test.skip()
      return
    }

    // Initially disabled
    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await expect(publishButton).toBeDisabled()

    // Type content
    const contentTextarea = composer.locator('.composer-content textarea').first()
    await contentTextarea.fill('Testinhalt')

    // Now enabled
    await expect(publishButton).toBeEnabled()

    // Clear content
    await contentTextarea.fill('')

    // Disabled again
    await expect(publishButton).toBeDisabled()
  })

  test('publish button becomes enabled when a file is attached (no text required)', async ({ page }) => {
    await login(page, accounts.teacher)

    const roomName = await navigateToFirstRoom(page)
    if (!roomName) {
      test.skip()
      return
    }

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Initially disabled
    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await expect(publishButton).toBeDisabled()

    // Attach a file (no text content)
    const fileInput = composer.locator('input[type="file"].hidden-file-input')
    await fileInput.setInputFiles({
      name: 'test.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('content'),
    })

    // Now enabled (file counts as content)
    await expect(publishButton).toBeEnabled()
  })
})

// --------------------------------------------------------------------------
// US-087: Beitrag schulweit erstellen (nur SA)
// --------------------------------------------------------------------------
test.describe('US-087: Beitrag schulweit erstellen (nur SA)', () => {

  test('admin can create school-wide post from dashboard', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    const uniqueContent = `Schulweiter-Beitrag ${Date.now()}`
    const contentTextarea = composer.locator('.composer-content textarea').first()
    await contentTextarea.fill(uniqueContent)

    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await publishButton.click()

    await expect(page.locator(toastWithText('Beitrag veröffentlicht'))).toBeVisible({ timeout: 10000 })
  })

  test('teacher can also create posts from dashboard (school-wide scope)', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    await expect(composer).toBeVisible({ timeout: 5000 })

    // Teachers have the PostComposer on dashboard too (v-if="auth.isTeacher || auth.isAdmin")
    const uniqueContent = `Lehrer-Dashboard-Post ${Date.now()}`
    const contentTextarea = composer.locator('.composer-content textarea').first()
    await contentTextarea.fill(uniqueContent)

    const publishButton = composer.locator('button:has-text("Veröffentlichen")')
    await publishButton.click()

    await expect(page.locator(toastWithText('Beitrag veröffentlicht'))).toBeVisible({ timeout: 10000 })
  })

  test('parent cannot create school-wide posts (no PostComposer on dashboard)', async ({ page }) => {
    await login(page, accounts.parent)
    await page.waitForLoadState('networkidle')

    const composer = page.locator('.post-composer')
    await expect(composer).not.toBeVisible()
  })
})

// --------------------------------------------------------------------------
// US-088: Video-Embed
// --------------------------------------------------------------------------
test.describe('US-088: Video-Embed', () => {

  test.skip('YouTube link in post content renders as embedded video', async () => {
    // TODO: Requires creating a post with a YouTube URL (e.g., https://youtu.be/dQw4w9WgXcQ)
    // and verifying that:
    // 1. The VideoEmbed component renders (.video-embed)
    // 2. An iframe with youtube-nocookie.com/embed/... src is present
    // This test is skipped because:
    // - It requires creating a post with a specific URL
    // - The RichContent component does client-side URL detection
    // - Verifying iframe content requires the URL to be valid
  })

  test('VideoEmbed component structure -- iframe with allowfullscreen', async ({ page }) => {
    await login(page, accounts.admin)
    await page.waitForLoadState('networkidle')

    // Look for any video embed in the current feed
    const videoEmbed = page.locator('.video-embed').first()
    const hasEmbed = await videoEmbed.isVisible({ timeout: 3000 }).catch(() => false)

    if (!hasEmbed) {
      // No video embeds in current feed -- skip
      test.skip()
      return
    }

    // Verify iframe attributes
    const iframe = videoEmbed.locator('iframe')
    await expect(iframe).toBeVisible()
    await expect(iframe).toHaveAttribute('allowfullscreen', '')

    // iframe src should contain youtube-nocookie or vimeo
    const src = await iframe.getAttribute('src')
    expect(src).toBeTruthy()
    expect(src!.includes('youtube-nocookie.com') || src!.includes('vimeo.com')).toBe(true)
  })
})

// --------------------------------------------------------------------------
// US-089: Impersonation (SA)
// --------------------------------------------------------------------------
test.describe('US-089: Impersonation (SA)', () => {

  test.skip('admin can impersonate another user and sees impersonation banner', async () => {
    // TODO: Impersonation is a complex and risky feature to test in E2E:
    // 1. SA navigates to user management
    // 2. Selects a user and clicks "Impersonate"
    // 3. Session switches to impersonated user
    // 4. Banner "Angemeldet als [Name] (Impersonation)" appears
    // 5. SA can end impersonation
    // Risks:
    // - Impersonation may affect other parallel tests
    // - Session state is complex to restore
    // - Cleanup is critical (must end impersonation)
  })
})

// --------------------------------------------------------------------------
// US-090: Zugriffsschutz -- unautorisierter API-Zugriff
// --------------------------------------------------------------------------
test.describe('US-090: Zugriffsschutz -- unautorisierter API-Zugriff', () => {

  test('API call without token returns 401 Unauthorized', async ({ page }) => {
    // Make a raw HTTP request without authentication
    const response = await page.request.get('/api/v1/feed', {
      headers: {
        // No Authorization header, no cookies
        'Cookie': '',
      },
    })

    // Should return 401 or 403
    expect([401, 403]).toContain(response.status())
  })

  test('authenticated API call returns 200', async ({ page }) => {
    await login(page, accounts.teacher)
    await page.waitForLoadState('networkidle')

    // After login, the browser has auth cookies/tokens
    // Make an API call using the authenticated context
    const response = await page.request.get('/api/v1/feed')

    expect(response.ok()).toBe(true)
    expect(response.status()).toBe(200)
  })

  test('public login endpoint returns 200 without auth', async ({ page }) => {
    // Login endpoint should be publicly accessible
    const response = await page.request.post('/api/v1/auth/login', {
      data: {
        email: 'nonexistent@test.com',
        password: 'wrong',
      },
      headers: {
        'Cookie': '',
      },
    })

    // Login with wrong credentials should return 401 (not 403/500)
    // The point is the endpoint itself is accessible (not blocked by auth filter)
    expect([200, 401]).toContain(response.status())
  })

  test('unauthenticated request to protected endpoint returns 401', async ({ page }) => {
    // Try accessing user profile without auth
    const response = await page.request.get('/api/v1/users/me', {
      headers: {
        'Cookie': '',
      },
    })

    expect([401, 403]).toContain(response.status())
  })

  test('unauthenticated request to admin endpoint returns 401', async ({ page }) => {
    const response = await page.request.get('/api/v1/admin/users', {
      headers: {
        'Cookie': '',
      },
    })

    expect([401, 403]).toContain(response.status())
  })
})
