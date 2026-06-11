/** 地理坐标点（内存中统一用对象形式） */
export interface LatLng {
  lat: number
  lng: number
}

/** 隐患点边界 + 走向（内存格式） */
export interface BoundaryCoords {
  polygon: LatLng[]
  strikeLine: [LatLng, LatLng] | null
  auxiliaryLines: LatLng[][]
}

export const EMPTY_BOUNDARY: BoundaryCoords = {
  polygon: [],
  strikeLine: null,
  auxiliaryLines: []
}

type SerializableVertex = [number, number]
type SerializablePolyline = SerializableVertex[]
type SerializableAux = SerializablePolyline[]

interface SerializableBoundary {
  polygon: SerializablePolyline
  strikeLine?: SerializablePolyline | null
  strikeCoords?: SerializablePolyline | null
  strikeAngle?: number | null
  auxiliaryLines?: SerializableAux
}

function toSerializableVertex(p: LatLng): SerializableVertex {
  return [p.lat, p.lng]
}

function fromSerializableVertex(arr: SerializableVertex): LatLng {
  return { lat: arr[0], lng: arr[1] }
}

function isSerializableVertex(v: unknown): v is SerializableVertex {
  return Array.isArray(v) && v.length === 2 && typeof v[0] === 'number' && typeof v[1] === 'number'
}

/** 序列化：内存 {lat,lng} → wire [lat,lng] 数组 */
export function serialize(b: BoundaryCoords): string {
  const wire: SerializableBoundary = {
    polygon: b.polygon.map(toSerializableVertex),
    strikeLine: b.strikeLine?.map(toSerializableVertex) as SerializablePolyline | undefined ?? null,
    auxiliaryLines: b.auxiliaryLines.map(line => line.map(toSerializableVertex))
  }
  return JSON.stringify(wire)
}

/** 反序列化：兼容旧 key (strikeCoords → strikeLine，strikeAngle 弃) */
export function deserialize(json: string | null | undefined): BoundaryCoords {
  if (!json) return { polygon: [], strikeLine: null, auxiliaryLines: [] }
  let obj: any
  try {
    obj = JSON.parse(json)
  } catch {
    return { polygon: [], strikeLine: null, auxiliaryLines: [] }
  }
  if (!obj || typeof obj !== 'object') return { polygon: [], strikeLine: null, auxiliaryLines: [] }

  const polygon = parsePolygon(obj.polygon)
  const strikeLine = parseStrikeLine(obj)
  const auxiliaryLines = parseAuxiliaryLines(obj.auxiliaryLines)

  return { polygon, strikeLine, auxiliaryLines }
}

function parsePolygon(raw: unknown): LatLng[] {
  if (!Array.isArray(raw)) return []
  return raw.filter(isSerializableVertex).map(fromSerializableVertex)
}

function parseStrikeLine(obj: Record<string, unknown>): [LatLng, LatLng] | null {
  if (Array.isArray(obj.strikeLine) && obj.strikeLine.length >= 2) {
    const pts = obj.strikeLine.slice(0, 2).filter(isSerializableVertex)
    if (pts.length === 2) return [fromSerializableVertex(pts[0]), fromSerializableVertex(pts[1])]
  }
  if (Array.isArray(obj.strikeCoords) && obj.strikeCoords.length >= 2) {
    const pts = obj.strikeCoords.slice(0, 2).filter(isSerializableVertex)
    if (pts.length === 2) return [fromSerializableVertex(pts[0]), fromSerializableVertex(pts[1])]
  }
  return null
}

function parseAuxiliaryLines(raw: unknown): LatLng[][] {
  if (!Array.isArray(raw)) return []
  return raw
    .filter(Array.isArray)
    .map((line: unknown) =>
      Array.isArray(line)
        ? line.filter(isSerializableVertex).map(fromSerializableVertex)
        : []
    )
    .filter(line => line.length >= 2)
}

/** 多边形质心（算法 b：带符号面积加权）。顶点数 < 3 返回 null */
export function centroid(polygon: LatLng[]): LatLng | null {
  if (polygon.length < 3) return null
  let signedArea = 0
  let cx = 0
  let cy = 0
  const n = polygon.length
  for (let i = 0; i < n; i++) {
    const a = polygon[i]
    const b = polygon[(i + 1) % n]
    const area = a.lng * b.lat - b.lng * a.lat
    signedArea += area
    cx += (a.lng + b.lng) * area
    cy += (a.lat + b.lat) * area
  }
  if (Math.abs(signedArea) < 1e-12) return null
  const factor = 1 / (3 * signedArea)
  return { lat: cy * factor, lng: cx * factor }
}

const DEG_TO_RAD = Math.PI / 180
const RAD_TO_DEG = 180 / Math.PI

/** 球面方位角（degrees，0=正北，顺时针）。同点返回 0 */
export function strikeAngle(line: [LatLng, LatLng]): number {
  const [a, b] = line
  const dLng = (b.lng - a.lng) * DEG_TO_RAD
  const lat1 = a.lat * DEG_TO_RAD
  const lat2 = b.lat * DEG_TO_RAD

  const y = Math.sin(dLng) * Math.cos(lat2)
  const x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)

  if (Math.abs(x) < 1e-12 && Math.abs(y) < 1e-12) return 0
  const deg = Math.atan2(y, x) * RAD_TO_DEG
  return (deg + 360) % 360
}

/**
 * 校验 boundary 整体是否合法。返回 null 表示合法, 否则返回中文化的错误描述。
 * 规则:
 *   - polygon 顶点数 == 0 或 >= 3
 *   - 每条辅助线顶点数 >= 2
 *   - strikeLine 不参与必填校验 (可选)
 */
export function assertValidBoundary(b: BoundaryCoords): string | null {
  if (b.polygon.length !== 0 && b.polygon.length < 3) {
    return '多边形至少需要 3 个顶点 (当前 ' + b.polygon.length + ' 个)'
  }
  for (let i = 0; i < b.auxiliaryLines.length; i++) {
    if (b.auxiliaryLines[i].length < 2) {
      return '第 ' + (i + 1) + ' 条辅助线至少需要 2 个点 (当前 ' + b.auxiliaryLines[i].length + ' 个)'
    }
  }
  return null
}
