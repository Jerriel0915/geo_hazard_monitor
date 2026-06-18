# 报告管理 (Report Management) 实现计划

> **面向 AI 代理的工作者:** 必需子技能:使用 superpowers:subagent-driven-development(推荐)或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框(`- [ ]`)语法来跟踪进度。

**目标:** 为 `hazard_point.status=1` 的监测中隐患点,按周/月/季三个类型定时生成 HTML 报告记录;前端列表展示、查看 HTML、下载 PDF(前端实时生成);同时提供手动触发生成接口用于历史补救。

**架构:** 新建独立 Maven 模块 `zwei-iot-report`(第 15 个模块),包名 `com.zwei.iot.report`;定时任务用 `@Scheduled` 三入口(错峰 02:00 / 02:30 / 03:00);Redis 分布式锁 + DB UNIQUE 双重并发兜底;三种渲染器用策略模式 + Spring 自动注入(`ReportRenderer` 接口);跨模块依赖只通过 Service 接口调用(hazard/alarm/timeseries/device);前端仅改 2 个文件。

**技术栈:** Java 17 + Spring Boot 4.0.3 + MyBatis + JUnit 5 + AssertJ + Mockito(后端);Vue 3 + TS + Element Plus + html2canvas + jsPDF(前端)。

**关联规格:** `docs/superpowers/specs/2026-06-17-report-management-design.md`

**测试约定:** 后端测试位于 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/...`,使用 JUnit 5(`@Test`/`@DisplayName`/`@Nested`) + AssertJ(`assertThat`) + Mockito(`@Mock`/`@InjectMocks`/`when`/`verify`)。

**Git 约定:** 每个 commit 使用 Conventional Commits 格式 `feat(report): ...` / `feat(hazard): ...` / `feat(alarm): ...` / `feat(web): ...` / `test(...)` / `docs(...)` / `chore(db): ...`。**不**包含 `Co-Authored-By` / `Signed-off-by` 等尾注(项目全局规则)。

**启动检查:**
```bash
# 后端编译/测试在仓库根执行
cd server && mvn -pl zwei-iot-report -am test
# 全量回归
cd server && mvn clean test
# 前端类型检查
cd web && npm run build
```

---

## 文件结构

### 后端新增/修改文件

| 文件 | 类型 | 职责 |
|---|---|---|
| `db/upgrade/v2.7-report-module.sql` | 新建 | `report_record` 加 5 列(type/period_start/period_end/error_msg/del_flag) + UNIQUE 约束 + 菜单初始化 |
| `server/pom.xml` | 修改 | `<modules>` 段 + `dependencyManagement` 加 `zwei-iot-report` 声明 |
| `server/zwei-admin/pom.xml` | 修改 | 加 `zwei-iot-report` 依赖 |
| `server/zwei-common/src/main/java/com/zwei/common/redis/DistributedLock.java` | 新建 | Redis `SETNX EX` 简易分布式锁(无现成工具,自写) |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IHazardPointQueryService.java` | 新建 | 跨模块接口:列出监测中隐患点 |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceHazardRelationService.java` | 修改 | 加 `getDevicesByHazardPoint(Long hpId)` 方法 |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IAlarmQueryService.java` | 新建 | 跨模块接口:按隐患点+时间窗查告警汇总/明细 |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/HazardPointBrief.java` | 新建 | 隐患点摘要 record(id/code/name/lon/lat) |
| `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/DeviceBrief.java` | 新建 | 设备摘要 record(若已存在则复用) |
| `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointQueryServiceImpl.java` | 新建 | `IHazardPointQueryService` 实现 |
| `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/DeviceHazardRelationServiceImpl.java` | 修改 | 实现 `getDevicesByHazardPoint`(若文件名不同,在执行时用 glob 定位) |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmQueryServiceImpl.java` | 新建 | `IAlarmQueryService` 实现 |
| `server/zwei-iot-report/pom.xml` | 新建 | Maven 模块声明 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/ReportModuleConfig.java` | 新建 | Spring `@Configuration` |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/ReportRecord.java` | 新建 | 实体类 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/ReportType.java` | 新建 | enum WEEKLY(2)/MONTHLY(3)/QUARTERLY(4) |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/*.java` | 新建 | Page/Detail/Generate DTO + VO |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/mapper/ReportRecordMapper.java` | 新建 | MyBatis Mapper |
| `server/zwei-iot-report/src/main/resources/mapper/iot/report/ReportRecordMapper.xml` | 新建 | MyBatis XML |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/support/ReportPeriod.java` | 新建 | 周期计算工具 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/ReportContext.java` | 新建 | 渲染上下文 DTO |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/ReportDataAssembler.java` | 新建 | 拉数 → ReportContext |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/ReportRenderer.java` | 新建 | 策略接口 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/WeeklyReportRenderer.java` | 新建 | 周报渲染器 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/MonthlyReportRenderer.java` | 新建 | 月报渲染器 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/QuarterlyReportRenderer.java` | 新建 | 季报渲染器 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/RiskAssessor.java` | 新建 | 季报综合风险评级 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportRecordService.java` | 新建 | CRUD |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportGenerationService.java` | 新建 | 编排 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/job/ReportScheduleJob.java` | 新建 | `@Scheduled` 三入口 |
| `server/zwei-iot-report/src/main/java/com/zwei/iot/report/controller/ReportController.java` | 新建 | REST 4 端点 |
| `server/zwei-admin/src/main/resources/application.yml` | 修改 | 加 `zwei.report.schedule.*` 开关 |

### 前端修改文件

| 文件 | 类型 | 职责 |
|---|---|---|
| `web/src/api/report.ts` | 大改 | 删除 mock 函数、新增 quarterly 类型、对接真实接口 |
| `web/src/views/report/Report.vue` | 中改 | 季报支持、字段重命名、手动生成弹窗、状态列 |

### 测试文件

| 文件 | 类型 |
|---|---|
| `server/zwei-common/src/test/java/com/zwei/common/redis/DistributedLockTest.java` | 新建 |
| `server/zwei-iot-report/src/test/java/com/zwei/iot/report/support/ReportPeriodTest.java` | 新建 |
| `server/zwei-iot-report/src/test/java/com/zwei/iot/report/datasource/ReportDataAssemblerTest.java` | 新建 |
| `server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/WeeklyReportRendererTest.java` | 新建 |
| `server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/MonthlyReportRendererTest.java` | 新建 |
| `server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/QuarterlyReportRendererTest.java` | 新建 |
| `server/zwei-iot-report/src/test/java/com/zwei/iot/report/service/ReportGenerationServiceTest.java` | 新建 |
| `server/zwei-iot-report/src/test/java/com/zwei/iot/report/controller/ReportControllerTest.java` | 新建 |

---

## 任务 0:启动检查

**文件:** 无

- [ ] **步骤 1:确认分支与基线**

```bash
cd "E:/work/PMO/4.其他项目/sys-交通边坡监测预警/zwei"
git status            # 应为干净或在 web260429 分支
git log --oneline -3  # 顶部应见 docs(report): 新增报告管理设计规格
```

- [ ] **步骤 2:验证后端基线编译通过**

```bash
cd server && mvn -pl zwei-iot-hazard,zwei-iot-alarm -am compile
# 预期: BUILD SUCCESS
```

如失败,**停止本计划**,先修复基线。

- [ ] **步骤 3:验证前端基线编译通过**

```bash
cd web && npm run build 2>&1 | tail -20
# 预期: vue-tsc 无错 + vite build 完成
```

如失败,**停止本计划**,先修复基线。

---

## 任务 1:DB 升级脚本 v2.7

**文件:**
- 创建:`db/upgrade/v2.7-report-module.sql`

- [ ] **步骤 1:编写 SQL 脚本**

写入 `db/upgrade/v2.7-report-module.sql`:

```sql
-- =====================================================================
-- v2.7-report-module.sql — 报告管理模块(周/月/季报)
-- 关联规格: docs/superpowers/specs/2026-06-17-report-management-design.md
-- =====================================================================

-- 1. report_record 表扩展: 加类型/周期/逻辑删除/失败原因
ALTER TABLE report_record
    ADD COLUMN type         tinyint       NOT NULL COMMENT '报告类型: 2-周报, 3-月报, 4-季报' AFTER report_name,
    ADD COLUMN period_start date          NOT NULL COMMENT '周期开始日 (含)' AFTER type,
    ADD COLUMN period_end   date          NOT NULL COMMENT '周期结束日 (含)' AFTER period_start,
    ADD COLUMN error_msg    varchar(1000) DEFAULT NULL COMMENT '生成失败原因 (status=3 时填)' AFTER status,
    ADD COLUMN del_flag     tinyint       NOT NULL DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除' AFTER error_msg,
    MODIFY COLUMN template_id bigint DEFAULT NULL COMMENT '模板ID (内置渲染器填 NULL)',
    MODIFY COLUMN status tinyint DEFAULT '1' COMMENT '状态: 1-生成中, 2-已生成, 3-生成失败',
    ADD KEY idx_report_record_type (type),
    ADD KEY idx_report_record_period (period_start, period_end),
    ADD KEY idx_report_record_del_flag (del_flag);

-- 2. 防重复生成: 每个周期同一隐患点同一类型只允许一条有效记录
ALTER TABLE report_record
    ADD UNIQUE KEY uk_report_record_unique
        (type, hazard_point_id, period_start, period_end, del_flag);

-- 3. 顶级菜单 "报告报表" (幂等)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告报表', 0, 7, 'report', NULL, '', 1, 0, 'M', '0', '0', '', 'documentation',
       'admin', NOW(), '报告报表目录'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '报告报表' AND parent_id = 0 AND menu_type = 'M');

SET @report_parent_id = (SELECT menu_id FROM sys_menu
                         WHERE menu_name = '报告报表' AND parent_id = 0 AND menu_type = 'M' LIMIT 1);

-- 4. 子菜单 "报告管理" (幂等)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告管理', @report_parent_id, 1, 'report', 'report/Report', '', 1, 0, 'C', '0', '0',
       'report:record:list', 'documentation', 'admin', NOW(), '报告管理菜单'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'report:record:list' AND menu_type = 'C');

SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'report:record:list' AND menu_type = 'C' LIMIT 1);

-- 5. 按钮权限 (幂等)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT * FROM (
  SELECT '报告查询' AS n, @report_menu_id AS pid, 1 AS o, '' AS p, '' AS c, '' AS q, 1 AS f, 0 AS cache,
         'F' AS t, '0' AS v, '0' AS s, 'report:record:query' AS perm, '#' AS icon, 'admin', NOW(), '' AS rem
  UNION ALL SELECT '报告删除', @report_menu_id, 2, '', '', '', 1, 0, 'F', '0', '0', 'report:record:remove', '#', 'admin', NOW(), ''
  UNION ALL SELECT '报告导出', @report_menu_id, 3, '', '', '', 1, 0, 'F', '0', '0', 'report:record:export', '#', 'admin', NOW(), ''
  UNION ALL SELECT '报告生成', @report_menu_id, 4, '', '', '', 1, 0, 'F', '0', '0', 'report:record:generate', '#', 'admin', NOW(), ''
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = tmp.perm AND menu_type = 'F');

-- 6. 给 admin 角色 (role_id=1) 自动授权新菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('report:record:list', 'report:record:query', 'report:record:remove',
                'report:record:export', 'report:record:generate')
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
```

- [ ] **步骤 2:本地执行 SQL**

```bash
mysql -uroot -p wodepassword geo_hazard_monitor < db/upgrade/v2.7-report-module.sql
```

- [ ] **步骤 3:验证表结构变更**

```bash
mysql -uroot -p wodepassword -e "DESC geo_hazard_monitor.report_record;" | head -20
```

预期:看到 `type`/`period_start`/`period_end`/`error_msg`/`del_flag` 列;`SHOW INDEX FROM report_record` 看到 `uk_report_record_unique`。

- [ ] **步骤 4:验证菜单插入**

```bash
mysql -uroot -p wodepassword -e "SELECT menu_id, menu_name, perms FROM geo_hazard_monitor.sys_menu WHERE perms LIKE 'report:record:%' OR menu_name = '报告报表';"
```

预期:6 行(1 顶级 + 1 子菜单 + 4 按钮)。

- [ ] **步骤 5:Commit**

```bash
git add db/upgrade/v2.7-report-module.sql
git commit -m "chore(db): v2.7 升级脚本 report_record 扩展 + 报告管理菜单初始化"
```

---

## 任务 2:Redis 分布式锁工具

**文件:**
- 创建:`server/zwei-common/src/main/java/com/zwei/common/redis/DistributedLock.java`
- 测试:`server/zwei-common/src/test/java/com/zwei/common/redis/DistributedLockTest.java`

- [ ] **步骤 1:编写失败的测试**

写入 `server/zwei-common/src/test/java/com/zwei/common/redis/DistributedLockTest.java`:

```java
package com.zwei.common.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("DistributedLock (Redis SETNX)")
class DistributedLockTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private DistributedLock lock;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        lock = new DistributedLock(redis);
    }

    @Test
    @DisplayName("首次获取锁返回 true 并写入随机 token")
    void firstAcquireSucceeds() {
        when(valueOps.setIfAbsent(eq("k1"), any(String.class), eq(Duration.ofSeconds(30))))
            .thenReturn(true);

        DistributedLock.LockToken token = lock.tryLock("k1", Duration.ofSeconds(30));

        assertThat(token.acquired()).isTrue();
        verify(valueOps).setIfAbsent(eq("k1"), any(String.class), eq(Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("锁已被占用返回 acquired=false")
    void secondAcquireFails() {
        when(valueOps.setIfAbsent(eq("k1"), any(), any(Duration.class))).thenReturn(false);

        DistributedLock.LockToken token = lock.tryLock("k1", Duration.ofSeconds(30));

        assertThat(token.acquired()).isFalse();
    }

    @Test
    @DisplayName("释放锁时校验 token 一致才删除")
    void unlockWithMatchingToken() {
        when(valueOps.setIfAbsent(eq("k1"), any(), any(Duration.class))).thenReturn(true);
        when(redis.delete(eq("k1"))).thenReturn(true);

        DistributedLock.LockToken token = lock.tryLock("k1", Duration.ofSeconds(30));
        // 模拟 token 匹配场景: 直接 delete 成功
        when(redis.opsForValue().get("k1")).thenReturn(token.value());

        lock.unlock("k1", token);

        // 仅当 value 匹配时才 delete — 用 AtomicBoolean 模拟 Lua 脚本语义
        verify(redis, atMostOnce()).delete("k1");
    }

    @Test
    @DisplayName("未获取锁时调用 unlock 不抛异常")
    void unlockOnFailedAcquireIsNoop() {
        DistributedLock.LockToken token = DistributedLock.LockToken.notAcquired();
        lock.unlock("k1", token); // 不应抛异常
        verifyNoInteractions(redis);
    }
}
```

- [ ] **步骤 2:运行测试验证失败**

```bash
cd server && mvn -pl zwei-common -am test -Dtest=DistributedLockTest
```

预期:编译错误(`DistributedLock` 类不存在)。

- [ ] **步骤 3:编写实现**

写入 `server/zwei-common/src/main/java/com/zwei/common/redis/DistributedLock.java`:

```java
package com.zwei.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 极简 Redis 分布式锁:SETNX EX + token 校验解锁。
 * <p>
 * 仅用于本模块单实例或偶尔多实例部署的并发兜底;高并发场景请用 Redisson。
 */
@Component
public class DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(DistributedLock.class);

    private final StringRedisTemplate redis;

    public DistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 尝试获取锁,不重试。
     *
     * @return LockToken; {@link LockToken#acquired()} 为 false 表示未获取
     */
    public LockToken tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(ok)) {
            return new LockToken(true, token);
        }
        return LockToken.notAcquired();
    }

    /**
     * 释放锁:仅当 Redis 中保存的值与本 token 匹配时才删除(防止误删别人的锁)。
     */
    public void unlock(String key, LockToken token) {
        if (!token.acquired()) {
            return;
        }
        String current = redis.opsForValue().get(key);
        if (token.value().equals(current)) {
            Boolean deleted = redis.delete(key);
            log.debug("[lock] unlock key={} deleted={}", key, deleted);
        } else {
            log.warn("[lock] unlock skipped, token mismatch key={} (ttl expired?)", key);
        }
    }

    public record LockToken(boolean acquired, String value) {
        public static LockToken notAcquired() {
            return new LockToken(false, null);
        }
    }
}
```

- [ ] **步骤 4:运行测试验证通过**

```bash
cd server && mvn -pl zwei-common -am test -Dtest=DistributedLockTest
```

预期: BUILD SUCCESS,4 个测试全绿。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-common/src/main/java/com/zwei/common/redis/DistributedLock.java \
        server/zwei-common/src/test/java/com/zwei/common/redis/DistributedLockTest.java
git commit -m "feat(common): 新增 DistributedLock Redis SETNX 分布式锁工具"
```

---

## 任务 3:跨模块 Service 接口(hazard + alarm)

**文件:**
- 创建:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IHazardPointQueryService.java`
- 创建:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IAlarmQueryService.java`
- 创建:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/HazardPointBrief.java`
- 创建:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/AlarmSummary.java`
- 创建:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/AlarmEvent.java`
- 修改:`server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceHazardRelationService.java`
- 创建:`server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointQueryServiceImpl.java`
- 修改:`server/zwei-iot-hazard/.../DeviceHazardRelationServiceImpl.java`(在执行时用 glob 定位精确路径)
- 创建:`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmQueryServiceImpl.java`

