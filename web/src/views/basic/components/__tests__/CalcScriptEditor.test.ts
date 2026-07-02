import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import CalcScriptEditor from '../CalcScriptEditor.vue'

/**
 * 偏差说明 (Element Plus + el-dialog 在 jsdom 中的适配):
 *
 * 1. mountWith 注入 ElementPlus 插件 — vitest.config.ts 未全局注册 Element Plus
 *    (unplugin-vue-components 仅在 vite build/dev 中生效), 不注入会导致
 *    el-dialog/el-button/el-alert 无法解析。
 *
 * 2. stub ElDialog 内联渲染 default + footer 插槽 — el-dialog 使用 Teleport,
 *    在 jsdom 中 footer 插槽内容可能 teleport 到 document.body 外部,
 *    导致 wrapper.find('[data-test="save-btn"]') 无法找到。
 *    stub 后 default 和 footer 插槽在同一容器内联渲染, 保证 find 可达。
 *
 * 3. el-alert 类型断言改为 .text() — Element Plus el-alert 的 type class
 *    (el-alert--warning/success/error) 渲染在内层 <div class="el-alert"> 上,
 *    而 data-test 属性落在外层 <transition> wrapper 上, 导致
 *    [data-test="status-bar"].classes() 返回空。改用 .text() 匹配状态条文案
 *    (含 ⚠️/✅/❌ emoji) 验证相同语义。
 *
 * 4. ElMessage mock — 静默控制台噪音, 避免组件内 ElMessage.warning 抛错。
 *
 * 5. testCalcScript mock — 模拟后端 API, 默认返回 success: true。
 */

// 静默 ElMessage (避免控制台噪音)
vi.mock('element-plus', async (orig) => {
  const actual = await orig()
  return {
    ...(actual as any),
    ElMessage: { warning: vi.fn(), error: vi.fn(), success: vi.fn() }
  }
})

// mock testCalcScript API, 默认返回成功
vi.mock('@/api/monitorType', async (orig) => {
  const actual = await orig() as any
  return {
    ...actual,
    testCalcScript: vi.fn().mockResolvedValue({
      success: true,
      executionTime: 100,
      result: { test_attr: 42 }
    })
  }
})

const baseProps = {
  modelValue: true,
  attrCode: 'test_attr',
  attrName: '测试属性',
  unit: 'mm',
  script: 'return 1',
  monitorTypeId: 1
}

