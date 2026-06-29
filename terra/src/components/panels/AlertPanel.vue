<!-- ======================================== -->
<!-- AlertPanel - 警报弹窗 -->
<!-- ======================================== -->

<template>
  <Teleport to="body">
    <Transition name="alert-fade">
      <div v-if="visible" class="alert-overlay" @click="handleOverlayClick">
        <div class="alert-card" :class="`level-${alertLevel}`" @click.stop>
          <!-- 警报头部 -->
          <div class="alert-header">
            <div class="alert-badge">{{ alertLevelText }}</div>
            <div class="alert-time">{{ formatTime(alert.timestamp) }}</div>
          </div>

          <!-- 警报标题 -->
          <div class="alert-title">{{ alert.title }}</div>

          <!-- 警报描述 -->
          <div class="alert-description">{{ alert.description }}</div>

          <!-- 建议 -->
          <div v-if="alert.suggestion" class="alert-suggestion">
            <span class="suggestion-label">TerraMens 建议：</span>
            <span class="suggestion-text">{{ alert.suggestion }}</span>
          </div>

          <!-- 操作按钮 -->
          <div v-if="alert.actions && alert.actions.length > 0" class="alert-actions">
            <button
              v-for="action in alert.actions"
              :key="action.id"
              class="alert-action"
              :class="{ primary: action.primary }"
              @click="handleAction(action.id)"
            >
              {{ action.label }}
            </button>
          </div>

          <!-- 关闭按钮 -->
          <button class="alert-close" @click="handleDismiss">
            我知道了
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useAlertStore } from '@/stores/alert'
import { useTerraStore } from '@/stores/terra'

/**
 * Store
 */
const alertStore = useAlertStore()
const terraStore = useTerraStore()

/**
 * 当前警报
 */
const currentAlert = computed(() => alertStore.currentAlert)

/**
 * 警报是否可见
 */
const visible = computed(() => alertStore.alertVisible)

/**
 * 警报级别
 */
const alertLevel = computed(() => currentAlert.value?.level || 'attention')

/**
 * 警报级别文本
 */
const alertLevelText = computed(() => {
  const texts = {
    attention: '关注',
    warning: '预警',
    critical: '紧急'
  }
  return texts[alertLevel.value] || '通知'
})

/**
 * 格式化时间
 */
function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

/**
 * 处理遮罩点击
 */
function handleOverlayClick() {
  // 点击遮罩不关闭，需要用户主动操作
}

/**
 * 处理操作
 */
function handleAction(actionId: string) {
  alertStore.handleAlertAction(actionId)
}

/**
 * 处理关闭
 */
function handleDismiss() {
  alertStore.hideAlert()
}

/**
 * 监听警报变化，自动显示
 */
