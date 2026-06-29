<!-- ======================================== -->
<!-- Timeline - 思考与执行时间线容器 -->
<!-- ======================================== -->

<template>
  <div class="timeline">
    <div class="timeline-header">
      <h3 class="timeline-title">思考与执行</h3>
      <span class="timeline-count">{{ items.length }}</span>
    </div>
    <div class="timeline-container" ref="containerRef">
      <TransitionGroup name="timeline-item">
        <TimelineItem
          v-for="item in reversedItems"
          :key="item.id"
          :item="item"
        />
      </TransitionGroup>
      <div v-if="items.length === 0" class="timeline-empty">
        暂无动态
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useTerraStore } from '@/stores/terra'
import TimelineItem from './TimelineItem.vue'
import type { TimelineItem as TimelineItemType } from '@/types'

/**
 * Props
 */
interface Props {
  items?: TimelineItemType[]
}

const props = withDefaults(defineProps<Props>(), {
  items: () => []
})

/**
 * Store
 */
const terraStore = useTerraStore()

/**
 * 使用 store 中的时间线条目或 props 传入的
 */
const items = computed(() => props.items.length > 0 ? props.items : terraStore.timelineItems)

/**
 * 反转列表（最新在顶部）
 */
const reversedItems = computed(() => [...items.value].reverse())

/**
 * 容器引用
 */
const containerRef = ref<HTMLElement>()

/**
 * 新增条目时滚动到顶部
 */
watch(items, () => {
  nextTick(() => {
    if (containerRef.value) {
      containerRef.value.scrollTop = 0
    }
  })
}, { deep: true })
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.timeline {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .timeline-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 2px solid $border-subtle;
    background-color: rgba($bg-tertiary, 0.3);

    .timeline-title {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
      text-shadow: $text-shadow-sm;
    }

    .timeline-count {
      font-size: 12px;
      font-weight: 500;
      color: $text-dim;
      padding: 2px 8px;
      background-color: $bg-tertiary;
      border-radius: $radius-sm;
      border: 1px solid $border-subtle;
    }
  }

  .timeline-container {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 8px 0;

    .timeline-empty {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100px;
      font-size: 13px;
      color: $text-secondary;
      font-style: italic;
    }
  }
}

// 列表项过渡动画
.timeline-item-enter-active {
  animation: slideIn 0.3s $ease-out;
}

.timeline-item-leave-active {
  animation: slideOut 0.3s $ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(20px);
  }
}
</style>
