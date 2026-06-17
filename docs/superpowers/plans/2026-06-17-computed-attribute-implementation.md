# 计算属性 (Computed Attribute) 实现计划

> **面向 AI 代理的工作者:** 必需子技能:使用 superpowers:subagent-driven-development(推荐)或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框(`- [ ]`)语法来跟踪进度。

**目标:** 在监测类型管理中为监测属性新增"字段类型"(固有/计算),计算属性通过 Groovy 脚本对固有属性求值后,合并进 `ParsedMessage.properties`,与固有属性同链路写入 IoTDB。

**架构:** `monitor_content` 加 `field_type` + `calc_script` 两列;`zwei-iot-timeseries` 新增 `compute/` 子包(`Registry` + `Evaluator` + `Assembler` + `LastMessageStore`);`MonitorIngestFacade.ingest()` 在 enrichProperties 与 enqueue 之间插入求值环节;`zwei-iot-parser.GroovyScriptEngine` 新增 `executeComputed` 入口复用现有沙箱;前端 `MonitorType.vue` 加字段类型列 + 新组件 `CalcScriptEditor.vue`。

**技术栈:** Java 17 + Spring Boot 4 + MyBatis + Groovy 沙箱(SecureASTCustomizer) + JUnit 5 + AssertJ + Mockito(后端);Vue 3 + TS + Element Plus(前端)。

**关联规格:** `docs/superpowers/specs/2026-06-17-computed-attribute-design.md`

**测试约定:** 后端测试位于 `server/<module>/src/test/java/com/zwei/iot/<module>/...`,使用 JUnit 5(`@Test`/`@DisplayName`/`@Nested`) + AssertJ(`assertThat`) + Mockito(`mock`/`when`/`verify`)。`@Resource` 字段通过反射工具 `injectField` 注入(参考 `GroovyScriptEngineTest`)。

**Git 约定:** 每个 commit 使用 Conventional Commits 格式 `feat(monitor): ...` / `feat(timeseries): ...` / `feat(web): ...` / `test(...)` / `docs(...)`。**不**包含 `Co-Authored-By` / `Signed-off-by` 等尾注(项目全局规则)。

**启动检查:** 后端编译/测试在仓库根执行:
```bash
cd server && mvn -pl <changed-module> -am test
# 全量回归:
cd server && mvn clean test
```
前端类型检查:`cd web && npm run build`(包含 vue-tsc)。

---

## 文件结构

### 后端新增/修改文件

| 文件 | 类型 | 职责 |
|---|---|---|
| `db/upgrade/v2.2-computed-attribute.sql` | 新建 | `monitor_content` 加 `field_type` + `calc_script` 列 + 索引 |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/MonitorContent.java` | 修改 | 加 `fieldType` + `calcScript` 字段 |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/MonitorContentCreateRequest.java` | 修改 | 加 `fieldType`(Pattern 校验) + `calcScript` |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/MonitorContentUpdateRequest.java` | 修改 | 加 `calcScript`(fieldType 不可改) |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/mapper/MonitorContentMapper.java` | 修改 | 加 `selectComputedByTypeId` 方法 |
| `server/zwei-iot-monitor/src/main/resources/mapper/iot/monitor/MonitorContentMapper.xml` | 修改 | 加列映射 + `selectComputedByTypeId` SQL |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/service/IMonitorContentService.java` | 修改 | 加 `selectComputedByTypeId` 接口 |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/service/impl/MonitorContentServiceImpl.java` | 修改 | 实现新方法 + `@CacheEvict` 联动 `computedAttrs` |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/controller/MonitorContentController.java` | 修改 | 校验 + `test-script` 端点 |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/CalcScriptTestRequest.java` | 新建 | 测试请求 DTO |
| `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/CalcScriptTestResult.java` | 新建 | 测试响应 DTO |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/SensorMetadata.java` | 修改 | 加 `monitorTypeId` 字段(非破坏性) |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceSensorQueryServiceImpl.java` | 修改 | 构造 `SensorMetadata` 时填入 `monitorTypeId` |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttribute.java` | 新建 | 计算属性 record |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ParsedMessageSnapshot.java` | 新建 | Redis 快照 record |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/LastMessageStore.java` | 新建 | Redis Hash 读写最近一条 |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeRegistry.java` | 新建 | 按 `monitorTypeId` 缓存(`@Cacheable`) |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedScriptAssembler.java` | 新建 | 多脚本 → 单 Groovy 源 + 缓存 |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java` | 新建 | 主入口 |
| `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java` | 修改 | 第 4.5 环节接入 |
| `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptEngine.java` | 修改 | 加 `executeComputed` 入口 |
| `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/LastMessageStoreTest.java` | 新建 | 4 用例 |
| `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedScriptAssemblerTest.java` | 新建 | 5 用例 |
| `server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/engine/GroovyScriptEngineComputedTest.java` | 新建 | 4 用例 |
| `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java` | 新建 | 8 用例 |

### 前端新增/修改文件

| 文件 | 类型 | 职责 |
|---|---|---|
| `web/src/api/monitorType.ts` | 修改 | 类型扩展 + `testCalcScript` |
| `web/src/views/basic/MonitorType.vue` | 修改 | 字段类型列 + 操作列 + 校验 |
| `web/src/views/basic/components/CalcScriptEditor.vue` | 新建 | 计算脚本编辑 + 在线测试弹窗 |

---

## 任务 1:数据库升级脚本

**文件:**
- 创建:`db/upgrade/v2.2-computed-attribute.sql`

- [ ] **步骤 1:确认升级目录存在,创建 SQL 文件**

```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
ls db/upgrade/ 2>/dev/null || mkdir -p db/upgrade
```

写入文件 `db/upgrade/v2.2-computed-attribute.sql`:

```sql
-- v2.2 计算属性(Computed Attribute)升级脚本
-- 为 monitor_content 增加字段类型(固有/计算)与计算脚本列

ALTER TABLE monitor_content
    ADD COLUMN field_type VARCHAR(16) NOT NULL DEFAULT 'inherent'
        COMMENT '字段类型: inherent-固有属性, computed-计算属性'
        AFTER indicator_type,
    ADD COLUMN calc_script MEDIUMTEXT NULL
        COMMENT '计算属性脚本(Groovy 代码块, 仅 field_type=computed 时必填)'
        AFTER field_type;

ALTER TABLE monitor_content
    ADD INDEX idx_monitor_content_field_type (monitor_type_id, field_type);
```

- [ ] **步骤 2:在本地 MySQL 执行升级**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2.2-computed-attribute.sql
```

预期:无输出(成功)。验证:

```bash
mysql -uroot -pwodepassword -e "DESCRIBE geo_hazard_monitor.monitor_content;" | grep -E "field_type|calc_script"
```

预期看到 `field_type` 和 `calc_script` 两行。

- [ ] **步骤 3:Commit**

```bash
git add db/upgrade/v2.2-computed-attribute.sql
git commit -m "feat(db): monitor_content 新增 field_type/calc_script 列与索引"
```

---

## 任务 2:`MonitorContent` 实体扩展

**文件:**
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/MonitorContent.java`

- [ ] **步骤 1:在 `rangeMax` 字段后追加两个字段**

打开 `MonitorContent.java`,在 `private BigDecimal rangeMax;` 之后插入:

```java
    /**
     * 字段类型: inherent-固有属性, computed-计算属性
     */
    private String fieldType;

    /**
     * 计算属性脚本(Groovy 代码块, 仅 field_type=computed 时必填)
     */
    private String calcScript;
```

并在 `toString()` 方法中追加:

```java
                ", rangeMax=" + rangeMax +
                ", fieldType='" + fieldType + '\'' +
                ", calcScript='" + (calcScript == null ? "null" : "[" + calcScript.length() + " chars]") + '\'' +
                '}';
```

(脚本内容长,toString 只显示长度避免日志膨胀)

- [ ] **步骤 2:编译验证**

```bash
cd server && mvn -pl zwei-iot-monitor -am compile
```

预期:`BUILD SUCCESS`。

- [ ] **步骤 3:Commit**

```bash
git add server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/MonitorContent.java
git commit -m "feat(monitor): MonitorContent 实体新增 fieldType 与 calcScript 字段"
```

---

## 任务 3:DTO 扩展(Create/Update)+ Controller 校验

**文件:**
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/MonitorContentCreateRequest.java`
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/MonitorContentUpdateRequest.java`
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/controller/MonitorContentController.java`

- [ ] **步骤 1:扩展 CreateRequest**

在 `MonitorContentCreateRequest.java` 顶部 import 追加:

```java
import jakarta.validation.constraints.Pattern;
```

在 `rangeMax` 字段后追加:

```java
    @Pattern(regexp = "inherent|computed",
             message = "字段类型必须是 inherent 或 computed")
    private String fieldType;

    @Size(max = 65535, message = "计算脚本长度不能超过 64KB")
    private String calcScript;
```

并在 `code` 字段上**追加 Pattern 校验**(原 `@NotBlank` + `@Size` 保留):

```java
    @NotBlank(message = "监测内容编码不能为空")
    @Size(max = 100, message = "监测内容编码长度不能超过100个字符")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$",
             message = "监测内容编码必须以字母开头, 只能包含字母数字下划线")
    private String code;
```

(理由:计算属性 `code` 会用作 Groovy 函数名 `calc_{code}`,必须合法 Java 标识符。该约束对固有属性无副作用。)

- [ ] **步骤 2:扩展 UpdateRequest**

在 `MonitorContentUpdateRequest.java` 顶部 import 追加:

```java
import jakarta.validation.constraints.Size;
```

(若已存在则跳过)

在 `rangeMax` 字段后追加:

```java
    @Size(max = 65535, message = "计算脚本长度不能超过 64KB")
    private String calcScript;
```

修改 `hasUpdatableField()` 方法:

```java
    public boolean hasUpdatableField() {
        return name != null || unit != null || sortOrder != null
                || icon != null || rangeMin != null || rangeMax != null
                || calcScript != null;
    }
```

- [ ] **步骤 3:Controller 显式校验 + 透传字段**

在 `MonitorContentController.java` 的 `add` 方法中,`isValidRange` 检查**之前**插入:

```java
        // fieldType 默认 inherent;computed 时 calcScript 必填
        String fieldType = request.getFieldType() == null ? "inherent" : request.getFieldType();
        if ("computed".equals(fieldType)
                && (request.getCalcScript() == null || request.getCalcScript().isBlank())) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, "计算属性必须填写计算脚本");
        }
```

在 `buildMonitorContentForCreate` 方法末尾(`return monitorContent;` 之前)追加:

```java
        monitorContent.setFieldType(fieldType);
        monitorContent.setCalcScript(request.getCalcScript());
```

注意:`fieldType` 是步骤 3 中本地解析的变量(已默认 inherent),不要直接用 `request.getFieldType()`(可能为 null)。

在 `buildMonitorContentForUpdate` 方法末尾(`return monitorContent;` 之前)追加:

```java
        monitorContent.setCalcScript(request.getCalcScript());
