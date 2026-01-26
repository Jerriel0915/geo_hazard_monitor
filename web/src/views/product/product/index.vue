<template>
  <div class="app-container">
    <div v-if="!showDetail">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="产品密钥" prop="productKey">
        <el-input
          v-model="queryParams.productKey"
          placeholder="请输入产品密钥"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入产品名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备类型" prop="nodeType">
        <el-select
          v-model="queryParams.nodeType"
          placeholder="请选择设备类型"
          clearable
          @keyup.enter="handleQuery"
        >
          <el-option label="直连设备" value="0"></el-option>
          <el-option label="网关" value="1"></el-option>
          <el-option label="传感器" value="2"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['product:product:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['product:product:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['product:product:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['product:product:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="数据ID" align="center" prop="id" />
      <el-table-column label="产品密钥" align="center" prop="productKey" />
      <el-table-column label="产品名称" align="center" prop="name">
        <template #default="scope">
          <el-button link type="primary" @click="handleDetail(scope.row)">{{ scope.row.name }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="设备类型" align="center" prop="nodeType">
        <template #default="scope">
          <el-tag v-if="scope.row.nodeType === 0">直连设备</el-tag>
          <el-tag v-else-if="scope.row.nodeType === 1" type="success">网关</el-tag>
          <el-tag v-else-if="scope.row.nodeType === 2" type="warning">传感器</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="remarks" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['product:product:edit']">修改</el-button>
          <el-button link type="primary" icon="Setting" @click="handleThingModel(scope.row)" v-hasPermi="['product:product:edit']">物模型设置</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['product:product:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />
    </div>

    <!-- 产品详情视图 -->
    <div v-else class="product-detail">
      <!-- 详情头部 -->
      <div class="detail-header">
        <el-page-header @back="handleBack" :title="'返回'">
          <template #content>
            <span class="text-large font-600 mr-3">{{ currentProduct.name }}</span>
            <el-tag type="success" size="small" class="ml-2">正常</el-tag>
          </template>
          <template #extra>
            <div class="flex items-center">
              <el-button type="primary" class="ml-2">应用配置</el-button>
            </div>
          </template>
        </el-page-header>
        <div class="detail-info mt-2 ml-10">
          <span class="info-label">设备数量：</span>
          <span class="info-value">{{ deviceCount }}</span>
        </div>
      </div>

      <!-- 详情页签 -->
      <el-tabs v-model="activeTab" class="detail-tabs mt-4">
        <el-tab-pane label="配置信息" name="config">
          <el-descriptions :column="2" border class="mt-4">
            <el-descriptions-item label="产品名称">{{ currentProduct.name }}</el-descriptions-item>
            <el-descriptions-item label="产品密钥">{{ currentProduct.productKey }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">
              <el-tag v-if="currentProduct.nodeType === 0">直连设备</el-tag>
              <el-tag v-else-if="currentProduct.nodeType === 1" type="success">网关</el-tag>
              <el-tag v-else-if="currentProduct.nodeType === 2" type="warning">传感器</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="产品描述">{{ currentProduct.remarks }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="物模型" name="tsl">
          <div class="tsl-container mt-4">
            <div class="mb-2">
              <el-button type="primary" plain icon="Plus" @click="openTslDialog('property')">添加属性</el-button>
              <el-button type="primary" plain icon="Plus" @click="openTslDialog('service')">添加功能</el-button>
              <el-button type="primary" plain icon="Plus" @click="openTslDialog('event')">添加事件</el-button>
              <el-button type="success" icon="Check" @click="saveTslToBackend">保存物模型</el-button>
              <el-button type="info" plain icon="Edit" @click="handleThingModel(currentProduct)">JSON编辑</el-button>
            </div>
            <el-tabs type="border-card">
              <el-tab-pane label="属性定义">
                <el-table :data="thingModelData.properties" style="width: 100%">
                  <el-table-column prop="identifier" label="标识符"/>
                  <el-table-column prop="name" label="名称"/>
                  <el-table-column prop="dataType.type" label="数据类型"/>
                  <el-table-column prop="accessMode" label="读写类型">
                    <template #default="scope">
                      {{ scope.row.accessMode === 'r' ? '只读' : '读写' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="unit" label="单位"/>
                  <el-table-column prop="description" label="描述"/>
                  <el-table-column label="操作" align="center" width="150">
                    <template #default="scope">
                      <el-button link type="primary" icon="Edit"
                                 @click="handleEditTslItem('property', scope.row, scope.$index)">修改
                      </el-button>
                      <el-button link type="primary" icon="Delete"
                                 @click="handleDeleteTslItem('property', scope.$index)">删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="功能定义">
                <el-table :data="thingModelData.services" style="width: 100%">
                  <el-table-column prop="identifier" label="标识符"/>
                  <el-table-column prop="name" label="名称"/>
                  <el-table-column prop="callType" label="调用方式">
                    <template #default="scope">
                      {{ scope.row.callType === 'async' ? '异步' : '同步' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="输入参数">
                    <template #default="scope">
                      {{ scope.row.inputData ? scope.row.inputData.length : 0 }} 个参数
                    </template>
                  </el-table-column>
                  <el-table-column label="输出参数">
                    <template #default="scope">
                      {{ scope.row.outputData ? scope.row.outputData.length : 0 }} 个参数
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" align="center" width="150">
                    <template #default="scope">
                      <el-button link type="primary" icon="Edit"
                                 @click="handleEditTslItem('service', scope.row, scope.$index)">修改
                      </el-button>
                      <el-button link type="primary" icon="Delete"
                                 @click="handleDeleteTslItem('service', scope.$index)">删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="事件定义">
                <el-table :data="thingModelData.events" style="width: 100%">
                  <el-table-column prop="identifier" label="标识符"/>
                  <el-table-column prop="name" label="名称"/>
                  <el-table-column prop="type" label="事件类型">
                    <template #default="scope">
                      <el-tag v-if="scope.row.type === 'info'">信息</el-tag>
                      <el-tag v-else-if="scope.row.type === 'alert'" type="warning">告警</el-tag>
                      <el-tag v-else-if="scope.row.type === 'error'" type="danger">故障</el-tag>
                      <span v-else>{{ scope.row.type }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="输出参数">
                    <template #default="scope">
                      {{ scope.row.outputData ? scope.row.outputData.length : 0 }} 个参数
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" align="center" width="150">
                    <template #default="scope">
                      <el-button link type="primary" icon="Edit"
                                 @click="handleEditTslItem('event', scope.row, scope.$index)">修改
                      </el-button>
                      <el-button link type="primary" icon="Delete" @click="handleDeleteTslItem('event', scope.$index)">
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 添加或修改产品管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="productRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="产品密钥" prop="productKey">
        <el-input v-model="form.productKey" placeholder="请输入产品密钥" />
      </el-form-item>
      <el-form-item label="产品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入产品名称" />
      </el-form-item>
      <el-form-item label="设备类型" prop="nodeType">
        <el-select v-model="form.nodeType" placeholder="请选择设备类型">
          <el-option label="直连设备" value="0"></el-option>
          <el-option label="网关" value="1"></el-option>
          <el-option label="传感器" value="2"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="描述" prop="remarks">
        <el-input v-model="form.remarks" type="textarea" placeholder="请输入描述" />
      </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 物模型编辑对话框 -->
    <el-dialog :title="thingModelTitle" v-model="thingModelOpen" width="800px" append-to-body>
      <el-form ref="thingModelRef" :model="thingModelForm" label-width="80px">
        <el-form-item label="产品编号">
          <el-input v-model="thingModelForm.productKey" disabled placeholder="产品编号" />
        </el-form-item>
        <el-form-item label="物模型内容">
          <el-input 
            v-model="thingModelForm.thingModelContent" 
            type="textarea" 
            :rows="10" 
            placeholder="请输入物模型JSON内容" 
          />
        </el-form-item>
        <el-form-item>
          <el-alert
            title="提示信息"
            type="info"
            description="物模型格式应为JSON格式，包含properties（属性）和events（事件）字段。保存后将自动创建对应的产品数据表。"
            show-icon
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitThingModel">保存物模型</el-button>
          <el-button @click="cancelThingModel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- TSL Item Editing Dialog -->
    <el-dialog :title="tslDialog.title" v-model="tslDialog.visible" width="600px" append-to-body>
      <el-form ref="tslItemRef" :model="currentTslItem" label-width="100px" :rules="tslItemRules">
        <!-- Common Fields -->
        <el-form-item label="标识符" prop="identifier">
          <el-input v-model="currentTslItem.identifier" placeholder="例如: temperature" :disabled="tslDialog.isEdit"/>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="currentTslItem.name" placeholder="例如: 温度"/>
        </el-form-item>

        <!-- Property Specific -->
        <template v-if="tslDialog.type === 'property'">
          <el-form-item label="数据类型" prop="dataType.type">
            <el-select v-model="currentTslItem.dataType.type" placeholder="请选择类型">
              <el-option label="int (整数)" value="int"/>
              <el-option label="float (单精度浮点)" value="float"/>
              <el-option label="double (双精度浮点)" value="double"/>
              <el-option label="text (字符串)" value="text"/>
              <el-option label="bool (布尔型)" value="bool"/>
              <el-option label="date (时间)" value="date"/>
              <el-option label="enum (枚举)" value="enum"/>
            </el-select>
          </el-form-item>

          <template v-if="currentTslItem.dataType">
            <el-form-item label="取值范围" v-if="['int', 'float', 'double'].includes(currentTslItem.dataType.type)">
              <div class="flex">
                <el-input v-model="currentTslItem.dataType.specs.min" placeholder="最小值" style="width: 140px"/>
                <span class="mx-2">-</span>
                <el-input v-model="currentTslItem.dataType.specs.max" placeholder="最大值" style="width: 140px"/>
              </div>
            </el-form-item>
            <el-form-item label="步长" v-if="['int', 'float', 'double'].includes(currentTslItem.dataType.type)">
              <el-input v-model="currentTslItem.dataType.specs.step" placeholder="步长"/>
            </el-form-item>
            <el-form-item label="单位" v-if="['int', 'float', 'double'].includes(currentTslItem.dataType.type)">
              <el-input v-model="currentTslItem.dataType.specs.unit" placeholder="单位，如：℃"/>
            </el-form-item>
            <el-form-item label="数据长度" v-if="currentTslItem.dataType.type === 'text'">
              <el-input v-model="currentTslItem.dataType.specs.length" placeholder="最大长度，默认10240"/>
            </el-form-item>
            <el-form-item label="布尔值" v-if="currentTslItem.dataType.type === 'bool'">
              <div class="flex flex-col">
                <div class="flex items-center mb-2">
                  <span class="w-10">0:</span>
                  <el-input v-model="currentTslItem.dataType.specs['0']" placeholder="例如：关"/>
                </div>
                <div class="flex items-center">
                  <span class="w-10">1:</span>
                  <el-input v-model="currentTslItem.dataType.specs['1']" placeholder="例如：开"/>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="枚举项" v-if="currentTslItem.dataType.type === 'enum'">
              <el-input type="textarea" v-model="currentTslItem._enumJson"
                        placeholder='例如: {"0": "未激活", "1": "正常"}' rows="3"/>
              <div class="text-xs text-gray-400">请输入JSON格式的枚举键值对</div>
            </el-form-item>
          </template>

          <el-form-item label="读写类型" prop="accessMode">
            <el-radio-group v-model="currentTslItem.accessMode">
              <el-radio label="r">只读</el-radio>
              <el-radio label="rw">读写</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- Service Specific -->
        <template v-if="tslDialog.type === 'service'">
          <el-form-item label="调用方式" prop="callType">
            <el-radio-group v-model="currentTslItem.callType">
              <el-radio label="sync">同步</el-radio>
              <el-radio label="async">异步</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- Event Specific -->
        <template v-if="tslDialog.type === 'event'">
          <el-form-item label="事件类型" prop="type">
            <el-select v-model="currentTslItem.type">
              <el-option label="信息 (Info)" value="info"/>
              <el-option label="告警 (Alert)" value="alert"/>
              <el-option label="故障 (Error)" value="error"/>
            </el-select>
          </el-form-item>
        </template>

        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="currentTslItem.description"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitTslItem">确 定</el-button>
          <el-button @click="tslDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Product">
import {addProduct, delProduct, getProduct, listProduct, updateProduct} from "@/api/product/product"
import {addProductTsl, getProductTsl, updateProductTsl} from "@/api/product/productTsl"
import {listDevice} from "@/api/device/device"

const { proxy } = getCurrentInstance()

const productList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
// 物模型相关变量
const thingModelOpen = ref(false)
const thingModelTitle = ref("")
const thingModelForm = reactive({
  productKey: '',
  productId: '',
  thingModelContent: '',
  isNewModel: false // 新增标志位，表示是否是新模型
})

// 详情页相关变量
const showDetail = ref(false)
const currentProduct = ref({})
const deviceCount = ref(0)
const activeTab = ref('config')
const thingModelData = reactive({
  properties: [],
  services: [],
  events: []
})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    productKey: null,
    name: null,
    nodeType: null,
  },
  rules: {
    productKey: [
      { required: true, message: "产品密钥不能为空", trigger: "blur" }
    ],
    name: [
      { required: true, message: "产品名称不能为空", trigger: "blur" }
    ],
    nodeType: [
      { required: true, message: "设备类型不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查看产品详情 */
function handleDetail(row) {
  currentProduct.value = row
  showDetail.value = true
  activeTab.value = 'config'
  deviceCount.value = 0 // 重置计数

  // 获取设备数量
  listDevice({productId: row.id}).then(res => {
    deviceCount.value = res.total
  })

  // 获取并解析物模型
  getProductTsl(row.id).then(res => {
    if (res.data && res.data.tsl) {
      let tsl = res.data.tsl
      // 如果是字符串则尝试解析
      if (typeof tsl === 'string') {
        try {
          tsl = JSON.parse(tsl)
        } catch (e) {
          console.error("Failed to parse TSL", e)
          tsl = {}
        }
      }

      thingModelData.properties = tsl.properties || []
      thingModelData.services = tsl.services || []
      thingModelData.events = tsl.events || []
    } else {
      thingModelData.properties = []
      thingModelData.services = []
      thingModelData.events = []
    }
  })
}

/** 返回列表 */
function handleBack() {
  showDetail.value = false
  currentProduct.value = {}
  deviceCount.value = 0
}

/** 查询产品管理列表 */
function getList() {
  loading.value = true
  listProduct(queryParams.value).then(response => {
    productList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    productKey: null,
    name: null,
    nodeType: null,
    remarks: null
  }
  proxy.resetForm("productRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加产品管理"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getProduct(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改产品管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["productRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除产品管理编号为"' + _ids + '"的数据项？').then(function() {
    return delProduct(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('product/product/export', {
    ...queryParams.value
  }, `product_${new Date().getTime()}.xlsx`)
}

/** 物模型设置按钮操作 */
function handleThingModel(row) {
  resetThingModel()
  thingModelForm.productId = row.id
  thingModelForm.productKey = row.productKey
  thingModelTitle.value = "设置物模型 - " + row.name
  
  // 获取现有物模型数据
  getProductThingModel(row.id).then(response => {
    // 保存是否是新模型的标志
    thingModelForm.isNewModel = response.data?.isNewModel || false
    
    if (response.data && response.data.thingModel) {
      thingModelForm.thingModelContent = JSON.stringify(response.data.thingModel, null, 2)
    } else {
      // 设置默认物模型模板
      thingModelForm.thingModelContent = JSON.stringify({
          "properties": [
            {
              "identifier": "temperature",
              "name": "温度",
              "dataType": {
                "type": "float"
              },
              "unit": "℃",
              "description": "设备温度"
            },
            {
              "identifier": "humidity",
              "name": "湿度",
              "dataType": {
                "type": "float"
              },
              "unit": "%",
              "description": "环境湿度"
            }
          ],
          "events": [
            {
              "identifier": "overheat",
              "name": "过热告警",
              "description": "设备温度超过阈值告警"
            }
          ]
      }, null, 2)
    }
    thingModelOpen.value = true
  }).catch(() => {
    // 如果获取失败，显示默认模板并标记为新模型
    thingModelForm.thingModelContent = JSON.stringify({
      "properties": [],
      "events": []
    }, null, 2)
    thingModelForm.isNewModel = true
    thingModelOpen.value = true
  })
}

/** 保存物模型 */
function submitThingModel() {
  // 验证JSON格式
  try {
    const thingModel = JSON.parse(thingModelForm.thingModelContent)
    
    // 构造提交数据
    const submitData = {
      productId: thingModelForm.productId,
      productKey: thingModelForm.productKey,
      thingModel: thingModelForm.thingModelContent,
      isNewModel: thingModelForm.isNewModel // 传递是否是新模型的标志
    }
    
    // 调用保存物模型接口
    saveProductThingModel(submitData).then(response => {
      proxy.$modal.msgSuccess("物模型保存成功")
      thingModelOpen.value = false
    }).catch(error => {
      proxy.$modal.msgError("物模型保存失败: " + (error.message || '未知错误'))
    })
  } catch (e) {
    proxy.$modal.msgError("物模型格式不正确，请检查JSON格式")
  }
}

/** 取消物模型编辑 */
function cancelThingModel() {
  thingModelOpen.value = false
  resetThingModel()
}

/** 重置物模型表单 */
function resetThingModel() {
  thingModelForm.productKey = ''
  thingModelForm.productId = ''
  thingModelForm.thingModelContent = ''
  thingModelForm.isNewModel = false // 重置新模型标志
}

// 将本地函数替换为从API导入的函数
const getProductThingModel = (productId) => {
  return getProductTsl(productId).then(response => {
    const hasExistingModel = response.data && response.data.tsl !== null && response.data.tsl !== undefined
    let tsl = response.data?.tsl || {}
    if (typeof tsl === 'string') {
      try {
        tsl = JSON.parse(tsl)
      } catch (e) {
        tsl = {}
      }
    }
    return {
      data: {
        thingModel: tsl,
        isNewModel: !hasExistingModel // 添加标志位表示是否是新模型
      }
    }
  })
}

const saveProductThingModel = (data) => {
  const productTsl = {
    productId: data.productId,
    tsl: data.thingModel
  }
  // 判断是否是新增场景（通过判断 data 中是否包含 existingModel 标志）
  if (data.isNewModel) {
    return addProductTsl(productTsl)
  } else {
    return updateProductTsl(productTsl)
  }
}

// TSL Editing Logic
const tslDialog = reactive({
  visible: false,
  title: '',
  type: '', // 'property', 'service', 'event'
  isEdit: false,
  editIndex: -1
})

const currentTslItem = ref({})

const tslItemRules = {
  identifier: [
    {required: true, message: "标识符不能为空", trigger: "blur"},
    {pattern: /^[a-zA-Z0-9_]+$/, message: "标识符只能包含字母、数字和下划线", trigger: "blur"}
  ],
  name: [{required: true, message: "名称不能为空", trigger: "blur"}],
  'dataType.type': [{required: true, message: "数据类型不能为空", trigger: "change"}]
}

function openTslDialog(type) {
  tslDialog.type = type
  tslDialog.isEdit = false
  tslDialog.editIndex = -1
  tslDialog.visible = true

  if (type === 'property') {
    tslDialog.title = '添加属性'
    currentTslItem.value = {
      identifier: '',
      name: '',
      accessMode: 'rw',
      dataType: {
        type: 'int',
        specs: {}
      },
      description: '',
      _enumJson: ''
    }
  } else if (type === 'service') {
    tslDialog.title = '添加功能'
    currentTslItem.value = {
      identifier: '',
      name: '',
      callType: 'sync',
      inputData: [],
      outputData: [],
      description: ''
    }
  } else if (type === 'event') {
    tslDialog.title = '添加事件'
    currentTslItem.value = {
      identifier: '',
      name: '',
      type: 'info',
      outputData: [],
      description: ''
    }
  }
}

function handleEditTslItem(type, row, index) {
  tslDialog.type = type
  tslDialog.isEdit = true
  tslDialog.editIndex = index
  tslDialog.visible = true

  // Deep copy
  currentTslItem.value = JSON.parse(JSON.stringify(row))

  // Handle enum JSON for display
  if (type === 'property' && currentTslItem.value.dataType?.type === 'enum') {
    currentTslItem.value._enumJson = JSON.stringify(currentTslItem.value.dataType.specs || {}, null, 2)
  }

  if (type === 'property') {
    tslDialog.title = '修改属性'
    if (!currentTslItem.value.dataType) {
      currentTslItem.value.dataType = {type: 'int', specs: {}}
    }
    if (!currentTslItem.value.dataType.specs) {
      currentTslItem.value.dataType.specs = {}
    }
  } else if (type === 'service') {
    tslDialog.title = '修改功能'
  } else if (type === 'event') {
    tslDialog.title = '修改事件'
  }
}

function handleDeleteTslItem(type, index) {
  proxy.$modal.confirm('确认删除该项吗？').then(() => {
    if (type === 'property') {
      thingModelData.properties.splice(index, 1)
    } else if (type === 'service') {
      thingModelData.services.splice(index, 1)
    } else if (type === 'event') {
      thingModelData.events.splice(index, 1)
    }
    proxy.$modal.msgSuccess("删除成功，请记得保存物模型")
  })
}

function submitTslItem() {
  proxy.$refs["tslItemRef"].validate(valid => {
    if (valid) {
      // Handle Enum JSON parsing
      if (tslDialog.type === 'property' && currentTslItem.value.dataType.type === 'enum') {
        try {
          const specs = JSON.parse(currentTslItem.value._enumJson || '{}')
          currentTslItem.value.dataType.specs = specs
        } catch (e) {
          proxy.$modal.msgError("枚举项JSON格式错误")
          return
        }
      }

      // Clean up temporary fields
      delete currentTslItem.value._enumJson

      const item = JSON.parse(JSON.stringify(currentTslItem.value))

      if (tslDialog.type === 'property') {
        if (tslDialog.isEdit) {
          thingModelData.properties.splice(tslDialog.editIndex, 1, item)
        } else {
          // Check duplicate identifier
          if (thingModelData.properties.some(p => p.identifier === item.identifier)) {
            proxy.$modal.msgError("标识符已存在")
            return
          }
          thingModelData.properties.push(item)
        }
      } else if (tslDialog.type === 'service') {
        if (tslDialog.isEdit) {
          thingModelData.services.splice(tslDialog.editIndex, 1, item)
        } else {
          if (thingModelData.services.some(s => s.identifier === item.identifier)) {
            proxy.$modal.msgError("标识符已存在")
            return
          }
          thingModelData.services.push(item)
        }
      } else if (tslDialog.type === 'event') {
        if (tslDialog.isEdit) {
          thingModelData.events.splice(tslDialog.editIndex, 1, item)
        } else {
          if (thingModelData.events.some(e => e.identifier === item.identifier)) {
            proxy.$modal.msgError("标识符已存在")
            return
          }
          thingModelData.events.push(item)
        }
      }

      tslDialog.visible = false
      proxy.$modal.msgSuccess(tslDialog.isEdit ? "修改成功" : "添加成功")
    }
  })
}

function saveTslToBackend() {
  const tslContent = {
    properties: thingModelData.properties,
    services: thingModelData.services,
    events: thingModelData.events
  }

  const submitData = {
    productId: currentProduct.value.id,
    productKey: currentProduct.value.productKey,
    thingModel: JSON.stringify(tslContent), // Backend expects String
    isNewModel: false // Assuming we are updating
  }

  saveProductThingModel(submitData).then(response => {
    proxy.$modal.msgSuccess("物模型保存成功")
  }).catch(error => {
    proxy.$modal.msgError("保存失败: " + error.message)
  })
}

getList()
</script>

<style scoped>
.product-detail {
  padding: 20px;
  background-color: #fff;
}

.detail-header {
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
}

.detail-info {
  font-size: 14px;
  color: #606266;
}

.info-label {
  font-weight: bold;
}

.info-value {
  margin-left: 8px;
}

.detail-tabs {
  min-height: 400px;
}
</style>
