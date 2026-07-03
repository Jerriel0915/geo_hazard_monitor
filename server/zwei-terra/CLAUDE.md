[根目录](../../CLAUDE.md) > [server](../) > **zwei-terra**

# zwei-terra — Terra AI Assistant (智能助手)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-terra**

## 模块职责

Terra 是知微系统的 AI 智能助手, 提供对话式交互、工具调用 (ReAct Loop)、值守模式看板操控三大能力。作为 Maven 父 POM 模块 (`packaging=pom`), 包含两个子模块:

- **zwei-terra-core** (`zwei-terra-core/`) — 接口协议层, 定义 Skill/Tool 抽象 (注解 + 接口 + DTO), 业务模块依赖此模块即可实现 Tool
- **zwei-terra-agent** (`zwei-terra-agent/`) — Agent 实现层, 包含 Anthropic API 调用、ReAct 循环引擎、SSE 流式对话、WebSocket 值守模式、技能/工具/人格管理、MySQL 持久化

### 子模块依赖关系

```
业务模块 (如 zwei-iot-*) → zwei-terra-core (加 @TerraTool 注解实现工具)
zwei-terra-agent → zwei-terra-core (扫描 @TerraTool Bean)
zwei-terra-agent → zwei-framework (权限/安全)
```

### 核心能力

- **SSE 流式对话** (`ChatService` + `TerraChatController`) — ReAct (Reasoning + Acting) 循环引擎, 调用 Anthropic API, SSE 推送
  `token`/`tool_call`/`tool_result`/`done`/`error` 事件
- **工具系统** (`ToolManager` + `@TerraTool` + `@ToolMethod`) — 启动时自动扫描 Spring 容器中 `@TerraTool` Bean, 反射构建
  `ToolDefinition`; 支持 code 工具 (反射调用) 和 config 工具 (数据库配置, HTTP 调用)
- **值守模式** (`TerraDutyWebSocketHandler` + `TerraDutyService` + `DashboardControlTool`) — WebSocket
  双向通信, Terra 可创建/操控看板面板 (图表/地图/表格/视频/图片/iframe), 设置 Terra 头像状态, 弹出告警; 前端通过
  terramens 协议交互
- **技能系统** (`SkillSyncService` + `SkillResolver` + `SkillManifest`) — 应用启动时从 `terra.skills.base-path/preset/`
  扫描预置 `SKILL.md`, 解析为 `SkillManifest`, 同步到 `terra_skill` 表
- **人格系统** (`ITerraPersonalityService`) — 管理 AI 角色人格配置 (角色名/系统提示词/参数), 对话时动态构建系统提示词
- **模型配置管理** (`ITerraModelConfigService`) — AI 模型提供商/端点/API Key/模型名/温度等参数管理, 支持激活/停用切换

## 关键依赖

### zwei-terra-core

- `zwei-common` (公共事件/基础类)
- lombok

### zwei-terra-agent

- `zwei-terra-core` (父模块, Skill/Tool 协议)
- `zwei-framework` (安全/权限/MyBatis/Redis 配置)
- `spring-boot-starter-webflux` (WebClient: Anthropic API HTTP 调用 + SSE 流式解析)
- `spring-boot-starter-websocket` (值守模式 WebSocket 实时双向通信)
- lombok

## 主要子包 (zwei-terra-core)

| 子包                  | 职责                                                              |
|---------------------|-----------------------------------------------------------------|
| `skill`             | `SkillManifest` (技能清单 POJO) / `SkillTrigger` (触发器配置)            |
| `tool`              | `TerraTool` (@注解) / `ToolMethod` (@注解) / `ToolDefinition` (工具定义) / `TerraBackendTool` (标记接口) / `ToolResult` (执行结果) |

## 主要子包 (zwei-terra-agent)

