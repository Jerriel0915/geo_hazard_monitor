<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="告警详情"
    width="1200px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="data" class="alarm-detail-container">
      <!-- 左侧：页签内容 -->
      <div class="detail-main">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <!-- 基础信息 -->
          <el-tab-pane label="基础信息" name="basic">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="隐患点名称">{{ data.hazardPointName }}</el-descriptions-item>
              <el-descriptions-item label="告警等级">
                <el-tag :type="getAlarmLevelType(data.alarmLevel)">{{ getAlarmLevelText(data.alarmLevel) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="告警类型">{{ getAlarmTypeText(data.alarmType) }}</el-descriptions-item>
              <el-descriptions-item label="警情状态">
                <el-tag :type="getStatusType(data.status)">{{ getStatusText(data.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="首次告警时间">{{ data.firstAlarmTime }}</el-descriptions-item>
              <el-descriptions-item label="最后告警时间">{{ data.lastAlarmTime }}</el-descriptions-item>
              <el-descriptions-item label="告警次数">{{ data.alarmCount }}</el-descriptions-item>
              <el-descriptions-item label="响应人员">{{ data.responderName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="响应时间">{{ data.responseTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="告警内容" :span="2">{{ data.alarmContent }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 告警记录 -->
          <el-tab-pane label="告警记录" name="alarmRecords">
            <div class="tab-search-bar">
              <el-input
                v-model="alarmRecordSearch.description"
                placeholder="描述模糊搜索"
                clearable
                class="tab-search-input"
              />
              <el-date-picker
                v-model="alarmRecordSearch.timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="tab-search-date"
              />
              <el-button type="primary" size="small" @click="queryAlarmRecords">查询</el-button>
              <el-button size="small" @click="resetAlarmRecords">重置</el-button>
            </div>
            <div class="table-wrap">
              <div class="table-wrap__scroll">
                <div style="max-height: 300px;">
                  <el-table :data="filteredAlarmRecords" border stripe style="width: 100%">
                    <el-table-column prop="alarmTime" label="告警时间" width="180" />
                    <el-table-column prop="alarmLevel" label="告警等级" width="100">
                      <template #default="{ row }">
                        <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
                  </el-table>
                </div>
              </div>
            </div>
            <div v-if="filteredAlarmRecords.length === 0" class="empty-hint">暂无告警记录</div>
          </el-tab-pane>

          <!-- 通知记录 -->
          <el-tab-pane label="通知记录" name="notifyRecords">
            <div class="tab-search-bar">
              <el-input
                v-model="notifyRecordSearch.account"
                placeholder="账号模糊搜索"
                clearable
                class="tab-search-input"
              />
              <el-date-picker
                v-model="notifyRecordSearch.timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="tab-search-date"
              />
              <el-button type="primary" size="small" @click="queryNotifyRecords">查询</el-button>
              <el-button size="small" @click="resetNotifyRecords">重置</el-button>
            </div>
            <div class="table-wrap">
              <div class="table-wrap__scroll">
                <div style="max-height: 300px;">
                  <el-table :data="filteredNotifyRecords" border stripe style="width: 100%">
                    <el-table-column prop="notifyTime" label="通知时间" width="180" />
                    <el-table-column prop="channelType" label="渠道类型" width="100" />
                    <el-table-column prop="account" label="账号/电话/邮箱" min-width="160" show-overflow-tooltip />
                    <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
                    <el-table-column prop="success" label="是否成功" width="90">
                      <template #default="{ row }">
                        <el-tag :type="row.success ? 'success' : 'danger'" size="small">
                          {{ row.success ? '成功' : '失败' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </div>
            <div v-if="filteredNotifyRecords.length === 0" class="empty-hint">暂无通知记录</div>
          </el-tab-pane>

          <!-- 处置记录 -->
          <el-tab-pane label="处置记录" name="disposalRecords">
            <div class="table-wrap">
              <div class="table-wrap__scroll">
                <div style="max-height: 350px;">
                  <el-table :data="disposalRecords" border stripe style="width: 100%">
                    <el-table-column prop="disposalTime" label="处置时间" width="180" />
                    <el-table-column prop="disposalType" label="处置类型" width="100" />
                    <el-table-column prop="operator" label="处置人员" width="120" />
                    <el-table-column prop="result" label="处置结果" min-width="150" show-overflow-tooltip />
                    <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
                  </el-table>
                </div>
              </div>
            </div>
            <div v-if="disposalRecords.length === 0" class="empty-hint">暂无处置记录</div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 右侧：时间线 -->
      <div class="timeline-panel">
        <div class="timeline-header">
          <span class="timeline-title">时间线</span>
        </div>
        <div class="timeline-container">
          <div v-if="timelineData.length > 0" class="timeline">
            <div v-for="(item, index) in timelineData" :key="index" class="timeline-item">
              <div class="timeline-dot" :class="item.type"></div>
              <div v-if="index < timelineData.length - 1" class="timeline-line"></div>
              <div class="timeline-content">
                <div class="timeline-time">{{ item.time }}</div>
                <div class="timeline-desc">{{ item.description }}</div>
              </div>
            </div>
          </div>
          <div v-else class="timeline-empty">
            <span>暂无事件记录</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  data: Record<string, any> | null
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const activeTab = ref('basic')

// 告警记录搜索
const alarmRecordSearch = ref({
  description: '',
  timeRange: [] as string[]
})

const alarmRecords = ref<{ alarmTime: string; alarmLevel: string; description: string }[]>([])

// 当弹窗打开时，从 data.alarmList 填充告警记录
watch(() => props.modelValue, (val) => {
  if (val && props.data) {
    activeTab.value = 'basic'
    alarmRecordSearch.value = { description: '', timeRange: [] }
    notifyRecordSearch.value = { account: '', timeRange: [] }
    // 从现有数据的 alarmList 填充告警记录
    if (props.data.alarmList) {
      alarmRecords.value = props.data.alarmList.map((item: any) => ({
        alarmTime: item.alarmTime || '',
        alarmLevel: item.alarmLevel || '',
        description: item.alarmContent || ''
      }))
    } else {
      alarmRecords.value = []
    }
  }
})

const filteredAlarmRecords = computed(() => {
  let result = [...alarmRecords.value]
  if (alarmRecordSearch.value.description) {
    const kw = alarmRecordSearch.value.description.toLowerCase()
    result = result.filter(r => r.description.toLowerCase().includes(kw))
  }
  if (alarmRecordSearch.value.timeRange.length === 2) {
    const [start, end] = alarmRecordSearch.value.timeRange
    result = result.filter(r => r.alarmTime >= start && r.alarmTime <= end)
  }
  return result
})

const queryAlarmRecords = () => { /* 触发 computed 更新 */ }
const resetAlarmRecords = () => {
  alarmRecordSearch.value = { description: '', timeRange: [] }
}

// 通知记录搜索
const notifyRecordSearch = ref({
  account: '',
  timeRange: [] as string[]
})

const notifyRecords = ref<{ notifyTime: string; channelType: string; account: string; content: string; success: boolean }[]>([])

const filteredNotifyRecords = computed(() => {
  let result = [...notifyRecords.value]
  if (notifyRecordSearch.value.account) {
    const kw = notifyRecordSearch.value.account.toLowerCase()
    result = result.filter(r => r.account.toLowerCase().includes(kw))
  }
  if (notifyRecordSearch.value.timeRange.length === 2) {
    const [start, end] = notifyRecordSearch.value.timeRange
    result = result.filter(r => r.notifyTime >= start && r.notifyTime <= end)
  }
  return result
})

const queryNotifyRecords = () => { /* 触发 computed 更新 */ }
const resetNotifyRecords = () => {
  notifyRecordSearch.value = { account: '', timeRange: [] }
}

// 处置记录
const disposalRecords = ref<{ disposalTime: string; disposalType: string; operator: string; result: string; remark: string }[]>([])

// 时间线数据
const timelineData = ref<{ time: string; description: string; type: string }[]>([])

// 工具函数
const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'success',
    '4': 'info'
  }
  return map[level] || 'info'
}

const getAlarmLevelText = (level: string) => {
  const map: Record<string, string> = {
    '1': '一级',
    '2': '二级',
    '3': '三级',
    '4': '四级'
  }
  return map[level] || level
}

const getAlarmTypeText = (type: string) => {
  const map: Record<string, string> = {
    'threshold': '阈值预警',
    'comprehensive': '综合预警'
  }
  return map[type] || type
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'pending': 'danger',
    'processing': 'warning',
    'false_alarm': 'info',
    'closed': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'pending': '待处理',
    'processing': '处理中',
    'false_alarm': '误报',
    'closed': '已销警'
  }
  return map[status] || status
}
</script>

<style scoped>
.alarm-detail-container {
  display: flex;
  gap: 16px;
  min-height: 420px;
}

/* 左侧主区域 */
.detail-main {
  flex: 1;
  min-width: 0;
}

.detail-tabs {
  --el-tabs-header-height: 36px;
}

/* 页签内搜索栏 */
.tab-search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.tab-search-input {
  width: 180px;
}

.tab-search-date {
  width: 260px;
}

.empty-hint {
  text-align: center;
  padding: 32px 0;
  color: #909399;
  font-size: 13px;
}

/* 右侧时间线面板 */
.timeline-panel {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
}

.timeline-header {
  padding: 12px 14px 8px;
  border-bottom: 1px solid #f0f0f0;
}

.timeline-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.timeline-container {
  flex: 1;
  padding: 12px 14px;
  overflow-y: auto;
  min-height: 80px;
}

.timeline {
  position: relative;
  padding-left: 16px;
}

.timeline-item {
  position: relative;
  padding-bottom: 18px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -16px;
  top: 3px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d9d9d9;
  z-index: 1;
}

.timeline-dot.trigger {
  background: #f56c6c;
}

.timeline-dot.notify {
  background: #409eff;
}

.timeline-dot.dispose {
  background: #67c23a;
}

.timeline-dot.system {
  background: #909399;
}

.timeline-line {
  position: absolute;
  left: -12px;
  top: 14px;
  width: 2px;
  height: calc(100% - 4px);
  background: #e8e8e8;
}

.timeline-content {
  padding-left: 8px;
}

.timeline-time {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
  line-height: 1.4;
}

.timeline-desc {
  font-size: 12px;
  color: #303133;
  line-height: 1.5;
}

.timeline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 80px;
  color: #909399;
  font-size: 12px;
}
</style>
