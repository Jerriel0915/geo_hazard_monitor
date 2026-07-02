package com.zwei.iot.parser.service;

import com.zwei.iot.parser.config.ParserProperties;
import com.zwei.iot.parser.dto.LastRunTimeEntry;
import com.zwei.iot.parser.mapper.DataParseLogMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 策略「最近运行时间」定时批量回写器 (B4 修复)。
 *
 * <p>每分钟查询各启用策略在 {@code iot_data_parse_log} 中最新一条 {@code create_time}，
 * 单语句批量 UPDATE 回写 {@code iot_data_parse_strategy.last_run_time}。
 *
 * <p>同步间隔由 {@code iot.parser.last-run-sync-ms} 配置（类型化于 {@link ParserProperties}，
 * 默认 60s），通过 {@code @Scheduled(fixedDelayString)} 读取。
 *
 * <p>相比「每条消息解析后同步写库」，定时批量方案将 DB 写压力从 O(消息量) 降到
 * O(策略数)/分钟，避免高吞吐场景（每秒数十条）下额外 DB 往返。
 *
 * <p>无日志的策略不更新（保留原值 / NULL），避免无意义写。
 */
@Slf4j
@Component
public class StrategyLastRunTimeUpdater {

    @Resource
    private DataParseStrategyMapper strategyMapper;
    @Resource
    private DataParseLogMapper logMapper;
    @Resource
    private ParserProperties parserProperties;

    @Scheduled(fixedDelayString = "${iot.parser.last-run-sync-ms:60000}", initialDelay = 60000)
    public void syncLastRunTime() {
        List<com.zwei.iot.parser.domain.DataParseStrategy> enabled;
        try {
            enabled = strategyMapper.selectEnabled();
        } catch (Exception e) {
            log.warn("selectEnabled failed, skip last_run_time sync", e);
            return;
        }
        if (enabled == null || enabled.isEmpty()) return;

        List<Long> strategyIds = enabled.stream().map(com.zwei.iot.parser.domain.DataParseStrategy::getId).toList();
        List<Map<String, Object>> rows;
        try {
            rows = logMapper.selectLatestCreateTimeByStrategyIds(strategyIds);
        } catch (Exception e) {
            log.warn("selectLatestCreateTimeByStrategyIds failed, skip sync", e);
            return;
        }
        if (rows == null || rows.isEmpty()) return;

        List<LastRunTimeEntry> entries = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object sid = row.get("strategyId");
            Object lrt = row.get("lastRunTime");
            if (sid == null || lrt == null) continue;
            if (!(lrt instanceof Date)) continue;
            entries.add(new LastRunTimeEntry(((Number) sid).longValue(), (Date) lrt));
        }
        if (entries.isEmpty()) return;

        try {
            strategyMapper.batchUpdateLastRunTime(entries);
            log.debug("Synced last_run_time for {} strategies", entries.size());
        } catch (Exception e) {
            log.warn("batchUpdateLastRunTime failed", e);
        }
    }
}
