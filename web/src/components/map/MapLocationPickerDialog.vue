<template>
  <el-dialog
      :model-value="modelValue"
      :title="title"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
      @opened="pickerRef?.invalidate()"
      @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <!-- 隐患点范围叠加(独立下拉框,仅用于预览,与表单字段不耦合) -->
    <div v-if="showHpOverlay" class="overlay-row">
      <span class="overlay-label">叠加隐患点范围:</span>
      <el-select
          v-model="overlayHpId"
          filterable
          clearable
          placeholder="选择一个隐患点,在地图上预览其区域范围"
          size="small"
          class="overlay-select"
      >
        <el-option
            v-for="hp in hazardPointList"
            :key="hp.id"
            :label="hp.name"
            :value="String(hp.id)"
        />
      </el-select>
    </div>

    <!-- 主体:左地图 + 右坐标只读面板(并排显示,无需滚动) -->
    <div class="map-dialog-body">
      <div class="map-side">
        <MapPointPicker
            ref="pickerRef"
            v-model="pickerLngLat"
            :readonly="readonly"
            :overlay-polygon="boundHpPolygon"
            height="400px"
        />
      </div>

      <div class="coord-side">
        <div class="coord-block">
          <div class="coord-block-label">当前位置(十进制)</div>
          <div class="coord-readonly" :class="{'is-empty': !pickerLngLat}">
            {{ decimalDisplay }}
          </div>
        </div>
        <div class="coord-block">
          <div class="coord-block-label">当前位置(度分秒)</div>
          <div class="coord-readonly" :class="{'is-empty': !pickerLngLat}">
            {{ dmsDisplay }}
          </div>
        </div>
        <div class="coord-block">
          <div class="coord-block-label">输入新坐标</div>
          <div class="coord-input-row">
            <el-input
                v-model="coordText"
                size="small"
                :placeholder="coordInputPlaceholder"
                @keyup.enter="applyCoordText"
            />
            <el-button
                size="small"
                type="primary"
                :disabled="!coordText.trim()"
                @click="applyCoordText"
            >使用
            </el-button>
          </div>
          <p class="coord-hint">
            支持十进制与度分秒,也可点击地图取点。
          </p>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button
          v-if="!readonly"
          type="primary"
          :disabled="!pickerLngLat"
          @click="handleConfirm"
      >确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {computed, nextTick, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import MapPointPicker from './MapPointPicker.vue'
import {centroid, decimalToDMS, deserialize, type LatLng} from '@/lib/boundaryCoords'
import {parseLatLngPair} from '@/lib/coordParser'
import {getHazardPointDetail} from '@/api/hazardPoint'

/** 公共组件:地图选点弹窗
 *
 * 用法:
 *   <MapLocationPickerDialog
 *     v-model="mapDialogVisible"
 *     :initial-point="somePoint"
 *     :hazard-point-list="hazardPointList"
 *     :show-hp-overlay="true"
 *     :initial-hp-id="formData.hazardPointId"
 *     :readonly="false"
 *     @confirm="onConfirm"
 *   />
 *
 * 事件:
 *   - update:modelValue: 弹窗显隐(双向)
 *   - confirm(point): 用户点击"确认坐标"时触发,readonly 时不会触发
 *   - cancel: 用户点击"取消"或关闭时触发
 */
export interface HazardPointOption {
  id: string
  name: string
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  initialPoint?: LatLng | null
  hazardPointList?: HazardPointOption[]
  showHpOverlay?: boolean
  initialHpId?: string
  readonly?: boolean
  title?: string
}>(), {
  initialPoint: null,
  hazardPointList: () => [],
  showHpOverlay: true,
  initialHpId: '',
  readonly: false,
  title: '在地图上选择安装位置',
})

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  'confirm': [point: LatLng]
  'cancel': []
}>()

const pickerRef = ref<InstanceType<typeof MapPointPicker> | null>(null)
const pickerLngLat = ref<LatLng | null>(null)
// 智能输入框:同时支持十进制与度分秒,由 parseLatLngPair 自动识别
const coordText = ref('')
// 抽到常量以避开 HTML 属性内嵌引号的解析冲突
const coordInputPlaceholder = `十进制(如 104.063, 30.671)或度分秒(如 104°03'48"E 30°40'16"N)`

