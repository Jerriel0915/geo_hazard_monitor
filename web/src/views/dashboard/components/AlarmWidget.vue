<template>
  <div class="panel-section alarm-section">
    <div class="section-header">
      <span class="section-title-group">
        <el-icon class="section-icon" :size="18"><Bell/></el-icon>
        <span class="section-title">告警态势</span>
      </span>
    </div>
    <div class="alarm-summary">
      <div class="alarm-summary-item">
        <div class="summary-badge pending">待办告警</div>
        <div class="summary-count">{{ alarmStats.pendingCount }}</div>
      </div>
      <div class="alarm-summary-item">
        <div class="summary-badge history">历史告警</div>
        <div class="summary-count">{{ alarmStats.historyCount }}</div>
      </div>
    </div>
    <div class="alarm-level-cards">
      <div class="level-card" v-for="level in alarmStats.levelStats" :key="level.key">
        <img :src="level.icon" class="level-card-img" :alt="level.name" />
        <span class="level-card-num" :class="level.key">{{ level.count }}</span>
        <div class="level-card-label">{{ level.name }}</div>
      </div>
    </div>
    <div class="alarm-list-section">
      <div class="list-header">
        <span class="list-title">实时告警事件</span>
        <span class="list-count">{{ alarmStats.recentAlarms.length }}条</span>
      </div>
      <div v-if="alarmStats.recentAlarms.length" class="alarm-list">
        <div v-for="alarm in alarmStats.recentAlarms" :key="alarm.id" class="alarm-item">
          <div class="alarm-level-dot" :class="alarm.level"></div>
          <div class="alarm-content">
            <div class="alarm-title">{{ alarm.title }}</div>
            <div class="alarm-meta">{{ alarm.source }} · {{ alarm.time }}</div>
          </div>
        </div>
      </div>
      <div v-else class="alarm-list-empty">暂无告警事件</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'

interface LevelStat {
  key: string
  name: string
  count: number
  icon: string
}

interface RecentAlarm {
  id: number
  level: string
  title: string
  source: string
  time: string
}

interface AlarmStats {
  pendingCount: number
  historyCount: number
  levelStats: LevelStat[]
  recentAlarms: RecentAlarm[]
}

defineProps<{
  alarmStats: AlarmStats
}>()
</script>

<style scoped>
.panel-section {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(24, 144, 255, 0.08);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.06);
  padding: 16px 18px;
  margin-bottom: 12px;
  flex-shrink: 0;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.panel-section:last-child {
  margin-bottom: 0;
}

.panel-section:hover {
  border-color: rgba(24, 144, 255, 0.15);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 10px;
  background: linear-gradient(135deg, rgba(245, 34, 45, 0.12) 0%, rgba(245, 34, 45, 0.03) 100%);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(245, 34, 45, 0.12);
  margin: -16px -18px 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  font-family: var(--font-display, inherit);
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
  background: rgba(245, 247, 250, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.summary-badge {
  font-size: 10px;
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
  font-family: var(--font-display, inherit);
  line-height: 1;
}

.alarm-level-cards {
  display: flex;
  gap: 8px;
  padding: 10px 0;
}

.level-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px 8px;
  background: rgba(245, 247, 250, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.04);
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

.alarm-list-section {
  margin-top: 14px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.list-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.list-count {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 8px 10px;
  background: rgba(245, 247, 250, 0.5);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.alarm-item:hover {
  background: rgba(24, 144, 255, 0.06);
  border-left-color: rgba(24, 144, 255, 0.3);
}

.alarm-level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 4px;
}

.alarm-level-dot.critical {
  background: #f5222d;
  box-shadow: 0 0 4px rgba(245, 34, 45, 0.4);
}

.alarm-level-dot.major {
  background: #faad14;
}

.alarm-level-dot.minor {
  background: #722ed1;
}

.alarm-level-dot.info {
  background: #1890ff;
}

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
  font-size: 11px;
  color: #86909c;
  margin-top: 2px;
}

.alarm-list-empty {
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
  color: #86909c;
}
</style>
