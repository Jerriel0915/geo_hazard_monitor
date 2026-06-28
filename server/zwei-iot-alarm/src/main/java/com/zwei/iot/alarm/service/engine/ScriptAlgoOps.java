package com.zwei.iot.alarm.service.engine;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ScriptAlgoOps {

    private final PythonAlgoExecutor executor;

    public ScriptAlgoOps(PythonAlgoExecutor executor) {
        this.executor = executor;
    }

    public AlgoResult execute(String algoCode, String versionNo,
                               String method, Map<String, Object> params) {
        return executor.execute(algoCode, versionNo, method, params);
    }

    public AlgoResult executeLatest(String algoCode,
                                     String method, Map<String, Object> params) {
        return executor.executeLatest(algoCode, method, params);
    }
}
