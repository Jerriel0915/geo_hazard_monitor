# 知微 - 交通边坡地质灾害监测预警系统

基于物联网的地质灾害监测预警平台，支持传感器数据采集、实时告警、规则引擎和可视化看板。

## 技术栈

### 后端

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 4.0.3 | 应用框架 |
| MyBatis | 4.0.1 | ORM 框架 |
| Druid | 1.2.28 | 数据库连接池 |
| PageHelper | 2.1.1 | 分页插件 |
| Mica MQTT | 2.6.3 | MQTT Broker（设备接入） |
| Apache IoTDB | 1.3.4 | 时序数据库（传感器数据） |
| SpringDoc | 3.0.2 | API 文档（Swagger） |
| JWT | 0.9.1 | 令牌认证 |
| FastJSON | 2.0.61 | JSON 处理 |
| Lombok | 1.18.44 | 代码简化 |

### 前端

| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.21 | UI 框架 |
| TypeScript | 5.3.3 | 类型系统 |
| Vite | 5.1.0 | 构建工具 |
| Element Plus | 2.6.1 | 组件库 |
| Vue Router | 4.3.0 | 路由管理 |
| Axios | 1.16.0 | HTTP 客户端 |
| ECharts | 6.0.0 | 数据可视化 |
| Leaflet | 1.9.4 | 地图引擎 |
| Sass | 1.70.0 | CSS 预处理器 |

### 基础设施

| 组件 | 版本 | 说明 |
|------|------|------|
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7 | 缓存 / Stream 消息队列 |
| IoTDB | 2.0.2 | 时序数据库 |
| Nginx | - | 前端静态资源 / 反向代理 |

## 项目结构

```
zwei/
├── server/                       # 后端（Spring Boot 多模块）
│   ├── zwei-admin/               # 启动模块（入口、Controller、配置）
│   ├── zwei-framework/           # 核心框架（安全、缓存、异常处理）
│   ├── zwei-common/              # 公共工具（注解、常量、工具类）
│   ├── zwei-system/              # 系统管理（用户、角色、菜单）
│   ├── zwei-iot/                 # 物联网（MQTT、设备、规则引擎）
│   ├── zwei-log/                 # 操作日志
│   ├── zwei-quartz/              # 定时任务
│   └── sql/                      # SQL 脚本
├── web/                          # 前端（Vue 3 + TypeScript）
│   ├── src/
│   │   ├── api/                  # API 请求封装
│   │   ├── views/                # 页面组件
│   │   ├── router/               # 路由配置
│   │   ├── utils/                # 工具函数
│   │   └── components/           # 公共组件
│   └── vite.config.ts
├── db/                           # 数据库初始化脚本
│   ├── geo_hazard_monitor_v1.9sql   # 完整建库脚本
│   └── upgrade/                  # 增量升级脚本
├── docs/                         # 项目文档
├── docker-compose.yml            # 生产环境编排
└── .env.example                  # 环境变量模板
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0
- Redis 7
- IoTDB 1.3+（可选，用于传感器时序数据）

### 本地开发

#### 1. 准备数据库

```bash
# 初始化 MySQL
mysql -u root -p < db/geo_hazard_monitor_v1.10.sql

