# 告警通知规则迭代设计

- **作者**：Claude（brainstorming + writing-plans）
- **日期**：2026-06-17
- **状态**：待审查
- **关联模块**：`server/zwei-iot-alarm`、`server/zwei-system`、`web/src/views/alarm/`、`web/src/layout/`

---

## 1. 背景

"告警中心 - 通知设置"（`NotificationSetting.vue`）当前存在以下缺陷：

1. **前后端字段不一致**：前端 `hazardPointIds` 数组、后端 `hazard_point_id` 单值，导致多选保存丢数据
2. **人员是快照式 JSON**：`recipients_json` 存 `[{userId, name, phone}]`，无法表达"按角色/部门"，人员改名后不同步
3. **渠道不完整**：`channels` 定义了 SYSTEM/SMS/EMAIL，仅 SYSTEM（SSE）真正实现
4. **离线通知未集成**：`AlarmNotifier` 仅监听 `AlarmTriggeredEvent`，未监听 `DeviceOfflineEvent`
5. **通知中心单源**：`listTop` 仅查 `sys_notice`（全员广播），无法承接定向告警通知

## 2. 目标

| 目标 | 验收标准 |
|------|---------|
| 弹窗标题统一 | "新增/编辑通知规则" |
| 隐患点多选 + 全部 | 关联表 `alarm_dispatch_rule_hazard_point`，`*` 通配 |
| 设备多选 + 全部（离线场景） | 关联表 `alarm_dispatch_rule_device`，`*` 通配 |
| 通知人员多维度 | ROLE/DEPT/USER 三类型可并存，关联表 `alarm_dispatch_rule_recipient` |
| 事件监听双场景 | 监听 `AlarmTriggeredEvent` + `DeviceOfflineEvent` |
| 接收人动态展开 | 按角色/部门/指定人员展开为 userId 集合（含 `*` 通配） |
| 三渠道真实现 | SYSTEM（SSE+落库）、SMS（阿里云）、EMAIL（SMTP+Thymeleaf） |
| 错误信息可追溯 | `[ERROR_CODE] 描述` 格式写入 `alarm_notification.error_msg` |
| 通知中心分类 | `web/src/layout/index.vue` 公告/事件 Tab 分类，事件默认 |
| 系统配置入口 | 复用 `Settings.vue` 新增"通知配置"分类 |

## 3. 设计决策汇总（基于 brainstorming）

| # | 决策项 | 结论 |
|---|--------|------|
| 1 | 隐患点/人员存储 | 关联表（符合 RuoYi RBAC 惯例） |
| 2 | 通配符表达 | 关联表行 `id='*'` 表示该维度全部 |
| 3 | 去重策略 | `(source_type, source_id, recipient_id, channel)` 唯一键 |
| 4 | 渠道范围 | SYSTEM/SMS/EMAIL 三渠道全做 |
| 5 | SMS 厂商 | 阿里云 dysmsapi20170525 |
| 6 | EMAIL | Spring Mail + Thymeleaf HTML 模板 |
| 7 | 凭证存储 | `sys_config` 表（明文，预留加密扩展点） |
| 8 | SYSTEM 落库 | 统一 `alarm_notification` 表 + `read_time` 字段 |
| 9 | 通知中心 listTop | 原接口保留（公告），新增事件类独立接口 |
| 10 | 离线通知范围 | 本次一并改造 |
| 11 | 跨模块查询 | `AlarmRecipientQueryMapper` 直接查 `sys_user*`（只读） |
| 12 | 事件类型字段 | `event_type` 单值（ALARM/OFFLINE 分两条规则） |
| 13 | 异步处理 | 独立线程池 `alarmNotifyExecutor` |
| 14 | 错误信息字段 | 统一用 `error_msg`（不新增 remarks） |
| 15 | 系统配置入口 | 复用 `Settings.vue`，新增分类 |

## 4. 数据库设计

### 4.1 主表精简（`alarm_dispatch_rule`）

```sql
-- 备份旧表
RENAME TABLE `alarm_dispatch_rule` TO `alarm_dispatch_rule_bak`;

CREATE TABLE `alarm_dispatch_rule` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `name`         varchar(200) NOT NULL COMMENT '规则名称',
    `event_type`   varchar(10)  NOT NULL COMMENT '事件类型: ALARM=告警 / OFFLINE=设备离线',
    `alarm_levels` varchar(50)  DEFAULT NULL COMMENT '订阅告警等级（逗号分隔）: 1,2,3,4；OFFLINE 类型时为 NULL',
    `channels`     varchar(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知渠道（逗号分隔）: SYSTEM,SMS,EMAIL',
    `is_enabled`   tinyint      DEFAULT 1,
    `del_flag`     tinyint      DEFAULT 0,
    `create_by`    varchar(64)  DEFAULT '',
    `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_by`    varchar(64)  DEFAULT '',
    `update_time`  datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `remark`       varchar(500) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dispatch_event_enabled` (`event_type`, `is_enabled`, `del_flag`)
) COMMENT='通知规则主表';
```

**字段决策**：
- 删除：`hazard_point_id`、`recipients_json`、`time_window`、`alarm_types`、`type`、`apply_to_all_types`、`device_ids`
- 保留：`name`、`alarm_levels`、`channels`、`is_enabled`、`del_flag`、`remark`、审计字段
- 新增：`event_type`（替代 `type` + `alarm_types`）

### 4.2 三张关联表

```sql
-- 关联表 1：隐患点（ALARM 规则使用）
CREATE TABLE `alarm_dispatch_rule_hazard_point` (
    `rule_id`         bigint      NOT NULL,
    `hazard_point_id` varchar(20) NOT NULL COMMENT '隐患点ID；"*" 表示全部',
    PRIMARY KEY (`rule_id`, `hazard_point_id`),
    KEY `idx_adrhp_hp` (`hazard_point_id`)
) COMMENT='通知规则-隐患点关联表';

-- 关联表 2：设备（OFFLINE 规则使用）
CREATE TABLE `alarm_dispatch_rule_device` (
    `rule_id`    bigint      NOT NULL,
    `device_id`  varchar(20) NOT NULL COMMENT '设备ID；"*" 表示全部',
    PRIMARY KEY (`rule_id`, `device_id`),
    KEY `idx_adrd_dev` (`device_id`)
) COMMENT='通知规则-设备关联表（离线通知专用）';

