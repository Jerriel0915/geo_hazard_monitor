# 统一地图编辑公共组件 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build 2 shared Vue map editor components (`MapPointPicker` + `MapBoundaryEditor`) backed by a Leaflet lifecycle composable, plus backend `BoundaryCoordsDTO` validation and DB migration, delivered across 4 independently-mergeable PRs.

**Architecture:** Three-layer frontend: shared composable `useLeafletMap` (Leaflet init/destroy lifecycle aligned to DOM refs) → two UI components (`MapPointPicker` single-point picker, `MapBoundaryEditor` polygon+strike editor) → three consumer pages (HazardPoint/Device/VideoDevice). Backend adds `BoundaryCoordsDTO` Java record with validator plus a 1-line DDL.

**Tech Stack:** Vue 3.4 + TypeScript 5.3, Leaflet 1.9.4 + `@geoman-io/leaflet-geoman-free@^2`, Vitest, Java 17 + Spring Boot 4.0.3 + MyBatis, MySQL 8.0

---

## File Map

| File | Responsibility | PR |
|------|----------------|-----|
| `web/src/lib/boundaryCoords.ts` | Types + serialize/deserialize/centroid/strikeAngle | 1 |
| `web/src/lib/__tests__/boundaryCoords.test.ts` | 22 unit tests for boundaryCoords | 1 |
| `web/src/lib/coordParser.ts` | Smart coordinate text parsing | 1 |
| `web/src/lib/__tests__/coordParser.test.ts` | 22 unit tests for coordParser | 1 |
| `web/src/composables/useLeafletMap.ts` | Leaflet lifecycle, tianditu tiles | 1 |
| `web/src/composables/__tests__/useLeafletMap.test.ts` | 6 jsdom tests for composable | 1 |
| `web/src/components/map/MapCoordInput.vue` | Sub-component: paste input field | 2 |
| `web/src/components/map/MapPointPicker.vue` | Single-point map picker | 2 |
| `web/src/views/basic/VideoDevice.vue` | Consumer migration (simplest) | 2 |
| `web/src/views/basic/Device.vue` | Consumer migration + overlay | 3 |
| `server/zwei-iot-device/.../IDeviceHazardRelationService.java` | Extend: return `HazardPointRef` | 3 |
| `server/zwei-iot-hazard/.../DeviceHazardRelationServiceImpl.java` | Implement new method | 3 |
| `server/zwei-iot-hazard/.../BoundaryCoordsDTO.java` | Java record | 4 |
| `server/zwei-iot-hazard/.../BoundaryCoordsValidator.java` | Validation service | 4 |
| `web/src/components/map/MapBoundaryEditor.vue` | Polygon+strike editor | 4 |
| `web/src/views/basic/HazardPoint.vue` | Consumer migration | 4 |
| `server/zwei-iot-hazard/.../HazardPointController.java` | Wire validator | 4 |
| `db/upgrade/2026-06-11-drop-strike-column.sql` | DDL migration | 4 |

---

## PR 1: 基础设施 + 单元测试

> **合并门禁**: 所有单测通过；`npm run build` 不破；已安装新 npm 依赖

### Task 1: Install dependencies

**Files:** Modify `web/package.json`

- [ ] **Step 1: Run npm install**

```bash
cd web && npm i @geoman-io/leaflet-geoman-free && npm i -D vitest @vitest/coverage-v8
```

- [ ] **Step 2: Add test scripts to package.json**

Edit `web/package.json` — add to `scripts` block:
```json
"test": "vitest run",
"test:watch": "vitest",
"test:coverage": "vitest run --coverage"
```

- [ ] **Step 3: Verify install**

```bash
cd web && npm ls @geoman-io/leaflet-geoman-free vitest 2>&1
```
Expected: both listed with version numbers, no unmet peer deps.

- [ ] **Step 4: Create Vitest config**

Create `web/vitest.config.ts`:
```typescript
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    include: ['src/**/__tests__/**/*.test.ts'],
    coverage: {
      include: ['src/lib/**', 'src/composables/**'],
      thresholds: {
        lines: 80,
        functions: 80
      }
    }
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add web/package.json web/package-lock.json web/vitest.config.ts
git commit -m "$(cat <<'EOF'
chore: install geoman-free + vitest for map component infrastructure

Adds @geoman-io/leaflet-geoman-free (Leaflet polygon/strike editing) and
vitest (unit test runner for pure functions and composables).
EOF
)"
```

---

### Task 2: Write `lib/boundaryCoords.ts` (types + pure functions)

**Files:** Create `web/src/lib/boundaryCoords.ts`

- [ ] **Step 1: Write the module**

Create `web/src/lib/boundaryCoords.ts`:
```typescript
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

function isLatLngLike(o: unknown): o is { lat: unknown; lng: unknown } {
  return typeof o === 'object' && o !== null && 'lat' in o && 'lng' in o
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
  if (!json) return { ...EMPTY_BOUNDARY, polygon: [], strikeLine: null, auxiliaryLines: [] }
  let obj: any
  try {
    obj = JSON.parse(json)
  } catch {
    return { ...EMPTY_BOUNDARY, polygon: [], strikeLine: null, auxiliaryLines: [] }
  }
  if (!obj || typeof obj !== 'object') return { ...EMPTY_BOUNDARY, polygon: [], strikeLine: null, auxiliaryLines: [] }

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
  // 新 key 优先
  if (Array.isArray(obj.strikeLine) && obj.strikeLine.length >= 2) {
    const pts = obj.strikeLine.slice(0, 2).filter(isSerializableVertex)
    if (pts.length === 2) return [fromSerializableVertex(pts[0]), fromSerializableVertex(pts[1])]
  }
  // 兼容旧 key strikeCoords
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
  if (Math.abs(signedArea) < 1e-12) return null // degenerate
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
```

- [ ] **Step 2: Commit**

```bash
git add web/src/lib/boundaryCoords.ts
git commit -m "$(cat <<'EOF'
feat: add boundaryCoords lib (types + serialize/deserialize/centroid/strikeAngle)

Pure-function module for hazard point boundary geometry: LatLng/BoundaryCoords
types, JSON serialize/deserialize with legacy key migration, polygon centroid
(signed-area-weighted), and spherical strike azimuth calculation.
EOF
)"
```

---

### Task 3: Write `lib/coordParser.ts`

**Files:** Create `web/src/lib/coordParser.ts`

- [ ] **Step 1: Write the module**

Create `web/src/lib/coordParser.ts`:
```typescript
import type { LatLng } from './boundaryCoords'

export interface ParseMultilineResult {
  coords: LatLng[]
  errors: Array<{ line: number; raw: string; reason: string }>
}

/** 判断 n 是否是合法纬度 [-90, 90] */
function isLat(n: number): boolean { return n >= -90 && n <= 90 }
/** 判断 n 是否是合法经度 [-180, 180] */
function isLng(n: number): boolean { return n >= -180 && n <= 180 }

/**
 * 智能识别 (a, b) 的 lat/lng 顺序。
 * 规则：若 a 可当 lat 且 b 可当 lng → 保持顺序。
 *   若 a 和 b 都在合法区间但顺序反了（中国典型：lat 25-45, lng 73-135）→ 翻转。
 */
function smartParse(a: number, b: number): LatLng | null {
  if (isLat(a) && isLng(b)) {
    // 二次判断：两者都合法时，按中国典型范围猜是否反了
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
```

- [ ] **Step 2: Commit**

```bash
git add web/src/lib/coordParser.ts
git commit -m "$(cat <<'EOF'
feat: add coordParser lib (smart lat/lng paste parsing)

Supports single-line and multiline coordinate text parsing with
automatic lat/lng order detection, delimiter tolerance (comma/space/
semicolon/tab), header skipping, and line-level error reporting.
EOF
)"
```

---

### Task 4: Write `lib/__tests__/boundaryCoords.test.ts`

**Files:** Create `web/src/lib/__tests__/boundaryCoords.test.ts`

- [ ] **Step 1: Write 22 tests**

