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
  - **唯一例外**：`device_hazard_point` / `video_device_hazard_point` 两表保留 FK（核心绑定）
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

---

## 九、第三方库约定（本节增量补扫追加）

### 9.1 MyBatis 使用约定

- **XML Mapper 位置**：`src/main/resources/mapper/{module}/{Entity}Mapper.xml`（与 Java 接口同包同名）
- **Mapper 接口**：放置在 `com.zwei.{module}.{sub}.mapper` 包下，与 XML 配套
- **动态 SQL 优先**用 `<where>` / `<if>` / `<foreach>`，避免在 Java 代码中拼接 SQL 字符串
- **分页**：使用 `PageHelper.startPage(pageNum, pageSize)`，Controller 层通过 `TableDataInfo` 统一返回
- **返回集合**：`SELECT` 查询统一返回 `List<T>`，单条用 `T selectById(Long id)` 返回 `T` 或 `null`
- **批量操作**：用 `<foreach collection="list" item="x" separator=";">` 批量 INSERT/UPDATE，避免 N+1
- **逻辑删除**：所有 SELECT 必须带 `del_flag = 0` 条件（除 `selectDeleted` 显式方法）
- **乐观锁**：`@Version` 字段 + `version` 列（仅高并发业务表，如 `alarm_criteria`）

### 9.2 FastJSON2 序列化约定

- 统一使用 `com.alibaba.fastjson2.JSON` / `JSONObject` / `JSONArray`（**不混用 fastjson 1.x**）
- 反序列化：`JSON.parseObject(text, Type.class)` 或 `JSON.parseArray(text, Item.class)`
- 序列化：`JSON.toJSONString(obj)` 默认输出紧凑格式
- 字段为 null：默认输出 `"field":null`；如需忽略 null，用 `JSON.toJSONString(obj, JSONWriter.Feature.WriteMapNullValue)` 控制
- 大文本字段（如 `notice_content` longblob）：在 Domain 上使用 `transient` 或自定义 `PropertyFilter` 排除
- **日期格式**：默认输出毫秒时间戳；如需 `yyyy-MM-dd HH:mm:ss`，使用
  `JSON.toJSONString(obj, JSONWriter.Feature.WriteDateUseDateFormat)`

### 9.3 Spring 注解使用约定

- **依赖注入**：**统一构造器注入**（`@Autowired` 可省略，4.0+ 默认支持），禁止 `@Resource` 字段注入
- **可选依赖**：`ObjectProvider<T>` 注入，调用 `getIfAvailable()`，避免 `NoSuchBeanDefinitionException`
- **事务**：`@Transactional(rollbackFor = Exception.class)` 显式指定回滚异常；只读方法加 `readOnly = true`
- **缓存**：`@Cacheable(value="...", key="#id")` / `@CacheEvict(value="...", key="#id")` / `@Caching` 多操作
- **异步**：本项目目前**未启用 `@Async`**（所有异步通过 `ExecutorService` 手动管理）；新增异步必须先讨论

---

## 十、异步/线程池使用约定（追加）

### 10.1 当前已用线程池

| 模块                    | 线程名                       | 类型         | 用途                      |
|-----------------------|---------------------------|------------|-------------------------|
| `zwei-iot-timeseries` | `monitor-ingest-consumer` | 单线程 daemon | Redis Stream 消费         |
| `zwei-iot-alarm`      | `groovy-eval`             | 单线程 daemon | Groovy 脚本执行             |
| `zwei-iot-alarm`      | alarm-CRON 调度             | Quartz 线程池 | `ComprehensiveAlarmJob` |

### 10.2 创建线程池

```java
// ✅ 推荐：显式 ThreadFactory + Daemon
this.executorService = Executors.newSingleThreadExecutor(r -> {
    Thread thread = new Thread(r, "{业务名}-{角色}");
    thread.setDaemon(true);  // 不阻塞 JVM 退出
    return thread;
});
```

```java
// ❌ 禁止：裸 Executors.newFixedThreadPool()（无界队列风险）
// ❌ 禁止：Thread.start() 显式启动未命名线程
```

### 10.3 优雅停机

```java
@PreDestroy
public void stop() throws InterruptedException {
    running = false;                // 1. 设置 volatile 标志
    executorService.shutdownNow();   // 2. 中断任务
    executorService.awaitTermination(5, TimeUnit.SECONDS);  // 3. 等待兜底
}
```

### 10.4 Spring `@EnableScheduling` / `@Scheduled`

