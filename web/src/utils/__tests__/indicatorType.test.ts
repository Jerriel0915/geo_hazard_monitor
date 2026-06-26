import {describe, expect, test} from 'vitest'
import {getValueType, indicatorTypeLabel, INDICATOR_TYPE_META, INDICATOR_TYPE_OPTIONS} from '@/utils/indicatorType'

describe('indicatorType', () => {
  test('INDICATOR_TYPE_META contains all expected codes', () => {
    expect(Object.keys(INDICATOR_TYPE_META)).toContain('wy')
    expect(Object.keys(INDICATOR_TYPE_META)).toContain('dx')
    expect(Object.keys(INDICATOR_TYPE_META)).toContain('sp')
  })

  test('INDICATOR_TYPE_OPTIONS matches META entries', () => {
    expect(INDICATOR_TYPE_OPTIONS).toHaveLength(Object.keys(INDICATOR_TYPE_META).length)
  })

  test('getValueType returns NUMBER for known numeric code', () => {
    expect(getValueType('wy')).toBe('NUMBER')
    expect(getValueType('sw')).toBe('NUMBER')
  })

  test('getValueType returns BOOLEAN for dx', () => {
    expect(getValueType('dx')).toBe('BOOLEAN')
  })

  test('getValueType returns STRING for sg/sp', () => {
    expect(getValueType('sg')).toBe('STRING')
    expect(getValueType('sp')).toBe('STRING')
  })

  test('getValueType returns NUMBER for undefined/unknown code', () => {
    expect(getValueType()).toBe('NUMBER')
    expect(getValueType('')).toBe('NUMBER')
    expect(getValueType('unknown')).toBe('NUMBER')
  })

  test('getValueType is case-insensitive and trims whitespace', () => {
    expect(getValueType(' WY ')).toBe('NUMBER')
    expect(getValueType('Dx')).toBe('BOOLEAN')
  })

  test('indicatorTypeLabel returns Chinese name', () => {
    expect(indicatorTypeLabel('wy')).toBe('位移')
    expect(indicatorTypeLabel('sw')).toBe('水位')
  })

  test('indicatorTypeLabel returns empty for undefined/unknown code', () => {
    expect(indicatorTypeLabel()).toBe('')
    expect(indicatorTypeLabel('')).toBe('')
    expect(indicatorTypeLabel('unknown')).toBe('')
  })

  test('each meta entry has all required fields', () => {
    for (const entry of Object.values(INDICATOR_TYPE_META)) {
      expect(entry).toHaveProperty('code')
      expect(entry).toHaveProperty('name')
      expect(entry).toHaveProperty('unit')
      expect(entry).toHaveProperty('valueType')
      expect(['NUMBER', 'DATETIME', 'STRING', 'BOOLEAN']).toContain(entry.valueType)
    }
  })
})
