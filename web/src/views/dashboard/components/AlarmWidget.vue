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
    <div class="alarm-level-stats">
      <div class="level-stat" v-for="level in alarmStats.levelStats" :key="level.name">
        <div class="level-dot" :class="level.key"></div>
        <span class="level-name">{{ level.name }}</span>
        <span class="level-count">{{ level.count }}</span>
      </div>
    </div>
    <div class="alarm-list-section">
      <div class="list-header">
        <span class="list-title">实时告警事件</span>
      </div>
      <div class="alarm-list">
        <div v-for="alarm in alarmStats.recentAlarms" :key="alarm.id" class="alarm-item">
          <div class="alarm-level-dot" :class="alarm.level"></div>
          <div class="alarm-content">
            <div class="alarm-title">{{ alarm.title }}</div>
            <div class="alarm-meta">{{ alarm.source }} · {{ alarm.time }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'

interface LevelStat {
  key: string
  name: string
  count: number
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
  padding-left: 10px;
  border-left: 3px solid #f5222d; /* red accent for alarms */
  margin-bottom: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-icon {
  color: #f5222d;
}

.alarm-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.alarm-summary-item {
  flex: 1;
  text-align: center;
  padding: 8px;
  background: rgba(245, 247, 250, 0.8);
  border-radius: 8px;
}

.summary-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
  margin-bottom: 4px;
}

.summary-badge.pending {
  background: rgba(250, 173, 20, 0.15);
  color: #faad14;
}

.summary-badge.history {
  background: rgba(24, 144, 255, 0.12);
  color: #1890ff;
}

.summary-count {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  font-family: var(--font-display);
}

.alarm-level-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
  border-top: 1px solid rgba(24, 144, 255, 0.12);
  border-bottom: 1px solid rgba(24, 144, 255, 0.12);
}

.level-stat {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.level-dot.critical {
  background: #f5222d;
}

.level-dot.major {
  background: #faad14;
}

.level-dot.minor {
  background: #722ed1;
}

.level-dot.info {
  background: #1890ff;
}

.level-name {
  flex: 1;
  font-size: 12px;
  color: #4e5969;
}

.level-count {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
}

.alarm-list-section {
  margin-top: 12px;
}

.list-header {
  margin-bottom: 8px;
}

.list-title {
  font-size: 12px;
  font-weight: 600;
  color: #4e5969;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 8px;
  background: rgba(245, 247, 250, 0.6);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.alarm-item:hover {
  background: rgba(24, 144, 255, 0.06);
}

.alarm-level-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 2px;
}

.alarm-level-dot.critical {
  background: #f5222d;
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
}

.alarm-meta {
  font-size: 11px;
  color: #86909c;
  margin-top: 2px;
}
</style>
