<template>
  <div
    ref="terminalRef"
    class="log-terminal"
    @scroll="onScroll"
  >
    <div v-if="!visibleLines.length" class="terminal-empty">
      <span class="cursor">&gt; <span class="cursor-blink">_</span></span>
    </div>
    <div
      v-for="(line, i) in enrichedLines"
      :key="i"
      class="terminal-line"
      :class="levelClass(line.level)"
    >
      <template v-if="line.parts">
        <span class="ts">{{ line.parts.timestamp }}</span>
        <span class="th">[</span><span class="tn">{{ line.parts.thread }}</span><span class="th">]</span>
        <span class="lv" :style="{ color: levelHex(line.parts.level) }">{{ line.parts.level }}</span>
        <span class="lg">{{ line.parts.logger }}</span>
        <span class="sp">-</span>
        <span class="mi">[{{ line.parts.method }}]</span>
        <span class="sp">-</span>
        <span class="msg">{{ line.parts.message }}</span>
      </template>
      <template v-else-if="line.raw">
        <span class="msg" :style="{ color: levelHex(line.level) }">{{ line.raw }}</span>
      </template>
      <template v-else>
        <span class="ts">{{ line.timestamp }}</span>
        <span class="lv" :style="{ color: levelHex(line.level) }">{{ line.level }}</span>
        <span class="msg">{{ line.message }}</span>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { SseType, TerminalLine } from '../composables/useLogStream'

const props = defineProps<{
  lines: readonly TerminalLine[]
  activeTypes?: ReadonlySet<SseType>
  levelFilter?: ReadonlySet<string>
  autoScroll: boolean
}>()

// ---------------------------------------------------------------------------
// Token system — terminal-inspired palette
// ---------------------------------------------------------------------------

const HEX: Record<string, string> = {
  bg:        '#0d1117',
  text:      '#c9d1d9',
  muted:     '#484f58',
  ts:        '#58a6ff',
  threadB:   '#30363d',
  thread:    '#8b949e',
  debug:     '#6e7681',
  info:      '#56d364',
  warn:      '#e3b341',
  error:     '#f85149',
  critical:  '#ff6b9d',
  logger:    '#79c0ff',
  method:    '#a5d6ff',
  sep:       '#30363d',
  stackPath: '#e3b341',
  stackLoc:  '#58a6ff',
}

const levelHex = (level: string): string => {
  const m: Record<string, string> = {
    DEBUG: HEX.debug, INFO: HEX.info, WARN: HEX.warn, WARNING: HEX.warn,
    ERROR: HEX.error, CRITICAL: HEX.critical, FATAL: HEX.critical, TRACE: HEX.debug,
  }
  return m[level.toUpperCase()] || HEX.text
}

const levelClass = (level: string): string => {
  const lv = level.toUpperCase()
  if (lv === 'WARN' || lv === 'WARNING') return 'border-warn'
  if (lv === 'ERROR') return 'border-error'
  if (lv === 'CRITICAL' || lv === 'FATAL') return 'border-critical'
  return ''
}

// ---------------------------------------------------------------------------
// Logback raw line parser
//   Pattern: %d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} - [%method,%line] - %msg%n
// ---------------------------------------------------------------------------

interface ParsedParts {
  timestamp: string
  thread: string
  level: string
  logger: string
  method: string
  message: string
}

const RE_LOGBACK = /^(\d{2}:\d{2}:\d{2}\.\d{3})\s+\[([^\]]*)\]\s+(DEBUG|INFO|WARN(?:ING)?|ERROR|CRITICAL|FATAL|TRACE)\s{1,2}(\S+)\s+-\s+\[([^\]]*)\]\s+-\s+(.*)$/

const parseLogback = (raw: string): ParsedParts | null => {
  const m = raw.match(RE_LOGBACK)
  if (!m) return null
  return {
    timestamp: m[1],
    thread: m[2],
    level: m[3].toUpperCase() === 'WARNING' ? 'WARN' : m[3].toUpperCase(),
    logger: m[4],
    method: m[5],
    message: m[6],
  }
}

interface EnrichedLine {
  timestamp: string
  level: string
  logType: string
  message: string
  raw?: string
  parts: ParsedParts | null
}

