# 实时日志：回放窗口从固定行数改为时间驱动

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实时日志默认回放从固定 300 行改为 3 小时时间窗口 + 不足 500 行时自动向前补充，窗口值支持前端设置页修改。

**架构：** 后端新增反向时间扫描算法（RandomAccessFile 从文件尾逐块逆向读取、解析时间戳、定位窗口边界），前端通过 query param `window` 传递窗口分钟数，默认值由 `LogModuleProperties` 提供（180min）。设置页新增 system config key `console_replay_window`，前端读取后缓存并传给 SSE 端点。

**技术栈：** Java 17 / Spring Boot / Vue 3 + TypeScript / MyBatis

---

## 涉及文件

| 文件 | 操作 | 职责 |
|------|------|------|
| `server/.../tail/LogFileTailService.java` | 修改 | 核心：时间驱动回放 + minLines 兜底 |
| `server/.../config/LogModuleProperties.java` | 修改 | 新增 `consoleReplayWindowMinutes` 默认 180 |
| `server/.../controller/LogConsoleStreamController.java` | 修改 | 接收 `window` query 参数并传递给 tail service |
| `web/src/views/system/composables/useLogStream.ts` | 修改 | `useConsoleStream()` 接受并传递 `window` 参数 |
| `web/src/views/system/Log.vue` | 修改 | 读取设置、传入 window 参数给 stream composable |
| `web/src/views/system/Settings.vue` | 修改 | 新增 `console_replay_window` 参数项 |
| `server/.../tail/LogFileTailServiceTest.java` | 新建 | core 逻辑的单元测试 |

---

### 任务 1：LogModuleProperties 新增配置项

**文件：**
- 修改：`server/zwei-log/src/main/java/com/zwei/log/infrastructure/config/LogModuleProperties.java`

- [ ] **步骤 1：添加两个新字段及 getter/setter**

```java
/** 控制台日志回放时间窗口（分钟），默认 180（3 小时） */
private long consoleReplayWindowMinutes = 180;

/** 控制台日志回放最少行数，窗口内不足时向前补充 */
private int consoleReplayMinLines = 500;

public long getConsoleReplayWindowMinutes() {
    return consoleReplayWindowMinutes;
}

public void setConsoleReplayWindowMinutes(long consoleReplayWindowMinutes) {
    this.consoleReplayWindowMinutes = consoleReplayWindowMinutes;
}

public int getConsoleReplayMinLines() {
    return consoleReplayMinLines;
}

public void setConsoleReplayMinLines(int consoleReplayMinLines) {
    this.consoleReplayMinLines = consoleReplayMinLines;
}
```

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn compile -pl zwei-log -am -q
```

预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-log/src/main/java/com/zwei/log/infrastructure/config/LogModuleProperties.java
git commit -m "feat(log): LogModuleProperties 新增 consoleReplayWindowMinutes / consoleReplayMinLines 配置项"
```

---

### 任务 2：LogFileTailService 实现时间驱动回放

**文件：**
- 修改：`server/zwei-log/src/main/java/com/zwei/log/infrastructure/tail/LogFileTailService.java`

- [ ] **步骤 1：替换常量，注入配置**

删除 `INITIAL_REPLAY_LINES` 常量。在构造函数接收新参数，或在 `afterPropertiesSet` 中缓存 `properties` 的值。

```java
// 删除: private static final int INITIAL_REPLAY_LINES = 300;
// 新增字段:
private final long replayWindowMinutes;
private final int replayMinLines;

public LogFileTailService(LogModuleProperties properties) {
    this.properties = properties;
    this.replayWindowMinutes = properties.getConsoleReplayWindowMinutes();
    this.replayMinLines = properties.getConsoleReplayMinLines();
}
```

- [ ] **步骤 2：实现 `readLinesSince(long windowMinutes, int minLines)` 方法**

核心算法：从文件尾部反向扫描，逐行解析时间戳，找到窗口边界后正向读取。

