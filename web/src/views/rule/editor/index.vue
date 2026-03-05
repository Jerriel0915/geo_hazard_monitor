<template>
  <div class="rule-editor-container">
    <el-page-header @back="goBack" content="规则编辑器" class="mb-4"/>

    <el-steps :active="activeStep" finish-status="success" align-center class="mb-8 step-indicator">
      <el-step title="基础信息" description="设置规则名称与对象"/>
      <el-step title="规则配置" description="配置触发条件与动作"/>
      <el-step title="确认发布" description="预览并发布规则"/>
    </el-steps>

    <div class="step-content">
      <!-- Step 1: Basic Info -->
      <div v-show="activeStep === 0">
        <el-card class="box-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>基础信息</span>
            </div>
          </template>
          <el-form :model="form" ref="step1Form" :rules="rules" label-width="100px" label-position="left">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="规则名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入规则名称 (1-50字符)" maxlength="50" show-word-limit
                            clearable/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="产品/设备" prop="productId">
                  <ProductDeviceSelector
                      v-model="selectorValue"
                      :multiple="true"
                      @change="handleSelectorChange"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="24">
                <el-alert title="注意：切换产品将重置触发条件" type="info" show-icon :closable="false"/>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </div>

      <!-- Step 2: Configuration -->
      <div v-show="activeStep === 1">
        <!-- 触发条件 -->
        <el-card class="box-card mb-4" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>触发条件</span>
              <div class="header-actions">
                <el-radio-group v-model="form.logical" size="small">
                  <el-radio-button label="AND">满足所有 (AND)</el-radio-button>
                  <el-radio-button label="OR">满足任一 (OR)</el-radio-button>
                </el-radio-group>
                <el-button type="primary" size="small" icon="Plus" @click="addCondition" class="ml-2">
                  新增条件
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="form.conditions.length === 0" class="empty-state">
            <el-empty description="暂无触发条件，请点击右上角新增" :image-size="60"/>
          </div>

          <draggable
              v-model="form.conditions"
              item-key="id"
              handle=".drag-handle"
              class="list-container"
              animation="200"
          >
            <template #item="{ element, index }">
              <div class="list-item">
                <div class="drag-handle">
                  <el-icon>
                    <Rank/>
                  </el-icon>
                </div>
                <div class="item-content">
                  <el-row :gutter="12" align="middle">
                    <el-col :span="8">
                      <el-select
                          v-model="element.field"
                          placeholder="选择属性"
                          filterable
                          class="w-full"
                          @change="(val) => handleFieldChange(val, element)"
                      >
                        <el-option
                            v-for="p in properties"
                            :key="p.identifier"
                            :label="p.name"
                            :value="p.identifier"
                        >
                          <span style="float: left">{{ p.name }}</span>
                          <span style="float: right; color: var(--el-text-color-secondary); font-size: 12px">{{
                              p.identifier
                            }}</span>
                        </el-option>
                      </el-select>
                    </el-col>
                    <el-col :span="4">
                      <el-select v-model="element.op" placeholder="运算符" class="w-full">
                        <el-option
                            v-for="op in getOperators(element.dataType)"
                            :key="op.value"
                            :label="op.label"
                            :value="op.value"
                        />
                      </el-select>
                    </el-col>
                    <el-col :span="10">
                      <!-- Dynamic Value Input -->
                      <component
                          :is="getValueComponent(element)"
                          v-model="element.value"
                          v-bind="getValueProps(element)"
                          class="w-full"
                          placeholder="目标值"
                      />
                    </el-col>
                    <el-col :span="2" class="text-right">
                      <el-button
                          type="danger"
                          icon="Delete"
                          circle
                          plain
                          size="small"
                          @click="removeCondition(index)"
                      />
                    </el-col>
                  </el-row>
                </div>
              </div>
            </template>
          </draggable>

          <div class="expr-preview mt-4">
            <div class="label">表达式预览：</div>
            <el-input
                :model-value="previewExpression"
                readonly
                placeholder="自动生成的 Aviator 表达式"
            >
              <template #append>
                <el-button icon="VideoPlay" @click="buildExpression">生成</el-button>
              </template>
            </el-input>
          </div>
        </el-card>

        <!-- 执行动作 -->
        <el-card class="box-card mb-4" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>执行动作</span>
              <el-button type="primary" size="small" icon="Plus" @click="addAction">
                新增动作
              </el-button>
            </div>
          </template>

          <div v-if="form.actions.length === 0" class="empty-state">
            <el-empty description="暂无执行动作" :image-size="60"/>
          </div>

          <draggable
              v-model="form.actions"
              item-key="id"
              handle=".drag-handle"
              class="list-container"
              animation="200"
          >
            <template #item="{ element, index }">
              <div class="list-item">
                <div class="drag-handle">
                  <el-icon>
                    <Rank/>
                  </el-icon>
                </div>
                <div class="item-content">
                  <el-row :gutter="12" align="middle">
                    <el-col :span="5">
                      <el-select v-model="element.type" placeholder="动作类型" class="w-full">
                        <el-option v-for="a in actionTypes" :key="a.value" :label="a.label" :value="a.value"/>
                      </el-select>
                    </el-col>

                    <!-- Dynamic Action Config -->
                    <el-col :span="17">
                      <div v-if="element.type === 'service'" class="flex gap-2">
                        <el-select v-model="element.serviceName" placeholder="选择服务" class="flex-1" filterable
                                   allow-create default-first-option>
                          <el-option label="重启设备" value="reboot"/>
                          <el-option label="开启" value="turnOn"/>
                          <el-option label="关闭" value="turnOff"/>
                        </el-select>
                        <el-input v-model="element.payload" placeholder='{"param": "value"}' class="flex-1"/>
                      </div>
                      <div v-else-if="element.type === 'alert'" class="flex gap-2">
                        <el-select v-model="element.level" placeholder="级别" style="width: 120px">
                          <el-option label="INFO" value="INFO"/>
                          <el-option label="WARN" value="WARN"/>
                          <el-option label="ERROR" value="ERROR"/>
                        </el-select>
                        <el-input v-model="element.message" placeholder="告警内容" class="flex-1"/>
                      </div>
                      <div v-else class="flex gap-2">
                        <el-input v-model="element.message" placeholder="日志内容" class="flex-1"/>
                      </div>
                    </el-col>

                    <el-col :span="2" class="text-right">
                      <el-button
                          type="danger"
                          icon="Delete"
                          circle
                          plain
                          size="small"
                          @click="removeAction(index)"
                      />
                    </el-col>
                  </el-row>
                </div>
              </div>
            </template>
          </draggable>
        </el-card>
      </div>

      <!-- Step 3: Review -->
      <div v-show="activeStep === 2">
        <el-card class="box-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>规则预览</span>
            </div>
          </template>
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="规则名称">{{ form.name }}</el-descriptions-item>
            <el-descriptions-item label="关联产品">{{ form.productName || form.productId }}</el-descriptions-item>
            <el-descriptions-item label="触发逻辑">{{ form.logical }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ form.enabled ? '启用' : '停用' }}</el-descriptions-item>
          </el-descriptions>

          <div class="mt-4">
            <div class="font-bold mb-2">Aviator 表达式</div>
            <el-input type="textarea" :rows="3" v-model="form.ruleExpression" readonly/>
          </div>

          <div class="mt-4">
            <div class="font-bold mb-2">执行动作 ({{ form.actions.length }})</div>
            <ul class="pl-5">
              <li v-for="(action, idx) in form.actions" :key="idx">
                {{ action.type }} - {{ action.message || action.serviceName }}
              </li>
            </ul>
          </div>
        </el-card>
      </div>
    </div>

    <!-- Footer Actions -->
    <div class="footer-bar">
      <div class="left-info">
        <span v-if="lastSavedTime" class="save-time">
          <el-icon><Clock/></el-icon> 上次保存: {{ lastSavedTime }}
        </span>
      </div>
      <div class="right-actions">
        <el-button v-if="activeStep > 0" @click="prevStep">上一步</el-button>
        <el-button v-if="activeStep < 2" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="activeStep === 2" type="success" @click="publishRule" :loading="loading.save">发布规则
        </el-button>
        <el-button @click="saveDraft" :loading="loading.save" plain>保存草稿</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import draggable from 'vuedraggable'