const enrichedLines = computed<EnrichedLine[]>(() => {
  let source = props.activeTypes
    ? props.lines.filter((l) => props.activeTypes!.has(l.logType.toLowerCase() as SseType))
    : props.lines

  if (props.levelFilter && props.levelFilter.size > 0) {
    source = source.filter((l) => props.levelFilter!.has(l.level))
  }

  return source.map((l) => ({
    ...l,
    parts: l.raw ? parseLogback(l.raw) : null,
  }))
})

// ---------------------------------------------------------------------------
// Filtering
// ---------------------------------------------------------------------------

const visibleLines = computed(() => enrichedLines.value)

// ---------------------------------------------------------------------------
// Scroll anchoring
// ---------------------------------------------------------------------------

const terminalRef = ref<HTMLElement | null>(null)
const userScrolledUp = ref(false)

const scrollToBottom = () => {
  const el = terminalRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const isNearBottom = (): boolean => {
  const el = terminalRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 48
}

const onScroll = () => {
  userScrolledUp.value = !isNearBottom()
  if (isNearBottom()) userScrolledUp.value = false
}

watch(
  () => props.lines,
  async () => {
    if (props.autoScroll && !userScrolledUp.value) {
      await nextTick()
      scrollToBottom()
    }
  }
)

watch(
  () => props.levelFilter,
  async () => {
    if (props.autoScroll) {
      userScrolledUp.value = false
      await nextTick()
      scrollToBottom()
    }
  }
)

watch(
  () => props.autoScroll,
  async (on) => {
    if (on) {
      userScrolledUp.value = false
      await nextTick()
      scrollToBottom()
    }
  }
)
</script>

<style scoped>
.log-terminal {
  --bg:       #0d1117;
  --text:     #c9d1d9;
  --muted:    #484f58;
  --ts:       #58a6ff;
  --thread-b: #30363d;
  --thread:   #8b949e;
  --debug:    #6e7681;
  --info:     #56d364;
  --warn:     #e3b341;
  --error:    #f85149;
  --critical: #ff6b9d;
  --logger:   #79c0ff;
  --method:   #a5d6ff;
  --sep:      #30363d;

  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: var(--bg);
  border-radius: 10px;
  padding: 10px 0;
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'Consolas', 'Courier New', monospace;
  font-size: 13.5px;
  line-height: 1.55;
  scroll-behavior: auto;
}

/* scrollbar — thin, subtle */
.log-terminal::-webkit-scrollbar { width: 5px; }
.log-terminal::-webkit-scrollbar-track { background: transparent; }
.log-terminal::-webkit-scrollbar-thumb { background: #21262d; border-radius: 3px; }
.log-terminal::-webkit-scrollbar-thumb:hover { background: #30363d; }

/* ── empty state ── */
.terminal-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--muted);
  font-size: 14px;
}

.cursor { color: var(--text); }
.cursor-blink { animation: blink 1.2s step-end infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

/* ── line ── */
.terminal-line {
  padding: 1px 12px 1px 10px;
  white-space: pre-wrap;
  word-break: break-all;
  border-left: 2px solid transparent;
  transition: background 80ms;
}

.terminal-line:hover {
  background: rgba(255, 255, 255, 0.03);
}

.terminal-line.border-warn  { border-left-color: var(--warn); }
.terminal-line.border-error { border-left-color: var(--error); }
.terminal-line.border-critical { border-left-color: var(--critical); }

/* ── token colours ── */
.ts { color: var(--ts); flex-shrink: 0; }               /* timestamp */
.th { color: var(--thread-b); flex-shrink: 0; }          /* bracket */
.tn { color: var(--thread); flex-shrink: 0; }            /* thread name */
.lv { flex-shrink: 0; font-weight: 600; }                /* level — colour set inline */
.lg { color: var(--logger); flex-shrink: 0; }            /* logger */
.sp { color: var(--sep); flex-shrink: 0; }               /* separator dash */
.mi { color: var(--method); flex-shrink: 0; }            /* [method,line] */
.msg { color: var(--text); }                             /* message */

/* spacing between tokens */
.ts { margin-right: 6px; }
.th + .tn + .th { /* brackets hug thread */ }
.th:last-of-type { margin-right: 6px; }
.lv { margin-right: 4px; }
.lg { margin-right: 6px; }
.sp { margin: 0 4px; }
.mi { margin-right: 0; }
</style>
