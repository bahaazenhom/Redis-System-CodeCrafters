package storage.operations;

import storage.core.DataType;
import storage.core.RedisValue;

public interface CommonOperations {
    
    RedisValue getValue(String key);
    
    void setValue(String key, RedisValue redisValue);
    
    boolean exists(String key);
    
    boolean delete(String key);
    
    DataType getType(String key);
    
    boolean isType(String key, DataType dataType);
    
    long getTTL(String key);
    
    void cleanup();
}
