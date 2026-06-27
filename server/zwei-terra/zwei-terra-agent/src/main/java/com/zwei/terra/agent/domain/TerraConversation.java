package com.zwei.terra.agent.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.Date;

/** Terra 对话会话 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerraConversation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    /** 状态: active, archived */
    private String status;

    private Date lastMessageTime;

    private Integer messageCount;

    private String delFlag;
}
