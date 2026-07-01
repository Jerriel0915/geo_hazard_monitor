package com.zwei.iot.parser.dto;

import java.util.Date;

/**
 * 策略最近运行时间批量回写单元。
 *
 * @param strategyId  策略 ID
 * @param lastRunTime 该策略最新一条解析日志的创建时间
 */
public record LastRunTimeEntry(Long strategyId, Date lastRunTime) {}
