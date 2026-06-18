package com.zwei.iot.alarm.algolib.service;

import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 算法版本 Service。
 *
 * @author zwei
 */
public interface IAlgoVersionService {

    List<AlgoVersion> selectByAlgoId(Long algoId);

    AlgoVersion selectById(Long id);

    /**
     * 上传新版本：
     * 1. 校验文件类型（仅 zip）与大小（≤100MB）
     * 2. 校验 algoId 存在且未删除
     * 3. 校验 versionNo 在该算法下唯一
     * 4. 落盘到 {zwei.profile}/algo-lib/yyyy/MM/dd/{uuid}.zip
     * 5. 计算 SHA256，写入 algo_version
     *
     * @return 新创建的版本 ID
     */
    Long upload(Long algoId, String versionNo, String remark,
                MultipartFile file, String createBy);

    /** 逻辑删除单个版本（不删物理文件） */
    int delete(Long id);

    /** 校验版本号唯一 */
    boolean checkVersionUnique(Long algoId, String versionNo);
}
