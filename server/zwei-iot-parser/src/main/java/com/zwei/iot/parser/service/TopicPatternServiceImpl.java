package com.zwei.iot.parser.service;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TopicPatternServiceImpl implements ITopicPatternService {

    private static final Pattern NEVER_MATCH = Pattern.compile("(?!)");

    @Resource
    private DataParseStrategyMapper strategyMapper;

    private final AtomicReference<CachedPatterns> cache = new AtomicReference<>();

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public boolean matches(String topic) {
        CachedPatterns p = cache.get();
        return p != null && p.pattern.matcher(topic == null ? "" : topic).matches();
    }

    @Override
    public TopicComponents resolveTopic(String topic) {
        CachedPatterns p = cache.get();
        if (p == null || p.sourceTypes.isEmpty()) {
            return null;
        }
        Matcher m = p.pattern.matcher(topic == null ? "" : topic);
        if (!m.matches()) {
            return null;
        }
        return new TopicComponents(m.group(1), m.group(2), m.group(3));
    }

    @Override
    public Set<String> getActiveSourceTypes() {
        CachedPatterns p = cache.get();
        return p == null ? Collections.emptySet() : p.sourceTypes;
    }

    @Override
    public void reload() {
        List<String> sourceTypes = strategyMapper.selectDistinctSourceTypes();
        Set<String> set = sourceTypes != null && !sourceTypes.isEmpty()
                ? Set.copyOf(sourceTypes)
                : Collections.emptySet();
        cache.set(new CachedPatterns(set));
    }

    private static class CachedPatterns {
        final Set<String> sourceTypes;
        final Pattern pattern;

        CachedPatterns(Set<String> sourceTypes) {
            this.sourceTypes = sourceTypes;
            if (sourceTypes.isEmpty()) {
                this.pattern = NEVER_MATCH;
            } else {
                String prefix = sourceTypes.stream()
                        .map(Pattern::quote)
                        .collect(Collectors.joining("|"));
                this.pattern = Pattern.compile(
                        "^(" + prefix + ")/v1/([A-Za-z0-9_-]{1,64})/([A-Za-z0-9_-]{1,100})/updata$");
            }
        }
    }
}
