package com.zwei.iot.alarm.dispatch.mapper;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知规则主表 Mapper（原生 MyBatis，不继承 BaseMapper）
 */
@Mapper
public interface AlarmDispatchRuleMapper {

    /** 按主键查询（del_flag=0） */
    AlarmDispatchRule selectById(Long id);

    /** 条件查询列表（del_flag=0 + 可选 name/eventType/isEnabled） */
    List<AlarmDispatchRule> selectListByWhere(@Param("where") AlarmDispatchRule where);

    /** 新增，useGeneratedKeys 回写 id */
    int insert(AlarmDispatchRule rule);

    /** 按主键更新 */
    int updateById(AlarmDispatchRule rule);

    /** 逻辑删除（del_flag=1） */
    int logicDeleteById(@Param("id") Long id);

    /** 启停切换 */
    int updateEnabled(@Param("id") Long id, @Param("isEnabled") Integer isEnabled);
}
