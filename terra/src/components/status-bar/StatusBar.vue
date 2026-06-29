<!-- ======================================== -->
<!-- StatusBar - 底部状态栏 -->
<!-- ======================================== -->

<template>
  <footer class="status-bar">
    <!-- 左侧：状态指示器 -->
    <div class="status-left">
      <div class="status-indicator" :class="terraState"></div>
      <div class="status-info">
        <div class="status-label">{{ statusLabel }}</div>
        <div class="status-detail">{{ statusDetail }}</div>
      </div>
    </div>

    <!-- 中间：关注点列表 -->
    <div class="status-center">
      <div class="watching-list">
        <span class="watching-label">我在盯着:</span>
        <span
          v-for="point in watchingPoints"
          :key="point"
          class="watching-item"
        >
          {{ point }}
        </span>
        <span v-if="watchingPoints.length === 0" class="watching-empty">
          暂无特别关注
        </span>
      </div>
    </div>

    <!-- 右侧：TerraMens 消息 -->
    <div class="status-right">
      <div class="terra-message">
        <span class="message-label">TerraMens 说:</span>
        <span class="message-text">{{ currentMessage || defaultMessage }}</span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTerraStore } from '@/stores/terra'
import type { TerraState } from '@/types'

/**
 * Store
 */
const terraStore = useTerraStore()

/**
 * TerraMens 状态
 */
const terraState = computed<TerraState>(() => terraStore.state)
const statusLabel = computed(() => terraStore.stateLabel)
const watchingPoints = computed(() => terraStore.watching)
const currentMessage = computed(() => terraStore.currentMessage)

/**
 * 状态详情
 */
const statusDetail = computed(() => {
  const count = 47
  const state = terraStore.state
  const watchingCount = terraStore.watching.length

  if (state === 'normal') {
    return `${count} 个点位 · 全区平稳`
  }
  if (state === 'info') {
    return `${count} 个点位 · ${watchingCount} 个关注`
  }
  if (state === 'caution') {
    return `${count} 个点位 · 需要注意`
  }
  if (state === 'warning') {
    return `${count} 个点位 · 1 个预警`
  }
  if (state === 'critical') {
    return `${count} 个点位 · 紧急情况`
  }
  return `${count} 个点位`
})

/**
 * 默认消息
 */
const defaultMessage = '一切正常，我在守护着大家。'
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.status-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: $statusbar-height;
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: linear-gradient(0deg, rgba($bg-primary, 0.98) 0%, rgba($bg-primary, 0.85) 60%, rgba($bg-primary, 0) 100%);
  z-index: $z-statusbar;
  border-top: 2px solid $border-subtle;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.3);

  // 左侧：状态指示器
  .status-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 200px;
    padding-right: 20px;
    border-right: 1px solid $border-subtle;

    .status-indicator {
      width: 14px;
      height: 14px;
      border-radius: 50%;
      flex-shrink: 0;
      border: 2px solid rgba(255, 255, 255, 0.3);
      animation: statusPulse 2s ease-in-out infinite;

      &.normal {
        background-color: $terra-normal;
        box-shadow: 0 0 14px $terra-normal, 0 0 20px rgba($terra-normal, 0.3);
      }

      &.info {
        background-color: $terra-info;
        box-shadow: 0 0 14px $terra-info, 0 0 20px rgba($terra-info, 0.3);
      }

      &.caution {
        background-color: $terra-caution;
        box-shadow: 0 0 14px $terra-caution, 0 0 20px rgba($terra-caution, 0.3);
      }

      &.warning {
        background-color: $terra-warning;
        box-shadow: 0 0 16px $terra-warning, 0 0 25px rgba($terra-warning, 0.4);
        animation: statusPulseCritical 0.8s ease-in-out infinite;
      }

      &.critical {
        background-color: $terra-critical;
        box-shadow: 0 0 18px $terra-critical, 0 0 30px rgba($terra-critical, 0.5);
        animation: statusPulseCritical 0.5s ease-in-out infinite;
      }
    }

    .status-info {
      .status-label {
        font-size: 14px;
        font-weight: 600;
        color: $text-primary;
        text-shadow: $text-shadow-sm;
      }

      .status-detail {
        font-size: 12px;
        color: $text-secondary;
        margin-top: 2px;
      }
    }
  }

  // 中间：关注点列表
  .status-center {
    flex: 1;
    display: flex;
    justify-content: center;
    padding: 0 24px;

    .watching-list {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;

      .watching-label {
        font-size: 11px;
        font-weight: 600;
        color: $text-dim;
        text-transform: uppercase;
        letter-spacing: 1px;
      }

      .watching-item {
        font-size: 13px;
        font-weight: 500;
        color: $color-highlight;
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 5px 12px;
        background-color: rgba($color-highlight, 0.15);
        border: 1px solid $color-highlight;
        border-radius: $radius-sm;

        &::before {
          content: '';
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background-color: $color-highlight;
          box-shadow: 0 0 8px $color-highlight;
        }
      }

      .watching-empty {
        font-size: 12px;
        color: $text-secondary;
        font-style: italic;
      }
    }
  }

  // 右侧：TerraMens 消息
  .status-right {
    max-width: 400px;

    .terra-message {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .message-label {
        font-size: 10px;
        font-weight: 600;
        color: $color-secondary;
        text-transform: uppercase;
        letter-spacing: 1px;
      }

      .message-text {
        font-size: 13px;
        font-weight: 400;
        color: $text-secondary;
        line-height: 1.5;
      }
    }
  }
}

// 状态指示器动画
@keyframes statusPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(0.92);
  }
}

@keyframes statusPulseFast {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(0.85);
  }
}

@keyframes statusPulseCritical {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
    box-shadow: 0 0 14px currentColor, 0 0 24px currentColor;
  }
  50% {
    opacity: 0.8;
    transform: scale(1.12);
    box-shadow: 0 0 20px currentColor, 0 0 35px currentColor, 0 0 50px rgba(239, 68, 68, 0.5);
  }
}
</style>