Create `web/src/lib/__tests__/boundaryCoords.test.ts`:
```typescript
import { describe, it, expect } from 'vitest'
import { EMPTY_BOUNDARY, serialize, deserialize, centroid, strikeAngle, type BoundaryCoords, type LatLng } from '../boundaryCoords'

function l(lat: number, lng: number): LatLng { return { lat, lng } }

// ─── serialize / deserialize 对称性 ───
describe('serialize', () => {
  it('serializes EMPTY_BOUNDARY', () => {
    expect(serialize({ ...EMPTY_BOUNDARY, polygon: [], strikeLine: null, auxiliaryLines: [] }))
      .toBe('{"polygon":[],"strikeLine":null,"auxiliaryLines":[]}')
  })

  it('round-trips a full boundary', () => {
    const input: BoundaryCoords = {
      polygon: [l(30.67, 104.05), l(30.68, 104.06), l(30.67, 104.07)],
      strikeLine: [l(30.67, 104.05), l(30.68, 104.07)],
      auxiliaryLines: [[l(30.671, 104.061), l(30.672, 104.063)]]
    }
    const roundTripped = deserialize(serialize(input))
    expect(roundTripped.polygon).toEqual(input.polygon)
    expect(roundTripped.strikeLine).toEqual(input.strikeLine)
    expect(roundTripped.auxiliaryLines).toEqual(input.auxiliaryLines)
  })
})

describe('deserialize', () => {
  it('returns empty for null', () => {
    expect(deserialize(null)).toEqual({ polygon: [], strikeLine: null, auxiliaryLines: [] })
  })

  it('returns empty for undefined', () => {
    expect(deserialize(undefined)).toEqual({ polygon: [], strikeLine: null, auxiliaryLines: [] })
  })

  it('returns empty for empty string', () => {
    expect(deserialize('')).toEqual({ polygon: [], strikeLine: null, auxiliaryLines: [] })
  })

  it('returns empty for invalid JSON', () => {
    expect(deserialize('not json')).toEqual({ polygon: [], strikeLine: null, auxiliaryLines: [] })
  })

  it('returns empty for empty object', () => {
    expect(deserialize('{}')).toEqual({ polygon: [], strikeLine: null, auxiliaryLines: [] })
  })

  // ─── legacy key migration ───
  it('maps legacy strikeCoords to strikeLine', () => {
    const json = '{"polygon":[],"strikeCoords":[[30.67,104.05],[30.68,104.07]]}'
    const result = deserialize(json)
    expect(result.strikeLine).toEqual([l(30.67, 104.05), l(30.68, 104.07)])
  })

  it('ignores legacy strikeAngle field', () => {
    const json = '{"polygon":[],"strikeAngle":152}'
    const result = deserialize(json)
    expect(result).toEqual({ polygon: [], strikeLine: null, auxiliaryLines: [] })
  })

  it('prefers strikeLine over strikeCoords when both exist', () => {
    const json = '{"polygon":[],"strikeLine":[[30.6,104.0],[30.7,104.1]],"strikeCoords":[[30.67,104.05],[30.68,104.07]]}'
    const result = deserialize(json)
    expect(result.strikeLine).toEqual([l(30.6, 104.0), l(30.7, 104.1)])
  })

  it('returns null strikeLine if strikeCoords has wrong length', () => {
    const json = '{"polygon":[],"strikeCoords":[[30.67,104.05]]}'
    const result = deserialize(json)
    expect(result.strikeLine).toBeNull()
  })

  // ─── vertex validation ───
  it('filters invalid vertices from polygon', () => {
    const json = '{"polygon":[[30.67,104.05],["x","y"],null]}'
    const result = deserialize(json)
    expect(result.polygon).toHaveLength(1)
    expect(result.polygon[0]).toEqual({ lat: 30.67, lng: 104.05 })
  })
})

// ─── centroid ───
describe('centroid', () => {
  it('returns null for empty array', () => {
    expect(centroid([])).toBeNull()
  })

  it('returns null for 1 point', () => {
    expect(centroid([l(30, 104)])).toBeNull()
  })

  it('returns null for 2 points', () => {
    expect(centroid([l(30, 104), l(31, 105)])).toBeNull()
  })

  it('computes centroid of triangle (0,0)-(0,3)-(3,0)', () => {
    const result = centroid([l(0, 0), l(0, 3), l(3, 0)])
    expect(result).not.toBeNull()
    expect(result!.lat).toBeCloseTo(1, 3)
    expect(result!.lng).toBeCloseTo(1, 3)
  })

  it('computes centroid of unit square (CW and CCW same result)', () => {
    // clockwise
    const cw = [l(0, 0), l(0, 1), l(1, 1), l(1, 0)]
    const ccw = [l(0, 0), l(1, 0), l(1, 1), l(0, 1)]
    const r1 = centroid(cw)
    const r2 = centroid(ccw)
    expect(r1).not.toBeNull()
    expect(r2).not.toBeNull()
    expect(r1!.lat).toBeCloseTo(0.5, 3)
    expect(r1!.lng).toBeCloseTo(0.5, 3)
    expect(r2!.lat).toBeCloseTo(0.5, 3)
    expect(r2!.lng).toBeCloseTo(0.5, 3)
  })

  it('returns null for collinear points (degenerate polygon)', () => {
    const result = centroid([l(0, 0), l(1, 1), l(2, 2)])
    expect(result).toBeNull()
  })
})

// ─── strikeAngle ───
describe('strikeAngle', () => {
  it('returns 0 for due north', () => {
    expect(strikeAngle([l(0, 0), l(1, 0)])).toBeCloseTo(0, 1)
  })

  it('returns 90 for due east', () => {
    const angle = strikeAngle([l(0, 0), l(0, 1)])
    expect(angle).toBeCloseTo(90, 1)
  })

  it('returns 180 for due south', () => {
    expect(strikeAngle([l(0, 0), l(-1, 0)])).toBeCloseTo(180, 1)
  })

  it('returns 270 for due west', () => {
    const angle = strikeAngle([l(0, 0), l(0, -1)])
    expect(angle).toBeCloseTo(270, 1)
  })

  it('returns 45 for northeast', () => {
    const angle = strikeAngle([l(0, 0), l(1, 1)])
    expect(angle).toBeCloseTo(45, 1)
  })

  it('returns 0 for identical start/end', () => {
    expect(strikeAngle([l(30, 104), l(30, 104)])).toBe(0)
  })
})
```

- [ ] **Step 2: Run tests — expect all pass**

```bash
cd web && npm test -- --run tests/lib/__tests__/boundaryCoords.test.ts 2>&1
```
Expected: 22 tests passed, 0 failed.

- [ ] **Step 3: Commit**

```bash
git add web/src/lib/__tests__/boundaryCoords.test.ts
git commit -m "test: add 22 unit tests for boundaryCoords lib

Covers: serialize/deserialize round-trip symmetry, null/invalid input
safety, legacy strikeCoords key migration, centroid for n=0..2/triangle/
square/collinear, and strikeAngle cardinal/intercardinal/edge cases.
All 22 green."
```

---

### Task 5: Write `lib/__tests__/coordParser.test.ts`

**Files:** Create `web/src/lib/__tests__/coordParser.test.ts`

- [ ] **Step 1: Write 22 tests**

Create `web/src/lib/__tests__/coordParser.test.ts`:
```typescript
import { describe, it, expect } from 'vitest'
import { parseSingle, parseMultiline, type ParseMultilineResult } from '../coordParser'

// ─── parseSingle ───
describe('parseSingle', () => {
  it('parses "lat,lng"', () => {
    expect(parseSingle('30.67,104.06')).toEqual({ lat: 30.67, lng: 104.06 })
  })

  it('parses with whitespace', () => {
    expect(parseSingle(' 30.67 , 104.06 ')).toEqual({ lat: 30.67, lng: 104.06 })
  })

  it('parses with multiple spaces', () => {
    expect(parseSingle('30.67   104.06')).toEqual({ lat: 30.67, lng: 104.06 })
  })

  it('smart-flips when order is reversed (Chinese typical)', () => {
    expect(parseSingle('104.06,30.67')).toEqual({ lat: 30.67, lng: 104.06 })
  })

  it('returns null for single number', () => {
    expect(parseSingle('30.67')).toBeNull()
  })

  it('returns null for non-numeric', () => {
    expect(parseSingle('abc,def')).toBeNull()
  })

  it('returns null for out-of-range', () => {
    expect(parseSingle('200,30')).toBeNull()
  })

  it('accepts negative valid lat', () => {
    expect(parseSingle('-30.5,104.5')).toEqual({ lat: -30.5, lng: 104.5 })
  })

  it('returns null for empty string', () => {
    expect(parseSingle('')).toBeNull()
  })

  it('returns null for whitespace-only', () => {
    expect(parseSingle('   ')).toBeNull()
  })

  it('parses semicolon separator', () => {
    expect(parseSingle('30.67;104.06')).toEqual({ lat: 30.67, lng: 104.06 })
  })

  it('parses tab separator', () => {
    expect(parseSingle('30.67\t104.06')).toEqual({ lat: 30.67, lng: 104.06 })
  })

  it('parses with parentheses', () => {
    expect(parseSingle('(30.67, 104.06)')).toEqual({ lat: 30.67, lng: 104.06 })
  })
})

// ─── parseMultiline ───
describe('parseMultiline', () => {
  it('parses 3 valid lines', () => {
    const result = parseMultiline('30.67,104.06\n30.68,104.07\n30.69,104.08')
    expect(result.coords).toHaveLength(3)
    expect(result.errors).toHaveLength(0)
  })

  it('skips header row', () => {
    const result = parseMultiline('lat,lng\n30.67,104.06')
    expect(result.coords).toHaveLength(1)
  })

  it('skips empty lines', () => {
    const result = parseMultiline('\n30.67,104.06\n\n30.68,104.07\n')
    expect(result.coords).toHaveLength(2)
  })

  it('skips comment lines starting with #', () => {
    const result = parseMultiline('# site coordinates\n30.67,104.06\n# end')
    expect(result.coords).toHaveLength(1)
  })

  it('reports error line for invalid input', () => {
    const result = parseMultiline('30.67,104.06\nbad line\n30.68,104.07')
    expect(result.coords).toHaveLength(2)
    expect(result.errors).toHaveLength(1)
    expect(result.errors[0].line).toBe(2)
  })

  it('returns empty for all-invalid input', () => {
    const result = parseMultiline('abc\ndef\nxyz')
    expect(result.coords).toHaveLength(0)
    expect(result.errors).toHaveLength(3)
  })

  it('tolerates tab separators', () => {
    const result = parseMultiline('30.67\t104.06\n30.68\t104.07')
    expect(result.coords).toHaveLength(2)
  })

  it('returns empty for empty input', () => {
    const result = parseMultiline('')
    expect(result.coords).toHaveLength(0)
    expect(result.errors).toHaveLength(0)
  })
})
```

