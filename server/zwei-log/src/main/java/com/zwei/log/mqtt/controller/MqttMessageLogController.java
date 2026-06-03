package com.zwei.log.mqtt.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.log.mqtt.MqttMessageLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MQTT 数据日志接口。
 * <p>
 * 提供设备监测消息的实时日志查询，支持按 clientId、topic 过滤，
 * 用于排查数据上报异常和观察消息流量。
 */
@RestController
@RequestMapping("/api/v1/monitor/mqtt/messages")
public class MqttMessageLogController {

    private final MqttMessageLogService messageLogService;

    public MqttMessageLogController(MqttMessageLogService messageLogService) {
        this.messageLogService = messageLogService;
    }

    /**
     * 分页查询 MQTT 数据日志（按接收时间倒序）。
     *
     * @param page     页码，默认 1
     * @param pageSize 每页大小，默认 20
     * @param clientId 可选：按 clientId 模糊过滤
     * @param topic    可选：按 topic 模糊过滤
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/page")
    public AjaxResult getMessages(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  @RequestParam(required = false) String clientId,
                                  @RequestParam(required = false) String topic) {
        MqttMessageLogService.PageResult result = messageLogService.query(page, pageSize, clientId, topic);
        return AjaxResult.success(result);
    }
}
