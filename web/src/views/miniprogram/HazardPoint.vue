<template>
  <div class="page-content">
    <div class="page-title">隐患点</div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="隐患点名称" clearable style="width: 200px"
                @keyup.enter="handleSearch"/>
      <el-select v-model="searchGroupId" placeholder="分组" clearable style="width: 160px; margin-left: 10px">
        <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id"/>
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="handleSearch">查询</el-button>
    </div>

    <!-- 隐患点列表 -->
    <el-table :data="list" v-loading="loading" border stripe style="margin-top: 16px">
      <el-table-column prop="code" label="编号" width="150" align="center"/>
      <el-table-column prop="name" label="名称" min-width="160"/>
      <el-table-column prop="groupName" label="分组" width="120" align="center"/>
      <el-table-column prop="statusName" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deviceCount" label="设备数" width="100" align="center"/>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="openDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadList"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
        v-model="detailVisible"
        :title="`隐患点详情 - ${currentRow?.name || ''}`"
        width="900px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-tabs v-model="detailTab" @tab-change="onTabChange">
        <!-- 已绑设备 -->
        <el-tab-pane label="已绑设备" name="devices">
          <div v-loading="deviceLoading">
            <div v-if="boundDevices.length === 0" class="empty-hint">暂无已绑定设备</div>
            <div v-for="dev in boundDevices" :key="dev.deviceId" class="device-card">
              <div class="device-header">
                <span class="device-name">{{ dev.deviceName }}</span>
                <el-tag :type="dev.deviceStatus === 1 ? 'success' : 'info'" size="small">
                  {{ dev.deviceStatus === 1 ? '在线' : '离线' }}
                </el-tag>
              </div>
              <div class="sensor-list">
                <el-tag v-for="s in dev.sensors" :key="s.id" size="small" type="warning" style="margin: 2px 4px">
                  {{ s.name }}
                </el-tag>
                <span v-if="!dev.sensors || dev.sensors.length === 0" class="no-sensor">暂无传感器</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 监测数据 -->
        <el-tab-pane label="监测数据" name="monitor">
          <MonitorDataExplorer
            v-if="currentRow"
            :hazard-point-id="Number(currentRow.id)"
            :hazard-point-name="currentRow.name"
          />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {getBoundDevices, getHazardPointGroups, getHazardPointPage} from '@/api/hazardPoint'
import MonitorDataExplorer from '@/components/MonitorDataExplorer.vue'

// ----- 列表 -----
const list = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const searchName = ref('')
const searchGroupId = ref<number | null>(null)
const groupList = ref<any[]>([])

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getHazardPointPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
      groupId: searchGroupId.value ?? undefined
    })
    list.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1;
  loadList()
}

const loadGroups = async () => {
  try {
    const res: any = await getHazardPointGroups()
    groupList.value = res.data || []
  } catch { /* ignore */
  }
}

// ----- 详情 -----
const detailVisible = ref(false)
const detailTab = ref('devices')
const currentRow = ref<any>(null)

// 设备
const boundDevices = ref<any[]>([])
const deviceLoading = ref(false)

// ----- 打开详情 -----
const openDetail = async (row: any) => {
  currentRow.value = row
  detailVisible.value = true
  detailTab.value = 'devices'
  await loadBoundDevices()
}

const loadBoundDevices = async () => {
  deviceLoading.value = true
  try {
    const res: any = await getBoundDevices(String(currentRow.value.id))
    boundDevices.value = (res.data || []).map((d: any) => ({
      ...d,
      sensors: d.sensors || []
    }))
  } finally {
    deviceLoading.value = false
  }
}

const onTabChange = (tab: string) => {
  if (tab === 'devices') loadBoundDevices()
}

// ----- 生命周期 -----
onMounted(() => {
  loadGroups()
  loadList()
})
</script>

<style scoped>
.page-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: calc(100% - 32px);
}
.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.search-bar {
  display: flex;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  padding: 40px;
  font-size: 14px;
}

.no-sensor {
  color: #c0c4cc;
  font-size: 12px;
}

.device-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.device-name {
  font-weight: 600;
  font-size: 15px;
}

.sensor-list {
  display: flex;
  flex-wrap: wrap;
}

</style>
