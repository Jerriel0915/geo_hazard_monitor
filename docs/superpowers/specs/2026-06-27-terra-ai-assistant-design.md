# Terra 智能助手设计规格

> 创建日期: 2026-06-27
> 状态: 待审查
> 作者: brainstorming session

## 1. 概述

Terra 是知微（Zwei）地质灾害监测管理系统内置的 AI 智能助手。基于 Spring AI Alibaba 底座开发，作为完全隔离的 Maven 模块嵌入现有系统，后续可拆解为独立服务。

### 1.1 MVP 范围

第一批交付（MVP）包含：
- 全部设置中心（人格定义、模型配置、技能管理、工具管理）
- 基础对话能力（SSE 流式回复）
- React Loop 工具调用框架（前端/后端双向执行）

后续批次将逐步添加具体的预置技能和工具实现（设备查询、告警查询、页面导航、报告生成等）。

### 1.2 设计约束

| 约束 | 说明 |
|------|------|
| 模块隔离 | terra 模块完全隔离，只依赖 zwei-common，不直接依赖业务模块 |
| 模型协议 | 只支持 Anthropic Messages API 协议（兼容任何符合此协议的服务） |
| 配置范围 | 全局共享配置（管理员配置一次，所有用户共用） |
| 对话持久化 | 支持多会话管理，历史对话持久化到 MySQL |
| 人格分层 | 核心灵魂（不可改）+ 可编辑角色外衣（参考 OpenClaw） |
| 技能规范 | 遵循 Anthropic skill 规范，目录式管理 + 可执行脚本 |
| 工具模型 | 混合模式：核心工具代码注册 + 扩展工具配置化创建 |

## 2. 模块架构

### 2.1 Maven 模块结构

```
server/
├── pom.xml                           # zwei 根 POM, 新增 <module>zwei-terra</module>
├── zwei-terra/                       # 父模块 (packaging: pom)
│   ├── pom.xml                      # parent: zwei, 声明两个子 module
│   ├── zwei-terra-core/             # 接口协议层 — 业务模块依赖此模块实现 tool
│   │   ├── pom.xml                 # parent: zwei-terra, 依赖: zwei-common
│   │   └── src/main/java/com/zwei/terra/core/
│   │       ├── tool/                # @TerraTool 注解、TerraTool/TerraBackendTool 接口、@ToolMethod、ToolSpec
│   │       ├── skill/               # SkillManifest、SkillSpec 规范接口
│   │       ├── model/               # 共享 DTO (ToolResult, ToolDefinition, ChatMessage 等)
│   │       └── event/               # 事件契约 (TerraActionEvent 等，预留前端操作)
│   │
│   └── zwei-terra-agent/            # Agent 实现 — 不依赖任何业务模块
│       ├── pom.xml                 # parent: zwei-terra, 依赖: zwei-terra-core + spring-ai-alibaba
│       └── src/main/java/com/zwei/terra/agent/
│           ├── config/              # Spring AI 配置、Anthropic ChatModel Bean
│           ├── controller/          # REST + SSE 控制器
│           ├── domain/              # 数据库实体类
│           ├── mapper/              # MyBatis Mapper
│           ├── service/             # 对话管理、设置管理 Service
│           │   └── impl/
│           ├── skill/               # SkillRegistry (发现 + 解析)、SkillResolver (运行时拼装)
│           ├── tool/                # ToolManager (Spring 容器扫描 TerraTool 实现，动态注册)
│           ├── chat/                # ChatClient 封装、上下文管理、Anthropic 消息构建、ReactLoop
│           └── stream/              # SSE Publisher (对话流式输出)
```

### 2.2 依赖关系

```
zwei-common ←── zwei-terra-core ←─── zwei-terra-agent
                    ↑                        ↑
                    │                        │ (运行时通过 Spring IoC 发现 tool 实现)
                    │                        │
            zwei-iot-device              (编译期无依赖)
            zwei-iot-alarm               (业务模块实现 TerraTool 接口)
            其他业务模块...
```

### 2.3 隔离保证

