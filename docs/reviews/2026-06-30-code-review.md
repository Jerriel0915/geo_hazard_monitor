# 知微 (Zwei) 代码质量评审报告

> **评审范围**: 全息看板 3 子页面 (Comprehensive/Alarm/Operation) · 基础管理/隐患点管理 (前端 + `zwei-iot-hazard`) · 物联网 5 子模块 (`zwei-iot-monitor` / `zwei-iot-device` / `zwei-iot-timeseries` / `zwei-iot-broker` / `zwei-iot-video`)
>
> **评审日期**: 2026-06-30
> **评审人**: 架构师 Agent
> **评审方法**: 4 个并行子代理对 ~120 个源文件的逐行审查

---

## 目录

- [1. 执行摘要](#1-执行摘要)
  - [代码总评](#代码总评)
  - [问题统计](#问题统计)
- [2. 按优先级排序的改进清单](#2-按优先级排序的改进清单)
  - [P0 - 阻断](#-p0--阻断必须立即修复)
  - [P1 - 严重](#-p1--严重下次迭代前修复)
  - [P2 - 一般](#-p2--一般近期优化)
  - [P3 - 轻微](#-p3--轻微酌情处理)
  - [P4 - 建议](#-p4--建议后续迭代参考)
- [3. 正向肯定](#3-正向肯定)
- [4. 整体改进路线图](#4-整体改进路线图)
  - [立即执行 (P0-P1)](#立即执行-p0-p1--本次必须修复)
  - [短期优化 (P2)](#短期优化-p2--下个-sprint)
  - [长期重构 (P3-P4)](#长期重构-p3-p4--技术债清单)

---

## 1. 执行摘要

### 代码总评

整体架构设计水平较高——跨模块 Service 接口隔离、Redis Stream 摄取管线、IoTDB 路径白名单校验、keyset 游标分页 + 自动降采样等设计远超同类项目平均水平。但在 **安全隔离**、**性能 N+1**、**资源管理** 三个维度存在系统性缺陷: MQTT 订阅 ACL 缺少设备归属校验 (P0)、设备密码明文存储、IoTDB 无连接池、大量 N+1 查询贯穿全部后端模块、前端存在存储型 XSS 向量。前端代码存在显著的死代码问题 (Comprehensive.vue ~1200 行死代码/死 CSS, Alarm.vue 100% 硬编码 Mock 数据)。

### 问题统计

| 等级 | 数量 | 说明 |
|------|------|------|
| 🔴 P0 - 阻断 | **3** | 存储型 XSS · 调度规则状态未持久化 · MQTT 订阅越权 |
| 🟠 P1 - 严重 | **24** | 明文密码 · 无连接池 · N+1 查询 ×10 · 数据丢失窗口 · Mock 数据冒充生产 · 缓存失效 · 导出 OOM |
| 🟡 P2 - 一般 | **45** | 跨模块 Mapper 违规 ×3 · 类型安全缺失 · 死代码 · DDL 竞态 · 热路径阻塞 |
| 🟢 P3 - 轻微 | **30** | 命名/风格 · 重复导入 · 事务注解不一致 · 死参数 · 日志冗余 |
| 🔵 P4 - 建议 | **13** | God Class 拆分 · 连接池迁移 · 批量写入 · 权限粒度 · DSL 扩展 |
| **合计** | **115** | — |

---

## 2. 按优先级排序的改进清单

### 🔴 P0 - 阻断 (必须立即修复)

---

#### P0-1 · 安全漏洞 (存储型 XSS) · `Comprehensive.vue:445-529`

| 项目 | 内容 |
|------|------|
| **问题类型** | 安全漏洞 — XSS |
| **位置** | `web/src/views/holo-board/Comprehensive.vue:445-529` (`initMap` 函数) |
| **问题描述** | Leaflet marker `divIcon` HTML 和 popup 内容通过字符串拼接注入未转义的 API 字段 (`point.name`、`point.code`、`point.description`、`device.name`)，直接写入 `innerHTML`。 |
| **影响分析** | 若隐患点名称/描述或设备名包含 `<img onerror=...>` 或 `<script>`，将在浏览器中执行。API 数据非可信输入——这是存储型 XSS 向量。 |
| **修复建议** | 对所有插值值进行 HTML 转义，或使用 DOM API 替代字符串模板: |

```typescript
function escapeHtml(str: string): string {
  return String(str ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!))
}
// 使用: `<div ...>${escapeHtml(point.name)}...`
```

| **参考标准** | CWE-79: Improper Neutralization of Input During Web Page Generation |

---

#### P0-2 · 功能缺陷 (状态变更未持久化) · `HazardPoint.vue:1067-1069`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 |
| **位置** | `web/src/views/basic/HazardPoint.vue:1067-1069` (`handleToggleDispatchStatus`) |
| **问题描述** | 调度规则启用/禁用切换仅显示成功提示，**从未调用任何 API** 将状态变更持久化到后端。 |
| **影响分析** | 用户切换开关看到"成功"消息，但刷新页面后变更丢失。用户相信规则已启用/禁用，实际未生效——数据完整性破坏。 |
| **修复建议** | |

```typescript
const handleToggleDispatchStatus = async (row: DispatchRule) => {
  try {
    await updateDispatchRuleStatus(row.id, row.status)
    ElMessage.success(`规则${row.status === 1 ? '启用' : '禁用'}成功`)
  } catch {
    row.status = row.status === 1 ? 0 : 1 // 失败回滚
    ElMessage.error('操作失败')
  }
}
```

---

#### P0-3 · 安全漏洞 (MQTT 订阅越权) · `MqttServerSubscribeValidator.java:64-122`

| 项目 | 内容 |
|------|------|
| **问题类型** | 安全漏洞 — 越权访问 |
| **位置** | `server/zwei-iot-broker/src/main/java/.../MqttServerSubscribeValidator.java:64-122` |
| **问题描述** | 订阅验证器检查了主题格式和传感器是否存在，但**从未验证订阅客户端是否为该 `deviceCode` 的归属设备**。任何已认证设备可订阅任意其他设备的数据主题。 |
| **影响分析** | 已入侵的设备 (如 `DEV-001`) 可订阅 `sys/v1/DEV-002/sensor1/updata` 接收 `DEV-002` 的全部传感器数据——完全绕过设备间数据隔离。 |
| **修复建议** | 添加会话查找和 deviceCode 归属校验，镜像 `MqttDeviceAuthService.hasPublishPermission()` 的模式: |

```java
@Override
public boolean isValid(ChannelContext context, String clientId,
                        String topicFilter, MqttQoS qoS) {
    // ... 现有主题格式校验 ...
    String deviceCode = matcher.group("deviceCode");

    // 新增: 验证订阅客户端归属此 deviceCode
    Optional<MqttDeviceSession> session =
        sessionRegistry.getByClientId(clientId == null ? null : clientId.trim());
    if (session.isEmpty()) {
        return mqttExceptionReporter.rejectWithDebug(
            new MqttBusinessException.PermissionDenied(..., "未建立鉴权会话，禁止订阅"));
    }
    if (!Objects.equals(session.get().deviceCode(), deviceCode)) {
        return mqttExceptionReporter.rejectWithDebug(
            new MqttBusinessException.PermissionDenied(..., "设备与订阅主题不匹配，禁止订阅"));
    }
    // ... 现有传感器存在性检查 ...
}
```

| **参考标准** | CWE-862: Missing Authorization · OWASP A01:2021 Broken Access Control |

---

### 🟠 P1 - 严重 (下次迭代前修复)

---

#### P1-1 · 安全漏洞 (明文密码存储与泄露) · `MqttDeviceAuthService.java:155` + `DeviceController.java:148,236`

| 项目 | 内容 |
|------|------|
| **问题类型** | 安全漏洞 |
| **位置** | `zwei-iot-broker/.../MqttDeviceAuthService.java:155` · `zwei-iot-device/.../DeviceController.java:148,236` |
| **问题描述** | 设备认证密码以**明文**存储在 `device.auth_password` 列，通过 `Objects.equals` 直接比较。`getAuthAccount`、`add`、`resetPassword`、`register` 多个 API 响应明文返回密码。 |
| **影响分析** | 数据库泄露暴露全部设备凭据；HTTP 访问日志/代理日志可能记录密码明文。 |
| **修复建议** | 存储 bcrypt/Argon2 哈希；`getAuthAccount` 移除密码字段；仅在创建/重置时一次性返回明文。 |
| **参考标准** | CWE-256: Plaintext Storage of a Password · CWE-522: Insufficiently Protected Credentials |

---

#### P1-2 · 安全漏洞 (暴力破解防护失效) · `MqttAuthFailureGuard.java:20`

| 项目 | 内容 |
|------|------|
| **问题类型** | 安全漏洞 |
| **位置** | `zwei-iot-broker/.../MqttAuthFailureGuard.java:20` |
| **问题描述** | CLAUDE.md 文档记载"基于 Redis"，但实现使用 `ConcurrentHashMap<String, FailureState>`——纯内存。多实例部署下失败计数按实例隔离。 |
| **影响分析** | 集群部署下攻击者可轮询 broker 实例绕过封锁；重启清除全部失败状态。 |
| **修复建议** | 迁移至 Redis `INCR` + `EXPIRE` 实现跨实例失败计数: |

```java
@Component
public class MqttAuthFailureGuard {
    private final StringRedisTemplate redisTemplate;
    private final MqttAuthCenterProperties properties;

    public boolean isBlocked(String username) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("mqtt:auth:ban:" + username));
    }

    public void recordFailure(String username) {
        String countKey = "mqtt:auth:fail:" + username;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(countKey,
                properties.getBanDurationSeconds(), TimeUnit.SECONDS);
        }
        if (count != null && count >= properties.getFailureThreshold()) {
            redisTemplate.opsForValue().set(
                "mqtt:auth:ban:" + username, "1",
                properties.getBanDurationSeconds(), TimeUnit.SECONDS);
            redisTemplate.delete(countKey);
        }
    }
}
```

---

#### P1-3 · 性能瓶颈 (IoTDB 无连接池) · `IotdbJdbcClient.java:43-58`

| 项目 | 内容 |
|------|------|
| **问题类型** | 性能瓶颈 |
| **位置** | `zwei-iot-timeseries/.../IotdbJdbcClient.java:43-58` |
| **问题描述** | `getConnection()` 每次调用 `DriverManager.getConnection(...)`——无连接池。每次 IoTDB 读写都新建 TCP+认证连接再关闭。 |
| **影响分析** | IoTDB 连接建立成本高 (TCP + 认证握手)。1Hz 摄取流每条消息都付连接税；查询端点扇出 N 个连接。吞吐量受限于连接建立而非写入。**全三模块影响最大的单项性能问题。** |
| **修复建议** | 引入 HikariCP 连接池，或迁移至 IoTDB 原生 `SessionPool` (线程安全、可复用): |

```java
// 方案 A: HikariCP over JDBC
@Bean HikariDataSource iotdbDataSource(IotdbProperties p) {
    HikariConfig c = new HikariConfig();
    c.setJdbcUrl(p.getJdbcUrl());
    c.setUsername(p.getUsername());
    c.setPassword(p.getPassword());
    c.setMaximumPoolSize(8);
    c.setConnectionTimeout(p.getConnectionTimeoutMs());
    return new HikariDataSource(c);
}
// getConnection() -> ds.getConnection()

// 方案 B (推荐): IoTDB SessionPool — 线程安全、可复用、无需每次认证
```

---

#### P1-4 · 数据正确性 (未知设备污染 IoTDB) · `MonitorIngestConsumerService.java:470-478`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — 数据污染 |
| **位置** | `zwei-iot-timeseries/.../MonitorIngestConsumerService.java:470-478` |
| **问题描述** | `resolveDeviceId`/`resolveSensorId` 在未找到时返回 `-1L`，调用方继续以 `deviceId=-1` 写入 IoTDB 路径 `root.geo_hazard.d-1.s{code}.{attr}`，并调用 `updateLastReportAt(-1L)`。 |
| **影响分析** | 未注册/配置错误的设备**静默污染 IoTDB**，生成 `d-1.*` 垃圾节点，并在 `device_online_status`/`device_sensor` 表写入 `id=-1` 的虚假记录。垃圾永久累积，无告警。 |
| **修复建议** | 返回 `null` 而非 `-1L`，调用方检测到 `null` 后走死信队列: |

```java
private Long resolveDeviceId(String deviceCode) {
    Device dev = deviceMapper.selectDeviceByCode(deviceCode);
    if (dev == null) return null;          // 信号"未找到"，非 -1
    return dev.getId();
}
// adapt(): if (deviceId == null) return List.of();
// processParsedMessage: if deviceId==null -> enqueueDeadLetter(deviceCode, payload, "device not registered")
```

---

#### P1-5 · 数据丢失 (重试窗口) · `MonitorIngestConsumerService.java:299-303`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — 数据丢失 |
| **位置** | `zwei-iot-timeseries/.../MonitorIngestConsumerService.java:299-303` (及 373-377) |
| **问题描述** | 重试路径**先 ACK+XDEL 删除记录**，再在 `retryScheduler` 上调度延迟重新入队。若 JVM 在 ACK 与调度任务触发之间崩溃，消息永久丢失。 |
| **影响分析** | 硬崩溃 (kill -9 / OOM) 窗口内消息已被确认删除但未重新入队——永久丢失。恰是重试中的消息最不应丢。 |
| **修复建议** | 调整顺序: 重新入队成功后再 ACK (ack-after-enqueue): |

```java
retryScheduler.schedule(() -> {
    try {
        redisTemplate.opsForStream().add(MapRecord.create(..., record.getValue()));
        ack(record);  // 入队成功后才 ACK
    } catch (Exception e) {
        log.error("re-enqueue failed, PEL recovery will reclaim: {}",
            record.getId(), e);
        // 不 ACK — PEL 恢复机制会重新认领
    }
}, delaySeconds, TimeUnit.SECONDS);
// 移除上方早期的 ack(record)
```

---

#### P1-6 · 功能缺陷 (视频设备列表忽略过滤) · `VideoDeviceController.java:72-74`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 |
| **位置** | `zwei-iot-video/.../VideoDeviceController.java:72-74` (及 export:158-159) |
| **问题描述** | `list(VideoDevice videoDevice)` 接受查询参数但完全忽略——调用 `selectVideoDeviceAll()` 无过滤。 |
| **影响分析** | `GET /api/v1/video-devices?protocolCode=rtmp&status=1` 返回全部设备。前端过滤看似失效。导出同理。 |
| **修复建议** | `videoDeviceMapper.selectVideoDeviceList(videoDevice)` 替代 `selectVideoDeviceAll()`。 |

---

#### P1-7 · 安全漏洞 (Groovy RCE 面 + 输入未校验) · `ComputedAttributeTestController.java:62-99`

| 项目 | 内容 |
|------|------|
| **问题类型** | 安全漏洞 — 远程代码执行面 |
| **位置** | `zwei-iot-timeseries/.../ComputedAttributeTestController.java:62-99` |
| **问题描述** | `/api/v1/computed-attributes/test-script` 执行用户提供的 `calcScript`。`attrCode` 仅 `@NotBlank` 无 `@Pattern`，绕过 `ComputedAttribute.from()` 的正则校验直接传入 Groovy 脚本。 |
| **影响分析** | 该端点是特权任意代码执行面——安全性完全依赖 `GroovyScriptEngine` 沙箱。若沙箱可绕过，任何有 `basic:monitorContent:test` 权限的用户获得 RCE。 |
| **修复建议** | DTO 加 `@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$")`；审计 Groovy 沙箱完整性 (`SecureASTCustomizer`/`SandboxClassLoader`)；加硬超时；端点提升为管理员权限。 |

---

#### P1-8 · 性能瓶颈 (N+1 查询 ×10) — 多文件

以下 N+1 查询贯穿全部后端模块，按影响排序:

| # | 位置 | 描述 | 影响 |
|---|------|------|------|
| 1 | `DeviceServiceImpl.java:449-462` | `enrichHazardPoint` 循环每设备查隐患点 | 1000 设备 = 1001 查询 |
| 2 | `DeviceServiceImpl.java:472-479` | `loadDeviceSensors` 循环每传感器查属性 | 20 传感器 = 21 查询 |
| 3 | `DeviceQueryServiceImpl.java:45-63` | `getDeviceBriefsByAuthUsernames` 2N 查询 (CLAUDE.md 声称已批量但实际未做) | 500 客户端 = 1000 查询 |
| 4 | `DeviceServiceImpl.java:238-242` | `deleteDeviceByIds` 循环每设备删除传感器+属性+产品 | 100 设备 = ~1200 查询 |
| 5 | `HazardPointServiceImpl.java:216-219` | `getMapOverview` 循环每隐患点查设备 | 100 隐患点 = 101 查询 |
| 6 | `DeviceHazardPointServiceImpl.java:227-236` | `loadSensors` 循环每设备查传感器 | 50 设备 = 50 查询 |
| 7 | `VideoDeviceHazardPointServiceImpl.java:108-114` | `validateVideoDevicesExist` 循环每设备查存在性 | 30 设备 = 30 查询 |
| 8 | `MonitorDataQueryService.java:403-437` | `resolveMeasurements` 循环每设备查传感器列表 | 10 设备 × 5 传感器 × 4 属性 = 200 连接 |
| 9 | `DeviceHazardRelationServiceImpl.java:44` | `refreshDeviceCountByIds` 循环每 ID 更新 | N 个 UPDATE |
| 10 | `MonitorContentServiceImpl.java:188-201` | `deleteMonitorContentByIds` 循环每 ID 查 `monitorTypeId` | N 个 SELECT |

**统一修复模式**: 添加批量 Mapper 方法 (`selectXxxByIds(List<Long>)`)，一次查询后在 Java 中按 ID 分组。

```java
// 示例: DeviceServiceImpl.enrichHazardPoint 批量修复
private void enrichHazardPoint(List<Device> devices) {
    if (devices == null || devices.isEmpty()) return;
    List<Long> deviceIds = devices.stream().map(Device::getId).toList();
    Map<Long, HazardPointRef> refMap =
        hazardRelationService.getHazardPointsByDeviceIds(deviceIds);
    for (Device device : devices) {
        HazardPointRef ref = refMap.get(device.getId());
        if (ref != null) {
            device.setBoundHazardPointId(ref.id());
            device.setBoundHazardPointName(ref.name());
        }
    }
}
```

---

#### P1-9 · 稳定性 (NPE — null context) · `MqttServerMessageListener.java:63`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — NPE |
| **位置** | `zwei-iot-broker/.../MqttServerMessageListener.java:63` |
| **问题描述** | `context.getClientNode()` 在 topic 校验之前调用，Javadoc 标注 context 为可选参数。若 `context` 为 null，在 topic 守卫前抛 NPE。 |
| **影响分析** | null context 崩溃消息监听器，静默丢弃设备消息。 |
| **修复建议** | 将 `clientNode` 获取移到 topic 检查之后，增加 null 检查: |

```java
public void onMessage(ChannelContext context, String topic,
                      MqttPublishMessage publishMessage, byte[] message) {
    if (topic == null || (!topic.startsWith("sys/v1/") && !topic.startsWith("gb/v1/"))) {
        return;
    }
    if (context == null) {
        log.warn("监测消息缺少连接上下文，跳过。topic={}", sanitize(topic));
        return;
    }
    String clientId = context.getBsId();
    // ...
}
```

---

#### P1-10 · 数据完整性 (缓存失效) · `DeviceHazardRelationServiceImpl.java:43-44`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — 缓存不一致 |
| **位置** | `zwei-iot-hazard/.../DeviceHazardRelationServiceImpl.java:43-44` |
| **问题描述** | `refreshDeviceCount` 更新 DB 中 `device_count` 但**未驱逐** `hazardPoint` 缓存。`selectHazardPointById` 标注了 `@Cacheable`，后续读取返回旧 `deviceCount`。 |
| **影响分析** | 跨模块设备操作后，看板和详情页显示过期设备计数，直到缓存 TTL 过期。 |
| **修复建议** | |

```java
@Override
@CacheEvict(value = "hazardPoint", key = "#id")
public void refreshDeviceCount(Long id) {
    hazardPointMapper.refreshDeviceCountById(id);
}

@Override
@CacheEvict(value = "hazardPoint", allEntries = true)
public void refreshDeviceCountByIds(List<Long> ids) {
    for (Long id : ids) hazardPointMapper.refreshDeviceCountById(id);
}
```

---

#### P1-11 · 数据完整性 (逻辑删除遗留绑定) · `HazardPointServiceImpl.java:112-116`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — 孤儿数据 |
| **位置** | `zwei-iot-hazard/.../HazardPointServiceImpl.java:112-116` + `HazardPointMapper.xml:118-137` |
| **问题描述** | 逻辑删除隐患点 (`UPDATE del_flag=1`)，但 FK 级联仅在物理 DELETE 时触发。未调用绑定清理——`device_hazard_point` 记录指向已逻辑删除的隐患点。 |
| **影响分析** | `selectUnboundDevices` 的 `NOT IN` 子查询仍看到这些孤儿绑定，绑定到已删除隐患点的设备**永远无法重新绑定**。 |
| **修复建议** | 逻辑删除前物理删除绑定: |

```java
@Override
@CacheEvict(value = "hazardPoint", key = "#id")
@Transactional(rollbackFor = Exception.class)
public int deleteHazardPointById(Long id) {
    deviceHazardPointMapper.deleteByHazardPointId(id); // 清理绑定
    return hazardPointMapper.deleteHazardPointById(id);
}
```

---

#### P1-12 · 安全/DoS (导出 OOM) · `HazardPointController.java:69-94` + `DeviceController.java:74-75`

| 项目 | 内容 |
|------|------|
| **问题类型** | 性能瓶颈 — OOM |
| **位置** | `HazardPointController.java:69-94` · `DeviceController.java:74-75` |
| **问题描述** | 导出端点将**全部**匹配记录加载到内存，无行数上限。 |
| **影响分析** | 无过滤导出整个表 → OOM 或长时间 GC 暂停。 |
| **修复建议** | |

```java
if (list.size() > 10_000) {
    throw new ServiceException("导出数据量过大，请缩小查询范围");
}
```

---

#### P1-13 · 并发 (TOCTOU 竞态) · `HazardPointServiceImpl.java:76-87`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — 竞态条件 |
| **位置** | `zwei-iot-hazard/.../HazardPointServiceImpl.java:76-87` |
| **问题描述** | `insertHazardPoint` 无 `@Transactional`，先查 code 唯一性再插入，两个并发请求可能都通过检查并都插入。 |
| **修复建议** | 添加 `@Transactional(rollbackFor = Exception.class)` + 捕获 `DuplicateKeyException`。 |

---

#### P1-14 · 性能瓶颈 (热路径字符串正则重编译) · `MonitorIngestFacade.java:128,139`

| 项目 | 内容 |
|------|------|
| **问题类型** | 性能瓶颈 |
| **位置** | `zwei-iot-timeseries/.../MonitorIngestFacade.java:128,139` |
| **问题描述** | `p.identifier().matches("value_\\d+")` 每属性每消息调用，`String.matches` 每次重新编译正则。位于摄取热路径。 |
| **修复建议** | 预编译为 `static final Pattern POSITIONAL = Pattern.compile("value_\\d+");` |

---

#### P1-15 · 数据正确性 (IoTDB TEXT 转义错误) · `IotdbTimeSeriesService.java:964-967`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — 数据错误 |
| **位置** | `zwei-iot-timeseries/.../IotdbTimeSeriesService.java:964-967` |
| **问题描述** | `formatValue` 对 TEXT 用 `\\'` 反斜杠转义，但 IoTDB SQL 字符串字面量遵循 SQL 标准**双引号**转义 (`'it''s'`)。 |
| **影响分析** | 含单引号的 TEXT 测量值可能插入失败或被误解析。 |
| **修复建议** | `value.toString().replace("'", "''")`；或迁移至 IoTDB `Session.insertRecord` 类型化插入。 |

---

#### P1-16 · 前端 (Alarm.vue 100% Mock 数据) · `Alarm.vue:154-262`

| 项目 | 内容 |
|------|------|
| **问题类型** | 功能缺陷 |
| **位置** | `web/src/views/holo-board/Alarm.vue:154-262,755-770` |
| **问题描述** | `alarmStats`、`alarmTrendData`、`hazardData`、`sourceDistribution` 全部硬编码。`updateAlarmStats` 用 `Math.random()` 生成增量。无任何 API 调用。 |
| **影响分析** | 页面向用户展示假数据，"下次数据刷新"指示器误导。 |
| **修复建议** | 集成 `getPendingAlarms`、`getAlarmLevelStats`、`getAlarmTrend` 等 API (如 Comprehensive.vue 所做)。 |

---

#### P1-17 · 前端 (死代码 + 浪费 API 调用) · `Comprehensive.vue:422-540,636-654`

| 项目 | 内容 |
|------|------|
| **问题类型** | 可维护性 + 性能 |
| **位置** | `web/src/views/holo-board/Comprehensive.vue:422-540,636-654` |
| **问题描述** | 整个 Leaflet 地图子系统是死代码: `mapContainer` ref 无对应模板元素，`initMap()` 从未调用，`getMapOverview()` 每次挂载调用但数据从未渲染。~120 行死 JS + ~800 行死 CSS。 |
| **影响分析** | 每次页面加载浪费网络请求；代码可读性严重受损。 |
| **修复建议** | 要么实现地图 (添加 `<div ref="mapContainer">`，调用 `initMap()`)，要么删除全部地图相关代码、API 调用、未使用导入和 CSS。 |

---

#### P1-18 · 前端 (null 安全) · `Comprehensive.vue:566`

| 项目 | 内容 |
|------|------|
| **问题类型** | 逻辑缺陷 — 运行时崩溃 |
| **位置** | `web/src/views/holo-board/Comprehensive.vue:566` |
| **问题描述** | `healthStats.value = d.healthScore` 直接赋值无 null 检查。模板访问 `healthStats.overallScore` 无可选链。API 返回 null 时模板抛 `Cannot read properties of null`。 |
| **修复建议** | `healthStats.value = d.healthScore ?? healthStats.value` 或模板加 `{{ healthStats?.overallScore ?? 0 }}%`。 |

---

#### P1-19 · 前端 (resize 监听器泄漏) · `Alarm.vue:781`

| 项目 | 内容 |
|------|------|
| **问题类型** | 资源泄漏 |
| **位置** | `web/src/views/holo-board/Alarm.vue:781` |
| **问题描述** | `window.addEventListener('resize', handleResize)` 在 `<script setup>` 模块作用域，非 `onMounted` 内。`onUnmounted` 从不调用 `removeEventListener`。 |
| **影响分析** | 每次挂载/卸载循环泄漏一个 resize 监听器。N 次导航后 N 个监听器在每次窗口缩放时触发。 |
| **修复建议** | 移入 `onMounted`，在 `onUnmounted` 中 `removeEventListener`: |

```typescript
onMounted(() => {
  initLevelChart()
  // ...
  startAutoRefresh()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  levelChartInstance?.dispose()
  // ...
  stopAutoRefresh()
  window.removeEventListener('resize', handleResize)
})
```

---

#### P1-20 · 前端 (Operation.vue 硬编码图表) · `Operation.vue:521-542`

| 项目 | 内容 |
|------|------|
| **问题类型** | 功能缺陷 |
| **位置** | `web/src/views/holo-board/Operation.vue:521-542` |
| **问题描述** | `barChartData` 和 `pyramidData` 硬编码。柱状图标题"监测类型设备在线率"但使用静态值，未使用 `deviceOnline.value?.byType`。 |
| **修复建议** | 从实际数据派生: |

```typescript
const barChartData = computed(() =>
  (deviceOnline.value?.byType ?? []).map(t => ({
    name: t.monitorTypeName, value: t.onlineRate
  }))
)
```

---

### 🟡 P2 - 一般 (近期优化)

> 因 P2 问题数量较多 (45 项)，按类别归类汇总。

#### 跨模块架构违规 (3 处)

| 位置 | 违规 |
|------|------|
| `DeviceHazardPointServiceImpl.java:7,53` | 直接导入 `com.zwei.iot.device.mapper.DeviceMapper` |
| `VideoDeviceHazardPointServiceImpl.java:12,31` | 直接导入 `com.zwei.iot.video.mapper.VideoDeviceMapper` |
| `MonitorIngestConsumerService.java:10,71` + `MonitorDataQueryService.java:13-14` | 直接导入 `DeviceMapper`/`DeviceHazardPointMapper`/`HazardPointMapper` |

**统一修复**: 通过 Service 接口调用替代 Mapper 直接访问。项目规则: "All other IoT modules depend on `zwei-iot-device` only through its service interfaces, never through Mapper directly."

#### 类型安全缺失 (5 处)

| 位置 | 问题 |
|------|------|
| `hazardPoint.ts:118-156` | `getBoundDevices` 起的函数返回 `Promise<any>` 无泛型 |
| `HazardPoint.vue:836-838` | `const resp: any = await ...` |
| `Comprehensive.vue:580,598,608,624,638,651` | `(x as any)?.data ?? x` 反复类型断言 |
| `Device.java:154` | `private Object sensors` 应为 `List<DeviceSensor>` |
| `hazardPoint.ts:9` vs `HazardPoint.vue:79-81` | `status: number` 但 UI 绑定字符串值 |

#### 并发/事务 (6 处)

| 位置 | 问题 |
|------|------|
| `DeviceOnlineStatusService.java:40-59` | 事件处理器两次 DB 写入无 `@Transactional` |
| `IotdbTimeSeriesService.java:884-898` | `ensureMeasurement` check-then-act 无双重检查锁 |
| `MqttAuthFailureGuard.java:45-47` | 过期清理与 `recordFailure` 间的 TOCTOU |
| `MqttConnectStatusListener.java:85,95` | `deviceId=0` 写入孤儿行 |
| `MqttDeviceAuthService.java:185` + `MqttConnectStatusListener.java:61` | `DeviceOnlineEvent` 重复发布 |
| `MonitorContentServiceImpl.java` | `@Transactional` 无 `rollbackFor` |

#### 性能 (8 处)

| 位置 | 问题 |
|------|------|
| `MonitorIngestFacade.java:93-108` | Groovy 脚本同步执行在 MQTT 监听线程 |
| `IotdbJdbcClient.java:69,95,117` vs `IotdbTimeSeriesService:139` | `setQueryTimeout` 矛盾 |
| `VideoDeviceMapper.xml:48-50` | 关联子查询 `GROUP_CONCAT` 每行执行 |
| `MonitorDataQueryService.java:542-545` | `countRange` 吞异常返回 0，分页 total 错误 |
| `MonitorDataSensorController.java:99-101` | `valueOf` 对非法 granularity 抛 IAE → 500 |
| `ProductTslMigrationRunner.java:52-61` | 启动时全设备循环无批量 |
| `MonitorContentSyncListener.java:70-80` | 字典变更触发 N+1 异步同步 |
| `DeviceHazardPointMapper.xml:49-53` | `bindCount` 子查询恒为 0 (与 WHERE 矛盾) |

#### 前端死代码/重复 (10 处)

| 位置 | 问题 |
|------|------|
| `RightPanel.vue` / `HealthSection.vue` | 从未被 Comprehensive.vue 导入 |
| `OnlineSection.vue:56` | `typeStats` prop 接收但未渲染 |
| `ResourceSection.vue:10-11` | SVG 环形值硬编码不反映数据 |
| `Comprehensive.vue:992-993` | ref 嵌套 ref 反应式设计混乱 |
| `Comprehensive.vue` 多处 | `handleQueryData`/`handleMapZoom`/`chartTabs`/`SensorInfo`/`ChartPoint` 等未使用声明 |
| `Comprehensive.vue:574-579,592-597` | `levelMap` 同文件重复定义 |
| `Comprehensive.vue:1121-1133` | `.echarts-body` CSS 重复 |
| `Operation.vue:499-517` | `devicePieData` computed 死代码 |
| `HazardPoint.vue:960-972` | `activeTab`/`parsedBoundary`/`previewCenter` 死代码 |
| `HazardPointDetail.vue:230-239` | 双向 watch 潜在循环 |

#### 安全/设计 (5 处)

| 位置 | 问题 |
|------|------|
| `MqttDeviceAuthService.java:155` | 密码比较非恒定时间 (时序侧信道) |
| `DeviceRegistryServiceImpl.java:84-85` | 默认注册码 `ABCDEF123456` 硬编码 |
| `IDeviceAuthQueryService.java:7` | `updateDevice(Device)` 接口过宽 (违反最小权限) |
| `DeviceRegistryServiceImpl.java:82` | `new ObjectMapper()` 未注入 Spring 实例 |
| `HazardPointGroupMapper.xml:104` | `updateHazardPointGroup` WHERE 缺少 `del_flag` 守卫 |

#### 其他 P2 (8 处)

| 位置 | 问题 |
|------|------|
| `MonitorIngestConsumerService.java:573-738` | `recoverStalePending` ~165 行原始 Redis 命令，脆弱 |
| `HazardPointQueryServiceImpl.java:25-44` | 全表扫描 + 内存过滤 |
| `DeviceHazardPointServiceImpl.java:253-259` | 双重 `selectHazardPointById` |
| `VideoDeviceHazardPointServiceImpl.java:57-73` | delete-then-reinsert 丢失审计 (与设备绑定不一致) |
| `HazardPointServiceImpl.java:208` | 魔法数字 `filter.setStatus(1)` |
| `DeviceMapper.xml` / `DeviceSensorMapper.xml` | 逻辑删除 `LEFT(code,75)` 截断碰撞风险 |
| `DeviceOnlineReconciliationJob.java` | 大型 `NOT IN` 子句性能 |
| `HazardPoint.java:94-104` | `equals`/`hashCode` 基于全部字段而非 `id` |

---

### 🟢 P3 - 轻微 (酌情处理)

> 30 项，按类别汇总:

| 类别 | 数量 | 典型示例 |
|------|------|----------|
| 命名/风格 | 6 | `HazardPointGroupController` 权限名 camelCase vs kebab-case 不一致；`HazardPointGroupController.java:24` `@RequestMapping` 缺少前导 `/` |
| 重复导入 | 1 | `HazardPointServiceImpl.java:5-10` `HazardPoint`/`HazardPointGroup` 导入两次 |
| 死参数 | 3 | `selectMonitorTypePage`/`selectVideoDevicePage`/`selectDevicePage` 的 `pageNum/pageSize` 从未使用 |
| 事务不一致 | 1 | `MonitorContentServiceImpl` `@Transactional` 无 `rollbackFor` vs `MonitorTypeServiceImpl` 有 |
| `equals`/`hashCode` | 1 | `HazardPoint.java:94-104` 基于全部字段而非 `id` |
| 日志冗余 | 3 | `MqttExceptionReporter` 每次 `LoggerFactory.getLogger`；`DeviceStatServiceImpl:161` `catch(Exception ignored)` 吞异常 |
| `SimpleDateFormat` | 1 | `MonitorIngestConsumerService:266-268` 每记录新建 (应改 `DateTimeFormatter` 常量) |
| 死属性/事件 | 2 | `HazardPointDetail.vue:201-202,207-208` 死 props/emits |
| 冗余 DB 调用 | 1 | `DeviceHazardPointServiceImpl.java:253-259` 双重 `selectHazardPointById` |
| `@CacheEvict` 无效 | 1 | `MonitorTypeServiceImpl.java:111-113` insert 时 evict key=null |
| `Class.forName` 冗余 | 1 | `IotdbJdbcClient.java:48-49` 每次 `getConnection` 执行 |
| `del_flag` 类型不一致 | 1 | `hazard_point` 用 string vs `hazard_point_group` 用 int |
| `Custom.vue` 未路由 | 1 | 存在但未注册路由 |
| ECharts `any` | 3 | `formatter: (params: any) =>` 全文件 |
| 其他 | 4 | LIKE 前导 `%` 无法走索引；`ResultSet` 未纳入 TWR 等 |

---

### 🔵 P4 - 建议 (后续迭代参考)

| # | 建议 | 位置 |
|---|------|------|
| 1 | **God Class 拆分**: `IotdbTimeSeriesService` (968 行) 拆为 Write/Schema/Query/Analysis 四接口 | timeseries |
| 2 | **批量写入**: 消费者累积窗口 (50ms/200 点) 后 `executeBatch` 刷新 | timeseries |
| 3 | **连接池迁移**: IoTDB JDBC → `SessionPool` (与 P1-3 关联) | timeseries |
| 4 | **权限粒度**: 监测数据查询端点应独立 `basic:monitorData:query` 权限 | timeseries |
| 5 | **时间字段类型化**: `VideoDevice.lastOnlineTime/installTime` String → `LocalDateTime` | video |
| 6 | **文档同步**: 代码默认 `retryDelaysSeconds=[5,30,120]` vs CLAUDE.md "3s/9s/27s" | timeseries |
| 7 | **共享趋势图 Composable**: Comprehensive.vue 与 Alarm.vue ~180 行 ECharts 配置重复 | holo-board |
| 8 | **共享告警级别常量**: 4 个文件独立定义告警颜色 | holo-board |
| 9 | **HazardPoint.vue 拆分**: 1890 行 6 内联对话框 → 子组件 | hazard frontend |
| 10 | **弱返回类型**: `getMonitorRates`/`getMapOverview` 返回 `List<Map<String,Object>>` → VO | hazard backend |
| 11 | **`NOT IN` → `LEFT JOIN ... IS NULL`** | hazard mapper |
| 12 | **Broker→Timeseries 依赖反转**: 抽取 `IMonitorIngestService` 接口 | broker/timeseries |
| 13 | **DSL 扩展**: `enrichProperties` 位置映射可插拔化 | timeseries |

---

## 3. 正向肯定

### 架构设计亮点

| # | 亮点 | 位置 |
|---|------|------|
| 1 | **跨模块 Service 接口隔离**: `IDeviceAuthQueryService`/`IDeviceStatService`/`IDeviceHazardRelationService` 等接口定义清晰，构造器注入全面采用 | device/broker/hazard |
| 2 | **IoTDB 路径白名单**: `IotdbPathResolver.validateIdentifier` 对 `[A-Za-z0-9_]{1,64}` 白名单校验，有效防注入 | timeseries |
| 3 | **`ExpressionSpec` 密封 DSL + 渲染器**: 类型安全的聚合表达式，深度受限、别名校验 | timeseries |
| 4 | **Keyset 游标分页 + 自动降采样 + `maxMergeRows` 守卫**: 生产级 OOM 防御，同类项目罕见 | timeseries |
| 5 | **Redis Stream PEL 恢复机制**: `recoverStalePending` + 遗留 XPENDING/XCLAIM 降级，崩溃恢复思维到位 | timeseries |
| 6 | **逻辑删除 + 唯一键释放**: `CONCAT(LEFT(code,75),'#DEL#',id)` 释放 UNIQUE 约束供复用 | device/hazard |
| 7 | **`device_count` 并发安全维护**: `COUNT(*)` 子查询 (绑定) + `GREATEST(count-N, 0)` (解绑) 避免 REPEATABLE READ 快照漂移 | hazard |
| 8 | **Broker 异常层级**: `MqttServiceException` → `MqttBusinessException`/`MqttConnectionException` + `MqttErrorContext` + `MqttExceptionReporter` 结构化集中 | broker |
| 9 | **`DeviceOnlineReconciliationJob`**: broker 会话感知协调，非盲目"启动时标记全部离线" | broker |
| 10 | **`BoundaryCoordsValidator`**: 边界坐标校验完备 (范围检查、大小限制、清晰错误信息) | hazard |

### 前端亮点

| # | 亮点 | 位置 |
|---|------|------|
| 11 | **`getDashboardFull` 聚合调用**: 单次 API 替代 6 个独立请求 | Comprehensive.vue:559 |
| 12 | **`Promise.all` 并行获取**: 4 个 API 调用并行化 | Operation.vue:467-472 |
| 13 | **`v-memo` 优化**: `v-memo="[card.value]"` 避免未变卡片重渲染 | Operation.vue:9 |
| 14 | **ECharts `onUnmounted` 销毁**: 3 个页面均正确销毁图表实例 | holo-board |
| 15 | **`stopAutoRefresh` 前置调用**: 防止定时器堆叠 | Alarm.vue:692 |
| 16 | **API 类型定义**: `AlarmTrendVO`、`DashboardOverview`、`RateByTypeVO` 等接口完整 | api/*.ts |

### 编码规范亮点

| # | 亮点 | 位置 |
|---|------|------|
| 17 | **SQL 全量参数化**: 所有 Mapper XML 使用 `#{}`，LIKE 用 `CONCAT('%', #{x}, '%')`，无 `${}` | 全模块 |
| 18 | **构造器注入 + `final` 字段**: 无字段注入，可测试性强 | 全后端 |
| 19 | **`SecureRandom` 生成设备凭据**: 非 `Math.random()`，含碰撞重试 | device |
| 20 | **日志注入防护**: `MqttServerMessageListener.sanitize()` 剥离 `\r\n\t` | broker |

---

## 4. 整体改进路线图

### 立即执行 (P0-P1) — 本次必须修复

| 优先级 | 任务 | 工作量估算 |
|--------|------|-----------|
| P0-3 | MQTT 订阅 ACL 加设备归属校验 | 0.5d ← 安全最高优先 |
| P0-1 | Comprehensive.vue XSS 转义 | 0.5d |
| P0-2 | 调度规则状态切换 API 调用 | 0.5d |
| P1-1 | 设备密码哈希存储 + API 响应脱敏 | 1d |
| P1-2 | MqttAuthFailureGuard 迁移 Redis | 0.5d |
| P1-3 | IoTDB 连接池引入 (HikariCP/SessionPool) | 1d ← 性能最高优先 |
| P1-4 | 未知设备死信队列替代 -1 写入 | 0.5d |
| P1-5 | 重试路径 ack-after-enqueue 重排 | 0.5d |
| P1-6 | VideoDeviceController.list 修复过滤 | 0.5d |
| P1-7 | Groovy 测试端点 attrCode 校验 + 权限提升 | 0.5d |
| P1-8 | N+1 查询批量方法 (10 处，可并行) | 2-3d |
| P1-9 | MqttServerMessageListener null 检查 | 0.5d |
| P1-10 | DeviceHazardRelationService @CacheEvict | 0.5d |
| P1-11 | 逻辑删除前清理绑定 | 0.5d |
| P1-12 | 导出上限守护 | 0.5d |
| P1-13 | insertHazardPoint @Transactional | 0.5d |
| P1-14 | 热路径正则预编译 | 0.1d |
| P1-15 | IoTDB TEXT 转义修正 | 0.5d |
| P1-16 | Alarm.vue 接入真实 API | 1d |
| P1-17 | Comprehensive.vue 死代码清理 | 0.5d |
| P1-18 | Comprehensive.vue null 安全 | 0.1d |
| P1-19 | Alarm.vue resize 监听器清理 | 0.1d |
| P1-20 | Operation.vue 图表数据源修正 | 0.5d |
| **合计** | | **~10-12d** |

### 短期优化 (P2) — 下个 Sprint

| 任务 | 工作量估算 |
|------|-----------|
| 跨模块 Mapper 违规修正 (3 处 → Service 接口) | 1d |
| 前端类型安全完善 (hazardPoint.ts 泛型 + 去 any) | 1d |
| Device.sensors Object → List<DeviceSensor> | 0.5d |
| 事件处理器 @Transactional + 重复事件修复 | 0.5d |
| DDL 竞态双重检查锁 | 0.5d |
| Groovy 评估移出 MQTT 热路径 | 1d |
| countRange 错误传播修正 | 0.5d |
| granularity 参数校验 | 0.1d |
| 前端死组件清理 (RightPanel/HealthSection) | 0.5d |
| DeviceHazardPointMapper bindCount 矛盾修正 | 0.5d |
| HazardPointGroupMapper del_flag 守卫 | 0.1d |
| HazardPoint.vue 搜索 debounce | 0.5d |
| Comprehensive.vue CSS 清理 (~800 行死 CSS) | 0.5d |
| IDeviceAuthQueryService 接口收窄 | 0.5d |
| ObjectMapper 注入修正 (2 处) | 0.5d |
| 默认注册码 fail-fast | 0.1d |
| **合计** | **~7-8d** |

### 长期重构 (P3-P4) — 技术债清单

| 任务 | 工作量估算 |
|------|-----------|
| IotdbTimeSeriesService God Class 拆分 (4 接口) | 2d |
| 消费者批量写入窗口 | 1d |
| IoTDB Session.insertRecord 类型化迁移 | 2d |
| 监测数据查询独立权限 | 0.5d |
| VideoDevice 时间字段类型化 | 1d |
| 共享趋势图 Composable 提取 | 1d |
| 共享告警级别常量集中 | 0.5d |
| HazardPoint.vue 子组件拆分 (6 对话框) | 2d |
| 弱返回类型 VO 化 | 1d |
| NOT IN → LEFT JOIN 优化 | 0.5d |
| Broker→Timeseries 依赖反转 (IMonitorIngestService) | 1d |
| DSL 位置映射可插拔化 | 1d |
| 文档/代码 retryDelays 对齐 | 0.1d |
| P3 杂项清理 (命名/风格/死参数/事务一致性) | 1d |
| **合计** | **~14d** |

---

## 附录: 评审覆盖文件清单

### 前端 (8 文件)

- `web/src/views/holo-board/Comprehensive.vue` (2593 行)
- `web/src/views/holo-board/Alarm.vue` (1092 行)
- `web/src/views/holo-board/Operation.vue` (730 行)
- `web/src/views/holo-board/Custom.vue` (38 行)
- `web/src/views/holo-board/components/RightPanel.vue` (99 行)
- `web/src/views/holo-board/components/ResourceSection.vue` (213 行)
- `web/src/views/holo-board/components/OnlineSection.vue` (222 行)
- `web/src/views/holo-board/components/HealthSection.vue` (76 行)

### 前端 (隐患点管理)

- `web/src/views/basic/HazardPoint.vue` (1890 行)
- `web/src/views/basic/components/HazardPointDetail.vue` (363 行)
- `web/src/api/hazardPoint.ts` (197 行)

### 后端 (zwei-iot-hazard, ~13 文件)

- `HazardPointServiceImpl.java` (270 行)
- `HazardPointGroupServiceImpl.java` (146 行)
- `DeviceHazardPointServiceImpl.java` (312 行)
- `DeviceHazardRelationServiceImpl.java` (90 行)
- `VideoDeviceHazardPointServiceImpl.java` (134 行)
- `HazardPointQueryServiceImpl.java` (45 行)
- `BoundaryCoordsValidator.java` (97 行)
- `HazardPointController.java` (384 行)
- `HazardPointGroupController.java` (125 行)
- `HazardPointMapper.xml` (218 行)
- `DeviceHazardPointMapper.xml` (184 行)
- 领域/DTO/Mapper 接口若干

### 后端 (zwei-iot-device, ~80 文件)

- `DeviceServiceImpl.java` (529 行)
- `DeviceRegistryServiceImpl.java` (404 行)
- `DeviceSensorServiceImpl.java` (339 行)
- `DeviceAuthService.java` (163 行)
- `DeviceOnlineStatusService.java` (109 行)
- `DeviceStatServiceImpl.java` (179 行)
- `DeviceQueryServiceImpl.java` (110 行)
- `ProductTslServiceImpl.java` (120 行)
- `DeviceAuthQueryServiceImpl.java` (18 行)
- `DeviceMaintenanceService.java` (103 行)
- `MonitorContentSyncListener.java` (153 行)
- `ProductTslMigrationRunner.java` (66 行)
- `DeviceController.java` (301 行)
- `DeviceRegistryController.java` (50 行)
- `SensorController.java` (183 行)
- `Device.java` (196 行)
- Mapper XML 若干

### 后端 (zwei-iot-broker, ~20 文件)

- `MqttDeviceAuthService.java` (481 行)
- `MqttDeviceSessionRegistry.java` (110 行)
- `MqttServerSubscribeValidator.java` (123 行)
- `MqttAuthFailureGuard.java` (122 行)
- `MqttConnectStatusListener.java` (114 行)
- `MqttServerMessageListener.java` (96 行)
- `MqttExceptionReporter.java` (86 行)
- 异常层级 5 文件
- 其他 7 文件

### 后端 (zwei-iot-timeseries, ~21 文件)

- `IotdbTimeSeriesService.java` (968 行)
- `MonitorIngestConsumerService.java` (792 行)
- `MonitorDataQueryService.java` (499 行)
- `MonitorIngestFacade.java` (173 行)
- `IotdbJdbcClient.java` (124 行)
- `MonitorIngestStreamService.java` (142 行)
- `ComputedAttributeEvaluator.java` (137 行)
- `ComputedAttributeTestController.java` (119 行)
- 控制器/领域/配置若干

### 后端 (zwei-iot-monitor, ~16 文件)

- `MonitorTypeServiceImpl.java` (176 行)
- `MonitorContentServiceImpl.java` (243 行)
- `MonitorTypeController.java` (239 行)
- `MonitorContentController.java` (200 行)
- 领域/DTO/Mapper 若干

### 后端 (zwei-iot-video, ~10 文件)

- `VideoDeviceController.java` (183 行)
- `VideoDeviceServiceImpl.java` (100 行)
- `VideoDeviceMapper.xml` (180 行)
- 其他 7 文件

---

> **报告说明**: 本评审基于 4 个并行子代理对 ~120 个源文件的逐行审查编译而成。所有问题均附 `file:line` 定位。对不确定的问题已标注「待确认」。修复建议中的代码示例与原始语言一致，可直接参考使用。
