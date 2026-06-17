package com.zwei.iot.report.job;

import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.service.ReportGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 报告定时任务 — 三入口错峰执行。
 * <ul>
 *   <li>每周一 02:00 生成上一自然周</li>
 *   <li>每月 1 号 02:30 生成上一自然月</li>
 *   <li>每季度首月 1 号 03:00 生成上一自然季度 (1/4/7/10 月触发)</li>
 * </ul>
 *
 * 通过 application.yml 的 {@code zwei.report.schedule.{weekly,monthly,quarterly}-enabled} 控制开关, 默认开。
 */
@Configuration
@ConditionalOnProperty(name = "zwei.report.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ReportScheduleJob {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduleJob.class);

    private final ReportGenerationService generationService;

    public ReportScheduleJob(ReportGenerationService generationService) {
        this.generationService = generationService;
    }

    @Scheduled(cron = "0 0 2 * * MON")
    @ConditionalOnProperty(name = "zwei.report.schedule.weekly-enabled", havingValue = "true", matchIfMissing = true)
    public void generateWeekly() {
        log.info("[report-job] weekly trigger");
        generationService.generateAll(ReportType.WEEKLY);
    }

    @Scheduled(cron = "0 30 2 1 * *")
    @ConditionalOnProperty(name = "zwei.report.schedule.monthly-enabled", havingValue = "true", matchIfMissing = true)
    public void generateMonthly() {
        log.info("[report-job] monthly trigger");
        generationService.generateAll(ReportType.MONTHLY);
    }

    @Scheduled(cron = "0 0 3 1 1,4,7,10")
    @ConditionalOnProperty(name = "zwei.report.schedule.quarterly-enabled", havingValue = "true", matchIfMissing = true)
    public void generateQuarterly() {
        log.info("[report-job] quarterly trigger");
        generationService.generateAll(ReportType.QUARTERLY);
    }
}
