<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">通知设置</h2>
        <span class="header__subtitle">告警分发规则配置与通知渠道管理</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAdd">新增规则</el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="queryParams.name" placeholder="规则名称" clearable
                @keyup.enter="handleQuery" @clear="handleQuery" />
      <el-select v-model="queryParams.eventType" placeholder="事件类型" clearable @change="handleQuery">
        <el-option label="阈值告警" value="THRESHOLD" />
        <el-option label="综合告警" value="COMPREHENSIVE" />
        <el-option label="设备离线" value="OFFLINE" />
      </el-select>
      <el-select v-model="queryParams.isEnabled" placeholder="状态" clearable @change="handleQuery">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleQuery">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table :data="pagedList" v-loading="loading" border stripe>
          <el-table-column label="名称" prop="name" min-width="160" />
          <el-table-column label="事件类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.eventType === 'OFFLINE' ? 'warning' : 'danger'" size="small">
                {{ eventTypeLabel(row.eventType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="告警等级" width="200">
            <template #default="{ row }">
              <template v-if="row.eventType !== 'OFFLINE'">
                <el-tag v-for="lv in row.alarmLevels" :key="lv" size="small"
                        :style="getAlarmLevelStyle(lv)" style="margin-right: 4px; border: none;">
                  {{ levelLabel(lv) }}
                </el-tag>
                <span v-if="!row.alarmLevels || row.alarmLevels.length === 0" class="empty-text">-</span>
              </template>
              <span v-else class="empty-text">-</span>
            </template>
          </el-table-column>
          <el-table-column label="隐患点/设备" min-width="180">
            <template #default="{ row }">
              <template v-if="row.eventType !== 'OFFLINE'">
                <el-tag v-if="row.hazardPointAll" size="small" type="warning">全部隐患点</el-tag>
                <span v-else>{{ (row.hazardPointNames || []).join('、') || '-' }}</span>
              </template>
              <template v-else>
                <el-tag v-if="row.deviceAll" size="small" type="warning">全部设备</el-tag>
                <span v-else>{{ (row.deviceNames || []).join('、') || '-' }}</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="接收人" min-width="160">
            <template #default="{ row }">
              <el-tag v-if="row.recipientAll" size="small" type="warning">全部</el-tag>
              <span v-else>{{ row.recipientSummary || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="渠道" min-width="150">
            <template #default="{ row }">
              <el-tag v-for="ch in row.channels" :key="ch" size="small"
                      :type="channelTagType(ch)" style="margin-right: 4px;">
                {{ channelLabel(ch) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch :model-value="row.isEnabled === 1"
                         @change="(v: boolean) => handleToggleEnabled(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
        <el-pagination
            v-model:current-page="queryParams.pageNum"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="list.length"
            layout="total, sizes, prev, pager, next, jumper"
            prev-text="上一页"
            next-text="下一页"
            :disabled="list.length === 0"
        />
      </div>
    </div>

    <!-- 规则弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="720px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入规则名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="事件类型" prop="eventType">
          <el-radio-group v-model="form.eventType" @change="onEventTypeChange">
            <el-radio label="THRESHOLD">阈值告警</el-radio>
            <el-radio label="COMPREHENSIVE">综合告警</el-radio>
            <el-radio label="OFFLINE">设备离线</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="告警等级" prop="alarmLevels" v-if="form.eventType !== 'OFFLINE'">
          <el-checkbox-group v-model="form.alarmLevels">
            <el-checkbox label="1">一级（警报）</el-checkbox>
            <el-checkbox label="2">二级（警戒）</el-checkbox>
            <el-checkbox label="3">三级（警示）</el-checkbox>
            <el-checkbox label="4">四级（注意）</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="隐患点" prop="hazardPointIds" v-if="form.eventType !== 'OFFLINE'">
          <el-select v-model="form.hazardPointIds" multiple filterable
                     placeholder="请选择（支持全部）" style="width: 100%;">
            <el-option label="全部隐患点" value="*" />
            <el-option v-for="hp in hazardPointOptions" :key="hp.id"
                       :label="hp.name" :value="String(hp.id)" />
          </el-select>
        </el-form-item>

        <el-form-item label="设备" prop="deviceIds" v-if="form.eventType === 'OFFLINE'">
          <el-select v-model="form.deviceIds" multiple filterable
                     placeholder="请选择（支持全部）" style="width: 100%;">
            <el-option label="全部设备" value="*" />
            <el-option v-for="d in deviceOptions" :key="d.id"
                       :label="d.name + (d.code ? '（' + d.code + '）' : '')" :value="String(d.id)" />
          </el-select>
        </el-form-item>

        <el-form-item label="通知人员" prop="recipients">
          <RecipientPicker v-model="form.recipients" />
        </el-form-item>

        <el-form-item label="通知渠道" prop="channels">
          <el-checkbox-group v-model="form.channels">
            <el-checkbox label="SYSTEM">系统消息</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="EMAIL">邮件</el-checkbox>
          </el-checkbox-group>
          <div class="form-help" v-if="form.eventType !== 'OFFLINE'">系统消息必选（确保站内可达）</div>
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getDispatchRuleList,
  getDispatchRuleDetail,
  createDispatchRule,
  updateDispatchRule,
  deleteDispatchRule,
  toggleDispatchRuleEnabled,
  type AlarmDispatchRuleItemVO,
  type AlarmDispatchRuleCreateRequest,
  type AlarmDispatchRuleQuery,
  type NotifyChannel
} from '@/api/alarmDispatch'
import { getHazardPointPage } from '@/api/hazardPoint'
import { getDevicePage } from '@/api/device'
import { getAlarmLevelStyle } from '@/api/alarm'
import RecipientPicker from './components/RecipientPicker.vue'

const loading = ref(false)
const submitting = ref(false)
const list = ref<AlarmDispatchRuleItemVO[]>([])
const queryParams = reactive<AlarmDispatchRuleQuery>({
  name: '', eventType: undefined, isEnabled: undefined,
  pageNum: 1, pageSize: 10
})

// 客户端分页：后端返回全量，前端切片展示
const pagedList = computed(() => {
  const start = ((queryParams.pageNum || 1) - 1) * (queryParams.pageSize || 10)
  const end = start + (queryParams.pageSize || 10)
  return list.value.slice(start, end)
})

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

interface FormState {
  id?: number
  name: string
  eventType: 'THRESHOLD' | 'COMPREHENSIVE' | 'OFFLINE'
  alarmLevels: string[]
  channels: NotifyChannel[]
  hazardPointIds: string[]
  deviceIds: string[]
  recipients: { roleIds?: string[]; deptIds?: string[]; userIds?: string[] }
  isEnabled: number
  remark: string
}

const defaultForm = (): FormState => ({
  name: '',
  eventType: 'THRESHOLD',
  alarmLevels: [],
  channels: ['SYSTEM'],
  hazardPointIds: [],
  deviceIds: [],
  recipients: {},
  isEnabled: 1,
  remark: ''
})
const form = reactive<FormState>(defaultForm())

const dialogTitle = computed(() => form.id ? '编辑通知规则' : '新增通知规则')

const rules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  alarmLevels: [{
    validator: (_r, _v, cb) => {
      if (form.eventType !== 'OFFLINE' && form.alarmLevels.length === 0)
        cb(new Error('告警事件必须选择等级'))
      else cb()
    }, trigger: 'change'
  }],
  hazardPointIds: [{
    validator: (_r, _v, cb) => {
      if (form.eventType !== 'OFFLINE' && form.hazardPointIds.length === 0)
        cb(new Error('请选择隐患点'))
      else cb()
    }, trigger: 'change'
  }],
  deviceIds: [{
    validator: (_r, _v, cb) => {
      if (form.eventType === 'OFFLINE' && form.deviceIds.length === 0)
        cb(new Error('请选择设备'))
      else cb()
    }, trigger: 'change'
  }],
  channels: [{ required: true, type: 'array' as const, min: 1, message: '请至少选择一个渠道', trigger: 'change' }],
  recipients: [{
    validator: (_r, _v, cb) => {
      const r = form.recipients
      const cnt = (r.roleIds?.length || 0) + (r.deptIds?.length || 0) + (r.userIds?.length || 0)
      if (cnt === 0) cb(new Error('请选择通知人员'))
      else cb()
    }, trigger: 'change'
  }]
}

// 隐患点 / 设备选项
const hazardPointOptions = ref<Array<{ id: number; name: string }>>([])
const deviceOptions = ref<Array<{ id: number; name: string; code?: string }>>([])

async function loadOptions() {
  try {
    const [hpRes, devRes] = await Promise.all([
      getHazardPointPage({ pageNum: 1, pageSize: 1000 }),
      getDevicePage({ pageNum: 1, pageSize: 1000 })
    ])
    // getHazardPointPage 返回原始响应体 {code, data: {rows, total}}
    const hpRows = (hpRes as any)?.data?.rows || (hpRes as any)?.rows || []
    hazardPointOptions.value = hpRows.map((hp: any) => ({ id: Number(hp.id), name: hp.name }))
    // getDevicePage 使用 unwrap，返回 PageResult<DeviceItem>
    const devRows = (devRes as any)?.rows || (devRes as any)?.data?.rows || []
    deviceOptions.value = devRows.map((d: any) => ({ id: Number(d.id), name: d.name, code: d.code || d.deviceCode }))
  } catch {
    ElMessage.error('选项加载失败')
  }
}

async function getList() {
  loading.value = true
  try {
    const res: any = await getDispatchRuleList(queryParams)
    list.value = res.rows || []
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.name = ''
  queryParams.eventType = undefined
  queryParams.isEnabled = undefined
  handleQuery()
}

function handleAdd() {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: AlarmDispatchRuleItemVO) {
  try {
    const res: any = await getDispatchRuleDetail(row.id)
    const d = res.data
    Object.assign(form, {
      id: d.id,
      name: d.name,
      eventType: d.eventType,
      alarmLevels: d.alarmLevels || [],
      channels: d.channels || [],
      hazardPointIds: d.hazardPointIds || [],
      deviceIds: d.deviceIds || [],
      recipients: {
        roleIds: d.recipients.hasWildcardRole ? ['*'] : (d.recipients.roles || []).map((r: any) => r.id),
        deptIds: d.recipients.hasWildcardDept ? ['*'] : (d.recipients.depts || []).map((r: any) => r.id),
        userIds: d.recipients.hasWildcardUser
          ? ['*']
          : (d.recipients.users || []).map((r: any) => r.id)
      },
      isEnabled: d.isEnabled,
      remark: d.remark || ''
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload: AlarmDispatchRuleCreateRequest = {
      id: form.id,
      name: form.name,
      eventType: form.eventType,
      alarmLevels: form.eventType !== 'OFFLINE' ? form.alarmLevels : undefined,
      channels: form.channels,
      hazardPointIds: form.eventType !== 'OFFLINE' ? form.hazardPointIds : undefined,
      deviceIds: form.eventType === 'OFFLINE' ? form.deviceIds : undefined,
      recipients: form.recipients,
      isEnabled: form.isEnabled,
      remark: form.remark
    }
    if (form.id) {
      await updateDispatchRule(form.id, payload)
      ElMessage.success('编辑成功')
    } else {
      await createDispatchRule(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: AlarmDispatchRuleItemVO) {
  await ElMessageBox.confirm(`确认删除规则「${row.name}」？`, '提示', { type: 'warning' })
  await deleteDispatchRule(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function handleToggleEnabled(row: AlarmDispatchRuleItemVO, v: boolean) {
  const isEnabled = v ? 1 : 0
  await toggleDispatchRuleEnabled(row.id, isEnabled)
  row.isEnabled = isEnabled
  ElMessage.success(v ? '已启用' : '已禁用')
}

function onEventTypeChange(v: string) {
  if (v === 'OFFLINE') {
    form.hazardPointIds = []
    form.alarmLevels = []
  } else {
    form.deviceIds = []
  }
}

function resetForm() {
  Object.assign(form, defaultForm())
  formRef.value?.clearValidate()
}

function eventTypeLabel(et: string) {
  return ({
    THRESHOLD: '阈值告警',
    COMPREHENSIVE: '综合告警',
    OFFLINE: '设备离线'
  } as Record<string, string>)[et] || et
}
function levelLabel(lv: string) {
  return ({
    '1': '一级（警报）',
    '2': '二级（警戒）',
    '3': '三级（警示）',
    '4': '四级（注意）'
  } as Record<string, string>)[lv] || lv
}
function channelLabel(ch: string) {
  return ({ SYSTEM: '系统', SMS: '短信', EMAIL: '邮件' } as Record<string, string>)[ch] || ch
}
function channelTagType(ch: string) {
  return ({ SYSTEM: 'success', SMS: 'warning', EMAIL: 'info' } as Record<string, string>)[ch] || ''
}

onMounted(() => {
  loadOptions()
  getList()
})
</script>

<style scoped>
.form-help {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.empty-text {
  color: #c0c4cc;
}
</style>
