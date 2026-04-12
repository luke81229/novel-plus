package com.java2nb.novel.core.cache.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Setter;

@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimedCacheValue<T> {
    private T value;
    private long expireAt;

    public TimedCacheValue(T value, long ttlSeconds) {
        this.value = value;
        this.expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
    }

    public T getValue() {
        return isExpired() ? null : value;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt;
    }

}
