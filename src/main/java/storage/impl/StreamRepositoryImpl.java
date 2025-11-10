package storage.impl;

import domain.RedisValue;
import domain.values.StreamValue;
import storage.concurrency.StreamWaitRegistry;
import storage.exception.InvalidStreamEntryException;
import storage.repository.StreamRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;

public class StreamRepositoryImpl implements StreamRepository {

    private final Map<String, RedisValue> store;
    private final StreamWaitRegistry streamWaitRegistry;

    public StreamRepositoryImpl(Map<String, RedisValue> store, StreamWaitRegistry streamWaitRegistry) {
        this.store = store;
        this.streamWaitRegistry = streamWaitRegistry;
    }

    @Override
    public String xadd(String streamKey, String entryID, HashMap<String, String> entryValues)
            throws InvalidStreamEntryException {

        StreamValue stream = getOrCreateStream(streamKey);
        TreeMap<String, HashMap<String, String>> streamMap = stream.getStream();

        if (!streamMap.isEmpty()) {
            String lastEntryID = stream.getLastEntryID();
            entryID = processEntryID(entryID, lastEntryID, false);
        } else {
            entryID = processEntryID(entryID, null, true);
        }

        streamMap.put(entryID, new HashMap<>());
        streamMap.get(entryID).putAll(entryValues);

        streamWaitRegistry.signalFirstWaiter(streamKey, entryID);

        return entryID;
    }

    @Override
    public List<List<Object>> XRANGE(String streamKey, String startEntryId, String endEntryId, boolean inclusion) {
        RedisValue stream = store.get(streamKey);
        if (stream == null || !(stream instanceof StreamValue)) {
            return new ArrayList<>();
        }

        NavigableMap<String, HashMap<String, String>> streamMap = ((StreamValue) stream).getStream();

        if (startEntryId.equals("-")) {
            startEntryId = "0-0";
        } else if (!startEntryId.contains("-") && inclusion) {
            startEntryId = startEntryId + "-0";
        }

        if (endEntryId.equals("+")) {
            endEntryId = Long.MAX_VALUE + "-" + Long.MAX_VALUE;
        } else if (!endEntryId.contains("-") && inclusion) {
            endEntryId = endEntryId + "-" + Long.MAX_VALUE;
        }

        NavigableMap<String, HashMap<String, String>> rangeMap = streamMap.subMap(startEntryId, inclusion, endEntryId,
                inclusion);

        List<List<Object>> values = new ArrayList<>();
        for (Map.Entry<String, HashMap<String, String>> entry : rangeMap.entrySet()) {
            HashMap<String, String> idValues = entry.getValue();
            List<Object> entryData = new ArrayList<>();
            entryData.add(entry.getKey());

            List<List<String>> entryValues = new ArrayList<>();
            for (Map.Entry<String, String> field : idValues.entrySet()) {
                List<String> fieldPair = new ArrayList<>();
                fieldPair.add(field.getKey());
                fieldPair.add(field.getValue());
                entryValues.add(fieldPair);
            }
            entryData.add(entryValues);
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
            
            if (startEntryId.equals("$")) {
                RedisValue redisValue = store.get(streamKey);
                if (redisValue != null && redisValue instanceof StreamValue) {
                    startEntryId = ((StreamValue) redisValue).getLastEntryID();
                } else {
                    startEntryId = "0-0";
                }
            }

            RedisValue redisValue = store.get(streamKey);
            boolean shouldBlock = (redisValue == null || 
                    (((StreamValue) redisValue).getLastEntryID().compareTo(startEntryId) <= 0)) && block;

            if (shouldBlock) {
                return streamWaitRegistry.awaitElement(streamKey, startEntryId, timeoutSeconds,
                        createStreamReadSupplier(streamKey, startEntryId));
            }

            String endEntryId = Long.MAX_VALUE + "-" + Long.MAX_VALUE;
            List<Object> streamRead = new ArrayList<>();
            streamRead.add(streamKey);
            streamRead.add(XRANGE(streamKey, startEntryId, endEntryId, false));
            streamsReads.add(streamRead);
        }
        return streamsReads;
    }

    private Supplier<List<List<Object>>> createStreamReadSupplier(String streamKey, String startEntryId) {
        return () -> {
            String endEntryId = Long.MAX_VALUE + "-" + Long.MAX_VALUE;
            List<List<Object>> xrangeResult = XRANGE(streamKey, startEntryId, endEntryId, false);
            if (xrangeResult.isEmpty()) {
                return null;
            }
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

    private String generatePartialEntryID(String entryID, String lastEntryID, boolean isEmptyStream)
            throws InvalidStreamEntryException {

        String newTimePart = entryID.substring(0, entryID.length() - 2);

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

    private void validateEntryID(String newEntryID, String lastEntryID)
            throws InvalidStreamEntryException {

        if (newEntryID.equals("0-0")) {
            throw new InvalidStreamEntryException(
                    "The ID specified in XADD must be greater than 0-0");
        }

        if (lastEntryID == null) {
            return;
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
