<template>
  <div ref="hostRef" class="cm-groovy-host"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { EditorView, keymap } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { basicSetup } from 'codemirror'
import { StreamLanguage } from '@codemirror/language'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { oneDark } from '@codemirror/theme-one-dark'
import { indentWithTab } from '@codemirror/commands'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const hostRef = ref<HTMLDivElement>()
let view: EditorView | null = null

// 防止内部变更触发 update 后又被外部 watch 回灌造成死循环
let internal = false

onMounted(() => {
  if (!hostRef.value) return
  view = new EditorView({
    state: EditorState.create({
      doc: props.modelValue || '',
      extensions: [
        basicSetup,
        StreamLanguage.define(groovy),
        oneDark,
        keymap.of([indentWithTab]),
        EditorView.lineWrapping,
        EditorView.updateListener.of(v => {
          if (v.docChanged && !internal) {
            internal = true
            emit('update:modelValue', v.state.doc.toString())
            // 下一个微任务里解除锁,以便外部 setProps 后内部仍能响应新输入
            queueMicrotask(() => { internal = false })
          }
        })
      ]
    }),
    parent: hostRef.value
  })
})

// 外部 modelValue 变化 → 同步到编辑器 (但要避免回环)
watch(() => props.modelValue, (newVal) => {
  if (!view || internal) return
  const current = view.state.doc.toString()
  if (newVal === current) return
  internal = true
  view.dispatch({
    changes: { from: 0, to: current.length, insert: newVal || '' }
  })
  queueMicrotask(() => { internal = false })
})

onBeforeUnmount(() => {
  view?.destroy()
  view = null
})
</script>

<style scoped>
.cm-groovy-host {
  height: 100%;
  min-height: 280px;
  font-size: 13px;
}

.cm-groovy-host :deep(.cm-editor) {
  height: 100%;
  border-radius: 4px;
}

.cm-groovy-host :deep(.cm-scroller) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  line-height: 1.6;
}
</style>