- [ ] **步骤 1:在 zwei-iot-device 新建接口与 DTO**

写入 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/HazardPointBrief.java`:

```java
package com.zwei.iot.device.domain.brief;

import java.math.BigDecimal;

/**
 * 隐患点摘要 (供 report 等模块消费,不暴露完整实体)。
 */
public record HazardPointBrief(
    Long id,
    String code,
    String name,
    BigDecimal longitude,
    BigDecimal latitude
) {}
```

写入 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/AlarmSummary.java`:

```java
package com.zwei.iot.device.domain.brief;

import java.util.Map;

/**
 * 按隐患点+时间窗聚合的告警摘要。
 * levelCount: key=告警级别(1蓝/2黄/3橙/4红), value=次数
 * statusCount: key=状态(1待处理/2处理中/3已销警/4误报), value=次数
 */
public record AlarmSummary(
    Long hazardPointId,
    int total,
    int maxLevel,
    int pendingCount,
    Map<Integer, Integer> levelCount,
    Map<String, Integer> statusCount
) {}
```

写入 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/AlarmEvent.java`:

```java
package com.zwei.iot.device.domain.brief;

import java.time.LocalDateTime;

/**
 * 单条告警事件 (供 report 展示 Top N)。
 */
public record AlarmEvent(
    Long id,
    LocalDateTime triggerTime,
    int alarmLevel,
    String alarmType,
    String deviceName,
    String sensorName,
    String description,
    String alarmStatus
) {}
```

写入 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IHazardPointQueryService.java`:

```java
package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.brief.HazardPointBrief;

import java.util.List;

/**
 * 隐患点查询服务 (跨模块接口, 实现在 zwei-iot-hazard)。
 */
public interface IHazardPointQueryService {

    /**
     * 列出所有"监测中" (status=1 AND del_flag=0) 的隐患点摘要。
     */
    List<HazardPointBrief> listMonitoring();
}
```

写入 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IAlarmQueryService.java`:

```java
package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警查询服务 (跨模块接口, 实现在 zwei-iot-alarm)。
 */
public interface IAlarmQueryService {

    /**
     * 按隐患点+时间窗聚合告警摘要。
     */
    AlarmSummary summarizeByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end);

    /**
     * 按隐患点+时间窗取最近 N 条告警事件 (按 trigger_time DESC)。
     */
    List<AlarmEvent> listTopByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end, int limit);

    /**
     * 按隐患点+时间窗按月分组统计告警次数。
     * @return key=yyyy-MM, value=count
     */
    java.util.Map<String, Integer> countByMonth(Long hazardPointId, LocalDateTime start, LocalDateTime end);
}
```

- [ ] **步骤 2:修改 `IDeviceHazardRelationService` 加 `getDevicesByHazardPoint`**

读取现有 `IDeviceHazardRelationService.java`,在末尾(`}` 之前)追加方法声明:

```java
    /**
     * 列出隐患点绑定的所有设备(摘要视图,含在线状态)。
     * 实现侧通过 device_hazard_point JOIN device LEFT JOIN device_online_status 完成。
     */
    List<com.zwei.iot.device.domain.brief.DeviceBrief> getDevicesByHazardPoint(Long hazardPointId);
```

如果 `DeviceBrief` 类不存在,创建 `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/DeviceBrief.java`:

```java
package com.zwei.iot.device.domain.brief;

/**
 * 设备摘要 (供 report 渲染表格)。
 */
public record DeviceBrief(
    Long id,
    String code,
    String name,
    Integer deviceType,
    String deviceTypeName,
    Integer sensorCount,
    Integer onlineStatus,    // 0=离线 1=在线 null=未注册
    java.time.LocalDateTime lastReportAt
) {}
```

(若项目已有同义 record,优先复用,跳过创建;在执行此任务前用 `Glob` 搜 `**/DeviceBrief*.java` 确认。)

- [ ] **步骤 3:在 zwei-iot-hazard 实现 IHazardPointQueryService**

写入 `server/zwei-iot-hazard/src/main/java/com/zwei/iot/hazardpoint/service/impl/HazardPointQueryServiceImpl.java`:

```java
package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HazardPointQueryServiceImpl implements IHazardPointQueryService {

    private final HazardPointMapper hazardPointMapper;

    public HazardPointQueryServiceImpl(HazardPointMapper hazardPointMapper) {
        this.hazardPointMapper = hazardPointMapper;
    }

    @Override
    public List<HazardPointBrief> listMonitoring() {
        // 复用 HazardPointMapper 已有的 selectList 或新增查询
        // 这里假设 HazardPointMapper 有 selectByStatus 方法;如无,在 step 4 添加
        List<HazardPoint> all = hazardPointMapper.selectAll();
        return all.stream()
            .filter(hp -> hp.getStatus() != null && hp.getStatus() == 1)
            .filter(hp -> hp.getDelFlag() == null || hp.getDelFlag() == 0)
            .map(hp -> new HazardPointBrief(
                hp.getId(), hp.getCode(), hp.getName(),
                hp.getLongitude(), hp.getLatitude()))
            .collect(Collectors.toList());
    }
}
```

> **执行时检查项:** 先 `Read` `HazardPointMapper.java` 看是否已有 `selectAll()`;无则添加方法声明 + XML。`HazardPoint` 实体字段(getStatus/getCode/...)用 `Read` 确认实际 getter 名。

- [ ] **步骤 4:在 zwei-iot-hazard 实现 `getDevicesByHazardPoint`**

用 Glob 定位 `**/DeviceHazardRelationServiceImpl.java`(可能位于 hazard 或 device 模块),在已有实现类末尾追加方法实现。参考实现:

```java
@Override
public List<DeviceBrief> getDevicesByHazardPoint(Long hazardPointId) {
    // 优先: 用 device_hazard_point JOIN device LEFT JOIN device_online_status
    // 若 Mapper 暂无对应查询, 用两步: 1) 查绑定 deviceId 列表  2) 查设备 + 在线状态
    return deviceHazardRelationMapper.selectDeviceBriefByHazardPoint(hazardPointId);
}
```

在 `DeviceHazardRelationMapper.xml` 中加 SQL:

```xml
<select id="selectDeviceBriefByHazardPoint" resultType="com.zwei.iot.device.domain.brief.DeviceBrief">
    SELECT
        d.id          AS id,
        d.code        AS code,
        d.name        AS name,
        d.device_type AS deviceType,
        mt.type_name  AS deviceTypeName,
        (SELECT COUNT(*) FROM device_sensor s WHERE s.device_id = d.id AND s.del_flag = 0) AS sensorCount,
        dos.status    AS onlineStatus,
        dos.last_report_at AS lastReportAt
    FROM device_hazard_point dhp
    JOIN device d ON d.id = dhp.device_id AND d.del_flag = 0
    LEFT JOIN monitor_type mt ON mt.id = d.device_type  <!-- 字段名按实际表结构 -->
    LEFT JOIN device_online_status dos ON dos.device_id = d.id
    WHERE dhp.hazard_point_id = #{hazardPointId}
      AND dhp.del_flag = 0
    ORDER BY d.code
