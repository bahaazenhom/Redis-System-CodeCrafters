package storage.model;

import java.util.List;

public class RedisValue {
    public enum DataType {
    STRING,
    LIST
}
   private final DataType type;    
    private final String stringValue;
    private final List<RedisValue> listValue;
    private final Long expiryTime;

    public RedisValue(String value) {
        this.type = DataType.STRING;
        this.stringValue = value;
        this.listValue = null;
        this.expiryTime = null;
    }

    public RedisValue(String value, Long ttlSeconds) {
        this.type = DataType.STRING;
        this.stringValue = value;
        this.listValue = null;
        this.expiryTime = ttlSeconds;
    }
    public RedisValue(List<RedisValue> listValue) {
        this.type = DataType.LIST;
        this.stringValue = null;
        this.listValue = listValue;
        this.expiryTime = null;
    }
    public RedisValue(List<RedisValue> listValue, Long ttlSeconds) {
        this.type = DataType.LIST;
        this.stringValue = null;
        this.listValue = listValue;
        this.expiryTime = ttlSeconds;
    }
    public DataType getType() {
        return type;
    }

    public boolean isList() {
        return type == DataType.LIST;
    }

    public List<RedisValue> getListValue() {
        if(!isList()) {
            throw new IllegalStateException("Value is not a list");
        }
        return listValue;
    }

    public String getValue() {
        return stringValue;
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
