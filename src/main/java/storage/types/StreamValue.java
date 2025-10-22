package storage.types;

import java.util.HashMap;
import java.util.LinkedHashMap;

import storage.core.DataType;
import storage.core.RedisValue;

public class StreamValue extends RedisValue {
    private final LinkedHashMap<String, HashMap<String, String>> stream;
    private String lastEntryID;
    
    public StreamValue() {
        super(DataType.STREAM);
        this.stream = new LinkedHashMap<>();
        this.lastEntryID = null;
    }

    public void put(String key, HashMap<String, String> value) {
        stream.put(key, value);
        lastEntryID = key;
    }

    public void remove(String key) {
        stream.remove(key);
        if (key.equals(lastEntryID)) {
            recalculateLastEntry();
        }
    }

    public String getLastEntryID() {
        return lastEntryID;
    }

    private void recalculateLastEntry() {
        lastEntryID = null;
        for (String key : stream.keySet()) {
            lastEntryID = key;
        }
    }

    @Override
    public Object getValue() {
        return stream;
    }

    public LinkedHashMap<String, HashMap<String, String>> getStream() {
        return stream;
    }
}
