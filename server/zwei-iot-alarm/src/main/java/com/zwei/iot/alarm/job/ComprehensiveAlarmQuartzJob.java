package com.zwei.iot.alarm.job;

import com.zwei.common.utils.spring.SpringUtils;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.service.engine.ComprehensiveAlarmExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz Job 类，按策略 cronExpression 触发综合告警脚本执行。
 *
 * <p>使用 {@link SpringUtils#getBean} 获取 Spring bean，因为 Quartz Job 实例由
 * Quartz 框架创建而非 Spring 容器管理。
 *
 * @author zwei
 */
@Slf4j
@DisallowConcurrentExecution
public class ComprehensiveAlarmQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long strategyId = context.getJobDetail().getJobDataMap().getLong("strategyId");

        AlarmStrategyMapper mapper = SpringUtils.getBean(AlarmStrategyMapper.class);
        ComprehensiveAlarmExecutionService execService =
            SpringUtils.getBean(ComprehensiveAlarmExecutionService.class);

        AlarmStrategy strategy = mapper.selectStrategyById(strategyId);
        if (strategy == null || strategy.getIsEnabled() == null || strategy.getIsEnabled() == 0) {
            log.debug("策略不存在或已停用，跳过: strategyId={}", strategyId);
            return;
        }

        try {
            execService.execute(strategy, null, "CRON");
        } catch (Exception e) {
            log.error("策略 CRON 执行失败 strategyId={}", strategyId, e);
            throw new JobExecutionException("策略执行失败: " + strategyId, e);
        }
    }
}