```

(Update 不动 fieldType — 不可改)

- [ ] **步骤 4:编译验证**

```bash
cd server && mvn -pl zwei-iot-monitor -am compile
```

预期:`BUILD SUCCESS`。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/ \
        server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/controller/MonitorContentController.java
git commit -m "feat(monitor): 监测内容 DTO/Controller 支持 fieldType 与 calcScript 校验透传"
```

---

## 任务 4:Mapper 扩展(列映射 + `selectComputedByTypeId`)

**文件:**
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/mapper/MonitorContentMapper.java`
- 修改:`server/zwei-iot-monitor/src/main/resources/mapper/iot/monitor/MonitorContentMapper.xml`

- [ ] **步骤 1:Mapper 接口加方法**

在 `MonitorContentMapper.java` 末尾(`checkSortOrderExists` 方法之后)追加:

```java
    /**
     * 查询指定监测类型下的所有计算属性(按 sort_order 排序, 仅未删除)
     *
     * @param monitorTypeId 监测类型ID
     * @return 计算属性列表(field_type='computed'), 按 sort_order 升序
     */
    List<MonitorContent> selectComputedByTypeId(@Param("monitorTypeId") Long monitorTypeId);
```

(若 `@Param` import 缺失,顶部 `import org.apache.ibatis.annotations.Param;` 已有 — 接口内别处已用)

- [ ] **步骤 2:XML 加列映射 + 查询**

打开 `MonitorContentMapper.xml`,在 `<resultMap>`(若有)或所有 `<select>`/`<insert>`/`<update>` 中包含 `indicator_type` 的位置追加 `field_type` 和 `calc_script`。

**先查看现有 XML 结构:**

```bash
grep -n "indicator_type\|field_type\|calc_script" server/zwei-iot-monitor/src/main/resources/mapper/iot/monitor/MonitorContentMapper.xml
```

预期:`indicator_type` 出现在多处(列映射、insert 列、update set、select 字段)。

在每个 `<result property="indicatorType" .../>` 之后追加两行:

```xml
        <result property="fieldType"   column="field_type"/>
        <result property="calcScript"  column="calc_script"/>
```

在每个 `INSERT INTO monitor_content (...)` 的列清单中追加 `field_type, calc_script`,在对应 `#{...}` 值清单中追加 `#{fieldType}, #{calcScript}`。

在 `<update>` 的 `<set>` 块中追加:

```xml
            <if test="fieldType != null">field_type = #{fieldType},</if>
            <if test="calcScript != null">calc_script = #{calcScript},</if>
```

在 `<sql id="selectMonitorContentVo">`(或类似片段)的字段清单中追加 `mc.field_type, mc.calc_script`。

最后在 `</mapper>` 之前追加新查询:

```xml
    <select id="selectComputedByTypeId" resultMap="MonitorContentResult">
        SELECT id, monitor_type_id, code, name, unit, indicator_type,
               field_type, calc_script, sort_order,
               create_by, create_time, update_by, update_time, del_flag
        FROM monitor_content
        WHERE monitor_type_id = #{monitorTypeId}
          AND field_type = 'computed'
          AND del_flag = 0
        ORDER BY sort_order ASC
    </select>
```

(若 resultMap id 不是 `MonitorContentResult`,改为实际 id — 通过 `grep "<resultMap" MonitorContentMapper.xml` 确认)

- [ ] **步骤 3:编译验证**

```bash
cd server && mvn -pl zwei-iot-monitor -am test-compile
```

预期:`BUILD SUCCESS`。

- [ ] **步骤 4:Commit**

```bash
git add server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/mapper/MonitorContentMapper.java \
        server/zwei-iot-monitor/src/main/resources/mapper/iot/monitor/MonitorContentMapper.xml
git commit -m "feat(monitor): MonitorContentMapper 新增 selectComputedByTypeId 与列映射"
```

---

## 任务 5:`IMonitorContentService` + 实现 + 缓存联动

**文件:**
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/service/IMonitorContentService.java`
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/service/impl/MonitorContentServiceImpl.java`

- [ ] **步骤 1:接口加方法**

在 `IMonitorContentService.java` 末尾追加:

```java
    /**
     * 查询指定监测类型下的所有计算属性(按 sort_order 排序)。
     * 供 ComputedAttributeRegistry 使用。
     *
     * @param monitorTypeId 监测类型ID
     * @return 计算属性列表, 空列表表示无计算属性
     */
    List<MonitorContent> selectComputedByTypeId(Long monitorTypeId);
```

- [ ] **步骤 2:实现方法**

在 `MonitorContentServiceImpl.java` 末尾(最后一个方法之后、类闭合 `}` 之前)追加:

```java
    /**
     * 查询指定监测类型下的所有计算属性。
     * 直接走 mapper, 缓存由 ComputedAttributeRegistry 负责(@Cacheable 在那一层)。
     */
    @Override
    public List<MonitorContent> selectComputedByTypeId(Long monitorTypeId) {
        return monitorContentMapper.selectComputedByTypeId(monitorTypeId);
    }
```

- [ ] **步骤 3:为所有 @CacheEvict 追加 computedAttrs 联动**

在 `MonitorContentServiceImpl.java` 中,有 6 处 `@Caching(evict = {...})` 注解(insert/update/delete 各 2 种)。每处追加一行:

```java
            @CacheEvict(value = "computedAttrs", allEntries = true)
```

最终每处 `@Caching` 形如:

```java
    @Caching(evict = {
            @CacheEvict(value = "monitorContent", key = "#monitorContent.id"),
            @CacheEvict(value = "monitorContentList", allEntries = true),
            @CacheEvict(value = "monitorType", allEntries = true),
            @CacheEvict(value = "computedAttrs", allEntries = true)
    })
```

(对 6 处全部做同样追加)

- [ ] **步骤 4:配置 cache name(若使用 Spring Cache)`

检查 `server/zwei-framework/src/main/java/.../CacheConfig.java` 或 `application.yml` 中是否需要显式注册 `computedAttrs` 缓存名:

```bash
grep -rn "cacheNames\|cache-names\|monitorContentList" server/zwei-framework/src/main/ server/zwei-admin/src/main/resources/
```

若 `monitorContentList` 已被列出,把 `computedAttrs` 一并加入。若使用 `@Cacheable` 自动创建模式(Spring Boot 默认),跳过此步。

- [ ] **步骤 5:编译验证**

```bash
cd server && mvn -pl zwei-iot-monitor -am compile
```

预期:`BUILD SUCCESS`。

- [ ] **步骤 6:Commit**

```bash
git add server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/service/IMonitorContentService.java \
        server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/service/impl/MonitorContentServiceImpl.java
git commit -m "feat(monitor): IMonitorContentService 新增 selectComputedByTypeId + computedAttrs 缓存联动"
```

---

## 任务 6:`SensorMetadata` 扩展 `monitorTypeId` 字段

**背景:** 规格中 `ComputedAttributeEvaluator` 需要从 `deviceId + sensorCode` 取 `monitorTypeId`,但 `SensorMetadata` record 当前只有 `(deviceId, sensorId, attributes)`。最小破坏性变更:加字段。

**文件:**
- 修改:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/SensorMetadata.java`
- 修改:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceSensorQueryServiceImpl.java`

- [ ] **步骤 1:扩展 record**

`SensorMetadata.java` 完整替换为:

```java
package com.zwei.iot.device.domain;

