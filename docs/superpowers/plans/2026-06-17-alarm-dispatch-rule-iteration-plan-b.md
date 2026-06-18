# 通知规则迭代 - 计划 B：事件分发 + 三渠道策略实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现告警/离线事件的双监听、规则匹配、接收人展开、按用户×渠道去重，以及 SYSTEM（SSE+落库）、SMS（阿里云）、EMAIL（SMTP+Thymeleaf）三渠道策略实现。

**架构：** `AlarmNotifier` 监听 `AlarmTriggeredEvent` 和 `DeviceOfflineEvent`，异步执行 → `IAlarmRuleMatcher` 匹配规则 → `IAlarmRecipientResolver` 展开 userId → 笛卡尔展开并按 (source_type, source_id, userId, channel) 去重 → `NotifyChannelDispatcher` 路由到 `INotifyChannel` 实现 → 各渠道实际发送并写 error_msg。

**技术栈：** Spring Boot 4.0.3 + 阿里云 dysmsapi20170525 + spring-boot-starter-mail + Thymeleaf + Spring Async

**关联规格：** `docs/superpowers/specs/2026-06-17-alarm-dispatch-rule-iteration-design.md` 第 5-7 节

**前置条件：** 计划 A 已完成（DDL、Domain、CRUD 可用）

---

## 文件结构

### 创建（22 个）

| 文件 | 职责 |
|------|------|
| `db/upgrade/v2026.06.17.003_notif_dedup_key.sql` | 补充 uk_notif_dedup 唯一键 |
| `server/zwei-iot-alarm/.../mapper/AlarmRecipientQueryMapper.java` + xml | 跨模块查 sys_user*（只读） |
| `.../service/IAlarmRuleMatcher.java` | 规则匹配接口 |
| `.../service/IAlarmRecipientResolver.java` | 接收人展开接口 |
| `.../service/impl/AlarmRuleMatcherImpl.java` | 匹配实现 |
| `.../service/impl/AlarmRecipientResolverImpl.java` | 展开实现 |
| `.../channel/INotifyChannel.java` | 渠道策略接口 |
| `.../channel/NotifyChannelDispatcher.java` | 路由分发器 |
| `.../channel/NotifyRecipientValidator.java` | 手机号/邮箱校验 |
| `.../channel/SystemNotifyChannel.java` | SSE + 落库 |
| `.../channel/NotifyConfigLoader.java` | sys_config 加载（含 @Cacheable） |
| `.../channel/config/SmsConfig.java` | SMS 配置 POJO |
| `.../channel/config/MailConfig.java` | Mail 配置 POJO |
| `.../channel/AliyunSmsClient.java` | 阿里云 SDK 封装 |
| `.../channel/SmsNotifyChannel.java` | SMS 渠道 |
| `.../channel/DynamicMailSender.java` | 动态 JavaMailSender |
| `.../channel/EmailNotifyChannel.java` | EMAIL 渠道 |
| `.../channel/NotifyTemplateService.java` | 模板渲染（三渠道） |
| `.../channel/NotifyContext.java` | 模板变量上下文 |
| `.../config/AlarmNotifyAsyncConfig.java` | 异步执行器 |
| `.../resources/templates/mail/alarm-notify.html` | 邮件 HTML 模板 |
| 测试 7 个（详见各任务） |

### 修改（5 个）

| 文件 | 改动 |
|------|------|
| `server/zwei-iot-alarm/pom.xml` | 新增 3 组依赖 |
| `.../domain/AlarmNotification.java` | 加 read_time/source_type/source_id 字段 + STATUS_* 常量 |
| `.../service/IAlarmNotificationService.java` | 加 markSent/markFailed/markRead 方法 |
| `.../service/impl/AlarmNotificationServiceImpl.java` | 实现新方法 |
| `.../mapper/AlarmNotificationMapper.xml` | 加 markFailed/markRead SQL |
| `.../AlarmNotifier.java` | 重构为双事件监听 + 去重 |

---

## 任务清单

### 任务 1：补充 uk_notif_dedup 唯一键

**文件：**
- 创建：`db/upgrade/v2026.06.17.003_notif_dedup_key.sql`

- [ ] **步骤 1：编写 SQL**

```sql
-- 补充 alarm_notification 去重唯一键（计划 A 暂未创建以避免迁移冲突）

-- 1) 清理潜在的历史重复数据（保留最早一条）
DELETE n1 FROM alarm_notification n1
INNER JOIN alarm_notification n2
  ON n1.source_type = n2.source_type
 AND n1.source_id = n2.source_id
 AND n1.recipient_id = n2.recipient_id
 AND n1.channel = n2.channel
 AND n1.id > n2.id;

-- 2) 兼容老数据的 alarm_id 字段（如有 NULL source_id 但有 alarm_id 的，回填）
UPDATE alarm_notification
SET source_type = 'alarm',
    source_id = alarm_id
WHERE source_id IS NULL
  AND alarm_id IS NOT NULL;

-- 3) 创建唯一键
ALTER TABLE `alarm_notification`
    ADD UNIQUE KEY `uk_notif_dedup`
        (`source_type`, `source_id`, `recipient_id`, `channel`);
```

- [ ] **步骤 2：执行验证**

```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/v2026.06.17.003_notif_dedup_key.sql
mysql -uroot -pwodepassword geo_hazard_monitor -e "SHOW INDEX FROM alarm_notification WHERE Key_name = 'uk_notif_dedup';"
```

预期：4 列复合唯一键存在。

- [ ] **步骤 3：Commit**

```bash
git add db/upgrade/v2026.06.17.003_notif_dedup_key.sql
git commit -m "feat(alarm/db): alarm_notification 去重唯一键 uk_notif_dedup"
```

---

### 任务 2：pom.xml 新增依赖

**文件：**
- 修改：`server/zwei-iot-alarm/pom.xml`

- [ ] **步骤 1：在 dependencies 段追加**

```xml
<!-- 阿里云短信 SDK -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>dysmsapi20170525</artifactId>
    <version>3.1.0</version>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>tea-openapi</artifactId>
    <version>0.3.6</version>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>tea</artifactId>
    <version>1.3.3</version>
</dependency>

<!-- SMTP 邮件 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Thymeleaf 模板渲染（HTML 邮件） -->
<dependency>
    <groupId>org.thymeleaf</groupId>
    <artifactId>thymeleaf-spring6</artifactId>
</dependency>
```

> 注：版本号可上移到父 pom.xml 的 `<dependencyManagement>` 统一管理（实现时按项目惯例调整）。

- [ ] **步骤 2：编译验证（拉依赖）**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS，无依赖冲突。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/pom.xml
git commit -m "build(alarm): 新增依赖 - 阿里云 SMS / SMTP 邮件 / Thymeleaf"
```

---

### 任务 3：扩展 AlarmNotification 实体与 Service

**文件：**
- 修改：`server/zwei-iot-alarm/.../domain/AlarmNotification.java`
- 修改：`.../service/IAlarmNotificationService.java`
- 修改：`.../service/impl/AlarmNotificationServiceImpl.java`
- 修改：`.../mapper/AlarmNotificationMapper.xml`

> 注：先 Read 各文件确认实际路径与现有结构。

- [ ] **步骤 1：扩展 AlarmNotification 实体**

在原实体类追加字段和常量：

```java
public class AlarmNotification {
    // 原有字段...

    /** 已读时间 */
    private Date readTime;
    /** 来源：alarm / offline */
    private String sourceType;
    /** 来源 ID（alarm_record.id 或 device.id） */
    private Long sourceId;

    // ===== 状态码常量 =====
    public static final int STATUS_PENDING                = 1;
    public static final int STATUS_SENT                   = 2;
    public static final int STATUS_FAILED                 = 3;
    public static final int STATUS_INVALID_RECIPIENT      = 4;
    public static final int STATUS_CHANNEL_NOT_CONFIGURED = 5;

    /** 根据错误码推导 status */
    public static int statusFromErrorCode(String errorCode) {
        return switch (errorCode) {
            case "RECIPIENT_PHONE_MISSING", "RECIPIENT_PHONE_INVALID",
                 "RECIPIENT_EMAIL_MISSING", "RECIPIENT_EMAIL_INVALID" -> STATUS_INVALID_RECIPIENT;
            case "CHANNEL_NOT_CONFIGURED"                            -> STATUS_CHANNEL_NOT_CONFIGURED;
            default                                                  -> STATUS_FAILED;
        };
    }
}
```

- [ ] **步骤 2：扩展 IAlarmNotificationService 接口**

```java
public interface IAlarmNotificationService {
    // 原有方法保留...

    /** 标记成功：status=SENT, send_time=NOW, error_msg=NULL */
    void markSent(Long id);

    /** 标记失败：根据 errorCode 决定 status，写 error_msg */
    void markFailed(Long id, String errorCode, String errorDescription);

    /** 标记已读：read_time=NOW（仅当当前用户为接收人时） */
    int markReadIfOwner(Long id, Long userId);

    /** 当前用户全部已读 */
    int markAllRead(Long userId, String channel);

    /** 取用户最近 N 条（channel=SYSTEM） */
    List<AlarmNotification> selectUserRecent(Long userId, int limit);

    /** 用户未读数 */
    int selectUnreadCount(Long userId, String channel);
}
```

- [ ] **步骤 3：实现新方法**

```java
@Service
public class AlarmNotificationServiceImpl implements IAlarmNotificationService {

    @Autowired private AlarmNotificationMapper notificationMapper;

    // 原有方法保留...

