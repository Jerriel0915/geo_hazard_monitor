package com.zwei.iot.alarm.dispatch.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 跨模块查 sys_user / sys_user_role（只读，不破坏 RBAC 表）
 */
@Mapper
public interface AlarmRecipientQueryMapper {

    /** 按角色 ID 查活跃用户 */
    List<Long> selectUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    /** 按部门 ID 查活跃用户 */
    List<Long> selectUserIdsByDeptIds(@Param("deptIds") List<String> deptIds);

    /** 全部活跃用户（'*' 通配时用） */
    List<Long> selectAllActiveUserIds();
}
