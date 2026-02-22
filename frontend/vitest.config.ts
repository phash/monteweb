import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  define: {
    __APP_VERSION__: JSON.stringify('0.0.0-test'),
    __BUILD_TIME__: JSON.stringify('2024-01-01T00:00:00.000Z'),
    __GIT_BRANCH__: JSON.stringify('test'),
  },
  test: {
    environment: 'jsdom',
    globals: true,
    // Suppress jsdom/undici unhandled rejection false positives (Node 22 compat)
    dangerouslyIgnoreUnhandledErrors: true,
    setupFiles: ['src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,vue}'],
      exclude: [
        'src/test/**',
        'src/**/*.test.ts',
        'src/**/*.spec.ts',
        'src/main.ts',
        'src/env.d.ts',
        'src/vite-env.d.ts',
        'src/types/**',
        'src/i18n/**',
        'src/api/**',
        'src/App.vue',
        'src/router/**',
        'src/components/layout/AppLayout.vue',
        'src/components/layout/AppHeader.vue',
      ],
      thresholds: {
        statements: 53,
      },
    },
  },
})
