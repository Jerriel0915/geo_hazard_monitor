<template>
  <div class="panel-section health-section">
    <div class="section-header">
      <span class="section-title">系统健康度</span>
      <span ref="triggerRef" class="health-question" @mouseenter="showPopover" @mouseleave="hidePopover">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
          <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17"
                                                                                                x2="12.01" y2="17"/>
        </svg>
      </span>
    </div>
    <div class="health-content">
      <div class="health-ring-container">
        <svg class="health-ring" viewBox="0 0 200 200">
          <circle class="ring-bg" cx="100" cy="100" r="85"/>
          <circle v-for="(seg, i) in ringSegments" :key="i" class="ring-segment"
                  :class="['segment-' + (i + 1), { active: activeSegment === i }]" cx="100" cy="100" r="85"
                  :stroke="seg.color" :stroke-dasharray="seg.dashArray" :stroke-dashoffset="seg.dashOffset"
                  :style="{ transform: 'rotate(' + seg.rotate + 'deg)', transformOrigin: 'center' }"
                  @mouseenter="activeSegment = i" @mouseleave="activeSegment = null"/>
        </svg>
        <div class="ring-center">
          <div class="ring-score">{{ healthStats.overallScore }}%</div>
          <div class="ring-label">综合健康度</div>
        </div>
      </div>
      <div class="health-bars">
        <div v-for="(item, i) in healthStats.items" :key="item.name" class="health-bar-item"
             :class="{ active: activeSegment === i }" @mouseenter="activeSegment = i" @mouseleave="activeSegment = null"
             @click="activeSegment = activeSegment === i ? null : i">
          <div class="bar-info"><span class="bar-name">{{ item.name }}</span><span class="bar-value"
                                                                                   :style="{ color: item.color }">{{
              item.value
            }}%</span></div>
          <div class="bar-track">
            <div class="bar-progress" :style="{ width: item.value + '%', backgroundColor: item.color }"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'

interface HealthItem {
  name: string;
  value: number;
  weight: number;
  color: string;
  dataSource?: string
}

interface SegmentInfo {
  color: string;
  dashArray: string;
  dashOffset: number;
  rotate: number
}

defineProps<{
  healthStats: { overallScore: number; items: HealthItem[] }
  ringSegments: SegmentInfo[]
}>()

const triggerRef = ref<HTMLElement | null>(null)
const activeSegment = ref<number | null>(null)

// Tooltip (kept simple since it was already Teleport-based in parent)
const showPopover = () => {
}
const hidePopover = () => {
}
</script>
