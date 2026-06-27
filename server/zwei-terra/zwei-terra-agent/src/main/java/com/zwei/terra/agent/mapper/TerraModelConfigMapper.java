package com.zwei.terra.agent.mapper;

import com.zwei.terra.agent.domain.TerraModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TerraModelConfigMapper {

    List<TerraModelConfig> selectList(TerraModelConfig query);

    TerraModelConfig selectById(Long id);

    TerraModelConfig selectActive();

    int insert(TerraModelConfig config);

    int update(TerraModelConfig config);

    int deactivateAll();

    int deleteById(Long id);

    TerraModelConfig checkNameUnique(@Param("name") String name, @Param("id") Long id);
}
