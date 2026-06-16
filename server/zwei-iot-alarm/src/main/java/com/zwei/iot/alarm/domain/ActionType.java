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