| 子包               | 职责                                                                                                                          |
|------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `chat`           | `ChatService` (ReAct 循环引擎) / `AnthropicChatModel` (API 调用 + SSE 流式解析) / `TerraSseEmitter` (SSE 事件封装)                            |
| `controller`     | `TerraChatController` (SSE 对话 + 前端工具回调) / `TerraConversationController` (会话管理) / `TerraPersonalityController` / `TerraModelConfigController` / `TerraSkillController` / `TerraToolController` |
| `config`         | `TerraAutoConfiguration` (自动装配入口, `@ComponentScan("com.zwei.terra")`) / `TerraProperties` (`terra.*` 配置绑定)                   |
| `domain`         | `TerraConversation` / `TerraMessage` / `TerraModelConfig` / `TerraPersonality` / `TerraSkill` / `TerraTool`                    |
| `duty`           | `TerraDutyWebSocketHandler` (WebSocket 消息处理) / `TerraDutyService` (值守 ReAct Loop) / `DashboardControlTool` (面板操控工具, `@TerraTool`) / `TerraDutySessionManager` / `TerraDutyHandshakeInterceptor` / `DutyProtocol` (terramens 协议) / `TerraDutyWebSocketConfig` |
| `mapper`         | 7 个 MyBatis Mapper: `TerraConversationMapper` / `TerraMessageMapper` / `TerraModelConfigMapper` / `TerraPersonalityMapper` / `TerraSkillMapper` / `TerraSkillToolMapper` / `TerraToolMapper` |
| `service`        | `ITerraModelConfigService` / `ITerraPersonalityService` / `ITerraSkillService` / `ITerraToolService` (4 个接口 + 实现)               |
| `skill`          | `SkillSyncService` (预置技能同步) / `SkillResolver` (SKILL.md 解析)                                                                  |
| `tool`           | `ToolManager` (工具注册/发现/执行, code + config 双来源) / `SystemQueryTool` (系统数据查询工具, `@TerraTool`) / `ToolRegistration` (工具注册 POJO)        |

## 对外接口 (Controller)

### 对话端点

| 路径                                  | 方法   | 权限            | 职责                                |
|-------------------------------------|------|---------------|-----------------------------------|
| `/api/v1/terra/chat`                | POST | `terra:chat`  | SSE 流式对话 (conversationId + message) |
| `/api/v1/terra/chat/tool-result`    | POST | `terra:chat`  | 前端工具执行结果回调 (callId + success + result) |

### 会话管理

| 路径                                            | 方法     | 权限           | 职责                              |
|-----------------------------------------------|--------|--------------|---------------------------------|
| `/api/v1/terra/conversations`                 | GET    | `terra:chat` | 当前用户会话列表 (按最后消息倒序)              |
| `/api/v1/terra/conversations/{id}/messages`   | GET    | `terra:chat` | 会话消息历史 (最多 100 条, 正序)            |
| `/api/v1/terra/conversations`                 | POST   | `terra:chat` | 新建会话 (title 可选)                 |
| `/api/v1/terra/conversations/{id}`            | DELETE | `terra:chat` | 逻辑删除会话 (含归属权验证)                |

### 人格管理

| 路径                                | 方法    | 权限                | 职责                   |
|-----------------------------------|-------|-------------------|----------------------|
| `/api/v1/terra/personality`       | GET   | `terra:settings`  | 获取人格配置列表             |
| `/api/v1/terra/personality`       | PUT   | `terra:settings`  | 更新人格配置 (角色名/系统提示词/参数) |
| `/api/v1/terra/personality/{id}/toggle` | PUT | `terra:settings` | 切换人格启用/停用状态          |

### 模型配置

| 路径                                      | 方法     | 权限                | 职责                        |
|-----------------------------------------|--------|-------------------|---------------------------|
| `/api/v1/terra/model-configs`           | GET    | `terra:settings`  | 获取模型配置列表                  |
| `/api/v1/terra/model-configs/{id}`      | GET    | `terra:settings`  | 获取模型配置详情                  |
| `/api/v1/terra/model-configs`           | POST   | `terra:settings`  | 新增模型配置 (提供商/端点/API Key/模型) |
| `/api/v1/terra/model-configs`           | PUT    | `terra:settings`  | 修改模型配置                    |
| `/api/v1/terra/model-configs/{id}`      | DELETE | `terra:settings`  | 删除模型配置                    |
| `/api/v1/terra/model-configs/{id}/activate` | PUT | `terra:settings` | 激活指定模型配置 (唯一激活)           |

### 技能管理

| 路径                                   | 方法     | 权限                | 职责            |
|--------------------------------------|--------|-------------------|---------------|
| `/api/v1/terra/skills`               | GET    | `terra:settings`  | 获取技能列表        |
| `/api/v1/terra/skills/{id}`          | GET    | `terra:settings`  | 获取技能详情        |
| `/api/v1/terra/skills/{id}/toggle`   | PUT    | `terra:settings`  | 启用/停用技能       |
| `/api/v1/terra/skills/{id}`          | DELETE | `terra:settings`  | 卸载技能          |

