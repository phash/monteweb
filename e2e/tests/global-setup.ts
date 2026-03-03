import { test as setup, type Page } from '@playwright/test'
import { accounts } from '../fixtures/test-accounts'
import * as fs from 'fs'
import * as path from 'path'

const authDir = path.join(__dirname, '..', 'auth-states')
const BASE = 'http://localhost'

// Cache tokens from the ensure step to avoid re-login in authenticate step
const tokenCache: Record<string, string> = {}

/**
 * Login via API with retry for rate limiting.
 */
async function apiLogin(page: Page, email: string, password: string): Promise<string> {
  for (let attempt = 0; attempt < 3; attempt++) {
    const res = await page.request.post(`${BASE}/api/v1/auth/login`, {
      data: { email, password },
    })
    const body = await res.json()

    if (body.data?.accessToken) return body.data.accessToken

    // Rate limited — wait and retry
    if (res.status() === 429 || body.error?.includes('Too many')) {
      await page.waitForTimeout(3000 * (attempt + 1))
      continue
    }

    throw new Error(`Login failed for ${email}: ${body.message || JSON.stringify(body)}`)
  }
  throw new Error(`Login failed for ${email} after 3 retries (rate limited)`)
}

/**
 * Ensure a test account exists and has terms accepted.
 */
async function ensureAccount(
  page: Page,
  adminToken: string,
  email: string,
  password: string,
  role: string,
  displayName: string,
) {
  // Try login
  let userToken: string | undefined
  try {
    userToken = await apiLogin(page, email, password)
  } catch {
    // User doesn't exist — register
    const nameParts = displayName.split(' ')
    await page.request.post(`${BASE}/api/v1/auth/register`, {
      data: {
        email,
        password,
        firstName: nameParts[0] || 'Test',
        lastName: nameParts.slice(1).join(' ') || 'User',
      },
    })

    // Find and approve via admin
    await page.waitForTimeout(500)
    const searchRes = await page.request.fetch(
      `${BASE}/api/v1/admin/users?search=${encodeURIComponent(email.split('@')[0])}&size=10`,
      { headers: { Authorization: `Bearer ${adminToken}` } },
    )
    const searchBody = await searchRes.json()
    const user = searchBody.data?.content?.find((u: any) => u.email === email)

    if (user) {
      await page.request.put(`${BASE}/api/v1/admin/users/${user.id}/status?active=true`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      })
      if (role !== 'PARENT') {
        await page.request.put(`${BASE}/api/v1/admin/users/${user.id}/roles`, {
          headers: { Authorization: `Bearer ${adminToken}` },
          data: { role },
        })
      }
      if (role === 'SECTION_ADMIN') {
        const secRes = await page.request.fetch(`${BASE}/api/v1/sections`, {
          headers: { Authorization: `Bearer ${adminToken}` },
        })
        const secBody = await secRes.json()
        if (secBody.data?.length > 0) {
          await page.request.put(`${BASE}/api/v1/admin/users/${user.id}/assigned-roles`, {
            headers: { Authorization: `Bearer ${adminToken}` },
            data: { role: 'SECTION_ADMIN', specialRoles: [`SECTION_ADMIN:${secBody.data[0].id}`] },
          })
        }
      }
    }

    await page.waitForTimeout(500)
    userToken = await apiLogin(page, email, password)
  }

  // Accept terms
  if (userToken) {
    await page.request.post(`${BASE}/api/v1/privacy/terms/accept`, {
      headers: { Authorization: `Bearer ${userToken}` },
    })
    tokenCache[email] = userToken
  }
}

// Ensure auth-states directory exists
setup('create auth directory', async () => {
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true })
  }
})

// Ensure all test accounts exist and terms are accepted
setup('ensure test accounts', async ({ page }) => {
  const adminToken = await apiLogin(page, 'admin@monteweb.local', 'test1234')
  tokenCache['admin@monteweb.local'] = adminToken

  // Verify admin has SUPERADMIN role (seed data may set wrong role)
  const meRes = await page.request.fetch(`${BASE}/api/v1/users/me`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  })
  const meBody = await meRes.json()
  if (meBody.data?.role !== 'SUPERADMIN') {
    // Admin doesn't have SUPERADMIN role — this can't be self-fixed via API.
    // The admin must be SUPERADMIN in the database for other test accounts to be provisioned.
    console.warn(`WARNING: admin@monteweb.local has role ${meBody.data?.role} instead of SUPERADMIN. Some tests may fail.`)
  }

  // Accept terms for admin first
  await page.request.post(`${BASE}/api/v1/privacy/terms/accept`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  })

  for (const [, account] of Object.entries(accounts)) {
    if (account.email === 'admin@monteweb.local') continue
    await ensureAccount(page, adminToken, account.email, account.password, account.role, account.displayName)
    await page.waitForTimeout(200) // small delay to avoid rate limiting
  }
})

// Create storage states by injecting cached tokens into sessionStorage
for (const [key, account] of Object.entries(accounts)) {
  setup(`authenticate as ${key}`, async ({ page, context }) => {
    // Use cached token or login fresh
    let token = tokenCache[account.email]
    if (!token) {
      token = await apiLogin(page, account.email, account.password)
    }

    // Navigate to app and inject token
    await page.goto('/')
    await page.evaluate((accessToken) => {
      sessionStorage.setItem('accessToken', accessToken)
    }, token)
    await page.reload()
    await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15000 })

    // Save storage state (cookies + localStorage — sessionStorage doesn't persist but we handle it)
    await context.storageState({ path: path.join(authDir, `${key}.json`) })
  })
}
