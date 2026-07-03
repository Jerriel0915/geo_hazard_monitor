package com.zwei.framework.web.service;

import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.zwei.common.constant.CacheConstants;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.common.exception.ServiceException;
import com.zwei.system.service.ISysConfigService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 短信验证码服务 — 登录验证码发送与校验
 * <p>
 * 独立于 zwei-iot-alarm 的 SMS 通知通道，使用相同的 sys_config 凭证配置，
 * 通过独立的 Aliyun SMS SDK 实例发送，避免模块间耦合。
 *
 * @author zwei
 */
@Component
public class SysSmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SysSmsCodeService.class);

    /** 中国大陆手机号正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;

    /** 验证码 TTL（分钟） */
    private static final int CODE_TTL_MINUTES = 5;

    /** 同手机号发送间隔（秒） */
    private static final int SEND_INTERVAL_SECONDS = 60;

    /** 同手机号每日上限 */
    private static final int DAILY_LIMIT = 10;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    private volatile Client smsClient;
    private volatile String cachedAccessKeyId;

    /**
     * 发送短信验证码
     *
     * @param phone 目标手机号
     */
    public void sendCode(String phone) {
        validatePhone(phone);
        checkRateLimit(phone);

        String code = generateCode();
        String templateCode = getRequiredConfig("notify.sms.template.login");
        String accessKeyId = getRequiredConfig("notify.sms.access-key-id");
        String accessKeySecret = getRequiredConfig("notify.sms.access-key-secret");
        String signName = getRequiredConfig("notify.sms.sign-name");

        // 缓存验证码
        String codeKey = CacheConstants.SMS_CODE_KEY + phone;
        redisCache.setCacheObject(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // 发送短信
        try {
            Client client = getOrBuildClient(accessKeyId, accessKeySecret);
            String templateParam = JSON.toJSONString(Collections.singletonMap("code", code));
            SendSmsRequest req = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam(templateParam);
            SendSmsResponse resp = client.sendSms(req);
            SendSmsResponseBody body = resp.getBody();
            String respCode = body != null ? body.getCode() : null;
            if (!"OK".equals(respCode)) {
                String respMsg = body != null ? body.getMessage() : "null body";
                log.error("短信发送失败: phone={}, code={}, msg={}", phone, respCode, respMsg);
                redisCache.deleteObject(codeKey);
                throw new ServiceException("短信发送失败");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信发送异常: phone={}", phone, e);
            redisCache.deleteObject(codeKey);
            throw new ServiceException("短信发送异常");
        }

        // 记录发送频率和日上限
        recordSendStats(phone);

        log.info("短信验证码已发送: phone={}", maskPhone(phone));
    }

    /**
     * 校验短信验证码
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @throws ServiceException 验证失败
     */
    public void verifyCode(String phone, String code) {
        if (StringUtils.isBlank(code)) {
            throw new ServiceException("验证码不能为空");
        }
        String key = CacheConstants.SMS_CODE_KEY + phone;
        String cachedCode = redisCache.getCacheObject(key);
        if (cachedCode == null) {
            throw new ServiceException("验证码已过期或未发送");
        }
        if (!cachedCode.equals(code)) {
            throw new ServiceException("验证码错误");
        }
        // 一次性消费
        redisCache.deleteObject(key);
    }

    // ---- private helpers ----

    private void validatePhone(String phone) {
        if (StringUtils.isBlank(phone) || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new ServiceException("手机号格式不正确");
        }
    }

    private void checkRateLimit(String phone) {
        String limitKey = CacheConstants.SMS_LIMIT_KEY + phone;
        if (redisCache.hasKey(limitKey)) {
            throw new ServiceException("发送过于频繁，请60秒后重试");
        }
        String dailyKey = CacheConstants.SMS_DAILY_KEY + phone;
        Long dailyCount = redisCache.getCacheObject(dailyKey);
        if (dailyCount != null && dailyCount >= DAILY_LIMIT) {
            throw new ServiceException("今日发送次数已达上限");
        }
    }

    private void recordSendStats(String phone) {
        String limitKey = CacheConstants.SMS_LIMIT_KEY + phone;
        redisCache.setCacheObject(limitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        String dailyKey = CacheConstants.SMS_DAILY_KEY + phone;
        Long dailyCount = redisCache.redisTemplate.opsForValue().increment(dailyKey, 1);
        // 每次 increment 后都设置过期时间（idempotent），消除 TTL 初始化竞态窗口
        if (dailyCount != null) {
            redisCache.redisTemplate.expire(dailyKey, getSecondsUntilMidnight(), TimeUnit.SECONDS);
        }
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private Client getOrBuildClient(String accessKeyId, String accessKeySecret) throws Exception {
        if (smsClient != null && Objects.equals(cachedAccessKeyId, accessKeyId)) {
            return smsClient;
        }
        synchronized (this) {
            if (smsClient == null || !Objects.equals(cachedAccessKeyId, accessKeyId)) {
                Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint("dysmsapi.aliyuncs.com");
                smsClient = new Client(config);
                cachedAccessKeyId = accessKeyId;
            }
        }
        return smsClient;
    }

    private String getRequiredConfig(String key) {
        String value = configService.selectConfigByKey(key);
        if (StringUtils.isBlank(value)) {
            throw new ServiceException("系统配置缺失: " + key);
        }
        return value;
    }

    private long getSecondsUntilMidnight() {
        java.time.LocalTime now = java.time.LocalTime.now();
        return java.time.Duration.between(now, java.time.LocalTime.MAX).getSeconds() + 1;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