### 工具管理

| 路径                                     | 方法   | 权限                | 职责                             |
|----------------------------------------|------|-------------------|--------------------------------|
| `/api/v1/terra/tools`                  | GET  | `terra:settings`  | 获取工具列表                         |
| `/api/v1/terra/tools/{id}`             | GET  | `terra:settings`  | 获取工具详情                         |
| `/api/v1/terra/tools/{id}/toggle`      | PUT  | `terra:settings`  | 启用/停用工具                        |
| `/api/v1/terra/tools/skill/{skillId}`  | GET  | `terra:settings`  | 获取指定技能的工具列表                    |
| `/api/v1/terra/tools/refresh`          | POST | `terra:settings`  | 刷新工具注册 (重新扫描 code 工具 + 重新加载 config 工具) |

### 值守模式 WebSocket

| 路径                  | 协议        | 职责                    |
|---------------------|-----------|-----------------------|
| `/ws/duty`          | WebSocket | 值守模式 Dashboard 双向通信端点 |

## 核心实现类索引

### Chat 层 (P0)

| 类                   | 文件                                   | 职责                                                       |
|---------------------|--------------------------------------|----------------------------------------------------------|
| `ChatService`       | `chat/ChatService.java`              | ReAct 循环引擎: 会话管理 → 历史加载 → LLM 调用 → 工具执行(前端/后端) → 循环    |
| `AnthropicChatModel` | `chat/AnthropicChatModel.java`      | Anthropic Messages API HTTP 调用 + SSE `text_delta` 流式解析       |
| `TerraSseEmitter`   | `chat/TerraSseEmitter.java`          | SSE 事件封装: `sendToken`/`sendToolCall`/`sendToolResult`/`sendDone`/`sendError` |

### 工具系统 (P0)

| 类                     | 文件                                   | 职责                                                                 |
|-----------------------|--------------------------------------|--------------------------------------------------------------------|
| `ToolManager`         | `tool/ToolManager.java`              | 工具注册/发现/执行: `@PostConstruct` 扫描 `@TerraTool` Bean, 反射构建 ToolDefinition; code 工具 (反射) + config 工具 (HTTP) |
| `ToolRegistration`    | `tool/ToolRegistration.java`         | 工具注册 POJO: toolKey + definition + bean + method (code) / endpoint (config) |
| `SystemQueryTool`     | `tool/SystemQueryTool.java`          | 系统查询工具 (`@TerraTool(name="system.query")`): 设备/隐患点/告警/传感器/系统概览数据查询, 通过 JdbcTemplate 隔离业务模块 |
| `DashboardControlTool` | `duty/DashboardControlTool.java`    | 值守模式面板操控工具 (`@TerraTool(name="dashboard")`): createChart/createMap/createTable/createVideo/createImage/createIframe/destroyPanel/clearAllPanels/setTerraState/showAlert/mapNavigate |

### 值守模式 (P0)

| 类                                | 文件                                          | 职责                                                     |
|----------------------------------|---------------------------------------------|--------------------------------------------------------|
| `TerraDutyService`               | `duty/TerraDutyService.java`                | 值守 ReAct Loop: WebSocket 输入 → LLM 调用 → 工具执行 → terramens 协议输出 |
| `TerraDutyWebSocketHandler`      | `duty/TerraDutyWebSocketHandler.java`       | WebSocket 消息处理: 连接建立/接收/关闭, 解析 `chat_message` → `handleChat` |
| `TerraDutySessionManager`        | `duty/TerraDutySessionManager.java`         | WebSocket 会话管理: register/unregister/sendTo/broadcast         |
| `TerraDutyHandshakeInterceptor`  | `duty/TerraDutyHandshakeInterceptor.java`   | WebSocket 握手拦截: 提取 userId/username 存入 session attributes     |

### 技能系统 (P0)

| 类                  | 文件                                   | 职责                                              |
|--------------------|--------------------------------------|-------------------------------------------------|
| `SkillSyncService` | `skill/SkillSyncService.java`        | 预置技能同步: `@EventListener(ApplicationReadyEvent)` 扫描 preset/ 目录 |
| `SkillResolver`    | `skill/SkillResolver.java`           | SKILL.md 解析: 提取 name/description/triggers/tools/instructions   |