-- 关联表 3：接收人（ROLE / DEPT / USER 三种类型可并存）
CREATE TABLE `alarm_dispatch_rule_recipient` (
    `rule_id`        bigint      NOT NULL,
    `recipient_type` varchar(10) NOT NULL COMMENT 'ROLE / DEPT / USER',
    `recipient_id`   varchar(20) NOT NULL COMMENT '角色/部门/用户ID；"*" 表示该类型全部',
    PRIMARY KEY (`rule_id`, `recipient_type`, `recipient_id`),
    KEY `idx_adrr_type_id` (`recipient_type`, `recipient_id`)
) COMMENT='通知规则-接收人关联表';
```

### 4.3 `alarm_notification` 扩展

```sql
ALTER TABLE `alarm_notification`
    ADD COLUMN `read_time`   datetime     DEFAULT NULL COMMENT '已读时间（NULL=未读）',
    ADD COLUMN `source_type` varchar(20)  DEFAULT 'alarm' COMMENT 'alarm=告警 / offline=设备离线',
    ADD COLUMN `source_id`   bigint       DEFAULT NULL COMMENT '来源ID（alarm_record.id 或 device.id）',
    MODIFY COLUMN `error_msg` varchar(1000) DEFAULT NULL
        COMMENT '渠道发送错误信息，格式 [ERROR_CODE] 描述',
    ADD UNIQUE KEY `uk_notif_dedup` (`source_type`, `source_id`, `recipient_id`, `channel`);
```

**status 状态机**：

| status | 含义 | 触发场景 |
|--------|------|---------|
| 1 PENDING | 待发送 | 初始落库 |
| 2 SENT | 已发送 | 渠道实现调用成功 |
| 3 FAILED | 发送失败 | 外部 API 业务错误、网络异常 |
| 4 INVALID_RECIPIENT | 接收人无效 | 手机号/邮箱缺失或格式错误 |
| 5 CHANNEL_NOT_CONFIGURED | 渠道未配置 | sys_config 缺关键参数 |

> 已读复用 `read_time` 字段，不占 status 位。

### 4.4 `sys_config` 新增 11 项

```sql
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`) VALUES
('短信-AccessKey ID',       'notify.sms.access-key-id',       '',           'Y', '阿里云 RAM AccessKey ID'),
('短信-AccessKey Secret',   'notify.sms.access-key-secret',   '',           'Y', '阿里云 RAM AccessKey Secret'),
('短信-签名',               'notify.sms.sign-name',           '知微监测',    'Y', '阿里云短信签名'),
('短信-告警模板',           'notify.sms.template.alarm',      'SMS_XXXXXX', 'Y', '告警通知短信模板Code'),
('短信-离线模板',           'notify.sms.template.offline',    'SMS_YYYYYY', 'Y', '设备离线短信模板Code'),
('邮件-SMTP主机',           'notify.mail.host',               'smtp.qq.com','Y', 'SMTP 服务器'),
('邮件-SMTP端口',           'notify.mail.port',               '465',        'Y', 'SMTP 端口'),
('邮件-用户名',             'notify.mail.username',           '',           'Y', '发件邮箱账号'),
('邮件-密码',               'notify.mail.password',           '',           'Y', 'SMTP 授权码'),
('邮件-发件人',             'notify.mail.from',               '',           'Y', '发件人邮箱地址'),
('邮件-是否SSL',            'notify.mail.ssl',                'true',       'Y', '是否启用 SSL');
```

### 4.5 数据迁移脚本

```sql
-- 1) 主表迁移
INSERT INTO alarm_dispatch_rule
    (id, name, event_type, alarm_levels, channels, is_enabled, del_flag, create_by, create_time, remark)
SELECT id, name,
       CASE WHEN type='offline' THEN 'OFFLINE' ELSE 'ALARM' END,
       alarm_levels, channels, is_enabled, del_flag, create_by, create_time, remark
FROM alarm_dispatch_rule_bak WHERE del_flag=0;

-- 2) 隐患点关联迁移（NULL → '*'）
INSERT INTO alarm_dispatch_rule_hazard_point (rule_id, hazard_point_id)
SELECT id, CASE WHEN hazard_point_id IS NULL THEN '*' ELSE CAST(hazard_point_id AS CHAR) END
FROM alarm_dispatch_rule_bak
WHERE del_flag=0 AND (type IS NULL OR type='alarm');

-- 3) 接收人关联迁移（recipients_json → USER 类型）
INSERT INTO alarm_dispatch_rule_recipient (rule_id, recipient_type, recipient_id)
SELECT r.id, 'USER', CAST(jt.userId AS CHAR)
FROM alarm_dispatch_rule_bak r,
     JSON_TABLE(r.recipients_json, '$[*]'
        COLUMNS (jt.userId BIGINT PATH '$.userId')) jt
WHERE r.del_flag=0 AND r.recipients_json IS NOT NULL;

-- 4) 设备关联：旧表未持久化 device_ids，无法迁移（用户需在 UI 重新编辑 OFFLINE 规则）
--    旧 OFFLINE 规则迁移后会因没有 device 关联而匹配不到任何设备

-- 5) 校验通过后：DROP TABLE alarm_dispatch_rule_bak;
```

## 5. 后端领域模型

### 5.1 包结构

```
server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/dispatch/
├── domain/
│   ├── AlarmDispatchRule.java
│   ├── AlarmDispatchRuleHazardPoint.java
│   ├── AlarmDispatchRuleDevice.java
│   ├── AlarmDispatchRuleRecipient.java
│   ├── AlarmEventType.java           # 枚举 ALARM / OFFLINE
│   ├── AlarmRecipientType.java       # 枚举 ROLE / DEPT / USER
│   └── NotifyChannel.java            # 枚举 SYSTEM / SMS / EMAIL
├── mapper/
│   ├── AlarmDispatchRuleMapper.java
│   ├── AlarmDispatchRuleHazardPointMapper.java
│   ├── AlarmDispatchRuleDeviceMapper.java
│   ├── AlarmDispatchRuleRecipientMapper.java
│   └── AlarmRecipientQueryMapper.java    # 跨模块查 sys_user（只读）
├── service/
│   ├── IAlarmDispatchRuleService.java
│   ├── IAlarmRuleMatcher.java
│   ├── IAlarmRecipientResolver.java
│   └── impl/...
├── dto/...
├── controller/
│   ├── AlarmDispatchRuleController.java
│   └── AlarmNotificationController.java
└── channel/
    ├── INotifyChannel.java
    ├── NotifyChannelDispatcher.java
    ├── SystemNotifyChannel.java
    ├── SmsNotifyChannel.java
    ├── EmailNotifyChannel.java
    ├── AliyunSmsClient.java
    ├── DynamicMailSender.java
    ├── NotifyConfigLoader.java
    ├── NotifyTemplateService.java
    └── NotifyRecipientValidator.java
```

