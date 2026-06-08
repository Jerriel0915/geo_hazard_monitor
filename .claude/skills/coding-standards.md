# Zwei 项目编码规范

> 本地项目级 Skill，为处理本项目的所有 Agent 提供编码格式指引。
> 在 `CLAUDE.md` 的架构说明基础上，补充具体代码编写约定。

---

## 一、注释语言

**统一使用中文**编写 JavaDoc 和行内注释。日志消息、异常消息使用中文。

```java
// ✅ 正确
/**
 * MQTT 设备会话注册中心 — 维护当前所有活跃连接的内存索引。
 */
@Component
public class MqttDeviceSessionRegistry { ... }

// ❌ 错误
/**
 * MQTT device session registry.
 */
```

---

## 二、JavaDoc 格式规范

### 2.1 类级 JavaDoc

类级注释必须包含以下要素：

```java
/**
 * 类功能简述。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li><b>职责一</b>：说明</li>
 *   <li><b>职责二</b>：说明</li>
 * </ul>
 *
 * <h3>关键设计决策（可选）</h3>
 * 说明为何选择某种实现方式、关键的并发语义、缓存策略等。
 *
 * @see 相关类
 * @author 作者
 */
```

**对于记录（record）和简单 DTO**：至少包含一行功能描述和字段列表。

### 2.2 方法级 JavaDoc

- **public 方法**：必须写 JavaDoc，包含 `@param` / `@return` / `@throws`
- **private 方法**：复杂逻辑必须写，简单 getter/setter 可省略
- `@Override` 方法：优先写，至少写一句话说明**做什么**（而非"实现了接口方法"）

### 2.3 行内注释

关键业务逻辑节点使用分隔注释标注阶段：

```java
// ── 阶段1: 幂等去重 ──
if(isDuplicate(point)){...}

// ── 阶段2: IoTDB 时序写入 ──
        iotdbTimeSeriesService.

writePoints(...);
```

条件分支的**非显而易见的判断**加注释说明原因：

```java
// 注意：此处使用子查询 COUNT 而非预查+原子递增，因为 REPEATABLE READ
// 下并发 bind 无法通过快照读准确计算新增数，存在 device_count 漂移风险。
hazardPointMapper.refreshDeviceCountById(hazardPointId);
```

---

## 三、模块边界规范

### 3.1 跨模块依赖

```
zwei-iot-device  ← 定义所有跨模块 Service 接口
       ↑
zwei-iot-broker / zwei-iot-hazard / zwei-iot-timeseries / zwei-monitor
       ↑
    只能通过 Service 接口访问，禁止直接 import Mapper 或 Domain
```

- **接口定义**在 `zwei-iot-device` 中
- **接口实现**在对应模块中（如 broker/hazard/timeseries）
- **调用方**通过 `ObjectProvider<T>` 可选注入（当实现模块可能不存在时）

### 3.2 新增跨模块方法

如需跨模块调用新能力：

1. 在 `zwei-iot-device` 中新增接口方法
2. 在对应模块中实现
3. 调用方通过接口 + `ObjectProvider` 注入（遵循既有模式）

---

## 四、错误处理

### 4.1 异常体系

- **broker 模块**使用专属异常体系（`MqttServiceException` → `MqttBusinessException` / `MqttConnectionException` /
  `MqttCommunicationException` / `MqttProtocolException`）
- **其他模块**使用 `com.zwei.common.exception.ServiceException`
- 每个异常内部类必须有触发场景说明

### 4.2 不可静默吞错

```java
// ✅ 正确 — 显式抛异常
if(mc ==null){
        throw new

ServiceException("监测内容不存在或已停用: id="+attr.getMonitorContentId());
        }

// ❌ 错误 — 静默跳过
        if(mc ==null)continue;
```

---

## 五、日志规范

- 使用 Lombok `@Slf4j`
- 关键业务节点用 `log.info`（鉴权成功/失败、设备上线/离线、数据落库成功）
- 异常处理使用 `log.error`（含完整上下文：deviceId, clientId, topic 等）
- 预期内可恢复错误用 `log.warn`（Broker 不可用、服务未启用）
- 调试信息用 `log.debug`
- **不记录密码、token 等敏感信息**

---

## 六、数据库设计约定

- 全量使用**逻辑删除**（`del_flag` 列），不使用物理 DELETE
- 因此**不使用数据库外键约束**（FK 无法感知 del_flag=0 过滤，导致僵尸引用）
- 参照完整性通过应用层 Service 校验保证
- 字段注释需区分语义，避免模糊命名（如 `sensor_code` vs `sensor_no`）
- 唯一约束使用 UNIQUE KEY，删除时通过重写列值（如 `#DEL#` 后缀）释放

---

## 七、命名约定

| 类别            | 规则                   | 示例                         |
|---------------|----------------------|----------------------------|
| 跨模块接口         | `I` + 模块 + `Service` | `IDeviceSessionService`    |
| 本地 Service 实现 | `模块` + `ServiceImpl` | `DeviceSessionServiceImpl` |
| MQTT 组件       | `Mqtt` + 功能          | `MqttServerAuthHandler`    |
| Mapper        | 实体 + `Mapper`        | `DeviceSensorMapper`       |

---

## 八、测试规范

- `@DisplayName` 使用中文描述测试场景
- 按场景使用 `@Nested` 分组
- 使用 `@ParameterizedTest` + `@CsvSource` 覆盖边界值
- Mock 使用 `@Mock` + `@InjectMocks`（MockitoExtension）
- 跨模块可选依赖的测试需要覆盖 `service unavailable` 场景

```java
@Test
@DisplayName("IDeviceSessionService 不可用时不应抛异常")
void resetDeviceAuthPassword_serviceUnavailable_shouldNotThrow() { ... }
```