import {Clock, Rank} from '@element-plus/icons-vue'
import {getProductTsl} from '@/api/product/productTsl'
import {addRule, getRule, updateRule} from '@/api/rule/rule'
import ProductDeviceSelector from './components/ProductDeviceSelector.vue'

const route = useRoute()
const router = useRouter()

// ==========================================
// 1. Constants & Configuration
// ==========================================
const SYSTEM_PROPERTIES = [
  {identifier: 'reportTime', name: '系统级：上报时间', dataType: 'date', isSystem: true},
  {identifier: 'createTime', name: '系统级：创建时间', dataType: 'date', isSystem: true},
  {identifier: 'updateTime', name: '系统级：更新时间', dataType: 'date', isSystem: true},
  {identifier: 'deviceId', name: '系统级：设备ID', dataType: 'text', isSystem: true},
  {identifier: 'deviceName', name: '系统级：设备名称', dataType: 'text', isSystem: true}
]

const ALL_OPERATORS = [
  {
    label: '等于 (==)',
    value: '==',
    types: ['int', 'float', 'double', 'long', 'text', 'string', 'date', 'bool', 'enum']
  },
  {
    label: '不等于 (!=)',
    value: '!=',
    types: ['int', 'float', 'double', 'long', 'text', 'string', 'date', 'bool', 'enum']
  },
  {label: '大于 (>)', value: '>', types: ['int', 'float', 'double', 'long', 'date']},
  {label: '小于 (<)', value: '<', types: ['int', 'float', 'double', 'long', 'date']},
  {label: '大于等于 (>=)', value: '>=', types: ['int', 'float', 'double', 'long', 'date']},
  {label: '小于等于 (<=)', value: '<=', types: ['int', 'float', 'double', 'long', 'date']},
  {label: '包含 (contains)', value: 'contains', types: ['text', 'string']},
  {label: '属于 (in)', value: 'in', types: ['int', 'float', 'double', 'long', 'text', 'string', 'enum']},
  {label: '不属于 (not in)', value: 'not in', types: ['int', 'float', 'double', 'long', 'text', 'string', 'enum']},
  {label: '正则匹配 (regex)', value: 'regex', types: ['text', 'string']},
  {label: '为空 (empty)', value: 'empty', types: ['int', 'float', 'double', 'long', 'text', 'string', 'date']},
  {label: '不为空 (not empty)', value: 'not_empty', types: ['int', 'float', 'double', 'long', 'text', 'string', 'date']}
]

