package com.zwei.terra.agent.mapper;

import com.zwei.terra.agent.domain.TerraMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TerraMessageMapper {

    List<TerraMessage> selectByConversationId(@Param("conversationId") Long conversationId,
                                               @Param("limit") int limit);

    int insert(TerraMessage message);

    int updateConversationStats(@Param("conversationId") Long conversationId);
}
