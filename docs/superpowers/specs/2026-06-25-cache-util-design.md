# CacheUtil 工具类 设计

> 日期: 2026-06-25
> 模块: `zwei-common`
> 状态: 已批准

## 1. 目标

静态工具类二次封装 `RedisCache`（不修改 RedisCache），补充基础数据类型便捷 get 方法。供非 Spring 管理的调用方（如 Groovy 脚本包装类）使用。

## 2. 定位

- **文件**: `server/zwei-common/src/main/java/com/zwei/common/utils/CacheUtil.java`（与 `DictUtils` 同包）
- **形式**: 静态工具类 + `SpringUtils.getBean(RedisCache.class)`（与 `DictUtils` 一致）
- **不修改** `RedisCache`，仅二次封装
- 核心转换逻辑抽成包私有方法接收 `RedisCache` 参数，便于单测注入 mock

## 3. API 设计

### 通用操作（委托 RedisCache）

| 方法 | 委托目标 |
|---|---|
| `set(String key, Object value)` | `setCacheObject(key, value)` |
| `set(String key, Object value, long timeout, TimeUnit unit)` | `setCacheObject(key, value, timeout, unit)` |
| `<T> T get(String key)` | `getCacheObject(key)` |
| `boolean delete(String key)` | `deleteObject(key)` |
| `boolean hasKey(String key)` | `hasKey(key)` |
| `boolean expire(String key, long timeout)` | `expire(key, timeout)`（秒） |
| `boolean expire(String key, long timeout, TimeUnit unit)` | `expire(key, timeout, unit)` |
| `long getExpire(String key)` | `getExpire(key)` |

### 基础类型 get（7 类 × 2 重载 = 14 个）

每类提供 `(key)`→包装类型（不存在/不匹配返回 `null`）与 `(key, defaultValue)`→基本类型（不存在/不匹配返回 `defaultValue`）：

`getInt` / `getLong` / `getDouble` / `getFloat` / `getBigDecimal` / `getString` / `getBoolean`

## 4. 类型转换规则（宽松）

先 `getCacheObject(key)` 取 Object，再按目标类型转换：

| 目标类型 | 来源为 Number | 来源为 String | 其他 |
|---|---|---|---|
| 数值类（int/long/double/float/BigDecimal） | `xxxValue()` / `BigDecimal.valueOf()` | `parseXxx`，失败 null | null |
| `getString` | **仅返回 String 类型值**，Number 不 toString（防误导） | 原值 | null |
| `getBoolean` | 仅 Boolean 直接返回 | `Boolean.parseBoolean` | null |

- key 不存在（`getCacheObject` 返回 null）：包装类型返回 `null`，基本类型重载返回 `defaultValue`
- 类型不匹配：同上（宽松，不抛异常）

## 5. 边界处理

| 场景 | 行为 |
|---|---|
| key 不存在 | 包装类型返回 null / 基本类型返回 defaultValue |
| 类型不匹配 | 同上（宽松） |
| RedisCache bean 未就绪 | `SpringUtils.getBean` 抛异常向上传播 |

## 6. 可测试性

- 公开静态方法：`getBean(RedisCache.class)` + 委托包私有方法
- 包私有核心方法：接收 `RedisCache` 参数，单测直接注入 mock RedisCache
- 类型转换辅助（`toInt`/`toLong`/`toBigDecimal`/`toBoolean` 等）抽成私有静态方法，单独可测

## 7. 测试计划

单元测试（`zwei-common/src/test/java/.../CacheUtilTest.java`），mock `RedisCache`：

1. 通用 set/get/delete 委托 — verify RedisCache 对应方法被调
2. getInt — Number 来源（intValue）、String 来源（parse）、不匹配（null）、默认值重载
3. getLong/getDouble/getFloat/getBigDecimal — 同上模式
4. getString — 仅 String 类型返回，Number 返回 null
5. getBoolean — Boolean 直接返回，String 用 parseBoolean，其他 null
6. key 不存在（getCacheObject 返回 null）— 包装类型 null / 基本类型 defaultValue

## 8. 使用示例

```java
// 存
CacheUtil.set("my:counter", 100);
CacheUtil.set("my:token", "abc", 30, TimeUnit.MINUTES);

// 取（基础类型）
Integer v = CacheUtil.getInt("my:counter");          // 100
int v2 = CacheUtil.getInt("missing", 0);             // 0（默认值）
String s = CacheUtil.getString("my:token");          // "abc"

// 通用
Object obj = CacheUtil.get("my:token");
CacheUtil.delete("my:counter");
CacheUtil.expire("my:token", 60);
```
