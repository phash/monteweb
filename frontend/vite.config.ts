import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'
import { fileURLToPath, URL } from 'node:url'
import { execSync } from 'node:child_process'

function getGitBranch(): string {
  try {
    return execSync('git rev-parse --abbrev-ref HEAD', { encoding: 'utf-8' }).trim()
  } catch {
    return 'unknown'
  }
}

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'robots.txt', 'icons/icon.svg'],
      manifest: {
        name: 'MonteWeb - Schul-Intranet',
        short_name: 'MonteWeb',
        description: 'Modulares Intranet fuer Montessori-Schulkomplexe',
        theme_color: '#3B82F6',
        background_color: '#F9FAFB',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          {
            src: '/icons/icon-192x192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: '/icons/icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
          },
          {
            src: '/icons/icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
          {
            src: '/icons/apple-touch-icon.png',
            sizes: '180x180',
            type: 'image/png',
            purpose: 'any',
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        navigateFallback: 'index.html',
        runtimeCaching: [
          {
            // User's own data (families, profile, notifications) — cache for offline
            urlPattern: /\/api\/v1\/(families\/mine|users\/me|notifications)/,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'user-data-cache',
              expiration: {
                maxEntries: 20,
                maxAgeSeconds: 86400, // 24 hours
              },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            // Calendar events and jobs — cache for offline viewing
            urlPattern: /\/api\/v1\/(calendar|jobs|cleaning\/my-slots|cleaning\/dashboard)/,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'content-cache',
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 3600, // 1 hour
              },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            // Feed and other API calls — shorter cache
            urlPattern: /\/api\/v1\//,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 300, // 5 minutes
              },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  define: {
    __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '0.1.0-beta'),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
    __GIT_BRANCH__: JSON.stringify(getGitBranch()),
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('/vue/') || id.includes('/@vue/') || id.includes('/vue-router/') || id.includes('/pinia/')) {
              return 'vue-vendor'
            }
            if (id.includes('/vue-i18n/') || id.includes('/@intlify/')) {
              return 'i18n-vendor'
            }
            if (id.includes('/axios/') || id.includes('/@stomp/')) {
              return 'http-vendor'
            }
          }
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
})
