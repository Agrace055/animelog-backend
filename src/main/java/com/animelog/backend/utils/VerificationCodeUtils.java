package com.animelog.backend.utils;

import com.animelog.backend.config.AnimeLogProperties;
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
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final AnimeLogProperties properties;

    public VerificationCodeUtils(StringRedisTemplate redisTemplate, AnimeLogProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 发送邮箱验证码。
     * 未配置阿里云时仅生成验证码存入 Redis（调试模式）。
     */
    public void sendEmailCode(String email, String purpose) {
        createAndStore("email", email, purpose);
        // 未配置阿里云密钥时跳过实际发送
        if (properties.aliyun().accessKeyId() == null || properties.aliyun().accessKeyId().isBlank()) {
            return;
        }
    }

    /**
     * 发送短信验证码。
     * 未配置阿里云时仅生成验证码存入 Redis（调试模式）。
     */
    public void sendSmsCode(String phone, String purpose) {
        createAndStore("sms", phone, purpose);
        if (properties.aliyun().accessKeyId() == null || properties.aliyun().accessKeyId().isBlank()) {
            return;
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