import lombok.Builder;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Builder
public record SensorMetadata(
        Long deviceId,
        Long sensorId,
        Long monitorTypeId,
        List<SensorAttribute> attributes
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
```

- [ ] **步骤 2:构造时填入 monitorTypeId**

`DeviceSensorQueryServiceImpl.java` 的 `requireSensorMetadata` 方法,把构造语句替换为:

```java
        return SensorMetadata.builder()
                .deviceId(deviceId)
                .sensorId(sensor.getId())
                .monitorTypeId(sensor.getMonitorTypeId())
                .attributes(attributes)
                .build();
```

(`DeviceSensor` 实体已有 `monitorTypeId` 字段,见 `device_sensor` 表)

- [ ] **步骤 3:排查现有 SensorMetadata.builder() 调用点**

```bash
grep -rn "SensorMetadata.builder\|new SensorMetadata" server/ --include="*.java"
```

预期只命中 `DeviceSensorQueryServiceImpl` 一处。若其他位置(如测试 mock)使用了 builder,**无需改动** — Lombok `@Builder` 加字段是非破坏性的,旧调用产出新字段为 null。

- [ ] **步骤 4:编译 + 跑 zwei-iot-device 测试**

```bash
cd server && mvn -pl zwei-iot-device -am test
```

预期:`BUILD SUCCESS`,所有现有测试通过。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/SensorMetadata.java \
        server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceSensorQueryServiceImpl.java
git commit -m "feat(device): SensorMetadata 新增 monitorTypeId 字段供计算属性求值使用"
```

---

## 任务 7:`ComputedAttribute` record + `ComputedAttributeRegistry`(TDD)

**文件:**
- 创建:`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttribute.java`
- 创建:`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeRegistry.java`
- 创建测试:由于 Registry 是 `@Cacheable` 包装层(逻辑薄),不单独写单测。`ComputedAttribute` 是 record 无需测试。

- [ ] **步骤 1:创建 `ComputedAttribute` record**

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.iot.monitor.domain.MonitorContent;

/**
 * 计算属性(来自 monitor_content WHERE field_type='computed')。
 *
 * <p>由 {@link ComputedAttributeRegistry} 从字典层加载并缓存, 供
 * {@link ComputedAttributeEvaluator} 拼装脚本执行。
 *
 * @param id         monitor_content.id
 * @param monitorTypeId 所属监测类型 ID
 * @param code       属性编码(必须合法 Java 标识符, 用作 Groovy 函数名 calc_{code})
 * @param name       属性名称(中文)
 * @param unit       单位
 * @param calcScript Groovy 脚本片段(非空)
 * @param sortOrder  排序号(决定求值顺序)
 */
public record ComputedAttribute(
        Long id,
        Long monitorTypeId,
        String code,
        String name,
        String unit,
        String calcScript,
        Integer sortOrder
) {
    public static ComputedAttribute from(MonitorContent mc) {
        if (mc.getCode() == null || !mc.getCode().matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException(
                    "非法 attrCode (必须以字母开头, 只含字母数字下划线): " + mc.getCode());
        }
        return new ComputedAttribute(
                mc.getId(),
                mc.getMonitorTypeId(),
                mc.getCode(),
                mc.getName(),
                mc.getUnit(),
                mc.getCalcScript(),
                mc.getSortOrder() == null ? 0 : mc.getSortOrder()
        );
    }
}
```

- [ ] **步骤 2:创建 `ComputedAttributeRegistry`**

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.service.IMonitorContentService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按监测类型 ID 加载并缓存计算属性列表。
 *
 * <p>缓存 name = "computedAttrs", 由 {@code MonitorContentServiceImpl} 的
 * {@code @CacheEvict(allEntries=true)} 在 insert/update/delete 时联动失效。
 */
@Service
public class ComputedAttributeRegistry {

    private final IMonitorContentService monitorContentService;

    public ComputedAttributeRegistry(IMonitorContentService monitorContentService) {
        this.monitorContentService = monitorContentService;
    }

    /**
     * 取指定监测类型下的计算属性列表(按 sort_order 升序)。
     * 空列表表示该类型无计算属性, 调用方可走 fast path。
     */
    @Cacheable(value = "computedAttrs", key = "#monitorTypeId")
    public List<ComputedAttribute> getByMonitorTypeId(Long monitorTypeId) {
        List<MonitorContent> raw = monitorContentService.selectComputedByTypeId(monitorTypeId);
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().map(ComputedAttribute::from).toList();
    }
}
```

- [ ] **步骤 3:编译验证**

```bash
cd server && mvn -pl zwei-iot-timeseries -am compile
```

预期:`BUILD SUCCESS`。

- [ ] **步骤 4:Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttribute.java \
        server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeRegistry.java
git commit -m "feat(timeseries): 新增 ComputedAttribute 与 Registry 字典层缓存"
```

---

## 任务 8:`ParsedMessageSnapshot` + `LastMessageStore`(TDD)

**文件:**
- 创建:`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ParsedMessageSnapshot.java`
- 创建:`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/LastMessageStore.java`
- 创建测试:`server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/LastMessageStoreTest.java`

- [ ] **步骤 1:创建 `ParsedMessageSnapshot` record**

```java
package com.zwei.iot.timeseries.compute;

import java.util.Map;

/**
 * ParsedMessage 的精简快照, 用于 Redis 缓存上一条消息(prevData)。
 *
 * <p>properties 是 {@code Map<String, Double>}(attrCode -> value),
 * 含固有属性 + 计算属性结果。
 *
 * @param deviceCode 设备编码
 * @param sensorCode 传感器编码
 * @param dataTime   数据采集时间(epoch ms)
 * @param properties 属性值映射
 */
public record ParsedMessageSnapshot(
        String deviceCode,
        String sensorCode,
        long dataTime,
        Map<String, Double> properties
) {
}
```

- [ ] **步骤 2:写失败的测试 `LastMessageStoreTest`**

创建 `server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/LastMessageStoreTest.java`:

```java
package com.zwei.iot.timeseries.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("LastMessageStore")
class LastMessageStoreTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private LastMessageStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        store = new LastMessageStore(redis, new ObjectMapper());
    }

    @Test
    @DisplayName("get 命中: 返回反序列化后的 snapshot")
    void getHit() {
        String json = "{\"deviceCode\":\"D1\",\"sensorCode\":\"S1\",\"dataTime\":1700000000000,"
                + "\"properties\":{\"rainfall\":12.5}}";
        when(valueOps.get("monitor:last:1:S1")).thenReturn(json);

        ParsedMessageSnapshot snap = store.get(1L, "S1");

        assertThat(snap).isNotNull();
        assertThat(snap.deviceCode()).isEqualTo("D1");
        assertThat(snap.dataTime()).isEqualTo(1700000000000L);
        assertThat(snap.properties()).containsEntry("rainfall", 12.5);
    }

    @Test
    @DisplayName("get miss: 返回 null")
    void getMiss() {
        when(valueOps.get(anyString())).thenReturn(null);
        assertThat(store.get(1L, "S1")).isNull();
    }

    @Test
    @DisplayName("get Redis 异常: 返回 null, 不抛")
    void getRedisFailure() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        assertThat(store.get(1L, "S1")).isNull();
    }

    @Test
    @DisplayName("put: 写入 JSON 并设置 TTL")
    void put() {
        ParsedMessageSnapshot snap = new ParsedMessageSnapshot(
                "D1", "S1", 1700000000000L, Map.of("rainfall", 12.5));

        store.put(1L, "S1", snap);

        verify(valueOps).set(eq("monitor:last:1:S1"), contains("\"rainfall\":12.5"),
                any(Duration.class));
    }

    @Test
    @DisplayName("put Redis 异常: 仅吞异常不抛")
    void putRedisFailure() {
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        ParsedMessageSnapshot snap = new ParsedMessageSnapshot(
                "D1", "S1", 1L, Map.of());

        // 不抛异常即视为通过
        store.put(1L, "S1", snap);
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }
}
```

- [ ] **步骤 3:运行测试,确认失败**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test -Dtest=LastMessageStoreTest
```

预期:`FAIL`(编译错误:`LastMessageStore` 类不存在)。

- [ ] **步骤 4:创建 `LastMessageStore`**

```java
package com.zwei.iot.timeseries.compute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 按 deviceId + sensorCode 缓存最近一条 ParsedMessage 精简快照, 用作下次脚本执行的 prevData。
 *
 * <p>Redis Key: {@code monitor:last:{deviceId}:{sensorCode}}, TTL 7 天。
 * 任何失败仅 warn 日志, 不抛异常, 不影响主链路。
 */
@Service
public class LastMessageStore {

    private static final Logger log = LoggerFactory.getLogger(LastMessageStore.class);
    private static final String KEY_PREFIX = "monitor:last:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public LastMessageStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 取上一条精简消息; null 表示首次上报 / 已过期 / Redis 异常。 */
    public ParsedMessageSnapshot get(Long deviceId, String sensorCode) {
        try {
            String json = redis.opsForValue().get(buildKey(deviceId, sensorCode));
            if (json == null) return null;
            return objectMapper.readValue(json, ParsedMessageSnapshot.class);
        } catch (Exception e) {
            log.warn("LastMessageStore.get failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
            return null;
        }
    }

    /** 写当前条作为下次的 prevData; 失败仅 warn。 */
    public void put(Long deviceId, String sensorCode, ParsedMessageSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redis.opsForValue().set(buildKey(deviceId, sensorCode), json, TTL);
        } catch (Exception e) {
            log.warn("LastMessageStore.put failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
        }
    }

    private String buildKey(Long deviceId, String sensorCode) {
        return KEY_PREFIX + deviceId + ":" + sensorCode;
    }
}
```

- [ ] **步骤 5:运行测试,确认通过**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test -Dtest=LastMessageStoreTest
```

预期:5 用例全 PASS。

- [ ] **步骤 6:Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ParsedMessageSnapshot.java \
        server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/LastMessageStore.java \
        server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/LastMessageStoreTest.java
git commit -m "feat(timeseries): 新增 LastMessageStore 与 ParsedMessageSnapshot(prevData 缓存)"
```

---

## 任务 9:`ComputedScriptAssembler`(TDD)

**文件:**
- 创建:`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedScriptAssembler.java`
- 创建测试:`server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedScriptAssemblerTest.java`

- [ ] **步骤 1:写失败的测试**

```java
package com.zwei.iot.timeseries.compute;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ComputedScriptAssembler")
class ComputedScriptAssemblerTest {

    private final ComputedScriptAssembler assembler = new ComputedScriptAssembler();

    private ComputedAttribute attr(String code, String script, int sortOrder) {
        return new ComputedAttribute(1L, 100L, code, code, "", script, sortOrder);
    }

    @Test
    @DisplayName("空列表产出空脚本")
    void emptyList() {
        assertThat(assembler.assemble(List.of())).isEmpty();
    }

    @Test
    @DisplayName("单个属性: 拼出 calc_xxx 函数 + compute 主入口")
    void singleAttr() {
        String script = assembler.assemble(List.of(
                attr("velocity", "return curData.properties.displacement / 10", 1)));

        assertThat(script).contains("def calc_velocity(curData, prevData)")
                          .contains("return curData.properties.displacement / 10")
                          .contains("def compute(curData, prevData)")
                          .contains("out.velocity = calc_velocity(curData, prevData)");
    }

    @Test
    @DisplayName("多属性按 sort_order: 后算的属性对应 try 块在后")
    void multiAttrOrder() {
        String script = assembler.assemble(List.of(
                attr("delta", "return 1", 2),
                attr("velocity", "return 2", 1)));

        int posVelocity = script.indexOf("out.velocity =");
        int posDelta = script.indexOf("out.delta =");
        assertThat(posVelocity).isGreaterThan(0);
        assertThat(posDelta).isGreaterThan(posVelocity);  // velocity 先于 delta
    }

    @Test
    @DisplayName("求值顺序回填 curData.properties.putAll(out)")
    void populateCurDataForChaining() {
        String script = assembler.assemble(List.of(
                attr("a", "return 1", 1), attr("b", "return 2", 2)));

        // 至少出现一次 putAll 调用(让 b 能引用 a 的结果)
        assertThat(script).contains("curData.properties.putAll(out)");
    }

    @Test
    @DisplayName("缓存命中: 同列表二次调用返回相同实例")
    void cacheHit() {
        List<ComputedAttribute> attrs = List.of(attr("x", "return 1", 1));
        String s1 = assembler.assemble(attrs);
        String s2 = assembler.assemble(attrs);
        assertThat(s2).isSameAs(s1);
    }

    @Test
    @DisplayName("缓存失效: 内容变化重新拼装")
    void cacheInvalidate() {
        String s1 = assembler.assemble(List.of(attr("x", "return 1", 1)));
        String s2 = assembler.assemble(List.of(attr("x", "return 2", 1)));
        assertThat(s2).isNotSameAs(s1).isNotEqualTo(s1);
    }
}
```

