<!-- src/pages/chart.vue - 监测数据 -->
<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <PageHeader title="监测数据" subtitle="多设备数据可视化分析" />

    <!-- 主体 -->
    <view class="page-body">
      <!-- 筛选胶囊卡片：已选设备 + 时间 Tab -->
      <view class="filter-capsule-card">
        <!-- 已选设备标签行 -->
        <scroll-view scroll-x class="tags-scroll">
          <view class="tags-container">
            <view
              v-for="device in selectedDevices"
              :key="device.id"
              class="device-tag"
            >
              <text class="tag-text">{{ device.name || device.deviceName }}</text>
              <text class="tag-close" @click="removeDevice(device.id)">&times;</text>
            </view>
            <view v-if="selectedDevices.length === 0" class="hint-tag">
              <text class="hint-text">点击右下角按钮添加设备</text>
            </view>
          </view>
        </scroll-view>
        <!-- 时间 Tab 行 -->
        <view class="time-capsule-row">
          <view
            v-for="tab in timeTabs"
            :key="tab.value"
            class="capsule-item"
            :class="{ active: activeTimeTab === tab.value }"
            @click="changeTimeTab(tab.value)"
          >
            <text class="capsule-text">{{ tab.label }}</text>
          </view>
        </view>
      </view>

      <!-- 图表滚动区 -->
      <scroll-view class="charts-scroll" scroll-y>
        <view class="charts-container">
          <view v-if="loading && chartGroups.length === 0" class="loading-wrapper">
            <text>加载中...</text>
          </view>

          <template v-else>
            <view
              v-for="group in chartGroups"
              :key="group.deviceId"
              class="device-chart-group"
            >
              <view class="chart-card">
                <view class="card-header">
                  <view class="card-header-left">
                    <text class="group-title">{{ group.deviceName }}</text>
                    <text class="group-sub">{{ group.deviceType }}</text>
                  </view>
                </view>
                <MonitorChart
                  :key="`chart-${group.deviceId}`"
                  :series="group.series"
                  chartType="auto"
                  showLegend
                  height="500rpx"
                />
              </view>
            </view>

            <EmptyState
              v-if="selectedDevices.length === 0"
              :use-image="true"
              title="暂无监测数据"
              description="点击右下角按钮添加设备"
              actionText="添加设备"
              @action="openPicker"
            />
          </template>
        </view>

        <view class="bottom-spacer" />
      </scroll-view>
    </view>

    <!-- FAB 添加设备按钮 -->
    <view class="fab-btn" @click="openPicker">
      <zui-svg-icon icon="device" :width="24" color="#ffffff" />
      <view v-if="selectedDevices.length > 0" class="fab-badge">
        <text class="fab-badge-text">{{ selectedDevices.length }}</text>
      </view>
    </view>

    <!-- 设备选择抽屉 -->
    <view v-if="showDevicePicker" class="picker-mask" @click="showDevicePicker = false">
      <view class="picker-panel" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择设备</text>
          <text class="picker-close" @click="showDevicePicker = false">&times;</text>
        </view>

        <!-- 双列布局 -->
        <view class="picker-body">
          <!-- 左列：隐患点列表 -->
          <view class="picker-col hazard-col">
            <view class="picker-search-bar">
              <view class="picker-search-icon">
                <zui-svg-icon icon="search" :width="14" color="#9ca3af" />
              </view>
              <input
                class="picker-search-input"
                type="text"
                placeholder="搜索隐患点"
                placeholder-class="picker-search-placeholder"
                :value="hazardKeyword"
                @input="onHazardSearch"
              >
            </view>
            <scroll-view class="picker-col-list" scroll-y>
              <view
                v-for="h in filteredPickerHazards"
                :key="h.id"
                class="hazard-option"
                :class="{ active: pickerSelectedHazardId === h.id }"
                @click="selectPickerHazard(h)"
              >
                <text class="hazard-option-text">{{ h.name }}</text>
              </view>
              <view v-if="filteredPickerHazards.length === 0" class="picker-col-empty">
                <text class="picker-col-empty-text">无匹配隐患点</text>
              </view>
            </scroll-view>
          </view>

          <!-- 右列：设备列表 -->
          <view class="picker-col device-col">
            <view v-if="pickerSelectedHazardName" class="device-col-header">
              <text class="device-col-title">{{ pickerSelectedHazardName }}</text>
            </view>
            <scroll-view class="picker-col-list" scroll-y>
              <view v-if="pickerLoading" class="picker-col-empty">
                <text class="picker-col-empty-text">加载中...</text>
              </view>
              <template v-else>
                <view
                  v-for="device in pickerDevices"
                  :key="device.id"
                  class="device-option"
                  :class="{ selected: isSelected(device.id) }"
                  @click="toggleDevice(device)"
                >
                  <view class="device-option-info">
                    <text class="device-option-name">{{ device.name || device.deviceName }}</text>
                    <text class="device-option-code">{{ device.code || device.deviceCode || '-' }}</text>
                  </view>
                  <view v-if="isSelected(device.id)" class="device-option-check">
                    <zui-svg-icon icon="check" :width="18" color="#3068e4" />
                  </view>
                </view>
                <view v-if="pickerDevices.length === 0 && pickerSelectedHazardId" class="picker-col-empty">
                  <text class="picker-col-empty-text">该隐患点暂无设备</text>
                </view>
                <view v-if="!pickerSelectedHazardId" class="picker-col-empty">
                  <text class="picker-col-empty-text">请先选择隐患点</text>
                </view>
              </template>
            </scroll-view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import MonitorChart from '@/components/MonitorChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import { deviceApi } from '@/utils/device'