### Service 实现 (P1)

| 类                               | 文件                                                | 职责                                 |
|---------------------------------|---------------------------------------------------|------------------------------------|
| `TerraModelConfigServiceImpl`   | `service/impl/TerraModelConfigServiceImpl.java`   | 模型配置 CRUD + activate (唯一激活)        |
| `TerraPersonalityServiceImpl`   | `service/impl/TerraPersonalityServiceImpl.java`   | 人格配置管理 + `buildSystemPrompt()`    |
| `TerraSkillServiceImpl`         | `service/impl/TerraSkillServiceImpl.java`         | 技能管理 (列表/详情/启停/卸载)                |
| `TerraToolServiceImpl`          | `service/impl/TerraToolServiceImpl.java`          | 工具管理 (列表/详情/启停/刷新/技能关联)           |

## 关键流程

### ReAct Loop (ChatService.executeReactLoop)

1. **会话管理**: 获取或创建 `TerraConversation` (首次对话自动建)
2. **保存用户消息**: `TerraMessageMapper.insert()` user 消息
3. **获取模型配置**: `TerraModelConfigMapper.selectActive()`
4. **构建系统提示词**: `ITerraPersonalityService.buildSystemPrompt()`
5. **加载历史消息**: 最近 N 条 (可配置, 默认 20), 反转为正序
6. **获取工具列表**: `ToolManager.getEnabledToolDefinitions()` (code + config)
7. **ReAct 循环** (最多 `terra.chat.max-react-rounds` 轮, 默认 10):
   - 调用 `AnthropicChatModel.streamChat()` → SSE 逐字推送 token
   - `stop_reason = end_turn` 且无 tool_use → 保存 assistant 消息, 推送 done, 结束
   - 有 `tool_calls` → 逐个执行: 前端工具 (SSE 推送 + `CompletableFuture` 等待回调) / 后端工具 (反射调用), 推送
     tool_call + tool_result, 追加到 messages, 继续循环
   - 超过最大轮数 → 推送 error

### 值守模式 (TerraDutyService.executeDutyChat)

与 SSE 对话模式的差异:
- 输出目标: WebSocket (terramens 协议) 而非 SSE
- token 流: `streamingTimelineItem` (runId 分组) + `streamingCompleteTimelineItem`
- 系统提示词: 在标准人格基础上叠加值守模式指令 (面板操控指南)
- 不支持前端工具 (WebSocket 无 SSE 回调机制)
- 同一连接的并发消息排队 (单线程 ExecutorService)
- 连接生命周期: `onConnect` 发送欢迎 + `terraState` + 心跳, `onDisconnect` 清理

### 工具扫描 (ToolManager.scanCodeTools)

1. `applicationContext.getBeansWithAnnotation(@TerraTool)` 扫描所有标注了
   `@com.zwei.terra.core.tool.TerraTool` 的 Bean
2. 遍历每个 Bean 的 `@ToolMethod` 方法, 构建 `ToolDefinition` (name/description/execSide/parametersSchema)
3. 尝试写入 `terra_tool` 表 (`ensureCodeToolInDb`), 标记 source=code
4. config 工具从 `terra_tool` 表加载, source=config

### 技能同步 (SkillSyncService.syncPresetSkills)

1. `@EventListener(ApplicationReadyEvent)` 触发
2. 扫描 `terra.skills.base-path/preset/` 下所有子目录
3. 解析每个目录下的 `SKILL.md` → `SkillManifest` (via `SkillResolver`)
4. 已存在的 `skill_key` 不覆盖 (`existing == null` 才 insert)

## 配置属性 (`terra.*`)

```yaml
terra:
  skills:
    base-path: "~/.terra/skills"           # 技能文件系统基路径
  chat:
    max-react-rounds: 10                   # ReAct 循环最大轮数
    default-timeout-seconds: 30            # 默认工具调用超时（秒）
    max-history-messages: 20               # 加载历史消息最大条数
```

## 数据模型

