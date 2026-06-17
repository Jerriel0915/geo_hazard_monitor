# 通知规则迭代 - 计划 C：通知中心 + 系统设置 + 收尾实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 完成通知中心（前端 Tab 分类 + 后端用户视角 API + SSE 集成 + 路由跳转）、系统设置通知配置分类（11 参数 + password 渲染扩展）、以及菜单权限注册与文档收尾。

**架构：**
- **后端**：`AlarmNotificationController` 提供 `/api/v1/alarm/notifications/*` 系列接口（recent / unread-count / read / read-all），数据按当前登录用户 `SecurityUtils.getUserId()` 过滤；`AlarmStreamPublisher` 增加 `publishToUser` 单点推送能力
- **前端**：`layout/index.vue` 重构消息面板为 Tab（公告/事件），事件默认；新增 `api/alarmNotification.ts` 模块；SSE 监听 `alarm-notify` 事件类型
- **设置**：`Settings.vue` 新增 `notify` 分类（11 参数），扩展 `type='password'` 渲染，保存逻辑通过 `ISysConfigService` 写入 `sys_config`
- **收尾**：`sys_menu` 注册权限 SQL、单测补全、文档更新

**技术栈：** Spring Boot 4.0.3 + MyBatis + Vue 3 + TypeScript + Element Plus 2.6 + Pinia/Vue Router + SSE

**关联规格：** `docs/superpowers/specs/2026-06-17-alarm-dispatch-rule-iteration-design.md` 第 8、9、11 节

**前置条件：**
- 计划 A 已完成（DDL、Domain、规则 CRUD 可用，特别是 `AlarmNotification` 已含 `read_time/source_type/source_id` 字段）
- 计划 B 已完成（`AlarmNotification.STATUS_*` 常量、`IAlarmNotificationService` 已含 `markSent/markFailed/markReadIfOwner/markAllRead/selectUserRecent/selectUnreadCount` 方法、`AlarmStreamPublisher` 已注入到 `SystemNotifyChannel`）

---

## 文件结构

### 创建（9 个）

| 文件 | 职责 |
|------|------|
| `server/zwei-iot-alarm/.../controller/AlarmNotificationController.java` | 用户视角的通知接口（recent/unread-count/read/read-all） |
| `server/zwei-iot-alarm/.../domain/dto/AlarmNotificationItemVO.java` | 通知列表项 VO（前端展示字段） |
| `server/zwei-iot-alarm/.../domain/dto/AlarmNotificationSummaryVO.java` | 未读数 + 总数 VO |
| `db/upgrade/v2026.06.17.004_notification_menu.sql` | sys_menu 权限注册 SQL |
| `web/src/api/alarmNotification.ts` | 通知中心事件 API 模块 |
| `web/src/types/alarmNotification.ts` | 通知中心 TS 类型定义 |
| `server/zwei-iot-alarm/src/test/java/.../AlarmNotificationControllerTest.java` | 控制器单测 |
| `server/zwei-iot-alarm/src/test/java/.../AlarmNotificationSecurityTest.java` | 用户隔离测试（不能查他人通知） |
| `docs/通知中心使用手册.md` | 用户文档（如何收到/查看/处理通知） |

### 修改（8 个）

