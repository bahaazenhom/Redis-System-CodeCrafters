package storage.impl;

import storage.DataStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public InMemoryDataStore() {}

    @Override
    public void set(String key, String value) {
        store.put(key, value);
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        store.put(key, value);
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }

    @Override
    public boolean exists(String key) {
        return store.containsKey(key);
    }

    @Override
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public void cleanup() {
        // No-op for in-memory store
    }
}
