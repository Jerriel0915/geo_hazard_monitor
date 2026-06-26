import {describe, expect, test} from 'vitest'
import {
  getAuthEventTag,
  getBusinessTypeTag,
  getLevelTag,
  getLiveSubtypeTag,
  getRequestMethodTag,
  getStreamEventTag,
} from '@/utils/logTags'

describe('logTags', () => {
  describe('getRequestMethodTag', () => {
    test('maps GET to success', () => expect(getRequestMethodTag('GET')).toBe('success'))
    test('maps POST to warning', () => expect(getRequestMethodTag('POST')).toBe('warning'))
    test('maps PUT to info', () => expect(getRequestMethodTag('PUT')).toBe('info'))
    test('maps DELETE to danger', () => expect(getRequestMethodTag('DELETE')).toBe('danger'))
    test('returns info for undefined', () => expect(getRequestMethodTag()).toBe('info'))
    test('returns info for unknown method', () => expect(getRequestMethodTag('PATCH')).toBe('info'))
    test('is case-insensitive', () => expect(getRequestMethodTag('get')).toBe('success'))
  })

  describe('getBusinessTypeTag', () => {
    test('maps INSERT to success', () => expect(getBusinessTypeTag('INSERT')).toBe('success'))
    test('maps UPDATE to warning', () => expect(getBusinessTypeTag('UPDATE')).toBe('warning'))
    test('maps DELETE to danger', () => expect(getBusinessTypeTag('DELETE')).toBe('danger'))
    test('maps EXPORT to info', () => expect(getBusinessTypeTag('EXPORT')).toBe('info'))
    test('returns info for undefined', () => expect(getBusinessTypeTag()).toBe('info'))
    test('is case-insensitive', () => expect(getBusinessTypeTag('insert')).toBe('success'))
  })

  describe('getAuthEventTag', () => {
    test('SUCCESS/LOGOUT map to success', () => {
      expect(getAuthEventTag('LOGIN_SUCCESS')).toBe('success')
      expect(getAuthEventTag('LOGOUT')).toBe('success')
    })
    test('UNAUTHORIZED/INVALID map to warning', () => {
      expect(getAuthEventTag('UNAUTHORIZED')).toBe('warning')
      expect(getAuthEventTag('TOKEN_INVALID')).toBe('warning')
    })
    test('other maps to danger', () => {
      expect(getAuthEventTag('LOGIN_FAILURE')).toBe('danger')
    })
    test('undefined maps to danger', () => {
      expect(getAuthEventTag()).toBe('danger')
    })
  })

  describe('getLevelTag', () => {
    test('maps INFO to info', () => expect(getLevelTag('INFO')).toBe('info'))
    test('maps WARN to warning', () => expect(getLevelTag('WARN')).toBe('warning'))
    test('maps ERROR to danger', () => expect(getLevelTag('ERROR')).toBe('danger'))
    test('undefined maps to info', () => expect(getLevelTag()).toBe('info'))
  })

  describe('getStreamEventTag', () => {
    test('maps known events', () => {
      expect(getStreamEventTag('ready')).toBe('success')
      expect(getStreamEventTag('replay')).toBe('warning')
      expect(getStreamEventTag('operation')).toBe('info')
      expect(getStreamEventTag('auth')).toBe('danger')
      expect(getStreamEventTag('runtime')).toBe('warning')
    })
    test('unknown maps to info', () => expect(getStreamEventTag('unknown')).toBe('info'))
  })

  describe('getLiveSubtypeTag', () => {
    test('OPERATION maps to warning', () => expect(getLiveSubtypeTag('OPERATION')).toBe('warning'))
    test('AUTH maps to danger', () => expect(getLiveSubtypeTag('AUTH')).toBe('danger'))
    test('RUNTIME maps to info', () => expect(getLiveSubtypeTag('RUNTIME')).toBe('info'))
    test('unknown maps to empty string', () => expect(getLiveSubtypeTag('unknown')).toBe(''))
  })
})
