<template>
  <div class="page settings-page">
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">系统设置</h2>
        <span class="header__subtitle">系统参数配置与全局偏好管理</span>
      </div>
    </div>

    <div class="settings-body">
      <div class="settings-card">
        <div class="params-sidebar">
          <div
              v-for="cat in paramCategories"
              :key="cat.key"
              class="category-item"
              :class="{ active: currentCategory === cat.key }"
              @click="scrollToCategory(cat.key)"
          >
            {{ cat.label }}
          </div>
      </div>
      <div class="params-main">
          <el-form
              ref="paramsFormRef"
              :model="paramsFormData"
              label-width="200px"
              class="params-form"
          >
            <div
                v-for="cat in paramCategories"
                :key="cat.key"
                :id="`category-${cat.key}`"
                class="param-section"
            >
              <h3 class="section-title">{{ cat.label }}</h3>
              <el-form-item
                  v-for="param in getParamsByCategory(cat.key)"
                  :key="param.code"
                  :label="param.name"
              >
                <template v-if="param.type === 'string'">
                  <el-input
                      v-model="paramsFormData[param.code]"
                      :placeholder="param.placeholder"
                      :maxlength="param.maxLength"
                      show-word-limit
                      style="width: 400px"
                  />
                </template>
                <template v-else-if="param.type === 'password'">
                  <el-input
                      v-model="paramsFormData[param.code]"
                      type="password"
                      show-password
                      :placeholder="param.placeholder"
                      :maxlength="param.maxLength"
                      style="width: 400px"
                  />
                </template>
                <template v-else-if="param.type === 'number'">
                  <el-input-number
                      v-model="paramsFormData[param.code]"
                      :min="param.min"
                      :max="param.max"
                      :step="param.step || 1"
                      controls-position="right"
                      style="width: 200px"
                  />
                </template>
                <template v-else-if="param.type === 'select'">
                  <el-select v-model="paramsFormData[param.code]" style="width: 200px">
                    <el-option
                        v-for="opt in param.options"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                    />
                  </el-select>
                </template>
                <template v-else-if="param.type === 'switch'">
                  <el-switch v-model="paramsFormData[param.code]"/>
                </template>
                <template v-else-if="param.type === 'textarea'">
                  <el-input
                      v-model="paramsFormData[param.code]"
                      type="textarea"
                      :rows="3"
                      :maxlength="param.maxLength"
                      show-word-limit
                      style="width: 400px"
                  />
                </template>
                <template v-else-if="param.type === 'geojson'">
                  <div class="geojson-editor">
                    <div class="geojson-actions">
                      <el-upload
                          accept=".json,.geojson"
                          :auto-upload="false"
                          :show-file-list="false"
                          :on-change="handleGeoJsonUpload"
                          style="display: inline-block; margin-right: 8px;"
                      >
                        <el-button type="primary" size="small">
                          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                               stroke-width="2" width="14" height="14"
                               style="vertical-align: middle; margin-right: 4px;">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="17 8 12 3 7 8"/>
                            <line x1="12" y1="3" x2="12" y2="15"/>
                          </svg>
                          导入GeoJSON
                        </el-button>
                      </el-upload>
                      <el-button type="success" size="small" @click="openMapDrawer">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                             stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                          <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/>
                          <line x1="8" y1="2" x2="8" y2="18"/>
                          <line x1="16" y1="6" x2="16" y2="22"/>
                        </svg>
                        地图绘制
                      </el-button>
                      <el-button type="warning" size="small" @click="handleExportGeoJson" v-if="geoJsonData">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                             stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                          <polyline points="7 10 12 15 17 10"/>
                          <line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                        导出GeoJSON
                      </el-button>
                      <el-button type="danger" size="small" @click="handleClearGeoJson" v-if="geoJsonData">
                        清除
                      </el-button>
                    </div>
                    <div class="geojson-preview" v-if="geoJsonData">
                      <div class="geojson-info">
                        <el-tag type="success" size="small">已设置关注区域</el-tag>
                        <span class="geojson-detail">{{ getGeoJsonSummary() }}</span>
                      </div>
                      <el-input
                          v-model="geoJsonText"
                          type="textarea"
                          :rows="6"
                          readonly
                          class="geojson-textarea"
                      />
                    </div>
                    <el-empty v-else description="暂未设置关注区域，请导入GeoJSON文件或在地图上绘制" :image-size="60"/>
                  </div>
                </template>
                <span class="param-remark">{{ param.remark }}</span>
              </el-form-item>
            </div>
          </el-form>
          <div class="params-actions">
            <el-button type="primary" @click="handleSaveParams" :loading="saveLoading">保存配置</el-button>
            <el-button @click="handleResetParams">重置</el-button>
          </div>
      </div>
      </div>
    </div>

    <!-- 地图绘制弹窗 -->
    <el-dialog
      title="地图绘制关注区域"
      v-model="mapDialogVisible"
      width="900px"
      :close-on-click-modal="false"
      @closed="cleanupMapDraw"
      class="map-draw-dialog"
    >
      <div class="map-draw-container">
        <div class="map-toolbar">
          <el-button-group>
            <el-button
              :type="drawMode === 'polygon' ? 'primary' : 'default'"
              size="small"
              @click="setDrawMode('polygon')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                <path d="M12 2l-11 6v10l11 6 11-6V8z"/>
              </svg>
              绘制多边形
            </el-button>
            <el-button
              :type="drawMode === 'rectangle' ? 'primary' : 'default'"
              size="small"
              @click="setDrawMode('rectangle')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
              </svg>
              绘制矩形
            </el-button>
            <el-button
              :type="drawMode === 'circle' ? 'primary' : 'default'"
              size="small"
              @click="setDrawMode('circle')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align: middle; margin-right: 4px;">
                <circle cx="12" cy="12" r="10"/>
              </svg>
              绘制圆形
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="clearDrawLayer"
            >
              清除绘制
            </el-button>
          </el-button-group>
          <span class="draw-hint" v-if="drawMode">
            <el-tag type="warning" size="small">{{ getDrawHint() }}</el-tag>
          </span>
          <span class="draw-hint" v-if="!drawMode">
            <el-tag type="info" size="small">选择上方绘制模式后点击地图开始绘制</el-tag>
          </span>
        </div>
        <div ref="mapContainerRef" class="map-container"></div>
        <div class="drawn-info" v-if="drawCoords.length > 0">
          <el-tag type="success" size="small">已添加 {{ drawCoords.length }} 个顶点</el-tag>
        </div>
      </div>
      <template #footer>
        <el-button @click="mapDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmDraw">
          确认使用此区域
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, reactive, ref} from 'vue'
import type {FormInstance, UploadFile} from 'element-plus'
import {ElMessage} from 'element-plus'
import {getFocusArea, getLogCleanupConfig, saveFocusArea, updateLogCleanupConfig} from '@/api/system'
import request from '@/utils/request'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

