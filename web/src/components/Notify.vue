<!-- 待办告警下处置功能的通知按钮-->
<template>
  <el-dialog
      v-model="dialogVisible"
      title="发送通知"
      width="500px"
      :close-on-click-modal="false"
      :append-to-body="true"
      class="notify-dialog"
  >
    <div class="notify-form">
      <!-- 消息内容 -->
      <div class="form-label">消息内容</div>
      <el-input
          v-model="formData.content"
          type="textarea"
          :rows="4"
          placeholder="请输入消息内容"
          class="notify-textarea"
      />

      <!-- 通知渠道 -->
      <div class="form-label">通知渠道</div>
      <div class="channel-group">
        <el-checkbox v-model="formData.channels.sms">短信</el-checkbox>
        <el-checkbox v-model="formData.channels.email">邮件</el-checkbox>
        <el-checkbox v-model="formData.channels.system">系统消息</el-checkbox>
      </div>

      <!-- 通知人员 -->
      <div class="form-label">通知人员</div>
      <el-select
          v-model="formData.personnel"
          placeholder="请选择通知人员"
          class="personnel-select"
      >
        <el-option label="管理员" value="admin" />
        <el-option label="普通人员" value="normal" />
      </el-select>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

interface Props {
  visible: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'submit', data: { content: string; channels: { sms: boolean; email: boolean; system: boolean }; personnel: string }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const formData = ref({
  content: '',
  channels: {
    sms: false,
    email: false,
    system: false
  },
  personnel: ''
})

// 监听 visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
})

watch(() => dialogVisible.value, (val) => {
  emit('update:visible', val)
})

// 重置表单
const resetForm = () => {
  formData.value = {
    content: '',
    channels: {
      sms: false,
      email: false,
      system: false
    },
    personnel: ''
  }
}

// 取消
const handleCancel = () => {
  resetForm()
  dialogVisible.value = false
}

// 确认
const handleConfirm = () => {
  // 验证至少选择一个渠道
  if (!formData.value.channels.sms && !formData.value.channels.email && !formData.value.channels.system) {
    ElMessage.warning('请至少选择一个通知渠道')
    return
  }

  // 验证通知人员已选择
  if (!formData.value.personnel) {
    ElMessage.warning('请选择通知人员')
    return
  }

  // 验证消息内容
  if (!formData.value.content.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  emit('submit', {
    content: formData.value.content,
    channels: formData.value.channels,
    personnel: formData.value.personnel
  })

  resetForm()
  dialogVisible.value = false
}
</script>

<style scoped>
.notify-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.notify-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: -8px;
}

.notify-textarea :deep(.el-textarea__inner) {
  font-size: 13px;
  border-radius: 6px;
  resize: vertical;
}

/* 消息内容输入框虚线样式 */
.notify-textarea :deep(.el-textarea__inner) {
  border: 1px dashed #dcdfe6;
}

.notify-textarea :deep(.el-textarea__inner):focus {
  border: 1px solid #409eff;
}

/* 渠道选项组 */
.channel-group {
  display: flex;
  gap: 24px;
  padding: 8px 0;
}

.channel-group :deep(.el-checkbox) {
  margin-right: 0;
}

.channel-group :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #606266;
}

/* 人员选择下拉框 */
.personnel-select {
  width: 100%;
}

.personnel-select :deep(.el-input__wrapper) {
  border-radius: 6px;
}

/* 底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>