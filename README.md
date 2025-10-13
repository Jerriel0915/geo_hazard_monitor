# 知微 - 监测预警平台

<p align="center">
  <img src="https://img.shields.io/badge/version-3.9.0-blue.svg">
  <img src="https://img.shields.io/badge/build-passing-brightgreen.svg">
  <img src="https://img.shields.io/badge/license-MIT-green.svg">
</p>

## 项目简介

知微是一个基于SpringBoot+Vue的现代化监测预警平台，专注于物联网设备数据采集、存储、分析和预警功能。该平台提供了完整的物联网设备管理、数据存储、通信协议支持等核心能力，可用于各类监测场景的数据管理与预警分析。

## 技术栈

### 后端
- **框架**: Spring Boot 2.5.15, Spring Security 5.7.12
- **数据库**: MySQL, Druid 1.2.23连接池
- **消息队列**: MQTT (基于Mica MQTT 2.5.3)
- **安全认证**: JWT 0.9.1
- **API文档**: Swagger 3.0.0
- **定时任务**: Spring Quartz
- **数据处理**: Fastjson2 2.0.57, POI 4.1.2

### 前端
- **框架**: Vue.js, Element UI
- **构建工具**: Vite
- **路由**: Vue Router
- **状态管理**: Vuex

## 项目结构

```
zwei/
├── framework/            # 基础框架
│   └── zwei-common/      # 通用工具类
├── module-infra/         # 基础设施模块
│   ├── zwei-generator/   # 代码生成器
│   └── zwei-quartz/      # 定时任务模块
├── module-system/        # 系统管理模块
│   └── zwei-system/      # 系统核心功能
├── module-iot/           # 物联网核心模块
│   ├── module-iot-core/  # IoT核心服务
│   ├── module-iot-device/# 设备管理模块
│   ├── module-iot-endpoint/ # 通信端点
│   │   └── iot-endpoint-mqtt/ # MQTT端点实现
│   └── module-iot-storage/ # 数据存储模块
│       ├── iot-storage-common/ # 存储公共接口
│       └── iot-storage-mysql/  # MySQL存储实现
├── server/               # 应用服务端
├── web/                  # 前端代码
├── doc/                  # 项目文档
│   └── sql/              # 数据库脚本
└── pom.xml               # Maven项目配置
```

## 核心功能模块

### 1. 系统管理
- 用户管理与权限控制
- 角色与权限配置
- 部门管理
- 菜单管理
- 系统参数配置
- 日志管理

### 2. 物联网核心功能
- **设备管理**：设备注册、状态监控、生命周期管理
- **通信端点**：MQTT协议支持，提供设备连接与通信能力
- **数据存储**：支持多种数据存储方式，默认MySQL实现
- **数据采集**：实时数据采集与处理
- **预警管理**：基于规则的预警触发与通知

### 3. 基础设施
- **代码生成器**：快速生成前后端代码，提高开发效率
- **定时任务**：支持复杂的定时调度需求
- **通用工具**：提供系统通用功能支持

## 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Node.js 14+

### 后端部署
1. 克隆项目代码
2. 导入数据库脚本（位于`doc/sql/`目录）
3. 修改数据库连接配置
4. 执行Maven构建：`mvn clean install`
5. 运行应用：`java -jar server/target/zwei-server.jar`

### 前端部署
1. 进入web目录：`cd web`
2. 安装依赖：`npm install`
3. 开发环境运行：`npm run dev`
4. 生产环境构建：`npm run build:prod`

### 一键启动脚本
- Windows: `bin/run.bat`
- Linux: `bin/run.sh`

## 模块说明

### IoT核心模块
IoT模块是本项目的核心，提供了完整的物联网设备管理和数据处理能力：

- **module-iot-core**: 定义IoT核心模型和接口
- **module-iot-device**: 设备管理实现
- **module-iot-endpoint**: 通信协议实现，目前支持MQTT
- **module-iot-storage**: 数据存储接口和实现，默认使用MySQL

## 开发指南

### 新增设备类型
1. 在`module-iot-core`中定义设备模型
2. 在`module-iot-device`中实现设备管理服务
3. 在`module-iot-storage`中实现数据存储适配

### 扩展通信协议
1. 在`module-iot-endpoint`下新增协议实现模块
2. 实现相应的连接管理器和消息处理器

## 许可证

本项目采用MIT许可证。详见[LICENSE](LICENSE)文件。

## 联系方式

如有问题或建议，请通过以下方式联系我们：

- 技术支持：support@zwei.com
- GitHub: https://github.com/zwei-project/zwei
- Gitee: https://gitee.com/zwei-project/zwei