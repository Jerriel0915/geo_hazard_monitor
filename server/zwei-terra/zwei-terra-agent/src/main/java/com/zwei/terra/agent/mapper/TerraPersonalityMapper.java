package com.zwei.terra.agent.mapper;

import com.zwei.terra.agent.domain.TerraPersonality;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TerraPersonalityMapper {

    List<TerraPersonality> selectList(TerraPersonality query);

    TerraPersonality selectById(Long id);

    TerraPersonality selectActiveCore();

    List<TerraPersonality> selectActiveRoles();

    int insert(TerraPersonality personality);

    int update(TerraPersonality personality);

    int deleteById(Long id);
}