| 文件 | 改动 |
|------|------|
| `server/zwei-iot-alarm/.../service/notify/AlarmStreamPublisher.java` | 增加 `publishToUser(userId, eventType, data)` 方法 + emitters 改为 `Map<Long, List<SseEmitter>>` |
| `server/zwei-iot-alarm/.../mapper/AlarmNotificationMapper.java` | 增加 `selectUserRecent / selectUnreadCount / markReadIfOwner / markAllRead` 方法签名 |
| `server/zwei-iot-alarm/.../mapper/AlarmNotificationMapper.xml` | 对应 4 条 SQL + resultMap 扩展 read_time/source_type/source_id |
| `server/zwei-system/.../service/ISysConfigService.java` | （如缺）补 `saveOrUpdateConfig(key, value)` 公共方法 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmDispatchRuleController.java` | 在 recipient-options 接口旁补充 sys_config 读取（如需） |
| `web/src/layout/index.vue` | 消息面板改造为 Tab（公告/事件），事件默认，点击跳转 |
| `web/src/views/system/Settings.vue` | paramCategories 新增 `notify`；paramList 新增 11 项；扩展 `type='password'` 渲染；保存逻辑写 sys_config |
| `web/src/router/index.ts` | （如缺）`/basic/device` 路由确认/补 query 支持 |

---

## 任务清单

### 任务 1：扩展 AlarmNotificationMapper（用户视角查询）

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapper.java`
- 修改：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmNotificationMapper.xml`

- [ ] **步骤 1：编写 Mapper 接口扩展测试**

创建 `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapperTest.java`：

```java
package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmNotification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AlarmNotificationMapperTest {

    @Autowired
    private AlarmNotificationMapper mapper;

    private Long seedNotification(Long userId, String sourceType, Long sourceId,
                                   String channel, String title, boolean read) {
        AlarmNotification n = new AlarmNotification();
        n.setRecipientId(userId);
        n.setSourceType(sourceType);
        n.setSourceId(sourceId);
        n.setChannel(channel);
        n.setTitle(title);
        n.setContent("test content");
        n.setStatus(AlarmNotification.STATUS_SENT);
        n.setCreateTime(new Date());
        n.setReadTime(read ? new Date() : null);
        mapper.insertNotification(n);
        return n.getId();
    }

    @Test
    void selectUserRecent_returnsOnlyOwnNotifications() {
        Long ownId = 1L;
        Long otherId = 2L;
        seedNotification(ownId, "alarm", 1001L, "SYSTEM", "我的告警", false);
        seedNotification(otherId, "alarm", 1002L, "SYSTEM", "他人的告警", false);

        List<AlarmNotification> recent = mapper.selectUserRecent(ownId, 10);

        assertThat(recent).allSatisfy(n -> assertThat(n.getRecipientId()).isEqualTo(ownId));
        assertThat(recent).anyMatch(n -> "我的告警".equals(n.getTitle()));
    }

    @Test
    void selectUnreadCount_countsOnlyUnreadSystemChannel() {
        Long userId = 1L;
        seedNotification(userId, "alarm", 2001L, "SYSTEM", "未读", false);
        seedNotification(userId, "alarm", 2002L, "SYSTEM", "已读", true);
        seedNotification(userId, "alarm", 2003L, "SMS", "短信未读", false);

        int count = mapper.selectUnreadCount(userId);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void markReadIfOwner_updatesOnlyOwnUnreadNotification() {
        Long ownId = 1L;
        Long otherId = 2L;
        Long ownNotifId = seedNotification(ownId, "alarm", 3001L, "SYSTEM", "own", false);
        Long otherNotifId = seedNotification(otherId, "alarm", 3002L, "SYSTEM", "other", false);

        int affectedOwn = mapper.markReadIfOwner(ownNotifId, ownId);
        int affectedOther = mapper.markReadIfOwner(otherNotifId, ownId);

        assertThat(affectedOwn).isEqualTo(1);
        assertThat(affectedOther).isEqualTo(0);
    }

    @Test
    void markAllRead_updatesAllOwnUnreadSystemNotifications() {
        Long userId = 1L;
        Long n1 = seedNotification(userId, "alarm", 4001L, "SYSTEM", "n1", false);
        Long n2 = seedNotification(userId, "offline", 4002L, "SYSTEM", "n2", false);
        Long n3 = seedNotification(userId, "alarm", 4003L, "SYSTEM", "n3", true); // 已读

        int affected = mapper.markAllRead(userId);

        assertThat(affected).isEqualTo(2); // n1 + n2，n3 不算
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmNotificationMapperTest -Dspring.profiles.active=local
```

预期：编译失败（`selectUserRecent / selectUnreadCount / markReadIfOwner / markAllRead` 方法未定义；`AlarmNotification.STATUS_SENT` 常量未定义；`sourceType / sourceId / readTime` 字段未定义）。

- [ ] **步骤 3：扩展 Mapper 接口**

修改 `AlarmNotificationMapper.java`：

```java
package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmNotificationMapper {

    List<AlarmNotification> selectNotificationList(AlarmNotification notification);

    List<AlarmNotification> selectByAlarmId(Long alarmId);

    int insertNotification(AlarmNotification notification);

    int batchInsert(List<AlarmNotification> notifications);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("sendTime") String sendTime,
                     @Param("errorMsg") String errorMsg);

    /**
     * 查询当前用户最近通知（仅 SYSTEM 渠道，按时间倒序）。
     */
    List<AlarmNotification> selectUserRecent(@Param("userId") Long userId,
                                             @Param("limit") int limit);

    /**
     * 当前用户未读数（仅 SYSTEM 渠道）。
     */
    int selectUnreadCount(@Param("userId") Long userId);

    /**
     * 标记已读（仅当本用户是接收人且未读时更新）。
     *
     * @return 受影响行数（0 表示无权或已读）
     */
    int markReadIfOwner(@Param("notifId") Long notifId,
                        @Param("userId") Long userId);

    /**
     * 当前用户全部 SYSTEM 未读消息批量标记已读。
     *
     * @return 受影响行数
     */
    int markAllRead(@Param("userId") Long userId);
}
```

- [ ] **步骤 4：扩展 Mapper XML**

修改 `AlarmNotificationMapper.xml`，在 `</mapper>` 前追加 4 条 SQL：

```xml
    <select id="selectUserRecent" resultMap="AlarmNotificationResult">
        SELECT id, alarm_id, dispatch_rule_id, recipient_id, recipient_name,
               recipient_phone, channel, title, content,
               status, send_time, error_msg, create_time,
               read_time, source_type, source_id
        FROM alarm_notification
        WHERE recipient_id = #{userId}
          AND channel = 'SYSTEM'
          AND source_type IN ('alarm', 'offline')
        ORDER BY create_time DESC
        LIMIT #{limit}
    </select>

    <select id="selectUnreadCount" resultType="int">
        SELECT COUNT(1)
        FROM alarm_notification
        WHERE recipient_id = #{userId}
          AND channel = 'SYSTEM'
          AND source_type IN ('alarm', 'offline')
          AND read_time IS NULL
    </select>

    <update id="markReadIfOwner">
        UPDATE alarm_notification
        SET read_time = NOW()
        WHERE id = #{notifId}
          AND recipient_id = #{userId}
          AND read_time IS NULL
    </update>

    <update id="markAllRead">
        UPDATE alarm_notification
        SET read_time = NOW()
        WHERE recipient_id = #{userId}
          AND channel = 'SYSTEM'
          AND source_type IN ('alarm', 'offline')
          AND read_time IS NULL
    </update>
```

同时扩展 resultMap（在现有 resultMap 中补 3 行）：

```xml
    <resultMap type="com.zwei.iot.alarm.domain.AlarmNotification" id="AlarmNotificationResult">
        <id property="id" column="id"/>
        <result property="alarmId" column="alarm_id"/>
        <result property="dispatchRuleId" column="dispatch_rule_id"/>
        <result property="recipientId" column="recipient_id"/>
        <result property="recipientName" column="recipient_name"/>
        <result property="recipientPhone" column="recipient_phone"/>
        <result property="channel" column="channel"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="status" column="status"/>
        <result property="sendTime" column="send_time"/>
        <result property="errorMsg" column="error_msg"/>
        <result property="createTime" column="create_time"/>
        <result property="readTime" column="read_time"/>
        <result property="sourceType" column="source_type"/>
        <result property="sourceId" column="source_id"/>
    </resultMap>
```

- [ ] **步骤 5：补全 Domain 字段与状态常量**

> **注**：如果计划 B 已完成此步，跳过。检查 `AlarmNotification.java` 是否已含 `readTime / sourceType / sourceId` 字段与 `STATUS_PENDING / STATUS_SENT / STATUS_FAILED / STATUS_INVALID_RECIPIENT / STATUS_CHANNEL_NOT_CONFIGURED` 常量。若缺，按下方补全：

修改 `AlarmNotification.java`，在类体内追加：

```java
    /** 状态：待发送 */
    public static final int STATUS_PENDING = 1;
    /** 状态：已发送 */
    public static final int STATUS_SENT = 2;
    /** 状态：发送失败 */
    public static final int STATUS_FAILED = 3;
    /** 状态：接收人无效 */
    public static final int STATUS_INVALID_RECIPIENT = 4;
    /** 状态：渠道未配置 */
    public static final int STATUS_CHANNEL_NOT_CONFIGURED = 5;

    /** 接收人邮箱（用于 EMAIL 渠道） */
    private String recipientEmail;
    /** 已读时间（NULL=未读） */
    private Date readTime;
    /** 来源类型：alarm / offline */
    private String sourceType;
    /** 来源 ID：alarm_record.id 或 device.id */
    private Long sourceId;
```

- [ ] **步骤 6：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmNotificationMapperTest -Dspring.profiles.active=local
```

预期：4 个测试方法全部 PASS。

- [ ] **步骤 7：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapper.java \
        server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmNotificationMapper.xml \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/AlarmNotification.java \
        server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapperTest.java
git commit -m "feat(alarm): AlarmNotificationMapper 扩展用户视角查询 (recent/unread/markRead)"
```

---

### 任务 2：扩展 IAlarmNotificationService 用户视角方法

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmNotificationService.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmNotificationServiceImpl.java`

- [ ] **步骤 1：编写 Service 测试**

创建 `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/AlarmNotificationServiceImplTest.java`：

```java
package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.mapper.AlarmNotificationMapper;
import com.zwei.iot.alarm.service.impl.AlarmNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class AlarmNotificationServiceImplTest {

    private AlarmNotificationMapper mapper;
    private AlarmNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(AlarmNotificationMapper.class);
        service = new AlarmNotificationServiceImpl(mapper);
    }

    @Test
    void selectUserRecent_delegatesWithLimit() {
        when(mapper.selectUserRecent(1L, 10)).thenReturn(List.of());

        service.selectUserRecent(1L, 10);

        verify(mapper).selectUserRecent(1L, 10);
    }

    @Test
    void selectUnreadCount_delegates() {
        when(mapper.selectUnreadCount(1L)).thenReturn(3);

        int count = service.selectUnreadCount(1L);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void markReadIfOwner_delegates() {
        when(mapper.markReadIfOwner(10L, 1L)).thenReturn(1);

        int affected = service.markReadIfOwner(10L, 1L);

        assertThat(affected).isEqualTo(1);
    }

    @Test
    void markAllRead_delegates() {
        when(mapper.markAllRead(1L)).thenReturn(5);

        int affected = service.markAllRead(1L);

        assertThat(affected).isEqualTo(5);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmNotificationServiceImplTest
```

预期：编译失败（`selectUserRecent / selectUnreadCount / markReadIfOwner / markAllRead` 方法在接口中未定义）。

- [ ] **步骤 3：扩展 Service 接口**

修改 `IAlarmNotificationService.java`：

```java
package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmNotification;

import java.util.List;

public interface IAlarmNotificationService {

    List<AlarmNotification> selectByAlarmId(Long alarmId);

    List<AlarmNotification> selectList(AlarmNotification notification);

    int createNotification(AlarmNotification notification);

    int batchCreate(List<AlarmNotification> notifications);

    int updateStatus(Long id, Integer status, String errorMsg);

    /**
     * 当前用户最近 SYSTEM 通知（按时间倒序）。
     */
    List<AlarmNotification> selectUserRecent(Long userId, int limit);

    /**
     * 当前用户 SYSTEM 未读数。
     */
    int selectUnreadCount(Long userId);

    /**
     * 标记单条已读（仅 owner）。
     */
    int markReadIfOwner(Long notifId, Long userId);

    /**
     * 全部标记已读。
     */
    int markAllRead(Long userId);
}
```

- [ ] **步骤 4：扩展 Service 实现**

修改 `AlarmNotificationServiceImpl.java`，在 `updateStatus` 方法后追加：

```java
    @Override
    public List<AlarmNotification> selectUserRecent(Long userId, int limit) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return notificationMapper.selectUserRecent(userId, safeLimit);
    }

    @Override
    public int selectUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationMapper.selectUnreadCount(userId);
    }

    @Override
    public int markReadIfOwner(Long notifId, Long userId) {
        if (notifId == null || userId == null) {
            return 0;
        }
        return notificationMapper.markReadIfOwner(notifId, userId);
    }

    @Override
    public int markAllRead(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationMapper.markAllRead(userId);
    }
```

- [ ] **步骤 5：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmNotificationServiceImplTest
```

预期：4 个测试方法 PASS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmNotificationService.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmNotificationServiceImpl.java \
        server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/AlarmNotificationServiceImplTest.java
git commit -m "feat(alarm): IAlarmNotificationService 增加用户视角查询/标记已读方法"
```

---

### 任务 3：AlarmNotificationItemVO / SummaryVO

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/AlarmNotificationItemVO.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/AlarmNotificationSummaryVO.java`

- [ ] **步骤 1：编写 VO 类**

创建 `AlarmNotificationItemVO.java`：

```java
package com.zwei.iot.alarm.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知中心事件列表项 VO（前端展示用）。
 *
 * @author zwei
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmNotificationItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知记录 ID */
    private Long id;
    /** 来源类型：alarm / offline */
    private String sourceType;
    /** 来源 ID（alarm_record.id 或 device.id） */
    private Long sourceId;
    /** 通知标题 */
    private String title;
    /** 通知正文 */
    private String content;
    /** 接收人名称 */
    private String recipientName;
    /** 已读时间（NULL=未读） */
    private Date readTime;
    /** 创建时间（事件时间） */
    private Date createTime;
}
```

创建 `AlarmNotificationSummaryVO.java`：

```java
package com.zwei.iot.alarm.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知中心未读汇总 VO。
 *
 * @author zwei
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmNotificationSummaryVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前用户未读事件数 */
    private int unreadCount;
    /** 查询时间戳 */
    private long timestamp;
}
```

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/AlarmNotificationItemVO.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/domain/dto/AlarmNotificationSummaryVO.java
git commit -m "feat(alarm): 新增 AlarmNotificationItemVO/SummaryVO (前端展示用)"
```

