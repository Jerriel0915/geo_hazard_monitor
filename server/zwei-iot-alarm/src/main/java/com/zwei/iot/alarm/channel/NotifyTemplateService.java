package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知模板渲染服务
 *
 * <p>三渠道格式：
 * <ul>
 *   <li>SYSTEM: 简洁标题 + 内容（直接用 AlarmNotification.title/content）</li>
 *   <li>SMS:    阿里云模板参数 (name/level/content/time)</li>
 *   <li>EMAIL:  HTML (alarm-notify.html)</li>
 * </ul>
 *
 * <p>简化设计：AlarmRecord 已反规范化 hazardPointName/deviceName/alarmLevelText，
 * 无需扩展跨模块 Query Service。
 *
 * @author zwei
 */
@Slf4j
@Service
public class NotifyTemplateService {

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private IAlarmRecordService alarmRecordService;

    /**
     * 构造模板上下文（从 AlarmNotification 反查业务数据）
     */
    public NotifyContext buildContext(AlarmNotification n) {
        NotifyContext.NotifyContextBuilder b = NotifyContext.builder()
            .sourceType(n.getSourceType())
            .sourceId(n.getSourceId())
            .alarmTitle(n.getTitle());

        if (n.getSourceId() != null && isAlarmSourceType(n.getSourceType())) {
            try {
                AlarmRecord record = alarmRecordService.selectById(n.getSourceId());
                if (record != null) {
                    b.eventTime(record.getFirstTriggerTime());
                    b.alarmLevel(record.getAlarmLevelText());
                    b.hazardPointName(record.getHazardPointName());
                    b.deviceName(record.getDeviceName());
                }
            } catch (Exception e) {
                log.warn("查 AlarmRecord 失败 sourceId={}, 降级为仅用 Notification 字段",
                    n.getSourceId(), e);
            }
        } else if ("offline".equalsIgnoreCase(n.getSourceType())) {
            // 离线事件：用通知创建时刻近似为事件时刻
            b.eventTime(n.getCreateTime());
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
        ctxTpl.setVariable("alarmLevel", ctx.getAlarmLevel());
        ctxTpl.setVariable("content", ctx.getAlarmTitle());
        ctxTpl.setVariable("eventTime", ctx.getEventTime());
        ctxTpl.setVariable("linkUrl", buildLinkUrl(ctx));
        ctxTpl.setVariable("headerStyle",
            isAlarmSourceType(ctx.getSourceType())
                ? "background:#f56c6c;color:#fff;"
                : "background:#e6a23c;color:#fff;");

        return templateEngine.process("alarm-notify", ctxTpl);
    }

    private String buildLinkUrl(NotifyContext ctx) {
        if (ctx.getSourceId() == null) {
            return null;
        }
        if (isAlarmSourceType(ctx.getSourceType())) {
            return "/alarm/realtime?alarmId=" + ctx.getSourceId();
        }
        return "/basic/device?deviceId=" + ctx.getSourceId();
    }

    private String defaultStr(String s, String defaultVal) {
        return (s == null || s.isEmpty()) ? defaultVal : s;
    }

    /**
     * 判断 sourceType 是否为告警类（阈值/综合）。
     *
     * <p>告警类型拆分后 (ALARM → THRESHOLD/COMPREHENSIVE)，sourceType 取值为
     * "threshold" / "comprehensive"；"alarm" 仅为迁移期兼容（迁移脚本执行后不再出现）。
     * 三者均按告警路径渲染模板。</p>
     */
    private boolean isAlarmSourceType(String sourceType) {
        if (sourceType == null) {
            return false;
        }
        String s = sourceType.toLowerCase();
        return "threshold".equals(s)
            || "comprehensive".equals(s)
            || "alarm".equals(s);
    }
}
