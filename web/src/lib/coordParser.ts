import type {LatLng} from './boundaryCoords'

export interface ParseMultilineResult {
  coords: LatLng[]
  errors: Array<{ line: number; raw: string; reason: string }>
}

function isLat(n: number): boolean { return n >= -90 && n <= 90 }
function isLng(n: number): boolean { return n >= -180 && n <= 180 }

/**
 * 智能识别 (a, b) 的 lat/lng 顺序。
 * 若 a 可当 lat 且 b 可当 lng → 保持顺序。
 * 若 a 和 b 都在合法区间但顺序反了（中国典型：lat 25-45, lng 73-135）→ 翻转。
 */
function smartParse(a: number, b: number): LatLng | null {
  if (isLat(a) && isLng(b)) {
    if (isLng(a) && isLat(b) && b > 25 && b < 45 && a > 73 && a < 135) {
      return { lat: b, lng: a }
    }
    return { lat: a, lng: b }
  }
  if (isLng(a) && !isLat(a) && isLat(b)) return { lat: b, lng: a }
  return null
}

const SEPARATOR_RE = /[\s,;]+/
const PARENS_RE = /^\((.*)\)$/

function splitPair(text: string): [string, string] | null {
  const cleaned = text.trim()
  const parenMatch = PARENS_RE.exec(cleaned)
  const src = parenMatch ? parenMatch[1] : cleaned
  const parts = src.split(SEPARATOR_RE).filter(Boolean)
  return parts.length === 2 ? [parts[0], parts[1]] : null
}

/** 解析单行 "lat,lng" / "lng,lat" / "lat lng" */
export function parseSingle(text: string): LatLng | null {
  const parts = splitPair(text)
  if (!parts) return null
  const a = Number(parts[0])
  const b = Number(parts[1])
  if (Number.isNaN(a) || Number.isNaN(b)) return null
  return smartParse(a, b)
}

function isTableHeader(line: string): boolean {
  const lower = line.trim().toLowerCase()
  return /^(lat|lng|latitude|longitude|纬度|经度|坐标)/.test(lower)
}

function isCommentOrEmpty(line: string): boolean {
  const trimmed = line.trim()
  return trimmed === '' || trimmed.startsWith('#')
}

/** 解析多行文本（自动识别分隔符 / 表头 / 注释 / 空行） */
export function parseMultiline(text: string): ParseMultilineResult {
  const rawLines = text.split(/\r?\n/)
  const coords: LatLng[] = []
  const errors: ParseMultilineResult['errors'] = []

  let lineNum = 0
  for (const raw of rawLines) {
    lineNum++
    if (isCommentOrEmpty(raw)) continue
    if (isTableHeader(raw)) continue

    const result = parseSingle(raw)
    if (result) {
      coords.push(result)
    } else {
      errors.push({ line: lineNum, raw: raw.trim(), reason: '无法解析为 lat,lng' })
    }
  }
  return { coords, errors }
}

// ── 度分秒 (DMS) ↔ 十进制 (Decimal) 互转(用于地图选点弹窗的智能输入) ──

/** 单段度分秒块:度 ° 分 ' 秒 " [方向后缀] */
const DMS_BLOCK_RE = /(-?\d+(?:\.\d+)?)\s*°\s*(\d+(?:\.\d+)?)\s*['′]\s*(\d+(?:\.\d+)?)\s*(?:["″])?\s*([NSEWnsew])?/g

/** 把两个带方向/无方向的数值按方向与范围归位为 {lng, lat} */
function resolveLatLng(a: { value: number, dir: string }, b: { value: number, dir: string }): LatLng | null {
    const aIsLat = a.dir === 'N' || a.dir === 'S'
    const aIsLng = a.dir === 'E' || a.dir === 'W'
    const bIsLat = b.dir === 'N' || b.dir === 'S'
    const bIsLng = b.dir === 'E' || b.dir === 'W'
    if (aIsLat && bIsLng) {
        if (Math.abs(a.value) > 90 || Math.abs(b.value) > 180) return null
        return {lat: a.value, lng: b.value}
    }
    if (aIsLng && bIsLat) {
        if (Math.abs(a.value) > 180 || Math.abs(b.value) > 90) return null
        return {lng: a.value, lat: b.value}
    }
    // 无方向:按值范围归位
    if (Math.abs(a.value) <= 90 && Math.abs(b.value) <= 180) return {lat: a.value, lng: b.value}
    if (Math.abs(a.value) <= 180 && Math.abs(b.value) <= 90) return {lng: a.value, lat: b.value}
    return null
}

/**
 * 把一对"经度,纬度"坐标文本解析成 {lat, lng}。支持两种格式混在一行:
 *   - 纯十进制: "104.063456, 30.671234" 或 "104.063456 30.671234"
 *   - 度分秒对: "104°03'48.44\"E 30°40'16.44\"N"
 *   - 混合:     "104.063456° 30.671234°"(按范围智能归位)
 * 解析失败返回 null
 */
export function parseLatLngPair(text: string): LatLng | null {
    const raw = text.trim()
    if (!raw) return null

    // 1) 优先尝试 DMS 对(用全局正则匹配两次)
    DMS_BLOCK_RE.lastIndex = 0
    const dmsMatches: { value: number, dir: string }[] = []
    let m: RegExpExecArray | null
    while ((m = DMS_BLOCK_RE.exec(raw)) !== null) {
        const deg = Number(m[1])
        const min = Number(m[2])
        const sec = Number(m[3])
        const dir = (m[4] || '').toUpperCase()
        let v = Math.abs(deg) + min / 60 + sec / 3600
        if (dir === 'S' || dir === 'W') v = -v
        dmsMatches.push({value: v, dir})
        if (dmsMatches.length === 2) break
    }
    if (dmsMatches.length === 2) {
        return resolveLatLng(dmsMatches[0], dmsMatches[1])
    }

    // 2) 退回到十进制解析(支持逗号/空格/分号)
    const parts = raw.split(/[,，;\s]+/).filter(Boolean)
    if (parts.length >= 2) {
        const a = Number(parts[0])
        const b = Number(parts[1])
        if (isNaN(a) || isNaN(b)) return null
        // 智能识别经纬度顺序(参见 parseSingle)
        if (isLat(a) && isLng(b)) {
            if (isLng(a) && isLat(b) && b > 25 && b < 45 && a > 73 && a < 135) {
                return {lat: b, lng: a}
            }
            return {lat: a, lng: b}
        }
        if (isLng(a) && !isLat(a) && isLat(b)) return {lat: b, lng: a}
    }
    return null
}
