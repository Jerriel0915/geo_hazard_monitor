<template>
  <el-pagination
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    :total="total"
    :page-sizes="pageSizes"
    layout="total, sizes, prev, pager, next, jumper"
    :disabled="total === 0"
    @size-change="$emit('sizeChange')"
    @current-change="$emit('pageChange')"
  />
</template>

<script setup lang="ts">
defineProps<{
  currentPage: number
  pageSize: number
  total: number
  pageSizes?: number[]
}>()

defineEmits<{
  sizeChange: []
  pageChange: []
}>()
</script>
