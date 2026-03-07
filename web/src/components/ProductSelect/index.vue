<template>
  <el-dialog
      title="选择产品"
      v-model="visible"
      width="800px"
      top="5vh"
      append-to-body
      @close="handleClose"
  >
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
      <el-form-item label="产品名称" prop="name">
        <el-input
            v-model="queryParams.name"
            placeholder="请输入产品名称"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品密钥" prop="productKey">
        <el-input
            v-model="queryParams.productKey"
            placeholder="请输入产品密钥"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
        v-loading="loading"
        :data="productList"
        @current-change="handleCurrentChange"
        highlight-current-row
        border
    >
      <el-table-column label="产品ID" align="center" prop="id" width="80"/>
      <el-table-column label="产品名称" align="center" prop="name"/>
      <el-table-column label="产品密钥" align="center" prop="productKey"/>
      <el-table-column label="设备类型" align="center" prop="nodeType">
        <template #default="scope">
          <el-tag v-if="scope.row.nodeType === 0">直连设备</el-tag>
          <el-tag v-else-if="scope.row.nodeType === 1" type="success">网关</el-tag>
          <el-tag v-else-if="scope.row.nodeType === 2" type="warning">传感器</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="remarks" show-overflow-tooltip/>
    </el-table>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect" :disabled="!selectedProduct">确 定</el-button>
        <el-button @click="handleClose">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {listProduct} from "@/api/product/product";
import {computed, defineEmits, defineProps, reactive, ref, toRefs, watch} from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'confirm']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(true);
const total = ref(0);
const productList = ref([]);
const selectedProduct = ref(null);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    productKey: undefined
  }
});

const {queryParams} = toRefs(data);

watch(() => props.modelValue, (val) => {
  if (val) {
    getList();
    selectedProduct.value = null;
  }
});

function getList() {
  loading.value = true;
  listProduct(queryParams.value).then(response => {
    productList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.value.name = undefined;
  queryParams.value.productKey = undefined;
  handleQuery();
}

function handleCurrentChange(val) {
  selectedProduct.value = val;
}

function confirmSelect() {
  if (selectedProduct.value) {
    emit('confirm', selectedProduct.value);
    visible.value = false;
  }
}

function handleClose() {
  visible.value = false;
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>
