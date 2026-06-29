# Terra 值守模式 (Duty Mode) 设计规格

> 日期: 2026-06-29
> 状态: 实施中

## 1. 概述

将 terramens 项目的 dashboard-ui（实时监控仪表盘）移植到 zwei，作为 Terra 智能助手的"值守模式"。前端独立部署在 `/terra` basePath 下，后端在 Java Spring Boot 中实现 WebSocket 端点，复用现有 Terra 基础设施。

### 核心交互流程

```
用户访问 /terra → dashboard-ui 加载 → WebSocket 连接 /ws/terra/duty
    → Terra 值守助手自动启动巡检
    → AI 通过面板展示设备状态、告警、图表
    → 用户可对话指挥 AI 操作面板
```

## 2. 架构设计

### 2.1 整体架构

```
┌──────────────────────────────────────────────────────────┐
│  Browser                                                  │
│  ┌─────────────────┐     ┌───────────────────────────┐   │
│  │  /terra (独立)   │     │  /web (zwei 主前端)        │   │
│  │  dashboard-ui    │     │  Vue 3 + Element Plus     │   │
│  │  Vue 3 + WS     │     │  TerraWidget 悬浮球        │   │
│  └────────┬────────┘     └───────────────────────────┘   │
│           │ WebSocket                                      │
└───────────┼──────────────────────────────────────────────┘
            │ ws://host/ws/terra/duty
┌───────────┼──────────────────────────────────────────────┐
│           │          Java Spring Boot Backend             │
│  ┌────────▼──────────────────────────────────────────┐   │
│  │  TerraDutyWebSocketHandler                         │   │
│  │  (Spring WebSocket, JWT 鉴权)                       │   │
│  └────────┬──────────────────────────────────────────┘   │
│           │                                               │
│  ┌────────▼──────────────────────────────────────────┐   │
│  │  TerraDutyService                                  │   │
│  │  ├── 值守模式 ReAct Loop (复用 AnthropicChatModel)  │   │
│  │  ├── WebSocket 事件推送 (terramens 协议)             │   │
│  │  ├── 面板控制指令                                    │   │
│  │  └── Terra 状态管理 (normal/warning/critical)       │   │
│  └────────┬──────────────────────────────────────────┘   │
│           │                                               │
│  ┌────────▼──────────────────────────────────────────┐   │
│  │  复用基础设施                                       │   │
│  │  ├── ToolManager (后端工具执行)                     │   │
│  │  ├── ITerraPersonalityService (系统提示词)          │   │
│  │  ├── TerraModelConfigMapper (模型配置)              │   │
│  │  └── SystemQueryTool + DashboardControlTool         │   │
│  └───────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

### 2.2 前端部署方式

- dashboard-ui 作为**独立 Vue 3 项目**放在 `terra/` 目录
- Vite `base: '/terra/'`，构建产物输出到 `web/public/terra/`
- Nginx 配置 `/terra` SPA fallback
- WebSocket 路径: `/ws/terra/duty`
- HTTP API 路径: `/api/v1/terra/duty/*`

### 2.3 通信协议 (WebSocket)

完整复用 terramens dashboard WebSocket 协议:

```json
{
  "version": "1.0",
  "id": "msg-{timestamp}-{random}",
  "timestamp": 1690000000000,
  "type": "command|event|query|response|error",
  "namespace": "core|panel|terra|data",
  "payload": { ... }
}
```

#### 关键消息类型

| 方向 | type | namespace | 用途 |
|------|------|-----------|------|
| 后端→前端 | event | terra | timelineItem (流式消息/巡检报告) |
| 后端→前端 | event | terra | terraState (头像状态更新) |
| 后端→前端 | event | terra | heartbeatTrigger (心跳动画) |
| 后端→前端 | event | terra | alert (告警弹窗) |
| 后端→前端 | command | panel | lifecycle:create/destroy (面板生命周期) |
| 后端→前端 | command | panel | setData/update/show/hide |
| 前端→后端 | command | core | chat_message (用户发送消息) |
| 后端→前端 | response | core | handshake (连接握手+能力声明) |
| 后端→前端 | query | core | get_state (查询前端面板状态) |

## 3. 后端实现

### 3.1 新增组件 (zwei-terra-agent 模块)

包路径: `com.zwei.terra.agent.duty`

#### TerraDutyWebSocketHandler
- Spring `TextWebSocketHandler` 实现
- 路径: `/ws/terra/duty`
- JWT 鉴权 (通过握手拦截器从 query param 提取 token)
- 维护 `TerraDutySession` (每连接一个)
- 消息路由: chat_message → TerraDutyService

#### TerraDutySessionManager
- 管理所有活跃 WebSocket 连接
- 广播方法: broadcastToAll(message), sendToSession(sessionId, message)
- 连接/断开事件处理

#### TerraDutyService
- 值守模式核心聊天编排
- 复用 AnthropicChatModel 做流式 LLM 调用
- 复用 ToolManager 执行后端工具
- 复用 ITerraPersonalityService 获取值守模式系统提示词
- 将 LLM 输出翻译为 terramens WebSocket 协议事件:
  - token → streaming timelineItem (isStreaming: true/false)
  - tool_call/tool_result → timelineItem (type: action/thinking)
  - done → finalize timeline
- 内置 ReAct Loop (与 ChatService 类似但输出到 WebSocket)

#### DashboardControlTool
- 实现 `TerraBackendTool` 接口
- 工具方法:
  - `createPanel(type, title, data, position)` — 创建面板
  - `destroyPanel(panelId)` — 销毁面板
  - `updateTerraState(state, message)` — 更新头像状态
  - `showAlert(level, title, description)` — 弹出告警
- 通过 TerraDutySessionManager 向前端发送 WebSocket 命令

#### TerraDutyHandshakeInterceptor
- 从 WebSocket 握手 URL query 参数提取 JWT token
- 验证 token 并设置 userId 到 attributes
- 失败则拒绝连接

### 3.2 WebSocket 配置

```java
@Configuration
@EnableWebSocket
public class TerraDutyWebSocketConfig implements WebSocketConfigurer {
    // 注册 /ws/terra/duty 端点
    // 添加 TerraDutyHandshakeInterceptor
    // 允许全部 origins (生产环境需配置)
}
```

### 3.3 REST 端点

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/v1/terra/duty/state` | 获取值守模式状态 |
| POST | `/api/v1/terra/duty/message` | 发送消息(HTTP备用) |

### 3.4 值守模式系统提示词

新增值守模式人格记录 (terra_personality 表):
- 使用"值守模式"角色外衣
- 包含面板控制工具的使用说明
- 指导 AI 主动巡检、展示数据、发出预警

## 4. 前端实现

### 4.1 项目结构

```
terra/
├── package.json
├── vite.config.ts        (base: '/terra/', proxy → :8080)
├── tsconfig.json
├── index.html
├── src/
│   ├── (完整复用 terramens dashboard-ui 源码)
│   ├── api/
│   │   ├── websocket.ts  (连接路径改为 /ws/terra/duty)
│   │   └── http.ts       (baseURL: /api/v1/terra/duty)
│   ├── components/
│   │   ├── panels/       (7种面板组件)
│   │   ├── terra-avatar/ (3D头像)
│   │   ├── status-bar/
│   │   └── timeline/
│   ├── stores/
│   ├── types/
│   ├── App.vue
│   └── main.ts
└── build.sh              (构建脚本 → web/public/terra/)
```

### 4.2 适配改动

1. **WebSocket 连接路径**: `/ws/dashboard` → `/ws/terra/duty?token=xxx`
2. **HTTP API baseURL**: `/api` → `/api/v1/terra/duty`
3. **JWT 鉴权**: WebSocket 握手时通过 query param 传递 JWT token
4. **品牌标识**: TERRAMENS → Terra 值守
5. **Vite base**: `/terra/`

### 4.3 构建与部署

```bash
cd terra
npm run build
# 输出到 dist/
# 复制到 web/public/terra/
```

Nginx 配置:
```nginx
location /terra {
    alias /usr/share/nginx/html/terra;
    try_files $uri $uri/ /terra/index.html;
}
```

## 5. 与现有 Terra 悬浮球的集成

- 当用户通过悬浮球说"进入值守模式"时，AI 使用 `frontend.navigate` 工具跳转到 `/terra`
- 值守模式页面独立加载，不依赖主前端的 Vue Router
- Token 通过 URL hash 或 localStorage 共享

## 6. 安全

- WebSocket 连接需 JWT 鉴权 (query param `?token=xxx`)
- 权限要求: `terra:chat`
- 与现有 Terra 悬浮球使用相同的 RBAC 权限体系
