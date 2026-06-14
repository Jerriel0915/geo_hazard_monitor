<!-- 待办告警下处置功能的反馈按钮 -->
<template>
  <el-dialog
      v-model="dialogVisible"
      title="反馈"
      width="500px"
      :close-on-click-modal="false"
      :append-to-body="true"
      class="feedback-dialog"
  >
    <div class="feedback-form">
      <div class="form-label">反馈内容</div>

      <el-input
          v-model="formData.content"
          type="textarea"
          :rows="4"
          placeholder="请输入反馈内容（非必填）"
          class="feedback-textarea"
      />

      <div class="form-label">反馈文件</div>

      <div class="file-upload-area">
        <div v-if="formData.files.length > 0" class="file-list">
          <div v-for="(file, index) in formData.files" :key="index" class="file-item">
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ formatFileSize(file.size) }}</span>
            <el-icon class="file-remove" @click="removeFile(index)"><Close /></el-icon>
          </div>
        </div>

        <div
            v-if="formData.files.length < 3"
            class="upload-trigger"
            @click="triggerFileUpload"
        >
          <el-icon class="plus-icon"><Plus /></el-icon>
          <span class="upload-text">添加文件</span>
        </div>

        <input
            ref="fileInputRef"
            type="file"
            multiple
            accept="image/jpeg,image/png,image/gif,image/jpg,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            style="display: none"
            @change="handleFileChange"
        />
      </div>

      <div class="upload-hint">
        支持图片、PDF、Word、Excel格式，最多上传3个文件
      </div>
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
import { Close, Plus } from '@element-plus/icons-vue'

interface Props {
  visible: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'submit', data: { content: string; files: File[] }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = ref(false)
const formData = ref({
  content: '',
  files: [] as File[]
})
const fileInputRef = ref<HTMLInputElement | null>(null)

// 监听 visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
})

watch(() => dialogVisible.value, (val) => {
  emit('update:visible', val)
})

// 重置表单
const resetForm = () => {
  formData.value = { content: '', files: [] }
}

// 触发文件选择
const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])

  if (formData.value.files.length + files.length > 3) {
    ElMessage.warning('最多只能上传3个文件')
    return
  }

  const allowedTypes = [
    'image/jpeg', 'image/png', 'image/gif', 'image/jpg',
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ]

  const invalidFiles = files.filter(file => !allowedTypes.includes(file.type))
  if (invalidFiles.length > 0) {
    ElMessage.warning('不支持的文件类型，请上传图片、PDF、Word或Excel文件')
    return
  }

  formData.value.files.push(...files)
  if (fileInputRef.value) fileInputRef.value.value = ''
}

// 删除文件
const removeFile = (index: number) => {
  formData.value.files.splice(index, 1)
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 取消
const handleCancel = () => {
  resetForm()
  dialogVisible.value = false
}

// 确认
const handleConfirm = () => {
  emit('submit', {
    content: formData.value.content,
    files: formData.value.files
  })
  resetForm()
  dialogVisible.value = false
}
</script>

<style scoped>
.feedback-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.feedback-form {
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

.feedback-textarea :deep(.el-textarea__inner) {
  font-size: 13px;
  border-radius: 6px;
  resize: vertical;
}

.file-upload-area {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  padding: 20px;
  background: #fafafa;
  transition: all 0.2s;
}

.file-upload-area:hover {
  border-color: #409eff;
  background: #f5f9ff;
}

.file-list {
  margin-bottom: 16px;
  max-height: 200px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 8px;
  border: 1px solid #e9ecef;
}

.file-item:last-child {
  margin-bottom: 0;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 11px;
  color: #909399;
  flex-shrink: 0;
}

.file-remove {
  font-size: 14px;
  color: #909399;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s;
}

.file-remove:hover {
  color: #f56c6c;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-trigger:hover {
  border-color: #409eff;
  background: #f5f9ff;
}

.plus-icon {
  font-size: 28px;
  color: #409eff;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 13px;
  color: #606266;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: -4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>