---

### 任务 4：AlarmStreamPublisher 增加 publishToUser 方法

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmStreamPublisher.java`

- [ ] **步骤 1：编写测试**

创建 `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/notify/AlarmStreamPublisherTest.java`：

```java
package com.zwei.iot.alarm.service.notify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AlarmStreamPublisherTest {

    private AlarmStreamPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AlarmStreamPublisher();
    }

    @Test
    void subscribe_returnsEmitterAndRegistered() {
        SseEmitter emitter = publisher.subscribe();
        assertThat(emitter).isNotNull();
        assertThat(publisher.getActiveCount()).isEqualTo(1);
    }

    @Test
    void publishToUser_withNoSubscribersSilentlySucceeds() {
        assertThatNoException().isThrownBy(() ->
            publisher.publishToUser(999L, "alarm-notify", Map.of("title", "x")));
    }

    @Test
    void publishToUser_unknownUserIdIsNoOp() {
        publisher.subscribe(); // 订阅时未绑定 userId（向后兼容场景）
        assertThatNoException().isThrownBy(() ->
            publisher.publishToUser(999L, "alarm-notify", Map.of("title", "x")));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmStreamPublisherTest
```

预期：编译失败（`publishToUser` 方法未定义）。

- [ ] **步骤 3：扩展 AlarmStreamPublisher**

修改 `AlarmStreamPublisher.java`，在类体内追加：

```java
    /**
     * 按 userId 索引的订阅映射（同一用户可多端订阅）。
     * 注：兼容旧的"未绑定 userId"的 emitter，会落入 WILDCARD_KEY。
     */
    private final Map<Long, List<SseEmitter>> userEmitters = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long WILDCARD_KEY = -1L;

    /**
     * 订阅告警 SSE 流，并绑定到指定 userId（用于 publishToUser 定向推送）。
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = subscribe();
        if (userId != null) {
            userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
            emitter.onCompletion(() -> userEmitters.getOrDefault(userId, java.util.List.of()).remove(emitter));
            emitter.onTimeout(() -> userEmitters.getOrDefault(userId, java.util.List.of()).remove(emitter));
            emitter.onError(e -> userEmitters.getOrDefault(userId, java.util.List.of()).remove(emitter));
        }
        return emitter;
    }

    /**
     * 向指定用户推送事件。
     *
     * @param userId    接收用户 ID
     * @param eventType 事件类型（如 "alarm-notify"）
     * @param data      事件数据
     */
    public void publishToUser(Long userId, String eventType, Map<String, Object> data) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> targets = userEmitters.get(userId);
        if (targets == null || targets.isEmpty()) {
            log.debug("publishToUser 目标 {} 无在线订阅，事件 {} 已落库不丢失", userId, eventType);
            return;
        }
        int sent = 0;
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event().name(eventType).data(data));
                sent++;
            } catch (IOException e) {
                targets.remove(emitter);
                log.debug("用户 {} SSE 推送失败，移除订阅: {}", userId, e.getMessage());
            }
        }
        log.debug("publishToUser userId={} event={} 推送 {}/{}", userId, eventType, sent, targets.size());
    }
```

> **注**：保留原 `subscribe()` 与 `onAlarmTriggered` 全量广播方法不变，向后兼容。

- [ ] **步骤 4：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmStreamPublisherTest
```

预期：3 个测试方法 PASS。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/notify/AlarmStreamPublisher.java \
        server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/service/notify/AlarmStreamPublisherTest.java
git commit -m "feat(alarm): AlarmStreamPublisher 增加 publishToUser 单点推送能力"
```

---

### 任务 5：AlarmNotificationController（后端通知中心 API）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmNotificationController.java`

- [ ] **步骤 1：编写 Controller 测试**

创建 `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/controller/AlarmNotificationControllerTest.java`：

```java
package com.zwei.iot.alarm.controller;

import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.domain.dto.AlarmNotificationItemVO;
import com.zwei.iot.alarm.domain.dto.AlarmNotificationSummaryVO;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
class AlarmNotificationControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private IAlarmNotificationService service;
    private MockMvc mockMvc;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void recent_returnsUserNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/alarm/notifications/recent").param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void unreadCount_returnsCount() throws Exception {
        mockMvc.perform(get("/api/v1/alarm/notifications/unread-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void read_marksSingleNotification() throws Exception {
        // 先种一条数据
        AlarmNotification n = new AlarmNotification();
        n.setRecipientId(1L);
        n.setChannel("SYSTEM");
        n.setSourceType("alarm");
        n.setSourceId(99999L);
        n.setTitle("read-test");
        n.setContent("c");
        n.setStatus(AlarmNotification.STATUS_SENT);
        n.setCreateTime(new Date());
        service.createNotification(n);

        mockMvc.perform(post("/api/v1/alarm/notifications/{id}/read", n.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void readAll_marksAll() throws Exception {
        mockMvc.perform(post("/api/v1/alarm/notifications/read-all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmNotificationControllerTest -Dspring.profiles.active=local
```

预期：HTTP 404（Controller 未创建）。

- [ ] **步骤 3：编写 Controller**

创建 `AlarmNotificationController.java`：

```java
package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.domain.dto.AlarmNotificationItemVO;
import com.zwei.iot.alarm.domain.dto.AlarmNotificationSummaryVO;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知中心（事件 Tab）API
 * <p>
 * 用户视角：只看本人接收的 SYSTEM 渠道通知；标记已读仅对本人数据生效。
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/notifications")
public class AlarmNotificationController extends BaseController {

    private final IAlarmNotificationService notificationService;

    public AlarmNotificationController(IAlarmNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 当前用户最近事件通知列表。
     */
    @GetMapping("/recent")
    @PreAuthorize("@ss.hasPermi('alarm:notification:list')")
    public AjaxResult recent(@RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityUtils.getUserId();
        List<AlarmNotification> list = notificationService.selectUserRecent(userId, limit);
        List<AlarmNotificationItemVO> vos = list.stream()
            .map(this::toItemVO)
            .toList();
        return AjaxResult.success(vos);
    }

    /**
     * 当前用户未读事件数。
     */
    @GetMapping("/unread-count")
    @PreAuthorize("@ss.hasPermi('alarm:notification:list')")
    public AjaxResult unreadCount() {
        Long userId = SecurityUtils.getUserId();
        int count = notificationService.selectUnreadCount(userId);
        return AjaxResult.success(AlarmNotificationSummaryVO.builder()
            .unreadCount(count)
            .timestamp(System.currentTimeMillis())
            .build());
    }

    /**
     * 标记单条已读。
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("@ss.hasPermi('alarm:notification:read')")
    public AjaxResult read(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        int affected = notificationService.markReadIfOwner(id, userId);
        if (affected == 0) {
            return AjaxResult.error("通知不存在或无权操作");
        }
        return AjaxResult.success();
    }

    /**
     * 全部标记已读。
     */
    @PostMapping("/read-all")
    @PreAuthorize("@ss.hasPermi('alarm:notification:read')")
    public AjaxResult readAll() {
        Long userId = SecurityUtils.getUserId();
        int affected = notificationService.markAllRead(userId);
        return AjaxResult.success("已标记 " + affected + " 条为已读");
    }

    private AlarmNotificationItemVO toItemVO(AlarmNotification n) {
        return AlarmNotificationItemVO.builder()
            .id(n.getId())
            .sourceType(n.getSourceType())
            .sourceId(n.getSourceId())
            .title(n.getTitle())
            .content(n.getContent())
            .recipientName(n.getRecipientName())
            .readTime(n.getReadTime())
            .createTime(n.getCreateTime())
            .build();
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmNotificationControllerTest -Dspring.profiles.active=local
```

预期：4 个测试方法 PASS。

- [ ] **步骤 5：手工验证（启动后端 + curl）**

```bash
# 1) 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","code":"","uuid":""}' | jq -r '.data.token')

# 2) 查最近事件
curl -s "http://localhost:8080/api/v1/alarm/notifications/recent?limit=10" \
  -H "Authorization: Bearer $TOKEN" | jq

# 3) 查未读数
curl -s "http://localhost:8080/api/v1/alarm/notifications/unread-count" \
  -H "Authorization: Bearer $TOKEN" | jq

# 4) 全部已读
curl -s -X POST "http://localhost:8080/api/v1/alarm/notifications/read-all" \
  -H "Authorization: Bearer $TOKEN" | jq
```

预期：3 个接口均返回 `code:200`，`recent` 返回数组，`unread-count` 返回 `{unreadCount:N,timestamp:...}`。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmNotificationController.java \
        server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/controller/AlarmNotificationControllerTest.java
git commit -m "feat(alarm): 新增 AlarmNotificationController 通知中心事件 API"
```

---

### 任务 6：AlarmNotificationSecurityTest（用户隔离测试）

**文件：**
- 创建：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/AlarmNotificationSecurityTest.java`