// 地图叠加层:独立的下拉框,用于在地图上预览任意隐患点的范围
const overlayHpId = ref<string>('')
const boundHpPolygon = ref<LatLng[] | null>(null)

// ── 只读展示 ──
const decimalDisplay = computed(() => {
  if (!pickerLngLat.value) return '尚未选择坐标'
  return `${pickerLngLat.value.lng}, ${pickerLngLat.value.lat}`
})

const dmsDisplay = computed(() => {
  if (!pickerLngLat.value) return '尚未选择坐标'
  return `${decimalToDMS(pickerLngLat.value.lng, false)} ${decimalToDMS(pickerLngLat.value.lat, true)}`
})

// ── HP 边界加载 ──
const loadHpBoundary = async (hpId: string) => {
  boundHpPolygon.value = null
  if (!hpId) return
  try {
    const resp: any = await getHazardPointDetail(hpId)
    if (resp?.code === 200 && resp.data?.boundaryCoords) {
      const bc = deserialize(resp.data.boundaryCoords)
      if (bc.polygon.length >= 3) {
        boundHpPolygon.value = bc.polygon
        // 自动把地图视图 pan 到该隐患点区域的几何中心
        const center = centroid(bc.polygon)
        if (center) {
          nextTick(() => {
            pickerRef.value?.focusToCoord(center.lng, center.lat)
          })
        }
      }
    }
  } catch {
    boundHpPolygon.value = null
  }
}

watch(overlayHpId, (id) => loadHpBoundary(id))

// ── 弹窗打开时同步初始状态 ──
// 注意:这里用 props.modelValue 监听,避免内部 state 反复触发 open
watch(() => props.modelValue, (visible) => {
  if (visible) {
    pickerLngLat.value = props.initialPoint ? {...props.initialPoint} : null
    overlayHpId.value = props.initialHpId || ''
    loadHpBoundary(overlayHpId.value)
    // 智能输入框每次打开清空,避免与"当前位置"显示重复
    coordText.value = ''
  }
}, {immediate: true})

// ── 弹窗操作 ──

/** 用户在智能输入框中输入新坐标(自动识别十进制/度分秒)后,应用到地图 */
const applyCoordText = () => {
  const pair = parseLatLngPair(coordText.value)
  if (!pair) {
    ElMessage.warning('坐标格式无效,请输入十进制(如 104.063, 30.671)或度分秒(如 104°03\'48"E 30°40\'16"N)')
    return
  }
  pickerLngLat.value = pair
  // 成功应用后清空输入框,避免和"当前位置"显示重复
  coordText.value = ''
}

const handleConfirm = () => {
  if (!pickerLngLat.value) return
  emit('confirm', {...pickerLngLat.value})
  emit('update:modelValue', false)
}

const handleCancel = () => {
  emit('cancel')
  emit('update:modelValue', false)
}
</script>

<style scoped>
/* ========== 地图选点弹窗 - 主体左右分栏布局 ========== */
.map-dialog-body {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.map-side {
  flex: 0 0 60%;
  min-width: 0;
}

.coord-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  min-width: 0;
}

.coord-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coord-block-label {
  font-size: 12px;
  color: #475569;
  font-weight: 500;
}

.coord-readonly {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  color: #1f2937;
  background: #ffffff;
  padding: 7px 10px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  word-break: break-all;
  line-height: 1.5;
  min-height: 32px;
  display: flex;
  align-items: center;
}

.coord-readonly.is-empty {
  color: #94a3b8;
  font-style: italic;
  font-family: inherit;
  font-size: 12px;
}

.coord-input-row {
  display: flex;
  gap: 8px;
  align-items: stretch;
}

.coord-input-row .el-input {
  flex: 1;
  min-width: 0;
}

.coord-input-row .el-button {
  flex-shrink: 0;
}

.coord-hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
}

/* ========== 叠加隐患点范围行 ========== */
.overlay-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.overlay-label {
  flex-shrink: 0;
  font-size: 12px;
  color: #475569;
}

.overlay-select {
  flex: 1;
}
</style>
