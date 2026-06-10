<template>
  <div class="panel-section health-section">
    <div class="section-header">
      <span class="section-title-group">
        <el-icon class="section-icon" :size="18"><TrendCharts/></el-icon>
        <span class="section-title">系统健康度</span>
      </span>
      <span class="health-question" @mouseenter="showHealthPopover" @mouseleave="hideHealthPopover">
        <el-icon :size="16"><Warning/></el-icon>
      </span>
    </div>
    <div class="health-content">
      <div class="health-ring-container">
        <svg class="health-ring" viewBox="0 0 120 120">
          <circle class="ring-bg" cx="60" cy="60" r="50"/>
          <circle
              v-for="(segment, index) in ringSegments"
              :key="index"
              class="ring-segment"
              :class="{ active: activeSegment === index }"
              cx="60"
              cy="60"
              r="50"
              :stroke="segment.color"
              :stroke-dasharray="segment.dashArray"
              :stroke-dashoffset="segment.dashOffset"
              :style="{ transform: 'rotate(' + segment.rotate + 'deg)', transformOrigin: 'center' }"
              @mouseenter="activeSegment = index"
              @mouseleave="activeSegment = null"
          />
        </svg>
        <div class="ring-center">
          <div class="ring-score">{{ healthStats.overallScore }}%</div>
          <div class="ring-label">综合健康度</div>
        </div>
      </div>
      <div class="health-bars">
        <div
            v-for="(item, index) in healthStats.items"
            :key="item.name"
            class="health-bar-item"
            :class="{ active: activeSegment === index }"
            @mouseenter="activeSegment = index"
            @mouseleave="activeSegment = null"
        >
          <div class="bar-info">
            <span class="bar-name">{{ item.name }}</span>
            <span class="bar-value" :style="{ color: item.color }">{{ item.value }}%</span>
          </div>
          <div class="bar-track">
            <div
                class="bar-progress"
                :style="{ width: item.value + '%', backgroundColor: item.color }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div
          class="algorithm-popover"
          v-if="showAlgorithmDesc"
          :style="healthPopoverStyle"
          @mouseenter="cancelHealthPopoverHide"
          @mouseleave="hideHealthPopover"
      >
        <div class="popover-arrow"></div>
        <p>系统健康度综合评估以下五个维度：</p>
        <ul>
          <li><strong>资料完善率</strong>：设备资料登记率与隐患点资料完善率的综合指标</li>
          <li><strong>设备在线率</strong>：在线设备数/隐患点关联设备总数 × 100%</li>
          <li><strong>设备正常率</strong>：状态正常设备数/设备总数 × 100%</li>
          <li><strong>告警及时响应率</strong>：首次告警1小时内响应的事件数/告警事件总数 × 100%</li>
          <li><strong>边坡稳定率</strong>：最近一个月未有效告警隐患点数/总隐患点数 × 100%</li>
        </ul>
        <p style="margin-top: 8px;">综合得分 = 各维度得分 × 权重之和（环形图分色展示各维度占比）</p>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue'
import { TrendCharts, Warning } from '@element-plus/icons-vue'

interface HealthItem {
  name: string
  value: number
  weight: number
  color: string
}

interface HealthStats {
  overallScore: number
  items: HealthItem[]
}

const props = defineProps<{
  healthStats: HealthStats
}>()

const activeSegment = ref<number | null>(null)
const showAlgorithmDesc = ref(false)
const healthPopoverStyle = ref<Record<string, string>>({})
let healthPopoverHideTimer: ReturnType<typeof setTimeout> | null = null

const showHealthPopover = (e: MouseEvent) => {
  if (healthPopoverHideTimer) {
    clearTimeout(healthPopoverHideTimer)
    healthPopoverHideTimer = null
  }
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  healthPopoverStyle.value = {
    position: 'fixed',
    top: rect.bottom + 8 + 'px',
    left: Math.min(rect.left, window.innerWidth - 320) + 'px',
  }
  showAlgorithmDesc.value = true
}

const hideHealthPopover = () => {
  healthPopoverHideTimer = setTimeout(() => {
    showAlgorithmDesc.value = false
  }, 150)
}

const cancelHealthPopoverHide = () => {
  if (healthPopoverHideTimer) {
    clearTimeout(healthPopoverHideTimer)
    healthPopoverHideTimer = null
  }
}

const ringSegments = computed(() => {
  const circumference = 2 * Math.PI * 50
  let currentOffset = 0
  return props.healthStats.items.map((item, index) => {
    const segmentLength = (item.weight * circumference * item.value / 100)
    const gapLength = 3
    const segment = {
      color: item.color,
      dashArray: `${segmentLength} ${circumference - segmentLength}`,
      dashOffset: -currentOffset,
      rotate: (index * 72) - 90
    }
    currentOffset += segmentLength + gapLength
    return segment
  })
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
  height: 420px;
  box-sizing: border-box;
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
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.12) 0%, rgba(24, 144, 255, 0.03) 100%);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid rgba(24, 144, 255, 0.12);
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
  color: #1890ff;
}

.health-question {
  color: #1890ff;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.health-question:hover {
  background: rgba(24, 144, 255, 0.1);
}

.health-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.health-ring-container {
  position: relative;
  width: 110px;
  height: 110px;
  margin: 0 auto;
}

.health-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.health-ring :deep(.ring-bg) {
  fill: none;
  stroke: rgba(24, 144, 255, 0.08);
  stroke-width: 8;
}

.ring-bg {
  fill: none;
  stroke: rgba(24, 144, 255, 0.08);
  stroke-width: 8;
}

.ring-segment {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  transition: all 0.3s ease;
  cursor: pointer;
}

.ring-segment.active {
  stroke-width: 10;
  filter: drop-shadow(0 0 4px currentColor);
}

.ring-segment:not(.active) {
  opacity: 0.5;
}

.ring-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.ring-score {
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
  font-family: var(--font-display, inherit);
}

.ring-label {
  font-size: 10px;
  color: #86909c;
  margin-top: 2px;
}

.health-bars {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.health-bar-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: transparent;
}

.health-bar-item.active {
  background: rgba(24, 144, 255, 0.06);
}

.bar-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bar-name {
  font-size: 12px;
  color: #4e5969;
  font-weight: 500;
}

.bar-value {
  font-size: 13px;
  font-weight: 700;
  font-family: var(--font-display, inherit);
}

.bar-track {
  height: 4px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.bar-progress {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease;
}

/* Popover (not scoped since it's teleported to body) */
:global(.algorithm-popover) {
  position: fixed;
  width: 300px;
  background: #ffffff;
  border: 1px solid rgba(24, 144, 255, 0.15);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(24, 144, 255, 0.12);
  padding: 14px 16px;
  z-index: 2000;
  cursor: default;
  pointer-events: auto;
}

:global(.algorithm-popover .popover-arrow) {
  position: absolute;
  top: -6px;
  left: 8px;
  width: 12px;
  height: 12px;
  background: #fff;
  border-left: 1px solid rgba(24, 144, 255, 0.15);
  border-top: 1px solid rgba(24, 144, 255, 0.15);
  transform: rotate(45deg);
}

:global(.algorithm-popover p) {
  font-size: 12px;
  color: #1d2129;
  line-height: 1.5;
  margin: 0;
}

:global(.algorithm-popover ul) {
  margin: 6px 0 0 0;
  padding-left: 16px;
}

:global(.algorithm-popover li) {
  font-size: 12px;
  color: #4e5969;
  margin-bottom: 3px;
  line-height: 1.5;
}

:global(.algorithm-popover li strong) {
  color: #1890ff;
}
</style>
