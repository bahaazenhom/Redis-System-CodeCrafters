package storage.impl;

import domain.DataType;
import domain.RedisValue;
import storage.repository.CommonRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommonRepositoryImpl implements CommonRepository {

    private final Map<String, RedisValue> store;

    public CommonRepositoryImpl(Map<String, RedisValue> store) {
        this.store = store;
    }

    @Override
    public void setValue(String key, RedisValue redisValue) {
        store.put(key, redisValue);
    }

    @Override
    public RedisValue getValue(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) {
            return null;
        }
        if (redisValue.isExpired()) {
            store.remove(key);
            return null;
        }
        return redisValue;
    }

    @Override
    public boolean exists(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) {
            return false;
        }
        if (redisValue.isExpired()) {
            store.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public void cleanup() {
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @Override
    public long getTTL(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) {
            return -2;
        }

        if (!redisValue.hasExpiry()) {
            return -1;
        }

        if (redisValue.isExpired()) {
            store.remove(key);
            return -2;
        }

        return redisValue.getExpiryTime() - System.currentTimeMillis();
    }

    @Override
    public DataType getType(String key) {
        RedisValue redisValue = store.get(key);
        return redisValue != null ? redisValue.getType() : null;
    }

    @Override
    public boolean isType(String key, DataType dataType) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) {
            return false;
        }
        return redisValue.getType() == dataType;
    }

    @Override
    public Set<String> getAllKeys() {
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, RedisValue> entry : store.entrySet()) {
            if (!entry.getValue().isExpired()) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }
}
