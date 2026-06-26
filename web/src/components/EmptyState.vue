<template>
  <div class="empty-state">
    <div class="empty-state-icon">
      <slot name="icon">
        <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="32" cy="28" r="14" stroke="currentColor" stroke-width="2" stroke-dasharray="4 3" opacity="0.4"/>
          <path d="M18 48c0-7.7 6.3-14 14-14s14 6.3 14 14" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
          <rect x="12" y="46" width="40" height="4" rx="2" stroke="currentColor" stroke-width="2" opacity="0.3"/>
        </svg>
      </slot>
    </div>
    <p class="empty-state-text">{{ description }}</p>
    <p v-if="actionLabel" class="empty-state-action">
      <el-button type="primary" link @click="$emit('action')">
        {{ actionLabel }}
      </el-button>
    </p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  description: string
  actionLabel?: string
}>()

defineEmits<{
  action: []
}>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
}
.empty-state-icon {
  width: 64px;
  height: 64px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 16px;
}
.empty-state-icon svg {
  width: 100%;
  height: 100%;
}
.empty-state-text {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 0 0 8px;
  max-width: 280px;
}
.empty-state-action {
  margin-top: 8px;
}
</style>
