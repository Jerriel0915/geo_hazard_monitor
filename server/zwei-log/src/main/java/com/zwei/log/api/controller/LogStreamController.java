package com.zwei.log.api.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.log.application.service.LogCenterService;
import com.zwei.log.application.service.LogReplayService;
import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.infrastructure.push.sse.LogStreamPublisher;

/**
 * 日志SSE接口
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogStreamController {

    private final LogStreamPublisher logStreamPublisher;
    private final LogCenterService logCenterService;
    private final LogReplayService logReplayService;

    public LogStreamController(LogStreamPublisher logStreamPublisher,
        LogCenterService logCenterService,
        LogReplayService logReplayService) {
        this.logStreamPublisher = logStreamPublisher;
        this.logCenterService = logCenterService;
        this.logReplayService = logReplayService;
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(value = "types", required = false) String types,
        @RequestParam(value = "subscriberKey", required = false) String subscriberKey,
        @RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader,
        HttpServletRequest request) {
        Set<LogType> logTypes = parseTypes(types);
        String resolvedSubscriberKey = resolveSubscriberKey(subscriberKey, logTypes, request);
        Long resumeEventId = logReplayService.resolveResumeEventId(resolvedSubscriberKey, logTypes, parseLastEventId(lastEventIdHeader));
        List<AbstractLogRecord> replayRecords = logReplayService.loadReplayRecords(logTypes, resumeEventId);
        return logStreamPublisher.subscribe(resolvedSubscriberKey, logTypes, replayRecords, resumeEventId);
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/stream/connections")
    public AjaxResult connectionCount() {
        return AjaxResult.success("成功", Collections.singletonMap("activeCount", logCenterService.getActiveStreamCount()));
    }

    private Set<LogType> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(types.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .map(String::toUpperCase)
            .map(LogType::valueOf)
            .collect(Collectors.toSet());
    }

    private Long parseLastEventId(String lastEventIdHeader) {
        if (StringUtils.isEmpty(lastEventIdHeader)) {
            return null;
        }
        try {
            return Long.parseLong(lastEventIdHeader);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveSubscriberKey(String subscriberKey, Set<LogType> logTypes, HttpServletRequest request) {
        if (StringUtils.isNotEmpty(subscriberKey)) {
            return subscriberKey;
        }
        String username = SecurityUtils.getUsername();
        if (StringUtils.isNotEmpty(username)) {
            String types = (logTypes == null || logTypes.isEmpty())
                ? "ALL"
                : logTypes.stream().map(Enum::name).sorted().collect(Collectors.joining("-"));
            return "user:" + username + ":" + types;
        }
        return "ip:" + request.getRemoteAddr();
    }
}
