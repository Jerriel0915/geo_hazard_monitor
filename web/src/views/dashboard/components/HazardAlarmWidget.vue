<template>
  <div class="hazard-alarm-widget">
    <!-- 告警概况 -->
    <div class="panel-section alarm-section">
      <div class="section-header">
        <span class="section-title-group">
          <el-icon class="section-icon" :size="18"><Bell/></el-icon>
          <span class="section-title">告警情况</span>
        </span>
      </div>
      <div class="alarm-summary">
        <div class="alarm-summary-item">
          <div class="summary-badge pending">待办告警</div>
          <div class="summary-count">{{ pendingCount }}</div>
        </div>
        <div class="alarm-summary-item">
          <div class="summary-badge history">历史告警</div>
          <div class="summary-count">{{ alarmList.length }}</div>
        </div>
      </div>
      <div class="alarm-level-stats">
        <div class="level-stat" v-for="level in levelStats" :key="level.key">
          <div class="level-dot" :class="level.key"></div>
          <span class="level-name">{{ level.name }}</span>
          <span class="level-count">{{ level.count }}</span>
        </div>
      </div>
    </div>

    <!-- 实时警情列表 -->
    <div class="panel-section alarm-list-section">
      <div class="list-section-header">
        <span class="list-title">实时警情</span>
        <span class="list-count">{{ alarmList.length }}条</span>
      </div>
      <div v-if="loading" class="alarm-loading">加载中...</div>
      <div v-else-if="alarmList.length === 0" class="alarm-empty">暂无告警记录</div>
      <div v-else class="alarm-list">
        <div v-for="alarm in alarmList" :key="alarm.id" class="alarm-item" :class="`status-${alarm.alarmStatus}`">
          <div class="alarm-level-dot" :class="levelKey(alarm.alarmLevel)"></div>
          <div class="alarm-content">
            <div class="alarm-title">{{ alarm.alarmDetail || alarm.alarmTypeName }}</div>
            <div class="alarm-meta">
              <span class="alarm-type-tag">{{ alarm.alarmTypeName }}</span>
              <span class="alarm-status-tag" :class="`tag-status-${alarm.alarmStatus}`">{{ alarm.alarmStatusName }}</span>
            </div>
            <div class="alarm-time">{{ formatTime(alarm.lastAlarmTime) }}</div>
          </div>
          <div class="alarm-count-badge">{{ alarm.alarmCount }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { getRealtimeAlarmByHazardPoint, type RealtimeAlarmDetail } from '@/api/realtimeAlarm'

const props = defineProps<{
  hazardPointId: number | null
}>()

const loading = ref(false)
const alarmList = ref<RealtimeAlarmDetail[]>([])

const pendingCount = computed(() =>
  alarmList.value.filter(a => a.alarmStatus === 0).length
)

const levelStats = computed(() => {
  const levels = [
    { key: 'critical', name: '一级', count: 0 },
    { key: 'major', name: '二级', count: 0 },
    { key: 'minor', name: '三级', count: 0 },
    { key: 'info', name: '四级', count: 0 }
  ]
  const map: Record<number, number> = { 1: 0, 2: 1, 3: 2, 4: 3 }
  alarmList.value.forEach(a => {
    const idx = map[a.alarmLevel]
    if (idx !== undefined) levels[idx].count++
  })
  return levels
})

const levelKey = (level: number): string => {
  const map: Record<number, string> = { 1: 'critical', 2: 'major', 3: 'minor', 4: 'info' }
  return map[level] || 'info'
}

const formatTime = (time: string) => {
  if (!time) return '--'
  return time.replace(/:\d{2}$/, '')
}

const fetchAlarms = async () => {
  if (!props.hazardPointId) {
    alarmList.value = []
    return
  }
  loading.value = true
  try {
    const res = await getRealtimeAlarmByHazardPoint(String(props.hazardPointId))
    if (res.code === 200 && res.data) {
      alarmList.value = (res.data as RealtimeAlarmDetail[])
        .sort((a, b) => new Date(b.lastAlarmTime).getTime() - new Date(a.lastAlarmTime).getTime())
    }
  } catch {
    alarmList.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.hazardPointId, fetchAlarms, { immediate: true })
</script>

<style scoped>
.hazard-alarm-widget {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-section {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(24, 144, 255, 0.15);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 16px 18px;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.panel-section:hover {
  border-color: rgba(24, 144, 255, 0.25);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 10px;
  background: linear-gradient(135deg, rgba(245, 34, 45, 0.18) 0%, rgba(245, 34, 45, 0.06) 100%);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(245, 34, 45, 0.18);
  margin: -16px -18px 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  color: #f5222d;
}

.alarm-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
}

.alarm-summary-item {
  flex: 1;
  text-align: center;
  padding: 10px 8px;
  background: rgba(245, 247, 250, 0.9);
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.summary-badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
  margin-bottom: 6px;
}

.summary-badge.pending {
  background: rgba(250, 173, 20, 0.2);
  color: #d48806;
}

.summary-badge.history {
  background: rgba(24, 144, 255, 0.18);
  color: #0958d9;
}

.summary-count {
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1;
}

.alarm-level-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.level-stat {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}

.level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.level-dot.critical { background: #f5222d; }
.level-dot.major { background: #faad14; }
.level-dot.minor { background: #722ed1; }
.level-dot.info { background: #1890ff; }

.level-name {
  flex: 1;
  font-size: 12px;
  color: #4e5969;
  font-weight: 500;
}

.level-count {
  font-size: 14px;
  font-weight: 700;
  color: #1d2129;
  min-width: 20px;
  text-align: right;
}

/* 警情列表 */
.list-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.list-title {
  font-size: 11px;
  font-weight: 500;
  color: #86909c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.list-count {
  font-size: 11px;
  color: #86909c;
}

.alarm-loading,
.alarm-empty {
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
  color: #86909c;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(245, 247, 250, 0.85);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.alarm-item:hover {
  background: rgba(24, 144, 255, 0.08);
  border-left-color: rgba(24, 144, 255, 0.4);
}

.alarm-item.status-0 {
  border-left-color: #f5222d;
}

.alarm-level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 4px;
}

.alarm-level-dot.critical { background: #f5222d; box-shadow: 0 0 4px rgba(245, 34, 45, 0.4); }
.alarm-level-dot.major { background: #faad14; }
.alarm-level-dot.minor { background: #722ed1; }
.alarm-level-dot.info { background: #1890ff; }

.alarm-content {
  flex: 1;
  min-width: 0;
}

.alarm-title {
  font-size: 12px;
  color: #1d2129;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.alarm-meta {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  align-items: center;
}

.alarm-type-tag,
.alarm-status-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
}

.alarm-type-tag {
  background: rgba(24, 144, 255, 0.12);
  color: #1890ff;
}

.alarm-status-tag.tag-status-0 { background: rgba(245, 34, 45, 0.15); color: #cf1322; }
.alarm-status-tag.tag-status-1 { background: rgba(250, 173, 20, 0.15); color: #d48806; }
.alarm-status-tag.tag-status-2 { background: rgba(82, 196, 26, 0.15); color: #389e0d; }
.alarm-status-tag.tag-status-3 { background: rgba(0, 0, 0, 0.06); color: #86909c; }
.alarm-status-tag.tag-status-4 { background: rgba(24, 144, 255, 0.15); color: #0958d9; }

.alarm-time {
  font-size: 11px;
  color: #86909c;
  margin-top: 2px;
}

.alarm-count-badge {
  font-size: 12px;
  font-weight: 700;
  color: #f5222d;
  min-width: 20px;
  text-align: center;
  align-self: center;
}
</style>
