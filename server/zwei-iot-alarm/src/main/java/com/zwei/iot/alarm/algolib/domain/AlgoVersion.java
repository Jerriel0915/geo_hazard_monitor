package com.zwei.iot.alarm.algolib.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 算法版本表 algo_version。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoVersion extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long algoId;
    private String versionNo;
    /** 相对路径，如 algo-lib/2026/06/17/uuid.zip */
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String sha256;
    /** 解压后的工作目录相对路径 (相对于 RuoYiConfig.profile) */
    private String workPath;
    private Integer delFlag;
}
