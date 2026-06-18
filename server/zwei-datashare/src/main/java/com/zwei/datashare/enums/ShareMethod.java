package com.zwei.datashare.enums;

public enum ShareMethod {
    UNIFIED_PUSH("统一化数据推送"),
    CUSTOM_PUSH("定制化数据推送"),
    UNIFIED_SERVICE("统一化数据服务"),
    CUSTOM_SERVICE("定制化数据服务");

    private final String label;

    ShareMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
