package storage.operations;

import java.util.HashMap;

import storage.exception.InvalidStreamEntryException;

public interface StreamOperations {
    
    String xadd(String streamKey, String entryID, HashMap<String, String> entryValues) 
        throws InvalidStreamEntryException;
}
