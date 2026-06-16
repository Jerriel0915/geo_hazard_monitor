# 待办告警对接后端 API 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 删除 `RealtimeAlarm.vue` 的硬编码 mock，对接 `/api/v1/alarm/records/*` 真实后端 API；同时重构 `AlarmRecordLog` → `AlarmRecordActionLog`（含 action_type 枚举 + 新字段），新增 `alarm_record_trigger_detail` 表与接口，补齐引擎/处置/通知三处的动作日志写入。

**架构：**
- 后端：MySQL 升级脚本 → Java 枚举/domain/mapper/service/controller 改造；`createOrUpdateAlarm` 拆分为 CREATE/RE_TRIGGER/LEVEL_CHANGE 三分支并双写 trigger_detail + action_log；dispose/batchDispose 改用 action_type 枚举；AlarmNotifier 补写 NOTIFY。
- 前端：`api/alarm.ts` 扩展参数与函数 → `RealtimeAlarm.vue` 删除 mock 并对接分页查询/批量处置 → `AlarmDetailDialog.vue` 并发拉取基础信息/触发明细/动作日志 → `FeedbackDialog` 附件上传走 `/common/upload`。

**技术栈：** Java 17 + Spring Boot 4.0.3 + MyBatis + Lombok（后端）；Vue 3 + TypeScript + Element Plus 2.6 + Axios（前端）；MySQL 8.0（升级脚本）；JUnit 5 + Mockito + AssertJ（测试）。

**规格来源：** `docs/superpowers/specs/2026-06-16-alarm-realtime-api-integration-design.md`

---

## 文件结构

### 新建文件

| 路径 | 职责 |
|---|---|
| `db/upgrade/v2.5-alarm-action-log.sql` | 幂等升级：新建 trigger_detail 表 + 重命名 log → action_log + 字段调整 + 数据迁移 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/ActionType.java` | action_type 枚举常量类（CREATE/RE_TRIGGER/LEVEL_CHANGE/FEEDBACK/DISPOSE_CLOSE/DISPOSE_FALSE_ALARM/NOTIFY） |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecordActionLog.java` | 重命名自 `AlarmRecordLog`，字段 actionType/fromValue/toValue/remarks/description/attachments |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordActionLogMapper.java` | 重命名自 `AlarmRecordLogMapper` |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordActionLogMapper.xml` | 重命名自 `AlarmRecordLogMapper.xml`，表名改为 `alarm_record_action_log` |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecordTriggerDetail.java` | 触发明细实体 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordTriggerDetailMapper.java` | 触发明细 Mapper |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordTriggerDetailMapper.xml` | 触发明细 XML |

### 修改文件

| 路径 | 改动 |
|---|---|
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecord.java` | 追加非持久化筛选字段（alarmLevels/alarmTypes/statusList/triggerTimeBegin/triggerTimeEnd） |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordMapper.java` | 新增 `updateAlarmLevel`；`selectPendingRecords` XML 扩展筛选 |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordMapper.xml` | 扩展 selectPendingRecords；新增 updateAlarmLevel |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/AlarmRecordDisposeRequest.java` | 加 description/attachments/remarks |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/BatchDisposeRequest.java` | 加 description/attachments/remarks |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmRecordService.java` | 改 selectLogsByAlarmId → selectActionLogsByAlarmId；新增 selectTriggerDetailsByAlarmId；dispose/batchDispose 签名加新参数 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImpl.java` | 核心改造：createOrUpdateAlarm 三分支 + dispose/batchDispose + 新方法 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java` | 注入 ActionLogMapper，dispatch 后写 NOTIFY |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmRecordController.java` | 新增 `/trigger-details` `/action-logs` 端点；dispose/batch 传新字段 |
| `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImplTest.java` | 改名为 ActionLog 断言；新增 LEVEL_CHANGE 三分支测试 |
| `web/src/api/alarm.ts` | 类型扩展 + 新增 getTriggerDetails/getActionLogs；改名 getAlarmRecordLogs → getActionLogs |
| `web/src/views/alarm/RealtimeAlarm.vue` | 删除 mock，对接 API，移除"人员名称"框，导出置灰 |
| `web/src/views/alarm/components/AlarmDetailDialog.vue` | 4 tab 对接真实数据；时间线由 action_logs 构造 |
| `web/src/components/FeedbackDialog.vue` | 附件上传走 /common/upload；提交调 disposeAlarm |

### 删除文件

| 路径 | 原因 |
|---|---|
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecordLog.java` | 已重命名为 AlarmRecordActionLog |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordLogMapper.java` | 已重命名 |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordLogMapper.xml` | 已重命名 |

---

## 任务 1：创建升级 SQL `db/upgrade/v2.5-alarm-action-log.sql`

**文件：**
- 创建：`db/upgrade/v2.5-alarm-action-log.sql`

- [ ] **步骤 1：编写幂等升级脚本**

```sql
-- ============================================================
-- 告警动作日志体系升级
-- 版本: v2.5
-- 描述:
--   1) 新建 alarm_record_trigger_detail (告警触发明细)
--   2) 重命名 alarm_record_log → alarm_record_action_log
--   3) 字段调整: disposal_type→action_type, from/to_status→from/to_value,
--      note→remarks, disposal_result→description, 新增 attachments
--   4) 数据迁移: 旧 disposal_type 映射为新 action_type 枚举
-- 幂等: 每步判断存在性，可重复执行
-- ============================================================