### 5.2 Domain 实体

```java
public class AlarmDispatchRule extends BaseEntity {
    private Long id;
    private String name;
    private AlarmEventType eventType;
    private String alarmLevels;
    private String channels;
    private Integer isEnabled;
    private Integer delFlag;
}

public class AlarmDispatchRuleHazardPoint {
    private Long ruleId;
    private String hazardPointId;     // "*" = 全部
}

public class AlarmDispatchRuleDevice {
    private Long ruleId;
    private String deviceId;          // "*" = 全部
}

public class AlarmDispatchRuleRecipient {
    private Long ruleId;
    private AlarmRecipientType recipientType;
    private String recipientId;       // "*" = 该类型全部
}
```

### 5.3 DTO

```java
public class AlarmDispatchRuleCreateRequest {
    private Long id;                  // 编辑时必填
    private String name;
    @NotBlank private String eventType;        // ALARM / OFFLINE

    private List<String> alarmLevels;          // ALARM 必填
    @NotEmpty private List<String> channels;

    private List<String> hazardPointIds;       // ALARM 必填，可含 "*"
    private List<String> deviceIds;            // OFFLINE 必填，可含 "*"

    private RecipientSelection recipients;
    private Integer isEnabled;
    private String remark;

    public static class RecipientSelection {
        private List<String> roleIds;          // 可含 "*"
        private List<String> deptIds;          // 可含 "*"
        private List<String> userIds;          // 可含 "*"
    }
}
```

### 5.4 Service 接口

```java
public interface IAlarmDispatchRuleService {
    TableDataInfo<AlarmDispatchRuleItemVO> selectList(AlarmDispatchRuleQuery query);
    AlarmDispatchRuleDetailVO selectDetail(Long id);
    int create(AlarmDispatchRuleCreateRequest req);
    int update(Long id, AlarmDispatchRuleCreateRequest req);
    int delete(Long id);
    int toggleEnabled(Long id, Integer isEnabled);
}

public interface IAlarmRuleMatcher {
    /** 告警事件：匹配 ALARM 类型 + 等级 + 隐患点（含 '*'） */
    List<AlarmDispatchRule> matchAlarmRules(Long hazardPointId, String alarmLevel);

    /** 设备离线：匹配 OFFLINE 类型 + 设备（含 '*'） */
    List<AlarmDispatchRule> matchOfflineRules(Long deviceId);
}

public interface IAlarmRecipientResolver {
    /** 展开为去重的 userId 集合 */
    Set<Long> resolveUserIds(Long ruleId);
}
```

### 5.5 关键 SQL

**规则匹配（告警）**：

```xml
<select id="matchAlarmRules" resultType="...AlarmDispatchRule">
  SELECT DISTINCT r.*
  FROM alarm_dispatch_rule r
  LEFT JOIN alarm_dispatch_rule_hazard_point hp ON hp.rule_id = r.id
  WHERE r.del_flag = 0 AND r.is_enabled = 1
    AND r.event_type = 'ALARM'
    AND FIND_IN_SET(#{alarmLevel}, r.alarm_levels)
    AND (hp.hazard_point_id = '*' OR hp.hazard_point_id = #{hazardPointIdStr})
</select>
```

**接收人展开（跨模块只读查询）**：

```xml
<!-- 按角色 -->
<select id="selectUserIdsByRoleIds" resultType="long">
  SELECT DISTINCT ur.user_id
  FROM sys_user_role ur JOIN sys_user u ON u.user_id = ur.user_id
  WHERE u.del_flag = '0' AND u.status = '0'
    AND ur.role_id IN
    <foreach collection="roleIds" item="id" open="(" separator="," close=")">#{id}</foreach>
</select>

<!-- 按部门 -->
<select id="selectUserIdsByDeptIds" resultType="long">
  SELECT user_id FROM sys_user
  WHERE del_flag='0' AND status='0'
    AND dept_id IN
    <foreach collection="deptIds" item="id" open="(" separator="," close=")">#{id}</foreach>
</select>

<!-- 全部活跃用户（'*' 通配时） -->
<select id="selectAllActiveUserIds" resultType="long">
  SELECT user_id FROM sys_user WHERE del_flag='0' AND status='0'
</select>
```

### 5.6 跨模块依赖说明

`AlarmRecipientQueryMapper` 直接查 `sys_user` / `sys_user_role`：
- RBAC 表结构是 RuoYi 标准、稳定
- 只读查询，无写入耦合
- 避免新增跨模块 Service 接口（轻量）

未来如需严格遵循"跨模块走 Service"惯例，可在 `zwei-common` 定义 `IUserRecipientQueryService`，由 `zwei-system` 实现。

## 6. 渠道策略（三渠道实现）

### 6.1 接口设计

```java
public interface INotifyChannel {
    String getChannel();   // "SYSTEM" / "SMS" / "EMAIL"

    /**
     * 实际发送 + 状态回写
     * 成功：markSent(id)
     * 失败：markFailed(id, errorCode, description)
     */
    void send(AlarmNotification notification);
}

@Component
public class NotifyChannelDispatcher {
    private final Map<String, INotifyChannel> channelMap;

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

### 6.2 SYSTEM 渠道

```java
@Component
public class SystemNotifyChannel implements INotifyChannel {
    public String getChannel() { return "SYSTEM"; }

    @Transactional
    public void send(AlarmNotification n) {
        // 1. SSE 实时推送（在线用户立刻收到）
        alarmStreamPublisher.publishToUser(n.getRecipientId(),
            SseMessage.builder().type("alarm-notify").data(NotifyPayload.from(n)).build());

        // 2. 直接 markSent（用户不在线也算发送成功，listTop 可查）
        notificationService.markSent(n.getId());
    }
}
```

> SYSTEM 不校验接收人（站内消息一定可达）；用户已读通过 `markRead(id)` 触发 `read_time` 更新。

### 6.3 SMS 渠道（阿里云）

```java
@Component
public class SmsNotifyChannel implements INotifyChannel {
    public String getChannel() { return "SMS"; }

