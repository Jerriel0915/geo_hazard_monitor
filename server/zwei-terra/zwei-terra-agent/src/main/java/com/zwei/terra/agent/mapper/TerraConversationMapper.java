package com.zwei.terra.agent.mapper;

import com.zwei.terra.agent.domain.TerraConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TerraConversationMapper {

    List<TerraConversation> selectByUserId(@Param("userId") Long userId);

    TerraConversation selectById(Long id);

    int insert(TerraConversation conversation);

    int update(TerraConversation conversation);

    int deleteById(Long id);
}