-- ---------- 1. 新建触发明细表 ----------
CREATE TABLE IF NOT EXISTS `alarm_record_trigger_detail` (
    `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `alarm_record_id` bigint        NOT NULL                COMMENT '告警记录ID',
    `trigger_time`    datetime      NOT NULL                COMMENT '告警时间',
    `alarm_level`     tinyint       DEFAULT NULL            COMMENT '触发时等级 1-4',
    `alarm_type`      varchar(20)   DEFAULT NULL            COMMENT 'THRESHOLD/COMPREHENSIVE',
    `alarm_message`   varchar(500)  DEFAULT NULL            COMMENT '告警描述',
    `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_trigger_aid` (`alarm_record_id`, `trigger_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='告警触发明细';

-- ---------- 2. 重命名 log → action_log ----------
SET @t_exists := (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_log');
SET @a_exists := (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log');

-- 仅当旧表存在且新表不存在时才 rename (避免覆盖已有 action_log)
SET @sql := IF(@t_exists = 1 AND @a_exists = 0,
    'RENAME TABLE alarm_record_log TO alarm_record_action_log',
    'SELECT "alarm_record_log rename skipped" AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 3. 新增字段（幂等：判断列是否存在）----------

-- action_type
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'action_type');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN action_type varchar(30) NULL COMMENT ''动作类型'' AFTER alarm_id',
    'SELECT "action_type exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- from_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'from_value');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN from_value varchar(20) NULL COMMENT ''变更前值''',
    'SELECT "from_value exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- to_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'to_value');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN to_value varchar(20) NULL COMMENT ''变更后值''',
    'SELECT "to_value exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- remarks
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'remarks');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN remarks varchar(500) NULL COMMENT ''备注''',
    'SELECT "remarks exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- description
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'description');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN description varchar(500) NULL COMMENT ''描述''',
    'SELECT "description exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- attachments
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'attachments');
SET @sql := IF(@col = 0,
    'ALTER TABLE alarm_record_action_log ADD COLUMN attachments varchar(1000) NULL COMMENT ''附件文件名(逗号分隔)''',
    'SELECT "attachments exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 4. 数据迁移 (旧字段 → 新字段) ----------
-- 仅当 action_type 全为 NULL 时执行 (即首次迁移)
SET @pending := (SELECT COUNT(*) FROM alarm_record_action_log WHERE action_type IS NULL);

-- 4.1 from_status → from_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'from_status');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET from_value = from_status WHERE from_value IS NULL AND from_status IS NOT NULL',
    'SELECT "from_value migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.2 to_status → to_value
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'to_status');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET to_value = to_status WHERE to_value IS NULL AND to_status IS NOT NULL',
    'SELECT "to_value migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.3 note → remarks (text 收窄入 varchar(500)，超长截断)
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'note');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET remarks = LEFT(note, 500) WHERE remarks IS NULL AND note IS NOT NULL',
    'SELECT "remarks migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.4 disposal_result → description
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_result');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET description = disposal_result WHERE description IS NULL AND disposal_result IS NOT NULL',
    'SELECT "description migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.5 disposal_type → action_type (CASE WHEN 映射)
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_type');
SET @sql := IF(@col = 1 AND @pending > 0,
    'UPDATE alarm_record_action_log SET action_type = CASE disposal_type WHEN ''开始处置'' THEN ''FEEDBACK'' WHEN ''已销警'' THEN ''DISPOSE_CLOSE'' WHEN ''标记误报'' THEN ''DISPOSE_FALSE_ALARM'' WHEN ''批量销警'' THEN ''DISPOSE_CLOSE'' WHEN ''批量标记误报'' THEN ''DISPOSE_FALSE_ALARM'' WHEN ''批量误报'' THEN ''DISPOSE_FALSE_ALARM'' WHEN ''批量标记处理中'' THEN ''FEEDBACK'' ELSE ''CREATE'' END WHERE action_type IS NULL',
    'SELECT "action_type migration skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 5. 重命名 alarm_id → alarm_record_id + 删除旧字段 ----------

-- 5.1 rename alarm_id
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'alarm_id');
SET @sql := IF(@col = 1,
    'ALTER TABLE alarm_record_action_log CHANGE COLUMN alarm_id alarm_record_id bigint NOT NULL COMMENT ''告警记录ID''',
    'SELECT "alarm_id rename skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5.2 drop 旧字段 (每列判断)
SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'from_status');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN from_status', 'SELECT "drop from_status skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'to_status');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN to_status', 'SELECT "drop to_status skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_type');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN disposal_type', 'SELECT "drop disposal_type skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'disposal_result');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN disposal_result', 'SELECT "drop disposal_result skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'alarm_record_action_log' AND column_name = 'note');
SET @sql := IF(@col = 1, 'ALTER TABLE alarm_record_action_log DROP COLUMN note', 'SELECT "drop note skipped" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 6. 更新表注释 ----------
ALTER TABLE `alarm_record_action_log` COMMENT = '告警动作日志';
```

- [ ] **步骤 2：本地执行验证**

运行（替换 USER/PASS 为本地连接）：

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2.5-alarm-action-log.sql
mysql -uroot -pwodepassword geo_hazard_monitor -e "DESC alarm_record_action_log; SHOW TABLES LIKE 'alarm_record_trigger_detail';"
```

预期：`alarm_record_action_log` 8 列（id/alarm_record_id/action_type/from_value/to_value/remarks/description/attachments/operator/create_time），`alarm_record_trigger_detail` 表存在。

- [ ] **步骤 3：二次执行验证幂等**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2.5-alarm-action-log.sql
```

预期：无报错，所有 "skipped" 消息出现。

- [ ] **步骤 4：Commit**

```bash
git add db/upgrade/v2.5-alarm-action-log.sql
git commit -m "feat(db): 新增 v2.5 告警动作日志升级脚本"
```

---

## 任务 2：创建 `ActionType` 枚举常量类

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/ActionType.java`

- [ ] **步骤 1：编写枚举类**

```java
package com.zwei.iot.alarm.domain;

/**
 * 告警动作类型枚举。对应 alarm_record_action_log.action_type 列。
 * <p>
 * from_value/to_value 语义随 action_type 变化：
 * <ul>
 *   <li>CREATE / FEEDBACK / DISPOSE_* → 状态值 (1=待处理 / 2=处理中 / 3=已销警 / 4=误报)</li>
 *   <li>LEVEL_CHANGE → 等级值 (1-4)</li>
 *   <li>RE_TRIGGER / NOTIFY → 留空</li>
 * </ul>
 *
 * @author zwei
 */
public enum ActionType {

    /** 引擎首次创建（to_value=1） */
    CREATE,
    /** 再次触发同级告警 */
    RE_TRIGGER,
    /** 再次触发且等级变化（from_value=旧等级, to_value=新等级） */
    LEVEL_CHANGE,
    /** 处置反馈 status→2 */
    FEEDBACK,
    /** 销警 status→3 */
    DISPOSE_CLOSE,
    /** 误报 status→4 */
    DISPOSE_FALSE_ALARM,
    /** 通知发送（remarks=渠道/接收人） */
    NOTIFY
}
```

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/ActionType.java
git commit -m "feat(alarm): 新增 ActionType 枚举常量类"
```

---

## 任务 3：重命名 `AlarmRecordLog` → `AlarmRecordActionLog` + 字段调整

**文件：**
- 重命名：`AlarmRecordLog.java` → `AlarmRecordActionLog.java`
- 重命名：`AlarmRecordLogMapper.java` → `AlarmRecordActionLogMapper.java`
- 重命名：`AlarmRecordLogMapper.xml` → `AlarmRecordActionLogMapper.xml`
- 修改：上述三个文件内容

- [ ] **步骤 1：用 git mv 重命名三个文件（保留 history）**

```bash
cd server/zwei-iot-alarm
git mv src/main/java/com/zwei/iot/alarm/domain/AlarmRecordLog.java \
       src/main/java/com/zwei/iot/alarm/domain/AlarmRecordActionLog.java
git mv src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordLogMapper.java \
       src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordActionLogMapper.java
git mv src/main/resources/mapper/alarm/AlarmRecordLogMapper.xml \
       src/main/resources/mapper/alarm/AlarmRecordActionLogMapper.xml
```

- [ ] **步骤 2：改写 `AlarmRecordActionLog.java` 内容**

```java
package com.zwei.iot.alarm.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 告警动作日志 alarm_record_action_log。
 * <p>
 * 全动作流水：创建/再触发/等级变化/反馈/销警/误报/通知。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecordActionLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 告警记录ID */
    private Long alarmRecordId;
    /** 动作类型 (ActionType 枚举 name) */
    private String actionType;
    /** 变更前值 (状态或等级) */
    private String fromValue;
    /** 变更后值 (状态或等级) */
    private String toValue;
    /** 备注/反馈内容 */
    private String remarks;
    /** 描述内容 (FEEDBACK 等动作附带) */
    private String description;
    /** 附件文件名，多个逗号分隔 (/common/upload 返回的 fileName) */
    private String attachments;
    /** 操作人 */
    private String operator;
    /** 创建时间 */
    private Date createTime;
}
```

- [ ] **步骤 3：改写 `AlarmRecordActionLogMapper.java` 内容**

```java
package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmRecordActionLogMapper {

    int insertLog(AlarmRecordActionLog log);

    int batchInsertLogs(List<AlarmRecordActionLog> logs);

    List<AlarmRecordActionLog> selectLogsByAlarmRecordId(Long alarmRecordId);
}
```

- [ ] **步骤 4：改写 `AlarmRecordActionLogMapper.xml` 内容**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.mapper.AlarmRecordActionLogMapper">

    <resultMap type="com.zwei.iot.alarm.domain.AlarmRecordActionLog" id="AlarmRecordActionLogResult">
        <id property="id" column="id"/>
        <result property="alarmRecordId" column="alarm_record_id"/>
        <result property="actionType" column="action_type"/>
        <result property="fromValue" column="from_value"/>
        <result property="toValue" column="to_value"/>
        <result property="remarks" column="remarks"/>
        <result property="description" column="description"/>
        <result property="attachments" column="attachments"/>
        <result property="operator" column="operator"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <insert id="insertLog" parameterType="com.zwei.iot.alarm.domain.AlarmRecordActionLog">
        INSERT INTO alarm_record_action_log (alarm_record_id, action_type, from_value, to_value,
                                             remarks, description, attachments, operator, create_time)
        VALUES (#{alarmRecordId}, #{actionType}, #{fromValue}, #{toValue},
                #{remarks}, #{description}, #{attachments}, #{operator}, #{createTime})
    </insert>

    <insert id="batchInsertLogs" parameterType="list">
        INSERT INTO alarm_record_action_log (alarm_record_id, action_type, from_value, to_value,
                                             remarks, description, attachments, operator, create_time)
        VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.alarmRecordId}, #{item.actionType}, #{item.fromValue}, #{item.toValue},
             #{item.remarks}, #{item.description}, #{item.attachments}, #{item.operator}, #{item.createTime})
        </foreach>
    </insert>

    <select id="selectLogsByAlarmRecordId" resultMap="AlarmRecordActionLogResult">
        SELECT id, alarm_record_id, action_type, from_value, to_value,
               remarks, description, attachments, operator, create_time
        FROM alarm_record_action_log
        WHERE alarm_record_id = #{alarmRecordId}
        ORDER BY create_time ASC
    </select>

</mapper>
```

- [ ] **步骤 5：编译验证（预期失败 — 其他文件还在引用旧类型）**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：**失败**，错误来自 `AlarmRecordServiceImpl` / `IAlarmRecordService` / `AlarmRecordController` / `AlarmRecordServiceImplTest` 仍引用旧 `AlarmRecordLog`。任务 7-8 会修复。

- [ ] **步骤 6：Commit（暂时记录重命名，编译错误将在后续任务修复）**

```bash
git add server/zwei-iot-alarm/
git commit -m "refactor(alarm): 重命名 AlarmRecordLog → AlarmRecordActionLog + 字段调整"
```

---

## 任务 4：新建 `AlarmRecordTriggerDetail` domain + Mapper + XML

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecordTriggerDetail.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordTriggerDetailMapper.java`
- 创建：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordTriggerDetailMapper.xml`

- [ ] **步骤 1：编写 `AlarmRecordTriggerDetail.java`**

```java
package com.zwei.iot.alarm.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 告警触发明细 alarm_record_trigger_detail。
 * <p>
 * 引擎每次触发写一条数据快照。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecordTriggerDetail implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 告警记录ID */
    private Long alarmRecordId;
    /** 告警时间 */
    private Date triggerTime;
    /** 触发时等级 1-4 */
    private Integer alarmLevel;
    /** THRESHOLD / COMPREHENSIVE */
    private String alarmType;
    /** 告警描述 */
    private String alarmMessage;
    /** 创建时间 */
    private Date createTime;
}
```

- [ ] **步骤 2：编写 `AlarmRecordTriggerDetailMapper.java`**

```java
package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmRecordTriggerDetailMapper {

    int insertDetail(AlarmRecordTriggerDetail detail);

    List<AlarmRecordTriggerDetail> selectByAlarmRecordId(Long alarmRecordId);
}
```

