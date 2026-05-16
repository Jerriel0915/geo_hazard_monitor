package com.zwei.iot.service.impl;

import com.zwei.iot.domain.MonitorContent;
import com.zwei.iot.mapper.MonitorContentMapper;
import com.zwei.iot.service.IMonitorContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监测内容Service实现类
 * <p>
 * 提供监测内容的管理操作实现，包括查询、新增、修改、删除等。
 *
 * @author zwei
 */
@Service
public class MonitorContentServiceImpl implements IMonitorContentService {

    /**
     * 注入监测内容Mapper
     */
    private final MonitorContentMapper monitorContentMapper;

    @Autowired
    public MonitorContentServiceImpl(MonitorContentMapper monitorContentMapper) {
        this.monitorContentMapper = monitorContentMapper;
    }

    /**
     * 查询监测内容列表
     *
     * @param monitorContent 查询条件
     * @return 监测内容列表
     */
    @Override
    public List<MonitorContent> selectMonitorContentList(MonitorContent monitorContent) {
        return monitorContentMapper.selectMonitorContentList(monitorContent);
    }

    /**
     * 查询所有监测内容列表（不分页）
     *
     * @param monitorTypeId 监测类型ID（可选）
     * @return 监测内容列表
     */
    @Override
    public List<MonitorContent> selectMonitorContentAll(Long monitorTypeId) {
        return monitorContentMapper.selectMonitorContentAll(monitorTypeId);
    }

    /**
     * 根据ID查询监测内容详情
     *
     * @param id 监测内容ID
     * @return 监测内容详情
     */
    @Override
    public MonitorContent selectMonitorContentById(Long id) {
        return monitorContentMapper.selectMonitorContentById(id);
    }

    /**
     * 根据编码查询监测内容
     *
     * @param code 监测内容编码
     * @return 监测内容详情
     */
    @Override
    public MonitorContent selectMonitorContentByCode(String code) {
        return monitorContentMapper.selectMonitorContentByCode(code);
    }

    /**
     * 新增监测内容
     *
     * @param monitorContent 监测内容信息
     * @return 影响行数
     */
    @Override
    public int insertMonitorContent(MonitorContent monitorContent) {
        return monitorContentMapper.insertMonitorContent(monitorContent);
    }

    /**
     * 修改监测内容
     *
     * @param monitorContent 监测内容信息
     * @return 影响行数
     */
    @Override
    public int updateMonitorContent(MonitorContent monitorContent) {
        return monitorContentMapper.updateMonitorContent(monitorContent);
    }

    /**
     * 删除监测内容（逻辑删除）
     *
     * @param id 监测内容ID
     * @return 影响行数
     */
    @Override
    public int deleteMonitorContentById(Long id) {
        return monitorContentMapper.deleteMonitorContentById(id);
    }

    /**
     * 批量删除监测内容（逻辑删除）
     *
     * @param ids 需要删除的监测内容ID数组
     * @return 影响行数
     */
    @Override
    public int deleteMonitorContentByIds(Long[] ids) {
        return monitorContentMapper.deleteMonitorContentByIds(ids);
    }

    /**
     * 校验监测内容编码是否唯一
     *
     * @param monitorContent 监测内容信息
     * @return true-唯一，false-已存在
     */
    @Override
    public boolean checkMonitorContentCodeUnique(MonitorContent monitorContent) {
        Long id = monitorContent.getId() == null ? 0L : monitorContent.getId();
        MonitorContent exist = monitorContentMapper.checkMonitorContentCodeUnique(monitorContent.getCode(), id);
        return exist == null;
    }
}