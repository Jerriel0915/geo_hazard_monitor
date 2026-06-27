package com.zwei.terra.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Terra 配置属性。
 *
 * <p>对应 application.yml 中 {@code terra.*} 前缀的配置项。</p>
 */
@Component
@ConfigurationProperties(prefix = "terra")
@Getter
@Setter
public class TerraProperties {

    private Skills skills = new Skills();
    private Chat chat = new Chat();

    @Getter
    @Setter
    public static class Skills {
        /** 技能文件系统基路径 */
        private String basePath = System.getProperty("user.home") + "/terra/skills";
    }

    @Getter
    @Setter
    public static class Chat {
        /** ReAct 循环最大轮数 */
        private int maxReactRounds = 10;
        /** 默认工具调用超时（秒） */
        private int defaultTimeoutSeconds = 30;
        /** 加载历史消息最大条数 */
        private int maxHistoryMessages = 20;
    }
}