- [ ] **步骤 3：编写 `AlarmRecordTriggerDetailMapper.xml`**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.mapper.AlarmRecordTriggerDetailMapper">

    <resultMap type="com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail" id="AlarmRecordTriggerDetailResult">
        <id property="id" column="id"/>
        <result property="alarmRecordId" column="alarm_record_id"/>
        <result property="triggerTime" column="trigger_time"/>
        <result property="alarmLevel" column="alarm_level"/>
        <result property="alarmType" column="alarm_type"/>
        <result property="alarmMessage" column="alarm_message"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <insert id="insertDetail" parameterType="com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO alarm_record_trigger_detail (alarm_record_id, trigger_time, alarm_level,
                                                 alarm_type, alarm_message, create_time)
        VALUES (#{alarmRecordId}, #{triggerTime}, #{alarmLevel}, #{alarmType}, #{alarmMessage}, #{createTime})
    </insert>

    <select id="selectByAlarmRecordId" resultMap="AlarmRecordTriggerDetailResult">
        SELECT id, alarm_record_id, trigger_time, alarm_level, alarm_type, alarm_message, create_time
        FROM alarm_record_trigger_detail
        WHERE alarm_record_id = #{alarmRecordId}
        ORDER BY trigger_time ASC
    </select>

</mapper>
```

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecordTriggerDetail.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordTriggerDetailMapper.java \
        server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordTriggerDetailMapper.xml
git commit -m "feat(alarm): 新增 AlarmRecordTriggerDetail 触发明细 domain + mapper"
```

---

## 任务 5：扩展 `AlarmRecord` domain + `AlarmRecordMapper` 筛选 + `updateAlarmLevel`

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecord.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordMapper.java`
- 修改：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordMapper.xml`

- [ ] **步骤 1：在 `AlarmRecord.java` 末尾 `resolutionNote` 字段后追加非持久化筛选字段**

在 `private String resolutionNote;` 之后、类闭合 `}` 之前插入：

```java
    // ── 非持久化字段：仅用于 selectPendingRecords / selectHistoryRecords 筛选 ──

    /** 告警等级多选筛选 (不入库) */
    private List<Integer> alarmLevels;
    /** 告警类型多选筛选 (不入库) */
    private List<String> alarmTypes;
    /** 状态多选筛选 (不入库) */
    private List<Integer> statusList;
    /** 触发时间范围 - 开始 (不入库) */
    private String triggerTimeBegin;
    /** 触发时间范围 - 结束 (不入库) */
    private String triggerTimeEnd;
```

并在文件顶部添加 `import java.util.List;`。

- [ ] **步骤 2：在 `AlarmRecordMapper.java` 接口末尾新增 `updateAlarmLevel`**

在 `int countByHazardPointId(@Param("hazardPointId") Long hazardPointId);` 之后追加：

```java
    /**
     * 更新告警等级 (再次触发且等级变化时调用)。
     */
    int updateAlarmLevel(@Param("id") Long id,
                         @Param("alarmLevel") Integer alarmLevel,
                         @Param("alarmLevelText") String alarmLevelText,
                         @Param("lastTriggerTime") String lastTriggerTime,
                         @Param("triggerCount") Integer triggerCount);
```

- [ ] **步骤 3：修改 `AlarmRecordMapper.xml` 的 `selectPendingRecords`**

将现有 `selectPendingRecords` 替换为：

```xml
    <select id="selectPendingRecords" resultMap="AlarmRecordResult">
        <include refid="selectRecordVo"/>
        <where>
            <choose>
                <when test="statusList != null and statusList.size() > 0">
                    status IN
                    <foreach collection="statusList" item="st" open="(" separator="," close=")">
                        #{st}
                    </foreach>
                </when>
                <otherwise>
                    status IN (1, 2)
                </otherwise>
            </choose>
            <if test="hazardPointId != null">AND hazard_point_id = #{hazardPointId}</if>
            <if test="alarmLevel != null">AND alarm_level = #{alarmLevel}</if>
            <if test="alarmType != null and alarmType != ''">AND alarm_type = #{alarmType}</if>
            <if test="alarmLevels != null and alarmLevels.size() > 0">
                AND alarm_level IN
                <foreach collection="alarmLevels" item="lv" open="(" separator="," close=")">
                    #{lv}
                </foreach>
            </if>
            <if test="alarmTypes != null and alarmTypes.size() > 0">
                AND alarm_type IN
                <foreach collection="alarmTypes" item="tp" open="(" separator="," close=")">
                    #{tp}
                </foreach>
            </if>
            <if test="hazardPointName != null and hazardPointName != ''">
                AND hazard_point_name LIKE CONCAT('%', #{hazardPointName}, '%')
            </if>
            <if test="triggerTimeBegin != null and triggerTimeBegin != ''">
                AND last_trigger_time &gt;= #{triggerTimeBegin}
            </if>
            <if test="triggerTimeEnd != null and triggerTimeEnd != ''">
                AND last_trigger_time &lt;= #{triggerTimeEnd}
            </if>
        </where>
        ORDER BY alarm_level DESC, last_trigger_time DESC
    </select>
```

- [ ] **步骤 4：在 `AlarmRecordMapper.xml` 的 `updateTriggerCount` 之后新增 `updateAlarmLevel`**

在 `<update id="updateTriggerCount">...</update>` 之后追加：

```xml
    <update id="updateAlarmLevel">
        UPDATE alarm_record
        SET alarm_level       = #{alarmLevel},
            alarm_level_text  = #{alarmLevelText},
            last_trigger_time = #{lastTriggerTime},
            trigger_count     = #{triggerCount},
            update_time       = NOW()
        WHERE id = #{id}
    </update>
```

- [ ] **步骤 5：编译验证（仍可能有其他文件编译错误，但 mapper 相关应该通过）**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q 2>&1 | grep -E "AlarmRecord|error" | head -20`
预期：错误集中在 `AlarmRecordServiceImpl` 等（任务 7 修复），与 `AlarmRecord` 本身无关。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmRecord.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmRecordMapper.java \
        server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecordMapper.xml
git commit -m "feat(alarm): 扩展 AlarmRecord 筛选字段 + 新增 updateAlarmLevel"
```

---

## 任务 6：扩展 `AlarmRecordDisposeRequest` + `BatchDisposeRequest` DTO

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/AlarmRecordDisposeRequest.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/BatchDisposeRequest.java`

- [ ] **步骤 1：在 `AlarmRecordDisposeRequest.java` 添加新字段**

将 `private String note;` 保留作向后兼容（旧前端仍可调用），并在其后追加：

```java
    /**
     * 处置描述 (FEEDBACK 时附带的详细描述)
     */
    private String description;
    /**
     * 附件文件名，多个逗号分隔 (/common/upload 返回的 fileName)
     */
    private String attachments;
    /**
     * 备注/反馈内容 (等价于 note，新前端优先使用 remarks)
     */
    private String remarks;
```

并在类末尾添加对应的 getter/setter：

```java
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
```

- [ ] **步骤 2：在 `BatchDisposeRequest.java` 同样追加**

在 `private String note;` 之后追加同样的三个字段 + getter/setter（同上代码）。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/
git commit -m "feat(alarm): 处置/批量处置 DTO 增加 description/attachments/remarks"
```

---

## 任务 7：改造 `AlarmRecordServiceImpl`（核心三分支 + dispose/batchDispose + 查询方法）

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImpl.java`

- [ ] **步骤 1：改写 import 与依赖注入**

将 import 段替换为：

```java
import com.zwei.iot.alarm.domain.ActionType;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import com.zwei.iot.alarm.mapper.AlarmRecordActionLogMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordTriggerDetailMapper;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
```

将字段与构造函数替换为：

```java
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmRecordMapper alarmRecordMapper;
    private final AlarmRecordActionLogMapper actionLogMapper;
    private final AlarmRecordTriggerDetailMapper triggerDetailMapper;

    public AlarmRecordServiceImpl(AlarmRecordMapper alarmRecordMapper,
                                  AlarmRecordActionLogMapper actionLogMapper,
                                  AlarmRecordTriggerDetailMapper triggerDetailMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
        this.actionLogMapper = actionLogMapper;
        this.triggerDetailMapper = triggerDetailMapper;
    }
```

- [ ] **步骤 2：改写 `createOrUpdateAlarm`**

将原方法（行 52-89）替换为：

```java
    @Override
    public AlarmRecord createOrUpdateAlarm(AlarmRecord record) {
        // 去重: 同一源(criteria/strategy)+隐患点下是否已有非终态告警
        AlarmRecord existing = null;
        if (record.getCriteriaId() != null) {
            existing = alarmRecordMapper.selectActiveByCriteria(
                    record.getCriteriaId(), record.getHazardPointId());
        } else if (record.getStrategyId() != null) {
            existing = alarmRecordMapper.selectActiveByStrategy(
                    record.getStrategyId(), record.getHazardPointId());
        }

        String now = LocalDateTime.now().format(FMT);
        Date nowDate = new Date();

        if (existing != null) {
            // ── 再次触发分支 ──
            int newCount = (existing.getTriggerCount() != null ? existing.getTriggerCount() : 0) + 1;
            Integer oldLevel = existing.getAlarmLevel();
            Integer newLevel = record.getAlarmLevel();
            boolean levelChanged = oldLevel != null && newLevel != null && !oldLevel.equals(newLevel);

            if (levelChanged) {
                // 等级变化：更新主表 alarmLevel + triggerCount + lastTriggerTime
                alarmRecordMapper.updateAlarmLevel(existing.getId(), newLevel,
                        AlarmConstants.resolveLevelText(newLevel), now, newCount);
                existing.setAlarmLevel(newLevel);
            } else {
                alarmRecordMapper.updateTriggerCount(existing.getId(), now, newCount);
            }

            // 写触发明细 (RE_TRIGGER 场景)
            triggerDetailMapper.insertDetail(AlarmRecordTriggerDetail.builder()
                    .alarmRecordId(existing.getId())
                    .triggerTime(nowDate)
                    .alarmLevel(newLevel)
                    .alarmType(record.getAlarmType())
                    .alarmMessage(record.getAlarmMessage())
                    .createTime(nowDate)
                    .build());

            // 写动作日志：RE_TRIGGER
            actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                    .alarmRecordId(existing.getId())
                    .actionType(ActionType.RE_TRIGGER.name())
                    .operator(AlarmConstants.SYSTEM_OPERATOR)
                    .createTime(nowDate)
                    .build());

            // 等级变化时追加 LEVEL_CHANGE 日志
            if (levelChanged) {
                actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                        .alarmRecordId(existing.getId())
                        .actionType(ActionType.LEVEL_CHANGE.name())
                        .fromValue(String.valueOf(oldLevel))
                        .toValue(String.valueOf(newLevel))
                        .operator(AlarmConstants.SYSTEM_OPERATOR)
                        .createTime(nowDate)
                        .build());
            }

            return existing;
        }

        // ── 新建分支 ──
        record.setFirstTriggerTime(record.getCreateTime() != null ? record.getCreateTime() : nowDate);
        record.setLastTriggerTime(record.getFirstTriggerTime());
        record.setTriggerCount(1);
        record.setStatus(1);
        record.setStatusName("待处理");
        alarmRecordMapper.insertRecord(record);

        // 写触发明细 (CREATE 场景)
        triggerDetailMapper.insertDetail(AlarmRecordTriggerDetail.builder()
                .alarmRecordId(record.getId())
                .triggerTime(nowDate)
                .alarmLevel(record.getAlarmLevel())
                .alarmType(record.getAlarmType())
                .alarmMessage(record.getAlarmMessage())
                .createTime(nowDate)
                .build());

        // 写动作日志：CREATE (to_value=1 即初始状态"待处理")
        actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                .alarmRecordId(record.getId())
                .actionType(ActionType.CREATE.name())
                .toValue("1")
                .operator(AlarmConstants.SYSTEM_OPERATOR)
                .createTime(nowDate)
                .build());

        return record;
    }