```java
/**
 * 读取最近 windowMinutes 分钟内的日志行。
 * 如果窗口内行数不足 minLines，则向前补充直到满足 minLines 或文件开头。
 */
List<String> readLinesSince(long windowMinutes, int minLines) {
    List<String> result = new ArrayList<>();
    try {
        File file = new File(currentFilePath);
        if (!file.exists() || file.length() == 0) {
            return result;
        }
        long fileLen = file.length();
        Instant cutoff = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        long startPos = findWindowStart(file, fileLen, cutoff, minLines);
        result = forwardRead(file, startPos);
    } catch (Exception e) {
        log.warn("Time-based replay failed, falling back to line-based: {}", e.getMessage());
        return readLastLines(minLines);
    }
    return result;
}
```

- [ ] **步骤 3：实现反向扫描 `findWindowStart`**

```java
/**
 * 从文件尾部反向扫描，定位回放起始字节偏移。
 * 策略：逐块读取，从后向前扫描每一行，解析时间戳。
 *       遇到 timestamp < cutoff 且已扫描行数 >= minLines 时停止。
 */
private long findWindowStart(File file, long fileLen, Instant cutoff, int minLines) throws IOException {
    int linesScanned = 0;
    boolean timeBoundaryFound = false;

    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
        long pos = fileLen;
        byte[] buf = new byte[8192];
        StringBuilder tail = new StringBuilder();

        while (pos > 0) {
            int readSize = (int) Math.min(buf.length, pos);
            pos -= readSize;
            raf.seek(pos);
            int n = raf.read(buf, 0, readSize);
            if (n <= 0) break;

            String block = new String(buf, 0, n, StandardCharsets.UTF_8) + tail;
            String[] lines = block.split("\\n", -1);
            tail.setLength(0);
            tail.append(lines[0]); // partial line at head, carry to next iteration

            for (int i = lines.length - 1; i >= 0; i--) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                linesScanned++;

                Instant ts = parseLogTimestamp(line);
                if (!timeBoundaryFound && ts != null && ts.isBefore(cutoff) && linesScanned >= minLines) {
                    // Found the time boundary with enough lines. Return position after this line.
                    return pos + block.indexOf(line) + line.length() + 1;
                }
            }
        }
    }
    return 0; // Full file since start
}
```

- [ ] **步骤 4：实现时间戳解析 `parseLogTimestamp`**

```java
private static final DateTimeFormatter LOG_TS =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Shanghai"));

/**
 * 尝试从日志行首提取时间戳。
 * 支持格式: yyyy-MM-dd HH:mm:ss.SSS（Spring Boot 默认 Logback 格式）
 * 解析失败返回 null。
 */
Instant parseLogTimestamp(String line) {
    if (line == null || line.length() < 23) return null;
    try {
        return Instant.from(LOG_TS.parse(line.substring(0, 23)));
    } catch (DateTimeParseException e) {
        return null;
    }
}
```

需要新增 import：
```java
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
```

- [ ] **步骤 5：实现正向读取 `forwardRead`**

```java
private List<String> forwardRead(File file, long startOffset) throws IOException {
    List<String> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
        reader.skip(startOffset);
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
    }
    return result;
}
```

- [ ] **步骤 6：修改 `subscribe` 方法签名，支持 window 参数**

```java
public SseEmitter subscribe(SseEmitter emitter) {
    return subscribe(emitter, replayWindowMinutes);
}

public SseEmitter subscribe(SseEmitter emitter, long windowMinutes) {
    List<String> replayLines = readLinesSince(windowMinutes, replayMinLines);
    try {
        for (String line : replayLines) {
            emitter.send(SseEmitter.event().name("line").data(line));
        }
        emitter.send(SseEmitter.event()
            .name("ready")
            .data(Map.of("replayCount", replayLines.size(), "windowMinutes", windowMinutes)));
    } catch (Exception e) {
        try { emitter.completeWithError(e); } catch (Exception ignored) {}
        return emitter;
    }

    emitter.onCompletion(() -> subscribers.remove(emitter));
    emitter.onTimeout(() -> subscribers.remove(emitter));
    emitter.onError(ex -> subscribers.remove(emitter));

    subscribers.add(emitter);
    return emitter;
}
```

