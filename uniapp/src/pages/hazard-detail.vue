<!-- src/pages/hazard-detail.vue -->
<script setup lang="ts">
import type { HazardDetail } from '@/utils/hazard'
import PageHeader from '@/components/PageHeader.vue'
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { hazardApi } from '@/utils/hazard'
import http from '@/utils/api'
import { wgs84ToGcj02 } from '@/utils/coordTransform'

// 服务器地址（去掉 /api/v1 后缀，用于拼接图片等静态资源）
const SERVER_ORIGIN = (http as any).BASE_URL?.replace(/\/api\/v1\/?$/, '') || ''

const hazardId = ref<number>(0)
const hazard = ref<Partial<HazardDetail>>({})
const devices = ref<any[]>([])
const loading = ref(true)

onLoad(async (options) => {
  if (options?.id) {
    hazardId.value = Number(options.id)
    await loadData()
  }
})

async function loadData() {
  loading.value = true
  try {
    const detail = await hazardApi.getById(hazardId.value)
    if (detail) {
      hazard.value = detail
      devices.value = await hazardApi.getBoundDevices(hazardId.value)
    }
    else {
      uni.showToast({ title: '隐患点不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
    }
  }
  catch (error) {
    console.error('加载隐患点详情失败:', error)
  }
  finally {
    loading.value = false
  }
}

function goToDeviceDetail(item: any) {
  uni.navigateTo({ url: `/pages/device-detail?id=${item.deviceId}` })
}

// === 地图数据 ===
const hasMap = computed(() => hazard.value.latitude != null && hazard.value.longitude != null)

const mapCenter = computed(() => {
  const lat = hazard.value.latitude
  const lng = hazard.value.longitude
  if (lat == null || lng == null) return { latitude: 30, longitude: 120 }
  return wgs84ToGcj02(lat, lng)
})

const mapMarkers = computed(() => {
  const markers: any[] = []
  // 隐患点中心 — 有区域范围时不显示中心 marker
  if (hasMap.value && mapPolygons.value.length === 0) {
    const gc = wgs84ToGcj02(hazard.value.latitude!, hazard.value.longitude!)
    markers.push({
      id: 0,
      latitude: gc.latitude,
      longitude: gc.longitude,
      title: hazard.value.name || '隐患点',
      width: 32,
      height: 32,
      iconPath: '/static/icons/hazard-active.png',
      callout: {
        content: hazard.value.name || '隐患点',
        color: '#ffffff',
        fontSize: 12,
        borderRadius: 8,
        bgColor: '#3068e4',
        padding: 8,
        display: 'ALWAYS',
      },
    })
  }
  // 关联设备
  devices.value.forEach((d: any, i: number) => {
    if (d.installLatitude != null && d.installLongitude != null) {
      const gc = wgs84ToGcj02(d.installLatitude, d.installLongitude)
      const iconRaw = d.iconPath || ''
      const icon = iconRaw.startsWith('http')
        ? iconRaw
        : iconRaw
          ? `${SERVER_ORIGIN}${iconRaw.startsWith('/') ? '' : '/'}${iconRaw}`
          : '/static/icons/device-tab.png'
      markers.push({
        id: i + 1,
        latitude: gc.latitude,
        longitude: gc.longitude,
        title: d.deviceName || '',
        width: 28,
        height: 28,
        iconPath: icon,
        label: {
          content: d.deviceName || '',
          color: '#1a1a2e',
          fontSize: 10,
          bgColor: '#ffffff',
          borderRadius: 6,
          padding: 4,
          anchorX: 0,
          anchorY: -20,
        },
      })
    }
  })
  return markers
})

// === 解析 boundaryCoords JSON ===
// 格式: { polygon: [[lat,lng],...], strikeLine: [[lat,lng],[lat,lng]]|null, auxiliaryLines: [[[lat,lng],...],...] }
function parseCoordPair(c: any): { latitude: number, longitude: number } | null {
  let raw: { latitude: number, longitude: number } | null = null
  if (Array.isArray(c) && c.length >= 2)
    raw = { latitude: Number(c[0]), longitude: Number(c[1]) }
  else if (c && typeof c === 'object' && c.lat != null)
    raw = { latitude: Number(c.lat), longitude: Number(c.lng) }
  else if (c && typeof c === 'object' && c.latitude != null)
    raw = { latitude: Number(c.latitude), longitude: Number(c.longitude) }
  if (!raw) return null
  // WGS84 → GCJ-02
  return wgs84ToGcj02(raw.latitude, raw.longitude)
}

const mapPolygons = computed(() => {
  const raw = hazard.value.boundaryCoords
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    // 从 parsed.polygon 或 parsed 本身（兼容旧格式）提取顶点数组
    const polySrc = Array.isArray(parsed) ? parsed : parsed.polygon
    if (!Array.isArray(polySrc)) return []

    const points = polySrc
      .map(parseCoordPair)
      .filter((p: any): p is { latitude: number, longitude: number } => p != null)

    if (points.length >= 3) {
      return [{
        points,
        strokeColor: '#3068e4',
        fillColor: '#3068e426',
        strokeWidth: 2,
        zIndex: 1,
      }]
    }
  }
  catch {
    // boundaryCoords 格式不合法，忽略
  }
  return []
})

const mapPolylines = computed(() => {
  const lines: { points: { latitude: number, longitude: number }[], color: string, width: number, dottedLine: boolean, arrowLine: boolean }[] = []
  const raw = hazard.value.boundaryCoords
  let hasStrikeLine = false

  // 优先使用 boundaryCoords.strikeLine（精确坐标）
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      // 走向线
      const strikeSrc = parsed?.strikeLine || parsed?.strikeCoords
      if (Array.isArray(strikeSrc) && strikeSrc.length >= 2) {
        const pts = strikeSrc.map(parseCoordPair).filter((p: any): p is { latitude: number, longitude: number } => p != null)
        if (pts.length >= 2) {
          lines.push({ points: pts, color: '#fa8c16', width: 3, dottedLine: false, arrowLine: true })
          hasStrikeLine = true
        }
      }
      // 辅助线
      const auxSrc = parsed?.auxiliaryLines
      if (Array.isArray(auxSrc)) {
        auxSrc.forEach((line: any) => {
          if (Array.isArray(line) && line.length >= 2) {
            const pts = line.map(parseCoordPair).filter((p: any): p is { latitude: number, longitude: number } => p != null)
            if (pts.length >= 2) {
              lines.push({ points: pts, color: '#13c2c2', width: 2, dottedLine: true, arrowLine: false })
            }
          }
        })
      }
    }
    catch {
      // ignore
    }
  }

  // 如果 boundaryCoords 中没有走向线，用 strike 角度估算
  if (!hasStrikeLine) {
    const strike = hazard.value.strike
    const lat = hazard.value.latitude
    const lng = hazard.value.longitude
    if (strike != null && lat != null && lng != null) {
      const rad = Number(strike) * Math.PI / 180
      const len = 0.004 // ~400m in degrees
      const dLat = Math.cos(rad) * len
      const dLng = Math.sin(rad) * len / Math.cos(lat * Math.PI / 180)
      const p1 = wgs84ToGcj02(lat - dLat, lng - dLng)
      const p2 = wgs84ToGcj02(lat + dLat, lng + dLng)
      lines.push({
        points: [p1, p2],
        color: '#fa8c16',
        width: 3,
        dottedLine: false,
        arrowLine: true,
      })
    }
  }

  return lines
})
</script>

