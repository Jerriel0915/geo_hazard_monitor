<template>
  <el-config-provider :locale="zhCn" size="default" :z-index="3000" :message="{ max: 3 }">
    <div v-if="appError" class="global-error-fallback">
      <div class="global-error-card">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <h3>页面异常</h3>
        <p>{{ appError }}</p>
        <el-button type="primary" @click="appError = null">关闭</el-button>
      </div>
    </div>
    <router-view v-else />
  </el-config-provider>
</template>

<script setup lang="ts">
import {onErrorCaptured, ref} from 'vue'
import {WarningFilled} from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const appError = ref<string | null>(null)

onErrorCaptured((err, _instance, info) => {
  console.error('[Component Error]', err, `\n  source: ${info}`)
  appError.value = import.meta.env.DEV
    ? `页面发生错误: ${(err as Error).message || String(err)}`
    : '页面加载异常，请刷新重试'
  return false // 阻止错误继续向上冒泡
})
</script>

<style scoped>
.global-error-fallback {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}
.global-error-card {
  text-align: center;
  padding: 48px 64px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.global-error-card h3 {
  margin: 16px 0 8px;
  color: #303133;
}
.global-error-card p {
  margin: 0 0 24px;
  color: #909399;
  font-size: 14px;
  max-width: 400px;
}
</style>