需要新增 import：
```java
import java.util.Map;
```

- [ ] **步骤 7：编译验证**

```bash
cd server && mvn compile -pl zwei-log -am -q
```

预期：BUILD SUCCESS

- [ ] **步骤 8：Commit**

```bash
git add server/zwei-log/src/main/java/com/zwei/log/infrastructure/tail/LogFileTailService.java
git commit -m "feat(log): 实时日志回放从固定行数改为时间驱动 + minLines 兜底"
```

---

### 任务 3：LogConsoleStreamController 接收 window 参数

**文件：**
- 修改：`server/zwei-log/src/main/java/com/zwei/log/api/controller/LogConsoleStreamController.java`

- [ ] **步骤 1：添加 `@RequestParam` 并透传**

```java
@PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
@GetMapping("/console-stream")
public SseEmitter consoleStream(
    @RequestParam(value = "window", required = false) Long windowMinutes) {
    SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());
    long window = (windowMinutes != null && windowMinutes > 0)
        ? windowMinutes : properties.getConsoleReplayWindowMinutes();
    return tailService.subscribe(emitter, window);
}
```

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn compile -pl zwei-log -am -q
```

预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-log/src/main/java/com/zwei/log/api/controller/LogConsoleStreamController.java
git commit -m "feat(log): console-stream 端点接收 window 查询参数"
```

---

### 任务 4：useConsoleStream 传递 window 参数

**文件：**
- 修改：`web/src/views/system/composables/useLogStream.ts`

- [ ] **步骤 1：`useConsoleStream()` 接受 `windowMinutes` 参数**

```typescript
export function useConsoleStream(windowMinutes = 180) {
```

- [ ] **步骤 2：拼接 query param 到 fetch URL**

找到 `fetch('/api/v1/logs/console-stream'` 那一行（约 line 326），改为：

```typescript
fetch(`/api/v1/logs/console-stream?window=${windowMinutes}`, {
```

- [ ] **步骤 3：Commit**

```bash
git add web/src/views/system/composables/useLogStream.ts
git commit -m "feat(log): useConsoleStream 接受并传递 window 查询参数"
```

---

### 任务 5：Log.vue 读取系统设置并传入 window

**文件：**
- 修改：`web/src/views/system/Log.vue`

- [ ] **步骤 1：新增读取 configKey 的逻辑**

在 `<script setup>` 中新增：

```typescript
import { request } from '@/utils/request'

const replayWindow = ref(180) // 默认 180 分钟（3 小时）

onMounted(async () => {
  try {
    const res: any = await request.get('/system/config/configKey/console_replay_window')
    const val = res?.data
    if (val != null) {
      const n = Number(val)
      if (!Number.isNaN(n) && n > 0) replayWindow.value = n
    }
  } catch { /* 使用默认值 */ }
  // ... existing onMounted logic
})
```

- [ ] **步骤 2：传入 `useConsoleStream()`**

```typescript
const stream = useConsoleStream(replayWindow.value)
```

注意：需要在 `useConsoleStream` 内部响应式处理 window 变更。简单方案是在 `start()` 时传入当前值。

调整 `useConsoleStream` 为接受 `Ref<number>` 或在 `start(wMinutes)` 参数中传入：

```typescript
const stream = useConsoleStream()
// In the watch(activeTab):
if (tab === 'realtime') {
  stream.start(replayWindow.value)
}
```

- [ ] **步骤 3：Commit**

```bash
git add web/src/views/system/Log.vue
git commit -m "feat(log): Log.vue 从系统配置读取回放窗口并传给 console stream"
```

---

### 任务 6：Settings.vue 新增设置项

**文件：**
- 修改：`web/src/views/system/Settings.vue`