<template>
  <view class="page-container">
    <!-- 头部 -->
    <PageHeader show-back :title="hazard.name" />

    <!-- 内容 -->
    <scroll-view class="page-body" scroll-y>
      <!-- 基本信息 -->
      <view class="section">
        <view class="info-card">
          <view class="info-row">
            <text class="info-label">位置</text>
            <text class="info-value">{{ hazard.location || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">状态</text>
            <view class="status-badge" :class="hazard.status === '监测中' ? 'monitoring' : 'resolved'">
              {{ hazard.status || '-' }}
            </view>
          </view>
          <view class="info-row">
            <text class="info-label">设备数量</text>
            <text class="info-value">{{ hazard.deviceCount || 0 }}台</text>
          </view>
          <view class="info-row">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ hazard.createTime || '-' }}</text>
          </view>
          <view class="info-row desc-row">
            <text class="info-label">描述</text>
            <text class="info-value desc-text">{{ hazard.description || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- 位置分布地图 -->
      <view class="section">
        <text class="section-title">位置分布</text>
        <view class="map-card">
          <map
            class="map-view"
            :latitude="mapCenter.latitude"
            :longitude="mapCenter.longitude"
            :markers="mapMarkers"
            :polygons="mapPolygons"
            :polyline="mapPolylines"
            :scale="16"
            show-location
            enable-satellite
            enable-overlooking
          />
          <view class="map-legend">
            <view class="legend-item">
              <view class="legend-dot legend-center" />
              <text class="legend-text">隐患点中心</text>
            </view>
            <view v-if="mapPolygons.length" class="legend-item">
              <view class="legend-dot legend-area" />
              <text class="legend-text">区域范围</text>
            </view>
            <view v-if="mapPolylines.length" class="legend-item">
              <view class="legend-dot legend-strike" />
              <text class="legend-text">走向</text>
            </view>
            <view class="legend-item">
              <view class="legend-dot legend-device" />
              <text class="legend-text">关联设备</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 关联设备 -->
      <view class="section">
        <text class="section-title">关联设备</text>
        <view class="device-list">
          <view
            v-for="device in devices"
            :key="device.deviceId || device.id"
            class="device-item"
            @click="goToDeviceDetail(device)"
          >
            <view class="device-left">
              <view class="device-main">
                <text class="device-name">{{ device.deviceName }}</text>
                <view class="device-type-tag">
                  {{ device.deviceCode || '-' }}
                </view>
              </view>
              <view v-if="device.sensors && device.sensors.length" class="sensor-tags">
                <text
                  v-for="s in device.sensors"
                  :key="s.id"
                  class="sensor-tag"
                >{{ s.name }}</text>
              </view>
              <text class="device-time">绑定时间：{{ device.bindTime || '-' }}</text>
            </view>
            <view class="device-status">
              <view
                class="status-dot"
                :class="device.onlineStatus === 1 ? 'online' : 'offline'"
              />
              <text class="status-text">{{ device.onlineStatus === 1 ? '在线' : '离线' }}</text>
            </view>
          </view>

          <view v-if="devices.length === 0" class="empty-logs">
            <text class="empty-text">暂无关联设备</text>
          </view>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}

.header {
  position: relative;
  flex-shrink: 0;
}

.page-body {
  flex: 1;
  height: 0;
}

.section {
  margin: 0 32rpx 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16rpx;
  display: block;
}

.info-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &.desc-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8rpx;
  }
}

