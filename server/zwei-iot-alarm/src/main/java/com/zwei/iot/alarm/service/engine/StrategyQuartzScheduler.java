package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.job.ComprehensiveAlarmQuartzJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

/**
 * Quartz 动态调度管理器，负责策略 → Quartz Job 的生命周期。
 *
 * @author zwei
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StrategyQuartzScheduler {

    private final Scheduler scheduler;
    private static final String JOB_GROUP = "ALARM_STRATEGY";
    private static final String DATA_STRATEGY_ID = "strategyId";

    /**
     * 注册或更新 CRON 策略的 Quartz 任务。
     */
    public void scheduleOrUpdate(AlarmStrategy strategy) {
        if (!"CRON".equals(strategy.getTriggerMode())) return;
        if (strategy.getCronExpression() == null || strategy.getCronExpression().isBlank()) return;

        JobKey jobKey = jobKey(strategy.getId());
        try {
            // 先删除旧任务
            scheduler.deleteJob(jobKey);

            JobDetail jobDetail = JobBuilder.newJob(ComprehensiveAlarmQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData(DATA_STRATEGY_ID, strategy.getId())
                .storeDurably()
                .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + strategy.getId(), JOB_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(strategy.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing())
                .build();

            scheduler.scheduleJob(jobDetail, trigger);

            if (strategy.getIsEnabled() != null && strategy.getIsEnabled() == 0) {
                scheduler.pauseJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("注册策略定时任务失败 strategyId={}", strategy.getId(), e);
        }
    }

    /**
     * 暂停指定策略的 Quartz 任务。
     */
    public void pause(Long strategyId) {
        try {
            scheduler.pauseJob(jobKey(strategyId));
        } catch (SchedulerException e) {
            log.error("暂停任务失败 strategyId={}", strategyId, e);
        }
    }

    /**
     * 恢复指定策略的 Quartz 任务。
     */
    public void resume(Long strategyId) {
        try {
            scheduler.resumeJob(jobKey(strategyId));
        } catch (SchedulerException e) {
            log.error("恢复任务失败 strategyId={}", strategyId, e);
        }
    }

    /**
     * 删除指定策略的 Quartz 任务。
     */
    public void unschedule(Long strategyId) {
        try {
            scheduler.deleteJob(jobKey(strategyId));
        } catch (SchedulerException e) {
            log.error("删除任务失败 strategyId={}", strategyId, e);
        }
    }

    private JobKey jobKey(Long strategyId) {
        return new JobKey("strategy_" + strategyId, JOB_GROUP);
    }
}
