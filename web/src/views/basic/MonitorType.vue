<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">监测类型管理</h2>
        <span class="header__subtitle">监测大类、类型与监测内容字典维护</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">+ 新增</el-button>
        <el-button @click="handleExport" :disabled="loading">导出</el-button>
      </div>
    </div>

    <div class="search">
      <el-select v-model="searchType">
        <el-option label="按编号" value="code" />
        <el-option label="按名称" value="name" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        :placeholder="searchType === 'code' ? '搜索监测类型编号' : '搜索监测类型名称'"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchStatus" clearable placeholder="状态">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table
          :data="sort.sorted(tableData)"
          border
          stripe
          v-loading="loading"
        >
          <el-table-column prop="code" label="编号" width="120" align="center">
            <template #header>
              <TableSortHeader label="编号" :order="sortInfo.order && sortInfo.field === 'code' ? sortInfo.order : ''" @toggle="sort.toggle('code')" />
            </template>
          </el-table-column>
          <el-table-column label="图标" width="80" align="center">
            <template #default="{ row }">
              <img v-if="row.icon" :src="row.icon" class="table-icon" alt="icon" />
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="160" align="center">
            <template #header>
              <TableSortHeader label="名称" :order="sortInfo.order && sortInfo.field === 'name' ? sortInfo.order : ''" @toggle="sort.toggle('name')" />
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" align="center">
            <template #default="{ row }">
              <span>{{ row.description || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #header>
              <TableSortHeader label="状态" :order="sortInfo.order && sortInfo.field === 'status' ? sortInfo.order : ''" @toggle="sort.toggle('status')" />
            </template>
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :loading="statusTogglingId === row.id"
                active-text="启用"
                inactive-text="停用"
                inline-prompt
                @change="(val: boolean) => handleStatusChange(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createBy" label="创建人" width="100" align="center" />
          <el-table-column prop="createTime" label="创建时间" min-width="170" align="center">
            <template #header>
              <TableSortHeader label="创建时间" :order="sortInfo.order && sortInfo.field === 'createTime' ? sortInfo.order : ''" @toggle="sort.toggle('createTime')" />
            </template>
          </el-table-column>
          <el-table-column prop="updateBy" label="修改人" width="100" align="center" />
          <el-table-column prop="updateTime" label="修改时间" min-width="170" align="center">
            <template #header>
              <TableSortHeader label="修改时间" :order="sortInfo.order && sortInfo.field === 'updateTime' ? sortInfo.order : ''" @toggle="sort.toggle('updateTime')" />
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序号" width="90" align="center">
            <template #header>
              <TableSortHeader label="排序号" :order="sortInfo.order && sortInfo.field === 'sortOrder' ? sortInfo.order : ''" @toggle="sort.toggle('sortOrder')" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
                <el-dropdown trigger="click" @command="(cmd: string) => handleRowCommand(cmd, row)">
                  <el-button type="primary" text size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">编辑</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <span class="danger-text">删除</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          prev-text="上一页"
          next-text="下一页"
          :disabled="total === 0"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <MonitorTypeDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :monitor-type-id="selectedId"
      @saved="loadTableData"
    />
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {ArrowDown} from '@element-plus/icons-vue'
import TableSortHeader from '@/components/TableSortHeader.vue'
import {useTableSort} from '@/composables/useTableSort'
import {
  getMonitorTypeListFiltered,
  getMonitorTypePage,
  type MonitorTypeItem,
  removeMonitorType,
  toggleMonitorTypeStatus
} from '@/api/monitorType'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import MonitorTypeDialog from './components/MonitorTypeDialog.vue'

type SearchType = 'code' | 'name'

const sort = useTableSort()
const sortInfo = sort.sortInfo

const loading = ref(false)
const tableData = ref<MonitorTypeItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchType = ref<SearchType>('name')
const searchKeyword = ref('')
const searchStatus = ref<number | ''>('')
const statusTogglingId = ref<number | null>(null)

// 弹窗组件状态
const dialogVisible = ref(false)
const dialogMode = ref<'view' | 'edit' | 'create'>('create')
const selectedId = ref<number | undefined>(undefined)

const normalizeMonitorType = (item: any): MonitorTypeItem => ({
  id: Number(item?.id),
  code: String(item?.code || ''),
  name: String(item?.name || ''),
  icon: String(item?.icon || ''),
  description: String(item?.description || ''),
  sortOrder: Number(item?.sortOrder ?? 0),
  status: Number(item?.status ?? 1),
  createTime: String(item?.createTime || ''),
  updateTime: String(item?.updateTime || ''),
  createBy: String(item?.createBy || ''),
  updateBy: String(item?.updateBy || ''),
  contents: item?.contents
})

const buildQueryParams = () => {
  const params: Record<string, any> = {
    pageNum: currentPage.value,
    pageSize: pageSize.value
  }

  const keyword = searchKeyword.value.trim()
  if (keyword) {
    params[searchType.value] = keyword
  }
  if (searchStatus.value !== '') {
    params.status = searchStatus.value
  }
  return params
}

const loadTableData = async () => {
  loading.value = true
  try {
    const data = await getMonitorTypePage(buildQueryParams())
    tableData.value = (data.rows || []).map(normalizeMonitorType)
    total.value = Number(data.total || 0)
  } catch (error) {
    console.error('获取监测类型列表失败:', error)
    showRequestErrorMessage(error, '获取监测类型列表失败')
  } finally {
    loading.value = false
  }
}

const downloadCsv = (fileName: string, rows: Array<Array<string | number>>) => {
  const formatCsvCell = (value: string | number) => `"${String(value ?? '').replace(/"/g, '""')}"`
  const csv = `﻿${rows.map((row) => row.map(formatCsvCell).join(',')).join('\n')}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleReset = () => {
  searchType.value = 'name'
  searchKeyword.value = ''
  searchStatus.value = ''
  currentPage.value = 1
  loadTableData()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadTableData()
}

const handlePageChange = () => {
  loadTableData()
}

const handleAdd = () => {
  dialogMode.value = 'create'
  selectedId.value = undefined
  dialogVisible.value = true
}

const handleEdit = (row: MonitorTypeItem) => {
  dialogMode.value = 'edit'
  selectedId.value = row.id
  dialogVisible.value = true
}

const handleView = (row: MonitorTypeItem) => {
  dialogMode.value = 'view'
  selectedId.value = row.id
  dialogVisible.value = true
}

const handleDelete = (row: MonitorTypeItem) => {
  ElMessageBox.confirm(
    `删除监测类型「${row.name}」后，该类型下的所有监测内容将被一并移除，历史数据保留但不可查询，新数据也不再按该类型字段解析。操作不可恢复，确定删除？`,
    '删除监测类型',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await removeMonitorType(row.id)
      ElMessage.success(`已删除监测类型「${row.name}」`)
      loadTableData()
    } catch (error) {
      showRequestErrorMessage(error, '删除监测类型失败')
    }
  }).catch(() => {})
}

const handleRowCommand = (cmd: string, row: MonitorTypeItem) => {
  if (cmd === 'edit') {
    handleEdit(row)
  } else if (cmd === 'delete') {
    handleDelete(row)
  }
}

const handleStatusChange = (row: MonitorTypeItem, newVal: boolean) => {
  const newStatus = newVal ? 1 : 0
  const actionText = newVal ? '启用' : '停用'
  const tipText = newVal
    ? `确定要启用监测类型"${row.name}"吗？`
    : `确定要停用监测类型"${row.name}"吗？停用后新建传感器将无法选用该类型。`

  ElMessageBox.confirm(tipText, `${actionText}确认`, {
    confirmButtonText: actionText,
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    statusTogglingId.value = row.id
    try {
      await toggleMonitorTypeStatus(row.id, newStatus)
      row.status = newStatus
      ElMessage.success(`已${actionText}`)
    } catch (error) {
      showRequestErrorMessage(error, '状态切换失败')
    } finally {
      statusTogglingId.value = null
    }
  }).catch(() => {})
}

const handleExport = async () => {
  try {
    const keyword = searchKeyword.value.trim()
    const status = searchStatus.value === '' ? undefined : searchStatus.value
    const params: Record<string, any> = {}
    if (keyword) params[searchType.value] = keyword
    if (status !== undefined) params.status = status
    const data = await getMonitorTypeListFiltered(params)
    const all = (Array.isArray(data) ? data : []).map(normalizeMonitorType)
    const rows = [
      ['编号', '名称', '描述', '排序号', '状态', '创建人', '创建时间', '修改人', '修改时间'],
      ...all.map((item: MonitorTypeItem) => [
        item.code,
        item.name,
        item.description || '',
        item.sortOrder,
        item.status === 1 ? '启用' : '禁用',
        item.createBy || '',
        item.createTime || '',
        item.updateBy || '',
        item.updateTime || ''
      ])
    ]
    downloadCsv(`monitor-types-${Date.now()}.csv`, rows)
    ElMessage.success(`导出成功，共 ${all.length} 条`)
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  loadTableData()
})
</script>

<style scoped>
.table-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.danger-text {
  color: #f56c6c;
}
</style>
