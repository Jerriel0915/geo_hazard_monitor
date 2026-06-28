<template>
  <el-drawer :model-value="visible" size="720px" @close="emit('update:visible', false)">
    <template #header>
      <div>
        <h3 style="margin: 0; font-size: 16px;">执行日志</h3>
        <p style="margin: 4px 0 0; font-size: 13px; color: #86909c;">{{ alarmName }}</p>
      </div>
    </template>

    <div v-loading="loading">
      <el-table :data="logs" stripe size="small" :header-cell-style="{ background: '#f7f8fa', fontWeight: 600 }">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-detail">
              <template v-if="parseScriptLogs(row.scriptLogs).length > 0">
                <div class="expand-section-title">脚本日志</div>
                <div v-for="(entry, i) in parseScriptLogs(row.scriptLogs)" :key="i" class="script-log-entry">
                  <el-tag size="small" :type="logLevelType(entry.level)" effect="plain" style="margin-right: 8px;">
                    {{ entry.level }}
                  </el-tag>
                  <span class="script-log-msg">{{ entry.msg }}</span>
                </div>
              </template>
              <div v-if="row.errorMessage" class="expand-section-title error-text">错误信息</div>
              <div v-if="row.errorMessage" class="error-detail">{{ row.errorMessage }}</div>
              <div v-if="row.hazardPointIds" class="expand-meta">
                隐患点: {{ row.hazardPointIds }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="triggerType" label="触发" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="triggerTypeStyle(row.triggerType)" effect="plain">
              {{ triggerTypeLabel(row.triggerType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.resultStatus)" effect="dark">
              {{ statusLabel(row.resultStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultLevel" label="等级" width="70" align="center">
          <template #default="{ row }">
            <template v-if="row.resultLevel">
              <el-tag size="small" effect="dark"
                :style="{ backgroundColor: levelColor(row.resultLevel), borderColor: levelColor(row.resultLevel) }">
                {{ levelText(row.resultLevel) }}
              </el-tag>
            </template>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="80" align="center">
          <template #default="{ row }">{{ row.durationMs }}ms</template>
        </el-table-column>
        <el-table-column prop="triggeredCount" label="告警数" width="70" align="center" />
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
import { getExecutionLogs, type ExecutionLogItem } from '@/api/alarm'

const props = defineProps<{
  visible: boolean
  alarmId: number
  alarmName: string
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
}>()

const loading = ref(false)
const logs = ref<ExecutionLogItem[]>([])
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
    const res = await getExecutionLogs(props.alarmId, {
      pageNum: logPageNum.value,
      pageSize: logPageSize.value
    })
    logs.value = (res as any)?.rows || []
    logTotal.value = (res as any)?.total || 0
  } finally {
    loading.value = false
  }
}

function triggerTypeLabel(type: string): string {
  const map: Record<string, string> = {
    'CRON': '定时',
    'DATA_INGEST': '数据',
    'ALARM_TRIGGER': '级联'
  }
  return map[type] || type
}

function triggerTypeStyle(type: string): string {
  const map: Record<string, string> = {
    'CRON': 'primary',
    'DATA_INGEST': 'success',
    'ALARM_TRIGGER': 'warning'
  }
  return map[type] || 'info'
}

function statusType(status: string): string {
  if (status === 'SUCCESS') return 'success'
  if (status === 'NO_ALARM') return 'info'
  if (status === 'FAIL') return 'danger'
  if (status === 'TIMEOUT') return 'warning'
  return 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    'SUCCESS': '成功',
    'NO_ALARM': '无告警',
    'FAIL': '失败',
    'TIMEOUT': '超时'
  }
  return map[status] || status
}

function levelText(level: number): string {
  const map: Record<number, string> = { 1: '红色', 2: '橙色', 3: '黄色', 4: '蓝色' }
  return map[level] || `L${level}`
}

function levelColor(level: number): string {
  const map: Record<number, string> = { 1: '#F53F3F', 2: '#FF7D00', 3: '#e1ff00', 4: '#1890FF' }
  return map[level] || '#909399'
}

function logLevelType(level: string): string {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'info'
}

function parseScriptLogs(raw: string | null): Array<{ level: string; msg: string }> {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed
    return []
  } catch {
    return []
  }
}
</script>

<style scoped>
.expand-detail {
  padding: 8px 16px;
}

.expand-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
  margin-bottom: 6px;
}

.expand-meta {
  font-size: 12px;
  color: #86909c;
  margin-top: 8px;
}

.script-log-entry {
  display: flex;
  align-items: flex-start;
  margin-bottom: 4px;
  font-size: 12px;
}

.script-log-msg {
  font-family: 'Courier New', monospace;
  color: #4e5969;
  word-break: break-all;
}

.error-text {
  color: #f53f3f;
}

.error-detail {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #f53f3f;
  word-break: break-all;
  padding: 8px;
  background: #fff2f0;
  border-radius: 4px;
}
</style>