- `zwei-terra-agent` 的 pom.xml **不允许**出现任何 `zwei-iot-*` 依赖
- 业务模块（如 `zwei-iot-device`）依赖 `zwei-terra-core` 实现 `TerraBackendTool` 接口
- 运行时由 `zwei-admin`（Spring Boot 启动模块）统一聚合，Spring IoC 容器完成依赖注入
- 后续拆为独立服务时，只需把 tool 调用改为 HTTP/RPC

### 2.4 父 POM 注册

```xml
<!-- server/pom.xml <modules> 新增 -->
<module>zwei-terra</module>

<!-- zwei-terra/pom.xml -->
<parent>
    <artifactId>zwei</artifactId>
    <groupId>com.zwei</groupId>
    <version>3.9.2</version>
</parent>
<artifactId>zwei-terra</artifactId>
<packaging>pom</packaging>
<modules>
    <module>zwei-terra-core</module>
    <module>zwei-terra-agent</module>
</modules>
```

### 2.5 Controller 扫描

terra 的 Controller 放在 `zwei-terra-agent` 模块内部。在 `zwei-admin` 的 Spring Boot 启动类上扩展 `@ComponentScan` 包含 `com.zwei.terra`，或通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动装配。

## 3. 数据模型

### 3.1 表清单

全部使用 `terra_` 前缀，遵循项目现有数据库约定（InnoDB、utf8mb4_0900_ai_ci、del_flag 逻辑删除）。

| 表名 | 用途 |
|------|------|
| `terra_personality` | 人格配置：核心灵魂 + 角色外衣 |
| `terra_model_config` | 模型服务商配置 |
| `terra_skill` | 技能元数据（指向文件系统目录） |
| `terra_tool` | 工具注册表（code 注册 + config 创建） |
| `terra_skill_tool` | 技能-工具关联表 |
| `terra_conversation` | 对话会话 |
| `terra_message` | 对话消息 |

### 3.2 terra_personality

```sql
CREATE TABLE terra_personality (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    layer_type      VARCHAR(20)  NOT NULL COMMENT 'core=核心灵魂(不可删), role=角色外衣(可编辑)',
    name            VARCHAR(100) NOT NULL COMMENT '层级名称',
    content         TEXT         NOT NULL COMMENT '人格内容（system prompt 文本）',
    is_active       TINYINT(1)   DEFAULT 1 COMMENT '是否激活',
    is_preset       TINYINT(1)   DEFAULT 0 COMMENT '是否系统预置',
    sort_order      INT          DEFAULT 0,
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag        CHAR(1)      DEFAULT '0' COMMENT '0=正常 1=删除',
    COMMENT 'terra 人格配置表'
);
```

- `layer_type='core'` 的记录由系统初始化插入，`is_preset=1`，不可删除、不可新增其他 core 记录
- `layer_type='role'` 的记录由管理员增删改，可有多条，运行时将所有 active 的 role 拼接到 core 之后

### 3.3 terra_model_config

```sql
CREATE TABLE terra_model_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT '配置名称',
    base_url        VARCHAR(500) NOT NULL COMMENT 'API 基础地址',
    api_key         VARCHAR(500) NOT NULL COMMENT 'API Key（加密存储）',
    model_name      VARCHAR(200) NOT NULL COMMENT '模型名称（如 claude-sonnet-4-20250514）',
    max_tokens      INT          DEFAULT 4096 COMMENT '最大输出 token 数',
    temperature     DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数',
    is_active       TINYINT(1)   DEFAULT 0 COMMENT '是否激活（同时只有一个激活）',
    sort_order      INT          DEFAULT 0,
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag        CHAR(1)      DEFAULT '0',
    COMMENT 'terra 模型服务商配置表'
);
```

- `api_key` 使用项目现有的加密工具（如 AES）加密后存储
- 激活操作（`PUT /{id}/activate`）会将其他记录的 `is_active` 置为 0

### 3.4 terra_skill

