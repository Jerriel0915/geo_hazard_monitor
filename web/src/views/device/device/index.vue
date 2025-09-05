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

    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="数据ID" align="center" prop="id" />
      <el-table-column label="设备编号" align="center" prop="sn" />
      <el-table-column label="设备名称" align="center" prop="name" />
      <el-table-column label="设备类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="dev_type" :value="scope.row.type"/>
        </template>
      </el-table-column>
      <el-table-column label="所属产品id" align="center" prop="productId" />
      <el-table-column label="通信协议" align="center" prop="commProtocol" />
      <el-table-column label="经度" align="center" prop="longitude" />
      <el-table-column label="纬度" align="center" prop="latitude" />
      <el-table-column label="供电方式" align="center" prop="powerSupply" />
      <el-table-column label="生产厂商" align="center" prop="manufacturer" />
      <el-table-column label="厂商电话" align="center" prop="suppierTel" />
      <el-table-column label="安装日期" align="center" prop="installData" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.installData, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="质保日期" align="center" prop="warrantyEnd" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.warrantyEnd, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="维保人姓名" align="center" prop="maintenanceName" />
      <el-table-column label="维保人电话" align="center" prop="maintenanceTel" />
      <el-table-column label="巡检频率" align="center" prop="inspectionCycle" />
      <el-table-column label="父设备id" align="center" prop="parentId" />
      <el-table-column label="接入地址" align="center" prop="gatewayIp" />
      <el-table-column label="端口号" align="center" prop="gatewayPort" />
      <el-table-column label="账号" align="center" prop="user" />
      <el-table-column label="密码" align="center" prop="password" />
      <el-table-column label="安装位置" align="center" prop="location" />
      <el-table-column label="设备安装附件" align="center" prop="installAttach" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['device:device:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['device:device:remove']">删除</el-button>
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

    <!-- 添加或修改设备基本信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="deviceRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="设备编号" prop="sn">
          <el-input v-model="form.sn" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称" />
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
        <el-form-item label="所属产品id" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入所属产品id" />
        </el-form-item>
        <el-form-item label="通信协议" prop="commProtocol">
          <el-input v-model="form.commProtocol" placeholder="请输入通信协议" />
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input v-model="form.longitude" placeholder="请输入经度" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input v-model="form.latitude" placeholder="请输入纬度" />
        </el-form-item>
        <el-form-item label="供电方式" prop="powerSupply">
          <el-input v-model="form.powerSupply" placeholder="请输入供电方式" />
        </el-form-item>
        <el-form-item label="生产厂商" prop="manufacturer">
          <el-input v-model="form.manufacturer" placeholder="请输入生产厂商" />
        </el-form-item>
        <el-form-item label="厂商电话" prop="suppierTel">
          <el-input v-model="form.suppierTel" placeholder="请输入厂商电话" />
        </el-form-item>
        <el-form-item label="安装日期" prop="installData">
          <el-date-picker clearable
            v-model="form.installData"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择安装日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="质保日期" prop="warrantyEnd">
          <el-date-picker clearable
            v-model="form.warrantyEnd"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择质保日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="维保人姓名" prop="maintenanceName">
          <el-input v-model="form.maintenanceName" placeholder="请输入维保人姓名" />
        </el-form-item>
        <el-form-item label="维保人电话" prop="maintenanceTel">
          <el-input v-model="form.maintenanceTel" placeholder="请输入维保人电话" />
        </el-form-item>
        <el-form-item label="巡检频率" prop="inspectionCycle">
          <el-input v-model="form.inspectionCycle" placeholder="请输入巡检频率" />
        </el-form-item>
        <el-form-item label="父设备id" prop="parentId">
          <el-input v-model="form.parentId" placeholder="请输入父设备id" />
        </el-form-item>
        <el-form-item label="接入地址" prop="gatewayIp">
          <el-input v-model="form.gatewayIp" placeholder="请输入接入地址" />
        </el-form-item>
        <el-form-item label="端口号" prop="gatewayPort">
          <el-input v-model="form.gatewayPort" placeholder="请输入端口号" />
        </el-form-item>
        <el-form-item label="账号" prop="user">
          <el-input v-model="form.user" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="安装位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入安装位置" />
        </el-form-item>
        <el-form-item label="设备安装附件" prop="installAttach">
          <el-input v-model="form.installAttach" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Device">
import { listDevice, getDevice, delDevice, addDevice, updateDevice } from "@/api/device/device"

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
    longitude: [
      { required: true, message: "经度不能为空", trigger: "blur" }
    ],
    latitude: [
      { required: true, message: "纬度不能为空", trigger: "blur" }
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
    commProtocol: null,
    longitude: null,
    latitude: null,
    powerSupply: null,
    manufacturer: null,
    suppierTel: null,
    installData: null,
    warrantyEnd: null,
    maintenanceName: null,
    maintenanceTel: null,
    inspectionCycle: null,
    parentId: null,
    gatewayIp: null,
    gatewayPort: null,
    user: null,
    password: null,
    location: null,
    installAttach: null
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

getList()
</script>