import type { DeviceInfo, DeviceSensor } from '@/utils/device'
import { hazardApi } from '@/utils/hazard'
import type { Hazard } from '@/utils/hazard'
import { monitorApi, calcGranularity } from '@/utils/monitor'
import type { ChartSeries } from '@/utils/monitor'

const timeTabs = [
  { label: '24小时', value: '24h' },
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' }
]

const loading = ref(false)
const allHazards = ref<Hazard[]>([])
const selectedDevices = ref<DeviceInfo[]>([])
const activeTimeTab = ref('24h')
const showDevicePicker = ref(false)

// === Picker 状态 ===
const hazardKeyword = ref('')
const pickerSelectedHazardId = ref(0)
const pickerSelectedHazardName = ref('')
const pickerDevices = ref<DeviceInfo[]>([])
const pickerLoading = ref(false)

// 隐患点搜索过滤
const filteredPickerHazards = computed(() => {
  if (!hazardKeyword.value.trim()) return allHazards.value
  const kw = hazardKeyword.value.trim().toLowerCase()
  return allHazards.value.filter(h =>
    h.name.toLowerCase().includes(kw)
    || (h.code && h.code.toLowerCase().includes(kw))
  )
})

// 当前已选隐患点对应的设备（用于图表加载）
const selectedHazardId = ref(0)

interface ChartGroup {
  deviceId: number
  deviceName: string
  deviceType: string
  series: ChartSeries[]
}

const chartGroups = ref<ChartGroup[]>([])

const isSelected = (id: number) => selectedDevices.value.some(d => d.id === id)

function openPicker() {
  showDevicePicker.value = true
  hazardKeyword.value = ''
  // 保持上次选中的隐患点
}

function onHazardSearch(e: any) {
  hazardKeyword.value = e.detail.value || ''
}

async function selectPickerHazard(h: Hazard) {
  pickerSelectedHazardId.value = h.id
  pickerSelectedHazardName.value = h.name
  pickerLoading.value = true
  pickerDevices.value = []
  try {
    const list = await hazardApi.getBoundDevices(h.id)
    pickerDevices.value = list.map((d: any) => ({
      id: d.deviceId ?? d.id,
      name: d.deviceName || d.name || d.deviceCode || '',
      code: d.deviceCode || d.code || '',
      deviceTypeName: d.deviceTypeName || d.deviceType || '',
      status: d.onlineStatus === 1 ? '在线' : '离线',
      onlineStatus: d.onlineStatus ?? 0,
      lastReportTime: d.lastReportTime || '',
      deviceName: d.deviceName || d.name || d.deviceCode || '',
      deviceCode: d.deviceCode || d.code || '',
      deviceType: d.deviceTypeName || d.deviceType || ''
    })) as DeviceInfo[]
  } catch (error) {
    console.error('加载设备列表失败:', error)
    pickerDevices.value = []
  } finally {
    pickerLoading.value = false
  }
}

