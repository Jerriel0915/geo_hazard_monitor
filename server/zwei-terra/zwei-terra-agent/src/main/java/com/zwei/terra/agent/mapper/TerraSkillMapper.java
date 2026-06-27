package com.zwei.terra.agent.mapper;

import com.zwei.terra.agent.domain.TerraSkill;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TerraSkillMapper {

    List<TerraSkill> selectList(TerraSkill query);

    TerraSkill selectByKey(String skillKey);

    TerraSkill selectById(Long id);

    List<TerraSkill> selectEnabled();

    int insert(TerraSkill skill);

    int update(TerraSkill skill);

    int deleteById(Long id);
}
