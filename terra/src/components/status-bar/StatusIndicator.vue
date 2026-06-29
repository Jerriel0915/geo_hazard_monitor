<!-- ======================================== -->
<!-- StatusIndicator - 状态指示器组件 -->
<!-- ======================================== -->

<template>
  <div class="status-indicator" :class="[`level-${level}`, { pulse }]">
    <span class="indicator-dot"></span>
    <span v-if="showLabel" class="indicator-label">{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
/**
 * Props
 */
interface Props {
  level?: 'normal' | 'info' | 'caution' | 'warning' | 'critical'
  label?: string
  showLabel?: boolean
  pulse?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  level: 'normal',
  pulse: true
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  .indicator-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }

  .indicator-label {
    font-size: 12px;
    font-weight: 500;
  }

  &.level-normal {
    .indicator-dot {
      background-color: $terra-normal;
    }

    .indicator-label {
      color: $terra-normal;
    }

    &.pulse .indicator-dot {
      animation: indicatorPulse 2s ease-in-out infinite;
      box-shadow: 0 0 10px $terra-normal;
    }
  }

  &.level-info {
    .indicator-dot {
      background-color: $terra-info;
    }

    .indicator-label {
      color: $terra-info;
    }

    &.pulse .indicator-dot {
      animation: indicatorPulseFast 1s ease-in-out infinite;
      box-shadow: 0 0 10px $terra-info;
    }
  }

  &.level-caution {
    .indicator-dot {
      background-color: $terra-caution;
    }

    .indicator-label {
      color: $terra-caution;
    }

    &.pulse .indicator-dot {
      animation: indicatorPulseFast 1.2s ease-in-out infinite;
      box-shadow: 0 0 10px $terra-caution;
    }
  }

  &.level-warning {
    .indicator-dot {
      background-color: $terra-warning;
    }

    .indicator-label {
      color: $terra-warning;
    }

    &.pulse .indicator-dot {
      animation: indicatorPulseCritical 0.8s ease-in-out infinite;
      box-shadow: 0 0 12px $terra-warning;
    }
  }

  &.level-critical {
    .indicator-dot {
      background-color: $terra-critical;
    }

    .indicator-label {
      color: $terra-critical;
    }

    &.pulse .indicator-dot {
      animation: indicatorPulseCritical 0.5s ease-in-out infinite;
      box-shadow: 0 0 14px $terra-critical;
    }
  }
}

@keyframes indicatorPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(0.88);
  }
}

@keyframes indicatorPulseFast {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
 50% {
    opacity: 0.5;
    transform: scale(0.82);
  }
}

@keyframes indicatorPulseCritical {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.15);
  }
}
</style>
