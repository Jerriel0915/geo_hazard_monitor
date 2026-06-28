package com.zwei.iot.alarm.service.engine;

import java.util.Map;

/**
 * Python 算法执行结果。
 *
 * @param success 是否执行成功
 * @param data    算法返回数据（成功时非 null）
 * @param error   错误信息（失败时非 null）
 * @author zwei
 */
public record AlgoResult(boolean success, Map<String, Object> data, String error) {

    public static AlgoResult ok(Map<String, Object> data) {
        return new AlgoResult(true, data, null);
    }

    public static AlgoResult fail(String error) {
        return new AlgoResult(false, null, error);
    }
}