```

- [ ] **步骤 3：改写 `dispose` 签名与实现**

将原 `dispose`（行 91-116）替换为：

```java
    @Override
    public int dispose(Long id, Integer newStatus, String description, String attachments,
                       String remarks, String operator) {
        AlarmRecord record = alarmRecordMapper.selectRecordById(id);
        if (record == null) {
            return 0;
        }
        int oldStatus = record.getStatus() != null ? record.getStatus() : 1;
        String statusName = AlarmConstants.resolveStatusName(newStatus);
        String now = LocalDateTime.now().format(FMT);
        Date nowDate = new Date();
        // 兼容旧字段 note（旧前端 / 测试）→ remarks
        String effectiveRemarks = remarks != null ? remarks : null;

        int rows = alarmRecordMapper.updateStatus(id, newStatus, statusName, operator, now, effectiveRemarks);
        if (rows > 0) {
            ActionType actionType = resolveDisposeActionType(newStatus);
            actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                    .alarmRecordId(id)
                    .actionType(actionType.name())
                    .toValue(String.valueOf(newStatus))
                    .remarks(effectiveRemarks)
                    .description(description)
                    .attachments(attachments)
                    .operator(operator)
                    .createTime(nowDate)
                    .build());
        }
        return rows;
    }

    /**
     * 根据目标状态推导 dispose 动作类型。
     */
    private ActionType resolveDisposeActionType(Integer newStatus) {
        if (newStatus == null) return ActionType.FEEDBACK;
        return switch (newStatus) {
            case 2 -> ActionType.FEEDBACK;
            case 3 -> ActionType.DISPOSE_CLOSE;
            case 4 -> ActionType.DISPOSE_FALSE_ALARM;
            default -> ActionType.FEEDBACK;
        };
    }
```

- [ ] **步骤 4：改写 `batchDispose` 签名与实现**

将原 `batchDispose`（行 118-143）替换为：

```java
    @Override
    public int batchDispose(Long[] ids, Integer status, String description, String attachments,
                            String remarks, String resolvedBy) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        String statusName = AlarmConstants.resolveStatusName(status);
        String now = LocalDateTime.now().format(FMT);
        Date nowDate = new Date();
        int rows = alarmRecordMapper.batchUpdateStatus(ids, status, statusName, resolvedBy, now);

        // 逐条写 action_log (每条记录的 action_type/status 一致)
        ActionType actionType = resolveDisposeActionType(status);
        List<AlarmRecordActionLog> logs = new ArrayList<>(ids.length);
        for (Long id : ids) {
            logs.add(AlarmRecordActionLog.builder()
                    .alarmRecordId(id)
                    .actionType(actionType.name())
                    .toValue(String.valueOf(status))
                    .remarks(remarks)
                    .description(description)
                    .attachments(attachments)
                    .operator(resolvedBy)
                    .createTime(nowDate)
                    .build());
        }
        actionLogMapper.batchInsertLogs(logs);

        return rows;
    }
```

- [ ] **步骤 5：改写 `selectLogsByAlarmId` 与新增方法**

将原 `selectLogsByAlarmId`（行 145-148）替换为：

```java
    @Override
    public List<AlarmRecordActionLog> selectActionLogsByAlarmRecordId(Long alarmRecordId) {
        return actionLogMapper.selectLogsByAlarmRecordId(alarmRecordId);
    }

    @Override
    public List<AlarmRecordTriggerDetail> selectTriggerDetailsByAlarmRecordId(Long alarmRecordId) {
        return triggerDetailMapper.selectByAlarmRecordId(alarmRecordId);
    }
```

并删除原 `resolveStatusName` / `resolveDisposalType` 私有方法（不再需要，`AlarmConstants.resolveStatusName` 已存在）。

- [ ] **步骤 6：编译验证（仍会有 IAlarmRecordService / Controller / Test 编译错误，任务 8-10 修复）**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q 2>&1 | grep "AlarmRecordServiceImpl" | head`
预期：本文件无错误（如果有，检查 import 与字段名）。

- [ ] **步骤 7：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImpl.java
git commit -m "refactor(alarm): AlarmRecordServiceImpl 三分支写入 + dispose/batchDispose 用 actionType"
```

---

## 任务 8：扩展 `IAlarmRecordService` + `AlarmRecordController`

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmRecordService.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmRecordController.java`

- [ ] **步骤 1：改写 `IAlarmRecordService.java`**

将 import 与方法签名替换为：

```java
package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;

import java.util.List;

/**
 * 告警记录服务接口
 *
 * @author zwei
 */
public interface IAlarmRecordService {

    List<AlarmRecord> selectPendingList(AlarmRecord record);

    List<AlarmRecord> selectHistoryList(AlarmRecord record);

    AlarmRecord selectById(Long id);

    AlarmRecord createOrUpdateAlarm(AlarmRecord record);

    /**
     * 处置告警 (状态流转)
     *
     * @param id          告警ID
     * @param newStatus   新状态 2=处理中 3=已销警 4=误报
     * @param description 描述 (FEEDBACK 时附带)
     * @param attachments 附件 fileName (逗号分隔)
     * @param remarks     备注/反馈内容
     * @param operator    操作人
     */
    int dispose(Long id, Integer newStatus, String description, String attachments,
                String remarks, String operator);

    /**
     * 批量处置
     */
    int batchDispose(Long[] ids, Integer status, String description, String attachments,
                     String remarks, String resolvedBy);

    /** 动作日志列表 */
    List<AlarmRecordActionLog> selectActionLogsByAlarmRecordId(Long alarmRecordId);

    /** 触发明细列表 */
    List<AlarmRecordTriggerDetail> selectTriggerDetailsByAlarmRecordId(Long alarmRecordId);

    int countPending();

    int countByHazardPointId(Long hazardPointId);
}
```

- [ ] **步骤 2：改写 `AlarmRecordController.java`**

完整替换：

```java
package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import com.zwei.iot.alarm.domain.dto.AlarmRecordDisposeRequest;
import com.zwei.iot.alarm.domain.dto.BatchDisposeRequest;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警记录管理 Controller
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/records")
public class AlarmRecordController extends BaseController {

    private final IAlarmRecordService alarmRecordService;

    public AlarmRecordController(IAlarmRecordService alarmRecordService) {
        this.alarmRecordService = alarmRecordService;
    }

    @GetMapping("/pending")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public TableDataInfo pending(AlarmRecord record) {
        startPage();
        List<AlarmRecord> list = alarmRecordService.selectPendingList(record);
        return getDataTable(list);
    }

    @GetMapping("/history")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public TableDataInfo history(AlarmRecord record) {
        startPage();
        List<AlarmRecord> list = alarmRecordService.selectHistoryList(record);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult getById(@PathVariable Long id) {
        return success(alarmRecordService.selectById(id));
    }

    @PutMapping("/{id}/dispose")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:dispose')")
    public AjaxResult dispose(@PathVariable Long id, @RequestBody AlarmRecordDisposeRequest request) {
        return toAjax(alarmRecordService.dispose(
                id,
                request.getStatus(),
                request.getDescription(),
                request.getAttachments(),
                request.getRemarks() != null ? request.getRemarks() : request.getNote(),
                getUsername()));
    }

    @PostMapping("/batch")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:batch')")
    public AjaxResult batchDispose(@RequestBody BatchDisposeRequest request) {
        return toAjax(alarmRecordService.batchDispose(
                request.getIds().toArray(new Long[0]),
                request.getStatus(),
                request.getDescription(),
                request.getAttachments(),
                request.getRemarks() != null ? request.getRemarks() : request.getNote(),
                getUsername()));
    }

    /** 触发明细列表 (告警记录 tab) */
    @GetMapping("/{id}/trigger-details")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult triggerDetails(@PathVariable Long id) {
        List<AlarmRecordTriggerDetail> details = alarmRecordService.selectTriggerDetailsByAlarmRecordId(id);
        return success(details);
    }

    /** 动作日志列表 (处置记录 tab + 时间线) */
    @GetMapping("/{id}/action-logs")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult actionLogs(@PathVariable Long id) {
        List<AlarmRecordActionLog> logs = alarmRecordService.selectActionLogsByAlarmRecordId(id);
        return success(logs);
    }
}
```

- [ ] **步骤 3：后端编译验证（Test 文件仍可能失败）**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：主代码 BUILD SUCCESS（main 编译通过；test 单独编译仍失败，由任务 10 修复）。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmRecordService.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmRecordController.java
git commit -m "feat(alarm): IAlarmRecordService + Controller 支持 trigger-details/action-logs 接口"
```

---

## 任务 9：改造 `AlarmNotifier` 写 NOTIFY 日志

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java`

- [ ] **步骤 1：注入 `AlarmRecordActionLogMapper`**

在 import 区追加：

```java
import com.zwei.iot.alarm.domain.ActionType;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.mapper.AlarmRecordActionLogMapper;
```

字段与构造函数改为：

```java
    private final IAlarmDispatchService dispatchService;
    private final IAlarmNotificationService notificationService;
    private final AlarmRecordActionLogMapper actionLogMapper;

    public AlarmNotifier(IAlarmDispatchService dispatchService,
                         IAlarmNotificationService notificationService,
                         AlarmRecordActionLogMapper actionLogMapper) {
        this.dispatchService = dispatchService;
        this.notificationService = notificationService;
        this.actionLogMapper = actionLogMapper;
    }
```

- [ ] **步骤 2：在 `dispatch` 方法末尾（`notificationService.batchCreate` 之后）写 NOTIFY 日志**

将 `dispatch` 方法的末尾段：

