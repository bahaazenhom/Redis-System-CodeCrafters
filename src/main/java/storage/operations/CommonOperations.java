package storage.operations;

import storage.core.DataType;
import storage.core.RedisValue;

import java.util.Set;

public interface CommonOperations {
    
    RedisValue getValue(String key);
    
    void setValue(String key, RedisValue redisValue);
    
    boolean exists(String key);
    
    boolean delete(String key);
    
    DataType getType(String key);
    
    boolean isType(String key, DataType dataType);
    
    long getTTL(String key);
    
    void cleanup();
    
    /**
     * Returns all keys currently stored in the data store.
     * This is primarily used for RDB persistence operations.
     * 
     * @return Set of all keys in the store
     */
    Set<String> getAllKeys();
}
