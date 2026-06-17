<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">监测报告</h2>
        <span class="header__subtitle">周期性监测报告生成与管理</span>
      </div>
      <div class="header__right">
        <el-button v-if="hasPermission('report:record:generate')" type="success" @click="showGenerateDialog = true">
          手动生成
        </el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="searchKeyword" placeholder="搜索报告名称" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
      <el-select v-model="searchType" placeholder="报告类型" clearable>
        <el-option label="周报" value="weekly" />
        <el-option label="月报" value="monthly" />
        <el-option label="季报" value="quarterly" />
      </el-select>
      <el-select v-model="searchStatus" placeholder="状态" clearable>
        <el-option label="生成中" :value="1" />
        <el-option label="已生成" :value="2" />
        <el-option label="生成失败" :value="3" />
      </el-select>
      <el-date-picker v-model="searchDateRange" type="daterange" start-placeholder="周期起始" end-placeholder="周期截止" value-format="YYYY-MM-DD" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table :data="tableData" border stripe v-loading="loading">
          <el-table-column prop="reportName" label="报告名称" min-width="280" show-overflow-tooltip />
          <el-table-column prop="type" label="报告类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="typeTagType(row.type)" effect="plain">
                {{ row.typeDesc || typeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="hazardPointName" label="隐患点" min-width="150" show-overflow-tooltip />
          <el-table-column label="报告周期" width="200" align="center">
            <template #default="{ row }">
              {{ row.periodStart }} ~ {{ row.periodEnd }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tooltip v-if="row.status === 3 && row.errorMsg" :content="row.errorMsg" placement="top">
                <el-tag :type="statusTagType(row.status)" effect="plain">
                  {{ row.statusDesc || statusLabel(row.status) }}
                </el-tag>
              </el-tooltip>
              <el-tag v-else :type="statusTagType(row.status)" effect="plain">
                {{ row.statusDesc || statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="生成时间" min-width="170" align="center" />
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
                <el-dropdown trigger="hover" @command="(cmd: string) => handleMoreCommand(cmd, row)">
                  <el-button type="primary" text size="small">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-item command="pdf" :disabled="row.status !== 2">下载PDF</el-dropdown-item>
                    <el-dropdown-item command="print">打印</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <span style="color: #f56c6c">删除</span>
                    </el-dropdown-item>
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
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- 手动生成弹窗 -->
    <el-dialog v-model="showGenerateDialog" title="手动生成报告" width="520px" destroy-on-close>
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="报告类型" required>
          <el-select v-model="generateForm.type" placeholder="请选择报告类型" style="width: 100%">
            <el-option label="周报" value="weekly" />
            <el-option label="月报" value="monthly" />
            <el-option label="季报" value="quarterly" />
          </el-select>
        </el-form-item>
        <el-form-item label="隐患点" required>
          <el-select v-model="generateForm.hazardPointId" placeholder="请选择隐患点" filterable style="width: 100%">
            <el-option v-for="hp in hazardOptions" :key="hp.id" :label="hp.name" :value="hp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期起始" required>
          <el-date-picker v-model="generateForm.periodStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="周期截止" required>
          <el-date-picker v-model="generateForm.periodEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- View dialog -->
    <el-dialog v-model="viewDialogVisible" :title="currentReport?.reportName || '报告详情'" width="900px" destroy-on-close>
      <div class="report-meta">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="报告类型">
            <el-tag :type="typeTagType(currentReport?.type || 'weekly')" size="small">
              {{ currentReport?.typeDesc || typeLabel(currentReport?.type || 'weekly') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="隐患点">{{ currentReport?.hazardPointName }}</el-descriptions-item>
          <el-descriptions-item label="报告周期">{{ currentReport?.periodStart }} ~ {{ currentReport?.periodEnd }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentReport?.status || 1)" size="small">
              {{ currentReport?.statusDesc || statusLabel(currentReport?.status || 1) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ currentReport?.createTime }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" v-if="currentReport?.errorMsg">
            <span style="color: #f56c6c">{{ currentReport?.errorMsg }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div ref="reportContentRef" class="report-content" v-html="currentReport?.content || '<p style=\'color:#909399\'>暂无报告内容</p>'" />
      <template #footer>
        <el-button @click="handlePrint">打印</el-button>
        <el-button type="primary" @click="handleExportPdfDialog" :loading="pdfLoading" :disabled="currentReport?.status !== 2">导出PDF</el-button>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {nextTick, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import {hasPermission} from '@/utils/permission'
import {
  getReportPage,
  getReportDetail,
  deleteReport,
  generateReport,
  getHazardPointOptions,
  type ReportItem,
  type ReportType,
  type ReportPageParams,
  type HazardPointOption,
} from '@/api/report'

// State
const loading = ref(false)
const tableData = ref<ReportItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchType = ref<ReportType | ''>('')
const searchStatus = ref<number | undefined>(undefined)
const searchDateRange = ref<[string, string] | null>(null)

// Generate dialog
const showGenerateDialog = ref(false)
const hazardOptions = ref<HazardPointOption[]>([])
const generating = ref(false)
const generateForm = reactive<{
  type: ReportType | ''
  hazardPointId: number | ''
  periodStart: string
  periodEnd: string
}>({
  type: '',
  hazardPointId: '',
  periodStart: '',
  periodEnd: '',
})

// View dialog
const viewDialogVisible = ref(false)
const currentReport = ref<ReportItem | null>(null)
const reportContentRef = ref<HTMLElement>()
const pdfLoading = ref(false)

// Helpers
function typeLabel(t: ReportType): string {
  const map: Record<ReportType, string> = { weekly: '周报', monthly: '月报', quarterly: '季报' }
  return map[t] ?? t
}

function typeTagType(t: ReportType): 'success' | 'warning' | 'danger' | '' {
  const map: Record<ReportType, 'success' | 'warning' | 'danger' | ''> = {
    weekly: 'success',
    monthly: 'warning',
    quarterly: 'danger',
  }
  return map[t] ?? ''
}

function statusLabel(s: number): string {
  const map: Record<number, string> = { 1: '生成中', 2: '已生成', 3: '生成失败' }
  return map[s] ?? '未知'
}

function statusTagType(s: number): 'info' | 'success' | 'danger' | '' {
  const map: Record<number, 'info' | 'success' | 'danger' | ''> = {
    1: 'info',
    2: 'success',
    3: 'danger',
  }
  return map[s] ?? ''
}

// Methods
const loadList = async () => {
  loading.value = true
  try {
    const params: ReportPageParams = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      type: (searchType.value as ReportType) || undefined,
      status: searchStatus.value,
      periodStart: searchDateRange.value?.[0] || undefined,
      periodEnd: searchDateRange.value?.[1] || undefined,
    }
    const result = await getReportPage(params)
    tableData.value = result.rows
    total.value = result.total
  } catch (error: any) {
    showRequestErrorMessage(error, '加载报告数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchType.value = ''
  searchStatus.value = undefined
  searchDateRange.value = null
  currentPage.value = 1
  loadList()
}

const handleView = async (row: ReportItem) => {
  try {
    const detail = await getReportDetail(row.id)
    currentReport.value = detail
    viewDialogVisible.value = true
  } catch (error) {
    showRequestErrorMessage(error, '获取报告详情失败')
  }
}

const handleDelete = async (row: ReportItem) => {
  try {
    await ElMessageBox.confirm(`确定删除报告"${row.reportName}"？`, '提示', { type: 'warning' })
    await deleteReport(row.id)
    ElMessage.success('删除成功')
    loadList()
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
    showRequestErrorMessage(error, '导出PDF失败')
  }
}

// PDF export from dialog
const handleExportPdfDialog = async () => {
  if (!reportContentRef.value || !currentReport.value) return
  pdfLoading.value = true
  try {
    await doExportPdf()
  } catch (error) {
    showRequestErrorMessage(error, '导出PDF失败')
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
  pdf.save(`${currentReport.value.reportName || '监测报告'}.pdf`)
}

// Manual generate
const handleGenerate = async () => {
  if (!generateForm.type) {
    ElMessage.warning('请选择报告类型')
    return
  }
  if (!generateForm.hazardPointId) {
    ElMessage.warning('请选择隐患点')
    return
  }
  if (!generateForm.periodStart || !generateForm.periodEnd) {
    ElMessage.warning('请选择报告周期')
    return
  }
  if (generateForm.periodStart >= generateForm.periodEnd) {
    ElMessage.warning('周期起始不能晚于周期截止')
    return
  }
  generating.value = true
  try {
    const result = await generateReport({
      type: generateForm.type,
      hazardPointId: generateForm.hazardPointId as number,
      periodStart: generateForm.periodStart,
      periodEnd: generateForm.periodEnd,
    })
    if (result.existed) {
      ElMessage.warning(`该报告已存在 (ID: ${result.reportId})，请勿重复生成`)
    } else {
      ElMessage.success(`报告生成成功 (ID: ${result.reportId})`)
    }
    showGenerateDialog.value = false
    loadList()
  } catch (error: any) {
    showRequestErrorMessage(error, '生成报告失败')
  } finally {
    generating.value = false
  }
}

onMounted(async () => {
  try { hazardOptions.value = await getHazardPointOptions() } catch { /* ignore */ }
  loadList()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header__left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.header__title {
  margin: 0;
}
.header__subtitle {
  color: #909399;
  font-size: 14px;
}
.header__right {
  display: flex;
  gap: 8px;
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
.search {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.table-wrap {
  margin-top: 16px;
}
.table-wrap__scroll {
  overflow-x: auto;
}
.table-wrap__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.op-cell {
  display: flex;
  justify-content: center;
  gap: 4px;
}
</style>