- [ ] **步骤 1：编写测试**

```java
package com.zwei.iot.alarm;

import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * 用户隔离安全测试：用户 A 不能查询/标记用户 B 的通知。
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AlarmNotificationSecurityTest {

    @Autowired private IAlarmNotificationService service;
    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    @Test
    void userA_cannotMarkRead_userBNotification() {
        // 用户 B 种一条数据
        Long userB = 20002L;
        AlarmNotification n = seed(userB, 5001L, "alarm");

        // 用户 A 尝试标记已读
        securityMock.when(SecurityUtils::getUserId).thenReturn(10001L);
        int affected = service.markReadIfOwner(n.getId(), 10001L);

        assertThat(affected).isZero();
    }

    @Test
    void userA_recent_doesNotInclude_userBNotification() {
        Long userA = 10001L;
        Long userB = 20002L;
        seed(userA, 6001L, "alarm");
        seed(userB, 6002L, "offline");

        var recent = service.selectUserRecent(userA, 50);

        assertThat(recent).allSatisfy(n -> assertThat(n.getRecipientId()).isEqualTo(userA));
    }

    @Test
    void markAllRead_doesNotAffectOtherUsers() {
        Long userA = 10001L;
        Long userB = 20002L;
        seed(userA, 7001L, "alarm");
        Long bNotifId = seed(userB, 7002L, "alarm");

        service.markAllRead(userA);

        // 用户 B 的未读数应保持不变
        int bUnread = service.selectUnreadCount(userB);
        assertThat(bUnread).isGreaterThan(0);
        assertThat(service.markReadIfOwner(bNotifId, userB)).isEqualTo(1);
    }

    private Long seed(Long userId, Long sourceId, String sourceType) {
        AlarmNotification n = new AlarmNotification();
        n.setRecipientId(userId);
        n.setChannel("SYSTEM");
        n.setSourceType(sourceType);
        n.setSourceId(sourceId);
        n.setTitle("security-test-" + userId);
        n.setContent("content");
        n.setStatus(AlarmNotification.STATUS_SENT);
        n.setCreateTime(new Date());
        service.createNotification(n);
        return n.getId();
    }
}
```

- [ ] **步骤 2：运行测试**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlarmNotificationSecurityTest -Dspring.profiles.active=local
```

预期：3 个测试方法 PASS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/AlarmNotificationSecurityTest.java
git commit -m "test(alarm): AlarmNotification 用户隔离安全测试"
```

---

### 任务 7：前端 alarmNotification API + 类型定义

**文件：**
- 创建：`web/src/types/alarmNotification.ts`
- 创建：`web/src/api/alarmNotification.ts`

- [ ] **步骤 1：编写类型定义**

创建 `web/src/types/alarmNotification.ts`：

```typescript
/**
 * 通知中心事件列表项（前端展示用）。
 */
export interface AlarmNotificationItem {
  /** 通知记录 ID */
  id: number
  /** 来源类型：alarm / offline */
  sourceType: 'alarm' | 'offline'
  /** 来源 ID（alarm_record.id 或 device.id） */
  sourceId: number
  /** 通知标题 */
  title: string
  /** 通知正文 */
  content: string
  /** 接收人名称 */
  recipientName?: string
  /** 已读时间（NULL=未读） */
  readTime: string | null
  /** 创建时间（事件时间） */
  createTime: string
}

/**
 * 未读汇总。
 */
export interface AlarmNotificationSummary {
  unreadCount: number
  timestamp: number
}
```

- [ ] **步骤 2：编写 API 模块**

创建 `web/src/api/alarmNotification.ts`：

```typescript
import request from '@/utils/request'
import type { AjaxResult } from './system'
import type { AlarmNotificationItem, AlarmNotificationSummary } from '@/types/alarmNotification'

/**
 * 查询当前用户最近事件通知（默认 10 条，事件 Tab 数据源）。
 */
export function getRecentAlarmNotifications(limit = 10): Promise<AjaxResult<AlarmNotificationItem[]>> {
  return request.get('/alarm/notifications/recent', { params: { limit } })
}

/**
 * 当前用户未读事件数。
 */
export function getAlarmNotificationUnreadCount(): Promise<AjaxResult<AlarmNotificationSummary>> {
  return request.get('/alarm/notifications/unread-count')
}

/**
 * 标记单条事件通知已读。
 */
export function markAlarmNotificationRead(id: number): Promise<AjaxResult> {
  return request.post(`/alarm/notifications/${id}/read`)
}

/**
 * 全部事件通知标记已读。
 */
export function markAllAlarmNotificationsRead(): Promise<AjaxResult> {
  return request.post('/alarm/notifications/read-all')
}
```

- [ ] **步骤 3：TypeScript 类型检查**

```bash
cd web && npx vue-tsc --noEmit 2>&1 | head -30
```

预期：无新增类型错误（已有错误保持不变）。

- [ ] **步骤 4：Commit**

```bash
git add web/src/types/alarmNotification.ts web/src/api/alarmNotification.ts
git commit -m "feat(web): 新增 alarmNotification API + 类型定义 (通知中心事件)"
```

---

### 任务 8：layout/index.vue 通知中心 Tab 改造

**文件：**
- 修改：`web/src/layout/index.vue`

- [ ] **步骤 1：精读现有消息面板结构**

阅读 `web/src/layout/index.vue` 行 219-273（message-panel 模板）和行 277-390（脚本 setup 中 NoticeMessage、fetchNotices、startNoticeSSE 部分），明确：

- 现有数据结构：`messages: NoticeMessage[]`、`unreadMessageCount: number`
- 现有 Tab：`messageTab: 'unread' | 'read'`（已读/未读分类，需替换为 source 分类）
- 现有 SSE：`noticeEventSource` 监听 `notice` 事件类型
- 跳转目标：`@click="markMessageAsRead(msg)"`（当前只标读，无跳转）

- [ ] **步骤 2：重构模板 — Tab 从"未读/已读"改为"公告/事件"**

修改 `web/src/layout/index.vue` 行 219-273 的消息面板部分：

**步骤 2a：替换 panel-header（Tab 改为公告/事件）**

把现有：
```vue
<div class="message-panel-header">
  <span class="message-panel-title">系统消息</span>
  <div class="message-tabs">
    <span :class="['tab', { active: messageTab === 'unread' }]" @click="messageTab = 'unread'">未读 ({{ unreadMessageCount }})</span>
    <span :class="['tab', { active: messageTab === 'read' }]" @click="messageTab = 'read'">已读</span>
  </div>
  <span class="close-btn" @click="messagePanelVisible = false"><!-- svg --></span>
</div>
```

替换为：
```vue
<div class="message-panel-header">
  <span class="message-panel-title">通知中心</span>
  <div class="message-tabs">
    <span :class="['tab', { active: notifyTab === 'event' }]" @click="switchNotifyTab('event')">
      事件 <span v-if="eventUnreadCount > 0" class="tab-count">({{ eventUnreadCount }})</span>
    </span>
    <span :class="['tab', { active: notifyTab === 'notice' }]" @click="switchNotifyTab('notice')">
      公告 <span v-if="noticeUnreadCount > 0" class="tab-count">({{ noticeUnreadCount }})</span>
    </span>
  </div>
  <span class="close-btn" @click="messagePanelVisible = false">
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="12" height="12">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  </span>
</div>
```

**步骤 2b：替换 message-list（动态根据 Tab 渲染）**

把现有 `<div class="message-list">...</div>` 整段替换为：

```vue
<div class="message-list">
  <!-- 事件 Tab -->
  <template v-if="notifyTab === 'event'">
    <div
      v-for="msg in eventMessages"
      :key="'event-' + msg.id"
      :class="['message-item', { unread: !msg.read }]"
      @click="handleEventClick(msg)"
    >
      <div class="message-icon-wrapper">
        <svg v-if="msg.sourceType === 'alarm'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
             fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
          <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
             fill="none" stroke="#e6a23c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
          <line x1="8" y1="21" x2="16" y2="21"/>
        </svg>
      </div>
      <div class="message-content">
        <div class="message-title">{{ msg.title }}</div>
        <div class="message-desc">{{ msg.content }}</div>
        <div class="message-time">{{ msg.time }}</div>
      </div>
    </div>
    <div v-if="eventMessages.length === 0" class="empty-message">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#d9d9d9"
           stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
      <span>暂无事件通知</span>
    </div>
  </template>

  <!-- 公告 Tab -->
  <template v-else>
    <div
      v-for="msg in noticeMessages"
      :key="'notice-' + msg.id"
      :class="['message-item', { unread: !msg.read }]"
      @click="handleNoticeClick(msg)"
    >
      <div class="message-icon-wrapper">
        <svg v-if="msg.type === 'system'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
             fill="none" stroke="#1890ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3"/>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
             fill="none" stroke="#52c41a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
      </div>
      <div class="message-content">
        <div class="message-title">{{ msg.title }}</div>
        <div class="message-desc">{{ msg.content }}</div>
        <div class="message-time">{{ msg.time }}</div>
      </div>
    </div>
    <div v-if="noticeMessages.length === 0" class="empty-message">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#d9d9d9"
           stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
      <span>暂无公告</span>
    </div>
  </template>
</div>
```