    @Override
    public void markSent(Long id) {
        AlarmNotification update = new AlarmNotification();
        update.setId(id);
        update.setStatus(AlarmNotification.STATUS_SENT);
        update.setSendTime(new Date());
        update.setErrorMsg(null);
        notificationMapper.updateById(update);
    }

    @Override
    public void markFailed(Long id, String errorCode, String errorDescription) {
        AlarmNotification update = new AlarmNotification();
        update.setId(id);
        update.setStatus(AlarmNotification.statusFromErrorCode(errorCode));
        update.setErrorMsg("[" + errorCode + "] " + errorDescription);
        update.setSendTime(new Date());
        notificationMapper.updateById(update);
    }

    @Override
    public int markReadIfOwner(Long id, Long userId) {
        return notificationMapper.markReadIfOwner(id, userId);
    }

    @Override
    public int markAllRead(Long userId, String channel) {
        return notificationMapper.markAllRead(userId, channel);
    }

    @Override
    public List<AlarmNotification> selectUserRecent(Long userId, int limit) {
        return notificationMapper.selectUserRecent(userId, limit);
    }

    @Override
    public int selectUnreadCount(Long userId, String channel) {
        return notificationMapper.selectUnreadCount(userId, channel);
    }
}
```

- [ ] **步骤 4：扩展 AlarmNotificationMapper.xml**

追加 SQL：

```xml
<update id="markReadIfOwner">
    UPDATE alarm_notification
    SET read_time = NOW()
    WHERE id = #{id}
      AND recipient_id = #{userId}
      AND read_time IS NULL
</update>

<update id="markAllRead">
    UPDATE alarm_notification
    SET read_time = NOW()
    WHERE recipient_id = #{userId}
      AND channel = #{channel}
      AND read_time IS NULL
</update>

<select id="selectUserRecent" resultType="com.zwei.iot.alarm.notify.domain.AlarmNotification">
    SELECT id, source_type, source_id, title, content,
           recipient_name, read_time, create_time, channel,
           dispatch_rule_id, status
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
      AND channel = #{channel}
      AND read_time IS NULL
</select>
```

> 注：resultType 包路径按项目实际调整。原 `AlarmNotification` 可能在 `com.zwei.iot.alarm.notify.domain.*` 或 `com.zwei.iot.alarm.dispatch.domain.*`，实现时核对。

- [ ] **步骤 5：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 6：Commit**

```bash
git add server/zwei-iot-alarm/
git commit -m "feat(alarm): AlarmNotification 扩展 - read_time/source_*/状态码/markSent/markFailed/markRead"
```

---

### 任务 4：编写 NotifyRecipientValidator（TDD）

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/NotifyRecipientValidator.java`
- 创建：`server/zwei-iot-alarm/src/test/java/.../channel/NotifyRecipientValidatorTest.java`

- [ ] **步骤 1：编写测试（TDD 红灯）**

```java
package com.zwei.iot.alarm.channel;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NotifyRecipientValidatorTest {

    @Test
    void phone_null_returns_missing() {
        assertThat(NotifyRecipientValidator.validatePhone(null))
            .isEqualTo("RECIPIENT_PHONE_MISSING");
    }

    @Test
    void phone_blank_returns_missing() {
        assertThat(NotifyRecipientValidator.validatePhone("  "))
            .isEqualTo("RECIPIENT_PHONE_MISSING");
    }

    @Test
    void phone_short_returns_invalid() {
        assertThat(NotifyRecipientValidator.validatePhone("1380"))
            .isEqualTo("RECIPIENT_PHONE_INVALID");
    }

    @Test
    void phone_invalid_prefix_returns_invalid() {
        assertThat(NotifyRecipientValidator.validatePhone("12812345678"))
            .isEqualTo("RECIPIENT_PHONE_INVALID");
    }

    @Test
    void phone_valid_returns_null() {
        assertThat(NotifyRecipientValidator.validatePhone("13812345678"))
            .isNull();
    }

    @Test
    void email_null_returns_missing() {
        assertThat(NotifyRecipientValidator.validateEmail(null))
            .isEqualTo("RECIPIENT_EMAIL_MISSING");
    }

    @Test
    void email_no_at_returns_invalid() {
        assertThat(NotifyRecipientValidator.validateEmail("abc.example.com"))
            .isEqualTo("RECIPIENT_EMAIL_INVALID");
    }

    @Test
    void email_valid_returns_null() {
        assertThat(NotifyRecipientValidator.validateEmail("abc@example.com"))
            .isNull();
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=NotifyRecipientValidatorTest
```

预期：编译失败（`NotifyRecipientValidator` 不存在）。

- [ ] **步骤 3：编写实现**

```java
package com.zwei.iot.alarm.channel;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 通知接收人手机号/邮箱校验工具
 */
public final class NotifyRecipientValidator {

    private static final Pattern PHONE =
        Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private NotifyRecipientValidator() {}

    /**
     * 校验手机号
     * @return null 表示通过；否则返回错误码
     */
    public static String validatePhone(String phone) {
        if (StringUtils.isBlank(phone)) return "RECIPIENT_PHONE_MISSING";
        if (!PHONE.matcher(phone).matches()) return "RECIPIENT_PHONE_INVALID";
        return null;
    }

    /**
     * 校验邮箱
     * @return null 表示通过；否则返回错误码
     */
    public static String validateEmail(String email) {
        if (StringUtils.isBlank(email)) return "RECIPIENT_EMAIL_MISSING";
        if (!EMAIL.matcher(email).matches()) return "RECIPIENT_EMAIL_INVALID";
        return null;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=NotifyRecipientValidatorTest
```

预期：8 个测试全部 PASS。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/NotifyRecipientValidator.java
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/channel/NotifyRecipientValidatorTest.java
git commit -m "feat(alarm): NotifyRecipientValidator - 手机号/邮箱校验 (TDD)"
```

---

### 任务 5：编写 SmsConfig / MailConfig POJO

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/config/SmsConfig.java`
- 创建：`server/zwei-iot-alarm/.../channel/config/MailConfig.java`

- [ ] **步骤 1：编写 SmsConfig**

```java
package com.zwei.iot.alarm.channel.config;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class SmsConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String alarmTemplateCode;      // 告警短信模板
    private String offlineTemplateCode;    // 离线短信模板

    /** 关键字段是否齐全 */
    public boolean isConfigured() {
        return StringUtils.isNotBlank(accessKeyId)
            && StringUtils.isNotBlank(accessKeySecret)
            && StringUtils.isNotBlank(signName);
    }

    /** 详细校验，返回缺失字段说明或 null */
    public String validate() {
        if (StringUtils.isBlank(accessKeyId))     return "阿里云 SMS accessKeyId 未配置";
        if (StringUtils.isBlank(accessKeySecret)) return "阿里云 SMS accessKeySecret 未配置";
        if (StringUtils.isBlank(signName))        return "阿里云 SMS 签名未配置";
        return null;
    }

    /** 按 sourceType 选模板 Code */
    public String selectTemplateCode(String sourceType) {
        if ("offline".equalsIgnoreCase(sourceType)) {
            return offlineTemplateCode;
        }
        return alarmTemplateCode;
    }
}
```

- [ ] **步骤 2：编写 MailConfig**

```java
package com.zwei.iot.alarm.channel.config;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class MailConfig {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String from;
    private Boolean ssl;

    public boolean isConfigured() {
        return StringUtils.isNotBlank(host)
            && port != null
            && StringUtils.isNotBlank(username)
            && StringUtils.isNotBlank(password)
            && StringUtils.isNotBlank(from);
    }

    public String validate() {
        if (StringUtils.isBlank(host))     return "SMTP 主机未配置";
        if (port == null)                  return "SMTP 端口未配置";
        if (StringUtils.isBlank(username)) return "SMTP 用户名未配置";
        if (StringUtils.isBlank(password)) return "SMTP 授权码未配置";
        if (StringUtils.isBlank(from))     return "发件人邮箱未配置";
        return null;
    }

    public boolean isSsl() {
        return Boolean.TRUE.equals(ssl);
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
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/config/
git commit -m "feat(alarm): SMS/Mail 配置 POJO"
```

---

### 任务 6：编写 NotifyConfigLoader

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/NotifyConfigLoader.java`

- [ ] **步骤 1：编写实现**

```java
package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.channel.config.MailConfig;
import com.zwei.iot.alarm.channel.config.SmsConfig;
import com.zwei.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 从 sys_config 加载 SMS/EMAIL 配置（带缓存）
 *
 * sys_config key 列表：
 *   notify.sms.access-key-id
 *   notify.sms.access-key-secret
 *   notify.sms.sign-name
 *   notify.sms.template.alarm
 *   notify.sms.template.offline
 *   notify.mail.host
 *   notify.mail.port
 *   notify.mail.username
 *   notify.mail.password
 *   notify.mail.from
 *   notify.mail.ssl
 */
@Component
public class NotifyConfigLoader {

    @Autowired
    private ISysConfigService sysConfigService;

    @Cacheable(value = "notify:config", key = "'sms'")
    public SmsConfig loadSmsConfig() {
        return SmsConfig.builder()
            .accessKeyId(get("notify.sms.access-key-id"))
            .accessKeySecret(get("notify.sms.access-key-secret"))
            .signName(get("notify.sms.sign-name"))
            .alarmTemplateCode(get("notify.sms.template.alarm"))
            .offlineTemplateCode(get("notify.sms.template.offline"))
            .build();
    }

