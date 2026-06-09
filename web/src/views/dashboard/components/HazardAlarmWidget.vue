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
      <div class="alarm-level-cards">
        <div class="level-card" v-for="level in levelStats" :key="level.key">
          <img :src="level.icon" class="level-card-img" :alt="level.name" />
          <span class="level-card-num" :class="level.key">{{ level.count }}</span>
          <div class="level-card-label">{{ level.name }}</div>
        </div>
      </div>
    </div>

    <!-- 实时警情列表 -->
    <div class="panel-section alarm-list-section">
      <div class="section-header list-header">
        <span class="section-title-group">
          <el-icon class="section-icon" :size="18"><Warning/></el-icon>
          <span class="section-title">实时警情</span>
        </span>
        <span class="list-count">{{ alarmList.length }}条</span>
      </div>
      <div v-if="loading" class="alarm-loading">加载中...</div>
      <div v-else-if="alarmList.length === 0" class="alarm-empty">暂无告警记录</div>
      <div v-else class="alarm-scroll">
        <div
          v-for="alarm in alarmList"
          :key="alarm.id"
          class="alarm-item"
          :class="`status-${alarm.alarmStatus}`"
          @click="showDetail(alarm)"
        >
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

    <!-- 告警详情弹框 -->
    <div v-if="detailVisible" class="alarm-detail-overlay" @click="detailVisible = false">
      <div class="alarm-detail-modal" @click.stop>
        <div class="detail-header">
          <span class="detail-title">告警详情</span>
          <button class="detail-close" @click="detailVisible = false">
            <el-icon :size="16"><Close/></el-icon>
          </button>
        </div>
        <div class="detail-body">
          <div class="detail-row">
            <span class="detail-label">告警类型</span>
            <span class="detail-value">{{ detailAlarm?.alarmTypeName || '--' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">告警等级</span>
            <span class="detail-value">{{ levelName(detailAlarm?.alarmLevel) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">告警状态</span>
            <span class="detail-value">{{ detailAlarm?.alarmStatusName || '--' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">告警详情</span>
            <span class="detail-value">{{ detailAlarm?.alarmDetail || '--' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">告警次数</span>
            <span class="detail-value">{{ detailAlarm?.alarmCount ?? '--' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">最近告警时间</span>
            <span class="detail-value">{{ detailAlarm?.lastAlarmTime || '--' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Bell, Close, Warning } from '@element-plus/icons-vue'
import { getRealtimeAlarmByHazardPoint, type RealtimeAlarmDetail } from '@/api/realtimeAlarm'

const props = defineProps<{
  hazardPointId: number | null
}>()

const loading = ref(false)
const alarmList = ref<RealtimeAlarmDetail[]>([])
const detailVisible = ref(false)
const detailAlarm = ref<RealtimeAlarmDetail | null>(null)

const pendingCount = computed(() =>
  alarmList.value.filter(a => a.alarmStatus === 0).length
)

const levelStats = computed(() => {
  const levels = [
    { key: 'critical', name: '一级', count: 0, icon: '/img/alarm/level1.png' },
    { key: 'major', name: '二级', count: 0, icon: '/img/alarm/level2.png' },
    { key: 'minor', name: '三级', count: 0, icon: '/img/alarm/level3.png' },
    { key: 'info', name: '四级', count: 0, icon: '/img/alarm/level4.png' }
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

const levelName = (level?: number): string => {
  const map: Record<number, string> = { 1: '一级', 2: '二级', 3: '三级', 4: '四级' }
  return level ? map[level] || '--' : '--'
}

const formatTime = (time: string) => {
  if (!time) return '--'
  return time.replace(/:\d{2}$/, '')
}

const showDetail = (alarm: RealtimeAlarmDetail) => {
  detailAlarm.value = alarm
  detailVisible.value = true
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
  gap: 10px;
  height: 100%;
}

.panel-section {
  background: #ffffff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 0;
  overflow: hidden;
  flex-shrink: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(245, 34, 45, 0.12) 0%, rgba(245, 34, 45, 0.04) 100%);
  border-bottom: 1px solid rgba(245, 34, 45, 0.15);
}

.section-title {
  font-size: 15px;
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
  padding: 14px 16px 0;
}

.alarm-summary-item {
  flex: 1;
  text-align: center;
  padding: 10px 8px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #e5e6eb;
}

.summary-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
  margin-bottom: 6px;
}

.summary-badge.pending {
  background: rgba(250, 173, 20, 0.15);
  color: #d48806;
}

.summary-badge.history {
  background: rgba(24, 144, 255, 0.12);
  color: #0958d9;
}

.summary-count {
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1;
}

.alarm-level-cards {
  display: flex;
  gap: 8px;
  padding: 12px 16px 14px;
}

.level-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px 8px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #e5e6eb;
}

.level-card-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.level-card-num {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.level-card-num.critical { color: #f5222d; }
.level-card-num.major { color: #d48806; }
.level-card-num.minor { color: #722ed1; }
.level-card-num.info { color: #1890ff; }

.level-card-label {
  font-size: 12px;
  color: #4e5969;
  font-weight: 500;
  white-space: nowrap;
}

/* 警情列表 — 撑满剩余高度 */
.alarm-list-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.list-header {
  background: linear-gradient(135deg, rgba(245, 34, 45, 0.12) 0%, rgba(245, 34, 45, 0.04) 100%);
  border-bottom: 1px solid rgba(245, 34, 45, 0.15);
  flex-shrink: 0;
}

.list-count {
  font-size: 13px;
  color: #6b7785;
  font-weight: 500;
}

.alarm-loading,
.alarm-empty {
  text-align: center;
  padding: 32px 0;
  font-size: 14px;
  color: #6b7785;
}

.alarm-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alarm-scroll::-webkit-scrollbar { width: 4px; }
.alarm-scroll::-webkit-scrollbar-thumb { background: rgba(0, 0, 0, 0.1); border-radius: 2px; }

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
  border: 1px solid transparent;
  border-left: 3px solid transparent;
}

.alarm-item:hover {
  background: #e8f4ff;
  border-color: #91caff;
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
  margin-top: 5px;
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
  font-size: 13px;
  color: #1d2129;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.5;
}

.alarm-meta {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  align-items: center;
}

.alarm-type-tag,
.alarm-status-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
}

.alarm-type-tag {
  background: rgba(24, 144, 255, 0.1);
  color: #1677ff;
}

.alarm-status-tag.tag-status-0 { background: rgba(245, 34, 45, 0.1); color: #cf1322; }
.alarm-status-tag.tag-status-1 { background: rgba(250, 173, 20, 0.1); color: #d48806; }
.alarm-status-tag.tag-status-2 { background: rgba(82, 196, 26, 0.1); color: #389e0d; }
.alarm-status-tag.tag-status-3 { background: rgba(0, 0, 0, 0.04); color: #86909c; }
.alarm-status-tag.tag-status-4 { background: rgba(24, 144, 255, 0.1); color: #0958d9; }

.alarm-time {
  font-size: 12px;
  color: #6b7785;
  margin-top: 2px;
}

.alarm-count-badge {
  font-size: 13px;
  font-weight: 700;
  color: #f5222d;
  min-width: 20px;
  text-align: center;
  align-self: center;
}

/* 告警详情弹框 */
.alarm-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.alarm-detail-modal {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
  width: 440px;
  max-width: 90vw;
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #e5e6eb;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.detail-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  color: #6b7785;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-close:hover {
  background: #f0f1f3;
  color: #1d2129;
}

.detail-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-row {
  display: flex;
  gap: 16px;
}

.detail-label {
  font-size: 13px;
  color: #6b7785;
  white-space: nowrap;
  min-width: 80px;
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: #1d2129;
  word-break: break-all;
  line-height: 1.5;
}
</style>
