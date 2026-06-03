package com.zwei.monitor.domain;

import lombok.Data;

import java.util.List;

/**
 * 分页查询 MQTT 客户端的响应。
 */
@Data
public class MqttClientPageResponse {
    private int pageNumber;
    private int pageSize;
    private int totalRow;
    private List<MqttClientInfo> list;
}
