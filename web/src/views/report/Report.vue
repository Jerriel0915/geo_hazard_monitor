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
        <el-button v-if="hasPermission('report:record:generate')" type="primary" @click="showBatchDialog = true">
          一键生成
        </el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="searchKeyword" placeholder="搜索报告名称" clearable style="width: 180px" @clear="handleSearch" @keyup.enter="handleSearch" />
      <el-select v-model="searchType" placeholder="报告类型" clearable style="width: 120px">
        <el-option label="周报" value="weekly" />
        <el-option label="月报" value="monthly" />
        <el-option label="季报" value="quarterly" />
      </el-select>
      <el-select v-model="searchStatus" placeholder="状态" clearable style="width: 110px">
        <el-option label="生成中" :value="1" />
        <el-option label="已生成" :value="2" />
        <el-option label="生成失败" :value="3" />
      </el-select>
      <el-date-picker
        v-model="searchDateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="周期起始"
        end-placeholder="周期截止"
        style="width: 260px"
        value-format="YYYY-MM-DD HH:mm:ss"
        :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table 
          :data="sortedTableData" 
          border 
          stripe 
          v-loading="loading"
          @sort-change="handleSortChange"
        >
          <el-table-column prop="reportName" label="报告名称" min-width="280" show-overflow-tooltip />
          <el-table-column prop="type" label="报告类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="typeTagType(row.type)" effect="plain">
                {{ row.typeDesc || typeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="hazardPointName" label="隐患点" min-width="150" show-overflow-tooltip />
          <!-- 报告周期列 - 移除 sortable，不需要排序 -->
          <el-table-column prop="periodStart" label="报告周期" width="200" align="center">
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
          <!-- 生成时间列 - 保留 sortable -->
          <el-table-column prop="createTime" label="生成时间" width="190" align="center" sortable="custom">
            <template #default="{ row }">
              {{ formatBjTime(row.createTime) }}
            </template>
          </el-table-column>
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
    <el-dialog v-model="showGenerateDialog" title="手动生成报告" width="480px" destroy-on-close>
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

    <!-- 批量一键生成弹窗 -->
    <el-dialog v-model="showBatchDialog" title="一键生成报告" width="480px" destroy-on-close>
      <el-form :model="batchForm" label-width="110px">
        <el-form-item label="报告类型" required>
          <el-select v-model="batchForm.type" placeholder="请选择报告类型" style="width: 100%">
            <el-option label="周报" value="weekly" />
            <el-option label="月报" value="monthly" />
            <el-option label="季报" value="quarterly" />
          </el-select>
        </el-form-item>
        <el-form-item label="参考日期">
          <el-date-picker v-model="batchForm.referenceDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" style="width: 100%" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px">留空则以当天为参考生成上一个完整周期的报告（与定时任务逻辑一致）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" :loading="batchGenerating" @click="handleBatchGenerate">开始批量生成</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗 -->
    <el-dialog
      v-model="viewDialogVisible"
      :title="currentReport?.reportName || '报告详情'"
      width="900px"
      destroy-on-close
      class="report-view-dialog"
    >
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
          <el-descriptions-item label="生成时间">{{ formatBjTime(currentReport?.createTime || '') }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" v-if="currentReport?.errorMsg">
            <span style="color: #f56c6c">{{ currentReport?.errorMsg }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div ref="reportContentRef" class="report-content" v-html="formattedContent || '<p style=\'color:#909399\'>暂无报告内容</p>'" />
      <template #footer>
        <el-button @click="handlePrint">打印</el-button>
        <el-button type="primary" @click="handleExportPdfDialog" :loading="pdfLoading" :disabled="currentReport?.status !== 2">导出PDF</el-button>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {nextTick, onMounted, reactive, ref, computed} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import {showRequestErrorMessage} from '@/utils/errorHandler'
import {hasPermission} from '@/utils/permission'
import {
  getReportPage, getReportDetail, deleteReport,
  generateReport, generateAllReports, getHazardPointOptions,
  type ReportItem, type ReportType, type ReportPageParams, type HazardPointOption,
} from '@/api/report'

const loading = ref(false)
const tableData = ref<ReportItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchType = ref<ReportType | ''>('')
const searchStatus = ref<number | undefined>(undefined)
const searchDateRange = ref<[string, string] | null>(null)

const showGenerateDialog = ref(false)
const hazardOptions = ref<HazardPointOption[]>([])
const generating = ref(false)
const generateForm = reactive<{ type: ReportType | ''; hazardPointId: number | ''; periodStart: string; periodEnd: string }>(
  { type: '', hazardPointId: '', periodStart: '', periodEnd: '' })

const viewDialogVisible = ref(false)
const currentReport = ref<ReportItem | null>(null)
const reportContentRef = ref<HTMLElement>()
const pdfLoading = ref(false)

// 清洗后端返回 HTML 中的 ISO 时间戳 → 北京时间格式（不修改后端）
const formattedContent = computed(() => {
  const raw = currentReport.value?.content
  if (!raw) return ''
  return raw.replace(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?/g, (match) => {
    const d = new Date(match)
    if (isNaN(d.getTime())) return match
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日 ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  })
})

function typeLabel(t: ReportType): string { return ({ weekly: '周报', monthly: '月报', quarterly: '季报' } as const)[t] ?? t }
function typeTagType(t: ReportType): string { return ({ weekly: 'success', monthly: 'warning', quarterly: 'danger' } as const)[t] ?? '' }
function statusLabel(s: number): string { return ({ 1: '生成中', 2: '已生成', 3: '生成失败' } as const)[s] ?? '未知' }
function statusTagType(s: number): string { return ({ 1: 'info', 2: 'success', 3: 'danger' } as const)[s] ?? '' }

// 北京时间格式化（后端返回的 createTime 为 yyyy-MM-dd HH:mm:ss 或 ISO）
function formatBjTime(raw: string): string {
  if (!raw) return '-'
  const d = new Date(raw.replace(' ', 'T'))
  if (isNaN(d.getTime())) return raw
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日 ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// 排序状态（只对生成时间排序）
const sortState = ref<{ prop: string; order: 'ascending' | 'descending' | null }>({ prop: '', order: null })

// 前端本地排序
const sortedTableData = computed(() => {
  const rows = [...tableData.value]
  const { prop, order } = sortState.value
  if (!prop || !order) return rows
  return rows.sort((a: any, b: any) => {
    const va = a[prop] ?? ''
    const vb = b[prop] ?? ''
    return order === 'ascending' 
      ? String(va).localeCompare(String(vb)) 
      : String(vb).localeCompare(String(va))
  })
})

// 排序变化事件
const handleSortChange = ({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) => {
  sortState.value = { prop, order }
}

const loadList = async () => {
  loading.value = true
  try {
    const params: ReportPageParams = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      type: (searchType.value as ReportType) || undefined,
      status: searchStatus.value,
    }
    if (searchDateRange.value?.length === 2) {
      params.periodStart = searchDateRange.value[0]
      params.periodEnd = searchDateRange.value[1]
    }
    const result = await getReportPage(params)
    tableData.value = result.rows; total.value = result.total
  } catch (error: any) { showRequestErrorMessage(error, '加载报告数据失败') }
  finally { loading.value = false }
}

const handleSearch = () => { currentPage.value = 1; loadList() }
const handleReset = () => {
  searchKeyword.value = ''
  searchType.value = ''
  searchStatus.value = undefined
  searchDateRange.value = null
  currentPage.value = 1
  loadList()
}

const handleView = async (row: ReportItem) => {
  try { currentReport.value = await getReportDetail(row.id); viewDialogVisible.value = true }
  catch (error) { showRequestErrorMessage(error, '获取报告详情失败') }
}

const handleDelete = async (row: ReportItem) => {
  try {
    await ElMessageBox.confirm(`确定删除报告"${row.reportName}"？`, '提示', { type: 'warning' })
    await deleteReport(row.id); ElMessage.success('删除成功'); loadList()
  } catch { /* cancelled */ }
}

const handleMoreCommand = (cmd: string, row: ReportItem) => {
  if (cmd === 'pdf') handleExportPdf(row)
  else if (cmd === 'print') handleView(row).then(() => nextTick(() => handlePrint()))
  else if (cmd === 'delete') handleDelete(row)
}

// ── 打印：克隆报告内容到独立 window，绕过 scoped CSS ──
const handlePrint = () => {
  document.querySelectorAll('#__print_frame').forEach(el => el.remove())

  const r = currentReport.value
  const title = r?.reportName || '监测报告'
  const contentHtml = formattedContent.value || '<p style="color:#909399">暂无报告内容</p>'
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const bjTime = `${now.getFullYear()}年${pad(now.getMonth() + 1)}月${pad(now.getDate())}日 ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`

  const iframe = document.createElement('iframe')
  iframe.id = '__print_frame'
  iframe.style.cssText = 'position:fixed;top:-9999px;left:-9999px;width:1px;height:1px;border:none;'
  document.body.appendChild(iframe)

  const doc = iframe.contentDocument || iframe.contentWindow!.document
  doc.open()
  doc.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>${title}</title>
<style>
  * { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; background: white; }
  body {
    font-family: "Microsoft YaHei", "PingFang SC", "Helvetica Neue", Arial, sans-serif;
    font-size: 15px; line-height: 1.9; color: #1d2129;
    padding: 0 14mm;
  }
  .print-header {
    display: flex; justify-content: space-between; align-items: baseline;
    padding: 8mm 0 4mm 0;
    margin-bottom: 6px;
    border-bottom: 1px solid #e0e0e0;
  }
  .print-header__title { font-size: 13px; font-weight: 600; color: #333; }
  .print-header__time { font-size: 12px; color: #666; }
  .print-footer {
    text-align: center; font-size: 11px; color: #999;
    padding: 6mm 0 4mm 0; margin-top: 20px;
    border-top: 1px solid #e8e8e8;
  }
  .print-body h2 { font-size: 20px; margin: 16px 0 10px; color: #303133; }
  .print-body h3 { font-size: 17px; margin: 12px 0 8px; color: #303133; }
  .print-body table { width: 100%; border-collapse: collapse; margin: 10px 0; }
  .print-body th, .print-body td {
    border: 1px solid #dcdfe6; padding: 6px 10px; text-align: center; font-size: 14px;
  }
  .print-body th { background: #f5f7fa; font-weight: bold; }
  .print-body p, .print-body li, .print-body span { font-size: 15px; }
  .print-body img { max-width: 100%; height: auto; }
  @page {
    margin: 20mm 14mm 15mm 14mm;
    size: A4;
  }
  @media print {
    html, body { margin: 0 !important; padding: 0 !important; }
  }
</style></head><body>
<div class="print-header">
  <span class="print-header__title">${title}</span>
  <span class="print-header__time">${bjTime}</span>
</div>
<div class="print-body">${contentHtml}</div>
<div class="print-footer">第 <span class="page-number"></span> 页</div>
</body></html>`)
  doc.close()

  setTimeout(() => {
    iframe.contentWindow?.focus()
    iframe.contentWindow?.print()
  }, 400)
}

// ── PDF 导出 ──

/** 构建隐藏截图容器 — 样式与预览 .report-content 完全一致 */
const buildCaptureContainer = (html: string): HTMLElement => {
  document.getElementById('__pdf_capture')?.remove()
  const wrap = document.createElement('div')
  wrap.id = '__pdf_capture'
  wrap.style.cssText = 'position:fixed;left:-9999px;top:0;width:840px;z-index:-1;'
  // 内联样式精确匹配预览中的 scoped .report-content 及其 :deep() 子选择器
  wrap.innerHTML = `<style>
    .capture-content { padding: 20px; border: 1px solid #e8e8e8; border-radius: 4px; line-height: 1.8; background: #fff; }
    .capture-content table { width: 100%; border-collapse: collapse; margin: 10px 0; }
    .capture-content th, .capture-content td { border: 1px solid #dcdfe6; padding: 8px 12px; text-align: center; }
    .capture-content th { background: #f5f7fa; font-weight: bold; }
    .capture-content h2 { margin: 15px 0 10px; font-size: 16px; color: #303133; }
    .capture-content h3 { margin: 12px 0 8px; font-size: 15px; color: #303133; }
    .capture-content p { margin: 8px 0; font-size: 15px; }
    .capture-content li, .capture-content span { font-size: 15px; }
    .capture-content img { max-width: 100%; height: auto; }
  </style>
  <div class="capture-content">${html}</div>`
  document.body.appendChild(wrap)
  return wrap.querySelector('.capture-content')!
}

/** 将 canvas 按 A4 分页输出 PDF */
const canvasToPdf = (canvas: HTMLCanvasElement, fileName: string) => {
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pageW = pdf.internal.pageSize.getWidth()
  const pageH = pdf.internal.pageSize.getHeight()
  const margin = 12
  const contentW = pageW - margin * 2
  const contentHPerPage = pageH - margin * 2
  const totalContentH = (canvas.height * contentW) / canvas.width

  const pageCount = Math.ceil(totalContentH / contentHPerPage)

  for (let i = 0; i < pageCount; i++) {
    if (i > 0) pdf.addPage()

    const srcY = Math.floor((i * contentHPerPage / totalContentH) * canvas.height)
    const slicePxH = Math.min(
      Math.floor((contentHPerPage / totalContentH) * canvas.height),
      canvas.height - srcY,
    )

    const pageCanvas = document.createElement('canvas')
    pageCanvas.width = canvas.width
    pageCanvas.height = slicePxH
    const ctx = pageCanvas.getContext('2d')!
    ctx.drawImage(canvas, 0, srcY, canvas.width, slicePxH, 0, 0, canvas.width, slicePxH)

    const sliceMmH = (slicePxH * contentW) / canvas.width
    pdf.addImage(pageCanvas.toDataURL('image/png'), 'PNG', margin, margin, contentW, sliceMmH)

    // 页码
    pdf.setFontSize(9)
    pdf.setTextColor(150, 150, 150)
    pdf.text(`${i + 1} / ${pageCount}`, pageW / 2, pageH - 6, { align: 'center' })
  }

  pdf.save(`${fileName}.pdf`)
}

/** 从列表「更多→下载PDF」直接导出（不弹窗） */
const handleExportPdf = async (row: ReportItem) => {
  try {
    const report = await getReportDetail(row.id)
    currentReport.value = report
    const html = formattedContent.value || '<p style="color:#909399">暂无报告内容</p>'
    const captureEl = buildCaptureContainer(html)
    // 等待图片等资源加载
    await new Promise(r => setTimeout(r, 250))
    const canvas = await html2canvas(captureEl, { scale: 2, useCORS: true, logging: false, backgroundColor: '#ffffff' })
    canvasToPdf(canvas, report?.reportName || '监测报告')
    document.getElementById('__pdf_capture')?.remove()
  } catch (error) { showRequestErrorMessage(error, '导出PDF失败') }
}

/** 从弹窗底部「导出PDF」按钮导出（弹窗已打开，直接截取预览内容） */
const handleExportPdfDialog = async () => {
  if (!reportContentRef.value || !currentReport.value) return
  pdfLoading.value = true
  const el = reportContentRef.value
  // 暂时移除 max-height/overflow 限制以保证截取完整内容
  const orig = { maxHeight: el.style.maxHeight, overflow: el.style.overflow }
  el.style.maxHeight = 'none'
  el.style.overflow = 'visible'
  try {
    await new Promise(r => setTimeout(r, 100))
    const canvas = await html2canvas(el, { scale: 2, useCORS: true, logging: false, backgroundColor: '#ffffff' })
    canvasToPdf(canvas, currentReport.value.reportName || '监测报告')
  } catch (error) { showRequestErrorMessage(error, '导出PDF失败') }
  finally {
    el.style.maxHeight = orig.maxHeight
    el.style.overflow = orig.overflow
    pdfLoading.value = false
  }
}

// ── 生成报告 ──
const handleGenerate = async () => {
  if (!generateForm.type) return ElMessage.warning('请选择报告类型')
  if (!generateForm.hazardPointId) return ElMessage.warning('请选择隐患点')
  if (!generateForm.periodStart || !generateForm.periodEnd) return ElMessage.warning('请选择报告周期')
  if (generateForm.periodStart >= generateForm.periodEnd) return ElMessage.warning('周期起始不能晚于周期截止')
  generating.value = true
  try {
    const result = await generateReport({
      type: generateForm.type,
      hazardPointId: generateForm.hazardPointId as number,
      periodStart: generateForm.periodStart,
      periodEnd: generateForm.periodEnd,
    })
    if (result.existed) ElMessage.warning(`该报告已存在 (ID: ${result.reportId})，请勿重复生成`)
    else ElMessage.success(`报告生成成功 (ID: ${result.reportId})`)
    showGenerateDialog.value = false; loadList()
  } catch (error: any) { showRequestErrorMessage(error, '生成报告失败') }
  finally { generating.value = false }
}

// ── 批量生成 ──
const showBatchDialog = ref(false)
const batchGenerating = ref(false)
const batchForm = reactive<{ type: ReportType | ''; referenceDate: string }>({ type: '', referenceDate: '' })

const handleBatchGenerate = async () => {
  if (!batchForm.type) return ElMessage.warning('请选择报告类型')
  batchGenerating.value = true
  try {
    await generateAllReports(batchForm.type, batchForm.referenceDate || undefined)
    ElMessage.success('批量生成已触发，稍后刷新查看结果')
    showBatchDialog.value = false; loadList()
  } catch (error: any) { showRequestErrorMessage(error, '批量生成失败') }
  finally { batchGenerating.value = false }
}

onMounted(async () => {
  try { hazardOptions.value = await getHazardPointOptions() } catch { /* ignore */ }
  loadList()
})
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.header__left { display: flex; align-items: baseline; gap: 12px; }
.header__title { margin: 0; }
.header__subtitle { color: #909399; font-size: 14px; }
.header__right { display: flex; gap: 8px; }
.report-meta { margin-bottom: 20px; }
.report-content { padding: 20px; border: 1px solid #e8e8e8; border-radius: 4px; max-height: 60vh; overflow-y: auto; line-height: 1.8; }
.report-content :deep(table) { width: 100%; border-collapse: collapse; margin: 10px 0; }
.report-content :deep(th), .report-content :deep(td) { border: 1px solid #dcdfe6; padding: 8px 12px; text-align: center; }
.report-content :deep(th) { background: #f5f7fa; font-weight: bold; }
.report-content :deep(h2) { margin: 15px 0 10px; font-size: 16px; color: #303133; }
.report-content :deep(p) { margin: 8px 0; }
.search { display: flex; gap: 12px; align-items: center; }
.table-wrap { margin-top: 16px; }
.table-wrap__scroll { overflow-x: auto; }
.table-wrap__pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
.op-cell { display: flex; justify-content: center; gap: 4px; }
</style>