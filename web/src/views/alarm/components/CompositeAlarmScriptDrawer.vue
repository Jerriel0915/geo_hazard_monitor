<template>
  <el-drawer
    :model-value="visible"
    size="70%"
    :title="'综合告警脚本编辑器'"
    @close="handleClose"
  >
    <CompositeAlarmScriptEditor
      :alarm-id="alarmId"
      :trigger-mode="triggerMode"
      @saved="onSaved"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import CompositeAlarmScriptEditor from './CompositeAlarmScriptEditor.vue'

defineProps<{
  visible: boolean
  alarmId: number
  triggerMode: 'PERIODIC' | 'REALTIME'
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  saved: []
}>()

function handleClose() {
  emit('update:visible', false)
}

function onSaved() {
  emit('saved')
}
</script>