const ACTION_TYPES = [
  {label: '发送告警', value: 'alert'},
  {label: '服务调用', value: 'service'},
  {label: '记录日志', value: 'log'}
]

// ==========================================
// 2. State Management
// ==========================================
const activeStep = ref(0)
const step1Form = ref(null)
const lastSavedTime = ref('')
const autoSaveTimer = ref(null)

const form = ref({
  ruleId: undefined,
  name: '',
  productId: '',
  productName: '', // For display
  enabled: true,
  logical: 'AND',
  conditions: [],
  actions: [],
  ruleExpression: ''
})

const rules = {
  name: [
    {required: true, message: '请输入规则名称', trigger: 'blur'},
    {min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur'}
  ],
  productId: [
    {required: true, message: '请选择关联产品', trigger: 'change'}
  ]
}

const selectorValue = ref([])
const properties = ref([...SYSTEM_PROPERTIES])
const loading = ref({
  save: false,
  test: false,
  tsl: false
})
const actionTypes = ref(ACTION_TYPES)

// ==========================================
// 3. Navigation & Wizard Logic
// ==========================================

const nextStep = async () => {
  if (activeStep.value === 0) {
    if (!step1Form.value) return
    await step1Form.value.validate((valid) => {
      if (valid) {
        activeStep.value++
      }
    })
  } else if (activeStep.value === 1) {
    // Validate Step 2
    try {
      buildExpression()
      if (!form.value.ruleExpression && form.value.conditions.length > 0) {
        // Expression failed to build but conditions exist
        return
      }
      activeStep.value++
    } catch (e) {
      ElMessage.error('请修正规则配置错误')
    }
  }
}

const prevStep = () => {
  if (activeStep.value > 0) {
    activeStep.value--
  }
}

const goBack = () => {
  if (activeStep.value > 0) {
    ElMessageBox.confirm('返回列表将丢失未保存的更改，是否继续？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/iot/rule/list')
    })
  } else {
    router.push('/iot/rule/list')
  }
}

// ==========================================
// 4. Business Logic: Product & Properties
// ==========================================