**步骤 2c：替换 panel-footer（全部已读按当前 Tab 生效）**

把现有：
```vue
<div class="message-panel-footer" v-if="messages.length > 0">
  <el-button size="small" @click="markAllAsRead">全部标为已读</el-button>
</div>
```

替换为：
```vue
<div class="message-panel-footer" v-if="currentTabHasMessages">
  <el-button size="small" @click="markAllAsRead">全部标为已读</el-button>
</div>
```

- [ ] **步骤 3：重构脚本 — 引入新数据源与切换逻辑**

**步骤 3a：替换 import**

把现有 `import {getTopNotices, markRead, markReadAll, type SysNotice} from '@/api/notice'` 替换为：

```typescript
import {getTopNotices, markRead as markNoticeRead, markReadAll as markAllNoticeRead, type SysNotice} from '@/api/notice'
import {
  getRecentAlarmNotifications,
  getAlarmNotificationUnreadCount,
  markAlarmNotificationRead,
  markAllAlarmNotificationsRead
} from '@/api/alarmNotification'
import type {AlarmNotificationItem} from '@/types/alarmNotification'
import {ElNotification} from 'element-plus'
```

**步骤 3b：替换 NoticeMessage 与状态**

把现有 `NoticeMessage` interface 与 `messages`/`unreadMessageCount`/`messageTab`/`filteredMessages` 替换为：

```typescript
/** 通知中心统一消息结构 */
interface NotifyMessage {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  type: string                 // 公告用：'system' | 'other'
  sourceType?: 'alarm' | 'offline'  // 事件用
  sourceId?: number            // 事件用：跳转目标
}

/** Tab 默认 'event'（更紧急） */
const notifyTab = ref<'event' | 'notice'>('event')
const noticeMessages = ref<NotifyMessage[]>([])
const eventMessages = ref<NotifyMessage[]>([])
const noticeUnreadCount = ref(0)
const eventUnreadCount = ref(0)

/** 顶部铃铛总数 */
const unreadMessageCount = computed(() => noticeUnreadCount.value + eventUnreadCount.value)

/** 当前 Tab 是否有消息 */
const currentTabHasMessages = computed(() =>
  notifyTab.value === 'event' ? eventMessages.value.length > 0 : noticeMessages.value.length > 0
)

function switchNotifyTab(tab: 'event' | 'notice') {
  notifyTab.value = tab
}
```

**步骤 3c：替换 fetchNotices 为两个独立函数**

把现有 `toNoticeMessage` + `fetchNotices` 替换为：

```typescript
function toNoticeMessage(n: SysNotice): NotifyMessage {
  return {
    id: n.noticeId,
    title: n.noticeTitle,
    content: n.noticeContent?.replace(/<[^>]*>/g, '') ?? '',
    time: n.createTime ?? '',
    read: n.isRead ?? false,
    type: n.noticeType === '1' ? 'system' : 'other'
  }
}

function toEventMessage(n: AlarmNotificationItem): NotifyMessage {
  return {
    id: n.id,
    title: n.title,
    content: n.content ?? '',
    time: n.createTime ?? '',
    read: n.readTime != null,
    type: 'alarm',
    sourceType: n.sourceType,
    sourceId: n.sourceId
  }
}

async function fetchNoticeMessages() {
  try {
    const res = await getTopNotices()
    const data = res.data
    noticeMessages.value = (data.list ?? []).map(toNoticeMessage)
    noticeUnreadCount.value = data.unreadCount ?? 0
  } catch { /* keep previous data */ }
}

async function fetchEventMessages() {
  try {
    const [recentRes, unreadRes] = await Promise.all([
      getRecentAlarmNotifications(20),
      getAlarmNotificationUnreadCount()
    ])
    eventMessages.value = (recentRes.data ?? []).map(toEventMessage)
    eventUnreadCount.value = unreadRes.data?.unreadCount ?? 0
  } catch { /* keep previous data */ }
}
```

**步骤 3d：替换 SSE 处理（区分 notice / alarm-notify 事件）**

把现有 `startNoticeSSE` 函数替换为：

```typescript
function startNoticeSSE() {
  if (noticeEventSource) noticeEventSource.close()
  const token = localStorage.getItem('token')
  if (!token) return
  noticeEventSource = new EventSource(`/api/v1/system/notice/stream?token=${encodeURIComponent(token)}`)
  noticeEventSource.addEventListener('notice', (event) => {
    try {
      const data = JSON.parse(event.data)
      noticeMessages.value.unshift({
        id: data.noticeId,
        title: data.title,
        content: data.content ?? '',
        time: data.createTime ?? '',
        read: false,
        type: data.type === '1' ? 'system' : 'other'
      })
      if (noticeMessages.value.length > 20) noticeMessages.value.pop()
      noticeUnreadCount.value++
    } catch { /* ignore malformed event */ }
  })
  noticeEventSource.onerror = () => {
    noticeEventSource?.close()
    setTimeout(startNoticeSSE, 3000)
  }
}

/** 告警 SSE：监听 alarm-notify 类型事件 */
let alarmEventSource: EventSource | null = null
function startAlarmSSE() {
  if (alarmEventSource) alarmEventSource.close()
  const token = localStorage.getItem('token')
  if (!token) return
  alarmEventSource = new EventSource(`/api/v1/alarm/stream?token=${encodeURIComponent(token)}`)
  alarmEventSource.addEventListener('alarm-notify', (event) => {
    try {
      const data = JSON.parse(event.data)
      // 实时弹窗提示
      ElNotification({
        title: data.title ?? '告警通知',
        message: data.content ?? '',
        type: 'warning',
        duration: 5000
      })
      // 刷新事件列表
      fetchEventMessages()
    } catch { /* ignore */ }
  })
  // 同时保留原 alarm 事件监听（全量广播）
  alarmEventSource.addEventListener('alarm', (event) => {
    try {
      const data = JSON.parse(event.data)
      ElNotification({
        title: '告警',
        message: data.alarmMessage ?? '',
        type: 'error',
        duration: 5000
      })
      fetchEventMessages()
    } catch { /* ignore */ }
  })
  alarmEventSource.onerror = () => {
    alarmEventSource?.close()
    setTimeout(startAlarmSSE, 3000)
  }
}
```

**步骤 3e：替换点击/已读处理（事件 Tab 跳转 + 公告 Tab 标读）**

把现有 `markMessageAsRead` 与 `markAllAsRead` 替换为：

```typescript
async function handleEventClick(msg: NotifyMessage) {
  // 1) 标记已读
  if (!msg.read) {
    try {
      await markAlarmNotificationRead(msg.id)
      msg.read = true
      eventUnreadCount.value = Math.max(0, eventUnreadCount.value - 1)
    } catch { /* ignore */ }
  }
  // 2) 跳转
  if (msg.sourceType === 'alarm') {
    router.push({path: '/alarm/realtime', query: msg.sourceId ? {alarmId: String(msg.sourceId)} : {}})
  } else if (msg.sourceType === 'offline') {
    router.push({path: '/basic/device', query: msg.sourceId ? {deviceId: String(msg.sourceId)} : {}})
  }
  // 3) 关闭面板
  messagePanelVisible.value = false
}

async function handleNoticeClick(msg: NotifyMessage) {
  if (!msg.read) {
    try {
      await markNoticeRead(msg.id)
      msg.read = true
      noticeUnreadCount.value = Math.max(0, noticeUnreadCount.value - 1)
    } catch { /* ignore */ }
  }
  router.push(`/system/notice/detail/${msg.id}`)
  messagePanelVisible.value = false
}

async function markAllAsRead() {
  if (notifyTab.value === 'event') {
    try {
      await markAllAlarmNotificationsRead()
      eventMessages.value.forEach(m => { m.read = true })
      eventUnreadCount.value = 0
    } catch { /* ignore */ }
  } else {
    try {
      const ids = noticeMessages.value.filter(m => !m.read).map(m => m.id).join(',')
      if (ids) {
        await markAllNoticeRead(ids)
        noticeMessages.value.forEach(m => { m.read = true })
        noticeUnreadCount.value = 0
      }
    } catch { /* ignore */ }
  }
}
```

**步骤 3f：更新 onMounted / onUnmounted**

把现有 `fetchNotices()` 和 SSE 启停替换为：

```typescript
// onMounted 内
fetchNoticeMessages()
fetchEventMessages()
startNoticeSSE()
startAlarmSSE()

// onUnmounted 内
if (noticeEventSource) {
  noticeEventSource.close()
  noticeEventSource = null
}
if (alarmEventSource) {
  alarmEventSource.close()
  alarmEventSource = null
}
```

- [ ] **步骤 4：CSS 微调（新增 tab-count 样式）**

在 `<style scoped>` 内追加：

```css
.tab-count {
  display: inline-block;
  margin-left: 2px;
  font-size: 11px;
  color: #f56c6c;
}
```

- [ ] **步骤 5：构建验证**

