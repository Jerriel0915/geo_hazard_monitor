package com.zwei.terra.core.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    private boolean success;
    private Object result;
    private String error;

    public static ToolResult success(Object result) {
        return ToolResult.builder().success(true).result(result).build();
    }

    public static ToolResult failure(String error) {
        return ToolResult.builder().success(false).error(error).build();
    }
}
