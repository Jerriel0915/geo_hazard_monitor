<template>
  <aside class="api-docs-sidebar">
    <div class="sidebar-title">📚 API 文档</div>

    <section
      v-for="group in API_DOCS"
      :key="group.name"
      class="api-group"
      :data-test="`group-${group.name}`"
    >
      <header class="group-header" :style="{ color: group.color }">
        <span class="group-icon">{{ group.icon }}</span>
        <span data-test="group-name">{{ group.name }}</span>
        <span v-if="group.description" class="group-desc">{{ group.description }}</span>
      </header>

      <ul class="method-list">
        <li v-for="(m, idx) in group.methods" :key="idx" class="method-item">
          <code class="method-sig" data-test="method-sig">{{ m.signature }}</code>
          <span v-if="m.note" class="method-note">{{ m.note }}</span>
        </li>
      </ul>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { API_DOCS } from './script-api-docs'
</script>

<style scoped>
.api-docs-sidebar {
  width: 240px;
  background: #fafbfc;
  border-left: 1px solid #ebeef5;
  padding: 10px 12px;
  font-size: 11px;
  color: #606266;
  overflow-y: auto;
  flex-shrink: 0;
}

.sidebar-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}

.api-group {
  margin-top: 10px;
  padding-top: 6px;
  border-top: 1px dashed #ebeef5;
}

.api-group:first-of-type {
  margin-top: 0;
  padding-top: 0;
  border-top: none;
}

.group-header {
  font-weight: 600;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.group-icon {
  font-size: 12px;
}

.group-desc {
  font-size: 10px;
  color: #909399;
  font-weight: 400;
  font-style: italic;
  margin-left: 4px;
}

.method-list {
  list-style: none;
  padding: 0;
  margin: 0 0 0 8px;
}

.method-item {
  line-height: 1.7;
}

.method-sig {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 10px;
  color: #303133;
  background: transparent;
  padding: 0;
}

.method-note {
  display: block;
  font-size: 9px;
  color: #909399;
  font-style: italic;
  margin-left: 8px;
  margin-top: -2px;
}
</style>
