package com.zwei.framework.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import com.zwei.framework.aspectj.LogAspect;
import jakarta.annotation.PostConstruct;

/**
 * LogAspect事件发布配置
 *
 * @author zwei
 */
@Configuration
public class LogAspectConfig {

    private final LogAspect logAspect;
    private final ApplicationEventPublisher eventPublisher;

    public LogAspectConfig(LogAspect logAspect, ApplicationEventPublisher eventPublisher) {
        this.logAspect = logAspect;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        logAspect.setEventPublisher(eventPublisher);
    }
}