```sql
CREATE TABLE terra_skill (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_key         VARCHAR(100)  NOT NULL UNIQUE COMMENT '技能唯一标识',
    name              VARCHAR(200)  NOT NULL COMMENT '技能名称',
    description       VARCHAR(500)  DEFAULT '' COMMENT '技能描述',
    directory_path    VARCHAR(500)  NOT NULL COMMENT '文件系统路径（如 preset/device-query）',
    triggers_summary  VARCHAR(500)  DEFAULT '' COMMENT '触发条件摘要（从 SKILL.md 提取）',
    tools_summary     VARCHAR(500)  DEFAULT '' COMMENT '关联 tool 列表摘要',
    is_preset         TINYINT(1)    DEFAULT 0 COMMENT '预置技能（不可卸载、不可禁用）',
    is_enabled        TINYINT(1)    DEFAULT 1 COMMENT '启用状态',
    installed_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    installed_by      VARCHAR(64)   DEFAULT '',
    sort_order        INT           DEFAULT 0,
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag          CHAR(1)       DEFAULT '0',
    COMMENT 'terra 技能元数据表'
);
```

### 3.5 terra_tool

```sql
CREATE TABLE terra_tool (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_key            VARCHAR(100)  NOT NULL UNIQUE COMMENT '工具唯一标识',
    name                VARCHAR(200)  NOT NULL COMMENT '显示名称',
    description         VARCHAR(500)  DEFAULT '' COMMENT '工具描述',
    source              VARCHAR(20)   NOT NULL COMMENT 'code=代码注册, config=配置化创建',
    exec_side           VARCHAR(20)   NOT NULL DEFAULT 'backend' COMMENT 'backend=服务端执行, frontend=前端执行',
    tool_type           VARCHAR(50)   DEFAULT 'query' COMMENT '工具类型: query/action/navigate/report',
    parameters_schema   JSON          COMMENT '参数 JSON Schema（Anthropic tool input_schema 格式）',
    endpoint            VARCHAR(500)  COMMENT '配置化工具的调用地址（HTTP endpoint）',
    timeout_seconds     INT           DEFAULT 30 COMMENT '执行超时（秒）',
    is_preset           TINYINT(1)    DEFAULT 0 COMMENT '代码注册的不可删除',
    is_enabled          TINYINT(1)    DEFAULT 1 COMMENT '启用状态',
    sort_order          INT           DEFAULT 0,
    create_by           VARCHAR(64)   DEFAULT '',
    create_time         DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64)   DEFAULT '',
    update_time         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag            CHAR(1)       DEFAULT '0',
    COMMENT 'terra 工具注册表'
);
```

### 3.6 terra_skill_tool

```sql
CREATE TABLE terra_skill_tool (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id    BIGINT NOT NULL COMMENT '技能 ID',
    tool_id     BIGINT NOT NULL COMMENT '工具 ID',
    UNIQUE KEY uk_skill_tool (skill_id, tool_id),
    COMMENT 'terra 技能-工具关联表'
);
```

### 3.7 terra_conversation

```sql
CREATE TABLE terra_conversation (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL COMMENT '用户 ID',
    title             VARCHAR(200) DEFAULT '新对话' COMMENT '会话标题',
    status            VARCHAR(20)  DEFAULT 'active' COMMENT 'active=活跃, archived=归档',
    last_message_time DATETIME     COMMENT '最后消息时间',
    message_count     INT          DEFAULT 0,
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag          CHAR(1)      DEFAULT '0',
    COMMENT 'terra 对话会话表'
);
```

### 3.8 terra_message

```sql
CREATE TABLE terra_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL COMMENT '会话 ID',
    role            VARCHAR(20) NOT NULL COMMENT 'user/assistant/tool',
    content         TEXT        COMMENT '消息文本内容',
    tool_calls      JSON        COMMENT 'assistant 发起的工具调用',
    tool_call_id    VARCHAR(100) COMMENT 'tool 消息对应的 call_id',
    tokens_used     INT         DEFAULT 0 COMMENT '本条消息消耗 token',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    COMMENT 'terra 对话消息表'
);
```

## 4. 技能系统

### 4.1 目录结构

```
{terra.skills.base-path}/            # application.yml 可配置路径
├── preset/                          # 预置技能（不可卸载）
│   ├── device-query/
│   │   ├── SKILL.md                 # 技能清单
│   │   ├── scripts/                 # 可执行脚本（Groovy/JS）
│   │   └── resources/              # 资源文件
│   └── alarm-query/
│       ├── SKILL.md
│       └── scripts/
└── custom/                          # 用户安装的自定义技能
    └── report-generate/
        ├── SKILL.md
        └── scripts/
```

