package com.zwei.iot.alarm.channel;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 通知接收人手机号/邮箱校验工具
 */
public final class NotifyRecipientValidator {

    private static final Pattern PHONE =
        Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private NotifyRecipientValidator() {}

    /**
     * 校验手机号
     * @return null 表示通过；否则返回错误码
     */
    public static String validatePhone(String phone) {
        if (StringUtils.isBlank(phone)) return "RECIPIENT_PHONE_MISSING";
        if (!PHONE.matcher(phone).matches()) return "RECIPIENT_PHONE_INVALID";
        return null;
    }

    /**
     * 校验邮箱
     * @return null 表示通过；否则返回错误码
     */
    public static String validateEmail(String email) {
        if (StringUtils.isBlank(email)) return "RECIPIENT_EMAIL_MISSING";
        if (!EMAIL.matcher(email).matches()) return "RECIPIENT_EMAIL_INVALID";
        return null;
    }
}
