package com.zwei.iot.device.support;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.mapper.DeviceMapper;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 设备接入账号生成器
 */
@Component
public class DeviceAuthAccountGenerator {
    private static final String USERNAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int USERNAME_LENGTH = 6;
    private static final int PASSWORD_LENGTH = 8;
    private static final int MAX_RETRY = 50;

    private final SecureRandom secureRandom = new SecureRandom();
    private final DeviceMapper deviceMapper;

    public DeviceAuthAccountGenerator(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    public String generateUsername() {
        for (int i = 0; i < MAX_RETRY; i++) {
            String candidate = randomString(USERNAME_CHARS, USERNAME_LENGTH);
            if (deviceMapper.selectDeviceByAuthUsername(candidate) == null) {
                return candidate;
            }
        }
        throw new ServiceException("设备用户名生成失败，请稍后重试");
    }

    public String generatePassword() {
        return randomString(PASSWORD_CHARS, PASSWORD_LENGTH);
    }

    private String randomString(String chars, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return builder.toString();
    }
}
