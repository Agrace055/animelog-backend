package com.animelog.backend.utils;

import com.animelog.backend.config.AnimeLogProperties;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;

/**
 * MinIO 对象存储工具类，提供文件上传、下载、删除和 URL 生成功能。
 */
@Component
public class MinioUtils {
    private final MinioClient minioClient;
    private final AnimeLogProperties.Minio minioProperties;

    public MinioUtils(AnimeLogProperties properties) {
        this.minioProperties = properties.minio();
        this.minioClient = MinioClient.builder()
            .endpoint(minioProperties.endpoint())
            .credentials(minioProperties.accessKey(), minioProperties.secretKey())
            .build();
    }

    /**
     * 上传文件到 MinIO。
     *
     * @param objectKey   对象键（文件路径）
     * @param inputStream 文件输入流
     * @param size        文件大小
     * @param contentType MIME 类型
     */
    public void upload(String objectKey, InputStream inputStream, long size, String contentType) throws Exception {
        // 确保存储桶存在
        ensureBucket();
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(minioProperties.bucket())
            .object(objectKey)
            .stream(inputStream, size, -1)
            .contentType(contentType)
            .build());
    }

    /** 从 MinIO 下载文件。 */
    public InputStream download(String objectKey) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
            .bucket(minioProperties.bucket())
            .object(objectKey)
            .build());
    }

    /** 从 MinIO 删除文件。 */
    public void delete(String objectKey) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket(minioProperties.bucket())
            .object(objectKey)
            .build());
    }

    /**
     * 获取对象的公开访问 URL。
     * 如果未配置 publicObjectBaseUrl，则直接返回 objectKey。
     */
    public String publicUrl(String objectKey) {
        String baseUrl = minioProperties.publicObjectBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return objectKey;
        }
        // 拼接 baseUrl 和 objectKey，去除多余的斜杠
        return baseUrl.replaceAll("/+$", "") + "/" + objectKey.replaceAll("^/+", "");
    }

    /**
     * 生成预签名 GET URL，用于临时授权访问私有文件。
     *
     * @param objectKey 对象键
     * @param expiry    URL 有效期
     * @return 预签名 URL
     */
    public String presignedGetUrl(String objectKey, Duration expiry) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(minioProperties.bucket())
            .object(objectKey)
            .expiry((int) expiry.toSeconds())
            .build());
    }

    /** 检查存储桶是否存在，不存在则自动创建。 */
    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.bucket()).build());
        }
    }
}
