[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-hazard**

# zwei-iot-hazard — 隐患点管理 + 分组 + 设备/视频绑定

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-hazard**

## 模块职责

隐患点 (灾害监测点) 域:

- 隐患点 CRUD (`hazard_point`) — 含经纬度/走向/边界 JSON/类型/状态/绑定设备数
- 隐患点分组 (`hazard_point_group`) — 支持排序 + 批量回填 pointCount
- **设备绑定** (`device_hazard_point`) — ON DUPLICATE KEY UPDATE 幂等绑定，维护 `hazard_point.device_count`
- **视频设备绑定** (`video_device_hazard_point` 在 video 模块) — 跨模块 FK 引用
- **实现跨模块接口** `IDeviceHazardRelationService` (由 `zwei-iot-device` 定义)

## 关键依赖

- `zwei-common`
- `zwei-iot-device` (设备/视频 + 跨模块接口)
- `zwei-iot-video` (视频设备，跨模块 FK)
- lombok

## 主要子包

| 子包             | 职责                                                                                                                                                   |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `controller`   | `HazardPointController` / `HazardPointGroupController`                                                                                               |
| `service`      | `IHazardPointService` / `IHazardPointGroupService` / `IDeviceHazardPointService` / `IVideoDeviceHazardPointService` / `IDeviceHazardRelationService` |
| `service.impl` | 5 个 Service 实现                                                                                                                                       |
| `domain`       | `HazardPoint` / `HazardPointGroup` / `DeviceHazardPoint`                                                                                             |
| `domain.dto`   | 15 个 DTO（创建/更新/绑定/导入/导出/安装位置等）                                                                                                                       |
| `mapper`       | `HazardPointMapper` / `HazardPointGroupMapper` / `DeviceHazardPointMapper`                                                                           |

## 对外接口 (Controller)

| 路径                                      | 职责                                                    |
|-----------------------------------------|-------------------------------------------------------|
| `/api/v1/iot/hazard-point/*`            | 隐患点 CRUD + 列表 (含统计) + 停测/恢复/完结/批量操作                   |
| `/api/v1/iot/hazard-point-group/*`      | 分组 CRUD                                               |
| `/api/v1/iot/hazard-point/{id}/devices` | 绑定设备 (POST) / 解绑 (DELETE) / 已绑列表 (GET) / 未绑列表 (GET)   |
| `/api/v1/iot/hazard-point/{id}/videos`  | 绑定视频设备 (POST) / 解绑 (DELETE) / 已绑列表 (GET) / 未绑列表 (GET) |

## 跨模块接口实现 (IDeviceHazardRelationService)

| 方法                                                                                         | 用途                                              |
|--------------------------------------------------------------------------------------------|-------------------------------------------------|
| `getHazardPointIdsByDeviceIds(List<Long>)`                                                 | 设备→隐患点 反查 (单 Map 索引)                            |
| `deleteBindingsByDeviceIds(List<Long>)`                                                    | 设备删除时清理绑定 (批量 DELETE)                           |
| `refreshDeviceCount(Long)` / `refreshDeviceCountByIds(List<Long>)`                         | 维护 `hazard_point.device_count` 缓存字段 (子查询 COUNT) |
| `getHazardPointNameByDeviceId(Long)`                                                       | 富化设备名 (按首关联查询)                                  |
| `countAllHazardPoints()` / `countHazardPointsByStatus()` / `countHazardPointsByMonth(int)` | 仪表盘统计                                           |

> **解耦**: `zwei-monitor` 注入 `IDeviceHazardRelationService` 即可调用上述方法, 不知道具体实现。

## 核心实现类索引 (P1)

| 类                                   | 文件                                                    | 关键方法 / 责任                                                                              |
|-------------------------------------|-------------------------------------------------------|----------------------------------------------------------------------------------------|
| `HazardPointServiceImpl`            | `service/impl/HazardPointServiceImpl.java`            | 隐患点 CRUD + `@Cacheable("hazardPoint")` + `@CacheEvict` + 状态机 (1监测中/2停测中/3已完结)          |
| `HazardPointGroupServiceImpl`       | `service/impl/HazardPointGroupServiceImpl.java`       | 分组 CRUD + 批量回填隐患点数 (避免 N+1) + 删除前校验 (子隐患点非空禁止)                                         |
| `DeviceHazardPointServiceImpl`      | `service/impl/DeviceHazardPointServiceImpl.java`      | 设备绑定：`bindDevices` 用 ON DUPLICATE KEY UPDATE 幂等 + `unbindDevices` 原子递减 + `@CacheEvict` |
| `DeviceHazardRelationServiceImpl`   | `service/impl/DeviceHazardRelationServiceImpl.java`   | 实现 `IDeviceHazardRelationService` (薄封装委托)                                              |
| `VideoDeviceHazardPointServiceImpl` | `service/impl/VideoDeviceHazardPointServiceImpl.java` | 视频设备绑定（未深入扫描，结构与 DeviceHazardPointServiceImpl 类似）                                      |

## 隐患点缓存

- `@Cacheable(value = "hazardPoint", key = "#id")` 用于 `selectHazardPointById`
- `@CacheEvict(value = "hazardPoint", key = "#id")` 用于 insert/update/delete/pause/complete
- `@CacheEvict(value = "hazardPoint", allEntries = true)` 用于批量删除/批量操作/分组变更
- `DeviceHazardPointServiceImpl.bindDevices` / `unbindDevices` 也触发 `@CacheEvict("hazardPoint", key="#hazardPointId")`
- `HazardPointGroupServiceImpl.updateHazardPointGroup` 触发 `@CacheEvict(value="hazardPoint", allEntries=true)`

## device_count 维护策略 (REPEATABLE READ 安全)

- **绑定 (bind)**: 不预查后递增，而是用 `hazardPointMapper.refreshDeviceCountById(hazardPointId)` 走子查询
  `COUNT(*) FROM device_hazard_point` —— 避免 REPEATABLE READ 下快照漂移
- **解绑 (unbind)**: 原子递减 `GREATEST(device_count - N, 0)`，N 为实际删除行数

## 状态机 (HazardPoint.status)

| 状态码 | 名称  | 操作                                 |
|-----|-----|------------------------------------|
| 1   | 监测中 | 默认                                 |
| 2   | 停测中 | `updateHazardPointPause(id, true)` |
| 3   | 已完结 | `completeHazardPoint(id)`          |

## 数据模型

- `hazard_point` — 隐患点 (id / code UNIQUE / name / groupId / longitude / latitude / strike 走向角度 / boundaryCoords
  JSON 多边形 / description / status / deviceCount / delFlag)
    - boundaryCoords JSON 格式: `{"polygon":[[lat,lng],...], "strikeCoords":[[lat,lng],...], "strikeAngle":...}`
- `hazard_point_group` — 分组 (id / code UNIQUE / name / description / sortOrder / status / delFlag)
- `device_hazard_point` — 设备绑定 (id / deviceId / hazardPointId / installLongitude / installLatitude / bindTime)
    - UNIQUE `uk_device_hazard_point` (deviceId, hazardPointId)
    - FK `fk_dhp_device` → device(id) / `fk_dhp_hp` → hazard_point(id)
    - CHECK `chk_dhp_lat` / `chk_dhp_lng` (经纬度范围)
- `video_device_hazard_point` (在 video 模块) — 视频设备绑定 (类似结构 + FK)

## 测试与质量

- 单元测试: 地理工具 (距离/包含)
- 集成测试: 隐患点批量导入 + 设备绑定关系校验

## 常见问题 (FAQ)

**Q: 删除隐患点是否会级联删除绑定?**
A: 是, 通过 `deleteBindingsByDeviceIds` + `device_hazard_point` 表的级联删除 (DB 级 FK + `@Transactional`)。建议在删除前提示用户。

**Q: 隐患点统计为什么用单独字段 (`device_count`)?**
A: 频繁查询时避免连表 COUNT, 由 `refreshDeviceCount()` 在绑定变更时同步维护。

**Q: 跨模块接口如何被 `zwei-monitor` 发现?**
A: 本模块用 `@Service` 注解实现类 (`DeviceHazardRelationServiceImpl`), Spring 自动注入到 `IDeviceHazardRelationService`
的注入点, 无需额外配置。

**Q: 隐患点已停用/已完结能否绑定设备?**
A: 不能，`DeviceHazardPointServiceImpl.ensureHazardPointExists()` 校验 `status=1 (监测中)` + `delFlag=0`。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/hazardpoint/controller/HazardPointController.java`
- `src/main/java/com/zwei/iot/hazardpoint/controller/HazardPointGroupController.java`
- `src/main/java/com/zwei/iot/hazardpoint/service/IHazardPointService.java`
- `src/main/java/com/zwei/iot/hazardpoint/service/IDeviceHazardPointService.java`
- `src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointGroupServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/hazardpoint/service/impl/DeviceHazardPointServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/hazardpoint/service/impl/DeviceHazardRelationServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/hazardpoint/service/impl/VideoDeviceHazardPointServiceImpl.java` (P1)

## 变更记录 (Changelog)

| 时间               | 变更                                                                                                           |
|------------------|--------------------------------------------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                                                                  |
| 2026-06-10 19:08 | 增量补扫: 新增核心实现类索引、@Cacheable/@CacheEvict 策略、device_count REPEATABLE READ 安全维护、隐患点状态机表、device_hazard_point 完整字段 |
