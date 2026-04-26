package com.animelog.backend.utils;

import com.animelog.backend.config.AnimeLogProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具类，提供令牌签发和解析功能。
 */
@Component
public class JwtUtils {
    private final AnimeLogProperties properties;

    public JwtUtils(AnimeLogProperties properties) {
        this.properties = properties;
    }

    /**
     * 签发 JWT Token。
     *
     * @param userId 用户 ID（存入 subject）
     * @param role   用户角色（存入自定义 claim）
     * @return JWT Token 字符串
     */
    public String issue(Long userId, String role) {
        Instant now = Instant.now();
        SecretKey key = key();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(properties.jwt().expirationSeconds())))
            .signWith(key)
            .compact();
    }

    /**
     * 解析 JWT Token，提取用户主体信息。
     *
     * @param token JWT Token 字符串
     * @return 包含用户 ID 和角色的主体记录
     */
    public JwtPrincipal parse(String token) {
        var claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return new JwtPrincipal(Long.valueOf(claims.getSubject()), claims.get("role", String.class));
    }

    /** 从配置密钥构建 HMAC-SHA 签名密钥。 */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    /** JWT 认证主体，包含用户 ID 和角色。 */
    public record JwtPrincipal(Long userId, String role) {
    }
}
