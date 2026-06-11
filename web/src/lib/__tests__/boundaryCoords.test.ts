import { describe, it, expect } from 'vitest'
import { EMPTY_BOUNDARY, serialize, deserialize, centroid, strikeAngle, assertValidBoundary, type BoundaryCoords, type LatLng } from '../boundaryCoords'

function l(lat: number, lng: number): LatLng { return { lat, lng } }

// ─── serialize / deserialize 对称性 ───
describe('serialize', () => {
  it('serializes EMPTY_BOUNDARY', () => {
    expect(serialize({ polygon: [], strikeLine: null, auxiliaryLines: [] }))
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

// ─── assertValidBoundary ───
describe('assertValidBoundary', () => {
  it('returns null for empty boundary', () => {
    expect(assertValidBoundary({ polygon: [], strikeLine: null, auxiliaryLines: [] })).toBeNull()
  })

  it('returns null for valid 3-vertex polygon', () => {
    const b = {
      polygon: [l(0, 0), l(0, 1), l(1, 1)],
      strikeLine: null,
      auxiliaryLines: []
    }
    expect(assertValidBoundary(b)).toBeNull()
  })

  it('errors when polygon has only 2 vertices', () => {
    const b = {
      polygon: [l(0, 0), l(0, 1)],
      strikeLine: null,
      auxiliaryLines: []
    }
    const err = assertValidBoundary(b)
    expect(err).toMatch(/多边形至少需要 3 个顶点/)
  })

  it('errors when an aux line has only 1 point', () => {
    const b = {
      polygon: [l(0, 0), l(0, 1), l(1, 1)],
      strikeLine: null,
      auxiliaryLines: [[l(0, 0)]]
    }
    const err = assertValidBoundary(b)
    expect(err).toMatch(/辅助线至少需要 2 个点/)
  })

  it('allows optional strikeLine with exactly 2 points', () => {
    const b: BoundaryCoords = {
      polygon: [l(0, 0), l(0, 1), l(1, 1)],
      strikeLine: [l(0, 0), l(1, 1)],
      auxiliaryLines: []
    }
    expect(assertValidBoundary(b)).toBeNull()
  })
})