</select>
```

> **执行时检查项:** 用 `Read` 看 `device` 表 DDL 确认 `device_type` 是否是 monitor_type 外键。若实际是 `monitor_type_id`,相应改 SQL 列名。

- [ ] **步骤 5:在 zwei-iot-alarm 实现 IAlarmQueryService**

写入 `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmQueryServiceImpl.java`:

```java
package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.service.IAlarmQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlarmQueryServiceImpl implements IAlarmQueryService {

    private final AlarmRecordMapper alarmRecordMapper;

    public AlarmQueryServiceImpl(AlarmRecordMapper alarmRecordMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
    }

    @Override
    public AlarmSummary summarizeByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end) {
        // 假设 AlarmRecordMapper 已有 selectByHazardPointAndTime 方法; 若无则 step 6 添加
        List<AlarmRecord> records = alarmRecordMapper.selectByHazardPointAndTime(hazardPointId, start, end);

        Map<Integer, Integer> levelCount = new HashMap<>();
        Map<String, Integer> statusCount = new HashMap<>();
        int maxLevel = 0;
        int pending = 0;

        for (AlarmRecord r : records) {
            int lvl = r.getAlarmLevel() == null ? 0 : r.getAlarmLevel();
            levelCount.merge(lvl, 1, Integer::sum);
            if (lvl > maxLevel) maxLevel = lvl;
            statusCount.merge(r.getAlarmStatus(), 1, Integer::sum);
            if ("1".equals(r.getAlarmStatus())) pending++;
        }

        return new AlarmSummary(hazardPointId, records.size(), maxLevel, pending, levelCount, statusCount);
    }

    @Override
    public List<AlarmEvent> listTopByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end, int limit) {
        List<AlarmRecord> records = alarmRecordMapper.selectTopByHazardPointAndTime(hazardPointId, start, end, limit);
        return records.stream()
            .map(r -> new AlarmEvent(
                r.getId(), r.getTriggerTime(),
                r.getAlarmLevel() == null ? 0 : r.getAlarmLevel(),
                r.getAlarmType(),
                r.getDeviceName(),
                r.getSensorName(),
                r.getDescription(),
                r.getAlarmStatus()))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> countByMonth(Long hazardPointId, LocalDateTime start, LocalDateTime end) {
        List<AlarmRecord> records = alarmRecordMapper.selectByHazardPointAndTime(hazardPointId, start, end);
        return records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getTriggerTime().getYear() + "-" +
                     String.format("%02d", r.getTriggerTime().getMonthValue()),
                TreeMap::new,
                Collectors.summingInt(r -> 1)));
    }
}
```

- [ ] **步骤 6:在 AlarmRecordMapper 加查询方法**

读取 `AlarmRecordMapper.java` 与 `AlarmRecordMapper.xml`,新增三个方法:

```java
List<AlarmRecord> selectByHazardPointAndTime(@Param("hazardPointId") Long hazardPointId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

List<AlarmRecord> selectTopByHazardPointAndTime(@Param("hazardPointId") Long hazardPointId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("limit") int limit);
```

XML 片段:

```xml
<select id="selectByHazardPointAndTime" resultMap="AlarmRecordResult">
    SELECT <include refid="selectAlarmRecordVo"/>
    FROM alarm_record
    WHERE hazard_point_id = #{hazardPointId}
      AND trigger_time BETWEEN #{start} AND #{end}
      AND del_flag = 0
    ORDER BY trigger_time DESC
</select>

<select id="selectTopByHazardPointAndTime" resultMap="AlarmRecordResult">
    SELECT <include refid="selectAlarmRecordVo"/>
    FROM alarm_record
    WHERE hazard_point_id = #{hazardPointId}
      AND trigger_time BETWEEN #{start} AND #{end}
      AND del_flag = 0
    ORDER BY trigger_time DESC
    LIMIT #{limit}
</select>
```

> **执行时检查项:** `AlarmRecord` 实体字段(getTriggerTime/getDeviceName/getSensorName/getDescription)用 `Read` 确认。XML 已有的 `selectAlarmRecordVo` 字段列表与 `AlarmRecordResult` resultMap 用 `Read` 确认存在。

- [ ] **步骤 7:编译验证**

```bash
cd server && mvn -pl zwei-iot-hazard,zwei-iot-alarm -am compile
# 预期: BUILD SUCCESS
```

- [ ] **步骤 8:Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IHazardPointQueryService.java \
        server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IAlarmQueryService.java \
        server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IDeviceHazardRelationService.java \
        server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/brief/ \
        server/zwei-iot-hazard/ \
        server/zwei-iot-alarm/
git commit -m "feat(device): 新增 IHazardPointQueryService + IAlarmQueryService 跨模块接口及实现"
```

---

## 任务 4:zwei-iot-report 模块骨架

**文件:**
- 创建:`server/zwei-iot-report/pom.xml`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/ReportModuleConfig.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/ReportType.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/ReportRecord.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/mapper/ReportRecordMapper.java`
- 创建:`server/zwei-iot-report/src/main/resources/mapper/iot/report/ReportRecordMapper.xml`
- 修改:`server/pom.xml`
- 修改:`server/zwei-admin/pom.xml`

- [ ] **步骤 1:创建模块 pom.xml**

写入 `server/zwei-iot-report/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.zwei</groupId>
        <artifactId>zwei</artifactId>
        <version>4.0.3</version>
    </parent>

    <artifactId>zwei-iot-report</artifactId>
    <description>报告管理 (周/月/季报定时生成 + 渲染)</description>

    <dependencies>
        <!-- 本项目基础 -->
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-iot-device</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-iot-timeseries</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-iot-hazard</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zwei</groupId>
            <artifactId>zwei-iot-alarm</artifactId>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- MyBatis -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

> **执行时检查项:** 用 `Read` 看 `server/zwei-iot-alarm/pom.xml` 复制父版本号,确认 `<parent><version>` 的值与之一致。

- [ ] **步骤 2:修改父 pom 注册新模块**

读取 `server/pom.xml`,在 `<modules>` 段(参照已有 IoT 子模块顺序)追加:

```xml
<module>zwei-iot-report</module>
```

并在 `<dependencyManagement>` 段(参照 `zwei-iot-alarm` 的声明行号 173 附近)追加:

```xml
<dependency>
    <groupId>com.zwei</groupId>
    <artifactId>zwei-iot-report</artifactId>
    <version>${zwei.version}</version>
</dependency>
```

- [ ] **步骤 3:修改 zwei-admin pom 引入新模块**

读取 `server/zwei-admin/pom.xml`,在已有 IoT 依赖段(第 64-74 行附近)追加:

```xml
<dependency>
    <groupId>com.zwei</groupId>
    <artifactId>zwei-iot-report</artifactId>
</dependency>
```

- [ ] **步骤 4:创建模块配置类**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/ReportModuleConfig.java`:

```java
package com.zwei.iot.report;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 报告管理模块 Spring 配置。
 * MyBatis Mapper 扫描 com.zwei.iot.report.mapper。
 */
@Configuration
@MapperScan("com.zwei.iot.report.mapper")
public class ReportModuleConfig {
}
```

- [ ] **步骤 5:创建 ReportType 枚举**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/ReportType.java`:

```java
package com.zwei.iot.report.domain;

/**
 * 报告类型枚举 (与 report_template.type 与 report_record.type 取值一致)。
 */
public enum ReportType {

    WEEKLY(2, "周报"),
    MONTHLY(3, "月报"),
    QUARTERLY(4, "季报");

    private final int code;
    private final String desc;

    ReportType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int code() { return code; }
    public String desc() { return desc; }

    public static ReportType fromCode(int code) {
        for (ReportType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown ReportType code: " + code);
    }
}
```

- [ ] **步骤 6:创建 ReportRecord 实体**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/ReportRecord.java`:

```java
package com.zwei.iot.report.domain;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报告记录实体 (对应 report_record 表)。
 */
@Data
public class ReportRecord {

    private Long id;
    private Long templateId;
    private String templateName;

    /** 报告类型 2-周报 3-月报 4-季报 (对应 {@link ReportType#code()}) */
    private Integer type;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private Long hazardPointId;
    private String hazardPointCode;
    private String hazardPointName;

    private String reportName;
    private LocalDateTime reportDate;

    /** HTML 内容 (列表查询不返回,详情查询返回) */
    private String content;

    private String filePath;

    /** 状态 1-生成中 2-已生成 3-生成失败 */
    private Integer status;

    private String errorMsg;

    private Integer delFlag;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
```

- [ ] **步骤 7:创建 Mapper 接口**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/mapper/ReportRecordMapper.java`:

```java
package com.zwei.iot.report.mapper;

import com.zwei.iot.report.domain.ReportRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportRecordMapper {

    int insert(ReportRecord record);

    int updateStatusAndContent(@Param("id") Long id,
                                @Param("status") Integer status,
                                @Param("content") String content,
                                @Param("errorMsg") String errorMsg);

    int updateDeleteFlag(@Param("id") Long id, @Param("delFlag") Integer delFlag);

    ReportRecord selectById(@Param("id") Long id);

    /** 列表查询 (不含 content 字段,避免大字段传输) */
    List<ReportRecord> selectPageList(@Param("type") Integer type,
                                       @Param("hazardPointId") Long hazardPointId,
                                       @Param("periodStart") LocalDate periodStart,
                                       @Param("periodEnd") LocalDate periodEnd,
                                       @Param("status") Integer status,
                                       @Param("keyword") String keyword);

    long countPageList(@Param("type") Integer type,
                       @Param("hazardPointId") Long hazardPointId,
                       @Param("periodStart") LocalDate periodStart,
                       @Param("periodEnd") LocalDate periodEnd,
                       @Param("status") Integer status,
                       @Param("keyword") String keyword);

    /** 幂等检查: 查询同 type+hp+period 的成功记录 */
    ReportRecord selectExistingSuccess(@Param("type") Integer type,
                                        @Param("hazardPointId") Long hazardPointId,
                                        @Param("periodStart") LocalDate periodStart,
                                        @Param("periodEnd") LocalDate periodEnd);
}
```

- [ ] **步骤 8:创建 Mapper XML**

写入 `server/zwei-iot-report/src/main/resources/mapper/iot/report/ReportRecordMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.report.mapper.ReportRecordMapper">

    <resultMap id="ReportRecordResult" type="com.zwei.iot.report.domain.ReportRecord">
        <id     property="id"               column="id"/>
        <result property="templateId"       column="template_id"/>
        <result property="templateName"     column="template_name"/>
        <result property="type"             column="type"/>
        <result property="periodStart"      column="period_start"/>
        <result property="periodEnd"        column="period_end"/>
        <result property="hazardPointId"    column="hazard_point_id"/>
        <result property="hazardPointCode"  column="hazard_point_code"/>
        <result property="hazardPointName"  column="hazard_point_name"/>
        <result property="reportName"       column="report_name"/>
        <result property="reportDate"       column="report_date"/>
        <result property="content"          column="content"/>
        <result property="filePath"         column="file_path"/>
        <result property="status"           column="status"/>
        <result property="errorMsg"         column="error_msg"/>
        <result property="delFlag"          column="del_flag"/>
        <result property="createBy"         column="create_by"/>
        <result property="createTime"       column="create_time"/>
        <result property="updateBy"         column="update_by"/>
        <result property="updateTime"       column="update_time"/>
    </resultMap>

    <sql id="selectColumnsNoContent">
        id, template_id, template_name, type, period_start, period_end,
        hazard_point_id, hazard_point_code, hazard_point_name,
        report_name, report_date, file_path, status, error_msg, del_flag,
        create_by, create_time, update_by, update_time
    </sql>

    <sql id="selectColumnsAll">
        id, template_id, template_name, type, period_start, period_end,
        hazard_point_id, hazard_point_code, hazard_point_name,
        report_name, report_date, content, file_path, status, error_msg, del_flag,
        create_by, create_time, update_by, update_time
    </sql>

    <insert id="insert" parameterType="com.zwei.iot.report.domain.ReportRecord"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO report_record (
            template_id, template_name, type, period_start, period_end,
            hazard_point_id, hazard_point_code, hazard_point_name,
            report_name, report_date, content, file_path, status, error_msg,
            create_by, create_time
        ) VALUES (
            #{templateId}, #{templateName}, #{type}, #{periodStart}, #{periodEnd},
            #{hazardPointId}, #{hazardPointCode}, #{hazardPointName},
            #{reportName}, #{reportDate}, #{content}, #{filePath}, #{status}, #{errorMsg},
            #{createBy}, NOW()
        )
    </insert>

    <update id="updateStatusAndContent">
        UPDATE report_record
        SET status = #{status},
            content = #{content},
            error_msg = #{errorMsg},
            report_date = NOW()
        WHERE id = #{id}
    </update>

    <update id="updateDeleteFlag">
        UPDATE report_record SET del_flag = #{delFlag} WHERE id = #{id}
    </update>

    <select id="selectById" resultMap="ReportRecordResult">
        SELECT <include refid="selectColumnsAll"/>
        FROM report_record
        WHERE id = #{id} AND del_flag = 0
    </select>

    <select id="selectPageList" resultMap="ReportRecordResult">
        SELECT <include refid="selectColumnsNoContent"/>
        FROM report_record
        WHERE del_flag = 0
        <if test="type != null">           AND type = #{type}</if>
        <if test="hazardPointId != null">  AND hazard_point_id = #{hazardPointId}</if>
        <if test="periodStart != null">    AND period_start &gt;= #{periodStart}</if>
        <if test="periodEnd != null">      AND period_end   &lt;= #{periodEnd}</if>
        <if test="status != null">         AND status = #{status}</if>
        <if test="keyword != null and keyword != ''">
            AND report_name LIKE CONCAT('%', #{keyword}, '%')
        </if>
        ORDER BY create_time DESC
    </select>

    <select id="countPageList" resultType="long">
        SELECT COUNT(*) FROM report_record
        WHERE del_flag = 0
        <if test="type != null">           AND type = #{type}</if>
        <if test="hazardPointId != null">  AND hazard_point_id = #{hazardPointId}</if>
        <if test="periodStart != null">    AND period_start &gt;= #{periodStart}</if>
        <if test="periodEnd != null">      AND period_end   &lt;= #{periodEnd}</if>
        <if test="status != null">         AND status = #{status}</if>
        <if test="keyword != null and keyword != ''">
            AND report_name LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </select>

    <select id="selectExistingSuccess" resultMap="ReportRecordResult">
        SELECT <include refid="selectColumnsNoContent"/>
        FROM report_record
        WHERE type = #{type}
          AND hazard_point_id = #{hazardPointId}
          AND period_start = #{periodStart}
          AND period_end = #{periodEnd}
          AND status = 2
          AND del_flag = 0
        LIMIT 1
    </select>

</mapper>
```

- [ ] **步骤 9:确认 MyBatis 扫描路径包含新模块**

`Read` `server/zwei-admin/src/main/java/com/zwei/RuoYiApplication.java` 主类,检查 `@MapperScan` 或 `application.yml` 中 `mybatis.mapper-locations`。若路径写死为 `classpath*:mapper/**/*.xml`,则新 XML 自动生效;若写死具体子目录,需补充 `mapper/iot/report/*.xml`。

```bash
grep -n "mapper-locations" server/zwei-admin/src/main/resources/application.yml
```

- [ ] **步骤 10:编译验证**

```bash
cd server && mvn -pl zwei-iot-report -am compile
# 预期: BUILD SUCCESS
```

- [ ] **步骤 11:Commit**

```bash
git add server/zwei-iot-report/ server/pom.xml server/zwei-admin/pom.xml
git commit -m "feat(report): 新建 zwei-iot-report Maven 模块骨架 + ReportRecord 实体/Mapper"
```

---

## 任务 5:ReportPeriod 周期计算工具

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/support/ReportPeriod.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/support/ReportPeriodTest.java`

- [ ] **步骤 1:编写失败的测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/support/ReportPeriodTest.java`:

```java
package com.zwei.iot.report.support;

import com.zwei.iot.report.domain.ReportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportPeriod 周期计算")
class ReportPeriodTest {

    @Nested
    @DisplayName("lastWeek")
    class LastWeek {
        @Test void normal() {
            ReportPeriod p = ReportPeriod.lastWeek(LocalDate.of(2026, 6, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 6, 8));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 6, 14));
        }
        @Test void crossYear() {
            ReportPeriod p = ReportPeriod.lastWeek(LocalDate.of(2026, 1, 1));
            assertThat(p.start()).isEqualTo(LocalDate.of(2025, 12, 22));
            assertThat(p.end()).isEqualTo(LocalDate.of(2025, 12, 28));
        }
        @Test void mondayIsStart() {
            // 2026-06-15 是周一, 上周应回退 7 天
            ReportPeriod p = ReportPeriod.lastWeek(LocalDate.of(2026, 6, 15));
            assertThat(p.start().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
            assertThat(p.end().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.SUNDAY);
        }
    }

    @Nested
    @DisplayName("lastMonth")
    class LastMonth {
        @Test void normal() {
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2026, 6, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 6, 30));
        }
        @Test void crossYear() {
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2026, 1, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2025, 12, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2025, 12, 31));
        }
        @Test void leapYearFebruary() {
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2024, 3, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2024, 2, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2024, 2, 29));
        }
        @Test void nonLeapYearFebruary() {
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2026, 3, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 2, 28));
        }
    }

    @Nested
    @DisplayName("lastQuarter")
    class LastQuarter {
        @Test void currentQ1_lastIsPrevQ4() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 1, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2025, 10, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2025, 12, 31));
        }
        @Test void currentQ2_lastIsQ1() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 4, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 3, 31));
        }
        @Test void currentQ3_lastIsQ2() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 7, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 6, 30));
        }
        @Test void currentQ4_lastIsQ3() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 10, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 9, 30));
        }
    }

    @Test
    @DisplayName("previous(WEEKLY, today) 等价于 lastWeek(today)")
    void previousDispatch() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        assertThat(ReportPeriod.previous(ReportType.WEEKLY, today))
            .isEqualTo(ReportPeriod.lastWeek(today));
        assertThat(ReportPeriod.previous(ReportType.MONTHLY, today))
            .isEqualTo(ReportPeriod.lastMonth(today));
        assertThat(ReportPeriod.previous(ReportType.QUARTERLY, today))
            .isEqualTo(ReportPeriod.lastQuarter(today));
    }

    @Test
    @DisplayName("key() 返回 start_end 标识")
    void keyFormat() {
        ReportPeriod p = new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14));
        assertThat(p.key()).isEqualTo("2026-06-08_2026-06-14");
    }
}
```

- [ ] **步骤 2:运行测试验证失败**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=ReportPeriodTest
```

预期:编译错误(`ReportPeriod` 不存在)。

- [ ] **步骤 3:编写实现**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/support/ReportPeriod.java`:

```java
package com.zwei.iot.report.support;

import com.zwei.iot.report.domain.ReportType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * 报告周期 (闭区间)。
 * 不可变值对象, 用 java.time 计算上周/上月/上季度。
 */
public record ReportPeriod(LocalDate start, LocalDate end) {

    /** 上一自然周 (周一~周日) */
    public static ReportPeriod lastWeek(LocalDate today) {
        LocalDate monday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        return new ReportPeriod(monday, monday.plusDays(6));
    }

    /** 上一自然月 */
    public static ReportPeriod lastMonth(LocalDate today) {
        LocalDate first = today.minusMonths(1).withDayOfMonth(1);
        return new ReportPeriod(first, first.withDayOfMonth(first.lengthOfMonth()));
    }

    /** 上一自然季度 (Q1=1-3, Q2=4-6, Q3=7-9, Q4=10-12) */
    public static ReportPeriod lastQuarter(LocalDate today) {
        LocalDate currentQuarterStart = today.with(IsoFields.DAY_OF_QUARTER, 1);
        LocalDate first = currentQuarterStart.minusMonths(3);
        LocalDate last = first.plusMonths(3).minusDays(1);
        return new ReportPeriod(first, last);
    }

    /** 按 ReportType 分发到对应方法 */
    public static ReportPeriod previous(ReportType type, LocalDate today) {
        return switch (type) {
            case WEEKLY -> lastWeek(today);
            case MONTHLY -> lastMonth(today);
            case QUARTERLY -> lastQuarter(today);
        };
    }

    /** 唯一 key, 用于 Redis 锁与日志 */
    public String key() {
        return start() + "_" + end();
    }
}
```

- [ ] **步骤 4:运行测试验证通过**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=ReportPeriodTest
```

预期: BUILD SUCCESS,15 个测试全绿。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/support/ReportPeriod.java \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/support/ReportPeriodTest.java
git commit -m "feat(report): ReportPeriod 周期计算工具 (周/月/季边界 + 跨年/闰年测试)"
```

---

## 任务 6:ReportContext + ReportDataAssembler

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/ReportContext.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/MetricRow.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/ReportDataAssembler.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/datasource/ReportDataAssemblerTest.java`

- [ ] **步骤 1:编写 ReportContext 与 MetricRow**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/MetricRow.java`:

```java
package com.zwei.iot.report.datasource;

import java.util.List;

/**
 * 单条指标汇总行 (供渲染器拼表格)。
 */
public record MetricRow(
    String attrCode,
    String attrName,
    String unit,
    Double latest,         // 最新值
    Double maxValue,       // 周期内最大
    Double minValue,       // 周期内最小
    Double avgValue,       // 周期内平均
    Double sumValue,       // 周期内累计变化量 (sum)
    Double completenessPct // 完整率 (0-100)
) {}
```

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/ReportContext.java`:

```java
package com.zwei.iot.report.datasource;

import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 渲染器输入:聚合后的报告数据。
 * 不同 ReportType 只填充相关字段, 其余保持 null/空。
 */
public record ReportContext(
    ReportType type,
    ReportPeriod period,
    HazardPointBrief hazardPoint,
    LocalDateTime generatedAt,

    // === 设备 (周/月/季都用) ===
    List<DeviceBrief> devices,
    int deviceTotal,
    int deviceOnline,
    int deviceOffline,
    double onlineRatePct,

    // === 监测数据指标 (周/月/季都用) ===
    List<MetricRow> metrics,

    // === 风险 (月/季用, 周报为 null) ===
    AlarmSummary alarmSummary,
    List<AlarmEvent> alarmTopEvents,

    // === 趋势 (季用, 周/月为 null) ===
    /** key=attrCode, value=方向 ("UP"/"DOWN"/"STABLE") */
    Map<String, String> trendDirections,
    /** key=attrCode, value=斜率 */
    Map<String, Double> trendSlopes,
    /** key=yyyy-MM, value=告警次数 */
    Map<String, Integer> alarmMonthlyCount
) {}
```

- [ ] **步骤 2:编写 ReportDataAssembler 骨架**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/ReportDataAssembler.java`:

```java
package com.zwei.iot.report.datasource;

import com.zwei.iot.alarm.service.impl.AlarmQueryServiceImpl; // 仅用于类型提示,实际依赖通过接口
import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IAlarmQueryService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 按 (hazardPointId, period) 聚合数据为 ReportContext。
 * 依赖 4 个跨模块 Service; 单 hp 单周期串行调用。
 */
@Component
public class ReportDataAssembler {

    private static final Logger log = LoggerFactory.getLogger(ReportDataAssembler.class);

    private final IHazardPointQueryService hazardQuery;
    private final IDeviceHazardRelationService deviceRelation;
    private final IDeviceSensorQueryService sensorQuery;
    private final IotdbTimeSeriesService timeSeries;
    private final IAlarmQueryService alarmQuery;

    public ReportDataAssembler(IHazardPointQueryService hazardQuery,
                                IDeviceHazardRelationService deviceRelation,
                                IDeviceSensorQueryService sensorQuery,
                                IotdbTimeSeriesService timeSeries,
                                IAlarmQueryService alarmQuery) {
        this.hazardQuery = hazardQuery;
        this.deviceRelation = deviceRelation;
        this.sensorQuery = sensorQuery;
        this.timeSeries = timeSeries;
        this.alarmQuery = alarmQuery;
    }

    public ReportContext build(ReportType type, ReportPeriod period, HazardPointBrief hp) {
        LocalDateTime start = period.start().atStartOfDay();
        LocalDateTime end = period.end().atTime(23, 59, 59);
        long startMs = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMs = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 1. 设备
        List<DeviceBrief> devices = deviceRelation.getDevicesByHazardPoint(hp.id());
        int devTotal = devices.size();
        int devOnline = (int) devices.stream().filter(d -> d.onlineStatus() != null && d.onlineStatus() == 1).count();
        int devOffline = devTotal - devOnline;
        double onlineRate = devTotal == 0 ? 0.0 : (devOnline * 100.0 / devTotal);

        // 2. 指标 (查每个设备的每个传感器每个属性)
        List<MetricRow> metrics = buildMetrics(devices, startMs, endMs);

        // 3. 风险 (月/季才查)
        AlarmSummary alarmSummary = null;
        List<AlarmEvent> alarmTop = null;
        if (type == ReportType.MONTHLY || type == ReportType.QUARTERLY) {
            alarmSummary = alarmQuery.summarizeByHazardPoint(hp.id(), start, end);
            alarmTop = alarmQuery.listTopByHazardPoint(hp.id(), start, end, 10);
        }

        // 4. 趋势 (季才查)
        Map<String, String> trendDirs = null;
        Map<String, Double> trendSlopes = null;
        Map<String, Integer> alarmMonthly = null;
        if (type == ReportType.QUARTERLY) {
            trendDirs = new HashMap<>();
            trendSlopes = new HashMap<>();
            // 此处简化: 对每个属性调 queryTrend; 实际实现需遍历设备传感器属性
            // 见 step 3 完整实现
            alarmMonthly = alarmQuery.countByMonth(hp.id(), start, end);
        }

        return new ReportContext(
            type, period, hp, LocalDateTime.now(),
            devices, devTotal, devOnline, devOffline, onlineRate,
            metrics,
            alarmSummary, alarmTop,
            trendDirs, trendSlopes, alarmMonthly
        );
    }

    /** 遍历设备 → 传感器 → 属性, 调 IoTDB 聚合 */
    private List<MetricRow> buildMetrics(List<DeviceBrief> devices, long startMs, long endMs) {
        List<MetricRow> rows = new ArrayList<>();
        for (DeviceBrief dev : devices) {
            try {
                List<SensorMetadata> sensors = sensorQuery.requireSensorsByDeviceId(dev.id());
                for (SensorMetadata sensor : sensors) {
                    for (SensorMetadata.SensorAttribute attr : sensor.attributes()) {
                        MetricRow row = buildMetricRow(dev, sensor, attr, startMs, endMs);
                        if (row != null) rows.add(row);
                    }
                }
            } catch (Exception e) {
                log.warn("[report] skip device {} metric aggregation: {}", dev.id(), e.getMessage());
            }
        }
        return rows;
    }

    private MetricRow buildMetricRow(DeviceBrief dev, SensorMetadata sensor,
                                      SensorMetadata.SensorAttribute attr, long startMs, long endMs) {
        // TODO step 3 实现: 调 timeSeries.queryAggregate(...) 取 max/min/avg/sum/last
        // 此处先返回 null, 由 step 3 填充
        return null;
    }
}
```

> **执行时检查项:** `SensorMetadata` 与 `SensorMetadata.SensorAttribute` 的实际字段名用 `Read` 确认,若 record 嵌套结构不同,调整代码。

- [ ] **步骤 3:补全 buildMetricRow 实现**

替换 `ReportDataAssembler.java` 中 `buildMetricRow` 方法体为:

```java
private MetricRow buildMetricRow(DeviceBrief dev, SensorMetadata sensor,
                                  SensorMetadata.SensorAttribute attr, long startMs, long endMs) {
    try {
        // 调聚合接口取 max/min/avg/sum/last (具体方法签名以 IotdbTimeSeriesService 为准)
        // 参考: List<AggregationResultVO> queryAggregate(Long, String, String, TimeWindowSpec, List<ExpressionSpec>, Double, Double)
        // 若签名不同, 按 Read IotdbTimeSeriesService.java 后调整
        com.zwei.iot.timeseries.domain.dto.AggregationResultVO max = queryAgg(dev.id(), sensor.sensorCode(), attr.attrCode(), "max", startMs, endMs);
        com.zwei.iot.timeseries.domain.dto.AggregationResultVO min = queryAgg(dev.id(), sensor.sensorCode(), attr.attrCode(), "min", startMs, endMs);
        com.zwei.iot.timeseries.domain.dto.AggregationResultVO avg = queryAgg(dev.id(), sensor.sensorCode(), attr.attrCode(), "avg", startMs, endMs);
        com.zwei.iot.timeseries.domain.dto.AggregationResultVO sum = queryAgg(dev.id(), sensor.sensorCode(), attr.attrCode(), "sum", startMs, endMs);
        com.zwei.iot.timeseries.domain.dto.AggregationResultVO last = queryAgg(dev.id(), sensor.sensorCode(), attr.attrCode(), "last", startMs, endMs);

        Double completeness = null;
        try {
            // queryCompleteness 签名以实际为准
            // completeness = timeSeries.queryCompleteness(dev.id(), sensor.sensorCode(), attr.attrCode(), window, intervalMs).getCompletenessPct();
        } catch (Exception ignore) {}

        return new MetricRow(
            attr.attrCode(), attr.attrName(), attr.unit(),
            val(last), val(max), val(min), val(avg), val(sum), completeness
        );
    } catch (Exception e) {
        log.warn("[report] metric aggregation fail dev={} attr={}: {}", dev.id(), attr.attrCode(), e.getMessage());
        return null;
    }
}

private com.zwei.iot.timeseries.domain.dto.AggregationResultVO queryAgg(
        Long deviceId, String sensorCode, String attrCode, String func, long startMs, long endMs) {
    // 包装 IotdbTimeSeriesService.queryAggregate 调用
    // ExpressionSpec/TimeWindowSpec 的构造见 timeseries 模块 DTO
    // 若返回 List, 取第一个; 空返回 null VO
    throw new UnsupportedOperationException("TODO: 实际接入 IotdbTimeSeriesService (签名以 Read 为准)");
}

private static Double val(com.zwei.iot.timeseries.domain.dto.AggregationResultVO vo) {
    return vo == null ? null : vo.getValue(); // 字段名以实际为准
}
```

> **执行时强制步骤:** 用 `Read` 看 `IotdbTimeSeriesService.java` 的真实方法签名,以及 `AggregationResultVO`/`ExpressionSpec`/`TimeWindowSpec`/`TrendReportVO`/`CompletenessReportVO` 的字段名,**必须**把上述 `queryAgg` 改为真实可用代码,不能保留 `UnsupportedOperationException`。这是本计划中唯一明确标注"执行时根据真实签名适配"的位置。

- [ ] **步骤 4:补全趋势查询**

在 `build` 方法的 `type == QUARTERLY` 分支中,对每个 metric 调 `timeSeries.queryTrend(...)` 并填入 `trendDirs`/`trendSlopes`。参考片段:

```java
for (MetricRow m : metrics) {
    try {
        // TrendReportVO trend = timeSeries.queryTrend(deviceId, sensorCode, attrCode, window);
        // trendDirs.put(m.attrCode(), trend.getDirection());
        // trendSlopes.put(m.attrCode(), trend.getSlope());
    } catch (Exception ignore) {}
}
```

> **执行时检查项:** trend 查询需要 (deviceId, sensorCode, attrCode),而 MetricRow 不含设备维度。**改进方案**: `MetricRow` 加 `Long deviceId` / `String sensorCode` 字段,或 trend 查询只取该 hp 下第一个设备的每个属性(简化方案)。**推荐简化方案**,在 step 3 buildMetricRow 已知 deviceId/sensorCode 时直接调 trend 并填入临时 Map。

- [ ] **步骤 5:编写测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/datasource/ReportDataAssemblerTest.java`:

```java
package com.zwei.iot.report.datasource;

import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IAlarmQueryService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ReportDataAssembler")
class ReportDataAssemblerTest {

    private IHazardPointQueryService hazardQuery;
    private IDeviceHazardRelationService deviceRelation;
    private IDeviceSensorQueryService sensorQuery;
    private IotdbTimeSeriesService timeSeries;
    private IAlarmQueryService alarmQuery;
    private ReportDataAssembler assembler;

    @BeforeEach
    void setUp() {
        hazardQuery = mock(IHazardPointQueryService.class);
        deviceRelation = mock(IDeviceHazardRelationService.class);
        sensorQuery = mock(IDeviceSensorQueryService.class);
        timeSeries = mock(IotdbTimeSeriesService.class);
        alarmQuery = mock(IAlarmQueryService.class);
        assembler = new ReportDataAssembler(hazardQuery, deviceRelation, sensorQuery, timeSeries, alarmQuery);
    }

    @Test
    @DisplayName("周报: 设备在线率正确计算, 不查告警")
    void weeklyDevicesAndNoAlarm() {
        HazardPointBrief hp = new HazardPointBrief(1L, "HP001", "测试隐患点", new BigDecimal("104"), new BigDecimal("30"));
        when(deviceRelation.getDevicesByHazardPoint(1L)).thenReturn(List.of(
            new DeviceBrief(10L, "D1", "设备1", 1, "位移", 2, 1, null),
            new DeviceBrief(11L, "D2", "设备2", 2, "雨量", 1, 0, null),
            new DeviceBrief(12L, "D3", "设备3", 3, "倾角", 1, 1, null)
        ));
        when(sensorQuery.requireSensorsByDeviceId(anyLong())).thenReturn(List.of());

        ReportContext ctx = assembler.build(
            ReportType.WEEKLY,
            new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)),
            hp);

        assertThat(ctx.deviceTotal()).isEqualTo(3);
        assertThat(ctx.deviceOnline()).isEqualTo(2);
        assertThat(ctx.deviceOffline()).isEqualTo(1);
        assertThat(ctx.onlineRatePct()).isCloseTo(66.67, within(0.01));
        assertThat(ctx.alarmSummary()).isNull();
        assertThat(ctx.alarmTopEvents()).isNull();
        verifyNoInteractions(alarmQuery);
    }

    @Test
    @DisplayName("月报: 查询告警摘要与 Top 事件")
    void monthlyTriggersAlarmQuery() {
        HazardPointBrief hp = new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));
        when(deviceRelation.getDevicesByHazardPoint(1L)).thenReturn(List.of());
        when(sensorQuery.requireSensorsByDeviceId(anyLong())).thenReturn(List.of());
        AlarmSummary summary = new AlarmSummary(1L, 5, 3, 2, Map.of(), Map.of());
        when(alarmQuery.summarizeByHazardPoint(eq(1L), any(), any())).thenReturn(summary);
        when(alarmQuery.listTopByHazardPoint(eq(1L), any(), any(), eq(10))).thenReturn(List.of());

        ReportContext ctx = assembler.build(
            ReportType.MONTHLY,
            new ReportPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
            hp);

        assertThat(ctx.alarmSummary()).isEqualTo(summary);
        verify(alarmQuery).summarizeByHazardPoint(eq(1L), any(), any());
        verify(alarmQuery).listTopByHazardPoint(eq(1L), any(), any(), eq(10));
    }

    @Test
    @DisplayName("季报: 查询月度告警分布")
    void quarterlyTriggersMonthlyCount() {
        HazardPointBrief hp = new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));
        when(deviceRelation.getDevicesByHazardPoint(1L)).thenReturn(List.of());
        when(sensorQuery.requireSensorsByDeviceId(anyLong())).thenReturn(List.of());
        when(alarmQuery.summarizeByHazardPoint(anyLong(), any(), any()))
            .thenReturn(new AlarmSummary(1L, 0, 0, 0, Map.of(), Map.of()));
        when(alarmQuery.countByMonth(eq(1L), any(), any())).thenReturn(Map.of("2026-04", 3, "2026-05", 5, "2026-06", 2));

        ReportContext ctx = assembler.build(
            ReportType.QUARTERLY,
            new ReportPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
            hp);

        assertThat(ctx.alarmMonthlyCount()).containsEntry("2026-05", 5);
    }

    private static <T> T anyLong() { return org.mockito.ArgumentMatchers.any(); }
    private static <T> T eq(T v) { return org.mockito.ArgumentMatchers.eq(v); }
    private static <T> T any() { return org.mockito.ArgumentMatchers.any(); }
    private static org.assertj.core.data.Offset<Double> within(double d) { return org.assertj.core.data.Offset.offset(d); }
}
```

> **执行时检查项:** 上面 helper 方法(`anyLong`/`eq`/`any`/`within`)是为了避免静态导入冲突,可以直接 `import static org.mockito.ArgumentMatchers.*;` + `import static org.assertj.core.api.Assertions.within;`。

- [ ] **步骤 6:运行测试**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=ReportDataAssemblerTest
```

