<template>
  <el-tree-select
      v-model="internalValue"
      :data="treeData"
      :props="defaultProps"
      :multiple="multiple"
      :render-after-expand="false"
      show-checkbox
      check-strictly
      node-key="uniqueId"
      check-on-click-node
      filterable
      clearable
      placeholder="请选择产品或设备"
      class="w-full"
      @check-change="handleCheckChange"
  >
    <template #default="{ node, data }">
      <div class="custom-tree-node">
        <span>{{ node.label }}</span>
        <el-tag v-if="data.type === 'product'" size="small" type="info" class="ml-2">产品</el-tag>
        <el-tag v-else size="small" type="success" class="ml-2">设备</el-tag>
      </div>
    </template>
  </el-tree-select>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {listProduct} from '@/api/product/product'
import {listDevice} from '@/api/device/device'

const props = defineProps({
  modelValue: {type: [String, Number, Array], default: () => []},
  multiple: {type: Boolean, default: false}
})
const emit = defineEmits(['update:modelValue', 'change', 'select-product'])

const treeData = ref([])
const productMap = ref({}) // id -> product
const deviceMap = ref({}) // id -> device
const isDataLoaded = ref(false)

const defaultProps = {
  children: 'children',
  label: 'name',
  value: 'uniqueId' // Use uniqueId as value to avoid collision
}

const internalValue = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loadData = async () => {
  try {
    const [pRes, dRes] = await Promise.all([
      listProduct({pageSize: 1000}),
      listDevice({pageSize: 1000})
    ])

    const products = pRes.rows || []
    const devices = dRes.rows || []

    const pNodes = []
    const newProductMap = {}
    const newDeviceMap = {}

    products.forEach(p => {
      const pid = String(p.id)
      newProductMap[pid] = p
      pNodes.push({
        ...p,
        uniqueId: `product-${pid}`,
        type: 'product',
        children: []
      })
    })

    const pNodeMap = {}
    pNodes.forEach(n => pNodeMap[n.id] = n)

    devices.forEach(d => {
      const did = String(d.id)
      newDeviceMap[did] = d
      const pId = d.productId
      if (pNodeMap[pId]) {
        pNodeMap[pId].children.push({
          ...d,
          uniqueId: `device-${did}`,
          type: 'device',
          leaf: true
        })
      }
    })

    productMap.value = newProductMap
    deviceMap.value = newDeviceMap
    treeData.value = pNodes
    isDataLoaded.value = true

    // 数据加载后，如果 modelValue 已有值，触发一次 change 以更新父组件状态（如回显名称）
    if (props.modelValue && (Array.isArray(props.modelValue) ? props.modelValue.length > 0 : props.modelValue)) {
      triggerChange(props.modelValue)
    }
  } catch (e) {
    console.error('Failed to load selector data', e)
  }
}

const triggerChange = (val) => {
  if (!isDataLoaded.value) return // Data not loaded yet

  const selectedIds = Array.isArray(val) ? val : [val].filter(Boolean)
  const selectedObjects = selectedIds.map(uid => {
    if (typeof uid !== 'string') return null
    if (uid.startsWith('product-')) {
      const id = uid.replace('product-', '')
      const p = productMap.value[id]
      return p ? {...p, type: 'product', uniqueId: uid, id: id} : null
    } else if (uid.startsWith('device-')) {
      const id = uid.replace('device-', '')
      const d = deviceMap.value[id]
      return d ? {...d, type: 'device', uniqueId: uid, id: id} : null
    }
    return null
  }).filter(Boolean)

  emit('change', selectedObjects)
}

// Watch internalValue for changes triggered by the component itself (e.g. selection)
watch(
    () => props.modelValue,
    (val) => {
      triggerChange(val)
    }
)

const handleCheckChange = (data, checked) => {
  if (checked) {
    // If selecting a node, we might want to emit details
    if (data.type === 'product') {
      emit('select-product', data)
    } else if (data.type === 'device') {
      // Find parent product
      const p = productMap.value[String(data.productId)]
      if (p) emit('select-product', p)
    }
  }
}

// Remove redundant watch on internalValue since we watch props.modelValue now
// watch(internalValue, (val) => { ... }) removed

onMounted(() => {
  loadData()
})

// Expose capability to get full object
defineExpose({
  getProduct: (id) => productMap.value[id],
  getDevice: (id) => deviceMap.value[id]
})
</script>

<style scoped>
.w-full {
  width: 100%;
}

.ml-2 {
  margin-left: 8px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}
</style>
