package com.zwei.iot.alarm.dispatch.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 通知规则详情返回（含关联表展开）
 */
@Data
public class AlarmDispatchRuleDetailVO {

    private Long id;
    private String name;
    private String eventType;
    private List<String> alarmLevels;
    private List<String> channels;

    private List<String> hazardPointIds;
    private List<HazardPointOption> hazardPointOptions;

    private List<String> deviceIds;
    private List<DeviceOption> deviceOptions;

    private RecipientDetail recipients;

    private Integer isEnabled;
    private String remark;
    private Date createTime;
    private String createBy;

    @Data
    public static class HazardPointOption {
        private String id;
        private String name;
    }

    @Data
    public static class DeviceOption {
        private String id;
        private String name;
        private String code;
    }

    @Data
    public static class RecipientDetail {
        private List<RoleOption> roles;
        private List<DeptOption> depts;
        private List<UserOption> users;
        private boolean hasWildcardRole;
        private boolean hasWildcardDept;
        private boolean hasWildcardUser;
    }

    @Data
    public static class RoleOption {
        private String id;
        private String name;
    }

    @Data
    public static class DeptOption {
        private String id;
        private String name;
    }

    @Data
    public static class UserOption {
        private String id;
        private String name;
    }
}
