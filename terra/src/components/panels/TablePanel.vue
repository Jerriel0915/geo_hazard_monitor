<!-- ======================================== -->
<!-- TablePanel - 数据表格面板组件 -->
<!-- ======================================== -->

<template>
  <div
    class="table-panel"
    :style="panelStyle"
  >
    <div class="panel-header">
      <h3 class="panel-title">{{ config.title }}</h3>
      <div class="panel-actions">
        <button class="action-btn" @click="handleMaximize" title="最大化">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 2h5v2H4v3H2V2zm7 0h5v5h-2V4H9V2zM2 9h2v3h3v2H2V9zm12 0h-2v3h-3v2h5V9z"/>
          </svg>
        </button>
        <button class="action-btn close" @click="handleClose" title="关闭">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
          </svg>
        </button>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              @click="handleSort(column.key)"
            >
              {{ column.label }}
              <span v-if="sortKey === column.key" class="sort-indicator">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, index) in sortedRows"
            :key="row.id || index"
            :class="{ highlighted: highlightedRows.has(index) }"
            @click="handleRowClick(row, index)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
            >
              {{ formatValue(row[column.key], column) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="loading" class="panel-loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { usePanelCommand } from '@/composables/usePanelCommand'
import { useTablePanelCommands } from '@/composables/useTablePanelCommands'
import type { PanelConfig } from '@/types'

/**
 * Props
 */
interface Props {
  id: string
  config: PanelConfig
}

const props = defineProps<Props>()

/**
 * Emits
 */
interface Emits {
  (e: 'focus', panelId: string): void
  (e: 'close', panelId: string): void
}

const emit = defineEmits<Emits>()

/**
 * 状态
 */
const loading = ref(false)
const isMaximized = ref(false)
const rows = ref<any[]>([])
const columns = ref<Array<{ key: string; label: string; format?: string }>>([])
const highlightedRows = ref<Set<number>>(new Set())
const sortKey = ref<string>('')
const sortOrder = ref<'asc' | 'desc'>('asc')

/**
 * 面板样式
 */
const panelStyle = computed(() => {
  if (!props.config?.position) {
    console.warn('[TablePanel] config.position is missing:', props.config)
    return {}
  }
  const { x, y, w, h } = props.config.position
  return {
    gridColumn: `${x} / span ${w}`,
    gridRow: `${y} / span ${isMaximized.value ? 999 : h}`
  }
})

/**
 * 排序后的行
 */
const sortedRows = computed(() => {
  if (!sortKey.value) return rows.value

  return [...rows.value].sort((a, b) => {
    const aVal = a[sortKey.value]
    const bVal = b[sortKey.value]

    if (aVal === bVal) return 0
    if (aVal == null) return 1
    if (bVal == null) return -1

    let result = 0
    if (typeof aVal === 'number' && typeof bVal === 'number') {
      result = aVal - bVal
    } else {
      result = String(aVal).localeCompare(String(bVal))
    }

    return sortOrder.value === 'asc' ? result : -result
  })
})

/**
 * 格式化值
 */
function formatValue(value: any, column: any) {
  if (value == null) return '-'

  if (column.format === 'number') {
    return Number(value).toFixed(2)
  }

  if (column.format === 'percent') {
    return `${Number(value).toFixed(1)}%`
  }

  if (column.format === 'date') {
    return new Date(value).toLocaleDateString()
  }

  if (column.format === 'datetime') {
    return new Date(value).toLocaleString()
  }

  return value
}

/**
 * 处理排序
 */
function handleSort(key: string) {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortOrder.value = 'asc'
  }
}

/**
 * 处理行点击
 */
function handleRowClick(row: any, index: number) {
  emit('focus', props.id)
}

/**
 * 最大化面板
 */
function handleMaximize() {
  isMaximized.value = !isMaximized.value
}

/**
 * 关闭面板
 */
function handleClose() {
  emit('close', props.id)
}

/**
 * 注册面板指令处理器
 */
const { registerAutoHandler } = usePanelCommand()

// 创建表格操作方法
const tableOperations = {
  setRows: (data: any[]) => {
    rows.value = data
    return { success: true, count: data.length }
  },

  addRow: (row: any) => {
    rows.value.push(row)
    return { success: true, index: rows.value.length - 1 }
  },

  updateRow: (params: { index: number; data: any }) => {
    if (params.index >= 0 && params.index < rows.value.length) {
      rows.value[params.index] = { ...rows.value[params.index], ...params.data }
      return { success: true }
    }
    return { success: false, error: 'Invalid row index' }
  },

  removeRow: (index: number) => {
    if (index >= 0 && index < rows.value.length) {
      const removed = rows.value.splice(index, 1)
      return { success: true, removed: removed[0] }
    }
    return { success: false, error: 'Invalid row index' }
  },

  highlightRow: (index: number) => {
    highlightedRows.value.add(index)
    return { success: true, highlighted: Array.from(highlightedRows.value) }
  },

  unhighlightRow: (index: number) => {
    highlightedRows.value.delete(index)
    return { success: true, highlighted: Array.from(highlightedRows.value) }
  },

  clearHighlight: () => {
    highlightedRows.value.clear()
    return { success: true }
  },

  sortBy: (key: string, order?: 'asc' | 'desc') => {
    sortKey.value = key
    if (order) sortOrder.value = order
    return { success: true, sortKey: key, sortOrder: sortOrder.value }
  },

  scrollTo: (index: number) => {
    // 延迟滚动，确保 DOM 已更新
    setTimeout(() => {
      const tbody = document.querySelector('.table-panel tbody')
      if (tbody) {
        const row = tbody.children[index]
        if (row) {
          row.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      }
    }, 100)
    return { success: true }
  }
}

// 注册指令
registerAutoHandler('table', 'setRows', (_, cmd) => tableOperations.setRows(cmd.params?.data || []))
registerAutoHandler('table', 'addRow', (_, cmd) => tableOperations.addRow(cmd.params))
registerAutoHandler('table', 'updateRow', (_, cmd) => tableOperations.updateRow(cmd.params))
registerAutoHandler('table', 'removeRow', (_, cmd) => tableOperations.removeRow(cmd.params?.index))
registerAutoHandler('table', 'highlightRow', (_, cmd) => tableOperations.highlightRow(cmd.params?.rowIndex))
registerAutoHandler('table', 'unhighlightRow', (_, cmd) => tableOperations.unhighlightRow(cmd.params?.rowIndex))
registerAutoHandler('table', 'clearHighlight', () => tableOperations.clearHighlight())
registerAutoHandler('table', 'sortBy', (_, cmd) => tableOperations.sortBy(cmd.params?.key, cmd.params?.order))
registerAutoHandler('table', 'scrollTo', (_, cmd) => tableOperations.scrollTo(cmd.params?.index))

/**
 * 初始化表格数据
 */
function initializeTable() {
  const data = props.config.data

  if (data.columns) {
    columns.value = data.columns
  } else if (data.rows && data.rows.length > 0) {
    // 从第一行推断列
    columns.value = Object.keys(data.rows[0]).map(key => ({
      key,
      label: key.toUpperCase()
    }))
  }

  if (data.rows) {
    rows.value = data.rows
  }
}

/**
 * 监听配置数据变化
 */
watch(() => props.config.data, () => {
  initializeTable()
}, { immediate: true })
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.table-panel {
  background: $bg-secondary;
  backdrop-filter: $backdrop-blur;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: $shadow-sm;
  transition: all $transition-fast $ease-out;
  height: 100%;
  min-height: 240px;

  &:hover {
    border-color: $border-medium;
    box-shadow: $shadow-md;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(0, 8, 20, 0.8);
  border-bottom: 1px solid $border-default;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    width: 30%;
    height: 1px;
    background: linear-gradient(
      to right,
      $border-accent,
      transparent
    );
  }

  .panel-title {
    font-family: $font-family-ui;
    font-size: 11px;
    font-weight: $font-weight-semibold;
    color: $color-primary;
    text-transform: uppercase;
    letter-spacing: 1px;
    text-shadow: $text-shadow-sm;
  }

  .panel-actions {
    display: flex;
    gap: 4px;

    .action-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      border: 1px solid $border-subtle;
      background: transparent;
      color: $text-secondary;
      border-radius: $radius-sm;
      cursor: pointer;
      transition: all $transition-fast $ease-out;

      &:hover {
        border-color: $border-accent;
        background: rgba($color-primary, 0.1);
        color: $color-primary;
        box-shadow: $glow-primary;
      }

      &.close:hover {
        border-color: $border-warning;
        background: rgba($terra-warning, 0.1);
        color: $terra-warning;
        box-shadow: $glow-warning;
      }
    }
  }
}

.table-container {
  flex: 1;
  min-height: 0;
  overflow: auto;

  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(0, 8, 20, 0.5);
    border: 1px solid $border-subtle;
    border-radius: $radius-sm;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 212, 255, 0.3);
    border: 1px solid $border-default;
    border-radius: $radius-sm;

    &:hover {
      background: rgba(0, 212, 255, 0.5);
    }
  }
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-family: $font-family-ui;
  font-size: $font-size-small;

  thead {
    position: sticky;
    top: 0;
    background: rgba(0, 8, 20, 0.95);
    backdrop-filter: $backdrop-blur-sm;
    z-index: 1;

    th {
      padding: 8px 12px;
      text-align: left;
      font-weight: $font-weight-semibold;
      color: $color-primary;
      text-transform: uppercase;
      letter-spacing: 1px;
      border-bottom: 1px solid $border-default;
      cursor: pointer;
      user-select: none;
      white-space: nowrap;

      &:hover {
        background: rgba($color-primary, 0.1);
        color: $text-primary;
        text-shadow: $text-shadow-sm;
      }

      .sort-indicator {
        margin-left: 6px;
        color: $border-accent;
        text-shadow: $text-shadow-sm;
      }
    }
  }

  tbody {
    tr {
      border-bottom: 1px solid $border-subtle;
      transition: all $transition-fast $ease-out;
      cursor: pointer;

      &:hover {
        background: rgba($color-primary, 0.05);
        border-color: $border-medium;
      }

      &.highlighted {
        background: rgba($color-highlight, 0.1);
        border-color: rgba($color-highlight, 0.3);

        td {
          color: $color-highlight;
          text-shadow: 0 0 8px rgba($color-highlight, 0.4);
        }
      }

      td {
        padding: 8px 12px;
        color: $text-secondary;
        border-left: 1px solid transparent;

        // 线框风格：列边框
        &:not(:last-child) {
          border-right: 1px solid $border-subtle;
        }
      }
    }
  }
}

.panel-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba($bg-secondary, 0.95);
  backdrop-filter: $backdrop-blur;
  color: $color-primary;
  font-family: $font-family-ui;
  font-size: $font-size-small;
  letter-spacing: 1px;
  text-transform: uppercase;

  .spinner {
    width: 28px;
    height: 28px;
    border: 2px solid $border-default;
    border-top-color: $border-accent;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
