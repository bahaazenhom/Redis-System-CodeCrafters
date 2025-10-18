package storage.model.concreteValues;

import java.util.HashMap;

import storage.model.DataType;
import storage.model.RedisValue;

public class StreamValue extends RedisValue {
    HashMap<String, HashMap<String, String>> stream;


    public StreamValue() {
        super(DataType.STREAM);
        this.stream = new HashMap<>();
    }

    @Override
    public Object getValue() {
        return stream;
    }

    public HashMap<String, HashMap<String, String>> getStream() {
        return stream;
    }
    
    
}