    @Cacheable(value = "notify:config", key = "'mail'")
    public MailConfig loadMailConfig() {
        String portStr = get("notify.mail.port");
        String sslStr = get("notify.mail.ssl");
        return MailConfig.builder()
            .host(get("notify.mail.host"))
            .port(StringUtils.isNotBlank(portStr) ? Integer.valueOf(portStr) : null)
            .username(get("notify.mail.username"))
            .password(get("notify.mail.password"))
            .from(get("notify.mail.from"))
            .ssl(StringUtils.isNotBlank(sslStr) ? Boolean.valueOf(sslStr) : true)
            .build();
    }

    /** 清除全部 notify 配置缓存（配置变更时调用） */
    @CacheEvict(value = "notify:config", allEntries = true)
    public void evictAll() {}

    private String get(String key) {
        String v = sysConfigService.selectConfigByKey(key);
        return v;
    }

    // Lombok 已经在 SmsConfig/MailConfig 用了 @Builder，这里借用 commons-lang3
    // 为避免引入新 import，简化写法：
    private static class StringUtils {
        static boolean isNotBlank(String s) { return s != null && !s.trim().isEmpty(); }
    }
}
```

> 注：上面 `StringUtils` 内部类只是为了演示逻辑，实现时直接用 `org.apache.commons.lang3.StringUtils.isNotBlank(...)`。删掉内部类，加 `import org.apache.commons.lang3.StringUtils;`。

- [ ] **步骤 2：处理配置变更事件（可选）**

如果项目有 `SysConfigChangedEvent`（CLAUDE.md 提到的 NoticeCreatedEvent 模式），监听并清除缓存。如果没有，依赖 ISysConfigService 自身的缓存机制即可。

```java
// 在 NotifyConfigLoader 加（如有事件类）：
// @EventListener
// public void onConfigChanged(SysConfigChangedEvent event) {
//     if (event.getKey().startsWith("notify.")) evictAll();
// }
```

- [ ] **步骤 3：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/NotifyConfigLoader.java
git commit -m "feat(alarm): NotifyConfigLoader - sys_config 加载 + 缓存"
```

---

### 任务 7：编写 IAlarmRuleMatcher + 实现（TDD）

**文件：**
- 创建：`server/zwei-iot-alarm/.../service/IAlarmRuleMatcher.java`
- 创建：`.../service/impl/AlarmRuleMatcherImpl.java`
- 创建：`.../mapper/AlarmDispatchRuleMapper.java`（扩展方法）
- 创建：`.../resources/mapper/alarm/AlarmDispatchRuleMapper.xml`（扩展 SQL）
- 创建：`.../src/test/java/.../AlarmRuleMatcherImplTest.java`

- [ ] **步骤 1：编写测试（TDD 红灯）**

```java
package com.zwei.iot.alarm.dispatch;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.service.impl.AlarmRuleMatcherImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmRuleMatcherImplTest {

    @Mock private AlarmDispatchRuleMapper ruleMapper;
    @InjectMocks private AlarmRuleMatcherImpl matcher;

    @Test
    void matchAlarmRules_should_return_rules_with_matching_level_and_hp() {
        AlarmDispatchRule r1 = new AlarmDispatchRule();
        r1.setId(1L);
        when(ruleMapper.matchAlarmRules("2", "2"))
            .thenReturn(List.of(r1));

        List<AlarmDispatchRule> result = matcher.matchAlarmRules(2L, "2");
        assertThat(result).hasSize(1);
    }

    @Test
    void matchAlarmRules_empty_when_no_match() {
        when(ruleMapper.matchAlarmRules(anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        List<AlarmDispatchRule> result = matcher.matchAlarmRules(999L, "4");
        assertThat(result).isEmpty();
    }

    @Test
    void matchOfflineRules_should_return_rules_for_device() {
        AlarmDispatchRule r = new AlarmDispatchRule();
        r.setId(10L);
        when(ruleMapper.matchOfflineRules("10"))
            .thenReturn(List.of(r));

        List<AlarmDispatchRule> result = matcher.matchOfflineRules(10L);
        assertThat(result).hasSize(1);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=AlarmRuleMatcherImplTest
```

预期：编译失败。

- [ ] **步骤 3：编写接口**

```java
package com.zwei.iot.alarm.dispatch.service;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;

import java.util.List;

public interface IAlarmRuleMatcher {

    /**
     * 告警事件：匹配 ALARM 类型 + 等级匹配 + 隐患点匹配（含 '*'）
     */
    List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel);

    /**
     * 设备离线：匹配 OFFLINE 类型 + 设备匹配（含 '*'）
     */
    List<AlarmDispatchRule> matchOfflineRules(Long deviceId);
}
```

- [ ] **步骤 4：编写实现**

```java
package com.zwei.iot.alarm.dispatch.service.impl;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.service.IAlarmRuleMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlarmRuleMatcherImpl implements IAlarmRuleMatcher {

    @Autowired
    private AlarmDispatchRuleMapper ruleMapper;

    @Override
    public List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel) {
        return ruleMapper.matchAlarmRules(
            hazardPointId == null ? null : String.valueOf(hazardPointId),
            alarmLevel);
    }

    @Override
    public List<AlarmDispatchRule> matchOfflineRules(Long deviceId) {
        return ruleMapper.matchOfflineRules(
            deviceId == null ? null : String.valueOf(deviceId));
    }
}
```

- [ ] **步骤 5：扩展 Mapper + XML**

在 `AlarmDispatchRuleMapper.java` 接口加：

```java
List<AlarmDispatchRule> matchAlarmRules(@Param("hazardPointIdStr") String hazardPointIdStr,
                                         @Param("alarmLevel") String alarmLevel);

List<AlarmDispatchRule> matchOfflineRules(@Param("deviceIdStr") String deviceIdStr);
```

在 `AlarmDispatchRuleMapper.xml` 追加：

```xml
<select id="matchAlarmRules" resultType="com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule">
    SELECT DISTINCT r.id, r.name, r.event_type, r.alarm_levels, r.channels,
           r.is_enabled, r.del_flag, r.create_by, r.create_time,
           r.update_by, r.update_time, r.remark
    FROM alarm_dispatch_rule r
    LEFT JOIN alarm_dispatch_rule_hazard_point hp ON hp.rule_id = r.id
    WHERE r.del_flag = 0
      AND r.is_enabled = 1
      AND r.event_type = 'ALARM'
      AND FIND_IN_SET(#{alarmLevel}, r.alarm_levels)
      AND (
          hp.hazard_point_id = '*'
          OR hp.hazard_point_id = #{hazardPointIdStr}
      )
</select>

<select id="matchOfflineRules" resultType="com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule">
    SELECT DISTINCT r.id, r.name, r.event_type, r.alarm_levels, r.channels,
           r.is_enabled, r.del_flag, r.create_by, r.create_time,
           r.update_by, r.update_time, r.remark
    FROM alarm_dispatch_rule r
    LEFT JOIN alarm_dispatch_rule_device d ON d.rule_id = r.id
    WHERE r.del_flag = 0
      AND r.is_enabled = 1
      AND r.event_type = 'OFFLINE'
      AND (
          d.device_id = '*'
          OR d.device_id = #{deviceIdStr}
      )
</select>
```

- [ ] **步骤 6：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=AlarmRuleMatcherImplTest
```

预期：3 个测试 PASS。

- [ ] **步骤 7：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/IAlarmRuleMatcher.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmRuleMatcherImpl.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/mapper/AlarmDispatchRuleMapper.java
git add server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmDispatchRuleMapper.xml
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/dispatch/AlarmRuleMatcherImplTest.java
git commit -m "feat(alarm): IAlarmRuleMatcher - 规则匹配引擎 (TDD)"
```

---

### 任务 8：编写接收人展开（TDD）

**文件：**
- 创建：`server/zwei-iot-alarm/.../mapper/AlarmRecipientQueryMapper.java` + xml
- 创建：`.../service/IAlarmRecipientResolver.java`
- 创建：`.../service/impl/AlarmRecipientResolverImpl.java`
- 创建：`.../src/test/java/.../AlarmRecipientResolverImplTest.java`

- [ ] **步骤 1：编写测试（TDD 红灯）**

```java
package com.zwei.iot.alarm.dispatch;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmRecipientQueryMapper;
import com.zwei.iot.alarm.dispatch.service.impl.AlarmRecipientResolverImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmRecipientResolverImplTest {

    @Mock private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Mock private AlarmRecipientQueryMapper queryMapper;
    @InjectMocks private AlarmRecipientResolverImpl resolver;

    @Test
    void resolveUserIds_with_role_specific_ids() {
        when(recipientMapper.selectByRuleId(1L)).thenReturn(List.of(
            buildRecip("ROLE", "1"),
            buildRecip("ROLE", "2")
        ));
        when(queryMapper.selectUserIdsByRoleIds(Arrays.asList("1", "2")))
            .thenReturn(Arrays.asList(10L, 20L, 30L));

        Set<Long> result = resolver.resolveUserIds(1L);

        assertThat(result).containsExactlyInAnyOrder(10L, 20L, 30L);
    }

    @Test
    void resolveUserIds_with_role_wildcard_returns_all_users() {
        when(recipientMapper.selectByRuleId(2L)).thenReturn(List.of(
            buildRecip("ROLE", "*")
        ));
        when(queryMapper.selectAllActiveUserIds()).thenReturn(Arrays.asList(1L, 2L, 3L));

        Set<Long> result = resolver.resolveUserIds(2L);

        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void resolveUserIds_mixed_types_deduplicates() {
        when(recipientMapper.selectByRuleId(3L)).thenReturn(List.of(
            buildRecip("USER", "1"),
            buildRecip("USER", "2"),
            buildRecip("DEPT", "100")
        ));
        // dept 100 的用户含 id=1
        when(queryMapper.selectUserIdsByDeptIds(List.of("100")))
            .thenReturn(Arrays.asList(1L, 5L));

        Set<Long> result = resolver.resolveUserIds(3L);

        // 1 来自 USER 和 DEPT，应去重
        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 5L);
    }

    private AlarmDispatchRuleRecipient buildRecip(String type, String id) {
        AlarmDispatchRuleRecipient r = new AlarmDispatchRuleRecipient();
        r.setRuleId(0L);
        r.setRecipientType(type);
        r.setRecipientId(id);
        return r;
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=AlarmRecipientResolverImplTest
```

