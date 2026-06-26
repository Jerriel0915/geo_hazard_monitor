import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import CodeMirrorGroovy from '../CodeMirrorGroovy.vue'

describe('CodeMirrorGroovy', () => {
  it('通过 modelValue 初始化编辑器内容', async () => {
    const wrapper = await mountWithContent('// hello\nreturn 42')
    const cmContent = wrapper.find('.cm-content').text()
    expect(cmContent).toContain('hello')
    expect(cmContent).toContain('return')
  })

  it('用户编辑触发 update:modelValue', async () => {
    const wrapper = await mountWithContent('// initial')
    // 模拟编辑器内容变化: 直接调用 dispatch
    const cmContent = wrapper.find('.cm-content').element as HTMLElement
    // CodeMirror 用 contentEditable, 通过 input 事件模拟
    cmContent.textContent = '// changed'
    cmContent.dispatchEvent(new InputEvent('input', { bubbles: true }))
    await flushPromises()
    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toContain('changed')
  })

  it('modelValue 外部变化时同步到编辑器', async () => {
    const wrapper = await mountWithContent('// initial')
    await wrapper.setProps({ modelValue: '// externally changed' })
    await flushPromises()
    expect(wrapper.find('.cm-content').text()).toContain('externally changed')
  })

  it('挂载 CodeMirror 实例,有 .cm-editor 根节点', async () => {
    const wrapper = await mountWithContent('')
    expect(wrapper.find('.cm-editor').exists()).toBe(true)
    // 应有行号槽
    expect(wrapper.find('.cm-gutters').exists()).toBe(true)
  })
})

async function mountWithContent(content: string) {
  return mount(CodeMirrorGroovy, {
    props: { modelValue: content },
    global: {
      plugins: []
    }
  })
}
