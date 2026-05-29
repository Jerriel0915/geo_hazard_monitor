package com.zwei.iot.device.support;

import com.zwei.iot.device.mapper.DeviceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceAuthAccountGenerator 单元测试")
class DeviceAuthAccountGeneratorTest {

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private DeviceAuthAccountGenerator generator;

    @Test
    @DisplayName("应生成6位大写字母数字用户名和8位密码")
    void shouldGenerateUsernameAndPasswordWithExpectedLength() {
        when(deviceMapper.selectDeviceByAuthUsername(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);

        String username = generator.generateUsername();
        String password = generator.generatePassword();

        assertEquals(6, username.length());
        assertEquals(8, password.length());
        assertTrue(username.matches("[A-Z0-9]{6}"));
        assertTrue(password.matches("[A-Za-z0-9]{8}"));
    }
}
