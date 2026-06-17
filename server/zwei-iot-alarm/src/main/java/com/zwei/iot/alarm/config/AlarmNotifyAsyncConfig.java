package com.zwei.iot.alarm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 告警通知异步执行器
 *
 * 用于 {@code @Async("alarmNotifyExecutor")} 标注的通知分发方法，
 * 与 MQTT 数据接入、IoTDB 写入等核心线程池隔离，避免阻塞数据通道。
 */
@Configuration
public class AlarmNotifyAsyncConfig {

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
