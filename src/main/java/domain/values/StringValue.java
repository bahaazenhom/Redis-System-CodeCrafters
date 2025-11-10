package domain.values;

import domain.DataType;
import domain.RedisValue;

public class StringValue extends RedisValue {
    private String value;

    public StringValue(String value){
        super(DataType.STRING);
        this.value = value;
    }

    public StringValue(String value, Long expiryTime){
        super(DataType.STRING, expiryTime);
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    public String getString(){
        return value;
    }
    
}
