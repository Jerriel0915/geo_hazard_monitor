<template>
  <div class="echarts-wrapper" :style="{ height: height }">
    <button
      v-if="showFullscreen && !isFullscreen"
      class="echarts-wrapper__fullscreen-btn"
      title="全屏展示"
      @click="enterFullscreen"
    >
      <el-icon :size="16"><FullScreen /></el-icon>
    </button>
    <div ref="chartRef" class="echarts-wrapper__chart" :style="{ height: '100%' }" />
  </div>

  <Teleport to="body" v-if="isFullscreen">
    <div class="echarts-fs-overlay" @click.self="exitFullscreen">
      <div class="echarts-fs-header">
        <span class="echarts-fs-title">监测数据图表</span>
        <button class="echarts-fs-close-btn" title="退出全屏 (ESC)" @click="exitFullscreen">
          <el-icon :size="20"><Close /></el-icon>
        </button>
      </div>
      <div ref="fsChartRef" class="echarts-fs-body" />
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { FullScreen, Close } from '@element-plus/icons-vue'
import type { EChartsOption } from 'echarts'
import echarts from '@/utils/echarts'

const props = withDefaults(defineProps<{
  option: EChartsOption
  height?: string
  theme?: string | object
  loading?: boolean
  showFullscreen?: boolean
}>(), {
  height: '400px',
  showFullscreen: false,
})

const emit = defineEmits<{
  (e: 'chart-ready', instance: ReturnType<typeof echarts.init>): void
}>()

const chartRef = ref<HTMLElement>()
const fsChartRef = ref<HTMLElement>()
const isFullscreen = ref(false)
let instance: ReturnType<typeof echarts.init> | null = null
let fsInstance: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null
let fsResizeObserver: ResizeObserver | null = null

function enterFullscreen() {
  isFullscreen.value = true
  document.addEventListener('keydown', onEscKey)
  document.body.style.overflow = 'hidden'

  nextTick(() => {
    if (!fsChartRef.value) return
    fsInstance = echarts.init(fsChartRef.value, props.theme)
    if (instance) {
      fsInstance.setOption(instance.getOption(), { notMerge: true })
    }
    fsResizeObserver = new ResizeObserver(() => fsInstance?.resize())
    fsResizeObserver.observe(fsChartRef.value)
  })
}

function exitFullscreen() {
  isFullscreen.value = false
  document.removeEventListener('keydown', onEscKey)
  document.body.style.overflow = ''

  fsResizeObserver?.disconnect()
  fsResizeObserver = null
  fsInstance?.dispose()
  fsInstance = null
}

function onEscKey(e: KeyboardEvent) {
  if (e.key === 'Escape') exitFullscreen()
}

function safeInit() {
  if (!chartRef.value) return
  const rect = chartRef.value.getBoundingClientRect()
  if (rect.width < 10 || rect.height < 10) return

  dispose()
  instance = echarts.init(chartRef.value, props.theme)
  instance.setOption(props.option, { notMerge: true })
  emit('chart-ready', instance)

  resizeObserver = new ResizeObserver(() => instance?.resize())
  resizeObserver.observe(chartRef.value)
}

function dispose() {
  resizeObserver?.disconnect()
  resizeObserver = null
  instance?.dispose()
  instance = null
}

onMounted(() => { safeInit() })

watch(() => props.option, (opt) => {
  if (instance && opt && Object.keys(opt).length > 0) {
    instance.setOption(opt, { notMerge: true })
  }
}, { deep: true })

watch(() => props.loading, (val) => {
  if (val) {
    instance?.showLoading()
  } else {
    instance?.hideLoading()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onEscKey)
  document.body.style.overflow = ''
  fsResizeObserver?.disconnect()
  fsInstance?.dispose()
  dispose()
})
</script>

<style scoped>
.echarts-wrapper {
  position: relative;
  width: 100%;
  min-height: 200px;
}

.echarts-wrapper__chart {
  width: 100%;
}

.echarts-wrapper__fullscreen-btn {
  position: absolute;
  top: 38px;
  right: 8px;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  color: #606266;
  transition: all 0.2s;
}

.echarts-wrapper__fullscreen-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}
</style>

<style>
/* 全屏覆盖层 (unscoped：Teleport 到 body 后不受 scoped 约束) */
.echarts-fs-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.echarts-fs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.echarts-fs-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.echarts-fs-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  color: #606266;
  transition: all 0.2s;
}

.echarts-fs-close-btn:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

.echarts-fs-body {
  flex: 1 1 0;
  min-height: 0;
  margin: 16px 24px 24px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
</style>