interface ParamItem {
  code: string
  name: string
  type: 'string' | 'number' | 'select' | 'switch' | 'textarea' | 'geojson' | 'password'
  category: string
  value: any
  placeholder?: string
  maxLength?: number
  min?: number
  max?: number
  step?: number
  options?: Array<{ label: string; value: any }>
  remark: string
}

const saveLoading = ref(false)
const currentCategory = ref('basic')

// 系统参数
const paramCategories = [
  { key: 'basic', label: '基础配置' },
  { key: 'data', label: '数据管理' },
  { key: 'alarm', label: '告警配置' },
  { key: 'security', label: '安全设置' },
  { key: 'notify', label: '通知配置' }
]

const paramList = ref<ParamItem[]>([
  { code: 'sys_name', name: '系统名称', type: 'string', category: 'basic', value: '地质灾害监测预警系统', placeholder: '请输入系统名称', maxLength: 50, remark: '系统显示名称' },
  { code: 'sys_logo', name: '系统Logo', type: 'string', category: 'basic', value: '', placeholder: '请输入Logo地址', maxLength: 200, remark: '系统Logo图片地址' },
  { code: 'sys_copyright', name: '版权信息', type: 'string', category: 'basic', value: '© 2024 地质灾害监测预警系统', placeholder: '请输入版权信息', maxLength: 100, remark: '页面底部版权信息' },
  { code: 'single_hazard_entry', name: '单一隐患点直接进入', type: 'switch', category: 'basic', value: false, remark: '只有一个隐患点时是否直接进入详情页' },
  { code: 'sys_focus_area', name: '系统关注范围区域', type: 'geojson', category: 'basic', value: null, remark: '系统在地图上关注的地理范围，支持GeoJSON格式' },

  { code: 'log_keep_days', name: '日志保留时长(天)', type: 'number', category: 'data', value: 365, min: 90, max: 3650, step: 30, remark: '系统日志保留天数' },
  { code: 'auto_cleanup', name: '自动清理', type: 'switch', category: 'data', value: true, remark: '是否启用数据自动清理' },
  {
    code: 'cleanup_time',
    name: '清理执行时间',
    type: 'string',
    category: 'data',
    value: '02:00',
    placeholder: '如: 02:00',
    remark: '每日自动清理执行时间'
  },

  { code: 'alarm_enable', name: '告警总开关', type: 'switch', category: 'alarm', value: true, remark: '是否启用系统告警功能' },
  { code: 'login_fail_lock', name: '登录失败锁定', type: 'switch', category: 'security', value: true, remark: '登录失败多次后是否锁定账号' },
  { code: 'login_fail_times', name: '允许失败次数', type: 'number', category: 'security', value: 5, min: 3, max: 10, remark: '允许的最大登录失败次数' },
  { code: 'lock_duration', name: '锁定时长(分钟)', type: 'number', category: 'security', value: 30, min: 5, max: 1440, step: 5, remark: '账号锁定后自动解锁时间' },
  { code: 'token_expire', name: 'Token过期(小时)', type: 'number', category: 'security', value: 2, min: 1, max: 24, remark: '用户登录Token有效期' },
  { code: 'password_expire', name: '密码有效期(天)', type: 'number', category: 'security', value: 90, min: 30, max: 365, step: 30, remark: '密码过期后需强制修改' },

  { code: 'notify.sms.access-key-id', name: '短信 AccessKeyId', type: 'string', category: 'notify', value: '', placeholder: '阿里云/腾讯云 AccessKeyId', maxLength: 128, remark: '短信服务商访问密钥 ID' },
  { code: 'notify.sms.access-key-secret', name: '短信 AccessKeySecret', type: 'password', category: 'notify', value: '', placeholder: 'AccessKeySecret', maxLength: 128, remark: '短信服务商访问密钥（不回显）' },
  { code: 'notify.sms.sign-name', name: '短信签名', type: 'string', category: 'notify', value: '', placeholder: '如：地质灾害监测', maxLength: 32, remark: '已审批通过的短信签名' },
  { code: 'notify.sms.template.alarm', name: '告警短信模板', type: 'string', category: 'notify', value: '', placeholder: '如：SMS_123456789', maxLength: 64, remark: '告警触发短信模板编号' },
  { code: 'notify.sms.template.offline', name: '离线短信模板', type: 'string', category: 'notify', value: '', placeholder: '如：SMS_987654321', maxLength: 64, remark: '设备离线短信模板编号' },
  { code: 'notify.mail.host', name: '邮件服务器', type: 'string', category: 'notify', value: '', placeholder: '如：smtp.qiye.aliyun.com', maxLength: 128, remark: 'SMTP 服务器地址' },
  { code: 'notify.mail.port', name: '邮件端口', type: 'number', category: 'notify', value: 465, min: 1, max: 65535, remark: 'SMTP 端口（SSL 通常 465）' },
  { code: 'notify.mail.username', name: '邮件账号', type: 'string', category: 'notify', value: '', placeholder: '发件邮箱地址', maxLength: 128, remark: 'SMTP 登录账号' },
  { code: 'notify.mail.password', name: '邮件密码', type: 'password', category: 'notify', value: '', placeholder: 'SMTP 密码或授权码', maxLength: 128, remark: 'SMTP 登录密码（不回显）' },
  { code: 'notify.mail.from', name: '发件人地址', type: 'string', category: 'notify', value: '', placeholder: 'noreply@example.com', maxLength: 128, remark: '发件人邮箱地址' },
  { code: 'notify.mail.ssl', name: '启用 SSL', type: 'switch', category: 'notify', value: true, remark: '是否使用 SSL 加密连接' }
])

