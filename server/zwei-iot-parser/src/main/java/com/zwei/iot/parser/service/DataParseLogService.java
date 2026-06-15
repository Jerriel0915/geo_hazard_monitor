package com.zwei.iot.parser.service;

import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.mapper.DataParseLogMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class DataParseLogService {

    @Resource
    private DataParseLogMapper logMapper;

    public void save(DataParseLog log) {
        log.setCreateTime(new Date());
        logMapper.insert(log);
    }

    public void info(Long strategyId, String message, String data) {
        DataParseLog log = DataParseLog.builder()
                .strategyId(strategyId).logLevel("INFO")
                .message(message).data(data).build();
        save(log);
    }

    public void error(Long strategyId, String message, String data, String errorStack) {
        DataParseLog log = DataParseLog.builder()
                .strategyId(strategyId).logLevel("ERROR")
                .message(message).data(data).errorStack(errorStack).build();
        save(log);
    }

    public List<DataParseLog> listByCondition(Long strategyId, String logLevel,
                                               String startTime, String endTime) {
        return logMapper.selectByCondition(strategyId, logLevel, startTime, endTime);
    }

    public void clearByStrategyId(Long strategyId) {
        logMapper.deleteByStrategyId(strategyId);
    }
}
