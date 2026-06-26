import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    include: ['src/**/__tests__/**/*.test.ts'],
    coverage: {
      include: ['src/lib/**', 'src/composables/**', 'src/components/**'],
      thresholds: {
        lines: 80,
        functions: 80
      }
    }
  }
})