watch(currentAlert, (newAlert) => {
  if (newAlert && !visible.value) {
    alertStore.showAlert(newAlert)
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.alert-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 1, 5, 0.85);
  backdrop-filter: $backdrop-blur-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal;
  padding: 20px;
}

.alert-card {
  width: 100%;
  max-width: 420px;
  background: $bg-secondary;
  backdrop-filter: $backdrop-blur;
  border-radius: $radius-sm;
  border: 1px solid;
  padding: 20px;
  box-shadow: $shadow-lg;

  // 级别样式（线框风格）
  &.level-attention {
    border-color: $border-warning;
    box-shadow: $glow-warning, $shadow-lg;
  }

  &.level-warning {
    border-color: $terra-warning;
    box-shadow: $glow-warning, $shadow-lg;
  }

  &.level-critical {
    border-color: $terra-critical;
    box-shadow: $glow-critical, $shadow-lg;
    animation: alertPulse 1.5s ease-in-out infinite;
  }

  .alert-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .alert-badge {
      padding: 4px 12px;
      border: 1px solid;
      border-radius: $radius-sm;
      font-family: $font-family-ui;
      font-size: 10px;
      font-weight: $font-weight-semibold;
      text-transform: uppercase;
      letter-spacing: 1px;
      background: transparent;
    }

    .alert-time {
      font-family: $font-family-ui;
      font-size: 10px;
      color: $text-dim;
      letter-spacing: 1px;
    }
  }

  &.level-attention .alert-badge {
    border-color: $color-highlight;
    color: $color-highlight;
    text-shadow: 0 0 8px rgba($color-highlight, 0.4);
  }

  &.level-warning .alert-badge {
    border-color: $terra-warning;
    color: $terra-warning;
    text-shadow: 0 0 8px rgba($terra-warning, 0.4);
  }

  &.level-critical .alert-badge {
    border-color: $terra-critical;
    color: $terra-critical;
    text-shadow: 0 0 8px rgba($terra-critical, 0.4);
  }

  .alert-title {
    font-family: $font-family-ui;
    font-size: 16px;
    font-weight: $font-weight-semibold;
    color: $color-primary;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 12px;
    text-shadow: $text-shadow-sm;
  }

  .alert-description {
    font-family: $font-family-base;
    font-size: $font-size-body;
    color: $text-secondary;
    line-height: 1.6;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: rgba(0, 12, 28, 0.6);
    border-radius: $radius-sm;
    border-left: 2px solid;
  }

  &.level-attention .alert-description {
    border-left-color: $color-highlight;
  }

  &.level-warning .alert-description {
    border-left-color: $terra-warning;
  }

  &.level-critical .alert-description {
    border-left-color: $terra-critical;
  }

  .alert-suggestion {
    font-family: $font-family-base;
    font-size: $font-size-small;
    color: $text-secondary;
    margin-bottom: 20px;
    padding: 12px;
    background: rgba(0, 212, 255, 0.05);
    border: 1px solid $border-subtle;
    border-radius: $radius-sm;

    .suggestion-label {
      color: $color-primary;
      font-weight: $font-weight-medium;
      text-shadow: $text-shadow-sm;
    }

    .suggestion-text {
      margin-left: 8px;
    }
  }

  .alert-actions {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;

    .alert-action {
      padding: 8px 16px;
      border: 1px solid $border-default;
      border-radius: $radius-sm;
      font-family: $font-family-ui;
      font-size: $font-size-small;
      font-weight: $font-weight-medium;
      cursor: pointer;
      transition: all $transition-fast $ease-out;
      background: transparent;
      color: $color-primary;
      text-transform: uppercase;
      letter-spacing: 1px;

      &:hover {
        background: rgba($color-primary, 0.15);
        border-color: $border-accent;
        box-shadow: $glow-primary;
      }

      &.primary {
        background: rgba($color-primary, 0.15);
        border-color: $border-accent;
        box-shadow: $glow-primary;

        &:hover {
          background: rgba($color-primary, 0.25);
        }
      }
    }
  }

  .alert-close {
    width: 100%;
    padding: 10px;
    border: 1px solid $border-subtle;
    border-radius: $radius-sm;
    font-family: $font-family-ui;
    font-size: $font-size-small;
    font-weight: $font-weight-medium;
    color: $text-secondary;
    background: transparent;
    cursor: pointer;
    transition: all $transition-fast $ease-out;
    text-transform: uppercase;
    letter-spacing: 1px;

    &:hover {
      border-color: $border-default;
      background: rgba($color-primary, 0.1);
      color: $color-primary;
    }
  }
}

// 动画
@keyframes alertPulse {
  0%, 100% {
    box-shadow: $glow-critical, $shadow-lg;
  }
  50% {
    box-shadow: 0 0 30px rgba($terra-critical, 0.5), $shadow-lg;
  }
}

.alert-fade-enter-active {
  animation: alertFadeIn 0.3s $ease-out;
}

.alert-fade-leave-active {
  animation: alertFadeOut 0.2s $ease-in;
}

@keyframes alertFadeIn {
  from {
    opacity: 0;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes alertFadeOut {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.98);
  }
}
</style>
