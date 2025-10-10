package storage.model;

public class RedisValue {
    private final String value;
    private final Long expiryTime;

    public RedisValue(String value, Long expiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }

    public RedisValue(String value) {
        this(value, null);
    }

    public String getValue() {
        return value;
    }

    public Long getExpiryTime() {
        return expiryTime;
    }

    public boolean isExpired() {
        return expiryTime != null && System.currentTimeMillis() > expiryTime;
    }

    public boolean hasExpiry() {
        return expiryTime != null;
    }
}
