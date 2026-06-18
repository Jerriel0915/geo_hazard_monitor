package com.zwei.datashare.enums;

public enum RunStatus {
    SUCCESS("成功"),
    ERROR("失败"),
    TIMEOUT("超时");

    private final String label;

    RunStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