- `terra_conversation` — 对话会话 (id / userId / title / status active|archived / lastMessageTime / messageCount / delFlag)
- `terra_message` — 对话消息 (id / conversationId / role user|assistant|tool / content / toolCalls JSON / toolCallId / createTime)
- `terra_model_config` — 模型配置 (id / provider / endpoint / apiKey / modelName / temperature / maxTokens / isActive / isEnabled)
- `terra_personality` — 人格配置 (id / roleName / systemPrompt / parameters / isActive / isEnabled)
- `terra_skill` — 技能 (id / skillKey / name / description / directoryPath / isPreset / isEnabled)
- `terra_tool` — 工具 (id / toolKey / name / description / source code|config / execSide frontend|backend / endpoint / parametersSchema JSON / isPreset / isEnabled / timeoutSeconds)

## Terra Core 协议 (zwei-terra-core)

业务模块如何实现一个后端工具:

1. 依赖 `zwei-terra-core` (不依赖 zwei-terra-agent)
2. 实现 `TerraBackendTool` 接口 (标记接口)
3. 在类上加 `@TerraTool(name="...", description="...")` 注解
4. 每个公开方法加 `@ToolMethod(description="...")` 注解
5. Spring 管理的 Bean → `ToolManager.scanCodeTools()`
   `@PostConstruct` 自动扫描注册

```java
@Component
@TerraTool(name = "my.domain", description = "业务领域工具", category = "business")
public class MyDomainTool implements TerraBackendTool {
    
    @ToolMethod(description = "查询设备信息。参数：deviceId(必填，设备ID)")
    public Map<String, Object> queryDevice(Long deviceId) {
        // ...
    }
}
```

## WebSocket 协议 (值守模式 terramens)

前端发送 (JSON):
```json
{
  "type": "command",
  "namespace": "core",
  "payload": {
    "action": "chat_message",
    "params": { "message": "查询系统概况" }
  }
}
```

后端推送事件:
- `handshake` — 连接握手响应
- `create_panel` — 创建面板 (id/type/title/data/position)
- `destroy_panel` — 销毁面板
- `streaming_timeline` — 流式 AI 回复 (`{content, isStreaming, runId}`)
- `streaming_complete_timeline` — 流式完成 (`{content, isComplete, runId}`)
- `timeline_item` — 时间线条目 (observation/action/thinking/warning, source: user/terra)
- `terra_state` — Terra 头像状态 (normal/info/caution/warning/critical)
- `alert` — 弹窗告警 (level/title/description)
- `heartbeat_trigger` — 心跳触发

## 测试与质量

- 运行: `mvn test -pl zwei-terra`

## 相关文件清单

### zwei-terra-core

- `pom.xml`
- `src/main/java/com/zwei/terra/core/skill/SkillManifest.java`
- `src/main/java/com/zwei/terra/core/skill/SkillTrigger.java`
- `src/main/java/com/zwei/terra/core/tool/TerraTool.java`
- `src/main/java/com/zwei/terra/core/tool/ToolMethod.java`
- `src/main/java/com/zwei/terra/core/tool/ToolDefinition.java`
- `src/main/java/com/zwei/terra/core/tool/ToolResult.java`
- `src/main/java/com/zwei/terra/core/tool/TerraBackendTool.java`

### zwei-terra-agent

- `pom.xml`
- `src/main/java/com/zwei/terra/agent/chat/ChatService.java` (P0)
- `src/main/java/com/zwei/terra/agent/chat/AnthropicChatModel.java` (P0)
- `src/main/java/com/zwei/terra/agent/tool/ToolManager.java` (P0)
- `src/main/java/com/zwei/terra/agent/tool/SystemQueryTool.java` (P0)
- `src/main/java/com/zwei/terra/agent/duty/DashboardControlTool.java` (P0)
- `src/main/java/com/zwei/terra/agent/duty/TerraDutyService.java` (P0)
- `src/main/java/com/zwei/terra/agent/duty/TerraDutyWebSocketHandler.java` (P0)
- `src/main/java/com/zwei/terra/agent/skill/SkillSyncService.java` (P1)
- `src/main/java/com/zwei/terra/agent/config/TerraAutoConfiguration.java`
- `src/main/java/com/zwei/terra/agent/config/TerraProperties.java`
- `src/main/java/com/zwei/terra/agent/controller/TerraChatController.java`
- `src/main/java/com/zwei/terra/agent/controller/TerraConversationController.java`
