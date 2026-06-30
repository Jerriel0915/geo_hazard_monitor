import vue from '@vitejs/plugin-vue'
import path from 'path'
import { defineConfig } from 'vite'

import { visualizer } from 'rollup-plugin-visualizer'
import AutoImport from 'unplugin-auto-import/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import Components from 'unplugin-vue-components/vite'
import compression from 'vite-plugin-compression'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需自动导入（JS 组件 tree-shake；CSS 由 main.ts 统一引入以避免内部子组件路径解析问题）
    AutoImport({
      resolvers: [ElementPlusResolver()],
      // 全局自动导入 Element Plus 命令式 API
      imports: [{
        'element-plus': [
          'ElMessage',
          'ElMessageBox',
          'ElNotification',
          'ElLoading',
        ],
      }],
    }),
    Components({resolvers: [ElementPlusResolver({importStyle: false})]}),
    // 构建时预压缩 — Nginx gzip_static 直接命中
    compression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240,
      deleteOriginFile: false,
    }),
    compression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 10240,
      deleteOriginFile: false,
    }),
    // Bundle 体积分析 → dist/stats.html
    visualizer({
      filename: 'dist/stats.html',
      gzipSize: true,
      brotliSize: true,
      open: false,
    }),
  ],

  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },

  build: {
    target: 'es2020',
    cssCodeSplit: true,
    chunkSizeWarningLimit: 1500,
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return

          // 重型 vendor 独立分包 — 仅在使用到它们的路由才加载
          if (id.includes('three')) return 'vendor-three'
          if (id.includes('echarts-gl')) return 'vendor-echarts-gl'
          if (id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('blockly')) return 'vendor-blockly'
          if (id.includes('leaflet')) return 'vendor-leaflet'
          if (id.includes('jspdf') || id.includes('html2canvas')) return 'vendor-pdf'
          if (id.includes('hls.js') || id.includes('mpegts.js')) return 'vendor-media'
          if (id.includes('element-plus')) return 'vendor-element'
          if (id.includes('@vue') || id.includes('vue-router') || /\/vue\//.test(id)) return 'vendor'

          return 'vendor'
        },
      },
    },
  },

  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
})
