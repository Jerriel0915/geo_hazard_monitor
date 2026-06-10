<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">历史告警</h2>
        <span class="header__subtitle">已处置告警记录归档与追溯</span>
      </div>
      <div class="header__right">
        <el-button type="info" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="queryParams.hazardPointName" placeholder="隐患点名称" clearable />
      <el-input v-model="queryParams.personName" placeholder="人员名称" clearable />
      <el-date-picker
        v-model="queryParams.alarmTimeRange"
        type="daterange"
        range-separator=""
        start-placeholder="告警时间:开始"
        end-placeholder="告警时间:结束"
        value-format="YYYY-MM-DD"
      />
      <el-select v-model="queryParams.alarmLevel" placeholder="告警等级" clearable multiple>
        <el-option label="一级" value="1" />
        <el-option label="二级" value="2" />
        <el-option label="三级" value="3" />
        <el-option label="四级" value="4" />
      </el-select>
      <el-select v-model="queryParams.alarmType" placeholder="告警类型" clearable multiple>
        <el-option label="阈值预警" value="threshold" />
        <el-option label="综合预警" value="comprehensive" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="警情状态" clearable multiple>
        <el-option label="误报" value="false_alarm" />
        <el-option label="已销警" value="closed" />
      </el-select>
      <el-button type="primary" @click="handleQuery">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table
          :data="tableData"
          style="width: 100%"
          @row-click="handleRowClick"
          border
          stripe
        >
          <el-table-column prop="hazardPointName" label="隐患点名称" min-width="160" />
          <el-table-column prop="alarmLevel" label="告警等级" width="90">
            <template #default="{ row }">
              <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="firstAlarmTime" label="首次告警时间" min-width="160" />
          <el-table-column prop="lastAlarmTime" label="最后告警时间" min-width="160" />
          <el-table-column prop="alarmCount" label="告警次数" width="90">
            <template #default="{ row }">
              <span class="alarm-count" @click.stop="handleView(row)">{{ row.alarmCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="alarmType" label="告警类型" width="100">
            <template #default="{ row }">
              {{ getAlarmTypeText(row.alarmType) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="警情状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responderName" label="响应人员" width="100" />
          <el-table-column prop="responseTime" label="响应时间" min-width="160" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click.stop="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
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
    <AlarmDetailDialog v-model="detailDialogVisible" :data="currentRow" />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {Download, View} from '@element-plus/icons-vue'
import AlarmDetailDialog from './components/AlarmDetailDialog.vue'

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

// 当前行
const currentRow = ref<any>(null)

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

// 重置（静默，不弹窗）
const handleReset = () => {
  queryParams.hazardPointName = ''
  queryParams.personName = ''
  queryParams.alarmTimeRange = []
  queryParams.alarmCountMin = null
  queryParams.alarmCountMax = null
  queryParams.alarmLevel = []
  queryParams.alarmType = []
  queryParams.status = []
  pagination.currentPage = 1
  pagination.total = filteredData.value.length
  tableData.value = paginatedData.value
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
.alarm-count {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}

.alarm-count:hover {
  color: #66b1ff;
}
</style>