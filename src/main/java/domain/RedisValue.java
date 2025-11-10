package domain;

public abstract class RedisValue {
    protected final DataType type;
    protected Long expiryTime;

    protected RedisValue(DataType type, Long expiryTime) {
        this.type = type;
        if (expiryTime != null) {
            this.expiryTime = expiryTime;
        }
    }

    protected RedisValue(DataType type) {
        this.type = type;
    }

    public abstract Object getValue();

    public DataType getType() {
        return type;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isExpired() {
        return expiryTime != null && System.currentTimeMillis() > expiryTime;
    }

    public boolean hasExpiry() {
        return expiryTime != null;
    }

    public void setExpiry(Long newExpiryTime) {
        if (newExpiryTime != null) {
            this.expiryTime = System.currentTimeMillis() + (newExpiryTime * 1000);
        }
    }

    public void removeExpiry() {
        if (hasExpiry())
            expiryTime = null;
    }
}
