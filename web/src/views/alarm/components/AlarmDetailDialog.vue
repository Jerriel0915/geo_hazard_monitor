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
              <el-descriptions-item label="首次告警时间">{{ data.firstTriggerTime }}</el-descriptions-item>
              <el-descriptions-item label="最后告警时间">{{ data.lastTriggerTime }}</el-descriptions-item>
              <el-descriptions-item label="告警次数">{{ data.triggerCount }}</el-descriptions-item>
              <el-descriptions-item label="响应人员">{{ data.resolvedBy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="响应时间">{{ data.resolvedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="告警内容" :span="2">{{ data.alarmMessage }}</el-descriptions-item>
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
                    <el-table-column prop="triggerTime" label="告警时间" width="180" />
                    <el-table-column prop="alarmLevel" label="告警等级" width="100">
                      <template #default="{ row }">
                        <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="alarmMessage" label="描述" min-width="200" show-overflow-tooltip />
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
                    <el-table-column prop="createTime" label="处置时间" width="180" />
                    <el-table-column prop="actionType" label="动作类型" width="140">
                      <template #default="{ row }">{{ getActionTypeText(row.actionType) }}</template>
                    </el-table-column>
                    <el-table-column prop="operator" label="处置人员" width="120" />
                    <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
                    <el-table-column prop="remarks" label="备注" min-width="180" show-overflow-tooltip />
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
import {
  getAlarmRecordDetail,
  getTriggerDetails,
  getActionLogs,
  type AlarmRecordItem,
  type AlarmRecordTriggerDetail,
  type AlarmRecordActionLog,
} from '@/api/alarm'

const props = defineProps<{
  modelValue: boolean
  data: Record<string, any> | null
}>()

defineEmits<{ 'update:modelValue': [value: boolean] }>()

const activeTab = ref('basic')
const detail = ref<AlarmRecordItem | null>(null)

const alarmRecordSearch = ref({ description: '', timeRange: [] as string[] })
const triggerDetails = ref<AlarmRecordTriggerDetail[]>([])

const disposalRecords = ref<AlarmRecordActionLog[]>([])

interface TimelineNode { time: string; description: string; type: string }
const timelineData = ref<TimelineNode[]>([])

// 通知记录 tab：暂不对接，保留搜索结构 + 空数据
const notifyRecordSearch = ref({ account: '', timeRange: [] as string[] })
const filteredNotifyRecords = computed(() => [])
const queryNotifyRecords = () => {}
const resetNotifyRecords = () => { notifyRecordSearch.value = { account: '', timeRange: [] } }

// 弹窗打开时并发拉取
watch(() => props.modelValue, async (val) => {
  if (!val || !props.data?.id) return
  activeTab.value = 'basic'
  alarmRecordSearch.value = { description: '', timeRange: [] }
  notifyRecordSearch.value = { account: '', timeRange: [] }
  const id = Number(props.data.id)

  try {
    const [d, t, l] = await Promise.all([
      getAlarmRecordDetail(id),
      getTriggerDetails(id),
      getActionLogs(id),
    ])
    const detailData = (d as any).data ?? d
    detail.value = detailData ?? null
    triggerDetails.value = (t as any).data ?? t ?? []
    const logs: AlarmRecordActionLog[] = (l as any).data ?? l ?? []
    disposalRecords.value = logs.filter((x: AlarmRecordActionLog) =>
      ['FEEDBACK', 'DISPOSE_CLOSE', 'DISPOSE_FALSE_ALARM'].includes(x.actionType))
    timelineData.value = buildTimeline(logs)
  } catch (e) {
    detail.value = null
    triggerDetails.value = []
    disposalRecords.value = []
    timelineData.value = []
  }
})

// 由动作日志构造时间线
function buildTimeline(logs: AlarmRecordActionLog[]): TimelineNode[] {
  return [...logs].sort((a, b) => (a.createTime || '').localeCompare(b.createTime || '')).map(log => {
    const typeMap: Record<string, string> = {
      CREATE: 'trigger', RE_TRIGGER: 'trigger', LEVEL_CHANGE: 'trigger',
      NOTIFY: 'notify',
      FEEDBACK: 'dispose', DISPOSE_CLOSE: 'dispose', DISPOSE_FALSE_ALARM: 'dispose',
    }
    const descMap: Record<string, string> = {
      CREATE: '告警创建', RE_TRIGGER: '告警再次触发',
      LEVEL_CHANGE: `等级变化 ${log.fromValue}→${log.toValue}`,
      FEEDBACK: '处置反馈', DISPOSE_CLOSE: '告警销警',
      DISPOSE_FALSE_ALARM: '标记误报', NOTIFY: `通知发送：${log.remarks || ''}`,
    }
    return {
      time: log.createTime,
      description: descMap[log.actionType] || log.actionType,
      type: typeMap[log.actionType] || 'system',
    }
  })
}

const filteredAlarmRecords = computed(() => {
  let list = triggerDetails.value
  if (alarmRecordSearch.value.description) {
    const kw = alarmRecordSearch.value.description.toLowerCase()
    list = list.filter(r => (r.alarmMessage || '').toLowerCase().includes(kw))
  }
  if (alarmRecordSearch.value.timeRange.length === 2) {
    const [s, e] = alarmRecordSearch.value.timeRange
    list = list.filter(r => r.triggerTime >= s && r.triggerTime <= e)
  }
  return list
})

const queryAlarmRecords = () => {}
const resetAlarmRecords = () => { alarmRecordSearch.value = { description: '', timeRange: [] } }

// 枚举映射（数字/大写）
const getAlarmLevelType = (level: number | string) => {
  const n = Number(level)
  return ({ 1: 'info', 2: 'warning', 3: 'warning', 4: 'danger' } as Record<number, string>)[n] || 'info'
}
const getAlarmLevelText = (level: number | string) => {
  const n = Number(level)
  return ({ 1: '一级', 2: '二级', 3: '三级', 4: '四级' } as Record<number, string>)[n] || String(level)
}
const getAlarmTypeText = (type: string) =>
  ({ THRESHOLD: '阈值预警', COMPREHENSIVE: '综合预警' } as Record<string, string>)[type] || type
const getStatusType = (status: number | string) => {
  const n = Number(status)
  return ({ 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' } as Record<number, string>)[n] || 'info'
}
const getStatusText = (status: number | string) => {
  const n = Number(status)
  return ({ 1: '待处理', 2: '处理中', 3: '已销警', 4: '误报' } as Record<number, string>)[n] || String(status)
}
const getActionTypeText = (t: string) =>
  ({ CREATE: '创建', RE_TRIGGER: '再次触发', LEVEL_CHANGE: '等级变化',
     FEEDBACK: '处置反馈', DISPOSE_CLOSE: '销警', DISPOSE_FALSE_ALARM: '误报',
     NOTIFY: '通知发送' } as Record<string, string>)[t] || t
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
