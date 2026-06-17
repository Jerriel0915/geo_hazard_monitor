<template>
  <el-drawer
    :model-value="modelValue"
    title="算法详情"
    size="720px"
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
    @open="loadDetail"
  >
    <div v-loading="loading" class="detail">
      <template v-if="algo">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="算法名称">{{ algo.name }}</el-descriptions-item>
          <el-descriptions-item label="算法编码">
            <code>{{ algo.code }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="algo.status === 1 ? 'success' : 'info'" size="small">
              {{ algo.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="版本数">{{ algo.versions?.length || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ algo.createTime }}</el-descriptions-item>
          <el-descriptions-item label="算法描述" :span="2">{{ algo.description || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ algo.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 版本列表 -->
        <div class="version-section">
          <div class="version-header">
            <h3>版本列表</h3>
            <el-button type="primary" size="small" @click="uploadVisible = true">
              <el-icon><Upload /></el-icon> 上传新版本
            </el-button>
          </div>

          <el-table :data="algo.versions || []" stripe>
            <el-table-column label="版本号" prop="versionNo" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.versionNo }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="原始文件" prop="originalName" show-overflow-tooltip />
            <el-table-column label="大小" width="100">
              <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="上传人" prop="createBy" width="100" />
            <el-table-column label="上传时间" prop="createTime" width="160" />
            <el-table-column label="备注" prop="remark" show-overflow-tooltip />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="handleDownload(row)">下载</el-button>
                <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </div>

    <!-- 上传新版本弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传新版本" width="480px" append-to-body>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="80px">
        <el-form-item label="版本号" prop="versionNo">
          <el-input v-model="uploadForm.versionNo" placeholder="如 v1.0.0" maxlength="64" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="uploadForm.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="算法包" prop="file">
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".zip"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将 zip 文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .zip 格式，最大 100MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="uploadProgress > 0" label="进度">
          <el-progress :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : ''" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUploadSubmit">上传</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  getAlgoLibraryDetail,
  getAlgoVersionList,
  uploadAlgoVersion,
  deleteAlgoVersion,
  downloadAlgoVersion,
  type AlgoInfo,
  type AlgoVersion
} from '@/api/algoLibrary'

const props = defineProps<{
  modelValue: boolean
  algoId: number | null
}>()

defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const loading = ref(false)
const algo = ref<AlgoInfo | null>(null)

const uploadVisible = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadFormRef = ref<FormInstance>()
const uploadForm = reactive<{ versionNo: string; remark: string; file: File | null }>({
  versionNo: '',
  remark: '',
  file: null
})
const uploadRules: FormRules = {
  versionNo: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  file: [{ required: true, message: '请选择算法包文件', trigger: 'change' }]
}

async function loadDetail() {
  if (!props.algoId) return
  loading.value = true
  try {
    const res: any = await getAlgoLibraryDetail(props.algoId)
    algo.value = res.data
    if (algo.value && !algo.value.versions) {
      const vRes: any = await getAlgoVersionList(props.algoId)
      algo.value.versions = vRes.data || []
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleFileChange(file: UploadFile) {
  if (file.raw) uploadForm.file = file.raw
}

function handleFileRemove() {
  uploadForm.file = null
}

async function handleUploadSubmit() {
  await uploadFormRef.value?.validate()
  if (!props.algoId || !uploadForm.file) return
  uploading.value = true
  uploadProgress.value = 0
  try {
    await uploadAlgoVersion(
      props.algoId,
      { file: uploadForm.file, versionNo: uploadForm.versionNo, remark: uploadForm.remark },
      (pct) => (uploadProgress.value = pct)
    )
    ElMessage.success('上传成功')
    uploadVisible.value = false
    uploadForm.versionNo = ''
    uploadForm.remark = ''
    uploadForm.file = null
    uploadProgress.value = 0
    await loadDetail()
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(row: AlgoVersion) {
  try {
    await ElMessageBox.confirm(`确定删除版本「${row.versionNo}」？`, '删除确认', { type: 'warning' })
    await deleteAlgoVersion(row.id)
    ElMessage.success('删除成功')
    await loadDetail()
  } catch { /* cancelled */ }
}

async function handleDownload(row: AlgoVersion) {
  try {
    const res = await downloadAlgoVersion(row.id)
    const blob = new Blob([res.data], { type: 'application/zip' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.originalName
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e.message || '下载失败')
  }
}

function formatSize(bytes: number): string {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
</script>

<style scoped>
.detail {
  padding: 16px;
}
.version-section {
  margin-top: 24px;
}
.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.version-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
</style>
