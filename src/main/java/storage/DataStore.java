package storage;

public interface DataStore {
    void set(String key, String value);
    void set(String key, String value, long ttlSeconds);
    String get(String key);
    boolean exists(String key);
    boolean delete(String key);
    void cleanup(); // for expired keys
}