const paramsFormData = reactive<Record<string, any>>({})

// 初始化参数表单数据
paramList.value.forEach(p => {
  paramsFormData[p.code] = p.value
})

const paramsFormRef = ref<FormInstance>()

// GeoJSON 相关
const geoJsonData = ref<any>(null)
const geoJsonText = computed(() => {
  return geoJsonData.value ? JSON.stringify(geoJsonData.value, null, 2) : ''
})

const getGeoJsonSummary = () => {
  if (!geoJsonData.value) return ''
  const features = geoJsonData.value.features || []
  const types = features.map((f: any) => f.geometry?.type).filter(Boolean)
  const uniqueTypes = [...new Set(types)]
  return `共 ${features.length} 个要素，类型: ${uniqueTypes.join('、')}`
}

const handleGeoJsonUpload = (uploadFile: UploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const content = e.target?.result as string
      const parsed = JSON.parse(content)

      if (!parsed.type || parsed.type !== 'FeatureCollection') {
        ElMessage.error('无效的GeoJSON文件，必须是FeatureCollection类型')
        return
      }

      geoJsonData.value = parsed
      paramsFormData.sys_focus_area = parsed
      ElMessage.success('GeoJSON文件导入成功')
    } catch (err) {
      ElMessage.error('GeoJSON文件解析失败，请检查文件格式')
    }
  }
  reader.readAsText(file)
}

