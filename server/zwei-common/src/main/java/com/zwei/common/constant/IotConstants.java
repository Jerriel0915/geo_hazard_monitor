package com.zwei.common.constant;

/**
 * IOT物联网模块常量信息
 *
 * @author zwei
 */
public class IotConstants
{
    /**
     * 隐患点状态：监测中
     */
    public static final int HAZARD_POINT_STATUS_MONITORING = 1;

    /**
     * 隐患点状态：停测中
     */
    public static final int HAZARD_POINT_STATUS_PAUSED = 2;

    /**
     * 隐患点状态：已完结
     */
    public static final int HAZARD_POINT_STATUS_COMPLETED = 3;

    /**
     * 隐患点操作类型：停测
     */
    public static final String OPERATION_PAUSE = "pause";

    /**
     * 隐患点操作类型：恢复
     */
    public static final String OPERATION_RESUME = "resume";

    /**
     * 隐患点操作类型：完结
     */
    public static final String OPERATION_COMPLETE = "complete";
}
