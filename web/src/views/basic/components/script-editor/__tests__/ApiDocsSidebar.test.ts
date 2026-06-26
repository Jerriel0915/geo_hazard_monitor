import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ApiDocsSidebar from '../ApiDocsSidebar.vue'

describe('ApiDocsSidebar', () => {
  it('渲染 4 个分组标题 (curData/prevData/cache/sensor)', () => {
    const wrapper = mount(ApiDocsSidebar)
    const headers = wrapper.findAll('[data-test="group-name"]')
    expect(headers).toHaveLength(4)
    expect(headers.map(h => h.text())).toEqual(['curData', 'prevData', 'cache', 'sensor'])
  })

  it('cache 分组渲染所有 13 条方法签名', () => {
    const wrapper = mount(ApiDocsSidebar)
    const cacheGroup = wrapper.find('[data-test="group-cache"]')
    const methods = cacheGroup.findAll('[data-test="method-sig"]')
    expect(methods.length).toBeGreaterThanOrEqual(13)
  })

  it('渲染 description (cache 有 "Redis 二次封装")', () => {
    const wrapper = mount(ApiDocsSidebar)
    const cacheGroup = wrapper.find('[data-test="group-cache"]')
    expect(cacheGroup.text()).toContain('Redis')
  })

  it('渲染 sensor 的异常吞噬提示', () => {
    const wrapper = mount(ApiDocsSidebar)
    const sensorGroup = wrapper.find('[data-test="group-sensor"]')
    expect(sensorGroup.text()).toContain('null')
  })

  it('顶部有 "API 文档" 标题 + "返回值必须是 Number" 提示', () => {
    const wrapper = mount(ApiDocsSidebar)
    expect(wrapper.text()).toContain('API 文档')
    expect(wrapper.text()).toContain('Number')
  })
})