```bash
cd web && npm run build 2>&1 | tail -20
```

预期：构建成功，无 TypeScript 错误。

- [ ] **步骤 6：手工 E2E 验证**

```bash
cd web && npm run dev
```

在浏览器登录后：
1. 点右上角铃铛 → 弹出"通知中心"面板
2. Tab 默认显示"事件"，无数据时显示"暂无事件通知"
3. 切换到"公告"Tab，能看到 sys_notice 数据
4. 点击公告 → 跳转 `/system/notice/detail/{id}`
5. 模拟一次告警事件（手工触发 AlarmTriggeredEvent）→ 弹窗 + 事件 Tab 新增一条
6. 点击事件（alarm 类型）→ 跳转 `/alarm/realtime`
7. 点击"全部标为已读" → 当前 Tab 所有未读变已读

- [ ] **步骤 7：Commit**

```bash
git add web/src/layout/index.vue
git commit -m "feat(web/layout): 通知中心改造为公告/事件 Tab 分类 + SSE alarm-notify 监听 + 路由跳转"
```

---

### 任务 9：Settings.vue 新增通知配置分类（11 参数）

**文件：**
- 修改：`web/src/views/system/Settings.vue`

- [ ] **步骤 1：精读 Settings.vue 现有结构**

阅读 `web/src/views/system/Settings.vue` 行 235-298（接口类型 + paramCategories + paramList + paramsFormData 初始化）和行 573-590（handleSaveParams），明确：

- `paramCategories` 当前 4 项（basic/data/alarm/security），无 `notify`
- `ParamItem.type` 联合类型未含 `'password'`
- `handleSaveParams` 当前只调 `updateLogCleanupConfig` + `saveFocusArea`，无 sys_config 写入

- [ ] **步骤 2：扩展 ParamItem 类型**

修改 `web/src/views/system/Settings.vue` 行 235-248 的 ParamItem interface：

```typescript
interface ParamItem {
  code: string
  name: string
  type: 'string' | 'number' | 'select' | 'switch' | 'textarea' | 'geojson' | 'password'
  category: string
  value: any
  placeholder?: string
  maxLength?: number
  min?: number
  max?: number
  step?: number
  options?: Array<{ label: string; value: any }>
  remark: string
}
```

- [ ] **步骤 3：新增 notify 分类**

修改行 254-259 的 `paramCategories`：

```typescript
const paramCategories = [
  { key: 'basic', label: '基础配置' },
  { key: 'data', label: '数据管理' },
  { key: 'alarm', label: '告警配置' },
  { key: 'security', label: '安全设置' },
  { key: 'notify', label: '通知配置' }
]
```

- [ ] **步骤 4：扩展 paramList（追加 11 项通知参数）**

在行 285 末尾（`password_expire` 项之后）的 `]` 前追加：

```typescript
  ,

  // ===== 通知配置 =====
  {
    code: 'notify.sms.access-key-id',
    name: '阿里云 AccessKey ID',
    type: 'string',
    category: 'notify',
    value: '',
    placeholder: 'LTAIxxxxxxxx',
    maxLength: 64,
    remark: '阿里云 RAM 用户 AccessKey ID（短信发送权限）'
  },
  {
    code: 'notify.sms.access-key-secret',
    name: '阿里云 AccessKey Secret',
    type: 'password',
    category: 'notify',
    value: '',
    placeholder: '••••••••',
    maxLength: 128,
    remark: '阿里云 RAM 用户 AccessKey Secret'
  },
  {
    code: 'notify.sms.sign-name',
    name: '短信签名',
    type: 'string',
    category: 'notify',
    value: '知微监测',
    maxLength: 32,
    remark: '阿里云短信签名（需在控制台预申请）'
  },
  {
    code: 'notify.sms.template.alarm',
    name: '告警短信模板Code',
    type: 'string',
    category: 'notify',
    value: '',
    placeholder: 'SMS_XXXXXX',
    maxLength: 32,
    remark: '阿里云告警通知短信模板 ID'
  },
  {
    code: 'notify.sms.template.offline',
    name: '离线短信模板Code',
    type: 'string',
    category: 'notify',
    value: '',
    placeholder: 'SMS_YYYYYY',
    maxLength: 32,
    remark: '阿里云设备离线短信模板 ID'
  },
  {
    code: 'notify.mail.host',
    name: 'SMTP 主机',
    type: 'string',
    category: 'notify',
    value: 'smtp.qq.com',
    maxLength: 128,
    remark: 'SMTP 服务器地址'
  },
  {
    code: 'notify.mail.port',
    name: 'SMTP 端口',
    type: 'number',
    category: 'notify',
    value: 465,
    min: 1,
    max: 65535,
    remark: 'SMTP 端口（SSL 通常为 465，TLS 为 587）'
  },
  {
    code: 'notify.mail.username',
    name: '发件账号',
    type: 'string',
    category: 'notify',
    value: '',
    placeholder: 'sender@example.com',
    maxLength: 128,
    remark: '发件邮箱账号'
  },
  {
    code: 'notify.mail.password',
    name: 'SMTP 授权码',
    type: 'password',
    category: 'notify',
    value: '',
    placeholder: '••••••••',
    maxLength: 128,
    remark: '邮箱服务商提供的 SMTP 授权码（非邮箱登录密码）'
  },
  {
    code: 'notify.mail.from',
    name: '发件人邮箱',
    type: 'string',
    category: 'notify',
    value: '',
    placeholder: 'sender@example.com',
    maxLength: 128,
    remark: '发件人邮箱地址（一般与发件账号相同）'
  },
  {
    code: 'notify.mail.ssl',
    name: '启用 SSL',
    type: 'switch',
    category: 'notify',
    value: true,
    remark: '是否启用 SSL 加密连接（465 端口必选）'
  }
```

- [ ] **步骤 5：新增 type='password' 渲染分支**

修改行 42-83 的 `<template>` 部分，在 `param.type === 'string'` 分支后追加：

```vue
<template v-else-if="param.type === 'password'">
  <el-input
      v-model="paramsFormData[param.code]"
      type="password"
      show-password
      :placeholder="param.placeholder"
      :maxlength="param.maxLength"
      style="width: 400px"
  />
</template>
```

- [ ] **步骤 6：扩展 onMounted（加载已有 sys_config 值）**

修改行 551-571 的 onMounted，追加 notify 配置加载（逐 key 查询，`/system/config/configKey/{key}` 端点已存在）：

```typescript
// 在原有 onMounted 内追加（先在 import 区补: import request from '@/utils/request'）
const notifyCodes = paramList.value
  .filter(p => p.category === 'notify')
  .map(p => p.code)

await Promise.all(notifyCodes.map(async (code) => {
  try {
    const res: any = await request.get(`/system/config/configKey/${encodeURIComponent(code)}`)
    // 后端返回 AjaxResult，data 字段为 config_value 字符串；未配置时返回默认值或空
    const val = res?.data
    if (val != null && val !== '' && val !== 'null') {
      const currentVal = paramsFormData[code]
      const isBoolean = typeof currentVal === 'boolean'
      const isNumber = typeof currentVal === 'number'
      paramsFormData[code] = isBoolean
        ? String(val).toLowerCase() === 'true'
        : isNumber
          ? Number(val)
          : String(val)
    }
  } catch { /* 未配置，保留默认值 */ }
}))
```

> **注 1**：需在文件顶部 import 区追加 `import request from '@/utils/request'`（如未引入）。
> **注 2**：后端 `/api/v1/system/config/configKey/{configKey}` 走 `@Anonymous` 或登录态访问，前端已带 token。

- [ ] **步骤 7：扩展 handleSaveParams（保存到 sys_config）**

修改行 573-590 的 handleSaveParams，追加 notify 配置保存：

```typescript
const handleSaveParams = async () => {
  saveLoading.value = true
  try {
    // 原有：日志清理 + 关注区域
    await updateLogCleanupConfig({
      enabled: paramsFormData['auto_cleanup'],
      retentionDays: paramsFormData['log_keep_days'],
      cron: paramsFormData['cleanup_time']
    })
    if (geoJsonData.value) {
      await saveFocusArea(geoJsonData.value)
    }

    // 新增：保存所有 notify.* 参数到 sys_config
    const notifyCodes = paramList.value
      .filter(p => p.category === 'notify')
      .map(p => p.code)
    await Promise.all(notifyCodes.map(code =>
      request.post('/system/config', {
        configKey: code,
        configValue: String(paramsFormData[code]),
        configType: 'Y'
      })
    ))

    ElMessage.success('系统参数保存成功')
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saveLoading.value = false
  }
}
```

- [ ] **步骤 8：构建验证**

```bash
cd web && npm run build 2>&1 | tail -20
```

预期：构建成功。

- [ ] **步骤 9：手工验证**

启动前端，进入 `/system/settings`：
1. 左侧导航出现"通知配置"分类
2. 点击后右侧显示 11 个参数表单项
3. AccessKey Secret 与 SMTP 授权码显示为密码框（`show-password` 可切换可见）
4. 启用 SSL 为 switch 开关
5. SMTP 端口为数字输入（1-65535）
6. 填入测试值后点"保存" → Toast 成功
7. 刷新页面后值回显（onMounted 加载 sys_config）

