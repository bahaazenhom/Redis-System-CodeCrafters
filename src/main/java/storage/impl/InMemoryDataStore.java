package storage.impl;

import storage.DataStore;
import storage.concurrency.WaitRegistry;
import storage.model.DataType;
import storage.model.RedisValue;
import storage.model.concreteValues.ListValue;
import storage.model.concreteValues.StreamValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import protocol.RESPSerializer;

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
    public String xadd(String streamKey, String entryID, HashMap<String, String> entryValues,
            BufferedWriter clientOutput) {
        RedisValue stream = store.computeIfAbsent(streamKey, k -> new StreamValue());
        TreeMap<String, HashMap<String, String>> streamMap = ((StreamValue) stream).getStream();

        if (!streamMap.isEmpty()) {
            String lastEntryID = streamMap.lastKey();
            try {
                if (!validateStreamEntryID(entryID, lastEntryID)) {
                    clientOutput.write(RESPSerializer
                            .error("The ID specified in XADD is equal or smaller than the target stream top item"));
                    clientOutput.flush();
                    return null;
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
        streamMap.put(entryID, new HashMap<>());

        for (Map.Entry<String, String> entry : entryValues.entrySet()) {
            streamMap.get(entryID).put(entry.getKey(), entry.getValue());
        }
        return entryID;
    }

    private boolean validateStreamEntryID(String newEntryID, String lastEntryID) {
        if (newEntryID.equals("0-0")) {
            return false;
        }
        String[] newParts = newEntryID.split("-");
        String[] lastParts = lastEntryID.split("-");

        if (newParts[0].compareTo(lastParts[0]) < 0) {
            return false;
        } else if ((newParts[0].compareTo(lastParts[0]) == 0)
                && !(newParts[1].compareTo(lastParts[1]) > 0)) {
            return false;
        }
        return true;
    }

}
