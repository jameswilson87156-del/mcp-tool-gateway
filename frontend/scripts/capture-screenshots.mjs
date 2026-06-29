import { chromium } from 'playwright'
import { execFile, spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const projectRoot = resolve(root, '..')
const backendPort = process.env.SCREENSHOT_BACKEND_PORT ?? '18080'
const frontendPort = process.env.SCREENSHOT_FRONTEND_PORT ?? '5173'
const url = `http://127.0.0.1:${frontendPort}`
const apiBase = `http://127.0.0.1:${backendPort}/api`
const apiUrl = `${apiBase}/tools`
const isWindows = process.platform === 'win32'

const backend = isWindows
  ? spawn('cmd.exe', ['/d', '/s', '/c', `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=${backendPort}`], {
      cwd: resolve(projectRoot, 'backend'),
      stdio: 'pipe'
    })
  : spawn('mvn', ['spring-boot:run'], {
      cwd: resolve(projectRoot, 'backend'),
      stdio: 'pipe'
    })

const frontend = isWindows
  ? spawn('cmd.exe', ['/d', '/s', '/c', `npm run dev -- --host 127.0.0.1 --port ${frontendPort} --strictPort`], {
      cwd: root,
      stdio: 'pipe',
      env: { ...process.env, BROWSER: 'none', VITE_API_BASE: apiBase }
    })
  : spawn('npm', ['run', 'dev', '--', '--host', '127.0.0.1'], {
      cwd: root,
      stdio: 'pipe',
      env: { ...process.env, BROWSER: 'none', VITE_API_BASE: apiBase }
    })

try {
  await waitForServer(apiUrl, 45000)
  await waitForServer(url)
  await mkdir(resolve(projectRoot, 'docs', 'images', 'large'), { recursive: true })
  const browser = await chromium.launch()
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 })
  await page.goto(url, { waitUntil: 'networkidle' })
  await capturePages(page, resolve(projectRoot, 'docs', 'images'))

  await page.setViewportSize({ width: 1920, height: 1080 })
  await page.goto(url, { waitUntil: 'networkidle' })
  await capturePages(page, resolve(projectRoot, 'docs', 'images', 'large'))
  await browser.close()
} finally {
  await stopProcess(frontend)
  await stopProcess(backend)
}

async function capturePages(page, outputDir) {
  await page.locator('button.nav-item').filter({ hasText: /Tool Call/ }).click()
  await page.waitForLoadState('networkidle')
  await page.screenshot({ path: resolve(outputDir, 'mcp-tool-workbench.png'), fullPage: true })

  await page.locator('button.nav-item').filter({ hasText: /Tool Registry/ }).click()
  await page.waitForTimeout(500)
  await page.screenshot({ path: resolve(outputDir, 'tool-registry.png'), fullPage: true })

  await page.locator('button.nav-item').filter({ hasText: /Human Review/ }).click()
  await page.waitForTimeout(500)
  await page.screenshot({ path: resolve(outputDir, 'human-review-center.png'), fullPage: true })

  await page.locator('button.nav-item').filter({ hasText: /Trace Evidence/ }).click()
  await page.waitForTimeout(500)
  await page.screenshot({ path: resolve(outputDir, 'trace-evidence.png'), fullPage: true })

  await page.locator('button.nav-item').filter({ hasText: /\u5ba1\u8ba1\u65e5\u5fd7/ }).click()
  await page.waitForTimeout(500)
  await page.screenshot({ path: resolve(outputDir, 'audit-log.png'), fullPage: true })

  await page.locator('button.nav-item').filter({ hasText: /Prompt|\u63d0\u793a\u8bcd/ }).click()
  await page.waitForTimeout(500)
  await page.getByRole('button', { name: /Render Prompt/ }).click()
  await page.waitForTimeout(700)
  await page.getByRole('button', { name: /New Prompt/ }).click()
  await page.waitForTimeout(400)
  await page.screenshot({ path: resolve(outputDir, 'prompt-resource.png'), fullPage: true })
}

async function waitForServer(target, timeout = 30000) {
  const started = Date.now()
  while (Date.now() - started < timeout) {
    try {
      const response = await fetch(target)
      if (response.ok) return
    } catch {
      await new Promise((resolveWait) => setTimeout(resolveWait, 500))
    }
  }
  throw new Error(`Timed out waiting for ${target}`)
}

async function stopProcess(child) {
  if (!child || child.exitCode !== null) return
  if (process.platform === 'win32') {
    await new Promise((resolveStop) => {
      execFile('taskkill', ['/pid', String(child.pid), '/T', '/F'], () => resolveStop())
    })
  } else {
    child.kill('SIGTERM')
  }
}