```java
        if (!notifications.isEmpty()) {
            notificationService.batchCreate(notifications);
            log.info("告警通知已创建: alarmId={}, 通知数={}", event.getAlarmId(), notifications.size());
        }
```

替换为：

```java
        if (!notifications.isEmpty()) {
            notificationService.batchCreate(notifications);
            // 写 NOTIFY 动作日志：聚合渠道/接收人摘要
            String channelSummary = notifications.stream()
                    .map(n -> n.getChannel() + ":" + (n.getRecipientName() != null ? n.getRecipientName() : "系统"))
                    .reduce((a, b) -> a + "," + b)
                    .orElse("SYSTEM");
            actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                    .alarmRecordId(event.getAlarmId())
                    .actionType(ActionType.NOTIFY.name())
                    .remarks(channelSummary)
                    .operator("SYSTEM")
                    .createTime(new Date())
                    .build());
            log.info("告警通知已创建: alarmId={}, 通知数={}", event.getAlarmId(), notifications.size());
        }
```

- [ ] **步骤 3：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmNotifier.java
git commit -m "feat(alarm): AlarmNotifier 写 NOTIFY 动作日志"
```

---

## 任务 10：更新 `AlarmRecordServiceImplTest` 覆盖三分支

**文件：**
- 修改：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImplTest.java`

- [ ] **步骤 1：改写整个测试文件**

```java
package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.ActionType;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import com.zwei.iot.alarm.mapper.AlarmRecordActionLogMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordTriggerDetailMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AlarmRecordServiceImpl 单元测试 — 覆盖三分支 + dispose/batchDispose + action_log。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmRecordServiceImpl")
class AlarmRecordServiceImplTest {

    @Mock private AlarmRecordMapper recordMapper;
    @Mock private AlarmRecordActionLogMapper actionLogMapper;
    @Mock private AlarmRecordTriggerDetailMapper triggerDetailMapper;

    private AlarmRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlarmRecordServiceImpl(recordMapper, actionLogMapper, triggerDetailMapper);
    }

    // ──────────── createOrUpdateAlarm 三分支 ────────────

    @Nested
    @DisplayName("createOrUpdateAlarm")
    class CreateOrUpdate {

        @Test
        @DisplayName("新建 → status=1 + 写 CREATE 日志 + 写触发明细")
        void newAlarmWritesCreateLog() {
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(null);
            doAnswer(inv -> { inv.<AlarmRecord>getArgument(0).setId(500L); return 1; })
                    .when(recordMapper).insertRecord(any(AlarmRecord.class));
            when(actionLogMapper.insertLog(any())).thenReturn(1);
            when(triggerDetailMapper.insertDetail(any())).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).hazardPointName("测试")
                    .criteriaId(1L)
                    .alarmLevel(3).alarmLevelText("橙色")
                    .alarmType("THRESHOLD").alarmMessage("test")
                    .currentValue(new BigDecimal("15.5"))
                    .createBy("SYSTEM").createTime(new Date())
                    .build();

            AlarmRecord result = service.createOrUpdateAlarm(input);

            assertThat(result.getId()).isEqualTo(500L);
            assertThat(result.getStatus()).isEqualTo(1);

            // CREATE 日志断言：to_value="1"，action_type=CREATE
            ArgumentCaptor<AlarmRecordActionLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordActionLog.class);
            verify(actionLogMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.CREATE.name());
            assertThat(logCaptor.getValue().getToValue()).isEqualTo("1");

            // 触发明细断言
            verify(triggerDetailMapper).insertDetail(argThat(d -> d.getAlarmLevel() == 3));
        }

        @Test
        @DisplayName("再次触发同级 → 更新 triggerCount + 写 RE_TRIGGER 日志（不写 LEVEL_CHANGE）")
        void reTriggerSameLevelWritesOnlyReTriggerLog() {
            AlarmRecord existing = AlarmRecord.builder()
                    .id(500L).criteriaId(1L).hazardPointId(100L)
                    .status(1).triggerCount(3).alarmLevel(3)
                    .build();
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(existing);
            when(recordMapper.updateTriggerCount(eq(500L), anyString(), eq(4))).thenReturn(1);
            when(actionLogMapper.insertLog(any())).thenReturn(1);
            when(triggerDetailMapper.insertDetail(any())).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).criteriaId(1L)
                    .alarmLevel(3)  // 同级
                    .alarmType("THRESHOLD").createBy("SYSTEM").createTime(new Date())
                    .build();

            service.createOrUpdateAlarm(input);

            verify(recordMapper, never()).updateAlarmLevel(anyLong(), anyInt(), anyString(), anyString(), anyInt());
            // 仅 1 条 RE_TRIGGER，无 LEVEL_CHANGE
            verify(actionLogMapper, times(1)).insertLog(argThat(l ->
                    ActionType.RE_TRIGGER.name().equals(l.getActionType())));
            verify(actionLogMapper, never()).insertLog(argThat(l ->
                    ActionType.LEVEL_CHANGE.name().equals(l.getActionType())));
        }

        @Test
        @DisplayName("再次触发等级变化(3→4) → 更新主表 alarmLevel + 写 RE_TRIGGER + LEVEL_CHANGE 两条日志")
        void reTriggerLevelChangeWritesTwoLogs() {
            AlarmRecord existing = AlarmRecord.builder()
                    .id(500L).criteriaId(1L).hazardPointId(100L)
                    .status(1).triggerCount(3).alarmLevel(3)
                    .build();
            when(recordMapper.selectActiveByCriteria(1L, 100L)).thenReturn(existing);
            when(recordMapper.updateAlarmLevel(eq(500L), eq(4), eq("红色"), anyString(), eq(4))).thenReturn(1);
            when(actionLogMapper.insertLog(any())).thenReturn(1);
            when(triggerDetailMapper.insertDetail(any())).thenReturn(1);

            AlarmRecord input = AlarmRecord.builder()
                    .hazardPointId(100L).criteriaId(1L)
                    .alarmLevel(4)  // 等级变化
                    .alarmType("THRESHOLD").createBy("SYSTEM").createTime(new Date())
                    .build();

            service.createOrUpdateAlarm(input);

            // 更新主表 alarmLevel
            verify(recordMapper).updateAlarmLevel(eq(500L), eq(4), eq("红色"), anyString(), eq(4));
            verify(recordMapper, never()).updateTriggerCount(anyLong(), anyString(), anyInt());

            // 两条日志：RE_TRIGGER + LEVEL_CHANGE
            verify(actionLogMapper).insertLog(argThat(l ->
                    ActionType.RE_TRIGGER.name().equals(l.getActionType())));
            verify(actionLogMapper).insertLog(argThat(l -> {
                if (!ActionType.LEVEL_CHANGE.name().equals(l.getActionType())) return false;
                assertThat(l.getFromValue()).isEqualTo("3");
                assertThat(l.getToValue()).isEqualTo("4");
                return true;
            });

            // 触发明细等级 = 新等级 4
            verify(triggerDetailMapper).insertDetail(argThat(d -> d.getAlarmLevel() == 4));
        }
    }

    // ──────────── dispose ────────────

    @Nested
    @DisplayName("dispose 状态流转")
    class Dispose {

        @Test
        @DisplayName("待处理→处理中(status=2) → 写 FEEDBACK 日志")
        void pendingToFeedbackWritesFeedbackLog() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(2), eq("处理中"), eq("admin"), anyString(), any())).thenReturn(1);
            when(actionLogMapper.insertLog(any())).thenReturn(1);

            int rows = service.dispose(1L, 2, "现场已派员", "a.txt,b.txt", "派员核查", "admin");

            assertThat(rows).isEqualTo(1);
            ArgumentCaptor<AlarmRecordActionLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordActionLog.class);
            verify(actionLogMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.FEEDBACK.name());
            assertThat(logCaptor.getValue().getToValue()).isEqualTo("2");
            assertThat(logCaptor.getValue().getDescription()).isEqualTo("现场已派员");
            assertThat(logCaptor.getValue().getAttachments()).isEqualTo("a.txt,b.txt");
            assertThat(logCaptor.getValue().getRemarks()).isEqualTo("派员核查");
        }

        @Test
        @DisplayName("销警(status=3) → 写 DISPOSE_CLOSE 日志")
        void disposeClose() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(3), eq("已销警"), eq("admin"), anyString(), any())).thenReturn(1);
            when(actionLogMapper.insertLog(any())).thenReturn(1);

            service.dispose(1L, 3, null, null, "解除", "admin");

            ArgumentCaptor<AlarmRecordActionLog> logCaptor = ArgumentCaptor.forClass(AlarmRecordActionLog.class);
            verify(actionLogMapper).insertLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.DISPOSE_CLOSE.name());
            assertThat(logCaptor.getValue().getToValue()).isEqualTo("3");
        }

        @Test
        @DisplayName("误报(status=4) → 写 DISPOSE_FALSE_ALARM 日志")
        void disposeFalseAlarm() {
            AlarmRecord record = AlarmRecord.builder().id(1L).status(1).build();
            when(recordMapper.selectRecordById(1L)).thenReturn(record);
            when(recordMapper.updateStatus(eq(1L), eq(4), eq("误报"), eq("admin"), anyString(), any())).thenReturn(1);
            when(actionLogMapper.insertLog(any())).thenReturn(1);

            service.dispose(1L, 4, null, null, "传感器故障", "admin");

            verify(actionLogMapper).insertLog(argThat(l ->
                    ActionType.DISPOSE_FALSE_ALARM.name().equals(l.getActionType())));
        }

        @Test
        @DisplayName("记录不存在返回 0")
        void recordNotFound() {
            when(recordMapper.selectRecordById(99L)).thenReturn(null);
            assertThat(service.dispose(99L, 2, null, null, null, "admin")).isZero();
        }
    }

    // ──────────── batchDispose ────────────

    @Nested
    @DisplayName("batchDispose 批量处置")
    class BatchDispose {

        @Test
        @DisplayName("批量销警：逐条写 DISPOSE_CLOSE 日志")
        void batchClose() {
            Long[] ids = {1L, 2L, 3L};
            when(recordMapper.batchUpdateStatus(eq(ids), eq(3), eq("已销警"), eq("admin"), anyString())).thenReturn(3);
            when(actionLogMapper.batchInsertLogs(anyList())).thenReturn(3);

            int rows = service.batchDispose(ids, 3, null, null, "批量销警", "admin");
            assertThat(rows).isEqualTo(3);

            ArgumentCaptor<List<AlarmRecordActionLog>> captor = ArgumentCaptor.forClass(List.class);
            verify(actionLogMapper).batchInsertLogs(captor.capture());
            assertThat(captor.getValue()).hasSize(3);
            assertThat(captor.getValue()).allSatisfy(l -> {
                assertThat(l.getActionType()).isEqualTo(ActionType.DISPOSE_CLOSE.name());
                assertThat(l.getToValue()).isEqualTo("3");
            });
        }

        @Test
        @DisplayName("空数组返回 0")
        void emptyIds() {
            assertThat(service.batchDispose(new Long[0], 3, null, null, null, "admin")).isZero();
        }
    }

    // ──────────── 查询方法 ────────────

    @Test
    @DisplayName("selectActionLogsByAlarmRecordId 委托 mapper")
    void selectActionLogs() {
        when(actionLogMapper.selectLogsByAlarmRecordId(1L)).thenReturn(List.of());
        service.selectActionLogsByAlarmRecordId(1L);
        verify(actionLogMapper).selectLogsByAlarmRecordId(1L);
    }

    @Test
    @DisplayName("selectTriggerDetailsByAlarmRecordId 委托 mapper")
    void selectTriggerDetails() {
        when(triggerDetailMapper.selectByAlarmRecordId(1L)).thenReturn(List.of());
        service.selectTriggerDetailsByAlarmRecordId(1L);
        verify(triggerDetailMapper).selectByAlarmRecordId(1L);
    }
}
```

