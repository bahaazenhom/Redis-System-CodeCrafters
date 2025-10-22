package storage.impl;

import storage.DataStore;
import storage.concurrency.WaitRegistry;
import storage.core.DataType;
import storage.core.RedisValue;
import storage.exception.InvalidStreamEntryException;
import storage.types.ListValue;
import storage.types.StreamValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {

    // ============================================
    // FIELDS
    // ============================================
    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();
    private final WaitRegistry waitRegistry = new WaitRegistry();

    public InMemoryDataStore() {
    }

    // ============================================
    // COMMON OPERATIONS
    // ============================================

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
            return -2; // Key doesn't exist
        }

        if (!redisValue.hasExpiry()) {
            return -1; // Key exists but has no expiry
        }

        if (redisValue.isExpired()) {
            store.remove(key);
            return -2; // Key expired
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

    // ============================================
    // LIST OPERATIONS
    // ============================================

    @Override
    public long rpush(String key, List<String> values) {
        ListValue redisValue = (ListValue) store.get(key);
        boolean wasEmpty = redisValue.getList().isEmpty();

        redisValue.getList().addAll(values);
        long size = redisValue.getList().size();

        if (wasEmpty) {
            waitRegistry.signalFirstWaiter(key, () -> lpop(key));
        }

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

        if (wasEmpty) {
            waitRegistry.signalFirstWaiter(key, () -> lpop(key));
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

        return waitRegistry.awaitElement(key, timestamp, () -> lpop(key));
    }

    // ============================================
    // STREAM OPERATIONS
    // ============================================

    @Override
    public String xadd(String streamKey, String entryID, HashMap<String, String> entryValues)
            throws InvalidStreamEntryException {

        StreamValue stream = getOrCreateStream(streamKey);
        TreeMap<String, HashMap<String, String>> streamMap = stream.getStream();

        // Handle entry ID generation and validation
        if (!streamMap.isEmpty()) {
            String lastEntryID = stream.getLastEntryID();
            entryID = processEntryID(entryID, lastEntryID, false);
        } else {
            entryID = processEntryID(entryID, null, true);
        }

        // Add entry to stream
        stream.put(entryID, new HashMap<>());
        streamMap.get(entryID).putAll(entryValues);

        return entryID;
    }

    @Override
    public int[][] XRANGE(String streamKey, String startEntryId, String endEntryId) {
        RedisValue stream = store.get(streamKey);
        NavigableMap<String, HashMap<String, String>> streamMap = ((StreamValue) stream).getStream();
        startEntryId = startEntryId + "-0";
        endEntryId = endEntryId.split("-")[0] + "-" + (Long.parseLong(endEntryId.split("-")[1]) + 1);
        streamMap = streamMap.tailMap(startEntryId, true);
        streamMap = streamMap.headMap(streamKey, false);

        List<List<Object>> values = new ArrayList<>();//[]
        // Collect results
        int entryIndex = 0;
        for (Map.Entry<String, HashMap<String, String>> entry : streamMap.entrySet()) {
            HashMap<String, String> idValues = entry.getValue();
            values.add(new ArrayList<>());//[[],[]]
            values.get(values.size()-1).add(entry.getKey());
            List<List<String>> entryValues = new ArrayList<>();
            for(Map.Entry<String, String> field : idValues.entrySet()) {
                List<String> entries = new ArrayList<>();
                entries.add(field.getKey());
                entries.add(field.getValue());
                entryValues.add(entries);
            }
            values.get(values.size()-1).add(entryValues);
        }

        return new int[][] {};
    }

    // ============================================
    // PRIVATE HELPER METHODS - List Operations
    // ============================================

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

    // ============================================
    // PRIVATE HELPER METHODS - Stream Operations
    // ============================================

    private StreamValue getOrCreateStream(String key) {
        return (StreamValue) store.computeIfAbsent(key, k -> new StreamValue());
    }

    /**
     * Process and validate entry ID for XADD command.
     * Handles auto-generation with * and partial generation with timestamp-*
     */
    private String processEntryID(String entryID, String lastEntryID, boolean isEmptyStream)
            throws InvalidStreamEntryException {

        if (entryID.equals("*")) {
            return generateFullEntryID(lastEntryID, isEmptyStream);
        } else if (entryID.endsWith("-*")) {
            return generatePartialEntryID(entryID, lastEntryID, isEmptyStream);
        } else {
            validateEntryID(entryID, lastEntryID);
            return entryID;
        }
    }

    /**
     * Generate a complete entry ID (both timestamp and sequence)
     */
    private String generateFullEntryID(String lastEntryID, boolean isEmptyStream)
            throws InvalidStreamEntryException {

        long currentMillis = System.currentTimeMillis();

        if (isEmptyStream) {
            return currentMillis + "-0";
        }

        String[] lastParts = lastEntryID.split("-");
        long lastMs = Long.parseLong(lastParts[0]);
        long lastSeq = Long.parseLong(lastParts[1]);

        if (currentMillis == lastMs) {
            return currentMillis + "-" + (lastSeq + 1);
        } else {
            return currentMillis + "-0";
        }
    }

    /**
     * Generate sequence number when timestamp is provided (format: timestamp-*)
     */
    private String generatePartialEntryID(String entryID, String lastEntryID, boolean isEmptyStream)
            throws InvalidStreamEntryException {

        String newTimePart = entryID.substring(0, entryID.length() - 2); // Remove "-*"

        if (isEmptyStream) {
            return newTimePart.equals("0") ? "0-1" : newTimePart + "-0";
        }

        String[] lastParts = lastEntryID.split("-");
        long lastMs = Long.parseLong(lastParts[0]);
        long lastSeq = Long.parseLong(lastParts[1]);
        long newMs = Long.parseLong(newTimePart);

        if (newMs < lastMs) {
            throw new InvalidStreamEntryException(
                    "The ID specified in XADD is equal or smaller than the target stream top item");
        }

        if (newMs == lastMs) {
            return newTimePart + "-" + (lastSeq + 1);
        } else {
            return newTimePart + "-0";
        }
    }

    /**
     * Validate that the new entry ID is greater than the last entry ID
     */
    private void validateEntryID(String newEntryID, String lastEntryID)
            throws InvalidStreamEntryException {

        if (newEntryID.equals("0-0")) {
            throw new InvalidStreamEntryException(
                    "The ID specified in XADD must be greater than 0-0");
        }

        if (lastEntryID == null) {
            return; // First entry in stream
        }

        String[] newParts = newEntryID.split("-");
        String[] lastParts = lastEntryID.split("-");

        long newMs = Long.parseLong(newParts[0]);
        long lastMs = Long.parseLong(lastParts[0]);
        long newSeq = Long.parseLong(newParts[1]);
        long lastSeq = Long.parseLong(lastParts[1]);

        if (newMs < lastMs || (newMs == lastMs && newSeq <= lastSeq)) {
            throw new InvalidStreamEntryException(
                    "The ID specified in XADD is equal or smaller than the target stream top item");
        }
    }

}