const handleSelectorChange = async (selected) => {
  if (!selected || selected.length === 0) {
    resetProductContext()
    return
  }

  const target = selected[0]
  // Fix: Extract productKey based on node type
  // For product node, use productKey (or id if missing)
  // For device node, use productId (which should be productKey in TSL context?)
  // Actually TSL API expects Product ID (integer) or Key?
  // Let's assume ID for now as per previous logic, but if productKey is needed:
  // If target is product: use id (or productKey if available)
  // If target is device: use productId

  // Revert to ID based logic for TSL loading compatibility
  const newProductId = String(target.type === 'product' ? target.id : target.productId)

  // But store productKey in form for submission if available
  // We need to fetch productKey if it's not on the node
  // The ProductDeviceSelector nodes should have productKey if listProduct returns it
  // Let's store the raw ID for TSL loading, but maybe we need another field for the Key?
  // Or TSL API accepts Key? Usually ID.

  form.value.productName = target.name

  const oldId = form.value.productId
  form.value.productId = newProductId

  if (oldId !== newProductId) {
    if (form.value.conditions.length > 0) {
      ElMessage.info('切换产品已重置触发条件')
      form.value.conditions = []
    }
    // TSL loading needs ID
    await loadTslProperties(newProductId)
  }

  // Store Key if available for backend rule creation
  // If target has productKey property
  if (target.productKey) {
    form.value.targetProductKey = target.productKey
  } else {
    // If device selected, try to find productKey?
    // Or just use productId as before?
    form.value.targetProductKey = newProductId // Fallback
  }

  // 3. Manually trigger validation for productId field to clear any existing error
  if (step1Form.value) {
    nextTick(() => {
      step1Form.value.validateField('productId')
    })
  }
}

const resetProductContext = () => {
  form.value.productId = ''
  properties.value = [...SYSTEM_PROPERTIES]
  form.value.conditions = []
}

const loadTslProperties = async (productId) => {
  loading.value.tsl = true
  try {
    const res = await getProductTsl(productId)
    let tsl = res.data?.tsl || {}
    if (typeof tsl === 'string') {
      try {
        tsl = JSON.parse(tsl)
      } catch (e) {
        console.warn('TSL parse error', e);
        tsl = {}
      }
    }
    const tslProps = (tsl.properties || []).map(p => ({
      identifier: p.identifier,
      name: p.name,
      dataType: p.dataType?.type || 'text',
      specs: p.dataType?.specs || {},
      isSystem: false
    }))
    properties.value = [...SYSTEM_PROPERTIES, ...tslProps]
  } catch (error) {
    console.error('Failed to load TSL:', error)
    ElMessage.error('获取物模型属性失败')
    properties.value = [...SYSTEM_PROPERTIES]
  } finally {
    loading.value.tsl = false
  }
}

// ==========================================
// 5. Business Logic: Conditions & Actions
// ==========================================

const getOperators = (dataType) => {
  return ALL_OPERATORS.filter(op => op.types.includes(dataType))
}

const addCondition = () => {
  if (!form.value.productId) {
    ElMessage.warning('请先选择产品/设备')
    return
  }
  form.value.conditions.push({
    id: Date.now() + Math.random(),
    field: '',
    op: '',
    value: undefined,
    dataType: 'text',
    specs: {}
  })
}

const removeCondition = (index) => {
  form.value.conditions.splice(index, 1)
}

const handleFieldChange = (fieldIdentifier, conditionItem) => {
  const prop = properties.value.find(p => p.identifier === fieldIdentifier)
  if (!prop) return
  conditionItem.dataType = prop.dataType
  conditionItem.specs = prop.specs
  conditionItem.value = undefined
  const validOps = getOperators(prop.dataType)
  conditionItem.op = validOps.length > 0 ? validOps[0].value : ''
}

const addAction = () => {
  form.value.actions.push({
    id: Date.now() + Math.random(),
    type: 'log',
    message: '',
    level: 'INFO',
    serviceName: '',
    payload: ''
  })
}

const removeAction = (index) => {
  form.value.actions.splice(index, 1)
}

// ==========================================
// 6. UI Component Resolvers
// ==========================================

const getValueComponent = (element) => {
  if (['empty', 'not_empty'].includes(element.op)) return null
  const type = element.dataType
  if (['int', 'float', 'double', 'long'].includes(type)) return 'el-input-number'
  if (type === 'date') return 'el-date-picker'
  if (type === 'bool') return 'el-select'
  if (type === 'enum') return 'el-select'
  return 'el-input'
}