/** 自动选中设备（供 device-detail 跳转和 URL 参数复用） */
async function autoSelectDevice(deviceId: number, hazardPointId: number) {
  // 加载设备信息
  let device: DeviceInfo | undefined
  try {
    device = await deviceApi.getById(deviceId)
  } catch (error) {
    console.error('预加载设备失败:', error)
  }
  if (!device) return

  // 若未指定 hazardPointId，从设备的 boundHazardPointId 取
  if (!hazardPointId && device.boundHazardPointId) {
    hazardPointId = device.boundHazardPointId
  }

  // 若设备仍无隐患点，取隐患点列表第一个作为默认（后端 chart 接口必传 hazardPointId）
  if (!hazardPointId && allHazards.value.length > 0) {
    hazardPointId = allHazards.value[0].id
  }

  // 设置隐患点上下文
  if (hazardPointId) {
    selectedHazardId.value = hazardPointId
    const hazard = allHazards.value.find(h => h.id === hazardPointId)
    if (hazard) {
      pickerSelectedHazardId.value = hazard.id
      pickerSelectedHazardName.value = hazard.name
      await selectPickerHazard(hazard)
    }
  }

  // 选中设备并加载图表
  const existing = pickerDevices.value.find(d => d.id === deviceId)
  selectedDevices.value = [existing || device]
  await loadAllCharts()
}

// 监听 device-detail 页面通过事件传递的自动选中参数
const onAutoSelect = (data: { deviceId: number; hazardPointId: number }) => {
  if (data?.deviceId) {
    autoSelectDevice(data.deviceId, data.hazardPointId || 0)
  }
}

onMounted(async () => {
  try {
    allHazards.value = await hazardApi.getAll()
  } catch (error) {
    console.error('加载隐患点失败:', error)
  }

  uni.$on('chart:auto-select', onAutoSelect)

  // 兼容 URL 参数（reLaunch 等场景）
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1] as any
  const options = currentPage?.options || currentPage?.$page?.options || {}

  if (options?.deviceId) {
    autoSelectDevice(
      Number(options.deviceId),
      options.hazardPointId ? Number(options.hazardPointId) : 0,
    )
  }
})

onUnmounted(() => {
  uni.$off('chart:auto-select', onAutoSelect)
})

const toggleDevice = (device: DeviceInfo) => {
  // 记录当前隐患点 ID（用于图表请求）
  if (pickerSelectedHazardId.value) {
    selectedHazardId.value = pickerSelectedHazardId.value
  }
  const idx = selectedDevices.value.findIndex(d => d.id === device.id)
  if (idx >= 0) {
    selectedDevices.value.splice(idx, 1)
  } else {
    selectedDevices.value.push(device)
  }
  loadAllCharts()
}

const removeDevice = (id: number) => {
  selectedDevices.value = selectedDevices.value.filter(d => d.id !== id)
  chartGroups.value = chartGroups.value.filter(g => g.deviceId !== id)
}

const changeTimeTab = (value: string) => {
  activeTimeTab.value = value
  loadAllCharts()
}

const getTimeRange = () => {
  let hours = 24
  switch (activeTimeTab.value) {
    case '7d': hours = 168; break
    case '30d': hours = 720; break
  }
  const endTime = new Date()
  const startTime = new Date(endTime.getTime() - hours * 3600000)
  const fmt = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  return { startTime: fmt(startTime), endTime: fmt(endTime) }
}

const loadAllCharts = async () => {
  if (selectedDevices.value.length === 0 || !selectedHazardId.value) {
    chartGroups.value = []
    return
  }

  loading.value = true
  try {
    const groups: ChartGroup[] = []
    const { startTime, endTime } = getTimeRange()
    const granularity = calcGranularity(startTime, endTime)
    const hazardPointId = selectedHazardId.value

    for (const device of selectedDevices.value) {
      let sensors: DeviceSensor[] = []
      try {
        sensors = await deviceApi.getSensors(device.id)
      } catch (error) {
        console.error(`获取设备 ${device.id} 传感器失败:`, error)
      }

      const allSeries: ChartSeries[] = []
      for (const sensor of sensors) {
        for (const attr of sensor.attrs) {
          try {
            const seriesList = await monitorApi.getChart({
              hazardPointId,
              deviceId: device.id,
              sensorId: sensor.id,
              attrCode: attr.attrCode,
              valueType: 'current',
              startTime,
              endTime,
              granularity,
            })
            allSeries.push(...seriesList)
          } catch (error) {
            console.error(`获取图表数据失败 device=${device.id} attr=${attr.attrCode}:`, error)
          }
        }
      }

      groups.push({
        deviceId: device.id,
        deviceName: device.name || device.deviceName,
        deviceType: device.deviceTypeName || device.deviceType,
        series: allSeries,
      })
    }

    chartGroups.value = groups
  } finally {
    loading.value = false
  }
}

</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}

/* 页面主体 */
.page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 筛选胶囊卡片 */
.filter-capsule-card {
  flex-shrink: 0;
  margin: 0 32rpx;
  padding: 16rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.1);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.tags-scroll {
  white-space: nowrap;
}

