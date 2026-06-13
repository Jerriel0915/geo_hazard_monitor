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
    <div class="online-chart">
      <div class="chart-title">分类型在线统计</div>
      <div class="type-bars">
        <div v-for="type in stats.typeStats" :key="type.name" class="type-bar-item">
          <div class="bar-label">{{ type.name }}</div>
          <div class="bar-container">
            <div class="bar-fill" :style="{ width: (type.online / type.total * 100) + '%' }"></div>
          </div>
          <div class="bar-count">{{ type.online }}/{{ type.total }}</div>
        </div>
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

interface TypeStat {
  name: string;
  online: number;
  total: number
}

interface Point {
  x: number;
  y: number
}

const props = defineProps<{
  stats: { onlineRate: number; onlineCount: number; totalCount: number; typeStats: TypeStat[] };
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
