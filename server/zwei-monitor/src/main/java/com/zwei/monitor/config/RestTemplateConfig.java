package com.zwei.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * MQTT HTTP API 专用 RestTemplate。
 * <p>
 * 针对 localhost:18083 的 mica-mqtt HTTP API 进行优化配置，
 * 包括 Basic Auth 认证头和连接/读取超时。
 */
@Configuration(proxyBeanMethods = false)
public class RestTemplateConfig {

    @Bean("mqttHttpRestTemplate")
    public RestTemplate mqttHttpRestTemplate(MqttHttpApiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        RestTemplate restTemplate = new RestTemplate(factory);
        // 设置 root URI
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(properties.baseUrl()));

        // 为每个请求自动附加 Basic Auth
        if (properties.getBasicAuth().isEnable()) {
            String auth = properties.getBasicAuth().getUsername() + ":" + properties.getBasicAuth().getPassword();
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().set("Authorization", "Basic " + encoded);
                return execution.execute(request, body);
            });
        }

        return restTemplate;
    }
}