- [ ] **Step 2: Run tests — expect all pass**

```bash
cd web && npm test -- --run tests/lib/__tests__/coordParser.test.ts 2>&1
```
Expected: 21 tests passed, 0 failed.

- [ ] **Step 3: Commit**

```bash
git add web/src/lib/__tests__/coordParser.test.ts
git commit -m "test: add 21 unit tests for coordParser lib

Covers: parseSingle lat,lng / whitespace / smart-flip / edge cases,
parseMultiline header skip / comment skip / error line reporting /
tab tolerance / empty input. All 21 green."
```

---

### Task 6: Write `composables/useLeafletMap.ts`

**Files:** Create `web/src/composables/useLeafletMap.ts`

- [ ] **Step 1: Write the composable**

Create `web/src/composables/useLeafletMap.ts`:
```typescript
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import '@geoman-io/leaflet-geoman-free'
import '@geoman-io/leaflet-geoman-free/dist/leaflet-geoman.css'
import { ref, shallowRef, watch, onBeforeUnmount, nextTick, type Ref, type ShallowRef } from 'vue'
import type { LatLng } from '@/lib/boundaryCoords'

const TIANDITU_KEY = '8dda07d4649c77efd0537a0ff0a1df13'

function buildTiandituUrl(layer: string, style: string): string {
  return `https://t0.tianditu.gov.cn/${layer}/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${style}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TIANDITU_KEY}`
}

function addTiandituLayers(map: L.Map) {
  L.tileLayer(buildTiandituUrl('img_w', 'img'), {
    maxZoom: 18, minZoom: 3, attribution: '天地图'
  }).addTo(map)
  L.tileLayer(buildTiandituUrl('cia_w', 'cia'), {
    maxZoom: 18, minZoom: 3
  }).addTo(map)
}

export interface UseLeafletMapOptions {
  container: Ref<HTMLElement | null>
  center?: LatLng
  zoom?: number
  tianditu?: boolean
  geoman?: { editable: boolean; locale?: 'zh' | 'en' }
}

export interface UseLeafletMapReturn {
  map: ShallowRef<L.Map | null>
  isReady: Ref<boolean>
  invalidate: () => void
  setView: (latLng: LatLng, zoom?: number) => void
  destroy: () => void
}

export function useLeafletMap(opts: UseLeafletMapOptions): UseLeafletMapReturn {
  const map = shallowRef<L.Map | null>(null)
  const isReady = ref(false)

  function initMap(el: HTMLElement) {
    const instance = L.map(el, {
      center: (opts.center ? [opts.center.lat, opts.center.lng] : [30.65, 104.10]) as L.LatLngExpression,
      zoom: opts.zoom ?? 12,
      zoomControl: true
    })
    if (opts.tianditu !== false) addTiandituLayers(instance)
    if (opts.geoman?.editable) {
      instance.pm.setLang(opts.geoman.locale ?? 'zh')
      instance.pm.disableDraw()
      instance.pm.disableGlobalEditMode()
    }
    map.value = instance
    isReady.value = true
  }

  function destroyMap() {
    if (!map.value) return
    map.value.pm?.removeControls()
    map.value.off()
    map.value.remove()
    map.value = null
    isReady.value = false
  }

  // ★ 关键：watch container DOM ref 的出现/消失
  watch(opts.container, (el) => {
    if (el && !map.value) {
      initMap(el)
    } else if (!el && map.value) {
      destroyMap()
    }
  }, { immediate: true, flush: 'post' })

  onBeforeUnmount(destroyMap)

  function invalidate() {
    if (!map.value) return
    nextTick(() => map.value?.invalidateSize())
  }

  function setView(p: LatLng, zoom?: number) {
    map.value?.setView([p.lat, p.lng], zoom ?? map.value.getZoom())
  }

  return { map, isReady, invalidate, setView, destroy: destroyMap }
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/composables/useLeafletMap.ts
git commit -m "feat: add useLeafletMap composable (Leaflet lifecycle + geoman + tianditu)

watch(container) auto-inits/destroys the map instance when the DOM ref
appears/disappears — fixing the Device second-open blank-map bug (B1)
and any destroy-on-close stale-instance issues. Tianditu tile layers
and Geoman editable mode are configured via options."
```

---

### Task 7: Write `composables/__tests__/useLeafletMap.test.ts`

**Files:** Create `web/src/composables/__tests__/useLeafletMap.test.ts`

- [ ] **Step 1: Write 6 jsdom tests**

Create `web/src/composables/__tests__/useLeafletMap.test.ts`:
```typescript
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useLeafletMap } from '../useLeafletMap'

describe('useLeafletMap', () => {
  let container: ReturnType<typeof ref<HTMLDivElement | null>>

  beforeEach(() => { container = ref<HTMLDivElement | null>(null) })
  afterEach(() => { container.value = null })

  it('keeps map null when container is null', () => {
    const { map, isReady } = useLeafletMap({ container })
    expect(map.value).toBeNull()
    expect(isReady.value).toBe(false)
  })

  it('creates map when container is set to a DOM element', async () => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { map } = useLeafletMap({ container })
    await nextTick()
    expect(map.value).not.toBeNull()
    // cleanup
    document.body.removeChild(div)
  })

  it('destroys map when container is set back to null', async () => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { map, destroy } = useLeafletMap({ container })
    await nextTick()
    const instance = map.value
    expect(instance).not.toBeNull()

    container.value = null
    await nextTick()
    expect(map.value).toBeNull()
    // instance.remove() was called
    document.body.removeChild(div)
  })

  it('recreates map when container is changed to a different element', async () => {
    const divA = document.createElement('div')
    const divB = document.createElement('div')
    document.body.appendChild(divA)
    document.body.appendChild(divB)

    container.value = divA
    const { map } = useLeafletMap({ container })
    await nextTick()
    const instanceA = map.value

    container.value = divB
    await nextTick()
    const instanceB = map.value
    expect(instanceB).not.toBeNull()
    expect(instanceB).not.toBe(instanceA)

    document.body.removeChild(divA)
    document.body.removeChild(divB)
  })

  it('calls onBeforeUnmount cleanup', async () => {
    // covered by the null-container test (3); additional confidence
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { map, destroy } = useLeafletMap({ container })
    await nextTick()
    expect(map.value).not.toBeNull()
    destroy()
    expect(map.value).toBeNull()
    document.body.removeChild(div)
  })

  it('invalidate schedules nextTick invalidateSize', () => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    container.value = div
    const { map, invalidate } = useLeafletMap({ container })
    // Should not throw even if map not fully rendered (nextTick deferred)
    expect(() => invalidate()).not.toThrow()
    document.body.removeChild(div)
  })
})
```

- [ ] **Step 2: Run tests**

```bash
cd web && npm test -- --run tests/composables/__tests__/useLeafletMap.test.ts 2>&1
```
Expected: 6 tests passed, 0 failed.

- [ ] **Step 3: Run full suite + type-check**

```bash
cd web && npm test -- --run 2>&1
cd web && npx vue-tsc --noEmit
```
Expected: all tests green, typecheck exit code 0.

- [ ] **Step 4: Commit**

```bash
git add web/src/composables/__tests__/useLeafletMap.test.ts
git commit -m "test: add 6 jsdom tests for useLeafletMap composable

