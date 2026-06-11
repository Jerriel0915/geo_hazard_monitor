import type { LatLng } from './boundaryCoords'

/** 两点之间的中点 */
export function midpoint(a: LatLng, b: LatLng): LatLng {
  return { lat: (a.lat + b.lat) / 2, lng: (a.lng + b.lng) / 2 }
}

/** 多边形第 i 条边的中点。i=0 取 poly[0]→poly[1]，最后一边取 poly[n-1]→poly[0] */
export function edgeMidpoint(polygon: LatLng[], i: number): LatLng {
  const n = polygon.length
  if (n === 0) throw new Error('edgeMidpoint: empty polygon')
  const a = polygon[i]
  const b = polygon[(i + 1) % n]
  return midpoint(a, b)
}

/** 多边形第 i 条边的两端点 */
export function edgeVertices(polygon: LatLng[], i: number): [LatLng, LatLng] {
  const n = polygon.length
  if (n === 0) throw new Error('edgeVertices: empty polygon')
  return [polygon[i], polygon[(i + 1) % n]]
}

const EARTH_RADIUS_M = 6_378_137
const DEG_TO_RAD = Math.PI / 180

/** 在给定 lat/zoom 下, 一个屏幕像素对应多少米 (Web Mercator) */
export function metersPerPixel(lat: number, zoom: number): number {
  return (2 * EARTH_RADIUS_M * Math.PI * Math.cos(lat * DEG_TO_RAD)) / (256 * Math.pow(2, zoom))
}

/** Haversine 距离 (米) */
function haversineMeters(a: LatLng, b: LatLng): number {
  const dLat = (b.lat - a.lat) * DEG_TO_RAD
  const dLng = (b.lng - a.lng) * DEG_TO_RAD
  const sa = Math.sin(dLat / 2) ** 2 +
    Math.cos(a.lat * DEG_TO_RAD) * Math.cos(b.lat * DEG_TO_RAD) * Math.sin(dLng / 2) ** 2
  return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(sa))
}

/** 鼠标 p 是否在 target 顶点的 tolerance 米内 */
export function hitVertex(p: LatLng, target: LatLng, toleranceMeters: number): boolean {
  return haversineMeters(p, target) <= toleranceMeters
}

/** 点 p 到线段 ab 的最短距离 (米) */
function pointToSegmentMeters(p: LatLng, a: LatLng, b: LatLng): number {
  // 平面近似: 把 lat/lng 当笛卡尔, 然后用 Haversine 修正
  // 对短距离 (< 1km) 足够精确
  const ax = a.lng, ay = a.lat
  const bx = b.lng, by = b.lat
  const px = p.lng, py = p.lat
  const dx = bx - ax, dy = by - ay
  const len2 = dx * dx + dy * dy
  if (len2 === 0) return haversineMeters(p, a)
  let t = ((px - ax) * dx + (py - ay) * dy) / len2
  t = Math.max(0, Math.min(1, t))
  const proj: LatLng = { lng: ax + t * dx, lat: ay + t * dy }
  return haversineMeters(p, proj)
}

/** 鼠标 p 是否在多边形第 i 条边的 tolerance 米内 */
export function hitEdge(p: LatLng, polygon: LatLng[], i: number, toleranceMeters: number): boolean {
  const [a, b] = edgeVertices(polygon, i)
  return pointToSegmentMeters(p, a, b) <= toleranceMeters
}

/** 鼠标 p 是否在折线 line 上某段的 tolerance 米内 */
export function hitPolyline(p: LatLng, line: LatLng[], toleranceMeters: number): boolean {
  for (let i = 0; i < line.length - 1; i++) {
    if (pointToSegmentMeters(p, line[i], line[i + 1]) <= toleranceMeters) return true
  }
  return false
}

/** 在第 i 条边之后插入新点。返回新数组 (不修改原数组) */
export function insertVertexAtEdge(polygon: LatLng[], edgeIndex: number, p: LatLng): LatLng[] {
  const n = polygon.length
  if (n === 0) throw new Error('insertVertexAtEdge: empty polygon')
  const insertAt = (edgeIndex + 1) % (n + 1) // 在 edgeIndex 边后, 即索引 (edgeIndex+1) 处
  return [...polygon.slice(0, insertAt), p, ...polygon.slice(insertAt)]
}

/** 删除第 i 个顶点。剩余 < 3 时返回 null。返回新数组 (不修改原数组) */
export function removeVertexSafe(polygon: LatLng[], index: number): LatLng[] | null {
  if (polygon.length <= 3) return null
  return [...polygon.slice(0, index), ...polygon.slice(index + 1)]
}

/** 线段 ab 与线段 cd 是否相交 (含共线/共端点视为 false — 邻接边允许共端点) */
function segmentsIntersect(a: LatLng, b: LatLng, c: LatLng, d: LatLng): boolean {
  // 共端点不算相交 (邻接边会共享顶点)
  const eq = (p: LatLng, q: LatLng) => p.lat === q.lat && p.lng === q.lng
  if (eq(a, c) || eq(a, d) || eq(b, c) || eq(b, d)) return false

  const cross = (o: LatLng, p: LatLng, q: LatLng) =>
    (p.lng - o.lng) * (q.lat - o.lat) - (p.lat - o.lat) * (q.lng - o.lng)
  const d1 = cross(c, d, a)
  const d2 = cross(c, d, b)
  const d3 = cross(a, b, c)
  const d4 = cross(a, b, d)
  return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
         ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
}

/** 多边形任意两条非邻接边是否相交 */
export function isSelfIntersecting(polygon: LatLng[]): boolean {
  const n = polygon.length
  if (n < 4) return false
  for (let i = 0; i < n; i++) {
    const a = polygon[i]
    const b = polygon[(i + 1) % n]
    // 跳过邻接边 (i-1 和 i+1) — 它们共享端点
    for (let j = i + 2; j < n; j++) {
      // j === i+1: 邻接; j === n-1 && i === 0: 邻接 (环); j === 0 && i === n-1: 邻接
      if (i === 0 && j === n - 1) continue
      const c = polygon[j]
      const d = polygon[(j + 1) % n]
      if (segmentsIntersect(a, b, c, d)) return true
    }
  }
  return false
}
