package storage.model.concreteValues;

import java.util.List;

import storage.model.DataType;
import storage.model.RedisValue;

public class ListValue extends RedisValue {
    private List<String> list;

    public ListValue(List<String> list){
        super(DataType.LIST);
        this.list = list;
    }

    public ListValue(List<String> list, Long expiryTime){
        super(DataType.LIST, expiryTime);
        this.list = list;
    }

    @Override
    public Object getValue() {
        return list;
    }

    public List<String> getList(){
        return list;
    }
}
