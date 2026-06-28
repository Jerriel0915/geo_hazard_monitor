package com.zwei.iot.alarm.service.engine;

import com.zwei.common.domain.AlgoResult;
import com.zwei.iot.timeseries.compute.IScriptAlgoOps;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ScriptAlgoOps implements IScriptAlgoOps {

    private final PythonAlgoExecutor executor;

    public ScriptAlgoOps(PythonAlgoExecutor executor) {
        this.executor = executor;
    }

    @Override
    public AlgoResult execute(String algoCode, String versionNo,
                               String method, Map<String, Object> params) {
        return executor.execute(algoCode, versionNo, method, params);
    }

    @Override
    public AlgoResult executeLatest(String algoCode,
                                     String method, Map<String, Object> params) {
        return executor.executeLatest(algoCode, method, params);
    }
}
