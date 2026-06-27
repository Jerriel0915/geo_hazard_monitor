<template>
  <div class="terra-settings">
    <div class="settings-sidebar">
      <h3 class="settings-title">Terra 设置</h3>
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="settings-tab"
        :class="{ active: currentTab === tab.path }"
        @click="$router.push(`/terra/settings/${tab.path}`)"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </div>
    <div class="settings-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { User, Setting, MagicStick, Tools } from '@element-plus/icons-vue'

const route = useRoute()
const tabs = [
  { path: 'personality', label: '人格配置', icon: User },
  { path: 'models', label: '模型配置', icon: Setting },
  { path: 'skills', label: '技能管理', icon: MagicStick },
  { path: 'tools', label: '工具管理', icon: Tools },
]
const currentTab = computed(() => route.path.split('/').pop() || 'personality')
</script>

<style scoped>
.terra-settings { display: flex; height: calc(100vh - 90px); background: #f5f7fa; }
.settings-sidebar { width: 200px; background: white; border-right: 1px solid #e4e7ed; padding: 16px 0; flex-shrink: 0; }
.settings-title { padding: 0 20px 16px; margin: 0; font-size: 16px; color: #303133; border-bottom: 1px solid #f0f0f0; }
.settings-tab { display: flex; align-items: center; gap: 8px; padding: 10px 20px; cursor: pointer; color: #606266; font-size: 14px; transition: all 0.2s; }
.settings-tab:hover { background: #f5f7fa; color: #409EFF; }
.settings-tab.active { background: #ecf5ff; color: #409EFF; border-right: 3px solid #409EFF; }
.settings-content { flex: 1; padding: 20px; overflow-y: auto; }
</style>
