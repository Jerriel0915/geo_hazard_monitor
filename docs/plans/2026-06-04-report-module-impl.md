# Report Module Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement three report module pages (Report.vue, Query.vue, Analysis.vue) plus shared API file, all with mock data.

**Architecture:** Each page is a standalone Vue 3 SFC following the project's existing patterns (Composition API, Element Plus, ECharts). A single `api/report.ts` file provides all mock data and API interfaces. PDF export uses html2canvas + jsPDF; CSV export uses native Blob.

**Tech Stack:** Vue 3 + TypeScript, Element Plus 2.6, ECharts 6, html2canvas, jsPDF

---

### Task 1: Install dependencies

**Files:**
- Modify: `web/package.json`

**Step 1: Install html2canvas and jspdf**

Run:
```bash
cd web && npm install html2canvas jspdf
```

**Step 2: Verify installation**

Run: `cat web/package.json | grep -E "html2canvas|jspdf"`
Expected: Both packages listed in dependencies

**Step 3: Commit**

```bash
git add web/package.json web/package-lock.json
git commit -m "chore(web): add html2canvas and jspdf dependencies for report module"
```

---

### Task 2: Create API file with mock data

**Files:**
- Create: `web/src/api/report.ts`

**Step 1: Create the report API file**

This file contains all TypeScript interfaces, mock data generators, and API functions (with mock fallback) for the three report pages.

Key interfaces to define:
- `ReportItem` — id, title, type ('weekly'|'monthly'), periodStart, periodEnd, createTime, content (HTML string)
- `ReportPageParams` — pageNum, pageSize, keyword?, type?, startDate?, endDate?
- `MonitorQueryParams` — hazardPointId?, deviceType?, deviceId?, attrCodes?, startTime?, endTime?, pageNum?, pageSize?
- `MonitorQueryRow` — time, deviceName, + dynamic key-value pairs
- `SensorSeriesItem` — id, hazardPointId, hazardPointName, deviceId, deviceName, sensorId, sensorName, attrCode, attrName, unit, color
- `ChartDataItem` — times: string[], values: number[]
- `HazardPointOption` — id, name
- `DeviceOption` — id, name, deviceType, hazardPointId
- `DeviceTypeOption` — value, label, attrs: { code, name, unit }[]
- `GridChartItem` — index, sensorSeriesId?, title?, chartData?

Mock data to generate:
- `generateMockReports()` — 12 reports (mix of weekly/monthly), each with realistic rich HTML content (tables, charts description, summary)
- `getMockHazardPoints()` — 5 hazard points
- `getMockDeviceTypes()` — 4 types: 位移计(X/Y/Z), 雨量计(降雨量), 倾角传感器(X/Y倾角), 土压力计(土压力)
- `getMockDevices()` — 10+ devices across hazard points and types
- `getMockQueryData()` — generate time-series rows based on selected device type
- `getMockChartData()` — generate chart points for analysis

API functions (all return mock data wrapped in AjaxResult):
- `getReportPage(params)` — paginated reports
- `getReportDetail(id)` — single report with full content
- `deleteReport(id)` — mock delete
- `getHazardPointOptions()` — hazard point list for dropdowns
- `getDeviceTypeOptions()` — device types with attribute definitions
- `getDeviceOptions(params)` — devices filtered by hazardPointId and/or deviceType
- `getMonitorQueryData(params)` — query result rows with dynamic columns
- `getChartData(params)` — chart data for analysis (times + values for a sensor attribute)
- `getGridChartData(params)` — batch chart data for grid mode

**Step 2: Verify the file compiles**

Run: `cd web && npx vue-tsc --noEmit --pretty 2>&1 | head -20`
Expected: No errors related to report.ts

**Step 3: Commit**

```bash
git add web/src/api/report.ts
git commit -m "feat(web): add report module API with mock data"
```

---

### Task 3: Implement Report.vue — Monitoring Reports

**Files:**
- Rewrite: `web/src/views/report/Report.vue`

**Step 1: Implement the full Report.vue page**

Follow Device.vue patterns exactly:

