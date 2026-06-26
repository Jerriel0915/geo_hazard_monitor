/**
 * Vitest 全局 setup。
 *
 * 仅 polyfill jsdom 缺失的 API, 不引入业务副作用。
 */
import { vi } from 'vitest'

// jsdom 未实现 Range.prototype.getClientRects / getBoundingClientRect,
// CodeMirror 6 的 DocView.measureTextSize 会异步调用并在测试结束后抛出
// "TypeError: textRange(...).getClientRects is not a function",
// 该未处理异常会被 vitest 归因到邻近的测试用例, 导致跨文件 flaky 失败。
// 这里在 DOMRect 上返回固定零值即可让 CodeMirror 测量逻辑走通。
if (typeof Range !== 'undefined' && !Range.prototype.getClientRects) {
  Range.prototype.getClientRects = () => []
}
if (typeof Range !== 'undefined' && !Range.prototype.getBoundingClientRect) {
  Range.prototype.getBoundingClientRect = () => ({
    x: 0, y: 0, top: 0, left: 0, right: 0, bottom: 0, width: 0, height: 0, toJSON: () => ({})
  }) as DOMRect
}
if (typeof Element !== 'undefined' && !Element.prototype.getClientRects) {
  Element.prototype.getClientRects = () => []
}
