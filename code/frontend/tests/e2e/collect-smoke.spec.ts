import { test, expect } from '@playwright/test'

test.describe('收集页冒烟', () => {
  test('idle 显示「开始收集」且可进入文本模式', async ({ page }) => {
    await page.goto('/collect')
    await expect(page.getByRole('button', { name: '开始收集' })).toBeVisible()
    await page.locator('.smart-input').fill('这是一条测试素材正文')
    await page.getByRole('button', { name: '开始收集' }).click()
    await expect(page.locator('.type-chip')).toHaveText('📝 文本')
    await expect(page.getByPlaceholder('正文内容')).toHaveValue('这是一条测试素材正文')
  })

  test('idle 单行 URL 点击开始收集进入链接模式', async ({ page }) => {
    await page.goto('/collect')
    await page
      .locator('.smart-input')
      .fill('https://example.com/')
    await page.getByRole('button', { name: '开始收集' }).click()
    await expect(page.locator('.type-chip')).toHaveText('🔗 链接')
    await expect(page.getByPlaceholder('https://...')).toHaveValue('https://example.com/')
  })
})
