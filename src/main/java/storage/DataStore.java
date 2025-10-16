package storage;


import java.util.List;

import storage.model.DataType;
import storage.model.RedisValue;

public interface DataStore {
    RedisValue getValue(String key);

    void setValue(String key, RedisValue redisValue);

    DataType getType(String key);

    boolean exists(String key);

    boolean delete(String key);

    boolean isType(String key, DataType dataType);

    void cleanup(); // for expired keys

    long getTTL(String key);

    long rpush(String key, List<String> values);
    
    long lpush(String key, List<String> values);



}
