<template>
  <aside class="api-docs-sidebar">
    <div class="sidebar-title">📚 API 文档</div>

    <section
      v-for="group in docs"
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

    <!-- 动态算法列表 (仅 alarm 模式) -->
    <section v-if="algoGroups.length" class="api-group algo-section">
      <header class="group-header" style="color: #9c27b0">
        <span class="group-icon">📦</span>
        <span>已注册算法</span>
        <span class="group-desc">algo.executeLatest(code, ...)</span>
      </header>
      <section v-for="algo in algoGroups" :key="algo.code" class="algo-item">
        <header class="algo-header">
          <code class="algo-code">{{ algo.code }}</code>
          <span class="algo-name">{{ algo.name }}</span>
        </header>
        <span v-if="algo.description" class="method-note">{{ algo.description }}</span>
        <ul v-if="algo.methods.length" class="method-list">
          <li v-for="(m, idx) in algo.methods" :key="idx" class="method-item">
            <code class="method-sig">  .{{ m.name }}({{ (m.params || []).join(', ') }})</code>
            <span v-if="m.summary" class="method-note">{{ m.summary }}</span>
          </li>
        </ul>
        <span v-else class="method-note">方法加载中...</span>
      </section>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getApiDocs } from './script-api-docs'
import type { ScriptMode, ApiGroup } from './script-api-docs'
import request from '@/utils/request'

const props = withDefaults(defineProps<{
  mode?: ScriptMode
}>(), {
  mode: 'calc'
})

const docs = computed(() => getApiDocs(props.mode))

// ── 动态算法列表 (alarm 模式) ──
interface AlgoMethod {
  name: string
  summary?: string
  params?: string[]
}
interface AlgoEntry {
  code: string
  name: string
  description?: string
  methods: AlgoMethod[]
}

const algoGroups = ref<AlgoEntry[]>([])

watch(() => props.mode, async (mode) => {
  if (mode !== 'alarm') {
    algoGroups.value = []
    return
  }
  try {
    const res = await request.get<any>('/algo-lib/page', {
      params: { status: 1, pageSize: 100 }
    })
    const rows = res?.rows || res?.data?.rows || []
    const entries: AlgoEntry[] = []
    for (const row of rows) {
      const entry: AlgoEntry = {
        code: row.code,
        name: row.name,
        description: row.description,
        methods: []
      }
      try {
        const desc = await request.get<any>(`/algo-lib/${row.code}/describe-latest`)
        const data = desc?.data || desc
        const methods = data?.methods
        if (methods && typeof methods === 'object' && !Array.isArray(methods)) {
          // 对象格式: { method_name: { summary, params } }
          entry.methods = Object.entries(methods).map(([name, info]: [string, any]) => ({
            name,
            summary: info?.summary,
            params: info?.params
              ? Object.entries(info.params).map(([pk, pv]: [string, any]) =>
                  `${pk}${pv?.required ? '' : '?'}: ${pv?.type || 'any'}`)
              : []
          }))
        } else if (Array.isArray(methods)) {
          entry.methods = methods.map((m: any) => ({
            name: m.name || m.method || String(m),
            summary: m.summary,
            params: m.params
              ? Object.entries(m.params).map(([pk, pv]: [string, any]) =>
                  `${pk}${pv?.required ? '' : '?'}: ${pv?.type || 'any'}`)
              : []
          }))
        }
      } catch {
        // describe 接口可能失败（算法未安装等），忽略
      }
      entries.push(entry)
    }
    algoGroups.value = entries
  } catch {
    algoGroups.value = []
  }
}, { immediate: true })
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

.algo-section {
  border-top: 2px solid #9c27b0 !important;
  padding-top: 8px;
}

.algo-item {
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px dotted #ebeef5;
}

.algo-item:last-child {
  border-bottom: none;
}

.algo-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 2px;
}

.algo-code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 10px;
  color: #9c27b0;
  font-weight: 600;
  background: #f3e5f5;
  padding: 1px 4px;
  border-radius: 2px;
}

.algo-name {
  font-size: 10px;
  color: #606266;
}
</style>
