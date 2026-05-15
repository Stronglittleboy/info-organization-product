import { test, expect } from '@playwright/test'

test.describe('浏览页冒烟', () => {
  test('展示标题与主导航', async ({ page }) => {
    await page.goto('/browse')
    await expect(page.getByRole('heading', { name: '按时间浏览' })).toBeVisible()
    await expect(page.locator('.nav-link.active').filter({ hasText: '浏览' })).toBeVisible()
  })
})
