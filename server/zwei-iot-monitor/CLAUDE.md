[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-monitor**

# zwei-iot-monitor — 监测字典 (category / type / content)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-monitor**

## 模块职责

定义 IoT 业务中"监测指标"的三级字典:

- **监测大类** (`monitor_category`) — 一级分类 (例: 表面位移、地下水位、雨量、视频)
- **监测类型** (`monitor_type`) — 二级分类 (例: 表面位移下的 GNSS、静力水准)
- **监测内容** (`monitor_content`) — 三级字段 (例: X 累计位移、Y 累计位移、瞬时水位)

> **叶子模块**: 不依赖任何其他 IoT 子模块, 也不被业务反向调用 (只提供字典查询)。

## 关键依赖

- `zwei-common` (基础)
- `lombok`
- MyBatis (由父 POM 引入)

## 主要子包

| 子包                         | 职责                                                                                 |
|----------------------------|------------------------------------------------------------------------------------|
| `controller`               | `MonitorCategoryController` / `MonitorTypeController` / `MonitorContentController` |
| `service` / `service.impl` | `IMonitorCategoryService` / `IMonitorTypeService` / `IMonitorContentService`       |
| `mapper`                   | MyBatis 数据访问                                                                       |
| `domain`                   | 三个层级实体                                                                             |

## 对外接口

| 路径前缀                           | 职责      |
|--------------------------------|---------|
| `/api/v1/iot/monitor-category` | 大类 CRUD |
| `/api/v1/iot/monitor-type`     | 类型 CRUD |
| `/api/v1/iot/monitor-content`  | 内容 CRUD |

## 关键 Service 接口

- `IMonitorCategoryService` — 大类
- `IMonitorTypeService` — 类型 (含 `getByCategoryId`)
- `IMonitorContentService` — 内容 (含 `getByTypeId`)

## 数据模型

- `iot_monitor_category` — (id, name, code, icon, sort, status, ...)
- `iot_monitor_type` — (id, categoryId, name, code, unit, valueType, ...)
- `iot_monitor_content` — (id, typeId, name, code, fieldKey, fieldType, sort, ...)

## 测试与质量

- 单元测试: 树形结构组装 (`MonitorCategoryService.listWithChildren`)
- 集成测试: 字典导入 (`POST /api/v1/iot/monitor-content/import`)

## 常见问题 (FAQ)

**Q: 设备/传感器如何绑定监测类型与内容?**
A: `zwei-iot-device` 的 `device_sensor` 表中有 `monitor_type_id` / `monitor_content_id` 外键, 查询时连表获取名称/单位/字段名。

**Q: 字段名 (`fieldKey`) 有什么约束?**
A: 必须唯一, 用于 IoTDB 时序列名; 建议 `snake_case` 且不含特殊字符, 与前端 ECharts 字段映射对齐。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/monitor/controller/MonitorCategoryController.java`
- `src/main/java/com/zwei/iot/monitor/controller/MonitorTypeController.java`
- `src/main/java/com/zwei/iot/monitor/controller/MonitorContentController.java`
- `src/main/java/com/zwei/iot/monitor/service/IMonitor*Service.java`

## 变更记录 (Changelog)

| 时间               | 变更                          |
|------------------|-----------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描) |
