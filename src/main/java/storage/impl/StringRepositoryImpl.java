package storage.impl;

import domain.RedisValue;
import domain.values.StringValue;
import storage.repository.StringRepository;

import java.util.Map;

public class StringRepositoryImpl implements StringRepository {

    private final Map<String, RedisValue> store;

    public StringRepositoryImpl(Map<String, RedisValue> store) {
        this.store = store;
    }

    @Override
    public long incr(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) {
            RedisValue newRedisValue = new StringValue("1");
            store.put(key, newRedisValue);
            return 1;
        }
        String stringValue = ((StringValue) redisValue).getString();
        long value;
        try {
            value = Long.parseLong(stringValue);
            value++;
            RedisValue newRedisValue = new StringValue(String.valueOf(value));
            store.put(key, newRedisValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("value is not an integer or out of range");
        }
        return value;
    }
}