Template structure:
```
<div class="report-page">
  <!-- Page header: title only, no add button -->
  <div class="page-header">
    <h2 class="page-title">监测报告</h2>
  </div>

  <!-- Search bar: keyword + report type + date range + search/reset -->
  <div class="search-bar">
    <el-input v-model="searchKeyword" placeholder="搜索报告标题" clearable ... />
    <el-select v-model="searchType" placeholder="报告类型" clearable>
      <el-option label="周报" value="weekly" />
      <el-option label="月报" value="monthly" />
    </el-select>
    <el-date-picker v-model="searchDateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
    <el-button type="primary" @click="handleSearch">搜索</el-button>
    <el-button @click="handleReset">重置</el-button>
  </div>

  <!-- Table -->
  <div class="table-container">
    <el-table :data="tableData" border stripe v-loading="loading" :header-cell-style="...">
      <el-table-column prop="id" label="报告编号" width="100" align="center" />
      <el-table-column prop="title" label="报告标题" min-width="250" align="center" />
      <el-table-column prop="type" label="报告类型" width="100" align="center">
        <!-- el-tag: weekly=blue, monthly=green -->
      </el-table-column>
      <el-table-column label="报告周期" width="220" align="center">
        <!-- periodStart ~ periodEnd -->
      </el-table-column>
      <el-table-column prop="createTime" label="生成时间" width="180" align="center" />
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <!-- 查看 / 下载PDF / 删除 -->
      </el-table-column>
    </el-table>
    <div class="pagination-container">
      <el-pagination ... />
    </div>
  </div>

  <!-- View dialog: width="900px", v-html for content, print/PDF buttons -->
  <el-dialog v-model="viewDialogVisible" :title="currentReport?.title" width="900px" destroy-on-close>
    <div class="report-meta">报告基本信息（类型、周期、生成时间）</div>
    <div ref="reportContentRef" class="report-content" v-html="currentReport?.content" />
    <template #footer>
      <el-button @click="handlePrint">打印</el-button>
      <el-button type="primary" @click="handleExportPdf" :loading="pdfLoading">导出PDF</el-button>
      <el-button @click="viewDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</div>
```

Script:
- Import from `@/api/report`: getReportPage, getReportDetail, deleteReport
- Import html2canvas and jsPDF
- Standard state: tableData, loading, currentPage, pageSize, total, searchKeyword, searchType, searchDateRange
- loadTableData() — call API, update table
- handleView(row) — fetch detail, open dialog
- handleDelete(row) — confirm then delete
- handleExportPdf() — html2canvas → jsPDF pipeline
- handlePrint() — window.print() with print CSS
- onMounted → loadTableData()

Styles: Copy from Device.vue pattern (.report-page, .page-header, .search-bar, .table-container, .pagination-container)

**Step 2: Verify page loads**

Run: `cd web && npm run dev`
Navigate to `/report/report` in browser.
Expected: Table with mock report data, search works, view dialog shows rich HTML content

**Step 3: Commit**

```bash
git add web/src/views/report/Report.vue
git commit -m "feat(web): implement monitoring report page with PDF export"
```

---

### Task 4: Implement Query.vue — Query Center

**Files:**
- Rewrite: `web/src/views/report/Query.vue`

**Step 1: Implement the full Query.vue page**

