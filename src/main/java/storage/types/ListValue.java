package storage.types;

import java.util.Deque;

import storage.core.DataType;
import storage.core.RedisValue;

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
