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
 * 告警反馈记录 alarm_feedback
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmFeedback implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 告警记录ID
     */
    private Long alarmId;
    /**
     * 反馈文本内容
     */
    private String content;
    /**
     * 附件列表 JSON [{name,url,size}]
     */
    private String files;
    /**
     * 反馈人
     */
    private String operator;
    /**
     * 创建时间
     */
    private Date createTime;
}
