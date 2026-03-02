import { test, expect } from '@playwright/test'

test.describe('Smoke Test', () => {
  test('app is running and login page loads', async ({ page }) => {
    await page.goto('/')
    // Should see login page or dashboard
    await expect(page).toHaveTitle(/MonteWeb|Montessori/)
    // Or at minimum the page should load without error
    await expect(page.locator('body')).toBeVisible()
  })
})
