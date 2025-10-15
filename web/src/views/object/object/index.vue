<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="对象名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入对象名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="对象编号" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入对象编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="对象类型" prop="type">
        <el-select v-model="queryParams.type" style="width: 120px" placeholder="所有类型" clearable>
          <el-option
            v-for="dict in object_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
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
          v-hasPermi="['monitor:object:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['monitor:object:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['monitor:object:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['monitor:object:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="monitoringObjectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="数据ID" align="center" prop="id" />
      <el-table-column label="对象编号" align="center" prop="code" />
      <el-table-column label="对象名称" align="center" prop="name" />
      <el-table-column label="对象类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="object_type" :value="scope.row.type"/>
        </template>
      </el-table-column>
      <el-table-column label="对象地址" align="center" prop="address" />
      <el-table-column label="经度" align="center" prop="centerLng" />
      <el-table-column label="纬度" align="center" prop="centerLat" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['monitor:object:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['monitor:object:remove']">删除</el-button>
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

    <!-- 添加或修改监测对象对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="monitoringObjectRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="对象编号" prop="code">
          <el-input v-model="form.code" placeholder="请输入对象编号" />
        </el-form-item>
        <el-form-item label="对象名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入对象名称" />
        </el-form-item>
        <el-form-item label="对象类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择对象类型">
            <el-option
              v-for="dict in object_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="对象地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入对象地址" />
        </el-form-item>
        <el-form-item label="经度" prop="centerLng">
          <el-input-number
            v-model="form.centerLng"
            :min="-180"
            :max="180"
            :step="0.000000001"
            placeholder="请输入经度"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="纬度" prop="centerLat">
          <el-input-number
            v-model="form.centerLat"
            :min="-90"
            :max="90"
            :step="0.000000001"
            placeholder="请输入纬度"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注信息" type="textarea" :rows="2" />
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

<script setup name="MonitoringObject">
import { listMonitoringObject, getMonitoringObject, delMonitoringObject, addMonitoringObject, updateMonitoringObject } from "@/api/monitor/object"

const { proxy } = getCurrentInstance()
const { object_type } = proxy.useDict('object_type')

const monitoringObjectList = ref([])
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
      code: null,
      type: null,
    },
    rules: {
      code: [
        { required: true, message: "对象编号不能为空", trigger: "blur" }
      ],
      name: [
        { required: true, message: "对象名称不能为空", trigger: "blur" }
      ],
      type: [
        { required: true, message: "对象类型不能为空", trigger: "change" }
      ],
      centerLng: [
        { required: true, message: "经度不能为空", trigger: "blur" },
        { type: 'number', message: "经度必须为数字类型", trigger: "blur" }
      ],
      centerLat: [
        { required: true, message: "纬度不能为空", trigger: "blur" },
        { type: 'number', message: "纬度必须为数字类型", trigger: "blur" }
      ],
    }
  })

const { queryParams, form, rules } = toRefs(data)

/** 查询监测对象列表 */
function getList() {
  loading.value = true
  listMonitoringObject(queryParams.value).then(response => {
    monitoringObjectList.value = response.rows
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
    code: null,
    name: null,
    type: null,
    address: null,
    centerLng: null,
    centerLat: null,
    remark: null
  }
  proxy.resetForm("monitoringObjectRef")
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
  title.value = "添加监测对象"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getMonitoringObject(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改监测对象"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["monitoringObjectRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateMonitoringObject(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMonitoringObject(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除监测对象编号为"' + _ids + '"的数据项？').then(function() {
    return delMonitoringObject(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('monitor/object/export', {
    ...queryParams.value
  }, `monitoring_object_${new Date().getTime()}.xlsx`)
}

getList()
</script>