<template>
  <div class="panel-section online-section">
    <div class="section-header"><span class="section-title">设备在线状态</span></div>
    <div class="online-overview">
      <div class="online-rate-section">
        <div class="online-rate"><span class="rate-value">{{ stats.onlineRate }}</span><span class="rate-unit">%</span>
        </div>
        <div class="online-text">设备在线率</div>
      </div>
      <div class="online-count-section">
        <div class="online-numbers"><span class="online-count">{{ stats.onlineCount }}</span><span
            class="online-separator">/</span><span class="total-count">{{ stats.totalCount }}</span></div>
        <div class="online-label">台设备在线</div>
      </div>
    </div>
    <div class="online-trend">
      <div class="trend-header"><span class="trend-title">历史在线趋势</span><span class="trend-subtitle">最近7天</span>
      </div>
      <div class="trend-chart">
        <div class="trend-y-axis"><span v-for="l in yLabels" :key="l">{{ l }}</span></div>
        <div class="trend-area">
          <svg class="trend-svg" viewBox="0 0 280 100" preserveAspectRatio="none">
            <defs>
              <linearGradient id="tg" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="rgba(82,196,26,0.4)"/>
                <stop offset="100%" stop-color="rgba(82,196,26,0)"/>
              </linearGradient>
            </defs>
            <path :d="areaPath" fill="url(#tg)"/>
            <path :d="linePath" fill="none" stroke="#52c41a" stroke-width="2"/>
            <circle v-for="(p,i) in points" :key="i" :cx="p.x" :cy="p.y" r="4" fill="#52c41a" stroke="#fff"
                    stroke-width="2"/>
          </svg>
          <div class="trend-x-axis"><span v-for="l in xLabels" :key="l">{{ l }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'

interface Point {
  x: number;
  y: number
}

const props = defineProps<{
  stats: { onlineRate: number; onlineCount: number; totalCount: number };
  points: Point[];
  yLabels: string[];
  xLabels: string[]
}>()

const linePath = computed(() => {
  const p = props.points;
  return p.length ? p.map((d, i) => `${i ? 'L' : 'M'} ${d.x} ${d.y}`).join(' ') : ''
})
const areaPath = computed(() => {
  const l = linePath.value;
  if (!l) return '';
  const p = props.points;
  return `${l} L ${p[p.length - 1]?.x || 280} 100 L ${p[0]?.x || 0} 100 Z`
})
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
}

.online-overview {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-bottom: 20px;
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
  font-size: 36px;
  font-weight: 700;
  color: #52c41a;
}

.rate-unit {
  font-size: 18px;
  color: #9ca3af;
}

.online-text {
  font-size: 14px;
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
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.online-separator {
  color: #9ca3af;
}

.total-count {
  font-size: 18px;
  color: #6b7280;
}

.online-label {
  font-size: 14px;
  color: #6b7280;
}

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
  font-size: 17px;
  font-weight: 600;
  color: #374151;
}

.trend-subtitle {
  font-size: 14px;
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
  font-size: 14px;
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
  min-height: 80px;
}

.trend-x-axis {
  display: flex;
  justify-content: space-between;
  padding-top: 6px;
  overflow: hidden;
}

.trend-x-axis span {
  flex: 1;
  min-width: 0;
  font-size: 11px;
  color: #9ca3af;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
