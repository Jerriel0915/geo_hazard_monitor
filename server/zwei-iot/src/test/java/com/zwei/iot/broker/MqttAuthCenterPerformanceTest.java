package com.zwei.iot.broker;

import com.zwei.iot.broker.component.MqttAuthFailureGuard;
import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.broker.config.MqttAuthCenterProperties;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.handler.MqttServerAuthHandler;
import com.zwei.iot.broker.service.MqttDeviceAuthService;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.DeviceAuthLogService;
import com.zwei.iot.device.service.IDeviceSensorService;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MQTT 鉴权中心性能烟测")
class MqttAuthCenterPerformanceTest {

    @Test
    @DisplayName("200 并发鉴权应全部成功且耗时可接受")
    void authenticate_shouldHandleBurstTraffic() throws Exception {
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        DeviceAuthLogService deviceAuthLogService = mock(DeviceAuthLogService.class);
        IDeviceSensorService deviceSensorService = mock(IDeviceSensorService.class);
        MqttServer mqttServer = mock(MqttServer.class);
        Device device = new Device();
        device.setId(101L);
        device.setAuthUsername("A7K9P2");
        device.setAuthPassword("m4T9x2Q8");
        device.setAuthStatus(1);
        device.setProtocolType("MQTT");
        when(deviceMapper.selectDeviceByAuthUsername(anyString())).thenReturn(device);

        MqttAuthCenterProperties properties = new MqttAuthCenterProperties();
        properties.setDisconnectPreviousClient(false);
        MqttExceptionReporter mqttExceptionReporter = new MqttExceptionReporter();
        MqttDeviceAuthService authService = new MqttDeviceAuthService(
                deviceMapper,
                deviceAuthLogService,
                deviceSensorService,
                new MqttDeviceSessionRegistry(),
                new MqttAuthFailureGuard(properties),
                properties,
                new StaticListableBeanFactory() {{
                    addBean("mqttServer", mqttServer);
                }}.getBeanProvider(MqttServer.class),
                mqttExceptionReporter
        );
        MqttServerAuthHandler authHandler = new MqttServerAuthHandler(authService, mqttExceptionReporter);

        // 使用固定线程池模拟设备短时间批量接入，验证核心鉴权路径不会出现竞争性失败。
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            final int index = i;
            tasks.add(() -> authHandler.authenticate(
                    mock(ChannelContext.class, Mockito.RETURNS_DEEP_STUBS),
                    "client-" + index,
                    "client-" + index,
                    "A7K9P2",
                    "m4T9x2Q8"
            ));
        }

        long start = System.nanoTime();
        List<Future<Boolean>> results = executorService.invokeAll(tasks);
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(30, TimeUnit.SECONDS));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        // 逐个获取 Future 结果，确保所有并发任务都真正完成且没有被异常吞掉。
        long successCount = results.stream().filter(future -> {
            try {
                return future.get();
            } catch (Exception e) {
                return false;
            }
        }).count();

        assertEquals(200, successCount);
        assertTrue(elapsedMillis < 5000, "并发鉴权耗时超出 5 秒，实际耗时: " + elapsedMillis + "ms");
    }
}
