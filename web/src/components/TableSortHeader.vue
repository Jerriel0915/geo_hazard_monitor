<template>
  <span class="sort-header" @click="$emit('toggle')">
    <span class="sort-header__label">{{ label }}</span>
    <span class="sort-header__arrows">
      <span :class="['sort-header__arrow', 'sort-header__arrow--up', { 'is-active': order === 'asc' }]">▲</span>
      <span :class="['sort-header__arrow', 'sort-header__arrow--down', { 'is-active': order === 'desc' }]">▼</span>
    </span>
  </span>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  order: 'asc' | 'desc' | ''
}>()

defineEmits<{
  toggle: []
}>()
</script>

<style scoped>
.sort-header {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  user-select: none;
}

.sort-header__label {
  white-space: nowrap;
}

.sort-header__arrows {
  display: inline-flex;
  flex-direction: column;
  line-height: 1;
  font-size: 9px;
  color: #c0c4cc;
  gap: 0;
  margin-top: -1px;
}

.sort-header__arrow.is-active {
  color: #409eff;
}
</style>