预期：编译失败。

- [ ] **步骤 3：编写 AlarmRecipientQueryMapper 接口**

```java
package com.zwei.iot.alarm.dispatch.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 跨模块查 sys_user / sys_user_role（只读，不破坏 RBAC 表）
 */
@Mapper
public interface AlarmRecipientQueryMapper {

    /** 按角色 ID 查活跃用户 */
    List<Long> selectUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    /** 按部门 ID 查活跃用户 */
    List<Long> selectUserIdsByDeptIds(@Param("deptIds") List<String> deptIds);

    /** 全部活跃用户（'*' 通配时用） */
    List<Long> selectAllActiveUserIds();
}
```

- [ ] **步骤 4：编写 AlarmRecipientQueryMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.dispatch.mapper.AlarmRecipientQueryMapper">

    <select id="selectUserIdsByRoleIds" resultType="long">
        SELECT DISTINCT ur.user_id
        FROM sys_user_role ur
        JOIN sys_user u ON u.user_id = ur.user_id
        WHERE u.del_flag = '0'
          AND u.status = '0'
          AND ur.role_id IN
          <foreach collection="roleIds" item="id" open="(" separator="," close=")">
              #{id}
          </foreach>
    </select>

    <select id="selectUserIdsByDeptIds" resultType="long">
        SELECT user_id
        FROM sys_user
        WHERE del_flag = '0'
          AND status = '0'
          AND dept_id IN
          <foreach collection="deptIds" item="id" open="(" separator="," close=")">
              #{id}
          </foreach>
    </select>

    <select id="selectAllActiveUserIds" resultType="long">
        SELECT user_id
        FROM sys_user
        WHERE del_flag = '0'
          AND status = '0'
    </select>

</mapper>
```

- [ ] **步骤 5：编写 IAlarmRecipientResolver 接口**

```java
package com.zwei.iot.alarm.dispatch.service;

import java.util.Set;

public interface IAlarmRecipientResolver {

