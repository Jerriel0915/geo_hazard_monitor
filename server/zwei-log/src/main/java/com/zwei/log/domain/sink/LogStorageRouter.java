package com.zwei.log.domain.sink;

import com.zwei.log.domain.model.AbstractLogRecord;

/**
 * 日志存储路由器
 *
 * @author zwei
 */
public interface LogStorageRouter {

    void route(AbstractLogRecord record);
}
