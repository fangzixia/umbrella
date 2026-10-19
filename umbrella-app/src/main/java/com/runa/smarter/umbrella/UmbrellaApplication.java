package com.runa.smarter.umbrella;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Umbrella 启动类（胖应用）：依赖 runtime + integration，扫描 {@code com.runa.smarter.umbrella}。
 * 默认排除 Redis/Flyway/MyBatis 自动配置，可按需从 excludeName 移除并启用对应能力。
 */
@SpringBootApplication(
        scanBasePackages = "com.runa.smarter.umbrella",
        excludeName = {
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        })
public class UmbrellaApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmbrellaApplication.class, args);
    }

}
