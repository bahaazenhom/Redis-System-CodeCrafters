package storage.impl;

import storage.DataStore;
import storage.model.RedisValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {
    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();

    public InMemoryDataStore() {
    }

    @Override
    public void set(String key, String value) {
        store.put(key, new RedisValue(value));
    }

    @Override
    public String get(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) {
            return null;
        }
        if (redisValue.isExpired()) {
            store.remove(key);
            return null;
        }
        return redisValue.getValue();
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
    public void setWithExpiry(String key, String value, Long ttlSeconds) {
        RedisValue redisValue = new RedisValue(value, ttlSeconds);
        store.put(key, redisValue);
    }

    @Override
    public long getTTL(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null)
            return -2;

        if (!redisValue.hasExpiry())
            return -1;

        if (redisValue.hasExpiry()) {
            store.remove(key);
            return -2;
        }

        return redisValue.getExpiryTime() - System.currentTimeMillis();
    }
}
