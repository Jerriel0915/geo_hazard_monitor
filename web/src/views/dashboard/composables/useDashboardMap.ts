import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {ref, type Ref, shallowRef} from 'vue'
import {getBoundDevices} from '@/api/hazardPoint'
import {buildTiandituUrl} from '@/composables/useLeafletMap'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface HazardPointInfo {
    id: number
    name: string
    code: string
    latitude: number
    longitude: number
    groupId?: number
    groupName?: string
    description?: string
    deviceCount: number
    status: string
    alarmLevel?: string | null
    boundaryCoords?: string | null
}

export interface DeviceInfo {
    id: number
    name: string
    type: string
    typeName: string
    status: 'online' | 'warning' | 'offline'
    sensorCount: number
    longitude: number
    latitude: number
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

export const LAYER_OPTIONS = [
    {
        id: 'image',
        name: '影像图',
        color: '#87CEEB',
        baseUrl: 'img_w',
        baseLayer: 'img',
        labelUrl: 'cia_w',
        labelLayer: 'cia'
    },
    {
        id: 'vector',
        name: '矢量图',
        color: '#90EE90',
        baseUrl: 'vec_w',
        baseLayer: 'vec',
        labelUrl: 'cva_w',
        labelLayer: 'cva'
    },
    {
        id: 'terrain',
        name: '地形图',
        color: '#DEB887',
        baseUrl: 'ter_w',
        baseLayer: 'ter',
        labelUrl: 'cta_w',
        labelLayer: 'cta'
    },
] as const

const ALARM_COLORS: Record<string, string> = {
    critical: '#f5222d', major: '#faad14', minor: '#722ed1', info: '#1890ff',
}

/** Create a Leaflet divIcon for a device marker */
function createDeviceIcon(status: string) {
    const color = status === 'online' ? '#52c41a' : status === 'warning' ? '#faad14' : '#f5222d'
    return L.divIcon({
        className: 'device-marker',
        html: `<div style="width:24px;height:24px;background:${color};border:2px solid white;border-radius:50%;box-shadow:0 2px 6px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;color:white;font-size:12px">📡</div>`,
        iconSize: [24, 24], iconAnchor: [12, 12],
    })
}

function getStatusText(status: string) {
    return status === 'online' ? '在线' : status === 'warning' ? '预警' : '离线'
}

/** Shared popup HTML for hazard point markers */
export function buildHazardPopup(point: HazardPointInfo): string {
    const alvMap: Record<string, { text: string; bg: string; color: string }> = {
        critical: {text: '严重', bg: 'rgba(245,34,45,0.1)', color: '#f5222d'},
        major: {text: '重要', bg: 'rgba(250,173,20,0.1)', color: '#fa8c16'},
        minor: {text: '一般', bg: 'rgba(250,215,64,0.1)', color: '#d4a017'},
        info: {text: '提示', bg: 'rgba(82,196,26,0.1)', color: '#52c41a'},
    }
    const alv = alvMap[point.alarmLevel || ''] || {
        text: point.alarmLevel || '--',
        bg: 'rgba(24,144,255,0.1)',
        color: '#1890ff'
    }
    const desc = point.description ? `<div class="hpv2-dash"></div><div class="hpv2-row single"><div class="hpv2-cell full"><span class="hpv2-label">描述</span><span class="hpv2-val">${point.description}</span></div></div>` : ''
    return `<div class="hpv2-card"><div class="hpv2-header"><span class="hpv2-title">${point.name}</span></div><div class="hpv2-dash"></div><div class="hpv2-body"><div class="hpv2-row"><div class="hpv2-cell"><span class="hpv2-label">编号</span><span class="hpv2-val">${point.code}</span></div><div class="hpv2-cell"><span class="hpv2-label">分组</span><span class="hpv2-val">${point.groupName || '--'}</span></div></div><div class="hpv2-dash"></div><div class="hpv2-row"><div class="hpv2-cell"><span class="hpv2-label">坐标</span><span class="hpv2-val">${point.latitude.toFixed(4)}, ${point.longitude.toFixed(4)}</span></div><div class="hpv2-cell"><span class="hpv2-label">设备</span><span class="hpv2-val">${point.deviceCount} 台</span></div></div><div class="hpv2-dash"></div><div class="hpv2-row single"><div class="hpv2-cell full"><span class="hpv2-label">预警等级</span><span class="hpv2-level" style="background:${alv.bg};color:${alv.color}">${alv.text}</span></div></div>${desc}</div></div>`
}

/** Shared popup HTML for device markers */
export function buildDevicePopup(device: DeviceInfo): string {
    return `<div class="hpv2-card"><div class="hpv2-header"><span class="hpv2-title">${device.name}</span></div><div class="hpv2-dash"></div><div class="hpv2-body"><div class="hpv2-row single"><div class="hpv2-cell full"><span class="hpv2-label">类型</span><span class="hpv2-val">${device.typeName}</span></div></div><div class="hpv2-dash"></div><div class="hpv2-row single"><div class="hpv2-cell full"><span class="hpv2-label">传感器</span><span class="hpv2-val">${device.sensorCount} 个</span></div></div><div class="hpv2-dash"></div><div class="hpv2-row single"><div class="hpv2-cell full"><span class="hpv2-label">状态</span><span class="hpv2-val">${getStatusText(device.status)}</span></div></div></div></div>`
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export interface UseDashboardMapOptions {
    container: Ref<HTMLDivElement | null>
    /** 点击隐患点标记时回调 */
    onMarkerClick: (point: HazardPointInfo) => void
    /** 双击隐患点标记时回调 */
    onMarkerDblClick: (point: HazardPointInfo) => void
}

export function useDashboardMap(opts: UseDashboardMapOptions) {
    const mapInstance = shallowRef<L.Map | null>(null)
    let mapInstanceRaw: L.Map | null = null
    let baseLayer: L.TileLayer | null = null
    let labelLayer: L.TileLayer | null = null
    let isLabelVisible = true

    let maskLayer: L.GeoJSON | null = null
    let focusAreaLayer: L.GeoJSON | null = null
    let hazardMarkerLayer: L.LayerGroup | null = null
    let hazardBoundaryLayer: L.LayerGroup | null = null
    const hazardMarkerMap = new Map<number, L.Marker>()
    const hazardPointDataMap = new Map<number, HazardPointInfo>()
    const ripples = new Map<number, L.Circle[]>()

    const currentLayer = ref('image')
    const showMaskLayer = ref(true)

    let savedMapView: { center: [number, number]; zoom: number } | null = null
    const getSavedMapView = () => savedMapView
    const setSavedMapView = (v: { center: [number, number]; zoom: number } | null) => {
        savedMapView = v
    }

    // ── Map init ──
    const initMap = () => {
        if (!opts.container.value) return
        const m = L.map(opts.container.value, {
            center: [30.67, 104.06],
            zoom: 10,
            zoomControl: false,
            attributionControl: false
        })
        mapInstanceRaw = m
        mapInstance.value = m
        addLayer('image')
        L.control.scale({maxWidth: 150, metric: true, imperial: false, position: 'bottomright'}).addTo(m)
    }

    const handleResize = () => {
        mapInstanceRaw?.invalidateSize()
    }

    // ── Layers ──
    const addLayer = (layerId: string) => {
        if (!mapInstanceRaw) return
        const layer = LAYER_OPTIONS.find(l => l.id === layerId)
        if (!layer) return
        if (baseLayer) mapInstanceRaw.removeLayer(baseLayer)
        if (labelLayer) {
            mapInstanceRaw.removeLayer(labelLayer);
            labelLayer = null
        }
        baseLayer = L.tileLayer(buildTiandituUrl(layer.baseUrl, layer.baseLayer), {
            maxZoom: 18,
            minZoom: 1
        }).addTo(mapInstanceRaw)
        if (isLabelVisible) addLabelOverlay(layer)
    }

    const addLabelOverlay = (layer?: typeof LAYER_OPTIONS[number]) => {
        if (!mapInstanceRaw) return
        const l = layer || LAYER_OPTIONS.find(l => l.id === currentLayer.value)
        if (!l) return
        if (labelLayer) {
            mapInstanceRaw.removeLayer(labelLayer);
            labelLayer = null
        }
        labelLayer = L.tileLayer(buildTiandituUrl(l.labelUrl, l.labelLayer), {
            maxZoom: 18,
            minZoom: 1
        }).addTo(mapInstanceRaw)
    }

    const switchLayer = (layerId: string) => {
        if (currentLayer.value === layerId) return
        currentLayer.value = layerId
        addLayer(layerId)
    }

    const toggleLabel = (visible: boolean) => {
        isLabelVisible = visible
        if (visible && !labelLayer) addLabelOverlay()
        else if (!visible && labelLayer) {
            mapInstanceRaw?.removeLayer(labelLayer);
            labelLayer = null
        }
    }

    // ── Focus area + mask ──
    const getMap = () => mapInstanceRaw

    const setFocusAreaLayer = (fl: L.GeoJSON | null) => {
        focusAreaLayer = fl
    }
    const getFocusAreaLayer = () => focusAreaLayer

    const fitToFocusArea = () => {
        if (!mapInstanceRaw) return
        if (focusAreaLayer && (focusAreaLayer as any).getBounds) {
            try {
                mapInstanceRaw.fitBounds((focusAreaLayer as any).getBounds(), {
                    padding: [20, 20],
                    animate: false,
                    maxZoom: 14
                });
                return
            } catch {
            }
        }
        mapInstanceRaw.setView([30.67, 104.06], 12)
    }

    const resetMapView = (point?: HazardPointInfo | null) => {
        if (point && mapInstanceRaw) {
            const center: [number, number] = [point.latitude, point.longitude]
            mapInstanceRaw.fitBounds(L.latLng(center).toBounds(1000), {padding: [40, 40], animate: true, maxZoom: 16})
        } else {
            fitToFocusArea()
        }
    }

    const focusOnHazardPoint = (point: HazardPointInfo) => {
        if (!mapInstanceRaw) return
        const bc = (point as any).boundaryCoords
        if (bc) {
            try {
                const obj = typeof bc === 'string' ? JSON.parse(bc) : bc
                if (obj.polygon?.length > 0) {
                    mapInstanceRaw.fitBounds(L.polygon(obj.polygon).getBounds(), {
                        padding: [30, 30],
                        animate: false,
                        maxZoom: 21
                    });
                    return
                }
            } catch {
            }
        }
        mapInstanceRaw.setView([point.latitude, point.longitude], 21)
    }

    // ── Mask ──
    const addMaskLayer = () => {
        if (!mapInstanceRaw) return
        if (maskLayer) {
            mapInstanceRaw.removeLayer(maskLayer);
            maskLayer = null
        }
        if (!focusAreaLayer) return
        const geojson: any = (focusAreaLayer as any)?.toGeoJSON?.()
        const features = geojson?.features || []
        if (!features.length) return
        const outerRing: [number, number][] = [[-180, -90], [180, -90], [180, 90], [-180, 90], [-180, -90]]
        const innerRings: [number, number][][] = []
        for (const f of features) {
            const geomType = f?.geometry?.type
            if (geomType === 'Polygon' && f.geometry.coordinates?.[0]?.length >= 3) innerRings.push(f.geometry.coordinates[0].map((c: number[]) => [c[0], c[1]] as [number, number]))
            else if (geomType === 'MultiPolygon') for (const polygon of f.geometry.coordinates || []) {
                if (polygon?.[0]?.length >= 3) innerRings.push(polygon[0].map((c: number[]) => [c[0], c[1]] as [number, number]))
            }
        }
        if (!innerRings.length) return
        const visible = showMaskLayer.value
        maskLayer = L.geoJSON({type: 'Polygon', coordinates: [outerRing, ...innerRings]} as any, {
            interactive: false,
            style: {fillColor: '#000000', fillOpacity: visible ? 0.35 : 0, color: 'transparent', weight: 0}
        }).addTo(mapInstanceRaw)
    }

    const toggleMaskLayer = () => {
        showMaskLayer.value = !showMaskLayer.value
        if (maskLayer) (maskLayer as any).setStyle({
            fillColor: '#000000',
            fillOpacity: showMaskLayer.value ? 0.35 : 0,
            color: 'transparent',
            weight: 0
        })
        if (focusAreaLayer) {
            (focusAreaLayer as any).setStyle(showMaskLayer.value ? {
                color: '#faad14',
                weight: 3,
                dashArray: '8 4'
            } : {color: 'transparent', weight: 0, dashArray: null})
        }
    }

    // ── Hazard point markers ──
    const addHazardPoints = (points: HazardPointInfo[], activeLayerKeys: Set<string>) => {
        if (!mapInstanceRaw) return
        if (hazardMarkerLayer) mapInstanceRaw.removeLayer(hazardMarkerLayer)
        hazardMarkerLayer = L.layerGroup().addTo(mapInstanceRaw)

        points.forEach(point => {
            const hasAlarm = !!point.alarmLevel
            const icon = L.icon({
                iconUrl: hasAlarm ? '/img/sy/auto_unnormal.png' : '/img/sy/auto_normal.png',
                iconSize: [32, 40],
                iconAnchor: [16, 40],
                popupAnchor: [0, -40]
            })
            const marker = L.marker([point.latitude, point.longitude], {icon}).addTo(hazardMarkerLayer!)
            hazardMarkerMap.set(point.id, marker)
            hazardPointDataMap.set(point.id, point)

            if (point.alarmLevel) startRipple(point.id)
            marker.bindPopup(buildHazardPopup(point), {maxWidth: 240, closeButton: false, autoClose: true})
            marker.on('click', () => opts.onMarkerClick(point))
            marker.on('dblclick', () => opts.onMarkerDblClick(point))
            marker.on('mouseover', () => marker.openPopup())
            marker.on('mouseout', () => marker.closePopup())
        })

        filterMarkers(activeLayerKeys)
    }

    const filterMarkers = (activeKeys: Set<string>) => {
        const activeGroups = new Set<number>()
        for (const key of activeKeys) {
            if (key.startsWith('group_')) activeGroups.add(Number(key.slice(6)))
        }
        const activeStatuses = new Set<string>()
        if (activeKeys.has('showMonitoring')) activeStatuses.add('MONITORING')
        if (activeKeys.has('showStopped')) activeStatuses.add('PAUSED')
        if (activeKeys.has('showCompleted')) activeStatuses.add('COMPLETED')
        const hasGroups = activeGroups.size > 0
        const hasStatuses = activeStatuses.size > 0
        hazardMarkerMap.forEach((marker, pointId) => {
            const data = hazardPointDataMap.get(pointId)
            if (!data) return
            const groupOk = hasGroups && activeGroups.has(data.groupId!) || !hasGroups
            const statusOk = hasStatuses && activeStatuses.has(data.status) || !hasStatuses
            const el = marker.getElement();
            if (el) el.style.display = (groupOk && statusOk) ? '' : 'none'
        })
    }

    const refreshHazardMarkers = (points: HazardPointInfo[], activeKeys: Set<string>) => {
        if (!mapInstanceRaw || !hazardMarkerLayer) return
        hazardMarkerLayer.clearLayers()
        addHazardPoints(points, activeKeys)
    }

    const startRipple = (pointId: number) => {
        if (!mapInstanceRaw) return
        const point = hazardPointDataMap.get(pointId)
        if (!point?.alarmLevel) return
        const color = ALARM_COLORS[point.alarmLevel]
        const center: [number, number] = [point.latitude, point.longitude]
        const circles: L.Circle[] = []
        for (let i = 0; i < 3; i++) {
            setTimeout(() => {
                const ripple = L.circle(center, {
                    radius: 10,
                    fillColor: color,
                    color,
                    weight: 2,
                    opacity: 0.8,
                    fillOpacity: 0.1
                }).addTo(mapInstanceRaw!)
                circles.push(ripple)
                setTimeout(() => ripple.setRadius(60).setStyle({opacity: 0, fillOpacity: 0}), 50)
                setTimeout(() => {
                    if (mapInstanceRaw) {
                        mapInstanceRaw.removeLayer(ripple);
                        circles.splice(circles.indexOf(ripple), 1)
                    }
                }, 2500)
            }, i * 1500)
        }
        ripples.set(pointId, circles)
        setTimeout(() => {
            if (ripples.has(pointId)) startRipple(pointId)
        }, 4500)
    }

    // ── Hazard view ──
    const showHazardOnMap = async (point: HazardPointInfo) => {
        if (!mapInstanceRaw) return
        if (hazardMarkerLayer) mapInstanceRaw.removeLayer(hazardMarkerLayer)
        hazardMarkerLayer = L.layerGroup().addTo(mapInstanceRaw)
        const bc: any = (point as any).boundaryCoords
        let hasBoundary = false
        if (bc) {
            try {
                const obj = typeof bc === 'string' ? JSON.parse(bc) : bc
                if (obj.polygon?.length > 0) {
                    L.polygon(obj.polygon, {
                        color: '#1890ff',
                        fillColor: '#1890ff',
                        fillOpacity: 0.15,
                        weight: 2
                    }).addTo(hazardMarkerLayer);
                    hasBoundary = true
                }
                if (obj.strikeCoords?.length >= 2) L.polyline(obj.strikeCoords, {
                    color: '#f56c6c',
                    weight: 3,
                    dashArray: '6 6'
                }).addTo(hazardMarkerLayer)
            } catch {
            }
        }
        if (!hasBoundary) L.circle([point.latitude, point.longitude], {
            radius: 500,
            color: '#f5222d',
            fillColor: '#f5222d',
            fillOpacity: 0.1,
            weight: 2,
            dashArray: '8,4'
        }).addTo(hazardMarkerLayer)
        try {
            const res = await getBoundDevices(String(point.id))
            if (res.code === 200 && res.data) {
                (res.data as any[]).forEach((item: any) => {
                    const device: DeviceInfo = {
                        id: item.deviceId,
                        name: item.deviceName || '未知设备',
                        type: (item.sensors?.[0]?.name || 'DEVICE').toUpperCase(),
                        typeName: item.sensors?.[0]?.name || '设备',
                        status: item.deviceStatus === 0 ? 'online' : item.deviceStatus === 1 ? 'warning' : 'offline',
                        sensorCount: item.sensors?.length || 0,
                        longitude: item.installLongitude ?? point.longitude,
                        latitude: item.installLatitude ?? point.latitude
                    }
                    L.marker([device.latitude, device.longitude], {icon: createDeviceIcon(device.status)}).addTo(hazardMarkerLayer!).bindPopup(buildDevicePopup(device))
                })
            }
        } catch { /* ignore */
        }
    }

    const renderHazardBoundary = (point: HazardPointInfo) => {
        if (!mapInstanceRaw) return
        if (!hazardBoundaryLayer) hazardBoundaryLayer = L.layerGroup().addTo(mapInstanceRaw)
        else hazardBoundaryLayer.clearLayers()
        const bc: any = (point as any).boundaryCoords
        if (bc) {
            try {
                const obj = typeof bc === 'string' ? JSON.parse(bc) : bc
                if (obj.polygon?.length > 0) L.polygon(obj.polygon, {
                    color: '#1890ff',
                    fillColor: '#1890ff',
                    fillOpacity: 0.15,
                    weight: 2
                }).addTo(hazardBoundaryLayer)
                if (obj.strikeCoords?.length >= 2) L.polyline(obj.strikeCoords, {
                    color: '#f56c6c',
                    weight: 3,
                    dashArray: '6 6'
                }).addTo(hazardBoundaryLayer)
            } catch {
            }
        }
    }

    const clearHazardBoundary = () => {
        hazardBoundaryLayer?.clearLayers()
    }

    const clearHazardMarkers = () => {
        if (hazardMarkerLayer) {
            hazardMarkerLayer.clearLayers()
        }
    }

    // ── Cleanup ──
    const destroy = () => {
        if (mapInstanceRaw) {
            mapInstanceRaw.remove();
            mapInstanceRaw = null;
            mapInstance.value = null
        }
        baseLayer = null;
        labelLayer = null;
        maskLayer = null;
        focusAreaLayer = null;
        hazardMarkerLayer = null;
        hazardBoundaryLayer = null
        hazardMarkerMap.clear();
        hazardPointDataMap.clear();
        ripples.clear()
    }

    return {
        mapInstance, currentLayer, showMaskLayer,
        getMap, getFocusAreaLayer, setFocusAreaLayer,
        getSavedMapView, setSavedMapView,
        hazardPointDataMap,
        initMap, handleResize, destroy,
        addLayer, switchLayer, toggleLabel,
        fitToFocusArea, resetMapView, focusOnHazardPoint,
        addMaskLayer, toggleMaskLayer,
        addHazardPoints, filterMarkers, refreshHazardMarkers,
        showHazardOnMap, renderHazardBoundary, clearHazardBoundary, clearHazardMarkers,
    }
}
