package com.zwei.log.mqtt.exception.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.constant.CacheConstants;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.log.api.service.ILogConfigService;
import com.zwei.log.mqtt.exception.domain.ExceptionLogExportVO;
import com.zwei.log.mqtt.exception.domain.ExceptionMessageLog;
import com.zwei.log.mqtt.exception.service.ExceptionMessageLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 异常报文接口。
 * <p>
 * 提供已认证但解析/报送失败的报文查询、导出与保留期配置，
 * 供"服务状态 → 异常报文"子页使用。
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/monitor/mqtt/exceptions")
public class MqttExceptionLogController {

    private static final String KEY_ENABLED = "mqtt.exception.cleanup.enabled";
    private static final String KEY_RETENTION = "mqtt.exception.retention-days";

    private static final int EXPORT_MAX_ROWS = 10_000;

    private final ExceptionMessageLogService exceptionLogService;
    private final RedisCache redisCache;
    private final ILogConfigService logConfigService;

    public MqttExceptionLogController(ExceptionMessageLogService exceptionLogService,
                                     RedisCache redisCache,
                                     ILogConfigService logConfigService) {
        this.exceptionLogService = exceptionLogService;
        this.redisCache = redisCache;
        this.logConfigService = logConfigService;
    }

    /**
     * 分页查询异常报文（按接收时间倒序）。
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/page")
    public AjaxResult page(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int pageSize,
                           @RequestParam(required = false) String clientId,
                           @RequestParam(required = false) String topic,
                           @RequestParam(required = false) String rejectReason,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        ExceptionMessageLogService.PageResult result = exceptionLogService.query(
                page, pageSize, clientId, topic, rejectReason, startTime, endTime);
        return AjaxResult.success(result);
    }

    /**
     * 导出异常报文列表。
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @Log(title = "异常报文", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String clientId,
                       @RequestParam(required = false) String topic,
                       @RequestParam(required = false) String rejectReason,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        List<ExceptionMessageLog> list = exceptionLogService.selectAll(
                clientId, topic, rejectReason, startTime, endTime);
        if (list.size() > EXPORT_MAX_ROWS) {
            throw new ServiceException("导出数据量过大（" + list.size() + " 条），请缩小查询范围");
        }
        List<ExceptionLogExportVO> exportList = new ArrayList<>(list.size());
        for (ExceptionMessageLog item : list) {
            ExceptionLogExportVO vo = new ExceptionLogExportVO();
            vo.setReceiveTime(item.getReceiveTime());
            vo.setClientId(item.getClientId());
            vo.setUsername(item.getUsername());
            vo.setDeviceId(item.getDeviceId());
            vo.setTopic(item.getTopic());
            vo.setRejectStage(item.getRejectStage());
            vo.setRejectReason(item.getRejectReason());
            vo.setPayload(item.getPayload());
            vo.setPayloadSize(item.getPayloadSize());
            vo.setCreateTime(item.getCreateTime());
            exportList.add(vo);
        }
        ExcelUtil<ExceptionLogExportVO> util = new ExcelUtil<>(ExceptionLogExportVO.class);
        util.exportExcel(response, exportList, "异常报文");
    }

    /**
     * 查询保留期配置。
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/retention-config")
    public AjaxResult getConfig() {
        String enabled = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_ENABLED);
        String retention = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_RETENTION);
        return AjaxResult.success(Map.of(
                "enabled", enabled != null ? Boolean.parseBoolean(enabled) : true,
                "retentionDays", retention != null ? Integer.parseInt(retention) : 60
        ));
    }

    /**
     * 更新保留期配置。
     * <p>
     * 先写 DB（sys_config 持久化），再刷新 Redis 缓存，确保 DB 为数据源。
     * DB 失败时 Redis 保持旧值，不会出现缓存领先于持久化的不一致。
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @PutMapping("/retention-config")
    public AjaxResult updateConfig(@RequestBody Map<String, Object> body) {
        if (body.containsKey("enabled")) {
            String val = String.valueOf(body.get("enabled"));
            logConfigService.upsertConfig(KEY_ENABLED, val, "是否启用异常报文定时清理");
            redisCache.setCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_ENABLED, val);
        }
        if (body.containsKey("retentionDays")) {
            String val = String.valueOf(body.get("retentionDays"));
            logConfigService.upsertConfig(KEY_RETENTION, val, "异常报文保留天数");
            redisCache.setCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_RETENTION, val);
        }
        return AjaxResult.success("配置已更新");
    }
}