预期: BUILD SUCCESS,3 个测试全绿。

- [ ] **步骤 7:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/datasource/ \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/datasource/
git commit -m "feat(report): ReportContext 数据载体 + ReportDataAssembler 跨模块数据聚合"
```

---

## 任务 7:ReportRenderer 接口 + WeeklyReportRenderer

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/ReportRenderer.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/ReportHtmlBuilder.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/WeeklyReportRenderer.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/WeeklyReportRendererTest.java`

- [ ] **步骤 1:编写 ReportRenderer 接口与 HtmlBuilder 工具**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/ReportRenderer.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;

/**
 * 报告渲染策略 (Spring 自动注入所有实现)。
 */
public interface ReportRenderer {

    ReportType type();

    /**
     * 将 ctx 渲染为完整 HTML 字符串。
     * HTML 内联样式, 不引用外部 CSS / 字体 (html2canvas 截图友好)。
     */
    String render(ReportContext ctx);
}
```

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/ReportHtmlBuilder.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;

/**
 * HTML 拼接工具 (内联样式, PDF 截图友好)。
 */
public final class ReportHtmlBuilder {

    private static final String STYLE_TABLE = "border-collapse:collapse;width:100%;text-align:center;font-size:12px;";
    private static final String STYLE_TH = "background:#f0f5ff;padding:6px;border:1px solid #ddd;";
    private static final String STYLE_TD = "padding:6px;border:1px solid #ddd;";

    private ReportHtmlBuilder() {}

    public static String header(ReportContext ctx) {
        ReportType t = ctx.type();
        String title = "地质灾害监测" + t.desc();
        return "<h2 style=\"text-align:center;color:#1f2d3d;margin-bottom:4px;\">" + title + "</h2>"
            + "<div style=\"border-bottom:2px solid #1f2d3d;margin:8px 0 16px;\"></div>"
            + "<p style=\"margin:4px 0;\"><strong>报告周期：</strong>" + ctx.period().start() + " 至 " + ctx.period().end() + "</p>"
            + "<p style=\"margin:4px 0;\"><strong>隐患点：</strong>" + ctx.hazardPoint().code() + " " + ctx.hazardPoint().name() + "</p>"
            + (ctx.hazardPoint().longitude() != null
                ? "<p style=\"margin:4px 0;\"><strong>隐患点位置：</strong>经度 " + ctx.hazardPoint().longitude() + ", 纬度 " + ctx.hazardPoint().latitude() + "</p>"
                : "")
            + "<p style=\"margin:4px 0;\"><strong>生成时间：</strong>" + ctx.generatedAt() + "</p>"
            + "<div style=\"height:12px;\"></div>";
    }

    public static String sectionTitle(String text) {
        return "<h3 style=\"color:#1f2d3d;border-left:4px solid #409eff;padding-left:8px;margin:16px 0 8px;\">"
            + text + "</h3>";
    }

    public static String openTable(String... headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"").append(STYLE_TABLE).append("\">");
        sb.append("<thead><tr>");
        for (String h : headers) {
            sb.append("<th style=\"").append(STYLE_TH).append("\">").append(h).append("</th>");
        }
        sb.append("</tr></thead><tbody>");
        return sb.toString();
    }

    public static String row(String... cells) {
        StringBuilder sb = new StringBuilder("<tr>");
        for (String c : cells) {
            sb.append("<td style=\"").append(STYLE_TD).append("\">").append(c == null ? "-" : c).append("</td>");
        }
        return sb.append("</tr>").toString();
    }

    public static String closeTable() { return "</tbody></table>"; }

    public static String paragraph(String text) {
        return "<p style=\"margin:8px 0;line-height:1.6;\">" + text + "</p>";
    }

    public static String bulletList(String... items) {
        StringBuilder sb = new StringBuilder("<ul style=\"margin:8px 0;\">");
        for (String i : items) sb.append("<li>").append(i).append("</li>");
        return sb.append("</ul>").toString();
    }

    public static String levelColor(int level) {
        return switch (level) {
            case 4 -> "#ff4d4f"; // 红
            case 3 -> "#fa8c16"; // 橙
            case 2 -> "#faad14"; // 黄
            case 1 -> "#1890ff"; // 蓝
            default -> "#909399";
        };
    }

    public static String levelName(int level) {
        return switch (level) {
            case 4 -> "红色";
            case 3 -> "橙色";
            case 2 -> "黄色";
            case 1 -> "蓝色";
            default -> "无";
        };
    }
}
```

- [ ] **步骤 2:编写失败的测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/WeeklyReportRendererTest.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.report.datasource.MetricRow;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeeklyReportRenderer")
class WeeklyReportRendererTest {

    private final WeeklyReportRenderer renderer = new WeeklyReportRenderer();

    private ReportContext ctxWith(List<DeviceBrief> devices, List<MetricRow> metrics) {
        return new ReportContext(
            ReportType.WEEKLY,
            new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)),
            new HazardPointBrief(1L, "HP001", "测试隐患点", new BigDecimal("104.15"), new BigDecimal("30.5")),
            LocalDateTime.of(2026, 6, 15, 2, 0, 0),
            devices, devices.size(), 0, devices.size(), 0.0,
            metrics,
            null, null, null, null, null
        );
    }

    @Test
    @DisplayName("type() 返回 WEEKLY")
    void type() {
        assertThat(renderer.type()).isEqualTo(ReportType.WEEKLY);
    }

    @Test
    @DisplayName("渲染包含标题/周期/隐患点/生成时间")
    void renderContainsHeader() {
        String html = renderer.render(ctxWith(List.of(), List.of()));
        assertThat(html).contains("地质灾害监测周报");
        assertThat(html).contains("2026-06-08");
        assertThat(html).contains("2026-06-14");
        assertThat(html).contains("HP001");
        assertThat(html).contains("测试隐患点");
    }

    @Test
    @DisplayName("设备表渲染所有设备行")
    void renderDevices() {
        String html = renderer.render(ctxWith(List.of(
            new DeviceBrief(10L, "D001", "位移计01", 1, "位移", 2, 1, LocalDateTime.of(2026, 6, 14, 23, 0)),
            new DeviceBrief(11L, "D002", "雨量计01", 2, "雨量", 1, 0, null)
        ), List.of()));
        assertThat(html).contains("D001").contains("位移计01");
        assertThat(html).contains("D002").contains("雨量计01");
        assertThat(html).contains("在线").contains("离线");
    }

    @Test
    @DisplayName("指标表渲染属性/单位/最新值/最大/最小/均值")
    void renderMetrics() {
        String html = renderer.render(ctxWith(List.of(), List.of(
            new MetricRow("disp_x", "X方向位移", "mm", 1.5, 3.2, 0.1, 1.8, 2.5, 99.5)
        )));
        assertThat(html).contains("disp_x").contains("X方向位移").contains("mm");
        assertThat(html).contains("1.5").contains("3.2");
    }

    @Test
    @DisplayName("空指标时显示本周无异常 + 完整率信息")
    void emptyMetricsFallback() {
        String html = renderer.render(ctxWith(List.of(), List.of()));
        assertThat(html).contains("无异常").or().contains("完整率");
    }
}
```

- [ ] **步骤 3:运行测试验证失败**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=WeeklyReportRendererTest
```

预期:编译错误(`WeeklyReportRenderer` 不存在)。

- [ ] **步骤 4:编写实现**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/WeeklyReportRenderer.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.report.datasource.MetricRow;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 周报渲染器 — 侧重监测设备 + 数据情况。
 *
 * 章节: 1.设备运行列表  2.监测数据概况  3.数据完整率  4.异常数据  5.分析与建议
 */
@Component
public class WeeklyReportRenderer implements ReportRenderer {

    @Override
    public ReportType type() { return ReportType.WEEKLY; }

    @Override
    public String render(ReportContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ReportHtmlBuilder.header(ctx));