.tags-container {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.device-tag {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 16rpx;
  background: #f0f5ff;
  border-radius: 24rpx;
  flex-shrink: 0;
}

.tag-text {
  font-size: 24rpx;
  color: #3068e4;
}

.tag-close {
  font-size: 28rpx;
  color: #9ca3af;
  padding-left: 4rpx;
}

.hint-tag {
  padding: 10rpx 20rpx;
}

.hint-text {
  font-size: 24rpx;
  color: #9ca3af;
}

/* 时间胶囊 */
.time-capsule-row {
  display: flex;
  gap: 16rpx;
  background: #f7f8fc;
  border-radius: 20rpx;
  padding: 6rpx;
}

.capsule-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 0;
  border-radius: 16rpx;
  transition: all 0.2s;

  &.active {
    background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
    box-shadow: 0 4rpx 16rpx rgba(48, 104, 228, 0.3);

    .capsule-text {
      color: #ffffff;
      font-weight: 600;
    }
  }
}

.capsule-text {
  font-size: 26rpx;
  color: #6b7280;
}

/* 图表滚动区 */
.charts-scroll {
  flex: 1;
  height: 0;
}

.charts-container {
  padding: 16rpx 32rpx 0;
}

.device-chart-group {
  margin-bottom: 24rpx;
}

.chart-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.card-header-left {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.group-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.group-sub {
  font-size: 24rpx;
  color: #9ca3af;
}

.chart-container {
  width: 100%;
  height: 500rpx;
  position: relative;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  padding: 80rpx 0;
  color: #9ca3af;
}

.bottom-spacer { height: 40rpx; }

/* FAB 按钮 */
.fab-btn {
  position: fixed;
  right: 32rpx;
  bottom: 200rpx;
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(48, 104, 228, 0.4);
  z-index: 100;

  &:active {
    opacity: 0.9;
    transform: scale(0.95);
  }
}

.fab-badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  min-width: 36rpx;
  height: 36rpx;
  background: #f5222d;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8rpx;
  border: 2rpx solid #ffffff;
}

.fab-badge-text {
  font-size: 20rpx;
  color: #ffffff;
  font-weight: 600;
}

/* 设备选择抽屉 */
.picker-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.picker-panel {
  width: 100%;
  max-height: 75vh;
  background: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
  flex-shrink: 0;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
}

.picker-close {
  font-size: 40rpx;
  color: #9ca3af;
}

/* 双列布局 */
.picker-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.picker-col {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.hazard-col {
  width: 280rpx;
  flex-shrink: 0;
  border-right: 1rpx solid #f0f0f0;
  background: #f7f8fc;
}

.device-col {
  flex: 1;
  min-width: 0;
}

.device-col-header {
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  flex-shrink: 0;
}

.device-col-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1a1a2e;
}

/* 搜索框 */
.picker-search-bar {
  display: flex;
  align-items: center;
  margin: 12rpx;
  padding: 0 16rpx;
  height: 64rpx;
  background: #ffffff;
  border-radius: 32rpx;
  flex-shrink: 0;
}

.picker-search-icon {
  display: flex;
  align-items: center;
  margin-right: 8rpx;
}

.picker-search-input {
  flex: 1;
  height: 64rpx;
  font-size: 24rpx;
  color: #1a1a2e;
}

.picker-search-placeholder {
  color: #9ca3af;
  font-size: 24rpx;
}

/* 列内列表 */
.picker-col-list {
  flex: 1;
  min-height: 0;
  max-height: 55vh;
}

.hazard-option {
  padding: 24rpx 20rpx;
  font-size: 26rpx;
  color: #4b5563;
  border-bottom: 1rpx solid #eef0f5;

  &.active {
    background: #ffffff;
    color: #3068e4;
    font-weight: 600;
  }
}

.hazard-option-text {
  font-size: 26rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.device-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &.selected {
    background: rgba(48, 104, 228, 0.05);
  }

  &:active {
    background: #f7f8fc;
  }
}

.device-option-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  flex: 1;
  min-width: 0;
}

.device-option-name {
  font-size: 28rpx;
  color: #1a1a2e;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-option-code {
  font-size: 22rpx;
  color: #9ca3af;
}

.device-option-check {
  flex-shrink: 0;
  margin-left: 12rpx;
}

.picker-col-empty {
  padding: 80rpx 0;
  text-align: center;
}

.picker-col-empty-text {
  font-size: 26rpx;
  color: #9ca3af;
}
</style>
