<template>
  <div class="panel-section device-status-section">
    <div class="section-header">
      <span class="section-title-group">
        <el-icon class="section-icon" :size="18"><Monitor/></el-icon>
        <span class="section-title">设备在线状态</span>
      </span>
    </div>

    <!-- 概览：在线率 + 在线数/总数 -->
    <div class="online-overview">
      <div class="online-rate-section">
        <div class="online-rate">
          <span class="rate-value">{{ stats.onlineRate }}</span>
          <span class="rate-unit">%</span>
        </div>
        <div class="online-text">设备在线率</div>
      </div>
      <div class="online-count-section">
        <div class="online-numbers">
          <span class="online-count">{{ stats.onlineCount }}</span>
          <span class="online-separator">/</span>
          <span class="total-count">{{ stats.totalCount }}</span>
        </div>
        <div class="online-label">台设备在线</div>
      </div>
    </div>

    <!-- 分类型在线统计 -->
    <div class="online-chart">
      <div class="chart-title">分类型在线统计</div>
      <div class="type-bars">
        <div v-for="item in stats.typeStats" :key="item.name" class="type-bar-item">
          <div class="bar-label">{{ item.name }}</div>
          <div class="bar-container">
            <div class="bar-fill" :style="{ width: barWidth(item.online, item.total) }"></div>
          </div>
          <div class="bar-count">{{ item.online }}/{{ item.total }}</div>
        </div>
      </div>
    </div>

    <!-- 历史在线趋势 -->
    <div class="online-trend">
      <div class="trend-header">
        <span class="trend-title">历史在线趋势</span>
        <span v-if="trendData.length >= 2" class="trend-subtitle">按监测类型</span>
      </div>
      <template v-if="trendData.length >= 2">
        <div class="trend-chart">
          <div class="trend-y-axis">
            <span v-for="label in trendYLabels" :key="label">{{ label }}</span>
          </div>
          <div class="trend-area">
            <svg class="trend-svg" viewBox="0 0 280 100" preserveAspectRatio="none">
              <defs>
                <linearGradient id="trendGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="rgba(82, 196, 26, 0.4)"/>
                  <stop offset="100%" stop-color="rgba(82, 196, 26, 0)"/>
                </linearGradient>
              </defs>
              <path :d="trendAreaPath" fill="url(#trendGradient)"/>
              <path :d="trendLinePath" fill="none" stroke="#52c41a" stroke-width="2"/>
              <circle
                v-for="(point, index) in trendDataPoints"
                :key="index"
                :cx="point.x"
                :cy="point.y"
                r="4"
                fill="#52c41a"
                stroke="#fff"
                stroke-width="2"
              />
            </svg>
            <div class="trend-x-axis">
              <span v-for="label in trendXLabels" :key="label">{{ label }}</span>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="trend-empty">暂无趋势数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Monitor } from '@element-plus/icons-vue'

interface TypeStat {
  name: string
  online: number
  total: number
}

interface DeviceStatusStats {
  onlineRate: number
  onlineCount: number
  totalCount: number
  typeStats: TypeStat[]
}

const props = defineProps<{
  stats: DeviceStatusStats
  trendData: number[]
  trendLabels?: string[]
}>()

const barWidth = (online: number, total: number) => {
  if (!total) return '0%'
  return Math.round((online / total) * 100) + '%'
}

// ---- 趋势图计算 ----
const trendYLabels = ['100', '80', '60', '40', '20', '0']

const trendXLabels = computed(() => {
  if (props.trendLabels?.length) return props.trendLabels
  // 自动生成最近 N 天的日期标签
  const count = props.trendData.length
  const labels: string[] = []
  const now = new Date()
  for (let i = count - 1; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 86400000)
    labels.push(`${d.getMonth() + 1}-${d.getDate()}`)
  }
  return labels
})

const trendDataPoints = computed(() => {
  const data = props.trendData
  if (data.length < 2) return []
  const padding = 14
  const width = 280 - padding * 2
  const step = data.length > 1 ? width / (data.length - 1) : 0
  return data.map((value, index) => ({
    x: padding + index * step,
    y: Math.max(0, Math.min(100, 100 - (value ?? 0)))
  }))
})

const trendLinePath = computed(() => {
  const points = trendDataPoints.value
  if (points.length === 0) return ''
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const trendAreaPath = computed(() => {
  const linePath = trendLinePath.value
  if (!linePath) return ''
  const points = trendDataPoints.value
  const lastX = points[points.length - 1]?.x || 280
  return `${linePath} L ${lastX} 100 L ${points[0]?.x || 0} 100 Z`
})
</script>

<style scoped>
.panel-section {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(24, 144, 255, 0.08);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 16px 18px;
  margin-bottom: 12px;
  flex-shrink: 0;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.panel-section:hover {
  border-color: rgba(24, 144, 255, 0.15);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.panel-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 10px;
  background: linear-gradient(135deg, rgba(82, 196, 26, 0.12) 0%, rgba(82, 196, 26, 0.03) 100%);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(82, 196, 26, 0.12);
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
  color: #52c41a;
}

/* 概览 */
.online-overview {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(82, 196, 26, 0.08);
  border-radius: 8px;
}

.online-rate-section {
  text-align: center;
}

.online-rate {
  display: flex;
  align-items: baseline;
  justify-content: center;
  margin-bottom: 4px;
}

.rate-value {
  font-size: 32px;
  font-weight: 700;
  color: #52c41a;
}

.rate-unit {
  font-size: 16px;
  color: #9ca3af;
}

.online-text {
  font-size: 13px;
  color: #6b7280;
}

.online-count-section {
  text-align: center;
}

.online-numbers {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 4px;
}

.online-count {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.online-separator {
  color: #9ca3af;
}

.total-count {
  font-size: 16px;
  color: #6b7280;
}

.online-label {
  font-size: 13px;
  color: #6b7280;
}

/* 分类型统计 */
.online-chart {
  margin-top: 4px;
}

.chart-title {
  font-size: 13px;
  color: #4b5563;
  font-weight: 500;
  margin-bottom: 10px;
}

.type-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-label {
  width: 56px;
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-container {
  flex: 1;
  height: 10px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 5px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #52c41a, #4facfe);
  border-radius: 5px;
  transition: width 0.5s ease;
}

.bar-count {
  width: 44px;
  font-size: 12px;
  color: #374151;
  text-align: right;
  white-space: nowrap;
}

/* 历史在线趋势 */
.online-trend {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(79, 172, 254, 0.2);
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.trend-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.trend-subtitle {
  font-size: 12px;
  color: #9ca3af;
}

.trend-chart {
  display: flex;
  gap: 8px;
}

.trend-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 0 2px;
}

.trend-y-axis span {
  font-size: 11px;
  color: #9ca3af;
}

.trend-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.trend-svg {
  flex: 1;
  width: 100%;
  min-height: 70px;
}

.trend-x-axis {
  display: flex;
  justify-content: space-around;
  padding-top: 4px;
}

.trend-x-axis span {
  font-size: 11px;
  color: #9ca3af;
}

.trend-empty {
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
  color: #86909c;
}
</style>
