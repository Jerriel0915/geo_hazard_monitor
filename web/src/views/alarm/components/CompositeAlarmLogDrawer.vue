<template>
  <el-drawer :model-value="visible" title="运行日志" size="680px" @close="emit('update:visible', false)">
    <template #header>
      <div>
        <h3 style="margin: 0; font-size: 16px;">运行日志</h3>
        <p style="margin: 4px 0 0; font-size: 13px; color: #86909c;">{{ alarmName }}</p>
      </div>
    </template>

    <div v-loading="loading">
      <el-table :data="logs" stripe size="small" :header-cell-style="{ background: '#f7f8fa', fontWeight: 600 }">
        <el-table-column prop="triggerTime" label="触发时间" width="170" />
        <el-table-column prop="triggerMode" label="方式" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.triggerMode === 'REALTIME' ? 'warning' : 'primary'" effect="plain">
              {{ row.triggerMode === 'REALTIME' ? '实时' : '周期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="80" align="center">
          <template #default="{ row }">{{ row.durationMs }}ms</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="输出/错误" min-width="200">
          <template #default="{ row }">
            <div v-if="row.status === 'SUCCESS'" class="log-output">{{ row.output || 'null' }}</div>
            <div v-else class="log-error">{{ row.errorMsg || row.output }}</div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="logTotal > 0" style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="logPageNum"
          v-model:page-size="logPageSize"
          :total="logTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { CompositeAlarmLog } from '@/api/alarm'

// 后端暂未实现策略执行日志接口，返回空结果兜底
const getCompositeAlarmLogs = async (_id: number, _params: Record<string, unknown>) => ({ rows: [] as CompositeAlarmLog[], total: 0 })

const props = defineProps<{
  visible: boolean
  alarmId: number
  alarmName: string
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
}>()

const loading = ref(false)
const logs = ref<CompositeAlarmLog[]>([])
const logTotal = ref(0)
const logPageNum = ref(1)
const logPageSize = ref(10)

watch(() => props.visible, (val) => {
  if (val) {
    logPageNum.value = 1
    loadLogs()
  }
}, { immediate: true })

async function loadLogs() {
  loading.value = true
  try {
    const res = await getCompositeAlarmLogs(props.alarmId, { pageNum: logPageNum.value, pageSize: logPageSize.value })
    logs.value = res.rows
    logTotal.value = res.total
  } finally {
    loading.value = false
  }
}

function statusType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'ERROR') return 'danger'
  if (status === 'TIMEOUT') return 'warning'
  return 'info'
}

function statusLabel(status: string) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'ERROR') return '错误'
  if (status === 'TIMEOUT') return '超时'
  return status
}
</script>

<style scoped>
.log-output {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #4e5969;
  word-break: break-all;
  max-height: 60px;
  overflow-y: auto;
}

.log-error {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #f53f3f;
  word-break: break-all;
}
</style>
