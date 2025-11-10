package domain.values;

import java.util.HashMap;
import java.util.TreeMap;

import domain.DataType;
import domain.RedisValue;

public class StreamValue extends RedisValue {
    private final TreeMap<String, HashMap<String, String>> stream;
    
    public StreamValue() {
        super(DataType.STREAM);
        this.stream = new TreeMap<>();
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
