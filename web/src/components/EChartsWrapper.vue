<template>
  <div class="echarts-wrapper" :class="{ 'echarts-wrapper--fullscreen': isFullscreen }" :style="{ height: isFullscreen ? '100vh' : height }">
    <button v-if="showFullscreen" class="echarts-wrapper__fullscreen-btn" :title="isFullscreen ? '退出全屏 (ESC)' : '全屏展示'" @click="toggleFullscreen">
      <el-icon :size="16">
        <FullScreen v-if="!isFullscreen" />
        <Close v-else />
      </el-icon>
    </button>
    <div ref="chartRef" class="echarts-wrapper__chart" :style="{ height: isFullscreen ? 'calc(100vh - 40px)' : '100%' }" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
const isFullscreen = ref(false)
let instance: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  if (isFullscreen.value) {
    document.addEventListener('keydown', onEscKey)
  } else {
    document.removeEventListener('keydown', onEscKey)
  }
  // ECharts 在动画完成后 resize
  requestAnimationFrame(() => {
    requestAnimationFrame(() => instance?.resize())
  })
}

function onEscKey(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    isFullscreen.value = false
    document.removeEventListener('keydown', onEscKey)
    requestAnimationFrame(() => {
      requestAnimationFrame(() => instance?.resize())
    })
  }
}

function safeInit() {
  if (!chartRef.value) return
  const rect = chartRef.value.getBoundingClientRect()
  if (rect.width < 10 || rect.height < 10) return

  dispose()
  instance = echarts.init(chartRef.value, props.theme)
  instance.setOption(props.option, { notMerge: true })
  emit('chart-ready', instance)

  resizeObserver = new ResizeObserver(() => {
    instance?.resize()
  })
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
  top: 6px;
  right: 36px;
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

.echarts-wrapper--fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: #fff;
  padding: 16px 24px 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
}

.echarts-wrapper--fullscreen .echarts-wrapper__fullscreen-btn {
  top: 16px;
  right: 24px;
}
</style>