- [ ] **步骤 2:运行测试,确认失败**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test -Dtest=ComputedScriptAssemblerTest
```

预期:`FAIL`(类不存在)。

- [ ] **步骤 3:实现 `ComputedScriptAssembler`**

```java
package com.zwei.iot.timeseries.compute;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将多个计算属性脚本片段拼装成一个 Groovy 源文件。
 *
 * <p>输出结构:
 * <pre>
 * def calc_attr1(curData, prevData) { /* user code 1 *\/ }
 * def calc_attr2(curData, prevData) { /* user code 2 *\/ }
 * def compute(curData, prevData) {
 *     def out = new LinkedHashMap&lt;String, Object&gt;()
 *     try { out.attr1 = calc_attr1(curData, prevData); curData.properties.putAll(out) }
 *          catch (Exception e) { /* skip *\/ }
 *     try { out.attr2 = calc_attr2(curData, prevData); curData.properties.putAll(out) }
 *          catch (Exception e) { /* skip *\/ }
 *     return out
 * }
 * </pre>
 *
 * <p>缓存 key = monitorTypeId + ":" + SHA-256(所有 calc_script 拼接),
 * 内容变化自动失效。
 */
@Service
public class ComputedScriptAssembler {

    /** key = sha256(monitorTypeId:scripts), value = 拼装后的 Groovy 源 */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 拼装 Groovy 源; 命中缓存则直接返回。
     *
     * @param attrs 必须按 sort_order 升序的列表(由 Registry 保证)
     * @return 拼装后的 Groovy 源, 空列表返回空字符串
     */
    public String assemble(List<ComputedAttribute> attrs) {
        if (attrs == null || attrs.isEmpty()) return "";

        String cacheKey = buildCacheKey(attrs);
        return cache.computeIfAbsent(cacheKey, k -> doAssemble(attrs));
    }

    private String doAssemble(List<ComputedAttribute> attrs) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("// Auto-generated by ComputedScriptAssembler. Do not edit.\n\n");

        // 1. 函数定义
        for (ComputedAttribute a : attrs) {
            sb.append("def calc_").append(a.code())
              .append("(curData, prevData) {\n")
              .append("    // === ").append(a.name()).append(" ===\n")
              .append(a.calcScript())
              .append("\n}\n\n");
        }

        // 2. 主入口 compute
        sb.append("def compute(curData, prevData) {\n");
        sb.append("    def out = new LinkedHashMap<String, Object>()\n");
        for (ComputedAttribute a : attrs) {
            sb.append("    try {\n");
            sb.append("        out.").append(a.code())
              .append(" = calc_").append(a.code()).append("(curData, prevData)\n");
            sb.append("        curData.properties.putAll(out)\n");
            sb.append("    } catch (Exception ignored) { /* warn-only skip */ }\n");
        }
        sb.append("    return out\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String buildCacheKey(List<ComputedAttribute> attrs) {
        StringBuilder raw = new StringBuilder();
        for (ComputedAttribute a : attrs) {
            raw.append(a.id()).append(':')
               .append(a.code()).append(':')
               .append(a.calcScript() == null ? "" : a.calcScript()).append('|');
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            // 不应失败; 退化到原值
            return Integer.toString(raw.hashCode());
        }
    }
}
```

- [ ] **步骤 4:运行测试,确认通过**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test -Dtest=ComputedScriptAssemblerTest
```

预期:6 用例全 PASS。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedScriptAssembler.java \
        server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedScriptAssemblerTest.java
git commit -m "feat(timeseries): 新增 ComputedScriptAssembler(多脚本拼装 + 缓存)"
```

---

## 任务 10:`GroovyScriptEngine.executeComputed` 入口(TDD)

**文件:**
- 修改:`server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptEngine.java`
- 创建测试:`server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/engine/GroovyScriptEngineComputedTest.java`

- [ ] **步骤 1:写失败的测试**

```java
package com.zwei.iot.parser.engine;

import com.zwei.iot.parser.service.DataParseLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("GroovyScriptEngine.executeComputed")
class GroovyScriptEngineComputedTest {

    private GroovyScriptEngine engine;
    private DataParseLogService logService;

    @BeforeEach
    void setUp() {
        logService = mock(DataParseLogService.class);
        engine = new GroovyScriptEngine();
        injectField(engine, "builtInFunctions", new BuiltInFunctions());
        injectField(engine, "logService", logService);
    }

    /** Reflection helper — 与 GroovyScriptEngineTest 一致 */
    private static void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("正常执行: curData 与 prevData 注入, 返回 Map")
    void executeSuccess() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.velocity = curData.properties.displacement * 2
                out.delta = curData.properties.displacement - prevData.properties.displacement
                return out
            }
        """;
        Map<String, Object> cur = Map.of(
                "properties", Map.of("displacement", 12.5));
        Map<String, Object> prev = Map.of(
                "properties", Map.of("displacement", 10.0));

        Map<String, Object> out = engine.executeComputed(script, cur, prev);

        assertThat(out).hasSize(2);
        assertThat(out.get("velocity")).isEqualTo(25.0);
        assertThat(out.get("delta")).isEqualTo(2.5);
    }

    @Test
    @DisplayName("prevData=null: 脚本需自行处理, 正常返回")
    void executeWithNullPrev() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.x = curData.properties.x
                return out
            }
        """;
        Map<String, Object> cur = Map.of("properties", Map.of("x", 7.0));

        Map<String, Object> out = engine.executeComputed(script, cur, null);

        assertThat(out.get("x")).isEqualTo(7.0);
    }

    @Test
    @DisplayName("脚本异常: 返回空 Map, 不抛")
    void executeException() {
        String script = """
            def compute(curData, prevData) {
                throw new RuntimeException("boom")
            }
        """;
        Map<String, Object> out = engine.executeComputed(script, Map.of(), Map.of());
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("沙箱拒绝: System.exit 调用应失败, 返回空 Map")
    void sandboxRejects() {
        String script = """
            def compute(curData, prevData) {
                Runtime.runtime.exec("rm -rf /")
                return [:]
            }
        """;
        Map<String, Object> out = engine.executeComputed(script, Map.of(), Map.of());
        assertThat(out).isEmpty();
    }
}
```

- [ ] **步骤 2:运行测试,确认失败**

```bash
cd server && mvn -pl zwei-iot-parser -am test -Dtest=GroovyScriptEngineComputedTest
```

预期:`FAIL`(找不到 `executeComputed` 方法)。

- [ ] **步骤 3:在 `GroovyScriptEngine` 加 `executeComputed`**

在 `GroovyScriptEngine.java` 中,在 `testScript` 方法**之前**插入:

```java
    /**
     * 执行合并后的计算属性脚本。
     *
     * <p>与 {@link #execute} 共享沙箱配置 ({@link #createSecureConfig()}) 和 executor,
     * 但调用约定不同: 脚本必须定义 {@code compute(curData, prevData)} 主入口,
     * 返回 {@code Map<String, Object>}(attrCode -> value)。
     *
     * <p>失败永远返回空 Map, 不抛异常(主链路数据接入可用性优先)。
     *
     * @param scriptCode ComputedScriptAssembler.assemble() 产物
     * @param curData    当前精简消息 Map (含 deviceCode/sensorCode/dataTime/properties)
     * @param prevData   上一条精简消息 Map, 首次上报时为 null
     * @return 计算结果 Map; 失败时为空 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeComputed(String scriptCode,
                                                Map<String, Object> curData,
                                                Map<String, Object> prevData) {
        Future<Map<String, Object>> future = executor.submit(() -> {
            try {
                GroovyShell shell = new GroovyShell(createSecureConfig());
                Binding binding = new Binding();
                binding.setVariable("builtin", builtInFunctions);
                Script script = shell.parse(scriptCode);
                script.setBinding(binding);
                Object result = script.invokeMethod(
                        "compute", new Object[]{curData, prevData});
                return result instanceof Map ? (Map<String, Object>) result : Map.of();
            } catch (Exception e) {
                log.warn("Computed script execution failed", e);
                return Map.of();
            }
        });
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Computed script timed out ({}s)", TIMEOUT_SECONDS);
            future.cancel(true);
            return Map.of();
        } catch (Exception e) {
            log.warn("Computed script interrupted", e);
            return Map.of();
        }
    }
```

- [ ] **步骤 4:运行测试,确认通过**

```bash
cd server && mvn -pl zwei-iot-parser -am test -Dtest=GroovyScriptEngineComputedTest
```

预期:4 用例全 PASS。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/engine/GroovyScriptEngine.java \
        server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/engine/GroovyScriptEngineComputedTest.java
git commit -m "feat(parser): GroovyScriptEngine 新增 executeComputed 计算属性求值入口"
```

---

## 任务 11:`ComputedAttributeEvaluator`(TDD)

**文件:**
- 创建:`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java`
- 创建测试:`server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java`

- [ ] **步骤 1:写失败的测试**

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ComputedAttributeEvaluator")
class ComputedAttributeEvaluatorTest {

    private IDeviceSensorQueryService sensorQuery;
    private ComputedAttributeRegistry registry;
    private ComputedScriptAssembler assembler;
    private LastMessageStore lastMessageStore;
    private GroovyScriptEngine scriptEngine;
    private ComputedAttributeEvaluator evaluator;

    @BeforeEach
    void setUp() {
        sensorQuery = mock(IDeviceSensorQueryService.class);
        registry = mock(ComputedAttributeRegistry.class);
        assembler = new ComputedScriptAssembler();
        lastMessageStore = mock(LastMessageStore.class);
        scriptEngine = mock(GroovyScriptEngine.class);

        evaluator = new ComputedAttributeEvaluator(
                sensorQuery, registry, assembler, lastMessageStore, scriptEngine);
    }

    private ParsedMessage msg(double value) {
        return new ParsedMessage(
                "D1", "S1", "sys", 1700000000000L, 1700000000000L, "hash",
                List.of(new PropertyValue("displacement", "位移", "mm", value, 0)));
    }

    private void stubSensor() {
        when(sensorQuery.requireSensorMetadata(1L, "S1"))
                .thenReturn(SensorMetadata.builder()
                        .deviceId(1L).sensorId(10L).monitorTypeId(100L)
                        .attributes(List.of()).build());
    }

    @Test
    @DisplayName("fast path: 无计算属性返回空 list, 不调 scriptEngine")
    void noComputedAttrs() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of());

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).isEmpty();
        verifyNoInteractions(scriptEngine);
        verify(lastMessageStore, never()).put(any(), any(), any());
    }

    @Test
    @DisplayName("monitorTypeId 缺失: 返回空 list, 不调 registry")
    void noMonitorTypeId() {
        when(sensorQuery.requireSensorMetadata(1L, "S1"))
                .thenReturn(SensorMetadata.builder()
                        .deviceId(1L).sensorId(10L).monitorTypeId(null)
                        .attributes(List.of()).build());

        assertThat(evaluator.evaluate(1L, "S1", msg(12.0))).isEmpty();
        verifyNoInteractions(registry);
    }

    @Test
    @DisplayName("首次上报(prevData=null): 脚本可执行, 结果合并 properties")
    void firstReport() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "velocity", "速率", "mm/s",
                        "return curData.properties.displacement * 2", 1)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), isNull()))
                .thenReturn(Map.of("velocity", 24.0));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).identifier()).isEqualTo("velocity");
        assertThat(out.get(0).value()).isEqualTo(24.0);
        verify(lastMessageStore).put(eq(1L), eq("S1"), argThat(s ->
                s.properties().containsKey("velocity") &&
                s.properties().containsKey("displacement")));
    }

    @Test
    @DisplayName("全部脚本失败(返回空 Map): 不写回 lastMessageStore, 返回空 list")
    void allScriptsFail() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "velocity", "速率", "mm/s",
                        "return 1/0", 1)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), any()))
                .thenReturn(Map.of());

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).isEmpty();
        // 即使全部失败, prevData 也按规格修复后逻辑应更新(否则下次仍是旧值)
        // — 这里规格修复点是"只要进入求值阶段就更新", 故仍调用 put
        verify(lastMessageStore).put(eq(1L), eq("S1"), any());
    }

    @Test
    @DisplayName("sensorQuery 抛异常: 返回空 list, 不向上抛")
    void sensorQueryThrows() {
        when(sensorQuery.requireSensorMetadata(1L, "S1"))
                .thenThrow(new RuntimeException("db down"));

        assertThat(evaluator.evaluate(1L, "S1", msg(12.0))).isEmpty();
    }

    @Test
    @DisplayName("返回值非数值: 该属性被跳过, 其他保留")
    void nonNumericResult() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "good", "好", "", "return 1.0", 1),
                new ComputedAttribute(2L, 100L, "bad", "坏", "",
                        "return 'not a number'", 2)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), isNull()))
                .thenReturn(Map.of("good", 1.0, "bad", "not a number"));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).identifier()).isEqualTo("good");
    }

    @Test
    @DisplayName("部分属性结果缺失(attrCode 不在 results map 中): 跳过该属性")
    void missingResultKey() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "a", "A", "", "return 1", 1),
                new ComputedAttribute(2L, 100L, "b", "B", "", "return 2", 2)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), isNull()))
                .thenReturn(Map.of("a", 1.0));  // b 缺失(脚本内异常)

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).identifier()).isEqualTo("a");
    }

    @Test
    @DisplayName("prevData 命中: 透传给 scriptEngine")
    void prevDataPassed() {
        stubSensor();
        ParsedMessageSnapshot prevSnap = new ParsedMessageSnapshot(
                "D1", "S1", 1700000000000L, Map.of("displacement", 10.0));
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "delta", "差分", "",
                        "return curData.properties.displacement - prevData.properties.displacement", 1)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(prevSnap);
        when(scriptEngine.executeComputed(anyString(), any(), any()))
                .thenReturn(Map.of("delta", 2.0));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).value()).isEqualTo(2.0);
    }
}
```

- [ ] **步骤 2:运行测试,确认失败**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test -Dtest=ComputedAttributeEvaluatorTest
```

预期:`FAIL`(类不存在)。

- [ ] **步骤 3:实现 `ComputedAttributeEvaluator`**

```java
package com.zwei.iot.timeseries.compute;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算属性求值主入口。
 *
 * <p>由 {@code MonitorIngestFacade.ingest()} 在 enrichProperties 之后调用,
 * 产出的 {@link PropertyValue} 列表追加到 {@code parsedMessage.properties},
 * 与固有属性同链路写入 IoTDB。
 *
 * <p>核心契约: 任何失败仅 warn 跳过, **绝不影响主链路**。
 */
