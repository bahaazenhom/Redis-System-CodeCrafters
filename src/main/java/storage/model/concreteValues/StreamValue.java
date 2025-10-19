package storage.model.concreteValues;

import java.util.HashMap;

import storage.model.DataType;
import storage.model.RedisValue;
import storage.model.MyDoublyLinkedList.MyDoubleLinkedList;
import storage.model.MyDoublyLinkedList.Node;

public class StreamValue extends RedisValue {

    
    HashMap<String, HashMap<String, String>> stream;
    HashMap<String, Node> keyNodes;
    MyDoubleLinkedList entriesKeyTracking;
    
    public StreamValue() {
        super(DataType.STREAM);
        this.stream = new HashMap<>();
        this.keyNodes = new HashMap<>();
        this.entriesKeyTracking = new MyDoubleLinkedList();
    }

    @Override
    public Object getValue() {
        return stream;
    }

    public HashMap<String, HashMap<String, String>> getStream() {
        return stream;
    }

    public HashMap<String, String> put(String key, HashMap<String, String> value){
        Node keyNode = entriesKeyTracking.insert(new Node(key));
        keyNodes.put(key, keyNode);
        return stream.put(key, value);
    }

    public void remove(String key){
        Node keyNode = keyNodes.get(key);
        entriesKeyTracking.remove(keyNode);
        keyNodes.remove(key);
        stream.remove(key);
    }

    public String getLastEntryID(){
        return entriesKeyTracking.getLastInsertedKey();
    }
    
}
