package com.zwei.datashare.enums;

public enum StrategyStatus {
    ENABLED("已启用"),
    DISABLED("已停用");

    private final String label;

    StrategyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
