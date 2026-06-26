<template>
  <div ref="chartRef" class="echarts-wrapper" :style="{ height: height }" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import echarts from '@/utils/echarts'

const props = withDefaults(defineProps<{
  option: EChartsOption
  height?: string
  theme?: string | object
  loading?: boolean
}>(), {
  height: '400px',
})

const emit = defineEmits<{
  (e: 'chart-ready', instance: ReturnType<typeof echarts.init>): void
}>()

const chartRef = ref<HTMLElement>()
let instance: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null

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
  if (instance && opt) {
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

onBeforeUnmount(() => { dispose() })
</script>

<style scoped>
.echarts-wrapper {
  width: 100%;
  min-height: 200px;
}
</style>
