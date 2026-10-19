package com.runa.smarter.umbrella.integration;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 集成层门面：数据库 ping、Redis 演示缓存等。
 * Redis 未配置时相关能力降级为空或 false。
 */
@Service
public class IntegrationGateway {

    private static final Duration DEMO_CACHE_TTL = Duration.ofMinutes(30);

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public IntegrationGateway(JdbcTemplate jdbcTemplate,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    public IntegrationHealth checkHealth() {
        boolean databaseUp = pingDatabase();
        boolean redisUp = pingRedis();
        String message = "database=%s, redis=%s".formatted(up(databaseUp), up(redisUp));
        return new IntegrationHealth(databaseUp, redisUp, message);
    }

    public void putDemoCache(String key, String value) {
        if (redisTemplate == null) {
            throw new IllegalStateException("Redis is not configured");
        }
        redisTemplate.opsForValue().set(demoKey(key), value, DEMO_CACHE_TTL);
    }

    public Optional<String> getDemoCache(String key) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(redisTemplate.opsForValue().get(demoKey(key)));
    }

    private boolean pingDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean pingRedis() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception ex) {
            return false;
        }
    }

    private static String demoKey(String key) {
        return "umbrella:demo:" + key;
    }

    private static String up(boolean available) {
        return available ? "UP" : "DOWN";
    }

}
