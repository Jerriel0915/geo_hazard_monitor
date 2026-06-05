<template>
  <div class="report-page">
    <!-- Page header: title only, NO add button -->
    <div class="page-header">
      <h2 class="page-title">监测报告</h2>
    </div>

    <!-- Search bar -->
    <div class="search-bar">
      <el-input v-model="searchKeyword" placeholder="搜索报告标题" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch">
        <template #prefix><span class="search-icon">🔍</span></template>
      </el-input>
      <el-select v-model="searchType" placeholder="报告类型" clearable class="type-select">
        <el-option label="周报" value="weekly" />
        <el-option label="月报" value="monthly" />
      </el-select>
      <el-date-picker v-model="searchDateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" class="date-picker" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- Table -->
    <div class="table-container">
      <el-table :data="tableData" border stripe v-loading="loading" :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontWeight: 'bold' }">
        <el-table-column prop="id" label="报告编号" width="100" align="center" />
        <el-table-column prop="title" label="报告标题" min-width="250" />
        <el-table-column prop="type" label="报告类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'weekly' ? '' : 'success'" effect="plain">
              {{ row.type === 'weekly' ? '周报' : '月报' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报告周期" width="220" align="center">
          <template #default="{ row }">
            {{ row.periodStart }} ~ {{ row.periodEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="180" align="center" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="op-cell">
              <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
              <el-dropdown trigger="hover" @command="(cmd: string) => handleMoreCommand(cmd, row)">
                <el-button type="primary" text size="small">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="pdf">下载PDF</el-dropdown-item>
                    <el-dropdown-item command="print">打印</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <span style="color: #f56c6c">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTableData"
          @current-change="loadTableData"
        />
      </div>
    </div>

    <!-- View dialog -->
    <el-dialog v-model="viewDialogVisible" :title="currentReport?.title" width="900px" destroy-on-close>
      <div class="report-meta">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="报告类型">
            <el-tag :type="currentReport?.type === 'weekly' ? '' : 'success'" size="small">
              {{ currentReport?.type === 'weekly' ? '周报' : '月报' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报告周期">{{ currentReport?.periodStart }} ~ {{ currentReport?.periodEnd }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ currentReport?.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div ref="reportContentRef" class="report-content" v-html="currentReport?.content" />
      <template #footer>
        <el-button @click="handlePrint">打印</el-button>
        <el-button type="primary" @click="handleExportPdfDialog" :loading="pdfLoading">导出PDF</el-button>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import { getReportPage, getReportDetail, deleteReport, type ReportItem } from '@/api/report'

// State
const loading = ref(false)
const tableData = ref<ReportItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchType = ref<'weekly' | 'monthly' | ''>('')
const searchDateRange = ref<[string, string] | null>(null)

// View dialog
const viewDialogVisible = ref(false)
const currentReport = ref<ReportItem | null>(null)
const reportContentRef = ref<HTMLElement>()
const pdfLoading = ref(false)

// Methods
const loadTableData = async () => {
  loading.value = true
  try {
    const data = await getReportPage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      type: searchType.value || undefined,
      startDate: searchDateRange.value?.[0] || undefined,
      endDate: searchDateRange.value?.[1] || undefined
    })
    tableData.value = data.rows || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载报告数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchType.value = ''
  searchDateRange.value = null
  currentPage.value = 1
  loadTableData()
}

const handleView = async (row: ReportItem) => {
  try {
    const detail = await getReportDetail(row.id)
    currentReport.value = detail
    viewDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取报告详情失败')
  }
}

const handleDelete = async (row: ReportItem) => {
  try {
    await ElMessageBox.confirm(`确定删除报告"${row.title}"？`, '提示', { type: 'warning' })
    await deleteReport(row.id)
    ElMessage.success('删除成功')
    loadTableData()
  } catch { /* cancelled */ }
}

const handleMoreCommand = (cmd: string, row: ReportItem) => {
  if (cmd === 'pdf') {
    handleExportPdf(row)
  } else if (cmd === 'print') {
    handleView(row).then(() => {
      nextTick(() => handlePrint())
    })
  } else if (cmd === 'delete') {
    handleDelete(row)
  }
}

const handlePrint = () => {
  window.print()
}

// PDF export from table row (quick download)
const handleExportPdf = async (row: ReportItem) => {
  try {
    const detail = await getReportDetail(row.id)
    currentReport.value = detail
    viewDialogVisible.value = true
    // Wait for dialog to render
    await new Promise(r => setTimeout(r, 300))
    await doExportPdf()
  } catch (error) {
    ElMessage.error('导出PDF失败')
  }
}

// PDF export from dialog
const handleExportPdfDialog = async () => {
  if (!reportContentRef.value) return
  pdfLoading.value = true
  try {
    await doExportPdf()
  } catch (error) {
    ElMessage.error('导出PDF失败')
  } finally {
    pdfLoading.value = false
  }
}

const doExportPdf = async () => {
  if (!reportContentRef.value || !currentReport.value) return
  const canvas = await html2canvas(reportContentRef.value, {
    scale: 2,
    useCORS: true,
    logging: false
  })
  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pdfWidth = pdf.internal.pageSize.getWidth()
  const pdfHeight = (canvas.height * pdfWidth) / canvas.width
  pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight)
  pdf.save(`${currentReport.value.title}.pdf`)
}

onMounted(() => {
  loadTableData()
})
</script>

<style scoped>
.report-page {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100% - 40px);
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
}
.search-input {
  width: 220px;
}
.type-select {
  width: 120px;
}
.date-picker {
  width: 280px;
}
.search-icon {
  font-size: 14px;
}
.table-container {
  background: #fff;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
.report-meta {
  margin-bottom: 20px;
}
.report-content {
  padding: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  max-height: 60vh;
  overflow-y: auto;
  line-height: 1.8;
}
/* Rich text content styles */
.report-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
}
.report-content :deep(th),
.report-content :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 8px 12px;
  text-align: center;
}
.report-content :deep(th) {
  background: #f5f7fa;
  font-weight: bold;
}
.report-content :deep(h2) {
  margin: 15px 0 10px;
  font-size: 16px;
  color: #303133;
}
.report-content :deep(p) {
  margin: 8px 0;
}
.op-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}
</style>
