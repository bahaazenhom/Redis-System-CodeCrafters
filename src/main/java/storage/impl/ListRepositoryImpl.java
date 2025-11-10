package storage.impl;

import domain.RedisValue;
import domain.values.ListValue;
import storage.concurrency.ListWaitRegistry;
import storage.repository.ListRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class ListRepositoryImpl implements ListRepository {

    private final Map<String, RedisValue> store;
    private final ListWaitRegistry listWaitRegistry;

    public ListRepositoryImpl(Map<String, RedisValue> store, ListWaitRegistry listWaitRegistry) {
        this.store = store;
        this.listWaitRegistry = listWaitRegistry;
    }

    @Override
    public long rpush(String key, List<String> values) {
        ListValue redisValue = getOrCreateList(key);
        boolean wasEmpty = redisValue.getList().isEmpty();

        redisValue.getList().addAll(values);
        long size = redisValue.getList().size();

        if (wasEmpty) {
            listWaitRegistry.signalFirstWaiter(key, () -> lpop(key));
        }

        return size;
    }

    @Override
    public long lpush(String key, List<String> values) {
        ListValue redisValue = getOrCreateList(key);
        boolean wasEmpty = redisValue.getList().isEmpty();

        for (String value : values) {
            redisValue.getList().addFirst(value);
        }
        long size = redisValue.getList().size();

        if (wasEmpty) {
            listWaitRegistry.signalFirstWaiter(key, () -> lpop(key));
        }

        return size;
    }

    @Override
    public String lpop(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof ListValue)) {
            return null;
        }
        ListValue listValue = (ListValue) redisValue;
        return listValue.getList().pollFirst();
    }

    @Override
    public List<String> lpop(String key, Long count) {
        if (count == null) {
            count = 1L;
        }

        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof ListValue)) {
            return null;
        }

        Deque<String> values = ((ListValue) redisValue).getList();
        List<String> removedValues = new ArrayList<>();

        while (count > 0 && !values.isEmpty()) {
            removedValues.add(values.pollFirst());
            count--;
        }

        return removedValues;
    }

    @Override
    public String BLPOP(String key, double timestamp) throws InterruptedException {
        String value = lpop(key);
        if (value != null) {
            return value;
        }

        return listWaitRegistry.awaitElement(key, timestamp, () -> lpop(key));
    }

    private ListValue getOrCreateList(String key) {
        RedisValue value = store.get(key);
        if (value == null) {
            value = new ListValue(new ArrayDeque<>());
            store.put(key, value);
        }
        if (!(value instanceof ListValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return (ListValue) value;
    }
}