### 4.2 SKILL.md 格式（Anthropic skill spec）

```yaml
---
name: device-query
description: 查询系统中的设备信息和传感器数据
triggers:
  - keyword: ["设备", "传感器", "在线状态"]
  - intent: "query_device_info"
tools:
  - query_device
  - query_sensor
---

# 行为指令

当用户询问设备信息时，优先使用 query_device 工具查询。
对传感器相关问题，使用 query_sensor 工具。

# 约束

- 不要泄露设备的 MQTT 认证信息
- 设备状态回答使用中文
```

### 4.3 技能生命周期

- **安装**：上传/创建技能目录 → 解析 SKILL.md → 提取 metadata 入库 → 关联 tools
- **卸载**：检查 `is_preset`（预置不可卸）→ 删除目录 → 删除数据库记录
- **启用/停用**：只改 `is_enabled` 字段，不碰文件（预置技能不可停用）
- **预置同步**：系统启动时扫描 `preset/` 目录，自动同步到数据库（不存在则创建，`is_preset=1`）

## 5. 后端架构

### 5.1 Spring AI 集成

使用 Spring AI Alibaba 的 `ChatClient` 封装 Anthropic Messages API 调用。由于只需要 Anthropic 协议，将自定义实现 `AnthropicChatModel`：

```java
// terra-agent: AnthropicChatService — 核心对话服务
public class AnthropicChatService {
    /**
     * 1. 从 DB 读取激活的 model_config，构建 AnthropicApi (base-url + api-key)
     * 2. 拼装 system prompt: 核心灵魂 + 激活的角色外衣 + 启用技能的 instructions
     * 3. 收集启用的 tools（code 注册 + config 注册），转为 Anthropic tool schema
     * 4. 调用 Anthropic Messages API，SSE 流式返回
     * 5. React Loop: 如果 LLM 返回 tool_call → 执行 tool → 将结果喂回 → 继续生成
     */
}
```

### 5.2 System Prompt 拼装逻辑

```
最终 system prompt =
    [核心灵魂] terra_personality WHERE layer_type='core' AND is_active=1
  + [角色外衣] terra_personality WHERE layer_type='role' AND is_active=1 (按 sort_order)
  + [技能指令] 所有启用技能 SKILL.md 中的 instructions 部分
```

### 5.3 React Loop 完整流程

```
用户发消息 (POST /api/v1/terra/chat)
  │
  ▼
┌─────────────────────────────────────────────────┐
│  React Loop (最多 max_react_rounds 轮，默认 10)   │
│                                                   │
│  1. 构建 messages（历史 + 本轮 tool results）       │
│  2. 构建 system prompt（灵魂 + 角色 + 技能）        │
│  3. 附加 enabled tools schema                     │
│  4. 调用 Anthropic Messages API                   │
│                                                   │
│  5. 解析响应：                                      │
│     ├─ stop_reason = "end_turn"                   │
│     │   → SSE 流式推送 token → 保存 message        │
│     │   → 跳出循环                                 │
│     │                                             │
│     └─ stop_reason = "tool_use"                   │
│         遍历每个 tool_call:                         │
│         ├─ exec_side = backend:                   │
│         │   ToolManager.execute() 同步执行          │
│         │   → 收集 result                          │
│         │                                         │
│         └─ exec_side = frontend:                  │
│             → SSE 推送 tool_call 事件 (含 call_id)  │
│             → 暂停，等待前端回调                     │
│             → 前端 POST 回调返回 result             │
│             → 收集 result                          │
│                                                   │
│         将所有 tool results 加入 messages           │
│         → 继续下一轮循环                            │
└─────────────────────────────────────────────────┘
```

### 5.4 前端工具回调机制

```
SSE 推送:
  event: tool_call
  data: {"callId":"tc_001", "tool":"navigate", "execSide":"frontend", "params":{"route":"/basic/device"}}

前端执行后回调:
  POST /api/v1/terra/chat/{conversationId}/tool-result
  Body: {"callId":"tc_001", "success":true, "result":{"navigated":true}}
```

