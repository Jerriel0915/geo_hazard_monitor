import { describe, it, expect } from 'vitest'
import { parseSingle, parseMultiline } from '../coordParser'

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