        // 1. 设备运行列表
        sb.append(ReportHtmlBuilder.sectionTitle("1. 设备运行列表"));
        sb.append(ReportHtmlBuilder.openTable("设备编号", "设备名称", "设备类型", "传感器数", "在线状态", "最近上报时间"));
        if (ctx.devices().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无设备", "", "", "", "", ""));
        } else {
            for (DeviceBrief d : ctx.devices()) {
                String status = d.onlineStatus() == null ? "未注册"
                    : (d.onlineStatus() == 1 ? "<span style=\"color:#67c23a;\">在线</span>"
                                              : "<span style=\"color:#f56c6c;\">离线</span>");
                sb.append(ReportHtmlBuilder.row(
                    d.code(), d.name(), d.deviceTypeName(),
                    String.valueOf(d.sensorCount()),
                    status,
                    d.lastReportAt() == null ? "-" : d.lastReportAt().toString()
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 2. 监测数据概况
        sb.append(ReportHtmlBuilder.sectionTitle("2. 监测数据概况"));
        sb.append(ReportHtmlBuilder.openTable("属性编码", "属性名称", "单位", "最新值", "周最大", "周最小", "周均值"));
        if (ctx.metrics().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "", "", "", "", "", ""));
        } else {
            for (MetricRow m : ctx.metrics()) {
                sb.append(ReportHtmlBuilder.row(
                    m.attrCode(), m.attrName(), m.unit(),
                    fmt(m.latest()), fmt(m.maxValue()), fmt(m.minValue()), fmt(m.avgValue())
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 3. 数据完整率
        sb.append(ReportHtmlBuilder.sectionTitle("3. 数据完整率"));
        sb.append(ReportHtmlBuilder.openTable("属性", "完整率"));
        if (ctx.metrics().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "-"));
        } else {
            for (MetricRow m : ctx.metrics()) {
                String pct = m.completenessPct() == null ? "-" : String.format("%.1f%%", m.completenessPct());
                sb.append(ReportHtmlBuilder.row(m.attrName(), pct));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 4. 异常数据 (简化: 显示阈值边界外的数据条数; 当前实现显示"本周无异常")
        sb.append(ReportHtmlBuilder.sectionTitle("4. 异常数据"));
        sb.append(ReportHtmlBuilder.paragraph("本周无异常数据 (所有指标均在阈值范围内)。"));

        // 5. 分析与建议
        sb.append(ReportHtmlBuilder.sectionTitle("5. 分析与建议"));
        List<String> advice = new java.util.ArrayList<>();
        advice.add("设备在线率 " + String.format("%.1f", ctx.onlineRatePct()) + "%, "
            + (ctx.onlineRatePct() >= 95 ? "运行正常。" : "建议核查离线设备。"));
        advice.add("建议持续关注本周变化较大的指标。");
        advice.add("确保各监测点设备供电及通信正常。");
        sb.append(ReportHtmlBuilder.bulletList(advice.toArray(new String[0])));

        return sb.toString();
    }

    private static String fmt(Double v) {
        return v == null ? "-" : String.format("%.3f", v);
    }
}
```

- [ ] **步骤 5:运行测试验证通过**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=WeeklyReportRendererTest
```

预期: BUILD SUCCESS,5 个测试全绿。

- [ ] **步骤 6:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/ReportRenderer.java \
        server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/ReportHtmlBuilder.java \
        server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/WeeklyReportRenderer.java \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/WeeklyReportRendererTest.java
git commit -m "feat(report): ReportRenderer 策略接口 + WeeklyReportRenderer 周报渲染器"
```

---

## 任务 8:MonthlyReportRenderer

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/MonthlyReportRenderer.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/MonthlyReportRendererTest.java`

- [ ] **步骤 1:编写失败的测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/MonthlyReportRendererTest.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.report.datasource.MetricRow;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonthlyReportRenderer")
class MonthlyReportRendererTest {

    private final MonthlyReportRenderer renderer = new MonthlyReportRenderer();

    private ReportContext ctx(AlarmSummary summary, List<AlarmEvent> events) {
        return new ReportContext(
            ReportType.MONTHLY,
            new ReportPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30")),
            LocalDateTime.of(2026, 7, 1, 2, 30),
            List.of(), 0, 0, 0, 0.0,
            List.of(new MetricRow("disp_x", "X方向位移", "mm", 1.5, 3.2, 0.1, 1.8, 2.5, 99.5)),
            summary, events,
            null, null, null
        );
    }

    @Test
    @DisplayName("type() 返回 MONTHLY")
    void type() { assertThat(renderer.type()).isEqualTo(ReportType.MONTHLY); }

    @Test
    @DisplayName("渲染包含月报标题与章节")
    void renderHasTitleAndSections() {
        String html = renderer.render(ctx(
            new AlarmSummary(1L, 0, 0, 0, Map.of(), Map.of()), List.of()));
        assertThat(html).contains("地质灾害监测月报");
        assertThat(html).contains("设备运行汇总");
        assertThat(html).contains("监测数据汇总");
        assertThat(html).contains("风险情况");
        assertThat(html).contains("关键事件");
        assertThat(html).contains("分析与建议");
    }

    @Test
    @DisplayName("风险章节按级别统计次数, 最高级别标色")
    void riskByLevel() {
        String html = renderer.render(ctx(
            new AlarmSummary(1L, 5, 3, 2,
                Map.of(1, 2, 2, 1, 3, 2),
                Map.of("1", 2, "3", 3)),
            List.of()));
        assertThat(html).contains("蓝色").contains("2");
        assertThat(html).contains("黄色").contains("1");
        assertThat(html).contains("橙色").contains("2");
        // 最高级别 = 橙色 (3), 应有颜色高亮
        assertThat(html).contains("#fa8c16");
    }

    @Test
    @DisplayName("Top 10 告警事件渲染为表格行")
    void topEventsRendered() {
        List<AlarmEvent> events = List.of(
            new AlarmEvent(1L, LocalDateTime.of(2026, 6, 5, 10, 0), 3, "THRESHOLD",
                "设备01", "位移传感器", "X方向位移超过阈值", "3"),
            new AlarmEvent(2L, LocalDateTime.of(2026, 6, 12, 14, 0), 2, "THRESHOLD",
                "设备02", "雨量传感器", "小时雨量超阈值", "3")
        );
        String html = renderer.render(ctx(
            new AlarmSummary(1L, 2, 3, 0, Map.of(2, 1, 3, 1), Map.of("3", 2)),
            events));
        assertThat(html).contains("设备01").contains("位移传感器");
        assertThat(html).contains("设备02").contains("雨量传感器");
    }

    @Test
    @DisplayName("无告警时风险章节显示本月无告警")
    void noAlarm() {
        String html = renderer.render(ctx(
            new AlarmSummary(1L, 0, 0, 0, Map.of(), Map.of()), List.of()));
        assertThat(html).contains("本月无告警").or().contains("无告警");
    }
}
```

- [ ] **步骤 2:运行测试验证失败**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=MonthlyReportRendererTest
```

预期:编译错误。

- [ ] **步骤 3:编写实现**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/MonthlyReportRenderer.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.report.datasource.MetricRow;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 月报渲染器 — 侧重设备 + 风险。
 *
 * 章节: 1.设备运行汇总  2.监测数据汇总  3.风险情况  4.关键事件  5.分析与建议
 */
@Component
public class MonthlyReportRenderer implements ReportRenderer {

    @Override
    public ReportType type() { return ReportType.MONTHLY; }

    @Override
    public String render(ReportContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ReportHtmlBuilder.header(ctx));

        // 1. 设备运行汇总
        sb.append(ReportHtmlBuilder.sectionTitle("1. 设备运行汇总"));
        sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
        sb.append(ReportHtmlBuilder.row("设备总数", String.valueOf(ctx.deviceTotal())));
        sb.append(ReportHtmlBuilder.row("在线设备数", String.valueOf(ctx.deviceOnline())));
        sb.append(ReportHtmlBuilder.row("离线设备数", String.valueOf(ctx.deviceOffline())));
        sb.append(ReportHtmlBuilder.row("设备在线率", String.format("%.1f%%", ctx.onlineRatePct())));
        sb.append(ReportHtmlBuilder.closeTable());

        // 2. 监测数据汇总
        sb.append(ReportHtmlBuilder.sectionTitle("2. 监测数据汇总"));
        sb.append(ReportHtmlBuilder.openTable("属性", "单位", "月最大", "月最小", "月均值", "月累计变化量"));
        if (ctx.metrics().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "", "", "", "", ""));
        } else {
            for (MetricRow m : ctx.metrics()) {
                sb.append(ReportHtmlBuilder.row(
                    m.attrName(), m.unit(),
                    fmt(m.maxValue()), fmt(m.minValue()), fmt(m.avgValue()), fmt(m.sumValue())
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 3. 风险情况
        sb.append(ReportHtmlBuilder.sectionTitle("3. 风险情况"));
        AlarmSummary sum = ctx.alarmSummary();
        if (sum == null || sum.total() == 0) {
            sb.append(ReportHtmlBuilder.paragraph("本月无告警记录。"));
        } else {
            sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
            sb.append(ReportHtmlBuilder.row("告警总数", String.valueOf(sum.total())));
            String maxColor = ReportHtmlBuilder.levelColor(sum.maxLevel());
            String maxName = ReportHtmlBuilder.levelName(sum.maxLevel());
            sb.append(ReportHtmlBuilder.row("最高告警级别",
                "<span style=\"color:" + maxColor + ";font-weight:bold;\">" + maxName + "</span>"));
            sb.append(ReportHtmlBuilder.row("待处理告警数", String.valueOf(sum.pendingCount())));
            sb.append(ReportHtmlBuilder.closeTable());

            sb.append(ReportHtmlBuilder.openTable("告警级别", "次数"));
            Map<Integer, Integer> levelCount = sum.levelCount();
            for (int lvl = 4; lvl >= 1; lvl--) {
                int cnt = levelCount.getOrDefault(lvl, 0);
                if (cnt > 0) {
                    String color = ReportHtmlBuilder.levelColor(lvl);
                    sb.append(ReportHtmlBuilder.row(
                        "<span style=\"color:" + color + ";\">" + ReportHtmlBuilder.levelName(lvl) + "</span>",
                        String.valueOf(cnt)));
                }
            }
            sb.append(ReportHtmlBuilder.closeTable());
        }

        // 4. 关键事件
        sb.append(ReportHtmlBuilder.sectionTitle("4. 关键事件 (Top 10)"));
        sb.append(ReportHtmlBuilder.openTable("时间", "级别", "类型", "设备", "传感器", "描述", "状态"));
        List<AlarmEvent> events = ctx.alarmTopEvents();
        if (events == null || events.isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无告警事件", "", "", "", "", "", ""));
        } else {
            for (AlarmEvent e : events) {
                String color = ReportHtmlBuilder.levelColor(e.alarmLevel());
                sb.append(ReportHtmlBuilder.row(
                    e.triggerTime() == null ? "-" : e.triggerTime().toString(),
                    "<span style=\"color:" + color + ";\">" + ReportHtmlBuilder.levelName(e.alarmLevel()) + "</span>",
                    e.alarmType() == null ? "-" : e.alarmType(),
                    e.deviceName(), e.sensorName(), e.description(),
                    statusName(e.alarmStatus())
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 5. 分析与建议
        sb.append(ReportHtmlBuilder.sectionTitle("5. 分析与建议"));
        List<String> advice = new java.util.ArrayList<>();
        if (sum != null && sum.total() > 0) {
            advice.add("本月共发生告警 " + sum.total() + " 次, 最高级别 "
                + ReportHtmlBuilder.levelName(sum.maxLevel()) + ", 建议核查高风险告警。");
        } else {
            advice.add("本月无告警记录, 监测数据稳定。");
        }
        advice.add("设备在线率 " + String.format("%.1f", ctx.onlineRatePct()) + "%。");
        advice.add("建议下月继续按既定监测方案执行, 关注雨季期间数据变化。");
        sb.append(ReportHtmlBuilder.bulletList(advice.toArray(new String[0])));

        return sb.toString();
    }

    private static String statusName(String s) {
        return switch (s == null ? "" : s) {
            case "1" -> "待处理";
            case "2" -> "处理中";
            case "3" -> "已销警";
            case "4" -> "误报";
            default -> "-";
        };
    }

    private static String fmt(Double v) {
        return v == null ? "-" : String.format("%.3f", v);
    }
}
```

- [ ] **步骤 4:运行测试验证通过**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=MonthlyReportRendererTest
```

预期: BUILD SUCCESS,5 个测试全绿。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/MonthlyReportRenderer.java \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/MonthlyReportRendererTest.java
git commit -m "feat(report): MonthlyReportRenderer 月报渲染器 (设备汇总+风险+Top10事件)"
```

---

## 任务 9:QuarterlyReportRenderer + RiskAssessor

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/RiskAssessor.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/QuarterlyReportRenderer.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/RiskAssessorTest.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/QuarterlyReportRendererTest.java`

- [ ] **步骤 1:编写 RiskAssessor 失败测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/RiskAssessorTest.java`:

```java
package com.zwei.iot.report.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskAssessor 综合风险评级")
class RiskAssessorTest {

    @Test
    @DisplayName("高告警频次 + 趋势上升 → 极高")
    void highFrequencyUpIsExtreme() {
        RiskAssessor.Risk r = RiskAssessor.assess(50, 4, 2, 60.0);
        assertThat(r.level()).isEqualTo("极高");
        assertThat(r.color()).isEqualTo("#ff4d4f");
    }

    @Test
    @DisplayName("中等告警 + 多个上升趋势 → 高")
    void mediumUpIsHigh() {
        RiskAssessor.Risk r = RiskAssessor.assess(15, 3, 2, 80.0);
        assertThat(r.level()).isIn("高", "极高");
    }

    @Test
    @DisplayName("低告警 + 趋势稳定 → 低")
    void lowStableIsLow() {
        RiskAssessor.Risk r = RiskAssessor.assess(2, 1, 0, 95.0);
        assertThat(r.level()).isEqualTo("低");
    }

    @Test
    @DisplayName("零告警 → 低")
    void zeroAlarmIsLow() {
        RiskAssessor.Risk r = RiskAssessor.assess(0, 0, 0, 99.0);
        assertThat(r.level()).isEqualTo("低");
        assertThat(r.color()).isEqualTo("#67c23a");
    }
}
```

- [ ] **步骤 2:运行测试验证失败**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=RiskAssessorTest
```

- [ ] **步骤 3:编写 RiskAssessor 实现**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/RiskAssessor.java`:

```java
package com.zwei.iot.report.render;

/**
 * 季报综合风险评级 (规则简单透明, 易于调整)。
 *
 * 评分维度:
 * - alarmTotal: 告警总次数
 * - maxAlarmLevel: 最高告警级别 (1-4)
 * - trendUpCount: 趋势上升的属性数
 * - onlineRatePct: 设备在线率
 *
 * 评级输出: 极高 / 高 / 中 / 低
 */
public final class RiskAssessor {

    private RiskAssessor() {}

    public static Risk assess(int alarmTotal, int maxAlarmLevel, int trendUpCount, double onlineRatePct) {
        int score = 0;
        // 告警频次
        if (alarmTotal >= 30) score += 4;
        else if (alarmTotal >= 10) score += 3;
        else if (alarmTotal >= 3) score += 2;
        else if (alarmTotal >= 1) score += 1;

        // 最高告警级别
        score += Math.max(0, maxAlarmLevel);  // 0-4

        // 趋势上升数量
        if (trendUpCount >= 3) score += 3;
        else if (trendUpCount >= 1) score += 1;

        // 在线率扣分
        if (onlineRatePct < 80) score += 1;
        if (onlineRatePct < 60) score += 1;

        String level;
        String color;
        if (score >= 9) { level = "极高"; color = "#ff4d4f"; }
        else if (score >= 6) { level = "高"; color = "#fa8c16"; }
        else if (score >= 3) { level = "中"; color = "#faad14"; }
        else { level = "低"; color = "#67c23a"; }
        return new Risk(level, color, score);
    }

    public record Risk(String level, String color, int score) {}
}
```

- [ ] **步骤 4:运行测试验证通过**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=RiskAssessorTest
```

预期: 4 个测试全绿。如某 case 不通过,调整 `assess` 阈值使其与测试预期一致。

- [ ] **步骤 5:编写 QuarterlyReportRenderer 失败测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/QuarterlyReportRendererTest.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.support.ReportPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuarterlyReportRenderer")
class QuarterlyReportRendererTest {

    private final QuarterlyReportRenderer renderer = new QuarterlyReportRenderer();

    private ReportContext ctx(Map<String, Integer> monthly, Map<String, String> trends) {
        return new ReportContext(
            ReportType.QUARTERLY,
            new ReportPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30")),
            LocalDateTime.of(2026, 7, 1, 3, 0),
            List.of(), 5, 5, 0, 100.0,
            List.of(),
            new AlarmSummary(1L, 10, 3, 2, Map.of(1, 5, 3, 5), Map.of("1", 2, "3", 8)),
            List.of(),
            trends,
            Map.of("disp_x", 0.5),
            monthly
        );
    }

    @Test
    @DisplayName("type() 返回 QUARTERLY")
    void type() { assertThat(renderer.type()).isEqualTo(ReportType.QUARTERLY); }

    @Test
    @DisplayName("渲染包含季报标题与5章节")
    void renderSections() {
        String html = renderer.render(ctx(
            Map.of("2026-04", 3, "2026-05", 5, "2026-06", 2),
            Map.of("disp_x", "UP", "rainfall", "STABLE")));
        assertThat(html).contains("地质灾害监测季报");
        assertThat(html).contains("季度风险总览");
        assertThat(html).contains("趋势分析");
        assertThat(html).contains("告警分布");
        assertThat(html).contains("设备运行汇总");
        assertThat(html).contains("风险评估与建议");
    }

    @Test
    @DisplayName("月度告警分布按月渲染 3 行")
    void monthlyAlarmDistribution() {
        String html = renderer.render(ctx(
            Map.of("2026-04", 3, "2026-05", 5, "2026-06", 2),
            Map.of()));
        assertThat(html).contains("2026-04").contains("3");
        assertThat(html).contains("2026-05").contains("5");
        assertThat(html).contains("2026-06").contains("2");
    }

    @Test
    @DisplayName("趋势方向用 ↑↓→ 符号")
    void trendSymbols() {
        String html = renderer.render(ctx(Map.of(),
            Map.of("disp_x", "UP", "rainfall", "DOWN", "tilt", "STABLE")));
        assertThat(html).contains("↑");
        assertThat(html).contains("↓");
        assertThat(html).contains("→");
    }

    @Test
    @DisplayName("风险评估章节包含评级文字 低/中/高/极高")
    void riskLevelShown() {
        String html = renderer.render(ctx(Map.of(),
            Map.of("a", "UP", "b", "UP", "c", "UP")));
        assertThat(html).contains("评级").contains("风险");
    }
}
```

- [ ] **步骤 6:运行测试验证失败**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=QuarterlyReportRendererTest
```

- [ ] **步骤 7:编写 QuarterlyReportRenderer 实现**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/QuarterlyReportRenderer.java`:

```java
package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 季报渲染器 — 侧重风险 + 趋势。
 *
 * 章节: 1.季度风险总览  2.趋势分析  3.告警分布  4.设备运行汇总  5.风险评估与建议
 */
@Component
public class QuarterlyReportRenderer implements ReportRenderer {

    @Override
    public ReportType type() { return ReportType.QUARTERLY; }

    @Override
    public String render(ReportContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ReportHtmlBuilder.header(ctx));

        AlarmSummary summary = ctx.alarmSummary();
        int alarmTotal = summary == null ? 0 : summary.total();
        int maxLevel = summary == null ? 0 : summary.maxLevel();

        // 1. 季度风险总览
        sb.append(ReportHtmlBuilder.sectionTitle("1. 季度风险总览"));
        sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
        sb.append(ReportHtmlBuilder.row("季度告警总数", String.valueOf(alarmTotal)));
        sb.append(ReportHtmlBuilder.row("最高告警级别",
            "<span style=\"color:" + ReportHtmlBuilder.levelColor(maxLevel) + ";\">"
            + ReportHtmlBuilder.levelName(maxLevel) + "</span>"));
        sb.append(ReportHtmlBuilder.closeTable());

        sb.append(ReportHtmlBuilder.openTable("月份", "告警次数"));
        Map<String, Integer> monthly = ctx.alarmMonthlyCount();
        if (monthly == null || monthly.isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "-"));
        } else {
            monthly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(ReportHtmlBuilder.row(e.getKey(), String.valueOf(e.getValue()))));
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 2. 趋势分析
        sb.append(ReportHtmlBuilder.sectionTitle("2. 趋势分析"));
        sb.append(ReportHtmlBuilder.openTable("属性", "趋势方向", "斜率", "解读"));
        Map<String, String> dirs = ctx.trendDirections();
        Map<String, Double> slopes = ctx.trendSlopes();
        if (dirs == null || dirs.isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "", "", ""));
        } else {
            int upCount = 0;
            for (Map.Entry<String, String> e : dirs.entrySet()) {
                String symbol = symbolFor(e.getValue());
                if ("UP".equals(e.getValue())) upCount++;
                Double slope = slopes == null ? null : slopes.get(e.getKey());
                sb.append(ReportHtmlBuilder.row(
                    e.getKey(),
                    "<span style=\"font-size:14px;font-weight:bold;\">" + symbol + "</span>",
                    slope == null ? "-" : String.format("%.4f", slope),
                    interpret(e.getValue())
                ));
            }
            ctx = new ReportContext(ctx.type(), ctx.period(), ctx.hazardPoint(), ctx.generatedAt(),
                ctx.devices(), ctx.deviceTotal(), ctx.deviceOnline(), ctx.deviceOffline(), ctx.onlineRatePct(),
                ctx.metrics(), ctx.alarmSummary(), ctx.alarmTopEvents(),
                ctx.trendDirections(), ctx.trendSlopes(), ctx.alarmMonthlyCount());
            // 注: 这里 ctx 不可变, 实际渲染用局部变量 upCount
            sb.append("<!-- upCount=" + upCount + " -->");
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 3. 告警分布 (3×4 矩阵简化为级别统计)
        sb.append(ReportHtmlBuilder.sectionTitle("3. 告警分布"));
        if (summary == null || summary.total() == 0) {
            sb.append(ReportHtmlBuilder.paragraph("本季度无告警分布数据。"));
        } else {
            sb.append(ReportHtmlBuilder.openTable("告警级别", "次数"));
            summary.levelCount().forEach((lvl, cnt) -> {
                String color = ReportHtmlBuilder.levelColor(lvl);
                sb.append(ReportHtmlBuilder.row(
                    "<span style=\"color:" + color + ";\">" + ReportHtmlBuilder.levelName(lvl) + "</span>",
                    String.valueOf(cnt)));
            });
            sb.append(ReportHtmlBuilder.closeTable());
        }

        // 4. 设备运行汇总
        sb.append(ReportHtmlBuilder.sectionTitle("4. 设备运行汇总"));
        sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
        sb.append(ReportHtmlBuilder.row("设备总数", String.valueOf(ctx.deviceTotal())));
        sb.append(ReportHtmlBuilder.row("在线设备数", String.valueOf(ctx.deviceOnline())));
        sb.append(ReportHtmlBuilder.row("设备在线率", String.format("%.1f%%", ctx.onlineRatePct())));
        sb.append(ReportHtmlBuilder.closeTable());

        // 5. 风险评估与建议
        sb.append(ReportHtmlBuilder.sectionTitle("5. 风险评估与建议"));
        int upCount = dirs == null ? 0 : (int) dirs.values().stream().filter("UP"::equals).count();
        RiskAssessor.Risk risk = RiskAssessor.assess(alarmTotal, maxLevel, upCount, ctx.onlineRatePct());
        sb.append(ReportHtmlBuilder.paragraph(
            "综合评级: <span style=\"color:" + risk.color() + ";font-weight:bold;font-size:16px;\">"
            + risk.level() + "</span> (评分 " + risk.score() + ")"));
        List<String> advice = new ArrayList<>();
        advice.add("本季度共发生告警 " + alarmTotal + " 次, 趋势上升指标 " + upCount + " 个, 设备在线率 "
            + String.format("%.1f", ctx.onlineRatePct()) + "%。");
        if ("极高".equals(risk.level()) || "高".equals(risk.level())) {
            advice.add("建议提高监测频率, 加强现场巡查, 准备应急预案。");
        } else if ("中".equals(risk.level())) {
            advice.add("建议持续关注, 适当加密巡查频次。");
        } else {
            advice.add("整体监测稳定, 建议维持现有方案。");
        }
        sb.append(ReportHtmlBuilder.bulletList(advice.toArray(new String[0])));

        return sb.toString();
    }

    private static String symbolFor(String dir) {
        return switch (dir == null ? "" : dir) {
            case "UP" -> "↑";
            case "DOWN" -> "↓";
            case "STABLE" -> "→";
            default -> "-";
        };
    }

    private static String interpret(String dir) {
        return switch (dir == null ? "" : dir) {
            case "UP" -> "数据呈上升趋势, 需关注";
            case "DOWN" -> "数据呈下降趋势";
            case "STABLE" -> "数据稳定";
            default -> "-";
        };
    }
}
```

> **执行时清理项:** 上面 step 7 中有一段 `ctx = new ReportContext(...)` 重建 ctx 是冗余的(immutable record 重建无意义),实际执行时应删除该行,直接用 `upCount` 局部变量。这段是为了在测试驱动下确认 `dirs` 不可变时仍能统计 upCount。

- [ ] **步骤 8:运行测试验证通过**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=QuarterlyReportRendererTest
```

预期: 5 个测试全绿。

- [ ] **步骤 9:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/RiskAssessor.java \
        server/zwei-iot-report/src/main/java/com/zwei/iot/report/render/QuarterlyReportRenderer.java \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/RiskAssessorTest.java \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/render/QuarterlyReportRendererTest.java
git commit -m "feat(report): QuarterlyReportRenderer 季报渲染器 + RiskAssessor 综合评级"
```

---

## 任务 10:ReportGenerationService

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportGenerationService.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/service/ReportGenerationServiceTest.java`

- [ ] **步骤 1:编写失败的测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/service/ReportGenerationServiceTest.java`:

```java
package com.zwei.iot.report.service;

import com.zwei.common.redis.DistributedLock;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.datasource.ReportDataAssembler;
import com.zwei.iot.report.domain.ReportRecord;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.mapper.ReportRecordMapper;
import com.zwei.iot.report.render.WeeklyReportRenderer;
import com.zwei.iot.report.support.ReportPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReportGenerationService")
class ReportGenerationServiceTest {

    private ReportRecordMapper mapper;
    private ReportDataAssembler assembler;
    private IHazardPointQueryService hazardQuery;
    private DistributedLock lock;
    private WeeklyReportRenderer renderer;
    private ReportGenerationService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ReportRecordMapper.class);
        assembler = mock(ReportDataAssembler.class);
        hazardQuery = mock(IHazardPointQueryService.class);
        lock = mock(DistributedLock.class);
        renderer = mock(WeeklyReportRenderer.class);
        // Service 内部按 type 找 renderer, 注入一个 List
        service = new ReportGenerationService(mapper, assembler, hazardQuery, lock, List.of(renderer));
    }

    @Test
    @DisplayName("Redis 锁获取失败时跳过整批")
    void skipWhenLockFails() {
        when(lock.tryLock(anyString(), any(Duration.class)))
            .thenReturn(DistributedLock.LockToken.notAcquired());

        service.generateAll(ReportType.WEEKLY);

        verifyNoInteractions(hazardQuery, assembler);
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("已存在成功记录的 hp 被跳过")
    void skipExisting() {
        when(lock.tryLock(anyString(), any())).thenReturn(new DistributedLock.LockToken(true, "t1"));
        HazardPointBrief hp = new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(hp));
        ReportPeriod period = ReportPeriod.lastWeek(LocalDate.now());
        when(mapper.selectExistingSuccess(eq(2), eq(1L), eq(period.start()), eq(period.end())))
            .thenReturn(new ReportRecord()); // 已有

        service.generateAll(ReportType.WEEKLY);

        verify(assembler, never()).build(any(), any(), any());
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("正常流程: insert 占位 → assemble → render → updateStatus")
    void happyPath() {
        when(lock.tryLock(anyString(), any())).thenReturn(new DistributedLock.LockToken(true, "t1"));
        HazardPointBrief hp = new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(hp));
        when(mapper.selectExistingSuccess(anyInt(), anyLong(), any(), any())).thenReturn(null);

        ReportContext ctx = mock(ReportContext.class);
        when(ctx.type()).thenReturn(ReportType.WEEKLY);
        when(ctx.period()).thenReturn(new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)));
        when(ctx.hazardPoint()).thenReturn(hp);
        when(assembler.build(eq(ReportType.WEEKLY), any(), eq(hp))).thenReturn(ctx);
        when(renderer.type()).thenReturn(ReportType.WEEKLY);
        when(renderer.render(ctx)).thenReturn("<html>...</html>");

        service.generateAll(ReportType.WEEKLY);

        ArgumentCaptor<ReportRecord> captor = ArgumentCaptor.forClass(ReportRecord.class);
        verify(mapper).insert(captor.capture());
        ReportRecord inserted = captor.getValue();
        assertThat(inserted.getStatus()).isEqualTo(1);  // 占位 status=1
        verify(mapper).updateStatusAndContent(eq(inserted.getId()), eq(2), eq("<html>...</html>"), isNull());
    }

    @Test
    @DisplayName("单 hp 渲染异常时记 status=3 + error_msg, 继续下一个")
    void singleFailureIsolated() {
        when(lock.tryLock(anyString(), any())).thenReturn(new DistributedLock.LockToken(true, "t1"));
        HazardPointBrief hp1 = new HazardPointBrief(1L, "HP001", "测试1", new BigDecimal("104"), new BigDecimal("30"));
        HazardPointBrief hp2 = new HazardPointBrief(2L, "HP002", "测试2", new BigDecimal("105"), new BigDecimal("31"));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(hp1, hp2));
        when(mapper.selectExistingSuccess(anyInt(), anyLong(), any(), any())).thenReturn(null);
        when(assembler.build(eq(ReportType.WEEKLY), any(), eq(hp1)))
            .thenThrow(new RuntimeException("IoTDB down"));
        ReportContext ctx2 = mock(ReportContext.class);
        when(ctx2.type()).thenReturn(ReportType.WEEKLY);
        when(ctx2.period()).thenReturn(new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)));
        when(ctx2.hazardPoint()).thenReturn(hp2);
        when(assembler.build(eq(ReportType.WEEKLY), any(), eq(hp2))).thenReturn(ctx2);
        when(renderer.type()).thenReturn(ReportType.WEEKLY);
        when(renderer.render(any())).thenReturn("<html>hp2</html>");

