package com.zwei.iot.timeseries.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis 原生响应解析工具。
 *
 * <p>处理 XAUTOCLAIM / XPENDING / XCLAIM 返回的嵌套 byte[] / List 结构,
 * 将 Redis Connection.execute 的原始返回值转换为 Java List。
 */
public final class RedisReplyParser {

    private RedisReplyParser() {}

    /**
     * 安全解析 Redis 响应为 List; 解析失败返回空列表。
     */
    public static List<Object> parseList(Object reply) {
        Object result = parse(reply);
        return result instanceof List<?> ? castList(result) : List.of();
    }

    /**
     * 递归解析 Redis 嵌套响应: List 递归处理, byte[] / null 原样返回。
     */
    static Object parse(Object reply) {
        if (reply instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(parse(item));
            }
            return result;
        }
        return reply;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object obj) {
        return (List<Object>) obj;
    }
}