Template structure:
```
<div class="query-page">
  <div class="page-header">
    <h2 class="page-title">查询中心</h2>
  </div>

  <!-- Cascading filters -->
  <div class="search-bar">
    <el-select v-model="queryParams.hazardPointId" placeholder="隐患点" clearable @change="onHazardPointChange">
      <el-option v-for="hp in hazardPointOptions" ... />
    </el-select>
    <el-select v-model="queryParams.deviceType" placeholder="设备类型" clearable @change="onDeviceTypeChange">
      <el-option v-for="dt in filteredDeviceTypes" ... />
    </el-select>
    <el-select v-model="queryParams.deviceId" placeholder="设备" clearable @change="onDeviceChange">
      <el-option v-for="d in filteredDevices" ... />
    </el-select>
    <el-select v-model="queryParams.attrCodes" placeholder="监测属性" multiple clearable collapse-tags>
      <el-option v-for="attr in availableAttrs" :key="attr.code" :label="attr.name" :value="attr.code" />
    </el-select>
    <el-date-picker v-model="queryParams.timeRange" type="datetimerange" start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss" />
    <el-button type="primary" @click="handleQuery">查询</el-button>
    <el-button @click="handleReset">重置</el-button>
    <el-button type="success" @click="handleExportCsv" :disabled="!tableData.length">导出CSV</el-button>
  </div>

  <!-- Dynamic column table -->
  <div class="table-container">
    <el-table :data="tableData" border stripe v-loading="loading" :header-cell-style="...">
      <el-table-column prop="time" label="时间" width="180" fixed />
      <el-table-column prop="deviceName" label="设备名称" width="150" />
      <!-- Dynamic columns rendered with v-for based on selected attrs -->
      <el-table-column v-for="col in dynamicColumns" :key="col.code" :prop="col.code" :label="col.name + '(' + col.unit + ')'" min-width="150" align="center" />
    </el-table>
    <div class="pagination-container">
      <el-pagination ... />
    </div>
  </div>
</div>
```

Script:
- Import from `@/api/report`: getHazardPointOptions, getDeviceTypeOptions, getDeviceOptions, getMonitorQueryData
- queryParams: reactive({ hazardPointId, deviceType, deviceId, attrCodes, timeRange, pageNum, pageSize })
- Cascading logic:
  - onHazardPointChange → reset deviceType/deviceId/attrCodes, filter device types for this hazard point
  - onDeviceTypeChange → reset deviceId/attrCodes, filter devices, update available attrs
  - onDeviceChange → update available attrs
- dynamicColumns computed — based on selected deviceType's attrs
- handleQuery() — validate at least deviceType selected, call API
- handleExportCsv() — build CSV string from tableData + dynamicColumns, create Blob, trigger download
- loadOptions() — load hazard points, device types, devices on mount

**Step 2: Verify page loads**

Navigate to `/report/query` in browser.
Expected: Cascading filters work, selecting device type reveals dynamic columns, query returns mock data, CSV export downloads file

**Step 3: Commit**

```bash
git add web/src/views/report/Query.vue
git commit -m "feat(web): implement query center with dynamic columns and CSV export"
```

---

### Task 5: Implement Analysis.vue — Mode Selection + Correlation Analysis

**Files:**
- Rewrite: `web/src/views/report/Analysis.vue`

**Step 1: Implement the full Analysis.vue page**

This is the largest component. It has three internal views: mode selection, correlation analysis, data grid.