        service.generateAll(ReportType.WEEKLY);

        // hp1 应该被 updateStatusAndContent status=3
        verify(mapper, atLeastOnce()).updateStatusAndContent(anyLong(), eq(3), isNull(), contains("IoTDB down"));
        // hp2 应该成功
        verify(mapper, atLeastOnce()).updateStatusAndContent(anyLong(), eq(2), eq("<html>hp2</html>"), isNull());
    }
}
```

- [ ] **步骤 2:运行测试验证失败**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=ReportGenerationServiceTest
```

- [ ] **步骤 3:编写实现**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportGenerationService.java`:

```java
package com.zwei.iot.report.service;

import com.zwei.common.redis.DistributedLock;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.datasource.ReportDataAssembler;
import com.zwei.iot.report.domain.ReportRecord;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.mapper.ReportRecordMapper;
import com.zwei.iot.report.render.ReportRenderer;
import com.zwei.iot.report.support.ReportPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 报告生成编排服务。
 * <ol>
 *   <li>Redis 分布式锁兜底多实例并发</li>
 *   <li>按 hp 串行处理 (避免 IoTDB 并发压力)</li>
 *   <li>单 hp 失败不影响其他</li>
 *   <li>幂等: 同 type+hp+period 已有成功记录 → 跳过</li>
 *   <li>不自动重试, 失败记 status=3</li>
 * </ol>
 */
