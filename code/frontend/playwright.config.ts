import { defineConfig, devices } from '@playwright/test'

/**
 * 冒烟 E2E（替代当前会话不可用的 Playwright MCP）：
 * - 默认：自动 `npm run dev` 在 5173，并以此为 baseURL。
 * - 对接 Docker 前端：`PLAYWRIGHT_BASE_URL=http://127.0.0.1:8081 npm run test:e2e`（需自行已启动容器）。
 * - 已手动起 dev 且勿重复拉起：`PLAYWRIGHT_NO_SERVER=1 npm run test:e2e`。
 */
const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'

export default defineConfig({
  testDir: 'tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'list',
  use: {
    baseURL,
    trace: 'on-first-retry'
  },
  webServer:
    process.env.PLAYWRIGHT_NO_SERVER || process.env.PLAYWRIGHT_BASE_URL
      ? undefined
      : {
          command: 'npm run dev -- --host 127.0.0.1 --port 5173',
          url: 'http://127.0.0.1:5173',
          reuseExistingServer: true,
          timeout: 120_000
        },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
})
