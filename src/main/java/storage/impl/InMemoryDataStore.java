package storage.impl;

import storage.DataStore;
import storage.concurrency.WaitRegistry;
import storage.core.DataType;
import storage.core.RedisValue;
import storage.exception.InvalidStreamEntryException;
import storage.types.ListValue;
import storage.types.StreamValue;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {
    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();
    private final WaitRegistry waitRegistry = new WaitRegistry();

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
        boolean wasEmpty = redisValue.getList().isEmpty();

        redisValue.getList().addAll(values);
        long size = redisValue.getList().size();
        if (wasEmpty)
            waitRegistry.signalFirstWaiter(key, () -> lpop(key));

        return size;
    }

    @Override
    public long lpush(String key, List<String> values) {
        ListValue redisValue = (ListValue) store.get(key);
        boolean wasEmpty = redisValue.getList().isEmpty();
        for (String value : values) {
            redisValue.getList().addFirst(value);
        }
        long size = redisValue.getList().size();
        if (wasEmpty)
            waitRegistry.signalFirstWaiter(key, () -> lpop(key));

        return size;
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
        if (count == null)
            count = 1L;
        RedisValue redisValue = store.get(key);
        if (redisValue == null || !(redisValue instanceof ListValue)) {
            return null;
        }
        Deque<String> values = ((ListValue) redisValue).getList();
        List<String> removedValues = new ArrayList<>();
        while (count > 0) {
            if (values.isEmpty())
                break;
            removedValues.add(values.pollFirst());
            count--;
        }
        return removedValues;
    }

    @Override
    public String BLPOP(String key, double timestamp) throws InterruptedException {
        String value = lpop(key);
        if (value != null)
            return value;

        value = waitRegistry.awaitElement(key, timestamp, () -> lpop(key));

        return value;

    }

    @Override
    public String xadd(String streamKey, String entryID, HashMap<String, String> entryValues)
            throws InvalidStreamEntryException {
        RedisValue stream = store.computeIfAbsent(streamKey, k -> new StreamValue());
        LinkedHashMap<String, HashMap<String, String>> streamMap = ((StreamValue) stream).getStream();
        if (!streamMap.isEmpty()) {
            String lastEntryID = ((StreamValue) stream).getLastEntryID();
            if (lastEntryID != null) {
                String validation = validateStreamEntryID(entryID, lastEntryID);
                if (validation != null) {
                    throw new InvalidStreamEntryException(validation);
                }
            }
        }

        ((StreamValue) stream).put(entryID, new HashMap<>());

        for (Map.Entry<String, String> entry : entryValues.entrySet()) {
            streamMap.get(entryID).put(entry.getKey(), entry.getValue());
        }
        return entryID;
    }

    private String validateStreamEntryID(String newEntryID, String lastEntryID) {
        if (newEntryID.equals("0-0")) {
            return "The ID specified in XADD must be greater than 0-0";
        }
        String[] newParts = newEntryID.split("-");
        String[] lastParts = lastEntryID.split("-");

        long newMs = Long.parseLong(newParts[0]);
        long lastMs = Long.parseLong(lastParts[0]);
        long newSeq = Long.parseLong(newParts[1]);
        long lastSeq = Long.parseLong(lastParts[1]);

        if (newMs < lastMs) {
            return "The ID specified in XADD is equal or smaller than the target stream top item";
        } else if (newMs == lastMs) {
            if(newSeq <= lastSeq)return "The ID specified in XADD is equal or smaller than the target stream top item";
        }
        return null;
    }

}
