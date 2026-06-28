package com.zwei.iot.timeseries.compute;

import com.zwei.common.domain.AlgoResult;

import java.util.Map;

/**
 * 算法执行工具接口 — 供计算属性脚本和综合告警脚本共用。
 *
 * <p>定义在 timeseries 模块以避免循环依赖；
 * 实现在 alarm 模块 ({@code ScriptAlgoOps})。
 *
 * @author zwei
 */
public interface IScriptAlgoOps {

    /**
     * 执行指定版本的算法方法。
     *
     * @param algoCode 算法编码
     * @param versionNo 版本号
     * @param method   方法名 (如 "calc_speed")
     * @param params   参数 Map
     * @return 算法执行结果
     */
    AlgoResult execute(String algoCode, String versionNo,
                       String method, Map<String, Object> params);

    /**
     * 执行算法的最新版本。
     *
     * @param algoCode 算法编码
     * @param method   方法名
     * @param params   参数 Map
     * @return 算法执行结果
     */
    AlgoResult executeLatest(String algoCode,
                             String method, Map<String, Object> params);
}
