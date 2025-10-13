<template>
  <div class="map-container">
    <!-- 地图主体 -->
    <div id="map" class="map"></div>
    <!-- 坐标显示区域 -->
    <div id="mouse-position" class="custom-mouse-position"></div>

    <!-- 地图控件面板 -->
    <div class="map-controls">
      <!-- 底图切换控件 -->
      <el-card class="control-card">
        <template #header>
          <div class="card-header">
            <span>底图切换</span>
          </div>
        </template>
        <el-radio-group v-model="baseMapType" size="small" @change="handleBaseMapChange">
          <el-radio-button label="vec">矢量图</el-radio-button>
          <el-radio-button label="img">卫星图</el-radio-button>
          <el-radio-button label="ter">地形图</el-radio-button>
        </el-radio-group>
      </el-card>

      <!-- 图层控制控件 -->
      <el-card class="control-card">
        <template #header>
          <div class="card-header">
            <span>图层控制</span>
          </div>
        </template>
        <el-checkbox-group v-model="enabledLayers" @change="handleLayerChange">
          <el-checkbox label="annotation" size="small">矢量注记</el-checkbox>
          <el-checkbox label="imgAnnotation" size="small">影像注记</el-checkbox>
          <el-checkbox label="boundary" size="small">行政边界</el-checkbox>
        </el-checkbox-group>
      </el-card>

      <!-- 地图信息显示 -->
      <el-card class="control-card info-card">
        <template #header>
          <div class="card-header">
            <span>地图信息</span>
          </div>
        </template>
        <div class="info-content">
          <p>当前坐标: {{ currentCoord }}</p>
          <p>缩放级别: {{ currentZoom }}</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Map, View, Tile } from 'ol';
import { Tile as TileLayer } from 'ol/layer';
import LayerGroup from 'ol/layer/Group';
import { XYZ } from 'ol/source';
import { ScaleLine } from 'ol/control';
import MousePosition from 'ol/control/MousePosition';
import { createStringXY } from 'ol/coordinate';
import * as proj from 'ol/proj';
import 'ol/ol.css';

// 响应式数据
const baseMapType = ref('vec'); // 默认矢量图
const enabledLayers = ref(['annotation']); // 默认启用矢量注记
const currentCoord = ref('');
const currentZoom = ref(10);

// 地图实例
let map = null;
let baseMapLayerGroup = null;
let overlayLayers = {};

// 天地图服务配置
const tianDiTuKey = 'fcb94c343782f4e289bb71d51218232c'; // 请替换为实际密钥
const tianDiTuServices = {
  // 矢量底图
  vec: {
    url: `https://t{0-7}.tianditu.gov.cn/vec_c/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=c&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=tiles&tk=${tianDiTuKey}`,
    layer: 'vec',
    matrixSet: 'c'
  },
  // 影像底图
  img: {
    url: `https://t{0-7}.tianditu.gov.cn/img_c/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=c&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=tiles&tk=${tianDiTuKey}`,
    layer: 'img',
    matrixSet: 'c'
  },
  // 地形底图
  ter: {
    url: `https://t{0-7}.tianditu.gov.cn/ter_c/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=ter&STYLE=default&TILEMATRIXSET=c&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=tiles&tk=${tianDiTuKey}`,
    layer: 'ter',
    matrixSet: 'c'
  },
  // 矢量注记
  annotation: {
    url: `https://t{0-7}.tianditu.gov.cn/cva_c/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=c&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=tiles&tk=${tianDiTuKey}`,
    layer: 'cva',
    matrixSet: 'c'
  },
  // 影像注记
  imgAnnotation: {
    url: `https://t{0-7}.tianditu.gov.cn/cia_c/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=c&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=tiles&tk=${tianDiTuKey}`,
    layer: 'cia',
    matrixSet: 'c'
  },
  // 行政边界
  boundary: {
    url: `https://t{0-7}.tianditu.gov.cn/ibo_c/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=ibo&STYLE=default&TILEMATRIXSET=c&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=tiles&tk=${tianDiTuKey}`,
    layer: 'ibo',
    matrixSet: 'c'
  }
};

