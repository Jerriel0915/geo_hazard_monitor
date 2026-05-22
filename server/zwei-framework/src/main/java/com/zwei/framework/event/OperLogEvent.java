package com.zwei.framework.event;

import com.zwei.system.domain.SysOperLog;

/**
 * 操作日志事件
 * 用于SSE实时推送日志
 *
 * @author zwei
 */
public record OperLogEvent(SysOperLog operLog) {}