package com.zwei.iot.alarm.algolib.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.List;

/**
 * 算法信息表 algo_info。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoInfo extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String description;
    /** 状态: 0-停用, 1-启用 */
    private Integer status;
    private Integer delFlag;

    // ── 非持久化字段（由 list 联表 algo_version 填充）──
    /** 该算法下未删除版本数 */
    private Integer versionCount;
    /** 最近一次上传的 version_no */
    private String latestVersionNo;
    /** 最近一次上传 create_time */
    private java.util.Date latestUploadTime;

    // ── 详情接口填充的版本列表（仅 selectDetailById 使用）──
    private List<AlgoVersion> versions;
}
