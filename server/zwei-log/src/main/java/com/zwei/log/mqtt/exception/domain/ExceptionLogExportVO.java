package com.zwei.log.mqtt.exception.domain;

import com.zwei.common.annotation.Excel;
import lombok.Setter;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 异常报文导出对象
 *
 * @author zwei
 */
@Setter
@Getter
public class ExceptionLogExportVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Excel(name = "接收时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date receiveTime;

    @Excel(name = "Client ID")
    private String clientId;

    @Excel(name = "用户名")
    private String username;

    @Excel(name = "设备ID")
    private Long deviceId;

    @Excel(name = "主题")
    private String topic;

    @Excel(name = "报错阶段")
    private String rejectStage;

    @Excel(name = "报错内容", width = 40)
    private String rejectReason;

    @Excel(name = "报文内容", width = 40)
    private String payload;

    @Excel(name = "大小")
    private Integer payloadSize;

    @Excel(name = "入库时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