    /**
     * 把规则里的接收人配置展开为去重后的 userId 列表
     * ROLE: 查 sys_user_role（'*' 则所有活跃用户）
     * DEPT: 查 sys_user.dept_id（'*' 则所有活跃用户）
     * USER: 直接用（'*' 则所有活跃用户）
     */
    Set<Long> resolveUserIds(Long ruleId);
}
```

- [ ] **步骤 6：编写实现**

```java
package com.zwei.iot.alarm.dispatch.service.impl;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmRecipientQueryMapper;
import com.zwei.iot.alarm.dispatch.service.IAlarmRecipientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlarmRecipientResolverImpl implements IAlarmRecipientResolver {

    private static final String WILDCARD = "*";

    @Autowired private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Autowired private AlarmRecipientQueryMapper queryMapper;

    @Override
    public Set<Long> resolveUserIds(Long ruleId) {
        List<AlarmDispatchRuleRecipient> recips = recipientMapper.selectByRuleId(ruleId);
        if (recips == null || recips.isEmpty()) return Collections.emptySet();

        // 按类型分组
        Map<String, List<String>> byType = recips.stream()
            .collect(Collectors.groupingBy(
                AlarmDispatchRuleRecipient::getRecipientType,
                Collectors.mapping(
                    AlarmDispatchRuleRecipient::getRecipientId,
                    Collectors.toList())));

        Set<Long> userIds = new HashSet<>();

        // ROLE
        List<String> roleIds = byType.getOrDefault("ROLE", Collections.emptyList());
        if (roleIds.contains(WILDCARD)) {
            userIds.addAll(queryMapper.selectAllActiveUserIds());
        } else if (!roleIds.isEmpty()) {
            userIds.addAll(queryMapper.selectUserIdsByRoleIds(roleIds));
        }

        // DEPT
        List<String> deptIds = byType.getOrDefault("DEPT", Collections.emptyList());
        if (deptIds.contains(WILDCARD)) {
            userIds.addAll(queryMapper.selectAllActiveUserIds());
        } else if (!deptIds.isEmpty()) {
            userIds.addAll(queryMapper.selectUserIdsByDeptIds(deptIds));
        }

        // USER
        List<String> userIdsStr = byType.getOrDefault("USER", Collections.emptyList());
        if (userIdsStr.contains(WILDCARD)) {
            userIds.addAll(queryMapper.selectAllActiveUserIds());
        } else {
            userIdsStr.stream()
                .filter(Objects::nonNull)
                .map(s -> {
                    try { return Long.parseLong(s); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        }

        return userIds;
    }
}
```

- [ ] **步骤 7：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=AlarmRecipientResolverImplTest
```

预期：3 个测试 PASS。

- [ ] **步骤 8：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/mapper/AlarmRecipientQueryMapper.java
git add server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmRecipientQueryMapper.xml
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/IAlarmRecipientResolver.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/service/impl/AlarmRecipientResolverImpl.java
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/dispatch/AlarmRecipientResolverImplTest.java
git commit -m "feat(alarm): IAlarmRecipientResolver - 接收人展开 (TDD)"
```

---

### 任务 9：编写 INotifyChannel + NotifyChannelDispatcher

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/INotifyChannel.java`
- 创建：`.../channel/NotifyChannelDispatcher.java`

- [ ] **步骤 1：编写 INotifyChannel 接口**

```java
package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.notify.domain.AlarmNotification;

/**
 * 通知渠道策略接口
 *
 * 实现类：
 *   - SystemNotifyChannel: SSE 推送 + 落库
 *   - SmsNotifyChannel:    阿里云 SMS
 *   - EmailNotifyChannel:  SMTP 邮件
 */
public interface INotifyChannel {

    /** 渠道标识：SYSTEM / SMS / EMAIL */
    String getChannel();

    /**
     * 实际发送 + 状态回写
     *
     * 实现内部应：
     *   - 成功：notificationService.markSent(id)
     *   - 失败：notificationService.markFailed(id, errorCode, description)
     */
    void send(AlarmNotification notification);
}
```

> 注：`AlarmNotification` 包路径按项目实际调整。

- [ ] **步骤 2：编写 NotifyChannelDispatcher**

```java
package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.notify.domain.AlarmNotification;
import com.zwei.iot.alarm.notify.service.IAlarmNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 渠道路由分发器：按 channel 字符串路由到对应 INotifyChannel 实现
 */
@Component
public class NotifyChannelDispatcher {

    private final Map<String, INotifyChannel> channelMap;
    private final IAlarmNotificationService notificationService;

    @Autowired
    public NotifyChannelDispatcher(
            List<INotifyChannel> channels,
            IAlarmNotificationService notificationService) {
        this.notificationService = notificationService;
        this.channelMap = channels.stream()
            .collect(Collectors.toMap(INotifyChannel::getChannel, c -> c));
    }

    public void dispatch(AlarmNotification n) {
        INotifyChannel ch = channelMap.get(n.getChannel());
        if (ch == null) {
            notificationService.markFailed(n.getId(), "UNKNOWN_CHANNEL",
                "未知渠道: " + n.getChannel());
            return;
        }
        ch.send(n);
    }
}
```

- [ ] **步骤 3：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS（无 INotifyChannel 实现类也能编译，因为 List<INotifyChannel> 可以为空）。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/INotifyChannel.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/NotifyChannelDispatcher.java
git commit -m "feat(alarm): INotifyChannel 接口 + 路由分发器"
```

---

### 任务 10：编写 SystemNotifyChannel

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/SystemNotifyChannel.java`

> 注：复用现有 `AlarmStreamPublisher`（SSE 推送给用户）。

- [ ] **步骤 1：编写实现**

```java
package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.notify.domain.AlarmNotification;
import com.zwei.iot.alarm.notify.service.IAlarmNotificationService;
import com.zwei.iot.alarm.notify.stream.AlarmStreamPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * SYSTEM 渠道：SSE 推送 + 落库
 *
 * - 不校验接收人（站内消息一定可达）
 * - 用户不在线也算发送成功（read_time 在用户点击消息中心"标记已读"时更新）
 */
@Slf4j
@Component
public class SystemNotifyChannel implements INotifyChannel {

    @Autowired private AlarmStreamPublisher alarmStreamPublisher;
    @Autowired private IAlarmNotificationService notificationService;

    @Override
    public String getChannel() {
        return "SYSTEM";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(AlarmNotification n) {
        try {
            // 1. SSE 实时推送（在线用户立刻收到）
            //    即使推送失败（用户不在线），也算"已落库可查"，不影响状态
            alarmStreamPublisher.publishToUser(
                n.getRecipientId(),
                buildPayload(n));

            // 2. 标记为已发送
            notificationService.markSent(n.getId());

        } catch (Exception e) {
            log.error("SYSTEM 渠道发送失败 notifId={} recipientId={}",
                n.getId(), n.getRecipientId(), e);
            notificationService.markFailed(n.getId(), "UNKNOWN",
                "[UNKNOWN] SSE 推送异常: " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
        }
    }

    private SsePayload buildPayload(AlarmNotification n) {
        SsePayload p = new SsePayload();
        p.setType("alarm-notify");
        p.setData(new SsePayload.Data(
            n.getId(),
            n.getSourceType(),
            n.getSourceId(),
            n.getTitle(),
            n.getContent(),
            n.getCreateTime()
        ));
        return p;
    }

    /** SSE 消息体（与前端 layout/index.vue onMessage 约定） */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SsePayload {
        private String type;
        private Data data;

        @lombok.Data
        @lombok.AllArgsConstructor
        @lombok.NoArgsConstructor
        public static class Data {
            private Long id;
            private String sourceType;
            private Long sourceId;
            private String title;
            private String content;
            private java.util.Date createTime;
        }
    }
}
```

> 注：`AlarmStreamPublisher.publishToUser(Long userId, Object payload)` 的方法签名按项目实际调整。原项目可能方法是 `publish(Long userId, String eventType, Object data)`，实现时核对。

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/SystemNotifyChannel.java
git commit -m "feat(alarm): SystemNotifyChannel - SSE 推送 + 落库"
```

---

### 任务 11：编写 AliyunSmsClient + SmsNotifyChannel

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/AliyunSmsClient.java`
- 创建：`.../channel/SmsNotifyChannel.java`

- [ ] **步骤 1：编写 AliyunSmsClient**

```java
package com.zwei.iot.alarm.channel;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.zwei.iot.alarm.channel.config.SmsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 阿里云短信客户端（懒加载单例，凭证变更时重建）
 */
@Slf4j
@Component
public class AliyunSmsClient {

    private volatile Client client;
    private volatile String cachedKey;   // accessKeyId 标识，变更时重建 client

    /**
     * 发送短信
     */
    public SendSmsResponse send(String phone, String templateCode,
                                Map<String, String> templateParams,
                                SmsConfig cfg) throws Exception {
        Client c = getOrBuildClient(cfg);

        SendSmsRequest req = new SendSmsRequest()
            .setPhoneNumbers(phone)
            .setSignName(cfg.getSignName())
            .setTemplateCode(templateCode);

        if (templateParams != null && !templateParams.isEmpty()) {
            req.setTemplateParam(com.alibaba.fastjson2.JSON.toJSONString(templateParams));
        }

        return c.sendSms(req);
    }

    private Client getOrBuildClient(SmsConfig cfg) throws Exception {
        if (!cfg.isConfigured()) {
            throw new IllegalStateException("阿里云 SMS 配置不完整");
        }
        if (client != null && Objects.equals(cachedKey, cfg.getAccessKeyId())) {
            return client;
        }
        synchronized (this) {
            if (client == null || !Objects.equals(cachedKey, cfg.getAccessKeyId())) {
                Config config = new Config()
                    .setAccessKeyId(cfg.getAccessKeyId())
                    .setAccessKeySecret(cfg.getAccessKeySecret())
                    .setEndpoint("dysmsapi.aliyuncs.com");
                client = new Client(config);
                cachedKey = cfg.getAccessKeyId();
                log.info("阿里云 SMS 客户端已重建 accessKeyId={}",
                    maskKey(cfg.getAccessKeyId()));
            }
        }
        return client;
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 6) return "***";
        return key.substring(0, 4) + "***" + key.substring(key.length() - 2);
    }
}
```

> 注：JSON 库按项目实际选 FastJSON2（`com.alibaba.fastjson2.JSON`）或 Jackson。

- [ ] **步骤 2：编写 SmsNotifyChannel**

```java
package com.zwei.iot.alarm.channel;

import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.zwei.iot.alarm.channel.config.SmsConfig;
import com.zwei.iot.alarm.notify.domain.AlarmNotification;
import com.zwei.iot.alarm.notify.service.IAlarmNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SMS 渠道（阿里云）
 */
@Slf4j
@Component
public class SmsNotifyChannel implements INotifyChannel {

    @Autowired private AliyunSmsClient aliyunSmsClient;
    @Autowired private NotifyConfigLoader configLoader;
    @Autowired private NotifyTemplateService templateService;
    @Autowired private IAlarmNotificationService notificationService;

    @Override
    public String getChannel() {
        return "SMS";
    }

    @Override
    public void send(AlarmNotification n) {
        // 1) 接收人校验
        String recipientErr = NotifyRecipientValidator.validatePhone(n.getRecipientPhone());
        if (recipientErr != null) {
            notificationService.markFailed(n.getId(), recipientErr,
                String.format("用户 %s 手机号无效: %s",
                    n.getRecipientName(),
                    StringUtils.defaultString(n.getRecipientPhone(), "(空)")));
            return;
        }

        // 2) 渠道配置校验
        SmsConfig cfg = configLoader.loadSmsConfig();
        String cfgErr = cfg.validate();
        if (cfgErr != null) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "[CHANNEL_NOT_CONFIGURED] " + cfgErr);
            return;
        }

        // 3) 模板参数
        Map<String, String> templateParams = templateService.buildSmsParams(n);
        String templateCode = cfg.selectTemplateCode(n.getSourceType());
        if (StringUtils.isBlank(templateCode)) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "[CHANNEL_NOT_CONFIGURED] sourceType=" + n.getSourceType()
                + " 的短信模板Code未配置");
            return;
        }

        // 4) 调用阿里云
        try {
            SendSmsResponse resp = aliyunSmsClient.send(
                n.getRecipientPhone(), templateCode, templateParams, cfg);

            if (resp == null || resp.getBody() == null) {
                notificationService.markFailed(n.getId(), "PROVIDER_ERROR",
                    "[PROVIDER_ERROR] 阿里云返回空响应");
                return;
            }

            String code = resp.getBody().getCode();
            if ("OK".equals(code)) {
                notificationService.markSent(n.getId());
            } else {
                // 业务错误（如 isv.MOBILE_NUMBER_ILLEGAL、isv.BUSINESS_LIMIT_CONTROL）
                notificationService.markFailed(n.getId(), "PROVIDER_ERROR",
                    "[PROVIDER_ERROR] 阿里云: " + code
                    + " - " + resp.getBody().getMessage());
            }
        } catch (Exception e) {
            log.error("SMS 发送异常 notifId={} phone={}",
                n.getId(), n.getRecipientPhone(), e);
            notificationService.markFailed(n.getId(), "NETWORK_ERROR",
                "[NETWORK_ERROR] " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
        }
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
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/AliyunSmsClient.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/SmsNotifyChannel.java
git commit -m "feat(alarm): AliyunSmsClient + SmsNotifyChannel"
```

---

### 任务 12：编写 DynamicMailSender + EmailNotifyChannel

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/DynamicMailSender.java`
- 创建：`.../channel/EmailNotifyChannel.java`

- [ ] **步骤 1：编写 DynamicMailSender**

```java
package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.channel.config.MailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Properties;

/**
 * 动态 JavaMailSender（基于 sys_config 配置实时构建，不走 spring.mail.* 自动配置）
 */
@Slf4j
@Component
public class DynamicMailSender {

    private volatile JavaMailSenderImpl sender;
    private volatile String cachedKey;   // host + username 标识

    /**
     * 发送 HTML 邮件
     */
    public void send(String to, String subject, String htmlBody, MailConfig cfg)
            throws MessagingException {
        JavaMailSenderImpl s = getOrBuild(cfg);
        MimeMessage mime = s.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(cfg.getFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);   // HTML
        s.send(mime);
    }

    private JavaMailSenderImpl getOrBuild(MailConfig cfg) {
        if (!cfg.isConfigured()) {
            throw new IllegalStateException("SMTP 配置不完整");
        }
        String key = cfg.getHost() + ":" + cfg.getUsername();
        if (sender != null && Objects.equals(cachedKey, key)) {
            return sender;
        }
        synchronized (this) {
            if (sender == null || !Objects.equals(cachedKey, key)) {
                JavaMailSenderImpl s = new JavaMailSenderImpl();
                s.setHost(cfg.getHost());
                s.setPort(cfg.getPort());
                s.setUsername(cfg.getUsername());
                s.setPassword(cfg.getPassword());
                s.setDefaultEncoding("UTF-8");

                Properties props = s.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.ssl.enable", String.valueOf(cfg.isSsl()));
                props.put("mail.smtp.starttls.enable", String.valueOf(!cfg.isSsl()));
                props.put("mail.smtp.socketFactory.class",
                    cfg.isSsl() ? "jakarta.net.ssl.SSLSocketFactory" : "");
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.put("mail.smtp.timeout", "5000");
                props.put("mail.smtp.connectiontimeout", "5000");
                props.put("mail.smtp.writetimeout", "5000");

                sender = s;
                cachedKey = key;
                log.info("JavaMailSender 已重建 host={} username={}",
                    cfg.getHost(), cfg.getUsername());
            }
        }
        return sender;
    }
}
```

> 注：`jakarta.net.ssl.SSLSocketFactory` 应为 `javax.net.ssl.SSLSocketFactory`（按 Spring Boot 4 / Jakarta Mail 实际包路径调整）。Spring Boot 4 使用 Jakarta Mail，包名是 `jakarta.mail.*`，但 SSLSocketFactory 仍来自 JDK `javax.net.ssl.*`。实现时核对。

- [ ] **步骤 2：编写 EmailNotifyChannel**

```java
package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.channel.config.MailConfig;
import com.zwei.iot.alarm.notify.domain.AlarmNotification;
import com.zwei.iot.alarm.notify.service.IAlarmNotificationService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Component;

/**
 * EMAIL 渠道（SMTP + Thymeleaf）
 */
@Slf4j
@Component
public class EmailNotifyChannel implements INotifyChannel {

    @Autowired private DynamicMailSender dynamicMailSender;
    @Autowired private NotifyConfigLoader configLoader;
    @Autowired private NotifyTemplateService templateService;
    @Autowired private IAlarmNotificationService notificationService;

    @Override
    public String getChannel() {
        return "EMAIL";
    }

    @Override
    public void send(AlarmNotification n) {
        // 1) 接收人校验
        String err = NotifyRecipientValidator.validateEmail(n.getRecipientEmail());
        if (err != null) {
            notificationService.markFailed(n.getId(), err,
                String.format("用户 %s 邮箱无效: %s",
                    n.getRecipientName(),
                    StringUtils.defaultString(n.getRecipientEmail(), "(空)")));
            return;
        }

        // 2) SMTP 配置校验
        MailConfig cfg = configLoader.loadMailConfig();
        String cfgErr = cfg.validate();
        if (cfgErr != null) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "[CHANNEL_NOT_CONFIGURED] " + cfgErr);
            return;
        }

        // 3) 渲染并发送
        try {
            String subject = templateService.renderEmailSubject(n);
            String html = templateService.renderEmailHtml(n);

            dynamicMailSender.send(n.getRecipientEmail(), subject, html, cfg);
            notificationService.markSent(n.getId());

        } catch (MailAuthenticationException e) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "[CHANNEL_NOT_CONFIGURED] SMTP 认证失败: " + e.getMessage());
        } catch (MailSendException e) {
            notificationService.markFailed(n.getId(), "PROVIDER_ERROR",
                "[PROVIDER_ERROR] 邮件发送被拒: " + e.getMessage());
        } catch (MessagingException e) {
            notificationService.markFailed(n.getId(), "NETWORK_ERROR",
                "[NETWORK_ERROR] MessagingException: " + e.getMessage());
        } catch (Exception e) {
            log.error("EMAIL 发送异常 notifId={} email={}",
                n.getId(), n.getRecipientEmail(), e);
            notificationService.markFailed(n.getId(), "UNKNOWN",
                "[UNKNOWN] " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
        }
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
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/DynamicMailSender.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/EmailNotifyChannel.java
git commit -m "feat(alarm): DynamicMailSender + EmailNotifyChannel"
```

---

### 任务 13：编写 NotifyTemplateService + Thymeleaf 模板

**文件：**
- 创建：`server/zwei-iot-alarm/.../channel/NotifyContext.java`
- 创建：`.../channel/NotifyTemplateService.java`
- 创建：`.../resources/templates/mail/alarm-notify.html`

- [ ] **步骤 1：编写 NotifyContext**

```java
package com.zwei.iot.alarm.channel;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 通知模板变量上下文
 */
@Data
@Builder
public class NotifyContext {
    private String sourceType;        // alarm / offline
    private Long sourceId;
    private String hazardPointName;   // 告警时填
    private String deviceName;        // 告警/离线都可能填
    private String deviceCode;        // 离线时填
    private String alarmLevel;        // 告警时填（字典 label）
    private String alarmTitle;
    private Date eventTime;           // alarm_time / offline_time
    private Date lastReportTime;      // 离线时填
}
```

- [ ] **步骤 2：编写 Thymeleaf 模板**

`server/zwei-iot-alarm/src/main/resources/templates/mail/alarm-notify.html`：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title th:text="${subject}">通知</title>
</head>
<body style="font-family:'Microsoft YaHei',sans-serif;color:#333;margin:0;padding:0;background:#f5f5f5;">
  <div style="max-width:600px;margin:24px auto;background:#fff;border-radius:4px;overflow:hidden;
              box-shadow:0 2px 8px rgba(0,0,0,0.08);">
    <div th:style="${headerStyle}" style="padding:16px 24px;">
      <h2 style="margin:0;font-size:18px;" th:text="${title}">告警通知</h2>
    </div>
    <div style="padding:24px;">
      <p th:if="${hazardPointName != null}" style="margin:8px 0;">
        <strong>隐患点：</strong><span th:text="${hazardPointName}">-</span>
      </p>
      <p th:if="${deviceName != null}" style="margin:8px 0;">
        <strong>设备：</strong>
        <span th:text="${deviceName}">-</span>
        <span th:if="${deviceCode != null}" th:text="'（' + ${deviceCode} + '）'"></span>
      </p>
      <p th:if="${alarmLevel != null}" style="margin:8px 0;">
        <strong>等级：</strong><span th:text="${alarmLevel}">-</span>
      </p>
      <p th:if="${content != null}" style="margin:8px 0;">
        <strong>事件：</strong><span th:text="${content}">-</span>
      </p>
      <p th:if="${eventTime != null}" style="margin:8px 0;">
        <strong>时间：</strong><span th:text="${#dates.format(eventTime, 'yyyy-MM-dd HH:mm:ss')}">-</span>
      </p>
      <p th:if="${lastReportTime != null}" style="margin:8px 0;color:#909399;font-size:13px;">
        最后上报：<span th:text="${#dates.format(lastReportTime, 'yyyy-MM-dd HH:mm:ss')}">-</span>
      </p>
      <a th:if="${linkUrl != null}" th:href="${linkUrl}"
         style="display:inline-block;margin-top:16px;padding:10px 24px;
                background:#409eff;color:#fff;text-decoration:none;border-radius:4px;">
        查看详情
      </a>
    </div>
    <div style="padding:12px 24px;background:#f9f9f9;font-size:12px;color:#909399;">
      知微地质灾害监测预警系统 · 此邮件由系统自动发送，请勿回复
    </div>
  </div>
</body>
</html>
```

- [ ] **步骤 3：编写 NotifyTemplateService**

```java
package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.notify.domain.AlarmNotification;
import com.zwei.iot.alarm.notify.domain.AlarmRecord;
import com.zwei.iot.alarm.notify.service.IAlarmRecordService;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.device.service.IDeviceQueryService;
import com.zwei.system.service.ISysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知模板渲染服务
 *
 * 三渠道格式：
 *   SYSTEM: 简洁标题 + 内容
 *   SMS:    阿里云模板参数 (name/level/content/time)
 *   EMAIL:  HTML (alarm-notify.html)
 */
@Service
public class NotifyTemplateService {

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired private TemplateEngine templateEngine;
    @Autowired private IHazardPointQueryService hazardPointQueryService;
    @Autowired private IDeviceQueryService deviceQueryService;
    @Autowired private ISysDictDataService sysDictDataService;
    @Autowired private IAlarmRecordService alarmRecordService;

    /**
     * 构造模板上下文（从 AlarmNotification 反查业务数据）
     */
    public NotifyContext buildContext(AlarmNotification n) {
        NotifyContext.NotifyContextBuilder b = NotifyContext.builder()
            .sourceType(n.getSourceType())
            .sourceId(n.getSourceId())
            .alarmTitle(n.getTitle());

        if ("alarm".equalsIgnoreCase(n.getSourceType())) {
            // 查告警记录
            AlarmRecord record = alarmRecordService.selectById(n.getSourceId());
            if (record != null) {
                b.eventTime(record.getAlarmTime());
                b.alarmLevel(dictLabel("alarm_level", String.valueOf(record.getAlarmLevel())));
                if (record.getHazardPointId() != null) {
                    b.hazardPointName(hazardPointQueryService.selectNameById(record.getHazardPointId()));
                }
                if (record.getDeviceId() != null) {
                    b.deviceName(deviceQueryService.selectNameById(record.getDeviceId()));
                }
            }
        } else if ("offline".equalsIgnoreCase(n.getSourceType())) {
            // 查设备
            com.zwei.iot.device.domain.Device device = deviceQueryService.selectById(n.getSourceId());
            if (device != null) {
                b.deviceName(device.getName());
                b.deviceCode(device.getCode());
                b.eventTime(new Date());   // 离线时间（事件触发时刻）
            }
        }

        return b.build();
    }

    /**
     * 构造 SMS 模板参数（阿里云 ${var} 占位符）
     */
    public Map<String, String> buildSmsParams(AlarmNotification n) {
        NotifyContext ctx = buildContext(n);
        Map<String, String> params = new HashMap<>();
        params.put("name", defaultStr(ctx.getHazardPointName(), ctx.getDeviceName()));
        params.put("level", defaultStr(ctx.getAlarmLevel(), "-"));
        params.put("content", defaultStr(ctx.getAlarmTitle(), "-"));
        params.put("time", ctx.getEventTime() != null ? DATE_FMT.format(ctx.getEventTime()) : "-");
        return params;
    }

    /**
     * 渲染邮件主题
     */
    public String renderEmailSubject(AlarmNotification n) {
        NotifyContext ctx = buildContext(n);
        if ("offline".equalsIgnoreCase(n.getSourceType())) {
            return "[知微] 设备离线：" + defaultStr(ctx.getDeviceName(), "-");
        }
        return "[知微告警] "
            + defaultStr(ctx.getHazardPointName(), "-")
            + " - " + defaultStr(ctx.getAlarmTitle(), "-");
    }

    /**
     * 渲染邮件 HTML
     */
    public String renderEmailHtml(AlarmNotification n) {
        NotifyContext ctx = buildContext(n);
        Context ctxTpl = new Context();
        ctxTpl.setVariable("title", n.getTitle());
        ctxTpl.setVariable("subject", renderEmailSubject(n));
        ctxTpl.setVariable("hazardPointName", ctx.getHazardPointName());
        ctxTpl.setVariable("deviceName", ctx.getDeviceName());
        ctxTpl.setVariable("deviceCode", ctx.getDeviceCode());
        ctxTpl.setVariable("alarmLevel", ctx.getAlarmLevel());
        ctxTpl.setVariable("content", ctx.getAlarmTitle());
        ctxTpl.setVariable("eventTime", ctx.getEventTime());
        ctxTpl.setVariable("lastReportTime", ctx.getLastReportTime());
        ctxTpl.setVariable("linkUrl", buildLinkUrl(ctx));
        ctxTpl.setVariable("headerStyle",
            "alarm".equalsIgnoreCase(ctx.getSourceType())
                ? "background:#f56c6c;color:#fff;"
                : "background:#e6a23c;color:#fff;");

        return templateEngine.process("alarm-notify", ctxTpl);
    }

    private String buildLinkUrl(NotifyContext ctx) {
        if (ctx.getSourceId() == null) return null;
        if ("alarm".equalsIgnoreCase(ctx.getSourceType())) {
            return "/alarm/realtime?alarmId=" + ctx.getSourceId();
        }
        return "/basic/device?deviceId=" + ctx.getSourceId();
    }

    private String dictLabel(String dictType, String dictValue) {
        String label = sysDictDataService.selectDictLabel(dictType, dictValue);
        return label != null ? label : dictValue;
    }

    private String defaultStr(String s, String defaultVal) {
        return (s == null || s.isEmpty()) ? defaultVal : s;
    }
}
```

> 注：`IHazardPointQueryService.selectNameById`、`IDeviceQueryService.selectNameById/selectById`、`IAlarmRecordService.selectById` 等方法如不存在，实现时在对应 Service 接口扩展（在计划 A 完成后这些 Service 已可用）。

- [ ] **步骤 4：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/NotifyContext.java
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/channel/NotifyTemplateService.java
git add server/zwei-iot-alarm/src/main/resources/templates/mail/alarm-notify.html
git commit -m "feat(alarm): NotifyTemplateService + 邮件 Thymeleaf 模板"
```

---

### 任务 14：编写 AlarmNotifyAsyncConfig

**文件：**
- 创建：`server/zwei-iot-alarm/.../config/AlarmNotifyAsyncConfig.java`

- [ ] **步骤 1：编写配置**

```java
package com.zwei.iot.alarm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 告警通知异步执行器
 */
@Configuration
@EnableAsync
public class AlarmNotifyAsyncConfig {

    @Bean("alarmNotifyExecutor")
    public Executor alarmNotifyExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(500);
        exec.setKeepAliveSeconds(60);
        exec.setThreadNamePrefix("alarm-notify-");
        // 队列满时由调用线程同步执行（避免丢任务）
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.initialize();
        return exec;
    }
}
```

> 注：如果项目主应用类已经标 `@EnableAsync`，这里不用重复。可改为只在配置类声明 Bean。

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/config/AlarmNotifyAsyncConfig.java
git commit -m "feat(alarm): alarmNotifyExecutor 异步执行器"
```

---

### 任务 15：改造 AlarmNotifier（双事件监听 + 去重）

**文件：**
- 修改：`server/zwei-iot-alarm/.../AlarmNotifier.java`（或对应路径）

> 注：先 Read 现有 `AlarmNotifier.java` 确认包路径与现有逻辑，本任务整体重写。

- [ ] **步骤 1：备份原文件**

```bash
cp server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/notify/AlarmNotifier.java \
   server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/notify/AlarmNotifier.java.bak
```

> 包路径按项目实际调整。

- [ ] **步骤 2：重写 AlarmNotifier**

```java
package com.zwei.iot.alarm.notify;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.iot.alarm.channel.NotifyChannelDispatcher;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.service.IAlarmRecipientResolver;
import com.zwei.iot.alarm.dispatch.service.IAlarmRuleMatcher;
import com.zwei.iot.alarm.notify.domain.AlarmNotification;
import com.zwei.iot.alarm.notify.service.IAlarmNotificationService;
import com.zwei.system.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * 告警通知分发器
 *
 * 监听：
 *   - AlarmTriggeredEvent  (告警触发)
 *   - DeviceOfflineEvent   (设备离线)
 *
 * 流程：
 *   事件 → 匹配规则 → 展开接收人 → 笛卡尔(userId × channel) 去重 → 批量落库 → 分发到渠道
 */
@Slf4j
@Component
public class AlarmNotifier {

    @Autowired private IAlarmRuleMatcher ruleMatcher;
    @Autowired private IAlarmRecipientResolver recipientResolver;
    @Autowired private IAlarmNotificationService notificationService;
    @Autowired private NotifyChannelDispatcher channelDispatcher;
    @Autowired private ISysUserService userService;

    // ============= 告警事件 =============
    @EventListener
    @Async("alarmNotifyExecutor")
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        try {
            log.info("收到告警事件 alarmId={} hazardPointId={} level={}",
                event.getAlarmId(), event.getHazardPointId(), event.getAlarmLevel());
            dispatchForAlarm(event);
        } catch (Exception e) {
            log.error("告警通知处理失败 alarmId={}", event.getAlarmId(), e);
        }
    }

    // ============= 设备离线事件 =============
    @EventListener
    @Async("alarmNotifyExecutor")
    public void onDeviceOffline(DeviceOfflineEvent event) {
        try {
            log.info("收到设备离线事件 deviceId={}", event.getDeviceId());
            dispatchForOffline(event);
        } catch (Exception e) {
            log.error("离线通知处理失败 deviceId={}", event.getDeviceId(), e);
        }
    }

    // ============= 告警分发 =============
    private void dispatchForAlarm(AlarmTriggeredEvent event) {
        List<AlarmDispatchRule> rules = ruleMatcher.matchAlarmRules(
            event.getHazardPointId(), String.valueOf(event.getAlarmLevel()));

        if (rules.isEmpty()) {
            log.debug("无匹配告警规则 alarmId={}", event.getAlarmId());
            return;
        }

        String title = "[告警] " + StringUtils.defaultString(event.getAlarmTitle(), "告警通知");
        String content = String.format("等级:%s | 隐患点:%s",
            event.getAlarmLevel(),
            StringUtils.defaultString(event.getHazardPointName(), "-"));

        Collection<AlarmNotification> notifications = buildAndDedup(
            rules, "alarm", event.getAlarmId(), title, content);

        dispatch(notifications);
    }

    // ============= 离线分发 =============
    private void dispatchForOffline(DeviceOfflineEvent event) {
        List<AlarmDispatchRule> rules = ruleMatcher.matchOfflineRules(event.getDeviceId());

        if (rules.isEmpty()) {
            log.debug("无匹配离线规则 deviceId={}", event.getDeviceId());
            return;
        }

        String title = "[设备离线] " + StringUtils.defaultString(event.getDeviceName(), "设备");
        String content = String.format("设备:%s | 时间:%s",
            StringUtils.defaultString(event.getDeviceName(), "-"),
            new Date());

        Collection<AlarmNotification> notifications = buildAndDedup(
            rules, "offline", event.getDeviceId(), title, content);

        dispatch(notifications);
    }

    // ============= 核心：构建 + 去重 =============
    private Collection<AlarmNotification> buildAndDedup(
            List<AlarmDispatchRule> rules,
            String sourceType,
            Long sourceId,
            String title,
            String content) {

        // 内存去重 key: userId + "|" + channel
        Map<String, AlarmNotification> dedup = new HashMap<>();

        for (AlarmDispatchRule rule : rules) {
            Set<Long> userIds = recipientResolver.resolveUserIds(rule.getId());
            if (userIds.isEmpty()) continue;

            Set<String> channels = parseChannels(rule.getChannels());
            if (channels.isEmpty()) continue;

            for (Long userId : userIds) {
                SysUser user = userService.selectUserById(userId);
                if (user == null) continue;
                if ("1".equals(user.getStatus())) continue;   // 0=正常 1=停用

                for (String channel : channels) {
                    String key = userId + "|" + channel;
                    if (dedup.containsKey(key)) continue;

                    AlarmNotification n = new AlarmNotification();
                    n.setSourceType(sourceType);
                    n.setSourceId(sourceId);
                    n.setAlarmId(sourceId);   // 兼容旧字段
                    n.setDispatchRuleId(rule.getId());
                    n.setRecipientId(userId);
                    n.setRecipientName(user.getUserName());
                    n.setRecipientPhone(user.getPhonenumber());
                    n.setRecipientEmail(user.getEmail());
                    n.setChannel(channel);
                    n.setTitle(title);
                    n.setContent(content);
                    n.setStatus(AlarmNotification.STATUS_PENDING);
                    dedup.put(key, n);
                }
            }
        }
        return dedup.values();
    }

    // ============= 分发 =============
    private void dispatch(Collection<AlarmNotification> notifications) {
        if (notifications.isEmpty()) return;

        // 1) 批量落库（带 uk_notif_dedup 兜底）
        try {
            notificationService.saveBatch(notifications);
        } catch (DuplicateKeyException e) {
            // 整批冲突 = 事件重放或并发触发，静默忽略
            log.warn("通知整批重复被忽略（事件已处理） sourceId 见日志", e);
            return;
        }

        // 2) 逐条分发到渠道
        for (AlarmNotification n : notifications) {
            try {
                channelDispatcher.dispatch(n);
            } catch (Exception e) {
                log.error("通知分发失败 notifId={} channel={}",
                    n.getId(), n.getChannel(), e);
                notificationService.markFailed(n.getId(), "UNKNOWN",
                    "[UNKNOWN] 分发异常: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            }
        }
    }

    private Set<String> parseChannels(String channelsCsv) {
        if (StringUtils.isBlank(channelsCsv)) return Collections.emptySet();
        Set<String> set = new LinkedHashSet<>();
        for (String c : channelsCsv.split(",")) {
            if (StringUtils.isNotBlank(c)) set.add(c.trim());
        }
        return set;
    }
}
```

> 注：
> 1. `AlarmTriggeredEvent` / `DeviceOfflineEvent` 的字段（如 `getAlarmId()`、`getHazardPointId()`、`getAlarmLevel()`、`getAlarmTitle()`、`getHazardPointName()`、`getDeviceId()`、`getDeviceName()`）按项目实际定义调整
> 2. `userService.selectUserById` 来自 RuoYi 标准 ISysUserService
> 3. `notificationService.saveBatch` 是 IAlarmNotificationService 已有方法

- [ ] **步骤 3：编译验证**

```bash
cd server && mvn compile -pl zwei-iot-alarm -am -q
```

预期：BUILD SUCCESS。

- [ ] **步骤 4：删除 .bak**

```bash
rm server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/notify/AlarmNotifier.java.bak
```

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/notify/AlarmNotifier.java
git commit -m "refactor(alarm): AlarmNotifier 双事件监听 + 用户×渠道去重"
```

---

### 任务 16：集成测试（事件触发 → 通知落库）

**文件：**
- 创建：`server/zwei-iot-alarm/src/test/java/.../AlarmNotifierIntegrationTest.java`

- [ ] **步骤 1：编写集成测试**

```java
package com.zwei.iot.alarm;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.iot.alarm.notify.AlarmNotifier;
import com.zwei.iot.alarm.notify.domain.AlarmNotification;
import com.zwei.iot.alarm.notify.service.IAlarmNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class AlarmNotifierIntegrationTest {

    @Autowired private AlarmNotifier notifier;
    @Autowired private IAlarmNotificationService notificationService;

    /**
     * 注：此测试需要：
     *   1. 数据库已有 alarm_dispatch_rule 数据（至少一条 ALARM 类型 + 等级 4 + 隐患点 1）
     *   2. 数据库已有 sys_user 数据（id=1 管理员）
     *   3. 数据库已有 sys_user_role 数据（user_id=1 关联到规则配置的角色）
     * 手工准备数据后再运行。
     */
    @Test
    void alarm_event_triggers_notification_records() {
        // given
        AlarmTriggeredEvent event = new AlarmTriggeredEvent();
        event.setAlarmId(99999L);
        event.setHazardPointId(1L);
        event.setAlarmLevel(4);     // 红色
        event.setAlarmTitle("集成测试告警");
        event.setHazardPointName("测试隐患点");

        // when
        notifier.onAlarmTriggered(event);

        // 等待异步执行（集成测试可加 sleep 或用 CountDownLatch）
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // then
        List<AlarmNotification> recent = notificationService.selectUserRecent(1L, 50);
        assertThat(recent).anyMatch(n ->
            n.getSourceId() != null && n.getSourceId() == 99999L);
    }
}
```

> 注：`AlarmTriggeredEvent` 的字段 setter 风格按实际类定义调整（可能需要用 builder 或全参构造）。

- [ ] **步骤 2：准备测试数据**

确保数据库有：
- `alarm_dispatch_rule` 至少 1 条 ALARM 类型、is_enabled=1、alarm_levels 含 '4'
- 对应 `alarm_dispatch_rule_hazard_point` 含 hazard_point_id=1 或 '*'
- 对应 `alarm_dispatch_rule_recipient` 含 user_id=1 或 '*'
- `sys_user` 中 user_id=1 是 status=0（正常）

- [ ] **步骤 3：运行集成测试**

```bash
cd server && mvn test -pl zwei-iot-alarm -am -Dtest=AlarmNotifierIntegrationTest
```

预期：测试 PASS。alarm_notification 表新增 source_id=99999 的记录。

- [ ] **步骤 4：手工 E2E 验证（启动后端）**

启动 `com.zwei.RuoYiApplication`（profile=local），手工触发一次告警事件（可通过修改 alarm_record 或调用告警 API），观察日志：

```
INFO  alarm-notify-1 AlarmNotifier - 收到告警事件 alarmId=...
DEBUG alarm-notify-1 AlarmNotifier - 匹配到 N 条规则
INFO  alarm-notify-1 AlarmNotifier - 落库 N 条通知
```

查 DB：

```sql
SELECT * FROM alarm_notification
WHERE create_time > NOW() - INTERVAL 5 MINUTE
ORDER BY id DESC;
```

预期：status 字段 SYSTEM=2(SENT)，SMS/EMAIL 视配置可能是 5(CHANNEL_NOT_CONFIGURED) 或 2(SENT) 或 3/4（失败）。error_msg 内容合理。

- [ ] **步骤 5：Commit**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/AlarmNotifierIntegrationTest.java
git commit -m "test(alarm): AlarmNotifier 集成测试 - 告警事件触发通知落库"
```

---

## 自检清单

### 规格覆盖度（计划 B 部分）

| 规格章节 | 对应任务 | 状态 |
|---------|---------|------|
| §6.1 INotifyChannel + Dispatcher | 任务 9 | ✅ |
| §6.2 SystemNotifyChannel | 任务 10 | ✅ |
| §6.3 SmsNotifyChannel（阿里云） | 任务 11 | ✅ |
| §6.4 EmailNotifyChannel（SMTP） | 任务 12 | ✅ |
| §6.5 错误码与状态映射 | 任务 3、4 | ✅ |
| §6.6 NotifyRecipientValidator | 任务 4 | ✅ |
| §6.7 NotifyConfigLoader | 任务 6 | ✅ |
| §6.8 AliyunSmsClient | 任务 11 | ✅ |
| §6.9 DynamicMailSender | 任务 12 | ✅ |
| §6.10 模板设计 | 任务 13 | ✅ |
| §6.11 Thymeleaf 邮件模板 | 任务 13 | ✅ |
| §7.1 双事件监听 | 任务 15 | ✅ |
| §7.2 去重主流程 | 任务 15 | ✅ |
| §7.3 分发 + DuplicateKeyException | 任务 15 | ✅ |
| §7.4 异步执行器 | 任务 14 | ✅ |
| §7.5 三层去重保障 | 任务 15 | ✅ |
| §7.6 时序图 | 任务 15 + 16 | ✅ |
| §5.4 IAlarmRuleMatcher | 任务 7 | ✅ |
| §5.4 IAlarmRecipientResolver | 任务 8 | ✅ |
| §5.5 跨模块查询 SQL | 任务 8 | ✅ |

### 计划 B 不涵盖（计划 A 已完成 + 计划 C 待办）

- 计划 A：DDL、Domain、规则 CRUD（已完成）
- 计划 C：通知中心 API + 前端 Tab + 系统设置 + 菜单注册（待办）

### 占位符扫描

- ✅ 所有 Java 类有完整代码
- ✅ Mapper XML 完整
- ✅ Thymeleaf HTML 模板完整
- ⚠️ 部分包路径（`com.zwei.iot.alarm.notify.domain.AlarmNotification` 等）按项目实际调整
- ⚠️ `AlarmStreamPublisher.publishToUser` 方法签名按项目实际调整
- ⚠️ 集成测试的 `AlarmTriggeredEvent` setter 按实际类定义

### 类型一致性

| 名称 | 定义位置 | 使用位置 |
|------|---------|---------|
| `INotifyChannel` | 任务 9 | 任务 10/11/12 |
| `NotifyChannelDispatcher` | 任务 9 | 任务 15 |
| `AlarmNotification.STATUS_*` | 任务 3 | 任务 10/11/12/15 |
| `NotifyRecipientValidator` | 任务 4 | 任务 11/12 |
| `SmsConfig` / `MailConfig` | 任务 5 | 任务 6/11/12 |
| `NotifyConfigLoader` | 任务 6 | 任务 11/12 |
| `AliyunSmsClient` | 任务 11 | 任务 11 |
| `DynamicMailSender` | 任务 12 | 任务 12 |
| `NotifyTemplateService` | 任务 13 | 任务 11/12 |
| `NotifyContext` | 任务 13 | 任务 13 |
| `IAlarmRuleMatcher` | 任务 7 | 任务 15 |
| `IAlarmRecipientResolver` | 任务 8 | 任务 15 |

### 已知风险

1. **`AlarmStreamPublisher.publishToUser` 方法签名**：实际项目里 SSE 推送 API 可能是 `publish(userId, eventType, data)`。实现时调整 SystemNotifyChannel.buildPayload 逻辑
2. **`AlarmTriggeredEvent` / `DeviceOfflineEvent` 字段**：实现时核对 `com.zwei.common.event.*` 实际类定义
3. **阿里云 SDK 凭证变更重建 client 的并发安全**：用 DCL（双重检查锁定）已处理
4. **集成测试需要数据库准备**：手工准备测试数据，避免依赖测试数据自动构建
5. **Thymeleaf 模板路径**：模板文件放 `src/main/resources/templates/mail/alarm-notify.html`，Spring Boot 自动配置 TemplateEngine 解析
6. **`IAlarmRecordService.selectById` 是否存在**：可能需要在 AlarmRecord 服务补全（计划 A 后应该可用）

---

## 执行交接

计划 B 已完成并保存到 `docs/superpowers/plans/2026-06-17-alarm-dispatch-rule-iteration-plan-b.md`。

继续写计划 C：通知中心 + 系统设置。
