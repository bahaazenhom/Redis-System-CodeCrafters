package storage.impl;

import storage.DataStore;
import storage.concurrency.ListWaitRegistry;
import storage.concurrency.StreamWaitRegistry;
import storage.core.DataType;
import storage.core.RedisValue;
import storage.exception.InvalidStreamEntryException;
import storage.types.StringValue;
import storage.types.ChannelManager;
import storage.types.ListValue;
import storage.types.StreamValue;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class InMemoryDataStore implements DataStore {

    // ============================================
    // FIELDS
    // ============================================
    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();
    private final ListWaitRegistry listWaitRegistry = new ListWaitRegistry();
    private final StreamWaitRegistry streamWaitRegistry = new StreamWaitRegistry();
    private final ChannelManager channelManager = new ChannelManager();

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

    @Override
    public Set<String> getAllKeys() {
        // Return a defensive copy to prevent external modification
        // Also filters out expired keys
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, RedisValue> entry : store.entrySet()) {
            if (!entry.getValue().isExpired()) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    // ============================================
    // STRING OPERATIONS
    // ============================================

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
            listWaitRegistry.signalFirstWaiter(key, () -> lpop(key));
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
        streamMap.put(entryID, new HashMap<>());
        streamMap.get(entryID).putAll(entryValues);

        // Always signal waiting clients when a new entry is added
        streamWaitRegistry.signalFirstWaiter(streamKey, entryID);

        return entryID;
    }

    @Override
    public List<List<Object>> XRANGE(String streamKey, String startEntryId, String endEntryId, boolean inclusion) {
        RedisValue stream = store.get(streamKey);
        if (stream == null || !(stream instanceof StreamValue)) {
            return new ArrayList<>(); // Empty result if stream doesn't exist
        }

        NavigableMap<String, HashMap<String, String>> streamMap = ((StreamValue) stream).getStream();

        // Handle special values and normalize IDs
        if (startEntryId.equals("-")) {
            startEntryId = "0-0";
        } else if (!startEntryId.contains("-") && inclusion) {
            // For start ID, sequence defaults to 0
            startEntryId = startEntryId + "-0";
        }

        if (endEntryId.equals("+")) {
            endEntryId = Long.MAX_VALUE + "-" + Long.MAX_VALUE;
        } else if (!endEntryId.contains("-") && inclusion) {
            // For end ID, sequence defaults to maximum
            endEntryId = endEntryId + "-" + Long.MAX_VALUE;
        }

        // Get the submap for the range
        NavigableMap<String, HashMap<String, String>> rangeMap = streamMap.subMap(startEntryId, inclusion, endEntryId,
                inclusion);

        List<List<Object>> values = new ArrayList<>();
        // Collect results
        for (Map.Entry<String, HashMap<String, String>> entry : rangeMap.entrySet()) {
            HashMap<String, String> idValues = entry.getValue();
            List<Object> entryData = new ArrayList<>();
            entryData.add(entry.getKey()); // Entry ID

            List<List<String>> entryValues = new ArrayList<>();
            for (Map.Entry<String, String> field : idValues.entrySet()) {
                List<String> fieldPair = new ArrayList<>();
                fieldPair.add(field.getKey());
                fieldPair.add(field.getValue());
                entryValues.add(fieldPair);
            }
            entryData.add(entryValues); // Field-value pairs
            values.add(entryData);
        }

        return values;
    }

    @Override
    public List<List<Object>> XREAD(List<String> streamsKeys, List<String> streamsStartEntriesIDs, boolean block,
            double timeoutSeconds)
            throws InterruptedException {
        List<List<Object>> streamsReads = new ArrayList<>();
        for (int index = 0; index < streamsKeys.size(); index++) {
            String streamKey = streamsKeys.get(index);
            String startEntryId = streamsStartEntriesIDs.get(index);
            // if the stream doesn't exist or the last entryId is smaller than or equal to
            // the
            // startEntryId:
            // then => block and wait.
            if (startEntryId.equals("$")) {
                if (exists(streamKey))
                    startEntryId = ((StreamValue) store.get(streamKey)).getLastEntryID();
                else
                    startEntryId = "0-0";
            }

            if ((exists(streamKey) == false
                    || (((StreamValue) store.get(streamKey)).getLastEntryID().compareTo(startEntryId) <= 0))
                    && block) {

                return streamWaitRegistry.awaitElement(streamKey, startEntryId, timeoutSeconds,
                        createStreamReadSupplier(streamKey, startEntryId));

            }

            String endEntryId = Long.MAX_VALUE + "-" + Long.MAX_VALUE;
            List<Object> streamRead = new ArrayList<>();
            streamRead.add(streamKey);
            streamRead.add(XRANGE(streamKey, startEntryId, endEntryId, false));
            streamsReads.add(streamRead); // Add the stream read to results
        }
        return streamsReads;
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

    private Supplier<List<List<Object>>> createStreamReadSupplier(String streamKey, String startEntryId) {
        return () -> {
            String endEntryId = Long.MAX_VALUE + "-" + Long.MAX_VALUE;
            List<List<Object>> xrangeResult = XRANGE(streamKey, startEntryId, endEntryId, false);
            if (xrangeResult.isEmpty()) {
                return null;
            }
            // Wrap the XRANGE result in XREAD format: [[streamKey, xrangeResult]]
            List<List<Object>> xreadResult = new ArrayList<>();
            List<Object> streamEntry = new ArrayList<>();
            streamEntry.add(streamKey);
            streamEntry.add(xrangeResult);
            xreadResult.add(streamEntry);
            return xreadResult;
        };
    }

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

    // ============================================
    // Channel Operations
    // ============================================

    @Override
    public void subscribe(String subscriberId, String channel) {
        // Use the single global channel manager instead of creating separate Channel objects
        channelManager.subscribe(channel, subscriberId);
    }

    @Override
    public int getSubscriberCount(String subscriberId) {
        // O(1) lookup from the single channel manager
        return channelManager.getSubscriberCount(subscriberId);
    }

}
