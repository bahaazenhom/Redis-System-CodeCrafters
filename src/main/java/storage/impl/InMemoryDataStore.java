package storage.impl;

import storage.DataStore;
import storage.model.DataType;
import storage.model.RedisValue;
import storage.model.concreteValues.ListValue;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {
    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();

    public InMemoryDataStore() {
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

    @Override
    public DataType getType(String key) {
        RedisValue redisValue = store.get(key);

        return redisValue.getType();
    }

    @Override
    public boolean isType(String key, DataType dataType) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null)
            return false;
        return redisValue.getType() == dataType;
    }

    @Override
    public long rpush(String key, List<String> values) {
        ListValue redisValue = (ListValue) store.get(key);
        redisValue.getList().addAll(values);
        return redisValue.getList().size();
    }

    @Override
    public long lpush(String key, List<String> values) {
        ListValue redisValue = (ListValue) store.get(key);
        for (String value : values) {
            redisValue.getList().addFirst(value);
        }
        return redisValue.getList().size();
    }

    @Override
    public String lpop(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof ListValue)) {
            return null;
        }
        ListValue listValue = (ListValue) redisValue;
        String value = listValue.getList().pollFirst();
        return value;
    }

    @Override
    public List<String> lpop(String key, Long count) {
        if(count == null)count = 1L;
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof ListValue)) {
            return null;
        }
        Deque<String> values = ((ListValue) redisValue).getList();
        List<String> removedValues = new ArrayList<>();
        while(count>0){
            if(values.isEmpty())break;
            removedValues.add(values.pollFirst());
            count--;
        }
        return removedValues;
    }

}