- [ ] **步骤 2：运行测试**

运行：`cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmRecordServiceImplTest -q`
预期：全部测试通过（约 10 个 test）。

- [ ] **步骤 3：如果失败，根据错误修复**
  - 常见错误：`updateAlarmLevel` stub 未匹配（检查参数顺序）
  - 常见错误：`argThat` lambda 内放断言（改为先捕获再断言）

- [ ] **步骤 4：运行整个 alarm 模块测试**

运行：`cd server && mvn test -pl zwei-iot-alarm -q`
预期：BUILD SUCCESS，全部测试通过。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/impl/AlarmRecordServiceImplTest.java
git commit -m "test(alarm): AlarmRecordServiceImplTest 覆盖三分支与 dispose 动作日志"
```

---

## 任务 11：扩展前端 `api/alarm.ts`

**文件：**
- 修改：`web/src/api/alarm.ts`

- [ ] **步骤 1：扩展 `AlarmRecordPageParams`**

将原定义（行 43-50）替换为：

```ts
export interface AlarmRecordPageParams {
    pageNum?: number
    pageSize?: number
    hazardPointId?: number
    hazardPointName?: string
    /** 原有单选 */
    alarmLevel?: number
    alarmType?: string
    /** 新增多选筛选 */
    alarmLevels?: number[]
    alarmTypes?: string[]
    statusList?: number[]
    /** 触发时间范围 */
    triggerTimeBegin?: string
    triggerTimeEnd?: string
}
```

- [ ] **步骤 2：扩展 `AlarmDisposePayload` 与 `AlarmBatchDisposePayload`**

将原定义（行 52-61）替换为：

```ts
export interface AlarmDisposePayload {
    status: number
    /** @deprecated 旧字段，保留向后兼容 */
    note?: string
    /** 描述 (FEEDBACK 时附带) */
    description?: string
    /** 附件 fileName (逗号分隔) */
    attachments?: string
    /** 备注/反馈内容 */
    remarks?: string
}

export interface AlarmBatchDisposePayload {
    ids: number[]
    status: number
    note?: string
    description?: string
    attachments?: string
    remarks?: string
}
```

- [ ] **步骤 3：替换 `AlarmRecordLog` 类型为 `AlarmRecordActionLog` 并新增 `AlarmRecordTriggerDetail`**

将原 `AlarmRecordLog` 接口（行 63-75）替换为：

```ts
/** 告警动作日志（处置记录 tab + 时间线） */
export interface AlarmRecordActionLog {
    id: number
    alarmRecordId: number
    /** 动作类型: CREATE/RE_TRIGGER/LEVEL_CHANGE/FEEDBACK/DISPOSE_CLOSE/DISPOSE_FALSE_ALARM/NOTIFY */
    actionType: string
    fromValue?: string
    toValue?: string
    remarks?: string
    description?: string
    attachments?: string
    operator?: string
    createTime: string
}

/** 告警触发明细（告警记录 tab） */
export interface AlarmRecordTriggerDetail {
    id: number
    alarmRecordId: number
    triggerTime: string
    alarmLevel?: number
    alarmType?: string
    alarmMessage?: string
    createTime: string
}
```

- [ ] **步骤 4：替换 API 函数 — 改名 `getAlarmRecordLogs` → `getActionLogs`，新增 `getTriggerDetails`**

将原行 241-243 的 `getAlarmRecordLogs` 替换为：

```ts
/** 告警触发明细列表 */
export const getTriggerDetails = (id: number) =>
    request.get<AlarmRecordTriggerDetail[]>(`/alarm/records/${id}/trigger-details`)

/** 告警动作日志列表（处置记录 + 时间线） */
export const getActionLogs = (id: number) =>
    request.get<AlarmRecordActionLog[]>(`/alarm/records/${id}/action-logs`)
```

- [ ] **步骤 5：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | grep -E "alarm.ts|error" | head -20`
预期：`alarm.ts` 无错误。其他用到 `getAlarmRecordLogs` 或 `AlarmRecordLog` 的文件可能报错，由任务 12-14 修复。

- [ ] **步骤 6：Commit**

```bash
git add web/src/api/alarm.ts
git commit -m "feat(api): alarm.ts 扩展筛选/处置参数 + 新增 action-logs/trigger-details"
```

---

## 任务 12：改造 `RealtimeAlarm.vue` 去 mock + 对接 API

**文件：**
- 修改：`web/src/views/alarm/RealtimeAlarm.vue`

- [ ] **步骤 1：删除"人员名称"搜索框（行 31）**

删除模板中的整行：

```html
<el-input v-model="queryParams.personName" placeholder="人员名称" clearable class="search__input" />
```

- [ ] **步骤 2：调整搜索表单枚举值为数字/大写（行 40-53）**

将告警等级、告警类型、警情状态三个 `el-select` 的 `el-option` value 改为：

```html
<el-select v-model="queryParams.alarmLevel" placeholder="告警等级" clearable multiple class="search__select">
  <el-option label="一级" :value="1" />
  <el-option label="二级" :value="2" />
  <el-option label="三级" :value="3" />
  <el-option label="四级" :value="4" />
</el-select>
<el-select v-model="queryParams.alarmType" placeholder="告警类型" clearable multiple class="search__select">
  <el-option label="阈值预警" value="THRESHOLD" />
  <el-option label="综合预警" value="COMPREHENSIVE" />
</el-select>
<el-select v-model="queryParams.status" placeholder="警情状态" clearable multiple class="search__select">
  <el-option label="待处理" :value="1" />
  <el-option label="处理中" :value="2" />
  <el-option label="已销警" :value="3" />
  <el-option label="误报" :value="4" />
</el-select>
```

- [ ] **步骤 3：导出按钮置灰（行 22-25）**

将导出按钮替换为：

```html
<el-tooltip content="暂未开放" placement="top">
  <el-button type="info" disabled>
    <el-icon><Download /></el-icon>
    导出
  </el-button>
</el-tooltip>
```

- [ ] **步骤 4：表格列字段映射（行 69-105）**

将列定义改为对接后端字段：

```html
<el-table-column prop="hazardPointName" label="隐患点名称" min-width="180" />
<el-table-column prop="alarmLevel" label="告警等级" width="100">
  <template #default="{ row }">
    <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ row.alarmLevelText || getAlarmLevelText(row.alarmLevel) }}</el-tag>
  </template>
</el-table-column>
<el-table-column prop="firstTriggerTime" label="首次告警时间" width="180" />
<el-table-column prop="lastTriggerTime" label="最后告警时间" width="180" />
<el-table-column prop="triggerCount" label="告警次数" width="100">
  <template #default="{ row }">
    <span class="alarm-count" @click.stop="handleView(row)">{{ row.triggerCount }}</span>
  </template>
</el-table-column>
<el-table-column prop="alarmType" label="告警类型" width="120">
  <template #default="{ row }">{{ getAlarmTypeText(row.alarmType) }}</template>
</el-table-column>
<el-table-column prop="status" label="警情状态" width="100">
  <template #default="{ row }">
    <el-tag :type="getStatusType(row.status)">{{ row.statusName || getStatusText(row.status) }}</el-tag>
  </template>
</el-table-column>
<el-table-column prop="resolvedBy" label="响应人员" width="120" />
<el-table-column prop="resolvedAt" label="响应时间" min-width="180" />
```

- [ ] **步骤 5：改写 `<script setup>` 全部逻辑**

完整替换 `<script setup lang="ts">` 区块：

