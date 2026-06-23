[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-monitor**

# zwei-iot-monitor — 监测字典 (type / content)

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-monitor**

## 模块职责

定义 IoT 业务中"监测指标"的二级字典:

- **监测类型** (`monitor_type`) — 一级分类 (例: 雨量监测、位移监测、水位监测)
- **监测内容** (`monitor_content`) — 二级字段 (例: 小时雨量、X轴位移、水位)

> **叶子模块**: 不依赖任何其他 IoT 子模块, 也不被业务反向调用 (只提供字典查询)。

> **历史变更 (2026-06-14)**: 原先的三级字典"监测大类 (monitor_category)"已移除。大类与类型几乎 1:1 映射, 且 MyBatis
> 层从未实际读写 category_id, 功能已事实失效。

## 关键依赖

- `zwei-common` (基础)
- `lombok`
- MyBatis (由父 POM 引入)

## 主要子包

| 子包                         | 职责                                                   |
|----------------------------|------------------------------------------------------|
| `controller`               | `MonitorTypeController` / `MonitorContentController` |
| `service` / `service.impl` | `IMonitorTypeService` / `IMonitorContentService`     |
| `mapper`                   | MyBatis 数据访问                                         |
| `domain`                   | 两个层级实体                                               |

## 对外接口

| 路径前缀                       | 职责      |
|----------------------------|---------|
| `/api/v1/monitor-types`    | 类型 CRUD |
| `/api/v1/monitor-contents` | 内容 CRUD |

## 关键 Service 接口

- `IMonitorTypeService` — 类型 (含 `selectMonitorTypeAllWithContents`)
- `IMonitorContentService` — 内容 (含 `getByTypeId`)

## 数据模型

- `monitor_type` — (id, code, name, icon, description, sort_order, status, ...)
- `monitor_content` — (id, monitor_type_id, code, name, unit, indicator_type, range_min, range_max, ...)

## 测试与质量

- 单元测试: 类型批量加载 (`MonitorTypeService.selectMonitorTypeAllWithContents`)
- 集成测试: 类型 CRUD + 内容同步

## 常见问题 (FAQ)

**Q: 设备/传感器如何绑定监测类型与内容?**
A: `zwei-iot-device` 的 `device_sensor` 表中有 `monitor_type_id` / `monitor_content_id` 外键, 查询时连表获取名称/单位/字段名。

**Q: 监测内容编码 (`code`) 有什么约束?**
A: 同一监测类型内唯一 (`uk_monitor_content_code(monitor_type_id, code)`，v2.10 从全局唯一改为类型内唯一)，用于与 sensor_attribute.attr_code 关联; 建议 `snake_case` 且不含特殊字符, 与前端 ECharts 字段映射对齐。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/monitor/controller/MonitorTypeController.java`
- `src/main/java/com/zwei/iot/monitor/controller/MonitorContentController.java`
- `src/main/java/com/zwei/iot/monitor/service/IMonitor*Service.java`

## 变更记录 (Changelog)

| 时间               | 变更                                |
|------------------|-----------------------------------|
| 2026-06-14       | 移除监测大类 (MonitorCategory), 三级简化为二级 |
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)       |
