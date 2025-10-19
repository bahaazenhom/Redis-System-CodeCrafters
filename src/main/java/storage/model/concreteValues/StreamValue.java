package storage.model.concreteValues;

import java.util.HashMap;
import java.util.TreeMap;

import storage.model.DataType;
import storage.model.RedisValue;

public class StreamValue extends RedisValue {
    TreeMap<String, HashMap<String, String>> stream;


    public StreamValue() {
        super(DataType.STREAM);
        this.stream = new TreeMap<>();
    }

    @Override
    public Object getValue() {
        return stream;
    }

    public TreeMap<String, HashMap<String, String>> getStream() {
        return stream;
    }
    
}
