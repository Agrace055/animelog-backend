package com.animelog.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AnimeLog 后端应用主入口。
 * <ul>
 *   <li>{@link EnableScheduling} — 启用定时任务支持</li>
 *   <li>{@link SpringBootApplication} — Spring Boot 应用</li>
 *   <li>{@link MapperScan} — MyBatis-Plus Mapper 接口扫描</li>
 *   <li>{@link ConfigurationPropertiesScan} — 配置属性类扫描</li>
 * </ul>
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.animelog.backend.mapper")
@ConfigurationPropertiesScan
public class AnimeLogApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnimeLogApplication.class, args);
    }
}
