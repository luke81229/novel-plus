package com.java2nb.novel.core.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 当使用 Guava 作为 {@link CacheService} 且未要求保留 Redis 自动配置时，排除 Spring Data Redis，
 * 避免启动时创建连接（单机部署可不装 Redis）。
 */
public class NovelCacheRedisEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROP_IMPL = "novel.cache.impl";
    private static final String PROP_KEEP_REDIS = "novel.cache.keep-redis-autoconfig";
    private static final String EXCLUDE_KEY = "spring.autoconfigure.exclude";

    private static final String[] REDIS_AUTOCONFIG = {
        RedisAutoConfiguration.class.getName(),
        RedisReactiveAutoConfiguration.class.getName(),
        RedisRepositoriesAutoConfiguration.class.getName()
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.parseBoolean(environment.getProperty(PROP_KEEP_REDIS, "false"))) {
            return;
        }
        String impl = environment.getProperty(PROP_IMPL, "guava");
        if (!"guava".equalsIgnoreCase(impl)) {
            return;
        }
        Set<String> merged = new LinkedHashSet<>();
        String existing = environment.getProperty(EXCLUDE_KEY);
        if (StringUtils.hasText(existing)) {
            Arrays.stream(existing.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(merged::add);
        }
        merged.addAll(Arrays.asList(REDIS_AUTOCONFIG));
        Map<String, Object> map = new HashMap<>(1);
        map.put(EXCLUDE_KEY, String.join(",", merged));
        environment.getPropertySources().addFirst(new MapPropertySource("novel-cache-exclude-redis", map));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
