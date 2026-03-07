<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="设备名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入设备名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择设备类型" clearable>
          <el-option
            v-for="dict in dev_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属产品id" prop="productId">
        <el-input
          v-model="queryParams.productId"
          placeholder="请输入所属产品id"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['device:device:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['device:device:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['device:device:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['device:device:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-row :gutter="20" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" v-for="item in deviceList" :key="item.id" class="mb-4">
        <el-card shadow="hover" class="device-card">
          <template #header>
            <div class="card-header">
              <span class="device-name">{{ item.name }}</span>
              <dict-tag :options="dev_type" :value="item.type" size="small"/>
            </div>
          </template>
          <div class="card-content">
            <div class="info-item">
              <span class="label">设备编号：</span>
              <span class="value">{{ item.sn }}</span>
            </div>
            <div class="info-item">
              <span class="label">产品ID：</span>
              <span class="value">{{ item.productId }}</span>
            </div>
            <div class="info-item">
              <span class="label">通信协议：</span>
              <span class="value">{{ item.commProtocol || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">生产厂商：</span>
              <span class="value">{{ item.manufacturer || '-' }}</span>
            </div>
          </div>
          <div class="card-footer">
            <el-tooltip content="修改" placement="top">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(item)"
                         v-hasPermi="['device:device:edit']"></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" icon="Delete" @click="handleDelete(item)"
                         v-hasPermi="['device:device:remove']"></el-button>
            </el-tooltip>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改设备基本信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="deviceRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="设备编号" prop="sn">
          <el-input v-model="form.sn" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="所属产品" prop="productId">
          <el-input v-model="form.productName" placeholder="请选择所属产品" readonly @click="openProductSelect">
            <template #append>
              <el-button icon="Search" @click="openProductSelect"/>
            </template>
          </el-input>
          <!-- Hidden input for productId -->
          <el-input v-model="form.productId" type="hidden" style="display:none"/>
        </el-form-item>
        <el-form-item label="设备类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择设备类型">
            <el-option
              v-for="dict in dev_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="通信协议" prop="commProtocol">
          <el-input v-model="form.commProtocol" placeholder="请输入通信协议" />
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input v-model.number="form.longitude" placeholder="请输入经度（数值类型）" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input v-model.number="form.latitude" placeholder="请输入纬度（数值类型）" />
        </el-form-item>
        <el-form-item label="供电方式" prop="powerSupply">
          <el-input v-model="form.powerSupply" placeholder="请输入供电方式" />
        </el-form-item>
        <el-form-item label="生产厂商" prop="manufacturer">
          <el-input v-model="form.manufacturer" placeholder="请输入生产厂商" />
        </el-form-item>
        <el-form-item label="设备密钥" prop="deviceKey">
          <el-input v-model="form.deviceKey" placeholder="请输入设备密钥" />
        </el-form-item>
        <el-form-item label="设备密码" prop="deviceSecret">
          <el-input v-model="form.deviceSecret" placeholder="请输入设备密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <ProductSelect v-model="productSelectVisible" @confirm="handleProductSelect"/>
  </div>
</template>

<script setup name="Device">
import {addDevice, delDevice, getDevice, listDevice, updateDevice} from "@/api/device/device"
import {getProduct} from "@/api/product/product"
import ProductSelect from "@/components/ProductSelect/index.vue"

const { proxy } = getCurrentInstance()
const { dev_type } = proxy.useDict('dev_type')

const deviceList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const productSelectVisible = ref(false)

const data = reactive({
    form: {},
    queryParams: {
      pageNum: 1,
      pageSize: 10,
      name: null,
      type: null,
      productId: null,
    },
    rules: {
      sn: [
        { required: true, message: "设备编号不能为空", trigger: "blur" }
      ],
      name: [
        { required: true, message: "设备名称不能为空", trigger: "blur" }
      ],
      type: [
        { required: true, message: "设备类型不能为空", trigger: "change" }
      ],
      productId: [
        {required: true, message: "所属产品不能为空", trigger: "change"}
      ],
      longitude: [
        { required: true, message: "经度不能为空", trigger: "blur" },
        { type: 'number', message: "经度必须为数字类型", trigger: "blur" }
      ],
      latitude: [
        { required: true, message: "纬度不能为空", trigger: "blur" },
        { type: 'number', message: "纬度必须为数字类型", trigger: "blur" }
      ],
    }
  })

const { queryParams, form, rules } = toRefs(data)

/** 查询设备基本信息列表 */
function getList() {
  loading.value = true
  listDevice(queryParams.value).then(response => {
    deviceList.value = response.rows
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
    sn: null,
    name: null,
    type: null,
    productId: null,
    productName: null,
    commProtocol: null,
    longitude: null,
    latitude: null,
    powerSupply: null,
    manufacturer: null,
    deviceKey: null,
    deviceSecret: null
  }
  proxy.resetForm("deviceRef")
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
  title.value = "添加设备基本信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getDevice(_id).then(response => {
    form.value = response.data
    if (form.value.productId) {
      getProduct(form.value.productId).then(res => {
        form.value.productName = res.data.name
      })
    }
    open.value = true
    title.value = "修改设备基本信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addDevice(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除设备基本信息编号为"' + _ids + '"的数据项？').then(function() {
    return delDevice(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('device/device/export', {
    ...queryParams.value
  }, `device_${new Date().getTime()}.xlsx`)
}

function openProductSelect() {
  productSelectVisible.value = true
}

function handleProductSelect(product) {
  form.value.productId = product.id
  form.value.productName = product.name
  // Auto-fill other fields based on product
  if (product.nodeType !== undefined) {
    form.value.type = product.nodeType.toString() // Ensure type consistency (string/number)
  }
  // Fill manufacturer or other info if available in product

  // Validate productId field to clear any error
  proxy.$refs["deviceRef"].validateField("productId")
}

getList()
</script>

<style scoped>
.device-card {
  transition: all 0.3s;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.device-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-name {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.card-content {
  flex: 1;
  margin: 15px 0;
}

.info-item {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: flex-start;
}

.info-item .label {
  width: 80px;
  flex-shrink: 0;
  color: #909399;
}

.info-item .value {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  border-top: 1px solid #EBEEF5;
  padding-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