.info-label {
  font-size: 26rpx;
  color: #6b7280;
  flex-shrink: 0;
}

.info-value {
  font-size: 26rpx;
  color: #1a1a2e;
  font-weight: 500;
  text-align: right;

  &.desc-text {
    max-width: 100%;
    line-height: 1.6;
    font-weight: 400;
    text-align: left;
    word-break: break-all;
    white-space: pre-wrap;
  }
}

.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &.monitoring {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
  }

  &.resolved {
    background: rgba(156, 163, 175, 0.1);
    color: #9ca3af;
  }
}

/* 关联设备 */
.device-list {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 8rpx 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.device-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  gap: 16rpx;

  &:last-child {
    border-bottom: none;
  }
}

.sensor-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-bottom: 6rpx;
}

.sensor-tag {
  font-size: 20rpx;
  color: #3068e4;
  background: rgba(48, 104, 228, 0.08);
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.device-left {
  flex: 1;
  margin-right: 16rpx;
}

.device-main {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.device-name {
  font-size: 28rpx;
  color: #1a1a2e;
  font-weight: 500;
}

.device-type-tag {
  font-size: 22rpx;
  color: #6b7280;
  background: #f5f5f5;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.device-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.device-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
  flex-shrink: 0;
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;

  &.online {
    background: #52c41a;
  }

  &.offline {
    background: #d9d9d9;
  }

  &.fault {
    background: #f5222d;
  }
}

.status-text {
  font-size: 24rpx;
  color: #6b7280;
}

.empty-logs {
  padding: 40rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 24rpx;
  color: #9ca3af;
}

/* 地图 */
.map-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.12);
}

.map-view {
  width: 100%;
  height: 450rpx;
}

.map-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 16rpx 24rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.legend-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}

.legend-center {
  background: #3068e4;
}

.legend-area {
  background: rgba(48, 104, 228, 0.25);
  border: 2rpx solid #3068e4;
}

.legend-strike {
  background: #fa8c16;
  border-radius: 2rpx;
  width: 24rpx;
  height: 6rpx;
}

.legend-device {
  background: #52c41a;
  width: 12rpx;
  height: 12rpx;
}

.legend-text {
  font-size: 22rpx;
  color: #6b7280;
}

.bottom-spacer {
  height: 32rpx;
}
</style>
