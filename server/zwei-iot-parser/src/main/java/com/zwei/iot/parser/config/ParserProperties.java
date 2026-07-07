package com.zwei.iot.parser.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据解析模块运行配置。
 *
 * <p>绑定前缀为 {@code iot.parser} 的配置项。</p>
 *
 * <ul>
 *   <li>{@code groovyPoolSize} — Groovy 脚本执行线程池大小，默认 4。
 *       解析任务彼此独立（脚本 + Binding 线程局部），可安全并发；
 *       过大可能增加 GC 与上下文切换开销，建议 2~8。</li>
 *   <li>{@code scriptCacheMaxSize} — 主解析链路编译缓存 LRU 上限，默认 200。
 *       key=策略主键，通常一个策略对应一条记录。</li>
 *   <li>{@code computedCacheMaxSize} — 计算属性编译缓存 LRU 上限，默认 500。
 *       key=脚本哈希，脚本内容变化即产生新条目。</li>
 * </ul>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "iot.parser")
public class ParserProperties {

    /** Groovy 脚本执行线程池大小。 */
    private int groovyPoolSize = 4;

    /** 主解析链路编译缓存 LRU 上限，默认 200 */
    private int scriptCacheMaxSize = 200;

    /** 计算属性编译缓存 LRU 上限，默认 500 */
    private int computedCacheMaxSize = 500;

    /**
     * 策略「最近运行时间」定时同步间隔（毫秒，默认 60s）。
     *
     * <p>由 {@link com.zwei.iot.parser.service.StrategyLastRunTimeUpdater} 的
     * {@code @Scheduled(fixedDelayString)} 读取同一属性键 {@code iot.parser.last-run-sync-ms}。
     * 此字段作为类型化配置的文档锚点，确保所有 {@code iot.parser.*} 配置项集中可发现。
     */
    private long lastRunSyncMs = 60000;
}