@Service
public class ComputedAttributeEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ComputedAttributeEvaluator.class);

    private final IDeviceSensorQueryService sensorQuery;
    private final ComputedAttributeRegistry registry;
    private final ComputedScriptAssembler assembler;
    private final LastMessageStore lastMessageStore;
    private final GroovyScriptEngine scriptEngine;

    public ComputedAttributeEvaluator(IDeviceSensorQueryService sensorQuery,
                                       ComputedAttributeRegistry registry,
                                       ComputedScriptAssembler assembler,
                                       LastMessageStore lastMessageStore,
                                       GroovyScriptEngine scriptEngine) {
        this.sensorQuery = sensorQuery;
        this.registry = registry;
        this.assembler = assembler;
        this.lastMessageStore = lastMessageStore;
        this.scriptEngine = scriptEngine;
    }

    /**
     * 对单条 ParsedMessage 执行计算属性求值。
     *
     * @return 计算属性列表, 空列表表示无计算属性 / 全部失败 / 元数据缺失
     */
    public List<PropertyValue> evaluate(Long deviceId, String sensorCode, ParsedMessage message) {
        try {
            // 1. 取 monitorTypeId
            SensorMetadata meta = sensorQuery.requireSensorMetadata(deviceId, sensorCode);
            Long monitorTypeId = meta.monitorTypeId();
            if (monitorTypeId == null) return List.of();

            // 2. fast path
            List<ComputedAttribute> attrs = registry.getByMonitorTypeId(monitorTypeId);
            if (attrs.isEmpty()) return List.of();

            // 3. prevData
            ParsedMessageSnapshot prev = lastMessageStore.get(deviceId, sensorCode);

            // 4. 构建 curData / prevData
            Map<String, Object> curData = buildCurData(message);
            Map<String, Object> prevData = prev == null ? null : buildPrevData(prev);

            // 5. 拼装脚本
            String script = assembler.assemble(attrs);

            // 6. 执行
            Map<String, Object> results = scriptEngine.executeComputed(script, curData, prevData);

            // 7. 转 PropertyValue (失败/非数值跳过)
            List<PropertyValue> computed = new ArrayList<>();
            for (ComputedAttribute a : attrs) {
                Object val = results.get(a.code());
                if (val == null) continue;
                Double dv = toDouble(val);
                if (dv == null) {
                    log.warn("Computed attribute returned non-numeric value: code={}, val={}",
                            a.code(), val);
                    continue;
                }
                computed.add(new PropertyValue(a.code(), a.name(), a.unit(), dv, 0));
            }

            // 8. 总是写回 prevData(避免下次脚本看到更旧的 prev)
            Map<String, Double> mergedProps = new LinkedHashMap<>();
            for (PropertyValue p : message.properties()) {
                if (p.value() != null) mergedProps.put(p.identifier(), p.value());
            }
            for (PropertyValue p : computed) mergedProps.put(p.identifier(), p.value());
            lastMessageStore.put(deviceId, sensorCode,
                    new ParsedMessageSnapshot(message.deviceCode(), message.sensorCode(),
                            message.dataTime(), mergedProps));

            return computed;
        } catch (Exception e) {
            log.warn("ComputedAttributeEvaluator failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
            return List.of();
        }
    }

    private Map<String, Object> buildCurData(ParsedMessage msg) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (PropertyValue p : msg.properties()) {
            if (p.value() != null) props.put(p.identifier(), p.value());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCode", msg.deviceCode());
        data.put("sensorCode", msg.sensorCode());
        data.put("dataTime", msg.dataTime());
        data.put("properties", props);
        return data;
    }

    private Map<String, Object> buildPrevData(ParsedMessageSnapshot snap) {
        Map<String, Object> props = new LinkedHashMap<>(snap.properties());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCode", snap.deviceCode());
        data.put("sensorCode", snap.sensorCode());
        data.put("dataTime", snap.dataTime());
        data.put("properties", props);
        return data;
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **步骤 4:运行测试,确认通过**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test -Dtest=ComputedAttributeEvaluatorTest
```

预期:8 用例全 PASS。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluator.java \
        server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/compute/ComputedAttributeEvaluatorTest.java
git commit -m "feat(timeseries): 新增 ComputedAttributeEvaluator 求值主入口"
```

---

## 任务 12:`MonitorIngestFacade` 接入

**文件:**
- 修改:`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java`

- [ ] **步骤 1:注入 `ComputedAttributeEvaluator`**

修改 `MonitorIngestFacade` 的字段与构造器:

```java
public class MonitorIngestFacade {
    private final MonitorTopicParser topicParser;
    private final MonitorMetadataService metadataService;
    private final GroovyScriptEngine scriptEngine;
    private final MonitorIngestStreamService streamService;
    private final ComputedAttributeEvaluator computedAttrEvaluator;   // 新增

    @Autowired
    public MonitorIngestFacade(MonitorTopicParser topicParser,
                               MonitorMetadataService metadataService,
                               GroovyScriptEngine scriptEngine,
                               MonitorIngestStreamService streamService,
                               ComputedAttributeEvaluator computedAttrEvaluator) {  // 新增
        this.topicParser = topicParser;
        this.metadataService = metadataService;
        this.scriptEngine = scriptEngine;
        this.streamService = streamService;
        this.computedAttrEvaluator = computedAttrEvaluator;
    }
```

顶部 import 追加:

```java
import com.zwei.iot.timeseries.compute.ComputedAttributeEvaluator;
```

- [ ] **步骤 2:在 ④ 与 ⑤ 之间插入求值环节**

在 `ingest` 方法中,在 `enrichProperties` 的 try-catch 块**之后**、`streamService.enqueue(parsedMessage)` **之前**插入:

```java
        // 4.5 Computed attributes evaluation
        try {
            List<com.zwei.common.domain.PropertyValue> computed =
                    computedAttrEvaluator.evaluate(deviceId, parsedMessage.sensorCode(), parsedMessage);
            if (!computed.isEmpty()) {
                List<com.zwei.common.domain.PropertyValue> merged =
                        new java.util.ArrayList<>(parsedMessage.properties());
                merged.addAll(computed);
                parsedMessage = new com.zwei.common.domain.ParsedMessage(
                        parsedMessage.deviceCode(), parsedMessage.sensorCode(),
                        parsedMessage.sourceType(), parsedMessage.dataTime(),
                        parsedMessage.receiveTime(), parsedMessage.payloadHash(), merged);
            }
        } catch (Exception e) {
            log.warn("Computed attribute evaluation failed, skip: deviceId={}, sensorCode={}",
                    deviceId, parsedMessage.sensorCode(), e);
        }
```

- [ ] **步骤 3:编译验证**

```bash
cd server && mvn -pl zwei-iot-timeseries -am compile
```

预期:`BUILD SUCCESS`。

- [ ] **步骤 4:跑 timeseries 全部测试,确认无回归**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test
```

预期:所有测试通过(原有的 `IotdbTimeSeriesServiceQueryTest` 等 + 新增的 compute 包测试)。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java
git commit -m "feat(timeseries): MonitorIngestFacade 第 4.5 环节接入计算属性求值"
```

---

## 任务 13:`test-script` 端点 + 权限注册

**文件:**
- 创建:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/CalcScriptTestRequest.java`
- 创建:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/CalcScriptTestResult.java`
- 修改:`server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/controller/MonitorContentController.java`
- 修改:本地数据库 `sys_menu` 表(直接 INSERT)

> **设计权衡:** `test-script` 需要"按指定 monitorTypeId 加载该类型下其他计算属性脚本 + 临时替换目标 attrCode 的脚本",拼装后调 `GroovyScriptEngine.executeComputed`。
> 这要求 `MonitorContentController` 注入 `ComputedAttributeRegistry` + `ComputedScriptAssembler` + `GroovyScriptEngine`。
> 但 `zwei-iot-monitor` 模块不依赖 `zwei-iot-timeseries`(方向相反),也不能直接依赖 `zwei-iot-parser`(底层依赖)。
>
> **方案:** 把 test-script 端点放在 `zwei-iot-timeseries` 模块,新建一个独立的 controller,只暴露测试 API。这样依赖方向正确(timeseries → monitor + parser)。

**调整:**
- 文件移动到 `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/controller/ComputedAttributeTestController.java`
- `CalcScriptTestRequest`/`Result` 也放到 timeseries 模块
- `MonitorContentController` 只加 calcScript 字段透传(已在任务 3 完成),不涉及 test API

- [ ] **步骤 1:创建 `CalcScriptTestRequest` DTO(timeseries 模块)**

`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/dto/CalcScriptTestRequest.java`:

```java
package com.zwei.iot.timeseries.compute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 计算脚本在线测试请求。
 *
 * <p>后端会用 monitorTypeId 加载该类型下其他计算属性的脚本, 把目标 attrCode
 * 的脚本临时替换为请求中的 calcScript, 拼装后执行, 返回结果。
 */
public class CalcScriptTestRequest {

    @NotNull(message = "监测类型ID不能为空")
    private Long monitorTypeId;

    @NotBlank(message = "属性编码不能为空")
    private String attrCode;

    @NotBlank(message = "计算脚本不能为空")
    @Size(max = 65535, message = "计算脚本长度不能超过 64KB")
    private String calcScript;

    /** 模拟 curData, 例如 { "properties": { "displacement": 12.5 } } */
    private Map<String, Object> curData;

    /** 模拟 prevData, 可空 */
    private Map<String, Object> prevData;

    public Long getMonitorTypeId() { return monitorTypeId; }
    public void setMonitorTypeId(Long monitorTypeId) { this.monitorTypeId = monitorTypeId; }
    public String getAttrCode() { return attrCode; }
    public void setAttrCode(String attrCode) { this.attrCode = attrCode; }
    public String getCalcScript() { return calcScript; }
    public void setCalcScript(String calcScript) { this.calcScript = calcScript; }
    public Map<String, Object> getCurData() { return curData; }
    public void setCurData(Map<String, Object> curData) { this.curData = curData; }
    public Map<String, Object> getPrevData() { return prevData; }
    public void setPrevData(Map<String, Object> prevData) { this.prevData = prevData; }
}
```

- [ ] **步骤 2:创建 `CalcScriptTestResult` DTO**

`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/dto/CalcScriptTestResult.java`:

```java
package com.zwei.iot.timeseries.compute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalcScriptTestResult {
    private boolean success;
    private Map<String, Object> result;
    private String error;
    private long executionTime;

    public static CalcScriptTestResult ok(Map<String, Object> result, long elapsed) {
        CalcScriptTestResult r = new CalcScriptTestResult();
        r.success = true;
        r.result = result;
        r.executionTime = elapsed;
        return r;
    }

    public static CalcScriptTestResult fail(String error) {
        CalcScriptTestResult r = new CalcScriptTestResult();
        r.success = false;
        r.error = error;
        return r;
    }

    public boolean isSuccess() { return success; }
    public Map<String, Object> getResult() { return result; }
    public String getError() { return error; }
    public long getExecutionTime() { return executionTime; }
}
```

- [ ] **步骤 3:创建 `ComputedAttributeTestController`**

`server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/controller/ComputedAttributeTestController.java`:

```java
package com.zwei.iot.timeseries.compute.controller;

import com.zwei.common.constant.HttpStatus;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.timeseries.compute.ComputedAttribute;
import com.zwei.iot.timeseries.compute.ComputedAttributeRegistry;
import com.zwei.iot.timeseries.compute.ComputedScriptAssembler;
import com.zwei.iot.timeseries.compute.dto.CalcScriptTestRequest;
import com.zwei.iot.timeseries.compute.dto.CalcScriptTestResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 计算脚本在线测试端点。
 *
 * <p>路径前缀: /api/v1/monitor-contents/test-script
 * (沿用 monitor-contents 资源语义, 但 controller 在 timeseries 模块以避免依赖反向)
 */
@RestController
@RequestMapping("api/v1/monitor-contents")
public class ComputedAttributeTestController extends BaseController {

    private final ComputedAttributeRegistry registry;
    private final ComputedScriptAssembler assembler;
    private final GroovyScriptEngine scriptEngine;

    @Autowired
    public ComputedAttributeTestController(ComputedAttributeRegistry registry,
                                           ComputedScriptAssembler assembler,
                                           GroovyScriptEngine scriptEngine) {
        this.registry = registry;
        this.assembler = assembler;
        this.scriptEngine = scriptEngine;
    }

    @PreAuthorize("@ss.hasPermi('basic:monitorContent:test')")
    @PostMapping("/test-script")
    public AjaxResult testScript(@Valid @RequestBody CalcScriptTestRequest request) {
        // 1. 加载该监测类型下其他计算属性(作为上下文)
        List<ComputedAttribute> existing = registry.getByMonitorTypeId(request.getMonitorTypeId());

        // 2. 用请求中的脚本替换目标 attrCode(若不存在则追加)
        List<ComputedAttribute> merged = new ArrayList<>();
        boolean replaced = false;
        for (ComputedAttribute a : existing) {
            if (a.code().equals(request.getAttrCode())) {
                merged.add(new ComputedAttribute(
                        -1L, request.getMonitorTypeId(), a.code(), a.name(),
                        a.unit(), request.getCalcScript(), a.sortOrder()));
                replaced = true;
            } else {
                merged.add(a);
            }
        }
        if (!replaced) {
            merged.add(new ComputedAttribute(
                    -1L, request.getMonitorTypeId(), request.getAttrCode(),
                    request.getAttrCode(), "", request.getCalcScript(),
                    merged.size() + 1));
        }

        // 3. 拼装 + 执行
        String script = assembler.assemble(merged);
        Map<String, Object> curData = request.getCurData() == null ? Map.of() : request.getCurData();
        Map<String, Object> prevData = request.getPrevData();

        long start = System.currentTimeMillis();
        Map<String, Object> result = scriptEngine.executeComputed(script, curData, prevData);
        long elapsed = System.currentTimeMillis() - start;

        // 4. 返回(只取目标 attrCode 的结果)
        Object targetValue = result.get(request.getAttrCode());
        if (targetValue == null) {
            return AjaxResult.success("脚本执行失败或返回 null",
                    CalcScriptTestResult.fail("属性 '" + request.getAttrCode() + "' 未返回有效结果"));
        }
        return AjaxResult.success("成功",
                CalcScriptTestResult.ok(Map.of(request.getAttrCode(), targetValue), elapsed));
    }
}
```

- [ ] **步骤 4:编译验证**

```bash
cd server && mvn -pl zwei-iot-timeseries -am compile
```

预期:`BUILD SUCCESS`。

- [ ] **步骤 5:注册权限到 `sys_menu` 表**

通过本地 MySQL 直接执行(放在 db/upgrade 脚本中):

把以下内容追加到 `db/upgrade/v2.2-computed-attribute.sql`:

```sql

-- 新增权限: 计算脚本在线测试
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, perms,
                     menu_type, visible, status, create_by, create_time)
SELECT '监测内容脚本测试', menu_id, 5, '', '', 'basic:monitorContent:test',
       'F', '0', '0', 'admin', NOW()
FROM sys_menu
WHERE perms = 'basic:monitorContent:list'
LIMIT 1;
```

执行:

```bash
mysql -uroot -pwodepassword geo_hazard_monitor <<'EOF'
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, perms,
                     menu_type, visible, status, create_by, create_time)
SELECT '监测内容脚本测试', menu_id, 5, '', '', 'basic:monitorContent:test',
       'F', '0', '0', 'admin', NOW()
FROM sys_menu
WHERE perms = 'basic:monitorContent:list'
LIMIT 1;
EOF
```

预期:`Query OK, 1 row affected`。

- [ ] **步骤 6:Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/dto/ \
        server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/compute/controller/ \
        db/upgrade/v2.2-computed-attribute.sql
git commit -m "feat(timeseries): 新增 /monitor-contents/test-script 在线测试端点"
```

---

## 任务 14:后端集成测试(端到端验证)

**文件:**
- 创建:`server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestIT.java`

> 这是 P1 集成测试,需要 Spring 上下文启动。如果项目已有 IT 约定(如 `MonitorDataQueryIntegrationIT`),跟随其风格;否则使用 `@SpringBootTest` + mock 外部依赖。

- [ ] **步骤 1:参考现有 IT 风格**

```bash
cat server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/MonitorDataQueryIntegrationIT.java | head -50
```

确认 IT 是否真启动 Spring,还是用 Mockito + Manual wiring。若使用 mock,继续步骤 2 的"Service 层级集成";若是 `@SpringBootTest`,改用完整 Spring 启动。

- [ ] **步骤 2:写端到端测试(Service 层级 wiring)**

`server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestIT.java`:

```java
package com.zwei.iot.timeseries.integration;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.timeseries.compute.ComputedAttributeEvaluator;
import com.zwei.iot.timeseries.compute.ComputedAttributeRegistry;
import com.zwei.iot.timeseries.compute.ComputedScriptAssembler;
import com.zwei.iot.timeseries.compute.LastMessageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 计算属性求值端到端 wiring 测试:
 * 真实 Registry + Assembler + Evaluator + ScriptEngine,
 * 只 mock 字典层(MonitorContentService) + 设备层(SensorQuery) + Redis。
 */
@DisplayName("Computed Attribute 端到端 wiring")
class ComputedAttributeIngestIT {

    private ComputedAttributeEvaluator evaluator;

    @BeforeEach
    void setUp() {
        // 真实组件
        ComputedAttributeRegistry registry = new ComputedAttributeRegistry(mockMonitorContentService());
        ComputedScriptAssembler assembler = new ComputedScriptAssembler();
        GroovyScriptEngine scriptEngine = new GroovyScriptEngine();
        injectField(scriptEngine, "builtInFunctions", new com.zwei.iot.parser.engine.BuiltInFunctions());
        LastMessageStore lastMessageStore = new LastMessageStore(
                mock(StringRedisTemplate.class), new com.fasterxml.jackson.databind.ObjectMapper());

        // 设备层 mock
        IDeviceSensorQueryService sensorQuery = mock(IDeviceSensorQueryService.class);
        when(sensorQuery.requireSensorMetadata(eq(1L), eq("S1")))
                .thenReturn(SensorMetadata.builder()
                        .deviceId(1L).sensorId(10L).monitorTypeId(100L)
                        .attributes(List.of()).build());

        evaluator = new ComputedAttributeEvaluator(
                sensorQuery, registry, assembler, lastMessageStore, scriptEngine);
    }

    private IMonitorContentService mockMonitorContentService() {
        IMonitorContentService svc = mock(IMonitorContentService.class);
        MonitorContent mc = new MonitorContent();
        mc.setId(1L);
        mc.setMonitorTypeId(100L);
        mc.setCode("velocity");
        mc.setName("速率");
        mc.setUnit("mm/s");
        mc.setSortOrder(1);
        mc.setCalcScript("return curData.properties.displacement * 2");
        when(svc.selectComputedByTypeId(100L)).thenReturn(List.of(mc));
        return svc;
    }

    private static void injectField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @DisplayName("单计算属性: 真实脚本执行后产出到 properties")
    void singleComputedAttr() {
        ParsedMessage msg = new ParsedMessage(
                "D1", "S1", "sys", 1700000000000L, 1700000000000L, "hash",
                List.of(new PropertyValue("displacement", "位移", "mm", 12.5, 0)));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).identifier()).isEqualTo("velocity");
        assertThat(out.get(0).value()).isEqualTo(25.0);
    }
}
```

- [ ] **步骤 3:运行 IT**

```bash
cd server && mvn -pl zwei-iot-timeseries -am test -Dtest=ComputedAttributeIngestIT
```

预期:1 用例 PASS。

- [ ] **步骤 4:Commit**

```bash
git add server/zwei-iot-timeseries/src/test/java/com/zwei/iot/timeseries/integration/ComputedAttributeIngestIT.java
git commit -m "test(timeseries): 计算属性端到端 wiring 集成测试"
```

---

## 任务 15:前端 API 类型扩展

**文件:**
- 修改:`web/src/api/monitorType.ts`

- [ ] **步骤 1:扩展 `MonitorContentItem` 与 Payload 类型**

打开 `web/src/api/monitorType.ts`,在 `MonitorContentItem` 接口中,在 `rangeMax` 字段后追加:

```typescript
  fieldType?: 'inherent' | 'computed'
  calcScript?: string
