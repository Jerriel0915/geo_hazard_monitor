# 通知规则迭代 - 计划 A：数据基础 + 规则 CRUD 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 完成通知规则的数据基础（DDL、领域模型、Mapper）和规则 CRUD（Service、Controller、前端 UI），支持隐患点多选+全部、设备多选+全部、通知人员按角色/部门/指定人员勾选。

**架构：** 主表 `alarm_dispatch_rule` 精简（删除 hazard_point_id/recipients_json/time_window 等冗余字段），新增 3 张关联表（隐患点/设备/接收人）。前端 `NotificationSetting.vue` 表单按事件类型（ALARM/OFFLINE）动态切换字段，新建 `RecipientPicker.vue` 组件支持角色/部门/用户三 Tab 多选 + '*' 全部。

**技术栈：** Java 17 + Spring Boot 4.0.3 + MyBatis + MySQL 8.0；Vue 3 + TypeScript + Element Plus 2.6

**关联规格：** `docs/superpowers/specs/2026-06-17-alarm-dispatch-rule-iteration-design.md`

**前置约束（来自项目 CLAUDE.md）：**
- 修改本地配置只动 `application-local.yml`（已 gitignore）
- profile 必须激活 `local`
- 本地 MySQL: `root/wodepassword @ localhost:3306/geo_hazard_monitor`
- 物理外键禁止（仅 device_hazard_point 等两张历史表保留），关联由 Service 维护
- 逻辑删除统一 `del_flag` 列

---

## 文件结构

### 创建（21 个）

| 文件 | 职责 |
|------|------|
| `db/upgrade/v2026.06.17.001_dispatch_rule_v2.sql` | DDL 升级 + 数据迁移 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/AlarmDispatchRule.java` | 主表实体 |
| `.../domain/AlarmDispatchRuleHazardPoint.java` | 隐患点关联实体 |
| `.../domain/AlarmDispatchRuleDevice.java` | 设备关联实体 |
| `.../domain/AlarmDispatchRuleRecipient.java` | 接收人关联实体 |
| `.../domain/enums/AlarmEventType.java` | 枚举：ALARM / OFFLINE |
| `.../domain/enums/AlarmRecipientType.java` | 枚举：ROLE / DEPT / USER |
| `.../mapper/AlarmDispatchRuleMapper.java` | 主表 Mapper |
| `.../mapper/AlarmDispatchRuleHazardPointMapper.java` | 隐患点关联 Mapper |
| `.../mapper/AlarmDispatchRuleDeviceMapper.java` | 设备关联 Mapper |
| `.../mapper/AlarmDispatchRuleRecipientMapper.java` | 接收人关联 Mapper |
| `.../service/IAlarmDispatchRuleService.java` | Service 接口 |
| `.../service/impl/AlarmDispatchRuleServiceImpl.java` | Service 实现（事务级联） |
| `.../dto/AlarmDispatchRuleCreateRequest.java` | 创建/编辑请求 DTO |
| `.../dto/AlarmDispatchRuleDetailVO.java` | 详情返回 VO |
| `.../dto/AlarmDispatchRuleItemVO.java` | 列表项 VO |
| `.../dto/AlarmDispatchRuleQuery.java` | 列表查询参数 |
| `.../controller/AlarmDispatchRuleController.java` | Controller |
| `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/dispatch/AlarmDispatchRuleServiceImplTest.java` | Service 单测 |
| `web/src/views/alarm/components/RecipientPicker.vue` | 接收人多维度选择组件 |
| `web/src/api/alarmDispatch.ts` | 通知规则 API 模块 |

### 修改（2 个）

| 文件 | 改动 |
|------|------|
| `web/src/views/alarm/NotificationSetting.vue` | 弹窗标题、表单结构、列表展示全面重构 |
| `server/zwei-iot-alarm/.../controller/AlarmDispatchRuleController.java`（如已存在） | 重写（实际项目里可能已有，需先备份后替换） |

### 资源（XML 等）

| 文件 | 职责 |
|------|------|
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleMapper.xml` | 主表 SQL（含 join 关联表查询） |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleHazardPointMapper.xml` | 隐患点关联 SQL |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleDeviceMapper.xml` | 设备关联 SQL |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleRecipientMapper.xml` | 接收人关联 SQL |

---

## 任务清单

### 任务 1：编写 DDL 升级与数据迁移脚本

**文件：**
- 创建：`db/upgrade/v2026.06.17.001_dispatch_rule_v2.sql`

- [ ] **步骤 1：编写 SQL 文件**

```sql
-- ============================================================
-- 通知规则迭代 v2 (2026-06-17)
-- 关联：docs/superpowers/specs/2026-06-17-alarm-dispatch-rule-iteration-design.md
-- ============================================================

-- ---------- 1. 备份旧主表 ----------
RENAME TABLE `alarm_dispatch_rule` TO `alarm_dispatch_rule_bak`;

