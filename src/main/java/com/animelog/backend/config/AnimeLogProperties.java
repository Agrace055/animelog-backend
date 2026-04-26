package com.animelog.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用配置属性，映射 application.yml 中 animelog 前缀的配置项。
 *
 * @param jwt    JWT 配置
 * @param minio  MinIO 对象存储配置
 * @param bangumi Bangumi 数据源配置
 * @param aliyun 阿里云配置（短信、邮件）
 */
@ConfigurationProperties(prefix = "animelog")
public record AnimeLogProperties(
    Jwt jwt,
    Minio minio,
    Bangumi bangumi,
    Aliyun aliyun
) {
    /** JWT 配置：密钥和过期时间（秒）。 */
    public record Jwt(String secret, long expirationSeconds) {
    }

    /** MinIO 配置：端点、访问密钥、存储桶和公开访问 URL。 */
    public record Minio(String endpoint, String accessKey, String secretKey, String bucket, String publicObjectBaseUrl) {
    }

    /** Bangumi 配置：最新归档 URL、工作目录、任务执行器开关、锁超时时间、标签导入最低 count 阈值。 */
    public record Bangumi(String latestJsonUrl, String workDir, boolean taskRunnerEnabled, long taskRunnerLockSeconds, int tagMinCount) {
    }

    /** 阿里云配置：短信签名、模板和邮件推送。 */
    public record Aliyun(
        String regionId,
        String accessKeyId,
        String accessKeySecret,
        String smsSignName,
        String smsTemplateCode,
        String dmAccountName,
        String dmFromAlias
    ) {
    }
}
