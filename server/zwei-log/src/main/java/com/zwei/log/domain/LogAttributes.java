package com.zwei.log.domain;

/**
 * 日志请求属性
 *
 * @author zwei
 */
public final class LogAttributes {

    public static final String TRACE_ID = "zwei.log.traceId";
    public static final String REQUEST_ID = "zwei.log.requestId";
    public static final String ASPECT_HANDLED = "zwei.log.aspectHandled";

    private LogAttributes() {
    }
}
