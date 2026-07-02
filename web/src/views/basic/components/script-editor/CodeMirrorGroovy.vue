<template>
  <div ref="hostRef" class="cm-groovy-host" :style="{ minHeight: minHeight + 'px' }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { EditorView, keymap } from '@codemirror/view'
import { EditorState, Compartment } from '@codemirror/state'
import { basicSetup } from 'codemirror'
import { StreamLanguage } from '@codemirror/language'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { oneDark } from '@codemirror/theme-one-dark'
import { indentWithTab } from '@codemirror/commands'

const props = withDefaults(defineProps<{
  modelValue: string
  readonly?: boolean
  minHeight?: number
  theme?: 'light' | 'dark'
}>(), {
  readonly: false,
  minHeight: 280,
  theme: 'dark'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const hostRef = ref<HTMLDivElement>()
let view: EditorView | null = null
const themeCompartment = new Compartment()

// 防止内部变更触发 update 后又被外部 watch 回灌造成死循环
let internal = false

function themeExtension(theme: 'light' | 'dark') {
  return theme === 'dark' ? oneDark : []
}

onMounted(() => {
  if (!hostRef.value) return
  const extensions: any[] = [
    basicSetup,
    StreamLanguage.define(groovy),
    themeCompartment.of(themeExtension(props.theme)),
    keymap.of([indentWithTab]),
    EditorView.lineWrapping
  ]
  if (props.readonly) {
    extensions.push(EditorView.editable.of(false))
  }
  extensions.push(EditorView.updateListener.of(v => {
    if (v.docChanged && !internal) {
      internal = true
      emit('update:modelValue', v.state.doc.toString())
      queueMicrotask(() => { internal = false })
    }
  }))
  view = new EditorView({
    state: EditorState.create({
      doc: props.modelValue || '',
      extensions
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

// 主题切换
watch(() => props.theme, (newTheme) => {
  if (!view) return
  view.dispatch({
    effects: themeCompartment.reconfigure(themeExtension(newTheme))
  })
})

onBeforeUnmount(() => {
  view?.destroy()
  view = null
})
</script>

<style scoped>
.cm-groovy-host {
  height: 100%;
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
