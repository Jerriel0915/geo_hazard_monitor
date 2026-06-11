import { describe, it, expect } from 'vitest'
import {
  midpoint,
  edgeMidpoint,
  edgeVertices,
  hitVertex,
  hitEdge,
  hitPolyline,
  metersPerPixel,
  insertVertexAtEdge,
  removeVertexSafe,
  isSelfIntersecting
} from '../mapGeometry'
import type { LatLng } from '../boundaryCoords'

const l = (lat: number, lng: number): LatLng => ({ lat, lng })

describe('midpoint', () => {
  it('averages two points', () => {
    expect(midpoint(l(0, 0), l(10, 20))).toEqual({ lat: 5, lng: 10 })
  })
  it('handles negative coords', () => {
    expect(midpoint(l(-1, -2), l(-3, -4))).toEqual({ lat: -2, lng: -3 })
  })
})

describe('edgeMidpoint', () => {
  it('returns midpoint of edge i → i+1', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10), l(10, 0)]
    expect(edgeMidpoint(poly, 0)).toEqual({ lat: 0, lng: 5 })
    expect(edgeMidpoint(poly, 1)).toEqual({ lat: 5, lng: 10 })
  })
  it('wraps around for last edge', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10)]
    // edge 2: poly[2] → poly[0] = (10,10) → (0,0)
    expect(edgeMidpoint(poly, 2)).toEqual({ lat: 5, lng: 5 })
  })
})

describe('edgeVertices', () => {
  it('returns edge endpoints', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10)]
    expect(edgeVertices(poly, 1)).toEqual([l(0, 10), l(10, 10)])
  })
  it('wraps around for last edge', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10)]
    expect(edgeVertices(poly, 2)).toEqual([l(10, 10), l(0, 0)])
  })
})

describe('hitVertex', () => {
  it('returns true when within tolerance', () => {
    const target = { lat: 30.67, lng: 104.06 }
    const close = { lat: 30.67001, lng: 104.06001 }
    // ~1.1m at this latitude; tolerance 5m
    expect(hitVertex(close, target, 5)).toBe(true)
  })
  it('returns false when far', () => {
    const target = { lat: 30.67, lng: 104.06 }
    const far = { lat: 30.68, lng: 104.06 }
    // ~1.1km; tolerance 5m
    expect(hitVertex(far, target, 5)).toBe(false)
  })
})

describe('hitEdge', () => {
  const poly = [l(0, 0), l(0, 0.001), l(0.001, 0.001)]
  it('detects click near edge midpoint', () => {
    // Edge 0 midpoint is at (0, 0.0005) ≈ (0, 55.5m)
    // Slightly offset
    const p = { lat: 0.00001, lng: 0.0005 }
    expect(hitEdge(p, poly, 0, 20)).toBe(true)
  })
  it('rejects click far from edge', () => {
    const p = { lat: 0.1, lng: 0.1 }
    expect(hitEdge(p, poly, 0, 20)).toBe(false)
  })
  it('rejects click near a different edge', () => {
    // Edge 1 goes (0, 0.001) → (0.001, 0.001)
    // A click near edge 0 midpoint should NOT hit edge 1
    const p = { lat: 0, lng: 0.0005 }
    expect(hitEdge(p, poly, 1, 20)).toBe(false)
  })
})

describe('hitPolyline', () => {
  const line = [l(0, 0), l(0, 0.001), l(0.001, 0.001)]
  it('detects click near segment', () => {
    const p = { lat: 0.0001, lng: 0 }
    expect(hitPolyline(p, line, 20)).toBe(true)
  })
  it('rejects far click', () => {
    expect(hitPolyline({ lat: 0.1, lng: 0.1 }, line, 20)).toBe(false)
  })
})

describe('metersPerPixel', () => {
  it('at zoom 0, one tile covers 256 px and ~40,000 km (equator)', () => {
    // Approximate: at lat 0, zoom 0, ~156543 m/pixel
    const m = metersPerPixel(0, 0)
    expect(m).toBeGreaterThan(150000)
    expect(m).toBeLessThan(160000)
  })
  it('halves with each zoom level', () => {
    const z10 = metersPerPixel(30, 10)
    const z11 = metersPerPixel(30, 11)
    expect(z10 / z11).toBeCloseTo(2, 1)
  })
  it('depends on latitude (cos(lat))', () => {
    const equator = metersPerPixel(0, 10)
    const polar = metersPerPixel(80, 10)
    expect(equator).toBeGreaterThan(polar)
  })
})

describe('insertVertexAtEdge', () => {
  it('inserts after given edge, does not mutate input', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10)]
    const newPt = l(0, 5)
    const result = insertVertexAtEdge(poly, 0, newPt)
    expect(result).toEqual([l(0, 0), l(0, 5), l(0, 10), l(10, 10)])
    expect(poly).toEqual([l(0, 0), l(0, 10), l(10, 10)]) // unchanged
  })
  it('inserts on the wrapped edge', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10)]
    // edge 2: poly[2] → poly[0]
    const result = insertVertexAtEdge(poly, 2, l(5, 5))
    expect(result).toEqual([l(0, 0), l(0, 10), l(10, 10), l(5, 5)])
  })
})

describe('removeVertexSafe', () => {
  it('removes a vertex when >= 4 remain', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10), l(10, 0)]
    expect(removeVertexSafe(poly, 0)).toEqual([l(0, 10), l(10, 10), l(10, 0)])
  })
  it('returns null when only 3 vertices (cannot go below)', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10)]
    expect(removeVertexSafe(poly, 1)).toBeNull()
  })
  it('does not mutate input', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10), l(10, 0)]
    removeVertexSafe(poly, 0)
    expect(poly.length).toBe(4)
  })
})

describe('isSelfIntersecting', () => {
  it('returns false for simple convex polygon', () => {
    const poly = [l(0, 0), l(0, 10), l(10, 10), l(10, 0)]
    expect(isSelfIntersecting(poly)).toBe(false)
  })
  it('returns false for triangle', () => {
    expect(isSelfIntersecting([l(0, 0), l(5, 10), l(10, 0)])).toBe(false)
  })
  it('returns true for bowtie (self-crossing quad)', () => {
    // (0,0) → (10,10) → (0,10) → (10,0) → close
    const bowtie = [l(0, 0), l(10, 10), l(0, 10), l(10, 0)]
    expect(isSelfIntersecting(bowtie)).toBe(true)
  })
  it('returns false for 2-vertex "polygon" (degenerate)', () => {
    expect(isSelfIntersecting([l(0, 0), l(10, 10)])).toBe(false)
  })
})