```ts
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, CircleClose, Download, View, Warning } from '@element-plus/icons-vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import AlarmDetailDialog from './components/AlarmDetailDialog.vue'
import FeedBack from '@/components/FeedBack.vue'
import {
  getPendingAlarms,
  disposeAlarm,
  batchDisposeAlarms,
  type AlarmRecordItem,
  type AlarmRecordPageParams,
} from '@/api/alarm'

// 查询参数（已移除人员名称）
const queryParams = reactive({
  hazardPointName: '',
  alarmTimeRange: [] as string[],
  alarmLevel: [] as number[],
  alarmType: [] as string[],
  status: [] as number[],
})

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

const tableData = ref<AlarmRecordItem[]>([])
const selectedRows = ref<AlarmRecordItem[]>([])

const detailDialogVisible = ref(false)
const feedbackDialogVisible = ref(false)
const batchFeedbackVisible = ref(false)
const currentRow = ref<AlarmRecordItem | null>(null)

// ── 数据加载 ──
async function loadList() {
  const params: AlarmRecordPageParams = {
    pageNum: pagination.currentPage,
    pageSize: pagination.pageSize,
    hazardPointName: queryParams.hazardPointName || undefined,
    alarmLevels: queryParams.alarmLevel.length > 0 ? queryParams.alarmLevel : undefined,
    alarmTypes: queryParams.alarmType.length > 0 ? queryParams.alarmType : undefined,
    statusList: queryParams.status.length > 0 ? queryParams.status : undefined,
  }
  if (queryParams.alarmTimeRange?.length === 2) {
    params.triggerTimeBegin = queryParams.alarmTimeRange[0]
    params.triggerTimeEnd = queryParams.alarmTimeRange[1]
  }
  const res = await getPendingAlarms(params)
  tableData.value = res.rows || []
  pagination.total = res.total || 0
}

onMounted(() => { loadList() })

// ── 枚举映射（数字/大写）──
const getAlarmLevelType = (level: number | string) => {
  const n = Number(level)
  return ({ 1: 'info', 2: 'warning', 3: 'warning', 4: 'danger' } as Record<number, string>)[n] || 'info'
}
const getAlarmLevelText = (level: number | string) => {
  const n = Number(level)
  return ({ 1: '一级', 2: '二级', 3: '三级', 4: '四级' } as Record<number, string>)[n] || String(level)
}
const getAlarmTypeText = (type: string) =>
  ({ THRESHOLD: '阈值预警', COMPREHENSIVE: '综合预警' } as Record<string, string>)[type] || type
const getStatusType = (status: number | string) => {
  const n = Number(status)
  return ({ 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' } as Record<number, string>)[n] || 'info'
}
const getStatusText = (status: number | string) => {
  const n = Number(status)
  return ({ 1: '待处理', 2: '处理中', 3: '已销警', 4: '误报' } as Record<number, string>)[n] || String(status)
}

// ── 查询 / 重置 / 分页 ──
const handleQuery = () => { pagination.currentPage = 1; loadList() }
const handleReset = () => {
  queryParams.hazardPointName = ''
  queryParams.alarmTimeRange = []
  queryParams.alarmLevel = []
  queryParams.alarmType = []
  queryParams.status = []
  pagination.currentPage = 1
  loadList()
}
const handleSizeChange = (size: number) => { pagination.pageSize = size; loadList() }
const handleCurrentChange = (page: number) => { pagination.currentPage = page; loadList() }

const handleSelectionChange = (rows: AlarmRecordItem[]) => { selectedRows.value = rows }

// ── 查看 ──
const handleView = (row: AlarmRecordItem) => { currentRow.value = row; detailDialogVisible.value = true }
const handleRowClick = (row: AlarmRecordItem) => { currentRow.value = row; detailDialogVisible.value = true }

// ── 处置反馈 ──
const handleFeedback = (row: AlarmRecordItem) => { currentRow.value = row; feedbackDialogVisible.value = true }

const handleFeedbackSubmit = async (payload: { description?: string; attachments?: string; remarks?: string }) => {
  if (!currentRow.value) return
  try {
    await disposeAlarm(currentRow.value.id, {
      status: 2,
      description: payload.description,
      attachments: payload.attachments,
      remarks: payload.remarks,
    })
    ElMessage.success('处置成功')
    feedbackDialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('处置失败')
  }
}

// ── 批量反馈 ──
const handleBatchFeedback = () => {
  if (selectedRows.value.length === 0) { ElMessage.warning('请先选择要反馈的记录'); return }
  batchFeedbackVisible.value = true
}

const handleBatchFeedbackSubmit = async (payload: { description?: string; attachments?: string; remarks?: string }) => {
  try {
    await batchDisposeAlarms({
      ids: selectedRows.value.map(r => r.id),
      status: 2,
      description: payload.description,
      attachments: payload.attachments,
      remarks: payload.remarks,
    })
    ElMessage.success(`已对 ${selectedRows.value.length} 条告警提交反馈`)
    batchFeedbackVisible.value = false
    selectedRows.value = []
    loadList()
  } catch (e) {
    ElMessage.error('批量反馈失败')
  }
}

// ── 批量误报 ──
const handleBatchFalseAlarm = async () => {
  if (selectedRows.value.length === 0) { ElMessage.warning('请先选择要标记为误报的记录'); return }
  try {
    await ElMessageBox.confirm(`确定将选中的 ${selectedRows.value.length} 条告警标记为误报吗？`, '误报确认', { type: 'warning' })
    await batchDisposeAlarms({ ids: selectedRows.value.map(r => r.id), status: 4 })
    ElMessage.success('已标记为误报')
    selectedRows.value = []
    loadList()
  } catch (e) { /* 用户取消 */ }
}

// ── 批量销警 ──
const handleBatchCloseAlarm = async () => {
  if (selectedRows.value.length === 0) { ElMessage.warning('请先选择要销警的记录'); return }
  try {
    await ElMessageBox.confirm(`确定要销警 ${selectedRows.value.length} 条告警吗？`, '销警确认', { type: 'warning' })
    await batchDisposeAlarms({ ids: selectedRows.value.map(r => r.id), status: 3 })
    ElMessage.success('销警成功')
    selectedRows.value = []
    loadList()
  } catch (e) { /* 用户取消 */ }
}

// 导出：按钮已 disabled，函数保留为空
const handleExport = () => { ElMessage.info('暂未开放') }
</script>
```

- [ ] **步骤 6：删除 mock 弹窗模板（误报/销警确认弹窗，行 137-152）**

由于改用 `ElMessageBox.confirm`，删除模板中的两个 `el-dialog`（`falseAlarmDialogVisible` / `closeAlarmDialogVisible`）。

- [ ] **步骤 7：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | grep "RealtimeAlarm" | head`
预期：无错误。

- [ ] **步骤 8：Commit**

```bash
git add web/src/views/alarm/RealtimeAlarm.vue
git commit -m "feat(alarm): RealtimeAlarm.vue 删除 mock 对接真实 API"
```

---

## 任务 13：改造 `AlarmDetailDialog.vue` 对接 4 tab + 时间线

**文件：**
- 修改：`web/src/views/alarm/components/AlarmDetailDialog.vue`

- [ ] **步骤 1：基础信息 tab 字段映射（行 25-30）**

将 `firstAlarmTime/lastAlarmTime/alarmCount/responderName/responseTime/alarmContent` 替换为：

```html
<el-descriptions-item label="首次告警时间">{{ data.firstTriggerTime }}</el-descriptions-item>
<el-descriptions-item label="最后告警时间">{{ data.lastTriggerTime }}</el-descriptions-item>
<el-descriptions-item label="告警次数">{{ data.triggerCount }}</el-descriptions-item>
<el-descriptions-item label="响应人员">{{ data.resolvedBy || '-' }}</el-descriptions-item>
<el-descriptions-item label="响应时间">{{ data.resolvedAt || '-' }}</el-descriptions-item>
<el-descriptions-item label="告警内容" :span="2">{{ data.alarmMessage }}</el-descriptions-item>
```

- [ ] **步骤 2：告警记录 tab 表格列对接 triggerDetail（行 58-66）**

将表格列改为：

```html
<el-table :data="filteredAlarmRecords" border stripe style="width: 100%">
  <el-table-column prop="triggerTime" label="告警时间" width="180" />
  <el-table-column prop="alarmLevel" label="告警等级" width="100">
    <template #default="{ row }">
      <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
    </template>
  </el-table-column>
  <el-table-column prop="alarmMessage" label="描述" min-width="200" show-overflow-tooltip />
</el-table>
```

- [ ] **步骤 3：处置记录 tab 表格列对接 actionLog（行 121-127）**

将表格列改为：

```html
<el-table :data="disposalRecords" border stripe style="width: 100%">
  <el-table-column prop="createTime" label="处置时间" width="180" />
  <el-table-column prop="actionType" label="动作类型" width="140">
    <template #default="{ row }">{{ getActionTypeText(row.actionType) }}</template>
  </el-table-column>
  <el-table-column prop="operator" label="处置人员" width="120" />
  <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
  <el-table-column prop="remarks" label="备注" min-width="180" show-overflow-tooltip />
</el-table>
```

- [ ] **步骤 4：改写 `<script setup>` 区块**

完整替换 `<script setup lang="ts">`：

```ts
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  getAlarmRecordDetail,
  getTriggerDetails,
  getActionLogs,
  type AlarmRecordItem,
  type AlarmRecordTriggerDetail,
  type AlarmRecordActionLog,
} from '@/api/alarm'

const props = defineProps<{
  modelValue: boolean
  data: Record<string, any> | null
}>()

defineEmits<{ 'update:modelValue': [value: boolean] }>()

const activeTab = ref('basic')
const detail = ref<AlarmRecordItem | null>(null)

const alarmRecordSearch = ref({ description: '', timeRange: [] as string[] })
const triggerDetails = ref<AlarmRecordTriggerDetail[]>([])

const disposalRecords = ref<AlarmRecordActionLog[]>([])

interface TimelineNode { time: string; description: string; type: string }
const timelineData = ref<TimelineNode[]>([])

const notifyRecords = ref<any[]>([])

// 弹窗打开时并发拉取
watch(() => props.modelValue, async (val) => {
  if (!val || !props.data?.id) return
  activeTab.value = 'basic'
  alarmRecordSearch.value = { description: '', timeRange: [] }
  const id = Number(props.data.id)

  try {
    const [d, t, l] = await Promise.all([
      getAlarmRecordDetail(id),
      getTriggerDetails(id),
      getActionLogs(id),
    ])
    detail.value = d
    triggerDetails.value = t || []
    disposalRecords.value = (l || []).filter(x =>
      ['FEEDBACK', 'DISPOSE_CLOSE', 'DISPOSE_FALSE_ALARM'].includes(x.actionType))
    timelineData.value = buildTimeline(l || [])
  } catch (e) {
    detail.value = null
    triggerDetails.value = []
    disposalRecords.value = []
    timelineData.value = []
  }
})

