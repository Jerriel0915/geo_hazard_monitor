// ============================================
// 表格面板指令处理器
// ============================================

import type { Ref } from 'vue'
import type { TablePanelData } from '@/types/panels'

/**
 * 表格数据引用
 */
export function useTablePanelCommands(tableData: Ref<TablePanelData | null>) {
  /**
   * 设置行数据
   */
  async function setRows(params: { rows: any[] }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    tableData.value.rows = params.rows
    return { success: true, rowCount: params.rows.length }
  }

  /**
   * 添加行
   */
  async function addRow(params: { row: any; index?: number }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    const { row, index } = params
    if (index !== undefined) {
      tableData.value.rows.splice(index, 0, row)
    } else {
      tableData.value.rows.push(row)
    }

    return { success: true, rowCount: tableData.value.rows.length }
  }

  /**
   * 更新行
   */
  async function updateRow(params: { rowIndex: number; data: any }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    const { rowIndex, data } = params
    if (rowIndex >= 0 && rowIndex < tableData.value.rows.length) {
      tableData.value.rows[rowIndex] = { ...tableData.value.rows[rowIndex], ...data }
      return { success: true, rowIndex }
    }

    throw new Error(`Invalid rowIndex: ${rowIndex}`)
  }

  /**
   * 删除行
   */
  async function removeRow(params: { rowIndex: number }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    const { rowIndex } = params
    if (rowIndex >= 0 && rowIndex < tableData.value.rows.length) {
      const removed = tableData.value.rows.splice(rowIndex, 1)
      return { success: true, removed }
    }

    throw new Error(`Invalid rowIndex: ${rowIndex}`)
  }

  /**
   * 高亮行
   */
  async function highlightRow(params: { rowIndex: number; color?: string }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    const { rowIndex, color } = params
    if (!tableData.value.highlightedRows) {
      tableData.value.highlightedRows = new Map()
    }

    tableData.value.highlightedRows.set(rowIndex, color || '#F97316')
    return { success: true, rowIndex, color }
  }

  /**
   * 取消高亮行
   */
  async function unhighlightRow(params: { rowIndex: number }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    if (tableData.value.highlightedRows) {
      tableData.value.highlightedRows.delete(params.rowIndex)
    }

    return { success: true, rowIndex: params.rowIndex }
  }

  /**
   * 滚动到指定行
   */
  async function scrollTo(params: { rowIndex: number }) {
    // 触发自定义事件，让组件处理滚动
    const event = new CustomEvent('table-scroll-to', { detail: params.rowIndex })
    window.dispatchEvent(event)

    return { success: true, rowIndex: params.rowIndex }
  }

  /**
   * 排序
   */
  async function sortBy(params: { column: string; order: 'asc' | 'desc' }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    const { column, order } = params
    const sorted = [...tableData.value.rows].sort((a, b) => {
      const aVal = a[column]
      const bVal = b[column]

      if (aVal < bVal) return order === 'asc' ? -1 : 1
      if (aVal > bVal) return order === 'asc' ? 1 : -1
      return 0
    })

    tableData.value.rows = sorted
    return { success: true, sortedBy: column, order }
  }

  /**
   * 设置数据
   */
  async function setData(params: { data: any }) {
    if (!tableData.value) {
      throw new Error('Table data not initialized')
    }

    tableData.value = { ...tableData.value, ...params.data }
    return { success: true }
  }

  return {
    // 生命周期
    lifecycle: {
      show: async () => ({ success: true }),
      hide: async () => ({ success: true })
    },

    // 布局
    layout: {
      maximize: async () => ({ success: true }),
      restore: async () => ({ success: true }),
      minimize: async () => ({ success: true })
    },

    // 数据
    data: {
      set: setData,
      update: async (params: any) => ({ success: true }),
      refresh: async () => ({ success: true })
    },

    // 表格特定指令
    table: {
      setRows,
      addRow,
      updateRow,
      removeRow,
      highlightRow,
      unhighlightRow,
      scrollTo,
      sortBy
    }
  }
}
