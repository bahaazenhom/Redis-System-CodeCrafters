package storage.model.concreteValues;

import java.util.Deque;

import storage.model.DataType;
import storage.model.RedisValue;

public class ListValue extends RedisValue {
    private Deque<String> list;

    public ListValue(Deque<String> list){
        super(DataType.LIST);
        this.list = list;
    }

    public ListValue(Deque<String> list, Long expiryTime){
        super(DataType.LIST, expiryTime);
        this.list = list;
    }

    @Override
    public Object getValue() {
        return list;
    }

    public Deque<String> getList(){
        return list;
    }
}
