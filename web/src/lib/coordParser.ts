import type { LatLng } from './boundaryCoords'

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