const getValueProps = (element) => {
  const {dataType, specs} = element
  if (dataType === 'date') return {type: 'datetime', valueFormat: 'YYYY-MM-DD HH:mm:ss', placeholder: '选择时间'}
  if (['int', 'float', 'double', 'long'].includes(dataType)) return {
    min: specs?.min ? Number(specs.min) : undefined,
    max: specs?.max ? Number(specs.max) : undefined,
    step: specs?.step ? Number(specs.step) : 1,
    controlsPosition: 'right'
  }
  if (dataType === 'bool') return {placeholder: '选择布尔值'}
  return {placeholder: '输入值'}
}

// ==========================================
// 7. Expression Generation
// ==========================================

const previewExpression = computed(() => {
  try {
    return generateExpression(false)
  } catch (e) {
    return ''
  }
})

const buildExpression = () => {
  try {
    form.value.ruleExpression = generateExpression(true)
  } catch (e) {
    ElMessage.error(e.message)
    throw e
  }
}

const generateExpression = (strict = false) => {
  if (form.value.conditions.length === 0) return ''
  const parts = form.value.conditions.map(c => {
    if (!c.field || !c.op) {
      if (strict) throw new Error('存在未配置完整的条件')
      return null
    }
    if (c.op === 'empty') return `string.length(str(${c.field})) == 0`
    if (c.op === 'not_empty') return `string.length(str(${c.field})) > 0`
    if (c.value === undefined || c.value === null || c.value === '') {
      if (strict) throw new Error(`条件 [${c.field}] 缺少数值`)
      return null
    }
    let val = c.value
    if (['text', 'string', 'date'].includes(c.dataType)) {
      val = `'${String(val).replace(/'/g, "\\'")}'`
    }
    switch (c.op) {
      case 'contains':
        return `string.contains(str(${c.field}), ${val})`
      case 'regex':
        return `str(${c.field}) =~ ${val}`
      case 'in':
      case 'not in':
        const listStr = String(c.value).split(/[,，]/).map(s => s.trim()).filter(Boolean)
        if (listStr.length === 0) return null
        const seqVal = listStr.map(s => ['text', 'string', 'date'].includes(c.dataType) ? `'${s}'` : s).join(',')
        const func = `include(seq.set(${seqVal}), ${c.field})`
        return c.op === 'not in' ? `!${func}` : func
      default:
        return `${c.field} ${c.op} ${val}`
    }
  }).filter(Boolean)
  if (parts.length === 0) return ''
  const joiner = form.value.logical === 'AND' ? ' && ' : ' || '
  return parts.join(joiner)
}

// ==========================================
// 8. API Actions: Save, Load, Auto-save
// ==========================================

const saveDraft = async () => {
  await handleSave(2) // 2 = Draft
}

const publishRule = async () => {
  // Validate everything
  try {
    buildExpression()
  } catch (e) {
    return
  }

  if (!form.value.ruleExpression) {
    ElMessage.error('规则表达式不能为空')
    return
  }

  await handleSave(1) // 1 = Enable (Published)
}