# 如有增量升级，依次执行
mysql -u root -p < db/upgrade/*.sql
```

#### 2. 启动后端

```bash
cd server

# 修改配置（数据库、Redis、IoTDB 连接信息）
# 编辑 zwei-admin/src/main/resources/application.yml
# 编辑 zwei-admin/src/main/resources/application-druid.yml

# 编译运行
mvn clean package -DskipTests
java -jar zwei-admin/target/zwei-admin.jar
```

后端默认端口 `8080`，API 文档地址 `http://localhost:8080/swagger-ui.html`。

#### 3. 启动前端

```bash
cd web

npm install

# 开发模式（默认代理到后端 8080）
npm run dev
```

前端默认端口 `5173`，访问 `http://localhost:5173`。

如需修改后端代理地址，编辑 `web/vite.config.ts` 中的 `proxy` 配置。

### Docker 部署

```bash
# 1. 复制环境变量文件并填写真实值
cp .env.example .env

# 2. 启动所有服务
docker compose up -d --build
```

Docker Compose 包含以下服务：

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| nginx | geo_nginx | 80 | 前端入口 |
| spring-boot | geo_springboot | 8080 (内部) | 后端 API |
| mysql | geo_mysql | 3306 (内部) | 关系型数据库 |
| redis | geo_redis | 6379 (内部) | 缓存 |
| iotdb | geo_iotdb | 6667 (内部) | 时序数据库 |

外部暴露端口：
- `80` - Web 前端
- `1883` - MQTT TCP
- `8083` - MQTT WebSocket
- `18083` - MQTT HTTP 管理 API

## 核心功能模块

### 物联网设备管理

- **设备注册/注销**：统一管理监测设备生命周期
- **MQTT 接入**：基于 Mica MQTT 的内置 Broker，支持 TCP / WebSocket / HTTP 协议
- **设备认证**：JWT 令牌 + 设备白名单双重认证，防暴力重试
- **数据链路**：MQTT 报文 → Redis Stream 缓冲 → 去重 → 异步写入 IoTDB

### 规则引擎

- 可配置的数据处理规则（阈值告警、数据转换、联动触发）
- 支持多级告警策略

### 监测预警

- **隐患点管理**：地质隐患点信息维护与地图标注
- **监测类型**：自定义传感器类型和监测参数
- **实时告警**：多级阈值告警、通知推送、处置流程

### 可视化看板

- 综合概览（系统运行状态）
- 告警视图（实时告警监控）
- 运营视图（设备运维管理）
- 自定义视图（按需配置）

### 系统管理

- 用户 / 角色 / 菜单 / 权限管理
- 操作日志 / 登录日志
- 定时任务管理

## 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `MYSQL_HOST` | 是 | mysql | MySQL 地址 |
| `MYSQL_DATABASE` | 否 | geo_hazard_monitor | 数据库名 |
| `MYSQL_USER` | 否 | root | 数据库用户 |
| `MYSQL_PASSWORD` | 是 | - | 数据库密码 |
| `REDIS_HOST` | 是 | redis | Redis 地址 |
| `REDIS_PORT` | 否 | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | 是 | - | Redis 密码 |
| `JWT_SECRET` | 是 | - | JWT 密钥 |
| `MQTT_USERNAME` | 否 | mica | MQTT 用户名 |
| `MQTT_PASSWORD` | 否 | mica | MQTT 密码 |
| `IOTDB_ENABLED` | 否 | true | 是否启用 IoTDB |
| `IOTDB_HOST` | 否 | iotdb | IoTDB 地址 |
| `IOTDB_PORT` | 否 | 6667 | IoTDB 端口 |
| `IOTDB_DATABASE` | 否 | root.geo_hazard | IoTDB 数据库 |
| `NGINX_HTTP_PORT` | 否 | 80 | Nginx 端口 |

## API 约定

- 基础路径：`/api`
- 认证方式：`Authorization` Header 携带 JWT Token
- 分页参数：`pageNum` / `pageSize`
- 响应格式：`{ code, msg, data }`

## 文档目录

| 文档 | 说明 |
|------|------|
| `docs/需求文档.md` | 功能需求与特性清单 |
| `docs/接口设计规范.md` | API 设计规范 |
| `docs/数据库设计规范.md` | 数据库命名与设计标准 |
| `docs/物联网平台接入文档.md` | IoT 设备接入指南 |
| `docs/设备管理.md` | 设备管理功能说明 |
| `docs/隐患点管理.md` | 隐患点管理功能说明 |
| `docs/系统管理.md` | 系统管理功能说明 |

## License

See [LICENSE](LICENSE).
