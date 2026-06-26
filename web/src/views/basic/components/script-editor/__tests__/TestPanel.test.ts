import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import TestPanel from '../TestPanel.vue'
import type { CalcScriptTestResult } from '@/api/monitorType'

/**
 * 偏差说明 (Option A + textarea 选择器修正):
 *
 * 1. mountPanel 注入 ElementPlus 插件 — vitest.config.ts 未全局注册 Element Plus
 *    (unplugin-vue-components 仅在 vite build/dev 中生效), 不注入会导致 el-input/el-button/el-alert
 *    无法解析。
 *
 * 2. setValue 直接在 [data-test="..."] 上调用 — Element Plus el-input type="textarea"
 *    将 data-test 属性直接渲染到 <textarea> 元素上 (非外层 wrapper div),
 *    因此 [data-test="..."] textarea 选择器匹配为空, 需去掉 textarea 后缀。
 *
 * 3. mountPanel 默认传入 { result: null, testing: false } — 避免未传 props 时的 Vue warn。
 */
function mountPanel(options: { props?: Record<string, unknown>; global?: Record<string, unknown> } = {}) {
  const { props: userProps, global: userGlobal, ...rest } = options
  return mount(TestPanel, {
    ...rest,
    props: { result: null, testing: false, ...userProps },
    global: {
      ...(userGlobal || {}),
      plugins: [ElementPlus]
    }
  })
}

describe('TestPanel', () => {
  it('默认展开（含 curData/prevData 输入框）', () => {
    const wrapper = mountPanel()
    expect(wrapper.find('[data-test="cur-data-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="prev-data-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="run-btn"]').exists()).toBe(true)
  })

  it('点击运行按钮 emit runTest 事件, 携带解析后的 curData/prevData', async () => {
    const wrapper = mountPanel()
    await wrapper.find('[data-test="cur-data-input"]').setValue('{"props":{"x":10}}')
    await wrapper.find('[data-test="prev-data-input"]').setValue('{"props":{"x":5}}')
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    const emitted = wrapper.emitted('runTest')
    expect(emitted).toBeTruthy()
    expect(emitted![0][0]).toEqual({ curData: { props: { x: 10 } }, prevData: { props: { x: 5 } } })
  })

  it('curData 不是合法 JSON 时, 不 emit 且显示错误消息', async () => {
    const wrapper = mountPanel()
    await wrapper.find('[data-test="cur-data-input"]').setValue('{ not json')
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    expect(wrapper.emitted('runTest')).toBeFalsy()
    expect(wrapper.text()).toContain('JSON')
  })

  it('prevData 为空时,emit 的 prevData 为 undefined', async () => {
    const wrapper = mountPanel()
    await wrapper.find('[data-test="cur-data-input"]').setValue('{}')
    await wrapper.find('[data-test="prev-data-input"]').setValue('')
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    const emitted = wrapper.emitted('runTest')!
    expect(emitted[0][0]).toEqual({ curData: {}, prevData: undefined })
  })

  it('result.success=true 时显示绿色成功提示 + 返回值', () => {
    const result: CalcScriptTestResult = {
      success: true,
      executionTime: 586,
      result: { hour_change: 25.0 }
    }
    const wrapper = mountPanel({ props: { result, testing: false } })
    const successAlert = wrapper.find('[data-test="result-success"]')
    expect(successAlert.exists()).toBe(true)
    expect(successAlert.text()).toContain('586')
    expect(successAlert.text()).toContain('25')
  })

  it('result.success=false 时显示红色错误 + error 字段', () => {
    const result: CalcScriptTestResult = {
      success: false,
      error: '属性 hour_change 执行异常: MissingPropertyException at line 2'
    }
    const wrapper = mountPanel({ props: { result, testing: false } })
    const errorAlert = wrapper.find('[data-test="result-error"]')
    expect(errorAlert.exists()).toBe(true)
    expect(errorAlert.text()).toContain('MissingPropertyException')
  })

  it('testing=true 时运行按钮处于 loading 状态且禁用', () => {
    const wrapper = mountPanel({ props: { testing: true } })
    const btn = wrapper.find('[data-test="run-btn"]')
    expect(btn.attributes('disabled')).toBeDefined()
    expect(btn.classes().join(' ')).toMatch(/loading|is-loading/)
  })

  it('清空按钮重置 curData 和 prevData', async () => {
    const wrapper = mountPanel()
    await wrapper.find('[data-test="cur-data-input"]').setValue('{"a":1}')
    await wrapper.find('[data-test="clear-btn"]').trigger('click')
    expect((wrapper.find('[data-test="cur-data-input"]').element as HTMLTextAreaElement).value).toBe('')
  })
})
