package com.java2nb.novel.core.cache.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.java2nb.novel.core.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Primary
@Slf4j
@Service
@ConditionalOnProperty(prefix = "novel.cache", name = "impl", havingValue = "guava", matchIfMissing = true)
public class GuavaCacheServiceImpl implements CacheService, InitializingBean, DisposableBean {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String PERSIST_FILE = TEMP_DIR + File.separator + "guava_cache.json";
    private static final long PERSIST_INTERVAL_SECONDS = 60;

    private final Cache<String, TimedCacheValue<String>> stringCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    private final Cache<String, TimedCacheValue<Object>> objectCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterPropertiesSet() {
        loadFromDisk();
        scheduler.scheduleAtFixedRate(this::persistToDisk, PERSIST_INTERVAL_SECONDS, PERSIST_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("GuavaCacheServiceImpl initialized via afterPropertiesSet(). Using file: {}", PERSIST_FILE);
    }

    @Override
    public void destroy() {
        try {
            persistToDisk();
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("GuavaCacheServiceImpl destroyed and scheduler stopped");
        } catch (Exception e) {
            log.warn("Error during GuavaCacheServiceImpl shutdown: {}", e.getMessage(), e);
        }
    }

    @Override
    public String get(String key) {
        TimedCacheValue<String> val = stringCache.getIfPresent(key);
        if (val == null || val.isExpired()) {
            stringCache.invalidate(key);
            log.debug("String key expired or not found. key={}", key);
            return null;
        }
        log.debug("Get string from cache. key={}, value={}", key, val.getValue());
        return val.getValue();
    }

    @Override
    public void set(String key, String value) {
        set(key, value, 3600);
    }

    @Override
    public void set(String key, String value, long timeout) {
        stringCache.put(key, new TimedCacheValue<>(value, timeout));
        log.debug("Set string in cache. key={}, value={}, ttl={}s", key, value, timeout);
    }

    @Override
    public Object getObject(String key) {
        TimedCacheValue<Object> val = objectCache.getIfPresent(key);
        if (val == null || val.isExpired()) {
            objectCache.invalidate(key);
            log.debug("Object key expired or not found. key={}", key);
            return null;
        }
        log.debug("Get object from cache. key={}, value={}", key, val.getValue());
        return val.getValue();
    }

    @Override
    public void setObject(String key, Object value) {
        setObject(key, value, 3600);
    }

    @Override
    public void setObject(String key, Object value, long timeout) {
        objectCache.put(key, new TimedCacheValue<>(value, timeout));
        log.debug("Set object in cache. key={}, value={}, ttl={}s", key, value, timeout);
    }

    @Override
    public void del(String key) {
        stringCache.invalidate(key);
        objectCache.invalidate(key);
        log.debug("Deleted cache key: {}", key);
    }

    @Override
    public boolean contains(String key) {
        boolean exists = get(key) != null || getObject(key) != null;
        log.debug("Cache contains key={} -> {}", key, exists);
        return exists;
    }

    @Override
    public void expire(String key, long timeout) {
        TimedCacheValue<String> sVal = stringCache.getIfPresent(key);
        if (sVal != null) stringCache.put(key, new TimedCacheValue<>(sVal.getValue(), timeout));

        TimedCacheValue<Object> oVal = objectCache.getIfPresent(key);
        if (oVal != null) objectCache.put(key, new TimedCacheValue<>(oVal.getValue(), timeout));

        log.debug("Updated TTL for key={} to {} seconds", key, timeout);
    }

    private void persistToDisk() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("stringCache", objectMapper.valueToTree(stringCache.asMap()));
            root.set("objectCache", objectMapper.valueToTree(objectCache.asMap()));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(PERSIST_FILE), root);
            log.info("Cache persisted to file: {}", PERSIST_FILE);
        } catch (Exception e) {
            log.error("Failed to persist cache to file: {}", e.getMessage(), e);
        }
    }

    private void loadFromDisk() {
        File file = new File(PERSIST_FILE);
        if (!file.exists()) {
            log.warn("Cache file not found, skipping load: {}", PERSIST_FILE);
            return;
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(file);
            Map<String, TimedCacheValue<String>> strMap = objectMapper.convertValue(
                    root.get("stringCache"),
                    new TypeReference<>() {}
            );
            Map<String, TimedCacheValue<Object>> objMap = objectMapper.convertValue(
                    root.get("objectCache"),
                    new TypeReference<>() {}
            );
            if (strMap != null) strMap.forEach(stringCache::put);
            if (objMap != null) objMap.forEach(objectCache::put);
            log.info("Cache loaded from file: {}", PERSIST_FILE);
        } catch (Exception e) {
            log.warn("Failed to load cache from file: {}", e.getMessage());
        }
    }
}
