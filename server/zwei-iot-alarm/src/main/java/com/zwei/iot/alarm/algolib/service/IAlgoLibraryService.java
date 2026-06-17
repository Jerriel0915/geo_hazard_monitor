package com.zwei.iot.alarm.algolib.service;

import com.zwei.iot.alarm.algolib.domain.AlgoInfo;

import java.util.List;

/**
 * 算法信息 Service。
 *
 * @author zwei
 */
public interface IAlgoLibraryService {

    List<AlgoInfo> selectList(AlgoInfo query);

    /** 详情（含版本列表，按 create_time DESC） */
    AlgoInfo selectDetailById(Long id);

    int insert(AlgoInfo algoInfo);

    int update(AlgoInfo algoInfo);

    /** 启停 */
    int updateStatus(Long id, Integer status, String updateBy);

    /** 删除算法（级联逻辑删版本，物理文件保留） */
    int deleteWithVersions(Long id);

    /** code 唯一校验 */
    boolean checkCodeUnique(String code, Long id);
}