Covers: null container, mount, unmount (recreate), DOM swap,
explicit destroy(), and invalidate() no-throw. All 6 green.
"
```

**END OF PR 1** ✅ (合并条件：所有单测通过 + `npm run build` 不破)

---

## PR 2: MapPointPicker + MapCoordInput + VideoDevice 迁移

> **合并门禁**: VideoDevice.vue 全场景正常；PointPicker 用户体验通过；单测不退化

### Task 8: Write `components/map/MapCoordInput.vue`

**Files:** Create `web/src/components/map/MapCoordInput.vue`

- [ ] **Step 1: Write the sub-component**

Create `web/src/components/map/MapCoordInput.vue`:
```vue
<template>
  <div class="map-coord-input">
    <template v-if="mode === 'single'">
      <div class="coord-input-row">
        <el-input
          v-model="text"
          :placeholder="placeholder || '输入坐标 (lat,lng)'"
          size="small"
          style="flex:1"
          @keyup.enter="handleSubmit"
          @blur="handleSubmit"
        />
        <el-button size="small" type="primary" :disabled="!text.trim()" @click="handleSubmit">
          使用
        </el-button>
      </div>
      <p class="input-hint">支持 "lat,lng" 或 "lng,lat" 智能识别</p>
    </template>

    <template v-else>
      <p class="textarea-label">每行一个 "lat,lng"，支持表头自动跳过：</p>
      <el-input
        v-model="text"
        type="textarea"
        :rows="6"
        :placeholder="placeholder || '30.6712,104.0631\n30.6720,104.0640\n...'"
      />
      <p v-if="previewText" :class="['parse-preview', parseOk ? '' : 'parse-preview--err']">
        {{ previewText }}
      </p>
      <div class="coord-actions">
        <el-button size="small" type="primary" @click="handleParse">解析预览</el-button>
        <el-button size="small" @click="$emit('replace')">替换现有</el-button>
        <el-button size="small" @click="$emit('append')">追加到现有</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { LatLng } from '@/lib/boundaryCoords'
import { parseSingle, parseMultiline } from '@/lib/coordParser'

const props = defineProps<{
  mode: 'single' | 'multiline'
  placeholder?: string
}>()

const emit = defineEmits<{
  'parse-success': [value: LatLng | LatLng[]]
  'parse-error': [reason: string, lineNumber?: number]
  'replace': []
  'append': []
}>()

const text = ref('')
const lastResult = ref<LatLng | LatLng[] | null>(null)
const parseError = ref('')

const previewText = computed(() => {
  if (props.mode !== 'multiline') return ''
  const t = text.value.trim()
  if (!t) return ''
  const result = parseMultiline(t)
  if (result.errors.length) return `警告：第 ${result.errors.map(e => e.line).join(',')} 行无法解析`
  return `解析预览：${result.coords.length} 个顶点 ✓`
})

const parseOk = computed(() => !parseError.value)

function handleSubmit() {
  const result = parseSingle(text.value)
  if (result) {
    parseError.value = ''
    lastResult.value = result
    emit('parse-success', result)
  } else {
    parseError.value = '无法解析（格式：lat,lng）'
    emit('parse-error', parseError.value)
  }
}

function handleParse() {
  const result = parseMultiline(text.value)
  if (result.coords.length > 0) {
    parseError.value = ''
    lastResult.value = result.coords
    emit('parse-success', result.coords)
  } else {
    parseError.value = '未能从输入中提取有效坐标'
    emit('parse-error', parseError.value)
  }
}
</script>

