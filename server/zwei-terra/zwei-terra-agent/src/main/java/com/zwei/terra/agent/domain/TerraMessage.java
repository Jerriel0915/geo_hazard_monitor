package com.zwei.terra.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** Terra 对话消息 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerraMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long conversationId;

    /** 角色: user, assistant, tool */
    private String role;

    private String content;

    /** JSON string */
    private String toolCalls;

    private String toolCallId;

    private Integer tokensUsed;

    private Date createTime;
}
