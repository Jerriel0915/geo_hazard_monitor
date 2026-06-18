package com.zwei.datashare.enums;

public enum ScopeType {
    HAZARD_POINT_GROUP("隐患点分组"),
    HAZARD_POINT("隐患点"),
    VENDOR("厂商"),
    DEVICE("设备");

    private final String label;

    ScopeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
