package storage.types;

import java.util.HashMap;
import java.util.TreeMap;

import storage.core.DataType;
import storage.core.RedisValue;

public class StreamValue extends RedisValue {
    private final TreeMap<String, HashMap<String, String>> stream;
    
    public StreamValue() {
        super(DataType.STREAM);
        this.stream = new TreeMap<>();
    }

    public void put(String key, HashMap<String, String> value) {
        stream.put(key, value);
    }

    public void remove(String key) {
        stream.remove(key);
    }

    public String getLastEntryID() {
        return stream.lastKey();
    }

    @Override
    public Object getValue() {
        return stream;
    }

    public TreeMap<String, HashMap<String, String>> getStream() {
        return stream;
    }
}
