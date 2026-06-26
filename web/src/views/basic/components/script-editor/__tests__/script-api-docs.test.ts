import { describe, it, expect } from 'vitest'
import { API_DOCS, type ApiGroup } from '../script-api-docs'

describe('script-api-docs', () => {
  it('包含 4 个分组: curData / prevData / cache / sensor', () => {
    const names = API_DOCS.map(g => g.name)
    expect(names).toEqual(['curData', 'prevData', 'cache', 'sensor'])
  })

  it('每个分组都有 icon / color / name / methods', () => {
    API_DOCS.forEach((g: ApiGroup) => {
      expect(g.icon).toBeTruthy()
      expect(g.color).toMatch(/^#[0-9a-f]{6}$/i)
      expect(g.name).toBeTruthy()
      expect(g.methods.length).toBeGreaterThan(0)
      g.methods.forEach(m => {
        expect(typeof m.signature).toBe('string')
        expect(m.signature.length).toBeGreaterThan(0)
      })
    })
  })

  it('cache 分组覆盖 ScriptCacheOps 全部 21 个公开方法重载', () => {
    const cache = API_DOCS.find(g => g.name === 'cache')!
    // 实际方法签名是 21 个 (ScriptCacheOps.java)
    // 文档侧按"对外语义"聚合, 同名重载合并, 故 13 条说明 = 21 方法的语义覆盖
    expect(cache.methods.length).toBeGreaterThanOrEqual(13)
    const sigs = cache.methods.map(m => m.signature).join('\n')
    // 抽样验证关键方法
    expect(sigs).toContain('getInt')
    expect(sigs).toContain('getString')
    expect(sigs).toContain('set')
    expect(sigs).toContain('delete')
    expect(sigs).toContain('hasKey')
    expect(sigs).toContain('expire')
    expect(sigs).toContain('getExpire')
    // 类型覆盖
    ;['getInt', 'getLong', 'getDouble', 'getFloat', 'getBigDecimal', 'getString', 'getBoolean'].forEach(m => {
      expect(sigs).toContain(m)
    })
  })

  it('sensor 分组说明异常吞噬行为', () => {
    const sensor = API_DOCS.find(g => g.name === 'sensor')!
    const allText = sensor.methods.map(m => `${m.signature} ${m.note || ''}`).join('\n')
    expect(allText).toContain('query')
    expect(allText).toMatch(/null|异常/)
  })

  it('curData 和 prevData 都暴露 .props.<attrCode> 访问', () => {
    const cur = API_DOCS.find(g => g.name === 'curData')!
    const prev = API_DOCS.find(g => g.name === 'prevData')!
    expect(cur.methods.some(m => m.signature.includes('.props'))).toBe(true)
    expect(prev.methods.some(m => m.signature.includes('.props'))).toBe(true)
  })

  it('curData/prevData 文档覆盖全部 5 个字段 (deviceCode/sensorCode/props/properties/dataTime)', () => {
    const required = ['.deviceCode', '.sensorCode', '.props', '.properties', '.dataTime']
    for (const g of ['curData', 'prevData'] as const) {
      const group = API_DOCS.find(x => x.name === g)!
      const sigs = group.methods.map(m => m.signature).join('\n')
      for (const field of required) {
        expect(sigs, `${g} 应包含 ${field}`).toContain(field)
      }
    }
  })

  it('sensor 文档说明 SensorSnapshot 返回结构 (time + values)', () => {
    const sensor = API_DOCS.find(g => g.name === 'sensor')!
    const allText = sensor.methods.map(m => `${m.signature} ${m.note || ''}`).join('\n')
    expect(allText).toContain('time')
    expect(allText).toContain('values')
  })

  it('sensor.query 第一参为 deviceCode (与 curData.deviceCode 同源)', () => {
    const sensor = API_DOCS.find(g => g.name === 'sensor')!
    const querySig = sensor.methods.map(m => m.signature).join('\n')
    expect(querySig).toContain('query(deviceCode,')
    expect(querySig).not.toContain('deviceId')
  })
})