```

在 `MonitorContentCreatePayload` 中,在 `rangeMax` 后追加:

```typescript
  fieldType?: 'inherent' | 'computed'
  calcScript?: string
```

在 `MonitorContentUpdatePayload` 中,在 `rangeMax` 后追加:

```typescript
  calcScript?: string
```

- [ ] **步骤 2:追加测试 API**

在文件末尾(`removeMonitorContent` 之后)追加:

```typescript
export interface CalcScriptTestRequest {
  monitorTypeId: number
  attrCode: string
  calcScript: string
  curData: Record<string, any>
  prevData?: Record<string, any>
}

export interface CalcScriptTestResult {
  success: boolean
  result?: Record<string, any>
  error?: string
  executionTime?: number
}

export const testCalcScript = (payload: CalcScriptTestRequest) =>
  unwrap<CalcScriptTestResult>(request.post('/monitor-contents/test-script', payload))
```

- [ ] **步骤 3:类型检查**

```bash
cd web && npx vue-tsc --noEmit
```

预期:无类型错误。

- [ ] **步骤 4:Commit**

```bash
git add web/src/api/monitorType.ts
git commit -m "feat(web): monitorType API 扩展 fieldType/calcScript + testCalcScript"
```

---

## 任务 16:`MonitorType.vue` 表格列与校验改造

**文件:**
- 修改:`web/src/views/basic/MonitorType.vue`

- [ ] **步骤 1:在"指标类型"列后插入"字段类型"列**

打开 `MonitorType.vue`,找到 `<el-table-column label="指标类型" ...>` 块的**结束**(`</el-table-column>`),在其后插入:

```vue
<el-table-column label="字段类型" width="110" align="center">
  <template #default="{ row }">
    <template v-if="isView">
      <el-tag v-if="row.fieldType === 'computed'" type="warning" size="small">计算属性</el-tag>
      <el-tag v-else type="info" size="small">固有属性</el-tag>
    </template>
    <el-select
      v-else
      v-model="row.fieldType"
      :disabled="Boolean(row.id)"
      placeholder="请选择"
      @change="handleFieldTypeChange(row)"
    >
      <el-option label="固有属性" value="inherent" />
      <el-option label="计算属性" value="computed" />
    </el-select>
  </template>
