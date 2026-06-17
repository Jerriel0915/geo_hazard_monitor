<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑算法' : '新增算法'"
    width="560px"
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="算法编码" prop="code">
        <el-input
          v-model="formData.code"
          :disabled="isEdit"
          placeholder="如 ALGO_RAIN_01（大写字母+数字+下划线）"
          maxlength="64"
        />
      </el-form-item>
      <el-form-item label="算法名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入算法名称" maxlength="128" />
      </el-form-item>
      <el-form-item label="算法描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入算法描述"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="可选"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createAlgoLibrary,
  updateAlgoLibrary,
  type AlgoInfo,
  type AlgoInfoPayload
} from '@/api/algoLibrary'

const props = defineProps<{
  modelValue: boolean
  algo?: AlgoInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<AlgoInfoPayload>({
  code: '',
  name: '',
  description: '',
  remark: ''
})

const rules: FormRules = {
  code: [
    { required: true, message: '请输入算法编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_]{2,63}$/,
      message: '必须以大写字母开头，3-64 字符，仅含大写字母/数字/下划线',
      trigger: 'blur'
    }
  ],
  name: [{ required: true, message: '请输入算法名称', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      isEdit.value = !!props.algo
      if (props.algo) {
        formData.code = props.algo.code
        formData.name = props.algo.name
        formData.description = props.algo.description || ''
        formData.remark = props.algo.remark || ''
      } else {
        formData.code = ''
        formData.name = ''
        formData.description = ''
        formData.remark = ''
      }
    }
  }
)

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: AlgoInfoPayload = {
      name: formData.name,
      description: formData.description,
      remark: formData.remark
    }
    if (isEdit.value && props.algo) {
      await updateAlgoLibrary(props.algo.id, payload)
      ElMessage.success('更新成功')
    } else {
      payload.code = formData.code
      await createAlgoLibrary(payload)
      ElMessage.success('创建成功')
    }
    emit('update:modelValue', false)
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>