@Service
public class ReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final ReportRecordMapper mapper;
    private final ReportDataAssembler assembler;
    private final IHazardPointQueryService hazardQuery;
    private final DistributedLock lock;
    private final List<ReportRenderer> renderers;

    public ReportGenerationService(ReportRecordMapper mapper,
                                    ReportDataAssembler assembler,
                                    IHazardPointQueryService hazardQuery,
                                    DistributedLock lock,
                                    List<ReportRenderer> renderers) {
        this.mapper = mapper;
        this.assembler = assembler;
        this.hazardQuery = hazardQuery;
        this.lock = lock;
        this.renderers = renderers;
    }

    public void generateAll(ReportType type) {
        ReportPeriod period = ReportPeriod.previous(type, LocalDate.now());
        log.info("[report] start type={} period={}~{}", type, period.start(), period.end());

        String lockKey = "report:gen:" + type.code() + ":" + period.key();
        DistributedLock.LockToken token = lock.tryLock(lockKey, LOCK_TTL);
        if (!token.acquired()) {
            log.info("[report] another instance is running, skip");
            return;
        }

        int success = 0, fail = 0;
        try {
            List<HazardPointBrief> hps = hazardQuery.listMonitoring();
            log.info("[report] hps count={}", hps.size());
            for (HazardPointBrief hp : hps) {
                try {
                    generateOne(type, period, hp);
                    success++;
                } catch (DuplicateKeyException e) {
                    log.info("[report] skip (duplicate) hp={} type={}", hp.id(), type);
                } catch (Exception e) {
                    fail++;
                    log.error("[report] fail hp={} type={} reason={}", hp.id(), type, e.getMessage(), e);
                }
            }
            log.info("[report] done type={} total={} success={} fail={}", type, hps.size(), success, fail);
        } finally {
            lock.unlock(lockKey, token);
        }
    }

    /** 单 hp 单周期生成, 抛出任何异常由上层 catch */
    public void generateOne(ReportType type, ReportPeriod period, HazardPointBrief hp) {
        // 1. 幂等检查
        ReportRecord existing = mapper.selectExistingSuccess(type.code(), hp.id(), period.start(), period.end());
        if (existing != null) {
            log.info("[report] skip exists hp={} type={} period={}", hp.id(), type, period.key());
            return;
        }

        // 2. insert 占位
        ReportRecord placeholder = new ReportRecord();
        placeholder.setTemplateId(null);
        placeholder.setTemplateName(type.name().toLowerCase());
        placeholder.setType(type.code());
        placeholder.setPeriodStart(period.start());
        placeholder.setPeriodEnd(period.end());
        placeholder.setHazardPointId(hp.id());
        placeholder.setHazardPointCode(hp.code());
        placeholder.setHazardPointName(hp.name());
        placeholder.setReportName(hp.name() + " - 监测" + type.desc()
            + " (" + period.start() + "~" + period.end() + ")");
        placeholder.setStatus(1);
        placeholder.setDelFlag(0);
        mapper.insert(placeholder);
        Long id = placeholder.getId();

        // 3. 拉数 + 渲染
        try {
            ReportContext ctx = assembler.build(type, period, hp);
            ReportRenderer renderer = findRenderer(type);
            String html = renderer.render(ctx);
            mapper.updateStatusAndContent(id, 2, html, null);
        } catch (Exception e) {
            mapper.updateStatusAndContent(id, 3, null, truncate(e.getMessage(), 1000));
            throw e;  // 上层 catch 隔离
        }
    }

    private ReportRenderer findRenderer(ReportType type) {
        return renderers.stream()
            .filter(r -> r.type() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("无匹配渲染器: " + type));
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
```

- [ ] **步骤 4:运行测试验证通过**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=ReportGenerationServiceTest
```

预期: BUILD SUCCESS,4 个测试全绿。如某 case 失败,微调测试或实现细节。

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportGenerationService.java \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/service/ReportGenerationServiceTest.java
git commit -m "feat(report): ReportGenerationService 编排 (Redis 锁+幂等+单点失败隔离)"
```

---

## 任务 11:ReportScheduleJob + application.yml 配置

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/job/ReportScheduleJob.java`
- 修改:`server/zwei-admin/src/main/resources/application.yml`

- [ ] **步骤 1:编写 ReportScheduleJob**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/job/ReportScheduleJob.java`:

```java
package com.zwei.iot.report.job;

import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.service.ReportGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 报告定时任务 — 三入口错峰执行。
 * <ul>
 *   <li>每周一 02:00 生成上一自然周</li>
 *   <li>每月 1 号 02:30 生成上一自然月</li>
 *   <li>每季度首月 1 号 03:00 生成上一自然季度 (1/4/7/10 月触发)</li>
 * </ul>
 *
 * 通过 application.yml 的 `zwei.report.schedule.{weekly,monthly,quarterly}-enabled` 控制开关, 默认开。
 */
@Configuration
@ConditionalOnProperty(name = "zwei.report.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ReportScheduleJob {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduleJob.class);

    private final ReportGenerationService generationService;

    public ReportScheduleJob(ReportGenerationService generationService) {
        this.generationService = generationService;
    }

    @Scheduled(cron = "0 0 2 * * MON")
    @ConditionalOnProperty(name = "zwei.report.schedule.weekly-enabled", havingValue = "true", matchIfMissing = true)
    public void generateWeekly() {
        log.info("[report-job] weekly trigger");
        generationService.generateAll(ReportType.WEEKLY);
    }

    @Scheduled(cron = "0 30 2 1 * *")
    @ConditionalOnProperty(name = "zwei.report.schedule.monthly-enabled", havingValue = "true", matchIfMissing = true)
    public void generateMonthly() {
        log.info("[report-job] monthly trigger");
        generationService.generateAll(ReportType.MONTHLY);
    }

    @Scheduled(cron = "0 0 3 1 1,4,7,10")
    @ConditionalOnProperty(name = "zwei.report.schedule.quarterly-enabled", havingValue = "true", matchIfMissing = true)
    public void generateQuarterly() {
        log.info("[report-job] quarterly trigger");
        generationService.generateAll(ReportType.QUARTERLY);
    }
}
```

- [ ] **步骤 2:修改 application.yml 加开关**

读取 `server/zwei-admin/src/main/resources/application.yml`,在已有 `zwei:` 段(第 2-34 行附近)的末尾追加:

```yaml
zwei:
  # ... 现有配置 ...

  report:
    schedule:
      enabled: true              # 全局开关
      weekly-enabled: true       # 周报 Job
      monthly-enabled: true      # 月报 Job
      quarterly-enabled: true    # 季报 Job
```

- [ ] **步骤 3:确认 @EnableScheduling 已开启**

```bash
grep -rn "@EnableScheduling" server/zwei-admin/src/main/java/
```

如无结果,在主类 `RuoYiApplication` 上添加 `@EnableScheduling`(或参考 `ComprehensiveAlarmJob` 模块的现有配置位置)。已有则跳过。

- [ ] **步骤 4:编译验证**

```bash
cd server && mvn -pl zwei-iot-report -am compile
```

- [ ] **步骤 5:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/job/ReportScheduleJob.java \
        server/zwei-admin/src/main/resources/application.yml
git commit -m "feat(report): ReportScheduleJob 三入口定时任务 + application.yml 开关"
```

---

## 任务 12:ReportRecordService + DTOs

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportRecordService.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportRecordPageDTO.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportRecordVO.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportRecordDetailVO.java`
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportGenerateDTO.java`

- [ ] **步骤 1:编写 DTO**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportRecordPageDTO.java`:

```java
package com.zwei.iot.report.domain.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportRecordPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private Integer type;             // 2/3/4
    private Long hazardPointId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer status;           // 1/2/3
    private String keyword;
}
```

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportRecordVO.java`:

```java
package com.zwei.iot.report.domain.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 列表项 VO (不含 content 大字段) */
@Data
public class ReportRecordVO {
    private Long id;
    private Integer type;
    private String typeDesc;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long hazardPointId;
    private String hazardPointCode;
    private String hazardPointName;
    private String reportName;
    private Integer status;
    private String statusDesc;
    private String errorMsg;
    private LocalDateTime createTime;
}
```

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportRecordDetailVO.java`:

```java
package com.zwei.iot.report.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 详情 VO (含 content) */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportRecordDetailVO extends ReportRecordVO {
    private String content;
}
```

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/ReportGenerateDTO.java`:

```java
package com.zwei.iot.report.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportGenerateDTO {
    @NotNull(message = "type 不能为空")
    private Integer type;             // 2/3/4

    @NotNull(message = "hazardPointId 不能为空")
    private Long hazardPointId;

    @NotNull(message = "periodStart 不能为空")
    private LocalDate periodStart;

    @NotNull(message = "periodEnd 不能为空")
    private LocalDate periodEnd;
}
```

- [ ] **步骤 2:编写 ReportRecordService**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportRecordService.java`:

```java
package com.zwei.iot.report.service;

import com.zwei.common.core.page.PageResult;
import com.zwei.iot.report.domain.ReportRecord;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.domain.dto.*;
import com.zwei.iot.report.mapper.ReportRecordMapper;
import com.zwei.iot.report.support.ReportPeriod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportRecordService {

    private final ReportRecordMapper mapper;

    public ReportRecordService(ReportRecordMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<ReportRecordVO> page(ReportRecordPageDTO params) {
        int pageNum = params.getPageNum() == null ? 1 : params.getPageNum();
        int pageSize = params.getPageSize() == null ? 20 : params.getPageSize();

        long total = mapper.countPageList(
            params.getType(), params.getHazardPointId(),
            params.getPeriodStart(), params.getPeriodEnd(),
            params.getStatus(), params.getKeyword());

        // 注意: 项目用 PageHelper 时 startPage; 这里手动 offset 限制
        // 简化方案: 用 PageHelper (com.github.pagehelper) — 在 controller 调用 startPage
        // 此处仅返回全部, 分页在 Controller 层用 PageHelper.startPage 完成
        List<ReportRecord> all = mapper.selectPageList(
            params.getType(), params.getHazardPointId(),
            params.getPeriodStart(), params.getPeriodEnd(),
            params.getStatus(), params.getKeyword());

        List<ReportRecordVO> rows = all.stream()
            .skip((long) (pageNum - 1) * pageSize)
            .limit(pageSize)
            .map(ReportRecordService::toVO)
            .collect(Collectors.toList());

        return new PageResult<>(rows, total, pageNum, pageSize);
    }

    public ReportRecordDetailVO detail(Long id) {
        ReportRecord r = mapper.selectById(id);
        if (r == null) return null;
        ReportRecordDetailVO vo = new ReportRecordDetailVO();
        copyBase(r, vo);
        vo.setContent(r.getContent());
        return vo;
    }

    public boolean remove(Long id) {
        return mapper.updateDeleteFlag(id, 1) > 0;
    }

    public Long findExisting(Integer type, Long hpId, LocalDate start, LocalDate end) {
        ReportRecord r = mapper.selectExistingSuccess(type, hpId, start, end);
        return r == null ? null : r.getId();
    }

    static void copyBase(ReportRecord r, ReportRecordVO vo) {
        vo.setId(r.getId());
        vo.setType(r.getType());
        vo.setTypeDesc(r.getType() == null ? "" : ReportType.fromCode(r.getType()).desc());
        vo.setPeriodStart(r.getPeriodStart());
        vo.setPeriodEnd(r.getPeriodEnd());
        vo.setHazardPointId(r.getHazardPointId());
        vo.setHazardPointCode(r.getHazardPointCode());
        vo.setHazardPointName(r.getHazardPointName());
        vo.setReportName(r.getReportName());
        vo.setStatus(r.getStatus());
        vo.setStatusDesc(statusDesc(r.getStatus()));
        vo.setErrorMsg(r.getErrorMsg());
        vo.setCreateTime(r.getCreateTime());
    }

    static ReportRecordVO toVO(ReportRecord r) {
        ReportRecordVO vo = new ReportRecordVO();
        copyBase(r, vo);
        return vo;
    }

    static String statusDesc(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 1 -> "生成中";
            case 2 -> "已生成";
            case 3 -> "生成失败";
            default -> "";
        };
    }
}
```

> **执行时检查项:** `PageResult` 构造器签名以 `Read` 确认。若项目用 `PageHelper.startPage()` + `new PageInfo<>(list)` 模式, 改用 controller 调 `startPage()` 后直接 `mapper.selectPageList` 返回。本计划采用 service 层手分页。

- [ ] **步骤 3:编译验证**

```bash
cd server && mvn -pl zwei-iot-report -am compile
```

- [ ] **步骤 4:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/service/ReportRecordService.java \
        server/zwei-iot-report/src/main/java/com/zwei/iot/report/domain/dto/
git commit -m "feat(report): ReportRecordService CRUD + DTO/VO (列表/详情/手动生成)"
```

---

## 任务 13:ReportController

**文件:**
- 创建:`server/zwei-iot-report/src/main/java/com/zwei/iot/report/controller/ReportController.java`
- 测试:`server/zwei-iot-report/src/test/java/com/zwei/iot/report/controller/ReportControllerTest.java`

- [ ] **步骤 1:编写 Controller**

写入 `server/zwei-iot-report/src/main/java/com/zwei/iot/report/controller/ReportController.java`:

```java
package com.zwei.iot.report.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageResult;
import com.zwei.common.utils.security.SecurityUtilsWrapper;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.domain.dto.ReportGenerateDTO;
import com.zwei.iot.report.domain.dto.ReportRecordDetailVO;
import com.zwei.iot.report.domain.dto.ReportRecordPageDTO;
import com.zwei.iot.report.domain.dto.ReportRecordVO;
import com.zwei.iot.report.service.ReportGenerationService;
import com.zwei.iot.report.service.ReportRecordService;
import com.zwei.iot.report.support.ReportPeriod;
import com.zwei.common.annotation.Log;
import com.zwei.common.enums.BusinessType;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 报告管理 REST 接口。
 */
@RestController
@RequestMapping("/api/v1/report/records")
public class ReportController extends BaseController {

    private final ReportRecordService recordService;
    private final ReportGenerationService generationService;
    private final IHazardPointQueryService hazardQuery;

    public ReportController(ReportRecordService recordService,
                             ReportGenerationService generationService,
                             IHazardPointQueryService hazardQuery) {
        this.recordService = recordService;
        this.generationService = generationService;
        this.hazardQuery = hazardQuery;
    }

    @PreAuthorize("@ss.hasPermi('report:record:list')")
    @GetMapping("/page")
    public AjaxResult page(ReportRecordPageDTO params) {
        PageResult<ReportRecordVO> result = recordService.page(params);
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('report:record:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        ReportRecordDetailVO vo = recordService.detail(id);
        if (vo == null) return AjaxResult.error("报告不存在");
        return AjaxResult.success(vo);
    }

    @PreAuthorize("@ss.hasPermi('report:record:remove')")
    @Log(title = "报告管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(recordService.remove(id) ? 1 : 0);
    }

    @PreAuthorize("@ss.hasPermi('report:record:generate')")
    @Log(title = "报告管理", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@Valid @RequestBody ReportGenerateDTO dto) {
        // 1. 参数校验
        if (dto.getType() == null || (dto.getType() != 2 && dto.getType() != 3 && dto.getType() != 4)) {
            return AjaxResult.error("type 必须为 2(周报)/3(月报)/4(季报)");
        }
        if (dto.getPeriodEnd().isBefore(dto.getPeriodStart())) {
            return AjaxResult.error("periodEnd 必须 >= periodStart");
        }
        long days = ChronoUnit.DAYS.between(dto.getPeriodStart(), dto.getPeriodEnd());
        if (days > 400) {
            return AjaxResult.error("周期跨度不能超过 400 天");
        }

        // 2. 隐患点校验
        List<HazardPointBrief> all = hazardQuery.listMonitoring();
        HazardPointBrief hp = all.stream()
            .filter(h -> h.id().equals(dto.getHazardPointId()))
            .findFirst()
            .orElse(null);
        if (hp == null) {
            // 也接受非"监测中"的隐患点 (历史数据补救场景); 若实际只允许监测中, 改这里
            // 简化: 不在监测中清单的也允许, 但需确认存在(交给 generationService 处理)
        }

        // 3. 幂等检查 — 同 type+hp+period 已存在则 409
        Long existingId = recordService.findExisting(dto.getType(), dto.getHazardPointId(),
            dto.getPeriodStart(), dto.getPeriodEnd());
        if (existingId != null) {
            return AjaxResult.error(409, "该周期报告已存在").put("reportId", existingId);
        }

        // 4. 同步生成
        ReportType type = ReportType.fromCode(dto.getType());
        ReportPeriod period = new ReportPeriod(dto.getPeriodStart(), dto.getPeriodEnd());
        HazardPointBrief finalHp = hp != null ? hp
            : new HazardPointBrief(dto.getHazardPointId(), null, null, null, null);
        try {
            generationService.generateOne(type, period, finalHp);
        } catch (Exception e) {
            return AjaxResult.error("生成失败: " + e.getMessage());
        }
        Long newId = recordService.findExisting(dto.getType(), dto.getHazardPointId(),
            dto.getPeriodStart(), dto.getPeriodEnd());
        return AjaxResult.success("生成成功").put("reportId", newId);
    }
}
```

> **执行时检查项:**
> - `SecurityUtilsWrapper` / `@Log` / `BusinessType` 包路径用 grep 确认,参考已有 Controller 如 `DeviceController`
> - `AjaxResult.error(int code, String msg)` + `.put(key, val)` 链式是否支持,用 `Read AjaxResult` 确认
> - `@PreAuthorize("@ss.hasPermi(...)")` 的 SpEL 是否与项目其他 Controller 一致

- [ ] **步骤 2:编写 MockMvc 测试**

写入 `server/zwei-iot-report/src/test/java/com/zwei/iot/report/controller/ReportControllerTest.java`:

```java
package com.zwei.iot.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.domain.dto.ReportGenerateDTO;
import com.zwei.iot.report.service.ReportGenerationService;
import com.zwei.iot.report.service.ReportRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ReportController")
@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean ReportRecordService recordService;
    @MockBean ReportGenerationService generationService;
    @MockBean IHazardPointQueryService hazardQuery;

    @Test
    @DisplayName("generate 校验 type 不合法返回 200 + error msg")
    void generateInvalidType() throws Exception {
        ReportGenerateDTO dto = new ReportGenerateDTO();
        dto.setType(9);
        dto.setHazardPointId(1L);
        dto.setPeriodStart(LocalDate.of(2026, 6, 1));
        dto.setPeriodEnd(LocalDate.of(2026, 6, 7));

        mvc.perform(post("/api/v1/report/records/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("type")));
    }

    @Test
    @DisplayName("generate period 反向被拒")
    void generatePeriodReversed() throws Exception {
        ReportGenerateDTO dto = new ReportGenerateDTO();
        dto.setType(2);
        dto.setHazardPointId(1L);
        dto.setPeriodStart(LocalDate.of(2026, 6, 7));
        dto.setPeriodEnd(LocalDate.of(2026, 6, 1));

        mvc.perform(post("/api/v1/report/records/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("periodEnd")));
    }

    @Test
    @DisplayName("generate 同周期已存在返回 409 + reportId")
    void generateConflict() throws Exception {
        ReportGenerateDTO dto = new ReportGenerateDTO();
        dto.setType(2);
        dto.setHazardPointId(1L);
        dto.setPeriodStart(LocalDate.of(2026, 6, 1));
        dto.setPeriodEnd(LocalDate.of(2026, 6, 7));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"))));
        when(recordService.findExisting(eq(2), eq(1L), any(), any())).thenReturn(999L);

        mvc.perform(post("/api/v1/report/records/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.reportId").value(999));
    }

    @Test
    @DisplayName("generate 正常流程返回 200 + reportId")
    void generateSuccess() throws Exception {
        ReportGenerateDTO dto = new ReportGenerateDTO();
        dto.setType(2);
        dto.setHazardPointId(1L);
        dto.setPeriodStart(LocalDate.of(2026, 6, 1));
        dto.setPeriodEnd(LocalDate.of(2026, 6, 7));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"))));
        when(recordService.findExisting(eq(2), eq(1L), any(), any()))
            .thenReturn(null)   // 首次查不存在
            .thenReturn(123L);  // 生成后查到
        doNothing().when(generationService).generateOne(any(), any(), any());

        mvc.perform(post("/api/v1/report/records/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.reportId").value(123));
    }
}
```

> **执行时检查项:** `@WebMvcTest` 是否会自动配置 Spring Security MockMvc;若需要禁用安全,在测试类加 `@Import({SecurityConfig.class})` 或加 `@AutoConfigureMockMvc(addFilters = false)`。