describe('CalcScriptEditor 状态机', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('初始打开: dirty=false, tested=false, 保存按钮可用', async () => {
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    expect(wrapper.find('[data-test="save-btn"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-test="status-bar"]').exists()).toBe(false)
  })

  it('修改脚本 (local != initial): 保存禁用, 黄条显示', async () => {
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    await wrapper.findComponent({ name: 'CodeMirrorGroovy' }).vm.$emit('update:modelValue', 'return 2')
    await flushPromises()
    const saveBtn = wrapper.find('[data-test="save-btn"]')
    expect(saveBtn.attributes('disabled')).toBeDefined()
    expect(saveBtn.classes().join(' ')).toMatch(/is-disabled|disabled/)
    expect(wrapper.find('[data-test="status-bar"]').text()).toMatch(/修改后必须通过测试才能保存|未测试/)
  })

  it('改回原样 (local === initial): dirty 自动解除, 保存恢复可用', async () => {
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    const cm = wrapper.findComponent({ name: 'CodeMirrorGroovy' })
    await cm.vm.$emit('update:modelValue', 'return 2')
    await flushPromises()
    await cm.vm.$emit('update:modelValue', 'return 1')
    await flushPromises()
    expect(wrapper.find('[data-test="save-btn"]').attributes('disabled')).toBeUndefined()
  })

  it('测试通过 (后端 success=true): 保存按钮变绿可用, 状态条绿色', async () => {
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    const cm = wrapper.findComponent({ name: 'CodeMirrorGroovy' })
    await cm.vm.$emit('update:modelValue', 'return 42')
    await flushPromises()
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="save-btn"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-test="status-bar"]').text()).toMatch(/测试通过/)
  })

  it('未保存监测类型 (monitorTypeId=0): 脚本测试仍可独立运行, 不再提示"请先保存"', async () => {
    const { testCalcScript } = await import('@/api/monitorType')
    const { ElMessage } = await import('element-plus')
    ;(testCalcScript as any).mockClear()
    const wrapper = await mountWith({ ...baseProps, monitorTypeId: 0 })
    await flushPromises()
    const cm = wrapper.findComponent({ name: 'CodeMirrorGroovy' })
    await cm.vm.$emit('update:modelValue', 'return 42')
    await flushPromises()
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    await flushPromises()
    // API 被调用, monitorTypeId=0 透传给后端
    expect((testCalcScript as any)).toHaveBeenCalledWith(
      expect.objectContaining({ monitorTypeId: 0, calcScript: 'return 42' })
    )
    // 未弹出"请先保存"警告
    expect((ElMessage as any).warning).not.toHaveBeenCalledWith('请先保存监测类型, 再测试脚本')
    // 测试结果正常显示
    expect(wrapper.find('[data-test="status-bar"]').text()).toMatch(/测试通过/)
  })

  it('空脚本点击测试: 提示"脚本不能为空", 不发起请求', async () => {
    const { testCalcScript } = await import('@/api/monitorType')
    ;(testCalcScript as any).mockClear()
    const wrapper = await mountWith({ ...baseProps, script: '   ' })
    await flushPromises()
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    await flushPromises()
    expect((testCalcScript as any)).not.toHaveBeenCalled()
  })

  it('测试失败 (后端 success=false): 保存禁用, 红条显示错误', async () => {
    const { testCalcScript } = await import('@/api/monitorType')
    ;(testCalcScript as any).mockResolvedValueOnce({
      success: false,
      error: 'MissingPropertyException at line 2'
    })
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    const cm = wrapper.findComponent({ name: 'CodeMirrorGroovy' })
    await cm.vm.$emit('update:modelValue', 'return undefined_var')
    await flushPromises()
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    await flushPromises()
    const saveBtn = wrapper.find('[data-test="save-btn"]')
    expect(saveBtn.attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="status-bar"]').text()).toMatch(/测试失败/)
  })

  it('测试通过后再次修改: testedPassed 重置, 保存再次禁用', async () => {
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    const cm = wrapper.findComponent({ name: 'CodeMirrorGroovy' })
    await cm.vm.$emit('update:modelValue', 'return 42')
    await flushPromises()
    await wrapper.find('[data-test="run-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="save-btn"]').attributes('disabled')).toBeUndefined()
    await cm.vm.$emit('update:modelValue', 'return 43')
    await flushPromises()
    expect(wrapper.find('[data-test="save-btn"]').attributes('disabled')).toBeDefined()
  })

  it('点击保存: emit save 携带当前脚本', async () => {
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    await wrapper.find('[data-test="save-btn"]').trigger('click')
    const emitted = wrapper.emitted('save')
    expect(emitted).toBeTruthy()
    expect(emitted![0][0]).toBe('return 1')
  })

  it('点击取消: emit update:modelValue(false) 关闭', async () => {
    const wrapper = await mountWith(baseProps)
    await flushPromises()
    await wrapper.find('[data-test="cancel-btn"]').trigger('click')
    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toBe(false)
  })
})

async function mountWith(props: any) {
  return mount(CalcScriptEditor, {
    props,
    global: {
      plugins: [ElementPlus],
      stubs: {
        CodeMirrorGroovy: {
          name: 'CodeMirrorGroovy',
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<div data-test="cm-stub">{{ modelValue }}</div>'
        },
        // el-dialog 使用 Teleport, 在 jsdom 中 footer 插槽内容可能 teleport
        // 到 document.body, 导致 wrapper.find 找不到按钮。stub 后内联渲染。
        ElDialog: {
          name: 'ElDialog',
          props: ['modelValue', 'title', 'width', 'closeOnClickModal', 'destroyOnClose'],
          emits: ['update:modelValue'],
          template: '<div class="el-dialog-stub"><slot></slot><div class="el-dialog__footer"><slot name="footer"></slot></div></div>'
        }
      }
    }
  })
}