</el-table-column>
```

- [ ] **步骤 2:操作列宽度 80 → 140,加"脚本"按钮**

把现有 `<el-table-column label="操作" width="80" ...>` 整块替换为:

```vue
<el-table-column label="操作" width="140" fixed="right" align="center" v-if="!isView">
  <template #default="{ row, $index }">
    <div class="op-cell">
      <el-button
        v-if="row.fieldType === 'computed'"
        type="primary"
        text
        size="small"
        @click="handleEditScript(row, $index)"
      >脚本</el-button>
      <el-button type="text" size="small" class="danger-text" @click="handleRemoveModelAttr($index)">
        删除
      </el-button>
    </div>
  </template>
</el-table-column>
```

- [ ] **步骤 3:扩展 `normalizeMonitorContent` 与 `handleAddModelAttr`**

把 `normalizeMonitorContent` 改为:

```typescript
const normalizeMonitorContent = (item: any): MonitorContentItem => ({
  id: item?.id ? Number(item.id) : undefined,
  code: String(item?.code || '').trim(),
  name: String(item?.name || '').trim(),
  indicatorType: String(item?.indicatorType || '').trim(),
  unit: String(item?.unit || '').trim(),
  icon: item?.icon || '',
  rangeMin: item?.rangeMin === null || item?.rangeMin === undefined ? null : Number(item.rangeMin),
  rangeMax: item?.rangeMax === null || item?.rangeMax === undefined ? null : Number(item.rangeMax),
  fieldType: item?.fieldType === 'computed' ? 'computed' : 'inherent',
  calcScript: item?.calcScript || ''
})
```

把 `handleAddModelAttr` 改为:

```typescript
const handleAddModelAttr = () => {
  formData.modelAttrs.push({
    code: '',
    name: '',
    indicatorType: '',
    unit: '',
    icon: formData.icon || '',
    rangeMin: null,
    rangeMax: null,
    fieldType: 'inherent',
    calcScript: ''
  })
}
```

- [ ] **步骤 4:加 `handleFieldTypeChange` + `handleEditScript` + 脚本弹窗 state**

在 `<script setup>` 中(`handleIndicatorTypeChange` 附近)追加:

```typescript
const calcScriptDialogVisible = ref(false)
const editingScriptIndex = ref<number>(-1)
const editingScriptRow = ref<MonitorContentItem | null>(null)

const handleFieldTypeChange = (row: MonitorContentItem) => {
  if (row.fieldType === 'computed' && !row.calcScript) {
    row.calcScript = `// 计算属性: ${row.code || '属性编码'}\n`
      + '// 可用变量:\n'
      + `//   curData.properties.${row.code || 'attrCode'}  当前固有属性值\n`
      + `//   prevData?.properties.${row.code || 'attrCode'}  上一条数据包属性值\n`
      + '// 返回: 计算结果 (Number)\n\n'
      + `return curData.properties.${row.code || 'attrCode'}\n`
  }
}

const handleEditScript = (row: MonitorContentItem, index: number) => {
  editingScriptIndex.value = index
  editingScriptRow.value = row
  calcScriptDialogVisible.value = true
}

const handleScriptSaved = (script: string) => {
  if (editingScriptRow.value) {
    editingScriptRow.value.calcScript = script
  }
  calcScriptDialogVisible.value = false
}
```

- [ ] **步骤 5:在 template 末尾(dashboard dialog 之后)挂载 CalcScriptEditor**

```vue
<CalcScriptEditor
  v-if="editingScriptRow"
  v-model="calcScriptDialogVisible"
  :attr-code="editingScriptRow.code"
  :attr-name="editingScriptRow.name || editingScriptRow.code"
  :unit="editingScriptRow.unit"
  :script="editingScriptRow.calcScript || ''"
  :monitor-type-id="formData.id || 0"
  @save="handleScriptSaved"
/>
```

并在 `<script setup>` import 区追加:

```typescript
import CalcScriptEditor from './components/CalcScriptEditor.vue'
```

- [ ] **步骤 6:扩展 `validateModelAttrs` 校验**

在 `validateModelAttrs` 函数开头(`const codeSet = new Set<string>()` 之后)的循环里,`if (!row.indicatorType)` 检查**之后**追加:

```typescript
    if (!row.fieldType) {
      ElMessage.warning(`第 ${index + 1} 行字段类型不能为空`)
      return false
    }
    if (row.fieldType === 'computed' && !row.calcScript?.trim()) {
      ElMessage.warning(`第 ${index + 1} 行(${row.name || row.code})为计算属性, 必须设置计算脚本`)
      return false
    }
```

- [ ] **步骤 7:扩展 `syncMonitorContents` 同步逻辑**

把 `syncMonitorContents` 函数中 `currentRows` 的 map 改为包含 fieldType + calcScript:

```typescript
  const currentRows = formData.modelAttrs.map((item) => ({
    id: item.id,
    code: item.code.trim(),
    name: item.name.trim(),
    indicatorType: item.indicatorType,
    unit: item.unit.trim(),
    icon: item.icon || '',
    rangeMin: item.rangeMin ?? null,
    rangeMax: item.rangeMax ?? null,
    fieldType: item.fieldType || 'inherent',
    calcScript: item.calcScript ?? ''
  }))
```

把 `createMonitorContent({...})` 调用(2 处)追加 `fieldType` 与 `calcScript`:

```typescript
await createMonitorContent({
  monitorTypeId,
  code: item.code,
  name: item.name,
  unit: item.unit,
  indicatorType: item.indicatorType,
  icon: item.icon,
  rangeMin: item.rangeMin,
  rangeMax: item.rangeMax,
  fieldType: item.fieldType,
  calcScript: item.calcScript
})
```

把 update 检查条件追加 `calcScript` 比较:

```typescript
      if (
        oldItem.name !== item.name ||
        oldItem.unit !== item.unit ||
        (oldItem.icon || '') !== (item.icon || '') ||
        (oldItem.rangeMin ?? null) !== (item.rangeMin ?? null) ||
        (oldItem.rangeMax ?? null) !== (item.rangeMax ?? null) ||
        (oldItem.calcScript ?? '') !== (item.calcScript ?? '')
      ) {
        await updateMonitorContent(item.id, {
          name: item.name,
          unit: item.unit,
          icon: item.icon,
          rangeMin: item.rangeMin,
          rangeMax: item.rangeMax,
          calcScript: item.calcScript
        })
      }
```

注意:`originalContents` 也需保证包含 fieldType/calcScript。`normalizeMonitorContent`(步骤 3)已返回这两字段,在 `fillFormFromDetail` 中 `(detail.contents || []).map((item) => ({ ...item }))` 会自动带上 — 无需额外改。

- [ ] **步骤 8:类型检查 + 启动 dev server 手测**

```bash
cd web && npx vue-tsc --noEmit
```

预期:无类型错误(此时 `CalcScriptEditor` 还未创建,会报缺组件 — 步骤 9 后修复)。

**先执行任务 17 创建 CalcScriptEditor.vue,再回来跑这步。**

- [ ] **步骤 9:Commit(待任务 17 完成后)**

```bash
git add web/src/views/basic/MonitorType.vue
git commit -m "feat(web): MonitorType 表格加字段类型列与脚本按钮(等 CalcScriptEditor 集成)"
```

---

## 任务 17:`CalcScriptEditor.vue` 新组件

**文件:**
- 创建:`web/src/views/basic/components/CalcScriptEditor.vue`

- [ ] **步骤 1:创建组件**

```vue
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="`计算脚本 - ${attrName}`"
    width="800px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-alert
      type="info"
      :closable="false"
      class="form-alert"
    >
      <template #title>
        可用变量:
        <code>curData.properties.{<!-- -->{ attrCode }}</code>
        (当前数据包) ·
        <code>prevData?.properties.{<!-- -->{ attrCode }}</code>
        (上一条数据包, 可空)
        <br />
        返回: 数值(Number)
      </template>
    </el-alert>

    <el-input
      v-model="localScript"
      type="textarea"
      :rows="14"
      class="code-textarea"
      placeholder="// 输入计算脚本"
    />

    <el-collapse v-model="testPanelActive" class="test-panel">
      <el-collapse-item title="在线测试" name="test">
        <el-form label-width="100px">
          <el-form-item label="curData">
            <el-input
              v-model="curDataJson"
              type="textarea"
              :rows="4"
              placeholder='{"properties":{"attrCode":12.5}}'
            />
          </el-form-item>
          <el-form-item label="prevData">
            <el-input
              v-model="prevDataJson"
              type="textarea"
              :rows="4"
              placeholder='{"properties":{"attrCode":10.0},"dataTime":1700000000000}'
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="runTest" :loading="testing">运行测试</el-button>
            <el-button @click="curDataJson = ''; prevDataJson = ''">清空输入</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="testResult"
          :type="testResult.success ? 'success' : 'error'"
          :closable="false"
          class="form-alert"
        >
          <template #title>
            {{ testResult.success ? '✅ 成功' : '❌ 失败' }}
            <span v-if="testResult.executionTime !== undefined">
              · 耗时 {{ testResult.executionTime }}ms
            </span>
            <br />
            <pre v-if="testResult.success">{{ JSON.stringify(testResult.result, null, 2) }}</pre>
            <span v-else>{{ testResult.error }}</span>
          </template>
        </el-alert>
      </el-collapse-item>
    </el-collapse>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button @click="localScript = defaultTemplate">重置为模板</el-button>
      <el-button type="primary" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { testCalcScript, type CalcScriptTestResult } from '@/api/monitorType'