Template structure:
```
<div class="analysis-page">

  <!-- Mode Selection (initial view) -->
  <div v-if="currentMode === ''" class="mode-selection">
    <h2 class="mode-title">数据分析</h2>
    <p class="mode-subtitle">请选择分析模式</p>
    <div class="mode-cards">
      <div class="mode-card" @click="currentMode = 'correlation'">
        <div class="mode-card-icon">📈</div>
        <div class="mode-card-title">关联分析</div>
        <div class="mode-card-desc">将多个传感器属性叠加在同一坐标系中，对比分析不同指标的变化趋势与关联关系</div>
      </div>
      <div class="mode-card" @click="currentMode = 'grid'">
        <div class="mode-card-icon">📊</div>
        <div class="mode-card-title">数据宫格</div>
        <div class="mode-card-desc">以九宫格模式同时查看多个传感器的独立数据图表，快速纵览整体监测态势</div>
      </div>
    </div>
  </div>

  <!-- Correlation Analysis Mode -->
  <div v-if="currentMode === 'correlation'" class="correlation-mode">
    <div class="mode-header">
      <el-button text @click="currentMode = ''">← 返回</el-button>
      <span class="mode-label">关联分析</span>
    </div>
    <div class="correlation-layout">
      <!-- Left panel -->
      <div class="correlation-panel">
        <div class="panel-section">
          <div class="panel-title">时间范围</div>
          <el-date-picker v-model="correlationTimeRange" type="datetimerange" ... />
        </div>
        <div class="panel-section">
          <div class="panel-title">传感器列表</div>
          <el-button type="primary" size="small" @click="addSensorDialogVisible = true" style="width:100%;margin-bottom:10px">+ 添加传感器</el-button>
          <div class="sensor-tags">
            <div v-for="(s, idx) in selectedSensors" :key="s.id" class="sensor-tag-item">
              <el-tag :color="s.color" closable @close="removeSensor(idx)">
                {{ s.deviceName }} - {{ s.attrName }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="panel-section">
          <div class="panel-title">分析工具</div>
          <el-checkbox-group v-model="analysisTools">
            <el-checkbox label="statistics">统计指标</el-checkbox>
            <el-checkbox label="trend">趋势分析</el-checkbox>
            <el-checkbox label="changeRate">变化率</el-checkbox>
          </el-checkbox-group>
        </div>
        <el-button type="primary" @click="loadCorrelationChart" :loading="chartLoading">生成图表</el-button>
      </div>

      <!-- Right chart area -->
      <div class="correlation-chart-area">
        <div class="chart-toolbar">
          <el-button size="small" @click="exportChartImage">导出图片</el-button>
        </div>
        <div ref="correlationChartRef" class="chart-container" />
        <!-- Statistics panel (shown when tool enabled) -->
        <div v-if="analysisTools.includes('statistics') && statisticsData.length" class="statistics-panel">
          <el-table :data="statisticsData" border size="small">
            <el-table-column prop="name" label="系列" />
            <el-table-column prop="max" label="最大值" />
            <el-table-column prop="min" label="最小值" />
            <el-table-column prop="avg" label="平均值" />
            <el-table-column prop="std" label="标准差" />
          </el-table>
        </div>
      </div>
    </div>

    <!-- Add sensor dialog -->
    <el-dialog v-model="addSensorDialogVisible" title="添加传感器" width="500px">
      <el-form label-width="80px">
        <el-form-item label="隐患点">
          <el-select v-model="addSensorForm.hazardPointId" @change="onAddSensorHpChange">...</el-select>
        </el-form-item>
        <el-form-item label="设备">
          <el-select v-model="addSensorForm.deviceId" @change="onAddSensorDeviceChange">...</el-select>
        </el-form-item>
        <el-form-item label="属性">
          <el-select v-model="addSensorForm.attrCode">...</el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addSensorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddSensor">确定</el-button>
      </template>
    </el-dialog>
  </div>

  <!-- Grid Mode (Task 6) -->
  <div v-if="currentMode === 'grid'" class="grid-mode">...</div>
</div>
```

Script key logic:
- currentMode: ref<string> ('' | 'correlation' | 'grid')
- Correlation mode:
  - selectedSensors: ref<SensorSeriesItem[]> — user-added sensors with auto-assigned colors
  - Color palette: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
  - correlationChartRef, chartInstance
  - loadCorrelationChart() — for each selectedSensor, call getChartData(), merge into ECharts multi-series option
  - ECharts option: xAxis=time, yAxis[]=one per sensor (with offset), series[]=line per sensor
  - If trend tool enabled: compute linear regression (least squares), add as dashed line series
  - If changeRate tool enabled: compute (v[i]-v[i-1])/v[i-1]*100, add as additional series
  - Statistics: computed from values (max, min, avg, std)
  - exportChartImage(): chartInstance.getDataURL({ type: 'png', pixelRatio: 2 }) → download

**Step 2: Verify correlation analysis**

Navigate to `/report/analysis`, click correlation card.
Expected: Left panel with add sensor + time range, right area with ECharts chart when sensors added and generated

**Step 3: Commit**

```bash
git add web/src/views/report/Analysis.vue
git commit -m "feat(web): implement data analysis page with mode selection and correlation analysis"
```

---

### Task 6: Implement Analysis.vue — Data Grid Mode

**Files:**
- Modify: `web/src/views/report/Analysis.vue` (append grid mode template and logic)

**Step 1: Add data grid mode to Analysis.vue**

Add to template (inside the `v-if="currentMode === 'grid'"` section):