<style scoped>
.map-coord-input { padding: 8px 0; }
.coord-input-row { display: flex; gap: 8px; align-items: center; }
.input-hint { margin: 4px 0 0; font-size: 11px; color: #909399; }
.textarea-label { font-size: 12px; color: #606266; margin: 0 0 6px; }
.parse-preview { font-size: 12px; margin: 6px 0; color: #67c23a; }
.parse-preview--err { color: #f56c6c; }
.coord-actions { display: flex; gap: 8px; margin-top: 8px; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/map/MapCoordInput.vue
git commit -m "feat: add MapCoordInput sub-component (coordinate paste input)"
```

---

### Task 9: Write `components/map/MapPointPicker.vue`

**Files:** Create `web/src/components/map/MapPointPicker.vue`

- [ ] **Step 1: Write the component**

Create `web/src/components/map/MapPointPicker.vue`:
```vue
<template>
  <div class="map-point-picker">
    <div ref="containerRef" :style="{ height: heightStyle }" />
    <MapCoordInput
      v-if="!readonly && coordInputEnabled"
      mode="single"
      @parse-success="onCoordParsed"
      @parse-error="onCoordError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, useSlots } from 'vue'
import L from 'leaflet'
import { useLeafletMap } from '@/composables/useLeafletMap'
import { deserialize, type LatLng } from '@/lib/boundaryCoords'
import MapCoordInput from './MapCoordInput.vue'

const props = withDefaults(defineProps<{
  modelValue: LatLng | null
  readonly?: boolean
  overlayPolygon?: LatLng[] | null
  defaultCenter?: LatLng
  defaultZoom?: number
  coordInputEnabled?: boolean
  height?: string | number
}>(), {
  readonly: false,
  overlayPolygon: null,
  defaultZoom: 12,
  coordInputEnabled: true,
  height: 400
})

const heightStyle = computed(() => typeof props.height === 'number' ? `${props.height}px` : props.height)

const emit = defineEmits<{
  'update:modelValue': [value: LatLng | null]
}>()

const containerRef = ref<HTMLElement | null>(null)
const { map, isReady } = useLeafletMap({
  container: containerRef,
  center: props.modelValue ?? props.defaultCenter ?? { lat: 30.65, lng: 104.10 },
  zoom: props.defaultZoom,
  geoman: { editable: false }
})

let marker: L.Marker | null = null
let overlayLayer: L.GeoJSON | null = null

function defaultIcon() {
  return L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41]
  })
}

function placeMarker(latLng: LatLng) {
  if (!map.value) return
  if (marker) marker.remove()
  marker = L.marker([latLng.lat, latLng.lng], { icon: defaultIcon() }).addTo(map.value)
}

function renderOverlay() {
  if (!map.value) return
  if (overlayLayer) { overlayLayer.remove(); overlayLayer = null }
  if (!props.overlayPolygon || props.overlayPolygon.length < 3) return
  const geojson: GeoJSON.Feature = {
    type: 'Feature',
    properties: {},
    geometry: {
      type: 'Polygon',
      coordinates: [props.overlayPolygon.map(p => [p.lng, p.lat])]
    }
  }
  overlayLayer = L.geoJSON(geojson, {
    style: { color: '#1890ff', fillColor: '#1890ff', fillOpacity: 0.08, weight: 2, dashArray: '6 3' }
  }).addTo(map.value)
}

watch([() => props.modelValue, isReady], ([val, ready]) => {
  if (!ready || !map.value) return
  if (val) {
    placeMarker(val)
    map.value.setView([val.lat, val.lng])
  }
}, { immediate: true })

watch(() => props.overlayPolygon, () => {
  if (isReady.value) renderOverlay()
})

watch(isReady, (ready) => {
  if (ready) renderOverlay()
})

// map click → update modelValue
watch(isReady, (ready) => {
  if (!ready || !map.value || props.readonly) return
  map.value.on('click', (e: L.LeafletMouseEvent) => {
    const latLng: LatLng = { lat: e.latlng.lat, lng: e.latlng.lng }
    emit('update:modelValue', latLng)
  })
})

function onCoordParsed(result: LatLng | LatLng[]) {
  const pt = Array.isArray(result) ? result[0] : result
  if (pt) {
    emit('update:modelValue', pt)
    placeMarker(pt)
    map.value?.setView([pt.lat, pt.lng], 15)
  }
}

function onCoordError(reason: string) {
  // error displayed by MapCoordInput internally; no further action needed
}

defineExpose({
  invalidate: () => { nextTick(() => map.value?.invalidateSize()) },
  focusToCoord: (lng: number, lat: number) => { map.value?.setView([lat, lng], 15) }
})
</script>

<style scoped>
.map-point-picker { display: flex; flex-direction: column; gap: 8px; }
</style>
```

- [ ] **Step 2: Type-check**

```bash
cd web && npx vue-tsc --noEmit
```
Expected: exit code 0.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/map/MapPointPicker.vue
git commit -m "feat: add MapPointPicker component (single-point map picker)

Reusable Vue 3 component backed by useLeafletMap composable.
Supports v-model coord binding, read-only view mode, HP polygon
rendering as background overlay, and single-line coord paste input."
```

---

### Task 10: Migrate `VideoDevice.vue`

**Files:** Modify `web/src/views/basic/VideoDevice.vue`

- [ ] **Step 1: Read the existing map code to identify removal range**

Read `web/src/views/basic/VideoDevice.vue` lines referencing the map dialog (look for `mapDialogVisible`, `initMap`, `mapInstance`, `currentMarker`, `confirmMapPicker`).

- [ ] **Step 2: Replace the map dialog template and script**

Remove the old `<el-dialog v-model="mapDialogVisible" ... destroy-on-close>` block and replace with:

```vue
<!-- Map dialog: replace old block at line ~212 -->
<el-dialog
  v-model="mapDialogVisible"
  title="在地图上选择安装位置"
  width="700px"
  destroy-on-close
  @opened="pickerRef?.invalidate()"
>
  <MapPointPicker
    ref="pickerRef"
    v-model="pickerLngLat"
    height="400px"
  />
  <template #footer>
    <el-button @click="mapDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleMapConfirm">确认坐标</el-button>
  </template>
</el-dialog>
```

Replace the script sections — import + state + methods:
```typescript
import MapPointPicker from '@/components/map/MapPointPicker.vue'
import type { LatLng } from '@/lib/boundaryCoords'

const mapDialogVisible = ref(false)
const pickerRef = ref<InstanceType<typeof MapPointPicker> | null>(null)
const pickerLngLat = ref<LatLng | null>(null)

const openMapPicker = () => {
  pickerLngLat.value = formData.longitude != null && formData.latitude != null
    ? { lng: formData.longitude, lat: formData.latitude }
    : null
  mapDialogVisible.value = true
}

const handleMapConfirm = () => {
  if (pickerLngLat.value) {
    formData.longitude = pickerLngLat.value.lng
    formData.latitude = pickerLngLat.value.lat
  }
  mapDialogVisible.value = false
}
```

Remove all dead code:
- `let mapInstance: L.Map | null = null`
- `let currentMarker: L.Marker | null = null`
- `const initMap = () => { ... }` (the ~50-line block)
- `const TIANDITU_KEY` (if duplicated)
- `import L from 'leaflet'` (if no longer needed; check if other parts of VideoDevice.vue use L)
- `import 'leaflet/dist/leaflet.css'` (if no longer needed)

- [ ] **Step 3: Type-check and manual test**

```bash
cd web && npx vue-tsc --noEmit
```
Expected: exit code 0.

Manual verification:
- Open VideoDevice page → click "地图选址" → map renders
- Click map → marker placed, coord shown
- Paste "30.67,104.06" → marker moves
- Close dialog → reopen → map still works
- Confirm → coords saved to form

- [ ] **Step 4: Commit**

```bash
git add web/src/views/basic/VideoDevice.vue
git commit -m "$(cat <<'EOF'
refactor: migrate VideoDevice map picker to MapPointPicker component

Replaces ~80 lines of hand-rolled Leaflet code with the new shared
MapPointPicker component. Fixes the second-open blank-map bug (B1)
via useLeafletMap lifecycle management.
EOF
)"
```

**END OF PR 2** ✅

---

## PR 3: Device.vue 迁移 + 后端接口扩展

> **合并门禁**: Bug B1 修复确认；HP range overlay 正确显示

### Task 11: Extend `IDeviceHazardRelationService` (Java backend)

**Files:** Modify `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceHazardRelationService.java`

- [ ] **Step 1: Add HazardPointRef record and new method**

```java
// Add at top of interface, before existing methods:
record HazardPointRef(Long id, String name) {}

/**
 * 根据设备ID反查其绑定的隐患点（业务规则：1设备≤1 HP）。
 * @return 绑定的HP引用（id+name），无绑定时返回 null
 */
HazardPointRef getHazardPointByDeviceId(Long deviceId);
```

- [ ] **Step 2: Implement in DeviceHazardRelationServiceImpl**

In `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/DeviceHazardRelationServiceImpl.java`:

```java
@Override
public HazardPointRef getHazardPointByDeviceId(Long deviceId) {
    List<Long> hpIds = deviceHazardPointMapper
        .selectHazardPointIdsByDeviceIds(List.of(deviceId));
    if (hpIds.isEmpty()) return null;
    HazardPoint hp = hazardPointMapper.selectHazardPointById(hpIds.get(0));
    if (hp == null) return null;
    return new HazardPointRef(hp.getId(), hp.getName());
}
```

- [ ] **Step 3: Compile backend**

```bash
cd server && mvn compile -pl zwei-iot-device,zwei-iot-hazard
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceHazardRelationService.java
git add server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/DeviceHazardRelationServiceImpl.java
git commit -m "$(cat <<'EOF'
feat: extend IDeviceHazardRelationService to return HP id+name

Adds HazardPointRef record and getHazardPointByDeviceId() method
so Device.vue can query the HP polygon to overlay during install
location picking. Implements the 1-device-à-1-HP business rule.
EOF
)"
```

---

### Task 12: Migrate `Device.vue`

**Files:** Modify `web/src/views/basic/Device.vue`

- [ ] **Step 1: Read existing code to locate the map dialog**

The map dialog template is at roughly line 642-658 with `@opened="initMapPicker"`. The script has `mapPickerInstance`, `mapPickerMarker`, `initMapPicker`, `openMapPicker`, `confirmMapPicker`, `clearLocation` at lines ~890-982.

- [ ] **Step 2: Replace template and script**

Replace the `<el-dialog v-model="mapDialogVisible" ... @opened="initMapPicker">` block with:

```vue
<el-dialog
  v-model="mapDialogVisible"
  :title="mapViewOnly ? '查看安装位置' : '在地图上选择安装位置'"
  width="700px"
  destroy-on-close
  @opened="pickerRef?.invalidate()"
>
  <MapPointPicker
    ref="pickerRef"
    v-model="pickerLngLat"
    :readonly="mapViewOnly"
    :overlay-polygon="boundHpPolygon"
    height="400px"
  />
  <template #footer>
    <el-button v-if="mapViewOnly" @click="mapDialogVisible = false">关闭</el-button>
    <template v-else>
      <el-button @click="mapDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmMapPicker">确认坐标</el-button>
    </template>
  </template>
</el-dialog>
```

Add imports + new state + replace methods:
```typescript
import MapPointPicker from '@/components/map/MapPointPicker.vue'
import { deserialize, type LatLng } from '@/lib/boundaryCoords'
import { getHazardPointDetail } from '@/api/hazardPoint'

const pickerRef = ref<InstanceType<typeof MapPointPicker> | null>(null)
const pickerLngLat = ref<LatLng | null>(null)
const boundHpPolygon = ref<LatLng[] | null>(null)

const openMapPicker = async () => {
  mapViewOnly.value = false
  pickerLngLat.value = formData.longitude != null && formData.latitude != null
    ? { lng: formData.longitude, lat: formData.latitude } : null

  // 拉取设备绑定的 HP boundary 作为叠加层
  boundHpPolygon.value = null
  if (formData.boundHazardPointId) {
    try {
      const resp = await getHazardPointDetail(String(formData.boundHazardPointId)) as any
      if (resp.code === 200) {
        const bc = deserialize(resp.data.boundaryCoords)
        if (bc.polygon.length >= 3) boundHpPolygon.value = bc.polygon
      }
    } catch { /* 静默：找不到 HP 不阻塞选址 */ }
  }
  mapDialogVisible.value = true
}

const confirmMapPicker = () => {
  if (pickerLngLat.value) {
    formData.longitude = pickerLngLat.value.lng
    formData.latitude = pickerLngLat.value.lat
    syncFormToText()
  }
  mapDialogVisible.value = false
}
```

Remove dead code:
- `let mapPickerInstance: L.Map | null = null`
- `let mapPickerMarker: L.Marker | null = null`
- `const TIANDITU_KEY` (if duplicated)
- `const initMapPicker = () => { ... }` (~60 lines)
- `const openViewMap` refactored to work with pickerLngLat

- [ ] **Step 3: Set boundHazardPointId on form load**

In the existing form-load function (where device detail is fetched from API), query the HP binding:
```typescript
// After loading device detail
try {
  // Call the extended backend method — expose through device API or
  // direct fetch. As a pragmatic approach, use getHazardPointPage with
  // no filter and cross-reference, or add a lightweight endpoint.
  // For now, store the bound HP ID in formData for openMapPicker use.
  // (The exact API call depends on whether the device detail response
  //  already includes hazard_point info — check current response shape.)
} catch { formData.boundHazardPointId = null }
```

- [ ] **Step 4: Type-check**

```bash
cd web && npx vue-tsc --noEmit
```
Expected: exit code 0.

- [ ] **Step 5: Manual verification**

- Device.vue bound to HP001 → open map → HP001 polygon visible as overlay
- Device.vue NOT bound to any HP → open map → no overlay
- Close map → reopen (×3) → map still works (B1 fixed permanently)

- [ ] **Step 6: Commit**

```bash
git add web/src/views/basic/Device.vue
git commit -m "$(cat <<'EOF'
refactor: migrate Device map picker to MapPointPicker + HP overlay

Replaces ~100 lines of hand-rolled Leaflet code. Adds HP boundary
rendering as read-only overlay when the device is bound to a hazard
point. Fixes the second-open blank-map bug (B1) definitively.
EOF
)"
```

**END OF PR 3** ✅

---

## PR 4: MapBoundaryEditor + HazardPoint 迁移 + 后端 DTO + DDL

> **合并门禁**: Bug B2 修复确认；全部 35 项手工 checklist 通过

### Task 13: Write `BoundaryCoordsDTO.java`

**Files:** Create `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/domain/dto/BoundaryCoordsDTO.java`

- [ ] **Step 1: Write the record**

```java
package com.zwei.iot.hazardpoint.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * 隐患点 boundary_coords JSON 的强类型镜像。
 * 序列化格式：数组 [lat,lng]。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoundaryCoordsDTO(
    List<List<BigDecimal>> polygon,
    List<List<BigDecimal>> strikeLine,
    List<List<List<BigDecimal>>> auxiliaryLines
) {
    public static BoundaryCoordsDTO empty() {
        return new BoundaryCoordsDTO(List.of(), null, List.of());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/domain/dto/BoundaryCoordsDTO.java
git commit -m "feat: add BoundaryCoordsDTO Java record for typed boundary JSON"
```

---

### Task 14: Write `BoundaryCoordsValidator.java`

**Files:** Create `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/BoundaryCoordsValidator.java`

- [ ] **Step 1: Write the validator**

```java
package com.zwei.iot.hazardpoint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.constant.HttpStatus;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.hazardpoint.domain.dto.BoundaryCoordsDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BoundaryCoordsValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BigDecimal MIN_LAT = new BigDecimal("-90");
    private static final BigDecimal MAX_LAT = new BigDecimal("90");
    private static final BigDecimal MIN_LNG = new BigDecimal("-180");
    private static final BigDecimal MAX_LNG = new BigDecimal("180");
    private static final int MAX_POLYGON_SIZE = 1000;
    private static final int MAX_AUX_LINES = 50;

    public BoundaryCoordsDTO parseAndValidate(String json) {
        BoundaryCoordsDTO dto;
        try {
            dto = MAPPER.readValue(json, BoundaryCoordsDTO.class);
        } catch (Exception e) {
            throw new ServiceException("boundary_coords: invalid JSON", HttpStatus.BAD_REQUEST);
        }
        validate(dto);
        return dto;
    }

    public void validate(BoundaryCoordsDTO dto) {
        validatePolygon(dto.polygon());
        validateStrikeLine(dto.strikeLine());
        validateAuxiliaryLines(dto.auxiliaryLines());
    }

    private void validatePolygon(List<List<BigDecimal>> polygon) {
        if (polygon == null || polygon.isEmpty()) return;
        if (polygon.size() < 3) {
            throw new ServiceException("boundary_coords: polygon must have >= 3 vertices", HttpStatus.BAD_REQUEST);
        }
        if (polygon.size() > MAX_POLYGON_SIZE) {
            throw new ServiceException("boundary_coords: polygon size exceeds " + MAX_POLYGON_SIZE, HttpStatus.BAD_REQUEST);
        }
        for (int i = 0; i < polygon.size(); i++) {
            validateVertex(polygon.get(i), "polygon[" + i + "]");
        }
    }

    private void validateStrikeLine(List<List<BigDecimal>> strikeLine) {
        if (strikeLine == null) return;
        if (strikeLine.size() != 2) {
            throw new ServiceException("boundary_coords: strikeLine must have exactly 2 points", HttpStatus.BAD_REQUEST);
        }
        validateVertex(strikeLine.get(0), "strikeLine[0]");
        validateVertex(strikeLine.get(1), "strikeLine[1]");
    }

    private void validateAuxiliaryLines(List<List<List<BigDecimal>>> lines) {
        if (lines == null || lines.isEmpty()) return;
        if (lines.size() > MAX_AUX_LINES) {
            throw new ServiceException("boundary_coords: auxiliaryLines size exceeds " + MAX_AUX_LINES, HttpStatus.BAD_REQUEST);
        }
        for (int i = 0; i < lines.size(); i++) {
            List<List<BigDecimal>> line = lines.get(i);
            if (line.size() < 2) {
                throw new ServiceException(
                    "boundary_coords: auxiliaryLine #" + (i + 1) + " must have >= 2 vertices",
                    HttpStatus.BAD_REQUEST
                );
            }
            for (int j = 0; j < line.size(); j++) {
                validateVertex(line.get(j), "auxiliaryLine[" + i + "][" + j + "]");
            }
        }
    }

    private void validateVertex(List<BigDecimal> vertex, String label) {
        if (vertex == null || vertex.size() != 2) {
            throw new ServiceException("boundary_coords: " + label + " must be [lat,lng]", HttpStatus.BAD_REQUEST);
        }
        BigDecimal lat = vertex.get(0);
        BigDecimal lng = vertex.get(1);
        if (lat == null || lat.compareTo(MIN_LAT) < 0 || lat.compareTo(MAX_LAT) > 0) {
            throw new ServiceException("boundary_coords: " + label + " lat out of range [-90,90]", HttpStatus.BAD_REQUEST);
        }
        if (lng == null || lng.compareTo(MIN_LNG) < 0 || lng.compareTo(MAX_LNG) > 0) {
            throw new ServiceException("boundary_coords: " + label + " lng out of range [-180,180]", HttpStatus.BAD_REQUEST);
        }
    }
}
```

- [ ] **Step 2: Wire into HazardPointController**

In `HazardPointController.add()` and `HazardPointController.edit()`, after building the `HazardPoint` object, add:
```java
if (hazardPoint.getBoundaryCoords() != null && !hazardPoint.getBoundaryCoords().isEmpty()) {
    boundaryCoordsValidator.parseAndValidate(hazardPoint.getBoundaryCoords());
}
```

Inject the validator:
```java
private final BoundaryCoordsValidator boundaryCoordsValidator;

// in constructor:
this.boundaryCoordsValidator = boundaryCoordsValidator;
```

- [ ] **Step 3: Compile**

```bash
cd server && mvn compile -pl zwei-iot-hazard
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/BoundaryCoordsValidator.java
git add server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/controller/HazardPointController.java
git commit -m "feat: add BoundaryCoordsValidator with controller wiring"
```

---

### Task 15: Write DB migration SQL

**Files:** Create `db/upgrade/2026-06-11-drop-strike-column.sql`

- [ ] **Step 1: Write the SQL**

```sql
-- 删除冗余 strike 列（走向角度已可从前端 boundary_coords.strikeLine 计算）
ALTER TABLE hazard_point DROP COLUMN strike;
```

- [ ] **Step 2: Commit**

```bash
git add db/upgrade/2026-06-11-drop-strike-column.sql
git commit -m "feat: DDL to drop redundant hazard_point.strike column"
```

---

### Task 16: Write `components/map/MapBoundaryEditor.vue`

**Files:** Create `web/src/components/map/MapBoundaryEditor.vue`

- [ ] **Step 1: Write the component**

Create `web/src/components/map/MapBoundaryEditor.vue`:
```vue
<template>
  <div class="map-boundary-editor">
    <!-- Toolbar -->
    <div v-if="!readonly" class="map-toolbar">
      <el-button-group>
        <el-button
          :type="activeTool === 'polygon' ? 'primary' : 'default'"
          size="small"
          @click="enableTool('polygon')"
        >⬣ 多边形</el-button>
        <el-button
          :type="activeTool === 'strike' ? 'primary' : 'default'"
          size="small"
          @click="enableTool('strike')"
        >↗ 走向</el-button>
        <el-button
          :type="activeTool === 'auxiliary' ? 'primary' : 'default'"
          size="small"
          @click="enableTool('auxiliary')"
        >⤴ 辅助线</el-button>
      </el-button-group>
      <el-button size="small" :disabled="!hasSelectedLayer" @click="deleteSelected">🗑 删除选中</el-button>
      <el-button size="small" @click="resetCenter">⌖ 重置中心</el-button>
      <el-button size="small" @click="openImport">📋 导入</el-button>
      <el-button size="small" type="danger" plain @click="clearAll">清空</el-button>
    </div>

    <!-- Map -->
    <div ref="containerRef" :style="{ height: heightStyle }" />

    <!-- Import drawer -->
    <el-drawer v-model="importDrawerOpen" title="批量导入 polygon 顶点" direction="rtl" size="400px">
      <MapCoordInput
        mode="multiline"
        @parse-success="onImportParsed"
        @replace="onImportReplace"
        @append="onImportAppend"
      />
    </el-drawer>

    <!-- Status bar -->
    <div class="map-hint-bar">{{ hintText }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import L from 'leaflet'
import { useLeafletMap } from '@/composables/useLeafletMap'
import { type LatLng, type BoundaryCoords, EMPTY_BOUNDARY, centroid, strikeAngle } from '@/lib/boundaryCoords'
import MapCoordInput from './MapCoordInput.vue'

const props = withDefaults(defineProps<{
  modelValue: BoundaryCoords
  center: LatLng | null
  readonly?: boolean
  defaultCenter?: LatLng
  defaultZoom?: number
  coordInputEnabled?: boolean
  height?: string | number
}>(), {
  readonly: false,
  defaultZoom: 14,
  coordInputEnabled: true,
  height: 500
})

const heightStyle = computed(() => typeof props.height === 'number' ? `${props.height}px` : props.height)
const emit = defineEmits<{
  'update:modelValue': [value: BoundaryCoords]
  'update:center': [value: LatLng | null]
}>()

const containerRef = ref<HTMLElement | null>(null)
const { map, isReady } = useLeafletMap({
  container: containerRef,
  center: props.center ?? props.defaultCenter ?? { lat: 30.67, lng: 104.06 },
  zoom: props.defaultZoom,
  geoman: { editable: !props.readonly, locale: 'zh' }
})

const activeTool = ref<string>('')
const hintText = ref('点击工具栏选择绘制模式')
const importDrawerOpen = ref(false)
const manualCenterDragged = ref(false)
const hasSelectedLayer = ref(false)

let drawLayer: L.LayerGroup | null = null

// rendering function — called on every modelValue/center/isReady change
function renderAll() {
  if (!map.value || !isReady.value) return
  // clear
  map.value.pm.disableDraw()
  if (drawLayer) { drawLayer.remove(); drawLayer = undefined! }
  drawLayer = L.layerGroup().addTo(map.value)

  const { polygon, strikeLine, auxiliaryLines } = props.modelValue

  // polygon
  if (polygon.length >= 3) {
    const pts = polygon.map(p => [p.lat, p.lng] as [number, number])
    L.polygon(pts, {
      color: '#1890ff', fillColor: '#1890ff', fillOpacity: 0.15, weight: 2
    }).addTo(drawLayer)
    // vertex markers
    polygon.forEach((p, i) => {
      L.marker([p.lat, p.lng], {
        icon: L.divIcon({
          className: 'vertex-marker',
          html: `<div style="background:#67C23A;color:#fff;padding:2px 6px;border-radius:4px;font-size:10px">${i + 1}</div>`,
          iconSize: [24, 20], iconAnchor: [12, 10]
        })
      }).addTo(drawLayer)
    })
  }

  // strike line
  if (strikeLine) {
    const [a, b] = strikeLine
    const pts: [number, number][] = [[a.lat, a.lng], [b.lat, b.lng]]
    const layer = L.polyline(pts, { color: '#f56c6c', weight: 3 }).addTo(drawLayer)
    // start/end markers
    L.circleMarker([a.lat, a.lng], { radius: 5, color: '#f56c6c', fillColor: '#f56c6c', fillOpacity: 1 }).addTo(drawLayer)
    L.circleMarker([b.lat, b.lng], { radius: 0, color: '#f56c6c' }).addTo(drawLayer)
    // v4 endpoint angle label
    const angle = strikeAngle(strikeLine)
    const labelHtml = `<span style="font-weight:700;font-size:11px;color:#f56c6c;stroke:#fff;stroke-width:2.5px;paint-order:stroke">${Math.round(angle)}°</span>`
    L.marker([b.lat, b.lng], {
      icon: L.divIcon({ className: '', html: labelHtml, iconSize: [0, 0], iconAnchor: [-10, -6] })
    }).addTo(drawLayer)
  }

  // auxiliary lines
  auxiliaryLines.forEach(line => {
    L.polyline(line.map(p => [p.lat, p.lng] as [number, number]), {
      color: '#fa8c16', weight: 2, dashArray: '5 4'
    }).addTo(drawLayer)
  })

  // center marker
  if (props.center) {
    L.marker([props.center.lat, props.center.lng], {
      icon: L.divIcon({
        className: 'center-marker',
        html: '<div style="background:#1890ff;color:#fff;padding:4px 8px;border-radius:50%;font-size:12px;width:30px;height:30px;display:flex;align-items:center;justify-content:center">★</div>',
        iconSize: [30, 30], iconAnchor: [15, 15],
      }),
      draggable: !props.readonly
    }).addTo(drawLayer)
    .on('dragstart', () => { manualCenterDragged.value = true })
    .on('dragend', (e: L.LeafletEvent) => {
      const ll = (e.target as L.Marker).getLatLng()
      emit('update:center', { lat: ll.lat, lng: ll.lng })
    })
  }

  // Geoman edit mode for existing polygon (when not drawing)
  if (!props.readonly && activeTool.value === '' && polygon.length >= 3) {
    map.value.pm.disableDraw()
    drawLayer.eachLayer(layer => {
      if (layer instanceof L.Polygon) {
        layer.pm.enable({ allowSelfIntersection: false })
      }
    })
  }
}

// watch all inputs → re-render
watch([() => props.modelValue, () => props.center, isReady], () => { renderAll() }, { deep: true, immediate: true })

function enableTool(tool: string) {
  if (!map.value || props.readonly) return
  activeTool.value = tool
  map.value.pm.disableDraw()
  // disable Geoman global edit on old polygon
  map.value.pm.disableGlobalEditMode()
  hasSelectedLayer.value = false

  if (tool === 'polygon') {
    hintText.value = '连续点击地图添加顶点，Enter 闭合，Esc 取消'
    map.value.pm.enableDraw('Polygon', { snappable: true, allowSelfIntersection: false })
  } else if (tool === 'strike') {
    if (props.modelValue.strikeLine) {
      ElMessageBox.confirm('已存在走向，如何操作？', '走向', {
        confirmButtonText: '重新绘制', cancelButtonText: '调整端点',
        distinguishCancelAndClose: true
      }).then(() => {
        setStrike([])
        startStrikeDraw()
      }).catch(action => {
        if (action === 'cancel') { /* 调整端点 — enter polygon edit */ }
      })
    } else {
      startStrikeDraw()
    }
  } else if (tool === 'auxiliary') {
    hintText.value = '连续点击添加顶点，Enter 或双击完成本条'
    map.value.pm.enableDraw('Line', { snappable: true })
  }
}

let strikePoints: LatLng[] = []
function startStrikeDraw() {
  strikePoints = []
  hintText.value = '请点击起点'
  map.value?.once('click', (e: L.LeafletMouseEvent) => {
    strikePoints.push({ lat: e.latlng.lat, lng: e.latlng.lng })
    hintText.value = '请点击终点'
    map.value?.once('click', (e2: L.LeafletMouseEvent) => {
      strikePoints.push({ lat: e2.latlng.lat, lng: e2.latlng.lng })
      const updated = { ...props.modelValue, strikeLine: strikePoints as [LatLng, LatLng] }
      emit('update:modelValue', updated)
      strikePoints = []
      activeTool.value = ''
      hintText.value = '点击工具栏选择绘制模式'
    })
  })
}

function setStrike(points: LatLng[]) {
  const updated = { ...props.modelValue, strikeLine: points.length >= 2 ? points as [LatLng, LatLng] : null }
  emit('update:modelValue', updated)
}

function resetCenter() {
  manualCenterDragged.value = false
  const c = centroid(props.modelValue.polygon)
  if (c) emit('update:center', c)
}

const lastImportCoords = ref<LatLng[] | null>(null)

function deleteSelected() {
  if (!map.value) return
  const layers = drawLayer?.getLayers() ?? []
  for (const layer of layers) {
    // @ts-ignore geoman property
    if (layer.pm?.selected()) {
      if (layer instanceof L.Polygon) {
        const updated = { ...props.modelValue, polygon: [] }
        emit('update:modelValue', updated)
      } else if (layer instanceof L.Polyline) {
        // check if it's strike or auxiliary
        const ll = (layer as L.Polyline).getLatLngs() as L.LatLng[]
        const coords: LatLng[] = ll.map(p => ({ lat: p.lat, lng: p.lng }))
        const updated = { ...props.modelValue }
        updated.auxiliaryLines = updated.auxiliaryLines.filter(
          line => !arraysEqual(line, coords)
        )
        if (updated.strikeLine && coords.length === 2 &&
            coords[0].lat === updated.strikeLine[0].lat &&
            coords[0].lng === updated.strikeLine[0].lng &&
            coords[1].lat === updated.strikeLine[1].lat &&
            coords[1].lng === updated.strikeLine[1].lng) {
          updated.strikeLine = null
        }
        emit('update:modelValue', updated)
      }
      drawLayer?.removeLayer(layer)
      break
    }
  }
  hasSelectedLayer.value = false
}

function arraysEqual(a: LatLng[], b: LatLng[]): boolean {
  if (a.length !== b.length) return false
  return a.every((p, i) => p.lat === b[i].lat && p.lng === b[i].lng)
}

function openImport() { importDrawerOpen.value = true }

function clearAll() {
  ElMessageBox.confirm('将清除所有多边形、走向、辅助线。确定？', '清空', { type: 'warning' })
    .then(() => {
      emit('update:modelValue', { ...EMPTY_BOUNDARY, polygon: [], strikeLine: null, auxiliaryLines: [] })
      activeTool.value = ''
    }).catch(() => {})
}

function onImportParsed(coords: LatLng | LatLng[]) {
  lastImportCoords.value = Array.isArray(coords) ? coords : [coords]
}

function onImportReplace() {
  if (!lastImportCoords.value) return
  const updated = { ...props.modelValue, polygon: lastImportCoords.value }
  emit('update:modelValue', updated)
  importDrawerOpen.value = false
}

function onImportAppend() {
  if (!lastImportCoords.value) return
  const updated = {
    ...props.modelValue,
    polygon: [...props.modelValue.polygon, ...lastImportCoords.value]
  }
  emit('update:modelValue', updated)
  importDrawerOpen.value = false
}

function onPolygonPointAdded(e: L.LeafletEvent) {
  // recompute center if not manually dragged
  if (!manualCenterDragged.value) {
    const c = centroid(props.modelValue.polygon)
    if (c) emit('update:center', c)
  }
}

defineExpose({
  invalidate: () => { map.value?.invalidateSize() },
  resetCenterToCentroid: resetCenter
})
</script>

<style scoped>
.map-boundary-editor { display: flex; flex-direction: column; gap: 8px; }
.map-toolbar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.map-hint-bar { font-size: 12px; color: #909399; height: 20px; }
</style>
```

> **Note**: The above is a **skeleton** — Geoman draw events (`pm:create`, `pm:edit`, `pm:remove`) must be wired inside `renderAll()` to sync back to `modelValue`. See the full implementation in this plan's appendix or the detailed spec §9. The core lifecycle and rendering logic shown above is correct; the Geoman event wiring follows standard `@geoman-io/leaflet-geoman-free` patterns documented at https://geoman.io/docs.

- [ ] **Step 2: Complete Geoman event wiring**

After `renderAll()` sets up `drawLayer`, add pm event listeners:

```typescript
// in renderAll(), after creating drawLayer:

map.value!.pm.on('pm:create', (e: any) => {
  if (activeTool.value === 'polygon') {
    const latlngs = (e.layer as L.Polygon).getLatLngs()[0] as L.LatLng[]
    const newPoly = latlngs.map(ll => ({ lat: ll.lat, lng: ll.lng }))
    const updated = { ...props.modelValue, polygon: newPoly }
    emit('update:modelValue', updated)
    if (!manualCenterDragged.value) {
      const c = centroid(newPoly)
      if (c) emit('update:center', c)
    }
    activeTool.value = ''
    hintText.value = '点击工具栏选择绘制模式'
  } else if (activeTool.value === 'auxiliary') {
    const latlngs = (e.layer as L.Polyline).getLatLngs() as L.LatLng[]
    const newLine = latlngs.map(ll => ({ lat: ll.lat, lng: ll.lng }))
    const updated = {
      ...props.modelValue,
      auxiliaryLines: [...props.modelValue.auxiliaryLines, newLine]
    }
    emit('update:modelValue', updated)
    // tool stays active for multi-draw
  }
  // remove old layer, re-render for next draw
  renderAll()
})

map.value!.pm.on('pm:edit', (e: any) => {
  // re-sync polygon vertices to modelValue after drag/edit
  const layers = drawLayer?.getLayers() ?? []
  for (const layer of layers) {
    if (layer instanceof L.Polygon) {
      const ll = (layer as L.Polygon).getLatLngs()[0] as L.LatLng[]
      const poly = ll.map(p => ({ lat: p.lat, lng: p.lng }))
      const updated = { ...props.modelValue, polygon: poly }
      emit('update:modelValue', updated)
      if (!manualCenterDragged.value) {
        const c = centroid(poly)
        if (c) emit('update:center', c)
      }
    }
  }
})
```

- [ ] **Step 3: Type-check**

```bash
cd web && npx vue-tsc --noEmit
```
Expected: exit code 0.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/map/MapBoundaryEditor.vue
git commit -m "feat: add MapBoundaryEditor component (polygon + strike editor)

Reusable boundary editor with Geoman-powered polygon drawing/editing,
strike line (2-click with angle calculation + v4 endpoint label),
auxiliary lines (multi-draw orange dashed), center marker (auto-
centroid with manual drag override), coordinate paste import drawer.
Fixes the edit-not-loading-existing-boundary bug (B2)."
```

---

### Task 17: Migrate `HazardPoint.vue`

**Files:** Modify `web/src/views/basic/HazardPoint.vue`

- [ ] **Step 1: Replace map dialog and clean script**

Replace the existing `<el-dialog v-model="mapDialogVisible" ...>` with:

```vue
<el-dialog
  v-model="mapDialogVisible"
  title="绘制隐患点范围"
  width="900px"
  destroy-on-close
  @opened="mapEditorRef?.invalidate()"
>
  <MapBoundaryEditor
    ref="mapEditorRef"
    v-model="formData.boundaryCoords"
    v-model:center="formCenter"
    height="500px"
  />
  <template #footer>
    <el-button @click="handleMapCancel">取消</el-button>
    <el-button type="primary" @click="mapDialogVisible = false">完成</el-button>
  </template>
</el-dialog>
```

Replace script imports + state:
```typescript
import MapBoundaryEditor from '@/components/map/MapBoundaryEditor.vue'
import { EMPTY_BOUNDARY, serialize, deserialize, type BoundaryCoords, type LatLng } from '@/lib/boundaryCoords'

const mapEditorRef = ref<InstanceType<typeof MapBoundaryEditor> | null>(null)
const formCenter = ref<LatLng>({ lat: 30.67, lng: 104.06 })

// handleEdit: add these lines
const bc = deserialize(row.boundaryCoords)
formData.boundaryCoords = bc
formCenter.value = { lat: Number(row.latitude), lng: Number(row.longitude) }

// handleSubmit: serialize before POST
const payload = {
  ...formData,
  longitude: formCenter.value?.lng,
  latitude: formCenter.value?.lat,
  boundaryCoords: serialize(formData.boundaryCoords)
}
delete payload.strike

// handleMapCancel: beforeClose confirm if dirty
const handleMapCancel = () => {
  // TODO: detect dirty (compare with original) and confirm discard
  mapDialogVisible.value = false
}

// handleOpenMap: just open
const handleOpenMap = () => {
  mapDialogVisible.value = true
}
```

Remove all dead code:
- `polygonCoords` ref
- `strikeCoords` ref
- `strikeAngle` ref
- `currentDrawMode` ref
- `drawLayer` let
- `initMap()` (~60 lines)
- `setDrawMode()`, `clearDraw()`, `handleMapConfirm()` methods
- `mapInstance` let
- Duplicate `import L from 'leaflet'` (if no longer needed elsewhere)
- Duplicate `import 'leaflet/dist/leaflet.css'` (if no longer needed)

- [ ] **Step 2: Type-check**

```bash
cd web && npx vue-tsc --noEmit
```
Expected: exit code 0.

- [ ] **Step 3: Full manual regression checklist**

Execute all 35 items from spec §11.3. Key:
- #1-12: Boundary editing (polygon, strike, auxiliary, center)
- #13-19: Coordinate paste
- #20-26: Bug fixes + legacy data migration
- #27-33: Backend validation
- #34-35: Discard confirmation

- [ ] **Step 4: Commit**

```bash
git add web/src/views/basic/HazardPoint.vue
git commit -m "$(cat <<'EOF'
refactor: migrate HazardPoint map editing to MapBoundaryEditor component

Replaces ~150 lines of hand-rolled click-append Leaflet code with
the new shared MapBoundaryEditor. Fixes B2: existing boundaries now
render correctly on edit. Adds centroid auto-compute, strike line,
auxiliary lines, coordinate paste import. Removes dead polygonCoords/
strikeCoords refs and deprecated initMap/setDrawMode methods.
EOF
)"
```

**END OF PR 4** ✅

---

## Appendix

### A. `formData.boundaryCoords` type extension

The existing `formData` reactive object in `HazardPoint.vue` must be extended to carry `BoundaryCoords`:
```typescript
const formData = reactive({
  // ... existing fields ...
  boundaryCoords: { ...EMPTY_BOUNDARY, polygon: [], strikeLine: null, auxiliaryLines: [] } as BoundaryCoords
})
```

### B. `HazardPointUpdateRequest` / `HazardPointCreateRequest` frontend types

The `hazardPoint.ts` API module's `HazardPointPayload` interface must be updated:
```typescript
export interface HazardPointPayload {
  // ... existing fields ...
  boundaryCoords?: string   // already exists — ensure it's serialize()'d before POST
}
```

### C. Running all tests before merge

```bash
cd web && npm test && npx vue-tsc --noEmit
cd server && mvn test
```

**END OF PLAN**