const handleExportGeoJson = () => {
  if (!geoJsonData.value) return
  const blob = new Blob([JSON.stringify(geoJsonData.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'system-focus-area.geojson'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('GeoJSON导出成功')
}

const handleClearGeoJson = () => {
  geoJsonData.value = null
  paramsFormData.sys_focus_area = null
  ElMessage.success('关注区域已清除')
}

// 地图绘制相关
const mapDialogVisible = ref(false)
const mapContainerRef = ref<HTMLElement | null>(null)
let mapInstance: L.Map | null = null
let drawLayer: L.FeatureGroup | null = null
const drawMode = ref<string>('')
const drawCoords = ref<L.LatLng[]>([])
let drawPolygonClosed = false

const openMapDrawer = () => {
  mapDialogVisible.value = true;
  drawMode.value = '';
  drawCoords.value = [];
  drawPolygonClosed = false
  nextTick(() => initMap())
}

const initMap = () => {
  if (!mapContainerRef.value) return;
  if (mapInstance) mapInstance.remove()
  mapInstance = L.map(mapContainerRef.value).setView([39.9042, 116.4074], 10)
  setTimeout(() => mapInstance?.invalidateSize(), 200)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {attribution: '&copy; OSM'}).addTo(mapInstance)
  drawLayer = L.featureGroup().addTo(mapInstance)

  // 回显已有 GeoJSON
  if (geoJsonData.value) {
    try {
      L.geoJSON(geoJsonData.value, {
        style: {
          color: '#1890ff',
          weight: 3,
          fillColor: '#1890ff',
          fillOpacity: 0.2
        }
      }).addTo(drawLayer)
      mapInstance.fitBounds(drawLayer.getBounds(), {padding: [50, 50]})
    } catch {
    }
  }

  // 点击地图处理
  mapInstance.on('click', (e: L.LeafletMouseEvent) => {
    if (drawPolygonClosed) return
    if (drawMode.value === 'polygon') {
      // 检查是否点击在起始点附近（容差约 15 像素），且已有点 >= 3 → 闭合
      if (drawCoords.value.length >= 3) {
        const firstPoint = mapInstance!.latLngToContainerPoint(drawCoords.value[0])
        const clickPoint = mapInstance!.latLngToContainerPoint(e.latlng)
        const dist = firstPoint.distanceTo(clickPoint)
        if (dist < 15) {
          finishPolygon()
          return
        }
      }
      drawCoords.value.push(e.latlng)
      redrawPolygonPreview()
    } else if (drawMode.value === 'rectangle') {
      drawCoords.value = [e.latlng]
      drawMode.value = 'rectangle2'
    }
  })

  // 矩形第二步
  mapInstance.on('mousemove', (e: L.LeafletMouseEvent) => {
    if (drawMode.value !== 'rectangle2' || drawCoords.value.length !== 1) return
    const p1 = drawCoords.value[0];
    const p2 = e.latlng
    drawLayer!.clearLayers()
    L.rectangle(L.latLngBounds(p1, p2), {
      color: '#1890ff',
      weight: 2,
      fillColor: '#1890ff',
      fillOpacity: 0.15
    }).addTo(drawLayer!)
  })

  mapInstance.on('dblclick', (e: L.LeafletMouseEvent) => {
    if (drawPolygonClosed) return
    if (drawMode.value === 'polygon' && drawCoords.value.length >= 3) {
      finishPolygon()
    } else if (drawMode.value === 'rectangle2') {
      drawLayer!.clearLayers()
      const p1 = drawCoords.value[0];
      const p2 = e.latlng
      L.rectangle(L.latLngBounds(p1, p2), {
        color: '#1890ff',
        weight: 2,
        fillColor: '#1890ff',
        fillOpacity: 0.15
      }).addTo(drawLayer!)
      drawCoords.value = []
      drawMode.value = ''
    }
  })
}

const redrawPolygonPreview = () => {
  drawLayer!.clearLayers()
  const coords = drawCoords.value
  // 虚线预览边
  if (coords.length > 1) L.polyline(coords, {color: '#1890ff', dashArray: '5,5', weight: 2}).addTo(drawLayer!)
  // 绘制顶点标记：第一个点红色（可点击闭合），其余蓝色
  coords.forEach((c, i) => {
    const isFirst = i === 0 && coords.length >= 3
    L.circleMarker(c, {
      radius: isFirst ? 6 : 4,
      color: isFirst ? '#f5222d' : '#1890ff',
      fillColor: isFirst ? '#f5222d' : '#fff',
      fillOpacity: 1,
      weight: 2
    }).addTo(drawLayer!)
  })
  // 从最后一个点到第一个点的虚线提示闭合
  if (coords.length >= 3) {
    L.polyline([coords[coords.length - 1], coords[0]], {
      color: '#f5222d',
      dashArray: '3,6',
      weight: 1.5,
      opacity: 0.6
    }).addTo(drawLayer!)
  }
}

const finishPolygon = () => {
  drawPolygonClosed = true
  drawLayer!.clearLayers()
  L.polygon(drawCoords.value, {color: '#1890ff', weight: 2, fillColor: '#1890ff', fillOpacity: 0.15}).addTo(drawLayer!)
  drawCoords.value = []
  drawMode.value = ''
  // 延迟重置 flag 防止 click/dblclick 事件冒泡触发额外处理
  setTimeout(() => {
    drawPolygonClosed = false
  }, 300)
}

const setDrawMode = (mode: string) => {
  drawMode.value = drawMode.value === mode ? '' : mode
  if (mode === 'polygon') drawCoords.value = []
  drawPolygonClosed = false
}

const clearDrawLayer = () => {
  drawLayer?.clearLayers();
  drawCoords.value = [];
  drawPolygonClosed = false
}

const getDrawHint = () => {
  const h: Record<string, string> = {
    polygon: '点击地图添加顶点，点击红色起始点或双击完成绘制',
    rectangle: '点击起点 → 移动鼠标 → 双击终点完成',
    circle: '暂不支持圆形'
  }
  return h[drawMode.value] || ''
}

const getFeatureTypesSummary = () => {
  const geojson = drawLayer?.toGeoJSON() as any
  if (!geojson?.features?.length) return ''
  const types = [...new Set(geojson.features.map((f: any) => f.geometry?.type))]
  return types.join('、')
}

const handleConfirmDraw = () => {
  const geojson = drawLayer?.toGeoJSON() as any
  if (!geojson?.features?.length) return
  geoJsonData.value = geojson
  paramsFormData.sys_focus_area = geojson
  mapDialogVisible.value = false;
  drawMode.value = '';
  drawCoords.value = []
  ElMessage.success('关注区域已保存')
}

const cleanupMapDraw = () => {
  drawMode.value = '';
  drawCoords.value = []
  if (mapInstance) {
    mapInstance.remove();
    mapInstance = null
  }
}

const scrollToCategory = (key: string) => {
  currentCategory.value = key
  nextTick(() => {
    const el = document.getElementById(`category-${key}`)
    el?.scrollIntoView({behavior: 'smooth', block: 'start'})
  })
}

const getParamsByCategory = (category: string) => {
  return paramList.value.filter(p => p.category === category)
}

// 页面加载时从后端拉取配置
onMounted(async () => {
  try {
    const cfg = await getLogCleanupConfig()
    paramsFormData['auto_cleanup'] = cfg.enabled
    paramsFormData['log_keep_days'] = cfg.retentionDays
    paramsFormData['cleanup_time'] = cfg.cron
  } catch { /* 使用默认值 */
  }
  try {
    const res: any = await getFocusArea()
    const val = res?.data || res?.msg || res
    if (val && typeof val === 'string' && val !== 'null') {
      const parsed = JSON.parse(val)
      if (parsed && parsed.type === 'FeatureCollection') {
        geoJsonData.value = parsed
        paramsFormData.sys_focus_area = parsed
      }
    }
  } catch { /* 未配置 */
  }
  // 加载通知配置（notify 分类下的所有 code 从 sys_config 读取）
  const notifyCodes = paramList.value
      .filter(p => p.category === 'notify')
      .map(p => p.code)
  const results = await Promise.allSettled(
      notifyCodes.map(code => request.get<any>(`/system/config/configKey/${code}`))
  )
  results.forEach((r, idx) => {
    if (r.status !== 'fulfilled') return // 配置项缺失，保留默认值
    const raw: any = (r.value as any)?.data ?? (r.value as any)?.msg
    if (raw == null || raw === '') return
    const code = notifyCodes[idx]
    const item = paramList.value.find(p => p.code === code)
    if (!item) return
    if (item.type === 'switch') {
      paramsFormData[code] = raw === true || raw === 'true' || raw === 'Y' || raw === 1 || raw === '1'
    } else if (item.type === 'number') {
      const n = Number(raw)
      paramsFormData[code] = Number.isNaN(n) ? item.value : n
    } else {
      paramsFormData[code] = String(raw)
    }
  })
})

const handleSaveParams = async () => {
  saveLoading.value = true
  try {
    await updateLogCleanupConfig({
      enabled: paramsFormData['auto_cleanup'],
      retentionDays: paramsFormData['log_keep_days'],
      cron: paramsFormData['cleanup_time']
    })
    if (geoJsonData.value) {
      await saveFocusArea(geoJsonData.value)
    }
    // 保存通知配置（notify 分类写入 sys_config）
    const notifyItems = paramList.value.filter(p => p.category === 'notify')
    await Promise.all(
        notifyItems.map(item => {
          const value = item.type === 'switch'
              ? (paramsFormData[item.code] ? 'true' : 'false')
              : String(paramsFormData[item.code] ?? '')
          return request.put(`/system/config/configKey/${item.code}`, {configValue: value})
        })
    )
    ElMessage.success('系统参数保存成功')
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saveLoading.value = false
  }
}

const handleResetParams = () => {
  paramList.value.forEach(p => {
    paramsFormData[p.code] = p.value
  })
  geoJsonData.value = null
  ElMessage.info('已重置为默认值')
}
</script>

<style scoped>

.settings-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
}

.settings-card {
  flex: 1;
  min-height: 0;
  display: flex;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: clip;
}

/* 左侧导航 */
.params-sidebar {
  width: 160px;
  flex-shrink: 0;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 12px;
}

.category-item {
  padding: 10px 14px;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 2px;
  font-size: 13px;
  color: #606266;
  transition: all 0.15s;
  flex-shrink: 0;
}

.category-item:hover {
  background: #f5f7fa;
  color: #1890ff;
}

.category-item.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

/* 右侧内容 */
.params-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 0 20px;
}

.param-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.param-remark {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.settings-page {
  height: calc(100% + 1px);
}

.params-actions {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 16px 0;
  border-top: 1px solid #e8e8e8;
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

/* GeoJSON编辑器样式 */
.geojson-editor {
  width: 100%;
}

.geojson-actions {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.geojson-preview {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.geojson-info {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.geojson-detail {
  color: #606266;
  font-size: 13px;
}

.geojson-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #f5f7fa;
}

/* 地图绘制样式 */
.map-draw-dialog :deep(.el-dialog__body) {
  padding: 10px 20px;
}

.map-draw-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.map-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.draw-hint {
  margin-left: auto;
}

.map-container {
  width: 100%;
  height: 500px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
}

.drawn-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-types {
  color: #606266;
  font-size: 13px;
}
</style>