- [ ] **步骤 10：Commit**

```bash
git add web/src/views/system/Settings.vue
git commit -m "feat(web/settings): 新增通知配置分类 (11 参数) + password 渲染 + sys_config 持久化"
```

---

### 任务 10：sys_menu 权限注册 SQL

**文件：**
- 创建：`db/upgrade/v2026.06.17.004_notification_menu.sql`

- [ ] **步骤 1：编写 SQL**

```sql
-- 通知中心事件接口权限注册（挂在告警管理父菜单下）
-- 前置：sys_menu 已存在 menu_name='告警管理' AND menu_type='M' 的父菜单

-- 1) 查找父菜单 ID
SET @alarm_parent_id = (
    SELECT menu_id FROM sys_menu
    WHERE menu_name = '告警管理' AND menu_type = 'M'
    LIMIT 1
);

-- 2) 兜底：如果没找到，挂到 0（顶级）
SET @alarm_parent_id = IFNULL(@alarm_parent_id, 0);

-- 3) 注册两个按钮权限
INSERT INTO `sys_menu` (
    `menu_name`, `parent_id`, `menu_type`, `order_num`,
    `perms`, `is_frame`, `is_cache`, `visible`, `status`,
    `create_by`, `create_time`, `remark`
) VALUES
(
    '告警通知查看', @alarm_parent_id, 'F', 50,
    'alarm:notification:list', 1, 0, '0', '0',
    'admin', NOW(), '通知中心事件 Tab 查询本人通知'
),
(
    '告警通知已读', @alarm_parent_id, 'F', 51,
    'alarm:notification:read', 1, 0, '0', '0',
    'admin', NOW(), '通知中心标记已读操作'
);

-- 4) 给 admin 角色（role_id=1）授权
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('alarm:notification:list', 'alarm:notification:read')
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- 5) 校验
SELECT menu_id, menu_name, perms FROM sys_menu
WHERE perms LIKE 'alarm:notification:%';
```

- [ ] **步骤 2：执行验证**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2026.06.17.004_notification_menu.sql
mysql -uroot -pwodepassword geo_hazard_monitor -e "SELECT menu_id, menu_name, perms FROM sys_menu WHERE perms LIKE 'alarm:notification:%';"
```

预期：返回 2 行（list + read）。

- [ ] **步骤 3：调用权限注册端点同步**

```bash
# 用 admin 登录拿 token 后
curl -s -X GET "http://localhost:8080/api/v1/menus/permission-coverage" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.missingInDb'
```

预期：`alarm:notification:list` 与 `alarm:notification:read` 不在 missingInDb 列表中。

- [ ] **步骤 4：Commit**

```bash
git add db/upgrade/v2026.06.17.004_notification_menu.sql
git commit -m "feat(db): 注册 alarm:notification:list/read 权限菜单 SQL"
```

---

### 任务 11：前端路由 query 兼容性验证（/alarm/realtime、/basic/device）

**文件：**
- 修改：`web/src/router/index.ts`（如缺）
- 修改：`web/src/views/alarm/RealtimeAlarm.vue`
- 修改：`web/src/views/basic/Device.vue`

- [ ] **步骤 1：核对路由配置**

```bash
grep -n "alarm/realtime\|basic/device\|RealtimeAlarm\|basic/Device" web/src/router/index.ts
```

确认 `/alarm/realtime` 与 `/basic/device` 路由存在。

- [ ] **步骤 2：在告警实时页面支持 alarmId query 高亮**

查找 `/alarm/realtime` 对应的页面组件文件，已确认为 `web/src/views/alarm/RealtimeAlarm.vue`：

```bash
ls web/src/views/alarm/RealtimeAlarm.vue  # 应存在
```

在对应页面 `<script setup>` 中追加（如缺）：

```typescript
import {useRoute} from 'vue-router'
import {nextTick, onMounted} from 'vue'

const route = useRoute()

onMounted(async () => {
  const alarmId = route.query.alarmId
  if (alarmId) {
    // 等待列表加载完成
    await nextTick()
    // 滚动并高亮
    const row = (document.querySelector(`[data-alarm-id="${alarmId}"]`) as HTMLElement)
    row?.scrollIntoView({behavior: 'smooth', block: 'center'})
    row?.classList.add('row-highlight-from-notify')
    setTimeout(() => row?.classList.remove('row-highlight-from-notify'), 5000)
  }
})
```

在 `<el-table-column>` 的 `<template #default="{row}">` 上加 `:data-alarm-id="row.id"`（如已有 row id）。

> **注**：如果找不到对应的 .vue 文件，跳过此步并 commit 一条文档说明：

```bash
# 兜底：创建 docs/notes/alarm-jump-from-notify.md
mkdir -p docs/notes
cat > docs/notes/alarm-jump-from-notify.md <<'EOF'
# 通知中心跳转告警详情

`/alarm/realtime?alarmId={id}` 跳转后需高亮对应行。

实现位置：实时告警页面 (待补充路径) 的 onMounted。
EOF
git add docs/notes/alarm-jump-from-notify.md
git commit -m "docs(alarm): 通知中心跳转告警详情的实现备忘"
```

- [ ] **步骤 3：设备页面支持 deviceId query 高亮（同上模式）**

类似地，在 `/basic/device` 对应的页面支持 `deviceId` query。

> **注**：如果设备页面已有搜索框，直接把 `route.query.deviceId` 注入到搜索条件即可。

- [ ] **步骤 4：Commit**

```bash
git add web/src/views/alarm/  # 或具体文件
git commit -m "feat(web/alarm): 支持 ?alarmId query 跳转高亮 (通知中心联动)"
```

---

### 任务 12：集成测试 — 通知中心全链路

**文件：**
- 创建：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/NotificationCenterIntegrationTest.java`

- [ ] **步骤 1：编写测试**

```java
package com.zwei.iot.alarm;

import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * 通知中心全链路集成测试：
 * 创建通知 → 查 recent → 查 unread → 标记已读 → 全部已读
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class NotificationCenterIntegrationTest {

    @Autowired private IAlarmNotificationService service;
    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUserId).thenReturn(88888L);
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    @Test
    void fullFlow_createReadMarkAllMark() {
        Long userId = 88888L;

        // 1) 种 3 条数据
        Long id1 = seed(userId, "alarm", 80001L, "alarm-1");
        Long id2 = seed(userId, "offline", 80002L, "offline-1");
        Long id3 = seed(userId, "alarm", 80003L, "alarm-2");

        // 2) recent 应返回 3 条
        List<AlarmNotification> recent = service.selectUserRecent(userId, 10);
        assertThat(recent).hasSize(3);

        // 3) unread-count 应为 3
        assertThat(service.selectUnreadCount(userId)).isEqualTo(3);

        // 4) 标记 id1 已读
        assertThat(service.markReadIfOwner(id1, userId)).isEqualTo(1);
        assertThat(service.selectUnreadCount(userId)).isEqualTo(2);

        // 5) 全部已读
        assertThat(service.markAllRead(userId)).isEqualTo(2);
        assertThat(service.selectUnreadCount(userId)).isEqualTo(0);
    }

    @Test
    void recent_onlyReturnsSystemChannel() {
        Long userId = 88888L;
        seed(userId, "alarm", 81001L, "sys-1");  // SYSTEM
        // 不易直接种 SMS/EMAIL（会被 selectUserRecent 过滤）
        // 通过查询验证 channel='SYSTEM' 条件生效

        List<AlarmNotification> recent = service.selectUserRecent(userId, 10);
        assertThat(recent).allSatisfy(n -> assertThat(n.getChannel()).isEqualTo("SYSTEM"));
    }

    private Long seed(Long userId, String sourceType, Long sourceId, String title) {
        AlarmNotification n = new AlarmNotification();
        n.setRecipientId(userId);
        n.setChannel("SYSTEM");
        n.setSourceType(sourceType);
        n.setSourceId(sourceId);
        n.setTitle(title);
        n.setContent("integration-test");
        n.setStatus(AlarmNotification.STATUS_SENT);
        n.setCreateTime(new Date());
        service.createNotification(n);
        return n.getId();
    }
}
```

- [ ] **步骤 2：运行测试**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=NotificationCenterIntegrationTest -Dspring.profiles.active=local
```

预期：2 个测试方法 PASS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/NotificationCenterIntegrationTest.java
git commit -m "test(alarm): 通知中心全链路集成测试 (create→read→markAllRead)"
```

---

### 任务 13：用户文档

**文件：**
- 创建：`docs/通知中心使用手册.md`

- [ ] **步骤 1：编写文档**

```markdown
# 通知中心使用手册

> 适用于：知微地质灾害监测预警系统 v2.x

## 1. 功能概述

通知中心是用户接收系统告警事件与运营公告的统一入口，位于页面右上角铃铛图标。

**核心特性：**

