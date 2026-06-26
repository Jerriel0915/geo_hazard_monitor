import {describe, expect, test} from 'vitest'
import {getRequestErrorMessage} from '@/utils/errorHandler'

// showRequestErrorMessage 调用 ElMessage + console.error，跳过集成测试

describe('errorHandler', () => {
  describe('getRequestErrorMessage', () => {
    const fallback = '操作失败'

    test('extracts from error.message', () => {
      expect(getRequestErrorMessage(new Error('网络错误'), fallback)).toBe('网络错误')
    })

    test('extracts from response.data.msg', () => {
      const err = {response: {data: {msg: '参数校验失败'}}}
      expect(getRequestErrorMessage(err, fallback)).toBe('参数校验失败')
    })

    test('returns fallback when no message available', () => {
      expect(getRequestErrorMessage(null, fallback)).toBe(fallback)
      expect(getRequestErrorMessage({}, fallback)).toBe(fallback)
    })

    test('prefers error.message over response.data.msg', () => {
      const err = {message: '请求超时', response: {data: {msg: '网络错误'}}}
      expect(getRequestErrorMessage(err, fallback)).toBe('请求超时')
    })

    test('handles undefined gracefully', () => {
      expect(getRequestErrorMessage(undefined, fallback)).toBe(fallback)
    })
  })
})