**后端等待机制：**
- 后端在推送 frontend tool_call 后，使用 `CompletableFuture` + 超时等待（默认按 `terra_tool.timeout_seconds`，默认 30s）
- 前端回调到达时 complete 该 future，react loop 继续
- 超时则返回错误结果给 LLM：`{"error":"frontend tool timeout"}`

### 5.5 SSE 事件格式

```
event: token         // 文本片段（流式输出）
data: {"content":"你好"}

event: tool_call     // 工具调用通知
data: {"callId":"tc_001","tool":"query_device","execSide":"backend","params":{...}}

event: tool_result   // 工具结果
data: {"callId":"tc_001","success":true,"result":{...}}

event: action        // 前端操作指令（frontend tool 专用）
data: {"callId":"tc_002","tool":"navigate","execSide":"frontend","params":{"route":"/basic/device"}}

event: done          // 对话结束
data: {"messageId":123,"tokensUsed":456}

event: error         // 错误
data: {"message":"模型调用失败","code":"MODEL_ERROR"}
```

### 5.6 Tool Registry 机制

#### terra-core 接口定义

```java
// 后端工具注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TerraTool {
    String name();
    String description();
    String category() default "general";
    String execSide() default "backend";  // backend or frontend
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolMethod {
    String description();
}

// 后端工具接口（业务模块实现）
public interface TerraBackendTool {
    // 标记接口，实现类加 @TerraTool 注解
}
```

#### 业务模块实现示例

```java
// 在 zwei-iot-device 中实现 tool
@TerraTool(name = "query_device", description = "查询设备信息")
public class DeviceQueryTerraTool implements TerraBackendTool {

    @Autowired
    private IDeviceQueryService deviceQueryService;

    @ToolMethod(description = "根据设备名称模糊查询设备列表")
    public List<DeviceBriefDTO> searchDevices(String keyword) {
        return deviceQueryService.searchByKeyword(keyword);
    }

    @ToolMethod(description = "根据设备 ID 查询设备详情")
    public DeviceDetail getDeviceDetail(Long deviceId) {
        return deviceQueryService.getDetailById(deviceId);
    }
}
```

#### terra-agent ToolManager

```java
@Component
public class ToolManager {
    private Map<String, ToolRegistration> codeTools;    // Spring 扫描
    private Map<String, ToolRegistration> configTools;  // DB 加载

    @PostConstruct
    void scanCodeTools() {
        // applicationContext.getBeansWithAnnotation(TerraTool.class)
        // → 解析 @ToolMethod → 构建 ToolSchema
        // → 注册到 codeTools map
    }

    /** 供 AnthropicChatService 调用 */
    List<ToolDefinition> getEnabledToolDefinitions() {
        // 合并 codeTools + configTools，过滤 is_enabled
    }

    /** 执行后端工具 */
    ToolResult execute(String toolKey, Map<String, Object> params) {
        ToolRegistration reg = codeTools.get(toolKey);
        if (reg != null) {
            // 反射调用 Spring Bean 方法
        }
        reg = configTools.get(toolKey);
        if (reg != null) {
            // HTTP 调用配置的 endpoint
        }
    }
}
```

### 5.7 预置技能启动同步

```java
@PostConstruct
void syncPresetSkills() {
    // 扫描 {terra.skills.base-path}/preset/ 目录
    // 逐个解析 SKILL.md → upsert terra_skill (is_preset=1)
    // 确保预置技能在 DB 中存在且 is_enabled=1
}
```

## 6. API 设计

所有接口遵循项目现有约定：`/api/v1/` 前缀，`{ code, msg, data }` 响应封装。

### 6.1 对话 API

| 方法 | 路径 | 用途 | 权限 |
|------|------|------|------|
| `POST` | `/api/v1/terra/chat` | 发送消息，返回 SSE 流 | `terra:chat` |
| `POST` | `/api/v1/terra/chat/{conversationId}/tool-result` | 前端工具执行结果回调 | `terra:chat` |
| `GET` | `/api/v1/terra/conversations` | 会话列表（分页，当前用户） | `terra:chat` |
| `GET` | `/api/v1/terra/conversations/{id}/messages` | 某会话的消息历史 | `terra:chat` |
| `POST` | `/api/v1/terra/conversations` | 新建会话 | `terra:chat` |
| `DELETE` | `/api/v1/terra/conversations/{id}` | 删除会话 | `terra:chat` |

