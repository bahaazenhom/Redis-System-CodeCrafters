package domain.values;

import domain.DataType;
import domain.RedisValue;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProperties extends RedisValue {
    private final Map<String, List<String>> userProperties;

    public UserProperties() {
        super(DataType.PROPERTY_VALUE);
        this.userProperties = new HashMap<>();
    }

    public UserProperties(Long expiryTime) {
        super(DataType.PROPERTY_VALUE, expiryTime);
        this.userProperties = new HashMap<>();
    }


    @Override
    public Map<String, List<String>> getValue() {
        System.out.println("userProperties = " + userProperties);
        return userProperties;
    }
}
