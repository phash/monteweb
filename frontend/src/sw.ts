/// <reference lib="webworker" />

import { cleanupOutdatedCaches, precacheAndRoute, createHandlerBoundToURL } from 'workbox-precaching'
import { clientsClaim } from 'workbox-core'
import { registerRoute, NavigationRoute } from 'workbox-routing'
import { NetworkFirst } from 'workbox-strategies'
import { ExpirationPlugin } from 'workbox-expiration'
import { CacheableResponsePlugin } from 'workbox-cacheable-response'

declare let self: ServiceWorkerGlobalScope

// Auto-update: skip waiting and claim clients immediately
self.skipWaiting()
clientsClaim()

// Clean up old caches from previous versions
cleanupOutdatedCaches()

// Precache static assets (injected by vite-plugin-pwa)
precacheAndRoute(self.__WB_MANIFEST)

// SPA navigation fallback
const navigationHandler = createHandlerBoundToURL('/index.html')
registerRoute(new NavigationRoute(navigationHandler))

// --- Runtime caching for API calls ---

// User's own data (families, profile, notifications) — cache 24h
registerRoute(
  ({ url }) => /\/api\/v1\/(families\/mine|users\/me|notifications)/.test(url.pathname),
  new NetworkFirst({
    cacheName: 'user-data-cache',
    plugins: [
      new ExpirationPlugin({ maxEntries: 20, maxAgeSeconds: 86400 }),
      new CacheableResponsePlugin({ statuses: [0, 200] }),
    ],
  }),
)

// Calendar events and jobs — cache 1h
registerRoute(
  ({ url }) => /\/api\/v1\/(calendar|jobs|cleaning\/my-slots|cleaning\/dashboard)/.test(url.pathname),
  new NetworkFirst({
    cacheName: 'content-cache',
    plugins: [
      new ExpirationPlugin({ maxEntries: 100, maxAgeSeconds: 3600 }),
      new CacheableResponsePlugin({ statuses: [0, 200] }),
    ],
  }),
)

// Feed and other API calls — cache 5min
registerRoute(
  ({ url }) => /\/api\/v1\//.test(url.pathname),
  new NetworkFirst({
    cacheName: 'api-cache',
    plugins: [
      new ExpirationPlugin({ maxEntries: 100, maxAgeSeconds: 300 }),
      new CacheableResponsePlugin({ statuses: [0, 200] }),
    ],
  }),
)

// --- Push Notifications ---

self.addEventListener('push', (event) => {
  const data = event.data?.json() ?? {}

  const options: NotificationOptions = {
    body: data.body || '',
    icon: '/icons/icon-192x192.png',
    badge: '/icons/icon-192x192.png',
    tag: data.tag || 'monteweb-notification',
    data: { url: data.url || '/' },
  }

  event.waitUntil(
    self.registration.showNotification(data.title || 'MonteWeb', options),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()

  const targetUrl = event.notification.data?.url || '/'

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // Focus existing window if one is open
      for (const client of clientList) {
        if ('focus' in client) {
          client.navigate(targetUrl)
          return client.focus()
        }
      }
      // Otherwise open a new window
      return self.clients.openWindow(targetUrl)
    }),
  )
})