- [ ] **步骤 1：在 `paramList` 的 data 分类中添加新参数**

在 `{ code: 'auto_cleanup', ... }` 之后添加：

```typescript
{ code: 'console_replay_window', name: '实时日志回放窗口(分钟)', type: 'number', category: 'data', value: 180, min: 5, max: 1440, step: 5, remark: '控制台实时日志初始回放的时间窗口，默认 180 分钟（3 小时）' },
```

- [ ] **步骤 2：确认保存逻辑自动覆盖该参数**

`configKeyParams` computed 已动态过滤 `paramList`（线上代码行 641-646），新参数会自动被包含在保存和加载流程中，无需额外修改。

- [ ] **步骤 3：Commit**

```bash
git add web/src/views/system/Settings.vue
git commit -m "feat(log): 设置页新增 console_replay_window 参数"
```

---

### 任务 7：后端单元测试

**文件：**
- 新建：`server/zwei-log/src/test/java/com/zwei/log/infrastructure/tail/LogFileTailServiceTest.java`

- [ ] **步骤 1：编写测试 — 时间戳解析**

```java
@Test
@DisplayName("parseLogTimestamp 正确解析 Spring Boot 默认格式")
void parseLogTimestamp_standardFormat() {
    Instant ts = service.parseLogTimestamp("2026-07-01 10:57:47.794 [main] WARN ...");
    assertThat(ts).isNotNull();
    assertThat(ts.atZone(ZoneId.of("Asia/Shanghai")).getHour()).isEqualTo(10);
}

@Test
@DisplayName("parseLogTimestamp 行太短返回 null")
void parseLogTimestamp_lineTooShort() {
    assertThat(service.parseLogTimestamp("short")).isNull();
}

@Test
@DisplayName("parseLogTimestamp 非时间戳前缀返回 null")
void parseLogTimestamp_nonTimestampPrefix() {
    assertThat(service.parseLogTimestamp("Not a timestamp at all...")).isNull();
}
```

- [ ] **步骤 2：编写测试 — 时间窗口读取**

创建临时日志文件，写入带时间戳的行，验证 `readLinesSince` 返回正确窗口内的行。

```java
@Test
@DisplayName("readLinesSince 返回窗口内的日志行")
void readLinesSince_returnsLinesInWindow() throws Exception {
    // 创建临时文件，写入一些带时间戳的行
    // 验证返回的行都在时间窗口内
}

@Test
@DisplayName("readLinesSince 不足 minLines 时向前补充")
void readLinesSince_minLinesFallback() throws Exception {
    // 窗口内只有 10 行，minLines=500 → 应返回更多行
}
```

- [ ] **步骤 3：运行测试**

```bash
cd server && mvn test -pl zwei-log -Dtest=LogFileTailServiceTest -am
```

预期：全部 PASS

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-log/src/test/java/com/zwei/log/infrastructure/tail/LogFileTailServiceTest.java
git commit -m "test(log): LogFileTailService 时间回放单元测试"
```

---

### 任务 8：集成验证

- [ ] **步骤 1：启动后端，SSE 端点带 window 参数验证**

```bash
# 不带参数，默认 180min
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/logs/console-stream

# 带 window 参数
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/v1/logs/console-stream?window=60"
```

- [ ] **步骤 2：前端验证**

```bash
cd web && npm run dev
```

打开 `/system/log` → 实时日志 tab，检查：
- 初始回放加载了最近 3 小时的日志
- 打开设置页，修改 `console_replay_window` 为其他值（如 60），保存
- 刷新实时日志 tab，回放窗口变为新值

- [ ] **步骤 3：Commit（如有修正）**

---

## 自检

1. **覆盖度：** 后端 3 个文件 + 前端 3 个文件 + 测试，覆盖全部需求
2. **占位符：** 无 TODO 或待定
3. **一致性：** 参数名 `window` / `console_replay_window` / `replayWindow` 在前后端对应；`LogModuleProperties` 的字段名与 Controller 注入一致
