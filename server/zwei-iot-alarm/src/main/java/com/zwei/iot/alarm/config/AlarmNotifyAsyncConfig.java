package com.zwei.iot.alarm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 告警异步执行器配置。
 *
 * <p>两个独立线程池：
 * <ul>
 *   <li>{@code alarmEvalExecutor} — 告警评估，单线程保证同设备事件有序、
 *       避免并发 check-then-act 产生重复告警记录</li>
 *   <li>{@code alarmNotifyExecutor} — 通知分发（短信/邮件/SYSTEM），
 *       与评估线程隔离避免慢 IO 阻塞评估</li>
 * </ul>
 *
 * <p>均使用 {@code CallerRunsPolicy}：队列满时由调用线程同步执行，避免丢任务。
 */
@Configuration
public class AlarmNotifyAsyncConfig {

    @Bean("alarmEvalExecutor")
    public Executor alarmEvalExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);
        exec.setMaxPoolSize(1);
        exec.setQueueCapacity(1000);
        exec.setThreadNamePrefix("alarm-eval-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(60);
        exec.initialize();
        return exec;
    }

    @Bean("alarmNotifyExecutor")
    public Executor alarmNotifyExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(500);
        exec.setKeepAliveSeconds(60);
        exec.setThreadNamePrefix("alarm-notify-");
        // 队列满时由调用线程同步执行（避免丢任务）
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.initialize();
        return exec;
    }
}
