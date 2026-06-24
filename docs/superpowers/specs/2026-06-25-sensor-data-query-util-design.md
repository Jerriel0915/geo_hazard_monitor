# 传感器数据查询工具类 设计

> 日期: 2026-06-25
> 模块: `zwei-iot-timeseries`
> 状态: 已批准

## 1. 目标

封装一个静态工具类，供非 Spring 管理的调用方（如 Groovy 告警脚本、域对象）查询传感器在某时刻的数据快照。

## 2. API 设计

### 方法签名

```java
public static SensorSnapshot query(Long deviceId, String sensorCode, long time, String attrCode)
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| deviceId | Long | 是 | 设备 ID（对应 IoTDB 路径 `d{deviceId}`） |
| sensorCode | String | 是 | 传感器编码（对应 IoTDB 路径 `s{sensorCode}`） |
| time | long | 是 | 查询时刻，毫秒时间戳 |
| attrCode | String | 否 | 属性编码；为 `null` 时查询全部业务属性 |

### 行为语义

- 返回 `time <= 传入时刻` 的**最近一条**数据快照（IoTDB `ORDER BY time DESC LIMIT 1`）
- `attrCode` 非空：只查该属性，`values` 含单个 entry
- `attrCode` 为空：`SELECT *` 查传感器下全部列，`values` 包含所有业务属性（排除 `quality` 列）
- 该时刻无数据：返回 `null`

### 返回结构

```java
@Getter @AllArgsConstructor
public class SensorSnapshot {
    /** 数据时间（<= 查询时刻的最近一条），毫秒时间戳 */
    private final long time;
    /** attrCode → value；查全部属性时排除 quality 列，只含非 null 值 */
    private final Map<String, Double> values;
}
```

JSON 序列化效果：

```json
{
  "time": 1718294400000,
  "values": {
    "rainfall_hour": 12.5,
    "temperature": 25.3
  }
}
```

## 3. 实现要点

### 文件清单（新建 2 个，均在 `zwei-iot-timeseries`）

| 文件 | 包 | 职责 |
|---|---|---|
| `SensorSnapshot.java` | `com.zwei.iot.timeseries.domain` | 数据快照值对象 |
| `SensorDataQueryUtil.java` | `com.zwei.iot.timeseries.util` | 静态查询工具类 |

### 依赖获取

通过 `SpringUtils.getBean()` 静态获取（项目已有此模式）：

- `IotdbJdbcClient` — 获取 JDBC 连接
- `IotdbPathResolver` — 构造 IoTDB 路径

### SQL 构造

路径：`pathResolver.buildSensorPath(deviceId, sensorCode)` → `root.{db}.d{deviceId}.s{sensorCode}`

- `attrCode` 非空：
  ```sql
  SELECT {attrCode} FROM {path} WHERE time <= {T} ORDER BY time DESC LIMIT 1
  ```
- `attrCode` 为空：
  ```sql
  SELECT * FROM {path} WHERE time <= {T} ORDER BY time DESC LIMIT 1
  ```

### 结果解析

- `time` = `rs.getLong("Time")`（IoTDB 时间列固定名）
- 列名通过 `ResultSetMetaData` 遍历：IoTDB JDBC 列名为完整路径（如 `root.geo_hazard.d1.s1.rainfall_hour`），**截取最后一个 `.` 之后**作为 attrCode
- 排除列名为 `quality` 的列
- 仅放入非 null 的值（`rs.getObject(col)` 为 null 时跳过）

### 不做的事

- **不调用 `ensureMeasurement`**：查询不应产生建表副作用。measurement 不存在时 IoTDB 返回 null 列，自然被过滤。
- **不查 `sensor_attribute` 表**：直接用 IoTDB 返回列名作 attrCode，避免额外 DB 查询。

## 4. 边界处理

| 场景 | 行为 |
|---|---|
| `deviceId` 或 `sensorCode` 为空 | 抛 `IllegalArgumentException` |
| `attrCode` 非法（非 `^[A-Za-z0-9_]+$`） | 抛 `IllegalArgumentException`（防 SQL 注入） |
| 无数据（`rs.next() == false`） | 返回 `null` |
| 某 measurement 不存在 | 该属性不入 map（不报错） |
| IoTDB 未启用 | `ServiceException` 向上传播（由 `IotdbJdbcClient.getConnection()` 抛出） |
| `attrCode` 为空字符串 | 视为 `null`，查全部属性 |

## 5. 测试计划

单元测试（`src/test/java/.../SensorDataQueryUtilTest.java`），mock `IotdbJdbcClient` + `IotdbPathResolver`：

1. 单属性查询 — 验证 SQL 含 `WHERE time <= T ORDER BY time DESC LIMIT 1`，结果含单个 entry
2. 全属性查询 — 验证 SQL 为 `SELECT *`，结果含多个 entry 且排除 quality
3. 列名截取 — mock ResultSet 列名为完整路径，验证 map key 为末段 attrCode
4. 无数据 — `rs.next() == false`，返回 `null`
5. 非法 attrCode（如 `a;b`、`a b`、空格） — 抛 `IllegalArgumentException`
6. deviceId/sensorCode 为 null — 抛 `IllegalArgumentException`
7. null 值过滤 — 某列值为 null，不进 map

## 6. 使用示例

```java
// 查询设备 101 传感器 WY_1 在某时刻的全部属性
SensorSnapshot snap = SensorDataQueryUtil.query(101L, "WY_1", 1718294400000L, null);
if (snap != null) {
    long t = snap.getTime();
    Double rain = snap.getValues().get("rainfall_hour");
}

// 只查单个属性
SensorSnapshot r = SensorDataQueryUtil.query(101L, "WY_1", 1718294400000L, "temperature");
```