- 轻量定时任务可使用 `@Scheduled(cron = "...")`，由 Spring 调度
- 复杂任务（需监控/可暂停）使用 `zwei-quartz` 模块的 Quartz Job
- 定时清理等数据级任务统一通过 `sys_job` 表配置（参考 `sys_config.log.cleanup.cron`）

---

## 十一、Redis 使用约定（追加）

### 11.1 Key 命名规范

```
{业务域}:{实体}:{主键/类型}:[{子键1}:{子键2}:...]
```

**已使用 Key 模式清单**：

| Key 模式                                                                       | 用途           | TTL                    | 序列化             |
|------------------------------------------------------------------------------|--------------|------------------------|-----------------|
| `login:token:{token}`                                                        | JWT token 缓存 | 30 分钟                  | String          |
| `sys_user_online:{token}`                                                    | 在线用户         | 同 session              | JSON            |
| `alarm:pre-trigger:{cid}:{hpId}:{level}`                                     | 告警预触发计数      | `preTriggerTtlSeconds` | String (Long)   |
| `alarm:last-trigger:{cid}:{hpId}`                                            | 告警最近触发时间     | 同上                     | String (millis) |
| `alarm:criteria:enabled`                                                     | 启用判据缓存       | 5 分钟                   | JSON (List)     |
| `stream:monitor:ingest`                                                      | 监测数据缓冲       | 永久 (Stream trim)       | Stream          |
| `{dedupeKeyPrefix}{deviceId}:{sensorNo}:{attrCode}:{dataTime}:{payloadHash}` | 幂等去重         | `dedupeTtlSeconds`     | String "1"      |
| `cache:hazardPoint::{id}` (Spring Cache)                                     | 隐患点缓存        | 5 分钟                   | JSON            |

### 11.2 序列化器

- 默认 `RedisTemplate<Object, Object>` + `StringRedisSerializer` / `JdkSerializationRedisSerializer`
- 复杂对象显式使用 `JSON.toJSONString()` / `JSON.parseObject()` 转换
- **禁止** 存储 `byte[]` / `InputStream` / 非 Serializable POJO 直接到 Redis

### 11.3 过期时间

- **必须**显式设置 TTL（避免内存泄漏）
- TTL 单位：业务配置项用 `Duration` 类型注入（`Duration.ofSeconds(60)`），配置键统一 `-ttl-seconds` / `-ttl-minutes` 后缀
- 业务常量直接用 `Duration.ofMinutes(5)` 等

### 11.4 分布式锁（待评估）

当前项目**未使用** `Redisson` / `SETNX` 锁。如需新增：

- 优先用 `RedisTemplate.opsForValue().setIfAbsent(key, val, Duration)`
- 锁粒度按主键 ID，不锁整表

---

## 十二、Controller 响应规范（追加）

### 12.1 统一返回结构

**所有** Controller 方法（除 SSE/文件下载）必须返回 `AjaxResult`：

```java
@GetMapping("/list")
public AjaxResult list(SysUser user) {
    return AjaxResult.success(service.selectUserList(user));
}

@PostMapping
public AjaxResult add(@RequestBody SysUser user) {
    return toAjax(service.insertUser(user));  // 自动转 success/error
}
```

### 12.2 分页返回

```java
@GetMapping("/page")
public TableDataInfo page(SysUser user, int pageNum, int pageSize) {
    startPage();  // PageHelper
    List<SysUser> list = service.selectUserList(user);
    return getDataTable(list);  // 自动包 { total, rows, pageNum, pageSize }
}
```

### 12.3 错误抛出

- 业务校验失败：`throw new ServiceException("用户不存在")`，由 `GlobalExceptionHandler` 统一拦截
- 鉴权失败：使用 `@PreAuthorize("@ss.hasPermi('system:user:list')")` 由 Spring Security 拦截
- 限流：使用 `@RateLimiter` 注解（AOP 切面）

### 12.4 参数校验

- DTO 入参用 `@Valid` + JSR-303 注解（`@NotBlank` / `@NotNull` / `@Size`）
- 路径变量用 `@PathVariable` + 自定义校验
- Controller 方法**不**做业务校验，只做参数格式校验；业务校验在 Service 层

---

## 变更记录

| 时间               | 变更                                                                                                 |
|------------------|----------------------------------------------------------------------------------------------------|
| 2026-05-08       | 初版 (8 个章节: 注释/JavaDoc/模块边界/错误处理/日志/数据库/命名/测试)                                                      |
| 2026-06-10 19:08 | 增量补扫追加 4 节: 九 第三方库约定 (MyBatis/FastJSON2/Spring 注解) / 十 异步与线程池 / 十一 Redis 使用约定 / 十二 Controller 响应规范 |
