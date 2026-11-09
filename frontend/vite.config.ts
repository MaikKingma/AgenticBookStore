/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The storefront is served by the bookstore under /shop/, so every built asset URL starts with that prefix.
// In dev mode the API calls are proxied to a bookstore on 127.0.0.1:8080 (not "localhost", which can resolve to IPv6).
const bookstore = 'http://127.0.0.1:8080'

export default defineConfig({
  base: '/shop/',
  plugins: [react()],
  server: {
    // Vite listens on "localhost" by default, which can be IPv6 only. Listen on the IPv4 loopback address instead.
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/books': bookstore,
      '/inventory': bookstore,
      '/orders': bookstore,
      '/assistant': bookstore,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