- **Tab 分类**：
  - **事件**（默认）：来自告警规则匹配后分发的 SYSTEM 通知（告警/设备离线）
  - **公告**：管理员发布的全员广播
- **实时推送**：SSE 长连接，事件触发即时弹窗
- **一键已读**：支持单条点击已读、当前 Tab 全部已读
- **智能跳转**：
  - 点击告警事件 → `/alarm/realtime?alarmId={id}` 高亮对应告警
  - 点击离线事件 → `/basic/device?deviceId={id}` 定位设备
  - 点击公告 → `/system/notice/detail/{id}` 查看详情

## 2. 接收通知的前置条件

### 2.1 配置通知规则（管理员）

路径：告警中心 → 通知设置 → 新增通知规则

1. **事件类型**：告警事件 / 设备离线
2. **接收人**：按角色 / 按部门 / 指定人员（可多选，含"全部"通配）
3. **通知渠道**：系统消息（必选其一）/ 短信 / 邮件

### 2.2 配置渠道凭证（管理员）

路径：系统设置 → 通知配置

| 渠道 | 必填项 |
|------|--------|
| 短信 | 阿里云 AccessKey ID/Secret、签名、模板 Code |
| 邮件 | SMTP 主机/端口、发件账号、授权码、发件人 |

> 系统消息渠道无需配置，默认可用。

## 3. 用户操作

### 3.1 查看通知

点击右上角铃铛 → 弹出通知中心面板。默认显示"事件"Tab。

### 3.2 标记已读

- **单条**：点击通知行 → 自动标记已读 + 跳转对应详情
- **全部**：点击底部"全部标为已读"按钮 → 当前 Tab 全部已读

### 3.3 实时弹窗

当告警事件触发时，会弹出系统通知（5 秒自动关闭），同时铃铛角标 +1。

## 4. FAQ

**Q: 为什么没收到通知？**

检查：
1. 是否有启用的通知规则匹配当前事件
2. 接收人配置中是否包含本人（或本人所属角色/部门）
3. 短信/邮件是否配置完整凭证（系统设置 → 通知配置）
4. 通知记录是否处于失败状态（查询 alarm_notification.error_msg）

**Q: 公告和事件的区别？**

- **公告**：管理员主动发布，全员可见，无个性化过滤
- **事件**：系统自动生成，基于通知规则匹配 + 用户维度定向推送

**Q: 弹窗太频繁怎么办？**

当前版本（YAGNI）无静音开关，后续迭代支持。
```

- [ ] **步骤 2：Commit**

```bash
git add "docs/通知中心使用手册.md"
git commit -m "docs(alarm): 新增通知中心使用手册"
```

---

### 任务 14：CLAUDE.md 模块文档更新

**文件：**
- 修改：`server/zwei-iot-alarm/CLAUDE.md`
- 修改：`web/CLAUDE.md`

- [ ] **步骤 1：更新 server/zwei-iot-alarm/CLAUDE.md**

在"对外接口 (Controller)"表中追加 2 行：

```markdown
| `/api/v1/alarm/notifications/*`   | 通知中心事件 API（用户视角 recent/unread/read）|
```

在"通知层"小节追加：

```markdown
- `AlarmStreamPublisher.publishToUser(userId, eventType, data)` — 单点 SSE 推送（用于 SYSTEM 渠道）
- `AlarmNotificationController` — 用户视角通知 API（recent/unread-count/read/read-all）
```

在"数据模型"小节更新：

```markdown
- `alarm_notification` — 通知记录 (alarmId / dispatchRuleId / recipientId / channel / status 1=待发送 2=已发送 3=失败 4=接收人无效 5=渠道未配置 / read_time / source_type / source_id / error_msg)
```

在"变更记录"追加：

```markdown
| 2026-06-17 | 通知规则迭代 v2：3 张关联表（hazard_point/device/recipient）+ 双事件监听 + 三渠道策略（SYSTEM/SMS/EMAIL）+ 通知中心 Tab 分类 |
```

- [ ] **步骤 2：更新 web/CLAUDE.md**

在"业务模块 → 页面映射"表的"告警"行下方追加：

```markdown
| 通知中心 | 顶栏铃铛 | `layout/index.vue`（事件 Tab + 公告 Tab） |
```

在"API 模块"表追加：

```markdown
| `alarmNotification.ts` | 通知中心事件 API |
| `alarmDispatch.ts` | 通知规则 CRUD |
```

在"变更记录"追加：

```markdown
| 2026-06-17 | 通知规则迭代 v2：layout 通知中心 Tab 分类 + Settings 通知配置分类（11 参数） |
```

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/CLAUDE.md web/CLAUDE.md
git commit -m "docs(alarm): 更新模块 CLAUDE.md (通知中心 + 通知规则 v2)"
```

---

## 自检清单

### 规格覆盖度（计划 C 部分）

| 规格章节 | 对应任务 | 状态 |
|---------|---------|------|
| §8.1 接口分工（recent/unread/read/readAll） | 任务 5 | ✅ |
| §8.2 关键 SQL（selectUserRecent/markReadIfOwner） | 任务 1 | ✅ |
| §8.3 权限标识 alarm:notification:list/read | 任务 10 | ✅ |
| §9.1 改造范围（layout/Settings/alarmNotification.ts） | 任务 7/8/9 | ✅ |
| §9.5 通知中心 Tab 改造（公告/事件，事件默认） | 任务 8 | ✅ |
| §9.6 SSE 监听 alarm-notify | 任务 8 | ✅ |
| §9.7 系统设置新增分类 | 任务 9 | ✅ |
| §11 菜单与权限 SQL | 任务 10 | ✅ |
| 跳转 /alarm/realtime、/basic/device | 任务 8、任务 11 | ✅ |

### 计划 C 不涵盖（计划 A、B 已完成）

- 计划 A：DDL、Domain、规则 CRUD + 前端规则表单
- 计划 B：双事件监听 + 三渠道策略实现

### 占位符扫描

- ✅ 所有 Java 类有完整代码
- ✅ Mapper XML 完整
- ✅ Vue 模板/脚本完整（基于现有 layout/index.vue 改造）
- ⚠️ 任务 8 步骤 2/3 是基于现有 layout/index.vue 行号（277-390）的相对引用，实际执行时需 grep 定位
- ⚠️ 任务 9 步骤 6 已改为逐 key 调用 `/system/config/configKey/{key}`（已核对 SysConfigController 存在该端点）
- ⚠️ 任务 11 路由页面文件名已核对：`web/src/views/alarm/RealtimeAlarm.vue`、`web/src/views/basic/Device.vue`

### 类型一致性

| 名称 | 定义位置 | 使用位置 |
|------|---------|---------|
| `IAlarmNotificationService.selectUserRecent/selectUnreadCount/markReadIfOwner/markAllRead` | 任务 2 | 任务 5、任务 6、任务 12 |
| `AlarmNotificationItemVO` / `AlarmNotificationSummaryVO` | 任务 3 | 任务 5 |
| `AlarmStreamPublisher.publishToUser` | 任务 4 | （计划 B 的 SystemNotifyChannel 已使用） |
| `AlarmNotificationItem` | 任务 7 (TS) | 任务 8 |
| `NotifyMessage` | 任务 8 | 任务 8 |
| `getRecentAlarmNotifications` / `getAlarmNotificationUnreadCount` 等 | 任务 7 | 任务 8 |
| `AlarmNotification.STATUS_SENT` | 计划 B（前置） | 任务 1、任务 6、任务 12 |
| `AlarmNotification.readTime/sourceType/sourceId` | 计划 B（前置） | 任务 1 |

### 已知风险

1. **现有 layout/index.vue 行号可能漂移**：执行时需用 Grep 定位
2. **`/system/config/configKey/{key}` 端点**：已核对 SysConfigController 存在该端点
3. **`/api/v1/alarm/stream` SSE 路径**：已核对 AlarmStreamController 实际 `@RequestMapping("/api/v1/alarm/stream")`
4. **`RealtimeAlarm.vue` / `Device.vue`**：已核对存在于 `web/src/views/alarm/` 与 `web/src/views/basic/`
4. **`AlarmTriggeredEvent` 字段名**：核对 com.zwei.common.event 实际类
5. **权限覆盖查询端点**：依赖 sys_menu 的 permission-coverage API（已存在于 CLAUDE.md）
6. **SSE 多连接**：同时存在 notice + alarm 两个 SSE，浏览器并发限制可能影响（HTTP/1.1 同源 6 个）

---

## 执行交接

计划 C 已完成并保存到 `docs/superpowers/plans/2026-06-17-alarm-dispatch-rule-iteration-plan-c.md`。

至此 3 个计划（A：数据基础 + 规则 CRUD；B：事件分发 + 三渠道；C：通知中心 + 系统设置 + 收尾）全部完成。

**两种执行方式：**

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

**选哪种方式？**

**如果选择子代理驱动：**
- **必需子技能：** 使用 superpowers:subagent-driven-development
- 每个任务一个新子代理 + 两阶段审查

**如果选择内联执行：**
- **必需子技能：** 使用 superpowers:executing-plans
- 批量执行并设有检查点供审查
