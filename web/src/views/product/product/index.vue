<template>
  <div class="app-container">
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
      <el-table-column label="产品名称" align="center" prop="name" />
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
  </div>
</template>

<script setup name="Product">
import { listProduct, getProduct, delProduct, addProduct, updateProduct } from "@/api/product/product"
import { getProductTsl, updateProductTsl, addProductTsl } from "@/api/product/productTsl"

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
    return {
      data: {
        thingModel: JSON.parse(response.data?.tsl || '{}', 2, null),
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

getList()
</script>
