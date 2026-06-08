<template>
  <view>
    <!-- App.vue 的模板通常不会被渲染，实际显示的是 pages.json 中定义的页面 -->
  </view>
</template>

<script setup>
import { onMounted } from 'vue'

// 应用启动时执行初始化
onMounted(() => {
  console.log('[App.vue] onMounted')

  try {
    // 设置安全区域 CSS 变量
    const systemInfo = uni.getSystemInfoSync()
    const statusBarHeight = systemInfo.statusBarHeight || 0

    // 存储到全局，供页面使用
    if (statusBarHeight > 0) {
      uni.setStorageSync('statusBarHeight', statusBarHeight)
    }

    // 注意：自动检查更新已移至 login.vue，因为 App.vue 的模板不会被渲染
  } catch (error) {
    console.error('[App.vue] 初始化失败:', error)
  }
})
</script>

<style lang="scss">
@import './styles/index.scss';
</style>