// 由动作日志构造时间线
function buildTimeline(logs: AlarmRecordActionLog[]): TimelineNode[] {
  return [...logs].sort((a, b) => a.createTime.localeCompare(b.createTime)).map(log => {
    const typeMap: Record<string, string> = {
      CREATE: 'trigger', RE_TRIGGER: 'trigger', LEVEL_CHANGE: 'trigger',
      NOTIFY: 'notify',
      FEEDBACK: 'dispose', DISPOSE_CLOSE: 'dispose', DISPOSE_FALSE_ALARM: 'dispose',
    }
    const descMap: Record<string, string> = {
      CREATE: '告警创建', RE_TRIGGER: '告警再次触发',
      LEVEL_CHANGE: `等级变化 ${log.fromValue}→${log.toValue}`,
      FEEDBACK: '处置反馈', DISPOSE_CLOSE: '告警销警',
      DISPOSE_FALSE_ALARM: '标记误报', NOTIFY: `通知发送：${log.remarks || ''}`,
    }
    return {
      time: log.createTime,
      description: descMap[log.actionType] || log.actionType,
      type: typeMap[log.actionType] || 'system',
    }
  })
}

const filteredAlarmRecords = computed(() => {
  let list = triggerDetails.value
  if (alarmRecordSearch.value.description) {
    const kw = alarmRecordSearch.value.description.toLowerCase()
    list = list.filter(r => (r.alarmMessage || '').toLowerCase().includes(kw))
  }
  if (alarmRecordSearch.value.timeRange.length === 2) {
    const [s, e] = alarmRecordSearch.value.timeRange
    list = list.filter(r => r.triggerTime >= s && r.triggerTime <= e)
  }
  return list
})

const queryAlarmRecords = () => {}
const resetAlarmRecords = () => { alarmRecordSearch.value = { description: '', timeRange: [] } }

// 通知记录 tab：暂不对接，保留空数据
const filteredNotifyRecords = computed(() => [])
const queryNotifyRecords = () => {}
const resetNotifyRecords = () => {}

// 枚举映射（数字/大写）
const getAlarmLevelType = (level: number | string) => {
  const n = Number(level)
  return ({ 1: 'info', 2: 'warning', 3: 'warning', 4: 'danger' } as Record<number, string>)[n] || 'info'
}
const getAlarmLevelText = (level: number | string) => {
  const n = Number(level)
  return ({ 1: '一级', 2: '二级', 3: '三级', 4: '四级' } as Record<number, string>)[n] || String(level)
}
const getAlarmTypeText = (type: string) =>
  ({ THRESHOLD: '阈值预警', COMPREHENSIVE: '综合预警' } as Record<string, string>)[type] || type
const getStatusType = (status: number | string) => {
  const n = Number(status)
  return ({ 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' } as Record<number, string>)[n] || 'info'
}
const getStatusText = (status: number | string) => {
  const n = Number(status)
  return ({ 1: '待处理', 2: '处理中', 3: '已销警', 4: '误报' } as Record<number, string>)[n] || String(status)
}
const getActionTypeText = (t: string) =>
  ({ CREATE: '创建', RE_TRIGGER: '再次触发', LEVEL_CHANGE: '等级变化',
     FEEDBACK: '处置反馈', DISPOSE_CLOSE: '销警', DISPOSE_FALSE_ALARM: '误报',
     NOTIFY: '通知发送' } as Record<string, string>)[t] || t
</script>
```

- [ ] **步骤 5：基础信息 tab 引用 detail（避免读到外层 row 的旧字段）**

将 `<el-descriptions-item>` 的 `data.xxx` 改为读 `detail?.xxx || data.xxx`，简化版（直接保留 `data.xxx`，因为列表查询返回的 AlarmRecordItem 字段已对齐）：

保留 `data.xxx` 即可，因为父组件传入的 `data` 已经是 `AlarmRecordItem`（来自后端），字段一致。

- [ ] **步骤 6：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | grep "AlarmDetailDialog" | head`
预期：无错误。

- [ ] **步骤 7：Commit**

```bash
git add web/src/views/alarm/components/AlarmDetailDialog.vue
git commit -m "feat(alarm): AlarmDetailDialog 对接触发明细/动作日志/时间线"
```

---

## 任务 14：改造 `FeedbackDialog` 附件上传 + E2E 验证

**文件：**
- 修改：`web/src/components/FeedbackDialog.vue`

- [ ] **步骤 1：补充附件上传 + 提交逻辑**

在 `<script setup lang="ts">` 顶部追加 import：

```ts
import request from '@/utils/request'
```

并补充上传函数与提交载荷组装（如果 FeedbackDialog 内部已有 `files` 状态，则改造其 `handleSubmit`）：

```ts
// 附件上传：调 /common/upload 拿 fileName，多个逗号拼接
async function uploadAttachments(files: File[]): Promise<string | undefined> {
  if (!files || files.length === 0) return undefined
  const fileNames: string[] = []
  for (const f of files) {
    const fd = new FormData()
    fd.append('file', f)
    const res = await request.post<{ fileName?: string; url?: string }>('/common/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (res.fileName) fileNames.push(res.fileName)
  }
  return fileNames.length > 0 ? fileNames.join(',') : undefined
}

// 提交时调用
const handleFeedbackSubmit = async () => {
  const attachments = await uploadAttachments(props.files || [])
  emit('submit', {
    description: feedbackContent.value,
    attachments,
    remarks: feedbackContent.value,
  })
}
```

> 注：具体字段名 `feedbackContent` / `props.files` 取决于 FeedbackDialog 现有状态字段；如名称不同，在步骤 1 内对应替换。`emit('submit')` 的事件载荷由父组件 `RealtimeAlarm.handleFeedbackSubmit` 接收。

- [ ] **步骤 2：类型检查**

运行：`cd web && npx vue-tsc --noEmit 2>&1 | head -30`
预期：BUILD SUCCESS（无类型错误）。

- [ ] **步骤 3：构建生产产物验证**

运行：`cd web && npm run build 2>&1 | tail -20`
预期：`dist/` 生成，无 TS 错误。

- [ ] **步骤 4：Commit**

```bash
git add web/src/components/FeedbackDialog.vue
git commit -m "feat(alarm): FeedbackDialog 附件上传走 /common/upload + disposeAlarm 载荷组装"
```

- [ ] **步骤 5：E2E Playwright 验证（手动）**

使用 Playwright skill 打开 `http://localhost:5173/alarm/realtime`，验证：
1. 表格加载真实数据（非 mock 文本"边坡监测点A-01"等）
2. 网络面板有 `GET /api/v1/alarm/records/pending` 请求
3. 等级多选/状态多选筛选生效
4. 点击"查看" → 弹窗 4 tab 加载真实数据（处置记录有 actionType 列）
5. 处置反馈提交 → `PUT /api/v1/alarm/records/{id}/dispose`
6. 导出按钮置灰且 tooltip 显示「暂未开放」

- [ ] **步骤 6：后端集成启动验证**

```bash
cd server && mvn clean package -DskipTests -q
```

启动 backend，确认：
- `AlarmRecordController` 新端点 `/trigger-details` `/action-logs` 可访问
- 引擎触发告警后 `alarm_record_trigger_detail` 与 `alarm_record_action_log` 双写
- `AlarmNotifier` 写 NOTIFY 日志

---

## 自检

### 规格覆盖度核对

| 规格章节 | 对应任务 |
|---|---|
| 3.1 alarm_record_trigger_detail 表 | 任务 1 + 任务 4 |
| 3.2 alarm_record_log → action_log 改造 | 任务 1 + 任务 3 |
| 3.3 action_type 枚举 | 任务 2 |
| 3.4 升级 SQL | 任务 1 |
| 4 写入时机（CREATE/RE_TRIGGER/LEVEL_CHANGE/FEEDBACK/DISPOSE_CLOSE/DISPOSE_FALSE_ALARM/NOTIFY） | 任务 7 + 任务 9 |
| 5 后端接口（pending 扩展/trigger-details/action-logs/dispose 扩展/batch 扩展） | 任务 5 + 任务 8 |
| 6.1 RealtimeAlarm 去 mock + 移除人员名称 + 导出置灰 | 任务 12 |
| 6.2 tag 颜色映射 | 任务 12 + 任务 13 |
| 6.3 AlarmDetailDialog 4 tab + 时间线 | 任务 13 |
| 6.4 FeedbackDialog 附件上传 | 任务 14 |
| 6.5 api/alarm.ts 扩展 | 任务 11 |
| 7 验收标准 #8 单测覆盖三分支 | 任务 10 |

### 占位符扫描

- 任务 14 步骤 1 提到"具体字段名 feedbackContent / props.files 取决于 FeedbackDialog 现有状态字段" — **可在执行时按实际字段名替换**，不算占位符。
- 任务 14 步骤 5 是手动 E2E 验证步骤，非占位符。

### 类型一致性

- `AlarmRecordActionLog` 字段：`alarmRecordId` / `actionType` / `fromValue` / `toValue` / `remarks` / `description` / `attachments` / `operator` / `createTime` — 在任务 3（Java）、任务 11（TS）、任务 13（消费）保持一致。
- `AlarmRecordTriggerDetail` 字段：`alarmRecordId` / `triggerTime` / `alarmLevel` / `alarmType` / `alarmMessage` / `createTime` — 任务 4 / 任务 11 / 任务 13 一致。
- ActionType 枚举值：`CREATE / RE_TRIGGER / LEVEL_CHANGE / FEEDBACK / DISPOSE_CLOSE / DISPOSE_FALSE_ALARM / NOTIFY` — 任务 2 / 任务 7 / 任务 9 / 任务 10 / 任务 13 一致。
- `dispose` 签名：`(id, newStatus, description, attachments, remarks, operator)` — 任务 7 / 任务 8 / 任务 10 一致。
- `batchDispose` 签名：`(ids, status, description, attachments, remarks, resolvedBy)` — 任务 7 / 任务 8 / 任务 10 一致。

---

## 执行交接

计划已完成并保存到 `docs/superpowers/plans/2026-06-16-alarm-realtime-api-integration-plan.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

选哪种方式？
