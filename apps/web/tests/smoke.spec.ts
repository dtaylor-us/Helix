import { test, expect } from '@playwright/test'

test('loads helix today shell', async ({ page }) => {
  await page.goto('/today')
  await expect(page.getByRole('heading', { name: 'Helix' })).toBeVisible()
})
