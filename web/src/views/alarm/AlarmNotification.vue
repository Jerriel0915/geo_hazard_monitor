<template>
  <div class="page-content">
    <div class="page-title">历史告警</div>
    <div class="page-body">
      <!-- 搜索和操作栏 -->
      <div class="search-bar">
        <div class="search-conditions">
          <el-form :inline="true" :model="queryParams" label-width="100px">
            <el-form-item label="隐患点名称">
              <el-input v-model="queryParams.hazardPointName" placeholder="请输入" clearable />
            </el-form-item>
            <el-form-item label="人员名称">
              <el-input v-model="queryParams.personName" placeholder="请输入" clearable />
            </el-form-item>
            <el-form-item label="告警时间">
              <el-date-picker
                v-model="queryParams.alarmTimeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item label="告警次数">
              <el-input-number v-model="queryParams.alarmCountMin" :min="0" placeholder="最小" style="width: 120px" />
              <span style="margin: 0 8px">至</span>
              <el-input-number v-model="queryParams.alarmCountMax" :min="0" placeholder="最大" style="width: 120px" />
            </el-form-item>
            <el-form-item label="告警等级">
              <el-select v-model="queryParams.alarmLevel" placeholder="请选择" clearable multiple style="width: 120px">
                <el-option label="一级" value="1" />
                <el-option label="二级" value="2" />
                <el-option label="三级" value="3" />
                <el-option label="四级" value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="告警类型">
              <el-select v-model="queryParams.alarmType" placeholder="请选择" clearable multiple style="width: 120px">
                <el-option label="阈值预警" value="threshold" />
                <el-option label="综合预警" value="comprehensive" />
              </el-select>
            </el-form-item>
            <el-form-item label="警情状态">
              <el-select v-model="queryParams.status" placeholder="请选择" clearable multiple style="width: 120px">
                <el-option label="误报" value="false_alarm" />
                <el-option label="已销警" value="closed" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleQuery">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="action-buttons">
          <el-button type="info" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <div class="table-container">
        <el-table
          :data="tableData"
          style="width: 100%"
          @row-click="handleRowClick"
          border
          stripe
        >
          <el-table-column prop="hazardPointName" label="隐患点名称" min-width="180" />
          <el-table-column prop="alarmLevel" label="告警等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="firstAlarmTime" label="首次告警时间" width="180" />
          <el-table-column prop="lastAlarmTime" label="最后告警时间" width="180" />
          <el-table-column prop="alarmCount" label="告警次数" width="100">
            <template #default="{ row }">
              <span class="alarm-count" @click.stop="showAlarmList(row)">{{ row.alarmCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="alarmType" label="告警类型" width="120">
            <template #default="{ row }">
              {{ getAlarmTypeText(row.alarmType) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="警情状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responderName" label="响应人员" width="120" />
          <el-table-column prop="responseTime" label="响应时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click.stop="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 告警详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="告警详情" width="800px">
      <div v-if="currentRow" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="隐患点名称">{{ currentRow.hazardPointName }}</el-descriptions-item>
          <el-descriptions-item label="告警等级">
            <el-tag :type="getAlarmLevelType(currentRow.alarmLevel)">{{ getAlarmLevelText(currentRow.alarmLevel) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="告警类型">{{ getAlarmTypeText(currentRow.alarmType) }}</el-descriptions-item>
          <el-descriptions-item label="警情状态">
            <el-tag :type="getStatusType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="首次告警时间">{{ currentRow.firstAlarmTime }}</el-descriptions-item>
          <el-descriptions-item label="最后告警时间">{{ currentRow.lastAlarmTime }}</el-descriptions-item>
          <el-descriptions-item label="告警次数">{{ currentRow.alarmCount }}</el-descriptions-item>
          <el-descriptions-item label="响应人员">{{ currentRow.responderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">{{ currentRow.responseTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="告警内容" :span="2">{{ currentRow.alarmContent }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 告警列表弹窗 -->
    <el-dialog v-model="alarmListDialogVisible" title="告警列表" width="900px">
      <div class="alarm-list-table">
        <el-table :data="currentAlarmList" style="width: 100%" border stripe>
          <el-table-column prop="alarmTime" label="告警时间" width="180" />
          <el-table-column prop="alarmLevel" label="告警等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="alarmContent" label="告警内容" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="alarmListDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {Download, View} from '@element-plus/icons-vue'

// 查询参数
const queryParams = reactive({
  hazardPointName: '',
  personName: '',
  alarmTimeRange: [] as string[],
  alarmCountMin: null as number | null,
  alarmCountMax: null as number | null,
  alarmLevel: [] as string[],
  alarmType: [] as string[],
  status: [] as string[]
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref<any[]>([])

// 弹窗
const detailDialogVisible = ref(false)
const alarmListDialogVisible = ref(false)

// 当前行
const currentRow = ref<any>(null)
const currentAlarmList = ref<any[]>([])

// Mock 数据 - 历史告警（已处理、误报、已销警）
const mockData = [
  {
    id: 1,
    hazardPointName: '桥梁监测点D-03',
    alarmLevel: '4',
    firstAlarmTime: '2024-06-01 00:00:00',
    lastAlarmTime: '2024-06-02 00:00:00',
    alarmCount: 3,
    alarmType: 'threshold',
    status: 'false_alarm',
    responderName: '王五',
    responseTime: '2024-06-01 08:00:00',
    alarmContent: '桥墩沉降超过阈值2mm，经核查为传感器故障',
    alarmList: [
      { alarmTime: '2024-06-01 00:00:00', alarmLevel: '4', alarmContent: '桥墩沉降2.1mm' },
      { alarmTime: '2024-06-02 00:00:00', alarmLevel: '4', alarmContent: '桥墩沉降2.3mm' }
    ]
  },
  {
    id: 2,
    hazardPointName: '水库监测点F-02',
    alarmLevel: '2',
    firstAlarmTime: '2024-05-25 06:00:00',
    lastAlarmTime: '2024-06-01 18:00:00',
    alarmCount: 12,
    alarmType: 'threshold',
    status: 'closed',
    responderName: '赵六',
    responseTime: '2024-06-01 20:00:00',
    alarmContent: '水库水位超过警戒水位50cm，当前已降至安全范围',
    alarmList: [
      { alarmTime: '2024-05-25 06:00:00', alarmLevel: '2', alarmContent: '水库水位超限30cm' },
      { alarmTime: '2024-06-01 18:00:00', alarmLevel: '2', alarmContent: '水库水位超限50cm' }
    ]
  },
  {
    id: 3,
    hazardPointName: '地质灾害点H-09',
    alarmLevel: '2',
    firstAlarmTime: '2024-05-15 14:00:00',
    lastAlarmTime: '2024-05-16 14:00:00',
    alarmCount: 8,
    alarmType: 'threshold',
    status: 'closed',
    responderName: '周八',
    responseTime: '2024-05-16 15:00:00',
    alarmContent: '裂缝宽度超过阈值，经处理已稳定',
    alarmList: [
      { alarmTime: '2024-05-15 14:00:00', alarmLevel: '2', alarmContent: '裂缝宽度5mm' },
      { alarmTime: '2024-05-16 14:00:00', alarmLevel: '2', alarmContent: '裂缝宽度8mm' }
    ]
  },
  {
    id: 4,
    hazardPointName: '边坡监测点I-11',
    alarmLevel: '4',
    firstAlarmTime: '2024-05-10 08:00:00',
    lastAlarmTime: '2024-05-10 12:00:00',
    alarmCount: 4,
    alarmType: 'threshold',
    status: 'false_alarm',
    responderName: '吴九',
    responseTime: '2024-05-10 14:00:00',
    alarmContent: '温度传感器误报，实际温度正常',
    alarmList: [
      { alarmTime: '2024-05-10 08:00:00', alarmLevel: '4', alarmContent: '温度异常预警' },
      { alarmTime: '2024-05-10 12:00:00', alarmLevel: '4', alarmContent: '温度持续异常' }
    ]
  },
  {
    id: 5,
    hazardPointName: '山体滑坡点J-15',
    alarmLevel: '3',
    firstAlarmTime: '2024-05-08 10:00:00',
    lastAlarmTime: '2024-05-09 10:00:00',
    alarmCount: 15,
    alarmType: 'threshold',
    status: 'closed',
    responderName: '郑十',
    responseTime: '2024-05-09 11:00:00',
    alarmContent: '山体滑坡风险已排除，现场监测数据恢复正常',
    alarmList: [
      { alarmTime: '2024-05-08 10:00:00', alarmLevel: '3', alarmContent: '位移监测异常' },
      { alarmTime: '2024-05-09 10:00:00', alarmLevel: '3', alarmContent: '已恢复正常' }
    ]
  },
  {
    id: 6,
    hazardPointName: '隧道监测点K-02',
    alarmLevel: '1',
    firstAlarmTime: '2024-05-05 06:00:00',
    lastAlarmTime: '2024-05-05 08:00:00',
    alarmCount: 2,
    alarmType: 'comprehensive',
    status: 'false_alarm',
    responderName: '钱十一',
    responseTime: '2024-05-05 09:00:00',
    alarmContent: '综合预警误报，经核实为设备校准误差',
    alarmList: [
      { alarmTime: '2024-05-05 06:00:00', alarmLevel: '1', alarmContent: '综合预警触发' },
      { alarmTime: '2024-05-05 08:00:00', alarmLevel: '1', alarmContent: '数据异常确认' }
    ]
  }
]

// 计算属性：过滤后的数据
const filteredData = computed(() => {
  let result = [...mockData]

  // 隐患点名称模糊查询
  if (queryParams.hazardPointName) {
    result = result.filter(item => 
      item.hazardPointName.includes(queryParams.hazardPointName)
    )
  }

  // 人员名称模糊查询
  if (queryParams.personName) {
    result = result.filter(item => 
      item.responderName && item.responderName.includes(queryParams.personName)
    )
  }

  // 告警等级筛选
  if (queryParams.alarmLevel.length > 0) {
    result = result.filter(item => queryParams.alarmLevel.includes(item.alarmLevel))
  }

  // 告警类型筛选
  if (queryParams.alarmType.length > 0) {
    result = result.filter(item => queryParams.alarmType.includes(item.alarmType))
  }

  // 警情状态筛选
  if (queryParams.status.length > 0) {
    result = result.filter(item => queryParams.status.includes(item.status))
  }

  // 告警次数范围筛选
  if (queryParams.alarmCountMin != null) {
    result = result.filter(item => item.alarmCount >= queryParams.alarmCountMin!)
  }
  if (queryParams.alarmCountMax != null) {
    result = result.filter(item => item.alarmCount <= queryParams.alarmCountMax!)
  }

  // 按最后告警时间倒序
  result.sort((a, b) => new Date(b.lastAlarmTime).getTime() - new Date(a.lastAlarmTime).getTime())

  return result
})

// 计算属性：分页数据
const paginatedData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredData.value.slice(start, end)
})

// 初始化
onMounted(() => {
  pagination.total = mockData.length
  tableData.value = paginatedData.value
})

// 获取告警等级类型
const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[level] || 'info'
}

// 获取告警等级文本
const getAlarmLevelText = (level: string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[level] || level
}

// 获取告警类型文本
const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'threshold': '阈值预警',
    'comprehensive': '综合预警'
  }
  return map[type] || type
}

// 获取状态类型
const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'false_alarm': 'info',
    'closed': 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'false_alarm': '误报',
    'closed': '已销警'
  }
  return map[status] || status
}

// 查询
const handleQuery = () => {
  pagination.currentPage = 1
  pagination.total = filteredData.value.length
  tableData.value = paginatedData.value
  ElMessage.success('查询成功')
}

// 重置
const handleReset = () => {
  queryParams.hazardPointName = ''
  queryParams.personName = ''
  queryParams.alarmTimeRange = []
  queryParams.alarmCountMin = null
  queryParams.alarmCountMax = null
  queryParams.alarmLevel = []
  queryParams.alarmType = []
  queryParams.status = []
  handleQuery()
}

// 行点击 - 查看详情
const handleRowClick = (row: any) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

// 查看详情
const handleView = (row: any) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

// 显示告警列表
const showAlarmList = (row: any) => {
  currentRow.value = row
  currentAlarmList.value = row.alarmList || []
  alarmListDialogVisible.value = true
}

// 导出
const handleExport = () => {
  ElMessage.success('导出功能已触发（模拟）')
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  tableData.value = paginatedData.value
}

// 分页页码变化
const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  tableData.value = paginatedData.value
}
</script>

<style scoped>
.page-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: calc(100% - 32px);
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.page-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.search-conditions {
  flex: 1;
  min-width: 0;
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.table-container {
  flex: 1;
  overflow: auto;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.alarm-count {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}

.alarm-count:hover {
  color: #66b1ff;
}

.detail-content {
  padding: 16px 0;
}

.alarm-list-table {
  max-height: 400px;
  overflow: auto;
}
</style>