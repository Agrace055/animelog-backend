package com.animelog.backend.utils;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dm20151123.models.SingleSendMailRequest;
import com.aliyun.teaopenapi.models.Config;
import com.animelog.backend.config.AnimeLogProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 验证码工具类，提供邮箱和短信验证码的生成、存储和校验功能。<br>
 * 验证码使用 Redis 存储，有效期 10 分钟。
 */
@Component
public class VerificationCodeUtils {
    private static final Logger log = LoggerFactory.getLogger(VerificationCodeUtils.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int EXPIRE_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;
    private final AnimeLogProperties properties;

    public VerificationCodeUtils(StringRedisTemplate redisTemplate, AnimeLogProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 发送邮箱验证码（阿里云邮件推送 DirectMail）。
     * 未配置阿里云时仅生成验证码存入 Redis（调试模式）。
     */
    public void sendEmailCode(String email, String purpose) {
        String code = createAndStore("email", email, purpose);
        AnimeLogProperties.Aliyun aliyun = properties.aliyun();
        if (aliyun.accessKeyId() == null || aliyun.accessKeyId().isBlank()) {
            log.warn("[DEBUG] 邮件验证码 {} -> {}", email, code);
            return;
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(aliyun.accessKeyId())
                    .setAccessKeySecret(aliyun.accessKeySecret())
                    .setRegionId(aliyun.regionId());
            com.aliyun.dm20151123.Client client = new com.aliyun.dm20151123.Client(config);
            SingleSendMailRequest request = new SingleSendMailRequest()
                    .setAccountName(aliyun.dmAccountName())
                    .setFromAlias(aliyun.dmFromAlias())
                    .setAddressType(1)
                    .setToAddress(email)
                    .setSubject("AnimeLog 验证码")
                    .setHtmlBody("您的验证码为：<b>" + code + "</b>，" + EXPIRE_MINUTES + " 分钟内有效，请勿泄露。");
            client.singleSendMail(request);
        } catch (Exception e) {
            log.error("邮件验证码发送失败 -> {}: {}", email, e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    /**
     * 发送短信验证码（阿里云号码认证服务 - 短信认证）。
     * 未配置阿里云时仅生成验证码存入 Redis（调试模式）。
     */
    public void sendSmsCode(String phone, String purpose) {
        String code = createAndStore("sms", phone, purpose);
        AnimeLogProperties.Aliyun aliyun = properties.aliyun();
        if (aliyun.accessKeyId() == null || aliyun.accessKeyId().isBlank()) {
            log.warn("[DEBUG] 短信验证码 {} -> {}", phone, code);
            return;
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(aliyun.accessKeyId())
                    .setAccessKeySecret(aliyun.accessKeySecret())
                    .setRegionId(aliyun.regionId());
            Client client = new Client(config);
            String templateParam = "{\"code\":\"" + code + "\",\"min\":\"" + EXPIRE_MINUTES + "\"}";
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(aliyun.smsSignName())
                    .setTemplateCode(aliyun.smsTemplateCode())
                    .setTemplateParam(templateParam);
            client.sendSmsVerifyCode(request);
        } catch (Exception e) {
            log.error("短信验证码发送失败 -> {}: {}", phone, e.getMessage());
            throw new RuntimeException("短信发送失败，请稍后重试");
        }
    }

    /**
     * 校验验证码。
     *
     * @param channel 通道（email/sms）
     * @param target  目标（邮箱/手机号）
     * @param purpose 用途（register/login 等）
     * @param code    用户输入的验证码
     * @return true 验证通过，false 验证失败
     */
    public boolean verify(String channel, String target, String purpose, String code) {
        String key = key(channel, target, purpose);
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null || !expected.equals(code)) {
            return false;
        }
        // 验证成功后删除已使用的验证码
        redisTemplate.delete(key);
        return true;
    }

    /** 生成 6 位随机验证码并存入 Redis。 */
    private String createAndStore(String channel, String target, String purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(key(channel, target, purpose), code, Duration.ofMinutes(10));
        return code;
    }

    /** 构造 Redis 键：verification:{channel}:{purpose}:{target}。 */
    private String key(String channel, String target, String purpose) {
        return "verification:%s:%s:%s".formatted(channel, purpose, target);
    }
}