```
<div class="grid-mode">
  <div class="mode-header">
    <el-button text @click="currentMode = ''">← 返回</el-button>
    <span class="mode-label">数据宫格</span>
    <div class="grid-time-range">
      <span>统一时间范围：</span>
      <el-date-picker v-model="gridTimeRange" type="datetimerange" ... />
      <el-button type="primary" size="small" @click="loadAllGridCharts">应用</el-button>
    </div>
  </div>
  <div class="grid-container">
    <div v-for="(cell, idx) in gridCells" :key="idx" class="grid-cell">
      <!-- Configured cell -->
      <template v-if="cell.sensorSeriesId">
        <div class="grid-cell-header">
          <span class="grid-cell-title">{{ cell.title }}</span>
          <el-dropdown trigger="click">
            <span class="grid-cell-more">···</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openGridConfig(idx)">修改</el-dropdown-item>
                <el-dropdown-item @click="clearGridCell(idx)">清除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div :ref="el => setGridChartRef(idx, el)" class="grid-chart" />
      </template>
      <!-- Empty cell -->
      <template v-else>
        <div class="grid-cell-empty" @click="openGridConfig(idx)">
          <span class="grid-cell-add-icon">+</span>
          <span>添加图表</span>
        </div>
      </template>
    </div>
  </div>
</div>

<!-- Grid config dialog (reuse add sensor pattern) -->
<el-dialog v-model="gridConfigDialogVisible" title="配置图表" width="450px">
  <el-form label-width="80px">
    <el-form-item label="隐患点"><el-select ...></el-form-item>
    <el-form-item label="设备"><el-select ...></el-form-item>
    <el-form-item label="属性"><el-select ...></el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="gridConfigDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="confirmGridConfig">确定</el-button>
  </template>
</el-dialog>
```

Script additions:
- gridCells: ref<GridChartItem[]> — array of 9 items (3×3), each with optional sensor config
- gridTimeRange: ref — unified time range for all cells
- gridChartRefs: Map<number, HTMLElement> — template refs for chart containers
- gridChartInstances: Map<number, echarts.ECharts>
- setGridChartRef(idx, el) — store ref
- openGridConfig(idx) — open dialog, remember target index
- confirmGridConfig() — save config to gridCells[idx], load chart
- clearGridCell(idx) — remove config, dispose chart
- loadAllGridCharts() — reload all configured cells with current time range
- loadGridCellChart(idx) — for one cell: call getChartData(), init ECharts mini chart
- onUnmounted — dispose all grid chart instances

Styles for grid:
```css
.grid-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 16px;
}
.grid-cell {
  aspect-ratio: 16/10;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.grid-chart { flex: 1; min-height: 0; }
.grid-cell-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  cursor: pointer;
  color: #909399;
}
.grid-cell-empty:hover { color: #409eff; background: #f5f7fa; }
```

**Step 2: Verify grid mode**

Navigate to `/report/analysis`, click grid card.
Expected: 3×3 grid with empty cells showing "添加图表", clicking opens config dialog, chart renders in cell, unified time range works

**Step 3: Commit**

```bash
git add web/src/views/report/Analysis.vue
git commit -m "feat(web): add data grid (9-grid) mode to analysis page"
```

---

### Task 7: Final integration verification

**Step 1: Verify all three pages compile**

Run: `cd web && npx vue-tsc --noEmit --pretty`
Expected: No TypeScript errors

**Step 2: Start dev server and test all pages**

Run: `cd web && npm run dev`

Test checklist:
- `/report/report` — table loads, search filters, view dialog shows content, PDF export triggers download
- `/report/query` — cascading selects work, dynamic columns render, CSV export downloads
- `/report/analysis` — mode selection cards display, correlation mode chart renders with multiple sensors, grid mode cells configure and render, back button works

**Step 3: Commit any fixes**

```bash
git add -A
git commit -m "fix(web): polish report module integration"
```

---

## Key Reference Files

When implementing, keep these files open for pattern reference:

| Purpose | File |
|---------|------|
| Table page pattern | `web/src/views/basic/Device.vue` |
| API + mock pattern | `web/src/api/device.ts`, `web/src/api/monitorData.ts` |
| Type definitions | `web/src/api/system.ts` (AjaxResult, PageResult) |
| Request wrapper | `web/src/utils/request.ts` |
| Router config | `web/src/router/index.ts` |
| Design doc | `docs/plans/2026-06-04-report-module-design.md` |