**POST /api/v1/terra/chat 请求体：**
```json
{
    "conversationId": 123,
    "message": "帮我查询雨量计设备的在线状态"
}
```

**响应：** `Content-Type: text/event-stream`（SSE 流式）

### 6.2 设置 API

| 方法 | 路径 | 用途 | 权限 |
|------|------|------|------|
| `GET` | `/api/v1/terra/personality` | 获取人格配置 | `terra:settings` |
| `PUT` | `/api/v1/terra/personality` | 编辑人格配置 | `terra:settings` |
| `GET` | `/api/v1/terra/model-configs` | 模型配置列表 | `terra:settings` |
| `POST` | `/api/v1/terra/model-configs` | 新增模型配置 | `terra:settings` |
| `PUT` | `/api/v1/terra/model-configs/{id}` | 编辑模型配置 | `terra:settings` |
| `DELETE` | `/api/v1/terra/model-configs/{id}` | 删除模型配置 | `terra:settings` |
| `PUT` | `/api/v1/terra/model-configs/{id}/activate` | 激活模型配置 | `terra:settings` |
| `GET` | `/api/v1/terra/skills` | 技能列表 | `terra:settings` |
| `POST` | `/api/v1/terra/skills/install` | 安装技能（上传目录 zip） | `terra:settings` |
| `DELETE` | `/api/v1/terra/skills/{id}` | 卸载技能（预置不可卸） | `terra:settings` |
| `PUT` | `/api/v1/terra/skills/{id}/toggle` | 启用/停用技能 | `terra:settings` |
| `GET` | `/api/v1/terra/tools` | 工具列表 | `terra:settings` |
| `POST` | `/api/v1/terra/tools` | 新增配置化工具 | `terra:settings` |
| `PUT` | `/api/v1/terra/tools/{id}` | 编辑配置化工具 | `terra:settings` |
| `DELETE` | `/api/v1/terra/tools/{id}` | 删除配置化工具（code 工具不可删） | `terra:settings` |
| `PUT` | `/api/v1/terra/tools/{id}/toggle` | 启用/停用工具 | `terra:settings` |

## 7. 前端架构

### 7.1 组件结构

```
web/src/
├── components/
│   └── terra/
│       ├── TerraWidget.vue         # 悬浮球入口（蓝色圆圈，可拖动）
│       ├── TerraChatPanel.vue      # 对话面板（右侧悬浮，透明背景）
│       ├── TerraMessage.vue        # 单条消息渲染（支持 markdown、tool 结果卡片）
│       ├── TerraToolExecutor.ts    # 前端工具执行器（注册 + 派发）
│       └── terra-sse.ts            # SSE 连接管理（对话流）
├── api/
│   └── terra.ts                    # terra API 封装
└── views/
    └── terra/                       # 设置页面（管理员）
        ├── SettingsLayout.vue       # 设置页布局
        ├── PersonalitySettings.vue  # 人格编辑
        ├── ModelConfigList.vue      # 模型配置管理
        ├── SkillManager.vue         # 技能管理
        └── ToolManager.vue          # 工具管理
```

### 7.2 悬浮球交互

- **TerraWidget**：`position: fixed; bottom: 24px; right: 24px; z-index: 9999`
  - 48x48 蓝色圆圈（`border-radius: 50%; background: #409EFF`）
  - 拖动：`mousedown → mousemove → mouseup`，位置存入 `localStorage`
  - 点击（非拖动结束）：展开/收起 `TerraChatPanel`

### 7.3 对话面板

- **TerraChatPanel**：`position: fixed; right: 24px; z-index: 9998`
  - `background: rgba(255, 255, 255, 0.75); backdrop-filter: blur(12px)`
  - 消息列表自下向上堆叠
  - 输入框在底部
  - 支持会话切换（左侧或顶部会话列表）

### 7.4 前端工具执行器（FrontendToolExecutor）

