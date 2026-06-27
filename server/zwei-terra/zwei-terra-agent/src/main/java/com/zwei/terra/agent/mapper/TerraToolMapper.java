package com.zwei.terra.agent.mapper;

import com.zwei.terra.agent.domain.TerraTool;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TerraToolMapper {

    List<TerraTool> selectList(TerraTool query);

    TerraTool selectByKey(String toolKey);

    TerraTool selectById(Long id);

    List<TerraTool> selectEnabled();

    int insert(TerraTool tool);

    int update(TerraTool tool);

    int deleteById(Long id);
}