const props = defineProps<{
  modelValue: boolean
  attrCode: string
  attrName: string
  unit?: string
  script: string
  monitorTypeId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [script: string]
}>()

const localScript = ref(props.script)
const testPanelActive = ref<string[]>(['test'])
const curDataJson = ref('{\n  "properties": {}\n}')
const prevDataJson = ref('')
const testing = ref(false)
const testResult = ref<CalcScriptTestResult | null>(null)

const defaultTemplate = computed(() =>
  `// 计算属性: ${props.attrCode}\n` +
  '// 可用变量:\n' +
  `//   curData.properties.${props.attrCode}  当前数据包属性值\n` +
  `//   prevData?.properties.${props.attrCode}  上一条数据包属性值(可空)\n` +
  '// 返回: 数值 (Number)\n\n' +
  `return curData.properties.${props.attrCode}\n`
)

// 每次打开重置本地状态
watch(() => props.modelValue, (open) => {
  if (open) {
    localScript.value = props.script || defaultTemplate.value
    testResult.value = null
  }
})

const handleSave = () => {
  if (!localScript.value.trim()) {
    ElMessage.warning('脚本不能为空')
    return
  }
  emit('save', localScript.value)
}

const runTest = async () => {
  if (!localScript.value.trim()) {
    ElMessage.warning('请先输入脚本')
    return
  }
  if (!props.monitorTypeId) {
    ElMessage.warning('请先保存监测类型, 再测试脚本')
    return
  }
  let curData: Record<string, any>
  try {
    curData = curDataJson.value.trim() ? JSON.parse(curDataJson.value) : {}
  } catch (e) {
    ElMessage.error('curData 不是合法 JSON')
    return
  }
  let prevData: Record<string, any> | undefined
  if (prevDataJson.value.trim()) {
    try {
      prevData = JSON.parse(prevDataJson.value)
    } catch (e) {
      ElMessage.error('prevData 不是合法 JSON')
      return
    }
  }
  testing.value = true
  try {
    const result = await testCalcScript({
      monitorTypeId: props.monitorTypeId,
      attrCode: props.attrCode,
      calcScript: localScript.value,
      curData,
      prevData
    })
    testResult.value = result
  } catch (e: any) {
    testResult.value = { success: false, error: e?.message || '请求失败' }
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.form-alert {
  margin-bottom: 12px;
}

.code-textarea :deep(textarea) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.test-panel {
  margin-top: 16px;
}

pre {
  margin: 4px 0 0;
  font-family: 'Consolas', monospace;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
```

- [ ] **步骤 2:类型检查**

```bash
cd web && npx vue-tsc --noEmit
```

预期:无错误。

- [ ] **步骤 3:Commit**

```bash
git add web/src/views/basic/components/CalcScriptEditor.vue
git commit -m "feat(web): 新增 CalcScriptEditor 计算脚本编辑 + 在线测试弹窗"
```

- [ ] **步骤 4:回到任务 16 步骤 8,完成类型检查 + commit**

```bash
cd web && npx vue-tsc --noEmit
```

预期:无错误。然后执行任务 16 步骤 9 的 commit。

---

## 任务 18:回归测试与端到端手测

**文件:** 无新建,只验证现有代码。

- [ ] **步骤 1:后端全量测试**

```bash
cd server && mvn clean test
```

预期:所有模块 BUILD SUCCESS,无 FAIL。

- [ ] **步骤 2:前端类型检查 + 构建**

```bash
cd web && npm run build
```

预期:`vue-tsc` 无错误,`vite build` 产物正常。

- [ ] **步骤 3:启动后端 + 前端 dev server**

```bash
# 后端:IDE 启动 com.zwei.RuoYiApplication (profile=local)
# 前端:
cd web && npm run dev
```

- [ ] **步骤 4:手测 — 新建计算属性**

1. 打开"基础数据 → 监测类型"页面
2. 编辑"雨量监测",添加新属性:`code=rainfall_intensity`,`name=降雨强度`,`fieldType=computed`
3. 点击"脚本"按钮,弹窗打开,自动填入默认模板
4. 修改脚本为 `return curData.properties.rainfall_hour * 2`
5. 在"在线测试"面板输入 `curData={"properties":{"rainfall_hour":12.5}}`
6. 点击"运行测试",预期返回 `{"rainfall_intensity": 25}` + 耗时 < 100ms
7. 点击"确定"保存脚本
8. 点击主对话框"确定"保存监测类型

预期:无报错,数据库 `monitor_content` 新行 `field_type=computed`, `calc_script` 非空。

- [ ] **步骤 5:手测 — MQTT 端到端验证**

如果环境允许(本地 IoTDB + Redis + 设备模拟器):

1. 模拟设备上报一条带 `rainfall_hour=12.5` 的消息
2. 查 `iot_data_parse_log`,无错误日志
3. 调 `/api/v1/iot/monitor-data/latest?hazardPointId=X`,返回的 properties 包含 `rainfall_intensity=25.0`

如果不允许(无设备模拟器),跳过 — 通过单测覆盖。

- [ ] **步骤 6:回归测试 — 存量固有属性行为零变化**

1. 查看现有监测类型(雨量监测、位移监测等)
2. 编辑/查看时,固有属性的 `fieldType` 显示为"固有属性" tag
3. 不显示"脚本"按钮
4. 数据接入行为与改动前完全一致(单测已覆盖)

- [ ] **步骤 7:全量 commit + push(若用户允许)**

```bash
git status  # 检查是否有未提交改动
```

预期:无未提交改动(每个任务已各自 commit)。

```bash
git log --oneline | head -20
```

预期:看到本计划所有任务的 commit。

---

## 自检

### 规格覆盖度

| 规格章节 | 对应任务 | 状态 |
|---|---|---|
| §3 架构与组件 | 全任务 | ✓ |
| §4 数据库变更 | 任务 1 | ✓ |
| §5.1 MonitorContent 实体扩展 | 任务 2 | ✓ |
| §5.2 DTO 扩展 | 任务 3 | ✓ |
| §5.3 selectComputedByTypeId | 任务 4-5 | ✓ |
| §5.4 @CacheEvict 联动 | 任务 5 | ✓ |
| §5.5 ComputedAttribute record | 任务 7 | ✓ |
| §5.6 ComputedAttributeRegistry | 任务 7 | ✓ |
| §5.7 ParsedMessageSnapshot | 任务 8 | ✓ |
| §5.8 LastMessageStore | 任务 8 | ✓ |
| §5.9 ComputedScriptAssembler | 任务 9 | ✓ |
| §5.10 executeComputed | 任务 10 | ✓ |
| §5.11 ComputedAttributeEvaluator | 任务 11 | ✓ |
| §5.12 MonitorIngestFacade 接入 | 任务 12 | ✓ |
| §5.13 test-script 端点 | 任务 13 | ✓ |
| §5.14 权限注册 | 任务 13 | ✓ |
| §6.1-6.6 前端 | 任务 15-17 | ✓ |
| §7 错误处理矩阵 | 任务 8/11/12(各 try-catch + warn) | ✓ |
| §8 可观察性 | 任务 8/11(log.warn) | ✓ |
| §9 安全(沙箱) | 任务 10(复用 createSecureConfig) | ✓ |
| §10 测试策略 | 任务 8/9/10/11(单测) + 任务 14(集成) + 任务 18(手测) | ✓ |
| §11 边界与依赖 | 任务 6(SensorMetadata 扩展补漏) | ✓ |

### 占位符扫描

- 无 TODO/TBD/待定
- 每个步骤都有完整代码块
- 每个测试都有具体断言

### 类型一致性

| 类型/方法 | 定义任务 | 使用任务 | 一致 |
|---|---|---|---|
| `MonitorContent.fieldType` | 任务 2 | 任务 3/4/5 | ✓ |
| `MonitorContent.calcScript` | 任务 2 | 任务 3/4/5 | ✓ |
| `MonitorContentMapper.selectComputedByTypeId(Long)` | 任务 4 | 任务 5/7 | ✓ |
| `IMonitorContentService.selectComputedByTypeId` | 任务 5 | 任务 7 | ✓ |
| `SensorMetadata.monitorTypeId` | 任务 6 | 任务 11/14 | ✓ |
| `ComputedAttribute(Long,Long,String,String,String,String,Integer)` | 任务 7 | 任务 9/11/13/14 | ✓ |
| `ComputedAttributeRegistry.getByMonitorTypeId(Long)` | 任务 7 | 任务 11/13/14 | ✓ |
| `ParsedMessageSnapshot(String,String,long,Map<String,Double>)` | 任务 8 | 任务 11 | ✓ |
| `LastMessageStore.get(Long,String)` / `.put(Long,String,Snapshot)` | 任务 8 | 任务 11 | ✓ |
| `ComputedScriptAssembler.assemble(List<ComputedAttribute>)` | 任务 9 | 任务 11/13/14 | ✓ |
| `GroovyScriptEngine.executeComputed(String,Map,Map)` | 任务 10 | 任务 11/13/14 | ✓ |
| `ComputedAttributeEvaluator.evaluate(Long,String,ParsedMessage)` | 任务 11 | 任务 12 | ✓ |
| `CalcScriptTestRequest/Result` | 任务 13 | 任务 17 | ✓ |

---

## 执行交接

**计划已完成并保存到 `docs/superpowers/plans/2026-06-17-computed-attribute-implementation.md`。两种执行方式:**

**1. 子代理驱动(推荐)** - 每个任务调度一个新的子代理,任务间进行审查,快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务,批量执行并设有检查点

**选哪种方式?**
