<template>
  <div class="right-panel">
    <div class="panel-section online-section">
      <div class="section-header"><span class="section-title">设备在线状态</span></div>
      <div class="online-overview">
        <div class="online-rate-section">
          <div class="online-rate"><span class="rate-value">{{ onlineStats.onlineRate }}</span><span
              class="rate-unit">%</span></div>
          <div class="online-text">设备在线率</div>
        </div>
        <div class="online-count-section">
          <div class="online-numbers"><span class="online-count">{{ onlineStats.onlineCount }}</span><span
              class="online-separator">/</span><span class="total-count">{{ onlineStats.totalCount }}</span></div>
          <div class="online-label">台设备在线</div>
        </div>
      </div>
      <div class="online-chart">
        <div class="chart-title">分类型在线统计</div>
        <div class="type-bars">
          <div v-for="type in onlineStats.typeStats" :key="type.name" class="type-bar-item">
            <div class="bar-label">{{ type.name }}</div>
            <div class="bar-container">
              <div class="bar-fill" :style="{ width: (type.online / type.total * 100) + '%' }"></div>
            </div>
            <div class="bar-count">{{ type.online }}/{{ type.total }}</div>
          </div>
        </div>
      </div>
      <div class="online-trend">
        <div class="trend-header"><span class="trend-title">历史在线趋势</span><span
            class="trend-subtitle">最近7天</span></div>
        <div class="trend-chart">
          <div class="trend-y-axis"><span v-for="label in trendYLabels" :key="label">{{ label }}</span></div>
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
              <circle v-for="(p, i) in trendDataPoints" :key="i" :cx="p.x" :cy="p.y" r="4" fill="#52c41a" stroke="#fff"
                      stroke-width="2"/>
            </svg>
            <div class="trend-x-axis"><span v-for="label in trendXLabels" :key="label">{{ label }}</span></div>
          </div>
        </div>
      </div>
    </div>

    <div class="panel-section alarm-section">
      <div class="section-header"><span class="section-title">告警态势</span></div>
      <div class="alarm-chart-container">
        <div ref="alarmChartEl" class="alarm-chart"></div>
      </div>
      <div class="alarm-stats-row">
        <div v-for="item in alarmStats" :key="item.label" class="alarm-stat-item">
          <span class="stat-label">{{ item.label }}</span>
          <span class="stat-value" :style="{ color: item.color }">{{ item.value }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'

interface TypeStat {
  name: string;
  online: number;
  total: number
}

const props = defineProps<{
  onlineStats: { onlineRate: number; onlineCount: number; totalCount: number; typeStats: TypeStat[] }
  trendDataPoints: { x: number; y: number }[]
  trendYLabels: string[]
  trendXLabels: string[]
  alarmStats: { label: string; value: number; color: string }[]
  alarmChartEl: HTMLDivElement | null
}>()

const trendLinePath = computed(() => {
  const pts = props.trendDataPoints;
  if (!pts.length) return '';
  return pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const trendAreaPath = computed(() => {
  const lp = trendLinePath.value;
  if (!lp) return '';
  const pts = props.trendDataPoints;
  const lx = pts[pts.length - 1]?.x || 280;
  return `${lp} L ${lx} 100 L ${pts[0]?.x || 0} 100 Z`
})
</script>