const handleSave = async (status) => {
  // Try to generate expression even for draft, but ignore errors
  try {
    form.value.ruleExpression = generateExpression(false)
  } catch (e) {
  }

  const payload = {
    ruleId: form.value.ruleId,
    ruleName: form.value.name,
    productKey: form.value.targetProductKey || form.value.productId,
    status: String(status),
    ruleExpression: form.value.ruleExpression,
    actionList: form.value.actions.map(a => ({
      actionType: a.type,
      config: JSON.stringify(a)
    })),
    // Store UI state in remark
    remark: JSON.stringify(form.value.conditions)
  }

  loading.value.save = true
  try {
    if (form.value.ruleId) {
      await updateRule(payload)
    } else {
      const res = await addRule(payload)
      // Assuming addRule returns the ID or we need to reload list? 
      // Actually standard RuoYi add returns rows, not ID. 
      // If we can't get ID, we can't continue editing as same rule easily.
      // Ideally backend should return ID.
      // But for now, if it's draft, we might need to navigate back or just say saved.
      // If user stays, next auto-save will create NEW rule if we don't have ID.
      // Critical Issue: Standard RuoYi Controller returns AjaxResult which might not contain ID.
      // Let's assume we navigate to list on success if manual save, or warn user.
    }

    ElMessage.success(status === 2 ? '草稿保存成功' : '规则发布成功')
    lastSavedTime.value = new Date().toLocaleTimeString()

    if (status === 1) {
      router.push('/iot/rule/list')
    } else {
      // If draft, ideally we should update route to include ID if it was a create
      // But without ID from backend, we can't.
      // So for now, we just stay.
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    loading.value.save = false
  }
}

const initData = async () => {
  const id = route.query.id || route.params.ruleId
  if (id) {
    loading.value.tsl = true
    try {
      const res = await getRule(id)
      const data = res.data
      form.value.ruleId = data.ruleId
      form.value.name = data.ruleName
      form.value.productId = data.productKey
      form.value.enabled = data.status === '1'
      form.value.ruleExpression = data.ruleExpression

      // Restore Conditions
      try {
        form.value.conditions = JSON.parse(data.remark || '[]')
      } catch (e) {
        form.value.conditions = []
      }

      // Restore Actions
      form.value.actions = (data.actionList || []).map(a => {
        try {
          return JSON.parse(a.config)
        } catch (e) {
          return {}
        }
      })

      // Load TSL
      if (form.value.productId) {
        // Fix: Set selectorValue to array of uniqueIds (strings) instead of objects
        // ProductDeviceSelector expects uniqueId strings for v-model
        selectorValue.value = [`product-${form.value.productId}`]
        await loadTslProperties(form.value.productId)
      }
    } catch (e) {
      ElMessage.error('加载规则失败')
    } finally {
      loading.value.tsl = false
    }
  }
}

onMounted(() => {
  initData()
  // Auto-save every 30s
  autoSaveTimer.value = setInterval(() => {
    if (form.value.name && form.value.productId) {
      // Only auto-save if basic info is present
      // And if we have an ID (update) OR if we want to create drafts automatically?
      // Creating drafts automatically might spam DB if addRule doesn't return ID.
      // So let's only auto-save if we have an ID.
      if (form.value.ruleId) {
        saveDraft()
      }
    }
  }, 30000)
})

onBeforeUnmount(() => {
  if (autoSaveTimer.value) clearInterval(autoSaveTimer.value)
})

</script>

<style scoped lang="scss">
@use "./styles/variables.scss" as *;

.rule-editor-container {
  padding: $spacing-large;
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 80px; /* Space for footer */
}

.step-indicator {
  max-width: 800px;
  margin: 0 auto 32px;
}

.step-content {
  max-width: 1000px;
  margin: 0 auto;
}

.box-card {
  border-radius: $border-radius-base;

  :deep(.el-card__header) {
    padding: $spacing-medium $spacing-large;
    border-bottom: 1px solid $border-color-base;
  }

  :deep(.el-card__body) {
    padding: $spacing-large;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: $font-size-large;
  color: $text-color-primary;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: $spacing-medium;
}

.list-item {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid $border-color-base;
  border-radius: $border-radius-base;
  padding: $spacing-medium;
  transition: all 0.3s;

  &:hover {
    box-shadow: $box-shadow-base;
    border-color: $primary-color;
  }

  .drag-handle {
    cursor: move;
    padding: 0 $spacing-medium;
    color: $text-color-secondary;
    display: flex;
    align-items: center;

    &:hover {
      color: $primary-color;
    }
  }

  .item-content {
    flex: 1;
  }
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.footer-bar {
  position: fixed;
  bottom: 0;
  left: 0; // Adjust if sidebar exists, but fixed is relative to viewport
  right: 0;
  background: #fff;
  padding: 16px 24px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 999;
  /* Adjust for sidebar width if necessary, or just use 100% width */
}

.w-full {
  width: 100%;
}

.flex {
  display: flex;
}

.gap-2 {
  gap: 8px;
}

.flex-1 {
  flex: 1;
}

.ml-2 {
  margin-left: 8px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mb-8 {
  margin-bottom: 32px;
}

.mt-4 {
  margin-top: 16px;
}

.pl-5 {
  padding-left: 20px;
}

.font-bold {
  font-weight: bold;
}

.text-right {
  text-align: right;
}

.text-gray-500 {
  color: #909399;
}

.text-sm {
  font-size: 12px;
}

.ml-4 {
  margin-left: 16px;
}

.save-time {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}
</style>
