# TerraMens Dashboard UI

TerraMens 地质智能态势平台 - Dashboard 前端界面

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

或使用启动脚本：

```bash
./dev.sh
```

访问地址: http://localhost:5173

### 生产构建

```bash
npm run build
```

### 预览构建结果

```bash
npm run preview
```

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - 类型安全
- **Vite** - 快速的构建工具
- **Pinia** - Vue 3 官方状态管理
- **Three.js** - 3D 图形库（TerraMens 虚拟形象）
- **Leaflet** - 开源地图库
- **SCSS** - CSS 预处理器

## 项目结构

```
dashboard-ui/
├── src/
│   ├── api/               # API 通信层
│   │   ├── websocket.ts   # WebSocket 客户端
│   │   └── http.ts        # HTTP 客户端
│   ├── components/        # 组件库
│   │   ├── terra-avatar/  # TerraMens 3D 虚拟形象
│   │   ├── timeline/       # 时间线组件
│   │   ├── panels/         # 面板组件
│   │   └── status-bar/     # 状态栏组件
│   ├── composables/       # 组合式函数
│   │   ├── useWebSocket.ts
│   │   └── usePanelCommand.ts
│   ├── stores/            # Pinia 状态管理
│   │   ├── websocket.ts
│   │   ├── terra.ts
│   │   ├── panel.ts
│   │   └── alert.ts
│   ├── styles/            # 全局样式
│   │   ├── variables.scss  # SCSS 变量
│   │   ├── reset.scss      # 样式重置
│   │   └── global.scss     # 通用样式
│   └── types/             # TypeScript 类型
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## WebSocket 通信协议

### 前端 → 后端

```typescript
// 用户操作
{
  version: "1.0",
  type: "command",
  namespace: "core",
  payload: {
    action: "user_action",
    target: "...",
    params: {}
  }
}

// 指令执行结果
{
  version: "1.0",
  type: "response",
  namespace: "core",
  payload: {
    commandId: "...",
    success: true,
    result: {}
  }
}
```

### 后端 → 前端

```typescript
// 面板指令
{
  version: "1.0",
  type: "command",
  namespace: "panel",
  payload: {
    panelId: "map-panel-1",
    action: "map:drawCircle",
    params: {
      center: [28.08, 103.42],
      radius: 500
    }
  }
}

// 状态推送
{
  version: "1.0",
  type: "event",
  namespace: "terra",
  payload: {
    terraState: "guarding",
    watching: ["七号点", "台风路径"],
    message: "..."
  }
}
```

## 开发指南

### 添加新的面板组件

1. 在 `src/components/panels/` 创建面板组件
2. 在 `src/composables/` 创建对应的指令处理器
3. 在面板组件中注册指令处理器

示例：

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import L from 'leaflet'
import { usePanelCommand } from '@/composables/usePanelCommand'
import { useMapPanelCommands } from '@/composables/useMapPanelCommands'

const mapContainer = ref<HTMLElement>()
const mapInstance = ref<L.Map | null>(null)
const { registerHandler } = usePanelCommand()

onMounted(() => {
  // 初始化地图
  mapInstance.value = L.map(mapContainer.value!)
  
  // 注册指令处理器
  const handlers = useMapPanelCommands(mapInstance)
  registerHandler('map', 'drawCircle', async (panelId, params) => {
    return await handlers.map.drawCircle(params)
  })
})
</script>
```

### 添加新的指令

在指令处理器中添加新方法：

```typescript
async function newCommand(params: any) {
  // 执行操作
  return { success: true, result: {} }
}
```

## 设计规范

基于 `STYLE.md`：

- **主背景色**: `#0f1720`
- **面板背景**: `#131d29`
- **主色**: `#4a7fb8`（科技青蓝）
- **TerraMens 状态色**:
  - 守护绿: `#34D399`
  - 思考琥珀: `#FBBF24`
  - 预警橙: `#F97316`
  - 紧急红: `#EF4444`

## 去机器化设计

所有界面语言使用第一人称"我"，有温度的表达：

- ✅ "我正在守护"
- ✅ "我在思考"
- ✅ "我有点担心"
- ❌ "系统正常"
- ❌ "检测到异常"

## License

MIT