```typescript
// 注册前端工具处理器
const toolHandlers = new Map<string, (params: any) => Promise<ToolResult>>()

toolHandlers.set('navigate', async (params) => {
    await router.push(params.route)
    return { success: true, result: { navigated: true } }
})

toolHandlers.set('query_page_data', async (params) => {
    const data = await getCurrentPageData(params)
    return { success: true, result: data }
})

// SSE 收到 tool_call(execSide=frontend) 时自动派发
async function executeTool(callId: string, tool: string, params: any) {
    const handler = toolHandlers.get(tool)
    const result = handler ? await handler(params) : { success: false, error: 'unknown tool' }
    await postToolResult(conversationId, callId, result)
}
```

### 7.5 Layout 挂载

在 `web/src/layout/index.vue` 中新增 `<TerraWidget />` 组件。

### 7.6 设置页面路由

```typescript
{
    path: '/terra/settings',
    component: () => import('@/views/terra/SettingsLayout.vue'),
    children: [
        { path: 'personality', component: () => import('@/views/terra/PersonalitySettings.vue') },
        { path: 'models', component: () => import('@/views/terra/ModelConfigList.vue') },
        { path: 'skills', component: () => import('@/views/terra/SkillManager.vue') },
        { path: 'tools', component: () => import('@/views/terra/ToolManager.vue') },
    ]
}
```

## 8. 错误处理

| 场景 | 处理方式 |
|------|---------|
| 模型配置未激活 | 返回错误 SSE event，提示用户在设置中配置模型 |
| 模型 API 调用失败（网络/认证） | SSE error 事件，附带错误信息 |
| React Loop 超过最大轮次 | 停止循环，返回当前已生成的内容 + 警告 |
| 后端工具执行异常 | 捕获异常，将错误信息作为 tool_result 喂给 LLM |
| 前端工具超时 | 返回 `{"error":"frontend tool timeout"}` 给 LLM |
| SSE 连接断开 | 前端自动重连（复用项目现有 SSE 重连机制） |

## 9. 安全考虑

- **API Key 加密**：`terra_model_config.api_key` 使用 AES 加密存储，不明文返回给前端
- **权限控制**：对话接口需 `terra:chat` 权限，设置接口需 `terra:settings` 权限
- **数据隔离**：对话按 `user_id` 隔离，用户只能查看自己的会话
- **工具执行沙箱**：技能脚本（Groovy/JS）在受限沙箱中执行，限制可用类和资源
- **React Loop 防护**：最大轮次限制 + 单次工具执行超时

## 10. 配置项

```yaml
# application.yml / application-local.yml
terra:
  skills:
    base-path: ${user.home}/terra/skills    # 技能目录根路径
  chat:
    max-react-rounds: 10                     # React Loop 最大轮次
    default-timeout-seconds: 30              # 前端工具默认超时
    max-history-messages: 20                 # 上下文窗口最大历史消息数
```

## 11. MVP 交付边界

### 第一批交付（本规格覆盖）

1. **zwei-terra-core**：工具接口、技能接口、共享 DTO
2. **zwei-terra-agent**：完整的 Agent 实现
   - Spring AI Alibaba 集成 + 自定义 Anthropic ChatModel
   - 对话管理（SSE 流式 + React Loop）
   - 设置管理（人格/模型/技能/工具 CRUD）
   - Tool Registry（代码扫描 + 配置加载 + 双向执行）
   - Skill 系统（目录管理 + SKILL.md 解析 + 预置同步）
3. **前端**：悬浮球 + 对话面板 + 设置页面 + 前端工具执行器框架

### 后续批次

1. 预置技能实现（device-query、alarm-query 等）
2. 具体的后端工具实现（在各业务模块中创建 TerraBackendTool 实现类）
3. 具体的前端工具实现（navigate、query_page_data、print_report 等）
4. 技能安装/上传功能完善
5. 对话上下文窗口优化（token 计数 + 截断策略）

## 12. 版本兼容性风险

- 项目当前 Spring Boot 4.0.3，需要确认 Spring AI Alibaba 的兼容版本
- 如果 Spring AI Alibaba 不兼容 Spring Boot 4.x，需评估降级方案或使用 Spring AI 原生（spring-ai-anthropic）
- 在实现计划的第一步中进行兼容性验证
