package storage.repository;

import domain.RedisValue;
import domain.DataType;
import java.util.Set;

public interface CommonRepository {
    
    RedisValue getValue(String key);
    
    void setValue(String key, RedisValue redisValue);
    
    boolean exists(String key);
    
    boolean delete(String key);
    
    DataType getType(String key);
    
    boolean isType(String key, DataType dataType);
    
    long getTTL(String key);
    
    void cleanup();
    
    Set<String> getAllKeys();
}
