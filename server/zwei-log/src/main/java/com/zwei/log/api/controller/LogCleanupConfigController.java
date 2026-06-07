package com.zwei.log.api.controller;

import com.zwei.common.constant.CacheConstants;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.log.infrastructure.persistence.mysql.LogConfigMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 日志清理配置接口。
 * <p>
 * 允许管理员通过系统设置界面实时调整日志清理策略（开关、保留天数），
 * 配置持久化到 sys_config 表并通过 Redis 缓存立即生效。
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogCleanupConfigController {

    private static final String KEY_ENABLED = "log.cleanup.enabled";
    private static final String KEY_RETENTION = "log.cleanup.retention-days";
    private static final String KEY_CRON = "log.cleanup.cron";

    private final RedisCache redisCache;
    private final LogConfigMapper logConfigMapper;

    public LogCleanupConfigController(RedisCache redisCache, LogConfigMapper logConfigMapper) {
        this.redisCache = redisCache;
        this.logConfigMapper = logConfigMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:config:query')")
    @GetMapping("/cleanup-config")
    public AjaxResult getConfig() {
        String enabled = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_ENABLED);
        String retention = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_RETENTION);
        String cron = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_CRON);
        return AjaxResult.success(Map.of(
                "enabled", enabled != null ? Boolean.parseBoolean(enabled) : true,
                "retentionDays", retention != null ? Integer.parseInt(retention) : 30,
                "cron", cron != null ? cron : "0 0 3 * * ?"
        ));
    }

    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @PutMapping("/cleanup-config")
    public AjaxResult updateConfig(@RequestBody Map<String, Object> body) {
        if (body.containsKey("enabled")) {
            String val = String.valueOf(body.get("enabled"));
            redisCache.setCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_ENABLED, val);
            logConfigMapper.upsertConfig(KEY_ENABLED, val, "是否启用日志定时清理任务");
        }
        if (body.containsKey("retentionDays")) {
            String val = String.valueOf(body.get("retentionDays"));
            redisCache.setCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_RETENTION, val);
            logConfigMapper.upsertConfig(KEY_RETENTION, val, "日志保留天数");
        }
        if (body.containsKey("cron")) {
            String val = String.valueOf(body.get("cron"));
            redisCache.setCacheObject(CacheConstants.SYS_CONFIG_KEY + KEY_CRON, val);
            logConfigMapper.upsertConfig(KEY_CRON, val, "清理执行 cron 表达式");
        }
        return AjaxResult.success("配置已更新");
    }
}
