package com.zwei.terra.agent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TerraSkillToolMapper {

    List<Map<String, Object>> selectBySkillId(Long skillId);

    int insert(@Param("skillId") Long skillId, @Param("toolId") Long toolId);

    int deleteBySkillId(Long skillId);
}
