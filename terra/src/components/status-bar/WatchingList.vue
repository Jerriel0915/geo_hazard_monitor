<!-- ======================================== -->
<!-- WatchingList - 关注点列表组件 -->
<!-- ======================================== -->

<template>
  <div class="watching-list">
    <span v-if="watchingPoints.length > 0" class="watching-label">我在盯着:</span>
    <span v-else class="watching-empty">暂无特别关注</span>

    <TransitionGroup name="watching-item">
      <span
        v-for="point in watchingPoints"
        :key="point"
        class="watching-item"
      >
        <span class="item-dot"></span>
        <span class="item-text">{{ point }}</span>
      </span>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTerraStore } from '@/stores/terra'

/**
 * Store
 */
const terraStore = useTerraStore()

/**
 * 关注点列表
 */
const watchingPoints = computed(() => terraStore.watching)
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.watching-list {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;

  .watching-label {
    font-size: 10px;
    color: $text-dim;
    text-transform: uppercase;
    letter-spacing: 1px;
  }

  .watching-empty {
    font-size: 12px;
    color: $text-dim;
    font-style: italic;
  }

  .watching-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 10px;
    background-color: rgba($color-highlight, 0.1);
    border-radius: $radius-sm;
    font-size: 12px;
    color: $color-highlight;

    .item-dot {
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background-color: $color-highlight;
    }
  }
}

.watching-item-enter-active {
  animation: watchingItemSlide 0.3s $ease-out;
}

.watching-item-leave-active {
  animation: watchingItemSlide 0.3s $ease-in;
}

@keyframes watchingItemSlide {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