    public void send(AlarmNotification n) {
        // 1. 接收人校验
        String recipientErr = NotifyRecipientValidator.validatePhone(n.getRecipientPhone());
        if (recipientErr != null) {
            notificationService.markFailed(n.getId(), recipientErr,
                String.format("用户 %s 手机号无效: %s", n.getRecipientName(),
                    StringUtils.defaultString(n.getRecipientPhone(), "(空)")));
            return;
        }

        // 2. 渠道配置校验
        SmsConfig cfg = notifyConfigLoader.loadSmsConfig();
        String cfgErr = cfg.validate();
        if (cfgErr != null) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "[CHANNEL_NOT_CONFIGURED] " + cfgErr);
            return;
        }

        // 3. 调用阿里云
        try {
            SendSmsResponse resp = aliyunSmsClient.send(
                n.getRecipientPhone(),
                cfg.selectTemplateCode(n.getSourceType()),
                notifyTemplateService.buildSmsParams(n), cfg);

            if ("OK".equals(resp.getBody().getCode())) {
                notificationService.markSent(n.getId());
            } else {
                notificationService.markFailed(n.getId(), "PROVIDER_ERROR",
                    "[PROVIDER_ERROR] 阿里云: " + resp.getBody().getCode()
                    + " - " + resp.getBody().getMessage());
            }
        } catch (Exception e) {
            notificationService.markFailed(n.getId(), "NETWORK_ERROR",
                "[NETWORK_ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
```

### 6.4 EMAIL 渠道（SMTP + Thymeleaf）

```java
@Component
public class EmailNotifyChannel implements INotifyChannel {
    public String getChannel() { return "EMAIL"; }

    public void send(AlarmNotification n) {
        // 1. 接收人校验
        String err = NotifyRecipientValidator.validateEmail(n.getRecipientEmail());
        if (err != null) {
            notificationService.markFailed(n.getId(), err,
                String.format("用户 %s 邮箱无效: %s", n.getRecipientName(),
                    StringUtils.defaultString(n.getRecipientEmail(), "(空)")));
            return;
        }
        // 2. SMTP 配置校验
        MailConfig cfg = notifyConfigLoader.loadMailConfig();
        String cfgErr = cfg.validate();
        if (cfgErr != null) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "[CHANNEL_NOT_CONFIGURED] " + cfgErr);
            return;
        }
        // 3. 渲染并发送
        try {
            String html = notifyTemplateService.renderEmailHtml(n);
            dynamicMailSender.send(n.getRecipientEmail(), n.getTitle(), html, cfg);
            notificationService.markSent(n.getId());
        } catch (MessagingException | MailAuthenticationException e) {
            notificationService.markFailed(n.getId(), "NETWORK_ERROR",
                "[NETWORK_ERROR] " + e.getMessage());
        } catch (Exception e) {
            notificationService.markFailed(n.getId(), "UNKNOWN",
                "[UNKNOWN] " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
```

### 6.5 错误码与状态映射

| ERROR_CODE | status | 说明 |
|---|---|---|
| `RECIPIENT_PHONE_MISSING` | 4 | 手机号为空 |
| `RECIPIENT_PHONE_INVALID` | 4 | 手机号格式错 |
| `RECIPIENT_EMAIL_MISSING` | 4 | 邮箱为空 |
| `RECIPIENT_EMAIL_INVALID` | 4 | 邮箱格式错 |
| `CHANNEL_NOT_CONFIGURED` | 5 | sys_config 缺关键参数 |
| `PROVIDER_ERROR` | 3 | 外部 SDK 业务错误 |
| `NETWORK_ERROR` | 3 | 网络/超时 |
| `UNKNOWN` | 3 | 未捕获异常 |
| `UNKNOWN_CHANNEL` | 3 | 未知渠道标识 |

### 6.6 接收人校验工具

```java
public final class NotifyRecipientValidator {
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static String validatePhone(String phone) {
        if (StringUtils.isBlank(phone))         return "RECIPIENT_PHONE_MISSING";
        if (!PHONE.matcher(phone).matches())    return "RECIPIENT_PHONE_INVALID";
        return null;
    }

    public static String validateEmail(String email) {
        if (StringUtils.isBlank(email))         return "RECIPIENT_EMAIL_MISSING";
        if (!EMAIL.matcher(email).matches())    return "RECIPIENT_EMAIL_INVALID";
        return null;
    }
}
```

### 6.7 配置加载（基于 sys_config + 缓存）

```java
@Component
public class NotifyConfigLoader {
    private final ISysConfigService sysConfigService;

    @Cacheable(value = "notify:config", key = "'sms'")
    public SmsConfig loadSmsConfig() { /* 读 5 个 notify.sms.* key */ }

    @Cacheable(value = "notify:config", key = "'mail'")
    public MailConfig loadMailConfig() { /* 读 6 个 notify.mail.* key */ }

    @EventListener
    public void onConfigChanged(SysConfigChangedEvent event) {
        if (event.getKey().startsWith("notify.")) evictCache();
    }
}
```

### 6.8 阿里云 SMS 客户端（懒加载）

```java
@Component
public class AliyunSmsClient {
    private volatile com.aliyun.dysmsapi20170525.Client client;
    private volatile String cachedKey;

    public SendSmsResponse send(String phone, String templateCode,
                                Map<String,String> params, SmsConfig cfg) throws Exception {
        // 凭证变更时重建 client（DCL）
        ...
        SendSmsRequest req = new SendSmsRequest()
            .setPhoneNumbers(phone)
            .setSignName(cfg.getSignName())
            .setTemplateCode(templateCode)
            .setTemplateParam(JSON.toJSONString(params));
        return client.sendSms(req);
    }
}
```

### 6.9 动态 SMTP JavaMailSender

```java
@Component
public class DynamicMailSender {
    private volatile JavaMailSenderImpl sender;
    private volatile String cachedKey;

    public void send(String to, String subject, String html, MailConfig cfg)
            throws MessagingException {
        JavaMailSenderImpl s = getOrBuild(cfg);   // DCL 按 host+username 重建
        MimeMessage mime = s.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(cfg.getFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        s.send(mime);
    }
}
```

### 6.10 模板设计

**告警事件模板变量**：

| 变量 | 来源 | 示例 |
|------|------|------|
| `{hazardPointName}` | hazard_point.name | 龙泉寺滑坡 |
| `{deviceName}` | device.name | GNSS-001 |
| `{alarmLevel}` | 等级字典 | 红色预警 |
| `{alarmTitle}` | alarm_record.title | 位移速率超阈值 |
| `{alarmTime}` | alarm_record.alarm_time | 2026-06-17 14:30 |
| `{alarmId}` | alarm_record.id | 1234 |

**三渠道格式**：

```
SYSTEM 消息：
  title:   "[告警] {hazardPointName} - {alarmTitle}"
  content: "等级：{alarmLevel} | 设备：{deviceName} | 时间：{alarmTime}"

SMS（阿里云控制台预申请的模板）：
  templateParams: {name, level, content, time}
  阿里云模板示例：【知微监测】${name}发生${level}，${content}，时间${time}

EMAIL（Thymeleaf HTML）：
  subject: "[知微告警] {hazardPointName} - {alarmTitle}"
  html:    resource/templates/mail/alarm-notify.html
```

**离线事件模板变量**：`{deviceName}`、`{deviceCode}`、`{offlineTime}`、`{lastReportTime}`

### 6.11 Thymeleaf 邮件模板

`server/zwei-iot-alarm/src/main/resources/templates/mail/alarm-notify.html`：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body style="font-family:'Microsoft YaHei',sans-serif;">
  <div style="max-width:600px;margin:0 auto;border:1px solid #eee;">
    <div style="background:#f56c6c;color:#fff;padding:16px;">
      <h2 th:text="'告警：' + ${hazardPointName}">告警标题</h2>
    </div>
    <div style="padding:20px;">
      <p><strong>等级：</strong><span th:text="${alarmLevel}">红色</span></p>
      <p><strong>设备：</strong><span th:text="${deviceName}">GNSS-001</span></p>
      <p><strong>事件：</strong><span th:text="${alarmTitle}">位移超阈值</span></p>
      <p><strong>时间：</strong><span th:text="${alarmTime}">2026-06-17 14:30</span></p>
      <a th:href="${linkUrl}"
         style="display:inline-block;background:#409eff;color:#fff;padding:10px 24px;
                text-decoration:none;margin-top:12px;">查看详情</a>
    </div>
  </div>
</body>
</html>
```

### 6.12 凭证安全（YAGNI 阶段）

- 当前：明文存 sys_config，依赖 DB 访问控制兜底
- 扩展点：`NotifyConfigLoader.decrypt()` 包装，未来用 AES 加密（密钥从 `application-local.yml` 读取）

## 7. AlarmNotifier 主流程

### 7.1 双事件监听

```java
@Component
@Slf4j
public class AlarmNotifier {
    @EventListener
    @Async("alarmNotifyExecutor")
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        dispatchForAlarm(event);
    }

    @EventListener
    @Async("alarmNotifyExecutor")
    public void onDeviceOffline(DeviceOfflineEvent event) {
        dispatchForOffline(event);
    }
}
```

### 7.2 主流程（去重核心）

```java
private Collection<AlarmNotification> buildAndDedupNotifications(
        List<AlarmDispatchRule> rules,
        String sourceType,
        Long sourceId,
        NotificationContext ctxCustomizer) {

    Map<String, AlarmNotification> dedup = new HashMap<>();   // key = userId+"|"+channel

    for (AlarmDispatchRule rule : rules) {
        Set<Long> userIds = recipientResolver.resolveUserIds(rule.getId());
        Set<String> channels = parseChannels(rule.getChannels());

        for (Long userId : userIds) {
            for (String channel : channels) {
                String key = userId + "|" + channel;
                if (dedup.containsKey(key)) continue;

                SysUser user = userQueryService.selectUserById(userId);
                if (user == null) continue;

                AlarmNotification n = new AlarmNotification();
                n.setSourceType(sourceType);
                n.setSourceId(sourceId);
                n.setDispatchRuleId(rule.getId());
                n.setRecipientId(userId);
                n.setRecipientName(user.getUserName());
                n.setRecipientPhone(user.getPhonenumber());
                n.setRecipientEmail(user.getEmail());
                n.setChannel(channel);
                n.setTitle(ctxCustomizer.getTitle());
                n.setContent(ctxCustomizer.getContent());
                n.setStatus(AlarmNotification.STATUS_PENDING);
                dedup.put(key, n);
            }
        }
    }
    return dedup.values();
}
```

### 7.3 分发

```java
private void dispatch(Collection<AlarmNotification> notifications) {
    if (notifications.isEmpty()) return;

    try {
        notificationService.saveBatch(notifications);
    } catch (DuplicateKeyException e) {
        // 唯一键 uk_notif_dedup 兜底：事件重放或并发触发导致整批重复，静默忽略
        // 注：saveBatch 是批量 INSERT，整批冲突意味着本次事件所有通知都已落库过（并发场景）
        log.warn("通知重复被忽略（整批冲突，事件已处理）", e);
        return;
    }

    for (AlarmNotification n : notifications) {
        try {
            channelDispatcher.dispatch(n);
        } catch (Exception e) {
            log.error("通知分发失败 notifId={} channel={}", n.getId(), n.getChannel(), e);
            notificationService.markFailed(n.getId(), "UNKNOWN",
                "[UNKNOWN] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
```

### 7.4 异步执行器

```java
@Configuration
public class AlarmNotifyAsyncConfig {
    @Bean("alarmNotifyExecutor")
    public Executor alarmNotifyExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(500);
        exec.setThreadNamePrefix("alarm-notify-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}
```

### 7.5 三层去重保障

| 层级 | 实现 | 目的 |
|------|------|------|
| 内存层 | `Map<userId+channel, Notification>` | 同次事件多规则覆盖时取首条 |
| 数据库层 | `uk_notif_dedup` 唯一键 | 防止事件重放/并发触发重复 |
| 异常处理层 | 捕获 `DuplicateKeyException` 静默忽略 | 多线程并发兜底 |

### 7.6 时序图（告警）

```
AlarmEvaluationEngine → publish AlarmTriggeredEvent
  └─ AlarmNotifier.onAlarmTriggered [@Async alarmNotifyExecutor]
       ├─ ruleMatcher.matchAlarmRules(hpId, level) → List<Rule>
       ├─ for each rule:
       │    ├─ recipientResolver.resolveUserIds(ruleId) → Set<userId>
       │    └─ for (userId × channel) → Map<key, Notification>
       ├─ notificationService.saveBatch()       [uk_notif_dedup 兜底]
       └─ for each Notification:
            └─ channelDispatcher.dispatch(n)
                 ├─ SYSTEM → SSE 推送 + markSent
                 ├─ SMS    → 阿里云 API + markSent/markFailed
                 └─ EMAIL  → SMTP + markSent/markFailed
```

## 8. 通知中心 API

### 8.1 接口分工

| 用途 | 接口 | 数据源 |
|------|------|--------|
| **公告** | `GET /api/v1/system/notice/listTop` （原接口不动） | `sys_notice` + `sys_notice_read` |
| **事件列表** | `GET /api/v1/alarm/notifications/recent?limit=10` | `alarm_notification` where channel=SYSTEM |
| 事件未读数 | `GET /api/v1/alarm/notifications/unread-count` | 同上 |
| 事件已读 | `POST /api/v1/alarm/notifications/{id}/read` | - |
| 事件全部已读 | `POST /api/v1/alarm/notifications/read-all` | - |

### 8.2 关键 SQL

```xml
<select id="selectUserRecent" resultType="...AlarmNotificationItemVO">
  SELECT id, source_type, source_id, title, content,
         recipient_name, read_time, create_time
  FROM alarm_notification
  WHERE recipient_id = #{userId}
    AND channel = 'SYSTEM'
    AND source_type IN ('alarm', 'offline')
  ORDER BY create_time DESC
  LIMIT #{limit}
</select>

<update id="markReadIfOwner">
  UPDATE alarm_notification
  SET read_time = NOW()
  WHERE id = #{notifId}
    AND recipient_id = #{userId}
    AND read_time IS NULL
</update>
```

### 8.3 权限标识

- `alarm:notification:list` — 查询本人事件通知
- `alarm:notification:read` — 标记已读

## 9. 前端 UI 改造

### 9.1 改造范围

| 文件 | 改动 |
|------|------|
| `web/src/views/alarm/NotificationSetting.vue` | 弹窗标题、表单结构、列表展示 |
| `web/src/views/alarm/components/RecipientPicker.vue` | **新建** |
| `web/src/api/alarm.ts` | 类型定义对齐后端 DTO |
| `web/src/layout/index.vue` | 通知中心 Tab 改造（公告/事件） |
| `web/src/api/alarmNotification.ts` | **新建** |
| `web/src/views/system/Settings.vue` | 新增"通知配置"分类 |

### 9.2 弹窗标题

```vue
<el-dialog :title="dialogTitle" v-model="dialogVisible">

<script>
const dialogTitle = computed(() => form.id ? '编辑通知规则' : '新增通知规则')
</script>
```

### 9.3 表单关键字段

```vue
<el-form-item label="规则名称" prop="name">...</el-form-item>

<el-form-item label="事件类型" prop="eventType">
  <el-radio-group v-model="form.eventType">
    <el-radio label="ALARM">告警事件</el-radio>
    <el-radio label="OFFLINE">设备离线</el-radio>
  </el-radio-group>
</el-form-item>

<!-- 告警等级、隐患点（ALARM 显示） -->
<el-form-item label="告警等级" v-if="form.eventType === 'ALARM'">
  <el-checkbox-group v-model="form.alarmLevels">
    <el-checkbox label="1">蓝色</el-checkbox>
    <el-checkbox label="2">黄色</el-checkbox>
    <el-checkbox label="3">橙色</el-checkbox>
    <el-checkbox label="4">红色</el-checkbox>
  </el-checkbox-group>
</el-form-item>

<el-form-item label="隐患点" v-if="form.eventType === 'ALARM'">
  <el-select v-model="form.hazardPointIds" multiple filterable>
    <el-option label="全部隐患点" value="*" />
    <el-option v-for="hp in hazardPointOptions" :key="hp.id"
               :label="hp.name" :value="String(hp.id)" />
  </el-select>
</el-form-item>

<!-- 设备（OFFLINE 显示） -->
<el-form-item label="设备" v-if="form.eventType === 'OFFLINE'">
  <el-select v-model="form.deviceIds" multiple filterable>
    <el-option label="全部设备" value="*" />
    <el-option v-for="d in deviceOptions" :key="d.id"
               :label="d.name + '（' + d.code + '）'" :value="String(d.id)" />
  </el-select>
</el-form-item>

<!-- 接收人 -->
<el-form-item label="通知人员" required>
  <RecipientPicker v-model="form.recipients" />
</el-form-item>

<!-- 渠道 -->
<el-form-item label="通知渠道" prop="channels" required>
  <el-checkbox-group v-model="form.channels">
    <el-checkbox label="SYSTEM">系统消息</el-checkbox>
    <el-checkbox label="SMS">短信</el-checkbox>
    <el-checkbox label="EMAIL">邮件</el-checkbox>
  </el-checkbox-group>
</el-form-item>
```

### 9.4 RecipientPicker 组件

```vue
<template>
  <div class="recipient-picker">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="按角色" name="role">
        <el-checkbox-group v-model="localValue.roleIds">
          <el-checkbox label="*">所有角色</el-checkbox>
          <el-checkbox v-for="r in roleOptions" :key="r.id"
                       :label="String(r.id)">{{ r.roleName }}</el-checkbox>
        </el-checkbox-group>
      </el-tab-pane>
      <el-tab-pane label="按部门" name="dept">
        <el-tree :data="deptTree" show-checkbox node-key="id" ... />
        <el-checkbox v-model="allDept">所有部门</el-checkbox>
      </el-tab-pane>
      <el-tab-pane label="指定人员" name="user">
        <el-select v-model="localValue.userIds" multiple filterable>
          <el-option label="所有用户" value="*" />
          <el-option v-for="u in userOptions" ... />
        </el-select>
      </el-tab-pane>
    </el-tabs>

    <div class="selection-summary">
      <el-tag v-if="hasWildcard('ROLE')" type="warning">全部角色</el-tag>
      <el-tag v-if="hasWildcard('DEPT')" type="warning">全部部门</el-tag>
      <el-tag v-if="hasWildcard('USER')" type="warning">全部用户</el-tag>
      <!-- 其他具体选项 -->
    </div>
  </div>
</template>
```

**交互**：选了 `*` 时清空具体选项（互斥）；已选汇总用 el-tag 展示，可删除。

### 9.5 通知中心 Tab 改造（`web/src/layout/index.vue`）

```
┌──────────────────────────────────────┐
│  [ 公告 (3) ] [ 事件 (5) ]    全部已读 │   ← Tab + 总未读
├──────────────────────────────────────┤
│  • 公告 1 标题                    ●  │
│    2026-06-17 14:30                  │
│  • ...                               │
└──────────────────────────────────────┘
```

**默认 Tab**：`event`（更紧急）

```typescript
const activeTab = ref<'notice' | 'event'>('event')
const totalUnread = computed(() => noticeUnread.value + eventUnread.value)

async function loadAll() {
  const [noticeRes, eventRes] = await Promise.all([
    getNoticeListTop(),
    getRecentAlarmNotifications(10)
  ])
  // ...
}

function handleClick(tab, item) {
  if (tab === 'notice') {
    router.push(`/system/notice/detail/${item.noticeId}`)
  } else if (item.sourceType === 'alarm') {
    router.push({ path: '/alarm/realtime', query: { alarmId: item.sourceId } })
  } else if (item.sourceType === 'offline') {
    router.push({ path: '/basic/device', query: { deviceId: item.sourceId } })
  }
  handleRead(tab, item.id)
}
```

### 9.6 SSE 监听 alarm-notify

```typescript
sseClient.onMessage((msg) => {
  if (msg.type === 'alarm-notify') {
    ElNotification({ title: msg.data.title, message: msg.data.content, type: 'warning' })
    loadAlarmNotifications()
  } else if (msg.type === 'notice-created') {
    loadNoticeListTop()
  }
})
```

### 9.7 系统设置新增分类（`Settings.vue`）

```typescript
const paramCategories = [
  { key: 'basic', label: '基础配置' },
  { key: 'data', label: '数据管理' },
  { key: 'alarm', label: '告警配置' },
  { key: 'security', label: '安全设置' },
  { key: 'notify', label: '通知配置' }   // 新增
]

const paramList = ref<ParamItem[]>([
  // ...原有项
  { code: 'notify.sms.access-key-id',     name: '阿里云 AccessKey ID',  type: 'string',   category: 'notify', value: '', remark: '...' },
  { code: 'notify.sms.access-key-secret', name: '阿里云 AccessKey Secret', type: 'password', category: 'notify', value: '', remark: '...' },
  { code: 'notify.sms.sign-name',         name: '短信签名',              type: 'string',   category: 'notify', value: '知微监测' },
  { code: 'notify.sms.template.alarm',    name: '告警短信模板Code',      type: 'string',   category: 'notify', value: '', placeholder: 'SMS_XXXXXX' },
  { code: 'notify.sms.template.offline',  name: '离线短信模板Code',      type: 'string',   category: 'notify', value: '', placeholder: 'SMS_YYYYYY' },
  { code: 'notify.mail.host',             name: 'SMTP 主机',             type: 'string',   category: 'notify', value: 'smtp.qq.com' },
  { code: 'notify.mail.port',             name: 'SMTP 端口',             type: 'number',   category: 'notify', value: 465, min: 1, max: 65535 },
  { code: 'notify.mail.username',         name: '发件账号',              type: 'string',   category: 'notify', value: '' },
  { code: 'notify.mail.password',         name: 'SMTP 授权码',           type: 'password', category: 'notify', value: '', remark: '邮箱服务商提供的授权码' },
  { code: 'notify.mail.from',             name: '发件人邮箱',            type: 'string',   category: 'notify', value: '' },
  { code: 'notify.mail.ssl',              name: '启用 SSL',              type: 'switch',   category: 'notify', value: true }
])
```

```vue
<!-- 扩展 type=password 渲染 -->
<template v-else-if="param.type === 'password'">
  <el-input v-model="paramsFormData[param.code]"
            type="password" show-password
            :placeholder="param.placeholder" style="width: 400px" />
</template>
```

### 9.8 列表页改造（联动）

```vue
<el-table-column label="事件类型">
  <template #default="{ row }">
    <el-tag :type="row.eventType === 'ALARM' ? 'danger' : 'warning'">
      {{ row.eventType === 'ALARM' ? '告警' : '设备离线' }}
    </el-tag>
  </template>
</el-table-column>

<el-table-column label="隐患点/设备">
  <template #default="{ row }">
    <span v-if="row.eventType === 'ALARM'">
      <el-tag size="small" v-if="row.hazardPointIds.includes('*')" type="warning">全部</el-tag>
      <el-tag size="small" v-for="hp in row.hazardPointNames" v-else>{{ hp }}</el-tag>
    </span>
    <span v-else>
      <el-tag size="small" v-if="row.deviceIds.includes('*')" type="warning">全部设备</el-tag>
      <el-tag size="small" v-for="d in row.deviceNames" v-else>{{ d }}</el-tag>
    </span>
  </template>
</el-table-column>

<el-table-column label="接收人">
  <template #default="{ row }">
    <el-tag v-if="row.recipients.hasWildcard" type="warning">全部</el-tag>
    <span v-else>{{ row.recipients.summaryText }}</span>
  </template>
</el-table-column>
```

## 10. Maven 依赖

`server/zwei-iot-alarm/pom.xml` 新增：

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

<!-- Thymeleaf 模板渲染 -->
<dependency>
    <groupId>org.thymeleaf</groupId>
    <artifactId>thymeleaf-spring6</artifactId>
</dependency>
```

## 11. 菜单与权限

```sql
-- 告警通知按钮权限（挂在 alarm 菜单父级下）
-- :alarmMenuId 为占位符，实现时查 sys_menu WHERE menu_name='告警管理' AND menu_type='M' 取实际 menu_id
INSERT INTO sys_menu(menu_name, parent_id, menu_type, perms) VALUES
('告警通知查看', :alarmMenuId, 'F', 'alarm:notification:list'),
('告警通知已读', :alarmMenuId, 'F', 'alarm:notification:read');
```

> 系统设置复用现有 `/system/settings` 路径，无需新增菜单。

## 12. 风险与缓解

| # | 风险 | 影响 | 缓解 |
|---|------|------|------|
| 1 | 数据迁移丢失 | 旧规则失效 | 旧表 `rename _bak`、迁移脚本验证后再删 |
| 2 | 离线规则 deviceIds 旧表未持久化 | OFFLINE 规则迁移后无设备关联 | 文档说明 + 前端编辑提示 |
| 3 | 接收人展开性能（DEPT '*' 可能 1000+） | 单次事件慢 | 异步执行器 + 单线程消费 + 指标监控 |
| 4 | AlarmRecipientQueryMapper 跨模块查 sys_user | 破坏"跨模块走 Service"惯例 | 注释明确只读 + RuoYi 标准 RBAC |
| 5 | 阿里云 SDK 依赖膨胀 | jar 体积 | 已隔离在 zwei-iot-alarm 模块 |
| 6 | sys_config 凭证明文 | 安全风险 | YAGNI 先明文，预留 decrypt() 扩展点 |
| 7 | 异步线程池并发触发 | 重复通知 | DB 唯一键 + DuplicateKeyException 兜底 |
| 8 | Settings.vue 现有保存逻辑未核对 | 配置可能未写入 sys_config | 阶段 8.3 第一步核对 |
| 9 | OFFLINE 不查隐患点 | 跨场景语义差异 | 文档明确 |

## 13. 实现计划（9 阶段）

```
阶段 1 · 数据库与领域模型
  1.1 DDL 升级脚本（db/upgrade/v2026.06.17.001_dispatch_rule_v2.sql）
  1.2 数据迁移脚本
  1.3 Domain 实体（4 个）+ 枚举（3 个）
  1.4 Mapper + XML（5 个）

阶段 2 · 规则 CRUD（后端）
  2.1 IAlarmDispatchRuleService + Impl（事务级联）
  2.2 AlarmDispatchRuleController（含 recipient-options 接口）
  2.3 DTO（CreateRequest / DetailVO / ItemVO）

阶段 3 · 规则匹配与接收人展开
  3.1 IAlarmRuleMatcher 实现（含 '*' 通配 SQL）
  3.2 AlarmRecipientQueryMapper
  3.3 IAlarmRecipientResolver 实现

阶段 4 · 渠道策略
  4.1 INotifyChannel 接口 + NotifyChannelDispatcher
  4.2 SystemNotifyChannel（SSE）
  4.3 NotifyConfigLoader（sys_config + 缓存）
  4.4 AliyunSmsClient + SmsNotifyChannel
  4.5 DynamicMailSender + EmailNotifyChannel + Thymeleaf
  4.6 NotifyTemplateService（事件 → 变量 → 三渠道渲染）
  4.7 NotifyRecipientValidator + 状态码映射

阶段 5 · AlarmNotifier 改造
  5.1 双事件监听
  5.2 buildAndDedupNotifications 主流程
  5.3 AlarmNotifyAsyncConfig
  5.4 DuplicateKeyException 兜底

阶段 6 · 通知中心
  6.1 AlarmNotificationController（recent / unread-count / read / read-all）
  6.2 web/src/api/alarmNotification.ts
  6.3 layout/index.vue Tab 改造 + SSE 监听
  6.4 跳转 /alarm/realtime、/basic/device

阶段 7 · 通知规则前端
  7.1 NotificationSetting.vue 弹窗 + 表单
  7.2 RecipientPicker.vue 新建
  7.3 列表页改造
  7.4 alarm.ts 类型对齐

阶段 8 · 系统设置
  8.1 Settings.vue 新增 notify 分类 + 11 参数
  8.2 type=password 渲染扩展
  8.3 保存逻辑核对（确认写入 sys_config）

阶段 9 · 收尾
  9.1 sys_menu 权限注册
  9.2 单测（匹配/展开/去重/校验）
  9.3 集成测试（事件触发 → 三渠道）
  9.4 文档更新（CLAUDE.md + 用户手册）
```

## 14. 测试策略（TDD）

| 模块 | 单测重点 |
|------|---------|
| `IAlarmRuleMatcher` | `*` 通配、等级包含、隐患点/设备多匹配 |
| `IAlarmRecipientResolver` | ROLE/DEPT/USER 三类型展开 + `*` 全部 + 去重 |
| `NotifyRecipientValidator` | 手机号/邮箱格式错误用例 |
| `SmsNotifyChannel` | mock 阿里云 SDK，校验 error_msg 写入 |
| `EmailNotifyChannel` | mock JavaMailSender，校验 SMTP 缺失场景 |
| `AlarmNotifier` | 多规则覆盖去重、DuplicateKeyException 兜底 |
| `NotifyAggregation` | 用户视角查询权限隔离 |

## 15. 验收清单

- [ ] 弹窗标题"新增/编辑通知规则"
- [ ] 隐患点支持多选 + "全部"
- [ ] 设备支持多选 + "全部"（OFFLINE 场景）
- [ ] 接收人 ROLE/DEPT/USER 三种类型可并存
- [ ] 规则按 `(event_type, alarm_level, hazard_point/device)` 匹配
- [ ] 接收人按规则展开为 userId 集合（含 `*` 通配）
- [ ] 去重 key `(source_type, source_id, recipient_id, channel)` 生效
- [ ] SYSTEM/SMS/EMAIL 三渠道实现策略模式
- [ ] 错误信息按 `[ERROR_CODE] 描述` 格式写入 `error_msg`
- [ ] 通知中心 Tab 分类（公告/事件），事件默认
- [ ] 点击告警事件 → `/alarm/realtime?alarmId=xxx`
- [ ] 点击离线事件 → `/basic/device?deviceId=xxx`
- [ ] SSE 实时推送 `alarm-notify` 类型
- [ ] 系统设置 → 通知配置（11 项参数）
- [ ] 旧 `alarm_dispatch_rule` 数据迁移成功
- [ ] 单测覆盖率 ≥ 70%（核心 Service）

## 16. 后续扩展（YAGNI 之外）

- AES 加密 sys_config 凭证
- 独立"消息中心"页面（分页/筛选）
- "测试发送"按钮（前端系统设置 + 后端 `/alarm/notify/test`）
- 跨模块 `IUserRecipientQueryService` 接口化
- 接收人展开性能优化（异步分批）
