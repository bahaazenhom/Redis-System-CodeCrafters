package storage.repository;

import java.util.List;

public interface ListRepository {
    
    long rpush(String key, List<String> values);
    
    long lpush(String key, List<String> values);
    
    String lpop(String key);
    
    List<String> lpop(String key, Long count);
    
    String BLPOP(String key, double timeout) throws InterruptedException;
}