// 创建天地图图层
function createTianDiTuLayer(type) {
  const service = tianDiTuServices[type];
  return new TileLayer({
    source: new XYZ({
      url: service.url,
      projection: 'EPSG:4326',
      crossOrigin: 'anonymous'
    })
  });
}

// 初始化地图
function initMap() {
  // 创建底图图层组
  baseMapLayerGroup = new LayerGroup({
    layers: [createTianDiTuLayer('vec')]
  });

  // 创建覆盖图层
  overlayLayers = {
    annotation: createTianDiTuLayer('annotation'),
    imgAnnotation: createTianDiTuLayer('imgAnnotation'),
    boundary: createTianDiTuLayer('boundary')
  };

  // 初始化地图
  map = new Map({
    target: 'map',
    layers: [
      baseMapLayerGroup,
      overlayLayers.annotation // 默认加载矢量注记
    ],
    view: new View({
      projection: 'EPSG:4326',
      center: [104.066541, 30.572762], // 成都坐标
      zoom: 10,
      minZoom: 1,
      maxZoom: 18
    }),
    controls: [] // 不使用默认控件
  });

  // 添加比例尺控件
  const scaleLineControl = new ScaleLine({
    units: 'metric',
    bar: true,
    text: true,
    minWidth: 120
  });
  map.addControl(scaleLineControl);

  // 添加鼠标位置控件
  const mousePositionControl = new MousePosition({
    coordinateFormat: createStringXY(6),
    projection: 'EPSG:4326',
    className: 'custom-mouse-position',
    target: document.getElementById('mouse-position'),
    undefinedHTML: '&nbsp;'
  });
  map.addControl(mousePositionControl);

  // 监听地图移动和缩放事件
  map.on('moveend', handleMapMove);
  
  // 初始更新地图信息
  handleMapMove();
}

// 处理底图切换
function handleBaseMapChange() {
  // 移除当前底图
  baseMapLayerGroup.getLayers().clear();
  // 添加新底图
  baseMapLayerGroup.getLayers().push(createTianDiTuLayer(baseMapType.value));
  
  // 根据底图类型调整注记图层
  updateOverlayLayers();
}

// 处理图层切换
function handleLayerChange() {
  updateOverlayLayers();
}

// 更新覆盖图层
function updateOverlayLayers() {
  // 先移除所有覆盖图层
  Object.keys(overlayLayers).forEach(key => {
    if (map.getLayers().getArray().includes(overlayLayers[key])) {
      map.removeLayer(overlayLayers[key]);
    }
  });
  
  // 根据选中状态添加图层
  enabledLayers.value.forEach(layer => {
    // 如果是影像底图，应该使用影像注记而不是矢量注记
    if (baseMapType.value === 'img' && layer === 'annotation') {
      // 不添加矢量注记，而是添加影像注记
      if (!map.getLayers().getArray().includes(overlayLayers.imgAnnotation)) {
        map.addLayer(overlayLayers.imgAnnotation);
      }
    } else {
      // 添加选中的图层
      if (overlayLayers[layer] && !map.getLayers().getArray().includes(overlayLayers[layer])) {
        map.addLayer(overlayLayers[layer]);
      }
    }
  });
}

// 处理地图移动事件
function handleMapMove() {
  const view = map.getView();
  const center = view.getCenter();
  currentCoord.value = center ? center.map(coord => coord.toFixed(6)).join(', ') : '';
  currentZoom.value = view.getZoom();
}

// 组件挂载
onMounted(() => {
  initMap();
});

// 组件卸载
onUnmounted(() => {
  if (map) {
    map.dispose();
  }
});
</script>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.map {
  width: 100%;
  height: 100%;
  z-index: 1;
}

.map-controls {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.control-card {
  width: 280px;
  background-color: rgba(255, 255, 255, 0.9);
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #333;
}

.info-content {
  font-size: 14px;
  line-height: 1.8;
}

.info-content p {
  margin: 5px 0;
  color: #666;
}

.custom-mouse-position {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(255, 255, 255, 0.8);
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  z-index: 100;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>

