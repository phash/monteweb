import { test as setup } from '@playwright/test'
import { loginAndSaveState } from '../helpers/auth'
import { accounts } from '../fixtures/test-accounts'
import * as fs from 'fs'
import * as path from 'path'

const authDir = path.join(__dirname, '..', 'auth-states')

// Ensure auth-states directory exists
setup('create auth directory', async () => {
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true })
  }
})

// Create storage states for each role
for (const [key, account] of Object.entries(accounts)) {
  setup(`authenticate as ${key}`, async ({ page, context }) => {
    await loginAndSaveState(page, context, account, path.join(authDir, `${key}.json`))
  })
}
