package storage;

public interface DataStore {
    void set(String key, String value);
    void setWithExpiry(String key, String value, Long ttlSeconds);
    String get(String key);
    boolean exists(String key);
    boolean delete(String key);
    void cleanup(); // for expired keys
    long getTTL(String key);
}