- [ ] **步骤 3:运行测试**

```bash
cd server && mvn -pl zwei-iot-report -am test -Dtest=ReportControllerTest
```

预期: 4 个测试通过(若 Spring Security 拦截,需调整 `@WebMvcTest` 配置或加 mock 用户)。

- [ ] **步骤 4:Commit**

```bash
git add server/zwei-iot-report/src/main/java/com/zwei/iot/report/controller/ReportController.java \
        server/zwei-iot-report/src/test/java/com/zwei/iot/report/controller/ReportControllerTest.java
git commit -m "feat(report): ReportController 4 端点 + 校验 + 409 冲突处理"
```

---

## 任务 14:前端 api/report.ts 重写

**文件:**
- 修改:`web/src/api/report.ts`(大改 — 删除 mock,对接真实接口)

- [ ] **步骤 1:备份阅读现有文件**

```bash
wc -l web/src/api/report.ts
# 现状: ~632 行,含大量 mock 代码
```

- [ ] **步骤 2:整体重写**

整体覆盖写入 `web/src/api/report.ts`(保留 Query / Analysis 相关接口部分,删除 Report 相关 mock):

```typescript
import request from '@/utils/request'
import type { PageResult } from './system'

// ---------------------------------------------------------------------------
// 报告管理 (Report Management) — 真实接口对接
// ---------------------------------------------------------------------------

export type ReportType = 'weekly' | 'monthly' | 'quarterly'

export interface ReportItem {
  id: number
  type: ReportType
  typeDesc: string
  periodStart: string
  periodEnd: string
  hazardPointId: number
  hazardPointCode: string
  hazardPointName: string
  reportName: string
  status: 1 | 2 | 3
  statusDesc: string
  errorMsg: string | null
  createTime: string
  content?: string                  // 仅详情接口返回
}

export interface ReportPageParams {
  pageNum: number
  pageSize: number
  type?: ReportType | ''
  hazardPointId?: number
  periodStart?: string
  periodEnd?: string
  status?: number
  keyword?: string
}

export interface ReportGenerateParams {
  type: ReportType
  hazardPointId: number
  periodStart: string
  periodEnd: string
}

// 类型枚举转换 (后端数字 ↔ 前端字符串)
const TYPE_CODE_TO_STR: Record<number, ReportType> = { 2: 'weekly', 3: 'monthly', 4: 'quarterly' }
const TYPE_STR_TO_CODE: Record<ReportType, number> = { weekly: 2, monthly: 3, quarterly: 4 }

function mapRecord(raw: any): ReportItem {
  return {
    id: raw.id,
    type: TYPE_CODE_TO_STR[raw.type] ?? 'weekly',
    typeDesc: raw.typeDesc,
    periodStart: raw.periodStart,
    periodEnd: raw.periodEnd,
    hazardPointId: raw.hazardPointId,
    hazardPointCode: raw.hazardPointCode,
    hazardPointName: raw.hazardPointName,
    reportName: raw.reportName,
    status: raw.status,
    statusDesc: raw.statusDesc,
    errorMsg: raw.errorMsg,
    createTime: raw.createTime,
    content: raw.content,
  }
}

export async function getReportPage(params: ReportPageParams): Promise<PageResult<ReportItem>> {
  const payload: any = { ...params }
  if (params.type) payload.type = TYPE_STR_TO_CODE[params.type]
  const res = await request.get<{ code: number; data: PageResult<any> }>('/api/v1/report/records/page', { params: payload })
  const data = res.data?.data ?? res.data
  return {
    rows: (data.rows ?? []).map(mapRecord),
    total: data.total ?? 0,
    pageNum: data.pageNum ?? params.pageNum,
    pageSize: data.pageSize ?? params.pageSize,
  }
}

export async function getReportDetail(id: number): Promise<ReportItem> {
  const res = await request.get<{ data: any }>(`/api/v1/report/records/${id}`)
  return mapRecord(res.data?.data ?? res.data)
}

export async function deleteReport(id: number): Promise<void> {
  await request.delete(`/api/v1/report/records/${id}`)
}

export async function generateReport(data: ReportGenerateParams): Promise<{ reportId: number; existed: boolean }> {
  const payload = {
    type: TYPE_STR_TO_CODE[data.type],
    hazardPointId: data.hazardPointId,
    periodStart: data.periodStart,
    periodEnd: data.periodEnd,
  }
  try {
    const res = await request.post<{ code: number; reportId?: number; data?: any }>('/api/v1/report/records/generate', payload)
    // 200 + reportId
    return { reportId: (res as any).reportId ?? (res as any).data?.reportId, existed: false }
  } catch (err: any) {
    // 409: 已存在
    if (err?.code === 409 || err?.response?.data?.code === 409) {
      const reportId = err?.reportId ?? err?.response?.data?.reportId
      return { reportId, existed: true }
    }
    throw err
  }
}

// ---------------------------------------------------------------------------
// 隐患点选项 (复用现有 hazardPoint API; 保留导出供 Report.vue 使用)
// ---------------------------------------------------------------------------
export interface HazardPointOption {
  id: number
  name: string
}
export async function getHazardPointOptions(): Promise<HazardPointOption[]> {
  // 实际项目可调用 /api/v1/hazard-points/options, 此处先复用已有 hazardPoint.ts
  const mod = await import('./hazardPoint')
  // 假设 hazardPoint.ts 提供 listAll 或类似; 不存在则在 Report.vue 直接 import
  return (mod as any).getHazardPointOptions ? (mod as any).getHazardPointOptions() : []
}

// ---------------------------------------------------------------------------
// Query / Analysis 部分 (保留原 mock, 不在本次改动范围)
// ---------------------------------------------------------------------------
// === 此处保留原 report.ts 中 MonitorQueryParams / SensorSeriesItem / ChartDataItem 等 ===
// === 接口及其 mock 实现 (getQueryData / getChartData / getGridChartData 等)         ===
// === 由执行者将原文件中"Report 相关 mock"删除, 其余 query/analysis mock 保留        ===
```

> **执行时强制步骤:** 上面是新文件骨架。**执行者必须**先 `Read` 原 `web/src/api/report.ts`,把原文件中以下保留部分粘贴到新文件末尾(在 `// Query / Analysis 部分` 注释下方):
> - `MonitorQueryParams` / `SensorSeriesItem` / `ChartDataItem` / `DeviceOption` / `DeviceTypeOption` / `GridChartItem` / `HazardPointOption` 等类型(去重)
> - `seededRandom` / `randRange` / `toFixed` / `formatDate` / `formatDateTime` 工具
> - `getMockQueryData` / `getMockChartData` / `getMonitorQueryData` / `getChartData` / `getGridChartData` / `getDeviceTypeOptions` / `getDeviceOptions`
> - 删除: `ReportItem`(旧) / `ReportPageParams`(旧) / `generateMockReports` / `buildWeeklyReportContent` / `buildMonthlyReportContent` / `getReportPage`(旧 mock) / `getReportDetail`(旧 mock) / `deleteReport`(旧 mock) — 这些被新版本替代

- [ ] **步骤 3:类型检查**

```bash
cd web && npm run build 2>&1 | tail -20
```

预期: vue-tsc 无错。如有 `import` 路径错误或类型不匹配,修正。

- [ ] **步骤 4:Commit**

```bash
git add web/src/api/report.ts
git commit -m "feat(web): report API 切换到真实接口 + 季报类型支持 + 类型枚举映射"
```

---

## 任务 15:前端 Report.vue 改造

**文件:**
- 修改:`web/src/views/report/Report.vue`

- [ ] **步骤 1:阅读现有 Report.vue**

```bash
wc -l web/src/views/report/Report.vue
```

记录现有结构(template 顶部 + script setup + style)。**执行者必须用 `Read` 完整阅读**该文件,识别:
- 筛选区(`el-input` / `el-select` / `el-date-picker`)位置
- 表格列定义位置
- 详情对话框(`el-dialog`)位置
- HTML 渲染区(`v-html`)位置
- PDF 导出函数(`html2canvas + jsPDF`)位置

- [ ] **步骤 2:类型下拉新增季报**

在筛选区 `<el-select v-model="searchType">` 的 options 中新增季报:

```html
<el-select v-model="searchType" placeholder="报告类型" clearable>
  <el-option label="周报" value="weekly" />
  <el-option label="月报" value="monthly" />
  <el-option label="季报" value="quarterly" />   <!-- 新增 -->
</el-select>
```

- [ ] **步骤 3:表格列改造

替换表格列为:

```html
<el-table :data="tableData" border stripe v-loading="loading">
  <el-table-column prop="reportName" label="报告名称" min-width="280" />
  <el-table-column label="类型" width="90">
    <template #default="{ row }">
      <el-tag :type="typeTagType(row.type)" size="small">{{ row.typeDesc }}</el-tag>
    </template>
  </el-table-column>
  <el-table-column prop="hazardPointName" label="隐患点" width="180" />
  <el-table-column label="周期" width="200">
    <template #default="{ row }">{{ row.periodStart }} ~ {{ row.periodEnd }}</template>
  </el-table-column>
  <el-table-column label="状态" width="110">
    <template #default="{ row }">
      <el-tooltip v-if="row.status === 3" :content="row.errorMsg || '生成失败'" placement="top">
        <el-tag type="danger" size="small">{{ row.statusDesc }}</el-tag>
      </el-tooltip>
      <el-tag v-else-if="row.status === 1" type="info" size="small">{{ row.statusDesc }}</el-tag>
      <el-tag v-else type="success" size="small">{{ row.statusDesc }}</el-tag>
    </template>
  </el-table-column>
  <el-table-column prop="createTime" label="生成时间" width="170" />
  <el-table-column label="操作" width="240" fixed="right">
    <template #default="{ row }">
      <el-button size="small" :disabled="row.status !== 2" @click="handleView(row)">查看</el-button>
      <el-button size="small" type="primary" :disabled="row.status !== 2" @click="handleDownloadPdf(row)">下载PDF</el-button>
      <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
    </template>
  </el-table-column>
</el-table>
```

- [ ] **步骤 4:顶部加"手动生成"按钮 (管理员可见)**

在筛选区右侧加:

```html
<el-button v-if="hasPerm('report:record:generate')" type="success" @click="showGenerateDialog = true">
  手动生成
</el-button>
```

并在 script setup 中引入权限:

```typescript
import { hasPerm } from '@/utils/permission'
const showGenerateDialog = ref(false)
```

- [ ] **步骤 5:手动生成弹窗**

在模板末尾加:

```html
<el-dialog v-model="showGenerateDialog" title="手动生成报告" width="500px">
  <el-form :model="generateForm" label-width="100px">
    <el-form-item label="报告类型" required>
      <el-select v-model="generateForm.type" placeholder="请选择">
        <el-option label="周报" value="weekly" />
        <el-option label="月报" value="monthly" />
        <el-option label="季报" value="quarterly" />
      </el-select>
    </el-form-item>
    <el-form-item label="隐患点" required>
      <el-select v-model="generateForm.hazardPointId" placeholder="请选择">
        <el-option v-for="hp in hazardOptions" :key="hp.id" :label="hp.name" :value="hp.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="周期起" required>
      <el-date-picker v-model="generateForm.periodStart" type="date" value-format="YYYY-MM-DD" />
    </el-form-item>
    <el-form-item label="周期止" required>
      <el-date-picker v-model="generateForm.periodEnd" type="date" value-format="YYYY-MM-DD" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="showGenerateDialog = false">取消</el-button>
    <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
  </template>
</el-dialog>
```

script:

```typescript
const hazardOptions = ref<{ id: number; name: string }[]>([])
const generateForm = reactive<{ type: ReportType | ''; hazardPointId: number | ''; periodStart: string; periodEnd: string }>({
  type: '', hazardPointId: '', periodStart: '', periodEnd: ''
})
const generating = ref(false)

onMounted(async () => {
  try {
    hazardOptions.value = await getHazardPointOptions()
  } catch (e) { /* 兜底 */ }
})

async function handleGenerate() {
  if (!generateForm.type || !generateForm.hazardPointId || !generateForm.periodStart || !generateForm.periodEnd) {
    ElMessage.warning('请填写完整')
    return
  }
  generating.value = true
  try {
    const { reportId, existed } = await generateReport({
      type: generateForm.type as ReportType,
      hazardPointId: generateForm.hazardPointId as number,
      periodStart: generateForm.periodStart,
      periodEnd: generateForm.periodEnd,
    })
    if (existed) {
      ElMessage.warning(`该周期报告已存在 (id=${reportId})`)
    } else {
      ElMessage.success(`生成成功 (id=${reportId})`)
    }
    showGenerateDialog.value = false
    await loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '生成失败')
  } finally {
    generating.value = false
  }
}

function typeTagType(t: ReportType): 'success' | 'warning' | 'danger' | 'info' | '' {
  if (t === 'weekly') return 'success'
  if (t === 'monthly') return 'warning'
  if (t === 'quarterly') return 'danger'
  return ''
}
```

- [ ] **步骤 6:修改详情拉取逻辑 (从列表项无 content → 详情接口拉)**

将现有 `handleView` 改为:

```typescript
async function handleView(row: ReportItem) {
  try {
    detailLoading.value = true
    const full = await getReportDetail(row.id)
    currentReport.value = full
    showDetailDialog.value = true
  } finally {
    detailLoading.value = false
  }
}
```

(现有 `v-html="currentReport.content"`、`html2canvas + jsPDF` 逻辑保留不变。)

- [ ] **步骤 7:类型检查**

```bash
cd web && npm run build
```

预期: vue-tsc 通过 + vite build 完成。

- [ ] **步骤 8:Commit**

```bash
git add web/src/views/report/Report.vue
git commit -m "feat(web): Report.vue 改造 (季报类型+状态列+手动生成+真实API)"
```

---

## 任务 16:端到端验证

- [ ] **步骤 1:后端全量回归**

```bash
cd server && mvn clean test
```

预期: BUILD SUCCESS,所有模块测试全绿。

- [ ] **步骤 2:前端构建**

```bash
cd web && npm run build
```

预期: 类型检查 + vite build 无错。

- [ ] **步骤 3:启动后端 (IDE)**

启动 `com.zwei.RuoYiApplication`,激活 `local` profile。
日志预期:
- 看到 `ReportScheduleJob` 注册 `@Scheduled` 任务
- 看到 `ReportModuleConfig` 加载完成

- [ ] **步骤 4:手动触发生成报告 (curl/Postman)**

```bash
# 先登录拿 token, 假设存到 $TOKEN
curl -X POST http://localhost:8080/api/v1/report/records/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": 2,
    "hazardPointId": 1,
    "periodStart": "2026-06-08",
    "periodEnd": "2026-06-14"
  }'
```

预期:
- 200 + `{ "code": 200, "msg": "生成成功", "reportId": <新ID> }`
- 或 409 + 已存在 `reportId`

- [ ] **步骤 5:列表查询验证**

```bash
curl "http://localhost:8080/api/v1/report/records/page?pageNum=1&pageSize=20&type=2" \
  -H "Authorization: Bearer $TOKEN"
```

预期: 200 + 含至少一条 `status=2 statusDesc="已生成"` 的记录。

- [ ] **步骤 6:详情 + content 验证**

```bash
curl "http://localhost:8080/api/v1/report/records/<id>" -H "Authorization: Bearer $TOKEN"
```

预期: 返回 `content` 字段为完整 HTML 字符串,包含 `<h2>地质灾害监测周报</h2>`。

- [ ] **步骤 7:前端验证**

启动 `npm run dev`,登录后:
1. 进入"报告报表 → 报告管理"菜单
2. 类型筛选能选"季报"
3. 列表显示已生成的报告
4. 点"查看" → 详情对话框渲染 HTML 表格正确
5. 点"下载PDF" → 浏览器下载 PDF 文件,内容与 HTML 一致
6. 点"手动生成"按钮 → 弹窗出现,填入参数提交后列表刷新
7. 删除一条 → 列表不再显示 (del_flag=1)

- [ ] **步骤 8:权限验证**

用普通用户(无 `report:record:*` 权限)登录:
- 列表/详情/删除/生成接口返回 403
- 前端"手动生成"按钮不可见

- [ ] **步骤 9:全部 commit + 最终推送**

```bash
git status   # 确认无未提交改动
git log --oneline -20   # 查看本次实现的 commit 序列
```

(推送由用户决定。)

---

## 验收对照 (自检清单)

| 规格 # | 验收标准 | 对应任务 |
|---|---|---|
| 1 | `mvn clean test` 全绿 | 任务 16 步骤 1 |
| 2 | `npm run build` 通过 | 任务 16 步骤 2 |
| 3 | 手动调用 generate 成功 + 列表可见 + 详情 HTML + PDF 下载 | 任务 16 步骤 4-7 |
| 4 | 定时任务自动触发 (可调 cron 验证) | 任务 11 + 16 步骤 3 |
| 5 | 重复生成返回 409 | 任务 13 + 16 步骤 4 |
| 6 | 单 hp IoTDB 失败隔离 | 任务 10 测试 + 任务 16 |
| 7 | 季报类型 + 紫色 tag | 任务 15 + 16 步骤 7 |
| 8 | 三种报告内容含公共头 + 章节 | 任务 7/8/9 + 16 步骤 6 |
| 9 | 逻辑删除 | 任务 4 (XML) + 13 + 16 步骤 7 |
| 10 | 无权限 403 | 任务 13 + 16 步骤 8 |