-- ---------- 2. 重建主表（精简） ----------
CREATE TABLE `alarm_dispatch_rule` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `name`         varchar(200) NOT NULL COMMENT '规则名称',
    `event_type`   varchar(10)  NOT NULL COMMENT '事件类型: ALARM=告警 / OFFLINE=设备离线',
    `alarm_levels` varchar(50)  DEFAULT NULL COMMENT '订阅告警等级（逗号分隔）: 1,2,3,4；OFFLINE 类型时为 NULL',
    `channels`     varchar(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知渠道（逗号分隔）: SYSTEM,SMS,EMAIL',
    `is_enabled`   tinyint      DEFAULT 1 COMMENT '0=禁用 1=启用',
    `del_flag`     tinyint      DEFAULT 0,
    `create_by`    varchar(64)  DEFAULT '',
    `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_by`    varchar(64)  DEFAULT '',
    `update_time`  datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `remark`       varchar(500) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dispatch_event_enabled` (`event_type`, `is_enabled`, `del_flag`)
) COMMENT='通知规则主表';

-- ---------- 3. 关联表：隐患点 ----------
CREATE TABLE `alarm_dispatch_rule_hazard_point` (
    `rule_id`         bigint      NOT NULL,
    `hazard_point_id` varchar(20) NOT NULL COMMENT '隐患点ID；"*" 表示全部',
    PRIMARY KEY (`rule_id`, `hazard_point_id`),
    KEY `idx_adrhp_hp` (`hazard_point_id`)
) COMMENT='通知规则-隐患点关联表';

-- ---------- 4. 关联表：设备（离线通知专用） ----------
CREATE TABLE `alarm_dispatch_rule_device` (
    `rule_id`    bigint      NOT NULL,
    `device_id`  varchar(20) NOT NULL COMMENT '设备ID；"*" 表示全部',
    PRIMARY KEY (`rule_id`, `device_id`),
    KEY `idx_adrd_dev` (`device_id`)
) COMMENT='通知规则-设备关联表';

-- ---------- 5. 关联表：接收人 ----------
CREATE TABLE `alarm_dispatch_rule_recipient` (
    `rule_id`        bigint      NOT NULL,
    `recipient_type` varchar(10) NOT NULL COMMENT 'ROLE / DEPT / USER',
    `recipient_id`   varchar(20) NOT NULL COMMENT '角色/部门/用户ID；"*" 表示该类型全部',
    PRIMARY KEY (`rule_id`, `recipient_type`, `recipient_id`),
    KEY `idx_adrr_type_id` (`recipient_type`, `recipient_id`)
) COMMENT='通知规则-接收人关联表';

-- ---------- 6. alarm_notification 表扩展（计划 B 用，此处一并升级） ----------
ALTER TABLE `alarm_notification`
    ADD COLUMN `read_time`   datetime     DEFAULT NULL COMMENT '已读时间',
    ADD COLUMN `source_type` varchar(20)  DEFAULT 'alarm' COMMENT 'alarm=告警 / offline=设备离线',
    ADD COLUMN `source_id`   bigint       DEFAULT NULL COMMENT '来源ID（alarm_record.id 或 device.id）',
    MODIFY COLUMN `error_msg` varchar(1000) DEFAULT NULL
        COMMENT '渠道发送错误信息，格式 [ERROR_CODE] 描述';

-- 注：uk_notif_dedup 唯一键在计划 B 中创建（避免本计划中存量数据冲突）

-- ---------- 7. 数据迁移 ----------
-- 7.1 主表迁移
INSERT INTO alarm_dispatch_rule
    (id, name, event_type, alarm_levels, channels, is_enabled, del_flag,
     create_by, create_time, update_by, update_time, remark)
SELECT
    id, name,
    CASE WHEN type='offline' THEN 'OFFLINE' ELSE 'ALARM' END,
    alarm_levels, channels, is_enabled, del_flag,
    create_by, create_time, create_by, create_time, remark
FROM alarm_dispatch_rule_bak
WHERE del_flag = 0;

-- 7.2 隐患点关联迁移（NULL → '*'；仅 ALARM 类型）
INSERT INTO alarm_dispatch_rule_hazard_point (rule_id, hazard_point_id)
SELECT
    id,
    CASE WHEN hazard_point_id IS NULL THEN '*'
         ELSE CAST(hazard_point_id AS CHAR)
    END
FROM alarm_dispatch_rule_bak
WHERE del_flag = 0
  AND (type IS NULL OR type = 'alarm' OR type = '');

-- 7.3 接收人迁移（recipients_json → USER 类型）
INSERT INTO alarm_dispatch_rule_recipient (rule_id, recipient_type, recipient_id)
SELECT
    r.id, 'USER', CAST(jt.userId AS CHAR)
FROM alarm_dispatch_rule_bak r,
     JSON_TABLE(r.recipients_json, '$[*]'
        COLUMNS (jt.userId BIGINT PATH '$.userId')) jt
WHERE r.del_flag = 0
  AND r.recipients_json IS NOT NULL
  AND JSON_LENGTH(r.recipients_json) > 0;

-- 7.4 设备关联：旧表未持久化 device_ids，无法迁移
--     （用户需在 UI 重新编辑 OFFLINE 规则）

-- ---------- 8. 校验 SQL（手工执行验证，不写入文件） ----------
-- SELECT COUNT(*) FROM alarm_dispatch_rule_bak WHERE del_flag=0;
-- SELECT COUNT(*) FROM alarm_dispatch_rule;
-- SELECT COUNT(DISTINCT rule_id) FROM alarm_dispatch_rule_hazard_point;
-- SELECT recipient_type, COUNT(*) FROM alarm_dispatch_rule_recipient GROUP BY recipient_type;

-- ---------- 9. 旧表暂保留（验证 1 周后由 DBA 手工 DROP） ----------
-- DROP TABLE alarm_dispatch_rule_bak;
```

- [ ] **步骤 2：在本地 MySQL 执行**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2026.06.17.001_dispatch_rule_v2.sql
```

预期：无报错，所有 ALTER/CREATE 执行成功。

- [ ] **步骤 3：验证表结构与数据**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor -e "
SHOW TABLES LIKE 'alarm_dispatch_rule%';
DESC alarm_dispatch_rule;
SELECT COUNT(*) AS cnt FROM alarm_dispatch_rule;
SELECT COUNT(DISTINCT rule_id) AS hp_cnt FROM alarm_dispatch_rule_hazard_point;
SELECT recipient_type, COUNT(*) FROM alarm_dispatch_rule_recipient GROUP BY recipient_type;
"
```

预期：
- 5 张 `alarm_dispatch_rule*` 表 + 1 张 `_bak`
- 主表字段含 `event_type`、不含旧 `hazard_point_id`/`recipients_json`/`time_window`/`type`/`alarm_types`
- 主表记录数 = bak 表中 del_flag=0 的记录数
- 关联表有迁移数据

- [ ] **步骤 4：Commit**

```bash
git add db/upgrade/v2026.06.17.001_dispatch_rule_v2.sql
git commit -m "feat(alarm/db): 通知规则表结构升级 v2 - 关联表化 + 迁移脚本"
```

---

### 任务 2：编写枚举类

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/enums/AlarmEventType.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/enums/AlarmRecipientType.java`

- [ ] **步骤 1：编写 AlarmEventType 枚举**

```java
package com.zwei.iot.alarm.dispatch.domain.enums;

/**
 * 通知规则事件类型
 */
public enum AlarmEventType {

    ALARM("ALARM", "告警事件"),
    OFFLINE("OFFLINE", "设备离线");

    private final String code;
    private final String label;

    AlarmEventType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static AlarmEventType fromCode(String code) {
        for (AlarmEventType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知事件类型: " + code);
    }
}
```

- [ ] **步骤 2：编写 AlarmRecipientType 枚举**

```java
package com.zwei.iot.alarm.dispatch.domain.enums;

/**
 * 通知规则接收人类型
 */
public enum AlarmRecipientType {

    ROLE("ROLE", "按角色"),
    DEPT("DEPT", "按部门"),
    USER("USER", "指定人员");

    private final String code;
    private final String label;

    AlarmRecipientType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static AlarmRecipientType fromCode(String code) {
        for (AlarmRecipientType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知接收人类型: " + code);
    }
}
```

- [ ] **步骤 3：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/enums/
git commit -m "feat(alarm): 通知规则枚举 - 事件类型/接收人类型"
```

---

### 任务 3：编写 Domain 实体（4 个）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/AlarmDispatchRule.java`
- 创建：`.../domain/AlarmDispatchRuleHazardPoint.java`
- 创建：`.../domain/AlarmDispatchRuleDevice.java`
- 创建：`.../domain/AlarmDispatchRuleRecipient.java`

- [ ] **步骤 1：编写 AlarmDispatchRule 主实体**

```java
package com.zwei.iot.alarm.dispatch.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 通知规则主表实体
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRule extends BaseEntity {

    private Long id;
    private String name;
    /** 事件类型: ALARM / OFFLINE */
    private String eventType;
    /** 订阅告警等级（逗号分隔）: 1,2,3,4 */
    private String alarmLevels;
    /** 通知渠道（逗号分隔）: SYSTEM,SMS,EMAIL */
    private String channels;
    private Integer isEnabled;
    private Integer delFlag;
}
```

- [ ] **步骤 2：编写 AlarmDispatchRuleHazardPoint 关联实体**

```java
package com.zwei.iot.alarm.dispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 通知规则-隐患点关联（'*' 表示全部）
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRuleHazardPoint implements Serializable {

    private Long ruleId;
    /** 隐患点ID；"*" 表示全部 */
    private String hazardPointId;
}
```

- [ ] **步骤 3：编写 AlarmDispatchRuleDevice 关联实体**

```java
package com.zwei.iot.alarm.dispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 通知规则-设备关联（'*' 表示全部，离线通知专用）
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRuleDevice implements Serializable {

    private Long ruleId;
    /** 设备ID；"*" 表示全部 */
    private String deviceId;
}
```

- [ ] **步骤 4：编写 AlarmDispatchRuleRecipient 关联实体**

```java
package com.zwei.iot.alarm.dispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 通知规则-接收人关联（ROLE/DEPT/USER，'*' 表示该类型全部）
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDispatchRuleRecipient implements Serializable {

    private Long ruleId;
    /** ROLE / DEPT / USER */
    private String recipientType;
    /** 角色/部门/用户 ID；"*" 表示该类型全部 */
    private String recipientId;
}
```

- [ ] **步骤 5：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/domain/AlarmDispatchRule*.java
git commit -m "feat(alarm): 通知规则领域实体 - 主表 + 3 张关联表"
```

---

### 任务 4：编写 Mapper 接口（4 个）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/mapper/AlarmDispatchRuleMapper.java`
- 创建：`.../mapper/AlarmDispatchRuleHazardPointMapper.java`
- 创建：`.../mapper/AlarmDispatchRuleDeviceMapper.java`
- 创建：`.../mapper/AlarmDispatchRuleRecipientMapper.java`

- [ ] **步骤 1：编写 AlarmDispatchRuleMapper**

```java
package com.zwei.iot.alarm.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmDispatchRuleMapper extends BaseMapper<AlarmDispatchRule> {

    /** 逻辑删除（按规范：del_flag=1） */
    int logicDeleteById(@Param("id") Long id);

    /** 启用/禁用切换 */
    int updateEnabled(@Param("id") Long id, @Param("isEnabled") Integer isEnabled);
}
```

- [ ] **步骤 2：编写 AlarmDispatchRuleHazardPointMapper**

```java
package com.zwei.iot.alarm.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleHazardPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmDispatchRuleHazardPointMapper
        extends BaseMapper<AlarmDispatchRuleHazardPoint> {

    /** 按 ruleId 删除（更新主表时先清空） */
    int deleteByRuleId(@Param("ruleId") Long ruleId);

    /** 批量插入 */
    int batchInsert(@Param("list") List<AlarmDispatchRuleHazardPoint> list);

    /** 按 ruleId 查询 */
    List<AlarmDispatchRuleHazardPoint> selectByRuleId(@Param("ruleId") Long ruleId);

    /** 批量按 ruleId 查询（列表展示用） */
    List<AlarmDispatchRuleHazardPoint> selectByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
```

- [ ] **步骤 3：编写 AlarmDispatchRuleDeviceMapper（同结构）**

```java
package com.zwei.iot.alarm.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmDispatchRuleDeviceMapper
        extends BaseMapper<AlarmDispatchRuleDevice> {

    int deleteByRuleId(@Param("ruleId") Long ruleId);

    int batchInsert(@Param("list") List<AlarmDispatchRuleDevice> list);

    List<AlarmDispatchRuleDevice> selectByRuleId(@Param("ruleId") Long ruleId);

    List<AlarmDispatchRuleDevice> selectByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
```

- [ ] **步骤 4：编写 AlarmDispatchRuleRecipientMapper**

```java
package com.zwei.iot.alarm.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmDispatchRuleRecipientMapper
        extends BaseMapper<AlarmDispatchRuleRecipient> {

    int deleteByRuleId(@Param("ruleId") Long ruleId);

    int batchInsert(@Param("list") List<AlarmDispatchRuleRecipient> list);

    List<AlarmDispatchRuleRecipient> selectByRuleId(@Param("ruleId") Long ruleId);

    List<AlarmDispatchRuleRecipient> selectByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
```

- [ ] **步骤 5：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/mapper/
git commit -m "feat(alarm): 通知规则 Mapper 接口 - 主表 + 3 关联表"
```

---

### 任务 5：编写 Mapper XML（4 个）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleMapper.xml`
- 创建：`.../mapper/alarm/AlarmDispatchRuleHazardPointMapper.xml`
- 创建：`.../mapper/alarm/AlarmDispatchRuleDeviceMapper.xml`
- 创建：`.../mapper/alarm/AlarmDispatchRuleRecipientMapper.xml`

- [ ] **步骤 1：编写 AlarmDispatchRuleMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper">

    <update id="logicDeleteById">
        UPDATE alarm_dispatch_rule
        SET del_flag = 1
        WHERE id = #{id}
    </update>

    <update id="updateEnabled">
        UPDATE alarm_dispatch_rule
        SET is_enabled = #{isEnabled}
        WHERE id = #{id}
    </update>

</mapper>
```

- [ ] **步骤 2：编写 AlarmDispatchRuleHazardPointMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleHazardPointMapper">

    <resultMap id="BaseResultMap"
               type="com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleHazardPoint">
        <id property="ruleId" column="rule_id"/>
        <id property="hazardPointId" column="hazard_point_id"/>
    </resultMap>

    <delete id="deleteByRuleId">
        DELETE FROM alarm_dispatch_rule_hazard_point
        WHERE rule_id = #{ruleId}
    </delete>

    <insert id="batchInsert">
        INSERT INTO alarm_dispatch_rule_hazard_point (rule_id, hazard_point_id) VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.ruleId}, #{item.hazardPointId})
        </foreach>
    </insert>

    <select id="selectByRuleId" resultMap="BaseResultMap">
        SELECT rule_id, hazard_point_id
        FROM alarm_dispatch_rule_hazard_point
        WHERE rule_id = #{ruleId}
    </select>

    <select id="selectByRuleIds" resultMap="BaseResultMap">
        SELECT rule_id, hazard_point_id
        FROM alarm_dispatch_rule_hazard_point
        WHERE rule_id IN
        <foreach collection="ruleIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </select>

</mapper>
```

- [ ] **步骤 3：编写 AlarmDispatchRuleDeviceMapper.xml（同结构）**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleDeviceMapper">

    <resultMap id="BaseResultMap"
               type="com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleDevice">
        <id property="ruleId" column="rule_id"/>
        <id property="deviceId" column="device_id"/>
    </resultMap>

    <delete id="deleteByRuleId">
        DELETE FROM alarm_dispatch_rule_device
        WHERE rule_id = #{ruleId}
    </delete>

    <insert id="batchInsert">
        INSERT INTO alarm_dispatch_rule_device (rule_id, device_id) VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.ruleId}, #{item.deviceId})
        </foreach>
    </insert>

    <select id="selectByRuleId" resultMap="BaseResultMap">
        SELECT rule_id, device_id
        FROM alarm_dispatch_rule_device
        WHERE rule_id = #{ruleId}
    </select>

    <select id="selectByRuleIds" resultMap="BaseResultMap">
        SELECT rule_id, device_id
        FROM alarm_dispatch_rule_device
        WHERE rule_id IN
        <foreach collection="ruleIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </select>

</mapper>
```

- [ ] **步骤 4：编写 AlarmDispatchRuleRecipientMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper">

    <resultMap id="BaseResultMap"
               type="com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient">
        <id property="ruleId" column="rule_id"/>
        <id property="recipientType" column="recipient_type"/>
        <id property="recipientId" column="recipient_id"/>
    </resultMap>

    <delete id="deleteByRuleId">
        DELETE FROM alarm_dispatch_rule_recipient
        WHERE rule_id = #{ruleId}
    </delete>

    <insert id="batchInsert">
        INSERT INTO alarm_dispatch_rule_recipient
            (rule_id, recipient_type, recipient_id) VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.ruleId}, #{item.recipientType}, #{item.recipientId})
        </foreach>
    </insert>

    <select id="selectByRuleId" resultMap="BaseResultMap">
        SELECT rule_id, recipient_type, recipient_id
        FROM alarm_dispatch_rule_recipient
        WHERE rule_id = #{ruleId}
    </select>

    <select id="selectByRuleIds" resultMap="BaseResultMap">
        SELECT rule_id, recipient_type, recipient_id
        FROM alarm_dispatch_rule_recipient
        WHERE rule_id IN
        <foreach collection="ruleIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </select>

</mapper>
```

- [ ] **步骤 5：编译 + 单元启动验证（确保 XML 加载无误）**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。可选用 IDE 启动 `com.zwei.RuoYiApplication`（profile=local），观察日志无 Mapper XML 解析错误。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRule*.xml
git commit -m "feat(alarm): 通知规则 Mapper XML - CRUD SQL"
```

---

### 任务 6：编写 DTO（4 个）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/dto/AlarmDispatchRuleCreateRequest.java`
- 创建：`.../dto/AlarmDispatchRuleDetailVO.java`
- 创建：`.../dto/AlarmDispatchRuleItemVO.java`
- 创建：`.../dto/AlarmDispatchRuleQuery.java`

- [ ] **步骤 1：编写 AlarmDispatchRuleCreateRequest**

```java
package com.zwei.iot.alarm.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 通知规则创建/编辑请求
 */
@Data
public class AlarmDispatchRuleCreateRequest {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "事件类型不能为空（ALARM/OFFLINE）")
    private String eventType;

    /** ALARM 必填；OFFLINE 时为 null */
    private List<String> alarmLevels;

    @NotEmpty(message = "通知渠道不能为空")
    private List<String> channels;

    /** ALARM 必填；元素可为 "*" */
    private List<String> hazardPointIds;

    /** OFFLINE 必填；元素可为 "*" */
    private List<String> deviceIds;

    private RecipientSelection recipients;

    private Integer isEnabled = 1;

    private String remark;

    @Data
    public static class RecipientSelection {
        /** 可含 "*" */
        private List<String> roleIds;
        /** 可含 "*" */
        private List<String> deptIds;
        /** 可含 "*" */
        private List<String> userIds;
    }
}
```

- [ ] **步骤 2：编写 AlarmDispatchRuleDetailVO**

```java
package com.zwei.iot.alarm.dispatch.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 通知规则详情返回（含关联表展开）
 */
@Data
public class AlarmDispatchRuleDetailVO {

    private Long id;
    private String name;
    private String eventType;
    private List<String> alarmLevels;
    private List<String> channels;

    private List<String> hazardPointIds;
    private List<HazardPointOption> hazardPointOptions;   // 含 id+name，前端展示用

    private List<String> deviceIds;
    private List<DeviceOption> deviceOptions;

    private RecipientDetail recipients;

    private Integer isEnabled;
    private String remark;
    private Date createTime;
    private String createBy;

    @Data
    public static class HazardPointOption {
        private String id;
        private String name;
    }

    @Data
    public static class DeviceOption {
        private String id;
        private String name;
        private String code;
    }

    @Data
    public static class RecipientDetail {
        private List<RoleOption> roles;       // 含 id+name
        private List<DeptOption> depts;
        private List<UserOption> users;
        private boolean hasWildcardRole;
        private boolean hasWildcardDept;
        private boolean hasWildcardUser;
    }

    @Data
    public static class RoleOption {
        private String id;
        private String name;
    }

    @Data
    public static class DeptOption {
        private String id;
        private String name;
    }

    @Data
    public static class UserOption {
        private String id;
        private String name;
    }
}
```

- [ ] **步骤 3：编写 AlarmDispatchRuleItemVO（列表项）**

```java
package com.zwei.iot.alarm.dispatch.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 通知规则列表项
 */
@Data
public class AlarmDispatchRuleItemVO {

    private Long id;
    private String name;
    private String eventType;
    private List<String> alarmLevels;
    private List<String> channels;

    // 列表展示用汇总（避免 N+1）
    private boolean hazardPointAll;          // 是否含 "*"
    private List<String> hazardPointNames;   // 非通配时的具体名
    private boolean deviceAll;
    private List<String> deviceNames;

    private boolean recipientAll;            // 三类任一含 "*"
    private String recipientSummary;         // 如 "3 角色 / 2 部门 / 5 人"

    private Integer isEnabled;
    private Date createTime;
    private String createBy;
    private String remark;
}
```

- [ ] **步骤 4：编写 AlarmDispatchRuleQuery**

```java
package com.zwei.iot.alarm.dispatch.dto;

import lombok.Data;

/**
 * 通知规则列表查询参数
 */
@Data
public class AlarmDispatchRuleQuery {

    private String name;
    private String eventType;
    private Integer isEnabled;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
```

- [ ] **步骤 5：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/dto/
git commit -m "feat(alarm): 通知规则 DTO - 创建/详情/列表/查询"
```

---

### 任务 7：编写 Service 接口

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/IAlarmDispatchRuleService.java`

- [ ] **步骤 1：编写接口**

```java
package com.zwei.iot.alarm.dispatch.service;

import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleDetailVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleItemVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleQuery;

import java.util.List;

public interface IAlarmDispatchRuleService {

    /** 分页列表（带关联展开汇总） */
    List<AlarmDispatchRuleItemVO> selectList(AlarmDispatchRuleQuery query);

    /** 详情（包含所有关联数据） */
    AlarmDispatchRuleDetailVO selectDetail(Long id);

    /** 创建（事务：主表 + 三张关联表） */
    int create(AlarmDispatchRuleCreateRequest req);

    /** 更新（事务：先删后插关联表） */
    int update(Long id, AlarmDispatchRuleCreateRequest req);

    /** 逻辑删除（连带物理删除关联表） */
    int delete(Long id);

    /** 启用/禁用 */
    int toggleEnabled(Long id, Integer isEnabled);

    /**
     * 接收人选项接口支撑（前端勾选用）
     * 返回所有角色、部门（树）、用户的轻量列表
     */
    RecipientOptions selectRecipientOptions();

    record RecipientOptions(
        List<AlarmDispatchRuleDetailVO.RoleOption> roles,
        List<AlarmDispatchRuleDetailVO.DeptOption> depts,
        List<AlarmDispatchRuleDetailVO.UserOption> users
    ) {}
}
```

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/IAlarmDispatchRuleService.java
git commit -m "feat(alarm): 通知规则 Service 接口"
```

---

### 任务 8：编写 Service 实现测试（TDD）

**文件：**
- 创建：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/dispatch/AlarmDispatchRuleServiceImplTest.java`

> **注**：本任务先写测试，期望失败。下个任务实现 Service。

- [ ] **步骤 1：编写测试类**

```java
package com.zwei.iot.alarm.dispatch;

import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleDetailVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleItemVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleQuery;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleDeviceMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleHazardPointMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.service.impl.AlarmDispatchRuleServiceImpl;
import com.zwei.iot.alarm.dispatch.service.IAlarmDispatchRuleService.RecipientOptions;
import com.zwei.common.core.domain.entity.SysRole;
import com.zwei.common.core.domain.entity.SysDept;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.system.service.ISysDeptService;
import com.zwei.system.service.ISysRoleService;
import com.zwei.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmDispatchRuleServiceImplTest {

    @Mock private AlarmDispatchRuleMapper ruleMapper;
    @Mock private AlarmDispatchRuleHazardPointMapper hpMapper;
    @Mock private AlarmDispatchRuleDeviceMapper deviceMapper;
    @Mock private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Mock private ISysRoleService roleService;
    @Mock private ISysDeptService deptService;
    @Mock private ISysUserService userService;

    @InjectMocks
    private AlarmDispatchRuleServiceImpl service;

    private AlarmDispatchRuleCreateRequest buildAlarmReq() {
        AlarmDispatchRuleCreateRequest req = new AlarmDispatchRuleCreateRequest();
        req.setName("测试规则");
        req.setEventType("ALARM");
        req.setAlarmLevels(Arrays.asList("3", "4"));
        req.setChannels(Arrays.asList("SYSTEM", "SMS"));
        req.setHazardPointIds(Arrays.asList("1", "2"));
        req.setIsEnabled(1);

        AlarmDispatchRuleCreateRequest.RecipientSelection rs =
            new AlarmDispatchRuleCreateRequest.RecipientSelection();
        rs.setRoleIds(Arrays.asList("1"));
        rs.setUserIds(Arrays.asList("1", "2"));
        req.setRecipients(rs);
        return req;
    }

    @Test
    void create_alarm_should_insert_main_and_three_relation_tables() {
        // given
        AlarmDispatchRuleCreateRequest req = buildAlarmReq();

        // when
        int result = service.create(req);

        // then
        assertThat(result).isEqualTo(1);
        verify(ruleMapper, times(1)).insert(any());
        verify(hpMapper, times(1)).batchInsert(anyList());
        verify(recipientMapper, times(1)).batchInsert(anyList());
        verify(deviceMapper, never()).batchInsert(anyList());   // ALARM 不写设备关联
    }

    @Test
    void create_offline_should_insert_device_relation_not_hazard() {
        // given
        AlarmDispatchRuleCreateRequest req = new AlarmDispatchRuleCreateRequest();
        req.setName("离线规则");
        req.setEventType("OFFLINE");
        req.setChannels(Arrays.asList("SYSTEM"));
        req.setDeviceIds(Arrays.asList("10", "11"));
        req.setIsEnabled(1);
        AlarmDispatchRuleCreateRequest.RecipientSelection rs =
            new AlarmDispatchRuleCreateRequest.RecipientSelection();
        rs.setUserIds(Arrays.asList("*"));
        req.setRecipients(rs);

        // when
        service.create(req);

        // then
        verify(ruleMapper, times(1)).insert(any());
        verify(deviceMapper, times(1)).batchInsert(anyList());
        verify(hpMapper, never()).batchInsert(anyList());
    }

    @Test
    void update_should_delete_then_insert_relations() {
        // given
        Long id = 100L;
        AlarmDispatchRuleCreateRequest req = buildAlarmReq();

        // when
        service.update(id, req);

        // then
        verify(hpMapper, times(1)).deleteByRuleId(id);
        verify(hpMapper, times(1)).batchInsert(anyList());
        verify(recipientMapper, times(1)).deleteByRuleId(id);
        verify(recipientMapper, times(1)).batchInsert(anyList());
    }

    @Test
    void delete_should_logic_delete_main_and_physical_delete_relations() {
        // given
        Long id = 100L;

        // when
        service.delete(id);

        // then
        verify(ruleMapper, times(1)).logicDeleteById(id);
        verify(hpMapper, times(1)).deleteByRuleId(id);
        verify(deviceMapper, times(1)).deleteByRuleId(id);
        verify(recipientMapper, times(1)).deleteByRuleId(id);
    }

    @Test
    void toggleEnabled_should_call_updateEnabled() {
        // given
        Long id = 100L;

        // when
        service.toggleEnabled(id, 0);

        // then
        verify(ruleMapper, times(1)).updateEnabled(id, 0);
    }

    @Test
    void wildcard_normalization_when_id_is_null_or_empty() {
        // given
        AlarmDispatchRuleCreateRequest req = buildAlarmReq();
        req.getRecipients().setRoleIds(Arrays.asList("*", "1", "2"));   // 通配+具体

        // when
        service.create(req);

        // then: 选了 "*" 应只保留 "*"，清空具体 ID
        verify(recipientMapper, times(1)).batchInsert(argThat(list -> {
            // list 应只有一条 ("RULE", "*")
            return list.size() == 1
                && "ROLE".equals(((com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient) list.get(0)).getRecipientType())
                && "*".equals(((com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient) list.get(0)).getRecipientId());
        }));
    }
}
```

> **类型说明**：`SysRole`/`SysDept`/`SysUser` 在该项目里实际包路径可能是 `com.zwei.common.core.domain.entity.*`（RuoYi 标准），实现阶段按实际包路径调整 import。

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=AlarmDispatchRuleServiceImplTest
```

预期：编译失败（`AlarmDispatchRuleServiceImpl` 不存在）。

- [ ] **步骤 3：Commit 测试文件**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/dispatch/AlarmDispatchRuleServiceImplTest.java
git commit -m "test(alarm): 通知规则 Service 单测 - TDD 红灯"
```

---

### 任务 9：编写 Service 实现（让测试通过）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmDispatchRuleServiceImpl.java`

- [ ] **步骤 1：编写 ServiceImpl**

```java
package com.zwei.iot.alarm.dispatch.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleDevice;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleHazardPoint;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleDetailVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleItemVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleQuery;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleDeviceMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleHazardPointMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.service.IAlarmDispatchRuleService;
import com.zwei.system.service.ISysDeptService;
import com.zwei.system.service.ISysRoleService;
import com.zwei.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlarmDispatchRuleServiceImpl implements IAlarmDispatchRuleService {

    @Autowired private AlarmDispatchRuleMapper ruleMapper;
    @Autowired private AlarmDispatchRuleHazardPointMapper hpMapper;
    @Autowired private AlarmDispatchRuleDeviceMapper deviceMapper;
    @Autowired private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Autowired private ISysRoleService roleService;
    @Autowired private ISysDeptService deptService;
    @Autowired private ISysUserService userService;

    private static final String WILDCARD = "*";

    // ============= 列表 =============
    @Override
    public List<AlarmDispatchRuleItemVO> selectList(AlarmDispatchRuleQuery query) {
        // 1. 分页查主表
        Page<AlarmDispatchRule> page = new Page<>(query.getPageNum(), query.getPageSize());
        AlarmDispatchRule where = new AlarmDispatchRule();
        where.setName(query.getName());
        where.setEventType(query.getEventType());
        where.setIsEnabled(query.getIsEnabled());
        where.setDelFlag(0);
        // 简单实现：直接 list 全量后内存分页（数据量小）
        List<AlarmDispatchRule> all = ruleMapper.selectListByWhere(where);
        int start = (query.getPageNum() - 1) * query.getPageSize();
        int end = Math.min(start + query.getPageSize(), all.size());
        List<AlarmDispatchRule> paged = start >= all.size()
            ? Collections.emptyList()
            : all.subList(start, end);

        if (paged.isEmpty()) return Collections.emptyList();

        List<Long> ruleIds = paged.stream().map(AlarmDispatchRule::getId).toList();

        // 2. 批量查关联（避免 N+1）
        Map<Long, List<AlarmDispatchRuleHazardPoint>> hpMap = groupByRuleId(
            hpMapper.selectByRuleIds(ruleIds));
        Map<Long, List<AlarmDispatchRuleDevice>> devMap = groupByRuleId(
            deviceMapper.selectByRuleIds(ruleIds));
        Map<Long, List<AlarmDispatchRuleRecipient>> recipMap = groupByRuleId(
            recipientMapper.selectByRuleIds(ruleIds));

        // 3. 装配 VO
        return paged.stream().map(rule -> toItemVO(
            rule, hpMap.getOrDefault(rule.getId(), Collections.emptyList()),
            devMap.getOrDefault(rule.getId(), Collections.emptyList()),
            recipMap.getOrDefault(rule.getId(), Collections.emptyList())
        )).collect(Collectors.toList());
    }

    // ============= 详情 =============
    @Override
    public AlarmDispatchRuleDetailVO selectDetail(Long id) {
        AlarmDispatchRule rule = ruleMapper.selectById(id);
        if (rule == null) return null;

        AlarmDispatchRuleDetailVO vo = new AlarmDispatchRuleDetailVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setEventType(rule.getEventType());
        vo.setAlarmLevels(splitCsv(rule.getAlarmLevels()));
        vo.setChannels(splitCsv(rule.getChannels()));
        vo.setIsEnabled(rule.getIsEnabled());
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        vo.setCreateBy(rule.getCreateBy());

        // 隐患点
        List<AlarmDispatchRuleHazardPoint> hps = hpMapper.selectByRuleId(id);
        vo.setHazardPointIds(hps.stream()
            .map(AlarmDispatchRuleHazardPoint::getHazardPointId).toList());

        // 设备
        List<AlarmDispatchRuleDevice> devs = deviceMapper.selectByRuleId(id);
        vo.setDeviceIds(devs.stream()
            .map(AlarmDispatchRuleDevice::getDeviceId).toList());

        // 接收人
        List<AlarmDispatchRuleRecipient> recips = recipientMapper.selectByRuleId(id);
        AlarmDispatchRuleDetailVO.RecipientDetail rd = new AlarmDispatchRuleDetailVO.RecipientDetail();
        rd.setHasWildcardRole(false);
        rd.setHasWildcardDept(false);
        rd.setHasWildcardUser(false);
        for (AlarmDispatchRuleRecipient r : recips) {
            switch (r.getRecipientType()) {
                case "ROLE" -> {
                    if (WILDCARD.equals(r.getRecipientId())) rd.setHasWildcardRole(true);
                }
                case "DEPT" -> {
                    if (WILDCARD.equals(r.getRecipientId())) rd.setHasWildcardDept(true);
                }
                case "USER" -> {
                    if (WILDCARD.equals(r.getRecipientId())) rd.setHasWildcardUser(true);
                }
            }
        }
        vo.setRecipients(rd);

        return vo;
    }

    // ============= 创建 =============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int create(AlarmDispatchRuleCreateRequest req) {
        AlarmDispatchRule rule = new AlarmDispatchRule();
        rule.setName(req.getName());
        rule.setEventType(req.getEventType());
        rule.setAlarmLevels(joinCsv(req.getAlarmLevels()));
        rule.setChannels(joinCsv(req.getChannels()));
        rule.setIsEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : 1);
        rule.setDelFlag(0);
        rule.setRemark(req.getRemark());
        rule.setCreateBy(SecurityUtils.getUsername());

        ruleMapper.insert(rule);

        Long ruleId = rule.getId();
        saveRelations(ruleId, req);
        return 1;
    }

    // ============= 更新 =============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(Long id, AlarmDispatchRuleCreateRequest req) {
        AlarmDispatchRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("规则不存在: " + id);
        }
        rule.setName(req.getName());
        rule.setEventType(req.getEventType());
        rule.setAlarmLevels(joinCsv(req.getAlarmLevels()));
        rule.setChannels(joinCsv(req.getChannels()));
        rule.setIsEnabled(req.getIsEnabled());
        rule.setRemark(req.getRemark());
        rule.setUpdateBy(SecurityUtils.getUsername());
        ruleMapper.updateById(rule);

        // 先删后插
        hpMapper.deleteByRuleId(id);
        deviceMapper.deleteByRuleId(id);
        recipientMapper.deleteByRuleId(id);
        saveRelations(id, req);
        return 1;
    }

    // ============= 删除 =============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id) {
        ruleMapper.logicDeleteById(id);
        hpMapper.deleteByRuleId(id);
        deviceMapper.deleteByRuleId(id);
        recipientMapper.deleteByRuleId(id);
        return 1;
    }

    // ============= 启停 =============
    @Override
    public int toggleEnabled(Long id, Integer isEnabled) {
        return ruleMapper.updateEnabled(id, isEnabled);
    }

    // ============= 接收人选项 =============
    @Override
    public RecipientOptions selectRecipientOptions() {
        // 复用 sys-service 接口拉取角色/部门/用户
        // 实现细节：调用 roleService.selectRoleAll()、deptService.selectDeptList()、userService.selectUserList()
        // 包路径与签名实现时核对（RuoYi 标准）
        List<AlarmDispatchRuleDetailVO.RoleOption> roles = roleService.selectRoleAll().stream()
            .map(r -> {
                AlarmDispatchRuleDetailVO.RoleOption o = new AlarmDispatchRuleDetailVO.RoleOption();
                o.setId(String.valueOf(r.getRoleId()));
                o.setName(r.getRoleName());
                return o;
            }).toList();

        List<AlarmDispatchRuleDetailVO.DeptOption> depts = deptService.selectDeptList(new com.zwei.common.core.domain.entity.SysDept()).stream()
            .map(d -> {
                AlarmDispatchRuleDetailVO.DeptOption o = new AlarmDispatchRuleDetailVO.DeptOption();
                o.setId(String.valueOf(d.getDeptId()));
                o.setName(d.getDeptName());
                return o;
            }).toList();

        List<AlarmDispatchRuleDetailVO.UserOption> users = userService.selectUserList(new com.zwei.common.core.domain.entity.SysUser()).stream()
            .map(u -> {
                AlarmDispatchRuleDetailVO.UserOption o = new AlarmDispatchRuleDetailVO.UserOption();
                o.setId(String.valueOf(u.getUserId()));
                o.setName(u.getUserName());
                return o;
            }).toList();

        return new RecipientOptions(roles, depts, users);
    }

    // ============= 私有辅助 =============

    private void saveRelations(Long ruleId, AlarmDispatchRuleCreateRequest req) {
        // 隐患点（仅 ALARM）
        if ("ALARM".equals(req.getEventType()) && req.getHazardPointIds() != null) {
            List<String> normalized = normalizeWildcard(req.getHazardPointIds());
            if (!normalized.isEmpty()) {
                hpMapper.batchInsert(normalized.stream().map(hp -> {
                    AlarmDispatchRuleHazardPoint e = new AlarmDispatchRuleHazardPoint();
                    e.setRuleId(ruleId);
                    e.setHazardPointId(hp);
                    return e;
                }).toList());
            }
        }
        // 设备（仅 OFFLINE）
        if ("OFFLINE".equals(req.getEventType()) && req.getDeviceIds() != null) {
            List<String> normalized = normalizeWildcard(req.getDeviceIds());
            if (!normalized.isEmpty()) {
                deviceMapper.batchInsert(normalized.stream().map(d -> {
                    AlarmDispatchRuleDevice e = new AlarmDispatchRuleDevice();
                    e.setRuleId(ruleId);
                    e.setDeviceId(d);
                    return e;
                }).toList());
            }
        }
        // 接收人
        if (req.getRecipients() != null) {
            List<AlarmDispatchRuleRecipient> list = new ArrayList<>();
            buildRecipients(ruleId, "ROLE", req.getRecipients().getRoleIds(), list);
            buildRecipients(ruleId, "DEPT", req.getRecipients().getDeptIds(), list);
            buildRecipients(ruleId, "USER", req.getRecipients().getUserIds(), list);
            if (!list.isEmpty()) recipientMapper.batchInsert(list);
        }
    }

    private void buildRecipients(Long ruleId, String type, List<String> ids,
                                  List<AlarmDispatchRuleRecipient> out) {
        if (ids == null || ids.isEmpty()) return;
        List<String> normalized = normalizeWildcard(ids);
        for (String id : normalized) {
            AlarmDispatchRuleRecipient r = new AlarmDispatchRuleRecipient();
            r.setRuleId(ruleId);
            r.setRecipientType(type);
            r.setRecipientId(id);
            out.add(r);
        }
    }

    /**
     * 通配符归一化：列表含 "*" 时只保留 "*"（与其他具体项互斥）。
     * 同时去重、去空。
     */
    private List<String> normalizeWildcard(List<String> ids) {
        if (ids == null) return Collections.emptyList();
        Set<String> seen = new LinkedHashSet<>();
        for (String id : ids) {
            if (StringUtils.isBlank(id)) continue;
            seen.add(id.trim());
        }
        if (seen.contains(WILDCARD)) {
            return Collections.singletonList(WILDCARD);
        }
        return new ArrayList<>(seen);
    }

    private AlarmDispatchRuleItemVO toItemVO(AlarmDispatchRule rule,
            List<AlarmDispatchRuleHazardPoint> hps,
            List<AlarmDispatchRuleDevice> devs,
            List<AlarmDispatchRuleRecipient> recips) {
        AlarmDispatchRuleItemVO vo = new AlarmDispatchRuleItemVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setEventType(rule.getEventType());
        vo.setAlarmLevels(splitCsv(rule.getAlarmLevels()));
        vo.setChannels(splitCsv(rule.getChannels()));
        vo.setIsEnabled(rule.getIsEnabled());
        vo.setCreateTime(rule.getCreateTime());
        vo.setCreateBy(rule.getCreateBy());
        vo.setRemark(rule.getRemark());

        // 隐患点
        boolean hpAll = hps.stream().anyMatch(h -> WILDCARD.equals(h.getHazardPointId()));
        vo.setHazardPointAll(hpAll);
        if (!hpAll) {
            vo.setHazardPointNames(hps.stream()
                .map(AlarmDispatchRuleHazardPoint::getHazardPointId).toList());
        }

        // 设备
        boolean devAll = devs.stream().anyMatch(d -> WILDCARD.equals(d.getDeviceId()));
        vo.setDeviceAll(devAll);
        if (!devAll) {
            vo.setDeviceNames(devs.stream()
                .map(AlarmDispatchRuleDevice::getDeviceId).toList());
        }

        // 接收人
        long roleCnt = recips.stream().filter(r -> "ROLE".equals(r.getRecipientType())).count();
        long deptCnt = recips.stream().filter(r -> "DEPT".equals(r.getRecipientType())).count();
        long userCnt = recips.stream().filter(r -> "USER".equals(r.getRecipientType())).count();
        boolean recipAll = recips.stream().anyMatch(r -> WILDCARD.equals(r.getRecipientId()));
        vo.setRecipientAll(recipAll);
        if (!recipAll) {
            vo.setRecipientSummary(
                roleCnt + " 角色 / " + deptCnt + " 部门 / " + userCnt + " 人");
        }
        return vo;
    }

    private <T> Map<Long, List<T>> groupByRuleId(List<T> list) {
        // 简化：实现时按对象 ruleId 反射或函数式分组
        // 此处使用具体类型分支（避免反射）
        return Collections.emptyMap();  // 实现时按具体类型补全
    }

    private List<String> splitCsv(String csv) {
        if (StringUtils.isBlank(csv)) return Collections.emptyList();
        return Arrays.asList(csv.split(","));
    }

    private String joinCsv(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }
}
```

> **注**：
> 1. `ruleMapper.selectListByWhere(where)` 需在 Mapper 接口加方法 + XML，实现阶段补全
> 2. `groupByRuleId` 通用版本实现时按具体类型补全（参考 RuoYi `stream().collect(Collectors.groupingBy(...))`）
> 3. SysRole/SysDept/SysUser 包路径按项目实际调整（RuoYi 标准为 `com.zwei.common.core.domain.entity.*`）
> 4. `SecurityUtils.getUsername()` 来自 `com.zwei.common.utils.SecurityUtils`

- [ ] **步骤 2：补充主表 selectListByWhere**

在 `AlarmDispatchRuleMapper` 接口加方法：

```java
List<AlarmDispatchRule> selectListByWhere(@Param("where") AlarmDispatchRule where);
```

在 `AlarmDispatchRuleMapper.xml` 加：

```xml
<select id="selectListByWhere" resultType="com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule">
    SELECT id, name, event_type, alarm_levels, channels, is_enabled, del_flag,
           create_by, create_time, update_by, update_time, remark
    FROM alarm_dispatch_rule
    WHERE del_flag = 0
    <if test="where.name != null and where.name != ''">
        AND name LIKE CONCAT('%', #{where.name}, '%')
    </if>
    <if test="where.eventType != null and where.eventType != ''">
        AND event_type = #{where.eventType}
    </if>
    <if test="where.isEnabled != null">
        AND is_enabled = #{where.isEnabled}
    </if>
    ORDER BY create_time DESC
</select>
```

- [ ] **步骤 3：补全 groupByRuleId 工具方法**

```java
// 在 ServiceImpl 内替换为具体类型版本
private Map<Long, List<AlarmDispatchRuleHazardPoint>> groupHp(List<AlarmDispatchRuleHazardPoint> list) {
    return list.stream().collect(Collectors.groupingBy(AlarmDispatchRuleHazardPoint::getRuleId));
}
private Map<Long, List<AlarmDispatchRuleDevice>> groupDev(List<AlarmDispatchRuleDevice> list) {
    return list.stream().collect(Collectors.groupingBy(AlarmDispatchRuleDevice::getRuleId));
}
private Map<Long, List<AlarmDispatchRuleRecipient>> groupRecip(List<AlarmDispatchRuleRecipient> list) {
    return list.stream().collect(Collectors.groupingBy(AlarmDispatchRuleRecipient::getRuleId));
}
```

并在 `selectList` 中替换 `groupByRuleId` 调用为这三个具体方法。

- [ ] **步骤 4：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 5：运行单测验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=AlarmDispatchRuleServiceImplTest
```

预期：所有 6 个测试 PASS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmDispatchRuleServiceImpl.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/mapper/AlarmDispatchRuleMapper.java
git add server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleMapper.xml
git commit -m "feat(alarm): 通知规则 Service 实现 - TDD 绿灯"
```

---

### 任务 10：编写 Controller

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/controller/AlarmDispatchRuleController.java`

> 注：项目实际 Controller 在 `zwei-admin` 模块下，按现有 `AlarmDispatchRuleController`（如果已存在）实际位置调整。

- [ ] **步骤 1：编写 Controller**

```java
package com.zwei.iot.alarm.dispatch.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleDetailVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleItemVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleQuery;
import com.zwei.iot.alarm.dispatch.service.IAlarmDispatchRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alarm/dispatch")
public class AlarmDispatchRuleController extends BaseController {

    @Autowired
    private IAlarmDispatchRuleService service;

    /** 分页列表 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:list')")
    @GetMapping("/list")
    public TableDataInfo list(AlarmDispatchRuleQuery query) {
        startPage();
        List<AlarmDispatchRuleItemVO> list = service.selectList(query);
        return getDataTable(list);
    }

    /** 详情 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        AlarmDispatchRuleDetailVO vo = service.selectDetail(id);
        return vo == null ? AjaxResult.error("规则不存在") : AjaxResult.success(vo);
    }

    /** 新增 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:add')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody AlarmDispatchRuleCreateRequest req) {
        return toAjax(service.create(req));
    }

    /** 编辑 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:edit')")
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id,
                              @Valid @RequestBody AlarmDispatchRuleCreateRequest req) {
        req.setId(id);
        return toAjax(service.update(id, req));
    }

    /** 删除 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(service.delete(id));
    }

    /** 启用/禁用 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:edit')")
    @PutMapping("/{id}/enabled")
    public AjaxResult toggleEnabled(@PathVariable Long id,
                                     @RequestBody java.util.Map<String, Object> body) {
        Integer isEnabled = (Integer) body.get("isEnabled");
        return toAjax(service.toggleEnabled(id, isEnabled));
    }

    /** 接收人选项（前端勾选用） */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:list')")
    @GetMapping("/recipient-options")
    public AjaxResult recipientOptions() {
        return AjaxResult.success(service.selectRecipientOptions());
    }
}
```

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：启动后端，手工调用接口验证**

启动 `com.zwei.RuoYiApplication`（profile=local），用 Postman 或 curl：

```bash
# 登录拿 token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","code":"...","uuid":"..."}'

# 列表
curl http://localhost:8080/api/v1/alarm/dispatch/list \
  -H "Authorization: Bearer <token>"

# 详情（用迁移后第一条 ID）
curl http://localhost:8080/api/v1/alarm/dispatch/1 \
  -H "Authorization: Bearer <token>"

# 新增
curl -X POST http://localhost:8080/api/v1/alarm/dispatch \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"E2E 测试规则",
    "eventType":"ALARM",
    "alarmLevels":["3","4"],
    "channels":["SYSTEM","SMS"],
    "hazardPointIds":["*"],
    "recipients":{"userIds":["*"]},
    "isEnabled":1
  }'

# 接收人选项
curl http://localhost:8080/api/v1/alarm/dispatch/recipient-options \
  -H "Authorization: Bearer <token>"
```

预期：
- 列表 200，含迁移过来的旧规则
- 新增后数据库 alarm_dispatch_rule、_hazard_point、_recipient 都有新记录
- recipient-options 返回角色/部门/用户列表

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/controller/AlarmDispatchRuleController.java
git commit -m "feat(alarm): 通知规则 Controller - CRUD + 接收人选项"
```

---

### 任务 11：前端 API 模块对齐

**文件：**
- 创建：`web/src/api/alarmDispatch.ts`

> 注：项目原有 `web/src/api/alarm.ts` 含 `AlarmDispatchRuleItem` 等类型，但与新版后端契约不一致。新建 `alarmDispatch.ts` 模块独立，原 alarm.ts 中相关定义保留但标记 deprecated（避免影响其他模块）。

- [ ] **步骤 1：编写 alarmDispatch.ts**

```typescript
import request from '@/utils/request'

export type AlarmEventType = 'ALARM' | 'OFFLINE'
export type AlarmRecipientType = 'ROLE' | 'DEPT' | 'USER'
export type NotifyChannel = 'SYSTEM' | 'SMS' | 'EMAIL'

export interface RecipientSelection {
  roleIds?: string[]
  deptIds?: string[]
  userIds?: string[]
}

export interface AlarmDispatchRuleCreateRequest {
  id?: number
  name: string
  eventType: AlarmEventType
  alarmLevels?: string[]
  channels: NotifyChannel[]
  hazardPointIds?: string[]
  deviceIds?: string[]
  recipients: RecipientSelection
  isEnabled: number
  remark?: string
}

export interface RoleOption { id: string; name: string }
export interface DeptOption { id: string; name: string }
export interface UserOption { id: string; name: string; deptName?: string }

export interface RecipientOptions {
  roles: RoleOption[]
  depts: DeptOption[]
  users: UserOption[]
}

export interface AlarmDispatchRuleItemVO {
  id: number
  name: string
  eventType: AlarmEventType
  alarmLevels: string[]
  channels: NotifyChannel[]
  hazardPointAll: boolean
  hazardPointNames?: string[]
  deviceAll: boolean
  deviceNames?: string[]
  recipientAll: boolean
  recipientSummary?: string
  isEnabled: number
  createTime: string
  createBy: string
  remark?: string
}

export interface AlarmDispatchRuleDetailVO {
  id: number
  name: string
  eventType: AlarmEventType
  alarmLevels: string[]
  channels: NotifyChannel[]
  hazardPointIds: string[]
  hazardPointOptions?: Array<{ id: string; name: string }>
  deviceIds: string[]
  deviceOptions?: Array<{ id: string; name: string; code: string }>
  recipients: {
    roles: RoleOption[]
    depts: DeptOption[]
    users: UserOption[]
    hasWildcardRole: boolean
    hasWildcardDept: boolean
    hasWildcardUser: boolean
  }
  isEnabled: number
  remark?: string
  createTime: string
  createBy: string
}

export interface AlarmDispatchRuleQuery {
  name?: string
  eventType?: AlarmEventType
  isEnabled?: number
  pageNum?: number
  pageSize?: number
}

// ===== API =====

export const getDispatchRuleList = (params: AlarmDispatchRuleQuery) =>
  request.get<{ rows: AlarmDispatchRuleItemVO[]; total: number }>(
    '/alarm/dispatch/list', { params }
  )

export const getDispatchRuleDetail = (id: number) =>
  request.get<{ data: AlarmDispatchRuleDetailVO }>(`/alarm/dispatch/${id}`)

export const createDispatchRule = (payload: AlarmDispatchRuleCreateRequest) =>
  request.post('/alarm/dispatch', payload)

export const updateDispatchRule = (id: number, payload: AlarmDispatchRuleCreateRequest) =>
  request.put(`/alarm/dispatch/${id}`, payload)

export const deleteDispatchRule = (id: number) =>
  request.delete(`/alarm/dispatch/${id}`)

export const toggleDispatchRuleEnabled = (id: number, isEnabled: number) =>
  request.put(`/alarm/dispatch/${id}/enabled`, { isEnabled })

export const getRecipientOptions = () =>
  request.get<{ data: RecipientOptions }>('/alarm/dispatch/recipient-options')
```

- [ ] **步骤 2：TypeScript 类型检查**

```bash
cd web && npx vue-tsc --noEmit
```

预期：无类型错误。

- [ ] **步骤 3：Commit**

```bash
git add web/src/api/alarmDispatch.ts
git commit -m "feat(alarm/web): 通知规则 API 模块 - 类型契约对齐"
```

---

### 任务 12：编写 RecipientPicker 组件

**文件：**
- 创建：`web/src/views/alarm/components/RecipientPicker.vue`

- [ ] **步骤 1：编写组件**

```vue
<template>
  <div class="recipient-picker">
    <el-tabs v-model="activeTab">
      <!-- 按角色 -->
      <el-tab-pane label="按角色" name="role">
        <el-checkbox
          v-model="roleAll"
          @change="onRoleAllChange"
          style="margin-bottom: 8px;"
        >所有角色</el-checkbox>
        <el-checkbox-group v-model="localRoleIds" :disabled="roleAll" class="checkbox-grid">
          <el-checkbox
            v-for="r in options.roles"
            :key="r.id"
            :label="r.id"
          >{{ r.name }}</el-checkbox>
        </el-checkbox-group>
      </el-tab-pane>

      <!-- 按部门 -->
      <el-tab-pane label="按部门" name="dept">
        <el-checkbox
          v-model="deptAll"
          @change="onDeptAllChange"
          style="margin-bottom: 8px;"
        >所有部门</el-checkbox>
        <el-tree
          ref="deptTreeRef"
          :data="deptTreeData"
          show-checkbox
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          :disabled="deptAll"
          @check="onDeptTreeCheck"
        />
      </el-tab-pane>

      <!-- 指定人员 -->
      <el-tab-pane label="指定人员" name="user">
        <el-select
          v-model="localUserIds"
          multiple
          filterable
          placeholder="搜索用户名"
          style="width: 100%;"
        >
          <el-option label="所有用户" value="*" />
          <el-option
            v-for="u in options.users"
            :key="u.id"
            :label="u.name + (u.deptName ? '(' + u.deptName + ')' : '')"
            :value="u.id"
          />
        </el-select>
      </el-tab-pane>
    </el-tabs>

    <!-- 已选汇总 -->
    <div class="selection-summary" v-if="hasAnySelection">
      <span class="summary-label">已选：</span>
      <el-tag v-if="roleAll" type="warning" closable @close="roleAll = false">全部角色</el-tag>
      <el-tag v-for="rid in localRoleIds" v-else :key="'r'+rid" type="info"
              closable @close="removeId('role', rid)">
        {{ roleName(rid) }}
      </el-tag>
      <el-tag v-if="deptAll" type="warning" closable @close="deptAll = false">全部部门</el-tag>
      <el-tag v-for="did in localDeptIds" v-else :key="'d'+did" type="info"
              closable @close="removeId('dept', did)">
        {{ deptName(did) }}
      </el-tag>
      <el-tag v-if="localUserIds.includes('*')" type="warning" closable
              @close="removeWildcard('user')">全部用户</el-tag>
      <el-tag v-for="uid in localUserIds.filter(i => i !== '*')" v-else :key="'u'+uid"
              type="info" closable @close="removeId('user', uid)">
        {{ userName(uid) }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRecipientOptions,
  type RecipientOptions,
  type RoleOption,
  type DeptOption,
  type UserOption
} from '@/api/alarmDispatch'

interface Props {
  modelValue: {
    roleIds?: string[]
    deptIds?: string[]
    userIds?: string[]
  }
}
const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: Props['modelValue']): void
}>()

const activeTab = ref<'role' | 'dept' | 'user'>('role')
const options = ref<RecipientOptions>({ roles: [], depts: [], users: [] })

const roleAll = ref(false)
const deptAll = ref(false)
const localRoleIds = ref<string[]>([])
const localDeptIds = ref<string[]>([])
const localUserIds = ref<string[]>([])

const deptTreeRef = ref()

// 加载选项
onMounted(async () => {
  try {
    const res = await getRecipientOptions()
    options.value = (res as any).data || res
  } catch (e) {
    ElMessage.error('接收人选项加载失败')
  }
})

// 初始化 modelValue（编辑回填）
watch(() => props.modelValue, (v) => {
  if (!v) return
  if (v.roleIds?.includes('*')) { roleAll.value = true; localRoleIds.value = [] }
  else localRoleIds.value = v.roleIds ? [...v.roleIds] : []
  if (v.deptIds?.includes('*')) { deptAll.value = true; localDeptIds.value = [] }
  else localDeptIds.value = v.deptIds ? [...v.deptIds] : []
  localUserIds.value = v.userIds ? [...v.userIds] : []
}, { immediate: true })

// 部门树构造（把扁平结构按 parent_id 转 tree；如果接口直接返回 tree 则跳过）
const deptTreeData = computed(() => {
  // 简化：后端返回扁平列表，前端按 parentId 拼装
  // 实现时根据 ISysDeptService.selectDeptList 实际返回结构调整
  return options.value.depts.map(d => ({ ...d, children: undefined }))
})

// 通配符互斥
function onRoleAllChange(v: any) {
  if (v) localRoleIds.value = []
  syncEmit()
}
function onDeptAllChange(v: any) {
  if (v) {
    localDeptIds.value = []
    deptTreeRef.value?.setCheckedKeys([])
  }
  syncEmit()
}
function onDeptTreeCheck() {
  const checked = deptTreeRef.value?.getCheckedKeys() || []
  localDeptIds.value = checked.filter((k: any) => k !== '*')
  syncEmit()
}

function removeId(type: 'role' | 'dept' | 'user', id: string) {
  if (type === 'role') localRoleIds.value = localRoleIds.value.filter(i => i !== id)
  if (type === 'dept') {
    localDeptIds.value = localDeptIds.value.filter(i => i !== id)
    deptTreeRef.value?.setChecked(id, false, false)
  }
  if (type === 'user') localUserIds.value = localUserIds.value.filter(i => i !== id)
  syncEmit()
}

function removeWildcard(type: 'user') {
  if (type === 'user') localUserIds.value = localUserIds.value.filter(i => i !== '*')
  syncEmit()
}

const hasAnySelection = computed(() => {
  return roleAll.value || deptAll.value
      || localUserIds.value.includes('*')
      || localRoleIds.value.length > 0
      || localDeptIds.value.length > 0
      || localUserIds.value.filter(i => i !== '*').length > 0
})

function syncEmit() {
  const result: Props['modelValue'] = {}
  if (roleAll.value) result.roleIds = ['*']
  else if (localRoleIds.value.length) result.roleIds = [...localRoleIds.value]

  if (deptAll.value) result.deptIds = ['*']
  else if (localDeptIds.value.length) result.deptIds = [...localDeptIds.value]

  if (localUserIds.value.length) result.userIds = [...localUserIds.value]

  emit('update:modelValue', result)
}

// 监听本地变更同步出去
watch([roleAll, localRoleIds, deptAll, localDeptIds, localUserIds], syncEmit, { deep: true })

function roleName(id: string) {
  return options.value.roles.find(r => r.id === id)?.name || id
}
function deptName(id: string) {
  return options.value.depts.find(d => d.id === id)?.name || id
}
function userName(id: string) {
  return options.value.users.find(u => u.id === id)?.name || id
}
</script>

<style scoped>
.recipient-picker {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
}
.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 4px 12px;
}
.selection-summary {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}
.summary-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}
.el-tag {
  margin: 2px 4px 2px 0;
}
</style>
```

- [ ] **步骤 2：启动 dev server 手工验证（独立预览）**

```bash
cd web && npm run dev
```

由于该组件需要父组件提供 modelValue 才能预览，可临时在 `views/dev/RecipientPickerPreview.vue` 写个 wrapper：

```vue
<template>
  <RecipientPicker v-model="recipients" />
  <pre>{{ recipients }}</pre>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import RecipientPicker from '@/views/alarm/components/RecipientPicker.vue'
const recipients = ref({})
</script>
```

并在 `router/index.ts` 临时加一条 `/dev/recipient-picker` 路由。

预期：
- 加载时调 `/api/v1/alarm/dispatch/recipient-options` 拿角色/部门/用户
- Tab 切换正常
- 选 "所有角色" → 其他角色 disabled，已选汇总显示 "全部角色"
- 选用户含 "*" → 不显示具体用户

- [ ] **步骤 3：移除预览路由**

预览验证后，删除 `views/dev/RecipientPickerPreview.vue` 和 router 中的临时路由。

- [ ] **步骤 4：Commit**

```bash
git add web/src/views/alarm/components/RecipientPicker.vue
git commit -m "feat(alarm/web): RecipientPicker 组件 - 角色/部门/用户三 Tab 多选"
```

---

### 任务 13：改造 NotificationSetting.vue

**文件：**
- 修改：`web/src/views/alarm/NotificationSetting.vue`

> 注：原文件较大（含列表 + 弹窗 + 表单）。本任务整体重写，保留原文件备份到 `.bak.vue` 便于回滚。

- [ ] **步骤 1：备份原文件**

```bash
cp web/src/views/alarm/NotificationSetting.vue web/src/views/alarm/NotificationSetting.vue.bak
```

- [ ] **步骤 2：重写 NotificationSetting.vue**

```vue
<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="queryParams" @submit.prevent>
      <el-form-item label="名称">
        <el-input v-model="queryParams.name" placeholder="规则名称" clearable
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="事件类型">
        <el-select v-model="queryParams.eventType" clearable placeholder="全部">
          <el-option label="告警事件" value="ALARM" />
          <el-option label="设备离线" value="OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.isEnabled" clearable placeholder="全部">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" @click="handleAdd">新增</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table :data="list" v-loading="loading" border>
      <el-table-column label="名称" prop="name" min-width="160" />
      <el-table-column label="事件类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.eventType === 'ALARM' ? 'danger' : 'warning'">
            {{ row.eventType === 'ALARM' ? '告警' : '设备离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="告警等级" width="180">
        <template #default="{ row }">
          <el-tag v-for="lv in row.alarmLevels" :key="lv" size="small"
                  :type="levelTagType(lv)" style="margin-right: 4px;">
            {{ levelLabel(lv) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="隐患点/设备" min-width="180">
        <template #default="{ row }">
          <template v-if="row.eventType === 'ALARM'">
            <el-tag v-if="row.hazardPointAll" size="small" type="warning">全部隐患点</el-tag>
            <span v-else>{{ (row.hazardPointNames || []).join('、') }}</span>
          </template>
          <template v-else>
            <el-tag v-if="row.deviceAll" size="small" type="warning">全部设备</el-tag>
            <span v-else>{{ (row.deviceNames || []).join('、') }}</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="接收人" min-width="160">
        <template #default="{ row }">
          <el-tag v-if="row.recipientAll" size="small" type="warning">全部</el-tag>
          <span v-else>{{ row.recipientSummary || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="渠道" width="180">
        <template #default="{ row }">
          <el-tag v-for="ch in row.channels" :key="ch" size="small"
                  :type="channelTagType(ch)" style="margin-right: 4px;">
            {{ channelLabel(ch) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.isEnabled === 1"
                     @change="(v: boolean) => handleToggleEnabled(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="720px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入规则名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="事件类型" prop="eventType">
          <el-radio-group v-model="form.eventType" @change="onEventTypeChange">
            <el-radio label="ALARM">告警事件</el-radio>
            <el-radio label="OFFLINE">设备离线</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="告警等级" prop="alarmLevels" v-if="form.eventType === 'ALARM'">
          <el-checkbox-group v-model="form.alarmLevels">
            <el-checkbox label="1">蓝色</el-checkbox>
            <el-checkbox label="2">黄色</el-checkbox>
            <el-checkbox label="3">橙色</el-checkbox>
            <el-checkbox label="4">红色</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="隐患点" prop="hazardPointIds" v-if="form.eventType === 'ALARM'">
          <el-select v-model="form.hazardPointIds" multiple filterable
                     placeholder="请选择（支持全部）" style="width: 100%;">
            <el-option label="全部隐患点" value="*" />
            <el-option v-for="hp in hazardPointOptions" :key="hp.id"
                       :label="hp.name" :value="String(hp.id)" />
          </el-select>
        </el-form-item>

        <el-form-item label="设备" prop="deviceIds" v-if="form.eventType === 'OFFLINE'">
          <el-select v-model="form.deviceIds" multiple filterable
                     placeholder="请选择（支持全部）" style="width: 100%;">
            <el-option label="全部设备" value="*" />
            <el-option v-for="d in deviceOptions" :key="d.id"
                       :label="d.name + '（' + d.code + '）'" :value="String(d.id)" />
          </el-select>
        </el-form-item>

        <el-form-item label="通知人员" prop="recipients">
          <RecipientPicker v-model="form.recipients" />
        </el-form-item>

        <el-form-item label="通知渠道" prop="channels">
          <el-checkbox-group v-model="form.channels">
            <el-checkbox label="SYSTEM">系统消息</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="EMAIL">邮件</el-checkbox>
          </el-checkbox-group>
          <div class="form-help" v-if="form.eventType === 'ALARM'">系统消息必选（确保站内可达）</div>
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getDispatchRuleList,
  getDispatchRuleDetail,
  createDispatchRule,
  updateDispatchRule,
  deleteDispatchRule,
  toggleDispatchRuleEnabled,
  type AlarmDispatchRuleItemVO,
  type AlarmDispatchRuleCreateRequest,
  type AlarmDispatchRuleQuery
} from '@/api/alarmDispatch'
import { getHazardPointPage } from '@/api/hazardPoint'
import { getDeviceList } from '@/api/device'
import RecipientPicker from './components/RecipientPicker.vue'

const loading = ref(false)
const submitting = ref(false)
const list = ref<AlarmDispatchRuleItemVO[]>([])
const total = ref(0)
const queryParams = reactive<AlarmDispatchRuleQuery>({
  name: '', eventType: undefined, isEnabled: undefined,
  pageNum: 1, pageSize: 10
})

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

interface FormState {
  id?: number
  name: string
  eventType: 'ALARM' | 'OFFLINE'
  alarmLevels: string[]
  channels: string[]
  hazardPointIds: string[]
  deviceIds: string[]
  recipients: { roleIds?: string[]; deptIds?: string[]; userIds?: string[] }
  isEnabled: number
  remark: string
}

const defaultForm = (): FormState => ({
  name: '',
  eventType: 'ALARM',
  alarmLevels: [],
  channels: ['SYSTEM'],
  hazardPointIds: [],
  deviceIds: [],
  recipients: {},
  isEnabled: 1,
  remark: ''
})
const form = reactive<FormState>(defaultForm())

const dialogTitle = computed(() => form.id ? '编辑通知规则' : '新增通知规则')

const rules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  alarmLevels: [{
    validator: (_r, _v, cb) => {
      if (form.eventType === 'ALARM' && form.alarmLevels.length === 0)
        cb(new Error('告警事件必须选择等级'))
      else cb()
    }, trigger: 'change'
  }],
  hazardPointIds: [{
    validator: (_r, _v, cb) => {
      if (form.eventType === 'ALARM' && form.hazardPointIds.length === 0)
        cb(new Error('请选择隐患点'))
      else cb()
    }, trigger: 'change'
  }],
  deviceIds: [{
    validator: (_r, _v, cb) => {
      if (form.eventType === 'OFFLINE' && form.deviceIds.length === 0)
        cb(new Error('请选择设备'))
      else cb()
    }, trigger: 'change'
  }],
  channels: [{ required: true, type: 'array', min: 1, message: '请至少选择一个渠道', trigger: 'change' }],
  recipients: [{
    validator: (_r, _v, cb) => {
      const r = form.recipients
      const total = (r.roleIds?.length || 0) + (r.deptIds?.length || 0) + (r.userIds?.length || 0)
      if (total === 0) cb(new Error('请选择通知人员'))
      else cb()
    }, trigger: 'change'
  }]
}

// 隐患点 / 设备选项
const hazardPointOptions = ref<Array<{ id: number; name: string }>>([])
const deviceOptions = ref<Array<{ id: number; name: string; code: string }>>([])

async function loadOptions() {
  try {
    const [hpRes, devRes] = await Promise.all([
      getHazardPointPage({ pageNum: 1, pageSize: 1000 }),
      getDeviceList({ pageNum: 1, pageSize: 1000 })
    ])
    hazardPointOptions.value = (hpRes as any).rows || []
    deviceOptions.value = (devRes as any).rows || []
  } catch (e) {
    ElMessage.error('选项加载失败')
  }
}

async function getList() {
  loading.value = true
  try {
    const res: any = await getDispatchRuleList(queryParams)
    list.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.name = ''
  queryParams.eventType = undefined
  queryParams.isEnabled = undefined
  handleQuery()
}

function handleAdd() {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: AlarmDispatchRuleItemVO) {
  try {
    const res: any = await getDispatchRuleDetail(row.id)
    const d = res.data
    Object.assign(form, {
      id: d.id,
      name: d.name,
      eventType: d.eventType,
      alarmLevels: d.alarmLevels || [],
      channels: d.channels || [],
      hazardPointIds: d.hazardPointIds || [],
      deviceIds: d.deviceIds || [],
      recipients: {
        roleIds: d.recipients.hasWildcardRole ? ['*'] : (d.recipients.roles || []).map((r: any) => r.id),
        deptIds: d.recipients.hasWildcardDept ? ['*'] : (d.recipients.depts || []).map((r: any) => r.id),
        userIds: d.recipients.hasWildcardUser
          ? ['*']
          : (d.recipients.users || []).map((r: any) => r.id)
      },
      isEnabled: d.isEnabled,
      remark: d.remark || ''
    })
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload: AlarmDispatchRuleCreateRequest = {
      id: form.id,
      name: form.name,
      eventType: form.eventType,
      alarmLevels: form.eventType === 'ALARM' ? form.alarmLevels : undefined,
      channels: form.channels as any,
      hazardPointIds: form.eventType === 'ALARM' ? form.hazardPointIds : undefined,
      deviceIds: form.eventType === 'OFFLINE' ? form.deviceIds : undefined,
      recipients: form.recipients,
      isEnabled: form.isEnabled,
      remark: form.remark
    }
    if (form.id) {
      await updateDispatchRule(form.id, payload)
      ElMessage.success('编辑成功')
    } else {
      await createDispatchRule(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: AlarmDispatchRuleItemVO) {
  await ElMessageBox.confirm(`确认删除规则「${row.name}」？`, '提示', { type: 'warning' })
  await deleteDispatchRule(row.id)
  ElMessage.success('删除成功')
  getList()
}

async function handleToggleEnabled(row: AlarmDispatchRuleItemVO, v: boolean) {
  const isEnabled = v ? 1 : 0
  await toggleDispatchRuleEnabled(row.id, isEnabled)
  row.isEnabled = isEnabled
  ElMessage.success(v ? '已启用' : '已禁用')
}

function onEventTypeChange(v: string) {
  // 切换时清空对方字段
  if (v === 'ALARM') {
    form.deviceIds = []
  } else {
    form.hazardPointIds = []
    form.alarmLevels = []
  }
}

function resetForm() {
  Object.assign(form, defaultForm())
  formRef.value?.clearValidate()
}

// 等级/渠道 label 辅助
function levelLabel(lv: string) {
  return { '1': '蓝色', '2': '黄色', '3': '橙色', '4': '红色' }[lv] || lv
}
function levelTagType(lv: string): any {
  return { '1': 'info', '2': 'warning', '3': 'warning', '4': 'danger' }[lv] || ''
}
function channelLabel(ch: string) {
  return { SYSTEM: '系统', SMS: '短信', EMAIL: '邮件' }[ch] || ch
}
function channelTagType(ch: string): any {
  return { SYSTEM: 'success', SMS: 'warning', EMAIL: 'info' }[ch] || ''
}

onMounted(() => {
  loadOptions()
  getList()
})
</script>

<style scoped>
.form-help {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
```

- [ ] **步骤 3：TypeScript 类型检查**

```bash
cd web && npx vue-tsc --noEmit
```

预期：无类型错误。

- [ ] **步骤 4：启动 dev server 手工 E2E 验证**

```bash
cd web && npm run dev
```

浏览器打开 `http://localhost:5173`，登录后访问 `/alarm/notification-setting`，逐项验证：

- [ ] 列表加载，含迁移过来的旧规则（事件类型、等级、隐患点、接收人、渠道列正常显示）
- [ ] 点击"新增" → 弹窗标题"新增通知规则"
- [ ] 选"告警事件" → 显示等级、隐患点；选"设备离线" → 显示设备
- [ ] 隐患点选"全部隐患点"，其他选项 disabled
- [ ] RecipientPicker 三 Tab 切换正常，选 "所有角色" 互斥生效
- [ ] 提交后列表刷新，新规则可见
- [ ] 点击"编辑" → 弹窗标题"编辑通知规则"，原值回填正确
- [ ] 编辑提交后列表更新
- [ ] 切换"启用/禁用"开关，后端 isEnabled 字段变更
- [ ] 删除规则后列表更新

- [ ] **步骤 5：删除 .bak 文件（确认无回归后）**

```bash
rm web/src/views/alarm/NotificationSetting.vue.bak
```

- [ ] **步骤 6：Commit**

```bash
git add web/src/views/alarm/NotificationSetting.vue
git rm -f web/src/views/alarm/NotificationSetting.vue.bak 2>/dev/null || true
git commit -m "feat(alarm/web): NotificationSetting 弹窗+表单+列表全面重构"
```

---

### 任务 14：注册菜单权限

**文件：**
- 创建：`db/upgrade/v2026.06.17.002_dispatch_menu.sql`

- [ ] **步骤 1：编写 SQL**

```sql
-- 通知规则菜单按钮权限
-- 假设 alarm 父菜单 menu_id 在每个环境不同，先用变量查询
SET @alarmParentId = (SELECT menu_id FROM sys_menu
                       WHERE menu_name = '告警管理' AND menu_type = 'M' LIMIT 1);
-- 如果实际父菜单名不同（如 "告警中心"），实现时调整

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, menu_type,
                     perms, icon, create_time)
SELECT '通知规则', @alarmParentId, 30, 'notification-setting',
       'alarm/NotificationSetting', 'C', 'alarm:dispatch:list', 'bell',
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:list' AND menu_type = 'C'
);

SET @ruleMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'alarm:dispatch:list' LIMIT 1);

INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知规则新增', @ruleMenuId, 'F', 'alarm:dispatch:add', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:add');

INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知规则编辑', @ruleMenuId, 'F', 'alarm:dispatch:edit', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:edit');

INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms, create_time)
SELECT '通知规则删除', @ruleMenuId, 'F', 'alarm:dispatch:remove', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'alarm:dispatch:remove');

-- 给 admin 角色绑定（role_id=1）
INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms LIKE 'alarm:dispatch:%'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
```

- [ ] **步骤 2：执行并验证**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2026.06.17.002_dispatch_menu.sql
mysql -uroot -pwodepassword geo_hazard_monitor -e "
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE perms LIKE 'alarm:dispatch:%';
"
```

预期：1 个 C 菜单 + 3 个 F 按钮权限。

- [ ] **步骤 3：用 admin 登录验证菜单可见**

启动后端，admin 登录，左侧菜单出现"通知规则"。访问 `/alarm/notification-setting` 正常。

- [ ] **步骤 4：Commit**

```bash
git add db/upgrade/v2026.06.17.002_dispatch_menu.sql
git commit -m "feat(alarm/db): 通知规则菜单与按钮权限注册"
```

---

## 自检清单（执行前请通读）

### 规格覆盖度（计划 A 部分）

| 规格章节 | 对应任务 | 状态 |
|---------|---------|------|
| §4.1 主表精简 | 任务 1 | ✅ |
| §4.2 三张关联表 | 任务 1 | ✅ |
| §4.5 数据迁移 | 任务 1 | ✅ |
| §5.1 包结构 | 任务 2-10 | ✅ |
| §5.2 Domain 实体 | 任务 3 | ✅ |
| §5.3 DTO | 任务 6 | ✅ |
| §5.4 Service 接口 | 任务 7 | ✅ |
| §5.5 关键 SQL | 任务 5 | ✅（selectByRuleIds 等批量查询） |
| §9.1 NotificationSetting.vue | 任务 13 | ✅ |
| §9.4 RecipientPicker.vue | 任务 12 | ✅ |
| §9.8 列表页改造 | 任务 13 | ✅ |
| §11 菜单与权限（dispatch 部分） | 任务 14 | ✅ |

### 计划 A 不涵盖（计划 B/C 处理）

- 阶段 3：规则匹配（`IAlarmRuleMatcher`）→ 计划 B
- 阶段 4：三渠道策略 → 计划 B
- 阶段 5：AlarmNotifier 改造 → 计划 B
- 阶段 6：通知中心 API + 前端 → 计划 C
- 阶段 8：系统设置（Settings.vue 新增分类）→ 计划 C
- 阶段 9.2-9.4：单测补充 + 集成测试 + 文档 → 计划 C

### 占位符扫描

- ✅ 所有 Mapper 接口和 XML 都有完整代码
- ✅ Service 实现的关键方法有完整实现
- ✅ Controller 完整
- ✅ 前端组件完整
- ⚠️ `groupByRuleId` 在任务 9 步骤 1 的伪代码里返回空 Map，**步骤 3 已明确替换为三个具体类型方法**
- ⚠️ RuoYi 实际包路径（`com.zwei.common.core.domain.entity.*`）需在实现时按项目实际调整 import

### 类型一致性

| 名称 | 定义位置 | 使用位置 |
|------|---------|---------|
| `AlarmEventType` 枚举 | 任务 2 | 任务 3、6、9 |
| `AlarmRecipientType` 枚举 | 任务 2 | 任务 3、9 |
| `AlarmDispatchRule` 实体 | 任务 3 | 任务 4、5、9 |
| `AlarmDispatchRuleHazardPoint` | 任务 3 | 任务 4、5、9 |
| `AlarmDispatchRuleDevice` | 任务 3 | 任务 4、5、9 |
| `AlarmDispatchRuleRecipient` | 任务 3 | 任务 4、5、9 |
| `AlarmDispatchRuleCreateRequest` | 任务 6 | 任务 7、9、10、11 |
| `AlarmDispatchRuleDetailVO` | 任务 6 | 任务 7、9、10、11 |
| `AlarmDispatchRuleItemVO` | 任务 6 | 任务 7、9、10、11、13 |
| `IAlarmDispatchRuleService` | 任务 7 | 任务 9、10 |

### 已知风险（实现时注意）

1. **`selectRoleAll()`/`selectDeptList()`/`selectUserList()` 的 RuoYi 标准签名**：实现时核对 `ISysRoleService`/`ISysDeptService`/`ISysUserService` 实际方法名，调整 `selectRecipientOptions` 实现
2. **PageHelper vs MyBatis-Plus 分页**：Controller 用了 `startPage()` + `getDataTable()`，需确认项目实际用 PageHelper 还是 MP。如用 MP，改为 `IPage<>` 风格
3. **部门树构造**：`RecipientPicker` 的 `deptTreeData` 需要把扁平结构按 `parent_id` 拼装，简化版只展示扁平列表（实现时按 ISysDeptService 实际返回调整）
4. **类型导入路径**：前端 `request.get<{rows:...}>` 实际返回结构需核对 `web/src/utils/request.ts`，可能不是直接 data
5. **`getHazardPointPage` 和 `getDeviceList` 的实际签名**：核对 `web/src/api/hazardPoint.ts` 和 `device.ts` 的实际函数名

---

## 执行交接

计划 A 已完成并保存到 `docs/superpowers/plans/2026-06-17-alarm-dispatch-rule-iteration-plan-a.md`。

**接下来两种选择：**

**1. 立即开始执行计划 A** - 两种方式：
   - **子代理驱动（推荐）** - 使用 superpowers:subagent-driven-development，每个任务调度一个新子代理，任务间审查
   - **内联执行** - 使用 superpowers:executing-plans，当前会话批量执行，设检查点

**2. 继续写计划 B、C** - 先把 3 个计划全部写完，再统一执行

选哪种？
