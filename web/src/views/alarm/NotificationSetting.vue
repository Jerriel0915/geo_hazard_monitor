<template>
  <div class="page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">通知设置</h2>
        <span class="header__subtitle">告警分发规则配置与通知渠道管理</span>
      </div>
      <div class="header__right">
        <el-button type="primary" @click="handleAddAlarmRule">新增规则</el-button>
        <el-button type="success" @click="handleBatchEnableAlarm" :disabled="selectedAlarmRules.length === 0">批量启用</el-button>
        <el-button type="warning" @click="handleBatchDisableAlarm" :disabled="selectedAlarmRules.length === 0">批量禁用</el-button>
        <el-button type="success" @click="handleImportAlarm">导入</el-button>
        <el-button type="warning" @click="handleExportAlarm">导出</el-button>
      </div>
    </div>

    <div class="search">
      <el-input v-model="alarmSearchForm.hazardPoint" placeholder="隐患点名称" clearable />
      <el-select v-model="alarmSearchForm.type" placeholder="类型" clearable>
        <el-option label="监测告警" value="alarm"/>
        <el-option label="设备离线通知" value="offline"/>
      </el-select>
      <el-select v-model="alarmSearchForm.status" placeholder="状态" clearable>
        <el-option label="启用" :value="1"/>
        <el-option label="禁用" :value="0"/>
      </el-select>
      <el-select v-model="alarmSearchForm.channel" placeholder="渠道" clearable>
        <el-option label="短信" value="sms"/>
        <el-option label="邮件" value="email"/>
        <el-option label="系统消息" value="system"/>
      </el-select>
      <el-button type="primary" @click="handleAlarmSearch">查询</el-button>
      <el-button @click="handleAlarmReset">重置</el-button>
    </div>

    <div class="table-wrap">
      <div class="table-wrap__scroll">
        <el-table :data="alarmRuleList" border stripe v-loading="loading" @selection-change="handleAlarmSelectionChange">
          <el-table-column type="selection" width="55" align="center"/>
          <el-table-column label="隐患点" min-width="160">
            <template #default="{ row }">
              <span v-for="(name, idx) in row.hazardPointNames" :key="idx">
                <el-tag size="small" style="margin-right: 4px;">{{ name }}</el-tag>
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="160" align="center">
            <template #default="{ row }">
              <el-tag :type="row.type === 'alarm' ? 'danger' : 'warning'" size="small">
                {{ row.type === 'alarm' ? '监测告警' : '设备离线通知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="告警等级/关联设备" min-width="180">
            <template #default="{ row }">
              <template v-if="row.type === 'alarm'">
                <el-tag v-for="(lvl, idx) in row.level" :key="idx" :type="getAlarmLevelType(lvl)" size="small" style="margin-right: 4px;">{{ lvl }}</el-tag>
                <span v-if="!row.level || row.level.length === 0" class="empty-text">无</span>
              </template>
              <template v-else-if="row.type === 'offline' && row.deviceNames && row.deviceNames.length > 0">
                <el-tag v-for="(name, idx) in row.deviceNames" :key="idx" size="small" style="margin-right: 4px;">{{ name }}</el-tag>
              </template>
              <span v-else class="empty-text">无</span>
            </template>
          </el-table-column>
          <el-table-column prop="persons" label="通知人员" min-width="140">
            <template #default="{ row }">
              <el-tag v-for="p in row.persons" :key="p" size="small" style="margin-right: 4px;">{{ p }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="channels" label="通知渠道" min-width="150">
            <template #default="{ row }">
              <span v-for="(c, idx) in row.channels" :key="c">
                {{ getChannelLabel(c) }}{{ idx < row.channels.length - 1 ? '、' : '' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="执行描述" min-width="180">
            <template #default="{ row }">
              {{ getExecDescription(row.execTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.status" :active-value="1" :inactive-value="0" size="small"/>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click="handleEditAlarmRule(row)">编辑</el-button>
              <el-button type="danger" text size="small" @click="handleDeleteAlarmRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrap__pagination">
        <el-pagination
            v-model:current-page="alarmPagination.page"
            v-model:page-size="alarmPagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="alarmPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            prev-text="上一页"
            next-text="下一页"
            :disabled="alarmPagination.total === 0"
            @size-change="handleAlarmSizeChange"
            @current-change="handleAlarmPageChange"
        />
      </div>
    </div>

    <!-- 告警规则弹窗 -->
    <el-dialog
        :title="alarmDialogTitle"
        v-model="alarmDialogVisible"
        width="650px"
        :close-on-click-modal="false"
    >
      <el-form
          ref="alarmFormRef"
          :model="alarmFormData"
          :rules="alarmFormRules"
          label-width="100px"
      >
        <el-form-item label="隐患点" :prop="isEditAlarm ? 'hazardPointId' : 'hazardPointIds'">
          <el-select
              v-model="currentHazardPoints"
              :multiple="!isEditAlarm"
              placeholder="请选择隐患点"
              style="width: 100%"
          >
            <el-option
                v-for="hp in hazardPointList"
                :key="hp.id"
                :label="hp.name"
                :value="hp.id"
            />
          </el-select>
          <span v-if="!isEditAlarm" class="form-hint">支持多选，确定后按隐患点列表循环保存</span>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="alarmFormData.type">
            <el-radio label="alarm">监测告警</el-radio>
            <el-radio label="offline">设备离线通知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="告警等级" prop="level" v-if="alarmFormData.type === 'alarm'">
          <el-select v-model="alarmFormData.level" multiple placeholder="请选择告警等级（支持多选）" style="width: 100%">
            <el-option label="四级(注意)" value="四级(注意)"/>
            <el-option label="三级(警示)" value="三级(警示)"/>
            <el-option label="二级(警戒)" value="二级(警戒)"/>
            <el-option label="一级(警报)" value="一级(警报)"/>
          </el-select>
        </el-form-item>
        <el-form-item label="关联设备" prop="deviceIds" v-if="alarmFormData.type === 'offline'">
          <el-select v-model="alarmFormData.deviceIds" multiple placeholder="请选择设备" style="width: 100%">
            <el-option
                v-for="device in deviceList"
                :key="device.id"
                :label="`${device.deviceCode} - ${device.name}`"
                :value="device.id"
            />
          </el-select>
          <span class="form-hint">支持多选</span>
        </el-form-item>
        <el-form-item label="通知人员" prop="personIds">
          <el-select v-model="alarmFormData.personIds" multiple placeholder="请选择通知人员" style="width: 100%">
            <el-option
                v-for="user in userList"
                :key="user.id"
                :label="user.realName"
                :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" prop="channels">
          <el-checkbox-group v-model="alarmFormData.channels">
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="system" checked>系统消息</el-checkbox>
          </el-checkbox-group>
          <span class="form-hint">系统消息包括PC端和移动端的系统消息，默认勾选</span>
        </el-form-item>
        <el-form-item label="执行时间" v-if="alarmFormData.type === 'offline'">
          <el-radio-group v-model="alarmFormData.execType" class="exec-type-group">
            <el-radio label="realtime">实时执行</el-radio>
            <el-radio label="timed">定时</el-radio>
          </el-radio-group>
          <div v-if="alarmFormData.execType === 'timed'" class="exec-time-config">
            <span class="exec-label">每</span>
            <el-input-number v-model="alarmFormData.execFrequencyNum" :min="1" :max="99" style="width: 80px"/>
            <el-select v-model="alarmFormData.execFrequencyUnit" style="width: 100px">
              <el-option label="分钟" value="minute"/>
              <el-option label="小时" value="hour"/>
              <el-option label="天" value="day"/>
              <el-option label="周" value="week"/>
              <el-option label="月" value="month"/>
              <el-option label="年" value="year"/>
            </el-select>
            <span class="exec-label">在</span>
            <el-input v-model="alarmFormData.execTimePoints" placeholder="多个时间点用逗号隔开" style="width: 150px"/>
            <span class="exec-label">执行</span>
            <span
                class="form-hint">时间点示例：分钟填秒数(10,20)，小时填分钟数(10,50)，天填小时数(8,10)，周填星期(1-7)，月填日期(1,16)，年填天数(1,36)</span>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="alarmFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
              v-model="alarmFormData.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入备注"
              maxlength="200"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAlarmSubmit" :loading="alarmSubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage, ElMessageBox} from 'element-plus'

interface AlarmRule {
  id: number
  hazardPointIds: number[]
  hazardPointNames: string[]
  type: 'alarm' | 'offline'
  level: string[]
  persons: string[]
  personIds: number[]
  deviceIds: number[]
  deviceNames: string[]
  channels: string[]
  execTime: string
  status: number
  remark: string
  createTime: string
}

interface HazardPoint {
  id: number
  name: string
}

interface User {
  id: number
  realName: string
}

interface Device {
  id: number
  name: string
  deviceCode: string
}

const loading = ref(false)

// 告警分发
const alarmSearchForm = reactive({
  hazardPoint: '',
  type: '',
  status: undefined as number | undefined,
  channel: ''
})

const alarmPagination = reactive({page: 1, size: 10, total: 0})

const hazardPointList = ref<HazardPoint[]>([
  {id: 1, name: 'XX山区滑坡监测点'},
  {id: 2, name: 'YY矿区沉降监测点'},
  {id: 3, name: 'ZZ水库坝体监测点'},
  {id: 4, name: 'WW公路边坡监测点'},
  {id: 5, name: 'QQ隧道口监测点'}
])

const userList = ref<User[]>([
  {id: 1, realName: '系统管理员'},
  {id: 2, realName: '张三'},
  {id: 3, realName: '李四'},
  {id: 4, realName: '王五'},
  {id: 5, realName: '赵六'}
])

const deviceList = ref<Device[]>([
  {id: 1, name: 'GNSS接收机-A1', deviceCode: 'GNSS-001'},
  {id: 2, name: '裂缝计-B1', deviceCode: 'LF-001'},
  {id: 3, name: '位移计-C1', deviceCode: 'WY-001'},
  {id: 4, name: '雨量计-D1', deviceCode: 'YL-001'},
  {id: 5, name: '水位计-E1', deviceCode: 'SW-001'},
  {id: 6, name: 'GNSS接收机-A2', deviceCode: 'GNSS-002'},
  {id: 7, name: '裂缝计-B2', deviceCode: 'LF-002'},
  {id: 8, name: '视频监控-F1', deviceCode: 'VD-001'}
])

const allAlarmRules = ref<AlarmRule[]>([
  {
    id: 1,
    hazardPointIds: [1],
    hazardPointNames: ['XX山区滑坡监测点'],
    type: 'alarm',
    level: ['一级(警报)'],
    persons: ['张三', '李四'],
    personIds: [2, 3],
    deviceIds: [],
    deviceNames: [],
    channels: ['sms', 'system'],
    execTime: '',
    status: 1,
    remark: '滑坡位移超限立即通知',
    createTime: '2024-01-01 10:00:00'
  },
  {
    id: 2,
    hazardPointIds: [1],
    hazardPointNames: ['XX山区滑坡监测点'],
    type: 'offline',
    level: [],
    persons: ['张三'],
    personIds: [2],
    deviceIds: [1, 2],
    deviceNames: ['GNSS-001 - GNSS接收机-A1', 'LF-001 - 裂缝计-B1'],
    channels: ['sms', 'email', 'system'],
    execTime: 'day|8,14,18',
    status: 1,
    remark: '设备离线通知',
    createTime: '2024-01-05 09:00:00'
  },
  {
    id: 3,
    hazardPointIds: [2, 3],
    hazardPointNames: ['YY矿区沉降监测点', 'ZZ水库坝体监测点'],
    type: 'alarm',
    level: ['二级(警戒)', '一级(警报)'],
    persons: ['王五', '赵六'],
    personIds: [4, 5],
    deviceIds: [],
    deviceNames: [],
    channels: ['sms', 'system'],
    execTime: '',
    status: 1,
    remark: '矿区沉降监测告警',
    createTime: '2024-01-10 08:30:00'
  },
  {
    id: 4,
    hazardPointIds: [3],
    hazardPointNames: ['ZZ水库坝体监测点'],
    type: 'alarm',
    level: ['一级(警报)'],
    persons: ['系统管理员', '张三'],
    personIds: [1, 2],
    deviceIds: [],
    deviceNames: [],
    channels: ['sms', 'system'],
    execTime: '',
    status: 0,
    remark: '水库坝体压力超限',
    createTime: '2024-01-12 10:00:00'
  },
  {
    id: 5,
    hazardPointIds: [4],
    hazardPointNames: ['WW公路边坡监测点'],
    type: 'offline',
    level: [],
    persons: ['李四'],
    personIds: [3],
    deviceIds: [6, 7],
    deviceNames: ['GNSS-002 - GNSS接收机-A2', 'LF-002 - 裂缝计-B2'],
    channels: ['email', 'system'],
    execTime: 'hour|30',
    status: 1,
    remark: '公路边坡设备状态',
    createTime: '2024-01-15 11:00:00'
  },
  {
    id: 6,
    hazardPointIds: [5],
    hazardPointNames: ['QQ隧道口监测点'],
    type: 'alarm',
    level: ['三级(警示)', '四级(注意)'],
    persons: ['王五'],
    personIds: [4],
    deviceIds: [],
    deviceNames: [],
    channels: ['sms', 'system'],
    execTime: '',
    status: 1,
    remark: '隧道口变形监测',
    createTime: '2024-01-20 14:00:00'
  }
])

const alarmRuleList = computed(() => {
  let result = allAlarmRules.value

  if (alarmSearchForm.hazardPoint) {
    result = result.filter(r => r.hazardPointNames.some(name => name.includes(alarmSearchForm.hazardPoint)))
  }
  if (alarmSearchForm.type) {
    result = result.filter(r => r.type === alarmSearchForm.type)
  }
  if (alarmSearchForm.status !== undefined) {
    result = result.filter(r => r.status === alarmSearchForm.status)
  }
  if (alarmSearchForm.channel) {
    result = result.filter(r => r.channels.includes(alarmSearchForm.channel))
  }

  alarmPagination.total = result.length
  const start = (alarmPagination.page - 1) * alarmPagination.size
  return result.slice(start, start + alarmPagination.size)
})

const getAlarmLevelType = (level: string) => {
  const map: Record<string, string> = {
    '一级(警报)': 'danger',
    '二级(警戒)': 'warning',
    '三级(警示)': 'info',
    '四级(注意)': 'success'
  }
  return map[level] || 'info'
}

const getChannelLabel = (channel: string) => {
  const map: Record<string, string> = {sms: '短信', email: '邮件', system: '系统消息'}
  return map[channel] || channel
}

const getExecDescription = (execTime: string) => {
  if (!execTime) return '-'
  const parts = execTime.split('|')
  if (parts.length !== 2) {
    return execTime || '-'
  }

  const [frequency, timeStr] = parts
  const freqLabels: Record<string, string> = {
    'minute': '分钟',
    'hour': '小时',
    'day': '天',
    'week': '周',
    'month': '月',
    'year': '年'
  }

  const freqLabel = freqLabels[frequency] || frequency
  const timeValues = timeStr.split(',').filter(t => t.trim())

  if (frequency === 'minute') {
    return `每${freqLabel}第${timeValues.join('、')}秒执行`
  } else if (frequency === 'hour') {
    return `每${freqLabel}第${timeValues.join('、')}分钟执行`
  } else if (frequency === 'day') {
    return `每${freqLabel}第${timeValues.join('、')}小时执行`
  } else if (frequency === 'week') {
    return `每周${timeValues.join('、')}执行`
  } else if (frequency === 'month') {
    return `每月${timeValues.join('、')}日执行`
  } else if (frequency === 'year') {
    return `每年第${timeValues.join('、')}天执行`
  }

  return `${freqLabel}: ${timeStr}`
}

const handleAlarmSearch = () => {
  alarmPagination.page = 1
}
const handleAlarmReset = () => {
  alarmSearchForm.hazardPoint = ''
  alarmSearchForm.type = ''
  alarmSearchForm.status = undefined
  alarmSearchForm.channel = ''
  alarmPagination.page = 1
}
const handleAlarmSizeChange = (val: number) => {
  alarmPagination.size = val;
  alarmPagination.page = 1
}
const handleAlarmPageChange = (val: number) => {
  alarmPagination.page = val
}

// 告警规则弹窗
const alarmDialogVisible = ref(false)
const alarmDialogTitle = ref('新增告警规则')
const alarmSubmitLoading = ref(false)
const alarmFormRef = ref<FormInstance>()
const isEditAlarm = ref(false)

// 批量操作
const selectedAlarmRules = ref<AlarmRule[]>([])

const alarmFormData = reactive({
  id: 0,
  hazardPointId: undefined as number | undefined,
  hazardPointIds: [] as number[],
  type: 'alarm' as 'alarm' | 'offline',
  level: ['四级(注意)'] as string[],
  personIds: [] as number[],
  deviceIds: [] as number[],
  channels: ['system'] as string[],
  execTime: '',
  execType: 'realtime' as 'realtime' | 'timed',
  execFrequencyNum: 1,
  execFrequencyUnit: 'hour' as 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year',
  execTimePoints: '',
  status: 1,
  remark: ''
})

const currentHazardPoints = computed({
  get: () => {
    if (isEditAlarm.value) {
      return alarmFormData.hazardPointId
    }
    return alarmFormData.hazardPointIds
  },
  set: (val: number | number[]) => {
    if (isEditAlarm.value) {
      alarmFormData.hazardPointId = val as number
    } else {
      alarmFormData.hazardPointIds = val as number[]
    }
  }
})

const alarmFormRules: FormRules = {
  hazardPointId: [{required: true, message: '请选择隐患点', trigger: 'change'}],
  hazardPointIds: [{required: true, message: '请选择隐患点', trigger: 'change', type: 'array'}],
  type: [{required: true, message: '请选择类型', trigger: 'change'}],
  level: [{required: true, message: '请选择告警等级', trigger: 'change', type: 'array'}],
  personIds: [{required: true, message: '请选择通知人员', trigger: 'change', type: 'array'}],
  deviceIds: [{required: true, message: '请选择关联设备', trigger: 'change', type: 'array'}],
  channels: [{required: true, message: '请选择通知渠道', trigger: 'change', type: 'array'}],
  execTime: [{required: true, message: '请输入执行时间', trigger: 'change'}]
}

const handleAddAlarmRule = () => {
  isEditAlarm.value = false
  alarmDialogTitle.value = '新增告警规则'
  resetAlarmForm()
  alarmDialogVisible.value = true
}

const handleEditAlarmRule = (row: AlarmRule) => {
  isEditAlarm.value = true
  alarmDialogTitle.value = '编辑告警规则'

  const execTime = row.execTime || ''
  let execType: 'realtime' | 'timed' = 'realtime'
  let execFrequencyNum = 1
  let execFrequencyUnit: 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year' = 'hour'
  let execTimePoints = ''

  if (execTime) {
    const parts = execTime.split('|')
    if (parts.length === 2) {
      execType = 'timed'
      execFrequencyUnit = parts[0] as 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year'
      execTimePoints = parts[1]
    }
  }

  Object.assign(alarmFormData, {
    id: row.id,
    hazardPointId: row.hazardPointIds.length > 0 ? row.hazardPointIds[0] : undefined,
    hazardPointIds: [...row.hazardPointIds],
    type: row.type,
    level: row.level || ['四级(注意)'],
    personIds: [...row.personIds],
    deviceIds: [...row.deviceIds],
    channels: [...row.channels],
    execTime: execTime,
    execType,
    execFrequencyNum,
    execFrequencyUnit,
    execTimePoints,
    status: row.status,
    remark: row.remark
  })
  alarmDialogVisible.value = true
}

const handleAlarmSelectionChange = (val: AlarmRule[]) => {
  selectedAlarmRules.value = val
}

const handleBatchEnableAlarm = () => {
  if (selectedAlarmRules.value.length === 0) {
    ElMessage.warning('请选择要启用的告警规则')
    return
  }
  const count = selectedAlarmRules.value.length
  ElMessageBox.confirm(`确定要批量启用选中的 ${count} 条告警规则吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    selectedAlarmRules.value.forEach(rule => {
      rule.status = 1
    })
    selectedAlarmRules.value = []
    ElMessage.success(`成功启用 ${count} 条告警规则`)
  }).catch(() => {
  })
}

const handleBatchDisableAlarm = () => {
  if (selectedAlarmRules.value.length === 0) {
    ElMessage.warning('请选择要禁用的告警规则')
    return
  }
  const count = selectedAlarmRules.value.length
  ElMessageBox.confirm(`确定要批量禁用选中的 ${count} 条告警规则吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    selectedAlarmRules.value.forEach(rule => {
      rule.status = 0
    })
    selectedAlarmRules.value = []
    ElMessage.success(`成功禁用 ${count} 条告警规则`)
  }).catch(() => {
  })
}

const handleDeleteAlarmRule = (row: AlarmRule) => {
  ElMessageBox.confirm(`确定要删除该告警规则吗？`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = allAlarmRules.value.findIndex(r => r.id === row.id)
    if (index !== -1) allAlarmRules.value.splice(index, 1)
    ElMessage.success('删除成功')
  }).catch(() => {
  })
}

const resetAlarmForm = () => {
  alarmFormData.id = 0
  alarmFormData.hazardPointId = undefined
  alarmFormData.hazardPointIds = []
  alarmFormData.type = 'alarm'
  alarmFormData.level = ['四级(注意)']
  alarmFormData.personIds = []
  alarmFormData.deviceIds = []
  alarmFormData.channels = ['system']
  alarmFormData.execTime = ''
  alarmFormData.status = 1
  alarmFormData.remark = ''
}

const handleAlarmSubmit = async () => {
  if (!alarmFormRef.value) return
  await alarmFormRef.value.validate((valid) => {
    if (valid) {
      let execTimeValue = ''
      if (alarmFormData.type === 'offline' && alarmFormData.execType === 'timed' && alarmFormData.execTimePoints) {
        execTimeValue = `${alarmFormData.execFrequencyUnit}|${alarmFormData.execTimePoints}`
      }

      alarmSubmitLoading.value = true
      setTimeout(() => {
        const persons = alarmFormData.personIds.map(id => userList.value.find(u => u.id === id)?.realName || '')
        const devices = alarmFormData.deviceIds.map(id => {
          const d = deviceList.value.find(dev => dev.id === id)
          return d ? `${d.deviceCode} - ${d.name}` : ''
        }).filter(Boolean)

        if (isEditAlarm.value) {
          const rule = allAlarmRules.value.find(r => r.id === alarmFormData.id)
          if (rule) {
            const hp = hazardPointList.value.find(h => h.id === alarmFormData.hazardPointId)
            Object.assign(rule, {
              hazardPointIds: alarmFormData.hazardPointId ? [alarmFormData.hazardPointId] : [],
              hazardPointNames: hp ? [hp.name] : [],
              type: alarmFormData.type,
              level: alarmFormData.type === 'alarm' ? [...alarmFormData.level] : [],
              personIds: [...alarmFormData.personIds],
              persons,
              deviceIds: [...alarmFormData.deviceIds],
              deviceNames: [...devices],
              channels: [...alarmFormData.channels],
              execTime: execTimeValue,
              status: alarmFormData.status,
              remark: alarmFormData.remark
            })
          }
          ElMessage.success('修改成功')
        } else {
          const selectedHps = alarmFormData.hazardPointIds.map(id => hazardPointList.value.find(h => h.id === id)).filter((hp): hp is HazardPoint => hp !== undefined)
          selectedHps.forEach(hp => {
            allAlarmRules.value.push({
              id: allAlarmRules.value.length + 1,
              hazardPointIds: [hp.id],
              hazardPointNames: [hp.name],
              type: alarmFormData.type,
              level: alarmFormData.type === 'alarm' ? [...alarmFormData.level] : [],
              personIds: [...alarmFormData.personIds],
              persons,
              deviceIds: [...alarmFormData.deviceIds],
              deviceNames: [...devices],
              channels: [...alarmFormData.channels],
              execTime: execTimeValue,
              status: alarmFormData.status,
              remark: alarmFormData.remark,
              createTime: new Date().toLocaleString('zh-CN', {hour12: false})
            })
          })
          ElMessage.success(`新增成功，共创建 ${selectedHps.length} 条告警规则`)
        }
        alarmDialogVisible.value = false
        alarmSubmitLoading.value = false
      }, 500)
    }
  })
}

const handleImportAlarm = () => {
  ElMessage.info('导入功能开发中')
}

const handleExportAlarm = () => {
  ElMessage.success('告警规则导出成功')
}
</script>

<style scoped>
.exec-type-group {
  display: flex;
  gap: 20px;
}

.exec-time-config {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.exec-label {
  color: #606266;
}

:deep(.el-form--inline .el-form-item) {
  margin-right: 16px;
  margin-bottom: 10px;
}
</style>